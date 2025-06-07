package xyz.daimones.ktor.admin

/**
 * Data classes for the Ktor Admin panel configuration and settings.
 *
 * This file contains data classes that define the structure and default values
 * for various configuration settings used throughout the admin panel.
 */

/**
 * Configuration settings for the Ktor Admin panel.
 *
 * This data class encapsulates all configuration options for customising the admin panel's
 * behavior, appearance, and security settings. It provides sensible defaults for all properties
 * while allowing developers to override specific settings as needed.
 *
 * @property url The URL path segment where the admin interface will be accessible.
 *             For example, with the default value "admin", the panel would be accessible at "/admin".
 *
 * @property endpoint The base endpoint for the index/dashboard view relative to the admin URL.
 *             With default values, the admin dashboard would be at "/admin/".
 *
 * @property setAuthentication Determines whether authentication is required to access the admin panel.
 *             When true, users will need to authenticate before accessing any admin features.
 *             Default is true.
 *
 * @property adminName The display name shown in the admin panel's UI elements, such as the header or title.
 *             This helps identify the admin panel when multiple instances exist or for branding purposes.
 *
 * @see Admin
 * @see BaseView
 */
data class Configuration(
    val url: String = "admin",
    val endpoint: String = "/",
    val setAuthentication: Boolean = true,
    val adminName: String = "Admin"
)

