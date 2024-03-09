package io.github.stonley890.eyeofonyx.web;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;

public class IpUtils {
    static HashMap<String, JSONObject> ipStorage = new HashMap<>();

    /**
     * Gets the ZonedDateTime of a given IP address.
     * @param ip The IP to get the timezone of.
     * @return The current ZonedDateTime with offset. Null if the address can't be reached.
     */
    public static @Nullable ZonedDateTime ipToTime(String ip) {
        String offset;
        if (ipStorage.containsKey(ip)) {
            offset = (String) ipStorage.get(ip).get("timeZone");
        } else {
            String url = "http://api.ipinfodb.com/v3/ip-city/?key=d7859a91e5346872d0378a2674821fbd60bc07ed63684c3286c083198f024138&ip=" + ip + "&format=json";
            JSONObject object;
            try {
                object = stringToJSON(getUrlSource(url));
            } catch (NullPointerException e) {
                return null;
            }
            String timezone = (String) object.get("timeZone");
            if (timezone != null && timezone.length() > 3) {
                offset = timezone;
                ipStorage.put(ip, object);
            } else {
                return null;
            }

        }
        return ZonedDateTime.now(ZoneId.of(offset));
    }

    public static String getCityName(String ip) {
        JSONObject obj;
        if (ipStorage.containsKey(ip)) {
            obj = ipStorage.get(ip);
        } else {
            String url = "[URL]http://api.ipinfodb.com/v3/ip-city/?key=d7859a91e5346872d0378a2674821fbd60bc07ed63684c3286c083198f024138&ip=[/url]" + ip + "&format=json";
            JSONObject object = stringToJSON(getUrlSource(url));
            obj = object;
            ipStorage.put(ip, object);
        }
        return (String) obj.get("cityName");
    }

    public static String getStateName(String ip) {
        JSONObject obj;
        if (ipStorage.containsKey(ip)) {
            obj = ipStorage.get(ip);
        } else {
            String url = "[url]http://api.ipinfodb.com/v3/ip-city/?key=d7859a91e5346872d0378a2674821fbd60bc07ed63684c3286c083198f024138&ip=[/url]" + ip + "&format=json";
            JSONObject object = stringToJSON(getUrlSource(url));
            obj = object;
            ipStorage.put(ip, object);
        }
        return (String) obj.get("regionName");
    }

    public static String getCountryName(String ip) {
        JSONObject obj;
        if (ipStorage.containsKey(ip)) {
            obj = ipStorage.get(ip);
        } else {
            String url = "[url]http://api.ipinfodb.com/v3/ip-city/?key=d7859a91e5346872d0378a2674821fbd60bc07ed63684c3286c083198f024138&ip=[/url]" + ip + "&format=json";
            JSONObject object = stringToJSON(getUrlSource(url));
            obj = object;
            ipStorage.put(ip, object);
        }
        String country = (String) obj.get("countryName");
        if (country.contains(",")) {
            country = country.split(",")[0];
        }
        return country;
    }

    public static String getCountryCode(String ip) {
        JSONObject obj;
        if (ipStorage.containsKey(ip)) {
            obj = ipStorage.get(ip);
        } else {
            String url = "http://api.ipinfodb.com/v3/ip-city/?key=d7859a91e5346872d0378a2674821fbd60bc07ed63684c3286c083198f024138&ip=" + ip + "&format=json";
            JSONObject object = stringToJSON(getUrlSource(url));
            obj = object;
            ipStorage.put(ip, object);
        }
        return (String) obj.get("countryCode");
    }

    public static void clearCache(String ip) {
        ipStorage.remove(ip);
    }

    private static JSONObject stringToJSON(String json) {
        return (JSONObject) JSONValue.parse(json);
    }

    private static @NotNull String getUrlSource(String url) {
        URL url2 = null;
        try {
            url2 = new URL(url);
        } catch (MalformedURLException ignored) {
        }
        URLConnection yc = null;
        try {
            yc = url2.openConnection();
        } catch (IOException ignored) {
        } catch (NullPointerException e) {
            Bukkit.getLogger().info("Could not get details of page " + url);
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(yc.getInputStream(), StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
        String inputLine;
        StringBuilder a = new StringBuilder();
        try {
            while ((inputLine = in.readLine()) != null)
                a.append(inputLine);
        } catch (IOException ignored) {
        }
        try {
            in.close();
        } catch (IOException ignored) {
        }

        return a.toString();
    }
}