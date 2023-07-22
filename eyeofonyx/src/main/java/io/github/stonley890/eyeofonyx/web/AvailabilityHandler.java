package io.github.stonley890.eyeofonyx.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AvailabilityHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {


        InputStream inputStream = EyeOfOnyx.getPlugin().getResource("availability_form.html");
        if (inputStream == null) {
            // Handle the case where the resource is not found
            sendResponse(httpExchange, 404, "Resource not found.");
            return;
        }

        // Read the contents of availability_form.html from the InputStream
        StringBuilder responseBuilder = new StringBuilder();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            responseBuilder.append(new String(buffer, 0, bytesRead));
        }

        // Close the InputStream after reading
        inputStream.close();

        // Set the response headers
        httpExchange.sendResponseHeaders(200, responseBuilder.length());

        // Get the response body and write the HTML content to it
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(responseBuilder.toString().getBytes());

        // Close the streams
        outputStream.flush();
        outputStream.close();
    }


    private void sendResponse(HttpExchange httpExchange, int statusCode, String response) throws IOException {
        httpExchange.sendResponseHeaders(statusCode, response.length());
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(response.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}
