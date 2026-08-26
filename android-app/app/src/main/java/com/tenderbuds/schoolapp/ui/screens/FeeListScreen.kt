package com.tenderbuds.schoolapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tenderbuds.schoolapp.data.Fee
import com.tenderbuds.schoolapp.data.FeeSummary
import com.tenderbuds.schoolapp.ui.components.verticalScrollbar
import com.tenderbuds.schoolapp.ui.theme.BrandIndigo
import com.tenderbuds.schoolapp.ui.theme.DangerRed
import com.tenderbuds.schoolapp.ui.theme.SuccessGreen
import com.tenderbuds.schoolapp.ui.theme.TenderBudsTheme
import java.text.NumberFormat
import java.util.Locale

/**
 * Screen — Fee Management.
 * Rebuilds the original web app's Fee Management tab: the summary strip
 * (Total Receivable / Received / Pending) and the fee table become a
 * card-based mobile list, with the same search-by-student behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeListScreen(
    fees: List<Fee> = emptyList(),
    summary: FeeSummary? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onRetry: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onEditClick: (Fee) -> Unit = {},
    onDeleteConfirm: (Fee) -> Unit = {}
) {
    var feePendingDelete by remember { mutableStateOf<Fee?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fee Management", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !isLoading) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandIndigo, titleContentColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick, containerColor = BrandIndigo, contentColor = Color.White) {
                Icon(Icons.Filled.Add, contentDescription = "Add Fee Record")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            FeeSummaryRow(summary ?: FeeSummary(0.0, 0.0, 0.0))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search by registration number or student name…") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )

            when {
                isLoading && fees.isEmpty() && errorMessage == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                errorMessage != null && fees.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(errorMessage, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onRetry, shape = RoundedCornerShape(16.dp)) { Text("Retry") }
                        }
                    }
                }
                fees.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.MoneyOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                if (searchQuery.isBlank()) "No fee records found" else "No fee records match \"$searchQuery\"",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 88.dp),
                        modifier = Modifier.verticalScrollbar(listState)
                    ) {
                        items(fees, key = { it.id }) { fee ->
                            FeeCard(
                                fee = fee,
                                onEditClick = { onEditClick(fee) },
                                onDeleteClick = { feePendingDelete = fee }
                            )
                        }
                    }
                }
            }
        }
    }

    val pending = feePendingDelete
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { feePendingDelete = null },
            title = { Text("Delete fee record?") },
            text = {
                Text(
                    "Are you sure you want to delete this ${pending.feeType} fee record for " +
                        "${pending.studentName} (${pending.regNo})?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteConfirm(pending)
                    feePendingDelete = null
                }) {
                    Text("Delete", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { feePendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FeeSummaryRow(summary: FeeSummary) {
    val currency = remember { NumberFormat.getIntegerInstance(Locale("en", "IN")) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FeeSummaryCard("Receivable", "₹${currency.format(summary.totalReceivable)}", BrandIndigo, Modifier.weight(1f))
        FeeSummaryCard("Received", "₹${currency.format(summary.totalReceived)}", SuccessGreen, Modifier.weight(1f))
        FeeSummaryCard("Pending", "₹${currency.format(summary.totalPending)}", DangerRed, Modifier.weight(1f))
    }
}

@Composable
private fun FeeSummaryCard(label: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeeCard(fee: Fee, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(fee.studentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${fee.regNo} · ${fee.feeType}${if (fee.month.isNotBlank()) " · ${fee.month}" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit fee for ${fee.studentName}", tint = BrandIndigo)
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete fee for ${fee.studentName}", tint = DangerRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AmountColumn("Total", fee.totalAmount, MaterialTheme.colorScheme.onSurface)
                AmountColumn("Paid", fee.amountPaid, SuccessGreen)
                AmountColumn("Due", fee.dueAmount, if (fee.dueAmount > 0) DangerRed else MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    formatDisplayDate(fee.paymentDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AmountColumn(label: String, amount: Double, valueColor: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "₹${amount.toInt()}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

private fun formatDisplayDate(isoDate: String): String {
    return try {
        val datePart = isoDate.substring(0, 10)
        val (year, month, day) = datePart.split("-")
        val months = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        "$day ${months[month.toInt() - 1]} $year"
    } catch (e: Exception) {
        isoDate
    }
}

@Preview(showBackground = true, name = "Fee Management")
@Composable
private fun FeeListScreenPreview() {
    TenderBudsTheme {
        FeeListScreen(
            fees = listOf(
                Fee("1", "2026001", "Aarav Sharma", "Tuition", "August", 5000.0, 5000.0, 0.0, "2026-08-05"),
                Fee("2", "2026002", "Priya Verma", "Admission", "", 12000.0, 8000.0, 4000.0, "2026-06-01")
            ),
            summary = FeeSummary(totalReceivable = 17000.0, totalReceived = 13000.0, totalPending = 4000.0)
        )
    }
}

@Preview(showBackground = true, name = "Fee Management — Empty")
@Composable
private fun FeeListScreenEmptyPreview() {
    TenderBudsTheme { FeeListScreen() }
}
