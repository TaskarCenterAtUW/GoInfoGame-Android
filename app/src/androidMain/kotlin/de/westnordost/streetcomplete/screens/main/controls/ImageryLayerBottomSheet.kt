package de.westnordost.streetcomplete.screens.main.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.util.satellite_layers.Imagery

/** Bottom sheet in which the user can choose the imagery source shown on the map, shown from the
 *  map's imagery layer button */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageryLayerBottomSheet(
    imageryList: List<Imagery>,
    selectedImagery: Imagery?,
    onSelectImagery: (Imagery?) -> Unit,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    // the imagery icons are served as SVGs, which coil's default ImageLoader can't decode
    val svgImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    val maxSheetHeight =  LocalWindowInfo.current.containerSize.height.dp * 0.85f

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.select_imagery_source),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Choose the map imagery shown while surveying",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_button),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    ImageryRow(
                        name = stringResource(R.string.default_imagery),
                        iconUrl = null,
                        imageLoader = svgImageLoader,
                        selected = selectedImagery == null,
                        onClick = { onSelectImagery(null) }
                    )
                }
                items(imageryList, key = { it.id }) { imagery ->
                    ImageryRow(
                        name = imagery.name,
                        iconUrl = imagery.icon,
                        imageLoader = svgImageLoader,
                        selected = imagery == selectedImagery,
                        onClick = { onSelectImagery(imagery) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageryRow(
    name: String,
    iconUrl: String?,
    imageLoader: ImageLoader,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        if (iconUrl != null) {
            AsyncImage(
                model = iconUrl,
                imageLoader = imageLoader,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(28.dp)
                    .clip(CircleShape)
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
