package sg.edu.np.mad.mad25_t02_team1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import sg.edu.np.mad.mad25_t02_team1.ui.theme.MAD25_T02_Team1Theme

class BuyTicketActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MAD25_T02_Team1Theme {
                val navController = rememberNavController()
                BuyTicketScreen(navController)

            }
        }
    }
}
