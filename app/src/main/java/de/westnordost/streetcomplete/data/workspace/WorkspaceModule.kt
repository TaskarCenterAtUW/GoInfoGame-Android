package de.westnordost.streetcomplete.data.workspace

import de.westnordost.streetcomplete.data.workspace.data.remote.WorkspaceApiService
import de.westnordost.streetcomplete.data.workspace.data.repository.WorkspaceRepositoryImpl
import de.westnordost.streetcomplete.data.workspace.domain.WorkspaceRepository
import de.westnordost.streetcomplete.screens.workspaces.WorkspaceViewModel
import de.westnordost.streetcomplete.screens.workspaces.WorkspaceViewModelImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val workspaceModule = module {
    factory { WorkspaceDao(get()) }
    single { WorkspaceApiService(get()) }
    single<WorkspaceRepository> { WorkspaceRepositoryImpl(get(), get()) }
    // single { GetWorkspaceUseCase(get()) }
    // single { LoginUseCase(get()) }

    viewModel<WorkspaceViewModel> { WorkspaceViewModelImpl(get()) }

}
