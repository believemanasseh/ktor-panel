package xyz.daimones.ktor.panel.database.dao

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.daimones.ktor.panel.database.DataAccessObjectInterface
import xyz.daimones.ktor.panel.database.entities.AdminUser
import xyz.daimones.ktor.panel.database.entities.AdminUsers
import xyz.daimones.ktor.panel.snakeToCamel
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.memberProperties

/**
 * ExposedDao is an implementation of DataAccessObjectInterface using Exposed ORM.
 * It provides methods to interact with the database, including CRUD operations and table creation.
 * 
 * @property database The Exposed Database instance used for transactions.
 * @property entityKClass The entity KClass used for database operations.
 */
internal class ExposedDao<T : Any>(
    private val database: Database,
    private val entityKClass: KClass<T>
) : DataAccessObjectInterface<T> {
    /** Companion object instance for the entity */
    private val companion = (entityKClass.companionObjectInstance as? IntEntityClass<IntEntity>)
        ?: throw IllegalArgumentException("Provided KClass must have a companion object that is an IntEntityClass")

    /**
     * Copies properties from the source object to the target IntEntity.
     * This is a utility function to avoid code duplication in save and update methods.
     * 
     * @param source The source object from which to copy properties.
     * @param target The target IntEntity to which properties will be copied.
     */
    private fun copyProperties(source: Any, target: Entity<Int>) {
        val targetProperties = target::class.memberProperties
            .filterIsInstance<KMutableProperty1<*, *>>()
            .associateBy { it.name }

        if (source is Map<*, *>) {
            // If the source is a Map, iterate through its key-value pairs.
            for ((key, value) in source) {
                var name = key as String
                name = snakeToCamel(name)
                if (name == "id") continue
                targetProperties[name]?.let { targetProp ->
                    @Suppress("UNCHECKED_CAST")
                    (targetProp as KMutableProperty1<Any, Any?>).set(target, value)
                }
            }
        } else {
            // This is the original logic that works for data classes.
            val sourceProperties = source::class.memberProperties.associateBy { it.name }
            for ((name, sourceProp) in sourceProperties) {
                if (name == "id") continue

                targetProperties[name]?.let { targetProp ->
                    val value = sourceProp.call(source)
                    @Suppress("UNCHECKED_CAST")
                    (targetProp as KMutableProperty1<Any, Any?>).set(target, value)
                }
            }
        }
    }

    /**
     * Finds an entity by its primary key.
     * 
     * @param id The primary key of the entity to find.
     * @return The found entity of type T, or null if not found.
     */
    override suspend fun findById(id: Any): T? {
        val result = withContext(Dispatchers.IO) {
            transaction(this@ExposedDao.database) {
                companion.findById(id.toString().toInt())
            }
        }
        @Suppress("UNCHECKED_CAST")
        return result as? T
    }

    /**
     * Finds all entities of a given type.
     *
     * @return A list of all entities of type T.
     */
    override suspend fun findAll(): List<T?> {
        val result = withContext(Dispatchers.IO) {
            transaction(this@ExposedDao.database) {
                companion.all().toList()
            }
        }
        @Suppress("UNCHECKED_CAST")
        return result as? List<T?> ?: emptyList()
    }

    /**
     * Finds an entity by its username.
     * 
     * @param username The username to search for.
     * @return The found entity of type T, or null if not found.
     */
    override suspend fun find(username: String): T? {
        val result = withContext(Dispatchers.IO) {
            transaction(this@ExposedDao.database) {
                AdminUser.find { AdminUsers.username eq username }.firstOrNull()
            }
        }
        @Suppress("UNCHECKED_CAST")
        return result as? T
    }

    /**
     * Updates an existing entity.
     * This method uses a map of field names to values to update the entity.
     *
     * @param data A map of field names to values to update.
     * @return The updated entity.
     * @throws IllegalArgumentException if the entity with the given ID is not found.
     */
    override suspend fun update(data: Map<String, Any>): T {
        val updatedEntity = withContext(Dispatchers.IO) {
            transaction(this@ExposedDao.database) {
                val id = data["id"].toString().toInt()
                val entityToUpdate = companion.findById(id)
                    ?: throw IllegalArgumentException("Entity with id $id not found for update.")
                copyProperties(data, entityToUpdate)
                entityToUpdate
            }
        }
        @Suppress("UNCHECKED_CAST")
        return updatedEntity as T
    }

    /**
     * Saves a new entity. This method uses a map of field names to values to create the entity.
     * 
     * @param data A map of field names to values to save.
     * @return The saved entity.
     */
    override suspend fun save(data: Map<String, Any>): T {
        val savedEntity = withContext(Dispatchers.IO) {
            transaction(this@ExposedDao.database) {
                companion.new { copyProperties(data, this) }
            }
        }
        @Suppress("UNCHECKED_CAST")
        return savedEntity as T
    }

    /**
     * Deletes an entity by its primary key.
     * 
     * @param id The primary key of the entity to delete.
     * @return The deleted entity, or null if no entity was found with the given ID.
     */
    override suspend fun delete(id: Int): T {
        val obj = withContext(Dispatchers.IO) {
            transaction(this@ExposedDao.database) { companion.findById(id) }
        }
        obj?.delete() ?: throw IllegalArgumentException("Entity with id $id not found.")
        @Suppress("UNCHECKED_CAST")
        return obj as T
    }

    /**
     * Creates a table for the given entity class.
     * This method uses Exposed's SchemaUtils to create the table in the database.
     */
    override suspend fun createTable() {
        withContext(Dispatchers.IO) { transaction(this@ExposedDao.database) { SchemaUtils.create(companion.table) } }
    }
}
