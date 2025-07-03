package xyz.daimones.ktor.panel

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.mustache.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import jakarta.persistence.Entity
import jakarta.persistence.EntityManagerFactory
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.JavaInstantColumnType
import org.jetbrains.exposed.sql.javatime.JavaLocalDateColumnType
import org.jetbrains.exposed.sql.javatime.JavaLocalDateTimeColumnType
import org.jetbrains.exposed.sql.json.JsonBColumnType
import org.mindrot.jbcrypt.BCrypt
import xyz.daimones.ktor.panel.database.DatabaseAccessObjectInterface
import xyz.daimones.ktor.panel.database.dao.ExposedDao
import xyz.daimones.ktor.panel.database.dao.JpaDao
import xyz.daimones.ktor.panel.database.entities.AdminUser
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

/**
 * Manages base view rendering for admin panel pages.
 *
 * This class is responsible for setting up routes and rendering templates for the admin panel. It
 * provides functionality for index, list, create, details, and update views.
 *
 * @property entityClass The database entity class to generate admin views for
 */
open class BaseView<T : Any>(private val entityClass: T) {
    /**
     * Configuration for the admin panel. This is set during [ModelView.configurePageViews] and
     * contains settings like URL, endpoint, and admin name.
     */
    protected var configuration: Configuration? = null

    /**
     * The application instance for setting up routes. Set during [ModelView.configurePageViews] and
     * used by the expose* methods.
     */
    protected var application: Application? = null

    /**
     * The database instance for data access. This is set during [ModelView.configurePageViews] and
     * used by the ExposedDao for database operations.
     */
    protected var database: Database? = null

    /**
     * The data access object interface for database operations. This is set during
     * [ModelView.configurePageViews] and used to interact with the database.
     */
    protected var dao: DatabaseAccessObjectInterface? = null

    /** Default Mustache template for the index page. */
    private val defaultIndexView = "kt-panel-index.hbs"

    /** Default Mustache template for the list page. */
    private val defaultListView = "kt-panel-list.hbs"

    /** Default Mustache template for the create page. */
    private val defaultCreateView = "kt-panel-create.hbs"

    /** Default Mustache template for the details page. */
    private val defaultDetailsView = "kt-panel-details.hbs"

    /** Default Mustache template for the login page. */
    private val defaultLoginView = "kt-panel-login.hbs"

    /**
     * List of headers for the model's columns. This is used to render table headers in the list
     * view.
     */
    private var headers: List<String>

    /**
     * Success message to be displayed after creating or updating an instance. This is set during
     * the create or update operations and can be used in templates.
     */
    private var successMessage: String? = null

    init {
        // Initialise headers based on the model type
        this.headers = setHeaders()
    }

    /**
     * Initialises the headers for the model's columns.
     *
     * This method sets up the headers based on the model type, either from Exposed IntIdTable or
     * JPA Entity annotations. It is called during the initialisation of the BaseView.
     */
    private fun setHeaders(): List<String> {
        println("${entityClass::class.java.name} what")
        return if (entityClass is IntEntityClass<IntEntity>) {
            entityClass.table.columns.map { it.name }
        } else if (entityClass::class.annotations.any { it is Entity }) {
            @Suppress("UNCHECKED_CAST")
            entityClass::class.memberProperties.toList() as List<String>
        } else {
            throw IllegalArgumentException("Model must be an IntEntityClass or annotated with @Entity")
        }
    }

    /**
     * Retrieves the column types of the model for rendering in templates.
     *
     * This method maps each column in the model to its HTML input type and original type, which is
     * useful for generating forms and input fields dynamically.
     *
     * @param model The IntIdTable model whose columns are to be inspected
     * @return A list of maps containing column names, HTML input types, and original types
     * @throws IllegalArgumentException if the model is not an IntEntity or does not have the @Entity annotation
     */
    private fun <T : Any> getColumnTypes(entityClass: T): List<Map<String, String>> {
        return if (entityClass is IntEntityClass<IntEntity>) {
            entityClass.table.columns.map { column ->
                val htmlInputType = getHtmlInputType(column)
                mapOf(
                    "name" to column.name,
                    "html_input_type" to htmlInputType,
                    "original_type" to column.columnType::class.simpleName.orEmpty()
                )
            }
        } else if (entityClass::class.annotations.any { it is Entity }) {
            entityClass::class.memberProperties.map { property ->
                val columnName = property.name
                val htmlInputType = getHtmlInputType(property.returnType)
                mapOf(
                    "name" to columnName,
                    "html_input_type" to htmlInputType,
                    "original_type" to property.returnType.toString()
                )
            }
        } else {
            throw IllegalArgumentException("Model must be an IntEntityClass or annotated with @Entity")
        }
    }

