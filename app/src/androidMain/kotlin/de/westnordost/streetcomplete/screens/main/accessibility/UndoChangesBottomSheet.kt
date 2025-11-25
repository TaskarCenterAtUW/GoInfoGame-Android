@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.westnordost.streetcomplete.data.edithistory.Edit
import de.westnordost.streetcomplete.screens.main.edithistory.EditDescription

@Composable
fun UndoChangesBottomSheetContent(
    edit: Edit?,
    type: String,
    dateTime: String,
    onRevertClick: () -> Unit,
    onCancelClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {

        // Title + close
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Undo the following changes?",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                ),
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        DashedDivider()
        Spacer(Modifier.height(12.dp))

        // Type + Date
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append("Type: ")
                }
                append(type)
            },
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append("Date & Time: ")
                }
                append(dateTime)
            },
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(16.dp))
        DashedDivider()

        if (edit != null) {
            SelectionContainer {
                EditDescription(edit)
            }
        }
        Spacer(Modifier.height(16.dp))

        // Changes list (static example – plug your own data)
        // ChangeSectionHeader("ADDED")
        // ChangeRow("ext:surface", "concrete")
        // Spacer(Modifier.height(8.dp))
        //
        // ChangeSectionHeader("ADDED")
        // ChangeRow("ext:obstruction", "no")
        // Spacer(Modifier.height(8.dp))
        //
        // ChangeSectionHeader("MODIFIED")
        // ChangeRow("ext:gig_complete", "yes")
        // Spacer(Modifier.height(8.dp))
        //
        // ChangeSectionHeader("ADDED")
        // ChangeRow("ext:surface", "concrete")
        // Spacer(Modifier.height(8.dp))
        //
        // ChangeSectionHeader("MODIFIED")
        // ChangeRow("ext:surface", "concrete")
        //
        // Spacer(Modifier.height(24.dp))

        // Buttons
        Button(
            onClick = onRevertClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE94057) // red/pink
            )
        ) {
            Text(
                text = "Revert Changes",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onCancelClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                width = 2.dp
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF3A0CA3) // purple
            )
        ) {
            Text(
                text = "Cancel",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

// ---------- Small helper composables ----------

@Composable
private fun ChangeSectionHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
            color = Color(0xFFB0B0B0),
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    )
}

@Composable
private fun ChangeRow(key: String, value: String) {
    Row {
        Text(
            text = key,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            )
        )
        Text(
            text = "  =  ",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DashedDivider() {
    val color = Color(0xFFE0E0E0)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        val dashWidth = 12f
        val dashGap = 8f
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(dashWidth, dashGap), 0f
            )
        )
    }
}
