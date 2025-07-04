import io.ktor.server.testing.*
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Persistence
import xyz.daimones.ktor.panel.Admin
import xyz.daimones.ktor.panel.Configuration
import xyz.daimones.ktor.panel.ModelView
import xyz.daimones.ktor.panel.database.entities.JpaAdminUser
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JpaTest {
    private lateinit var entityManagerFactory: EntityManagerFactory

    @BeforeTest
    fun setup() {
        entityManagerFactory = Persistence.createEntityManagerFactory("ktor-panel-test")
    }

    @Test
    fun testAdminInit() = testApplication {
        application {
            val configuration = Configuration(setAuthentication = false)
            val admin =
                Admin(application = this, configuration = configuration, entityManagerFactory = entityManagerFactory)
            admin.addView(ModelView(JpaAdminUser()))
            assertEquals(1, admin.countModelViews(), "Admin should have one model view registered")
        }
    }
}