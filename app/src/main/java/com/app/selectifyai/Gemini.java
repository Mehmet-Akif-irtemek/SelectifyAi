package com.app.selectifyai;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Gemini {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    public interface GroqCallback {
        void onResponse(List<PoiModel> filteredPois);
        void onFailure(String error);
    }

    public static void getLogicalPOIs(Context context, List<PoiModel> pois, String mod, String yas, String sorumluluk,
                                      String hava, String il, String ilce, GroqCallback callback) {
        try {
            JSONArray poiArray = new JSONArray();
            for (PoiModel poi : pois) {
                JSONObject obj = new JSONObject();
                obj.put("ad", poi.getAd());
                obj.put("kategori", poi.getTur());
                poiArray.put(obj);
            }

            String prompt = "Mod: " + mod + ", yaş: " + yas + ", sorumluluk: " + sorumluluk +
                    ", hava: " + hava + ", konum: " + il + "/" + ilce + ". " +
                    "Aşağıdaki yerler arasından en az 10 tanesini seç. Yakın ve bilindik mekanlara öncelik ver. " +
                    "Yağmurluysa kapalı, güneşliyse açık alanlar öner. Gençler için sosyal, sorumluluğu yüksekse sakin yerleri tercih et. " +
                    "Yanıt sadece mekan adlarından oluşan sade bir JSON dizisi olsun. Açıklama, yorum, kod bloğu yazma. " +
                    "Örnek: [\"Starbucks\", \"Kütüphane\"]\n\nYerler:\n" + poiArray.toString();

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "moonshotai/kimi-k2-instruct");

            JSONArray messages = new JSONArray();
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.put(userMsg);

            jsonBody.put("messages", messages);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, GROQ_URL, jsonBody,
                    response -> {
                        try {
                            Log.d("GroqResponse", response.toString());

                            String resultText = response
                                    .getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content")
                                    .trim();

                            resultText = resultText.replace("```json", "")
                                    .replace("```", "")
                                    .trim();

                            JSONArray filteredNames = new JSONArray(resultText);
                            List<PoiModel> filteredPois = new ArrayList<>();

                            for (int i = 0; i < filteredNames.length(); i++) {
                                String name = filteredNames.getString(i).toLowerCase(Locale.ROOT);
                                for (PoiModel poi : pois) {
                                    if (poi.getAd().toLowerCase(Locale.ROOT).contains(name)) {
                                        filteredPois.add(poi);
                                        break;
                                    }
                                }
                            }
                            callback.onResponse(filteredPois);
                        } catch (Exception e) {
                            Log.e("GroqParseError", e.toString());
                            callback.onFailure("İşleme hatası: " + e.getMessage());
                        }
                    },
                    error -> {
                        Log.e("GroqVolleyError", error.toString());
                        callback.onFailure("Groq isteği başarısız: " + error.toString());
                    }) {
                @Override
                public java.util.Map<String, String> getHeaders() {
                    java.util.Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Authorization", "Bearer " + KeyDecryptor.getGroqKey());
                    return headers;
                }
            };

            RequestQueue queue = Volley.newRequestQueue(context);
            queue.add(request);

        } catch (Exception e) {
            Log.e("GroqJSONError", e.toString());
            callback.onFailure("JSON hatası: " + e.getMessage());
        }
    }
}