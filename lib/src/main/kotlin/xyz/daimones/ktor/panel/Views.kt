package xyz.daimones.ktor.panel

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.mustache.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Id
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.JavaLocalDateColumnType
import org.jetbrains.exposed.sql.javatime.JavaLocalDateTimeColumnType
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import xyz.daimones.ktor.panel.database.*
import xyz.daimones.ktor.panel.database.dao.ExposedDao
import xyz.daimones.ktor.panel.database.dao.JpaDao
import xyz.daimones.ktor.panel.database.dao.MongoDao
import xyz.daimones.ktor.panel.database.entities.AdminUsers
import xyz.daimones.ktor.panel.database.entities.JpaAdminUser
import xyz.daimones.ktor.panel.database.entities.MongoAdminUser
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField
import org.jetbrains.exposed.sql.Column as ExposedColumn

/**
 * Manages base view rendering for admin panel pages.
 *
 * This class is responsible for setting up routes and rendering templates for the admin panel. It
 * provides functionality for index, list, create, details, and update views.
 *
 * @param entityKClass The KClass of the database entity class to generate admin views for
 */
open class BaseView<T : Any>(private val entityKClass: KClass<T>) {
    /**
     * Configuration for the admin panel. This is set during [EntityView.configurePageViews] and
     * contains settings like URL, endpoint, and admin name.
     */
    protected var configuration: Configuration? = null

    /**
     * The application instance for setting up routes. Set during [EntityView.configurePageViews] and
     * used by the expose* methods.
     */
    protected var application: Application? = null

    /**
     * The database instance for data access. This is set during [EntityView.configurePageViews] and
     * used by the ExposedDao or MongoDao for database operations.
     */
    protected var database: Any? = null

    /**
     * The data access object interface for database operations. This is set during
     * [EntityView.configurePageViews] and used to interact with the database.
     */
    protected var dao: DataAccessObjectInterface<T>? = null

    /** ORM/ODM type used for the entity class. */
    protected lateinit var driverType: DriverType

    /** Default Mustache template for the index page. */
    private val defaultIndexTemplate = "kt-panel-index.hbs"

    /** Default Mustache template for the list page. */
    private val defaultListTemplate = "kt-panel-list.hbs"

    /** Default Mustache template for the create page. */
    private val defaultCreateTemplate = "kt-panel-create.hbs"

    /** Default Mustache template for the details page. */
    private val defaultDetailsTemplate = "kt-panel-details.hbs"

    /** Default Mustache template for the login page. */
    private val defaultLoginTemplate = "kt-panel-login.hbs"

    /** Default Mustache template for the logout page. */
    private val defaultLogoutTemplate = "kt-panel-logout.hbs"

    /** Default Mustache template for the delete confirmation page. */
    private val defaultDeleteTemplate = "kt-panel-delete.hbs"

    /**
     * List of headers for the entity's columns. This is used to render table headers in the list
     * view.
     */
    lateinit var headers: List<String>

    /**
     * Success message to be displayed after creating or updating an instance. This is set during
     * the create or update operations and can be used in templates.
     */
    private var successMessage: String? = null

    /**
     * Initialises the headers for the entity's columns.
     *
     * This method sets up the headers based on the entity type, either from Exposed Entity class or
     * JPA Entity annotations. It is called during the initialisation of the BaseView.
     *
     * @param configuration The configuration settings for the admin panel
     */
    fun setHeaders(configuration: Configuration): List<String> {
        fun <T> reorderProperty(
            properties: MutableList<KProperty1<T, *>>, idProperty: KProperty1<T, *>?,
        ) {
            if (idProperty != null) {
                properties.remove(idProperty)
                properties.add(0, idProperty)
            }
        }

        fun filterProperties(
            exposedProperties: MutableList<ExposedColumn<*>>? = null,
            otherProperties: MutableList<KProperty1<T, *>>? = null,
            primaryKeyProperty: KProperty1<T, *>? = null,
        ): List<String> {
            if (configuration.listFields != null && configuration.listFields.isNotEmpty() && exposedProperties != null) {
                val selectedProperties =
                    exposedProperties.filter { it -> configuration.listFields.contains(snakeToCamel(it.name)) || it.name == "id" || it.name == primaryKeyProperty?.name }
                return selectedProperties.map { it.name }
            } else if (configuration.listFields == null && exposedProperties != null) {
                return exposedProperties.map { it.name }
            } else if (configuration.listFields != null && configuration.listFields.isNotEmpty() && otherProperties != null) {
                val selectedProperties =
                    otherProperties.filter { it -> configuration.listFields.contains(it.name) || it.name == "id" || it.annotations.any { it is PrimaryKeyField } }
                return selectedProperties.map { it.name }
            } else if (configuration.listFields == null && otherProperties != null) {
                return otherProperties.map { it.name }
            } else if (exposedProperties != null) {
                return exposedProperties.map { it.name }
            } else {
                return otherProperties!!.map { it.name }
            }
        }

        return if (driverType == DriverType.EXPOSED) {
            val properties = (entityKClass.objectInstance as Table).columns.toMutableList()
            val primaryKeyProperty =
                entityKClass.declaredMemberProperties.find { p -> p.annotations.any { it is PrimaryKeyField } }
            filterProperties(properties, primaryKeyProperty = primaryKeyProperty)
        } else if (driverType == DriverType.JPA) {
            val properties = entityKClass.memberProperties.sortedBy { it.name }.toMutableList()
            val idProperty = properties.find { p -> p.javaField?.isAnnotationPresent(Id::class.java) == true }
            reorderProperty(properties, idProperty)
            filterProperties(otherProperties = properties)
        } else if (driverType == DriverType.MONGO) {
            val properties = entityKClass.memberProperties.sortedBy { it.name }.toMutableList()
            val idProperty =
                properties.find { p -> p.javaField?.name == "id" || p.annotations.any { it is PrimaryKeyField } }
            reorderProperty(properties, idProperty)
            filterProperties(otherProperties = properties)
        } else {
            throw IllegalArgumentException("Model must be an IntEntityClass or annotated with @Entity")
        }
    }

