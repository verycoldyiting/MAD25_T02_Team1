package sg.edu.np.mad.mad25_t02_team1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import sg.edu.np.mad.mad25_t02_team1.ui.BookingHistoryScaffold
import sg.edu.np.mad.mad25_t02_team1.ui.theme.MAD25_T02_Team1Theme

class BookingHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MAD25_T02_Team1Theme {
                // Only call the main scaffold, which contains the footer and the screen content.
                BookingHistoryScaffold()
            }
        }
    }
}
