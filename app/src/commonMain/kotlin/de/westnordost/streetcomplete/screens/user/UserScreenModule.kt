package de.westnordost.streetcomplete.screens.user

import de.westnordost.streetcomplete.screens.user.login.LoginViewModel
import de.westnordost.streetcomplete.screens.user.login.LoginViewModelImpl
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val userScreenModule = module {
    viewModel<ProfileViewModel> {
        ProfileViewModelImpl(
            get(), get(), get()
        )
    }

    viewModel<LoginViewModel> { LoginViewModelImpl(get(), get(), get()) }

    viewModel<UserViewModel> { UserViewModelImpl(get()) }
}
