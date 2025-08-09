package xyz.daimones.ktor.panel

interface TemplateRenderer {
    /**
     * Renders a template with the given configuration and model.
     *
     * @param configuration The configuration settings for the admin panel.
     * @param view The name of the view to render.
     * @param defaultTemplate The name of the default template to render.
     * @param model The model to use for rendering the template.
     * @return The rendered content, which can be of any type depending on the renderer implementation.
     */
    fun render(configuration: Configuration, view: String, defaultTemplate: String, model: Map<String, Any?>): Any
}

