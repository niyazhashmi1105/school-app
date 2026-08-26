package com.tenderbuds.schoolapp.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Wc
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
import com.tenderbuds.schoolapp.data.Stock
import com.tenderbuds.schoolapp.ui.components.AuthTextField
import com.tenderbuds.schoolapp.ui.theme.BrandIndigo
import com.tenderbuds.schoolapp.ui.theme.DangerRed
import com.tenderbuds.schoolapp.ui.theme.SuccessGreen
import com.tenderbuds.schoolapp.ui.theme.TenderBudsTheme
import com.tenderbuds.schoolapp.ui.theme.WarningAmber
import com.tenderbuds.schoolapp.ui.validation.Validators
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val CLASS_OPTIONS = listOf(
    "Play" to "Play", "Nursery" to "Nursery", "KG" to "KG",
    "1" to "Class 1", "2" to "Class 2", "3" to "Class 3", "4" to "Class 4",
    "5" to "Class 5", "6" to "Class 6", "7" to "Class 7", "8" to "Class 8",
    "9" to "Class 9", "10" to "Class 10"
)

private val BOOK_CATEGORY_OPTIONS = listOf(
    "English Book", "Hindi Book", "Maths Book", "EVS Book", "Computer Book", "Notebook"
)

private val NOTEBOOK_SUBJECT_OPTIONS = listOf(
    "English Notebook", "Hindi Notebook", "Maths Notebook", "Drawing Notebook", "3-in-1 Notebook"
)

private val UNIFORM_TYPE_OPTIONS = listOf("Summer Uniform", "Winter Uniform")

private val WINTER_SIZE_OPTIONS = listOf("10", "12", "14", "16", "18", "20", "22", "24", "26", "28", "30", "32", "34", "36", "38", "40")

private fun piecesForGender(gender: String): List<String> = when (gender) {
    "Boys" -> listOf("Pant", "Shirt")
    "Girls" -> listOf("Skirt", "Shirt")
    else -> emptyList()
}

/** Everything needed to build the exact item key + name, mirroring the original web app's buildBookItemKey/buildDressItemKey. */
private data class StockItemKey(
    val itemType: String,
    val category: String,
    val subCategory: String,
    val gender: String,
    val studentClass: String,
    val size: String,
    val itemName: String
)

private fun matches(a: Stock, key: StockItemKey): Boolean =
    a.itemType == key.itemType && a.category == key.category && a.subCategory == key.subCategory &&
        a.gender == key.gender && a.studentClass == key.studentClass && a.size == key.size

