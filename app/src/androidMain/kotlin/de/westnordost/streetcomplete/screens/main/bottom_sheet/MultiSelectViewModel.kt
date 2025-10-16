package de.westnordost.streetcomplete.screens.main.bottom_sheet

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MultiSelectViewModel : ViewModel() {
    val dynamicText: MutableLiveData<String> = MutableLiveData()
}
