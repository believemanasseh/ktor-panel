package xyz.daimones.ktor.panel

import org.hibernate.jpa.HibernatePersistenceConfiguration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.vendors.currentDialect
import xyz.daimones.ktor.panel.database.entities.JpaAdminUser

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

/**
 * Converts a camelCase string to snake_case.
 *
 * This function takes a string in camelCase format and converts it to snake_case format.
 * Each uppercase letter is preceded by an underscore and converted to lowercase.
 * This is useful for converting programming identifiers to database column names or other formats.
 *
 * @param camel The camelCase string to convert
 * @return The converted snake_case string
 */
internal fun camelToSnake(camel: String): String {
    val regex = "(?=[A-Z])".toRegex()
    return camel.split(regex).joinToString("_") { it.lowercase() }
}

/**
 * Registers the panel admin entity with the Hibernate persistence configuration.
 *
 * This function adds the JpaAdminUser entity to the Hibernate configuration,
 * allowing it to be managed by the JPA provider.
 *
 * @param config The HibernatePersistenceConfiguration to register entities with
 */
fun registerAdminEntity(config: HibernatePersistenceConfiguration) {
    config.managedClass(JpaAdminUser::class.java)
}

/**
 * Retrieves a custom template based on the specified view and configuration.
 *
 * This function returns the appropriate custom template for the given view type
 * from the provided configuration. It supports various views such as details,
 * list, create, login, logout, delete, and index.
 *
 * @param view The type of view for which to retrieve the custom template
 * @param configuration Configuration settings for the admin panel
 * @return The custom template for the specified view
 * @throws IllegalArgumentException if the view type is unknown
 */
internal fun getCustomTemplate(view: String, configuration: Configuration): String? {
    return when (view) {
        "details" -> configuration.customDetailsTemplate
        "list" -> configuration.customListTemplate
        "create" -> configuration.customCreateTemplate
        "login" -> configuration.customLoginTemplate
        "logout" -> configuration.customLogoutTemplate
        "delete" -> configuration.customDeleteTemplate
        "index" -> configuration.customIndexTemplate
        else -> throw IllegalArgumentException("Unknown view: $view")
    }
}
