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
    private val indexView = "index.hbs"
    private val listView = "list.hbs"
    private val createView = "create.hbs"
    private val detailsView = "details.hbs"
    private val updateView = "update.hbs"

    fun renderIndexView(data: Map<String, Any>) {
        application.routing {
            route("/") {
                get {
                    call.respond(MustacheContent(indexView, data))
                }
            }
        }
    }

    fun renderListView(data: Map<String, Any>) {
        application.routing {
            route("/list") {
                get {
                    call.respond(MustacheContent(listView, data))
                }
            }
        }
    }

    fun renderCreateView(data: Map<String, Any>) {
        application.routing {
            route("/new") {
                post {
                    call.respond(MustacheContent(createView, data))
                }
            }
        }
    }

    fun renderDetailsView(data: Map<String, Any>) {
        application.routing {
            route("/details") {
                get {
                    call.respond(MustacheContent(detailsView, data))
                }
            }
        }
    }

    fun renderUpdateView(data: Map<String, Any>) {
        application.routing {
            route("/update") {
                get {
                    call.respond(MustacheContent(updateView, data))
                }
            }
        }
    }
}