Usage Guide
============

Ktor Panel supports multiple Object-Relational Mappers (ORMs) and Object-Document Mappers (ODMs) for seamless integration with various databases.

Currently supported ORMs and ODMs include:
------------------------------------------

- `Exposed <https://github.com/JetBrains/Exposed>`__ (Kotlin SQL ORM)
- `Hibernate <https://hibernate.org/orm/documentation/7.0/>`__ (Java ORM/JPA)
- `MongoDB Kotlin Driver <https://www.mongodb.com/docs/drivers/kotlin/coroutine/current/quick-start/>`__ (Kotlin Coroutine Driver, ODM)

Each integration requires you to define your entities and register them with the admin panel.

Exposed (SQL)
-------------

Exposed is a lightweight Kotlin SQL library.  
Define your entities using Exposed's DSL and register them:

.. code:: kotlin

   object Users : IntIdTable() {
       val username = varchar("username", 50)
   }

   class User(id: EntityID<Int>) : IntEntity(id) {
       companion object : IntEntityClass<User>(Users)
       var username by Users.username
   }

   fun Application.module() {
       val configuration = Configuration(setAuthentication = false)
       val admin = Admin(this, configuration, database)
       admin.addView(EntityView(User::class))
   }

Hibernate (JPA)
---------------

Hibernate is a popular Java ORM.  
Define your JPA entities and register them:

.. code:: kotlin

   @Entity
   @Table(name = "products")
   class Product(
       @Id @GeneratedValue var id: Long? = null,
       var name: String = ""
   )

   fun Application.module() {
       val config = HibernatePersistenceConfiguration("Products")
       registerAdminEntity(config) // Registers library's admin entity
       val entityManagerFactory = config
           .managedClass(Product::class.java)
           .jdbcUrl("jdbc:h2:mem:hibernate_example;DB_CLOSE_DELAY=-1")
           .jdbcDriver("org.h2.Driver")
           .jdbcCredentials("sa", "")
           .schemaToolingAction(Action.CREATE_DROP)
           .jdbcPoolSize(16)
           .showSql(true, true, true)
           .createEntityManagerFactory()
       val configuration = Configuration(setAuthentication = false)
       val admin = Admin(this, configuration, entityManagerFactory = entityManagerFactory)
       admin.addView(EntityView(Product::class))
   }

MongoDB (ODM)
-------------

Ktor Panel supports MongoDB via the official Kotlin coroutine driver.  
Define your data classes and register them:

.. code:: kotlin

   @Serializable
   data class BlogPost(
       @BsonId val id: Id<BlogPost>? = null,
       val title: String,
       val content: String
   )

   fun Application.module() {
       val configuration = Configuration(setAuthentication = false)
       val admin = Admin(this, configuration, database)
       admin.addView(EntityView(BlogPost::class))
   }

Notes
-----

- Ensure you have the necessary dependencies in your project (e.g., Exposed, Hibernate, MongoDB driver).
- When using hibernate, you have to provide an ``EntityManagerFactory`` instance to the ``Admin`` constructor.