package com.example.coinary.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.coinary.R

/**
 * BottomNavigationBar: A custom navigation component for the main application interface.
 * Instead of using standard Material BottomNavigation, it implements a bespoke design
 * using stacked images and custom click handlers for a unique brand identity.
 */
@Composable
fun BottomNavigationBar(
    navController: NavController,
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ====================================================================================
            // REGION: HOME NAVIGATION
            // Direct access to the main overview/dashboard.
            // ====================================================================================
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { navController.navigate("home") }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.button_background),
                        contentDescription = "Button background",
                        modifier = Modifier.fillMaxSize()
                    )
                    Image(
                        painter = painterResource(id = R.drawable.home_icon),
                        contentDescription = "Home icon",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // ====================================================================================
            // REGION: QUICK ACTIONS / ADD MENU
            // Triggers the modal or grid menu for adding new financial records.
            // ====================================================================================
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { onMenuClick() }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.button_background),
                        contentDescription = "Button background",
                        modifier = Modifier.fillMaxSize()
                    )
                    Image(
                        painter = painterResource(id = R.drawable.add_icon),
                        contentDescription = "Add icon",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // ====================================================================================
            // REGION: TRANSACTION ENTRY (QUICK RECORD)
            // Specialized button for rapid entry of movements (Incomes/Expenses).
            // Uses the pencil icon to represent the 'record' action.
            // ====================================================================================
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { navController.navigate("movement") }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.button_background),
                        contentDescription = "Button background",
                        modifier = Modifier.fillMaxSize()
                    )
                    Image(
                        painter = painterResource(id = R.drawable.pencil_icon),
                        contentDescription = "Movement entry icon",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}