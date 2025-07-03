package xyz.daimones.ktor.panel

import io.ktor.server.application.*
import jakarta.persistence.Entity
import jakarta.persistence.EntityManagerFactory
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.Database
import kotlin.reflect.KClass

/**
 * Base admin class for the Ktor Panel library.
 *
 * This class serves as the main entry point for the admin panel functionality.
 * It manages the registration and rendering of model views and handles the overall
 * admin panel configuration.
 *
 * @property application Ktor application instance used to set up routes and serve content
 * @property database Application database instance for data access and table information
 * @property configuration General admin configuration containing settings like URL paths and names
 */
class Admin(
    private val application: Application,
    private val database: Database,
    private val configuration: Configuration
) {
    /**
     * Collection of model views registered with this admin panel.
     * Each view represents a database table that will be managed through the admin interface.
     */
    private val modelViews: MutableList<ModelView<*>> = mutableListOf()

    /**
     * List of table names retrieved from the database.
     * Used for generating navigation and displaying available tables in the admin interface.
     */
    private val tableNames: MutableList<String> = mutableListOf()

    /**
     * List of entity classes registered with this admin panel.
     * Each pair contains the KClass of the entity and its corresponding IntEntityClass.
     * This is used for type-safe database operations and model management.
     */
    private val entityCompanions: MutableList<Pair<KClass<out IntEntityClass<IntEntity>>, IntEntityClass<IntEntity>>> =
        mutableListOf()

    /**
     * Returns the number of model views registered with this admin panel.
     *
     * This method provides a way to check how many models are currently being
     * managed by the admin panel. Useful for diagnostics and testing.
     *
     * @return The count of ModelView instances added to this admin panel
     */
    fun countModelViews(): Int {
        return this.modelViews.size
    }

    /**
     * Adds a single model view to the admin panel.
     *
     * This method registers a model view with the admin panel and immediately renders
     * its associated pages by calling [ModelView.configurePageViews].
     *
     * @param view ModelView instance to be added to the admin panel
     */
    fun addView(view: ModelView<*>) {
        val tableName = if (view.entityClass is IntEntityClass<IntEntity>) {
            view.entityClass.table.tableName
        } else if (view.entityClass::class.annotations.any { it is Entity }) {
            view.entityClass::class.simpleName.toString()
        } else {
            throw IllegalArgumentException("Model must be an IntEntity or annotated with @Entity")
        }
        this.tableNames.add(tableName)
        this.modelViews.add(view)

        if (view.entityClass is IntEntityClass<IntEntity>) {
            // If the model is an IntEntityClass, we can safely add it to the entity class pairs.
            @Suppress("UNCHECKED_CAST")
            this.entityCompanions.add(
                Pair(
                    view.entityClass::class as KClass<IntEntityClass<IntEntity>>,
                    view.entityClass
                )
            )
        }

        view.configurePageViews(
            database = this.database,
            application = this.application,
            configuration = this.configuration,
            tableNames = this.tableNames,
            entityCompanions = if (view.entityClass is IntEntityClass<IntEntity>) this.entityCompanions else null,
            entityManagerFactory = if (this.configuration.entityManagerFactory is EntityManagerFactory) this.configuration.entityManagerFactory else null
        )
    }

    /**
     * Adds multiple model views to the admin panel.
     *
     * This method iteratively adds each view in the provided list by calling [addView]
     * for each element.
     *
     * @param views Array containing model views to be added to the admin panel
     */
    fun addViews(views: Array<ModelView<*>>) {
        for (view in views) {
            this.addView(view)
        }
    }
}

