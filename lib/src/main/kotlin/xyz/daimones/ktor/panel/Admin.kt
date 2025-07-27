package xyz.daimones.ktor.panel

import com.github.mustachejava.DefaultMustacheFactory
import io.ktor.server.application.*
import io.ktor.server.mustache.*
import jakarta.persistence.Entity
import jakarta.persistence.EntityManagerFactory
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.Database
import java.io.Reader
import kotlin.reflect.full.companionObjectInstance

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
        val entityKClass = view.entityKClass
        val hasEntityAnnotation = entityKClass.annotations.any { it is Entity }
        val hasSerializableAnnotation = entityKClass.annotations.any { it is Serializable }

        val tableName = if (database is Database) {
            (entityKClass.companionObjectInstance as IntEntityClass<IntEntity>).table.tableName
        } else if (hasEntityAnnotation || hasSerializableAnnotation || entityKClass.isData) {
            entityKClass.simpleName.toString()
        } else {
            throw IllegalArgumentException("Entity must be an IntEntity, Data Class or annotated with @Entity or @Serializable")
        }
        this.tableNames.add(tableName)
        this.entityViews.add(view)

        view.configurePageViews(
            application = this.application,
            configuration = this.configuration,
            tableNames = this.tableNames,
            database = this.database,
            entityManagerFactory = this.entityManagerFactory
        )
    }

    /**
     * Adds multiple entity views to the admin panel.
     *
     * This method iteratively adds each view in the provided list by calling [addView]
     * for each element.
     *
     * @param views Array containing entity views to be added to the admin panel
     */
    fun addViews(views: Array<EntityView<*>>) {
        for (view in views) {
            this.addView(view)
        }
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

