package de.westnordost.streetcomplete.view

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.annotation.DrawableRes
import coil.ImageLoader
import coil.disk.DiskCache
import coil.dispose
import coil.load
import coil.request.CachePolicy
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.preferences.Preferences
import org.koin.java.KoinJavaComponent.inject

/* Same idea here as the Icon class introduced in min API level 23. If the min API level is
   Build.VERSION_CODES_M, usage of this class can be replaced with Icon */

sealed interface Image
data class ResImage(@DrawableRes val resId: Int) : Image
data class DrawableImage(val drawable: Drawable) : Image
data class ImageUrl(val url: String? = "https://picsum.photos/320/480") : Image

// Built once and reused for every load. Building a new ImageLoader (and DiskCache pointed at the
// same cache directory) per bind caused concurrent DiskCache instances to fight over the same
// cache journal, so most simultaneous loads (e.g. several grid cells sharing one URL) silently failed.
private var sharedImageLoader: ImageLoader? = null

private fun sharedImageLoader(context: android.content.Context): ImageLoader =
    sharedImageLoader ?: ImageLoader.Builder(context.applicationContext)
        .diskCache {
            DiskCache.Builder()
                .directory(context.applicationContext.cacheDir.resolve("image_cache"))
                .maxSizePercent(0.02) // 2% of app storage
                .build()
        }
        .memoryCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
        .build()
        .also { sharedImageLoader = it }

fun ImageView.setImage(
    image: Image?,
    imageIsEmptyUpdateTextSize: () -> Unit = {},
    progressBar: ProgressBar? = null,
) {
    val preferences: Preferences by inject(Preferences::class.java)

    when (image) {
        is ResImage -> {
            this.dispose()
            progressBar?.visibility = View.GONE
            setImageResource(image.resId)
        }

        is DrawableImage -> {
            this.dispose()
            progressBar?.visibility = View.GONE
            setImageDrawable(image.drawable)
        }

        is ImageUrl -> {
            val url = image.url
            if (url.isNullOrEmpty() || preferences.isLowBandwidthModeEnabled) {
                this.dispose()
                progressBar?.visibility = View.GONE
                setImageResource(R.drawable.blank_big)
                imageIsEmptyUpdateTextSize()
            } else {
                // Show progress bar when starting to load from URL
                progressBar?.visibility = View.VISIBLE
                this.load(url, sharedImageLoader(context)) {
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
            this.dispose()
            progressBar?.visibility = View.GONE
            setImageDrawable(null)
        }
    }
}
