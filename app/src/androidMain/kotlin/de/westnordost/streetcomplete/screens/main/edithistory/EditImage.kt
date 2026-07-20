package de.westnordost.streetcomplete.screens.main.edithistory

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import de.westnordost.streetcomplete.data.edithistory.Edit
import de.westnordost.streetcomplete.data.osm.edits.ElementEdit
import de.westnordost.streetcomplete.data.osm.edits.create.CreateNodeAction
import de.westnordost.streetcomplete.quests.create_feature.AddFeaturePreset
import de.westnordost.streetcomplete.quests.create_feature.CustomIconCache
import de.westnordost.streetcomplete.quests.create_feature.FeaturePresetCatalog
import de.westnordost.streetcomplete.view.presetIconIndex
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

/** Icon representing an edit (main icon + overlay icon) */
@Composable
fun EditImage(
    edit: Edit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val presetPainter = featurePresetPainter(edit)
        if (presetPainter != null) {
            // preset glyphs have tiny intrinsic sizes - scale and center them in the box
            Image(
                painter = presetPainter,
                contentDescription = null,
                modifier = Modifier
                    .size(maxWidth * 0.75f)
                    .align(Alignment.Center)
            )
        } else {
            val editIcon = edit.icon
            if (editIcon != 0) {
                Image(
                    painterResource(edit.icon), null, modifier = Modifier
                        .size(maxWidth * 0.75f)
                        .align(Alignment.Center)
                )
            }
        }
        val overlayIcon = edit.overlayIcon
        if (overlayIcon != null) {
            Image(
                painter = painterResource(overlayIcon),
                contentDescription = null,
                modifier = Modifier
                    .size(maxWidth * 0.75f)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

/** For a node created from a workspace feature preset, the icon of the actual feature that was
 *  added (e.g. a bench) instead of the generic edit-type pin; null for every other edit. */
@Composable
private fun featurePresetPainter(edit: Edit): Painter? {
    if (edit !is ElementEdit || edit.type !is AddFeaturePreset) return null
    val action = edit.action as? CreateNodeAction ?: return null

    val catalog = koinInject<FeaturePresetCatalog>()
    val iconName = catalog.findPresetFor(action.tags)?.icon ?: return null

    val context = LocalContext.current
    val resId = presetIconIndex[iconName]
        ?: context.resources.getIdentifier(iconName, "drawable", context.packageName)
            .takeIf { it != 0 }
    if (resId != null) return painterResource(resId)

    // workspace custom icon: only served from the persistent cache; SVGs and not-yet-downloaded
    // icons fall back to the edit type's default icon (null)
    val url = catalog.findCustomIconUrl(iconName) ?: return null
    val iconCache = koinInject<CustomIconCache>()
    return remember(url) {
        iconCache.getCached(url)
            ?.let { BitmapFactory.decodeFile(it.path) }
            ?.let { BitmapPainter(it.asImageBitmap()) }
    }
}
