import io.ktor.server.application.*
import io.ktor.server.mustache.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.id.IntIdTable

class BaseView(
    val model: IntIdTable,
    val name: String,
    val url: String,
    val endpoint: String,
    private val application: Application,
    private val configuration: Configuration
) {
    private val defaultIndexView = "index.hbs"
    private val defaultListView = "list.hbs"
    private val defaultCreateView = "create.hbs"
    private val defaultDetailsView = "details.hbs"
    private val defaultUpdateView = "update.hbs"

    fun renderIndexView(data: Map<String, Any>, template: String? = null) {
        application.routing {
            route("/") {
                get {
                    call.respond(MustacheContent(template ?: defaultIndexView, data))
                }
            }
        }
    }

    fun renderListView(data: Map<String, Any>, template: String? = null) {
        application.routing {
            route("/list") {
                get {
                    call.respond(MustacheContent(template ?: defaultListView, data))
                }
            }
        }
    }

    fun renderCreateView(data: Map<String, Any>, template: String? = null) {
        application.routing {
            route("/new") {
                post {
                    call.respond(MustacheContent(template ?: defaultCreateView, data))
                }
            }
        }
    }

    fun renderDetailsView(data: Map<String, Any>, template: String? = null) {
        application.routing {
            route("/details") {
                get {
                    call.respond(MustacheContent(template ?: defaultDetailsView, data))
                }
            }
        }
    }

    fun renderUpdateView(data: Map<String, Any>, template: String? = null) {
        application.routing {
            route("/update") {
                get {
                    call.respond(MustacheContent(template ?: defaultUpdateView, data))
                }
            }
        }
    }
}