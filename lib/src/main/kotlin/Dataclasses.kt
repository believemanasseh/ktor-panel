/**
 * A data class representing all default configurations.
 * @property url Default url of the admin interface.
 * @property endpoint Base endpoint name for index view.
 * @property setAuthentication Boolean field indicating whether to authenticate users.
 * @property adminName Default admin name.
 */
data class Configuration(val url: String, val endpoint: String, val setAuthentication: Boolean, val adminName: String)
