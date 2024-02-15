package com.administrativektor.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

interface DatabaseAccessObjectInterface<T> {
    fun <T> findById(id: Int, table: IntIdTable, rowMapper: (ResultRow) -> T): T?
    fun <T> findAll(table: IntIdTable, rowMapper: (ResultRow) -> T): List<T?>
    fun save(table: IntIdTable)
    fun update(table: IntIdTable)
    fun delete(table: IntIdTable)
}