    /**
     * Retrieves the column types of the entity for rendering in templates.
     *
     * This method maps each column in the entity to its HTML input type and original type, which is
     * useful for generating forms and input fields dynamically.
     *
     * @return A list of maps containing column names, HTML input types, and original types
     * @throws IllegalArgumentException if the entity is not an IntEntity or does not have the @Entity annotation
     */
    private fun getColumnTypes(): List<Map<String, Any?>> {
        return if (driverType == DriverType.EXPOSED) {
            (entityKClass.objectInstance as Table).columns
                .mapNotNull { column ->
                    if (column.name == "id") return@mapNotNull null // Skip primary key field

                    val camelCaseName = snakeToCamel(column.name)
                    val properties = entityKClass.declaredMemberProperties.toMutableList()
                    val property = properties.find { it.name == camelCaseName }
                    val isFileUploadField = property!!.annotations.any { it is FileUploadField }
                    val htmlInputType = getHtmlInputType(column.columnType, isFileUploadField)

                    @Suppress("UNCHECKED_CAST")
                    val enumValues: Array<out Enum<*>>? =
                        getEnumValues(htmlInputType, column)?.first as? Array<out Enum<*>>

                    mapOf(
                        "name" to column.name,
                        "html_input_type" to htmlInputType,
                        "original_type" to property.returnType.toString(),
                        "enum_values" to enumValues?.map { enumValue -> enumValue.name }
                    )
                }
        } else {
            entityKClass.memberProperties.map { property ->
                val columnName = property.name
                val isFileUploadField = property.annotations.any { it is FileUploadField }
                val htmlInputType = getHtmlInputType(property.returnType, isFileUploadField)
                val enumValues: Array<out Any>? = when (property.returnType.classifier) {
                    is KClass<*> -> {
                        val kClass = property.returnType.classifier as KClass<*>
                        if (kClass.java.isEnum) {
                            kClass.java.enumConstants
                        } else {
                            null
                        }
                    }

                    else -> null
                }
                mapOf(
                    "name" to columnName,
                    "html_input_type" to htmlInputType,
                    "original_type" to property.returnType.toString(),
                    "enum_values" to enumValues?.map { enumValue ->
                        when (enumValue) {
                            is Enum<*> -> enumValue.name
                            is ObjectId -> enumValue.toHexString()
                            else -> enumValue.toString()
                        }
                    }
                )
            }
        }
    }

    /**
     * Determines the HTML input type for a given column.
     *
     * This function maps Exposed column types to appropriate HTML input types for rendering forms
     * in the admin panel.
     *
     * @param column The column for which to determine the HTML input type
     * @param isFileUploadField Boolean indicating if the column is annotated for file upload
     * @return A string representing the HTML input type (e.g., "text", "number", "checkbox", etc.)
     */
    private fun <T : Any> getHtmlInputType(column: T, isFileUploadField: Boolean): String {
        return if (column is ColumnType<*>) {
            when (column) {
                is VarCharColumnType -> {
                    if (isFileUploadField) {
                        "file"
                    } else {
                        "text"
                    }
                }
                is BooleanColumnType -> "checkbox"
                is JavaLocalDateTimeColumnType -> "datetime-local"

                is JavaLocalDateColumnType -> "date"
                is EnumerationNameColumnType<*> -> "select"
                is BasicBinaryColumnType, is BlobColumnType -> "file"
                else -> "number"

            }
        } else if (column is KType) {
            val classifier = column.classifier
            when {
                classifier == String::class -> {
                    if (isFileUploadField) {
                        "file"
                    } else {
                        "text"
                    }
                }
                classifier == Int::class || classifier == Long::class || classifier == Short::class -> "number"
                classifier == Float::class || classifier == Double::class -> "number"
                classifier == Boolean::class -> "checkbox"
                classifier == LocalDateTime::class -> "datetime-local"

                classifier == Date::class -> "date"
                classifier is KClass<*> && classifier.java.isEnum -> "select"
                classifier == ByteArray::class -> "file"
                else -> "text"
            }
        } else {
            "text" // Default fallback for unsupported types
        }
    }

    /**
     * Retrieves all table data values for the entity.
     *
     * This method queries the database for all records in the specified entity and returns a list of
     * maps containing the ID and column values for each record.
     *
     * @param configuration The configuration settings for the admin panel
     *
     * @return A list of maps where each map represents a record with its ID and column values
     */
    private suspend fun getTableDataValues(configuration: Configuration): List<Map<String, Any?>?> {
        if (driverType == DriverType.EXPOSED) {
            @Suppress("UNCHECKED_CAST")
            dao!!.findAll() as List<ResultRow>
            val table = entityKClass.objectInstance as Table

            @Suppress("UNCHECKED_CAST")
            val rows = dao!!.findAll() as List<ResultRow>
            return rows.map { row ->
                val rowData = if (configuration.listFields != null && configuration.listFields.isNotEmpty()) {
                    table.columns.filter { configuration.listFields.contains(snakeToCamel(it.name)) || it.name == "id" }
                        .associate { col ->
                            col.name to row[col]
                        }
                } else {
                    table.columns.associate { col ->
                        col.name to row[col]
                    }
                }
                mapOf("id" to rowData.values.toList()[0], "nums" to rowData.values.toList())
            }
        } else {
            val entities = dao!!.findAll()
            return entities.map { entity ->
                var rowData = mutableListOf<Any?>()
                val properties = entityKClass.declaredMemberProperties.toMutableList()
                var primaryKeyField: String? = null
                properties.forEach { property ->
                    if (configuration.listFields != null && configuration.listFields.isNotEmpty() && (!configuration.listFields.contains(
                            property.name
                        ) && property.name != "id")
                    ) {
                        return@forEach
                    }
                    val value = property.call(entity)
                    val actualValue = if (value is LocalDateTime) {
                        value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                    } else {
                        value
                    }
                    val annotation = property.findAnnotation<PrimaryKeyField>()
                    if (annotation != null) {
                        primaryKeyField = property.name
                    }
                    rowData.add(Pair(property.name, (actualValue ?: "null")))
                }

                val map: MutableMap<Int, String> = mutableMapOf()
                var index = 1
                for (data in rowData) {
                    if (data is Pair<*, *>) {
                        if (data.first == "id" || data.first == primaryKeyField) {
                            map[0] = data.second.toString()
                        } else {
                            map[index] = data.second.toString()
                            index++
                        }
                    }
                }

                val arr = arrayOfNulls<String>(map.size)
                for ((key, value) in map) {
                    arr[key] = value
                }
                rowData = arr.toMutableList()
                mapOf("id" to rowData[0], "nums" to rowData)
            }
        }
    }

