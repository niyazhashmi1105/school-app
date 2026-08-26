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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
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
import com.tenderbuds.schoolapp.data.Stock
import com.tenderbuds.schoolapp.ui.components.verticalScrollbar
import com.tenderbuds.schoolapp.ui.theme.BrandIndigo
import com.tenderbuds.schoolapp.ui.theme.DangerRed
import com.tenderbuds.schoolapp.ui.theme.SuccessGreen
import com.tenderbuds.schoolapp.ui.theme.TenderBudsTheme
import com.tenderbuds.schoolapp.ui.theme.WarningAmber

/**
 * Screen — Stock Records.
 * Rebuilds the original web app's Stock Records tab as a card list. The
 * original has no search box on this tab (unlike Students/Fees), so none is
 * added here either — same fields, same behavior, just a mobile layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListScreen(
    stockItems: List<Stock> = emptyList(),
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRefresh: () -> Unit = {},
    onRetry: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onEditClick: (Stock) -> Unit = {},
    onDeleteConfirm: (Stock) -> Unit = {}
) {
    var stockPendingDelete by remember { mutableStateOf<Stock?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock Records", fontWeight = FontWeight.SemiBold) },
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
                Icon(Icons.Filled.Add, contentDescription = "Add Stock Item")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                isLoading && stockItems.isEmpty() && errorMessage == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                errorMessage != null && stockItems.isEmpty() -> {
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
                stockItems.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No stock records found",
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
                        items(stockItems, key = { it.id }) { stock ->
                            StockCard(
                                stock = stock,
                                onEditClick = { onEditClick(stock) },
                                onDeleteClick = { stockPendingDelete = stock }
                            )
                        }
                    }
                }
            }
        }
    }

    val pending = stockPendingDelete
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { stockPendingDelete = null },
            title = { Text("Delete stock item?") },
            text = { Text("Are you sure you want to delete \"${pending.itemName}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteConfirm(pending)
                    stockPendingDelete = null
                }) {
                    Text("Delete", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { stockPendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

private fun stockStatus(remainingStock: Int): Pair<String, Color> = when {
    remainingStock <= 0 -> "Out of Stock" to DangerRed
    remainingStock < 10 -> "Low Stock" to WarningAmber
    else -> "In Stock" to SuccessGreen
}

@Composable
private fun StockCard(stock: Stock, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    val (statusLabel, statusColor) = stockStatus(stock.remainingStock)
    val classOrSize = if (stock.studentClass.isNotBlank()) {
        "Class ${stock.studentClass}"
    } else if (stock.size.isNotBlank()) {
        "Size ${stock.size}"
    } else {
        "-"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stock.itemName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${stock.itemType} · $classOrSize",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit ${stock.itemName}", tint = BrandIndigo)
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete ${stock.itemName}", tint = DangerRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                QuantityColumn("Total", stock.totalQuantity, MaterialTheme.colorScheme.onSurface)
                QuantityColumn("Sold", stock.quantitySold, MaterialTheme.colorScheme.onSurface)
                Column {
                    Text("Remaining", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${stock.remainingStock}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                    Text(statusLabel, style = MaterialTheme.typography.bodySmall, color = statusColor)
                }
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
                    formatDisplayDate(stock.date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuantityColumn(label: String, value: Int, valueColor: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "$value",
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

@Preview(showBackground = true, name = "Stock Records")
@Composable
private fun StockListScreenPreview() {
    TenderBudsTheme {
        StockListScreen(
            stockItems = listOf(
                Stock("1", "Book", "English Book", "", "", "3", "", "Class 3 - English Book", 50, 45, 5, "2026-06-01"),
                Stock("2", "Dress", "Summer Uniform", "Pant", "Boys", "", "30", "Boys Summer Uniform (Pant) - Size 30", 20, 20, 0, "2026-05-10")
            )
        )
    }
}

@Preview(showBackground = true, name = "Stock Records — Empty")
@Composable
private fun StockListScreenEmptyPreview() {
    TenderBudsTheme { StockListScreen() }
}
