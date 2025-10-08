package de.westnordost.streetcomplete.data.workspace.domain.use_case.login// package de.westnordost.streetcomplete.data.workspace.domain.use_case.login
//
// import de.westnordost.streetcomplete.common.Resource
// import de.westnordost.streetcomplete.data.workspace.domain.WorkspaceRepository
// import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
// import kotlinx.coroutines.flow.Flow
// import kotlinx.coroutines.flow.flow
//
// class LoginUseCase(private val repository: WorkspaceRepository) {
//     operator fun invoke(username : String, password : String): Flow<Resource<LoginResponse>> = flow {
//         try {
//             emit(Resource.Loading())
//             val response = repository.loginToWorkspace(username, password)
//             emit(Resource.Success(response))
//         } catch (e: Exception) {
//             emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
//         }
//     }
// }
