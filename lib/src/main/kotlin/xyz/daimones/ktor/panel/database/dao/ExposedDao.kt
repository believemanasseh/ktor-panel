package xyz.daimones.ktor.panel.database.dao

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.daimones.ktor.panel.database.DatabaseAccessObjectInterface
import xyz.daimones.ktor.panel.database.entities.AdminUser
import xyz.daimones.ktor.panel.database.entities.AdminUsers
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

/**
 * ExposedDao is an implementation of DatabaseAccessObjectInterface using Exposed ORM.
 * It provides methods to interact with the database, including CRUD operations and table creation.
 * 
 * @property database The Exposed Database instance used for transactions.
 * @property entityCompanions A list of pairs containing KClass and IntEntityClass for registered entities.
 */
class ExposedDao(
    private val database: Database,
    private val entityCompanions: List<Pair<KClass<out IntEntityClass<IntEntity>>, IntEntityClass<IntEntity>>>
) : DatabaseAccessObjectInterface {
    // The registry stores the powerful entity classes themselves.
    // It's created once when the DAO is initialised.
    @Suppress("UNCHECKED_CAST")
    private val entityCompanionRegistry: Map<KClass<out IntEntityClass<IntEntity>>, IntEntityClass<IntEntity>> =
        entityCompanions.toMap() + mapOf(AdminUser::class as KClass<out IntEntityClass<IntEntity>> to AdminUser)

    /**
     * Retrieves the companion object for a given entity class.
     * This method checks the companion registry to find the appropriate IntEntityClass for the given entity class.
     *
     * @param kClass The KClass of the entity for which to retrieve the entity class.
     * @return The IntEntity object for the specified kotlin reflection object (KClass).
     * @throws IllegalArgumentException if the entity class is not registered with this DAO.
     */
    private fun <T : Any> getEntityCompanion(kClass: KClass<T>): IntEntityClass<IntEntity> {
        @Suppress("UNCHECKED_CAST")
        return entityCompanionRegistry[kClass as KClass<out IntEntityClass<IntEntity>>]
            ?: throw IllegalArgumentException("Entity class '${kClass.simpleName}' is not registered with this DAO.")
    }

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
                val name = key as? String ?: continue 
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
     * @param kClass The KClass of the entity to find.
     * @return The found entity of type T, or null if not found.
     */
    override fun <T : Any> findById(id: Int, kClass: KClass<T>): T? {
        val companion = getEntityCompanion(kClass)

        val result = transaction(this.database) {
            companion.findById(id)
        }
        @Suppress("UNCHECKED_CAST")
        return result as? T?
    }

    /**
     * Finds all entities of a given type.
     *
     * @param kClass The KClass of the entity to find.
     * @return A list of all entities of type T.
     */
    override fun <T : Any> findAll(kClass: KClass<T>): List<T?> {
        val companion = getEntityCompanion(kClass)
        val result = transaction(this.database) {
            companion.all().toList()
        }
        @Suppress("UNCHECKED_CAST")
        return result as? List<T?> ?: emptyList()
    }

    /**
     * Finds an entity by its username.
     * 
     * @param username The username to search for.
     * @param kClass The KClass of the entity to find.
     * @return The found entity of type T, or null if not found.
     */
    override fun <T : Any> find(
            username: String,
            kClass: KClass<T>,
    ): T? {
        val companion = getEntityCompanion(kClass)
        val result = transaction(this.database) {
            companion.find { AdminUsers.username eq username }.firstOrNull()
        }
        @Suppress("UNCHECKED_CAST")
        return result as? T?
    }

    /**
     * Updates an existing entity.
     * This method uses a map of field names to values to update the entity.
     *
     * @param kClass The KClass of the entity to update.
     * @param data A map of field names to values to update.
     * @return The updated entity.
     * @throws IllegalArgumentException if the entity with the given ID is not found.
     */
    override fun <T : Any> update(data: Map<String, Any>, kClass: KClass<T>): T {
        val companion = getEntityCompanion(kClass)
        val updatedEntity = transaction(this.database) {
            val id = data["id"].toString().toInt()
            val entityToUpdate = companion.findById(id)
                ?: throw IllegalArgumentException("Entity with id $id not found for update.")
            copyProperties(data, entityToUpdate)
            entityToUpdate
        }
        @Suppress("UNCHECKED_CAST")
        return updatedEntity as T
    }

    /**
     * This method is not supported in ExposedDao.
     * 
     * @param entity The entity to update.
     * @return The updated entity.
     * @throws NotImplementedError if this method is called directly.
     */
    override fun <T : Any> update(entity: T): T {
        throw NotImplementedError("ExposedDao requires a map and entity class. Use update(data: Map<String, Any>, kClass: KClass<T>) instead")
    }

    /**
     * Saves a new entity. This method uses a map of field names to values to create the entity.
     * 
     * @param data A map of field names to values to save.
     * @param kClass The KClass of the entity to save.
     * @return The saved entity.
     */
    override fun <T : Any> save(data: Map<String, Any>, kClass: KClass<T>): T {
        val companion = getEntityCompanion(kClass)
        val savedEntity = transaction(this.database) {
            companion.new { copyProperties(data, this) }
        }
        @Suppress("UNCHECKED_CAST")
        return savedEntity as T
    }

    /**
     * This method is not supported in ExposedDao.
     * 
     * @param entity The entity to save.
     * @return The saved entity.
     * @throws NotImplementedError if this method is called directly.
     */
    override fun <T: Any> save(entity: T): T {
        throw NotImplementedError("ExposedDao requires a map and entity class. Use save(data: Map<String, Any>, kClass: KClass<T>) instead.")
    }

    /**
     * Deletes an entity by its primary key.
     * 
     * @param id The primary key of the entity to delete.
     * @param kClass The KClass of the entity to delete.
     * @return The deleted entity, or null if no entity was found with the given ID.
     */
    override fun <T : Any> delete(id: Int, kClass: KClass<T>): T? {
        val companion = getEntityCompanion(kClass)
        val obj = transaction(this.database) { companion.findById(id) }
        @Suppress("UNCHECKED_CAST")
        return obj?.id as T?
    }

    /**
     * Creates a table for the given entity class.
     * This method uses Exposed's SchemaUtils to create the table in the database.
     *
     * @param kClass The KClass of the entity for which to create the table.
     */
    override fun <T : Any> createTable(kClass: KClass<T>) {
        val companion = getEntityCompanion(kClass)
        transaction(this.database) { SchemaUtils.create(companion.table) }
    }
}
