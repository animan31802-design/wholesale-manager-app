package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.printer.BillStyle
import com.animan.wholesalemanager.printer.PrinterManager
import com.animan.wholesalemanager.utils.AppLanguage
import com.animan.wholesalemanager.utils.AppPreferences
import com.animan.wholesalemanager.utils.Language
import com.animan.wholesalemanager.utils.PrinterPreferences
import com.animan.wholesalemanager.utils.UpiQrGenerator
import com.animan.wholesalemanager.viewmodel.BackupViewModel
import com.animan.wholesalemanager.work.BackupScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context        = LocalContext.current
    val S              = AppLanguage.strings
    val backupViewModel: BackupViewModel = viewModel()
    val scope          = rememberCoroutineScope()
    val snackbarHost   = remember { SnackbarHostState() }

    var shopName        by remember { mutableStateOf(AppPreferences.getShopName(context)) }
    var upiId           by remember { mutableStateOf(AppPreferences.getUpiId(context)) }
    var backupEnabled   by remember { mutableStateOf(AppPreferences.isBackupEnabled(context)) }
    var backupFrequency by remember { mutableStateOf(AppPreferences.getBackupFrequency(context)) }
    var shopNameSaved   by remember { mutableStateOf("") }
    var upiSaved        by remember { mutableStateOf("") }
    var isTestPrinting  by remember { mutableStateOf(false) }

    var selectedBillStyle by remember {
        mutableStateOf(PrinterPreferences.getBillStyle(context))
    }

    val previewQr = remember(upiId) {
        if (upiId.isNotBlank())
            UpiQrGenerator.generate(upiId, shopName, 1.0, "Test", 256)
        else null
    }

    val frequencyOptions = listOf("Manual only", "Daily", "Weekly")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Shop name ─────────────────────────────────────────────
            SectionHeader(Icons.Filled.Store, "Shop details")

            OutlinedTextField(
                value = shopName, onValueChange = { shopName = it; shopNameSaved = "" },
                label = { Text("Shop name") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            if (shopNameSaved.isNotEmpty())
                Text(shopNameSaved, color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall)

            Button(onClick = {
                AppPreferences.setShopName(context, shopName.trim())
                shopNameSaved = "Saved."
            }, modifier = Modifier.fillMaxWidth()) { Text("Save shop name") }

            HorizontalDivider()

            // ── UPI ID ────────────────────────────────────────────────
            SectionHeader(Icons.Filled.QrCode, "UPI payment")

            Text(
                "Enter your UPI ID so customers can pay by scanning a QR on the bill screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = upiId, onValueChange = { upiId = it; upiSaved = "" },
                label = { Text("UPI ID (e.g. yourshop@upi)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("yourshop@okicici") }
            )
            if (upiSaved.isNotEmpty())
                Text(upiSaved, color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall)

            Button(onClick = {
                AppPreferences.setUpiId(context, upiId.trim())
                upiSaved = "UPI ID saved."
            }, modifier = Modifier.fillMaxWidth()) { Text("Save UPI ID") }

            if (previewQr != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Preview QR (amount: ₹1 — for testing only)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Image(bitmap = previewQr.asImageBitmap(),
                        contentDescription = "UPI QR preview",
                        modifier = Modifier.size(160.dp))
                }
            }

            HorizontalDivider()

            // ── Language ──────────────────────────────────────────────
            SectionHeader(Icons.Filled.Language, S.language)

            Text(
                "Current: ${if (AppLanguage.current == Language.TAMIL) "தமிழ்" else "English"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        AppLanguage.setLanguage(context, Language.ENGLISH)
                        (context as? android.app.Activity)?.recreate()
                    },
                    modifier = Modifier.weight(1f),
                    colors = if (AppLanguage.current == Language.ENGLISH)
                        ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
                ) { Text("English") }

                Button(
                    onClick = {
                        AppLanguage.setLanguage(context, Language.TAMIL)
                        (context as? android.app.Activity)?.recreate()
                    },
                    modifier = Modifier.weight(1f),
                    colors = if (AppLanguage.current == Language.TAMIL)
                        ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
                ) { Text("தமிழ்") }
            }

            HorizontalDivider()

            // ── Printer ───────────────────────────────────────────────
            SectionHeader(Icons.Filled.Print, "Printer")

            OutlinedButton(
                onClick = { navController.navigate("printer_selector") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Select / change Bluetooth printer") }

            HorizontalDivider()

            // ── Bill style ────────────────────────────────────────────
            SectionHeader(Icons.Filled.Receipt, "Bill style")

            Text(
                "Choose how your printed receipts look. Tap a style to select it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))

            BillStyleCard(
                title       = "Style 1 — Minimal",
                description = "Clean & compact. Item / Qty / Total.\nUnit price in small text. Quick kirana billing.",
                preview     = minimalPreview(),
                selected    = selectedBillStyle == BillStyle.MINIMAL,
                onClick     = {
                    selectedBillStyle = BillStyle.MINIMAL
                    PrinterPreferences.setBillStyle(context, BillStyle.MINIMAL)
                }
            )
            BillStyleCard(
                title       = "Style 2 — Professional",
                description = "Bold headers, Item / Qty / Price / Total columns.\nSolid dividers. Best for general retail.",
                preview     = professionalPreview(),
                selected    = selectedBillStyle == BillStyle.PROFESSIONAL,
                onClick     = {
                    selectedBillStyle = BillStyle.PROFESSIONAL
                    PrinterPreferences.setBillStyle(context, BillStyle.PROFESSIONAL)
                }
            )
            BillStyleCard(
                title       = "Style 3 — GST Detailed (Tax Invoice)",
                description = "Full CGST + SGST breakdown per item.\nTaxable amount summary. Best for B2B wholesale.",
                preview     = gstDetailedPreview(),
                selected    = selectedBillStyle == BillStyle.GST_DETAILED,
                onClick     = {
                    selectedBillStyle = BillStyle.GST_DETAILED
                    PrinterPreferences.setBillStyle(context, BillStyle.GST_DETAILED)
                }
            )

            // ── Test print ────────────────────────────────────────────
            // FIX: Bluetooth connect + socket.write must stay on the same
            // thread. The dashboard works because it calls printBill() from
            // a plain onClick (main thread). We mirror that here:
            // - coroutine launches on Default (keeps UI responsive)
            // - but the actual BluetoothPrinter calls are NOT moved to IO
            //   because many BT SPP implementations are NOT thread-safe and
            //   the socket must be opened and written on one thread.
            // If your BluetoothPrinter already handles threading internally,
            // Dispatchers.Default here is equivalent to the dashboard call.
            val styleLabel = when (selectedBillStyle) {
                BillStyle.MINIMAL      -> "Minimal"
                BillStyle.PROFESSIONAL -> "Professional"
                BillStyle.GST_DETAILED -> "GST Detailed"
            }

            Button(
                onClick = {
                    if (isTestPrinting) return@Button
                    isTestPrinting = true
                    // Capture applicationContext so it outlives any
                    // recomposition that might happen during the print.
                    val appCtx = context.applicationContext
                    scope.launch(Dispatchers.Default) {
                        // PrinterManager is stateless — safe to create here.
                        val pm     = PrinterManager()
                        val result = pm.printTestBill(appCtx)
                        withContext(Dispatchers.Main) {
                            isTestPrinting = false
                            snackbarHost.showSnackbar(result)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled  = !isTestPrinting
            ) {
                if (isTestPrinting) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Printing…")
                } else {
                    Icon(Icons.Filled.Print, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Print test bill ($styleLabel)")
                }
            }

            HorizontalDivider()

            // ── Backup ────────────────────────────────────────────────
            SectionHeader(Icons.Filled.CloudUpload, "Cloud backup")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Enable automatic backup")
                    Text("Backs up to Firebase",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = backupEnabled, onCheckedChange = {
                    backupEnabled = it
                    AppPreferences.setBackupEnabled(context, it)
                    BackupScheduler.schedule(context)
                })
            }

            if (backupEnabled) {
                Text("Backup frequency", style = MaterialTheme.typography.labelMedium)
                frequencyOptions.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = backupFrequency == option,
                            onClick  = {
                                backupFrequency = option
                                AppPreferences.setBackupFrequency(context, option)
                                BackupScheduler.schedule(context)
                            }
                        )
                        Text(option, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            val lastBackup = backupViewModel.lastBackupTime.value
            if (lastBackup.isNotEmpty())
                Text("Last backup: $lastBackup",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

            backupViewModel.backupStatus.value.let { status ->
                if (status.isNotEmpty())
                    Text(
                        status,
                        color = if (status.startsWith("Error"))
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
            }

            Button(
                onClick  = { backupViewModel.backupNow(context) },
                modifier = Modifier.fillMaxWidth(),
                enabled  = !backupViewModel.isBackingUp.value
            ) {
                if (backupViewModel.isBackingUp.value)
                    CircularProgressIndicator(strokeWidth = 2.dp)
                else {
                    Icon(Icons.Filled.CloudUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Backup now")
                }
            }

            HorizontalDivider()

            SectionHeader(Icons.Filled.CloudDownload, "Restore")

            Text(
                "Restore will overwrite your current local data with the latest backup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick  = { backupViewModel.restoreNow(context) },
                modifier = Modifier.fillMaxWidth(),
                enabled  = !backupViewModel.isBackingUp.value
            ) { Text("Restore from last backup") }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Bill style card ───────────────────────────────────────────────────

@Composable
private fun BillStyleCard(
    title: String,
    description: String,
    preview: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        border   = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text       = title,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                if (selected)
                    Icon(Icons.Filled.CheckCircle, "Selected",
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
            }
            Text(
                text  = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape    = RoundedCornerShape(6.dp),
                color    = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text       = preview,
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 9.sp,
                    lineHeight = 13.sp,
                    modifier   = Modifier.padding(8.dp),
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Receipt previews ──────────────────────────────────────────────────

private fun minimalPreview() = """
HAPPY MART
- - - - - - - - - - - - - - - - -
Bill No     : B123456
Date & Time : 24/04/2026 11:45 AM
- - - - - - - - - - - - - - - - -
To : ANIMAN   Ph : 9876543210
- - - - - - - - - - - - - - - - -
ITEM                  QTY    TOTAL
. . . . . . . . . . . . . . . . .
Rice (1kg)              2   100.00
  @ Rs.50.00/unit  GST 5%: Rs.5.00
Sunflower Oil (1L)      1   120.00
  @ Rs.120.00/unit
- - - - - - - - - - - - - - - - -
SUBTOTAL                    265.00
GST                          11.00
TOTAL AMOUNT                276.00
- - - - - - - - - - - - - - - - -
AMOUNT PAID                 276.00
    ** PAID IN FULL **
  --- ♥ Thank You! ♥ ---
""".trimIndent()

private fun professionalPreview() = """
        HAPPY MART
           BILL
------------------------------------------------
Bill No. : B123456   24/04/2026 11:45 AM
------------------------------------------------
To : ANIMAN
Ph : 9876543210
------------------------------------------------
ITEM                 QTY     PRICE      TOTAL
- - - - - - - - - - - - - - - - - - - - - - -
Rice (1kg)             2   Rs.50.00  Rs.100.00
  GST 5% : Rs.5.00
- - - - - - - - - - - - - - - - - - - - - - -
Sunflower Oil (1L)     1  Rs.120.00  Rs.120.00
  GST 5% : Rs.6.00
- - - - - - - - - - - - - - - - - - - - - - -
SUBTOTAL                              265.00
GST                                    11.00
TOTAL AMOUNT                          276.00
------------------------------------------------
AMOUNT PAID                           276.00
     ** PAID IN FULL **
   -- ♥ Thank You! ♥ --
""".trimIndent()

private fun gstDetailedPreview() = """
        HAPPY MART
        TAX INVOICE
------------------------------------------------
Bill No   : B123456
Date/Time : 24/04/2026 11:45 AM
------------------------------------------------
Billed To :
  ANIMAN  |  Ph : 9876543210
------------------------------------------------
 # ITEM              QTY    RATE      AMT
- - - - - - - - - - - - - - - - - - - - - - -
 1 Rice (1kg)          2   50.00   100.00
   CGST 2.50%:2.50  SGST 2.50%:2.50
 2 Sunflower Oil (1L)  1  120.00   120.00
   CGST 2.50%:3.00  SGST 2.50%:3.00
- - - - - - - - - - - - - - - - - - - - - - -
Taxable amount                        265.00
  CGST                                  5.50
  SGST                                  5.50
Total GST                              11.00
GRAND TOTAL                           276.00
------------------------------------------------
      ** PAID IN FULL **
""".trimIndent()

// ── Section header ────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null)
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}