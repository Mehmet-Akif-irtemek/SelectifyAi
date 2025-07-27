package com.app.selectifyai;

import java.util.HashMap;
import java.util.Map;

public class ChatMessage {
    private String message;
    private boolean isUser;
    private long timestamp;
    private String imageBase64; // Görsel için base64 string
    private boolean hasImage;   // Görselin olup olmadığını kontrol etmek için
    
    public ChatMessage() {
        // Firebase için boş constructor gerekli
    }
    
    public ChatMessage(String message, boolean isUser, long timestamp) {
        this.message = message;
        this.isUser = isUser;
        this.timestamp = timestamp;
        this.hasImage = false;
        this.imageBase64 = null;
    }
    
    public ChatMessage(String message, boolean isUser, long timestamp, String imageBase64) {
        this.message = message;
        this.isUser = isUser;
        this.timestamp = timestamp;
        this.imageBase64 = imageBase64;
        this.hasImage = imageBase64 != null && !imageBase64.isEmpty();
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean isUser() {
        return isUser;
    }
    
    public void setUser(boolean user) {
        isUser = user;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getImageBase64() {
        return imageBase64;
    }
    
    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
        this.hasImage = imageBase64 != null && !imageBase64.isEmpty();
    }
    
    public boolean hasImage() {
        return hasImage;
    }
    
    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
    }
    
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("message", message);
        map.put("isUser", isUser);
        map.put("timestamp", timestamp);
        map.put("imageBase64", imageBase64);
        map.put("hasImage", hasImage);
        return map;
    }
}