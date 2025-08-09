package xyz.daimones.ktor.panel

import io.ktor.server.freemarker.*
import io.ktor.server.mustache.*
import io.ktor.server.thymeleaf.*

class MustacheTemplateRenderer : TemplateRenderer {
    override fun render(
        configuration: Configuration,
        view: String,
        defaultTemplate: String,
        model: Map<String, Any>
    ): Any {
        val customTemplate = getCustomTemplate(view, configuration)
        return MustacheContent(customTemplate ?: defaultTemplate, model)
    }
}

class FreeMarkerTemplateRenderer : TemplateRenderer {
    override fun render(
        configuration: Configuration,
        view: String,
        defaultTemplate: String,
        model: Map<String, Any>
    ): Any {
        val customTemplate = getCustomTemplate(view, configuration)
        return FreeMarkerContent(customTemplate ?: defaultTemplate, model)
    }
}

class ThymeleafTemplateRenderer : TemplateRenderer {
    override fun render(
        configuration: Configuration,
        view: String,
        defaultTemplate: String,
        model: Map<String, Any>
    ): Any {
        val customTemplate = getCustomTemplate(view, configuration)
        return ThymeleafContent(customTemplate ?: defaultTemplate, model)
    }
}