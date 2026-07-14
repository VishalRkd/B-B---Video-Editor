package com.beatsandbeyond.video_editor.feature.projectdetail

import android.content.Context
import android.net.Uri
import android.view.Surface
import androidx.lifecycle.SavedStateHandle
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.data.engine.KotlinTimelineEngine
import com.beatsandbeyond.video_editor.core.domain.engine.AudioEngine
import com.beatsandbeyond.video_editor.core.domain.engine.ExportEngine
import com.beatsandbeyond.video_editor.core.domain.engine.PreviewEngine
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.AudioBuffer
import com.beatsandbeyond.video_editor.core.domain.model.AudioTrackData
import com.beatsandbeyond.video_editor.core.domain.model.ExportConfig
import com.beatsandbeyond.video_editor.core.domain.model.ExportProgress
import com.beatsandbeyond.video_editor.core.domain.model.PlaybackState
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.model.Track
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import com.beatsandbeyond.video_editor.core.domain.model.VideoFilter
import com.beatsandbeyond.video_editor.core.domain.repository.AssetRepository
import com.beatsandbeyond.video_editor.core.domain.repository.ProjectRepository
import com.beatsandbeyond.video_editor.core.domain.usecase.audio.AddAudioTrackUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.audio.ExtractWaveformUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.audio.SetClipVolumeUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.effects.AddAdjustmentClipUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.effects.AddEffectClipUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.effects.AddOverlayClipUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.effects.AddTextClipUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.effects.ApplyFilterUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.project.GetProjectByIdUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.project.UpdateProjectUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.asset.GetAssetsByProjectIdUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.timeline.AddTransitionUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.timeline.AddVideoClipUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.timeline.DeleteClipUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.timeline.MoveClipUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.timeline.MoveClipsUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.timeline.RemoveTransitionUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.timeline.RippleTrimUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.timeline.SetSpeedUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.timeline.SplitClipUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.timeline.TrimClipUseCase
import com.beatsandbeyond.video_editor.core.utils.CoroutineDispatchers
import com.beatsandbeyond.video_editor.feature.projectdetail.model.ProjectDetailUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.After
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectDetailViewModelTest {

    private lateinit var viewModel: ProjectDetailViewModel
    private lateinit var projectRepository: FakeProjectRepository
    private lateinit var assetRepository: FakeAssetRepository
    private lateinit var previewEngine: FakePreviewEngine
    private lateinit var dispatchers: CoroutineDispatchers

    private val projectId = "project_1"

    @Before
    fun setUp() {
        val testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        dispatchers = CoroutineDispatchers(
            main = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            unconfined = testDispatcher
        )

        projectRepository = FakeProjectRepository()
        assetRepository = FakeAssetRepository()
        previewEngine = FakePreviewEngine()

        val timelineEngine = KotlinTimelineEngine()

        // Seed project and assets
        val timeline = Timeline.empty("t_1", "v_1")
        val project = Project(
            id = projectId,
            name = "Test",
            createdAt = 0L,
            updatedAt = 0L,
            timeline = timeline
        )
        projectRepository.projects.add(project)

        val getProjectByIdUseCase = GetProjectByIdUseCase(projectRepository)
        val updateProjectUseCase = UpdateProjectUseCase(projectRepository)
        val getAssetsByProjectIdUseCase = GetAssetsByProjectIdUseCase(assetRepository)

        viewModel = ProjectDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("projectId" to projectId)),
            getProjectByIdUseCase = getProjectByIdUseCase,
            updateProjectUseCase = updateProjectUseCase,
            getAssetsByProjectIdUseCase = getAssetsByProjectIdUseCase,
            previewEngine = previewEngine,
            trimClipUseCase = TrimClipUseCase(timelineEngine),
            splitClipUseCase = SplitClipUseCase(timelineEngine),
            deleteClipUseCase = DeleteClipUseCase(timelineEngine),
            moveClipUseCase = MoveClipUseCase(timelineEngine),
            moveClipsUseCase = MoveClipsUseCase(timelineEngine),
            rippleTrimUseCase = RippleTrimUseCase(timelineEngine),
            setSpeedUseCase = SetSpeedUseCase(timelineEngine),
            addAudioTrackUseCase = AddAudioTrackUseCase(timelineEngine),
            setClipVolumeUseCase = SetClipVolumeUseCase(timelineEngine),
            addTextClipUseCase = AddTextClipUseCase(timelineEngine),
            addOverlayClipUseCase = AddOverlayClipUseCase(timelineEngine),
            applyFilterUseCase = ApplyFilterUseCase(timelineEngine),
            extractWaveformUseCase = ExtractWaveformUseCase(FakeAudioEngine()),
            audioEngine = FakeAudioEngine(),
            exportEngine = FakeExportEngine(),
            context = DummyContext(),
            assetRepository = assetRepository,
            addVideoClipUseCase = AddVideoClipUseCase(timelineEngine),
            addEffectClipUseCase = AddEffectClipUseCase(timelineEngine),
            addAdjustmentClipUseCase = AddAdjustmentClipUseCase(timelineEngine),
            addTransitionUseCase = AddTransitionUseCase(timelineEngine),
            removeTransitionUseCase = RemoveTransitionUseCase(timelineEngine),
            dispatchers = dispatchers
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loading project updates uiState to Content`() = runTest {
        viewModel.loadProject(projectId)
        val state = viewModel.uiState.value
        assertTrue(state is ProjectDetailUiState.Content)
        val content = state as ProjectDetailUiState.Content
        assertEquals(projectId, content.project.id)
    }

    @Test
    fun `undo redo states reflect in UI state`() = runTest {
        viewModel.loadProject(projectId)
        // Check that initial content cannot undo or redo
        val content = viewModel.uiState.value as ProjectDetailUiState.Content
        assertEquals(false, content.canUndo)
        assertEquals(false, content.canRedo)
    }

    // ── Fakes ───────────────────────────────────────────────────────────────

    class FakeProjectRepository : ProjectRepository {
        val projects = mutableListOf<Project>()
        override fun getAllProjects(): Flow<AppResult<List<Project>>> = flowOf(AppResult.Success(projects))
        override suspend fun getProjectById(id: String): AppResult<Project> {
            val p = projects.find { it.id == id }
            return if (p != null) AppResult.Success(p) else AppResult.Error(NoSuchElementException())
        }
        override suspend fun createProject(project: Project): AppResult<Project> {
            projects.add(project)
            return AppResult.Success(project)
        }
        override suspend fun updateProject(project: Project): AppResult<Project> {
            val idx = projects.indexOfFirst { it.id == project.id }
            if (idx >= 0) projects[idx] = project
            return AppResult.Success(project)
        }
        override suspend fun deleteProject(id: String): AppResult<Unit> {
            projects.removeIf { it.id == id }
            return AppResult.Success(Unit)
        }
    }

    class FakeAssetRepository : AssetRepository {
        override fun getAllAssets(): Flow<AppResult<List<Asset>>> = flowOf(AppResult.Success(emptyList()))
        override fun getAssetsByProject(projectId: String): Flow<AppResult<List<Asset>>> = flowOf(AppResult.Success(emptyList()))
        override suspend fun getAssetById(id: String): AppResult<Asset> = AppResult.Error(NoSuchElementException())
        override suspend fun importAssets(assets: List<Asset>, projectId: String): AppResult<List<Asset>> = AppResult.Success(assets)
        override suspend fun updateAsset(asset: Asset): AppResult<Asset> = AppResult.Success(asset)
        override suspend fun deleteAsset(id: String): AppResult<Unit> = AppResult.Success(Unit)
    }

    class FakePreviewEngine : PreviewEngine {
        override val playbackState = MutableStateFlow(PlaybackState.Idle)
        override val currentPositionMs = MutableStateFlow(0L)
        override val activeClipFilter = MutableStateFlow<VideoFilter>(VideoFilter.None)
        override fun attach(surface: Surface) {}
        override fun detach() {}
        override suspend fun loadTimeline(timeline: Timeline) {}
        override fun play() {}
        override fun pause() {}
        override fun stop() {}
        override suspend fun seekTo(positionMs: Long) {}
        override fun release() {}
    }

    class FakeAudioEngine : AudioEngine {
        override suspend fun extractWaveform(asset: Asset, samplesPerSecond: Int): AppResult<FloatArray> =
            AppResult.Success(floatArrayOf(0.1f, 0.2f))
        
        override suspend fun loadAudioTrack(asset: Asset): AppResult<AudioTrackData> =
            AppResult.Error(UnsupportedOperationException())
            
        override fun mixTracks(tracks: List<AudioTrackData>): Flow<AudioBuffer> =
            flow {}
    }

    class FakeExportEngine : ExportEngine {
        override fun export(project: Project, config: ExportConfig): Flow<ExportProgress> = flow { emit(ExportProgress.Started) }
        override fun cancel(projectId: String) {}
    }

    class DummyContext : Context() {
        override fun getPackageName(): String = "test.package"
        override fun getApplicationContext(): Context = this
        
        override fun getAssets(): android.content.res.AssetManager = TODO()
        override fun getResources(): android.content.res.Resources = TODO()
        override fun getPackageManager(): android.content.pm.PackageManager = TODO()
        override fun getContentResolver(): android.content.ContentResolver = TODO()
        override fun getMainLooper(): android.os.Looper = TODO()
        override fun setTheme(resid: Int) = TODO()
        override fun getTheme(): android.content.res.Resources.Theme = TODO()
        override fun getClassLoader(): ClassLoader = TODO()
        override fun getPackageResourcePath(): String = TODO()
        override fun getPackageCodePath(): String = TODO()
        override fun getSharedPreferences(name: String?, mode: Int): android.content.SharedPreferences = TODO()
        override fun openFileInput(name: String?): java.io.FileInputStream = TODO()
        override fun openFileOutput(name: String?, mode: Int): java.io.FileOutputStream = TODO()
        override fun deleteFile(name: String?): Boolean = TODO()
        override fun getFileStreamPath(name: String?): File = TODO()
        override fun fileList(): Array<String> = TODO()
        override fun getFilesDir(): File = TODO()
        override fun getNoBackupFilesDir(): File = TODO()
        override fun getExternalFilesDir(type: String?): File? = TODO()
        override fun getExternalFilesDirs(type: String?): Array<File> = TODO()
        override fun getObbDir(): File = TODO()
        override fun getObbDirs(): Array<File> = TODO()
        override fun getCacheDir(): File = File("")
        override fun getCodeCacheDir(): File = TODO()
        override fun getExternalCacheDir(): File? = TODO()
        override fun getExternalCacheDirs(): Array<File> = TODO()
        override fun getExternalMediaDirs(): Array<File> = TODO()
        override fun getDir(name: String?, mode: Int): File = TODO()
        override fun openOrCreateDatabase(name: String?, mode: Int, factory: android.database.sqlite.SQLiteDatabase.CursorFactory?): android.database.sqlite.SQLiteDatabase = TODO()
        override fun openOrCreateDatabase(name: String?, mode: Int, factory: android.database.sqlite.SQLiteDatabase.CursorFactory?, errorHandler: android.database.DatabaseErrorHandler?): android.database.sqlite.SQLiteDatabase = TODO()
        override fun deleteDatabase(name: String?): Boolean = TODO()
        override fun databaseList(): Array<String> = TODO()
        override fun getWallpaper(): android.graphics.drawable.Drawable = TODO()
        override fun peekWallpaper(): android.graphics.drawable.Drawable = TODO()
        override fun getWallpaperDesiredMinimumWidth(): Int = TODO()
        override fun getWallpaperDesiredMinimumHeight(): Int = TODO()
        override fun setWallpaper(bitmap: android.graphics.Bitmap?) = TODO()
        override fun setWallpaper(data: java.io.InputStream?) = TODO()
        override fun clearWallpaper() = TODO()
        override fun startActivity(intent: android.content.Intent?) = TODO()
        override fun startActivity(intent: android.content.Intent?, options: android.os.Bundle?) = TODO()
        override fun startActivities(intents: Array<out android.content.Intent>?) = TODO()
        override fun startActivities(intents: Array<out android.content.Intent>?, options: android.os.Bundle?) = TODO()
        override fun startIntentSender(intent: android.content.IntentSender?, fillInIntent: android.content.Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int) = TODO()
        override fun startIntentSender(intent: android.content.IntentSender?, fillInIntent: android.content.Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, options: android.os.Bundle?) = TODO()
        override fun sendBroadcast(intent: android.content.Intent?) = TODO()
        override fun sendBroadcast(intent: android.content.Intent?, receiverPermission: String?) = TODO()
        override fun sendOrderedBroadcast(intent: android.content.Intent?, receiverPermission: String?) = TODO()
        override fun sendOrderedBroadcast(intent: android.content.Intent, receiverPermission: String?, resultReceiver: android.content.BroadcastReceiver?, scheduler: android.os.Handler?, initialCode: Int, initialData: String?, initialExtras: android.os.Bundle?) = TODO()
        override fun sendBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?) = TODO()
        override fun sendBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?, receiverPermission: String?) = TODO()
        override fun sendOrderedBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?, receiverPermission: String?, resultReceiver: android.content.BroadcastReceiver?, scheduler: android.os.Handler?, initialCode: Int, initialData: String?, initialExtras: android.os.Bundle?) = TODO()
        override fun sendStickyBroadcast(intent: android.content.Intent?) = TODO()
        override fun sendStickyOrderedBroadcast(intent: android.content.Intent?, resultReceiver: android.content.BroadcastReceiver?, scheduler: android.os.Handler?, initialCode: Int, initialData: String?, initialExtras: android.os.Bundle?) = TODO()
        override fun removeStickyBroadcast(intent: android.content.Intent?) = TODO()
        override fun sendStickyBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?) = TODO()
        override fun sendStickyOrderedBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?, resultReceiver: android.content.BroadcastReceiver?, scheduler: android.os.Handler?, initialCode: Int, initialData: String?, initialExtras: android.os.Bundle?) = TODO()
        override fun removeStickyBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?) = TODO()
        override fun registerReceiver(receiver: android.content.BroadcastReceiver?, filter: android.content.IntentFilter?): android.content.Intent? = TODO()
        override fun registerReceiver(receiver: android.content.BroadcastReceiver?, filter: android.content.IntentFilter?, flags: Int): android.content.Intent? = TODO()
        override fun registerReceiver(receiver: android.content.BroadcastReceiver?, filter: android.content.IntentFilter?, broadcastPermission: String?, scheduler: android.os.Handler?): android.content.Intent? = TODO()
        override fun registerReceiver(receiver: android.content.BroadcastReceiver?, filter: android.content.IntentFilter?, broadcastPermission: String?, scheduler: android.os.Handler?, flags: Int): android.content.Intent? = TODO()
        override fun unregisterReceiver(receiver: android.content.BroadcastReceiver?) = TODO()
        override fun startService(service: android.content.Intent?): android.content.ComponentName? = TODO()
        override fun startForegroundService(service: android.content.Intent?): android.content.ComponentName? = TODO()
        override fun stopService(service: android.content.Intent?): Boolean = TODO()
        override fun bindService(service: android.content.Intent, conn: android.content.ServiceConnection, flags: Int): Boolean = TODO()
        override fun unbindService(conn: android.content.ServiceConnection) = TODO()
        override fun startInstrumentation(className: android.content.ComponentName, profileFile: String?, arguments: android.os.Bundle?): Boolean = TODO()
        override fun getSystemService(name: String): Any = TODO()
        override fun getSystemServiceName(serviceClass: Class<*>): String? = TODO()
        override fun checkPermission(permission: String, pid: Int, uid: Int): Int = TODO()
        override fun checkCallingPermission(permission: String): Int = TODO()
        override fun checkCallingOrSelfPermission(permission: String): Int = TODO()
        override fun checkSelfPermission(permission: String): Int = TODO()
        override fun enforcePermission(permission: String, pid: Int, uid: Int, message: String?) = TODO()
        override fun enforceCallingPermission(permission: String, message: String?) = TODO()
        override fun enforceCallingOrSelfPermission(permission: String, message: String?) = TODO()
        override fun grantUriPermission(toPackage: String?, uri: android.net.Uri?, modeFlags: Int) = TODO()
        override fun revokeUriPermission(uri: android.net.Uri?, modeFlags: Int) = TODO()
        override fun revokeUriPermission(toPackage: String?, uri: android.net.Uri?, modeFlags: Int) = TODO()
        override fun checkUriPermission(uri: android.net.Uri?, pid: Int, uid: Int, modeFlags: Int): Int = TODO()
        override fun checkUriPermission(uri: android.net.Uri?, readPermission: String?, writePermission: String?, pid: Int, uid: Int, modeFlags: Int): Int = TODO()
        override fun checkCallingUriPermission(uri: android.net.Uri?, modeFlags: Int): Int = TODO()
        override fun checkCallingOrSelfUriPermission(uri: android.net.Uri?, modeFlags: Int): Int = TODO()
        override fun enforceUriPermission(uri: android.net.Uri?, pid: Int, uid: Int, modeFlags: Int, message: String?) = TODO()
        override fun enforceUriPermission(uri: android.net.Uri?, readPermission: String?, writePermission: String?, pid: Int, uid: Int, modeFlags: Int, message: String?) = TODO()
        override fun enforceCallingUriPermission(uri: android.net.Uri?, modeFlags: Int, message: String?) = TODO()
        override fun enforceCallingOrSelfUriPermission(uri: android.net.Uri?, modeFlags: Int, message: String?) = TODO()
        override fun createPackageContext(packageName: String?, flags: Int): Context = TODO()
        override fun createConfigurationContext(overrideConfiguration: android.content.res.Configuration): Context = TODO()
        override fun createDisplayContext(display: android.view.Display): Context = TODO()
        override fun createDeviceProtectedStorageContext(): Context = TODO()
        override fun isDeviceProtectedStorage(): Boolean = TODO()
        override fun getApplicationInfo(): android.content.pm.ApplicationInfo = TODO()
        override fun getDatabasePath(name: String?): File = TODO()
        override fun getDataDir(): File = TODO()
        override fun moveDatabaseFrom(sourceContext: Context?, name: String?): Boolean = TODO()
        override fun moveSharedPreferencesFrom(sourceContext: Context?, name: String?): Boolean = TODO()
        override fun createContextForSplit(splitName: String?): Context = TODO()
        override fun deleteSharedPreferences(name: String?): Boolean = TODO()
    }
}
