import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import xyz.daimones.ktor.panel.Admin
import xyz.daimones.ktor.panel.Configuration
import xyz.daimones.ktor.panel.EntityView
import xyz.daimones.ktor.panel.database.dao.ExposedDao
import xyz.daimones.ktor.panel.database.entities.AdminUsers
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ExposedTest {
    private lateinit var database: Database
    private lateinit var configuration: Configuration
    private lateinit var dao: ExposedDao<*>

    @BeforeTest
    fun setup() {
        database = Database.connect(url = "jdbc:h2:mem:exposed_test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction(database) { SchemaUtils.create(AdminUsers) }
        configuration = Configuration(setAuthentication = false)
    }

    @Test
    fun testAdminInit() = testApplication {
        application {
            val admin = Admin(this, configuration, database)
            admin.addView(EntityView(AdminUsers::class))
            assertEquals(1, admin.countEntityViews(), "Admin should have one entity view registered")
        }
    }

    @Test
    fun testGetTableName() = testApplication {
        application {
            val admin = Admin(this, configuration, database)
            val entityView = EntityView(AdminUsers::class)
            val tableName = admin.getTableName(entityView)
            assertEquals("admin_users", tableName, "Table name should match the entity's table name")
        }
    }

    @Test
    fun testDaoFindById() = testApplication {
        dao = ExposedDao(database, AdminUsers::class)
        createUser()
        val entity = dao.findById(1, true)
        println("Entity: $entity")
        (AdminUsers::class.objectInstance as Table).columns.forEach { column ->
            @Suppress("UNCHECKED_CAST")
            val value = (entity as Map<String, Any?>)[column.name]
            println("Column: ${column.name}, Value: $value")
            if (column.name == "id") {
                assertEquals(1, value.toString().toInt(), "ID should be 1")
            }
        }
    }

    fun createUser() {
        runBlocking {
            dao.createTable()
            val hashedPassword = BCrypt.hashpw(configuration.adminPassword, BCrypt.gensalt())
            val entity = mapOf("username" to configuration.adminUsername, "password" to hashedPassword)
            dao.save(entity)
        }
    }
}
