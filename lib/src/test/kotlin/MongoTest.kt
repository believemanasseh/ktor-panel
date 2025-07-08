import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.ktor.server.testing.*
import xyz.daimones.ktor.panel.Admin
import xyz.daimones.ktor.panel.Configuration
import xyz.daimones.ktor.panel.EntityView
import xyz.daimones.ktor.panel.database.entities.MongoAdminUser
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MongoTest {
    private lateinit var database: MongoDatabase

    @BeforeTest
    fun setup() {
        val uri = "mongodb://localhost:27017/test"
        val mongoClient = MongoClient.create(uri)
        database = mongoClient.getDatabase("test")
    }

    @Test
    fun testAdminInit() = testApplication {
        application {
            val configuration = Configuration(setAuthentication = false)
            val admin =
                Admin(this, configuration, database)
            admin.addView(EntityView(MongoAdminUser::class))
            assertEquals(1, admin.countEntityViews(), "Admin should have one entity view registered")
        }
    }
}