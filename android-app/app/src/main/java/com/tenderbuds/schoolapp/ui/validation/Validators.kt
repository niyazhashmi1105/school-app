package com.tenderbuds.schoolapp.ui.validation

/**
 * Field-level validation, mirroring the API's own rules (see api/src/routes/auth.ts)
 * so a form never submits something the server would reject anyway.
 * Each function returns null when the value is valid, or a user-facing
 * error message when it isn't.
 */
object Validators {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun fullName(value: String): String? =
        if (value.trim().isEmpty()) "Full name is required" else null

    fun username(value: String): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> "Username is required"
            trimmed.length < 3 -> "Username must be at least 3 characters"
            trimmed.contains(" ") -> "Username cannot contain spaces"
            else -> null
        }
    }

    fun email(value: String): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> "Email is required"
            !EMAIL_REGEX.matches(trimmed) -> "Enter a valid email address"
            else -> null
        }
    }

    fun password(value: String): String? = when {
        value.isEmpty() -> "Password is required"
        value.length < 6 -> "Password must be at least 6 characters"
        else -> null
    }

    fun confirmPassword(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Please confirm your password"
        confirm != password -> "Passwords do not match"
        else -> null
    }

    fun registrationNumber(value: String): String? =
        if (value.trim().isEmpty()) "Registration number is required" else null

    fun studentName(value: String): String? =
        if (value.trim().isEmpty()) "Student name is required" else null

    fun studentClass(value: String): String? =
        if (value.isEmpty()) "Please select a class" else null

    fun fatherName(value: String): String? =
        if (value.trim().isEmpty()) "Father's name is required" else null

    fun phone(value: String): String? {
        val digitsOnly = value.filter { it.isDigit() }
        return when {
            value.trim().isEmpty() -> "Phone number is required"
            digitsOnly.length < 10 -> "Enter a valid 10-digit phone number"
            else -> null
        }
    }

    fun admissionDate(value: String): String? =
        if (value.isEmpty()) "Admission date is required" else null

    fun feeStudent(value: String): String? =
        if (value.trim().isEmpty()) "Please select a student" else null

    fun feeType(value: String): String? =
        if (value.isEmpty()) "Please select a fee type" else null

    fun feeAmount(value: String, fieldLabel: String): String? {
        val trimmed = value.trim()
        val amount = trimmed.toDoubleOrNull()
        return when {
            trimmed.isEmpty() -> "$fieldLabel is required"
            amount == null -> "Enter a valid amount"
            amount < 0 -> "$fieldLabel cannot be negative"
            else -> null
        }
    }

    /** Mirrors the API's own check (see api/src/routes/fees.ts) so the form never submits a value the server would reject. */
    fun amountPaidNotExceeding(totalAmount: String, amountPaid: String): String? {
        val total = totalAmount.trim().toDoubleOrNull() ?: return null
        val paid = amountPaid.trim().toDoubleOrNull() ?: return null
        return if (paid > total) "Amount paid cannot exceed total amount" else null
    }

    fun paymentDate(value: String): String? =
        if (value.isEmpty()) "Payment date is required" else null

    fun stockItemType(value: String): String? =
        if (value.isEmpty()) "Please select an item type" else null

    fun stockBookCategory(value: String): String? =
        if (value.isEmpty()) "Please select a book category" else null

    fun stockNotebookSubject(value: String): String? =
        if (value.isEmpty()) "Please select a notebook subject" else null

    fun stockBookClass(value: String): String? =
        if (value.isEmpty()) "Please select a class" else null

    fun stockUniformType(value: String): String? =
        if (value.isEmpty()) "Please select a uniform type" else null

    fun stockUniformGender(value: String): String? =
        if (value.isEmpty()) "Please select a gender" else null

    fun stockUniformPiece(value: String): String? =
        if (value.isEmpty()) "Please select an item piece" else null

    fun stockUniformSize(value: String): String? =
        if (value.trim().isEmpty()) "Please enter a size" else null

    fun stockQuantity(value: String, fieldLabel: String): String? {
        val trimmed = value.trim()
        val quantity = trimmed.toIntOrNull()
        return when {
            trimmed.isEmpty() -> "$fieldLabel is required"
            quantity == null -> "Enter a valid whole number"
            quantity < 0 -> "$fieldLabel cannot be negative"
            else -> null
        }
    }

    /** Mirrors the API's own check (see api/src/routes/stock.ts) so the form never submits a value the server would reject. */
    fun quantitySoldNotExceeding(totalQuantity: String, quantitySold: String): String? {
        val total = totalQuantity.trim().toIntOrNull() ?: return null
        val sold = quantitySold.trim().toIntOrNull() ?: return null
        return if (sold > total) "Quantity sold cannot exceed total quantity" else null
    }

    fun stockDate(value: String): String? =
        if (value.isEmpty()) "Date is required" else null
}
