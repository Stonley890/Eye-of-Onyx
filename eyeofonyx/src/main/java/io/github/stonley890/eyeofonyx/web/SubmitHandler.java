package io.github.stonley890.eyeofonyx.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.commands.CmdChallenge;
import io.github.stonley890.eyeofonyx.files.Notification;
import io.github.stonley890.eyeofonyx.files.NotificationType;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
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
            String code = formData.split("&")[0].split("=")[1]; // Extracting code value from the form data

            // Parse the availability dates and times
            List<LocalDateTime> availabilities = new ArrayList<>();
            String[] availabilityParams = formData.split("&availability=");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            try {
                for (int i = 1; i < availabilityParams.length; i++) {
                    String dateTimeString = URLDecoder.decode(availabilityParams[i], StandardCharsets.UTF_8.name());
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
                        Bukkit.getLogger().info("Found match.");

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

                            // Could not find attacker
                            Bukkit.getLogger().warning("Could not find attacker of player " + player.getUniqueId() + "\nTribe: " + playerTribe + "\n");
                            player.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "There was a problem finding your challenger. Please contact a staff member.");

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

                            // Set attacker in board.yml
                            RoyaltyBoard.setAttacker(playerTribe, RoyaltyBoard.getPositionIndexOfUUID(player.getUniqueId().toString()), attackerUuid);

                            //
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

                    return;
                }

            } else {
                // No codes exist
                Bukkit.getLogger().warning("No codes exist.");

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

    private void sendResponse(HttpExchange httpExchange, int statusCode, String response) throws IOException {
        httpExchange.sendResponseHeaders(statusCode, response.length());
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(response.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}
