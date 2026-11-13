package com.example.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.MediRelayRepository
import com.example.model.CartItem
import com.example.model.DeliveryAddress
import com.example.model.MedicineOrder
import com.example.model.OrderStatus
import com.example.notifications.OrderNotificationHelper
import com.example.ui.MediRelayViewModel
import com.example.ui.UiState
import com.example.ui.auth.authStateFlow
import com.example.ui.components.formatCurrency
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartCheckoutScreen
import com.example.ui.screens.MedicinesCatalogScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProfileAddressesScreen
import com.example.ui.screens.StoreDetailScreen
import com.example.ui.screens.StoresHomeScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_STORES = "stores"
const val ROUTE_MEDICINES = "medicines"
const val ROUTE_STORE_DETAIL = "store_detail/{storeId}"
const val ROUTE_CART = "cart"
const val ROUTE_ORDERS = "orders"
const val ROUTE_PROFILE = "profile"

private fun storeDetailRoute(storeId: String): String = "store_detail/$storeId"

private data class TopDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topDestinations = listOf(
    TopDestination(
        route = ROUTE_STORES,
        labelRes = R.string.nav_stores,
        selectedIcon = Icons.Filled.LocalPharmacy,
        unselectedIcon = Icons.Outlined.LocalPharmacy,
        testTag = "nav_tab_stores"
    ),
    TopDestination(
        route = ROUTE_MEDICINES,
        labelRes = R.string.nav_medicines,
        selectedIcon = Icons.Filled.Medication,
        unselectedIcon = Icons.Outlined.Medication,
        testTag = "nav_tab_medicines"
    ),
    TopDestination(
        route = ROUTE_CART,
        labelRes = R.string.nav_cart,
        selectedIcon = Icons.Filled.ShoppingBag,
        unselectedIcon = Icons.Outlined.ShoppingBag,
        testTag = "nav_tab_cart"
    ),
    TopDestination(
        route = ROUTE_ORDERS,
        labelRes = R.string.nav_orders,
        selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
        unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
        testTag = "nav_tab_orders"
    ),
    TopDestination(
        route = ROUTE_PROFILE,
        labelRes = R.string.nav_profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_tab_profile"
    )
)

@Composable
fun AppNavigation(
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            MediRelayAuthenticatedShell(
                currentUserId = user.uid,
                userDisplayName = user.displayName,
                userEmail = user.email,
                userPhoneNumber = user.phoneNumber
            )
        }
    }
}

