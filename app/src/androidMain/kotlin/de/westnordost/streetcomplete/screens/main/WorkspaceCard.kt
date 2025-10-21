package de.westnordost.streetcomplete.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WorkspaceCard(
    modifier: Modifier = Modifier,
    workspaceName: String = "Workspace",
    pendingCount: Int = 0,
    onRefreshClick: () -> Unit = {},
    onLayersClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Avatar + Texts
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF5C2D91), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Workspace",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = workspaceName,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp) // spacing between buttons
            ) {
                // Refresh button with badge
                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(
                        onClick = onRefreshClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFD5D1DB), shape = CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }

                    if (pendingCount > 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = 4.dp, y = (-4).dp) // position badge top-right
                                .size(16.dp)
                                .background(Color.Red, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pendingCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onLayersClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFD5D1DB), shape = CircleShape)
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = "Layers",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFD5D1DB), shape = CircleShape)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
            }
        }
    }
}

@Preview()
@Composable
private fun PreviewWorkspaceCard() {
    WorkspaceCard()
}
