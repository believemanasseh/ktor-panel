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
import xyz.daimones.ktor.panel.snakeToCamel
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
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
    private val companion = (entityKClass.objectInstance as? Table)
        ?: throw IllegalArgumentException("Provided KClass must have an object instance that is a Table.")

    /**
     * Copies properties from the source object to the target IntEntity.
     * This is a utility function to avoid code duplication in save and update methods.
     * 
     * @param source The source object from which to copy properties.
     * @param target The target IntEntity to which properties will be copied.
     */
    private fun copyProperties(source: Any, target: Table) {
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
                    val propClass = (targetProp as KMutableProperty1<Any, Any?>).returnType.classifier as? KClass<*>
                    val finalValue = if (propClass?.java?.isEnum == true && value is String) {
                        @Suppress("UNCHECKED_CAST")
                        java.lang.Enum.valueOf(propClass.java as Class<out Enum<*>>, value)
                    } else {
                        value
                    }
                    targetProp.set(target, finalValue)
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

    fun assignMapToInsertOrUpdate(
        table: Table,
        data: Map<String, Any>,
        insert: InsertStatement<Number>? = null,
        update: UpdateStatement? = null
    ) {
        fun updateTable(insert: InsertStatement<Number>? = null, update: UpdateStatement? = null) {
            for ((key, value) in data) {
                if (key == "id") continue // skip id if auto-generated
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
            updateTable(insert)
        } else if (update != null) {
            updateTable(update = update)
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
        withContext(Dispatchers.IO) {
            transaction(this@ExposedDao.database) {
                companion.update({ idColumn eq id }) {
                    assignMapToInsertOrUpdate(companion, data, update = it)
                }

            }
        }
        val entity = this@ExposedDao.findById(id)
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
