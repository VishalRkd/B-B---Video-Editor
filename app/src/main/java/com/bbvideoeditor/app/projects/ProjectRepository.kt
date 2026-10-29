package com.bbvideoeditor.app.projects

import android.content.Context
import android.util.AtomicFile
import com.bbvideoeditor.core.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import com.bbvideoeditor.core.CURRENT_PROJECT_SCHEMA
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.coroutineContext

enum class ProjectReadFailure { CORRUPT_DOCUMENT, UNSUPPORTED_SCHEMA, MISSING_FILE, STORAGE_FAILURE }

class ProjectReadException(val reason: ProjectReadFailure, message: String, cause: Throwable? = null) : IOException(message, cause)

enum class ProjectRecoveryKind { PREVIOUS_VERSION, INTERRUPTED_WRITE }
data class ProjectRecovery(val projectId: String, val kind: ProjectRecoveryKind)

/** Disk operations are serialized, atomic, and never run on the UI thread. */
class ProjectRepository(context: Context, private val directory:File = File(context.applicationContext.filesDir,"projects")) {
    private val preferences = context.applicationContext.getSharedPreferences("editor_projects", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val _issues = MutableStateFlow<List<String>>(emptyList())
    val issues: StateFlow<List<String>> = _issues.asStateFlow()
    private val _recoveries = MutableStateFlow<List<ProjectRecovery>>(emptyList())
    val recoveries: StateFlow<List<ProjectRecovery>> = _recoveries.asStateFlow()

    suspend fun list(): List<Project> = withContext(Dispatchers.IO) {
        mutex.withLock {
            directory.mkdirs()
            val problems = mutableListOf<String>()
            // Include interrupted AtomicFile writes, whose committed data may be in .bak.
            val ids = directory.listFiles().orEmpty().mapNotNull { file ->
                when {
                    file.name.endsWith(".json") -> file.name.removeSuffix(".json")
                    file.name.endsWith(".json.bak") -> file.name.removeSuffix(".json.bak")
                    file.name.endsWith(".previous") -> file.name.removeSuffix(".previous")
                    else -> null
                }
            }.distinct()
            val projects = ids.mapNotNull { id ->
                coroutineContext.ensureActive()
                try {
                    readWithRecovery(id, problems)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    problems += "Could not open project $id: ${error.message ?: "damaged project file"}"
                    null
                }
            }
            _issues.value = problems
            projects.sortedByDescending { it.updatedAt }
        }
    }

    suspend fun load(id: String): Project = withContext(Dispatchers.IO) {
        mutex.withLock {
            val problems = mutableListOf<String>()
            readWithRecovery(id, problems).also { _issues.value = problems }
        }
    }

    suspend fun save(project: Project) = withContext(Dispatchers.IO) {
        project.validate()
        mutex.withLock {
            directory.mkdirs()
            val current = projectFile(project.id)
            for(protected in listOf(File(current.path+".bak"),backupFile(project.id),File(backupFile(project.id).path+".bak"))) {
                if(protected.exists())try{read(protected,project.id)}catch(error:ProjectReadException) {
                    if(error.reason in setOf(ProjectReadFailure.UNSUPPORTED_SCHEMA,ProjectReadFailure.STORAGE_FAILURE))throw error
                }
            }
            val serialized = json.encodeToString(project).toByteArray(Charsets.UTF_8)
            require(serialized.size <= MAX_PROJECT_BYTES) { "The project file exceeds the supported size" }
            coroutineContext.ensureActive()
            // Preserve the previous known-good snapshot independently of AtomicFile's write recovery.
            if (current.exists()) {
                val previous = try { read(current, project.id) } catch (error: ProjectReadException) {
                    // A newer document must also be protected from a stale autosave, not just load.
                    if (error.reason != ProjectReadFailure.CORRUPT_DOCUMENT) throw error
                    null
                }
                previous?.let { writeAtomic(backupFile(project.id), json.encodeToString(it).toByteArray(Charsets.UTF_8)) }
            }
            writeAtomic(current, serialized)
        }
    }

    suspend fun create(name: String): Project {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Enter a project name" }
        require(normalizedName.length <= 120) { "Use a project name of 120 characters or fewer" }
        return Project(name = normalizedName).also { save(it); rememberLastProject(it.id) }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            AtomicFile(projectFile(id)).delete()
            AtomicFile(backupFile(id)).delete()
            if (preferences.getString(LAST_PROJECT, null) == id) {
                check(preferences.edit().remove(LAST_PROJECT).commit()) { "Could not update recent projects" }
            }
        }
    }

