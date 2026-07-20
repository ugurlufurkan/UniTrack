package com.unitrack.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unitrack.app.ui.components.GlassCard
import com.unitrack.app.ui.components.PremiumButton
import com.unitrack.app.ui.components.PremiumTextButton
import com.unitrack.app.util.AppLinks

@Composable
fun AccountDeletionSection(
    modifier: Modifier = Modifier,
    viewModel: AccountDeletionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is AccountDeletionState.Error) {
            Toast.makeText(
                context,
                "Hesap silinemedi. İnternet bağlantını kontrol edip tekrar dene, ya da web üzerinden talep gönder.",
                Toast.LENGTH_LONG
            ).show()
            viewModel.dismissError()
        }
    }

    GlassCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = "Hesabımı Sil",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Hesabın ve tüm ders/not/GANO, devamsızlık, görev ve takvim verilerin kalıcı olarak silinir. Bu işlem geri alınamaz.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (state is AccountDeletionState.Deleting) {

                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.error
                )

            } else {

                PremiumButton(
                    onClick = {
                        showConfirmDialog = true
                    }
                ) {
                    Text("Hesabımı Sil")
                }

            }

            Spacer(modifier = Modifier.height(4.dp))

            PremiumTextButton(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(AppLinks.deleteAccountWebUrl)
                    )
                    context.startActivity(intent)
                }
            ) {
                Text("Web üzerinden talep et")
            }
        }
    }

    if (showConfirmDialog) {

        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
            },
            title = {
                Text("Hesabını silmek istediğine emin misin?")
            },
            text = {
                Text("Bu işlem geri alınamaz. Tüm akademik verilerin kalıcı olarak silinecek.")
            },
            confirmButton = {
                PremiumTextButton(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.deleteAccount()
                    }
                ) {
                    Text(
                        text = "Kalıcı Olarak Sil",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                PremiumTextButton(
                    onClick = {
                        showConfirmDialog = false
                    }
                ) {
                    Text("Vazgeç")
                }
            }
        )
    }
}