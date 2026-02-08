package sg.edu.np.mad.mad25_t02_team1

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.util.Locale

class SpeechNavigationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launchSpeechRecognizer()
    }

    private fun launchSpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
            )
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Say a navigation command"
            )
        }

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Speech recognition is not supported on this device",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }
    @Deprecated("Using legacy Activity Result API for coursework")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            val spokenText = data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.lowercase()

            val resultIntent = Intent().apply {
                putExtra(EXTRA_VOICE_COMMAND, spokenText)
            }

            setResult(Activity.RESULT_OK, resultIntent)
        }

        // Always close activity after speech input
        finish()
    }

    companion object {
        private const val SPEECH_REQUEST_CODE = 1001
        const val EXTRA_VOICE_COMMAND = "VOICE_COMMAND"
    }
}
