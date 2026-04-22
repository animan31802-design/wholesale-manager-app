package com.animan.wholesalemanager.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.animan.wholesalemanager.utils.AppPreferences
import com.animan.wholesalemanager.utils.PriceUtils.toRupees
import com.animan.wholesalemanager.utils.UpiQrGenerator

@Composable
fun UpiPaymentDialog(
    amount: Double,
    billNote: String,
    onMarkPaid: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val shopName = AppPreferences.getShopName(context)
    val upiId    = AppPreferences.getUpiId(context)

    val qrBitmap = remember(upiId, amount) {
        if (upiId.isNotBlank() && amount > 0)
            UpiQrGenerator.generate(upiId, shopName, amount, billNote, 512)
        else null
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.QrCode, contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary)

                Text("Pay via UPI", style = MaterialTheme.typography.titleLarge)

                Text(
                    "Amount: ${amount.toRupees()}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                if (upiId.isBlank()) {
                    // No UPI ID configured
                    Card(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            "UPI ID not configured.\nGo to Settings → UPI payment to add your UPI ID.",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else if (qrBitmap != null) {
                    // QR code
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "UPI QR code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        "Scan with GPay, PhonePe, Paytm\nor any UPI app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        "UPI ID: $upiId",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Open UPI app directly button
                    OutlinedButton(
                        onClick = {
                            val upiUrl = "upi://pay?pa=${upiId}&pn=${shopName.replace(" ","%20")}" +
                                    "&am=${"%.2f".format(amount)}&cu=INR&tn=${billNote.replace(" ","%20")}"
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(upiUrl))
                                    .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Open UPI app") }
                }

                HorizontalDivider()

                Text(
                    "Once customer pays, verify in your UPI app then tap Mark as Paid",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onMarkPaid,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Mark as paid")
                }

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}