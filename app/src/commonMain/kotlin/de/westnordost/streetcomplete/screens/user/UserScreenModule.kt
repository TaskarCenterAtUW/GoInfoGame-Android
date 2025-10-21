package de.westnordost.streetcomplete.screens.user

import de.westnordost.streetcomplete.screens.user.login.LoginViewModel
import de.westnordost.streetcomplete.screens.user.login.LoginViewModelImpl
import de.westnordost.streetcomplete.screens.user.profile.ProfileViewModel
import de.westnordost.streetcomplete.screens.user.profile.ProfileViewModelImpl
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val userScreenModule = module {
    viewModel<ProfileViewModel> {
        ProfileViewModelImpl(
            get(), get(), get(), get(), get(), get(), get(), get(named("AvatarsCacheDirectory"))
        )
    }

    viewModel<LoginViewModel> { LoginViewModelImpl(get(), get(), get()) }

    viewModel<UserViewModel> { UserViewModelImpl(get()) }
}
