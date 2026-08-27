package com.ramapalani.civics2025.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AboutLegalScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("Back") }
        Text("About and privacy", style = MaterialTheme.typography.headlineMedium)
        Section(
            "This is not an official USCIS app",
            """
Civics 2025 is an independent practice tool. It is not affiliated with, endorsed by, or approved by U.S. Citizenship and Immigration Services (USCIS), the Department of Homeland Security, or any other U.S. government agency.

Passing a quiz here is not a USCIS test result and does not affect a naturalization application. Officers ask questions orally and decide what answers to accept.

Current office-holder names (President, Vice President, Speaker of the House, Chief Justice) can change. Confirm those answers at uscis.gov/citizenship/testupdates before your interview, then update them in Settings along with your state’s senator, representative, governor, and capital. Settings links to usa.gov/presidents for President and Vice President, supremecourt.gov for current justices (including the Chief Justice), and congress.gov/members for senator and representative by address.

This app is not legal advice.
            """.trimIndent(),
        )
        Section(
            "Study materials",
            """
Question text and accepted answers are based on the official 2025 civics materials USCIS publishes for the public, including the 128 Questions and Answers (M-1778) and the study guide One Nation, One People.

Those publications are U.S. government works. This app’s software, layout, and icon are original and are not USCIS property.
            """.trimIndent(),
        )
        Section(
            "Privacy",
            """
The developer of this app does not operate a server for it and does not collect, sell, or share your information.

What stays on this phone
• Practice answers, scores, streaks, and which questions you missed (stored in the app’s local database).
• Optional names you type in Settings for current federal officials and your senator, representative, governor, and capital.
• Notification preference and daily-question hour.
• The One Nation, One People PDF, if you download it (stored in the app’s private files).

What we do not do
• No account, sign-in, or advertising identifier from this app.
• No analytics or crash SDK. If you install from Google Play, Play may show crash reports to the developer in Play Vitals. That is Play’s process, not an SDK in this app.
• No sending of your answers or officials to a backend.

Textbook download
The 128 questions and the study guide's page text (used for the Text and Listen views) are bundled in the app. The study-guide PDF's page images are not bundled. If you tap the textbook, the app can download the PDF from uscis.gov and keep it on this phone so you can see the original pages. That request goes to USCIS, not to the developer. You can skip the download and still use Learn, Test, Daily, and the textbook's Text and Listen views.

Notifications
If you turn on the daily question, Android may ask for notification permission and Alarms & reminders permission so the reminder can fire at the hour you pick. You can deny either or turn the reminder off in Settings. The reminder is scheduled on the device.

Speaking textbook pages
Listen uses the speech engine already on your phone. Text is processed on the device.

Backups
This app does not allow Android backup or device transfer of its files. Practice answers, Settings names, and a downloaded textbook stay on this phone and are not copied by Google Backup.

Deleting data
Uninstalling the app removes its local database, preferences, and any downloaded study-guide PDF on this device. You can also trim old results from Settings.

Children
This app is meant for adults preparing for naturalization, not for children.

Questions about this policy
There is no web form. The full policy is this screen. If you obtained the app from Google Play, you can also use the Play Store listing’s email contact for the developer.
            """.trimIndent(),
        )
        Text("Version 1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Section(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    Text(body, style = MaterialTheme.typography.bodyLarge)
}
