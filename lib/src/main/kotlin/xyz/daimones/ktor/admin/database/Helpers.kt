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

/**
 * Cleans and formats a list of table names retrieved from the database.
 *
 * This function processes raw table names by:
 * 1. Extracting the table name from schema-qualified names (taking the part after the dot)
 * 2. Converting the names to uppercase for consistent presentation
 * 3. Filtering out any empty names that might result from the splitting
 * 4. Sorting the names alphabetically
 *
 * @param tableNames The raw list of table names, potentially schema-qualified (e.g., "public.users")
 * @return A sorted list of clean, uppercase table names without schema qualification
 */
fun cleanTableNames(tableNames: List<String>): List<String> {
    return tableNames.map { it.split(".")[1].replaceFirstChar(Char::titlecase) }.filter { it.isNotEmpty() }.sorted()
}
