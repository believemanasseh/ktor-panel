Custom DAOs
===========

You can implement your own Data Access Object (DAO) layer by extending the ``DataAccessObjectInterface`` interface.

.. code:: kotlin

  import xyz.daimones.ktor.panel.database.DataAccessObjectInterface

  class MyCustomDao<T : Any> : DataAccessObjectInterface<T> { /* ... */ }

DataAccessObjectInterface Definition
------------------------------------

The ``DataAccessObjectInterface`` defines the contract for custom DAOs. Here is its basic structure:

.. code:: kotlin

  interface DataAccessObjectInterface<T> {
      suspend fun findById(id: castToEntityId: Boolean): T? {
        throw NotImplementedError("This method is not implemented for this DAO.")
      }
      suspend fun findById(id: Any): T? {
        throw NotImplementedError("This method is not implemented for this DAO.")
      }
      suspend fun findAll(): List<T?>
      suspend fun find(username: String): T?
      suspend fun save(data: Map<String, Any>): T {
        throw NotImplementedError("This method is not implemented for this DAO.")
      }
      suspend fun save(entity: T): T {
        throw NotImplementedError("This method is not implemented for this DAO.")
      }
      suspend fun update(data: Map<String, Any>): T {
        throw NotImplementedError("This method is not implemented for this DAO.")
      }
      suspend fun update(entity: T): T {
        throw NotImplementedError("This method is not implemented for this DAO.")
      }
      suspend fun delete(id: Int): T?
      suspend fun createTable()
  }

You must implement these methods for your entity type.

For the latest version, see the
`DataAccessObjectInterface on GitHub <https://github.com/believemanasseh/ktor-panel/blob/main/lib/src/main/kotlin/xyz/daimones/ktor/panel/database/Interfaces.kt>`__.