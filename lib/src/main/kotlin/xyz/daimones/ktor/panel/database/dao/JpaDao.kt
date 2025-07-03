package xyz.daimones.ktor.panel.database.dao

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Persistence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.daimones.ktor.panel.database.DatabaseAccessObjectInterface
import kotlin.reflect.KClass

/**
 * Implementation of the DatabaseAccessObjectInterface using JPA. This class provides methods to
 * interact with the database using JPA's EntityManager. It supports basic CRUD operations and can
 * be extended for more complex queries.
 *
 * @property entityManagerFactory The EntityManagerFactory used to create EntityManagers.
 */
class JpaDao(private val entityManagerFactory: EntityManagerFactory) :
    DatabaseAccessObjectInterface {
    /**
     * Executes a read operation using the provided block of code. This method creates an
     * EntityManager, executes the block, and ensures the EntityManager is closed afterwards.
     *
     * @param block The block of code to execute with the EntityManager.
     * @return The result of the block execution.
     */
    private fun <R> execute(block: (EntityManager) -> R): R {
        val entityManager = entityManagerFactory.createEntityManager()
        return entityManager.use { manager ->
            block(manager)
        }
    }

    /**
     * Executes a write operation within a transaction using the provided block of code. This method
     * creates an EntityManager, begins a transaction, executes the block, commits the transaction,
     * and ensures the EntityManager is closed afterwards.
     *
     * @param block The block of code to execute with the EntityManager.
     * @return The result of the block execution.
     */
    private fun <R> executeInTransaction(block: (EntityManager) -> R): R {
        val entityManager = entityManagerFactory.createEntityManager()
        try {
            entityManager.transaction.begin()
            val result = block(entityManager)
            entityManager.transaction.commit()
            return result
        } catch (e: Exception) {
            if (entityManager.transaction.isActive) {
                entityManager.transaction.rollback()
            }
            throw e // Re-throw the exception to the caller
        } finally {
            entityManager.close()
        }
    }

    /**
     * Finds an entity by its primary key. This method uses the EntityManager to find the entity of
     * the specified class with the given ID.
     *
     * @param id The primary key of the entity to find.
     * @param kClass The KClass of the entity to find.
     * @return The found entity of type T, or null if not found.
     */
    override suspend fun <T : Any> findById(id: Int, kClass: KClass<T>): T? {
        return withContext(Dispatchers.IO) {
            execute { entityManager ->
            entityManager.find(kClass.java, id)
            }
        }
    }

    /**
     * Finds all entities of a given type. This method uses the EntityManager to create a query that
     * selects all entities of the specified class.
     *
     * @param kClass The KClass of the entity to find.
     * @return A list of all entities of type T.
     */
    override suspend fun <T : Any> findAll(kClass: KClass<T>): List<T> {
        return withContext(Dispatchers.IO) {
            execute { entityManager ->
            val jpql = "SELECT e FROM ${kClass.simpleName} e"
            entityManager.createQuery(jpql, kClass.java).resultList
            }
        }
    }

    /**
     * Finds an entity by its username. This method uses the EntityManager to create a query that
     * selects an entity with the specified username.
     *
     * @param username The username to search for.
     * @param kClass The KClass of the entity to find.
     * @return The found entity of type T, or null if not found.
     */
    override suspend fun <T : Any> find(username: String, kClass: KClass<T>): T? {
        return withContext(Dispatchers.IO) {
            execute { entityManager ->
            val jpql = "SELECT e FROM ${kClass.simpleName} e WHERE e.username = :username"
            entityManager
                .createQuery(jpql, kClass.java)
                .setParameter("username", username)
                .resultList
                .firstOrNull()
            }
        }
    }

    /**
     * Saves a new entity. This method uses the EntityManager to merge the entity, which can be used
     * for both saving and updating.
     *
     * @param entity The entity to save.
     * @return The saved entity.
     */
    override suspend fun <T : Any> save(entity: T): T {
        return executeInTransaction { entityManager -> entityManager.merge(entity) }
    }

    /**
     * This method is not supported in JpaDao.
     *
     * @param data Map of property names to values to save
     * @param kClass Kotlin class of the entity to save
     * @return Entity instance of type T
     * @throws NotImplementedError Always throws this error as this operation is not supported
     * @see save(entity: T) Use this method instead with an entity instance
     */
    override suspend fun <T : Any> save(data: Map<String, Any>, kClass: KClass<T>): T {
        throw NotImplementedError("JpaDao requires an entity instance. Use save(entity: T) instead")
    }

    /**
     * Updates an existing entity. This method uses the EntityManager to merge the entity, which can
     * be used for both saving and updating.
     *
     * @param entity The entity to update.
     * @return The updated entity.
     */
    override suspend fun <T : Any> update(entity: T): T {
        return withContext(Dispatchers.IO) { executeInTransaction { entityManager -> entityManager.merge(entity) } }
    }

    /**
     * This method is not supported in JpaDao.
     *
     * @param data Map of property names to values to save
     * @param kClass Kotlin class of the entity to save
     * @return Entity instance of type T
     * @throws NotImplementedError Always throws this error as this operation is not supported
     * @see update(entity: T) Use this method instead with an entity instance
     */
    override suspend fun <T : Any> update(data: Map<String, Any>, kClass: KClass<T>): T {
        throw NotImplementedError(
            "JpaDao requires an entity instance. Use update(entity: T) instead"
        )
    }

    /**
     * Deletes an entity of type [T] from the database by its ID. This method uses the EntityManager
     * to find the entity by its ID and then removes it.
     *
     * @param id The primary key of the entity to delete
     * @param kClass The Kotlin class of the entity type
     * @return The deleted entity, or null if no entity was found with the given ID
     */
    override suspend fun <T : Any> delete(id: Int, kClass: KClass<T>): T? {
        return withContext(Dispatchers.IO) {
            executeInTransaction { entityManager ->
            val entityToDelete = entityManager.find(kClass.java, id)
            entityToDelete?.let { entityManager.remove(it) }
            @Suppress("UNCHECKED_CAST")
            id as? T?
            }
        }
    }

    /**
     * Creates a table for the given entity class. This method uses JPA's schema generation features
     * to create the table in the database.
     *
     * @param kClass The KClass of the entity for which to create the table.
     */
    override suspend fun <T : Any> createTable(kClass: KClass<T>) {
        withContext(Dispatchers.IO) {
            // Get the existing properties from the main factory.
            // This includes the database URL, user, password, and dialect.
            val existingProperties = entityManagerFactory.properties

            val schemaGenProperties = HashMap<String, Any>(existingProperties)

            // Set the schema generation strategy
            schemaGenProperties["jakarta.persistence.schema-generation.database.action"] = "create"

            // This tells the provider which classes to consider for table creation.
            schemaGenProperties["jakarta.persistence.managed-class"] = kClass.java.name

            // This is a common Hibernate property to prevent it from validating the rest of the schema.
            // It helps isolate the creation to just what's needed.
            schemaGenProperties["hibernate.hbm2ddl.auto"] = "create"

            val tempSchemaFactory: EntityManagerFactory? =
                try {
                    Persistence.createEntityManagerFactory("schema-generator", schemaGenProperties)
                } catch (e: Exception) {
                    println(
                        "Schema generation for ${kClass.simpleName} may have been skipped: ${e.message}"
                    )
                    null
                }

            tempSchemaFactory?.close()
        }
    }
}