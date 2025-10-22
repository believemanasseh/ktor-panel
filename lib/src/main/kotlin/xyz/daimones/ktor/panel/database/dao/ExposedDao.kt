package xyz.daimones.ktor.panel.database.dao

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.daimones.ktor.panel.camelToSnake
import xyz.daimones.ktor.panel.database.DataAccessObjectInterface
import xyz.daimones.ktor.panel.database.entities.AdminUsers
import kotlin.reflect.KClass

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
    private val companion = (entityKClass.objectInstance as? Table)
        ?: throw IllegalArgumentException("Provided KClass must have an object instance that is a Table.")

    /**
     * Assigns values from a map to an InsertStatement or UpdateStatement.
     *
     * @param table The Exposed Table to which the data belongs.
     * @param data The map containing field names and their corresponding values.
     * @param insert The InsertStatement to which values will be assigned (if not null).
     * @param update The UpdateStatement to which values will be assigned (if not null).
     */
    fun assignMapToInsertOrUpdate(
        table: Table,
        data: Map<String, Any>,
        insert: InsertStatement<Number>? = null,
        update: UpdateStatement? = null
    ) {
        fun populateTable(insert: InsertStatement<Number>? = null, update: UpdateStatement? = null) {
            for ((key, value) in data) {
                if (key == "id") continue // skip id if auto-generated
                if (value == "") continue // skip empty values
                val column = table.columns.find { it.name == camelToSnake(key) }
                if (column != null) {
                    val columnType = column.columnType
                    val finalValue = when (columnType) {
                        is EnumerationNameColumnType<*> -> {
                            val enumClass = columnType.klass.java
                            java.lang.Enum.valueOf(enumClass, value.toString())
                        }

                        is EnumerationColumnType<*> -> {
                            val enumClass = columnType.klass.java
                            java.lang.Enum.valueOf(enumClass, value.toString())
                        }

                        else -> value
                    }
                    @Suppress("UNCHECKED_CAST")
                    if (insert != null) {
                        (column as? Column<Any>)?.let { insert[it] = finalValue }
                    } else if (update != null) {
                        (column as? Column<Any>)?.let { update[it] = finalValue }
                    }
                }
            }
        }
        if (insert != null) {
            populateTable(insert)
        } else if (update != null) {
            populateTable(update = update)
        }
    }

    /**
     * Converts a ResultRow to a Map representing the entity.
     *
     * @param row The ResultRow to convert.
     * @return A Map representing the entity.
     */
    private fun resultRowToEntity(row: ResultRow): Map<String, Any?> {
        return row.fieldIndex.keys.associate { col ->
            (col as Column<*>).name to row[col]
        }
    }


    /**
     * Finds an entity by its primary key.
     * 
     * @param id The primary key of the entity to find.
     * @return The found entity of type T, or null if not found.
     */
    override suspend fun findById(id: Any, castToEntityId: Boolean): T? {
        @Suppress("UNCHECKED_CAST")
        val idColumn = companion.primaryKey?.columns?.firstOrNull() as? Column<Any>
            ?: throw IllegalStateException("No primary key defined for table.")
        val result = withContext(Dispatchers.IO) {
            transaction(this@ExposedDao.database) {
                var entityId: Any = id
                if (castToEntityId) {
                    @Suppress("UNCHECKED_CAST")
                    entityId = EntityID(id as Int, idColumn.table as IdTable<Int>)
                }
                companion.selectAll().where { idColumn eq entityId }
                    .withDistinct()
                    .map {
                        resultRowToEntity(it)
                    }.firstOrNull()
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
        companion.primaryKey?.columns?.firstOrNull()
            ?: throw IllegalStateException("No primary key defined for table.")
        val result = withContext(Dispatchers.IO) {
            transaction(this@ExposedDao.database) {
                companion.selectAll().toList()
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
                AdminUsers.select(AdminUsers.username, AdminUsers.password).where { AdminUsers.username eq username }
                    .withDistinct()
                    .map { it[AdminUsers.username] to it[AdminUsers.password] }.firstOrNull()
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
        @Suppress("UNCHECKED_CAST")
        val idColumn = companion.primaryKey?.columns?.firstOrNull() as? Column<Any>
            ?: throw IllegalStateException("No primary key defined for table.")
        val id = data["id"].toString().toInt()

        @Suppress("UNCHECKED_CAST")
        val entityId = EntityID(id, idColumn.table as IdTable<Int>)
        withContext(Dispatchers.IO) {
            transaction(this@ExposedDao.database) {
                companion.update({ idColumn eq entityId }) {
                    assignMapToInsertOrUpdate(companion, data, update = it)
                }

            }
        }
        val entity = this.findById(entityId, false)
        @Suppress("UNCHECKED_CAST")
        return entity as T
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
                companion.insert { assignMapToInsertOrUpdate(companion, data, it) }
            }
        }
        val idColumn: Column<*>? = companion.columns.find { it.name == "id" } ?: companion.primaryKey?.columns?.first()
        val id = savedEntity[idColumn!!]
        val entity = this.findById(id!!, false)
        @Suppress("UNCHECKED_CAST")
        return entity as T
    }

    /**
     * Deletes an entity by its primary key.
     * 
     * @param id The primary key of the entity to delete.
     * @return The deleted entity, or null if no entity was found with the given ID.
     */
    override suspend fun delete(id: Any): T {
        val obj = withContext(Dispatchers.IO) {
//            transaction(this@ExposedDao.database) { companion.findById(id.toString().toInt()) }
        }
//        transaction(database) { obj?.delete() } ?: throw IllegalArgumentException("Entity with id $id not found.")
        @Suppress("UNCHECKED_CAST")
        return obj as T
    }

    /**
     * Creates a table for the given entity class.
     * This method uses Exposed's SchemaUtils to create the table in the database.
     */
    override suspend fun createTable() {
        withContext(Dispatchers.IO) { transaction(this@ExposedDao.database) { SchemaUtils.create(companion) } }
    }
}
