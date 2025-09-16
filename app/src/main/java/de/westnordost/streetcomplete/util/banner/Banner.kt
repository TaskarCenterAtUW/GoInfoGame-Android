package de.westnordost.streetcomplete.util.banner

import android.R
import androidx.annotation.ColorRes

data class Banner(
    val bannerGravity: BannerGravity = BannerGravity.END,
    @ColorRes val bannerColorRes: Int = R.color.holo_red_dark,
    @ColorRes val textColorRes: Int = R.color.white,
    val bannerText: String = "DEBUG"
)
