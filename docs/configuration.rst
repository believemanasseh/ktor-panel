Configuration
=============

The ``Configuration`` class allows you to customise the admin panel:

- ``url``: The base URL for the admin panel (default is ``admin``).
- ``endpoint``: The endpoint for the admin panel (default is ``/admin``).
- ``setAuthentication``: Enable or disable authentication (default is ``true``).
- ``adminName``: The name displayed in the admin panel (default is ``Admin``).
- ``adminUsername``: The username for the default admin user (default is ``admin``).
- ``adminPassword``: The password for the default admin user (default is ``admin``).
- ``customIndexTemplate``: Path to a custom index template (default is ``kt-panel-index.hbs``).
- ``customListTemplate``: Path to a custom list template (default is ``kt-panel-list.hbs``).
- ``customCreateTemplate``: Path to a custom create template (default is ``kt-panel-create.hbs``).
- ``customDetailsTemplate``: Path to a custom details template (default is ``kt-panel-details.hbs``).
- ``customLoginTemplate``: Path to a custom login template (default is ``kt-panel-login.hbs``).
- ``customLogoutTemplate``:  Path to a custom logout template (default is ``kt-panel-logout.hbs``).
- ``customDeleteTemplate``: Path to a custom delete template (default is ``kt-panel-delete.hbs``).
- ``favicon``: Path to a custom favicon (default is ``/static/favicon.ico``).
- ``templateRenderer``: Custom template renderer (default is Mustache).

.. code-block:: kotlin

    val config = Configuration(
      url = "dashboard",
      endpoint = "/",
      setAuthentication = true,
      adminName = "Custom Admin",
      adminUsername = "my_admin",
      adminPassword = "a_very_strong_password"
   )