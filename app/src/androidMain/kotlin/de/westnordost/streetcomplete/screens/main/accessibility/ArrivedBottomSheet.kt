package de.westnordost.streetcomplete.screens.main.accessibility

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.westnordost.streetcomplete.screens.user.DottedDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArrivedBottomSheet(
    modifier: Modifier = Modifier,
    questType: String = "Sidewalk",
    onClose: () -> Unit = {},
    onStartAnswering: () -> Unit = {},
    onHide: () -> Unit = {},
    onNotNow: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // header row: title + close circle button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "You've Arrived!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                    ),
                    modifier = Modifier.weight(1f)
                )

                // circular close
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // dashed divider
            DottedDivider()

            Spacer(modifier = Modifier.height(18.dp))

            // center icon
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Nav icon",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // big message
            Text(
                text = "You’ve arrived at the quest location!",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                // center the text
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quest type row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quest Type:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = questType,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    onStartAnswering()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Start answering the questions",
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // Outlined button
            OutlinedButton(
                onClick = {
                    onHide()
                    onClose()
                },
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
                    text = "Hide this quest",
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            // Text button at bottom
            TextButton(
                onClick = {
                    onNotNow()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Not now",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ArrivedBottomSheetPreview() {
    MaterialTheme {
        // to preview like a bottom sheet, add a gray background and place the sheet at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F2F4))
                .padding(bottom = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                ArrivedBottomSheet()
            }
        }
    }
}
