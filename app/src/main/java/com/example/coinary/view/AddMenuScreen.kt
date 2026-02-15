package com.example.coinary.view

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.coinary.R
import com.google.accompanist.systemuicontroller.rememberSystemUiController

/**
 * MenuItem: Domain model for dashboard navigation options.
 * Uses string resource IDs to support multi-language localization.
 */
data class MenuItem(
    @StringRes val titleResId: Int,
    val route: String
)

/**
 * Menu configuration defining the grid options.
 * Each item maps to a specific navigation route within the AppNavigation graph.
 */
val menuOptions = listOf(
    MenuItem(R.string.menu_debts, "debts_goals/deudas"),
    MenuItem(R.string.menu_movements, "movement"),
    MenuItem(R.string.menu_savings, "debts_goals/ahorros"),
    MenuItem(R.string.menu_reports, "stats"),
    MenuItem(R.string.menu_reminders, "reminder"),
    MenuItem(R.string.menu_ant_expenses, "ant_expenses"),
    MenuItem(R.string.menu_export_pdf, "pdf_report"),
    MenuItem(R.string.menu_currency, "currency_screen")
)

/**
 * AddMenuScreen: A visual dashboard that provides quick access to core financial modules.
 * Features a dark-themed UI with a background overlay and a 2-column responsive grid.
 */
@Composable
fun AddMenuScreen(
    navController: NavController
) {
    // System UI status bar management for a consistent dark theme experience
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.Black,
            darkIcons = false
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // --- BACKGROUND IMAGE LAYER ---
        Image(
            painter = painterResource(id = R.drawable.fondo_movimentos),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // --- TOP NAVIGATION BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back_desc),
                        tint = Color.White
                    )
                }

                IconButton(onClick = { navController.navigate("notifications") }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = stringResource(R.string.notifications_desc),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(150.dp))

            // --- NAVIGATION GRID ---
            // Renders the menu items in a balanced 2-column layout
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(menuOptions) { item ->
                    MenuGridItem(
                        item = item,
                        onClick = {
                            navController.navigate(item.route)
                        }
                    )
                }
            }
        }
    }
}

/**
 * MenuGridItem: Individual card component for the navigation grid.
 * Applies a custom vertical gradient and manages click interactions.
 */
@Composable
fun MenuGridItem(
    item: MenuItem,
    onClick: () -> Unit
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF150F33),
            Color(0xFF282626)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(brush = gradientBrush)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = item.titleResId),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}