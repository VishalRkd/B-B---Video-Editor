package com.videoeditorpro.android.domain.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.videoeditorpro.android.domain.model.EditorProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Handles persistent storage of video editing project drafts on disk.
 * Uses JSON serialization to save and restore full project state.
 */
class ProjectDraftRepository(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val draftsDir: File
        get() = File(context.filesDir, "drafts").apply { if (!exists()) mkdirs() }

    suspend fun saveProject(project: EditorProject): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(draftsDir, "${project.id}.json")
            val json = gson.toJson(project)
            file.writeText(json)
            Log.d(TAG, "Project saved: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save project draft", e)
            false
        }
    }

    suspend fun loadProject(projectId: String): EditorProject? = withContext(Dispatchers.IO) {
        try {
            val file = File(draftsDir, "$projectId.json")
            if (!file.exists()) return@withContext null
            val json = file.readText()
            gson.fromJson(json, EditorProject::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load project draft: $projectId", e)
            null
        }
    }

    suspend fun listDrafts(): List<EditorProject> = withContext(Dispatchers.IO) {
        val list = mutableListOf<EditorProject>()
        val files = draftsDir.listFiles { f -> f.extension == "json" } ?: return@withContext emptyList()
        for (file in files.sortedByDescending { it.lastModified() }) {
            try {
                val proj = gson.fromJson(file.readText(), EditorProject::class.java)
                if (proj != null) list.add(proj)
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing draft file: ${file.name}", e)
            }
        }
        list
    }

    suspend fun deleteDraft(projectId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(draftsDir, "$projectId.json")
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete draft: $projectId", e)
            false
        }
    }

    companion object {
        private const val TAG = "ProjectDraftRepository"
    }
}
