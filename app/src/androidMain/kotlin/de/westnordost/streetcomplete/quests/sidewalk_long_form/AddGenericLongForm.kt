package de.westnordost.streetcomplete.quests.sidewalk_long_form

import android.os.Bundle
import de.westnordost.streetcomplete.quests.ALongForm
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormQuest
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.seedFrom

class AddGenericLongForm : ALongForm<List<LongFormQuest?>>() {

    override val items: List<LongFormQuest?>
        get() {
            val quests = arguments?.getParcelableArrayList<LongFormQuest>("quests")
            val copy = mutableListOf<LongFormQuest?>()
            quests?.forEach { quest ->
                quest?.seedFrom(element.tags)
                copy.add(quest)
            }
            return copy
        }

    companion object {
        fun newInstance(quests: List<LongFormQuest?>): AddGenericLongForm {
            val fragment = AddGenericLongForm()
            val bundle = Bundle()

            // store list as parcelable array list if LongFormQuest implements Parcelable
            bundle.putParcelableArrayList("quests", ArrayList(quests))
            fragment.arguments = bundle
            return fragment
        }
    }
}

