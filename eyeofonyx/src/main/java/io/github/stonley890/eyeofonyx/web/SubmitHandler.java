package io.github.stonley890.eyeofonyx.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.stonley890.dreamvisitor.Main;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.commands.CmdChallenge;
import io.github.stonley890.eyeofonyx.files.*;
import javassist.NotFoundException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SubmitHandler implements HttpHandler {
    @Override
    public void handle(@NotNull HttpExchange httpExchange) throws IOException {

        if (httpExchange.getRequestMethod().equalsIgnoreCase("POST")) {

            // Parse the submitted form data
            InputStreamReader isr = new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String formData = br.readLine();

            Main.debug("FORM DATA: " + formData);

            // Parse the code
            String encodedCode = formData.split("code=")[1].split("&availability=")[0]; // Extracting code value from the form data
            String code = URLDecoder.decode(encodedCode, StandardCharsets.UTF_8);

            Main.debug(encodedCode + "AND" + code);

            // Parse the availability dates and times
            List<LocalDateTime> availabilities = new ArrayList<>();
            String[] availabilityParams = formData.split("availability=");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            try {
                if (availabilityParams.length == 0) {
                    Main.debug("No dates.");
                    sendInvalid(httpExchange, "You did not send any dates! Go back and add at least one availability.");
                    return;
                }
                for (int i = 1; i < availabilityParams.length; i++) {
                    // Extract only the date and time part from the availability string
                    Main.debug("Processing string: " + availabilityParams[i]);
                    String[] splitAvailabilities = availabilityParams[i].split("&");
                    if (splitAvailabilities.length == 0) {
                        break;
                    }
                    String dateTimeString = splitAvailabilities[0]; // Get the part before the "&"
                    dateTimeString = URLDecoder.decode(dateTimeString, StandardCharsets.UTF_8);
                    Main.debug("Processed string: " + dateTimeString);
                    if (!dateTimeString.isEmpty() && !dateTimeString.equals("&")) {
                        Main.debug("Added time " + dateTimeString);

                        // Parse
                        LocalDateTime parsedTime = LocalDateTime.parse(dateTimeString, formatter);
                        // Get as UTC
                        ZonedDateTime zDateTime = ZonedDateTime.of(parsedTime, ZoneId.of("UTC"));
                        // Convert to LocalDateTime
                        LocalDateTime lDateTime = zDateTime.toLocalDateTime();
                        // Add to list
                        availabilities.add(lDateTime);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Main.debug("Values obtained.");
            // Now you have the code and the list of LocalDateTime objects representing availabilities.
            // You can further process or store this data as needed.

            Player player = null;

            Main.debug("Checking code match.");

            // Check code match
            if (!CmdChallenge.codesOnForm.isEmpty()) {
                for (int i = 0; i < CmdChallenge.codesOnForm.size(); i++) {
                    if (CmdChallenge.codesOnForm.get(i).equals(code)) {

                        Main.debug("Found code match.");

                        player = CmdChallenge.playersOnForm.get(i);

                        for (LocalDateTime availability : availabilities) {
                            if (availability.isBefore(LocalDateTime.now())) {
                                Main.debug("Invalid; time is before now.");
                                // Time is before now: invalid
                                player.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Invalid time! You cannot schedule a time before now!");

                                sendInvalid(httpExchange, "One of your times was invalid! You submitted a date that is before now!");

                                return;

                            } else {

                                // Time cannot be within 30 minutes of another challenge
                                try {
                                    for (Challenge challenge : Challenge.getChallenges()) {
                                        for (LocalDateTime time : challenge.time) {
                                            if (availability.isBefore(time.plusMinutes(30)) || availability.isAfter((time.minusMinutes(30)))) {
                                                Main.debug("Invalid; time is within 30 mins of another");

                                                player.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Invalid time! " + availability.format(DateTimeFormatter.ofPattern("MM/dd hh:mm a")) + " is within 30 minutes of a challenge at " + time.format(DateTimeFormatter.ofPattern("hh:mm a")));

                                                sendInvalid(httpExchange, "One of your times was invalid! " + availability.format(DateTimeFormatter.ofPattern("MM/dd hh:mm a")) + " is within 30 minutes of a challenge at " + time.format(DateTimeFormatter.ofPattern("hh:mm a")));

                                                return;
                                            }
                                        }
                                    }
                                } catch (InvalidConfigurationException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }

                        Main.debug("No availability issues");

                        // Remove notification
                        try {
                            for (Notification notification : Notification.getNotificationsOfPlayer(player.getUniqueId())) {
                                if (notification.type == NotificationType.CHALLENGE_REQUESTED) {
                                    Notification.removeNotification(notification);
                                    Main.debug("Removed notification.");
                                }
                            }
                        } catch (IOException | InvalidConfigurationException e) {
                            e.printStackTrace();
                        }

                        int playerTribe = 0;
                        try {
                            playerTribe = PlayerTribe.getTribeOfPlayer(player.getUniqueId());
                        } catch (NotFoundException e) {
                            // Player has no associated tribe (should not happen)
                            sendInvalid(httpExchange, "You do not have an associated tribe! Contact a staff member.");
                            player.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You do not have an associated tribe! Contact a staff member.");
                        }

                        Main.debug("Finding attacker.");
                        // Notify attacker
                        UUID attackerUuid = null;
                        try {
                            attackerUuid = RoyaltyBoard.getAttacker(playerTribe, RoyaltyBoard.getPositionIndexOfUUID(player.getUniqueId()));
                        } catch (NotFoundException e) {
                            // Player has no associated tribe (should not happen)
                            sendInvalid(httpExchange, "You do not have an associated tribe! Contact a staff member.");
                            player.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You do not have an associated tribe! Contact a staff member.");
                        }

                        if (attackerUuid == null) {

                            Bukkit.getLogger().warning("Could not find attacker.");

                            // Could not find the attacker
                            Bukkit.getLogger().warning("Could not find attacker of player " + player.getUniqueId() + "\nTribe: " + playerTribe + "\n");
                            player.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "There was a problem finding your challenger. Please contact a staff member.");

                            sendInvalid(httpExchange, "There was a problem finding your challenger. Please contact a staff member.");
                            return;
                        }

                        // Set attacker in board.yml
                        try {
                            RoyaltyBoard.setAttacker(playerTribe, RoyaltyBoard.getPositionIndexOfUUID(player.getUniqueId()), attackerUuid);
                        } catch (NotFoundException e) {
                            // Player has no associated tribe (should not happen)
                            sendInvalid(httpExchange, "You do not have an associated tribe! Contact a staff member.");
                            player.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You do not have an associated tribe! Contact a staff member.");
                        }
                        Main.debug("Set data in board.yml");

                        // Modify challenge
                        UUID finalAttackerUuid = attackerUuid;
                        Player finalPlayer = player;
                        Bukkit.getScheduler().runTaskAsynchronously(EyeOfOnyx.getPlugin(), () -> {
                            Challenge challenge = Challenge.getChallengeOfPlayers(finalAttackerUuid, finalPlayer.getUniqueId());
                            if (challenge == null) {
                                finalPlayer.sendMessage(EyeOfOnyx.EOO + net.md_5.bungee.api.ChatColor.RED + "Your challenge could not be found! Contact a staff member.");
                                return;
                            }
                            challenge.state = Challenge.State.ACCEPTED;
                        });

                        Main.debug("Created the challenge");

                        // Remove code
                        CmdChallenge.codesOnForm.remove(i);
                        CmdChallenge.playersOnForm.remove(i);

                        player.sendMessage(EyeOfOnyx.EOO + "Your availabilities have been recorded!");

                        // report
                        RoyaltyBoard.report(player.getName(), player.getName() + " chose possible times for their challenge.");

                        break;
                    }
                }

                // Player was not found
                if (player == null) {
                    Bukkit.getLogger().warning("Could not find player from code.");

                    sendInvalid(httpExchange, "Your code is invalid or expired.");

                    return;
                }

            } else {
                // No codes exist
                Bukkit.getLogger().warning("No codes exist.");

                sendInvalid(httpExchange, "Your code is invalid or expired.");

                return;

            }

            // Success
            Main.debug("Success");

            // Send a response to the client
            InputStream inputStream = EyeOfOnyx.getPlugin().getResource("availability_done.html");
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

        } else {
            // If the request method is not POST, send an error response
            sendResponse(httpExchange, 405, "Method not allowed.");
        }
    }

    //
    //
    //

    private void sendInvalid(HttpExchange httpExchange, String errorExplanation) throws IOException {

        // Load the Thymeleaf template engine with the correct template resolver
        String renderedHTML = getHtml(errorExplanation);

        // Set the response headers with the content type
        // httpExchange.getResponseHeaders().clear();
        // httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=" + StandardCharsets.UTF_8.name());

        // Get the response body and write the HTML content to it
        // try (OutputStream outputStream = httpExchange.getResponseBody()) {
        //    outputStream.write(renderedHTML.getBytes(StandardCharsets.UTF_8));
        // }


        // httpExchange.sendResponseHeaders(200, renderedHTML.getBytes(StandardCharsets.UTF_8).length);

        sendResponse(httpExchange, 200, renderedHTML);

        /*
        // Send a response to the client
        InputStream inputStream = EyeOfOnyx.getPlugin().getResource("availability_invalid.html");
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

        sendResponse(httpExchange, 200, responseBuilder.toString());

         */
    }

    private static String getHtml(String errorExplanation) {
        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/"); // This sets the path to the resources directory
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        templateEngine.setTemplateResolver(templateResolver);

        // Create a Thymeleaf context and add the maxDaysConfigValue as a variable
        Context context = new Context();
        context.setVariable("errorExplanation", errorExplanation);

        // Render the HTML template with Thymeleaf and get the final HTML content
        return templateEngine.process("availability_invalid", context);
    }

    private void sendResponse(@NotNull HttpExchange httpExchange, int statusCode, @NotNull String response) throws IOException {
        httpExchange.sendResponseHeaders(statusCode, response.length());
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(response.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}