/**
 * Screen — Add/Edit Stock Item.
 * Rebuilds the original web app's Add/Edit Stock Item modal: the cascading
 * Book (category → notebook subject → class) and Dress (uniform type →
 * gender → piece → size, or straight to size for Winter Uniform) flows are
 * unchanged, including the "same item already exists → this is a restock"
 * preview the original form shows before saving.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditStockScreen(
    isEditMode: Boolean,
    existingStock: List<Stock> = emptyList(),
    initialStock: Stock? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onSaveClick: (Stock) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var itemType by rememberSaveable { mutableStateOf(initialStock?.itemType ?: "") }

    var bookCategory by rememberSaveable {
        mutableStateOf(if (initialStock?.itemType == "Book") initialStock.category else "")
    }
    var notebookSubject by rememberSaveable {
        mutableStateOf(if (initialStock?.itemType == "Book" && initialStock.category == "Notebook") initialStock.subCategory else "")
    }
    var bookClass by rememberSaveable {
        mutableStateOf(if (initialStock?.itemType == "Book") initialStock.studentClass else "")
    }

    var uniformType by rememberSaveable {
        mutableStateOf(if (initialStock?.itemType == "Dress") initialStock.category else "")
    }
    var uniformGender by rememberSaveable {
        mutableStateOf(if (initialStock?.itemType == "Dress" && initialStock.category != "Winter Uniform") initialStock.gender else "")
    }
    var uniformPiece by rememberSaveable {
        mutableStateOf(if (initialStock?.itemType == "Dress" && initialStock.category != "Winter Uniform") initialStock.subCategory else "")
    }
    var uniformSizeWinter by rememberSaveable {
        mutableStateOf(if (initialStock?.itemType == "Dress" && initialStock.category == "Winter Uniform") initialStock.size else "")
    }
    var uniformSizeSummer by rememberSaveable {
        mutableStateOf(if (initialStock?.itemType == "Dress" && initialStock.category != "Winter Uniform") initialStock.size else "")
    }

    var totalQuantity by rememberSaveable { mutableStateOf(initialStock?.totalQuantity?.toString() ?: "") }
    var quantitySold by rememberSaveable { mutableStateOf(initialStock?.quantitySold?.toString() ?: "") }
    var date by rememberSaveable { mutableStateOf(initialStock?.date ?: "") }

    var itemTypeError by rememberSaveable { mutableStateOf<String?>(null) }
    var bookCategoryError by rememberSaveable { mutableStateOf<String?>(null) }
    var notebookSubjectError by rememberSaveable { mutableStateOf<String?>(null) }
    var bookClassError by rememberSaveable { mutableStateOf<String?>(null) }
    var uniformTypeError by rememberSaveable { mutableStateOf<String?>(null) }
    var uniformGenderError by rememberSaveable { mutableStateOf<String?>(null) }
    var uniformPieceError by rememberSaveable { mutableStateOf<String?>(null) }
    var uniformSizeError by rememberSaveable { mutableStateOf<String?>(null) }
    var totalQuantityError by rememberSaveable { mutableStateOf<String?>(null) }
    var quantitySoldError by rememberSaveable { mutableStateOf<String?>(null) }
    var dateError by rememberSaveable { mutableStateOf<String?>(null) }

    var itemTypeMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var bookCategoryMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var notebookSubjectMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var bookClassMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var uniformTypeMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var uniformGenderMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var uniformPieceMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var uniformSizeWinterMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val isBook = itemType == "Book"
    val isDress = itemType == "Dress"
    val isWinter = uniformType == "Winter Uniform"
    val isNotebook = bookCategory == "Notebook"

    val itemKey = remember(itemType, bookCategory, notebookSubject, bookClass, uniformType, uniformGender, uniformPiece, uniformSizeWinter, uniformSizeSummer) {
        when {
            isBook -> {
                if (bookCategory.isEmpty() || bookClass.isEmpty()) return@remember null
                val classLabel = CLASS_OPTIONS.firstOrNull { it.first == bookClass }?.second ?: bookClass
                if (isNotebook) {
                    if (notebookSubject.isEmpty()) return@remember null
                    StockItemKey("Book", "Notebook", notebookSubject, "", bookClass, "", "$classLabel - $notebookSubject")
                } else {
                    StockItemKey("Book", bookCategory, "", "", bookClass, "", "$classLabel - $bookCategory")
                }
            }
            isDress -> {
                if (uniformType.isEmpty()) return@remember null
                if (isWinter) {
                    if (uniformSizeWinter.isEmpty()) return@remember null
                    StockItemKey("Dress", uniformType, "", "", "", uniformSizeWinter, "$uniformType - Size $uniformSizeWinter")
                } else {
                    val size = uniformSizeSummer.trim()
                    if (uniformGender.isEmpty() || uniformPiece.isEmpty() || size.isEmpty()) return@remember null
                    StockItemKey("Dress", uniformType, uniformPiece, uniformGender, "", size, "$uniformGender $uniformType ($uniformPiece) - Size $size")
                }
            }
            else -> null
        }
    }

    val matchingStock = remember(itemKey, existingStock, initialStock) {
        itemKey?.let { key -> existingStock.firstOrNull { it.id != initialStock?.id && matches(it, key) } }
    }

    fun validateAndSubmit() {
        itemTypeError = Validators.stockItemType(itemType)
        bookCategoryError = null; notebookSubjectError = null; bookClassError = null
        uniformTypeError = null; uniformGenderError = null; uniformPieceError = null; uniformSizeError = null

        if (isBook) {
            bookCategoryError = Validators.stockBookCategory(bookCategory)
            if (isNotebook) notebookSubjectError = Validators.stockNotebookSubject(notebookSubject)
            bookClassError = Validators.stockBookClass(bookClass)
        } else if (isDress) {
            uniformTypeError = Validators.stockUniformType(uniformType)
            if (isWinter) {
                uniformSizeError = Validators.stockUniformSize(uniformSizeWinter)
            } else {
                uniformGenderError = Validators.stockUniformGender(uniformGender)
                uniformPieceError = Validators.stockUniformPiece(uniformPiece)
                uniformSizeError = Validators.stockUniformSize(uniformSizeSummer)
            }
        }

        totalQuantityError = Validators.stockQuantity(totalQuantity, "Total quantity")
        quantitySoldError = Validators.stockQuantity(quantitySold, "Quantity sold")
            ?: Validators.quantitySoldNotExceeding(totalQuantity, quantitySold)
        dateError = Validators.stockDate(date)

        val hasErrors = listOf(
            itemTypeError, bookCategoryError, notebookSubjectError, bookClassError,
            uniformTypeError, uniformGenderError, uniformPieceError, uniformSizeError,
            totalQuantityError, quantitySoldError, dateError
        ).any { it != null }

        val key = itemKey
        if (!hasErrors && key != null) {
            val total = totalQuantity.trim().toInt()
            val sold = quantitySold.trim().toInt()
            onSaveClick(
                Stock(
                    id = initialStock?.id ?: "",
                    itemType = key.itemType,
                    category = key.category,
                    subCategory = key.subCategory,
                    gender = key.gender,
                    studentClass = key.studentClass,
                    size = key.size,
                    itemName = key.itemName,
                    totalQuantity = total,
                    quantitySold = sold,
                    remainingStock = maxOf(0, total - sold),
                    date = date
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Stock Item" else "Add Stock Item", fontWeight = FontWeight.SemiBold) },
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
                expanded = itemTypeMenuExpanded,
                onExpandedChange = { itemTypeMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = itemType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Item Type") },
                    placeholder = { Text("Select Type") },
                    leadingIcon = { Icon(Icons.Filled.Inventory2, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemTypeMenuExpanded) },
                    isError = itemTypeError != null,
                    supportingText = { if (itemTypeError != null) Text(itemTypeError!!, color = MaterialTheme.colorScheme.error) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = itemTypeMenuExpanded, onDismissRequest = { itemTypeMenuExpanded = false }) {
                    listOf("Book", "Dress").forEach { value ->
                        DropdownMenuItem(
                            text = { Text(value) },
                            onClick = {
                                itemType = value
                                bookCategory = ""; notebookSubject = ""; bookClass = ""
                                uniformType = ""; uniformGender = ""; uniformPiece = ""
                                uniformSizeWinter = ""; uniformSizeSummer = ""
                                itemTypeError = null
                                itemTypeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            if (isBook) {
                Spacer(modifier = Modifier.height(14.dp))
                ExposedDropdownMenuBox(
                    expanded = bookCategoryMenuExpanded,
                    onExpandedChange = { bookCategoryMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = bookCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Book Category") },
                        placeholder = { Text("Select Category") },
                        leadingIcon = { Icon(Icons.Filled.MenuBook, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bookCategoryMenuExpanded) },
                        isError = bookCategoryError != null,
                        supportingText = { if (bookCategoryError != null) Text(bookCategoryError!!, color = MaterialTheme.colorScheme.error) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = bookCategoryMenuExpanded, onDismissRequest = { bookCategoryMenuExpanded = false }) {
                        BOOK_CATEGORY_OPTIONS.forEach { value ->
                            DropdownMenuItem(
                                text = { Text(value) },
                                onClick = {
                                    bookCategory = value
                                    notebookSubject = ""
                                    bookCategoryError = null
                                    bookCategoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (isNotebook) {
                    Spacer(modifier = Modifier.height(14.dp))
                    ExposedDropdownMenuBox(
                        expanded = notebookSubjectMenuExpanded,
                        onExpandedChange = { notebookSubjectMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = notebookSubject,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Notebook Subject") },
                            placeholder = { Text("Select Notebook Subject") },
                            leadingIcon = { Icon(Icons.Filled.MenuBook, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = notebookSubjectMenuExpanded) },
                            isError = notebookSubjectError != null,
                            supportingText = { if (notebookSubjectError != null) Text(notebookSubjectError!!, color = MaterialTheme.colorScheme.error) },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = notebookSubjectMenuExpanded, onDismissRequest = { notebookSubjectMenuExpanded = false }) {
                            NOTEBOOK_SUBJECT_OPTIONS.forEach { value ->
                                DropdownMenuItem(
                                    text = { Text(value) },
                                    onClick = {
                                        notebookSubject = value
                                        notebookSubjectError = null
                                        notebookSubjectMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                ExposedDropdownMenuBox(
                    expanded = bookClassMenuExpanded,
                    onExpandedChange = { bookClassMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = CLASS_OPTIONS.firstOrNull { it.first == bookClass }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Class") },
                        placeholder = { Text("Select Class") },
                        leadingIcon = { Icon(Icons.Filled.School, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bookClassMenuExpanded) },
                        isError = bookClassError != null,
                        supportingText = { if (bookClassError != null) Text(bookClassError!!, color = MaterialTheme.colorScheme.error) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = bookClassMenuExpanded, onDismissRequest = { bookClassMenuExpanded = false }) {
                        CLASS_OPTIONS.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    bookClass = value
                                    bookClassError = null
                                    bookClassMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (isDress) {
                Spacer(modifier = Modifier.height(14.dp))
                ExposedDropdownMenuBox(
                    expanded = uniformTypeMenuExpanded,
                    onExpandedChange = { uniformTypeMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uniformType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Uniform Type") },
                        placeholder = { Text("Select Uniform Type") },
                        leadingIcon = { Icon(Icons.Filled.Checkroom, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uniformTypeMenuExpanded) },
                        isError = uniformTypeError != null,
                        supportingText = { if (uniformTypeError != null) Text(uniformTypeError!!, color = MaterialTheme.colorScheme.error) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = uniformTypeMenuExpanded, onDismissRequest = { uniformTypeMenuExpanded = false }) {
                        UNIFORM_TYPE_OPTIONS.forEach { value ->
                            DropdownMenuItem(
                                text = { Text(value) },
                                onClick = {
                                    uniformType = value
                                    uniformGender = ""; uniformPiece = ""
                                    uniformSizeWinter = ""; uniformSizeSummer = ""
                                    uniformTypeError = null
                                    uniformTypeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (isWinter) {
                    // Winter Uniform: no Gender, no Item Piece field — Size alone is enough.
                    Spacer(modifier = Modifier.height(14.dp))
                    ExposedDropdownMenuBox(
                        expanded = uniformSizeWinterMenuExpanded,
                        onExpandedChange = { uniformSizeWinterMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = uniformSizeWinter,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Size") },
                            placeholder = { Text("Select Size") },
                            leadingIcon = { Icon(Icons.Filled.Straighten, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uniformSizeWinterMenuExpanded) },
                            isError = uniformSizeError != null,
                            supportingText = { if (uniformSizeError != null) Text(uniformSizeError!!, color = MaterialTheme.colorScheme.error) },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = uniformSizeWinterMenuExpanded, onDismissRequest = { uniformSizeWinterMenuExpanded = false }) {
                            WINTER_SIZE_OPTIONS.forEach { value ->
                                DropdownMenuItem(
                                    text = { Text(value) },
                                    onClick = {
                                        uniformSizeWinter = value
                                        uniformSizeError = null
                                        uniformSizeWinterMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else if (uniformType.isNotEmpty()) {
                    // Summer Uniform: Gender first, then Piece (options depend on Gender), then a free-text Size.
                    Spacer(modifier = Modifier.height(14.dp))
                    ExposedDropdownMenuBox(
                        expanded = uniformGenderMenuExpanded,
                        onExpandedChange = { uniformGenderMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = uniformGender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender") },
                            placeholder = { Text("Select Gender") },
                            leadingIcon = { Icon(Icons.Filled.Wc, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uniformGenderMenuExpanded) },
                            isError = uniformGenderError != null,
                            supportingText = { if (uniformGenderError != null) Text(uniformGenderError!!, color = MaterialTheme.colorScheme.error) },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = uniformGenderMenuExpanded, onDismissRequest = { uniformGenderMenuExpanded = false }) {
                            listOf("Boys", "Girls").forEach { value ->
                                DropdownMenuItem(
                                    text = { Text(value) },
                                    onClick = {
                                        uniformGender = value
                                        uniformPiece = ""; uniformSizeSummer = ""
                                        uniformGenderError = null
                                        uniformGenderMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (uniformGender.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        ExposedDropdownMenuBox(
                            expanded = uniformPieceMenuExpanded,
                            onExpandedChange = { uniformPieceMenuExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = uniformPiece,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Item Piece") },
                                placeholder = { Text("Select Piece") },
                                leadingIcon = { Icon(Icons.Filled.Checkroom, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uniformPieceMenuExpanded) },
                                isError = uniformPieceError != null,
                                supportingText = { if (uniformPieceError != null) Text(uniformPieceError!!, color = MaterialTheme.colorScheme.error) },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = uniformPieceMenuExpanded, onDismissRequest = { uniformPieceMenuExpanded = false }) {
                                piecesForGender(uniformGender).forEach { value ->
                                    DropdownMenuItem(
                                        text = { Text(value) },
                                        onClick = {
                                            uniformPiece = value
                                            uniformSizeSummer = ""
                                            uniformPieceError = null
                                            uniformPieceMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (uniformPiece.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        AuthTextField(
                            value = uniformSizeSummer,
                            onValueChange = { uniformSizeSummer = it; uniformSizeError = null },
                            label = "Size",
                            leadingIcon = Icons.Filled.Straighten,
                            errorMessage = uniformSizeError
                        )
                    }
                }
            }

            val key = itemKey
            if (key != null) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = key.itemName,
                    onValueChange = {},
                    enabled = false,
                    label = { Text("Selected Item") },
                    leadingIcon = { Icon(Icons.Filled.Inventory2, contentDescription = null) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                StockStatusBox(matchingStock = matchingStock, isEditMode = isEditMode)
            }

            Spacer(modifier = Modifier.height(14.dp))

            AuthTextField(
                value = totalQuantity,
                onValueChange = { totalQuantity = it; totalQuantityError = null; quantitySoldError = null },
                label = "Total Quantity",
                leadingIcon = Icons.Filled.Numbers,
                errorMessage = totalQuantityError,
                keyboardType = KeyboardType.Number
            )
            Spacer(modifier = Modifier.height(14.dp))

            AuthTextField(
                value = quantitySold,
                onValueChange = { quantitySold = it; quantitySoldError = null },
                label = "Quantity Sold",
                leadingIcon = Icons.Filled.Numbers,
                errorMessage = quantitySoldError,
                keyboardType = KeyboardType.Number
            )
            Spacer(modifier = Modifier.height(14.dp))

            val remaining = totalQuantity.trim().toIntOrNull()?.let { total ->
                quantitySold.trim().toIntOrNull()?.let { sold -> maxOf(0, total - sold) }
            }
            OutlinedTextField(
                value = remaining?.toString() ?: "",
                onValueChange = {},
                enabled = false,
                label = { Text("Remaining Stock") },
                placeholder = { Text("Auto-calculated") },
                leadingIcon = { Icon(Icons.Filled.Numbers, contentDescription = null) },
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
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    enabled = false,
                    label = { Text("Date") },
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
                    Text(if (isEditMode) "Update Stock" else "Add Stock Item", fontWeight = FontWeight.SemiBold)
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
                        date = format.format(java.util.Date(millis))
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

@Composable
private fun StockStatusBox(matchingStock: Stock?, isEditMode: Boolean) {
    val (text, color) = if (matchingStock != null) {
        val (statusLabel, statusColor) = stockStatusLabel(matchingStock.remainingStock)
        val suffix = if (!isEditMode) " The quantities you enter below will be added to this record." else ""
        "Existing record — Total: ${matchingStock.totalQuantity}, Sold: ${matchingStock.quantitySold}, Remaining: ${matchingStock.remainingStock} ($statusLabel).$suffix" to statusColor
    } else {
        "No existing stock for this item yet — a new record will be created." to null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun stockStatusLabel(remainingStock: Int): Pair<String, Color> = when {
    remainingStock <= 0 -> "Out of Stock" to DangerRed
    remainingStock < 10 -> "Low Stock" to WarningAmber
    else -> "In Stock" to SuccessGreen
}

@Preview(showBackground = true, name = "Add Stock — Book")
@Composable
private fun AddEditStockScreenBookPreview() {
    TenderBudsTheme {
        AddEditStockScreen(isEditMode = false)
    }
}

@Preview(showBackground = true, name = "Edit Stock — Dress")
@Composable
private fun AddEditStockScreenDressPreview() {
    TenderBudsTheme {
        AddEditStockScreen(
            isEditMode = true,
            initialStock = Stock("1", "Dress", "Summer Uniform", "Pant", "Boys", "", "30", "Boys Summer Uniform (Pant) - Size 30", 20, 20, 0, "2026-05-10")
        )
    }
}
