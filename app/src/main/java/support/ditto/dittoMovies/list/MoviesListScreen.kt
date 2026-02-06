package support.ditto.dittoMovies.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import support.ditto.dittoMovies.BuildConfig
import support.ditto.dittoMovies.data.Movie

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesListScreen(navController: NavController) {
    val viewModel: MoviesListScreenViewModel = viewModel()
    val movies: List<Movie> by viewModel.movies.collectAsState()
    val syncEnabled: Boolean by viewModel.syncEnabled.collectAsState()
    val showWatched: Boolean by viewModel.showWatched.collectAsState()
    val showDeleted: Boolean by viewModel.showDeleted.collectAsState()


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Ditto Movies",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${movies.size} movies",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Sync",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 10.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Switch(
                            checked = syncEnabled,
                            onCheckedChange = { viewModel.setSyncEnabled(it) }
                        )
                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ditto Tools") },
                                onClick = {
                                    menuExpanded = false
                                    navController.navigate("toolsviewer")
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = showWatched,
                                            onCheckedChange = null
                                        )
                                        Text(
                                            "Show Watched",
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                },
                                onClick = { viewModel.setShowWatched(!showWatched) }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = showDeleted,
                                            onCheckedChange = null
                                        )
                                        Text(
                                            "Show Deleted",
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                },
                                onClick = { viewModel.setShowDeleted(!showDeleted) }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                icon = { Icon(Icons.Filled.Add, "", tint = Color.White) },
                text = { Text(text = "New Movie", color = Color.White) },
                onClick = { navController.navigate("movies/edit") },
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                containerColor = MaterialTheme.colorScheme.primary
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn {
                    item {
                        DittoEnvironmentInfoCard()
                    }
                    items(movies, key = { it._id }) { movie ->
                        MovieRow(
                            movie = movie,
                            onClick = {
                                navController.navigate("movies/${it._id}")
                            }
                        )
                    }
                }
            }
        }
    )

}

@Composable
fun DittoEnvironmentInfoCard() {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = "Ditto Environment",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp
                    else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                    EnvRow(label = "App ID", value = BuildConfig.DITTO_APP_ID)
                    EnvRow(label = "Auth URL", value = BuildConfig.DITTO_AUTH_URL)
                    EnvRow(label = "WebSocket URL", value = BuildConfig.DITTO_WEBSOCKET_URL)
                    EnvRow(label = "Playground Token", value = BuildConfig.DITTO_PLAYGROUND_TOKEN)
                }
            }
        }
    }
}

@Composable
private fun EnvRow(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
