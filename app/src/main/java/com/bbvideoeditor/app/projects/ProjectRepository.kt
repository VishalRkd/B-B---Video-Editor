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
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.coroutineContext

/** Disk operations are serialized, atomic, and never run on the UI thread. */
class ProjectRepository(context: Context) {
    private val directory = File(context.applicationContext.filesDir, "projects")
    private val preferences = context.applicationContext.getSharedPreferences("editor_projects", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val _issues = MutableStateFlow<List<String>>(emptyList())
    val issues: StateFlow<List<String>> = _issues.asStateFlow()

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
            val serialized = json.encodeToString(project).toByteArray(Charsets.UTF_8)
            require(serialized.size <= MAX_PROJECT_BYTES) { "The project file exceeds the supported size" }
            coroutineContext.ensureActive()
            // Preserve the previous known-good snapshot independently of AtomicFile's write recovery.
            if (current.exists()) {
                runCatching { read(current, project.id) }.getOrNull()?.let { previous ->
                    writeAtomic(backupFile(project.id), json.encodeToString(previous).toByteArray(Charsets.UTF_8))
                }
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

    suspend fun lastProjectId(): String? = withContext(Dispatchers.IO) {
        preferences.getString(LAST_PROJECT, null)
    }

    suspend fun rememberLastProject(id: String?) = withContext(Dispatchers.IO) {
        if (id != null) checkId(id)
        check(preferences.edit().putString(LAST_PROJECT, id).commit()) { "Could not update recent projects" }
    }

    private fun readWithRecovery(id: String, problems: MutableList<String>): Project {
        return try {
            read(projectFile(id), id)
        } catch (original: Exception) {
            val recovered = try {
                read(backupFile(id), id)
            } catch (_: Exception) {
                throw IOException("Project is unavailable or damaged", original)
            }
            writeAtomic(projectFile(id), json.encodeToString(recovered).toByteArray(Charsets.UTF_8))
            problems += "Recovered \"${recovered.name}\" from its previous saved version."
            recovered
        }
    }

    private fun read(file: File, expectedId: String): Project {
        val bytes = AtomicFile(file).openRead().use { input ->
            require(input.channel.size() <= MAX_PROJECT_BYTES) { "Project file is too large" }
            input.readBytes()
        }
        return json.decodeFromString<Project>(bytes.toString(Charsets.UTF_8)).upgradeSchema().also {
            require(it.id == expectedId) { "Project identifier does not match its file" }
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
