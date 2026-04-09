@file:OptIn(ExperimentalMaterial3Api::class)

package de.westnordost.streetcomplete.screens.main.accessibility

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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
                    contentDescription = "Close dialog"
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
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics {
                // This provides a "flat" string for TalkBack to read
                // while the visual remains styled.
                contentDescription = "Type: $type"
            }
        )
        Spacer(Modifier.height(4.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append("Date & Time: ")
                }
                append(dateTime)
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics {
                // This provides a "flat" string for TalkBack to read
                // while the visual remains styled.
                contentDescription = "Date & Time: $dateTime"
            }
        )

        Spacer(Modifier.height(16.dp))
        DashedDivider()

        if (edit != null) {
            SelectionContainer {
                EditDescription(edit, modifier = Modifier.padding(24.dp))
            }
        }
        DashedDivider()
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onRevertClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary // red/pink
            )
        ) {
            Text(
                text = "Revert Changes",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onCancelClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            border = ButtonDefaults.outlinedButtonBorder(true).copy(
                width = 1.5.dp,
                brush = SolidColor(MaterialTheme.colorScheme.primary)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
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
