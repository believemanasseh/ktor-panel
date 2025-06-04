# Ktor Admin

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0+-blue.svg)](https://kotlinlang.org)
[![Ktor](https://img.shields.io/badge/Ktor-3.0.2+-blue.svg)](https://ktor.io/)
[![License](https://img.shields.io/badge/License-BSD_3--Clause-blue.svg)](LICENSE)

A lightweight, customisable admin interface library for Ktor applications. Ktor Admin provides a simple way to manage
your database models through an intuitive web interface with minimal configuration.

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("xyz.daimones:ktor-admin:0.0.1")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'xyz.daimones:ktor-admin:0.0.1'
}
```

### Maven

```xml

<dependency>
    <groupId>xyz.daimones</groupId>
    <artifactId>ktor-admin</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Quick Start

### Basic Setup

```kotlin
// Import necessary components
import xyz.daimones.ktor.admin.Admin
import xyz.daimones.ktor.admin.Configuration
import xyz.daimones.ktor.admin.ModelView
import org.jetbrains.exposed.sql.Database

fun Application.configureAdminPanel() {
    // Setup your database connection
    val database = Database.connect(
        "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver"
    )

    // Create admin configuration
    val config = Configuration(
        url = "admin",           // Access at /admin
        adminName = "My App Admin"
    )

    // Initialize admin panel
    val admin = Admin(this, database, config)

    // Add your models to the admin panel
    admin.addView(ModelView(Users))
    admin.addView(ModelView(Products))
}
```

### Add to your Ktor application

```kotlin
fun Application.module() {
    // Configure other Ktor features
    install(ContentNegotiation) {
        json()
    }

    // Install Mustache for templates
    install(Mustache) {
        mustacheFactory = DefaultMustacheFactory("templates")
    }

    // Add admin panel
    configureAdminPanel()
}
```

## Customisation

### Custom Configuration

```kotlin
val config = Configuration(
    url = "dashboard",          // Change URL to /dashboard
    endpoint = "/",             // Set index endpoint
    setAuthentication = true,   // Enable authentication
    adminName = "Custom Admin"  // Change admin panel name
)
```

### Custom Templates

Create your own Mustache templates in your resources directory to override the defaults:

- `kt-admin-index.hbs` - Main dashboard template
- `kt-admin-list.hbs` - List view for database records
- `kt-admin-create.hbs` - Form for creating new records
- `kt-admin-details.hbs` - Detailed view of a record
- `kt-admin-update.hbs` - Form for updating existing records

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the BSD 3-Clause License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Ktor](https://ktor.io/) - Kotlin async web framework
- [Exposed](https://github.com/JetBrains/Exposed) - Kotlin SQL library
- [Mustache](https://github.com/spullara/mustache.java) - Logic-less templates
- [Flask-Admin](https://github.com/flask-admin/flask-admin) - Inspiration for this project
