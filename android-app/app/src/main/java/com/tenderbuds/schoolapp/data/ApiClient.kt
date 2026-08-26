package com.tenderbuds.schoolapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class LoggedInUser(
    val token: String,
    val name: String,
    val username: String,
    val email: String
)

data class DashboardSummary(
    val totalStudents: Int,
    val totalCollected: Double,
    val totalDues: Double,
    val totalStock: Int,
    val outOfStockCount: Int
)

data class StudentFeeStatus(
    val regNo: String,
    val name: String,
    val studentClass: String,
    val totalFees: Double,
    val totalPaid: Double,
    val totalDue: Double,
    val status: String
)

data class ClassStockItem(
    val classLabel: String,
    val itemType: String,
    val itemName: String,
    val remainingStock: Int,
    val status: String
)

data class Student(
    val regNo: String,
    val name: String,
    val studentClass: String,
    val fatherName: String,
    val phone: String,
    val admissionDate: String
)

data class Fee(
    val id: String,
    val regNo: String,
    val studentName: String,
    val feeType: String,
    val month: String,
    val totalAmount: Double,
    val amountPaid: Double,
    val dueAmount: Double,
    val paymentDate: String
)

data class FeeSummary(
    val totalReceivable: Double,
    val totalReceived: Double,
    val totalPending: Double
)

data class Stock(
    val id: String,
    val itemType: String,
    val category: String,
    val subCategory: String,
    val gender: String,
    val studentClass: String,
    val size: String,
    val itemName: String,
    val totalQuantity: Int,
    val quantitySold: Int,
    val remainingStock: Int,
    val date: String
)

data class BackupImportSummary(
    val addedStudents: Int,
    val skippedStudents: Int,
    val addedFees: Int,
    val skippedFees: Int,
    val addedStock: Int,
    val skippedStock: Int
)

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val message: String) : ApiResult<Nothing>()
}

object ApiClient {
    // 10.0.2.2 is the Android emulator's alias for the host machine's
    // localhost — this is where the local docker-compose API is running.
    // A real device on the same network would use the host's LAN IP instead.
    private const val BASE_URL = "http://10.0.2.2:4000"

    suspend fun login(username: String, password: String): ApiResult<LoggedInUser> =
        withContext(Dispatchers.IO) {
            try {
                val connection = openConnection("/api/auth/login", "POST")
                writeJsonBody(connection, JSONObject().apply {
                    put("username", username)
                    put("password", password)
                })

                val status = connection.responseCode
                val body = readBody(connection, status)

                if (status == 200) {
                    val json = JSONObject(body)
                    val user = json.getJSONObject("user")
                    ApiResult.Success(
                        LoggedInUser(
                            token = json.getString("token"),
                            name = user.getString("name"),
                            username = user.getString("username"),
                            email = user.getString("email")
                        )
                    )
                } else {
                    ApiResult.Failure(extractErrorMessage(body, status))
                }
            } catch (e: IOException) {
                ApiResult.Failure("Could not reach the server. Is the API running at $BASE_URL?")
            } catch (e: Exception) {
                ApiResult.Failure("Unexpected error: ${e.message}")
            }
        }

    suspend fun signup(name: String, username: String, email: String, password: String): ApiResult<LoggedInUser> =
        withContext(Dispatchers.IO) {
            try {
                val connection = openConnection("/api/auth/signup", "POST")
                writeJsonBody(connection, JSONObject().apply {
                    put("name", name)
                    put("username", username)
                    put("email", email)
                    put("password", password)
                })

                val status = connection.responseCode
                val body = readBody(connection, status)

                if (status == 201) {
                    val json = JSONObject(body)
                    val user = json.getJSONObject("user")
                    ApiResult.Success(
                        LoggedInUser(
                            token = json.getString("token"),
                            name = user.getString("name"),
                            username = user.getString("username"),
                            email = user.getString("email")
                        )
                    )
                } else {
                    ApiResult.Failure(extractErrorMessage(body, status))
                }
            } catch (e: IOException) {
                ApiResult.Failure("Could not reach the server. Is the API running at $BASE_URL?")
            } catch (e: Exception) {
                ApiResult.Failure("Unexpected error: ${e.message}")
            }
        }

