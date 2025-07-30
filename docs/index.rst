Ktor Panel
==========

|Maven Central| |Github| |Kotlin| |Ktor| |License|

A lightweight, customisable admin interface library for Ktor
applications. Ktor Panel provides a simple way to manage your database
entities through an intuitive and secure web interface with minimal configuration.

.. toctree::
   :maxdepth: 2
   :caption: Guides

   installation
   usage
   authentication
   configuration
   dao
   screenshots

Installation
------------

Gradle (Kotlin DSL)
~~~~~~~~~~~~~~~~~~~

.. code:: kotlin

   dependencies {
       implementation("xyz.daimones:ktor-panel:0.1.0")
   }

Gradle (Groovy)
~~~~~~~~~~~~~~~

.. code:: groovy

   dependencies {
       implementation 'xyz.daimones:ktor-panel:0.1.0'
   }

Maven
~~~~~

.. code:: xml


   <dependency>
       <groupId>xyz.daimones</groupId>
       <artifactId>ktor-panel</artifactId>
       <version>0.1.0</version>
   </dependency>

Quick Start
-----------

Basic Setup
~~~~~~~~~~~

.. code:: kotlin

   // Import necessary components
   import xyz.daimones.ktor.panel.Admin
   import xyz.daimones.ktor.panel.Configuration
   import xyz.daimones.ktor.panel.EntityView
   import org.jetbrains.exposed.sql.Database

   fun Application.configureAdminPanel(database: Database) {
       // Create admin configuration
       val config = Configuration(
           url = "admin",           // Access at /admin
           adminName = "My App Admin"
       )

       // Initialise admin panel
       val admin = Admin(this, config, database)

       // Add your entities to the admin panel
       admin.addView(EntityView(User::class))
       admin.addView(EntityView(Product::class))
   }

Add to your Ktor application
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. code:: kotlin

   fun Application.module() {
       // Configure other Ktor features
       install(ContentNegotiation) {
           json()
       }

       // Setup your database connection
       val database = Database.connect(
           "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
           driver = "org.h2.Driver"
       )

       // OPTIONAL: Install Mustache for templates (by default ktor-panel configures Mustache for rendering views)
       install(Mustache) {
           val roots = listOf("templates", "panel_templates")
           mustacheFactory = object : DefaultMustacheFactory() {
               override fun getReader(resourceName: String): Reader {
                   for (root in roots) {
                       val stream = this.javaClass.classLoader.getResourceAsStream("$root/$resourceName")
                       if (stream != null) {
                           return stream.reader()
                       }
                   }
                   throw java.io.FileNotFoundException("Template $resourceName not found in $roots")
               }
           }
       }

       // Add admin panel
       configureAdminPanel(database)
   }

Customisation
-------------

Custom Configuration
~~~~~~~~~~~~~~~~~~~~

.. code:: kotlin

   val config = Configuration(
      url = "dashboard",          // Change URL to /dashboard
      endpoint = "/",             // Set index endpoint
      setAuthentication = true,   // Enable authentication (default is true)
      adminName = "Custom Admin", // Change admin panel name
      adminUsername = "my_admin", // Set the default username
      adminPassword = "a_very_strong_password" // Set the default password
   )

Custom Templates
~~~~~~~~~~~~~~~~

Create your own Mustache templates in your resources directory to
override the defaults:

-  ``kt-panel-login.hbs`` - Login form template
-  ``kt-panel-index.hbs`` - Main dashboard template
-  ``kt-panel-list.hbs`` - List view for database records
-  ``kt-panel-create.hbs`` - Form for creating new records
-  ``kt-panel-details.hbs`` - Detailed view of a record
-  ``kt-panel-delete.hbs`` - Confirmation for deleting records

License
-------

This project is licensed under the BSD 3-Clause License - see the
`LICENSE <LICENSE>`__ file for details.

Acknowledgments
---------------

-  `Ktor <https://ktor.io/>`__ - Kotlin async web framework
-  `Exposed <https://github.com/JetBrains/Exposed>`__ - Kotlin SQL
   library
-  `Hibernate <https://hibernate.org/orm/documentation/7.0/>`__ - Java
   ORM library
-  `MongoDB Kotlin Driver <https://www.mongodb.com/docs/drivers/kotlin/coroutine/current/quick-start/>`__ - Official MongoDB driver for Kotlin
-  `Mustache <https://github.com/spullara/mustache.java>`__ - Logic-less
   templates
-  `Flask-Admin <https://github.com/flask-admin/flask-admin>`__ -
   Inspiration for this project

.. |Kotlin| image:: https://img.shields.io/badge/Kotlin-2.1.0+-blue.svg
   :target: https://kotlinlang.org
.. |Ktor| image:: https://img.shields.io/badge/Ktor-3.0.2+-blue.svg
   :target: https://ktor.io/
.. |License| image:: https://img.shields.io/badge/License-BSD_3--Clause-blue.svg
   :target: LICENSE
.. |GitHub| image:: https://img.shields.io/badge/GitHub-Repository-blue?logo=github
   :target: https://github.com/believemanasseh/ktor-panel
.. |Maven Central| image:: https://img.shields.io/maven-central/v/xyz.daimones/ktor-panel?color=blue&label=Maven%20Central
   :target: https://central.sonatype.com/artifact/xyz.daimones/ktor-panel
