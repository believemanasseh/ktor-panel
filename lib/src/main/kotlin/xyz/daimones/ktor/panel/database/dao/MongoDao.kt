package xyz.daimones.ktor.panel.database.dao

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.client.result.UpdateResult
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId
import xyz.daimones.ktor.panel.database.DataAccessObjectInterface
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * MongoDao is an implementation of DataAccessObjectInterface using MongoDB.
 * It provides methods to interact with the database, including CRUD operations and collection management.
 *
 * @property database The MongoDatabase instance used for operations.
 * @property entityKClass The KClass of the entity being managed.
 */
class MongoDao<T : Any>(private val database: MongoDatabase, private val entityKClass: KClass<T>) :
    DataAccessObjectInterface<T> {
    /**
     * The MongoDB collection for the entity type.
     * The collection name is derived from the simple name of the entity class.
     */
    private val collection: MongoCollection<T> =
        database.getCollection(entityKClass.java.simpleName.toString().lowercase(), entityKClass.java)

    /** Finds an entity by its ID.
     *
     * @param id The ID of the entity to find.
     * @return The found entity of type T, or null if not found.
     */
    override suspend fun findById(id: Any): T {
        val document = collection.find(Filters.eq("_id", ObjectId(id.toString()))).first()
        return document
    }

    /**
     * Finds an entity by username
     *
     * @param username The username to search for.
     * @return The found entity of type T
     */
    override suspend fun find(username: String): T {
        val document = collection.find(Filters.eq("username", username)).first()
        return document
    }

    /**
     * Finds all entities of a given type.
     *
     * @return A list of all entities of type T.
     */
    override suspend fun findAll(): List<T> {
        val documents = collection.find().toList()
        return documents
    }

    /**
     * Saves a new entity.
     * This method inserts the entity into the collection and returns the saved entity.
     *
     * @param entity The entity to save.
     * @return The saved entity of type T.
     */
    override suspend fun save(entity: T): T {
        val document = collection.insertOne(entity)
        val insertedId = document.insertedId
        if (insertedId != null) {
            val idProperty = entity::class.memberProperties
                .filterIsInstance<kotlin.reflect.KMutableProperty1<T, *>>()
                .find { it.name == "id" }

            if (idProperty != null) {
                // The insertedId is a BsonValue, so we get its value.
                val objectId = insertedId.asObjectId().value
                idProperty.setter.call(entity, objectId)
            } else {
                throw IllegalStateException("Entity does not have a mutable 'id' property to set the inserted ID.")
            }
        }
        return entity
    }

    /**
     * Updates an existing entity.
     * This method uses the entity's ID to find and update the entity in the collection.
     *
     * @param entity The entity to update.
     * @return The updated entity of type T.
     */
    override suspend fun update(entity: T): T {
        val idProperty = (entity::class as KClass<*>).memberProperties.find { it.name == "id" }
            ?: throw IllegalArgumentException("Entity must have an 'id' property for updates.")

        val idValue = idProperty.call(entity)
            ?: throw IllegalStateException("Entity 'id' property cannot be null.")

        val filter = Filters.eq("_id", idValue)
        var result: Any? = null
        for (property in entity::class.memberProperties) {
            if (property.name == "id") continue
            val value = property.call(entity)
            result = Updates.set(property.name, value).let { update -> collection.updateOne(filter, update) }
        }
        @Suppress("UNCHECKED_CAST")
        return (result as UpdateResult) as T
    }

    /**
     * Deletes an entity by its ID.
     *
     * @param id The ID of the entity to delete.
     * @return The deleted entity of type T, or null if not found.
     */
    override suspend fun delete(id: Int): T? {
        val document = collection.findOneAndDelete(Filters.eq("_id", id))
        return document
    }

    /**
     * Creates a table (collection) for the entity.
     * This is a no-op in MongoDB, as collections are created implicitly
     * when documents are inserted.
     */
    override suspend fun createTable() {
        database.createCollection(entityKClass::class.simpleName.toString())
    }
}