    /**
     * Always reports success on a well-formed request — the API never
     * reveals whether the username exists, to avoid account enumeration.
     */
    suspend fun resetPassword(username: String, newPassword: String): ApiResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val connection = openConnection("/api/auth/reset-password", "POST")
                writeJsonBody(connection, JSONObject().apply {
                    put("username", username)
                    put("newPassword", newPassword)
                })

                val status = connection.responseCode
                val body = readBody(connection, status)

                if (status == 202) {
                    val message = try {
                        JSONObject(body).getString("message")
                    } catch (e: Exception) {
                        "If that account exists, its password has been reset."
                    }
                    ApiResult.Success(message)
                } else {
                    ApiResult.Failure(extractErrorMessage(body, status))
                }
            } catch (e: IOException) {
                ApiResult.Failure("Could not reach the server. Is the API running at $BASE_URL?")
            } catch (e: Exception) {
                ApiResult.Failure("Unexpected error: ${e.message}")
            }
        }

    suspend fun getDashboardSummary(token: String): ApiResult<DashboardSummary> =
        withContext(Dispatchers.IO) {
            authGet("/api/dashboard/summary", token) { body ->
                val json = JSONObject(body)
                DashboardSummary(
                    totalStudents = json.getInt("totalStudents"),
                    totalCollected = json.getDouble("totalCollected"),
                    totalDues = json.getDouble("totalDues"),
                    totalStock = json.getInt("totalStock"),
                    outOfStockCount = json.getInt("outOfStockCount")
                )
            }
        }

    suspend fun getStudentFeeStatus(token: String): ApiResult<List<StudentFeeStatus>> =
        withContext(Dispatchers.IO) {
            authGet("/api/dashboard/student-fee-status", token) { body ->
                val array = JSONObject(body).getJSONArray("studentFeeStatus")
                (0 until array.length()).map { i ->
                    val item = array.getJSONObject(i)
                    StudentFeeStatus(
                        regNo = item.getString("regNo"),
                        name = item.getString("name"),
                        studentClass = item.getString("class"),
                        totalFees = item.getDouble("totalFees"),
                        totalPaid = item.getDouble("totalPaid"),
                        totalDue = item.getDouble("totalDue"),
                        status = item.getString("status")
                    )
                }
            }
        }

    suspend fun getClassStockAvailability(token: String, filter: String = ""): ApiResult<List<ClassStockItem>> =
        withContext(Dispatchers.IO) {
            val query = if (filter.isNotBlank()) "?filter=${java.net.URLEncoder.encode(filter, "UTF-8")}" else ""
            authGet("/api/stock/class-availability$query", token) { body ->
                val array = JSONObject(body).getJSONArray("classAvailability")
                (0 until array.length()).map { i ->
                    val item = array.getJSONObject(i)
                    ClassStockItem(
                        classLabel = item.getString("classLabel"),
                        itemType = item.getString("itemType"),
                        itemName = item.getString("itemName"),
                        remainingStock = item.getInt("remainingStock"),
                        status = item.getString("status")
                    )
                }
            }
        }

    suspend fun getStudents(token: String, search: String = ""): ApiResult<List<Student>> =
        withContext(Dispatchers.IO) {
            val query = if (search.isNotBlank()) "?search=${java.net.URLEncoder.encode(search, "UTF-8")}" else ""
            authGet("/api/students$query", token) { body ->
                val array = JSONObject(body).getJSONArray("students")
                (0 until array.length()).map { i -> parseStudent(array.getJSONObject(i)) }
            }
        }

    suspend fun getStudent(token: String, regNo: String): ApiResult<Student> =
        withContext(Dispatchers.IO) {
            val path = "/api/students/${java.net.URLEncoder.encode(regNo, "UTF-8")}"
            authGet(path, token) { body -> parseStudent(JSONObject(body).getJSONObject("student")) }
        }

    suspend fun createStudent(token: String, student: Student): ApiResult<Student> =
        withContext(Dispatchers.IO) {
            authWrite("/api/students", "POST", token, studentToJson(student), successStatus = 201) { body ->
                parseStudent(JSONObject(body).getJSONObject("student"))
            }
        }

    suspend fun updateStudent(token: String, originalRegNo: String, student: Student): ApiResult<Student> =
        withContext(Dispatchers.IO) {
            val path = "/api/students/${java.net.URLEncoder.encode(originalRegNo, "UTF-8")}"
            authWrite(path, "PUT", token, studentToJson(student), successStatus = 200) { body ->
                parseStudent(JSONObject(body).getJSONObject("student"))
            }
        }

    suspend fun deleteStudent(token: String, regNo: String): ApiResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val path = "/api/students/${java.net.URLEncoder.encode(regNo, "UTF-8")}"
                val connection = openConnection(path, "DELETE", token)
                val status = connection.responseCode
                if (status == 204) {
                    ApiResult.Success(Unit)
                } else {
                    val body = readBody(connection, status)
                    ApiResult.Failure(extractErrorMessage(body, status))
                }
            } catch (e: IOException) {
                ApiResult.Failure("Could not reach the server. Is the API running at $BASE_URL?")
            } catch (e: Exception) {
                ApiResult.Failure("Unexpected error: ${e.message}")
            }
        }

    suspend fun getFees(token: String, search: String = ""): ApiResult<List<Fee>> =
        withContext(Dispatchers.IO) {
            val query = if (search.isNotBlank()) "?search=${java.net.URLEncoder.encode(search, "UTF-8")}" else ""
            authGet("/api/fees$query", token) { body ->
                val array = JSONObject(body).getJSONArray("fees")
                (0 until array.length()).map { i -> parseFee(array.getJSONObject(i)) }
            }
        }

    suspend fun getFeeSummary(token: String): ApiResult<FeeSummary> =
        withContext(Dispatchers.IO) {
            authGet("/api/fees/summary", token) { body ->
                val json = JSONObject(body)
                FeeSummary(
                    totalReceivable = json.getDouble("totalReceivable"),
                    totalReceived = json.getDouble("totalReceived"),
                    totalPending = json.getDouble("totalPending")
                )
            }
        }

    suspend fun getFee(token: String, feeId: String): ApiResult<Fee> =
        withContext(Dispatchers.IO) {
            val path = "/api/fees/${java.net.URLEncoder.encode(feeId, "UTF-8")}"
            authGet(path, token) { body -> parseFee(JSONObject(body).getJSONObject("fee")) }
        }

    suspend fun createFee(
        token: String,
        regNo: String,
        feeType: String,
        month: String,
        totalAmount: Double,
        amountPaid: Double,
        paymentDate: String
    ): ApiResult<Fee> =
        withContext(Dispatchers.IO) {
            authWrite("/api/fees", "POST", token, feeToJson(regNo, feeType, month, totalAmount, amountPaid, paymentDate), successStatus = 201) { body ->
                parseFee(JSONObject(body).getJSONObject("fee"))
            }
        }

    suspend fun updateFee(
        token: String,
        feeId: String,
        regNo: String,
        feeType: String,
        month: String,
        totalAmount: Double,
        amountPaid: Double,
        paymentDate: String
    ): ApiResult<Fee> =
        withContext(Dispatchers.IO) {
            val path = "/api/fees/${java.net.URLEncoder.encode(feeId, "UTF-8")}"
            authWrite(path, "PUT", token, feeToJson(regNo, feeType, month, totalAmount, amountPaid, paymentDate), successStatus = 200) { body ->
                parseFee(JSONObject(body).getJSONObject("fee"))
            }
        }

    suspend fun deleteFee(token: String, feeId: String): ApiResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val path = "/api/fees/${java.net.URLEncoder.encode(feeId, "UTF-8")}"
                val connection = openConnection(path, "DELETE", token)
                val status = connection.responseCode
                if (status == 204) {
                    ApiResult.Success(Unit)
                } else {
                    val body = readBody(connection, status)
                    ApiResult.Failure(extractErrorMessage(body, status))
                }
            } catch (e: IOException) {
                ApiResult.Failure("Could not reach the server. Is the API running at $BASE_URL?")
            } catch (e: Exception) {
                ApiResult.Failure("Unexpected error: ${e.message}")
            }
        }

    suspend fun getStockList(token: String): ApiResult<List<Stock>> =
        withContext(Dispatchers.IO) {
            authGet("/api/stock", token) { body ->
                val array = JSONObject(body).getJSONArray("stock")
                (0 until array.length()).map { i -> parseStock(array.getJSONObject(i)) }
            }
        }

    suspend fun getStock(token: String, stockId: String): ApiResult<Stock> =
        withContext(Dispatchers.IO) {
            val path = "/api/stock/${java.net.URLEncoder.encode(stockId, "UTF-8")}"
            authGet(path, token) { body -> parseStock(JSONObject(body).getJSONObject("stock")) }
        }

    suspend fun createStock(token: String, stock: Stock): ApiResult<Stock> =
        withContext(Dispatchers.IO) {
            authWrite("/api/stock", "POST", token, stockToJson(stock), successStatus = 201) { body ->
                parseStock(JSONObject(body).getJSONObject("stock"))
            }
        }

    suspend fun updateStock(token: String, stockId: String, stock: Stock): ApiResult<Stock> =
        withContext(Dispatchers.IO) {
            val path = "/api/stock/${java.net.URLEncoder.encode(stockId, "UTF-8")}"
            authWrite(path, "PUT", token, stockToJson(stock), successStatus = 200) { body ->
                parseStock(JSONObject(body).getJSONObject("stock"))
            }
        }

    suspend fun deleteStock(token: String, stockId: String): ApiResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val path = "/api/stock/${java.net.URLEncoder.encode(stockId, "UTF-8")}"
                val connection = openConnection(path, "DELETE", token)
                val status = connection.responseCode
                if (status == 204) {
                    ApiResult.Success(Unit)
                } else {
                    val body = readBody(connection, status)
                    ApiResult.Failure(extractErrorMessage(body, status))
                }
            } catch (e: IOException) {
                ApiResult.Failure("Could not reach the server. Is the API running at $BASE_URL?")
            } catch (e: Exception) {
                ApiResult.Failure("Unexpected error: ${e.message}")
            }
        }

    private fun stockToJson(stock: Stock): JSONObject = JSONObject().apply {
        put("itemType", stock.itemType)
        put("category", stock.category)
        put("subCategory", stock.subCategory)
        put("gender", stock.gender)
        put("class", stock.studentClass)
        put("size", stock.size)
        put("itemName", stock.itemName)
        put("totalQuantity", stock.totalQuantity)
        put("quantitySold", stock.quantitySold)
        put("date", stock.date)
    }

    private fun parseStock(json: JSONObject): Stock = Stock(
        id = json.getString("id"),
        itemType = json.getString("itemType"),
        category = json.getString("category"),
        subCategory = json.optString("subCategory", ""),
        gender = json.optString("gender", ""),
        studentClass = json.optString("class", ""),
        size = json.optString("size", ""),
        itemName = json.getString("itemName"),
        totalQuantity = json.getInt("totalQuantity"),
        quantitySold = json.getInt("quantitySold"),
        remainingStock = json.getInt("remainingStock"),
        date = json.getString("date").take(10)
    )

    private fun feeToJson(
        regNo: String,
        feeType: String,
        month: String,
        totalAmount: Double,
        amountPaid: Double,
        paymentDate: String
    ): JSONObject = JSONObject().apply {
        put("regNo", regNo)
        put("feeType", feeType)
        put("month", month)
        put("totalAmount", totalAmount)
        put("amountPaid", amountPaid)
        put("paymentDate", paymentDate)
    }

    private fun parseFee(json: JSONObject): Fee = Fee(
        id = json.getString("id"),
        regNo = json.getString("regNo"),
        studentName = json.getString("studentName"),
        feeType = json.getString("feeType"),
        month = json.optString("month", ""),
        totalAmount = json.getDouble("totalAmount"),
        amountPaid = json.getDouble("amountPaid"),
        dueAmount = json.getDouble("dueAmount"),
        paymentDate = json.getString("paymentDate").take(10)
    )

    private fun studentToJson(student: Student): JSONObject = JSONObject().apply {
        put("regNo", student.regNo)
        put("name", student.name)
        put("class", student.studentClass)
        put("fatherName", student.fatherName)
        put("phone", student.phone)
        put("admissionDate", student.admissionDate)
    }

    private fun parseStudent(json: JSONObject): Student = Student(
        regNo = json.getString("regNo"),
        name = json.getString("name"),
        studentClass = json.getString("class"),
        fatherName = json.getString("fatherName"),
        phone = json.getString("phone"),
        admissionDate = json.getString("admissionDate").take(10)
    )

    /** Returns the raw backup JSON exactly as the API produced it, ready to write straight to a file. */
    suspend fun exportBackup(token: String): ApiResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val connection = openConnection("/api/backup/export", "GET", token)
                val status = connection.responseCode
                val body = readBody(connection, status)
                if (status == 200) {
                    ApiResult.Success(body)
                } else {
                    ApiResult.Failure(extractErrorMessage(body, status))
                }
            } catch (e: IOException) {
                ApiResult.Failure("Could not reach the server. Is the API running at $BASE_URL?")
            } catch (e: Exception) {
                ApiResult.Failure("Unexpected error: ${e.message}")
            }
        }

    /** Forwards a previously-exported backup file's raw JSON text as-is — the API merges it in. */
    suspend fun importBackup(token: String, rawBackupJson: String): ApiResult<BackupImportSummary> =
        withContext(Dispatchers.IO) {
            try {
                val connection = openConnection("/api/backup/import", "POST", token)
                connection.doOutput = true
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(rawBackupJson) }

                val status = connection.responseCode
                val body = readBody(connection, status)
                if (status == 200) {
                    val summary = JSONObject(body).getJSONObject("summary")
                    ApiResult.Success(
                        BackupImportSummary(
                            addedStudents = summary.getInt("addedStudents"),
                            skippedStudents = summary.getInt("skippedStudents"),
                            addedFees = summary.getInt("addedFees"),
                            skippedFees = summary.getInt("skippedFees"),
                            addedStock = summary.getInt("addedStock"),
                            skippedStock = summary.getInt("skippedStock")
                        )
                    )
                } else {
                    ApiResult.Failure(extractErrorMessage(body, status))
                }
            } catch (e: IOException) {
                ApiResult.Failure("Could not reach the server. Is the API running at $BASE_URL?")
            } catch (e: Exception) {
                ApiResult.Failure("Unexpected error: ${e.message}")
            }
        }

    private inline fun <T> authGet(path: String, token: String, parse: (String) -> T): ApiResult<T> {
        return try {
            val connection = openConnection(path, "GET", token)
            val status = connection.responseCode
            val body = readBody(connection, status)
            if (status == 200) {
                ApiResult.Success(parse(body))
            } else {
                ApiResult.Failure(extractErrorMessage(body, status))
            }
        } catch (e: IOException) {
            ApiResult.Failure("Could not reach the server. Is the API running at $BASE_URL?")
        } catch (e: Exception) {
            ApiResult.Failure("Unexpected error: ${e.message}")
        }
    }

    private inline fun <T> authWrite(
        path: String,
        method: String,
        token: String,
        json: JSONObject,
        successStatus: Int,
        parse: (String) -> T
    ): ApiResult<T> {
        return try {
            val connection = openConnection(path, method, token)
            writeJsonBody(connection, json)
            val status = connection.responseCode
            val body = readBody(connection, status)
            if (status == successStatus) {
                ApiResult.Success(parse(body))
            } else {
                ApiResult.Failure(extractErrorMessage(body, status))
            }
        } catch (e: IOException) {
            ApiResult.Failure("Could not reach the server. Is the API running at $BASE_URL?")
        } catch (e: Exception) {
            ApiResult.Failure("Unexpected error: ${e.message}")
        }
    }

    private fun openConnection(path: String, method: String, token: String? = null): HttpURLConnection {
        val url = URL(BASE_URL + path)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        if (token != null) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
        return connection
    }

    private fun writeJsonBody(connection: HttpURLConnection, json: JSONObject) {
        connection.doOutput = true
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(json.toString())
            writer.flush()
        }
    }

    private fun readBody(connection: HttpURLConnection, status: Int): String {
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }

    private fun extractErrorMessage(body: String, status: Int): String {
        return try {
            JSONObject(body).getJSONObject("error").getString("message")
        } catch (e: Exception) {
            "Request failed (HTTP $status)"
        }
    }
}
