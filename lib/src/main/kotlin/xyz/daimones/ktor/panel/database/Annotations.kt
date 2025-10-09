package xyz.daimones.ktor.panel.database

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class FileUpload(val storage: String = "local", val path: String = "/uploads")