    /** Read-only maintenance snapshot. A future/corrupt/unreadable document disables collection;
     * maintenance must never recover or rewrite documents to decide which assets can be removed. */
    suspend fun protectedMediaUris(): Set<String>? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val references=mutableSetOf<String>()
            val files=directory.listFiles() ?: if(directory.exists())return@withLock null else emptyArray()
            for(file in files.filter {
                it.name.endsWith(".json") || it.name.endsWith(".json.bak") || it.name.endsWith(".previous") || it.name.endsWith(".previous.bak")
            }) {
                val id=file.name.removeSuffix(".bak").removeSuffix(".json").removeSuffix(".previous")
                val project=try{read(file,checkId(id))}catch(_:Exception){return@withLock null}
                references+=project.media.values.map{it.uri}
            }
            references
        }
    }

    suspend fun lastProjectId(): String? = withContext(Dispatchers.IO) {
        preferences.getString(LAST_PROJECT, null)
    }

    suspend fun rememberLastProject(id: String?) = withContext(Dispatchers.IO) {
        if (id != null) checkId(id)
        check(preferences.edit().putString(LAST_PROJECT, id).commit()) { "Could not update recent projects" }
    }

    private fun readWithRecovery(id: String, problems: MutableList<String>): Project {
        val current = projectFile(id)
        try { return read(current, id) } catch (original: ProjectReadException) {
            if (original.reason !in setOf(ProjectReadFailure.CORRUPT_DOCUMENT, ProjectReadFailure.MISSING_FILE)) throw original
            // Do not call AtomicFile.openRead before inspecting the current schema: its .bak
            // promotion can itself overwrite a valid future-version current document.
            val candidates = listOf(File(current.path + ".bak") to ProjectRecoveryKind.INTERRUPTED_WRITE,
                backupFile(id) to ProjectRecoveryKind.PREVIOUS_VERSION)
            for ((file, kind) in candidates) {
                val recovered = try { read(file, id) } catch (error: ProjectReadException) {
                    if (error.reason in setOf(ProjectReadFailure.UNSUPPORTED_SCHEMA, ProjectReadFailure.STORAGE_FAILURE)) throw error
                    continue
                }
                writeAtomic(current, json.encodeToString(recovered).toByteArray(Charsets.UTF_8))
                _recoveries.value = (_recoveries.value.filterNot { it.projectId == id } + ProjectRecovery(id, kind)).takeLast(100)
                problems += "Recovered \"${recovered.name}\" from its previous saved version."
                return recovered
            }
            throw original
        }
    }

    private fun read(file: File, expectedId: String): Project {
        val bytes = try {
            file.inputStream().use { input ->
                if (input.channel.size() > MAX_PROJECT_BYTES) throw ProjectReadException(ProjectReadFailure.STORAGE_FAILURE, "Project file exceeds the supported size; it was left unchanged.")
                input.readBytes()
            }
        } catch (error: ProjectReadException) { throw error }
          catch (error: IOException) {
            throw ProjectReadException(if (file.exists()) ProjectReadFailure.STORAGE_FAILURE else ProjectReadFailure.MISSING_FILE,
                if (file.exists()) "Project storage is unavailable; the document was left unchanged." else "Project file is missing.", error)
        }
        val element = try { json.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject }
            catch (error: IllegalArgumentException) { throw ProjectReadException(ProjectReadFailure.CORRUPT_DOCUMENT, "Project JSON is damaged.", error) }
        val version = try { element["schemaVersion"]?.jsonPrimitive?.intOrNull ?: if ("schemaVersion" in element) -1 else CURRENT_PROJECT_SCHEMA }
            catch (error: IllegalArgumentException) { throw ProjectReadException(ProjectReadFailure.CORRUPT_DOCUMENT, "Invalid schema field.", error) }
        if (version > CURRENT_PROJECT_SCHEMA || version == 0) throw ProjectReadException(ProjectReadFailure.UNSUPPORTED_SCHEMA,
            "This project uses schema $version; this app supports up to $CURRENT_PROJECT_SCHEMA. Open it with a compatible version. The document was left unchanged.")
        return try {
            json.decodeFromJsonElement<Project>(element).upgradeSchema().also { require(it.id == expectedId) { "Project identifier does not match its file" } }
        } catch (error: IllegalArgumentException) {
            throw ProjectReadException(ProjectReadFailure.CORRUPT_DOCUMENT, "Project data is invalid.", error)
        }
    }

    private fun writeAtomic(file: File, bytes: ByteArray) {
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        try {
            stream.write(bytes)
            atomic.finishWrite(stream)
        } catch (error: Throwable) {
            atomic.failWrite(stream)
            throw error
        }
    }

    private fun projectFile(id: String): File = File(directory, "${checkId(id)}.json")
    private fun backupFile(id: String): File = File(directory, "${checkId(id)}.previous")
    private fun checkId(id: String): String {
        require(runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)) { "Invalid project identifier" }
        return id
    }

    private companion object {
        const val LAST_PROJECT = "last_project"
        const val MAX_PROJECT_BYTES = 64 * 1024 * 1024
    }
}
