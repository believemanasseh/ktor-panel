package xyz.daimones.ktor.panel.database

/**
 * Interface for data access objects (DAOs).
 * This interface defines the methods that any DAO should implement to interact with the database.
 * It is designed to be generic, allowing for different entity types to be handled.
 */
interface DataAccessObjectInterface<T> {
    /**
     * Finds an entity by its primary key.
     * This is a method that can be used to find any entity type.
     * 
     * @param id The primary key of the entity to find.
     * @param castToEntityId Whether to cast the id to the entity's ID type.
     * @return The found entity of type T, or null if not found.
     */
    suspend fun findById(id: Any, castToEntityId: Boolean): T? {
        throw NotImplementedError("This method is not implemented for this DAO.")
    }

    /**
     * Finds an entity by its primary key.
     * This is a method that can be used to find any entity type.
     *
     * @param id The primary key of the entity to find.
     * @return The found entity of type T, or null if not found.
     */
    suspend fun findById(id: Any): T? {
        throw NotImplementedError("This method is not implemented for this DAO.")
    }

    /**
     * Finds all entities of a given type.
     * This is a method that can be used to find any entity type.
     *
     * @return A list of all entities of type T.
     */
    suspend fun findAll(): List<T?>

    /**
     * Finds an entity by its username.
     * This is a method that can be used to find any entity with a username field.
     * 
     * @param username The username to search for.
     * @return The found entity of type T, or null if not found.
     */
    suspend fun find(username: String): T?

    /**
     * Saves a new entity.
     * This method is for the ExposedDao implementation.
     * 
     * @param data The data to save, which is a map.
     * @return The saved entity of type T.
     */
    suspend fun save(data: Map<String, Any>): T {
        throw NotImplementedError("This method is not implemented for this DAO.")
    }

    /**
     * Saves a new entity.
     * A convenience overload for type-safe frameworks like JPA.
     * 
     * @param entity The entity to save.
     * @return The saved entity.
     */
    suspend fun save(entity: T): T {
        throw NotImplementedError("This method is not implemented for this DAO.")
    }

    /**
     * Updates an existing entity.
     * This method is for the ExposedDao implementation.
     * 
     * @param data The data to update, which is a map.
     * @return The updated entity of type T.
     */
    suspend fun update(data: Map<String, Any>): T {
        throw NotImplementedError("This method is not implemented for this DAO.")
    }

    /**
     * Updates an existing entity.
     * A convenience overload for type-safe frameworks like JPA.
     * 
     * @param entity The entity to update.
     * @return The updated entity.
     */
    suspend fun update(entity: T): T {
        throw NotImplementedError("This method is not implemented for this DAO.")
    }

    /**
     * Deletes an entity by its primary key.
     * 
     * @param id The primary key of the entity to delete.
     * @return The number of rows affected (should be 1 if successful).
     */
    suspend fun delete(id: Any): T?

    /**
     * Creates a table for the given entity class.
     *
     * This is a method that can be used to create any table.
     */
    suspend fun createTable()
}
