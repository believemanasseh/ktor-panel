package xyz.daimones.ktor.admin.database

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder

interface DatabaseAccessObjectInterface<T> {
    fun <T> findById(id: Int, table: IntIdTable, rowMapper: (ResultRow) -> T): T?
    fun <T> findAll(table: IntIdTable, rowMapper: (ResultRow) -> T): List<T?>
    fun save(table: IntIdTable, saveData: (UpdateBuilder<*>) -> Unit): EntityID<Int>
    fun update(id: Int, table: IntIdTable, updateColumns: (UpdateBuilder<*>) -> Unit): Int
    fun delete(id: Int, table: IntIdTable): Int
}