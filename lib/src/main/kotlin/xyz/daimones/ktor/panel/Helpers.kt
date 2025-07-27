package xyz.daimones.ktor.panel

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.vendors.currentDialect

/**
 * Helper functions for the Ktor Panel library.
 *
 * This file contains utility functions to assist with database operations,
 * such as retrieving table names and converting string formats.
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
internal fun retrieveTableNames(database: Database): List<String> {
    return transaction(database) {
        currentDialect.allTablesNames().map { it.split(".")[1].replaceFirstChar(Char::titlecase) }
            .filter { it.isNotEmpty() }.sorted()
    }
}

/**
 * Converts a snake_case string to camelCase.
 *
 * This function takes a string in snake_case format and converts it to camelCase format.
 * The first part of the string remains in lowercase, while subsequent parts are capitalised.
 * This is useful for converting database column names or other identifiers
 *
 * @param snake The snake_case string to convert
 * @return The converted camelCase string
 */
internal fun snakeToCamel(snake: String): String {
    var capitaliseNext = false
    return snake.asSequence().map { char ->
        if (char == '_') {
            capitaliseNext = true
            ""
        } else {
            val newChar = if (capitaliseNext) {
                capitaliseNext = false
                char.uppercase()
            } else {
                char.toString()
            }
            newChar
        }
    }.joinToString("")
}

