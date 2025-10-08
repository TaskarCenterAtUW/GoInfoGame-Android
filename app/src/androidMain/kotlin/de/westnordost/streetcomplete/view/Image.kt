package de.westnordost.streetcomplete.view

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil.Coil.setImageLoader
import coil.ImageLoader
import coil.load
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.util.logs.Log
import okhttp3.OkHttpClient

/* Same idea here as the Icon class introduced in min API level 23. If the min API level is
   Build.VERSION_CODES_M, usage of this class can be replaced with Icon */

sealed interface Image
data class ResImage(@DrawableRes val resId: Int) : Image
data class DrawableImage(val drawable: Drawable) : Image
data class ImageUrl(val url: String? = "https://picsum.photos/320/480") : Image

fun ImageView.setImage(image: Image?) {

    val customImageLoader = ImageLoader.Builder(context)
        .okHttpClient {
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val newRequest = chain.request().newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Android)") // Mimic browser
                        .header(
                            "Referer",
                            "https://png.pngtree.com/"
                        ) // Optional, but often required
                        .build()
                    chain.proceed(newRequest)
                }
                .build()
        }
        .build()

    when (image) {
        is ResImage -> setImageResource(image.resId)
        is DrawableImage -> setImageDrawable(image.drawable)
        is ImageUrl -> this.load(image.url) {
            setImageLoader(customImageLoader)
            placeholder(R.drawable.blank_big)
            error(R.drawable.blank_big)
            listener(
                onError = { _, throwable ->
                    Log.w(
                        "ImageView",
                        "Failed to load image from URL: ${image.url}",
                        throwable.throwable
                    )
                    setImageResource(R.drawable.blank_big)
                },
                onSuccess = { _, _ ->
                    Log.w("ImageView", "Success to load image from URL: ${image.url}")
                }
            )
        }

        null -> setImageDrawable(null)
    }
}
