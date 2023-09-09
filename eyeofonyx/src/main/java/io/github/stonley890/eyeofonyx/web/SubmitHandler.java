package io.github.stonley890.eyeofonyx.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.commands.CmdChallenge;
import io.github.stonley890.eyeofonyx.files.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SubmitHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        if (httpExchange.getRequestMethod().equalsIgnoreCase("POST")) {

            // Parse the submitted form data
            InputStreamReader isr = new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String formData = br.readLine();

            // Parse the code
            String encodedCode = formData.split("code=")[1].split("&availability=")[0]; // Extracting code value from the form data
            String code = URLDecoder.decode(encodedCode, StandardCharsets.UTF_8.name());

            // Parse the availability dates and times
            List<LocalDateTime> availabilities = new ArrayList<>();
            String[] availabilityParams = formData.split("&availability=");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            try {
                for (int i = 1; i < availabilityParams.length; i++) {
                    // Extract only the date and time part from the availability string
                    String dateTimeString = availabilityParams[i].split("&")[0]; // Get the part before the "&"
                    dateTimeString = URLDecoder.decode(dateTimeString, StandardCharsets.UTF_8.name());
                    LocalDateTime dateTime = LocalDateTime.parse(dateTimeString, formatter);
                    availabilities.add(dateTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Now you have the code and the list of LocalDateTime objects representing availabilities.
            // You can further process or store this data as needed.

            Player player = null;

            // Check code match
            if (!CmdChallenge.codesOnForm.isEmpty()) {
                for (int i = 0; i < CmdChallenge.codesOnForm.size(); i++) {
                    if (CmdChallenge.codesOnForm.get(i).equals(code)) {

                        player = CmdChallenge.playersOnForm.get(i);

                        for (LocalDateTime availability : availabilities) {
                            if (availability.isBefore(LocalDateTime.now())) {
                                // Time is before now: invalid
                                player.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Invalid time! You cannot schedule a time before now!");

                                sendInvalid(httpExchange);

                                return;

                            } else {

                                // Time cannot be within 15 minutes of another challenge
                                try {
                                    for (Challenge challenge : Challenge.getChallenges()) {
                                        for (LocalDateTime time : challenge.time) {
                                            if (availability.isBefore(time.plusMinutes(15)) || availability.isAfter((time.minusMinutes(15)))) {
                                                player.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Invalid time! " + availability.format(DateTimeFormatter.ofPattern("MM/dd hh:mm a")) + " is within 15 minutes of a challenge at " + time.format(DateTimeFormatter.ofPattern("hh:mm a")));

                                                sendInvalid(httpExchange);

                                                return;
                                            }
                                        }
                                    }
                                } catch (InvalidConfigurationException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }


                        // Remove notification
                        try {
                            for (Notification notification : Notification.getNotificationsOfPlayer(player.getUniqueId().toString())) {
                                if (notification.type == NotificationType.CHALLENGE_REQUESTED) {
                                    Notification.getNotificationsOfPlayer(player.getUniqueId().toString()).remove(notification);
                                }
                            }
                        } catch (IOException | InvalidConfigurationException e) {
                            e.printStackTrace();
                        }

                        // Notify attacker
                        // Get their uuid from "challenging" key on board.yml
                        int playerTribe = RoyaltyBoard.getTribeIndexOfUsername(player.getName());
                        String attackerUuid = null;

                        for (int j = 1; j < RoyaltyBoard.getValidPositions().length; j++) {
                            String attacking = RoyaltyBoard.getAttacking(playerTribe, j);
                            if (attacking.equals(player.getUniqueId().toString())) {
                                attackerUuid = RoyaltyBoard.getUuid(playerTribe, j);
                            }
                        }

                        if (attackerUuid == null) {

                            Bukkit.getLogger().warning("Could not find attacker.");

                            // Could not find the attacker
                            Bukkit.getLogger().warning("Could not find attacker of player " + player.getUniqueId() + "\nTribe: " + playerTribe + "\n");
                            player.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "There was a problem finding your challenger. Please contact a staff member.");

                            sendInvalid(httpExchange);

                            // Set attacker in board.yml
                            RoyaltyBoard.setAttacker(playerTribe, RoyaltyBoard.getPositionIndexOfUUID(player.getUniqueId().toString()), attackerUuid);

                            // Create challenge
                            new Challenge(attackerUuid, player.getUniqueId().toString(), ChallengeType.UNKNOWN, availabilities).save();

                            // Remove code
                            CmdChallenge.codesOnForm.remove(i);
                            CmdChallenge.playersOnForm.remove(i);

                            return;
                        }

                        new Notification(attackerUuid, "Challenge Accepted!", player.getName() + " has accepted your challenge. Choose one of the times below.", NotificationType.CHALLENGE_ACCEPTED).create();

                        player.sendMessage(EyeOfOnyx.EOO + "Your availabilities have been recorded!");

                        break;
                    }
                }

                // Player was not found
                if (player == null) {
                    Bukkit.getLogger().warning("Could not find player from code.");

                    sendInvalid(httpExchange);

                    return;
                }

            } else {
                // No codes exist
                Bukkit.getLogger().warning("No codes exist.");

                sendInvalid(httpExchange);

                return;

            }

            // Success

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

    private void sendInvalid(HttpExchange httpExchange) throws IOException {
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
    }

    private void sendResponse(HttpExchange httpExchange, int statusCode, String response) throws IOException {
        httpExchange.sendResponseHeaders(statusCode, response.length());
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(response.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}