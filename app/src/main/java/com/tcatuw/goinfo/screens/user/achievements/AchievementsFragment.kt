package com.tcatuw.goinfo.screens.user.achievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.user.achievements.Achievement
import com.tcatuw.goinfo.data.user.achievements.AchievementsSource
import com.tcatuw.goinfo.data.user.statistics.StatisticsSource
import com.tcatuw.goinfo.databinding.CellAchievementBinding
import com.tcatuw.goinfo.databinding.FragmentAchievementsBinding
import com.tcatuw.goinfo.util.ktx.awaitLayout
import com.tcatuw.goinfo.util.ktx.dpToPx
import com.tcatuw.goinfo.util.ktx.viewLifecycleScope
import com.tcatuw.goinfo.util.viewBinding
import com.tcatuw.goinfo.view.GridLayoutSpacingItemDecoration
import com.tcatuw.goinfo.view.ListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/** Shows the icons for all achieved achievements and opens a AchievementInfoFragment to show the
 *  details on click. */
class AchievementsFragment : Fragment(R.layout.fragment_achievements) {

    private val achievementsSource: AchievementsSource by inject()
    private val statisticsSource: StatisticsSource by inject()

    private val binding by viewBinding(FragmentAchievementsBinding::bind)

    private var actualCellWidth: Int = 0

    interface Listener {
        fun onClickedAchievement(achievement: Achievement, level: Int, achievementBubbleView: View)
    }
    private val listener: Listener? get() = parentFragment as? Listener ?: activity as? Listener

    /* --------------------------------------- Lifecycle ---------------------------------------- */

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        val minCellWidth = ctx.dpToPx(144)
        val itemSpacing = ctx.resources.getDimensionPixelSize(R.dimen.achievements_item_margin)

        viewLifecycleScope.launch {
            view.awaitLayout()

            binding.emptyText.visibility = View.GONE

            val spanCount = (view.width / (minCellWidth + itemSpacing)).toInt()
            actualCellWidth = (view.width.toFloat() / spanCount - itemSpacing).toInt()

            val layoutManager = GridLayoutManager(ctx, spanCount, RecyclerView.VERTICAL, false)
            binding.achievementsList.layoutManager = layoutManager
            binding.achievementsList.addItemDecoration(GridLayoutSpacingItemDecoration(itemSpacing))
            binding.achievementsList.clipToPadding = false

            val achievements = withContext(Dispatchers.IO) {
                achievementsSource.getAchievements()
            }
            binding.achievementsList.adapter = AchievementsAdapter(achievements)

            binding.emptyText.isGone = achievements.isNotEmpty()
        }
    }

    override fun onStart() {
        super.onStart()

        if (statisticsSource.isSynchronizing) {
            binding.emptyText.setText(R.string.stats_are_syncing)
        } else {
            binding.emptyText.setText(R.string.achievements_empty)
        }
    }

    /* -------------------------------------- Interaction --------------------------------------- */

    private inner class AchievementsAdapter(
        achievements: List<Pair<Achievement, Int>>
    ) : ListAdapter<Pair<Achievement, Int>>(achievements) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = CellAchievementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            binding.root.updateLayoutParams {
                width = actualCellWidth
                height = actualCellWidth
            }
            return ViewHolder(binding)
        }

        inner class ViewHolder(val binding: CellAchievementBinding) : ListAdapter.ViewHolder<Pair<Achievement, Int>>(binding) {
            override fun onBind(with: Pair<Achievement, Int>) {
                val achievement = with.first
                val level = with.second
                binding.achievementIconView.icon = context?.getDrawable(achievement.icon)
                binding.achievementIconView.level = level
                binding.achievementIconView.setOnClickListener {
                    listener?.onClickedAchievement(achievement, level, binding.achievementIconView)
                }
            }
        }
    }
}
