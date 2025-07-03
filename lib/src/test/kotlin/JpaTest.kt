import io.ktor.server.testing.*
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Persistence
import org.h2.jdbcx.JdbcDataSource
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
        val dataSource = JdbcDataSource().apply {
            setURL("jdbc:h2:mem:jpa_test;DB_CLOSE_DELAY=-1")
            user = "sa"
            password = ""
        }
        val properties = mapOf(
            "jakarta.persistence.nonJtaDataSource" to dataSource
        )
        entityManagerFactory = Persistence.createEntityManagerFactory("ktor-panel-test", properties)
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