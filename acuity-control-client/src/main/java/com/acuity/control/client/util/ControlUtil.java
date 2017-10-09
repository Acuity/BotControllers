package com.acuity.control.client.util;

import com.acuity.db.util.Json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 10/9/2017.
 */
public class ControlUtil {

    public static final String HOST = "174.53.192.24";

    public static HashMap<String, Object> getGlobalInfoDoc() throws IOException {
        return getGlobalInfoDoc("http://" + HOST + ":8081/GlobalInfo");
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, Object> getGlobalInfoDoc(String url) throws IOException {
        String json;
        URLConnection conn = new URL(url).openConnection();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            json = reader.lines().collect(Collectors.joining("\n"));
        }

        return Json.GSON.fromJson(json, HashMap.class);
    }
}
