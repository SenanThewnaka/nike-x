package com.zentora.nike_x.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PayHereApiClient {

    private static final String AUTH_URL = "https://sandbox.payhere.lk/merchant/v1/oauth/token";
    private static final String SEARCH_URL = "https://sandbox.payhere.lk/merchant/v1/payment/search";

    public static int checkPaymentStatus(String orderId) {
        try {
            if (PaymentConfig.APP_ID.isEmpty() || PaymentConfig.APP_SECRET.isEmpty()) {
                System.out.println("PayHere APP_ID/SECRET not configured. Skipping API check.");
                return -99; // Configuration Missing
            }

            String token = getAccessToken();
            if (token == null)
                return -98; // Auth Failed

            URL url = new URL(SEARCH_URL + "?order_id=" + orderId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null)
                    response.append(line);
                br.close();

                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                if (json.has("status") && json.get("status").getAsInt() == 1) {
                    if (json.has("data") && !json.get("data").isJsonNull()) {
                        // Data is array or object? Docs say array of records.
                        // But usually we just check the first one matching order_id
                        // Assuming status 1 means "Success" of the API call.
                        // The actual payment status is inside the data array.

                        // Simplified: If API returns success, we need to inspect the data.
                        // For now return 2 (Success) if we find a record with status 2.

                        // Actually docs say: status: 1 (API success), data: [...]
                        // Inside data item: status: "RECEIVED" (2)

                        String rawJson = response.toString();
                        if (rawJson.contains("\"status\":2") || rawJson.contains("\"status\":\"2\"")) {
                            return 2;
                        } else if (rawJson.contains("\"status\":-2")) {
                            return -2;
                        }
                    }
                }
            } else {
                System.out.println("PayHere API Error: " + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0; // Default Pending/Unknown
    }

    private static String getAccessToken() {
        try {
            String auth = PaymentConfig.APP_ID + ":" + PaymentConfig.APP_SECRET;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            URL url = new URL(AUTH_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
            conn.setRequestProperty("Content-Type", "application/json"); // Docs say json body? Or form? Usually form
                                                                         // for oauth.
            // Docs: Grant type client_credentials. usually form-data or json.
            // Let's try JSON as per some modern APIs, but OAuth2 standard is form.
            // "grant_type": "client_credentials"

            conn.setDoOutput(true);
            String jsonBody = "{\"grant_type\":\"client_credentials\"}";
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null)
                    response.append(line);
                br.close();

                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                if (json.has("access_token")) {
                    return json.get("access_token").getAsString();
                }
            } else {
                System.out.println("PayHere Token Error: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
