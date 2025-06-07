package xyz.daimones.ktor.panel

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

/**
 * Base admin class for the Ktor admin panel library.
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
    private val modelViews: MutableList<ModelView> = mutableListOf()

    /**
     * List of table names retrieved from the database.
     * Used for generating navigation and displaying available tables in the admin interface.
     */
    private val tableNames: MutableList<String> = mutableListOf()

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
     * its associated pages by calling [ModelView.renderPageViews].
     *
     * @param view ModelView instance to be added to the admin panel
     */
    fun addView(view: ModelView) {
        this.tableNames.add(view.model.tableName)
        this.modelViews.add(view)
        view.renderPageViews(this.database, this.application, this.configuration, this.tableNames)
    }

    /**
     * Adds multiple model views to the admin panel.
     *
     * This method iteratively adds each view in the provided list by calling [addView]
     * for each element.
     *
     * @param views List containing model views to be added to the admin panel
     */
    fun addViews(views: MutableList<ModelView>) {
        for (view in views) {
            this.addView(view)
        }
    }
}

