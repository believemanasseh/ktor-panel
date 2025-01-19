import database.retrieveTableNames
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

/**
 * Base admin class for library.
 *
 * @property application Ktor application instance.
 * @property database Application database instance
 * @property configuration Admin configuration
 */
class Admin(
    private val application: Application,
    private val database: Database,
    private val configuration: Configuration
) {
    private val tableNames: List<String> = retrieveTableNames(database)
    private val modelViews: MutableList<BaseView> = mutableListOf()
    private val registeredModels: MutableList<BaseView> = mutableListOf()

    /**
     * Adds model view.
     *
     * @property view BaseView instance to be added
     * @return Nothing
     */
    fun addView(view: BaseView) {
        this.modelViews.add(view)
        this.registerRoutes()
    }

    /**
     * Adds multiple model views.
     *
     * @property views List containing model views
     * @return Nothing
     */
    fun addViews(views: MutableList<BaseView>) {
        for (view in views) {
            this.addView(view)
        }
    }

    /**
     * Exposes database model endpoints
     * @return Nothing
     */
    private fun registerRoutes() {
        for (view in this.modelViews) {
            if (view !in this.registeredModels) {
                view.renderIndexView(mapOf("tables" to this.tableNames, "adminName" to this.configuration.adminName))
                view.renderListView(mapOf("tables" to this.tableNames))
                view.renderCreateView(mapOf("model" to view.model))
                view.renderDetailsView(mapOf("model" to view.model))
                view.renderUpdateView(mapOf("model" to view.model))
            }
        }
    }
}