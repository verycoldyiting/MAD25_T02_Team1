package sg.edu.np.mad.mad25_t02_team1

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem( //define bottom nav items
    val title: String,
    val route: String,
    val icon: ImageVector? = null,
    val iconPainter: Int? = null
) {
    object Home : BottomNavItem("Home", "home", icon = Icons.Default.Home)
    object Search : BottomNavItem("Search", "search", icon = Icons.Default.Search)
    object Tickets : BottomNavItem("Tickets", "tickets", iconPainter = R.drawable.ticket)
    object Profile : BottomNavItem("Profile", "profile", icon = Icons.Default.Person)
}


@Composable
fun BottomNavigationBar(
    selectedItem: BottomNavItem,
    onItemSelected: (BottomNavItem) -> Unit
) {
    NavigationBar(containerColor = Color(0xFF00A2FF)) { //blue color

        val items = listOf(  //list of icons
            BottomNavItem.Home,
            BottomNavItem.Search,
            BottomNavItem.Tickets,
            BottomNavItem.Profile
        )

        items.forEach { item ->
            NavigationBarItem(
                selected = selectedItem == item,
                onClick = { onItemSelected(item) },

                icon = {
                    when { //flexible bottom bar
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
