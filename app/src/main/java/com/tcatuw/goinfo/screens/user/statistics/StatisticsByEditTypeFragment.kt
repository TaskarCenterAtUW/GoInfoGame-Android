package com.tcatuw.goinfo.screens.user.statistics

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.edits.EditType
import com.tcatuw.goinfo.data.overlays.OverlayRegistry
import com.tcatuw.goinfo.data.quest.QuestTypeRegistry
import com.tcatuw.goinfo.data.user.statistics.StatisticsSource
import com.tcatuw.goinfo.databinding.FragmentStatisticsBallPitBinding
import com.tcatuw.goinfo.util.ktx.dpToPx
import com.tcatuw.goinfo.util.ktx.viewLifecycleScope
import com.tcatuw.goinfo.util.viewBinding
import com.tcatuw.goinfo.view.CircularOutlineProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/** Shows the user's solved quests of each type in some kind of ball pit. Clicking on each opens
 *  a QuestTypeInfoFragment that shows the quest's details. */
class StatisticsByEditTypeFragment : Fragment(R.layout.fragment_statistics_ball_pit) {
    private val statisticsSource: StatisticsSource by inject()
    private val questTypeRegistry: QuestTypeRegistry by inject()
    private val overlayRegistry: OverlayRegistry by inject()

    interface Listener {
        fun onClickedQuestType(editType: EditType, solvedCount: Int, questBubbleView: View)
    }
    private val listener: Listener? get() = parentFragment as? Listener ?: activity as? Listener

    private val binding by viewBinding(FragmentStatisticsBallPitBinding::bind)

    /* --------------------------------------- Lifecycle ---------------------------------------- */

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycle.addObserver(binding.ballPitView)

        viewLifecycleScope.launch {
            val statistics = withContext(Dispatchers.IO) { statisticsSource.getEditTypeStatistics() }
            binding.ballPitView.setViews(statistics.mapNotNull { statistic ->
                val editType = questTypeRegistry.getByName(statistic.type)
                    ?: overlayRegistry.getByName(statistic.type) ?: return@mapNotNull null
                createEditTypeBubbleView(editType, statistic.count) to statistic.count
            })
        }
    }

    private fun createEditTypeBubbleView(editType: EditType, solvedCount: Int): View {
        val ctx = requireContext()
        val questView = ImageView(ctx)
        questView.id = View.generateViewId()
        questView.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        questView.scaleType = ImageView.ScaleType.FIT_XY
        questView.setImageResource(editType.icon)

        val clickableContainer = FrameLayout(ctx)
        clickableContainer.layoutParams = ViewGroup.LayoutParams(256, 256)
        clickableContainer.foreground = requireContext().getDrawable(R.drawable.round_pressed)
        clickableContainer.elevation = ctx.dpToPx(6)
        clickableContainer.outlineProvider = CircularOutlineProvider
        clickableContainer.addView(questView)
        clickableContainer.setOnClickListener { v ->
            listener?.onClickedQuestType(editType, solvedCount, v)
        }

        return clickableContainer
    }
}
