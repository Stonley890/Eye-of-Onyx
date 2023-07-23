package io.github.stonley890.eyeofonyx.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import org.bukkit.configuration.file.FileConfiguration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class AvailabilityHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        // Load the Thymeleaf template engine with the correct template resolver
        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/"); // This sets the path to the resources directory
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        templateEngine.setTemplateResolver(templateResolver);

        // Get the maxDaysConfigValue from config.yml
        FileConfiguration config = EyeOfOnyx.getPlugin().getConfig();
        int maxDaysConfigValue = config.getInt("time-selection-period");

        // Create a Thymeleaf context and add the maxDaysConfigValue as a variable
        Context context = new Context();
        context.setVariable("maxDaysConfigValue", maxDaysConfigValue);

        // Render the HTML template with Thymeleaf and get the final HTML content
        String renderedHTML = templateEngine.process("availability_form", context);

        // Set the response headers with the content type
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=" + StandardCharsets.UTF_8.name());
        httpExchange.sendResponseHeaders(200, renderedHTML.getBytes(StandardCharsets.UTF_8).length);

        // Get the response body and write the HTML content to it
        try (OutputStream outputStream = httpExchange.getResponseBody()) {
            outputStream.write(renderedHTML.getBytes(StandardCharsets.UTF_8));
        }
    }


    private void sendResponse(HttpExchange httpExchange, int statusCode, String response) throws IOException {
        httpExchange.sendResponseHeaders(statusCode, response.length());
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(response.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}
