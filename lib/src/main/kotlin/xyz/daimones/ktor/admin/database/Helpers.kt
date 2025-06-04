package xyz.daimones.ktor.admin.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.vendors.currentDialect

/**
 * Database helper functions for the Ktor Admin panel.
 *
 * This file contains utility functions that interact with the database
 * to retrieve metadata and assist with admin panel operations.
 */

/**
 * Retrieves a list of all table names from the database.
 *
 * This function executes a database transaction to query all available table names
 * using the current SQL dialect. The retrieved table names are used by the admin panel
 * to generate navigation menus and identify available models for administration.
 *
 * @param database The database connection to query for table names
 * @return A list of strings containing all table names in the database
 *
 * @see Database
 * @see org.jetbrains.exposed.sql.vendors.currentDialect
 */
fun retrieveTableNames(database: Database): List<String> {
    return transaction(database) {
        currentDialect.allTablesNames()
    }
}

