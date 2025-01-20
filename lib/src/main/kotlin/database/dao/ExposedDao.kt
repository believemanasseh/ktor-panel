package database.dao

import database.DatabaseAccessObjectInterface
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedDao<T> : DatabaseAccessObjectInterface<T> {
    private lateinit var database: Database

    constructor(database: Database) {
        this.database = database
    }

    override fun <T> findById(id: Int, table: IntIdTable, rowMapper: (ResultRow) -> T): T? {
        return transaction(this.database) {
            val resultRow = table.select { table.id eq id }.singleOrNull()
            resultRow?.let { rowMapper(it) }
        }
    }

    override fun <T> findAll(table: IntIdTable, rowMapper: (ResultRow) -> T): List<T?> {
        return transaction(this.database) {
            val resultRow = table.selectAll()
            resultRow.map { rowMapper(it) }
        }
    }

    override fun update(id: Int, table: IntIdTable, updateColumns: (UpdateBuilder<*>) -> Unit): Int {
        return transaction(this.database) {
            table.update({ table.id eq id }) {
                updateColumns(it)
            }
        }
    }

    override fun save(table: IntIdTable, saveData: (UpdateBuilder<*>) -> Unit): EntityID<Int> {
        return transaction(this.database) {
            table.insertAndGetId { saveData(it) }
        }
    }

    override fun delete(id: Int, table: IntIdTable): Int {
        return transaction(this.database) {
            table.deleteWhere { table.id eq id }
        }
    }
}