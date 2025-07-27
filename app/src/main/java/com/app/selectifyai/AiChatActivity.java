package com.app.selectifyai;

import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AiChatActivity extends AnaAktivite {

    private TextView textViewWelcome;
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageView btnSend;
    private ImageButton btnBack, btnMenu;
    private ImageView btnUploadImage;
    private LinearLayout typingIndicator;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    
    // Image preview components
    private LinearLayout imagePreviewContainer;
    private ImageView imagePreview;
    private ImageButton btnRemoveImage;
    private Bitmap selectedImageBitmap;

    private double guncelSicaklik = 0;
    private String guncelHavaDurumu = "";
    private String il = "",
        ilce = "";

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private HELPER aiHelper;
    private ZarAiManager zarAiManager;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private String currentChatId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);
        hideSystemUI();

        currentChatId = getIntent().getStringExtra("chatId");

        initializeComponents();
        setupRecyclerView();
        setupListeners();
        setupMessageLongClick();
        loadChatHistory();
        konumuAl();

        // Kullanıcı adıyla hoş geldin mesajı
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            firestore
                .collection("kullanicilar")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String ad = doc.getString("kullaniciAdi");
                    if (ad != null && !ad.trim().isEmpty()) {
                        String fullText = getSelamliHosgeldinMesaji(ad.trim());
                        SpannableString spannable = new SpannableString(
                            fullText
                        );
                        int start = fullText.indexOf(ad.trim());
                        int end = start + ad.trim().length();
                        int[] gradientColors = {
                            Color.parseColor("#FF0000"),
                            Color.parseColor("#FF00FF"),
                            Color.parseColor("#8D00F1"),
                        };
                        spannable.setSpan(
                            new GradientSpan(gradientColors, ad.trim()),
                            start,
                            end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                        textViewWelcome.setText(spannable);
                        textViewWelcome.setTranslationY(-100f);
                        textViewWelcome.setAlpha(0f);
                        textViewWelcome.setVisibility(View.VISIBLE);
                        textViewWelcome
                            .animate()
                            .translationY(0f)
                            .alpha(1f)
                            .setDuration(600)
                            .start();
                    }
                });
        }

        zarAiManager = new ZarAiManager(this, false);
        
        // Hak sayısını göster
        updateRemainingRights();
    }

    private String getSelamliHosgeldinMesaji(String ad) {
        if (ad == null || ad.trim().isEmpty()) return "";

        ad = ad.trim();
        Calendar calendar = Calendar.getInstance();
        int saat = calendar.get(Calendar.HOUR_OF_DAY);

        String mesaj;

        if (saat >= 5 && saat < 12) {
            // Sabah
            mesaj = getString(R.string.gunaydin, ad);
        } else if (saat >= 12 && saat < 17) {
            // Öğle
            mesaj = getString(R.string.tunaydin, ad);
        } else if (saat >= 17 && saat < 22) {
            // Akşam
            mesaj = getString(R.string.iyi_aksamlar, ad);
        } else {
            // Gece
            mesaj = getString(R.string.iyi_geceler, ad);
        }

        return mesaj;
    }

    private void konumuAl() {
        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                100
            );
            return;
        }

        FusedLocationProviderClient fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient
            .getLastLocation()
            .addOnSuccessListener(location -> {
                if (location != null) {
                    updateLocationUI(location);
                }
            });
    }

    private void updateLocationUI(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> adresler = geocoder.getFromLocation(
                location.getLatitude(),
                location.getLongitude(),
                1
            );
            if (!adresler.isEmpty()) {
                Address adres = adresler.get(0);
                ilce = adres.getSubAdminArea();
                il = adres.getAdminArea();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        havaDurumunuGetir(location.getLatitude(), location.getLongitude());
    }

    private void havaDurumunuGetir(double lat, double lon) {
        String apiKey = KeyDecryptor.getWeatherKey();
        String url = String.format(
            getString(R.string.weather_api_url_template),
            lat,
            lon,
            apiKey
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
                try {
                    JSONObject main = response.getJSONObject(
                        getString(R.string.main_field)
                    );
                    guncelSicaklik = main.getDouble(
                        getString(R.string.temp_field)
                    );

                    JSONArray weatherArray = response.getJSONArray(
                        getString(R.string.weather_field)
                    );
                    JSONObject weather = weatherArray.getJSONObject(0);
                    guncelHavaDurumu = weather
                        .getString(getString(R.string.description_field))
                        .toLowerCase(Locale.ROOT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            },
            error -> {
                Toast.makeText(
                    this,
                    getString(R.string.hava_durumu_alinamadi_toast),
                    Toast.LENGTH_SHORT
                ).show();
            }
        );
        queue.add(request);
    }

    private void initializeComponents() {
        textViewWelcome = findViewById(R.id.textViewWelcome);
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        btnSend = findViewById(R.id.T);
        btnBack = findViewById(R.id.btnBack);
        btnMenu = findViewById(R.id.btnMenu);
        typingIndicator = findViewById(R.id.typingIndicator);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        btnUploadImage = findViewById(R.id.up);
        
        // Image preview components
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);
        imagePreview = findViewById(R.id.imagePreview);
        btnRemoveImage = findViewById(R.id.btnRemoveImage);

        messageList = new ArrayList<>();
        aiHelper = new HELPER(this);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Başta mesaj listesini gizle
        recyclerViewMessages.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recyclerViewMessages.setLayoutManager(lm);
        recyclerViewMessages.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnMenu.setOnClickListener(v -> {
            updateChatMenu();
            drawerLayout.openDrawer(navigationView);
        });
        btnSend.setOnClickListener(v -> sendMessage());
        if (btnUploadImage != null) {
            btnUploadImage.setOnClickListener(v -> openImagePicker());
        }
        
        // Image preview remove button
        btnRemoveImage.setOnClickListener(v -> removeSelectedImage());

        editTextMessage.addTextChangedListener(
            new TextWatcher() {
                @Override
                public void beforeTextChanged(
                    CharSequence s,
                    int st,
                    int c,
                    int a
                ) {}

                @Override
                public void onTextChanged(
                    CharSequence s,
                    int st,
                    int b,
                    int c
                ) {
                    // Hoşgeldin yazısını gizle, mesaj listesini göster
                    if (
                        s.length() > 0 &&
                        textViewWelcome.getVisibility() == View.VISIBLE
                    ) {
                        textViewWelcome
                            .animate()
                            .alpha(0f)
                            .translationY(-50f)
                            .setDuration(400)
                            .withEndAction(() ->
                                textViewWelcome.setVisibility(View.GONE)
                            )
                            .start();
                        recyclerViewMessages.setVisibility(View.VISIBLE);
                        recyclerViewMessages.setAlpha(0f);
                        recyclerViewMessages.setTranslationY(50f);
                        recyclerViewMessages
                            .animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(500)
                            .start();
                    }
                    btnSend.setEnabled(s.toString().trim().length() > 0);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            }
        );

        editTextMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (btnSend.isEnabled()) {
                sendMessage();
                return true;
            }
            return false;
        });

        setupNavigationListener();
    }

    private void setupNavigationListener() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId(),
                group = item.getGroupId();
            if (id == R.id.menu_new_message) {
                startNewChat();
            } else if (id == R.id.menu_settings) {
                openSettings();
            } else if (id == R.id.menu_delete_all_messages) {
                confirmAndDeleteAllMessages();
            } else if (group == 1001) {
                String chatId = item.getTitleCondensed().toString();
                if (!chatId.equals(currentChatId)) openChat(chatId);
            }
            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void sendMessage() {
        String text = editTextMessage.getText().toString().trim();
        
        // Check if there's an image or text
        if (text.isEmpty() && selectedImageBitmap == null) {
            return;
        }
        
        // HAK KONTROLÜ EKLE
        String uid = auth.getUid();
        if (uid == null) {
            Toast.makeText(this, "Kullanıcı girişi gerekli", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String tarih = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(new java.util.Date());
        com.google.firebase.firestore.DocumentReference limitRef = firestore.collection("kullanicilar").document(uid)
                .collection("groq_limit").document(tarih);
                
        limitRef.get().addOnSuccessListener(doc -> {
            // Maksimum günlük hakkı al (davet ile artabilir)
            Long maxDaily = doc.getLong("gunluk_max_hak");
            long gunlukMax = maxDaily != null ? maxDaily : 50; // Varsayılan 50
            
            long kullanilan = doc.exists() && doc.contains("count") ? doc.getLong("count") : 0;
            
            if (kullanilan >= gunlukMax) {
                // Günlük limit dolu
                Toast.makeText(this, getString(R.string.gunluk_groq_limit_doldu), Toast.LENGTH_LONG).show();
                return;
            }
            
            // Limit uygunsa mesaj gönderebilir
            proceedWithSendMessage(text, limitRef, kullanilan);
            
        }).addOnFailureListener(e -> {
            Log.e("AiChat", "Hak kontrolü hatası: " + e.getMessage());
            Toast.makeText(this, "Hak kontrolü başarısız", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void proceedWithSendMessage(String text, com.google.firebase.firestore.DocumentReference limitRef, long currentUsage) {
        // Check if only image without text
        if (selectedImageBitmap != null && text.isEmpty()) {
            text = "Bu fotoğrafı açıkla ve ne gördüğünü detaylı anlat.";
        }
        
        final String finalText = text; // Final yapıyoruz
        
        // Create user message with image
        String displayText = selectedImageBitmap != null ? "📷 " + finalText : finalText;
        ChatMessage userMsg;
        
        if (selectedImageBitmap != null) {
            // Görseli base64'e çevir ve mesajla birlikte kaydet
            String imageBase64 = bitmapToBase64(selectedImageBitmap);
            userMsg = new ChatMessage(displayText, true, System.currentTimeMillis(), imageBase64);
        } else {
            userMsg = new ChatMessage(displayText, true, System.currentTimeMillis());
        }
        
        messageList.add(userMsg);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
        saveChatMessage(userMsg);
        
        // Clear input and hide image preview
        editTextMessage.setText("");
        
        // Show typing indicator
        showTypingIndicator(true);
        
        // Hakkı kullan (count'u artır)
        limitRef.set(java.util.Collections.singletonMap("count", currentUsage + 1), com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                // Hak sayısını güncelle
                updateRemainingRights();
                
                // Send to AI
                if (selectedImageBitmap != null) {
                    // Send image with text
                    Bitmap imageToSend = selectedImageBitmap;
                    removeSelectedImage(); // Clear preview
                    sendImageToAI(imageToSend, finalText);
                } else {
                    // Send text only
                    getAIResponse(finalText);
                }
            })
            .addOnFailureListener(e -> {
                Log.e("AiChat", "Hak kullanımı kaydedilemedi: " + e.getMessage());
                showTypingIndicator(false);
                Toast.makeText(this, "Mesaj gönderilemedi", Toast.LENGTH_SHORT).show();
            });
    }
    
    private String bitmapToBase64(Bitmap bitmap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Mesajlarda göstermek için daha düşük kalite kullan
            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e("ImageProcessing", "Bitmap base64'e çevrilemedi: " + e.getMessage());
            return null;
        }
    }
    
    private void sendImageToAI(Bitmap bitmap, String text) {
        try {
            getUserInfo(userInfo -> {
                aiHelper.sendImageAsBase64ToAI(bitmap, text, new HELPER.AICallback() {
                    @Override
                    public void onResponse(String response) {
                        runOnUiThread(() -> {
                            showTypingIndicator(false);
                            ChatMessage aiMessage = new ChatMessage(response, false, System.currentTimeMillis());
                            messageList.add(aiMessage);
                            chatAdapter.notifyItemInserted(messageList.size() - 1);
                            scrollToBottom();
                            saveChatMessage(aiMessage);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            showTypingIndicator(false);
                            String errorMessage;
                            if (error.contains("400")) {
                                errorMessage = "Görsel formatı desteklenmiyor veya çok büyük. Lütfen farklı bir görsel deneyin.";
                            } else if (error.contains("413")) {
                                errorMessage = "Görsel çok büyük. Lütfen daha küçük bir görsel seçin.";
                            } else if (error.contains("rate")) {
                                errorMessage = "API limiti aşıldı. Lütfen birkaç dakika bekleyin.";
                            } else {
                                errorMessage = "Üzgünüm, görseli analiz edemedim. Lütfen tekrar deneyin.";
                            }
                            ChatMessage errorMessage_obj = new ChatMessage(errorMessage, false, System.currentTimeMillis());
                            messageList.add(errorMessage_obj);
                            chatAdapter.notifyItemInserted(messageList.size() - 1);
                            scrollToBottom();
                            saveChatMessage(errorMessage_obj);
                        });
                    }
                });
            });
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void getAIResponse(String userMessage) {
        try {
            getUserInfo(userInfo -> {
                // Chat geçmişini HELPER.Message listesine çevir
                List<HELPER.Message> history = new ArrayList<>();
                for (ChatMessage msg : messageList) {
                    history.add(
                        new HELPER.Message(msg.isUser(), msg.getMessage())
                    );
                }
                try {
                    aiHelper.getContextualResponse(
                        history,
                        userInfo,
                        new HELPER.AICallback() {
                            @Override
                            public void onResponse(String response) {
                                runOnUiThread(() -> {
                                    showTypingIndicator(false);
                                    // Add AI response
                                    ChatMessage aiMessage = new ChatMessage(
                                        response,
                                        false,
                                        System.currentTimeMillis()
                                    );
                                    messageList.add(aiMessage);
                                    chatAdapter.notifyItemInserted(
                                        messageList.size() - 1
                                    );
                                    scrollToBottom();
                                    // Save AI message to Firebase
                                    saveChatMessage(aiMessage);
                                });
                            }

                            @Override
                            public void onError(String error) {
                                runOnUiThread(() -> {
                                    showTypingIndicator(false);
                                    Toast.makeText(
                                        AiChatActivity.this,
                                        getString(
                                            R.string.ai_cevabi_alinamadi
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show();
                                });
                            }
                        }
                    );
                } catch (JSONException e) {
                    showTypingIndicator(false);
                    Toast.makeText(
                        AiChatActivity.this,
                        getString(R.string.ai_format_hatasi),
                        Toast.LENGTH_SHORT
                    ).show();
                }
            });
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void getUserInfo(UserInfoCallback callback) throws JSONException {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onUserInfo(getString(R.string.ai_system_prompt1));
            return;
        }

        firestore
            .collection("kullanicilar")
            .document(uid)
            .get()
            .addOnSuccessListener(userDoc -> {
                String name = userDoc.getString("kullaniciAdi");
                Long age = userDoc.getLong("yas");
                String responsibility = userDoc.getString("sorumluluk");

                @SuppressLint("DefaultLocale")
                String userInfo =
                    getString(R.string.ai_system_prompt1) +
                    String.format(
                        "\nKullanıcı: %s\nYaş: %d\nSorumluluk: %s\nKonum: %s/%s\nHava Durumu: %s, %.1f°C",
                        name != null ? name : getString(R.string.bilinmiyor),
                        age != null ? age : 0,
                        responsibility != null
                            ? responsibility
                            : getString(R.string.belirtilmemis),
                        il.isEmpty() ? "?" : il,
                        ilce.isEmpty() ? "?" : ilce,
                        guncelHavaDurumu.isEmpty()
                            ? "bilinmiyor"
                            : guncelHavaDurumu,
                        guncelSicaklik
                    );

                try {
                    callback.onUserInfo(userInfo);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            })
            .addOnFailureListener(e -> {
                @SuppressLint("DefaultLocale")
                String userInfo =
                    getString(R.string.ai_system_prompt1) +
                    String.format(
                        "\n" +
                        getString(R.string.kullanici_bilgisi_alinamadi) +
                        "\nKonum: %s/%s\nHava Durumu: %s, %.1f°C",
                        il.isEmpty() ? "?" : il,
                        ilce.isEmpty() ? "?" : ilce,
                        guncelHavaDurumu.isEmpty()
                            ? "bilinmiyor"
                            : guncelHavaDurumu,
                        guncelSicaklik
                    );
                try {
                    callback.onUserInfo(userInfo);
                } catch (JSONException ex) {
                    throw new RuntimeException(ex);
                }
            });
    }

    private interface UserInfoCallback {
        void onUserInfo(String userInfo) throws JSONException;
    }

    private void showTypingIndicator(boolean show) {
        typingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            scrollToBottom();
        }
    }

    private void scrollToBottom() {
        if (messageList.size() > 0) {
            recyclerViewMessages.smoothScrollToPosition(messageList.size() - 1);
        }
    }

    private void loadChatHistory() {
        String uid = auth.getUid();
        if (uid == null || currentChatId == null) return;
        firestore
            .collection("kullanicilar")
            .document(uid)
            .collection("chats")
            .document(currentChatId)
            .collection("messages")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener(qs -> {
                messageList.clear();
                for (DocumentSnapshot doc : qs) {
                    String msg = doc.getString("message");
                    Boolean isUser = doc.getBoolean("isUser");
                    Long ts = doc.getLong("timestamp");
                    String imageBase64 = doc.getString("imageBase64");
                    Boolean hasImage = doc.getBoolean("hasImage");
                    
                    if (msg != null && isUser != null && ts != null) {
                        ChatMessage chatMessage = new ChatMessage(msg, isUser, ts);
                        if (hasImage != null && hasImage && imageBase64 != null) {
                            chatMessage.setImageBase64(imageBase64);
                            chatMessage.setHasImage(true);
                        }
                        messageList.add(chatMessage);
                    }
                }
                chatAdapter.notifyDataSetChanged();
                scrollToBottom();
                recyclerViewMessages.postDelayed(
                    () -> {
                        if (chatAdapter.getItemCount() > 0) {
                            textViewWelcome.setVisibility(View.GONE);
                            recyclerViewMessages.setVisibility(View.VISIBLE);
                        }
                    },
                    300
                );
            });
    }

    private void saveChatMessage(ChatMessage message) {
        String uid = auth.getUid();
        if (uid == null) return;
        String chatId = currentChatId;
        if (chatId == null) return;

        firestore
            .collection("kullanicilar")
            .document(uid)
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .add(message.toMap())
            .addOnSuccessListener(ref -> {
                // Eğer ilk mesajsa, başlık olarak kaydet ve timestamp ekle
                if (messageList.size() == 1 && message.isUser()) {
                    Map<String, Object> chatData = new HashMap<>();
                    chatData.put("firstMessage", message.getMessage());
                    chatData.put("timestamp", System.currentTimeMillis());
                    chatData.put("lastUpdated", System.currentTimeMillis());

                    firestore
                        .collection("kullanicilar")
                        .document(uid)
                        .collection("chats")
                        .document(chatId)
                        .update(chatData);
                } else {
                    // Son güncelleme zamanını güncelle
                    firestore
                        .collection("kullanicilar")
                        .document(uid)
                        .collection("chats")
                        .document(chatId)
                        .update("lastUpdated", System.currentTimeMillis());
                }
            })
            .addOnFailureListener(e -> {
                // Sessizce başarısız ol, kullanıcıyı rahatsız etme
            });
    }

    private void openSettings() {
        // Ayarlar sayfasını aç
        Intent intent = new Intent(this, Ayarlar.class);
        startActivity(intent);
    }

    // Düzeltilmiş updateChatMenu metodu
    private void updateChatMenu() {
        String uid = auth.getUid();
        if (uid == null) return;

        firestore
            .collection("kullanicilar")
            .document(uid)
            .collection("chats")
            .orderBy(
                "lastUpdated",
                com.google.firebase.firestore.Query.Direction.DESCENDING
            )
            .limit(20) // Son 20 chat
            .get()
            .addOnSuccessListener(snapshots -> {
                Menu menu = navigationView.getMenu();
                menu.clear();

                // Sabit menüler üst grup
                int staticGroup = 1000;
                menu
                    .add(
                        staticGroup,
                        R.id.menu_new_message,
                        Menu.NONE,
                        getString(R.string.menu_new_message)
                    )
                    .setIcon(R.drawable.ic_new_message);
                menu
                    .add(
                        staticGroup,
                        R.id.menu_delete_all_messages,
                        Menu.NONE,
                        getString(R.string.tum_mesajlari_sil)
                    )
                    .setIcon(R.drawable.icon_01d);

                // Chatler
                int chatGroup = 1001;
                for (DocumentSnapshot doc : snapshots) {
                    String chatId = doc.getId();
                    String firstMessage = doc.getString("firstMessage");
                    if (firstMessage == null || firstMessage.trim().isEmpty()) {
                        firstMessage = getString(R.string.bos_baslik);
                    }
                    if (firstMessage.length() > 35) {
                        firstMessage = firstMessage.substring(0, 32) + "...";
                    }
                    MenuItem item = menu.add(
                        chatGroup,
                        View.generateViewId(),
                        Menu.NONE,
                        firstMessage
                    );
                    item.setIcon(R.drawable.ic_history);
                    item.setTitleCondensed(chatId);
                    if (chatId.equals(currentChatId)) {
                        item.setChecked(true);
                        item.setCheckable(true);
                    }
                }

                // Sabit menüler alt grup
                int settingsGroup = 1002;
                menu
                    .add(
                        settingsGroup,
                        R.id.menu_settings,
                        Menu.NONE,
                        getString(R.string.menu_settings)
                    )
                    .setIcon(R.drawable.ic_settings);

                // Chat item'larına tıklayınca popup menü aç
                setupChatPopupMenuForChats();
            });
    }

    // Chat item'ına tıklayınca popup menü aç
    private void setupChatPopupMenuForChats() {
        Menu menu = navigationView.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getGroupId() == 1001) {
                // Chatler grubu
                View itemView = navigationView.findViewById(item.getItemId());
                if (itemView != null) {
                    itemView.setOnClickListener(v -> showChatPopupMenu(item));
                }
            }
        }
    }

    private void setupChatLongClickListeners() {
        // NavigationView'daki chat öğelerine long click listener ekle
        if (navigationView.getMenu() != null) {
            Menu menu = navigationView.getMenu();
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (item.getGroupId() == 1001) {
                    // Chat öğeleri
                    // Long click için custom view gerekebilir, şimdilik context menu kullan
                }
            }
        }
    }

    private void startNewChat() {
        String uid = auth.getUid();
        if (uid == null) return;

        String chatId = firestore
            .collection("kullanicilar")
            .document(uid)
            .collection("chats")
            .document()
            .getId();

        // Chat'i timestamp ile oluştur
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("firstMessage", getString(R.string.hint_type_message1));
        chatData.put("timestamp", System.currentTimeMillis());
        chatData.put("lastUpdated", System.currentTimeMillis());

        firestore
            .collection("kullanicilar")
            .document(uid)
            .collection("chats")
            .document(chatId)
            .set(chatData)
            .addOnSuccessListener(aVoid -> {
                openChat(chatId);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(
                    this,
                    "Yeni sohbet oluşturulamadı",
                    Toast.LENGTH_SHORT
                ).show();
            });
    }

    private void openChat(String chatId) {
        Intent intent = new Intent(this, AiChatActivity.class);
        intent.putExtra("chatId", chatId);
        startActivity(intent);
        finish();
    }

    private void confirmAndDeleteAllMessages() {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.tum_mesajlari_sil_baslik))
            .setMessage(getString(R.string.tum_mesajlari_sil_mesaj))
            .setPositiveButton(
                getString(R.string.evet_button),
                (dialog, which) -> deleteAllMessages()
            )
            .setNegativeButton(getString(R.string.hayir_button), null)
            .show();
    }

    private void deleteAllMessages() {
        String uid = auth.getUid();
        if (uid == null || currentChatId == null) return;

        firestore
            .collection("kullanicilar")
            .document(uid)
            .collection("chats")
            .document(currentChatId)
            .collection("messages")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                WriteBatch batch = firestore.batch();

                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    batch.delete(doc.getReference());
                }

                batch
                    .commit()
                    .addOnSuccessListener(aVoid -> {
                        messageList.clear();
                        chatAdapter.notifyDataSetChanged();

                        // Hoşgeldin mesajını göster
                        textViewWelcome.setVisibility(View.VISIBLE);
                        recyclerViewMessages.setVisibility(View.GONE);

                        Toast.makeText(
                            this,
                            getString(R.string.tum_mesajlar_silindi),
                            Toast.LENGTH_SHORT
                        ).show();
                    });
            });
    }

    private void setupMessageLongClick() {
        chatAdapter.setOnMessageLongClickListener((position, message) -> {
            new AlertDialog.Builder(this)
                .setTitle(getString(R.string.mesaji_sil))
                .setMessage(getString(R.string.mesaji_sil_onay))
                .setPositiveButton(
                    getString(R.string.evet_button),
                    (dialog, which) -> deleteSingleMessage(position, message)
                )
                .setNegativeButton(getString(R.string.hayir_button), null)
                .show();
        });
    }

    private void deleteSingleMessage(int position, ChatMessage message) {
        String uid = auth.getUid();
        if (uid == null || currentChatId == null) {
            Log.e(TAG, "Kullanıcı ID veya Chat ID bulunamadı.");
            Toast.makeText(
                this,
                "Mesaj silinemedi: Kullanıcı bilgisi eksik",
                Toast.LENGTH_SHORT
            ).show();
            return;
        }

        DocumentReference chatRef = firestore
            .collection("kullanicilar")
            .document(uid)
            .collection("chats")
            .document(currentChatId);

        chatRef
            .collection("messages")
            .whereEqualTo("timestamp", message.getTimestamp())
            .whereEqualTo("isUser", message.isUser())
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (querySnapshot.isEmpty()) {
                    Log.w(TAG, "Silinecek mesaj bulunamadı.");
                    Toast.makeText(
                        this,
                        "Mesaj bulunamadı",
                        Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                WriteBatch batch = firestore.batch();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    batch.delete(doc.getReference());
                }

                batch
                    .commit()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Mesaj başarıyla silindi.");
                        if (position >= 0 && position < messageList.size()) {
                            messageList.remove(position);
                            chatAdapter.notifyItemRemoved(position);
                            Toast.makeText(
                                this,
                                "Mesaj silindi",
                                Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            Log.e(TAG, "Geçersiz pozisyon: " + position);
                            chatAdapter.notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Mesaj silinirken hata oluştu", e);
                        Toast.makeText(
                            this,
                            "Mesaj silinemedi: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                        ).show();
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Mesaj sorgulanırken hata oluştu", e);
                Toast.makeText(
                    this,
                    "Mesaj sorgulanamadı: " + e.getMessage(),
                    Toast.LENGTH_SHORT
                ).show();
            });
    }

    // Düzeltilmiş chat silme metodu
    private void deleteChat(String chatId) {
        String uid = auth.getUid();
        if (uid == null) return;

        // Önce tüm mesajları sil
        firestore
            .collection("kullanicilar")
            .document(uid)
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                // Batch delete için WriteBatch kullan
                WriteBatch batch = firestore.batch();

                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    batch.delete(doc.getReference());
                }

                // Chat dökümanını da sil
                DocumentReference chatRef = firestore
                    .collection("kullanicilar")
                    .document(uid)
                    .collection("chats")
                    .document(chatId);
                batch.delete(chatRef);

                // Batch'i commit et
                batch
                    .commit()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(
                            this,
                            "Sohbet silindi",
                            Toast.LENGTH_SHORT
                        ).show();

                        // Eğer silinen chat aktif chat ise, yeni chat başlat
                        if (chatId.equals(currentChatId)) {
                            startNewChat();
                        } else {
                            updateChatMenu();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(
                            this,
                            "Sohbet silinemedi",
                            Toast.LENGTH_SHORT
                        ).show();
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(
                    this,
                    "Sohbet silinemedi",
                    Toast.LENGTH_SHORT
                ).show();
            });
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            final WindowInsetsController insetsController =
                getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(
                    WindowInsets.Type.statusBars() |
                    WindowInsets.Type.navigationBars()
                );
                insetsController.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }
    }

    // Add this function to enable long-press-to-delete for chat menu items
    private void addLongClickListenersToMenuItems() {
        navigationView.post(() -> {
            Menu menu = navigationView.getMenu();
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (item.getGroupId() == 1001) {
                    // Chatler grubu
                    View itemView = navigationView.findViewById(
                        item.getItemId()
                    );
                    if (itemView != null) {
                        itemView.setOnLongClickListener(view -> {
                            String chatId = item.getTitleCondensed() != null
                                ? item.getTitleCondensed().toString()
                                : null;
                            if (chatId != null) {
                                showDeleteChatDialog(chatId);
                                return true;
                            }
                            return false;
                        });
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ai_chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.btnBack) {
            confirmAndDeleteCurrentChat();
            return true;
        } else if (id == R.id.btnBack) {
            startNewChat();
            return true;
        } else if (id == R.id.btnBack) {
            openSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void confirmAndDeleteCurrentChat() {
        if (currentChatId == null) return;

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.mesaji_sil))
            .setMessage(getString(R.string.sohbeti_sil))
            .setPositiveButton(
                getString(R.string.evet_button),
                (dialog, which) -> {
                    deleteChat(currentChatId);
                }
            )
            .setNegativeButton(getString(R.string.hayir_button), null)
            .show();
    }

    // PopupMenu ile chat item'ı için seçenekler sun
    private void showChatPopupMenu(MenuItem item) {
        // Menü item'ının view'unu bul
        View itemView = navigationView.findViewById(item.getItemId());
        if (itemView == null) {
            // View bulunamazsa, doğrudan sohbete git
            String chatId = item.getTitleCondensed() != null
                ? item.getTitleCondensed().toString()
                : null;
            if (chatId != null && !chatId.equals(currentChatId)) {
                openChat(chatId);
            }
            drawerLayout.closeDrawers();
            return;
        }
        PopupMenu popup = new PopupMenu(this, itemView);
        popup.getMenu().add("Sohbete Git");
        popup.getMenu().add("Sil");
        popup.setOnMenuItemClickListener(menuItem -> {
            String chatId = item.getTitleCondensed() != null
                ? item.getTitleCondensed().toString()
                : null;
            if (menuItem.getTitle().equals("Sohbete Git")) {
                if (chatId != null && !chatId.equals(currentChatId)) {
                    openChat(chatId);
                }
            } else if (menuItem.getTitle().equals("Sil")) {
                if (chatId != null) {
                    showDeleteChatDialog(chatId);
                }
            }
            drawerLayout.closeDrawers();
            return true;
        });
        popup.show();
    }

    private void showDeleteChatDialog(String chatId) {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.sohbeti_sil))
            .setMessage(getString(R.string.mesaji_sil))
            .setPositiveButton(
                getString(R.string.evet_button),
                (dialog, which) -> {
                    deleteChat(chatId);
                }
            )
            .setNegativeButton(getString(R.string.hayir_button), null)
            .show();
    }


    // --- Image upload support ---
    private static final int REQUEST_IMAGE_PICK = 101;
    private static final int REQUEST_CAMERA = 102;

    private void openImagePicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Görsel Seç");
        builder.setItems(new String[]{"📷 Kamera", "🖼️ Galeri"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    openCamera();
                    break;
                case 1:
                    openGallery();
                    break;
            }
        });
        builder.show();
    }

    private void openCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
            return;
        }
        
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CAMERA);
        } else {
            Toast.makeText(this, "Kamera uygulaması bulunamadı", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }
    
    private void removeSelectedImage() {
        selectedImageBitmap = null;
        imagePreviewContainer.setVisibility(View.GONE);
        editTextMessage.setHint("Mesajınızı yazın...");
    }
    
    private void showImagePreview(Bitmap bitmap) {
        // Görsel boyutunu kontrol et ve gerekirse optimize et
        if (bitmap != null) {
            // Çok büyük görselleri uyar
            int pixels = bitmap.getWidth() * bitmap.getHeight();
            if (pixels > 33177600) { // 33 megapiksel
                Toast.makeText(this, "Görsel çok büyük, otomatik olarak küçültülecek", Toast.LENGTH_SHORT).show();
            }
            
            selectedImageBitmap = bitmap;
            imagePreview.setImageBitmap(bitmap);
            imagePreviewContainer.setVisibility(View.VISIBLE);
            editTextMessage.setHint("Bu fotoğraf hakkında ne sormak istiyorsun?");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Bitmap bitmap = null;
        
        if (resultCode == RESULT_OK) {
            try {
                if (requestCode == REQUEST_IMAGE_PICK && data != null && data.getData() != null) {
                    // Galeriden seçilen görsel
                    Uri imageUri = data.getData();
                    InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    bitmap = BitmapFactory.decodeStream(imageStream);
                    
                } else if (requestCode == REQUEST_CAMERA && data != null) {
                    // Kameradan çekilen fotoğraf
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        bitmap = (Bitmap) extras.get("data");
                    }
                }
                
                if (bitmap != null) {
                    // Show image preview instead of immediately processing
                    showImagePreview(bitmap);
                }
                
            } catch (Exception e) {
                Log.e("ImageProcessing", "Görsel işlenemedi: " + e.getMessage());
                Toast.makeText(this, "Görsel okunamadı, lütfen tekrar deneyin", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateRemainingRights();
    }
    
    private void updateRemainingRights() {
        String uid = auth.getUid();
        if (uid == null) return;
        
        String tarih = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(new java.util.Date());
        firestore.collection("kullanicilar").document(uid)
                .collection("groq_limit").document(tarih)
                .get()
                .addOnSuccessListener(doc -> {
                    // Maksimum günlük hakkı al (davet ile artabilir)
                    Long maxDaily = doc.getLong("gunluk_max_hak");
                    long gunlukMax = maxDaily != null ? maxDaily : 50; // Varsayılan 50
                    
                    long kullanilan = doc.exists() && doc.contains("count") ? doc.getLong("count") : 0;
                    long kalan = Math.max(0, gunlukMax - kullanilan);
                    
                    // Başlığa hak sayısını ekle
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle("AI Chat (" + kalan + "/" + gunlukMax + ")");
                    }
                    
                    // Eğer hak bitmiş ise kullanıcıyı uyar
                    if (kalan == 0 && editTextMessage != null) {
                        editTextMessage.setHint("Günlük limitiniz doldu. Yarın tekrar deneyin.");
                        editTextMessage.setEnabled(false);
                    } else if (editTextMessage != null) {
                        editTextMessage.setHint("Mesajınızı yazın... (" + kalan + " hak kaldı)");
                        editTextMessage.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AiChat", "Hak sayısı alınamadı: " + e.getMessage());
                });
    }
}