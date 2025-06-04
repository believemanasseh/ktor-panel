package xyz.daimones.ktor.admin

import io.ktor.server.application.*
import io.ktor.server.mustache.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import xyz.daimones.ktor.admin.database.retrieveTableNames


/**
 * Manages base view rendering for admin panel pages.
 *
 * This class is responsible for setting up routes and rendering templates for the admin panel.
 * It provides functionality for index, list, create, details, and update views.
 *
 * @property model The database table model to generate admin views for
 */
open class BaseView(private val model: IntIdTable? = null) {

    /**
     * Configuration for the admin panel.
     * This is set during [renderPageViews] and contains settings like URL, endpoint, and admin name.
     */
    private var configuration: Configuration? = null

    /**
     * List of table names retrieved from the database.
     * Used for generating navigation and displaying available tables in the admin interface.
     */
    private var tableNames: List<String> = emptyList()

    /**
     * The application instance for setting up routes.
     * Set during [renderPageViews] and used by the expose* methods.
     */
    private var application: Application? = null

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
     * Default Mustache template for the update page.
     */
    private val defaultUpdateView = "kt-admin-update.hbs"

    /**
     * Sets up all the admin panel views and routes.
     *
     * This method initialises the necessary properties and calls the individual
     * expose methods to set up routes for different admin panel views.
     *
     * @param application The Ktor application instance for setting up routes
     * @param database The database connection for retrieving table information
     * @param configuration Configuration settings for the admin panel
     */
    fun renderPageViews(application: Application, database: Database, configuration: Configuration) {
        this.configuration = configuration
        this.tableNames = retrieveTableNames(database)
        this.application = application

        exposeIndexView(mapOf("tables" to tableNames, "adminName" to configuration.adminName))
        exposeListView(mapOf("tables" to this.tableNames))
        exposeCreateView(mapOf("model" to model?.tableName))
        exposeDetailsView(mapOf("model" to model?.tableName))
        exposeUpdateView(mapOf("model" to model?.tableName))
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
    private fun exposeIndexView(data: Map<String, Any>, template: String? = null) {
        application?.routing {
            val endpoint = if (configuration?.endpoint === "/") "" else configuration?.endpoint
            route("/${configuration?.url}/${endpoint}") {
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
     */
    private fun exposeListView(data: Map<String, Any>, template: String? = null) {
        application?.routing {
            route("/${configuration?.url}/list") {
                get {
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
     */
    private fun exposeCreateView(data: Map<String, Any?>, template: String? = null) {
        application?.routing {
            route("/${configuration?.url}/new") {
                post {
                    call.respond(MustacheContent(template ?: defaultCreateView, data))
                }
            }
        }
    }

    /**
     * Sets up the route for the details view that displays a single record.
     *
     * This method creates a GET route for viewing details of a specific record,
     * using the specified template (or default template if none provided).
     *
     * @param data Map of data to be passed to the template
     * @param template Optional custom template name, if null the default template is used
     */
    private fun exposeDetailsView(data: Map<String, Any?>, template: String? = null) {
        application?.routing {
            route("/${configuration?.url}/details") {
                get {
                    call.respond(MustacheContent(template ?: defaultDetailsView, data))
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
     */
    private fun exposeUpdateView(data: Map<String, Any?>, template: String? = null) {
        application?.routing {
            route("/${configuration?.url}/update") {
                get {
                    call.respond(MustacheContent(template ?: defaultUpdateView, data))
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
class ModelView : BaseView()

