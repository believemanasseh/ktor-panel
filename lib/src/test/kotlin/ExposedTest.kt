import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.daimones.ktor.panel.Admin
import xyz.daimones.ktor.panel.Configuration
import xyz.daimones.ktor.panel.ModelView
import xyz.daimones.ktor.panel.database.entities.AdminUser
import xyz.daimones.ktor.panel.database.entities.AdminUsers
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ExposedTest {
    private lateinit var database: Database

    @BeforeTest
    fun setup() {
        database = Database.connect(url = "jdbc:h2:mem:exposed_test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction(database) { SchemaUtils.create(AdminUsers) }
    }

    @Test
    fun testAdminInit() = testApplication {
        application {
            val configuration = Configuration(setAuthentication = false)
            val admin = Admin(this, configuration, database)
            admin.addView(ModelView(AdminUser))
            assertEquals(1, admin.countModelViews(), "Admin should have one model view registered")
        }
    }
}
