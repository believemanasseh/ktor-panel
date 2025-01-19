package database.dao

import database.DatabaseAccessObjectInterface
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedDao<T> : DatabaseAccessObjectInterface<T> {
    private lateinit var database: Database

    fun init(database: Database) {
        this.database = database
    }

    override fun <T> findById(id: Int, table: IntIdTable, rowMapper: (ResultRow) -> T): T? {
        return transaction(database) {
            val resultRow = table.select { table.id eq id }.singleOrNull()
            resultRow?.let { rowMapper(it) }
        }
    }

    override fun <T> findAll(table: IntIdTable, rowMapper: (ResultRow) -> T): List<T?> {
        TODO("Not yet implemented")
    }

    override fun update(table: IntIdTable) {
        TODO("Not yet implemented")
    }

    override fun save(table: IntIdTable) {
        TODO("Not yet implemented")
    }

    override fun delete(table: IntIdTable) {
        TODO("Not yet implemented")
    }
}