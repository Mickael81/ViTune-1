package me.knighthat.component.tab.toolbar

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.fast4x.rimusic.R
import it.fast4x.rimusic.enums.ThumbnailRoundness
import it.fast4x.rimusic.ui.components.themed.IconButton
import it.fast4x.rimusic.ui.styling.favoritesIcon
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.secondary
import it.fast4x.rimusic.utils.semiBold
import it.fast4x.rimusic.utils.thumbnailRoundnessKey
import me.knighthat.colorPalette
import me.knighthat.typography

class SearchComponent private constructor(
    private val visibleState: MutableState<Boolean>,
    private val focusState: MutableState<Boolean>,
    private val inputState: MutableState<String>
): Icon {

    companion object {
        @JvmStatic
        @Composable
        fun init(): SearchComponent =
            SearchComponent(
                rememberSaveable { mutableStateOf(false) },
                rememberSaveable { mutableStateOf(false) },
                rememberSaveable { mutableStateOf("") }
            )
    }

    var isVisible: Boolean = visibleState.value
        set(value) {
            visibleState.value = value
            field = value
        }
    var isFocused: Boolean = focusState.value
        set(value) {
            focusState.value = value
            field = value
        }
    var input: String = inputState.value
        set(value) {
            inputState.value = value
            field = value
        }
    override val iconId: Int
        @DrawableRes
        get() = R.drawable.search_circle

    @Composable
    private fun ColumnScope.DecorationBox( innerTextField: @Composable () -> Unit ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f)
                               .padding(horizontal = 10.dp)
        ) {
            IconButton(
                onClick = {},
                icon = R.drawable.search,
                color = colorPalette().favoritesIcon,
                modifier = Modifier.align( Alignment.CenterStart )
                                   .size(16.dp)
            )
        }
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f)
                               .padding(horizontal = 30.dp)
        ) {
            // Search hint
            androidx.compose.animation.AnimatedVisibility(
                visible = inputState.value.isBlank(),
                enter = fadeIn(tween(100)),
                exit = fadeOut(tween(100)),
            ) {
                BasicText(
                    text = stringResource(R.string.search),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = typography().xs
                                        .semiBold
                                        .secondary
                                        .copy(
                                            color = colorPalette().textDisabled
                                        )
                )
            }

            // Actual text from user
            innerTextField()
        }
    }

    fun onItemSelected() {
        if ( isVisible )
            if ( input.isBlank() )
                isVisible = false
            else
                isFocused = false
    }

    @Composable
    fun SearchBar( colScope: ColumnScope ) {
        var showSearchBar by visibleState
        var isFocused by focusState
        var input by inputState

        val thumbnailRoundness by rememberPreference(
            thumbnailRoundnessKey,
            ThumbnailRoundness.Heavy
        )

        val focusRequester = remember { FocusRequester() }

        AnimatedVisibility(
            visible = showSearchBar,
            modifier = Modifier.padding(all = 10.dp)
                               .fillMaxWidth()
        ) {
            // Auto focus on search bar when it's visible
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current
            LaunchedEffect( showSearchBar, isFocused ) {
                if( !showSearchBar ) return@LaunchedEffect

                if( isFocused )
                    focusRequester.requestFocus()
                else {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            }

            /*
                TextFieldValue gives control over cursor.

                This prevents the cursor from being placed
                at the beginning of search term.
             */
            var searchInput by remember {
                mutableStateOf( TextFieldValue( input ) )
            }
            BasicTextField(
                value = searchInput,
                onValueChange = {
                    searchInput = it.copy(
                        selection = TextRange( it.text.length )
                    )
                    input = it.text
                },
                textStyle = typography().xs.semiBold,
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions( imeAction = ImeAction.Done ),
                keyboardActions = KeyboardActions(onDone = {
                    showSearchBar = input.isNotBlank()
                    isFocused = false
                    keyboardController?.hide()
                }),
                cursorBrush = SolidColor(colorPalette().text),
                decorationBox = { colScope.DecorationBox( it ) },
                modifier = Modifier.height( 30.dp )
                                   .fillMaxWidth()
                                   .focusRequester(focusRequester)
                                   .background(
                                       colorPalette().background4,
                                       thumbnailRoundness.shape()
                                   )
            )
        }
    }

    override fun onShortClick() {
        isVisible = !isVisible
        isFocused = isVisible
    }
}