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
}
