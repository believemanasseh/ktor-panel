import io.ktor.server.testing.*
import jakarta.persistence.EntityManagerFactory
import kotlinx.coroutines.runBlocking
import org.hibernate.jpa.HibernatePersistenceConfiguration
import org.hibernate.tool.schema.Action
import org.mindrot.jbcrypt.BCrypt
import xyz.daimones.ktor.panel.Admin
import xyz.daimones.ktor.panel.Configuration
import xyz.daimones.ktor.panel.EntityView
import xyz.daimones.ktor.panel.database.dao.JpaDao
import xyz.daimones.ktor.panel.database.entities.JpaAdminUser
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.full.memberProperties
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JpaTest {
    private lateinit var entityManagerFactory: EntityManagerFactory
    private lateinit var configuration: Configuration
    private lateinit var dao: JpaDao<JpaAdminUser>

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

    @Test
    fun testDaoFindById() = testApplication {
        dao = JpaDao(entityManagerFactory, JpaAdminUser::class)
        createUser()
        val entity = dao.findById(1)
        JpaAdminUser::class.memberProperties.forEach { property ->
            val value = property.call(entity)
            var actualValue = if (value is LocalDateTime) {
                value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
            } else {
                value
            }

            if (property.name == "id") {
                assertEquals(1, actualValue.toString().toInt(), "ID should be 1")
            }

            if (property.name == "username") {
                assertEquals(
                    configuration.adminUsername,
                    actualValue,
                    "Username should match the created user"
                )
            }

            if (property.name == "password") {
                assert(BCrypt.checkpw(configuration.adminPassword, actualValue.toString())) {
                    "Password should match the created user's password"
                }
            }
        }
    }

    fun createUser() {
        runBlocking {
            dao.createTable()
            val hashedPassword = BCrypt.hashpw(configuration.adminPassword, BCrypt.gensalt())
            val entity = JpaAdminUser(username = configuration.adminUsername, password = hashedPassword)
            dao.save(entity)
        }
    }
}
