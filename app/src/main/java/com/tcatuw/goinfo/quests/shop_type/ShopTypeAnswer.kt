package com.tcatuw.goinfo.quests.shop_type

sealed interface ShopTypeAnswer

object IsShopVacant : ShopTypeAnswer
data class ShopType(val tags: Map<String, String>) : ShopTypeAnswer
