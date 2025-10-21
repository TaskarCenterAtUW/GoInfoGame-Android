package de.westnordost.streetcomplete.ui.common.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.window.DialogProperties
import de.westnordost.streetcomplete.resources.Res
import de.westnordost.streetcomplete.resources.cancel
import de.westnordost.streetcomplete.resources.ok
import org.jetbrains.compose.resources.stringResource

/** Slight specialization of an alert dialog: AlertDialog with OK and Cancel button. Both buttons
 *  call [onDismissRequest] and the OK button additionally calls [onConfirmed]. */
@Composable
fun ConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButtonText: String = stringResource(Res.string.ok),
    cancelButtonText: String = stringResource(Res.string.cancel),
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    properties: DialogProperties = DialogProperties(),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = { onConfirmed(); onDismissRequest() }) { Text(confirmButtonText) }
        },
        modifier = modifier,
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(cancelButtonText) } },
        title = title,
        text = text,
        shape = shape,
        containerColor = backgroundColor,
        iconContentColor = contentColor,
        titleContentColor = contentColor,
        textContentColor = contentColor,
        properties = properties,
    )
}
