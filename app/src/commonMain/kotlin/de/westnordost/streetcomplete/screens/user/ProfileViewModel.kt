package de.westnordost.streetcomplete.screens.user

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import de.westnordost.streetcomplete.data.UnsyncedChangesCountSource
import de.westnordost.streetcomplete.data.user.UserDataSource
import de.westnordost.streetcomplete.data.user.UserLoginController
import de.westnordost.streetcomplete.data.user.statistics.CountryStatistics
import de.westnordost.streetcomplete.util.ktx.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

abstract class ProfileViewModel : ViewModel() {
    abstract val userName: StateFlow<String?>
    abstract val achievementLevels: StateFlow<Int>

    abstract val unsyncedChangesCount: StateFlow<Int>

    abstract val datesActive: StateFlow<DatesActiveInRange>
    abstract val daysActive: StateFlow<Int>

    abstract val editCount: StateFlow<Int>
    abstract val editCountCurrentWeek: StateFlow<Int>

    abstract val rank: StateFlow<Int>
    abstract val rankCurrentWeek: StateFlow<Int>

    abstract val biggestSolvedCountCountryStatistics: StateFlow<CountryStatistics?>
    abstract val biggestSolvedCountCurrentWeekCountryStatistics: StateFlow<CountryStatistics?>

    abstract fun logOutUser()
}

@Immutable
data class DatesActiveInRange(val datesActive: List<LocalDate>, val range: Int)

class ProfileViewModelImpl(
    private val userDataSource: UserDataSource,
    private val userLoginController: UserLoginController,
    private val unsyncedChangesCountSource: UnsyncedChangesCountSource,
) : ProfileViewModel() {

    override val userName = MutableStateFlow<String?>(null)
    override val achievementLevels = MutableStateFlow(0)
    override val unsyncedChangesCount = MutableStateFlow(0)
    override val datesActive = MutableStateFlow(DatesActiveInRange(emptyList(), 0))
    override val daysActive = MutableStateFlow(0)
    override val editCount = MutableStateFlow(0)
    override val editCountCurrentWeek = MutableStateFlow(0)
    override val rank = MutableStateFlow(-1)
    override val rankCurrentWeek = MutableStateFlow(-1)
    override val biggestSolvedCountCountryStatistics = MutableStateFlow<CountryStatistics?>(null)
    override val biggestSolvedCountCurrentWeekCountryStatistics =
        MutableStateFlow<CountryStatistics?>(null)

    override fun logOutUser() {
        launch { userLoginController.logOut() }
    }

    private val unsyncedChangesCountListener = object : UnsyncedChangesCountSource.Listener {
        override fun onIncreased() {
            unsyncedChangesCount.update { it + 1 }
        }

        override fun onDecreased() {
            unsyncedChangesCount.update { it - 1 }
        }
    }
    private val userListener = object : UserDataSource.Listener {
        override fun onUpdated() {
            userName.value = userDataSource.userName
        }
    }

    init {
        userName.value = userDataSource.userName

        userDataSource.addListener(userListener)
        unsyncedChangesCountSource.addListener(unsyncedChangesCountListener)
    }

    override fun onCleared() {
        unsyncedChangesCountSource.removeListener(unsyncedChangesCountListener)
        userDataSource.removeListener(userListener)
    }
}
