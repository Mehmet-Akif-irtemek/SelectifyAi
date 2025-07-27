package com.app.selectifyai;

import android.content.Context;
import android.util.Log; // Logcat için eklendi
import android.graphics.Bitmap;
import android.util.Base64;
import java.io.ByteArrayOutputStream;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * AI API etkileşimleri için yardımcı sınıf
 */
public class HELPER {
    private final Context context;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final Map<String, String> aiCache = new HashMap<>(); // Önbellek burada saklanacak

    public interface AICallback {
        void onResponse(String response);
        void onError(String error);
    }

    public HELPER(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    // getChoiceResponse metodu kaldırıldı, AIChatActivity'de çağrılmıyor.
    // Eğer kullanılıyorsa geri ekleyebilirsiniz.


    public void getContextualResponse(List<Message> chatHistory, String userInfo, AICallback callback) throws JSONException {
        JSONArray messages = new JSONArray();

        // Sistem mesajı: Bu prompt, AI'nın sohbet geçmişini nasıl yorumlayacağını belirler.
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", context.getString(R.string.ai_system_prompt1)));

        // userInfo'yu ayrı bir user mesajı olarak ekle
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", userInfo));

        // Sohbet geçmişi mesajları
        for (Message msg : chatHistory) {
            messages.put(new JSONObject()
                    .put("role", msg.isUser ? "user" : "assistant")
                    .put("content", msg.text));
        }

        // Önbellek anahtarı oluştur: Geçmiş ve kullanıcı bilgisi birleştirilerek hashlenir.
        String cacheKey = generateCacheKey(messages.toString() + userInfo);

        // Önbelleği kontrol et
        if (aiCache.containsKey(cacheKey)) {
            Log.d("AI_CACHE", "Yanıt önbellekten alındı.");
            callback.onResponse(aiCache.get(cacheKey));
            return;
        }

        // API isteği: Önbellekte yoksa, AI'a istek gönderilir.
        fetchAIResponseWithMessages(messages, cacheKey, callback);
    }

    private void fetchAIResponseWithMessages(JSONArray messages, String cacheKey, AICallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);

                String apiKey = KeyDecryptor.getGroqKey(); // Anahtarınızı buradan alıyorsunuz
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject data = new JSONObject();
                data.put("model", "moonshotai/kimi-k2-instruct");
                data.put("messages", messages); // Mesajlar JSON array'i gönderilir

                OutputStream os = conn.getOutputStream();
                os.write(data.toString().getBytes());
                os.flush();

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    java.util.Scanner s = new java.util.Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String result = s.hasNext() ? s.next() : "";

                    JSONObject response = new JSONObject(result);
                    String reply = response.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    String finalReply = reply.trim().replace("<think>", "");

                    // Yanıtı önbelleğe kaydet
                    aiCache.put(cacheKey, finalReply);
                    Log.d("AI_CACHE", "Yanıt önbelleğe kaydedildi.");

                    callback.onResponse(finalReply);
                } else {
                    Log.e("AI_API", "Groq API yanıtı başarısız: " + conn.getResponseCode() + " " + conn.getResponseMessage());
                    callback.onError("AI yanıtı alınamadı: " + conn.getResponseCode());
                }

            } catch (Exception e) {
                Log.e("AI_API", "API isteği sırasında hata: " + e.getMessage(), e);
                callback.onError(e.getMessage());
            } finally {
                // Bağlantıyı kapat
                // conn.disconnect(); // Gerekirse bağlantıyı burada kapatın
            }
        }).start();
    }

    // fetchAIResponse tek başına soru-cevap için (getYesNoResponse tarafından kullanılır)
    private void fetchAIResponse(String prompt, AICallback callback) {
        // Önbelleği kontrol et
        if (aiCache.containsKey(prompt)) {
            Log.d("AI_CACHE", "Yanıt önbellekten alındı (tekli prompt).");
            callback.onResponse(aiCache.get(prompt));
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);

                String apiKey = KeyDecryptor.getGroqKey();
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject data = new JSONObject();
                data.put("model", "meta-llama/llama-4-scout-17b-16e-instruct");

                JSONArray messages = new JSONArray();
                messages.put(new JSONObject()
                        .put("role", "system")
                        .put("content", context.getString(R.string.ai_system_prompt))); // Bu sistem prompt'u farklı olabilir
                messages.put(new JSONObject()
                        .put("role", "user")
                        .put("content", prompt));
                data.put("messages", messages);

                OutputStream os = conn.getOutputStream();
                os.write(data.toString().getBytes());
                os.flush();

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    java.util.Scanner s = new java.util.Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String result = s.hasNext() ? s.next() : "";

                    JSONObject response = new JSONObject(result);
                    String reply = response.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    String finalReply = reply.trim().replace("<think>", "");

                    // Yanıtı önbelleğe kaydet
                    aiCache.put(prompt, finalReply);
                    Log.d("AI_CACHE", "Yanıt önbelleğe kaydedildi (tekli prompt).");

                    callback.onResponse(finalReply);
                } else {
                    Log.e("AI_API", "Groq API yanıtı başarısız (tekli): " + conn.getResponseCode() + " " + conn.getResponseMessage());
                    callback.onError("AI yanıtı alınamadı: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                Log.e("AI_API", "API isteği sırasında hata (tekli): " + e.getMessage(), e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // MD5 hash oluşturma metodu - Önbellek anahtarı için güvenli bir yol
    private String generateCacheKey(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e("AI_CACHE", "MD5 algoritması bulunamadı, hashCode kullanılıyor.", e);
            return String.valueOf(input.hashCode()); // Hata olursa basit hashCode kullan
        }
    }

    public void sendImageAsBase64ToAI(Bitmap bitmap, String prompt, AICallback callback) {
        try {
            // Bitmap'i optimize et ve Base64'e çevir
            Bitmap resizedBitmap = resizeBitmapIfNeeded(bitmap);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // İlk olarak düşük kalitede dene
            int quality = 60;
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            byte[] imageBytes = baos.toByteArray();
            
            // Groq'un 4MB base64 limitini kontrol et
            while (imageBytes.length > 3 * 1024 * 1024 && quality > 20) { // 3MB güvenli limit
                baos.reset();
                quality -= 10;
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                imageBytes = baos.toByteArray();
                Log.d("AI_IMAGE_VISION", "Görsel boyutu optimize ediliyor, kalite: " + quality + ", boyut: " + imageBytes.length);
            }
            
            if (imageBytes.length > 4 * 1024 * 1024) {
                callback.onError("Görsel çok büyük. Lütfen daha küçük bir görsel seçin.");
                return;
            }
            
            String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            Log.d("AI_IMAGE_VISION", "Görsel base64 boyutu: " + base64Image.length() + " karakter");

            // Groq Vision API formatında mesaj oluştur
            JSONArray messages = new JSONArray();
            
            // Sistem mesajı - kullanıcının mesajına göre görseli yorumlayacak
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "Sen yardımçı bir AI asistanısın. Kullanıcının gönderdiği görselleri analiz edebilir ve kullanıcının mesajına göre görseli yorumlayabilirsin. Kullanıcının sorusuna veya isteğine göre görseli detaylı şekilde açıkla. Türkçe cevap ver ve samimi bir dil kullan. Görselde gördüklerini açık ve anlaşılır şekilde anlat."));

            // Kullanıcı mesajı - Groq Vision formatında
            JSONArray contentArray = new JSONArray();
            
            // Metin kısmı (kullanıcının görsel hakkındaki sorusu/yorumu)
            contentArray.put(new JSONObject()
                    .put("type", "text")
                    .put("text", prompt));
            
            // Görsel kısmı - Groq formatı
            contentArray.put(new JSONObject()
                    .put("type", "image_url")
                    .put("image_url", new JSONObject()
                            .put("url", "data:image/jpeg;base64," + base64Image)));

            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", contentArray));

            String cacheKey = generateCacheKey(prompt + base64Image.substring(0, Math.min(100, base64Image.length())));
            fetchAIResponseWithVision(messages, cacheKey, callback);
            
        } catch (Exception e) {
            Log.e("AI_IMAGE_VISION", "Görsel analiz hatası: " + e.getMessage());
            callback.onError("Görsel işlenemedi: " + e.getMessage());
        }
    }
    
    private Bitmap resizeBitmapIfNeeded(Bitmap bitmap) {
        // Groq'un 33 megapiksel limitini kontrol et
        int maxPixels = 33177600; // 33 megapiksel
        int currentPixels = bitmap.getWidth() * bitmap.getHeight();
        
        if (currentPixels <= maxPixels) {
            return bitmap;
        }
        
        // Oranı koru ve boyutu küçült
        double ratio = Math.sqrt((double) maxPixels / currentPixels);
        int newWidth = (int) (bitmap.getWidth() * ratio);
        int newHeight = (int) (bitmap.getHeight() * ratio);
        
        Log.d("AI_IMAGE_VISION", "Görsel yeniden boyutlandırılıyor: " + bitmap.getWidth() + "x" + bitmap.getHeight() + " -> " + newWidth + "x" + newHeight);
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private void fetchAIResponseWithVision(JSONArray messages, String cacheKey, AICallback callback) {
        // Önbelleği kontrol et
        if (aiCache.containsKey(cacheKey)) {
            Log.d("AI_CACHE", "Görsel analiz yanıtı önbellekten alındı.");
            callback.onResponse(aiCache.get(cacheKey));
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(20000); // Görsel analizi için daha uzun timeout
                conn.setReadTimeout(40000);

                String apiKey = KeyDecryptor.getGroqKey();
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject data = new JSONObject();
                data.put("model", "meta-llama/llama-4-scout-17b-16e-instruct"); // Groq vision model
                data.put("messages", messages);
                data.put("max_tokens", 1024);
                data.put("temperature", 0.7);
                data.put("top_p", 1);
                data.put("stream", false);

                Log.d("AI_VISION", "Groq Vision API'ye istek gönderiliyor...");
                
                OutputStream os = conn.getOutputStream();
                os.write(data.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("AI_VISION", "API yanıt kodu: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    java.util.Scanner s = new java.util.Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                    String result = s.hasNext() ? s.next() : "";
                    s.close();

                    Log.d("AI_VISION", "API yanıtı alındı, boyut: " + result.length());

                    JSONObject response = new JSONObject(result);
                    if (response.has("choices") && response.getJSONArray("choices").length() > 0) {
                        String reply = response.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        String finalReply = reply.trim();

                        // Yanıtı önbelleğe kaydet
                        aiCache.put(cacheKey, finalReply);
                        Log.d("AI_CACHE", "Görsel analiz yanıtı önbelleğe kaydedildi.");

                        callback.onResponse(finalReply);
                    } else {
                        Log.e("AI_VISION", "API yanıtında choices bulunamadı: " + result);
                        callback.onError("Geçersiz API yanıtı");
                    }
                } else {
                    // Hata mesajını oku
                    String errorMsg = "";
                    try {
                        java.util.Scanner s = new java.util.Scanner(conn.getErrorStream(), "UTF-8").useDelimiter("\\A");
                        errorMsg = s.hasNext() ? s.next() : "";
                        s.close();
                        Log.e("AI_VISION", "API hata yanıtı: " + errorMsg);
                    } catch (Exception e2) {
                        Log.e("AI_VISION", "Hata mesajı okunamadı: " + e2.getMessage());
                    }
                    
                    Log.e("AI_VISION", "Görsel analiz API yanıtı başarısız: " + responseCode);
                    callback.onError("Görsel analizi başarısız (" + responseCode + "): " + errorMsg);
                }
            } catch (Exception e) {
                Log.e("AI_VISION", "Görsel analiz API hatası: " + e.getMessage(), e);
                callback.onError("Görsel analizi hatası: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    public static class Message {
        public boolean isUser;
        public String text;

        public Message(boolean isUser, String text) {
            this.isUser = isUser;
            this.text = text;
        }
    }
}