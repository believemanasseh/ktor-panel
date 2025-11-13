package xyz.daimones.ktor.panel

import com.github.mustachejava.DefaultMustacheFactory
import io.ktor.server.application.*
import io.ktor.server.mustache.*
import jakarta.persistence.EntityManagerFactory
import org.jetbrains.exposed.sql.Table
import xyz.daimones.ktor.panel.database.EntityName
import java.io.Reader
import kotlin.reflect.full.findAnnotation
import jakarta.persistence.Table as JpaTable

/**
 * Base admin class for the Ktor Panel library.
 *
 * This class serves as the main entry point for the admin panel functionality.
 * It manages the registration and rendering of entity views and handles the overall
 * admin panel configuration.
 *
 * @property application Ktor application instance used to set up routes and serve content
 * @property configuration General admin configuration containing settings like URL paths and names
 * @property database Optional application database instance for data access and table information
 * @property entityManagerFactory Optional JPA EntityManagerFactory for database operations.
 */
class Admin(
    private val application: Application,
    private val configuration: Configuration,
    private val database: Any? = null,
    private val entityManagerFactory: EntityManagerFactory? = null
) {
    /**
     * Collection of entity views registered with this admin panel.
     * Each view represents a database table that will be managed through the admin interface.
     */
    private val entityViews: MutableList<EntityView<*>> = mutableListOf()

    /**
     * List of table names retrieved from the database.
     * Used for generating navigation and displaying available tables in the admin interface.
     */
    private val tableNames: MutableList<String> = mutableListOf()

    /**
     * Initialises the admin panel by checking if the Mustache plugin is installed.
     * If not, it installs the Mustache plugin with a custom factory that supports multiple template roots.
     *
     * This setup allows the admin panel to render views using Mustache templates located in specified directories.
     */
    init {
        if (application.pluginOrNull(Mustache) == null) {
            // If Mustache plugin is not installed, install it with a custom factory
            application.install(Mustache) {
                mustacheFactory = MultiRootMustacheFactory(
                    listOf(
                        "panel_templates",
                        "templates"
                    )
                )
            }
        }
    }

    /**
     * Returns the number of entity views registered with this admin panel.
     *
     * This method provides a way to check how many models are currently being
     * managed by the admin panel. Useful for diagnostics and testing.
     *
     * @return The count of EntityView instances added to this admin panel
     */
    fun countEntityViews(): Int {
        return this.entityViews.size
    }

    /**
     * Adds a single entity view to the admin panel.
     *
     * This method registers an entity view with the admin panel and immediately renders
     * its associated pages by calling [EntityView.configurePageViews].
     *
     * @param view EntityView instance to be added to the admin panel
     */
    fun addView(view: EntityView<*>) {
        val tableName = getTableName(view)
        this.entityViews.add(view)
        this.tableNames.add(tableName)
        view.configurePageViews(
            application = this.application,
            configuration = this.configuration,
            tableNames = this.tableNames,
            currentTableName = tableName,
            database = this.database,
            entityManagerFactory = this.entityManagerFactory
        )
        view.clear()
    }

    /**
     * Adds multiple entity views to the admin panel.
     *
     * This method iteratively adds each view in the provided list by calling [EntityView.configurePageViews]
     * for each element.
     *
     * @param views Array containing entity views to be added to the admin panel
     */
    fun addViews(views: Array<EntityView<*>>) {
        for (view in views) {
            val tableName = getTableName(view)
            this.entityViews.add(view)
            this.tableNames.add(tableName)
        }
        for ((index, view) in views.withIndex()) {
            view.configurePageViews(
                application = this.application,
                configuration = this.configuration,
                tableNames = this.tableNames,
                currentTableName = this.tableNames[index],
                database = this.database,
                entityManagerFactory = this.entityManagerFactory
            )
            view.clear()
        }
    }

    /**
     * Retrieves the table name associated with the given entity view.
     *
     * This method determines the table name based on the type of database being used
     * (by checking the class of the provided database or entity manager factory).
     *
     * @param view The EntityView instance for which to retrieve the table name
     * @return The name of the table associated with the entity view
     * @throws IllegalArgumentException if neither database nor entityManagerFactory is provided
     */
    private fun getTableName(view: EntityView<*>): String {
        val entityKClass = view.entityKClass
        val tableName = if (database != null && database::class.qualifiedName == "org.jetbrains.exposed.sql.Database") {
            (entityKClass.objectInstance as Table).tableName
        } else if (database != null && database::class.qualifiedName == "com.mongodb.kotlin.client.coroutine.MongoDatabase") {
            val collectionNameAnnotation = entityKClass.findAnnotation<EntityName>()
            collectionNameAnnotation?.name ?: entityKClass.simpleName.toString()
        } else if (entityManagerFactory != null && entityManagerFactory::class.qualifiedName == "org.hibernate.internal.SessionFactoryImpl") {
            val tableAnnotation = entityKClass.findAnnotation<JpaTable>()
            tableAnnotation?.name ?: entityKClass.simpleName.toString()
        } else {
            throw IllegalArgumentException("Database, MongoDatabase or EntityManagerFactory must be provided to add an EntityView")
        }
        return tableName
    }
}

/**
 * Custom Mustache factory that allows loading templates from multiple root directories.
 *
 * This factory extends the DefaultMustacheFactory to support multiple template roots,
 * enabling the application to search for templates in several locations.
 *
 * @property roots List of root directories where templates can be found
 */
internal class MultiRootMustacheFactory(private val roots: List<String>) : DefaultMustacheFactory() {
    override fun getReader(resourceName: String): Reader {
        for (root in roots) {
            val stream = this.javaClass.classLoader.getResourceAsStream("$root/$resourceName")
            if (stream != null) {
                return stream.reader()
            }
        }
        throw java.io.FileNotFoundException("Template $resourceName not found in $roots")
    }
}

