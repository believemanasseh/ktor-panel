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
    private lateinit var configuration: Configuration

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
        configuration = Configuration(setAuthentication = false)
    }

    @Test
    fun testAdminInit() = testApplication {
        application {
            val admin =
                Admin(application = this, configuration = configuration, entityManagerFactory = entityManagerFactory)
            admin.addView(EntityView(JpaAdminUser::class))
            assertEquals(1, admin.countEntityViews(), "Admin should have one entity view registered")
        }
    }

    @Test
    fun testGetTableName() = testApplication {
        application {
            val admin = Admin(this, configuration, entityManagerFactory = entityManagerFactory)
            val entityView = EntityView(JpaAdminUser::class)
            val tableName = admin.getTableName(entityView)
            assertEquals("admin_users", tableName, "Table name should match the entity's table name")
        }
    }
}