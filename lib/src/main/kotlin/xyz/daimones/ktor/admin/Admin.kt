package xyz.daimones.ktor.admin

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
     * Adds a single model view to the admin panel.
     *
     * This method registers a model view with the admin panel and immediately renders
     * its associated pages by calling [ModelView.renderPageViews].
     *
     * @param view ModelView instance to be added to the admin panel
     */
    fun addView(view: ModelView) {
        this.modelViews.add(view)
        view.renderPageViews(this.application, this.database, this.configuration)
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

