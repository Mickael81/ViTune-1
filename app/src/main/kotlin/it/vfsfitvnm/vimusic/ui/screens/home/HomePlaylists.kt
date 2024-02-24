package it.vfsfitvnm.vimusic.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.compose.persist.persistList
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.enums.BuiltInPlaylist
import it.vfsfitvnm.vimusic.enums.PlaylistSortBy
import it.vfsfitvnm.vimusic.enums.SortOrder
import it.vfsfitvnm.vimusic.models.Playlist
import it.vfsfitvnm.vimusic.models.PlaylistPreview
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.ui.components.themed.TextFieldDialog
import it.vfsfitvnm.vimusic.ui.items.PlaylistItem
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.px
import it.vfsfitvnm.vimusic.utils.playlistSortByKey
import it.vfsfitvnm.vimusic.utils.playlistSortOrderKey
import it.vfsfitvnm.vimusic.utils.rememberPreference

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun HomePlaylists(
    onBuiltInPlaylist: (BuiltInPlaylist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit
) {
    var isCreatingANewPlaylist by rememberSaveable {
        mutableStateOf(false)
    }

    if (isCreatingANewPlaylist) {
        TextFieldDialog(
            hintText = stringResource(id = R.string.playlist_name_hint),
            onDismiss = {
                isCreatingANewPlaylist = false
            },
            onDone = { text ->
                query {
                    Database.insert(Playlist(name = text))
                }
            }
        )
    }

    var sortBy by rememberPreference(playlistSortByKey, PlaylistSortBy.Name)
    var sortOrder by rememberPreference(playlistSortOrderKey, SortOrder.Ascending)

    var items by persistList<PlaylistPreview>("home/playlists")

    LaunchedEffect(sortBy, sortOrder) {
        Database.playlistPreviews(sortBy, sortOrder).collect { items = it }
    }

    val thumbnailSizeDp = 108.dp
    val thumbnailSizePx = thumbnailSizeDp.px

    val sortOrderIconRotation by animateFloatAsState(
        targetValue = if (sortOrder == SortOrder.Ascending) 0f else 180f,
        label = "rotation"
    )

    var isSorting by rememberSaveable {
        mutableStateOf(false)
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(Dimensions.thumbnails.song * 2 + Dimensions.itemsVerticalPadding * 2),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(Dimensions.itemsVerticalPadding * 2),
        horizontalArrangement = Arrangement.spacedBy(
            space = Dimensions.itemsVerticalPadding * 2,
            alignment = Alignment.CenterHorizontally
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        item(
            key = "header",
            span = { GridItemSpan(maxLineSpan) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { isSorting = true }
                ) {
                    Text(
                        text = when (sortBy) {
                            PlaylistSortBy.Name -> stringResource(id = R.string.name)
                            PlaylistSortBy.DateAdded -> stringResource(id = R.string.date_added)
                            PlaylistSortBy.SongCount -> stringResource(id = R.string.song_count)
                        }
                    )
                }

                IconButton(
                    onClick = { sortOrder = !sortOrder },
                    modifier = Modifier.graphicsLayer { rotationZ = sortOrderIconRotation }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.weight(1F))

                Text(
                    text =
                    if (items.size == 1) "1 ${stringResource(id = R.string.playlist).lowercase()}"
                    else "${items.size} ${stringResource(id = R.string.playlists).lowercase()}",
                    style = MaterialTheme.typography.labelLarge
                )

                DropdownMenu(
                    expanded = isSorting,
                    onDismissRequest = { isSorting = false }
                ) {
                    PlaylistSortBy.entries.forEach { entry ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = when (entry) {
                                        PlaylistSortBy.Name -> stringResource(id = R.string.name)
                                        PlaylistSortBy.DateAdded -> stringResource(id = R.string.date_added)
                                        PlaylistSortBy.SongCount -> stringResource(id = R.string.song_count)
                                    }
                                )
                            },
                            onClick = {
                                isSorting = false
                                sortBy = entry
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = entry.icon,
                                    contentDescription = entry.name
                                )
                            },
                            trailingIcon = {
                                RadioButton(
                                    selected = sortBy == entry,
                                    onClick = { sortBy = entry }
                                )
                            }
                        )
                    }
                }
            }
        }

        item(key = "favorites") {
            PlaylistItem(
                icon = Icons.Default.Favorite,
                name = stringResource(id = R.string.favorites),
                songCount = null,
                thumbnailSizeDp = thumbnailSizeDp,
                modifier = Modifier.clickable(onClick = { onBuiltInPlaylist(BuiltInPlaylist.Favorites) }),
                alternative = true
            )
        }

        item(key = "offline") {
            PlaylistItem(
                icon = Icons.Default.DownloadForOffline,
                name = stringResource(id = R.string.offline),
                songCount = null,
                thumbnailSizeDp = thumbnailSizeDp,
                modifier = Modifier.clickable(onClick = { onBuiltInPlaylist(BuiltInPlaylist.Offline) }),
                alternative = true
            )
        }

        item(key = "new") {
            PlaylistItem(
                icon = Icons.Default.Add,
                name = stringResource(id = R.string.new_playlist),
                songCount = null,
                thumbnailSizeDp = thumbnailSizeDp,
                modifier = Modifier.clickable(onClick = { isCreatingANewPlaylist = true }),
                alternative = true
            )
        }

        items(items = items, key = { it.playlist.id }) { playlistPreview ->
            PlaylistItem(
                playlist = playlistPreview,
                thumbnailSizeDp = thumbnailSizeDp,
                thumbnailSizePx = thumbnailSizePx,
                alternative = true,
                modifier = Modifier
                    .clickable(onClick = { onPlaylistClick(playlistPreview.playlist) })
                    .animateItemPlacement()
            )
        }
    }
}
