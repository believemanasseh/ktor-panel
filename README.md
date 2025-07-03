# Ktor Panel

[![GitHub Repo](https://img.shields.io/badge/GitHub-Repository-blue?logo=github)](https://github.com/believemanasseh/ktor-panel)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0+-blue.svg)](https://kotlinlang.org)
[![Ktor](https://img.shields.io/badge/Ktor-3.0.2+-blue.svg)](https://ktor.io/)
[![License](https://img.shields.io/badge/License-BSD_3--Clause-blue.svg)](LICENSE)

A lightweight, customisable admin interface library for Ktor applications. Ktor Panel provides a simple way to manage
your database models through an intuitive and secure web interface with minimal configuration.

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("xyz.daimones:ktor-panel:0.0.1")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'xyz.daimones:ktor-panel:0.0.1'
}
```

### Maven

```xml

<dependency>
    <groupId>xyz.daimones</groupId>
    <artifactId>ktor-panel</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Quick Start

### Basic Setup

```kotlin
// Import necessary components
import xyz.daimones.ktor.panel.Admin
import xyz.daimones.ktor.panel.Configuration
import xyz.daimones.ktor.panel.ModelView
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

    // Initialise admin panel
    val admin = Admin(this, config, database)

    // Add your models to the admin panel
    admin.addView(ModelView(User))
    admin.addView(ModelView(Product))
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
    setAuthentication = true,   // Enable authentication (default is true)
    adminName = "Custom Admin", // Change admin panel name
    adminUsername = "my_admin", // Set the default username
    adminPassword = "a_very_strong_password" // Set the default password
)
```

> **Important:** For production environments, always change the default `adminUsername` and `adminPassword`.

### Custom Templates

Create your own Mustache templates in your resources directory to override the defaults:

- `kt-panel-login.hbs` - Login form template
- `kt-panel-index.hbs` - Main dashboard template
- `kt-panel-list.hbs` - List view for database records
- `kt-panel-create.hbs` - Form for creating new records
- `kt-panel-details.hbs` - Detailed view of a record
- `kt-panel-update.hbs` - Form for updating existing records

## Testing

To run the tests for this project, you can use the following Gradle command:

```bash
./gradlew test
```

After running the tests, you can find:

- Test reports in the `lib/build/reports/tests/test/` directory
- Test coverage reports in the `lib/build/reports/jacoco/test/` directory

## Documentation

To build the Sphinx documentation, use the following command:

```bash
sphinx-build -b html docs/ build/
```

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