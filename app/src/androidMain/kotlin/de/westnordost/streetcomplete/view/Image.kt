package de.westnordost.streetcomplete.view

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.annotation.DrawableRes
import coil.Coil.setImageLoader
import coil.ImageLoader
import coil.disk.DiskCache
import coil.load
import coil.request.CachePolicy
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.util.logs.Log
import okhttp3.OkHttpClient

/* Same idea here as the Icon class introduced in min API level 23. If the min API level is
   Build.VERSION_CODES_M, usage of this class can be replaced with Icon */

sealed interface Image
data class ResImage(@DrawableRes val resId: Int) : Image
data class DrawableImage(val drawable: Drawable) : Image
data class ImageUrl(val url: String? = "https://picsum.photos/320/480") : Image

fun ImageView.setImage(image: Image?, imageIsEmptyUpdateTextSize: () -> Unit = {}, progressBar: ProgressBar? = null) {

    val customImageLoader = ImageLoader.Builder(context)
        .okHttpClient {
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val newRequest = chain.request().newBuilder()
                        .build()
                    chain.proceed(newRequest)
                }
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(0.02) // 2% of app storage
                .build()
        }
        .memoryCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
        .build()

    when (image) {
        is ResImage -> {
            progressBar?.visibility = View.GONE
            setImageResource(image.resId)
        }
        is DrawableImage -> {
            progressBar?.visibility = View.GONE
            setImageDrawable(image.drawable)
        }
        is ImageUrl -> {
            val url = image.url
            if (url.isNullOrEmpty()) {
                progressBar?.visibility = View.GONE
                setImageResource(R.drawable.blank_big)
                imageIsEmptyUpdateTextSize()
            } else {
                // Show progress bar when starting to load from URL
                progressBar?.visibility = View.VISIBLE
                this.load(url) {
                    setImageLoader(customImageLoader)
                    placeholder(R.drawable.blank_big)
                    error(R.drawable.blank_big)
                    listener(
                        onError = { _, _ ->
                            // Hide progress bar on error
                            progressBar?.visibility = View.GONE
                            setImageResource(R.drawable.blank_big)
                            imageIsEmptyUpdateTextSize()
                        },
                        onSuccess = { _, _ ->
                            // Hide progress bar on success
                            progressBar?.visibility = View.GONE
                        }
                    )
                }
            }
        }

        null -> {
            progressBar?.visibility = View.GONE
            setImageDrawable(null)
        }
    }
}
