package sg.edu.np.mad.mad25_t02_team1

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val title: String,
    val icon: ImageVector? = null,
    val iconPainter: Int? = null
) {
    object Home : BottomNavItem("Home", icon = Icons.Default.Home)
    object Search : BottomNavItem("Search", icon = Icons.Default.Search)
    object Tickets : BottomNavItem("Tickets", iconPainter = R.drawable.ticket)
    object Profile : BottomNavItem("Profile", icon = Icons.Default.Person)
}

@Composable
fun BottomNavigationBar(
    selectedItem: BottomNavItem,
    onItemSelected: (BottomNavItem) -> Unit
) {
    NavigationBar(containerColor = Color(0xFF00A2FF)) {

        listOf(
            BottomNavItem.Home,
            BottomNavItem.Search,
            BottomNavItem.Tickets,
            BottomNavItem.Profile
        ).forEach { item ->

            NavigationBarItem(
                selected = selectedItem == item,
                onClick = { onItemSelected(item) },

                icon = {
                    when {
                        item.iconPainter != null ->
                            Icon(
                                painter = painterResource(item.iconPainter),
                                contentDescription = item.title
                            )
                        item.icon != null ->
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                    }
                },

                label = { Text(item.title) },
                alwaysShowLabel = false
            )
        }
    }
}
