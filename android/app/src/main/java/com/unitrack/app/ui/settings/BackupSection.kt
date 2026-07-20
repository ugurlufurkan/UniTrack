package com.unitrack.app.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unitrack.app.ui.components.PremiumTextButton

/**
 * "Verilerimi Dışa Aktar" — Ayarlar/Hesabım bölümlerinde kullanılacak,
 * tek satırlık, kendi kendine yeten bir buton. Tıklanınca backend'den tüm
 * kullanıcı verisini JSON olarak indirir ve sistemin paylaşım sayfasını
 * (WhatsApp, Drive, E-posta, "Dosyalarım'a kaydet"...) açar.
 */
@Composable
fun DataExportButton(
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        val current = state
        when (current) {
            is BackupExportState.Ready -> {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, current.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Yedeği kaydet / paylaş")
                )
                viewModel.consumeState()
            }
            is BackupExportState.Error -> {
                Toast.makeText(context, current.message, Toast.LENGTH_LONG).show()
                viewModel.consumeState()
            }
            else -> Unit
        }
    }

    Row(modifier = modifier) {
        PremiumTextButton(
            onClick = { viewModel.exportData() },
            enabled = state !is BackupExportState.Loading
        ) {
            Text("Verilerimi Dışa Aktar")
        }

        if (state is BackupExportState.Loading) {
            Spacer(modifier = Modifier.width(8.dp))
            CircularProgressIndicator(
                modifier = Modifier.width(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
