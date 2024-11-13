package it.fast4x.rimusic.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import it.fast4x.compose.persist.persistList
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.MODIFIED_PREFIX
import it.fast4x.rimusic.R
import it.fast4x.rimusic.enums.AlbumSortBy
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.enums.SortOrder
import it.fast4x.rimusic.enums.UiType
import it.fast4x.rimusic.models.Album
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.models.SongPlaylistMap
import it.fast4x.rimusic.query
import it.fast4x.rimusic.transaction
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.themed.AlbumsItemMenu
import it.fast4x.rimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.fast4x.rimusic.ui.components.themed.HeaderInfo
import it.fast4x.rimusic.ui.components.themed.InputTextDialog
import it.fast4x.rimusic.ui.components.themed.MultiFloatingActionsContainer
import it.fast4x.rimusic.ui.items.AlbumItem
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.utils.addNext
import it.fast4x.rimusic.utils.albumSortByKey
import it.fast4x.rimusic.utils.albumSortOrderKey
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.disableScrollingTextKey
import it.fast4x.rimusic.utils.enqueue
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.showFloatingIconKey
import kotlinx.coroutines.flow.Flow
import me.knighthat.colorPalette
import me.knighthat.component.tab.TabHeader
import me.knighthat.component.tab.toolbar.ItemSize
import me.knighthat.component.tab.toolbar.Randomizer
import me.knighthat.component.tab.toolbar.SearchComponent
import me.knighthat.component.tab.toolbar.SongsShuffle
import me.knighthat.component.tab.toolbar.Sort
import me.knighthat.preference.Preference
import me.knighthat.preference.Preference.HOME_ALBUM_ITEM_SIZE
import me.knighthat.thumbnailShape

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalTextApi
@UnstableApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun HomeAlbums(
    onAlbumClick: (Album) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // Essentials
    val menuState = LocalMenuState.current
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val lazyGridState = rememberLazyGridState()

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    var items by persistList<Album>( "home/albums" )

    // Sort states
    val sortBy = rememberPreference(albumSortByKey, AlbumSortBy.DateAdded)
    val sortOrder = rememberPreference(albumSortOrderKey, SortOrder.Descending)
    // Size state
    val sizeState = Preference.remember( HOME_ALBUM_ITEM_SIZE )

    var itemsOnDisplay by persistList<Album>( "home/albums/on_display" )

    val search = SearchComponent.init()

    val sort = object: Sort<AlbumSortBy> {
        override val menuState = menuState
        override val sortOrderState = sortOrder
        override val sortByEnum = AlbumSortBy.entries
        override val sortByState = sortBy
    }
    val itemSize = object: ItemSize{
        override val menuState = menuState
        override val sizeState = sizeState
    }
    val randomizer = object: Randomizer<Album> {
        override fun getItems(): List<Album> = itemsOnDisplay
        override fun onClick(index: Int) = onAlbumClick( itemsOnDisplay[index] )
    }
    val shuffle = object: SongsShuffle{
        override val binder = binder
        override val context = context

        override fun query(): Flow<List<Song>?> = Database.songsInAllBookmarkedAlbums()
    }

    LaunchedEffect(sort.sortByState.value, sort.sortOrderState.value) {
        Database.albums(sort.sortByState.value, sort.sortOrderState.value).collect { items = it }
    }
    LaunchedEffect( items, search.input ) {
        itemsOnDisplay = items.filter {
            it.title?.contains( search.input, true) ?: false
                    || it.year?.contains( search.input, true) ?: false
                    || it.authorsText?.contains( search.input, true) ?: false
        }
    }

    Box(
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxHeight()
            .fillMaxWidth(
                if( NavigationBarPosition.Right.isCurrent() )
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
    ) {
        Column( Modifier.fillMaxSize() ) {
            // Sticky tab's title
            TabHeader(R.string.albums) {
                HeaderInfo(items.size.toString(), R.drawable.album)
            }

            // Sticky tab's tool bar
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(vertical = 4.dp)
                    .fillMaxWidth()
            ) {
                sort.ToolBarButton()

                search.ToolBarButton()

                randomizer.ToolBarButton()

                shuffle.ToolBarButton()

                itemSize.ToolBarButton()
            }

            // Sticky search bar
            search.SearchBar( this )

            LazyVerticalGrid(
                state = lazyGridState,
                columns = GridCells.Adaptive( itemSize.sizeState.value.dp ),
                //contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                modifier = Modifier.background( colorPalette().background0 )
                                   .fillMaxSize(),
                contentPadding = PaddingValues( bottom = Dimensions.bottomSpacer )
            ) {
                items(
                    items = itemsOnDisplay,
                    key = Album::id
                ) { album ->
                    var songs = remember { listOf<Song>() }
                    query {
                        songs = Database.albumSongsList(album.id)
                    }

                    var showDialogChangeAlbumTitle by remember {
                        mutableStateOf(false)
                    }
                    var showDialogChangeAlbumAuthors by remember {
                        mutableStateOf(false)
                    }
                    var showDialogChangeAlbumCover by remember {
                        mutableStateOf(false)
                    }

                    var onDismiss: () -> Unit = {}
                    var titleId = 0
                    var defValue = ""
                    var placeholderTextId: Int = 0
                    var queryBlock: (Database, String, String) -> Int = { _, _, _ -> 0}

                    if( showDialogChangeAlbumCover ) {
                        onDismiss = { showDialogChangeAlbumCover = false }
                        titleId = R.string.update_cover
                        defValue = album.thumbnailUrl.toString()
                        placeholderTextId = R.string.cover
                        queryBlock = Database::updateAlbumCover
                    } else if( showDialogChangeAlbumTitle ) {
                        onDismiss = { showDialogChangeAlbumTitle = false }
                        titleId = R.string.update_title
                        defValue = album.title.toString()
                        placeholderTextId = R.string.title
                        queryBlock = Database::updateAlbumTitle
                    } else if( showDialogChangeAlbumAuthors ) {
                        onDismiss = { showDialogChangeAlbumAuthors = false }
                        titleId = R.string.update_authors
                        defValue = album.authorsText.toString()
                        placeholderTextId = R.string.authors
                        queryBlock = Database::updateAlbumAuthors
                    }

                    if( showDialogChangeAlbumTitle || showDialogChangeAlbumAuthors || showDialogChangeAlbumCover )
                        InputTextDialog(
                            onDismiss = onDismiss,
                            title = stringResource( titleId ),
                            value = defValue,
                            placeholder = stringResource( placeholderTextId ),
                            setValue = {
                                if (it.isNotEmpty())
                                    query { queryBlock( Database, album.id, it ) }
                            },
                            prefix = MODIFIED_PREFIX
                        )

                    var position by remember {
                        mutableIntStateOf(0)
                    }
                    val context = LocalContext.current

                    AlbumItem(
                        alternative = true,
                        showAuthors = true,
                        album = album,
                        thumbnailSizeDp = itemSize.sizeState.value.dp,
                        thumbnailSizePx = itemSize.sizeState.value.px,
                        modifier = Modifier
                            .combinedClickable(

                                onLongClick = {
                                    menuState.display {
                                        AlbumsItemMenu(
                                            onDismiss = menuState::hide,
                                            album = album,
                                            onChangeAlbumTitle = {
                                                showDialogChangeAlbumTitle = true
                                            },
                                            onChangeAlbumAuthors = {
                                                showDialogChangeAlbumAuthors = true
                                            },
                                            onChangeAlbumCover = {
                                                showDialogChangeAlbumCover = true
                                            },
                                            onPlayNext = {
                                                println("mediaItem ${songs}")
                                                binder?.player?.addNext(
                                                    songs.map(Song::asMediaItem), context
                                                )

                                            },
                                            onEnqueue = {
                                                println("mediaItem ${songs}")
                                                binder?.player?.enqueue(
                                                    songs.map(Song::asMediaItem), context
                                                )

                                            },
                                            onAddToPlaylist = { playlistPreview ->
                                                position =
                                                    playlistPreview.songCount.minus(1) ?: 0
                                                //Log.d("mediaItem", " maxPos in Playlist $it ${position}")
                                                if (position > 0) position++ else position =
                                                    0
                                                //Log.d("mediaItem", "next initial pos ${position}")
                                                //if (listMediaItems.isEmpty()) {
                                                songs.forEachIndexed { index, song ->
                                                    transaction {
                                                        Database.insert(song.asMediaItem)
                                                        Database.insert(
                                                            SongPlaylistMap(
                                                                songId = song.asMediaItem.mediaId,
                                                                playlistId = playlistPreview.playlist.id,
                                                                position = position + index
                                                            )
                                                        )
                                                    }
                                                    //Log.d("mediaItemPos", "added position ${position + index}")
                                                }
                                                //}
                                            },
                                            disableScrollingText = disableScrollingText
                                        )
                                    }
                                },
                                onClick = {
                                    search.onItemSelected()
                                    onAlbumClick( album )
                                }
                            )
                            .clip(thumbnailShape()),
                        disableScrollingText = disableScrollingText
                    )
                }
            }
        }

        FloatingActionsContainerWithScrollToTop( lazyGridState )

        val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
        if ( UiType.ViMusic.isCurrent() && showFloatingIcon )
            MultiFloatingActionsContainer(
                iconId = R.drawable.search,
                onClick = onSearchClick,
                onClickSettings = onSettingsClick,
                onClickSearch = onSearchClick
            )
    }
}
