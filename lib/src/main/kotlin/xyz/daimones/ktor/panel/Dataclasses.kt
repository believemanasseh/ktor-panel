package xyz.daimones.ktor.panel

/**
 * Data classes for the Ktor Panel configuration and settings.
 *
 * This file contains data classes that define the structure and default values
 * for various configuration settings used throughout the admin panel.
 */

/**
 * Configuration settings for the Ktor Panel library.
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
 * @property adminUsername The default username for the admin user. This is used for authentication purposes.
 *             Default is "admin". This can be overridden in the admin user management interface.
 * 
 * @property adminPassword The default password for the admin user. This is used for authentication purposes.
 *             Default is "admin". This can be overridden in the admin user management interface.
 *
 * @property customIndexTemplate Optional custom Mustache template for the index/dashboard view.
 *
 * @property customListTemplate Optional custom Mustache template for listing entities in the admin panel.
 *
 * @property customCreateTemplate Optional custom Mustache template for creating new entities in the admin panel.
 *
 * @property customDetailsTemplate Optional custom Mustache template for displaying details of a specific entity.
 *
 * @property customLoginTemplate Optional custom Mustache template for the login page of the admin panel.
 *
 * @property customDeleteTemplate Optional custom Mustache template for the delete confirmation page in the admin panel.
 *
 * @property customLogoutTemplate Optional custom Mustache template for the logout confirmation page in the admin panel.
 *
 * @property favicon Optional favicon URL for the admin panel. This allows you to set a custom icon.
 *
 * @property templateRenderer The template renderer/engine used to render templates.
 *
 * @see Admin
 * @see BaseView
 */
data class Configuration(
    val url: String = "admin",
    val endpoint: String = "/",
    val setAuthentication: Boolean = true,
    val adminName: String = "Admin",
    val adminUsername: String = "admin",
    val adminPassword: String = "admin",
    val customIndexTemplate: String? = null,
    val customListTemplate: String? = null,
    val customCreateTemplate: String? = null,
    val customDetailsTemplate: String? = null,
    val customLoginTemplate: String? = null,
    val customDeleteTemplate: String? = null,
    val customLogoutTemplate: String? = null,
    val favicon: String = "/static/logo.ico",
    val templateRenderer: TemplateRenderer = MustacheTemplateRenderer()
)
