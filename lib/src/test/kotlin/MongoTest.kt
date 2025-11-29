import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.flapdoodle.embed.mongo.commands.ServerAddress
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.transitions.ImmutableMongod
import de.flapdoodle.embed.mongo.transitions.Mongod
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess
import de.flapdoodle.reverse.TransitionWalker
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.mindrot.jbcrypt.BCrypt
import xyz.daimones.ktor.panel.Admin
import xyz.daimones.ktor.panel.Configuration
import xyz.daimones.ktor.panel.EntityView
import xyz.daimones.ktor.panel.database.dao.MongoDao
import xyz.daimones.ktor.panel.database.entities.MongoAdminUser
import kotlin.reflect.full.memberProperties
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test


class MongoTest {
    private lateinit var database: MongoDatabase
    private lateinit var serverAddress: ServerAddress
    private lateinit var running: TransitionWalker.ReachedState<RunningMongodProcess>
    private lateinit var configuration: Configuration
    private lateinit var dao: MongoDao<MongoAdminUser>
    private lateinit var id: ObjectId

    @BeforeTest
    fun setup() {
        val mongodConfig: ImmutableMongod = Mongod.instance()
        val version: Version.Main = Version.Main.V8_0

        running = mongodConfig.start(version)
        serverAddress = running.current().serverAddress

        val uri = "mongodb://${serverAddress.host}:${serverAddress.port}"
        val mongoClient = MongoClient.create(uri)
        database = mongoClient.getDatabase("test")
        configuration = Configuration(setAuthentication = false)
    }

    @Test
    fun testAdminInit() = testApplication {
        application {
            val admin =
                Admin(this, configuration, database)
            admin.addView(EntityView(MongoAdminUser::class))
            assert(1 == admin.countEntityViews()) {
                "Admin should have one entity view registered"
            }
        }
    }

    @Test
    fun testGetTableName() = testApplication {
        application {
            val admin = Admin(this, configuration, database)
            val entityView = EntityView(MongoAdminUser::class)
            val tableName = admin.getTableName(entityView)
            assert("admin_users" == tableName) {
                "Table name should match the entity's table name"
            }
        }
    }

    @Test
    fun testDaoFindById() = testApplication {
        dao = MongoDao(database, MongoAdminUser::class)
        createUser()
        val entity = dao.findById(id)
        MongoAdminUser::class.memberProperties.forEach { property ->
            val value = property.get(entity)

            if (property.name == "id") {
                assert(value is ObjectId) { "ID should be of type ObjectId" }
            }

            if (property.name == "username") {
                assert(value == configuration.adminPassword) {
                    "Username should match the created user's username"
                }
            }

            if (property.name == "password") {
                assert(BCrypt.checkpw(configuration.adminPassword, value.toString())) {
                    "Password should match the created user's password"
                }
            }
        }
    }

    fun createUser() {
        runBlocking {
            dao.createTable()
            id = ObjectId()
            val hashedPassword = BCrypt.hashpw(configuration.adminPassword, BCrypt.gensalt())
            val entity = MongoAdminUser(
                id = id,
                username = configuration.adminPassword,
                password = hashedPassword
            )
            dao.save(entity)
        }
    }

    @AfterTest
    fun teardownMongodb() {
        running.close()
    }
}