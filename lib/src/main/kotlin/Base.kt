import com.administrativektor.Configuration
import io.ktor.server.application.*
import io.ktor.server.mustache.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.id.IntIdTable

class BaseView(
    val model: IntIdTable,
    val name: String,
    val url: String,
    private val application: Application,
    private val configuration: Configuration
) {
    fun renderIndexView(template: String, data: Map<String, Any>) {
        application.routing {
            route("/") {
                get {
                    call.respond(MustacheContent(template, data))
                }
            }
        }
    }

    fun renderCreateView(template: String, data: Map<String, Any>) {
        application.routing {
            route("/new") {
                post {
                    call.respond(MustacheContent(template, data))
                }
            }
        }
    }

    fun renderDetailsView(template: String, data: Map<String, Any>) {
        application.routing {
            route("/details") {
                get {
                    call.respond(MustacheContent(template, data))
                }
            }
        }
    }
}