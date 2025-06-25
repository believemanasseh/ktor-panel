Authentication
==============

Ktor Panel includes a built-in authentication system which is **enabled by default**. This guide explains how it works and how to configure it.

How It Works
------------

When you first run your application with authentication enabled, the panel will automatically:

1.  Create an ``admin_users`` table in your database if it doesn't already exist.
2.  Create a default admin user based on the ``adminUsername`` and ``adminPassword`` properties in your ``Configuration``. The password will be securely hashed using BCrypt.

After the first run, you can manage users (create, update, delete) through the admin interface itself.

Configuring Default Credentials
-------------------------------

You **must** change the default credentials for any production application. You can configure them when you initialise the admin panel:

.. code:: kotlin

   val config = Configuration(
       setAuthentication = true, // This is the default
       adminUsername = "my_admin",
       adminPassword = "a_very_strong_password"
   )

.. important::
   If you do not provide these values, the system will default to ``admin`` / ``admin``. Always change these for production environments.

Disabling Authentication
------------------------

If you do not need authentication (e.g., for a local-only tool or an internally secured service), you can disable it entirely by setting ``setAuthentication`` to ``false``.

.. code:: kotlin

   val config = Configuration(
       setAuthentication = false
   )

When authentication is disabled, the login page will be bypassed, and the ``admin_users`` table will not be created or used.