@Composable
private fun MediRelayAuthenticatedShell(
    currentUserId: String,
    userDisplayName: String?,
    userEmail: String?,
    userPhoneNumber: String?
) {
    val viewModel: MediRelayViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MediRelayViewModel(MediRelayRepository(db), currentUserId)
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val topRoutes = remember { topDestinations.map { it.route }.toSet() }
    val showTopLevelNav = currentRoute in topRoutes

    val cartState by viewModel.cartState.collectAsStateWithLifecycle()
    val addressesState by viewModel.addressesState.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val open24hOnly by viewModel.open24hOnly.collectAsStateWithLifecycle()
    val freeDeliveryOnly by viewModel.freeDeliveryOnly.collectAsStateWithLifecycle()
    val favoritesOnly by viewModel.favoritesOnly.collectAsStateWithLifecycle()
    val rxFreeOnly by viewModel.rxFreeOnly.collectAsStateWithLifecycle()
    val storeSortOption by viewModel.storeSortOption.collectAsStateWithLifecycle()
    val catalogSortOption by viewModel.catalogSortOption.collectAsStateWithLifecycle()
    val favoriteStoreIds by viewModel.favoriteStoreIdsState.collectAsStateWithLifecycle()
    val appliedCouponCode by viewModel.appliedCouponCode.collectAsStateWithLifecycle()
    val riderTipAmount by viewModel.riderTipAmount.collectAsStateWithLifecycle()
    val selectedPaymentMethod by viewModel.selectedPaymentMethod.collectAsStateWithLifecycle()
    val selectedInstructions by viewModel.selectedInstructions.collectAsStateWithLifecycle()
    val customDeliveryNote by viewModel.customDeliveryNote.collectAsStateWithLifecycle()
    val stores by viewModel.filteredStores.collectAsStateWithLifecycle()
    val offerings by viewModel.filteredOfferings.collectAsStateWithLifecycle()
    val medicineGroups by viewModel.filteredMedicineGroups.collectAsStateWithLifecycle()
    val prescriptionUri by viewModel.prescriptionUri.collectAsStateWithLifecycle()
    val prescriptionLabel by viewModel.prescriptionLabel.collectAsStateWithLifecycle()
    val selectedAddressId by viewModel.selectedAddressId.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val orderAlertEvent by viewModel.orderAlertEvent.collectAsStateWithLifecycle()
    val liveTransitProgress by viewModel.liveTransitProgress.collectAsStateWithLifecycle()
    val liveTrackings by viewModel.liveTrackingsState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedbackMessage) {
        val message = feedbackMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeFeedback()
        }
    }

    LaunchedEffect(orderAlertEvent) {
        val alert = orderAlertEvent
        if (alert != null) {
            OrderNotificationHelper.notifyOrderMilestone(
                context = context,
                orderId = alert.orderId,
                title = alert.title,
                message = alert.message
            )
            snackbarHostState.showSnackbar("${alert.title}: ${alert.message}")
            viewModel.consumeOrderAlert()
        }
    }

    val cartItems: List<CartItem> = (cartState as? UiState.Success)?.data ?: emptyList()
    val addresses: List<DeliveryAddress> = (addressesState as? UiState.Success)?.data ?: emptyList()
    val orders: List<MedicineOrder> = (ordersState as? UiState.Success)?.data ?: emptyList()

    val cartBadgeCount = remember(cartItems) { cartItems.sumOf { it.quantity }.toInt() }
    val cartSubtotal = remember(cartItems) { cartItems.sumOf { it.unitPrice * it.quantity } }
    val cartStoreCount = remember(cartItems) { cartItems.map { it.storeId }.distinct().size }
    val activeAddress = remember(addresses, selectedAddressId) {
        addresses.firstOrNull { it.id == selectedAddressId }
            ?: addresses.firstOrNull { it.isDefault }
            ?: addresses.firstOrNull()
    }
    val activeOrder = remember(orders) {
        orders.firstOrNull { !OrderStatus.fromWire(it.status).isTerminal }
    }
    var trackedOrderId by remember { mutableStateOf<String?>(null) }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val showFloatingMiniCart = cartBadgeCount > 0 &&
        currentRoute != ROUTE_CART &&
        currentRoute != ROUTE_STORE_DETAIL

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail && showTopLevelNav) {
                NavigationRail {
                    topDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTopLevel(dest.route) },
                            icon = {
                                NavTabIcon(
                                    destination = dest,
                                    selected = selected,
                                    cartBadgeCount = cartBadgeCount
                                )
                            },
                            label = { Text(stringResource(dest.labelRes)) },
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavigationRail && showTopLevelNav) {
                        NavigationBar(
                            windowInsets = WindowInsets.navigationBars,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            topDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(dest.route) },
                                    icon = {
                                        NavTabIcon(
                                            destination = dest,
                                            selected = selected,
                                            cartBadgeCount = cartBadgeCount
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) { innerPadding ->
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_STORES,
                        modifier = Modifier
                            .widthIn(max = 840.dp)
                            .fillMaxSize()
                    ) {
                        composable(ROUTE_STORES) {
                            StoresHomeScreen(
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                open24hOnly = open24hOnly,
                                freeDeliveryOnly = freeDeliveryOnly,
                                favoritesOnly = favoritesOnly,
                                rxFreeOnly = rxFreeOnly,
                                storeSortOption = storeSortOption,
                                favoriteStoreIds = favoriteStoreIds,
                                appliedCouponCode = appliedCouponCode,
                                stores = stores,
                                offerings = offerings,
                                cartItems = cartItems,
                                pastOrders = orders,
                                activeAddress = activeAddress,
                                activeOrder = activeOrder,
                                liveTransitFraction = activeOrder?.let { liveTransitProgress[it.id] },
                                liveTracking = activeOrder?.let { liveTrackings[it.id] },
                                onSearchChange = viewModel::updateSearchQuery,
                                onCategorySelected = viewModel::selectCategory,
                                onToggleOpen24h = viewModel::toggleOpen24hOnly,
                                onToggleFreeDelivery = viewModel::toggleFreeDeliveryOnly,
                                onToggleFavoritesOnly = viewModel::toggleFavoritesOnly,
                                onToggleRxFreeOnly = viewModel::toggleRxFreeOnly,
                                onSelectStoreSort = viewModel::setStoreSortOption,
                                onToggleFavoriteStore = viewModel::toggleFavoriteStore,
                                onApplyCoupon = viewModel::applyCoupon,
                                onReorderOrder = { pastOrder ->
                                    viewModel.reorderPastOrder(pastOrder) {
                                        navigateTopLevel(ROUTE_CART)
                                    }
                                },
                                onStoreClick = { storeId ->
                                    navController.navigate(storeDetailRoute(storeId))
                                },
                                onViewAllMedicines = { navigateTopLevel(ROUTE_MEDICINES) },
                                onTrackOrder = { orderId ->
                                    trackedOrderId = orderId
                                    navigateTopLevel(ROUTE_ORDERS)
                                },
                                onAddToCart = viewModel::addToCart,
                                onIncrementCartItem = viewModel::incrementCartItem,
                                onDecrementCartItem = viewModel::decrementCartItem,
                                onManageAddresses = { navigateTopLevel(ROUTE_PROFILE) }
                            )
                        }

                        composable(ROUTE_MEDICINES) {
                            MedicinesCatalogScreen(
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                rxFreeOnly = rxFreeOnly,
                                catalogSortOption = catalogSortOption,
                                medicineGroups = medicineGroups,
                                cartItems = cartItems,
                                onSearchChange = viewModel::updateSearchQuery,
                                onCategorySelected = viewModel::selectCategory,
                                onToggleRxFreeOnly = viewModel::toggleRxFreeOnly,
                                onSelectCatalogSort = viewModel::setCatalogSortOption,
                                onAddToCart = viewModel::addToCart,
                                onIncrementCartItem = viewModel::incrementCartItem,
                                onDecrementCartItem = viewModel::decrementCartItem,
                                onOpenStore = { storeId ->
                                    navController.navigate(storeDetailRoute(storeId))
                                }
                            )
                        }

                        composable(
                            route = ROUTE_STORE_DETAIL,
                            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
                        ) { entry ->
                            val storeId = entry.arguments?.getString("storeId") ?: ""
                            StoreDetailScreen(
                                storeId = storeId,
                                cartItems = cartItems,
                                isFavorite = storeId in favoriteStoreIds,
                                appliedCouponCode = appliedCouponCode,
                                onBack = { navController.popBackStack() },
                                onToggleFavorite = viewModel::toggleFavoriteStore,
                                onApplyCoupon = viewModel::applyCoupon,
                                onAddToCart = viewModel::addToCart,
                                onIncrementCartItem = viewModel::incrementCartItem,
                                onDecrementCartItem = viewModel::decrementCartItem,
                                onOpenCart = { navigateTopLevel(ROUTE_CART) }
                            )
                        }

                        composable(ROUTE_CART) {
                            when (cartState) {
                                is UiState.Loading -> LoadingCentered()
                                is UiState.Error -> ErrorMessageCentered((cartState as UiState.Error).message)
                                is UiState.Success -> {
                                    CartCheckoutScreen(
                                        cartItems = cartItems,
                                        addresses = addresses,
                                        selectedAddressId = selectedAddressId,
                                        userPhoneNumber = userPhoneNumber,
                                        prescriptionUri = prescriptionUri,
                                        prescriptionLabel = prescriptionLabel,
                                        appliedCouponCode = appliedCouponCode,
                                        selectedTip = riderTipAmount,
                                        selectedPaymentMethod = selectedPaymentMethod,
                                        selectedInstructions = selectedInstructions,
                                        customDeliveryNote = customDeliveryNote,
                                        isSubmitting = isSubmitting,
                                        onSelectAddress = viewModel::selectAddress,
                                        onSaveAddress = { label, full, landmark, phone, isDefault ->
                                            viewModel.saveAddress(label, full, landmark, phone, isDefault)
                                        },
                                        onAttachPrescription = viewModel::attachPrescription,
                                        onClearPrescription = viewModel::clearPrescription,
                                        onApplyCoupon = viewModel::applyCoupon,
                                        onClearCoupon = viewModel::clearCoupon,
                                        onSelectTip = viewModel::selectRiderTip,
                                        onSelectPaymentMethod = viewModel::selectPaymentMethod,
                                        onToggleInstruction = viewModel::toggleDeliveryInstruction,
                                        onCustomDeliveryNoteChange = viewModel::updateCustomDeliveryNote,
                                        onAddRecommendedItem = viewModel::addToCart,
                                        onIncrementCartItem = viewModel::incrementCartItem,
                                        onDecrementCartItem = viewModel::decrementCartItem,
                                        onClearStoreCart = viewModel::clearStoreCart,
                                        onPlaceStoreOrder = { storeId, items, addr ->
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                !OrderNotificationHelper.hasNotificationPermission(context)
                                            ) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                            viewModel.placeStoreOrder(storeId, items, addr) { orderId ->
                                                trackedOrderId = orderId
                                                navigateTopLevel(ROUTE_ORDERS)
                                            }
                                        },
                                        onBrowseStores = { navigateTopLevel(ROUTE_STORES) }
                                    )
                                }
                            }
                        }

                        composable(ROUTE_ORDERS) {
                            when (ordersState) {
                                is UiState.Loading -> LoadingCentered()
                                is UiState.Error -> ErrorMessageCentered((ordersState as UiState.Error).message)
                                is UiState.Success -> {
                                    OrdersScreen(
                                        orders = orders,
                                        liveTransitProgress = liveTransitProgress,
                                        liveTrackings = liveTrackings,
                                        trackedOrderId = trackedOrderId,
                                        onConsumeTrackedOrderId = { trackedOrderId = null },
                                        onCancelOrder = viewModel::cancelOrder,
                                        onReorderOrder = { pastOrder ->
                                            viewModel.reorderPastOrder(pastOrder) {
                                                navigateTopLevel(ROUTE_CART)
                                            }
                                        },
                                        onSubmitRating = viewModel::submitOrderRating,
                                        onBrowseStores = { navigateTopLevel(ROUTE_STORES) }
                                    )
                                }
                            }
                        }

                        composable(ROUTE_PROFILE) {
                            ProfileAddressesScreen(
                                userDisplayName = userDisplayName,
                                userEmail = userEmail,
                                userPhoneNumber = userPhoneNumber,
                                addresses = addresses,
                                orders = orders,
                                favoriteStoreIds = favoriteStoreIds,
                                onSaveAddress = { label, full, landmark, phone, isDefault, existingId ->
                                    viewModel.saveAddress(label, full, landmark, phone, isDefault, existingId)
                                },
                                onDeleteAddress = viewModel::deleteAddress,
                                onOpenStore = { storeId ->
                                    navController.navigate(storeDetailRoute(storeId))
                                },
                                onToggleFavoriteStore = viewModel::toggleFavoriteStore,
                                onApplyCoupon = { code ->
                                    viewModel.applyCoupon(code)
                                    navigateTopLevel(ROUTE_CART)
                                }
                            )
                        }
                    }

                    // Swiggy / Zomato Floating Persistent Mini-Cart Bar
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showFloatingMiniCart,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .widthIn(max = 600.dp)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        FloatingMiniCartBar(
                            itemCount = cartBadgeCount,
                            storeCount = cartStoreCount,
                            subtotal = cartSubtotal,
                            onViewCart = { navigateTopLevel(ROUTE_CART) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingMiniCartBar(
    itemCount: Int,
    storeCount: Int,
    subtotal: Double,
    onViewCart: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 6.dp,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onViewCart)
            .testTag("floating_mini_cart_bar")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingBag,
                    contentDescription = "Medicine bag",
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "$itemCount item${if (itemCount == 1) "" else "s"} • ${formatCurrency(subtotal)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$storeCount Kochi medical store${if (storeCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "View Cart",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Go to cart",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun NavTabIcon(
    destination: TopDestination,
    selected: Boolean,
    cartBadgeCount: Int
) {
    val icon = if (selected) destination.selectedIcon else destination.unselectedIcon
    if (destination.route == ROUTE_CART && cartBadgeCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(cartBadgeCount.toString())
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = stringResource(destination.labelRes))
        }
    } else {
        Icon(imageVector = icon, contentDescription = stringResource(destination.labelRes))
    }
}

@Composable
private fun LoadingCentered() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorMessageCentered(message: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}
