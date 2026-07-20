package com.unitrack.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.unitrack.app.ui.components.GlassCard
import com.unitrack.app.ui.components.PressableIconButton
import com.unitrack.app.ui.components.PremiumTextButton
import com.unitrack.app.util.AppLinks

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                navigationIcon = {
                    PressableIconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                GlassCard(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            "Verilerim",
                            style = MaterialTheme.typography.titleMedium
                        )

                        DataExportButton(
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            item {
                GlassCard {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {

                        Text(
                            "Yasal",
                            style = MaterialTheme.typography.titleMedium
                        )

                        PremiumTextButton(
                            modifier = Modifier.padding(top = 8.dp),
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(AppLinks.privacyPolicyUrl)
                                    )
                                )
                            }
                        ) {
                            Text("Gizlilik Politikası")
                        }
                    }
                }
            }

            item {
                AccountDeletionSection(
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}