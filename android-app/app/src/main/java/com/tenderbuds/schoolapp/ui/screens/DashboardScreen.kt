package com.tenderbuds.schoolapp.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tenderbuds.schoolapp.data.ClassStockItem
import com.tenderbuds.schoolapp.data.DashboardSummary
import com.tenderbuds.schoolapp.data.StudentFeeStatus
import com.tenderbuds.schoolapp.ui.components.verticalScrollbar
import com.tenderbuds.schoolapp.ui.theme.BrandIndigo
import com.tenderbuds.schoolapp.ui.theme.BrandPurple
import com.tenderbuds.schoolapp.ui.theme.DangerRed
import com.tenderbuds.schoolapp.ui.theme.SuccessGreen
import com.tenderbuds.schoolapp.ui.theme.TenderBudsTheme
import com.tenderbuds.schoolapp.ui.theme.WarningAmber
import java.text.NumberFormat
import java.util.Locale

/** Max visible height before a section switches to its own internal (scrollbar-equipped) scroll. */
private val SECTION_MAX_HEIGHT = 420.dp

/**
 * Screen 4 of 14 — Dashboard.
 * The post-login home. Rebuilds the original web app's Dashboard tab (stat
 * cards, per-student fee status, class-wise stock availability) as a
 * scrollable mobile layout instead of wide desktop tables.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userName: String,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    summary: DashboardSummary? = null,
    studentFeeStatus: List<StudentFeeStatus> = emptyList(),
    classStock: List<ClassStockItem> = emptyList(),
    feeSearchQuery: String = "",
    onFeeSearchQueryChange: (String) -> Unit = {},
    stockFilter: String = "",
    onStockFilterChange: (String) -> Unit = {},
    isExporting: Boolean = false,
    isImporting: Boolean = false,
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onLogout: () -> Unit = {},
    onRetry: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dashboard", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Welcome, $userName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !isLoading) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, contentDescription = "Log out", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandIndigo,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when {
            isLoading && summary == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            errorMessage != null && summary == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetry, shape = RoundedCornerShape(16.dp)) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        BackupPanel(
                            isExporting = isExporting,
                            isImporting = isImporting,
                            onExportClick = onExportClick,
                            onImportClick = onImportClick
                        )
                    }

                    item {
                        StatCardsRow(summary ?: DashboardSummary(0, 0.0, 0.0, 0, 0))
                    }

                    item {
                        SectionHeader(title = "Fee Status by Student", modifier = Modifier.padding(top = 8.dp))
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            OutlinedTextField(
                                value = feeSearchQuery,
                                onValueChange = onFeeSearchQueryChange,
                                placeholder = { Text("Search by registration number…") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    item {
                        if (studentFeeStatus.isEmpty()) {
                            EmptyStateRow(
                                if (feeSearchQuery.isBlank()) "No fee records found" else "No students match \"$feeSearchQuery\""
                            )
                        } else {
                            val feeListState = rememberLazyListState()
                            LazyColumn(
                                state = feeListState,
                                modifier = Modifier
                                    .heightIn(max = SECTION_MAX_HEIGHT)
                                    .verticalScrollbar(feeListState)
                            ) {
                                items(studentFeeStatus, key = { it.regNo }) { student ->
                                    FeeStatusCard(student)
                                }
                            }
                        }
                    }

                    item {
                        SectionHeader(title = "Class-wise Stock Availability", modifier = Modifier.padding(top = 16.dp))
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            OutlinedTextField(
                                value = stockFilter,
                                onValueChange = onStockFilterChange,
                                placeholder = { Text("Filter by class or item…") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    item {
                        if (classStock.isEmpty()) {
                            EmptyStateRow("No stock records found")
                        } else {
                            val stockListState = rememberLazyListState()
                            LazyColumn(
                                state = stockListState,
                                modifier = Modifier
                                    .heightIn(max = SECTION_MAX_HEIGHT)
                                    .verticalScrollbar(stockListState)
                            ) {
                                items(classStock, key = { "${it.classLabel}-${it.itemType}-${it.itemName}" }) { stock ->
                                    StockAvailabilityCard(stock)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupPanel(
    isExporting: Boolean,
    isImporting: Boolean,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BrandIndigo.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "💾 Backup & Restore Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Export a backup of all Students, Fees, and Stock data. Import a backup file to restore it — existing data is never deleted.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onExportClick,
                    enabled = !isExporting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export")
                    }
                }
                OutlinedButton(
                    onClick = onImportClick,
                    enabled = !isImporting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BrandPurple, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import")
                    }
                }
            }
        }
    }
}

/**
 * A 2-column grid rather than a horizontally-scrolling row. A LazyRow here
 * previously required a sideways swipe to see the later cards, but a
 * horizontal scroll nested inside a vertically-scrolling page is notoriously
 * unreliable on a real touchscreen — any vertical drift in the swipe gets
 * captured by the page's own vertical scroll instead. A grid needs no
 * swipe gesture at all, so it sidesteps the problem entirely.
 */
