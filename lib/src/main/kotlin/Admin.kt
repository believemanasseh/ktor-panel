import io.ktor.server.application.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database


class Admin(private val application: Application, private val database: Database, val configuration: Configuration) {
    private val modelViews: MutableList<BaseView> = mutableListOf()
    private val models: MutableList<IntIdTable> = mutableListOf()
    private val registeredModels: MutableList<BaseView> = mutableListOf()

    /**
     * Adds model view.
     *
     * @property view BaseView instance to be added
     * @return Nothing
     */
    fun addView(view: BaseView) {
        this.models.add(view.model)
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
                view.renderIndexView(mapOf("models" to this.models, "adminName" to this.configuration.adminName))
                view.renderListView(mapOf("models" to this.models))
                view.renderCreateView(mapOf("model" to view.model))
                view.renderDetailsView(mapOf("model" to view.model))
                view.renderUpdateView(mapOf("model" to view.model))
            }
        }
    }
}