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
            // In multi-select, the answer is about to be applied to several elements at once -
            // pre-filling from just the primary element's tags (and treating those questions as
            // "already answered") doesn't make sense when other selected elements may have
            // different or no existing values. seedFrom(emptyMap()) blanks every field the same
            // way it already does for a tag that's absent - see seedFrom's own reset behavior.
            val tags = if (isMultiSelectActive) emptyMap() else element.tags
            quests?.forEach { quest ->
                quest?.seedFrom(tags)
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