@Composable
private fun StatCardsRow(summary: DashboardSummary) {
    val currency = remember(summary) {
        NumberFormat.getIntegerInstance(Locale("en", "IN"))
    }
    val cards = listOf(
        Triple(Icons.Filled.Groups, "Total Students", summary.totalStudents.toString()) to BrandIndigo,
        Triple(Icons.Filled.Payments, "Fees Collected", "₹${currency.format(summary.totalCollected)}") to SuccessGreen,
        Triple(Icons.Filled.ReportProblem, "Total Dues", "₹${currency.format(summary.totalDues)}") to DangerRed,
        Triple(Icons.Filled.Inventory2, "Stock Items", summary.totalStock.toString()) to WarningAmber,
        Triple(Icons.Filled.RemoveShoppingCart, "Out of Stock", summary.outOfStockCount.toString()) to DangerRed
    )

    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        cards.chunked(2).forEach { rowCards ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowCards.forEach { (info, accentColor) ->
                    val (icon, label, value) = info
                    StatCard(
                        icon = icon,
                        label = label,
                        value = value,
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowCards.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun FeeStatusCard(student: StudentFeeStatus) {
    val isPaid = student.status == "Paid"
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${student.regNo} · Class ${student.studentClass}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(text = student.status, color = if (isPaid) SuccessGreen else DangerRed)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AmountLabel("Total", student.totalFees, MaterialTheme.colorScheme.onSurface)
                AmountLabel("Paid", student.totalPaid, SuccessGreen)
                AmountLabel("Due", student.totalDue, if (student.totalDue > 0) DangerRed else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun AmountLabel(label: String, amount: Double, valueColor: Color) {
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

@Composable
private fun StockAvailabilityCard(stock: ClassStockItem) {
    val statusColor = when (stock.status) {
        "Out of Stock" -> DangerRed
        "Low Stock" -> WarningAmber
        else -> SuccessGreen
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stock.itemName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${stock.classLabel} · ${stock.itemType}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stock.remainingStock.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Text(stock.status, style = MaterialTheme.typography.bodyMedium, color = statusColor)
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyStateRow(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Preview(showBackground = true, name = "Dashboard — With data")
@Composable
private fun DashboardScreenPreview() {
    TenderBudsTheme {
        DashboardScreen(
            userName = "Admin",
            summary = DashboardSummary(totalStudents = 42, totalCollected = 185000.0, totalDues = 23000.0, totalStock = 18, outOfStockCount = 2),
            studentFeeStatus = listOf(
                StudentFeeStatus("2026001", "Aarav Sharma", "3", 5000.0, 5000.0, 0.0, "Paid"),
                StudentFeeStatus("2026002", "Priya Verma", "5", 6000.0, 3000.0, 3000.0, "Pending")
            ),
            classStock = listOf(
                ClassStockItem("Class 3", "Book", "Class 3 - English Book", 40, "In Stock"),
                ClassStockItem("Class 5", "Dress", "Boys Summer Uniform (Shirt) - Size 30", 4, "Low Stock"),
                ClassStockItem("Class 8", "Book", "Class 8 - Maths Book", 0, "Out of Stock")
            )
        )
    }
}

@Preview(showBackground = true, name = "Dashboard — Empty")
@Composable
private fun DashboardScreenEmptyPreview() {
    TenderBudsTheme {
        DashboardScreen(
            userName = "Admin",
            summary = DashboardSummary(0, 0.0, 0.0, 0, 0)
        )
    }
}
