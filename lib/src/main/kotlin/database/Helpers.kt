package com.administrativektor.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.vendors.currentDialect

fun retrieveTableNames(database: Database): List<String> {
    return transaction(database) {
        currentDialect.allTablesNames()
    }
}