package xyz.daimones.ktor.admin

import io.ktor.server.application.*
import io.ktor.server.mustache.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.JavaInstantColumnType
import org.jetbrains.exposed.sql.javatime.JavaLocalDateColumnType
import org.jetbrains.exposed.sql.javatime.JavaLocalDateTimeColumnType
import org.jetbrains.exposed.sql.json.JsonBColumnType
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import xyz.daimones.ktor.admin.database.DatabaseAccessObjectInterface
import xyz.daimones.ktor.admin.database.dao.ExposedDao
import java.time.LocalDateTime


/**
 * Manages base view rendering for admin panel pages.
 *
 * This class is responsible for setting up routes and rendering templates for the admin panel.
 * It provides functionality for index, list, create, details, and update views.
 *
 * @property model The database table model to generate admin views for
 */
open class BaseView(private val model: IntIdTable) {
    /**
     * Configuration for the admin panel.
     * This is set during [ModelView.renderPageViews] and contains settings like URL, endpoint, and admin name.
     */
    protected var configuration: Configuration? = null

    /**
     * The application instance for setting up routes.
     * Set during [ModelView.renderPageViews] and used by the expose* methods.
     */
    protected var application: Application? = null

    /**
     * The database instance for data access.
     * This is set during [ModelView.renderPageViews] and used by the ExposedDao for database operations.
     */
    protected var database: Database? = null

    /**
     * The data access object interface for database operations.
     * This is set during [ModelView.renderPageViews] and used to interact with the database.
     */
    protected var dao: DatabaseAccessObjectInterface? = null

    /**
     * Default Mustache template for the index page.
     */
    private val defaultIndexView = "kt-admin-index.hbs"

    /**
     * Default Mustache template for the list page.
     */
    private val defaultListView = "kt-admin-list.hbs"

    /**
     * Default Mustache template for the create page.
     */
    private val defaultCreateView = "kt-admin-create.hbs"

    /**
     * Default Mustache template for the details page.
     */
    private val defaultDetailsView = "kt-admin-details.hbs"

    /**
     * List of headers for the model's columns.
     * This is used to render table headers in the list view.
     */
    private val headers = model.columns.map { it.name }

    /**
     * Success message to be displayed after creating or updating an instance.
     * This is set during the create or update operations and can be used in templates.
     */
    private var successMessage: String? = null

    /**
     * Retrieves the column types of the model for rendering in templates.
     *
     * This method maps each column in the model to its HTML input type and original type,
     * which is useful for generating forms and input fields dynamically.
     *
     * @param model The IntIdTable model whose columns are to be inspected
     * @return A list of maps containing column names, HTML input types, and original types
     */
    private fun getColumnTypes(model: IntIdTable): List<Map<String, String>> {
        return model.columns.map { column ->
            val htmlInputType = getHtmlInputType(column)
            mapOf(
                "name" to column.name,
                "html_input_type" to htmlInputType,
                "original_type" to column.columnType::class.simpleName.orEmpty()
            )
        }
    }

