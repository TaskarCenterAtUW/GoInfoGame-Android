package de.westnordost.streetcomplete.data.workspace

import de.westnordost.streetcomplete.data.workspace.data.remote.WorkspaceApiService
import de.westnordost.streetcomplete.data.workspace.data.repository.WorkspaceRepositoryImpl
import de.westnordost.streetcomplete.data.workspace.domain.WorkspaceRepository
import de.westnordost.streetcomplete.screens.workspaces.WorkspaceViewModel
import de.westnordost.streetcomplete.screens.workspaces.WorkspaceViewModelImpl
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

val workspaceModule = module {
    factory { WorkspaceDao(get()) }
    single { WorkspaceApiService(get(), get(), get(), get(), get(named("osmClient"))) }
    single<WorkspaceRepository> { WorkspaceRepositoryImpl(get(), get()) }
    // single { GetWorkspaceUseCase(get()) }
    // single { LoginUseCase(get()) }

    viewModel<WorkspaceViewModel> {
        // app-specific external storage - no runtime permission needed to read/write it, and
        // `adb push`/`adb pull` can reach it without root, so the test long-form JSON can be
        // edited on-device without rebuilding the app. See WorkspaceViewModelImpl.readTestLongFormJson.
        val testLongFormJsonFile = File(androidContext().getExternalFilesDir(null), "test_workspace_longform.json")
        WorkspaceViewModelImpl(get(), get(), get(), get(), testLongFormJsonFile)
    }
}
