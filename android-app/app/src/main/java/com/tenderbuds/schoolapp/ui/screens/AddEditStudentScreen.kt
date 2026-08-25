package com.tenderbuds.schoolapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tenderbuds.schoolapp.data.Student
import com.tenderbuds.schoolapp.ui.components.AuthTextField
import com.tenderbuds.schoolapp.ui.theme.BrandIndigo
import com.tenderbuds.schoolapp.ui.theme.TenderBudsTheme
import com.tenderbuds.schoolapp.ui.validation.Validators
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val CLASS_OPTIONS = listOf(
    "Play" to "Play",
    "Nursery" to "Nursery",
    "KG" to "KG",
    "1" to "Class 1",
    "2" to "Class 2",
    "3" to "Class 3",
    "4" to "Class 4",
    "5" to "Class 5",
    "6" to "Class 6",
    "7" to "Class 7",
    "8" to "Class 8",
    "9" to "Class 9",
    "10" to "Class 10"
)

/**
 * Screen 6 of 14 — Add/Edit Student.
 * The original web app used a modal for this; on mobile a full screen gives
 * each of the six fields room to show its own validation error clearly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditStudentScreen(
    isEditMode: Boolean,
    initialStudent: Student? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onSaveClick: (Student) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var regNo by rememberSaveable { mutableStateOf(initialStudent?.regNo ?: "") }
    var name by rememberSaveable { mutableStateOf(initialStudent?.name ?: "") }
    var studentClass by rememberSaveable { mutableStateOf(initialStudent?.studentClass ?: "") }
    var fatherName by rememberSaveable { mutableStateOf(initialStudent?.fatherName ?: "") }
    var phone by rememberSaveable { mutableStateOf(initialStudent?.phone ?: "") }
    var admissionDate by rememberSaveable { mutableStateOf(initialStudent?.admissionDate ?: "") }

    var regNoError by rememberSaveable { mutableStateOf<String?>(null) }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var classError by rememberSaveable { mutableStateOf<String?>(null) }
    var fatherNameError by rememberSaveable { mutableStateOf<String?>(null) }
    var phoneError by rememberSaveable { mutableStateOf<String?>(null) }
    var dateError by rememberSaveable { mutableStateOf<String?>(null) }

    var classMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    fun validateAndSubmit() {
        regNoError = Validators.registrationNumber(regNo)
        nameError = Validators.studentName(name)
        classError = Validators.studentClass(studentClass)
        fatherNameError = Validators.fatherName(fatherName)
        phoneError = Validators.phone(phone)
        dateError = Validators.admissionDate(admissionDate)

        val hasErrors = listOf(regNoError, nameError, classError, fatherNameError, phoneError, dateError).any { it != null }
        if (!hasErrors) {
            onSaveClick(
                Student(
                    regNo = regNo.trim(),
                    name = name.trim(),
                    studentClass = studentClass,
                    fatherName = fatherName.trim(),
                    phone = phone.trim(),
                    admissionDate = admissionDate
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Student" else "Add Student", fontWeight = FontWeight.SemiBold) },
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
            AuthTextField(
                value = regNo,
                onValueChange = { regNo = it; regNoError = null },
                label = "Registration Number",
                leadingIcon = Icons.Filled.Badge,
                errorMessage = regNoError
            )
            Spacer(modifier = Modifier.height(14.dp))

            AuthTextField(
                value = name,
                onValueChange = { name = it; nameError = null },
                label = "Student Name",
                leadingIcon = Icons.Filled.Person,
                errorMessage = nameError
            )
            Spacer(modifier = Modifier.height(14.dp))

            ExposedDropdownMenuBox(
                expanded = classMenuExpanded,
                onExpandedChange = { classMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = CLASS_OPTIONS.firstOrNull { it.first == studentClass }?.second ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Class") },
                    leadingIcon = { Icon(Icons.Filled.School, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classMenuExpanded) },
                    isError = classError != null,
                    supportingText = { if (classError != null) Text(classError!!, color = MaterialTheme.colorScheme.error) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = classMenuExpanded, onDismissRequest = { classMenuExpanded = false }) {
                    CLASS_OPTIONS.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                studentClass = value
                                classError = null
                                classMenuExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            AuthTextField(
                value = fatherName,
                onValueChange = { fatherName = it; fatherNameError = null },
                label = "Father's Name",
                leadingIcon = Icons.Filled.Person,
                errorMessage = fatherNameError
            )
            Spacer(modifier = Modifier.height(14.dp))

            AuthTextField(
                value = phone,
                onValueChange = { phone = it; phoneError = null },
                label = "Phone Number",
                leadingIcon = Icons.Filled.Phone,
                errorMessage = phoneError,
                keyboardType = KeyboardType.Phone
            )
            Spacer(modifier = Modifier.height(14.dp))

            Box {
                // A readOnly TextField still intercepts taps for its own
                // cursor/selection handling, so a clickable modifier on it
                // never fires reliably. Disabling the field removes that
                // interaction entirely, and a transparent Box on top catches
                // every tap instead — colors are overridden so "disabled"
                // still looks identical to the other enabled fields.
                OutlinedTextField(
                    value = admissionDate,
                    onValueChange = {},
                    enabled = false,
                    label = { Text("Admission Date") },
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
                    Text(if (isEditMode) "Update Student" else "Add Student", fontWeight = FontWeight.SemiBold)
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
                        admissionDate = format.format(java.util.Date(millis))
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

@Preview(showBackground = true, name = "Add Student")
@Composable
private fun AddEditStudentScreenPreview() {
    TenderBudsTheme {
        AddEditStudentScreen(isEditMode = false)
    }
}

@Preview(showBackground = true, name = "Edit Student")
@Composable
private fun AddEditStudentScreenEditPreview() {
    TenderBudsTheme {
        AddEditStudentScreen(
            isEditMode = true,
            initialStudent = Student("2026001", "Aarav Sharma", "3", "Rakesh Sharma", "9876543210", "2026-04-01")
        )
    }
}