    /**
     * Determines the HTML input type for a given column.
     *
     * This function maps Exposed column types to appropriate HTML input types for rendering forms
     * in the admin panel.
     *
     * @param column The column for which to determine the HTML input type
     * @return A string representing the HTML input type (e.g., "text", "number", "checkbox", etc.)
     */
    private fun <T : Any> getHtmlInputType(column: T): String {
        return if (column is Column<*>) {
            when (column.columnType) {
                is EntityIDColumnType<*> -> "number"
                is VarCharColumnType, is TextColumnType, is CharacterColumnType -> {
                    if (column.name.contains("password", ignoreCase = true)) "password" else "text"
                }

                is IntegerColumnType, is LongColumnType, is ShortColumnType -> "number"
                is DecimalColumnType, is FloatColumnType, is DoubleColumnType -> "number"
                is BooleanColumnType -> "checkbox"
                is JavaLocalDateColumnType -> "date"
                is JavaLocalDateTimeColumnType, is JavaInstantColumnType -> "datetime-local"
                is EnumerationColumnType<*>, is EnumerationNameColumnType<*> -> "select"
                is JsonBColumnType<*>, is BlobColumnType -> "textarea"
                is BinaryColumnType -> "file"
                else -> "text" // Default fallback
            }
        } else if (column is KType) {
            when (column.classifier) {
                String::class -> "text"
                Int::class, Long::class, Short::class -> "number"
                Boolean::class -> "checkbox"
                LocalDateTime::class -> "datetime-local"
                else -> "text" // Default fallback
            }
        } else {
            "text" // Default fallback for unsupported types
        }
    }

    /**
     * Retrieves all table data values for the model.
     *
     * This method queries the database for all records in the specified model and returns a list of
     * maps containing the ID and column values for each record.
     *
     * @return A list of maps where each map represents a record with its ID and column values
     */
    private suspend fun getTableDataValues(): List<Map<String, Any?>?> {
        val entities = dao!!.findAll(entityClass::class)
        return entities.map { entity ->
            val rowData = mutableListOf<Any?>()
            if (entityClass is IntEntityClass<IntEntity>) {
                var actualValue: Any?
                entityClass.table.columns.forEach { column ->
                    // Find the corresponding property on the entity object using reflection.
                    val property = entity!!::class.memberProperties.find { it.name == column.name }
                    if (property != null) {
                        // Get the value of the property.
                        var value = property.call(entity)
                        value = when (value) {
                            is EntityID<*> -> value.value
                            is IntIdTable -> value.id
                            else -> value
                        }
                        actualValue = value
                    } else {
                        actualValue = "null"
                    }
                    rowData.add(actualValue)
                }
            } else if (entityClass::class.annotations.any { it is Entity }) {
                entityClass::class.memberProperties.forEach { property ->
                    val value = property.call(entity)
                    rowData.add(value ?: "null")
                }
            }
            mapOf("id" to rowData[0], "nums" to rowData)
        }
    }

