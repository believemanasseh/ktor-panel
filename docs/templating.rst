Templating and Custom Renderers
===============================

Overview
--------
Ktor Panel supports pluggable template rendering via the `TemplateRenderer` abstraction. This allows you to use different templating engines (e.g., Mustache, FreeMarker, Thymeleaf) or provide your own.

Default Renderers
-----------------
Out of the box, the following renderers are available:

- MustacheTemplateRenderer (uses Ktor's Mustache support)
- FreeMarkerTemplateRenderer (uses Ktor's FreeMarker support)
- ThymeleafTemplateRenderer (uses Ktor's Thymeleaf support)

Selecting a Renderer
--------------------
You can specify the renderer in your `Configuration`:

.. code-block:: kotlin

    val configuration = Configuration(
        templateRenderer = ThymeleafTemplateRenderer(), // default is MustacheTemplateRenderer
        // other options...
    )

Implementing a Custom Renderer
------------------------------
To use a different engine or your own logic, implement the `TemplateRenderer` interface:

.. code-block:: kotlin

    class MyCustomRenderer : TemplateRenderer {
        override fun render(configuration: Configuration, view: String, defaultTemplate: String, model: Map<String, Any>): Any {
            val customTemplate = getCustomTemplate(view, configuration)
            // The rest rendering logic here
        }
    }

Then, pass your renderer to the configuration:

.. code-block:: kotlin

    val configuration = Configuration(
        templateRenderer = MyCustomRenderer()
    )

API Reference
-------------
- `TemplateRenderer` interface: https://github.com/believemanasseh/ktor-panel/blob/main/lib/src/main/kotlin/xyz/daimones/ktor/panel/Interfaces.kt
- Built-in template renderer implementations: https://github.com/believemanasseh/ktor-panel/blob/main/lib/src/main/kotlin/xyz/daimones/ktor/panel/Renderers.kt