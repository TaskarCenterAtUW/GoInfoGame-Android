package com.tcatuw.goinfo.quests.max_weight

sealed interface MaxWeightAnswer

data class MaxWeight(val sign: MaxWeightSign, val weight: Weight) : MaxWeightAnswer
object NoMaxWeightSign : MaxWeightAnswer
