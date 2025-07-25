import io.ktor.server.testing.*
import jakarta.persistence.EntityManagerFactory
import org.hibernate.jpa.HibernatePersistenceConfiguration
import org.hibernate.tool.schema.Action
import xyz.daimones.ktor.panel.Admin
import xyz.daimones.ktor.panel.Configuration
import xyz.daimones.ktor.panel.EntityView
import xyz.daimones.ktor.panel.database.entities.JpaAdminUser
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JpaTest {
    private lateinit var entityManagerFactory: EntityManagerFactory

    @BeforeTest
    fun setup() {
        entityManagerFactory =
            HibernatePersistenceConfiguration("AdminUsers")
                .managedClass(JpaAdminUser::class.java)
                .jdbcUrl("jdbc:h2:mem:hibernate_example;DB_CLOSE_DELAY=-1")
                .jdbcDriver("org.h2.Driver")
                .jdbcCredentials("sa", "")
                .schemaToolingAction(Action.CREATE_DROP)
                .jdbcPoolSize(16)
                .showSql(true, true, true)
                .createEntityManagerFactory()
    }

    @Test
    fun testAdminInit() = testApplication {
        application {
            val configuration = Configuration(setAuthentication = false)
            val admin =
                Admin(application = this, configuration = configuration, entityManagerFactory = entityManagerFactory)
            admin.addView(EntityView(JpaAdminUser::class))
            assertEquals(1, admin.countEntityViews(), "Admin should have one entity view registered")
        }
    }
}