    /**
     * Validates the user session based on cookies.
     *
     * This method checks if the session ID from the request cookies exists in Redis,
     * indicating a valid session.
     *
     * @param call The routing call to respond to
     * @return True if the session is valid, false otherwise
     */
    private suspend fun validateSession(call: RoutingCall): Boolean {
        val cookies = call.request.cookies
        val sessionId = cookies["session_id"] ?: ""
        val session = InMemorySessionManager.get(sessionId)
        return !(configuration?.setAuthentication == true && session == null)
    }

    /**
     * Retrieves enum values for a column if it is an enumeration type.
     *
     * This method checks if the column is an ExposedColumn or KType and returns the enum constants
     * if applicable. It is used to populate select options in forms.
     *
     * @param htmlInputType The HTML input type to check against (e.g., "select")
     * @param column The column to check for enum values
     * @return A pair containing the enum values array (or null if not applicable) and the column type
     */
    private fun <T : Any> getEnumValues(htmlInputType: String, column: T): Pair<Array<out Any>?, String>? {
        return if (column is ExposedColumn<*> && htmlInputType == "select") {
            val enumConstants = if (column.columnType is EnumerationColumnType<*>) {
                (column.columnType as EnumerationColumnType<*>).klass.java.enumConstants
            } else {
                (column.columnType as EnumerationNameColumnType<*>).klass.java.enumConstants
            }
            Pair(enumConstants, "exposed")
        } else if (column is KType && htmlInputType == "select" && column.classifier is KClass<*> && (column.classifier as KClass<*>).java.isEnum) {
            Pair((column.classifier as KClass<*>).java.enumConstants, "non-exposed")
        } else {
            null
        }
    }

    /**
     * Retrieves the column values for a given column type and parameter value.
     *
     * This method converts the parameter value to the appropriate type based on the column type,
     * handling various data types like Int, Long, Boolean, LocalDateTime, etc.
     *
     * @param column The column type to convert the parameter value for
     * @param paramValue The parameter value to convert
     * @return The converted value or null if conversion fails
     */
    private fun <T : Any> getColumnValues(column: T, paramValue: String?): Any? {
        return if (driverType == DriverType.EXPOSED) {
            when ((column as ExposedColumn<*>).columnType) {
                is EntityIDColumnType<*> -> paramValue?.toIntOrNull()
                is BooleanColumnType -> paramValue?.toBoolean()
                is IntegerColumnType -> paramValue?.toIntOrNull()
                is LongColumnType -> paramValue?.toLongOrNull()
                is DecimalColumnType -> paramValue?.toBigDecimalOrNull()
                is JavaLocalDateTimeColumnType -> paramValue?.let { LocalDateTime.parse(it) }
                else -> paramValue
            }
        } else {
            @Suppress("UNCHECKED_CAST")
            when ((column as KProperty1<T, *>).returnType.classifier) {
                Int::class -> paramValue?.toIntOrNull()
                Long::class -> paramValue?.toLongOrNull()
                Boolean::class -> paramValue?.toBoolean() == true
                LocalDateTime::class -> paramValue?.let { LocalDateTime.parse(it) }
                ByteArray::class -> paramValue?.toByteArray()
                else -> paramValue
            }
        }
    }

    /**
     * Constructs a map of data to save from the provided parameters.
     *
     * This method iterates over the entity's columns or properties, retrieves the corresponding
     * parameter values, and constructs a map of data to be saved. It also handles password hashing
     * for common password fields.
     *
     * @param params The parameters received from the request
     * @param dataToSave The mutable map to populate with data to save
     */
    private suspend fun constructDataToSave(
        params: MultiPartData,
        dataToSave: MutableMap<String, Any>,
        view: String = "details"
    ) {
        if (driverType == DriverType.EXPOSED) {
            (entityKClass.objectInstance as Table).columns.forEach { column ->
                val columnName = column.name
                if (view == "create" && columnName.equals("id", ignoreCase = true)) {
                    return@forEach
                }

                val value = readMultipartData(params, column)

                transaction(this.database as Database) {
                    if (value != null) {
                        val property =
                            entityKClass.declaredMemberProperties.find { it.name == snakeToCamel(columnName) }
                        if (property?.findAnnotation<PasswordField>() != null || columnName == "password") {
                            dataToSave[columnName] = BCrypt.hashpw(value.toString(), BCrypt.gensalt())
                        } else if (column.columnType is BlobColumnType) {
                            dataToSave[columnName] = ExposedBlob(value as ByteArray)
                        } else {
                            dataToSave[columnName] = value
                        }
                    }
                }
            }
        } else {
            entityKClass.declaredMemberProperties.forEach { property ->
                val columnName = property.name
                if (view == "create" && columnName.equals("id", ignoreCase = true)) {
                    return@forEach
                }


                val value = readMultipartData(params, property)

                if (value != null) {
                    val property = entityKClass.declaredMemberProperties.find { it.name == columnName }
                    if (property?.findAnnotation<PasswordField>() != null || columnName == "password") {
                        dataToSave[columnName] = BCrypt.hashpw(value.toString(), BCrypt.gensalt())
                    } else {
                        dataToSave[columnName] = value
                    }
                }
            }
        }
    }