    /**
     * Determines the HTML input type for a given column.
     *
     * This function maps Exposed column types to appropriate HTML input types
     * for rendering forms in the admin panel.
     *
     * @param column The column for which to determine the HTML input type
     * @return A string representing the HTML input type (e.g., "text", "number", "checkbox", etc.)
     */
    private fun getHtmlInputType(column: Column<*>): String {
        return when (column.columnType) {
            is EntityIDColumnType<*> -> "number"
            is VarCharColumnType, is TextColumnType, is CharacterColumnType -> {
                if (column.name.contains("password", ignoreCase = true)) "password"
                else "text"
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
    }

    /**
     * Retrieves all table data values for the model.
     *
     * This method queries the database for all records in the specified model
     * and returns a list of maps containing the ID and column values for each record.
     *
     * @return A list of maps where each map represents a record with its ID and column values
     */
    private fun getTableDataValues(): List<Map<String, Any>?> {
        return dao!!.findAll(model) { resultRow ->
            val idValue = resultRow[model.id].value
            val columnValues = model.columns.map { column ->
                resultRow[column].let { value ->
                    if (value != null) {
                        when (value) {
                            is EntityID<*> -> value.value
                            is IntIdTable -> value.id
                            else -> value
                        }
                    } else {
                        ""
                    }

                }
            }
            mapOf("id" to idValue, "nums" to columnValues)
        }
    }

    /**
     * Sets up the route for the index view.
     *
     * This method creates a GET route for the admin panel's main page that renders
     * using the specified template (or default template if none provided).
     *
     * @param data Map of data to be passed to the template
     * @param template Optional custom template name, if null the default template is used
     */
    protected fun exposeIndexView(data: Map<String, Any>, template: String? = null) {
        application?.routing {
            val endpoint = if (configuration?.endpoint === "/") "" else "/${configuration?.endpoint}"
            route("/${configuration?.url}${endpoint}") {
                get {
                    call.respond(MustacheContent(template ?: defaultIndexView, data))
                }
            }
        }
    }

    /**
     * Sets up the route for the list view that displays table records.
     *
     * This method creates a GET route for viewing all records in a table,
     * using the specified template (or default template if none provided).
     *
     * @param data Map of data to be passed to the template
     * @param template Optional custom template name, if null the default template is used
     * @param modelPath The path to the model being listed, used in the URL
     */
    protected fun exposeListView(data: MutableMap<String, Any>, template: String? = null, modelPath: String) {
        application?.routing {
            route("/${configuration?.url}/${modelPath}/list") {
                get {
                    val headers = model.columns.map { it.name }
                    val tableDataValues = getTableDataValues()
                    val tablesData = mapOf(
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
                }
            }
        }
    }

    /**
     * Sets up the route for the create view that allows adding new records.
     *
     * This method creates a POST route for adding new records to a table,
     * using the specified template (or default template if none provided).
     *
     * @param data Map of data to be passed to the template
     * @param template Optional custom template name, if null the default template is used
     * @param modelPath The path to the model being created, used in the URL
     */
    protected fun exposeCreateView(data: MutableMap<String, Any?>, template: String? = null, modelPath: String) {
        application?.routing {
            route("/${configuration?.url}/${modelPath}/new") {
                val tableDataValues = getTableDataValues()
                val tablesData = mapOf(
                    "headers" to headers,
                    "data" to mapOf("values" to tableDataValues)
                )
                data["tablesData"] = tablesData

                get {
                    val columnTypes = getColumnTypes(model)
                    val fieldsForTemplate = columnTypes.map { props ->
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
                            "is_general_input" to !listOf(
                                "checkbox",
                                "select",
                                "textarea",
                                "hidden"
                            ).contains(inputType)
                            // TODO: For "is_select", we need to add an "options" list to this map
                            // e.g., "options" to listOf(mapOf("value" to "opt1", "text" to "Option 1", "selected" to true/false))
                        )
                    }
                    data["fields"] = fieldsForTemplate
                    call.respond(MustacheContent(template ?: defaultCreateView, data))
                }

                post {
                    val params = call.receiveParameters()
                    val id = dao!!.save(model) { builder ->
                        model.columns.forEach { column ->
                            val columnName = column.name
                            val value: Any? = when (column.columnType) {
                                is EntityIDColumnType<*> -> params[columnName]?.toIntOrNull()
                                    ?.let { EntityID(it, model) }

                                is BooleanColumnType -> params[columnName]?.toBoolean()
                                is IntegerColumnType -> params[columnName]?.toIntOrNull()
                                is LongColumnType -> params[columnName]?.toLongOrNull()
                                is DecimalColumnType -> params[columnName]?.toBigDecimalOrNull()
                                is JavaLocalDateTimeColumnType -> params[columnName]?.let { LocalDateTime.parse(it) }
                                else -> params[columnName]
                            }

                            if (value != null) {
                                @Suppress("UNCHECKED_CAST")
                                (builder as UpdateBuilder<Any>)[column as Column<Any>] = value
                            }
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
     * This method creates a GET route for editing existing records in a table,
     * using the specified template (or default template if none provided).
     *
     * @param data Map of data to be passed to the template
     * @param template Optional custom template name, if null the default template is used
     * @param modelPath The path to the model being updated, used in the URL
     */
    fun exposeDetailsView(data: MutableMap<String, Any?>, template: String? = null, modelPath: String) {
        application?.routing {
            route("/${configuration?.url}/${modelPath}/edit/{id}") {
                get {
                    val idValue = call.parameters["id"]
                    val obj = dao!!.findById(idValue?.toInt() ?: 0, model) { resultRow ->
                        model.columns.associate { column ->
                            val actualValue = resultRow[column].let { value ->
                                // Exposed stores IDs as EntityID, so we extract the actual value.
                                if (value is EntityID<*>) value.value else value
                            }

                            val htmlInputType = getHtmlInputType(column)
                            column.name to mapOf(
                                "value" to actualValue,
                                "html_input_type" to htmlInputType,
                                "original_type" to column.columnType::class.simpleName
                            )
                        }
                    }

                    val fieldsForTemplate = obj?.entries?.map { (name, props) ->
                        val inputType = props["html_input_type"] as String
                        val originalType = props["original_type"] as? String ?: ""
                        val isReadOnly = name.equals("id", ignoreCase = true) ||
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
                            "is_general_input" to !listOf(
                                "checkbox",
                                "select",
                                "textarea",
                                "hidden"
                            ).contains(inputType)
                            // TODO: For "is_select", we need to add an "options" list to this map
                            // e.g., "options" to listOf(mapOf("value" to "opt1", "text" to "Option 1", "selected" to true/false))
                        )
                    } ?: emptyList()

                    data["fields"] = fieldsForTemplate
                    data["idValue"] = idValue.toString()
                    data["object"] = obj
                    call.respond(MustacheContent(template ?: defaultDetailsView, data))
                }
            }
        }
    }
}


/**
 * A specialised view for database model administration.
 *
 * ModelView is a concrete implementation of [BaseView] that provides
 * standard admin panel functionality for database models. It inherits all the
 * capabilities of BaseView including route setup and template rendering for
 * CRUD operations.
 *
 * This class can be extended to customise admin behavior for specific models,
 * or used directly for standard database administration needs.
 */
class ModelView(val model: IntIdTable) : BaseView(model) {
    /**
     * Sets up all the admin panel views and routes.
     *
     * This method initialises the necessary properties and calls the individual
     * expose methods to set up routes for different admin panel views.
     *
     * @param database The database connection to be used for data access
     * @param application The Ktor application instance for setting up routes
     * @param configuration Configuration settings for the admin panel
     * @param tableNames List of table names to be managed in the admin panel
     */
    fun renderPageViews(
        database: Database,
        application: Application,
        configuration: Configuration,
        tableNames: List<String>
    ) {
        super.configuration = configuration
        super.application = application
        super.database = database
        super.dao = ExposedDao(database)

        exposeIndexView(
            mapOf(
                "tables" to tableNames.map { it.lowercase() },
                "configuration" to configuration
            )
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
                    "model" to table, "configuration" to configuration,
                    "tableName" to table,
                    "tableNameLowercased" to table.lowercase(),
                    "modelPath" to table.lowercase()
                ),
                modelPath = table.lowercase()
            )
            this.exposeDetailsView(
                mutableMapOf("model" to table, "configuration" to configuration, "modelPath" to table.lowercase()),
                modelPath = table.lowercase()
            )
        }
    }
}
