package xyz.daimones.ktor.panel

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.mustache.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Id
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
import xyz.daimones.ktor.panel.database.entities.JpaAdminUser
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import org.jetbrains.exposed.sql.Column as ExposedColumn

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
     * used by the ExposedDao for database operations.
     */
    protected var database: Database? = null

    /**
     * The data access object interface for database operations. This is set during
     * [EntityView.configurePageViews] and used to interact with the database.
     */
    protected var dao: DatabaseAccessObjectInterface? = null

    /** ORM type used for the entity class. */
    protected var ormType: String? = null

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

    /** Default Mustache template for the delete confirmation page. */
    private val defaultDeleteTemplate = "kt-panel-delete.hbs"

    /**
     * List of headers for the entity's columns. This is used to render table headers in the list
     * view.
     */
    private var headers: List<String>

    /**
     * Success message to be displayed after creating or updating an instance. This is set during
     * the create or update operations and can be used in templates.
     */
    private var successMessage: String? = null

    init {
        // Initialise headers based on the entity type
        this.headers = setHeaders()
    }

    /**
     * Initialises the headers for the entity's columns.
     *
     * This method sets up the headers based on the entity type, either from Exposed IntIdTable or
     * JPA Entity annotations. It is called during the initialisation of the BaseView.
     */
    private fun setHeaders(): List<String> {
        return if (entityClass is IntEntityClass<IntEntity>) {
            entityClass.table.columns.map { it.name }
        } else if (entityClass::class.annotations.any { it is Entity }) {
            val properties = entityClass::class.memberProperties.sortedBy { it.name }.toMutableList()
            val idProperty = properties.find { p -> p.javaField?.isAnnotationPresent(Id::class.java) == true }
            val createdProperty = properties.find { p ->
                val names = setOf(
                    "created",
                    "created_at",
                    "createdAt",
                    "creationDate",
                    "createdOn",
                    "creation_date",
                    "created_on"
                )
                if (p.javaField?.isAnnotationPresent(Column::class.java) == true) {
                    names.contains(p.javaField?.getAnnotation(Column::class.java)?.name)
                } else {
                    false
                }
            }
            val modifiedProperty = properties.find { p ->
                val names = setOf(
                    "modified",
                    "updated_at",
                    "updatedAt",
                    "lastModified",
                    "lastUpdated",
                    "last_modified",
                    "last_updated"
                )
                if (p.javaField?.isAnnotationPresent(Column::class.java) == true) {
                    names.contains(p.javaField?.getAnnotation(Column::class.java)?.name)
                } else {
                    false
                }
            }
            if (idProperty != null) {
                properties.remove(idProperty)
                properties.add(0, idProperty)
            }
            if (createdProperty != null) {
                properties.remove(createdProperty)
                properties.add(properties.size, createdProperty)
            }
            if (modifiedProperty != null) {
                properties.remove(modifiedProperty)
                properties.add(properties.size, modifiedProperty)
            }
            properties.map { it.name }
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
     * @param entityClass The entity class for which to retrieve column types
     * @return A list of maps containing column names, HTML input types, and original types
     * @throws IllegalArgumentException if the entity is not an IntEntity or does not have the @Entity annotation
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
        return if (column is ExposedColumn<*>) {
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
     * Retrieves all table data values for the entity.
     *
     * This method queries the database for all records in the specified entity and returns a list of
     * maps containing the ID and column values for each record.
     *
     * @return A list of maps where each map represents a record with its ID and column values
     */
    private suspend fun getTableDataValues(): List<Map<String, Any?>?> {
        val entities = dao!!.findAll(entityClass::class)
        return entities.map { entity ->
            val rowData = mutableListOf<Any?>()
            if (ormType == "Exposed") {
                var actualValue: Any?
                (entityClass as IntEntityClass<IntEntity>).table.columns.forEach { column ->
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
     * Validates the session cookie and responds with the appropriate view.
     *
     * This method checks if the session cookie exists. If it does, it renders the index view with
     * the provided data. If not, it redirects to the login page or renders the login view based on
     * the endpoint.
     *
     * @param call The routing call to respond to
     * @param cookies The request cookies to check for session ID
     * @param data Map of data to be passed to the template
     * @param endpoint The endpoint being accessed (default is "login")
     */
    private suspend fun validateCookie(
        call: RoutingCall,
        cookies: RequestCookies,
        data: Map<String, Any>,
        endpoint: String = "login"
    ) {
        val sessionId = cookies["session_id"]
        if (sessionId != null) {
            call.respond(MustacheContent(configuration?.customIndexTemplate ?: defaultIndexTemplate, data))
        } else {
            val loginUrl = "/${configuration?.url}/login"
            if (endpoint == "login") {
                call.respond(MustacheContent(configuration?.customLoginTemplate ?: defaultLoginTemplate, data))
            } else {
                call.respondRedirect(loginUrl)
            }
        }
    }

    /**
     * Sets up the route for the login view.
     *
     * This method creates a GET and POST route for the admin panel's login page, rendering the
     * specified template (or default template if none provided).
     *
     * @param data Map of data to be passed to the template
     */
    protected fun exposeLoginView(data: MutableMap<String, Any>) {
        application?.routing {
            route("/${configuration?.url}/login") {
                get {
                    val cookies = call.request.cookies
                    validateCookie(call, cookies, data)
                }

                post {
                    val params = call.receiveParameters()
                    val username = params["username"]
                    val password = params["password"]

                    suspend fun validateUser(user: Any?, password: String?, hashedPassword: String?) {
                        if (user != null && BCrypt.checkpw(password.toString(), hashedPassword)) {
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
                            call.respond(
                                MustacheContent(
                                    configuration?.customLoginTemplate ?: defaultLoginTemplate,
                                    data
                                )
                            )
                        }
                    }

                    val user: Any?
                    when (ormType) {
                        "Exposed" -> {
                            user = dao!!.find(username.toString(), AdminUser::class)
                            validateUser(user, password, user?.password)
                        }

                        "JPA" -> {
                            user = dao!!.find(username.toString(), JpaAdminUser::class)
                            validateUser(user, password, user?.password)
                        }

                        else -> throw IllegalStateException("Unsupported ORM type: $ormType")
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
     */
    protected fun exposeIndexView(data: Map<String, Any>) {
        application?.routing {
            val endpoint =
                if (configuration?.endpoint === "/") "" else "/${configuration?.endpoint}"
            route("/${configuration?.url}${endpoint}") {
                get {
                    val cookies = call.request.cookies
                    validateCookie(call, cookies, data, "index")
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
     * @param entityPath The path to the entity being listed, used in the URL
     */
    protected fun exposeListView(data: MutableMap<String, Any>, entityPath: String) {
        application?.routing {
            route("/${configuration?.url}/${entityPath}/list") {
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

                        call.respond(MustacheContent(configuration?.customListTemplate ?: defaultListTemplate, data))
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
     * This method creates a POST route for adding new records to a table, using a custom
     * template (or default template if none provided).
     *
     * @param data Map of data to be passed to the template
     * @param entityPath The path to the entity being created, used in the URL
     */
    protected fun exposeCreateView(data: MutableMap<String, Any?>, entityPath: String) {
        application?.routing {
            route("/${configuration?.url}/${entityPath}/new") {
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
                        call.respond(
                            MustacheContent(
                                configuration?.customCreateTemplate ?: defaultCreateTemplate,
                                data
                            )
                        )
                    } else {
                        val loginUrl = "/${configuration?.url}/login"
                        call.respondRedirect(loginUrl)
                    }
                }

                post {
                    val params = call.receiveParameters()
                    val dataToSave = mutableMapOf<String, Any>()
                    var id: Int? = null
                    if (ormType == "Exposed") {
                        (entityClass as IntEntityClass<IntEntity>).table.columns.forEach { column ->
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
                        id = if (instance is IntEntity) {
                            instance.id.value
                        } else {
                            throw IllegalStateException("Saved instance is not an IntEntity")
                        }
                    }
                    successMessage = "Instance created successfully with ID: $id"
                    call.respondRedirect("/${configuration?.url}/$entityPath/list")
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
     * @param entityPath The path to the entity being updated, used in the URL
     */
    fun exposeDetailsView(data: MutableMap<String, Any?>, entityPath: String) {
        application?.routing {
            route("/${configuration?.url}/${entityPath}/edit/{id}") {
                get {
                    val cookies = call.request.cookies
                    val sessionId = cookies["session_id"]

                    if (sessionId != null) {
                        val idValue =
                            call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("ID parameter is required")
                        val entity = dao!!.findById(idValue, entityClass::class)
                        val obj = if (ormType == "Exposed") {
                            (entityClass as IntEntityClass<IntEntity>).table.columns.associate { column ->
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
                        call.respond(
                            MustacheContent(
                                configuration?.customDetailsTemplate ?: defaultDetailsTemplate,
                                data
                            )
                        )
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
     * @param entityPath The path to the entity being deleted, used in the URL
     */
    fun exposeDeleteView(data: MutableMap<String, Any?>, entityPath: String) {
        application?.routing {
            route("/${configuration?.url}/${entityPath}/delete/{id}") {
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
                        call.respond(
                            MustacheContent(
                                configuration?.customDeleteTemplate ?: defaultDeleteTemplate,
                                data
                            )
                        )
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
 * A specialised view for database entity administration.
 *
 * EntityView is a concrete implementation of [BaseView] that provides standard admin panel
 * functionality for database entitys. It inherits all the capabilities of BaseView including route
 * setup and template rendering for CRUD operations.
 *
 * This class can be extended to customise admin behavior for specific entity classes, or used directly for
 * standard database administration needs.
 */
class EntityView<T : Any>(val entityClass: T) : BaseView<T>(entityClass) {
    /**
     * Sets up all the admin panel views and routes.
     *
     * This method initialises the necessary properties and calls the individual expose methods to
     * set up routes for different admin panel views.
     *
     * @param application The Ktor application instance for setting up routes
     * @param configuration Configuration settings for the admin panel
     * @param tableNames List of table names to be managed in the admin panel
     * @param entityCompanions Optional list of pairs containing entity classes for the entity
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
            super.ormType = "Exposed"
            ExposedDao(database, entityCompanions)
        } else if (entityManagerFactory != null) {
            super.ormType = "JPA"
            JpaDao(entityManagerFactory)
        } else {
            throw IllegalArgumentException("Either database or entityManagerFactory must be provided")
        }

        if (configuration.setAuthentication) {
            runBlocking {
                // Check if the admin user already exists
                // and create it if it doesn't
                val existingAdminUser: Any?
                val hashedPassword = BCrypt.hashpw(configuration.adminPassword, BCrypt.gensalt())
                if (super.ormType == "Exposed") {
                    // Create the AdminUser table if it doesn't exist
                    this@EntityView.dao!!.createTable(AdminUser::class)
                    existingAdminUser = this@EntityView.dao!!.find(
                        configuration.adminUsername,
                        AdminUser::class
                    )
                    if (existingAdminUser == null) {
                        val entity = mapOf("username" to configuration.adminUsername, "password" to hashedPassword)
                        this@EntityView.dao!!.save(entity, AdminUser::class)
                    }
                } else {
                    // Create the JpaAdminUser table if it doesn't exist
                    this@EntityView.dao!!.createTable(JpaAdminUser::class)
                    existingAdminUser = this@EntityView.dao!!.find(configuration.adminUsername, JpaAdminUser::class)
                    if (existingAdminUser == null) {
                        val entity = JpaAdminUser(username = configuration.adminUsername, password = hashedPassword)
                        this@EntityView.dao!!.save(entity)
                    }
                }

                // Expose the authentication view
                this@EntityView.exposeLoginView(mutableMapOf("configuration" to configuration))
            }
        }

        this.exposeIndexView(
            mapOf(
                "tables" to tableNames.map { it.lowercase() },
                "configuration" to configuration
            )
        )

        // Determine the entity name based on the type of entity class provided
        val entity = when (super.ormType) {
            "Exposed" -> (this.entityClass as IntEntityClass<IntEntity>).table.tableName
            "JPA" -> this.entityClass::class.simpleName
                ?: throw IllegalArgumentException("Entity must have a simple name")

            else -> throw IllegalArgumentException("Entity must be an IntEntityClass or annotated with @Entity")
        }

        // Use the entity name to determine the path for delete, list, create, and details views
        val entityPath = when (super.ormType) {
            "Exposed" -> (this.entityClass as IntEntityClass<IntEntity>).table.tableName.lowercase()
            "JPA" -> this.entityClass::class.simpleName?.lowercase()
                ?: throw IllegalArgumentException("Entity must have a simple name")

            else -> throw IllegalArgumentException("Entity must be an IntEntityClass or annotated with @Entity")
        }

        this.exposeDeleteView(
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
                mutableMapOf(
                    "configuration" to configuration,
                    "tableName" to table,
                    "tableNameLowercased" to table.lowercase(),
                ),
                entityPath = table.lowercase()
            )
            this.exposeCreateView(
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
                mutableMapOf(
                    "entity" to table,
                    "configuration" to configuration,
                    "entityPath" to table.lowercase()
                ),
                entityPath = table.lowercase()
            )
        }
    }
}
