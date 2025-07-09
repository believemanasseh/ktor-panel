import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.flapdoodle.embed.mongo.commands.ServerAddress
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.transitions.ImmutableMongod
import de.flapdoodle.embed.mongo.transitions.Mongod
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess
import de.flapdoodle.reverse.TransitionWalker
import io.ktor.server.testing.*
import org.junit.jupiter.api.AfterEach
import xyz.daimones.ktor.panel.Admin
import xyz.daimones.ktor.panel.Configuration
import xyz.daimones.ktor.panel.EntityView
import xyz.daimones.ktor.panel.database.entities.MongoAdminUser
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


class MongoTest {
    private lateinit var database: MongoDatabase
    private lateinit var serverAddress: ServerAddress
    private lateinit var running: TransitionWalker.ReachedState<RunningMongodProcess>

    @BeforeTest
    fun setup() {
        val mongodConfig: ImmutableMongod = Mongod.instance()
        val version: Version.Main = Version.Main.V8_0

        running = mongodConfig.start(version)
        serverAddress = running.current().serverAddress

        val uri = "mongodb://${serverAddress.host}:${serverAddress.port}"
        val mongoClient = MongoClient.create(uri)
        database = mongoClient.getDatabase("test")
    }

    @AfterEach
    fun teardownMongodb() {
        running.close()
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