package com.tcatuw.goinfo.overlays.way_lit

import android.os.Bundle
import android.view.View
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapChangesBuilder
import com.tcatuw.goinfo.data.osm.edits.update_tags.UpdateElementTagsAction
import com.tcatuw.goinfo.osm.changeToSteps
import com.tcatuw.goinfo.osm.lit.LitStatus
import com.tcatuw.goinfo.osm.lit.LitStatus.AUTOMATIC
import com.tcatuw.goinfo.osm.lit.LitStatus.NIGHT_AND_DAY
import com.tcatuw.goinfo.osm.lit.LitStatus.NO
import com.tcatuw.goinfo.osm.lit.LitStatus.UNSUPPORTED
import com.tcatuw.goinfo.osm.lit.LitStatus.YES
import com.tcatuw.goinfo.osm.lit.applyTo
import com.tcatuw.goinfo.osm.lit.asItem
import com.tcatuw.goinfo.osm.lit.createLitStatus
import com.tcatuw.goinfo.overlays.AImageSelectOverlayForm
import com.tcatuw.goinfo.overlays.AnswerItem
import com.tcatuw.goinfo.util.ktx.couldBeSteps
import com.tcatuw.goinfo.view.image_select.DisplayItem

class WayLitOverlayForm : AImageSelectOverlayForm<LitStatus>() {

    override val items: List<DisplayItem<LitStatus>> =
        listOf(YES, NO, AUTOMATIC, NIGHT_AND_DAY).map { it.asItem() }

    private var originalLitStatus: LitStatus? = null

    override val otherAnswers get() = listOfNotNull(
        createConvertToStepsAnswer()
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val litStatus = createLitStatus(element!!.tags)
        originalLitStatus = if (litStatus != UNSUPPORTED) litStatus else null
        selectedItem = originalLitStatus?.asItem()
    }

    override fun hasChanges(): Boolean =
        selectedItem?.value != originalLitStatus

    override fun onClickOk() {
        val tagChanges = StringMapChangesBuilder(element!!.tags)
        selectedItem!!.value!!.applyTo(tagChanges)
        applyEdit(UpdateElementTagsAction(element!!, tagChanges.create()))
    }

    private fun createConvertToStepsAnswer(): AnswerItem? =
        if (element!!.couldBeSteps()) AnswerItem(R.string.quest_generic_answer_is_actually_steps) { changeToSteps() }
        else null

    private fun changeToSteps() {
        val tagChanges = StringMapChangesBuilder(element!!.tags)
        tagChanges.changeToSteps()
        applyEdit(UpdateElementTagsAction(element!!, tagChanges.create()))
    }
}
