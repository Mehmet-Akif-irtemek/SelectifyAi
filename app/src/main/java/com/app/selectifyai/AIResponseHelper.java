
package com.app.selectifyai;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.io.ByteArrayOutputStream;
import android.graphics.Bitmap;
import android.util.Base64;

public class AIResponseHelper {

    private final Context context;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final Map<String, String> aiCache = new HashMap<>();

    public interface AICallback {
        void onResponse(String response);
        void onError(String error);
    }

    public AIResponseHelper(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public void getChoiceResponse(
            @NonNull List<String> choices,
            @NonNull String userInfo,
            @NonNull AICallback callback
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(userInfo).append("\n")
                .append(context.getString(R.string.prompt_choice_intro)).append("\n");
        for (int i = 0; i < choices.size(); i++) {
            prompt.append(
                    String.format(
                            Locale.ROOT,
                            context.getString(R.string.prompt_choice_item_format),
                            i + 1,
                            choices.get(i)
                    )
            ).append("\n");
        }
        prompt.append(context.getString(R.string.prompt_choice_suffix)).append("\n");
        fetchAIResponse(prompt.toString(), callback);
    }

    public void getYesNoResponse(
            @NonNull String question,
            @NonNull String userInfo,
            @NonNull AICallback callback
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(userInfo).append("\n")
                .append(context.getString(R.string.prompt_yesno_intro)).append("\n")
                .append(
                        String.format(
                                Locale.ROOT,
                                context.getString(R.string.prompt_yesno_question),
                                question
                        )
                ).append("\n")
                .append(context.getString(R.string.prompt_yesno_suffix)).append("\n");
        fetchAIResponse(prompt.toString(), callback);
    }
    // AI cevabını hem answer hem gerekçe için analiz eden fonksiyon
    private static String[] parseAnswerAndJustification(String aiResponse) {
        if (aiResponse == null) return new String[]{"", ""};

        // 1. "answer:" veya "cevap:" ile başlıyorsa, gerekçeyi ayıkla
        Pattern p = Pattern.compile(
                "(?i)^(?:Answer|Cevap)[\\s\\-:=]+(.+?)(?:(?:\\s+)?(?:Justification|Gerekçe|Reason(?:ing|ıng|ıon|ıngs)?|Açıklama|Sebep|Neden|Rationale|Motivasyon)[\\s\\-:=]+(.+))?$"
        );
        Matcher m = p.matcher(aiResponse.trim());
        if (m.find()) {
            String answer = m.group(1).trim();
            String justification = m.group(2) != null ? m.group(2).trim() : "";
            return new String[]{answer, justification};
        }

        // 2. Gerekçe başlıklarından biriyle ayır ("reasoning", "reason", "gerekçe", "açıklama", ... hepsi!)
        p = Pattern.compile(
                "(?i)^(.+?)[\\s\\-:=]+(?:Justification|Gerekçe|Reason(?:ing|ıng|ıon|ıngs)?|Açıklama|Sebep|Neden|Rationale|Motivasyon)[\\s\\-:=]+(.+)$"
        );
        m = p.matcher(aiResponse.trim());
        if (m.find()) {
            String answer = m.group(1).trim();
            String justification = m.group(2).trim();
            return new String[]{answer, justification};
        }

        // 3. Son çare: Tek kelime cevap, geri kalan gerekçe
        String[] parts = aiResponse.split("\\s+", 2);
        if (parts.length == 2) {
            return new String[]{parts[0].trim(), parts[1].trim()};
        }
        return new String[]{aiResponse.trim(), ""};
    }
    public void getEmojiResponse(
            @NonNull String text,
            @NonNull AICallback callback
    ) {
        String prompt = context.getString(R.string.prompt_emoji_intro)
                + "\n"
                + text;
        fetchAIResponse(prompt, callback);
    }
    @SuppressLint("StringFormatInvalid")
    private void fetchAIResponse(
            @NonNull String prompt,
            @NonNull AICallback callback
    ) {
        final String normalizedPrompt = prompt
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);

        if (aiCache.containsKey(normalizedPrompt)) {
            callback.onResponse(aiCache.get(normalizedPrompt));
            return;
        }

        new Thread(() -> {
            try {
                String apiUrl    = context.getString(R.string.api_url);
                String modelName = context.getString(R.string.api_model);
                String apiKey    = KeyDecryptor.getGroqKey();

                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(15_000);
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject data = new JSONObject();
                data.put("model", modelName);
                data.put("temperature", 0.0);
                data.put("top_p", 0.0);

                SharedPreferences prefs = context
                        .getSharedPreferences("ayarlar", Context.MODE_PRIVATE);
                String languageCode = prefs.getString("dil", "tr");
                Log.d("AI_LANG", "Kullanıcının tercih ettiği dil: " + languageCode);

                JSONArray messages = new JSONArray();
                String systemMsg = context.getString(
                        R.string.system_message_template,
                        languageCode
                );
                messages.put(
                        new JSONObject()
                                .put("role", "system")
                                .put("content", systemMsg)
                );
                messages.put(
                        new JSONObject()
                                .put("role", "user")
                                .put("content", prompt) // or normalizedPrompt if you want to keep normalization
                );
                data.put("messages", messages);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(data.toString().getBytes());
                    os.flush();
                }

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    String result;
                    try (Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A")) {
                        result = s.hasNext() ? s.next() : "";
                    }
                    Log.d("AIResponseHelper", "Raw AI response: " + result);

                    JSONObject response = new JSONObject(result);
                    String reply = response
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    // Clean raw reply
                    String cleanedReply = reply
                        .trim()
                        .replace("<think>", "")
                        .replaceAll("\\s+", " ");

                    // Geniş kapsamlı cevap/gerekçe ayrıştırıcı
                    String[] answerJustification = parseAnswerAndJustification(cleanedReply.trim());
                    String answer = answerJustification[0];
                    String justification = answerJustification[1];

                    // Assemble with localized Answer/Justification markers
                    String answerLabel = "Answer:";
                    String justificationLabel = "Justification:";
                    if ("tr".equals(languageCode)) {
                        answerLabel = "Cevap:";
                        justificationLabel = "Gerekçe:";
                    }
                    StringBuilder finalBuilder = new StringBuilder();
                    finalBuilder.append(answerLabel).append(" ").append(answer.trim());
                    if (!justification.isEmpty()) {
                        finalBuilder.append("\n").append(justificationLabel).append(" ").append(justification.trim());
                    }
                    String finalReply = finalBuilder.toString();

                    aiCache.put(normalizedPrompt, finalReply);
                    callback.onResponse(finalReply);

                } else {
                    callback.onError("AI isteği başarısız oldu. Hata kodu: " + conn.getResponseCode());
                }

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }
    /**
     * Görsel analizi için AI'ya istek gönderen fonksiyon.
     * @param imageUrl Analiz edilecek görselin URL'si
     * @param prompt   Kullanıcı promptu veya sistem mesajı
     * @param callback Sonuç callback'i
     */
    public void sendImageForAnalysis(String imageUrl, String prompt, AICallback callback) {
        String fullPrompt = prompt + "\n" + "Aşağıdaki görseli analiz et: " + imageUrl;
        fetchAIResponse(fullPrompt, callback);
    }

}