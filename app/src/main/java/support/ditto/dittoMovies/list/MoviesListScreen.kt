package support.ditto.dittoMovies.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import support.ditto.dittoMovies.data.Movie

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesListScreen(navController: NavController) {
    val viewModel: MoviesListScreenViewModel = viewModel()
    val movies: List<Movie> by viewModel.movies.observeAsState(emptyList())
    val syncEnabled: Boolean by viewModel.syncEnabled.observeAsState(true)

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteDialogMovieId by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Ditto Movies",
                            style = MaterialTheme.typography.titleMedium
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
                    items(movies) { movie ->
                        MovieRow(
                            movie = movie,
                            onClick = {
                                navController.navigate("movies/edit/${it._id}")
                            },
                            onClickDelete = {
                                deleteDialogMovieId = it._id
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Confirm Deletion",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(text = "Are you sure you want to delete this movie?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete(deleteDialogMovieId)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
