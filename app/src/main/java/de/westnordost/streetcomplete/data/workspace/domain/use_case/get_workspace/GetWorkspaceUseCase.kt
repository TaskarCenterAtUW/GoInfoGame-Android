// package de.westnordost.streetcomplete.data.workspace.domain.use_case.get_workspace
//
// import de.westnordost.streetcomplete.common.Resource
// import de.westnordost.streetcomplete.data.workspace.domain.WorkspaceRepository
// import de.westnordost.streetcomplete.data.workspace.domain.model.Workspace
// import kotlinx.coroutines.flow.Flow
// import kotlinx.coroutines.flow.flow
//
// class GetWorkspaceUseCase(private val repository: WorkspaceRepository) {
//     operator fun invoke(): Flow<Resource<List<Workspace>>> = flow {
//         try {
//             emit(Resource.Loading())
//             val workspaces = repository.getWorkspaces()
//             emit(Resource.Success(workspaces))
//         } catch (e: Exception) {
//             emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
//         }
//     }
// }