    /**
     * Sets up the route for the login view.
     *
     * This method creates a GET and POST route for the admin panel's login page, rendering the
     * specified template (or default template if none provided).
     *
     * @param data Map of data to be passed to the template
     * @param template Optional custom template name, if null the default template is used
     */
    protected fun exposeLoginView(data: MutableMap<String, Any>, template: String? = null) {
        application?.routing {
            route("/${configuration?.url}/login") {
                get {
                    val cookies = call.request.cookies
                    val sessionId = cookies["session_id"]

                    if (sessionId != null) {
                        call.respond(MustacheContent(template ?: defaultLoginView, data))
                    } else {
                        val loginUrl = "/${configuration?.url}/login"
                        call.respondRedirect(loginUrl)
                    }
                }

                post {
                    val params = call.receiveParameters()
                    val username = params["username"]
                    val password = params["password"]

                    val user = dao!!.find(username.toString(), AdminUser::class)

                    if (user != null && BCrypt.checkpw(password.toString(), user.password)) {
                        val sessionId = UUID.randomUUID().toString()
                        call.response.cookies.append(
                            Cookie(
                                name = "session_id",
                                value = sessionId,
                                httpOnly = true,
                                path = "/",
                                maxAge = 60 * 60 * 24 * 30, // 30 days
                                secure = false
                            )
                        )

                        val endpoint =
                            if (configuration?.endpoint === "/") ""
                            else "/${configuration?.endpoint}"
                        call.respondRedirect("/${configuration?.url}${endpoint}")
                    } else {
                        data["errorMessage"] = "Invalid username or password"
                        call.respond(MustacheContent(template ?: defaultLoginView, data))
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
     * @param data Map of data to be passed to the template
     * @param template Optional custom template name, if null the default template is used
     */
    protected fun exposeIndexView(data: Map<String, Any>, template: String? = null) {
        application?.routing {
            val endpoint =
                if (configuration?.endpoint === "/") "" else "/${configuration?.endpoint}"
            route("/${configuration?.url}${endpoint}") {
                get {
                    val cookies = call.request.cookies
                    val sessionId = cookies["session_id"]
                    if (sessionId != null) {
                        call.respond(MustacheContent(template ?: defaultIndexView, data))
                    } else {
                        val loginUrl = "/${configuration?.url}/login"
                        call.respondRedirect(loginUrl)
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
     * @param data Map of data to be passed to the template
     * @param template Optional custom template name, if null the default template is used
     * @param modelPath The path to the model being listed, used in the URL
     */
    protected fun exposeListView(
        data: MutableMap<String, Any>,
        template: String? = null,
        modelPath: String
    ) {
        application?.routing {
            route("/${configuration?.url}/${modelPath}/list") {
                get {
                    val cookies = call.request.cookies
                    val sessionId = cookies["session_id"]
                    if (sessionId != null) {
                        val tableDataValues = getTableDataValues()
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

                        call.respond(MustacheContent(template ?: defaultListView, data))
                    } else {
                        val loginUrl = "/${configuration?.url}/login"
                        call.respondRedirect(loginUrl)
                    }
                }
            }
        }
    }

    /**
     * Sets up the route for the create view that allows adding new records.
     *
     * This method creates a POST route for adding new records to a table, using the specified
     * template (or default template if none provided).
     *
     * @param data Map of data to be passed to the template
     * @param template Optional custom template name, if null the default template is used
     * @param modelPath The path to the model being created, used in the URL
     */
    protected fun exposeCreateView(
        data: MutableMap<String, Any?>,
        template: String? = null,
        modelPath: String
    ) {
        application?.routing {
            route("/${configuration?.url}/${modelPath}/new") {
                get {
                    val cookies = call.request.cookies
                    val sessionId = cookies["session_id"]

                    if (sessionId != null) {
                        val columnTypes = getColumnTypes(entityClass)
                        val fieldsForTemplate =
                            columnTypes.map { props ->
                                val inputType = props["html_input_type"] as String
                                val originalType = props["original_type"] ?: ""
                                val isReadOnly = props["name"].equals("id", ignoreCase = true)

                                mapOf(
                                    "name" to props["name"],
                                    "value" to props["value"],
                                    "html_input_type" to inputType,
                                    "original_type" to originalType,
                                    "is_checkbox" to (inputType == "checkbox"),
                                    "is_select" to (inputType == "select"),
                                    "is_textarea" to (inputType == "textarea"),
                                    "is_hidden" to (props["name"] == "id"),
                                    "is_readonly" to isReadOnly,
                                    "is_general_input" to
                                            !listOf("checkbox", "select", "textarea", "hidden")
                                                .contains(inputType)
                                    // TODO: For "is_select", we need to add an "options" list
                                    // to this map
                                    // e.g., "options" to listOf(mapOf("value" to "opt1", "text"
                                    // to "Option 1", "selected" to true/false))
                                )
                            }
                        data["fields"] = fieldsForTemplate
                        val tableDataValues = getTableDataValues()
                        val tablesData =
                            mapOf("headers" to headers, "data" to mapOf("values" to tableDataValues))
                        data["tablesData"] = tablesData
                        call.respond(MustacheContent(template ?: defaultCreateView, data))
                    } else {
                        val loginUrl = "/${configuration?.url}/login"
                        call.respondRedirect(loginUrl)
                    }
                }

                post {
                    val params = call.receiveParameters()
                    val dataToSave = mutableMapOf<String, Any>()
                    var id: Int? = null
                    if (entityClass is IntEntityClass<IntEntity>) {
                        entityClass.table.columns.forEach { column ->
                            val columnName = column.name

                            if (columnName.equals("id", ignoreCase = true)) {
                                return@forEach
                            }

                            val paramValue = params[columnName]

                            val value: Any? = when (column.columnType) {
                                is EntityIDColumnType<*> -> paramValue?.toIntOrNull()
                                is BooleanColumnType -> paramValue?.toBoolean() ?: false
                                is IntegerColumnType -> paramValue?.toIntOrNull()
                                is LongColumnType -> paramValue?.toLongOrNull()
                                is DecimalColumnType -> paramValue?.toBigDecimalOrNull()
                                is JavaLocalDateTimeColumnType -> paramValue?.let { LocalDateTime.parse(it) }
                                else -> paramValue
                            }

                            if (value != null) {
                                dataToSave[columnName] = value
                            }
                        }
                        val instance = dao!!.save(dataToSave as Map<String, Any>, entityClass::class)
                        if (instance is IntEntity) {
                            id = instance.id.value
                        } else {
                            throw IllegalStateException("Saved instance is not an IntEntity")
                        }
                    }
                    successMessage = "Instance created successfully with ID: $id"
                    call.respondRedirect("/${configuration?.url}/$modelPath/list")
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
     * @param data Map of data to be passed to the template
     * @param template Optional custom template name, if null the default template is used
     * @param modelPath The path to the model being updated, used in the URL
     */
    fun exposeDetailsView(
        data: MutableMap<String, Any?>,
        template: String? = null,
        modelPath: String
    ) {
        application?.routing {
            route("/${configuration?.url}/${modelPath}/edit/{id}") {
                get {
                    val cookies = call.request.cookies
                    val sessionId = cookies["session_id"]

                    if (sessionId != null) {
                        val idValue =
                            call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("ID parameter is required")
                        val entity = dao!!.findById(idValue, entityClass::class)
                        val obj = if (entityClass is IntEntityClass<IntEntity>) {
                            entityClass.table.columns.associate { column ->
                                val property = entity!!::class.memberProperties.find { it.name == column.name }
                                val actualValue: Any?
                                if (property != null) {
                                    // Get the value of the property.
                                    val value = property.call(entity)
                                    actualValue = if (value is EntityID<*>) value.value else value
                                } else {
                                    actualValue = null
                                }

                                val htmlInputType = getHtmlInputType(column)
                                column.name to
                                        mapOf(
                                            "value" to actualValue,
                                            "html_input_type" to htmlInputType,
                                            "original_type" to
                                                    column.columnType::class.simpleName
                                        )
                            }
                        } else {
                            entityClass::class.memberProperties.associate { property ->
                                val columnName = property.name
                                val value = property.call(entity)
                                val htmlInputType = getHtmlInputType(property.returnType)

                                columnName to
                                        mapOf(
                                            "value" to value,
                                            "html_input_type" to htmlInputType,
                                            "original_type" to property.returnType.toString()
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

                                mapOf(
                                    "name" to name,
                                    "value" to props["value"],
                                    "html_input_type" to inputType,
                                    "original_type" to originalType,
                                    "is_checkbox" to (inputType == "checkbox"),
                                    "is_select" to (inputType == "select"),
                                    "is_textarea" to (inputType == "textarea"),
                                    "is_readonly" to isReadOnly,
                                    "is_general_input" to
                                            !listOf("checkbox", "select", "textarea", "hidden")
                                                .contains(inputType)
                                    // TODO: For "is_select", we need to add an "options" list
                                    // to this map
                                    // e.g., "options" to listOf(mapOf("value" to "opt1", "text"
                                    // to "Option 1", "selected" to true/false))
                                )
                            }

                        data["fields"] = fieldsForTemplate
                        data["idValue"] = idValue.toString()
                        data["object"] = obj
                        call.respond(MustacheContent(template ?: defaultDetailsView, data))
                    } else {
                        val loginUrl = "/${configuration?.url}/login"
                        call.respondRedirect(loginUrl)
                    }
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
     * @param data Map of data to be passed to the template
     * @param template Optional custom template name, if null the default template is used
     * @param modelPath The path to the model being deleted, used in the URL
     */
    fun exposeDeleteView(
        data: MutableMap<String, Any?>,
        template: String? = null,
        modelPath: String
    ) {
        application?.routing {
            route("/${configuration?.url}/${modelPath}/delete/{id}") {
                get {
                    val cookies = call.request.cookies
                    val sessionId = cookies["session_id"]

                    if (sessionId != null) {
                        val idValue =
                            call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("ID parameter is required")
                        val instance = dao!!.delete(idValue, entityClass::class)
                        val instanceId: Int?
                        if (instance is IntEntityClass<IntEntity>) {
                            instanceId = instance.table.id.toString().toInt()
                        } else {
                            throw IllegalStateException("Deleted instance is not an IntEntity")
                        }
                        data["instanceId"] = instanceId
                        call.respond(MustacheContent(template ?: "kt-panel-delete.hbs", data))
                    } else {
                        val loginUrl = "/${configuration?.url}/login"
                        call.respondRedirect(loginUrl)
                    }
                }
            }
        }
    }
}

/**
 * A specialised view for database model administration.
 *
 * ModelView is a concrete implementation of [BaseView] that provides standard admin panel
 * functionality for database models. It inherits all the capabilities of BaseView including route
 * setup and template rendering for CRUD operations.
 *
 * This class can be extended to customise admin behavior for specific entity classes, or used directly for
 * standard database administration needs.
 */
class ModelView<T : Any>(val entityClass: T) : BaseView<T>(entityClass) {
    /**
     * Sets up all the admin panel views and routes.
     *
     * This method initialises the necessary properties and calls the individual expose methods to
     * set up routes for different admin panel views.
     *
     * @param application The Ktor application instance for setting up routes
     * @param configuration Configuration settings for the admin panel
     * @param tableNames List of table names to be managed in the admin panel
     * @param entityCompanions Optional list of pairs containing entity classes for the models
     * @param database The database connection to be used for data access
     * @param entityManagerFactory Optional JPA EntityManagerFactory for JPA-based data access
     */
    fun configurePageViews(
        application: Application,
        configuration: Configuration,
        tableNames: MutableList<String>,
        entityCompanions: MutableList<Pair<KClass<out IntEntityClass<IntEntity>>, IntEntityClass<IntEntity>>>?,
        database: Database?,
        entityManagerFactory: EntityManagerFactory?
    ) {
        super.configuration = configuration
        super.application = application
        super.database = database
        super.dao = if (database != null && entityCompanions != null) {
            ExposedDao(database, entityCompanions)
        } else if (entityManagerFactory != null) {
            JpaDao(entityManagerFactory)
        } else {
            throw IllegalArgumentException("Either database or entityManagerFactory must be provided")
        }

        if (configuration.setAuthentication) {
            runBlocking {
                // Create the AdminUsers table if it doesn't exist
                this@ModelView.dao!!.createTable(AdminUser::class)

                // Check if the admin user already exists
                val existingAdminUser: AdminUser? = this@ModelView.dao!!.find(
                    configuration.adminUsername,
                    AdminUser::class
                )
                if (existingAdminUser == null) {
                    // Create the admin user with hashed password
                    val hashedPassword = BCrypt.hashpw(configuration.adminPassword, BCrypt.gensalt())
                    val entity = mapOf("username" to configuration.adminUsername, "password" to hashedPassword)
                    this@ModelView.dao!!.save(entity, AdminUser::class)
                }

                // Expose the authentication view
                this@ModelView.exposeLoginView(mutableMapOf("configuration" to configuration))
            }
        }

        this.exposeIndexView(
            mapOf(
                "tables" to tableNames.map { it.lowercase() },
                "configuration" to configuration
            )
        )

        // Determine the model name based on the type of entity class provided
        val model = if (this.entityClass is IntEntityClass<IntEntity>) {
            this.entityClass.table.tableName
        } else if (this.entityClass::class.annotations.any { it is Entity }) {
            this.entityClass::class.simpleName ?: throw IllegalArgumentException("Model must have a simple name")
        } else {
            throw IllegalArgumentException("Model must be an IntEntityClass or annotated with @Entity")
        }

        // Use the model name to determine the path for delete, list, create, and details views
        val modelPath = if (this.entityClass is IntEntityClass<IntEntity>) {
            this.entityClass.table.tableName.lowercase()
        } else if (this.entityClass::class.annotations.any { it is Entity }) {
            this.entityClass::class.simpleName?.lowercase()
                ?: throw IllegalArgumentException("Model must have a simple name")
        } else {
            throw IllegalArgumentException("Model must be an IntEntityClass or annotated with @Entity")
        }

        this.exposeDeleteView(
            mutableMapOf(
                "tables" to tableNames.map { it.lowercase() },
                "configuration" to configuration,
                "model" to model,
                "modelPath" to modelPath
            ),
            modelPath = modelPath
        )

        for (table in tableNames) {
            this.exposeListView(
                mutableMapOf(
                    "configuration" to configuration,
                    "tableName" to table,
                    "tableNameLowercased" to table.lowercase(),
                ),
                modelPath = table.lowercase()
            )
            this.exposeCreateView(
                mutableMapOf(
                    "model" to table,
                    "configuration" to configuration,
                    "tableName" to table,
                    "tableNameLowercased" to table.lowercase(),
                    "modelPath" to table.lowercase()
                ),
                modelPath = table.lowercase()
            )
            this.exposeDetailsView(
                mutableMapOf(
                    "model" to table,
                    "configuration" to configuration,
                    "modelPath" to table.lowercase()
                ),
                modelPath = table.lowercase()
            )
        }
    }
}