    /**
     * Reads multipart data for a given column.
     *
     * This method reads the multipart data from the request and retrieves the value for the
     * specified column. It handles both form items and file items, returning the appropriate value.
     *
     * @param params The multipart data received from the request
     * @param column The column to read the value for
     * @return The value for the column, or null if not found
     */
    private suspend fun readMultipartData(params: MultiPartData, column: Any): Any? {
        var saveToDisk = false
        var path: String? = null
        val column =
            if (driverType == DriverType.EXPOSED) {
                transaction(this.database as Database) {
                    val idProp =
                        entityKClass.memberProperties.firstOrNull { it.name == "id" } ?: throw IllegalArgumentException(
                            "Exposed Entity must have an 'id' property"
                        )
                    val property =
                        entityKClass.declaredMemberProperties.plus(idProp)
                            .find { it.name == snakeToCamel((column as ExposedColumn<*>).name) } as KProperty1<T, *>
                    val annotation = property.findAnnotation<FileUploadField>()
                    if (annotation != null) {
                        saveToDisk = true
                        path = annotation.path
                    }
                    property.getter.call(entityKClass.objectInstance as Table)
                }
            } else {
                @Suppress("UNCHECKED_CAST")
                val property =
                    entityKClass.declaredMemberProperties.find { it.name == (column as KProperty1<T, *>).name } as KProperty1<T, *>
                val annotation = property.findAnnotation<FileUploadField>()
                if (annotation != null) {
                    saveToDisk = true
                    path = annotation.path
                }
                column
            }

        val partData: PartData? = params.readPart()
        var value: Any? = null
        if (partData is PartData.FormItem) {
            var paramValue = partData.value

            // Handle checkbox values
            if (paramValue == "on") {
                paramValue = "true"
            } else if (paramValue == "off") {
                paramValue = "false"
            }

            value = getColumnValues(column!!, paramValue)
        } else if (partData is PartData.FileItem) {
            val channel = partData.provider()
            if (saveToDisk) {
                // Save file to disk
                val fileName = partData.originalFileName as String
                val file = File("${path?.slice(1..path.length - 1)}/$fileName")
                channel.copyAndClose(file.writeChannel())
                value = file.path
            } else {
                // Read file content into a ByteArray
                val buffer = ByteArray(4096)
                value = mutableListOf<Byte>()
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer)
                    if (bytesRead > 0) {
                        value.addAll(buffer.take(bytesRead))
                    }
                }
                value = value.toByteArray()
            }

        }

        partData?.dispose()

        return value
    }

    /**
     * Sets up the route for the login view.
     *
     * This method creates a GET and POST route for the admin panel's login page, rendering the
     * specified template (or default template if none provided).
     *
     * @param configuration The configuration settings for the admin panel
     * @param data Map of data to be passed to the template
     */
    protected fun exposeLoginView(configuration: Configuration, data: MutableMap<String, Any>) {
        application?.routing {
            staticResources("/static", "static")
            route("/${configuration.url}/login") {
                get {
                    val isValid = validateSession(call)
                    if (!isValid) {
                        call.respond(
                            configuration.templateRenderer.render(
                                configuration, "login", defaultLoginTemplate, data
                            )
                        )
                    } else {
                        val endpoint =
                            if (configuration.endpoint === "/") "" else "/${configuration.endpoint}"
                        call.respondRedirect("/${configuration.url}${endpoint}")
                    }
                }

                post {
                    val params = call.receiveParameters()
                    val username = params["username"]
                    val password = params["password"]


                    // Check if username and password are provided
                    suspend fun validateUser(user: Any?, password: String?, hashedPassword: String?) {
                        if (user != null && BCrypt.checkpw(password.toString(), hashedPassword)) {
                            val sessionId = UUID.randomUUID().toString()
                            val maxAge = 60 * 60 * 24 * 30

                            InMemorySessionManager.set(sessionId, username.toString(), maxAge.toLong())

                            call.response.cookies.append(
                                Cookie(
                                    name = "session_id",
                                    value = sessionId,
                                    httpOnly = true,
                                    path = "/",
                                    maxAge = maxAge, // 30 days
                                    secure = false
                                )
                            )

                            val endpoint =
                                if (configuration.endpoint === "/") ""
                                else "/${configuration.endpoint}"
                            call.respondRedirect("/${configuration.url}${endpoint}")
                        } else {
                            data["errorMessage"] = "Invalid username or password"
                            call.respond(
                                MustacheContent(
                                    configuration.customLoginTemplate ?: defaultLoginTemplate,
                                    data
                                )
                            )
                        }
                    }

                    val user: Any?
                    when (driverType) {
                        DriverType.EXPOSED -> {
                            user = dao!!.find(username.toString())
                            @Suppress("UNCHECKED_CAST")
                            validateUser(user, password, (user as Pair<String, String>).second)
                        }

                        DriverType.JPA -> {
                            user = dao!!.find(username.toString())
                            validateUser(user, password, (user as? JpaAdminUser)?.password)
                        }

                        DriverType.MONGO -> {
                            user = dao!!.find(username.toString())
                            validateUser(user, password, (user as? MongoAdminUser)?.password)
                        }
                    }
                }
            }
        }
    }

    /**
     * Sets up the route for the index view.
     *
     * This method creates a GET route for the admin panel's main page that renders using the
     * specified template (or default template if none provided).
     *
     * @param configuration The configuration settings for the admin panel
     * @param data Map of data to be passed to the template
     */
    protected fun exposeIndexView(configuration: Configuration, data: Map<String, Any>) {
        application?.routing {
            staticResources("/static", "static")
            val endpoint =
                if (configuration.endpoint === "/") "" else "/${configuration.endpoint}"
            route("/${configuration.url}${endpoint}") {
                get {
                    val isValid = validateSession(call)
                    if (!isValid) {
                        call.respondRedirect("/${configuration.url}/login")
                    } else {
                        call.respond(
                            configuration.templateRenderer.render(
                                configuration, "index", defaultIndexTemplate, data
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Sets up the route for the list view that displays table records.
     *
     * This method creates a GET route for viewing all records in a table, using the specified
     * template (or default template if none provided).
     *
     * @param configuration The configuration settings for the admin panel
     * @param data Map of data to be passed to the template
     * @param entityPath The path to the entity being listed, used in the URL
     */
    protected fun exposeListView(configuration: Configuration, data: MutableMap<String, Any>, entityPath: String) {
        application?.routing {
            staticResources("/static", "static")
            route("/${configuration.url}/${entityPath}/list") {
                get {
                    val isValid = validateSession(call)
                    if (isValid) {
                        val tableDataValues = getTableDataValues(configuration)
                        val tablesData =
                            mapOf(
                                "headers" to headers,
                                "data" to mapOf("values" to tableDataValues)
                            )
                        data["tablesData"] = tablesData

                        if (successMessage != null) {
                            data["successMessage"] = successMessage.toString()
                            successMessage = null
                        } else {
                            data.remove("successMessage")
                        }

                        call.respond(
                            configuration.templateRenderer.render(
                                configuration, "list", defaultListTemplate, data
                            )
                        )
                    } else {
                        val loginUrl = "/${configuration.url}/login"
                        call.respondRedirect(loginUrl)
                    }
                }
            }
        }
    }

    /**
     * Sets up the route for the create view that allows adding new records.
     *
     * This method creates a POST route for adding new records to a table, using a custom
     * template (or default template if none provided).
     *
     * @param configuration The configuration settings for the admin panel
     * @param data Map of data to be passed to the template
     * @param entityPath The path to the entity being created, used in the URL
     */
    protected fun exposeCreateView(configuration: Configuration, data: MutableMap<String, Any>, entityPath: String) {
        application?.routing {
            staticResources("/static", "static")
            route("/${configuration.url}/${entityPath}/new") {
                get {
                    val isValid = validateSession(call)
                    if (isValid) {
                        val columnTypes = getColumnTypes()
                        val fieldsForTemplate =
                            columnTypes.map { props ->
                                val inputType = props["html_input_type"] as String
                                val originalType = props["original_type"] ?: ""
                                val isReadOnly = (props["name"] as String).equals("id", ignoreCase = true)

                                val map = mutableMapOf(
                                    "name" to props["name"],
                                    "html_input_type" to inputType,
                                    "original_type" to originalType,
                                    "is_checkbox" to (inputType == "checkbox"),
                                    "is_select" to (inputType == "select"),
                                    "is_textarea" to (inputType == "textarea"),
                                    "is_hidden" to (props["name"] == "id"),
                                    "is_readonly" to isReadOnly,
                                    "is_file" to (inputType == "file"),
                                    "is_general_input" to
                                            !listOf("checkbox", "select", "textarea", "hidden", "file")
                                                .contains(inputType)
                                )

                                if (props["enum_values"] != null && inputType == "select") {
                                    @Suppress("UNCHECKED_CAST")
                                    map["options"] = (props["enum_values"] as? List<String>)?.map { value ->
                                        mapOf("value" to value, "text" to value, "selected" to false)
                                    }
                                }

                                map
                            }

                        data["fields"] = fieldsForTemplate
                        data["tablesData"] =
                            mapOf("headers" to headers, "data" to mapOf("values" to getTableDataValues(configuration)))

                        call.respond(
                            configuration.templateRenderer.render(
                                configuration, "create", defaultCreateTemplate, data
                            )
                        )
                    } else {
                        val loginUrl = "/${configuration.url}/login"
                        call.respondRedirect(loginUrl)
                    }
                }

                post {
                    val isValid = validateSession(call)
                    if (!isValid) {
                        call.respondRedirect("/${configuration.url}/login")
                        return@post
                    }

                    val params = call.receiveMultipart()
                    var dataToSave = mutableMapOf<String, Any>()
                    val id: Any?
                    if (driverType == DriverType.EXPOSED) {
                        constructDataToSave(params, dataToSave, "create")
                        val instance = dao!!.save(dataToSave)
                        @Suppress("UNCHECKED_CAST")
                        id = (instance as Map<String, Any?>)["id"]
                            ?: throw IllegalArgumentException("Saved entity does not have an ID.")
                    } else {
                        constructDataToSave(params, dataToSave, "create")
                        val constructor = entityKClass.primaryConstructor
                            ?: throw IllegalArgumentException("Entity class must have a primary constructor.")

                        val args = constructor.parameters.associateWith { param ->
                            when {
                                param.name == "id" && driverType == DriverType.MONGO -> ObjectId()

                                (param.type.classifier as? KClass<*>)?.java?.isEnum == true -> {
                                    val value = dataToSave[param.name]
                                    if (value is String) {
                                        val enumClass = param.type.classifier as KClass<*>
                                        try {
                                            @Suppress("UNCHECKED_CAST")
                                            java.lang.Enum.valueOf(
                                                enumClass.java as Class<out Enum<*>>,
                                                value
                                            )
                                        } catch (e: Exception) {
                                            throw IllegalArgumentException("Invalid enum value '$value' for ${param.name}: ${e.message}")
                                        }
                                    } else {
                                        dataToSave[param.name]
                                    }
                                }

                                else -> dataToSave[param.name]
                            }
                        }.toMap()

                        val entityInstance = constructor.callBy(args)
                        val savedInstance = dao!!.save(entityInstance)
                        val idProperty =
                            savedInstance::class.memberProperties.find {
                                setOf(
                                    "id",
                                    "_id"
                                ).contains(it.name)
                            }

                        id = when (val value = idProperty?.getter?.call(savedInstance)) {
                            is ObjectId -> value.toHexString()
                            else -> value?.toString()
                        }

                    }
                    successMessage = "Instance created successfully with ID: $id"
                    call.respondRedirect("/${configuration.url}/$entityPath/list")
                }
            }
        }
    }

    /**
     * Sets up the route for the update view that allows editing existing records.
     *
     * This method creates a GET route for editing existing records in a table, using the specified
     * template (or default template if none provided).
     *
     * @param configuration The configuration settings for the admin panel
     * @param data Map of data to be passed to the template
     * @param entityPath The path to the entity being updated, used in the URL
     */
    fun exposeDetailsView(configuration: Configuration, data: MutableMap<String, Any>, entityPath: String) {
        application?.routing {
            staticResources("/static", "static")
            route("/${configuration.url}/${entityPath}/edit/{id}") {
                get {
                    val isValid = validateSession(call)

                    if (isValid) {
                        val idValue: Any = try {
                            call.parameters["id"]?.toInt()
                                ?: throw IllegalArgumentException("ID parameter is required")
                        } catch (e: NumberFormatException) {
                            call.parameters["id"] ?: throw IllegalArgumentException("ObjectID parameter is required")
                        }

                        val obj = if (driverType == DriverType.EXPOSED) {
                            val entity = dao!!.findById(idValue, true)
                            (entityKClass.objectInstance as Table).columns.associate { column ->
                                val camelCaseName = snakeToCamel(column.name)
                                val idProp = entityKClass.memberProperties.firstOrNull { it.name == "id" }
                                    ?: throw IllegalArgumentException("Exposed Entity must have an 'id' property")
                                val properties =
                                    entityKClass.declaredMemberProperties.plus(idProp)
                                val property = properties.find { it.name == camelCaseName }

                                @Suppress("UNCHECKED_CAST")
                                var actualValue = (entity as Map<String, Any?>)[column.name]
                                if (actualValue is LocalDateTime) {
                                    actualValue = actualValue.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                                }
                                val isFileUploadField = property!!.annotations.any { it is FileUploadField }
                                if (isFileUploadField) {
                                    actualValue = (actualValue as String).split("/").last()
                                }
                                val htmlInputType = getHtmlInputType(column.columnType, isFileUploadField)
                                val enumValues: Pair<Array<out Any>?, String>? = getEnumValues(htmlInputType, column)
                                column.name to
                                        mapOf(
                                            "value" to actualValue,
                                            "html_input_type" to htmlInputType,
                                            "original_type" to
                                                    column.columnType::class.simpleName,
                                            "enum_values" to enumValues
                                        )
                            }
                        } else {
                            val entity = dao!!.findById(idValue)
                            entityKClass.memberProperties.associate { property ->
                                val value = property.call(entity)
                                var actualValue = if (value is LocalDateTime) {
                                    value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                                } else {
                                    value
                                }
                                val isFileUploadField = property.annotations.any { it is FileUploadField }
                                if (isFileUploadField) {
                                    actualValue = (actualValue as String).split("/").last()
                                }
                                val htmlInputType = getHtmlInputType(property.returnType, isFileUploadField)
                                val enumValues: Pair<Array<out Any>?, String>? =
                                    getEnumValues(htmlInputType, property.returnType)
                                property.name to
                                        mapOf(
                                            "value" to actualValue,
                                            "html_input_type" to htmlInputType,
                                            "original_type" to property.returnType.toString(),
                                            "enum_values" to enumValues
                                        )
                            }
                        }

                        val fieldsForTemplate =
                            obj.entries.map { (name, props) ->
                                val inputType = props["html_input_type"] as String
                                val originalType = props["original_type"] as? String ?: ""
                                val isReadOnly =
                                    name.equals("id", ignoreCase = true) ||
                                            name.equals("created", ignoreCase = true) ||
                                            name.equals("modified", ignoreCase = true) ||
                                            name.equals("password", ignoreCase = true)

                                val map = mutableMapOf(
                                    "name" to name,
                                    "value" to props["value"],
                                    "html_input_type" to inputType,
                                    "original_type" to originalType,
                                    "is_checkbox" to (inputType == "checkbox"),
                                    "is_select" to (inputType == "select"),
                                    "is_textarea" to (inputType == "textarea"),
                                    "is_readonly" to isReadOnly,
                                    "is_file" to (inputType == "file"),
                                    "is_blob" to (
                                            originalType == "BasicBinaryColumnType"
                                                    || originalType == "BlobColumnType"
                                                    || originalType == "kotlin.ByteArray?"
                                                    || originalType == "kotlin.ByteArray"
                                            ),
                                    "is_general_input" to
                                            !listOf("checkbox", "select", "textarea", "hidden", "file")
                                                .contains(inputType)
                                )

                                if (inputType == "select") {
                                    @Suppress("UNCHECKED_CAST")
                                    val enumValues = if (driverType == DriverType.EXPOSED) {
                                        ((props["enum_values"] as? Pair<Array<out Any>?, String>?)?.first as? Array<out Enum<*>>)?.map { it.name }
                                    } else {
                                        (props["enum_values"] as? Pair<Array<out Any>?, String>?)?.first?.map { enumValue ->
                                            when (enumValue) {
                                                is Enum<*> -> enumValue.name
                                                is ObjectId -> enumValue.toHexString()
                                                else -> enumValue.toString()
                                            }
                                        }
                                    }
                                    if (enumValues != null) {
                                        map["options"] = enumValues.map { value ->
                                            val originalEnumValue = (props["value"] as? Enum<*>)?.name
                                                ?: props["value"]?.toString()

                                            mapOf(
                                                "value" to value,
                                                "text" to value,
                                                "selected" to (originalEnumValue == value)
                                            )
                                        }
                                    }
                                }

                                map
                            }

                        data["fields"] = fieldsForTemplate
                        data["idValue"] = idValue.toString()
                        data["object"] = obj

                        if (successMessage != null) {
                            data["successMessage"] = successMessage.toString()
                            successMessage = null
                        } else {
                            data.remove("successMessage")
                        }

                        call.respond(
                            configuration.templateRenderer.render(
                                configuration, "details", defaultDetailsTemplate, data
                            )
                        )
                    } else {
                        val loginUrl = "/${configuration.url}/login"
                        call.respondRedirect(loginUrl)
                    }
                }

                post {
                    val isValid = validateSession(call)
                    if (!isValid) {
                        call.respondRedirect("/${configuration.url}/login")
                        return@post
                    }

                    val params = call.receiveMultipart()

                    try {
                        call.parameters["id"]?.toInt()
                            ?: throw IllegalArgumentException("ID parameter is required")
                    } catch (e: NumberFormatException) {
                        call.parameters["id"] ?: throw IllegalArgumentException("ID parameter is required")
                    }

                    val dataToSave = mutableMapOf<String, Any>()

                    if (driverType == DriverType.EXPOSED) {
                        constructDataToSave(params, dataToSave)
                        dao!!.update(dataToSave)
                    } else {
                        constructDataToSave(params, dataToSave)
                        val constructor = entityKClass.primaryConstructor
                            ?: throw IllegalArgumentException("Entity class must have a primary constructor.")

                        val args = constructor.parameters.associateWith { param ->
                            val value = dataToSave[param.name]

                            when (param.type.classifier) {
                                ObjectId::class -> (value as? String)?.let { ObjectId(it) }
                                LocalDateTime::class -> {
                                    if (value!!::class == LocalDateTime::class) {
                                        value
                                    } else if (value is String) {
                                        LocalDateTime.parse(value)
                                    } else {
                                        null
                                    }

                                }
                                is KClass<*> -> if ((param.type.classifier as KClass<*>).java.isEnum) {
                                    value.let { enumValue ->
                                        @Suppress("UNCHECKED_CAST")
                                        java.lang.Enum.valueOf(
                                            (param.type.classifier as KClass<*>).java as Class<out Enum<*>>,
                                            enumValue as String
                                        )
                                    }
                                } else value

                                else -> value
                            }
                        }

                        val entityInstance = constructor.callBy(args)
                        dao!!.update(entityInstance)
                    }

                    successMessage = "Instance updated successfully with ID: ${dataToSave["id"]}"
                    call.respondRedirect("/${configuration.url}/$entityPath/edit/${dataToSave["id"]}")
                }
            }
        }
    }

    /**
     * Sets up the route for the delete view that allows deleting records.
     *
     * This method creates a GET route for confirming deletion of a record in a table, using the
     * specified template (or default template if none provided).
     *
     * @param configuration The configuration settings for the admin panel
     * @param data Map of data to be passed to the template
     * @param entityPath The path to the entity being deleted, used in the URL
     */
    fun exposeDeleteView(configuration: Configuration, data: MutableMap<String, Any>, entityPath: String) {
        application?.routing {
            staticResources("/static", "static")
            route("/${configuration.url}/${entityPath}/delete/{id}") {
                get {
                    val isValid = validateSession(call)
                    if (isValid) {
                        val idValue = if (driverType == DriverType.MONGO) {
                            call.parameters["id"]?.toString()
                                ?: throw IllegalArgumentException("ObjectID parameter is required")
                        } else {
                            call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("ID parameter is required")
                        }

                        dao?.delete(idValue)
                            ?: throw IllegalStateException("Delete operation failed for driver type: $driverType")
                        data["instanceId"] = idValue

                        call.respond(
                            MustacheContent(
                                configuration.customDeleteTemplate ?: defaultDeleteTemplate,
                                data
                            )
                        )
                        call.respond(
                            configuration.templateRenderer.render(
                                configuration, "delete", defaultDeleteTemplate, data
                            )
                        )
                    } else {
                        val loginUrl = "/${configuration.url}/login"
                        call.respondRedirect(loginUrl)
                    }
                }
            }
        }
    }

    /**
     * Sets up the route for logging out of the admin panel.
     *
     * This method creates a GET route for logging out, which clears the session cookie and redirects
     * to the login page.
     *
     * @param configuration Configuration settings for the admin panel
     * @param data Map of data to be passed to the template
     */
    fun exposeLogoutView(configuration: Configuration, data: MutableMap<String, Any>) {
        application?.routing {
            staticResources("/static", "static")
            route("/${configuration.url}/logout") {
                get {
                    val isValid = validateSession(call)
                    if (isValid) {
                        call.response.cookies.append(
                            Cookie(
                                name = "session_id",
                                value = "",
                                httpOnly = true,
                                path = "/",
                                maxAge = 0,
                                secure = false
                            )
                        )
                        call.respond(
                            configuration.templateRenderer.render(
                                configuration, "logout", defaultLogoutTemplate, data
                            )
                        )
                    } else {
                        call.respondRedirect("/${configuration.url}/login")
                    }
                }
            }
        }
    }
}

/**
 * A specialised view for database entity administration.
 *
 * EntityView is a concrete implementation of [BaseView] that provides standard admin panel
 * functionality for database entity. It inherits all the capabilities of BaseView including route
 * setup and template rendering for CRUD operations.
 *
 * This class can be extended to customise admin behavior for specific entity classes, or used directly for
 * standard database administration needs.
 *
 * @param entityKClass The KClass of the database entity class to generate admin views for
 * @see BaseView
 */
class EntityView<T : Any>(val entityKClass: KClass<T>) : BaseView<T>(entityKClass) {
    /**
     * Sets up all the admin panel views and routes.
     *
     * This method initialises the necessary properties and calls the individual expose methods to
     * set up routes for different admin panel views.
     *
     * @param application The Ktor application instance for setting up routes
     * @param configuration Configuration settings for the admin panel
     * @param tableNames List of table names to be managed in the admin panel
     * @param database The database connection to be used for data access
     * @param entityManagerFactory Optional JPA EntityManagerFactory for JPA-based data access
     * @throws IllegalArgumentException if neither database nor entityManagerFactory is provided
     */
    fun configurePageViews(
        application: Application,
        configuration: Configuration,
        tableNames: MutableList<String>,
        database: Any?,
        entityManagerFactory: EntityManagerFactory?
    ) {
        super.configuration = configuration
        super.application = application
        super.database = database
        super.dao = if (database is Database) {
            super.driverType = DriverType.EXPOSED
            ExposedDao(database, entityKClass)
        } else if (entityManagerFactory is EntityManagerFactory) {
            super.driverType = DriverType.JPA
            JpaDao(entityManagerFactory, entityKClass)
        } else if (database is MongoDatabase) {
            super.driverType = DriverType.MONGO
            MongoDao(database, entityKClass)
        } else {
            throw IllegalArgumentException("Either database or entityManagerFactory must be provided")
        }
        super.headers = super.setHeaders(configuration)

        this.setAuthentication(configuration, entityManagerFactory)

        this.exposeIndexView(
            configuration,
            mapOf(
                "tables" to tableNames.map { it.lowercase() },
                "configuration" to configuration
            )
        )

        // Determine the entity name based on the type of entity class provided
        val entity = when (super.driverType) {
            DriverType.EXPOSED -> (this.entityKClass.objectInstance as Table).tableName
            DriverType.JPA, DriverType.MONGO -> this.entityKClass.simpleName
                ?: throw IllegalArgumentException("Entity must have a simple name")
        }

        // Use the entity name to determine the path for delete, list, create, and details views
        val entityPath = when (super.driverType) {
            DriverType.EXPOSED -> (this.entityKClass.objectInstance as Table).tableName.lowercase()
            DriverType.JPA, DriverType.MONGO -> this.entityKClass.simpleName?.lowercase()
                ?: throw IllegalArgumentException("Entity must have a simple name")
        }

        this.exposeDeleteView(
            configuration,
            mutableMapOf(
                "tables" to tableNames.map { it.lowercase() },
                "configuration" to configuration,
                "entity" to entity,
                "entityPath" to entityPath
            ),
            entityPath = entityPath
        )

        for (table in tableNames) {
            this.exposeListView(
                configuration,
                mutableMapOf(
                    "configuration" to configuration,
                    "tableName" to table,
                    "tableNameLowercased" to table.lowercase(),
                ),
                entityPath = table.lowercase()
            )
            this.exposeCreateView(
                configuration,
                mutableMapOf(
                    "entity" to table,
                    "configuration" to configuration,
                    "tableName" to table,
                    "tableNameLowercased" to table.lowercase(),
                    "entityPath" to table.lowercase()
                ),
                entityPath = table.lowercase()
            )
            this.exposeDetailsView(
                configuration,
                mutableMapOf(
                    "entity" to table,
                    "configuration" to configuration,
                    "entityPath" to table.lowercase()
                ),
                entityPath = table.lowercase()
            )
        }
    }

    /**
     * Sets up the authentication for the admin panel.
     *
     * This method checks if authentication is enabled in the configuration and sets up an admin user
     * with a hashed password if it doesn't already exist.
     *
     * @param configuration The configuration settings for the admin panel
     * @param entityManagerFactory Optional JPA EntityManagerFactory for JPA-based data access
     */
    private fun setAuthentication(configuration: Configuration, entityManagerFactory: EntityManagerFactory?) {
        if (configuration.setAuthentication) {
            runBlocking {
                var existingAdminUser: Any? = null
                val hashedPassword = BCrypt.hashpw(configuration.adminPassword, BCrypt.gensalt())
                if (super.driverType == DriverType.EXPOSED) {
                    // Create the AdminUser table if it doesn't exist
                    val dao = ExposedDao(database as Database, AdminUsers::class)
                    dao.createTable()

                    // Create the admin user if it doesn't exist
                    existingAdminUser = dao.find(configuration.adminUsername)
                    if (existingAdminUser == null) {
                        val entity = mapOf("username" to configuration.adminUsername, "password" to hashedPassword)
                        dao.save(entity)
                    }
                } else {
                    // Create the AdminUser table if it doesn't exist
                    if (driverType == DriverType.JPA) {
                        val dao = JpaDao(entityManagerFactory!!, JpaAdminUser::class)
                        dao.createTable()
                        existingAdminUser = dao.find(configuration.adminUsername)
                    } else if (driverType == DriverType.MONGO) {
                        val dao = MongoDao(database as MongoDatabase, MongoAdminUser::class)
                        dao.createTable()
                        existingAdminUser = dao.find(configuration.adminUsername)
                    }

                    // Create the admin user if it doesn't exist
                    if (existingAdminUser == null) {
                        if (driverType == DriverType.JPA) {
                            val dao = JpaDao(entityManagerFactory!!, JpaAdminUser::class)
                            val entity = JpaAdminUser(username = configuration.adminUsername, password = hashedPassword)
                            dao.save(entity)
                        } else if (driverType == DriverType.MONGO) {
                            val entity = MongoAdminUser(
                                ObjectId(),
                                username = configuration.adminPassword,
                                password = hashedPassword
                            )
                            val dao = MongoDao(database as MongoDatabase, MongoAdminUser::class)
                            dao.save(entity)
                        }
                    }
                }

                // Expose authentication views
                this@EntityView.exposeLoginView(configuration, mutableMapOf("configuration" to configuration))
                this@EntityView.exposeLogoutView(configuration, mutableMapOf("configuration" to configuration))
            }
        }
    }
}
