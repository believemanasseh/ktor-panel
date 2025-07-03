package xyz.daimones.ktor.panel.database

import kotlin.reflect.KClass

/**
 * Interface for database access objects (DAOs).
 * This interface defines the methods that any DAO should implement to interact with the database.
 * It is designed to be generic, allowing for different entity types to be handled.
 */
interface DatabaseAccessObjectInterface {
    /**
     * Finds an entity by its primary key.
     * This is a generic method that can be used to find any entity type.
     * 
     * @param id The primary key of the entity to find.
     * @param kClass The KClass of the entity to find.
     * @return The found entity of type T, or null if not found.
     */
    suspend fun <T : Any> findById(id: Int, kClass: KClass<T>): T?

    /**
     * Finds all entities of a given type.
     * This is a generic method that can be used to find any entity type.
     *
     * @param kClass The KClass of the entity to find.
     * @return A list of all entities of type T.
     */
    suspend fun <T : Any> findAll(kClass: KClass<T>): List<T?>

    /**
     * Finds an entity by its username.
     * This is a generic method that can be used to find any entity with a username field.
     * 
     * @param username The username to search for.
     * @param kClass The KClass of the entity to find.
     * @return The found entity of type T, or null if not found.
     */
    suspend fun <T : Any> find(username: String, kClass: KClass<T>): T?

    /**
     * Saves a new entity.
     * This method is for the ExposedDao implementation.
     * 
     * @param data The data to save, which is a map.
     * @param kClass The KClass of the entity to save.
     * @return The saved entity of type T.
     */
    suspend fun <T : Any> save(data: Map<String, Any>, kClass: KClass<T>): T

    /**
     * Saves a new entity.
     * A convenience overload for type-safe frameworks like JPA.
     * 
     * @param entity The entity to save.
     * @return The saved entity.
     */
    suspend fun <T : Any> save(entity: T): T

    /**
     * Updates an existing entity.
     * This method is for the ExposedDao implementation.
     * 
     * @param data The data to update, which is a map.
     * @param kClass The KClass of the entity to update.
     * @return The updated entity of type T.
     */
    suspend fun <T : Any> update(data: Map<String, Any>, kClass: KClass<T>): T

    /**
     * Updates an existing entity.
     * A convenience overload for type-safe frameworks like JPA.
     * 
     * @param entity The entity to update.
     * @return The updated entity.
     */
    suspend fun <T : Any> update(entity: T): T

    /**
     * Deletes an entity by its primary key.
     * 
     * @param id The primary key of the entity to delete.
     * @param kClass The KClass of the entity to delete.
     * @return The number of rows affected (should be 1 if successful).
     */
    suspend fun <T : Any> delete(id: Int, kClass: KClass<T>): T?

    /**
     * Creates a table for the given entity class.
     * 
     * This is a generic method that can be used to create any table.
     * @param kClass The KClass of the entity for which to create the table.
     */
    suspend fun <T : Any> createTable(kClass: KClass<T>)
}