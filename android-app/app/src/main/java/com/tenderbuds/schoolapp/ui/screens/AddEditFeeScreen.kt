package com.tenderbuds.schoolapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tenderbuds.schoolapp.data.Fee
import com.tenderbuds.schoolapp.data.Student
import com.tenderbuds.schoolapp.ui.components.AuthTextField
import com.tenderbuds.schoolapp.ui.theme.BrandIndigo
import com.tenderbuds.schoolapp.ui.theme.TenderBudsTheme
import com.tenderbuds.schoolapp.ui.validation.Validators
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val FEE_TYPE_OPTIONS = listOf(
    "Admission" to "Admission Fee",
    "Tuition" to "Tuition Fee",
    "Both" to "Both"
)

private val MONTH_OPTIONS = listOf("" to "None") + listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
).map { it to it }

/** What the screen hands back on Save — the server computes id, studentName, and dueAmount. */
data class FeeFormResult(
    val regNo: String,
    val feeType: String,
    val month: String,
    val totalAmount: Double,
    val amountPaid: Double,
    val paymentDate: String
)

/**
 * Screen — Add/Edit Fee Record.
 * Mirrors the original web app's Add/Edit Fee Record modal: pick a student,
 * fee type, and month, enter total/paid amounts, and the due amount and
 * payment date round out the record — same auto-calculation behavior the
 * original form had (Due = Total − Paid, floored at zero).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFeeScreen(
    isEditMode: Boolean,
    students: List<Student> = emptyList(),
    initialFee: Fee? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onSaveClick: (FeeFormResult) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var selectedRegNo by rememberSaveable { mutableStateOf(initialFee?.regNo ?: "") }
    var selectedStudentName by rememberSaveable { mutableStateOf(initialFee?.studentName ?: "") }
    var feeType by rememberSaveable { mutableStateOf(initialFee?.feeType ?: "") }
    var month by rememberSaveable { mutableStateOf(initialFee?.month ?: "") }
    var totalAmount by rememberSaveable { mutableStateOf(initialFee?.totalAmount?.let { formatAmount(it) } ?: "") }
    var amountPaid by rememberSaveable { mutableStateOf(initialFee?.amountPaid?.let { formatAmount(it) } ?: "") }
    var paymentDate by rememberSaveable { mutableStateOf(initialFee?.paymentDate ?: "") }

    var studentError by rememberSaveable { mutableStateOf<String?>(null) }
    var feeTypeError by rememberSaveable { mutableStateOf<String?>(null) }
    var totalAmountError by rememberSaveable { mutableStateOf<String?>(null) }
    var amountPaidError by rememberSaveable { mutableStateOf<String?>(null) }
    var dateError by rememberSaveable { mutableStateOf<String?>(null) }

    var studentMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var feeTypeMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var monthMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val dueAmount = remember(totalAmount, amountPaid) {
        val total = totalAmount.trim().toDoubleOrNull()
        val paid = amountPaid.trim().toDoubleOrNull()
        if (total != null && paid != null) maxOf(0.0, total - paid) else null
    }

    fun validateAndSubmit() {
        studentError = Validators.feeStudent(selectedRegNo)
        feeTypeError = Validators.feeType(feeType)
        totalAmountError = Validators.feeAmount(totalAmount, "Total amount")
        amountPaidError = Validators.feeAmount(amountPaid, "Amount paid")
            ?: Validators.amountPaidNotExceeding(totalAmount, amountPaid)
        dateError = Validators.paymentDate(paymentDate)

        val hasErrors = listOf(studentError, feeTypeError, totalAmountError, amountPaidError, dateError).any { it != null }
        if (!hasErrors) {
            onSaveClick(
                FeeFormResult(
                    regNo = selectedRegNo,
                    feeType = feeType,
                    month = month,
                    totalAmount = totalAmount.trim().toDouble(),
                    amountPaid = amountPaid.trim().toDouble(),
                    paymentDate = paymentDate
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Fee Record" else "Add Fee Record", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandIndigo, titleContentColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = studentMenuExpanded,
                onExpandedChange = { studentMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = if (selectedRegNo.isNotEmpty()) "$selectedRegNo · $selectedStudentName" else "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Student Registration Number") },
                    placeholder = { Text("Select Student") },
                    leadingIcon = { Icon(Icons.Filled.PersonSearch, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentMenuExpanded) },
                    isError = studentError != null,
                    supportingText = { if (studentError != null) Text(studentError!!, color = MaterialTheme.colorScheme.error) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = studentMenuExpanded, onDismissRequest = { studentMenuExpanded = false }) {
                    if (students.isEmpty()) {
                        DropdownMenuItem(text = { Text("No students found") }, onClick = {}, enabled = false)
                    }
                    students.forEach { student ->
                        DropdownMenuItem(
                            text = { Text("${student.regNo} · ${student.name}") },
                            onClick = {
                                selectedRegNo = student.regNo
                                selectedStudentName = student.name
                                studentError = null
                                studentMenuExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            AuthTextField(
                value = selectedStudentName,
                onValueChange = {},
                label = "Student Name",
                leadingIcon = Icons.Filled.Person
            )
            Spacer(modifier = Modifier.height(14.dp))

            ExposedDropdownMenuBox(
                expanded = feeTypeMenuExpanded,
                onExpandedChange = { feeTypeMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = FEE_TYPE_OPTIONS.firstOrNull { it.first == feeType }?.second ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fee Type") },
                    placeholder = { Text("Select Fee Type") },
                    leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = feeTypeMenuExpanded) },
                    isError = feeTypeError != null,
                    supportingText = { if (feeTypeError != null) Text(feeTypeError!!, color = MaterialTheme.colorScheme.error) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = feeTypeMenuExpanded, onDismissRequest = { feeTypeMenuExpanded = false }) {
                    FEE_TYPE_OPTIONS.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                feeType = value
                                feeTypeError = null
                                feeTypeMenuExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            ExposedDropdownMenuBox(
                expanded = monthMenuExpanded,
                onExpandedChange = { monthMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = MONTH_OPTIONS.firstOrNull { it.first == month }?.second ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Month") },
                    placeholder = { Text("Select Month") },
                    leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthMenuExpanded) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = monthMenuExpanded, onDismissRequest = { monthMenuExpanded = false }) {
                    MONTH_OPTIONS.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                month = value
                                monthMenuExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            AuthTextField(
                value = totalAmount,
                onValueChange = { totalAmount = it; totalAmountError = null; amountPaidError = null },
                label = "Total Amount",
                leadingIcon = Icons.Filled.CurrencyRupee,
                errorMessage = totalAmountError,
                keyboardType = KeyboardType.Decimal
            )
            Spacer(modifier = Modifier.height(14.dp))

            AuthTextField(
                value = amountPaid,
                onValueChange = { amountPaid = it; amountPaidError = null },
                label = "Amount Paid",
                leadingIcon = Icons.Filled.CurrencyRupee,
                errorMessage = amountPaidError,
                keyboardType = KeyboardType.Decimal
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = dueAmount?.let { formatAmount(it) } ?: "",
                onValueChange = {},
                enabled = false,
                label = { Text("Due Amount") },
                placeholder = { Text("Auto-calculated") },
                leadingIcon = { Icon(Icons.Filled.CurrencyRupee, contentDescription = null) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))

            Box {
                // Same disabled+overlay pattern as the Admission Date field on
                // Add/Edit Student — a readOnly field swallows taps meant for
                // an attached clickable modifier, but a disabled one doesn't.
                OutlinedTextField(
                    value = paymentDate,
                    onValueChange = {},
                    enabled = false,
                    label = { Text("Payment Date") },
                    placeholder = { Text("Select a date") },
                    leadingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
                    isError = dateError != null,
                    supportingText = { if (dateError != null) Text(dateError!!, color = MaterialTheme.colorScheme.error) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = if (dateError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledSupportingTextColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { validateAndSubmit() },
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(if (isEditMode) "Update Fee Record" else "Add Fee Record", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        format.timeZone = TimeZone.getTimeZone("UTC")
                        paymentDate = format.format(java.util.Date(millis))
                        dateError = null
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun formatAmount(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

@Preview(showBackground = true, name = "Add Fee Record")
@Composable
private fun AddEditFeeScreenPreview() {
    TenderBudsTheme {
        AddEditFeeScreen(
            isEditMode = false,
            students = listOf(
                Student("2026001", "Aarav Sharma", "3", "Rakesh Sharma", "9876543210", "2026-04-01"),
                Student("2026002", "Priya Verma", "5", "Suresh Verma", "9876500001", "2026-04-02")
            )
        )
    }
}

@Preview(showBackground = true, name = "Edit Fee Record")
@Composable
private fun AddEditFeeScreenEditPreview() {
    TenderBudsTheme {
        AddEditFeeScreen(
            isEditMode = true,
            students = listOf(Student("2026001", "Aarav Sharma", "3", "Rakesh Sharma", "9876543210", "2026-04-01")),
            initialFee = Fee("1", "2026001", "Aarav Sharma", "Tuition", "August", 5000.0, 2000.0, 3000.0, "2026-08-05")
        )
    }
}
