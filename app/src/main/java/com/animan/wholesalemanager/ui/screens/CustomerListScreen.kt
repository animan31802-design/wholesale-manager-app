package com.animan.wholesalemanager.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.utils.PriceUtils.round2dp
import com.animan.wholesalemanager.utils.PriceUtils.toRupees
import com.animan.wholesalemanager.viewmodel.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(navController: NavController) {
    val viewModel: CustomerViewModel = viewModel()
    val context = LocalContext.current

    var searchQuery      by remember { mutableStateOf("") }
    var deleteTargetId   by remember { mutableStateOf<String?>(null) }
    var deleteTargetName by remember { mutableStateOf("") }

    LaunchedEffect(true) { viewModel.fetchCustomers() }
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) viewModel.fetchCustomers()
        else viewModel.searchCustomers(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customers") },
                actions = {
                    IconButton(onClick = { navController.navigate("add_customer") }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Add customer")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder   = { Text("Search by name or phone") },
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine    = true,
                shape         = RoundedCornerShape(50),
                leadingIcon   = { Icon(Icons.Filled.Search, null) },
                trailingIcon  = {
                    if (searchQuery.isNotEmpty())
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, null)
                        }
                }
            )

            if (viewModel.isLoading.value)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            if (viewModel.customerList.value.isEmpty() && !viewModel.isLoading.value) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.PeopleAlt, null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Text("No customers yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilledTonalButton(onClick = { navController.navigate("add_customer") }) {
                            Icon(Icons.Filled.PersonAdd, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add first customer")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.customerList.value, key = { it.id }) { customer ->
                        val hasBalance = customer.balance.round2dp() > 0.005

                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            colors    = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column {
                                // ── Coloured top strip ────────────────────
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .background(
                                            if (hasBalance)
                                                MaterialTheme.colorScheme.error
                                            else
                                                MaterialTheme.colorScheme.primary
                                        )
                                )

                                Column(modifier = Modifier.padding(14.dp)) {

                                    // ── Avatar + name + contact ───────────
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Avatar circle with first letter
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                customer.name.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                customer.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (customer.phone.isNotBlank()) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(Icons.Filled.Phone, null,
                                                        modifier = Modifier.size(12.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(customer.phone,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            if (customer.address.isNotBlank()) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(Icons.Filled.LocationOn, null,
                                                        modifier = Modifier.size(12.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(customer.address,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1)
                                                }
                                            }
                                        }

                                        // Quick-dial button
                                        if (customer.phone.isNotBlank()) {
                                            FilledIconButton(
                                                onClick = {
                                                    context.startActivity(Intent(
                                                        Intent.ACTION_DIAL,
                                                        Uri.parse("tel:${customer.phone}")
                                                    ))
                                                },
                                                modifier = Modifier.size(36.dp),
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                                )
                                            ) {
                                                Icon(Icons.Filled.Call, "Call",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        // Navigate button
                                        if (customer.latitude != null && customer.longitude != null) {
                                            FilledIconButton(
                                                onClick = {
                                                    val uri = Uri.parse(
                                                        "google.navigation:q=${customer.latitude},${customer.longitude}")
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_VIEW, uri)
                                                            .setPackage("com.google.android.apps.maps"))
                                                },
                                                modifier = Modifier.size(36.dp),
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                                )
                                            ) {
                                                Icon(Icons.Filled.Navigation, "Navigate",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    // ── Balance badge ─────────────────────
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (hasBalance)
                                                MaterialTheme.colorScheme.errorContainer
                                            else
                                                MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(
                                                    horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    if (hasBalance) Icons.Filled.AccountBalanceWallet
                                                    else Icons.Filled.CheckCircle,
                                                    null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = if (hasBalance)
                                                        MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    if (hasBalance) "Due: ${customer.balance.toRupees()}"
                                                    else "Cleared",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (hasBalance)
                                                        MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        // Total purchase stat
                                        Text(
                                            "Total: ${customer.totalPurchase.toRupees()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(Modifier.height(10.dp))
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    Spacer(Modifier.height(8.dp))

                                    // ── Action row ────────────────────────
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        ListActionButton(Icons.Filled.ReceiptLong, "Bill",
                                            MaterialTheme.colorScheme.primary) {
                                            navController.navigate("billing/${customer.id}")
                                        }
                                        ListActionButton(Icons.Filled.MenuBook, "Ledger",
                                            MaterialTheme.colorScheme.secondary) {
                                            navController.navigate("ledger/${customer.id}")
                                        }
                                        ListActionButton(
                                            icon    = Icons.Filled.Payments,
                                            label   = "Pay",
                                            tint    = if (hasBalance)
                                                MaterialTheme.colorScheme.tertiary
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(0.25f),
                                            enabled = hasBalance
                                        ) { navController.navigate("payment/${customer.id}") }
                                        ListActionButton(Icons.Filled.Edit, "Edit",
                                            MaterialTheme.colorScheme.onSurfaceVariant) {
                                            navController.navigate("edit_customer/${customer.id}")
                                        }
                                        ListActionButton(Icons.Filled.DeleteOutline, "Delete",
                                            MaterialTheme.colorScheme.error) {
                                            deleteTargetId   = customer.id
                                            deleteTargetName = customer.name
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            viewModel.errorMessage.value?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp))
            }
        }
    }

    if (deleteTargetId != null) {
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            icon    = { Icon(Icons.Filled.Warning, null,
                tint = MaterialTheme.colorScheme.error) },
            title   = { Text("Delete customer") },
            text    = { Text("Delete \"$deleteTargetName\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteCustomer(deleteTargetId!!) {}; deleteTargetId = null },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteTargetId = null }) { Text("Cancel") }
            }
        )
    }
}

// ── Shared action button (icon + label stacked) ───────────────────────

@Composable
fun ListActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp)
    ) {
        IconButton(
            onClick  = onClick,
            enabled  = enabled,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                icon, contentDescription = label,
                tint     = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            label,
            fontSize = 10.sp,
            color    = if (enabled) tint
            else MaterialTheme.colorScheme.onSurface.copy(0.25f),
            fontWeight = FontWeight.Medium
        )
    }
}