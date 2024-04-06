package it.vfsfitvnm.vimusic.ui.screens.album

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.bodies.BrowseBody
import it.vfsfitvnm.innertube.requests.albumPage
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.Album
import it.vfsfitvnm.vimusic.models.Section
import it.vfsfitvnm.vimusic.models.SongAlbumMap
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.ui.components.TabScaffold
import it.vfsfitvnm.vimusic.ui.components.themed.adaptiveThumbnailContent
import it.vfsfitvnm.vimusic.ui.items.AlbumItem
import it.vfsfitvnm.vimusic.ui.items.ItemPlaceholder
import it.vfsfitvnm.vimusic.ui.screens.search.ItemsPage
import it.vfsfitvnm.vimusic.utils.asMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun AlbumScreen(
    browseId: String,
    pop: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    var album: Album? by remember { mutableStateOf(null) }
    var albumPage: Innertube.PlaylistOrAlbumPage? by remember { mutableStateOf(null) }
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        Database
            .album(browseId)
            .combine(snapshotFlow { tabIndex }) { album, tabIndex -> album to tabIndex }
            .collect { (currentAlbum, tabIndex) ->
                album = currentAlbum

                if (albumPage == null && (currentAlbum?.timestamp == null || tabIndex == 1)) {
                    withContext(Dispatchers.IO) {
                        Innertube.albumPage(BrowseBody(browseId = browseId))
                            ?.onSuccess { currentAlbumPage ->
                                albumPage = currentAlbumPage

                                Database.clearAlbum(browseId)

                                Database.upsert(
                                    Album(
                                        id = browseId,
                                        title = currentAlbumPage.title,
                                        thumbnailUrl = currentAlbumPage.thumbnail?.url,
                                        year = currentAlbumPage.year,
                                        authorsText = currentAlbumPage.authors
                                            ?.joinToString("") { it.name ?: "" },
                                        shareUrl = currentAlbumPage.url,
                                        timestamp = System.currentTimeMillis(),
                                        bookmarkedAt = album?.bookmarkedAt
                                    ),
                                    currentAlbumPage
                                        .songsPage
                                        ?.items
                                        ?.map(Innertube.SongItem::asMediaItem)
                                        ?.onEach(Database::insert)
                                        ?.mapIndexed { position, mediaItem ->
                                            SongAlbumMap(
                                                songId = mediaItem.mediaId,
                                                albumId = browseId,
                                                position = position
                                            )
                                        } ?: emptyList()
                                )
                            }
                    }
                }
            }
    }

    val thumbnailContent =
        adaptiveThumbnailContent(album?.timestamp == null, album?.thumbnailUrl)

    TabScaffold(
        topIconButtonId = Icons.AutoMirrored.Outlined.ArrowBack,
        onTopIconButtonClick = pop,
        sectionTitle = album?.title ?: "",
        appBarActions = {
            val context = LocalContext.current

            IconButton(
                onClick = {
                    val bookmarkedAt =
                        if (album?.bookmarkedAt == null) System.currentTimeMillis() else null

                    query {
                        album
                            ?.copy(bookmarkedAt = bookmarkedAt)
                            ?.let(Database::update)
                    }
                }
            ) {
                Icon(
                    imageVector =
                    if (album?.bookmarkedAt == null) Icons.Outlined.BookmarkAdd
                    else Icons.Filled.Bookmark,
                    contentDescription = null
                )
            }

            IconButton(
                onClick = {
                    album?.shareUrl?.let { url ->
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                        }

                        context.startActivity(
                            Intent.createChooser(
                                sendIntent,
                                null
                            )
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null
                )
            }
        },
        tabIndex = tabIndex,
        onTabChanged = { tabIndex = it },
        tabColumnContent = listOf(
            Section(stringResource(id = R.string.songs), Icons.Outlined.MusicNote),
            Section(stringResource(id = R.string.other_versions), Icons.Outlined.Album)
        )
    ) { index ->
        saveableStateHolder.SaveableStateProvider(index) {
            when (index) {
                0 -> AlbumSongs(
                    browseId = browseId,
                    thumbnailContent = thumbnailContent,
                    onGoToArtist = onGoToArtist
                )

                1 -> {
                    ItemsPage(
                        tag = "album/$browseId/alternatives",
                        initialPlaceholderCount = 1,
                        continuationPlaceholderCount = 1,
                        emptyItemsText = stringResource(id = R.string.no_alternative_versions),
                        itemsPageProvider = albumPage?.let {
                            ({
                                Result.success(
                                    Innertube.ItemsPage(
                                        items = albumPage?.otherVersions,
                                        continuation = null
                                    )
                                )
                            })
                        },
                        itemContent = { album ->
                            AlbumItem(
                                album = album,
                                onClick = { onAlbumClick(album.key) }
                            )
                        },
                        itemPlaceholderContent = {
                            ItemPlaceholder()
                        }
                    )
                }
            }
        }
    }
}