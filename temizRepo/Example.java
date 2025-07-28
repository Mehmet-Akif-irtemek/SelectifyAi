package com.app.selectifyai;

import android.content.Context;
import android.util.Log; // For Logcat output
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
 * 🧠 SelectifyAI - AI Helper Class
 *
 * This class provides utility methods for interacting with AI APIs in the SelectifyAI app.
 * - Supports sending both text and image messages to the Groq API.
 * - Implements caching of responses for efficiency.
 * - Handles image resizing and compression to meet Groq Vision API limits.
 * - All API keys are securely managed and not hardcoded here.
 * - This file is a sample and does not represent the full source code.
 *
 * Main responsibilities:
 *   - Build and send requests to Groq's chat and vision endpoints.
 *   - Maintain a simple in-memory cache for AI responses.
 *   - Preprocess images (resize/compress) for vision API usage.
 *   - Provide callback interfaces for asynchronous AI responses.
 */
public class Example {
    private final Context context;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final Map<String, String> aiCache = new HashMap<>();

    /**
     * Callback interface for AI responses.
     */
    public interface AICallback {
        void onResponse(String response);
        void onError(String error);
    }

    /**
     * Constructor initializes context, Firebase, and cache.
     * @param context Application context
     */
    public Example(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Sends a contextual chat request to the AI, combining user info and chat history.
     * Caches the response based on a hash of the conversation and user info.
     *
     * @param chatHistory List of previous chat messages (user/assistant)
     * @param userInfo    Additional user context to provide to the AI
     * @param callback    Callback for success/error
     * @throws JSONException if JSON construction fails
     */
    public void getContextualResponse(List<Message> chatHistory, String userInfo, AICallback callback) throws JSONException {
        // Build the message array for the API
        JSONArray messages = new JSONArray();

        // Add system prompt to guide AI's interpretation of the conversation
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", context.getString(R.string.ai_system_prompt1)));

        // Add user info as a separate user message
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", userInfo));

        // Add chat history messages
        for (Message msg : chatHistory) {
            messages.put(new JSONObject()
                    .put("role", msg.isUser ? "user" : "assistant")
                    .put("content", msg.text));
        }

        // Create a cache key by hashing the message content and user info
        String cacheKey = generateCacheKey(messages.toString() + userInfo);

        // Check cache for existing response
        if (aiCache.containsKey(cacheKey)) {
            Log.d("AI_CACHE", "Response retrieved from cache.");
            callback.onResponse(aiCache.get(cacheKey));
            return;
        }

        // If not cached, send request to AI API
        fetchAIResponseWithMessages(messages, cacheKey, callback);
    }

    /**
     * Sends a chat completion request with a message array to the Groq API.
     * Stores the result in cache using the provided cacheKey.
     *
     * @param messages JSONArray of chat messages for the API
     * @param cacheKey Cache key derived from message/user info
     * @param callback Callback for response
     */
    private void fetchAIResponseWithMessages(JSONArray messages, String cacheKey, AICallback callback) {
        new Thread(() -> {
            try {
                // Prepare Groq chat completion API endpoint
                URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);

                // Set authentication and content headers
                String apiKey = KeyDecryptor.getGroqKey(); // Retrieve API key securely
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Build request payload
                JSONObject data = new JSONObject();
                data.put("model", "moonshotai/kimi-k2-instruct");
                data.put("messages", messages); // Attach messages as JSON array

                // Send request payload
                OutputStream os = conn.getOutputStream();
                os.write(data.toString().getBytes());
                os.flush();

                // Handle response
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    java.util.Scanner s = new java.util.Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String result = s.hasNext() ? s.next() : "";

                    // Parse AI reply from JSON
                    JSONObject response = new JSONObject(result);
                    String reply = response.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    String finalReply = reply.trim().replace("<think>", "");

                    // Store response in cache
                    aiCache.put(cacheKey, finalReply);
                    Log.d("AI_CACHE", "Response cached.");

                    callback.onResponse(finalReply);
                } else {
                    Log.e("AI_API", "Groq API response failed: " + conn.getResponseCode() + " " + conn.getResponseMessage());
                    callback.onError("AI response not received: " + conn.getResponseCode());
                }

            } catch (Exception e) {
                Log.e("AI_API", "Exception during API request: " + e.getMessage(), e);
                callback.onError(e.getMessage());
            } finally {
                // Connection will be closed automatically; explicit disconnect if needed
            }
        }).start();
    }

    /**
     * Sends a single-prompt chat request to the AI (used for simple Q&A).
     * Utilizes caching based on the prompt string.
     *
     * @param prompt   The user prompt/question
     * @param callback Callback for response
     */
    private void fetchAIResponse(String prompt, AICallback callback) {
        // Check cache first
        if (aiCache.containsKey(prompt)) {
            Log.d("AI_CACHE", "Response retrieved from cache (single prompt).");
            callback.onResponse(aiCache.get(prompt));
            return;
        }

        new Thread(() -> {
            try {
                // Prepare Groq chat completion API endpoint
                URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);

                // Set headers for authentication and content type
                String apiKey = KeyDecryptor.getGroqKey();
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Build request payload
                JSONObject data = new JSONObject();
                data.put("model", "meta-llama/llama-4-scout-17b-16e-instruct");

                JSONArray messages = new JSONArray();
                // Add system prompt (can be different for single prompt)
                messages.put(new JSONObject()
                        .put("role", "system")
                        .put("content", context.getString(R.string.ai_system_prompt)));
                messages.put(new JSONObject()
                        .put("role", "user")
                        .put("content", prompt));
                data.put("messages", messages);

                // Send request payload
                OutputStream os = conn.getOutputStream();
                os.write(data.toString().getBytes());
                os.flush();

                // Handle response
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    java.util.Scanner s = new java.util.Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String result = s.hasNext() ? s.next() : "";

                    JSONObject response = new JSONObject(result);
                    String reply = response.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    String finalReply = reply.trim().replace("<think>", "");

                    // Cache the response
                    aiCache.put(prompt, finalReply);
                    Log.d("AI_CACHE", "Response cached (single prompt).");

                    callback.onResponse(finalReply);
                } else {
                    Log.e("AI_API", "Groq API response failed (single): " + conn.getResponseCode() + " " + conn.getResponseMessage());
                    callback.onError("AI response not received: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                Log.e("AI_API", "Exception during API request (single): " + e.getMessage(), e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    /**
     * Generates a cache key by computing the MD5 hash of the input string.
     * Used to uniquely identify AI requests for caching.
     * Falls back to hashCode if MD5 is not available.
     *
     * @param input String to hash (e.g., concatenation of messages/user info)
     * @return MD5 hash as hex string, or hashCode as fallback
     */
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
            Log.e("AI_CACHE", "MD5 algorithm not found, using hashCode fallback.", e);
            return String.valueOf(input.hashCode()); // Use simple hashCode if MD5 fails
        }
    }

    /**
     * Sends an image and prompt to the AI as a base64-encoded JPEG, after resizing and compressing.
     * Handles image size limits of Groq Vision API and builds the correct message format.
     * Caches the response using a key derived from prompt and image content.
     *
     * @param bitmap   Input image (Bitmap)
     * @param prompt   User's question or comment about the image
     * @param callback Callback for response
     */
    public void sendImageAsBase64ToAI(Bitmap bitmap, String prompt, AICallback callback) {
        try {
            // Resize bitmap if it exceeds Groq's 33 megapixel limit
            Bitmap resizedBitmap = resizeBitmapIfNeeded(bitmap);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Start compressing at 60% quality, decrease if needed
            int quality = 60;
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            byte[] imageBytes = baos.toByteArray();

            // Compress further if image is still too large (Groq Vision base64 limit: 4MB)
            while (imageBytes.length > 3 * 1024 * 1024 && quality > 20) { // Safe limit: 3MB
                baos.reset();
                quality -= 10;
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                imageBytes = baos.toByteArray();
                Log.d("AI_IMAGE_VISION", "Optimizing image size, quality: " + quality + ", size: " + imageBytes.length);
            }

            // If still too large, abort with error
            if (imageBytes.length > 4 * 1024 * 1024) {
                callback.onError("Image is too large. Please select a smaller image.");
                return;
            }

            // Encode image to base64 string (no line breaks)
            String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            Log.d("AI_IMAGE_VISION", "Base64 image size: " + base64Image.length() + " characters");

            // Build message array for Groq Vision API
            JSONArray messages = new JSONArray();

            // Add system message to instruct AI on image analysis
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "Sen yardımçı bir AI asistanısın. Kullanıcının gönderdiği görselleri analiz edebilir ve kullanıcının mesajına göre görseli yorumlayabilirsin. Kullanıcının sorusuna veya isteğine göre görseli detaylı şekilde açıkla. Türkçe cevap ver ve samimi bir dil kullan. Görselde gördüklerini açık ve anlaşılır şekilde anlat."));

            // Build user message with both text and image (Groq Vision format)
            JSONArray contentArray = new JSONArray();
            // Add prompt as text part
            contentArray.put(new JSONObject()
                    .put("type", "text")
                    .put("text", prompt));
            // Add image part as base64 data URL
            contentArray.put(new JSONObject()
                    .put("type", "image_url")
                    .put("image_url", new JSONObject()
                            .put("url", "data:image/jpeg;base64," + base64Image)));

            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", contentArray));

            // Use only a short portion of the base64 string for cache key (to avoid huge keys)
            String cacheKey = generateCacheKey(prompt + base64Image.substring(0, Math.min(100, base64Image.length())));
            fetchAIResponseWithVision(messages, cacheKey, callback);

        } catch (Exception e) {
            Log.e("AI_IMAGE_VISION", "Image analysis error: " + e.getMessage());
            callback.onError("Image could not be processed: " + e.getMessage());
        }
    }

    /**
     * Resizes the bitmap if it exceeds Groq Vision's 33 megapixel input limit.
     * Preserves aspect ratio.
     *
     * @param bitmap Input bitmap
     * @return Resized bitmap if needed, otherwise original
     */
    private Bitmap resizeBitmapIfNeeded(Bitmap bitmap) {
        // Groq Vision API limit: 33 megapixels (33177600 pixels)
        int maxPixels = 33177600;
        int currentPixels = bitmap.getWidth() * bitmap.getHeight();

        if (currentPixels <= maxPixels) {
            // No resizing needed
            return bitmap;
        }

        // Compute resize ratio to fit under maxPixels while preserving aspect ratio
        double ratio = Math.sqrt((double) maxPixels / currentPixels);
        int newWidth = (int) (bitmap.getWidth() * ratio);
        int newHeight = (int) (bitmap.getHeight() * ratio);

        Log.d("AI_IMAGE_VISION", "Resizing image: " + bitmap.getWidth() + "x" + bitmap.getHeight() + " -> " + newWidth + "x" + newHeight);
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /**
     * Sends a vision request (image + prompt) to the Groq API and handles the response.
     * Uses cache to avoid redundant processing.
     *
     * @param messages JSONArray containing system and user (text+image) messages
     * @param cacheKey Cache key based on prompt and image
     * @param callback Callback for response
     */
    private void fetchAIResponseWithVision(JSONArray messages, String cacheKey, AICallback callback) {
        // Check cache for previous response
        if (aiCache.containsKey(cacheKey)) {
            Log.d("AI_CACHE", "Vision response retrieved from cache.");
            callback.onResponse(aiCache.get(cacheKey));
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                // Prepare Groq Vision API endpoint
                URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                // Use longer timeouts for image analysis
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(40000);

                // Set authentication and content headers
                String apiKey = KeyDecryptor.getGroqKey();
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Build vision request payload
                JSONObject data = new JSONObject();
                data.put("model", "meta-llama/llama-4-scout-17b-16e-instruct"); // Groq vision model
                data.put("messages", messages);
                data.put("max_tokens", 1024);
                data.put("temperature", 0.7);
                data.put("top_p", 1);
                data.put("stream", false);

                Log.d("AI_VISION", "Sending request to Groq Vision API...");

                // Send request
                OutputStream os = conn.getOutputStream();
                os.write(data.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("AI_VISION", "API response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read the API response
                    java.util.Scanner s = new java.util.Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                    String result = s.hasNext() ? s.next() : "";
                    s.close();

                    Log.d("AI_VISION", "API response received, size: " + result.length());

                    // Parse the AI's reply
                    JSONObject response = new JSONObject(result);
                    if (response.has("choices") && response.getJSONArray("choices").length() > 0) {
                        String reply = response.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        String finalReply = reply.trim();

                        // Cache the vision response
                        aiCache.put(cacheKey, finalReply);
                        Log.d("AI_CACHE", "Vision response cached.");

                        callback.onResponse(finalReply);
                    } else {
                        Log.e("AI_VISION", "No choices found in API response: " + result);
                        callback.onError("Invalid API response");
                    }
                } else {
                    // Read error message from error stream
                    String errorMsg = "";
                    try {
                        java.util.Scanner s = new java.util.Scanner(conn.getErrorStream(), "UTF-8").useDelimiter("\\A");
                        errorMsg = s.hasNext() ? s.next() : "";
                        s.close();
                        Log.e("AI_VISION", "API error response: " + errorMsg);
                    } catch (Exception e2) {
                        Log.e("AI_VISION", "Could not read error message: " + e2.getMessage());
                    }

                    Log.e("AI_VISION", "Vision API response failed: " + responseCode);
                    callback.onError("Vision analysis failed (" + responseCode + "): " + errorMsg);
                }
            } catch (Exception e) {
                Log.e("AI_VISION", "Vision API exception: " + e.getMessage(), e);
                callback.onError("Vision analysis error: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    /**
     * Simple message class for chat history.
     * Represents either a user or assistant message.
     */
    public static class Message {
        public boolean isUser;
        public String text;

        public Message(boolean isUser, String text) {
            this.isUser = isUser;
            this.text = text;
        }
    }
}