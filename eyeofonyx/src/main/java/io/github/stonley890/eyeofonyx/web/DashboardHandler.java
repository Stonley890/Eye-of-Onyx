package io.github.stonley890.eyeofonyx.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.stonley890.dreamvisitor.Bot;
import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.mokulu.discord.oauth.DiscordAPI;
import io.mokulu.discord.oauth.DiscordOAuth;
import io.mokulu.discord.oauth.model.TokensResponse;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class DashboardHandler implements HttpHandler {
    @Override
    public void handle(@NotNull HttpExchange httpExchange) throws IOException {

        String apiEndpoint = "https://discord.com/api/v10";
        String clientId = Bot.getJda().getSelfUser().getApplicationId();
        String clientSecret = Bot.getJda().getToken();
        String redirectUri = "http://" + EyeOfOnyx.getPlugin().getConfig().getString("address") + ":" + EyeOfOnyx.getPlugin().getConfig().getString("port") + "/dashboard";

        DiscordOAuth oauthHandler = new DiscordOAuth(clientId, clientSecret, redirectUri, new String[]{"identify"});

        // Parse the submitted form data
        InputStreamReader isr = new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        String formData = br.readLine();

        Dreamvisitor.debug("FORM DATA: " + formData);

        // Parse the code
        String encodedCode = formData.split("code=")[1].split("&state=")[0]; // Extracting code value from the form data
        String code = URLDecoder.decode(encodedCode, StandardCharsets.UTF_8.name());

        Dreamvisitor.debug(encodedCode + "AND" + code);

        // Load the Thymeleaf template engine with the correct template resolver
        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/"); // This sets the path to the resources directory
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        templateEngine.setTemplateResolver(templateResolver);

        String page = "dashboard_auth";

        if (code != null && !code.isEmpty()) {
            TokensResponse tokens = oauthHandler.getTokens(code);
            String accessToken = tokens.getAccessToken();
            String refreshToken = tokens.getRefreshToken();

            DiscordAPI discordAPI = new DiscordAPI(accessToken);

            String id = discordAPI.fetchUser().getId();

            Member member = Bot.gameLogChannel.getGuild().retrieveMemberById(id).complete();

            if (member != null && member.hasPermission(Permission.MANAGE_ROLES)) page = "dashboard_main";

        }

        // Get the maxDaysConfigValue from config.yml
        FileConfiguration config = EyeOfOnyx.getPlugin().getConfig();
        int maxDaysConfigValue = config.getInt("time-selection-period");

        // Create a Thymeleaf context and add the maxDaysConfigValue as a variable
        Context context = new Context();
        context.setVariable("maxDaysConfigValue", maxDaysConfigValue);

        // Render the HTML template with Thymeleaf and get the final HTML content
        String renderedHTML = templateEngine.process(page, context);

        // Set the response headers with the content type
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=" + StandardCharsets.UTF_8.name());
        httpExchange.sendResponseHeaders(200, renderedHTML.getBytes(StandardCharsets.UTF_8).length);

        // Get the response body and write the HTML content to it
        try (OutputStream outputStream = httpExchange.getResponseBody()) {
            outputStream.write(renderedHTML.getBytes(StandardCharsets.UTF_8));
        }

    }
}
