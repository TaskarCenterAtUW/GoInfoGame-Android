package com.tcatuw.goinfo.quests.max_height

import com.tcatuw.goinfo.osm.Length

sealed interface MaxHeightAnswer

data class MaxHeight(val value: Length) : MaxHeightAnswer
data class NoMaxHeightSign(val isTallEnough: Boolean) : MaxHeightAnswer
