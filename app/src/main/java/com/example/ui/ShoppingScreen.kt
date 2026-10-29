package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShoppingItem
import com.example.ui.components.ClarificationDialog
import com.example.ui.components.EditItemDialog
import com.example.ui.components.RawListDialog
import com.example.ui.components.TripCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.data.model.ShoppingTrip

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShoppingScreen(
    viewModel: ShoppingViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val tripCount = uiState.planResult.trips.size

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 76.dp)
            )
        },
        bottomBar = {
            BottomAddBar(
                inputText = uiState.quickCommandText,
                onTextChange = { viewModel.onQuickCommandChange(it) },
                onSubmit = { viewModel.onQuickCommandSubmit() },
                onAddSingleItem = { viewModel.openAddItemDialog() },
                onAddRawList = { viewModel.openRawListDialog() },
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Clean Top Header (Only "Cumpărături")
            CleanTopHeader()

            // Horizontal Trip Navigation Bar with Arrows & Pager Dots
            if (tripCount > 0) {
                val safeIndex = uiState.currentTripIndex.coerceIn(0, tripCount - 1)
                HorizontalTripNavigator(
                    currentIndex = safeIndex,
                    totalTrips = tripCount,
                    onPrevClick = {
                        if (safeIndex > 0) {
                            viewModel.onTripIndexChange(safeIndex - 1)
                        }
                    },
                    onNextClick = {
                        if (safeIndex < tripCount - 1) {
                            viewModel.onTripIndexChange(safeIndex + 1)
                        }
                    }
                )
            }

            // Main Content Area: Horizontal Pager for trips or clean empty state
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (tripCount == 0) {
                    CleanEmptyState(
                        onOpenRawList = { viewModel.openRawListDialog() }
                    )
                } else {
                    TripPagerContent(
                        trips = uiState.planResult.trips,
                        currentTripIndex = uiState.currentTripIndex,
                        onTripIndexChange = { viewModel.onTripIndexChange(it) },
                        onTogglePurchased = { viewModel.onTogglePurchased(it) },
                        onItemClick = { viewModel.openEditItemDialog(it) },
                        onItemLongClick = { viewModel.openEditItemDialog(it) },
                        onDeleteItem = { viewModel.onDeleteItem(it) }
                    )
                }
            }
        }
    }

    // Clarification Dialog: instant clean window if item needs clarification
    uiState.clarifyingItem?.let { clarifyingItem ->
        ClarificationDialog(
            item = clarifyingItem,
            onConfirm = { updated -> viewModel.onConfirmClarification(updated) },
            onOpenFullEdit = { updated -> viewModel.openEditItemDialog(updated) },
            onDismiss = { viewModel.dismissClarification() }
        )
    }

    // Raw batch text dialog
    if (uiState.isRawListDialogOpen) {
        RawListDialog(
            onDismiss = { viewModel.closeRawListDialog() },
            onConfirm = { rawText -> viewModel.onAddBatchRawText(rawText) }
        )
    }

    // Detailed add/edit modal (also accessible on long-press)
    if (uiState.isAddItemDialogOpen) {
        EditItemDialog(
            item = uiState.editingItem,
            totalTripsCount = tripCount,
            onDismiss = { viewModel.closeEditItemDialog() },
            onSave = { id, name, qty, zone, subLoc, weight, volLiters, price, imp, tripNum ->
                viewModel.onSaveItem(id, name, qty, zone, subLoc, weight, volLiters, price, imp, tripNum)
            }
        )
    }
}

@Composable
private fun CleanTopHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Cumpărături",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                fontSize = 28.sp,
                letterSpacing = (-0.5).sp
            )
        )
    }
}

@Composable
private fun HorizontalTripNavigator(
    currentIndex: Int,
    totalTrips: Int,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Prev Arrow
        IconButton(
            onClick = onPrevClick,
            enabled = currentIndex > 0,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Tura anterioară",
                tint = if (currentIndex > 0) AccentCyan else TextMuted.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Pager indicator / Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "TURA ${currentIndex + 1} din $totalTrips",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            )

            // Small indicator dots
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 0 until totalTrips) {
                    val isCurrent = i == currentIndex
                    Box(
                        modifier = Modifier
                            .size(if (isCurrent) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (isCurrent) AccentCyan else DarkOutline)
                    )
                }
            }
        }

        // Next Arrow
        IconButton(
            onClick = onNextClick,
            enabled = currentIndex < totalTrips - 1,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Tura următoare",
                tint = if (currentIndex < totalTrips - 1) AccentCyan else TextMuted.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CleanEmptyState(
    onOpenRawList: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AccentCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "Lista este goală",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Text(
                text = "Scrie un produs în bara de jos (ex: \"4 lapte\", \"2 carnat\", \"2 kg mere\", \"paine\").\nSau apasă pe '+' pentru opțiuni de adăugare.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            )
        }
    }
}

@Composable
private fun BottomAddBar(
    inputText: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onAddSingleItem: () -> Unit,
    onAddRawList: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(24.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AccentCyan.copy(alpha = 0.4f)))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Plus button with options menu: Adaugă manual sau Lipește listă brută
                Box {
                    IconButton(
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier.size(38.dp).testTag("bottom_bar_add_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Opțiuni Adăugare",
                            tint = AccentCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier.background(DarkSurfaceElevated).border(1.dp, DarkOutline, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Adaugă un produs detaliat", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                isMenuExpanded = false
                                onAddSingleItem()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Lipește listă brută (multi-linie)", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                isMenuExpanded = false
                                onAddRawList()
                            }
                        )
                    }
                }

                // Text Field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_command_input"),
                    placeholder = null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentCyan
                    )
                )

                // Send Button
                Button(
                    onClick = onSubmit,
                    enabled = inputText.isNotBlank(),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan,
                        contentColor = DarkSurface,
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = TextMuted
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("quick_command_submit")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Trimite",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TripPagerContent(
    trips: List<ShoppingTrip>,
    currentTripIndex: Int,
    onTripIndexChange: (Int) -> Unit,
    onTogglePurchased: (ShoppingItem) -> Unit,
    onItemClick: (ShoppingItem) -> Unit,
    onItemLongClick: (ShoppingItem) -> Unit,
    onDeleteItem: (ShoppingItem) -> Unit
) {
    val totalTrips = trips.size
    val safeInitialPage = currentTripIndex.coerceIn(0, maxOf(0, totalTrips - 1))
    val pagerState = rememberPagerState(
        initialPage = safeInitialPage,
        pageCount = { totalTrips }
    )

    LaunchedEffect(currentTripIndex) {
        if (currentTripIndex in 0 until totalTrips && pagerState.currentPage != currentTripIndex) {
            pagerState.animateScrollToPage(currentTripIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage in 0 until totalTrips && pagerState.currentPage != currentTripIndex) {
            onTripIndexChange(pagerState.currentPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 16.dp
    ) { pageIndex ->
        val trip = trips.getOrNull(pageIndex)
        if (trip != null) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(vertical = 10.dp)
            ) {
                TripCard(
                    trip = trip,
                    onTogglePurchased = onTogglePurchased,
                    onItemClick = onItemClick,
                    onItemLongClick = onItemLongClick,
                    onDeleteItem = onDeleteItem
                )
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
