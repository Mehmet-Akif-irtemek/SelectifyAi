package com.app.selectifyai;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Manages dice rights and user limits for ZarAi functionality
 */
public class ZarAiManager {

    private static final int FREE_DICE_LIMIT = 20; // Free kullanıcı için default hak
    private static final int INVITED_DICE_LIMIT = 30; // Davet eden free kullanıcı için hak
    private static final int PREMIUM_DICE_LIMIT = 30;
    private static final int PRO_DICE_LIMIT = Integer.MAX_VALUE; // Sınırsız
    private static final int FREE_AI_LIMIT = 20;
    private static final int PREMIUM_AI_LIMIT = 50;
    private static final int PRO_AI_LIMIT = Integer.MAX_VALUE; // Sınırsız
    private static final int FREE_DAILY_LIMIT = 20;
    private static final int PRO_DAILY_LIMIT = Integer.MAX_VALUE; // Sınırsız

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final Context context;
    private final boolean isPro;

    public interface RightsCallback {
        void onRightsLoaded(long remainingRights);
        void onError(String error);
    }

    public interface AILimitCallback {
        void onResult(boolean canUse, int remaining);
    }

    public ZarAiManager(Context context, boolean isPro) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.isPro = isPro;
    }

    /**
     * Kullanıcının zar atma haklarını kontrol eder
     * @param callback Haklar yüklendiğinde veya hata oluştuğunda çağrılacak callback
     */
    public void checkDiceRights(RightsCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        // Kullanıcı bilgilerini al
        firestore
            .collection(context.getString(R.string.kullanicilar))
            .document(uid)
            .get()
            .addOnSuccessListener(userDoc -> {
                // Üyelik tipini kontrol et, varsayılan olarak "free" kullan
                String membershipType = userDoc.contains("membershipType")
                    ? userDoc.getString("membershipType")
                    : "free";

                // Free kullanıcı davet ettiyse hak 30, etmediyse 20
                int diceLimit;
                if ("premium".equals(membershipType)) {
                    diceLimit = PREMIUM_DICE_LIMIT;
                } else if ("pro".equals(membershipType)) {
                    diceLimit = PRO_DICE_LIMIT;
                } else {
                    // Free ise davet kontrolü
                    boolean invitedSomeone = false;
                    if (userDoc.contains("invitedSomeone")) {
                        Boolean invited = userDoc.getBoolean("invitedSomeone");
                        invitedSomeone = invited != null && invited;
                    }
                    diceLimit = invitedSomeone
                        ? INVITED_DICE_LIMIT
                        : FREE_DICE_LIMIT;
                }

                String today = getTodayString();

                // Bugünkü zar hakkı bilgisini al
                firestore
                    .collection(context.getString(R.string.kullanicilar))
                    .document(uid)
                    .collection(
                        context.getString(R.string.zar_hakki_collection)
                    )
                    .document(today)
                    .get()
                    .addOnSuccessListener(doc -> {
                        try {
                            // Kalan hak sayısını al, belge yoksa veya değer null ise limit değerini kullan
                            Long remainingRaw = doc.getLong(
                                context.getString(R.string.kalan)
                            );
                            Long remaining = (remainingRaw == null)
                                ? (long) diceLimit
                                : remainingRaw;

                            // Negatif değer olmamasını sağla
                            remaining = Math.max(0, remaining);

                            callback.onRightsLoaded(remaining);
                        } catch (Exception e) {
                            android.util.Log.e(
                                "ZarAiManager",
                                "Zar hakkı bilgisi işlenirken hata: " +
                                e.getMessage()
                            );
                            callback.onError(
                                "Zar hakkı bilgisi işlenirken hata: " +
                                e.getMessage()
                            );
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e(
                            "ZarAiManager",
                            "Zar hakkı bilgisi alınamadı: " + e.getMessage()
                        );
                        callback.onError(e.getMessage());
                    });
            });
    }

    /**
     * Bir zar atma hakkı tüketir
     * @param callback İşlem sonucunda çağrılacak callback
     */
    public void consumeDiceRight(RightsCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        String today = getTodayString();
        var hakRef = firestore
            .collection(context.getString(R.string.kullanicilar))
            .document(uid)
            .collection(context.getString(R.string.zar_hakki_collection))
            .document(today);

        hakRef
            .get()
            .addOnSuccessListener(doc -> {
                try {
                    // Kalan hak sayısını al, belge yoksa veya değer null ise varsayılan limit değerini kullan
                    Long remaining = doc.getLong(
                        context.getString(R.string.kalan)
                    );
                    if (remaining == null) {
                        // Kullanıcının üyelik tipini kontrol et
                        firestore
                            .collection(
                                context.getString(R.string.kullanicilar)
                            )
                            .document(uid)
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                String membershipType = userDoc.contains(
                                        "membershipType"
                                    )
                                    ? userDoc.getString("membershipType")
                                    : "free";

                                long defaultLimit;
                                if (
                                    "premium".equals(membershipType)
                                ) defaultLimit = PREMIUM_DICE_LIMIT;
                                else if (
                                    "pro".equals(membershipType)
                                ) defaultLimit = PRO_DICE_LIMIT;
                                else defaultLimit = FREE_DICE_LIMIT;

                                processRightConsumption(
                                    hakRef,
                                    defaultLimit,
                                    callback
                                );
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e(
                                    "ZarAiManager",
                                    "Üyelik tipi alınamadı: " + e.getMessage()
                                );
                                // Hata durumunda varsayılan olarak FREE_DICE_LIMIT kullan
                                processRightConsumption(
                                    hakRef,
                                    FREE_DICE_LIMIT,
                                    callback
                                );
                            });
                    } else {
                        processRightConsumption(hakRef, remaining, callback);
                    }
                } catch (Exception e) {
                    android.util.Log.e(
                        "ZarAiManager",
                        "Zar hakkı tüketilirken hata: " + e.getMessage()
                    );
                    callback.onError(
                        "Zar hakkı tüketilirken hata: " + e.getMessage()
                    );
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e(
                    "ZarAiManager",
                    "Zar hakkı bilgisi alınamadı: " + e.getMessage()
                );
                callback.onError(
                    "Zar hakkı bilgisi alınamadı: " + e.getMessage()
                );
            });
    }

    /**
     * Zar hakkı tüketim işlemini gerçekleştirir
     * @param hakRef Firestore referansı
     * @param remaining Kalan hak sayısı
     * @param callback İşlem sonucunda çağrılacak callback
     */
    private void processRightConsumption(
        DocumentReference hakRef,
        long remaining,
        RightsCallback callback
    ) {
        if (remaining > 0) {
            hakRef
                .set(
                    Collections.singletonMap(
                        context.getString(R.string.kalan),
                        remaining - 1
                    ),
                    SetOptions.merge()
                )
                .addOnSuccessListener(aVoid ->
                    callback.onRightsLoaded(remaining - 1)
                )
                .addOnFailureListener(e -> {
                    android.util.Log.e(
                        "ZarAiManager",
                        "Zar hakkı güncellenirken hata: " + e.getMessage()
                    );
                    callback.onError(
                        "Zar hakkı güncellenirken hata: " + e.getMessage()
                    );
                });
        } else {
            callback.onError("No rights remaining");
        }
    }

    /**
     * Ekstra zar hakkı ekler
     * @param amount Eklenecek hak miktarı
     * @param callback İşlem sonucunda çağrılacak callback
     */
    public void addExtraRights(int amount, RightsCallback callback) {
        // Geçersiz miktar kontrolü
        if (amount <= 0) {
            callback.onError("Geçersiz hak miktarı: " + amount);
            return;
        }

        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        String today = getTodayString();
        var hakRef = firestore
            .collection(context.getString(R.string.kullanicilar))
            .document(uid)
            .collection(context.getString(R.string.zar_hakki_collection))
            .document(today);

        hakRef
            .get()
            .addOnSuccessListener(doc -> {
                try {
                    Long remaining = doc.getLong(
                        context.getString(R.string.kalan)
                    );
                    if (remaining == null) {
                        // Kullanıcının üyelik tipine göre varsayılan limiti belirle
                        firestore
                            .collection(
                                context.getString(R.string.kullanicilar)
                            )
                            .document(uid)
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                try {
                                    String membershipType = userDoc.contains(
                                            "membershipType"
                                        )
                                        ? userDoc.getString("membershipType")
                                        : "free";

                                    long defaultLimit;
                                    if (
                                        "premium".equals(membershipType)
                                    ) defaultLimit = PREMIUM_DICE_LIMIT;
                                    else if (
                                        "pro".equals(membershipType)
                                    ) defaultLimit = PRO_DICE_LIMIT;
                                    else defaultLimit = FREE_DICE_LIMIT;

                                    // Ekstra hakları varsayılan limite ekle
                                    updateRights(
                                        hakRef,
                                        defaultLimit,
                                        amount,
                                        callback
                                    );
                                } catch (Exception e) {
                                    android.util.Log.e(
                                        "ZarAiManager",
                                        "Üyelik tipi işlenirken hata: " +
                                        e.getMessage()
                                    );
                                    callback.onError(
                                        "Üyelik tipi işlenirken hata: " +
                                        e.getMessage()
                                    );
                                }
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e(
                                    "ZarAiManager",
                                    "Kullanıcı bilgisi alınamadı: " +
                                    e.getMessage()
                                );
                                callback.onError(
                                    "Kullanıcı bilgisi alınamadı: " +
                                    e.getMessage()
                                );
                            });
                    } else {
                        // Mevcut limite ekstra hakları ekle
                        updateRights(hakRef, remaining, amount, callback);
                    }
                } catch (Exception e) {
                    android.util.Log.e(
                        "ZarAiManager",
                        "Hak ekleme işlemi sırasında hata: " + e.getMessage()
                    );
                    callback.onError(
                        "Hak ekleme işlemi sırasında hata: " + e.getMessage()
                    );
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e(
                    "ZarAiManager",
                    "Zar hakkı bilgisi alınamadı: " + e.getMessage()
                );
                callback.onError(
                    "Zar hakkı bilgisi alınamadı: " + e.getMessage()
                );
            });
    }

    /**
     * Zar haklarını günceller
     * @param hakRef Firestore referansı
     * @param currentRights Mevcut hak sayısı
     * @param amountToAdd Eklenecek hak miktarı
     * @param callback İşlem sonucunda çağrılacak callback
     */
    private void updateRights(
        DocumentReference hakRef,
        long currentRights,
        int amountToAdd,
        RightsCallback callback
    ) {
        long newRights = currentRights + amountToAdd;
        hakRef
            .set(
                Collections.singletonMap(
                    context.getString(R.string.kalan),
                    newRights
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener(aVoid -> {
                android.util.Log.d(
                    "ZarAiManager",
                    "Zar hakları güncellendi: " + newRights
                );
                callback.onRightsLoaded(newRights);
            })
            .addOnFailureListener(e -> {
                android.util.Log.e(
                    "ZarAiManager",
                    "Zar hakları güncellenirken hata: " + e.getMessage()
                );
                callback.onError(
                    "Zar hakları güncellenirken hata: " + e.getMessage()
                );
            });
    }

    /**
     * Kullanıcının AI kullanım limitini kontrol eder
     * @param callback Limit kontrolü sonucunda çağrılacak callback
     */
    public void checkAILimit(AILimitCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onResult(false, 0);
            return;
        }

        int dailyLimit = isPro ? PRO_DAILY_LIMIT : FREE_DAILY_LIMIT;

        String today = new SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(new Date());
        firestore
            .collection("kullanicilar")
            .document(uid)
            .collection("aiKullanim")
            .document(today)
            .get()
            .addOnSuccessListener(document -> {
                Long usageCount = document.getLong("count");
                int used = usageCount != null ? usageCount.intValue() : 0;
                int remaining = Math.max(0, dailyLimit - used);
                callback.onResult(remaining > 0, remaining);
            })
            .addOnFailureListener(e -> callback.onResult(true, dailyLimit));
    }

    /**
     * AI kullanım sayısını bir artırır
     */
    public void incrementAIUsage(Runnable onSuccess, Runnable onFailure) {
        String uid = auth.getUid();
        if (uid == null) {
            onFailure.run();
            return;
        }

        String today = new SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(new Date());
        DocumentReference usageRef = firestore
            .collection("kullanicilar")
            .document(uid)
            .collection("aiKullanim")
            .document(today);

        firestore
            .runTransaction(
                (Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot snapshot = transaction.get(usageRef);
                    long newCount;
                    if (snapshot.exists()) {
                        Long currentCount = snapshot.getLong("count");
                        if (currentCount != null) {
                            newCount = currentCount + 1;
                        } else {
                            newCount = 1;
                        }
                    } else {
                        newCount = 1;
                    }
                    transaction.set(
                        usageRef,
                        new HashMap<String, Object>() {
                            {
                                put("count", newCount);
                            }
                        },
                        SetOptions.merge()
                    );
                    return null;
                }
            )
            .addOnSuccessListener(aVoid -> onSuccess.run())
            .addOnFailureListener(e -> onFailure.run());
    }

    /**
     * Kullanıcının AI kullanım limitini artırır
     * @param amount Eklenecek limit miktarı
     * @param callback İşlem sonucunda çağrılacak callback
     */
    public void addAILimit(int amount, AILimitCallback callback) {
        // Geçersiz miktar kontrolü
        if (amount <= 0) {
            callback.onResult(false, 0);
            android.util.Log.e(
                "ZarAiManager",
                "Geçersiz AI limit miktarı: " + amount
            );
            return;
        }

        String uid = auth.getUid();
        if (uid == null) {
            callback.onResult(false, 0);
            android.util.Log.e(
                "ZarAiManager",
                "Kullanıcı oturum açmamış, AI limiti artırılamadı"
            );
            return;
        }

        // Önce mevcut limiti kontrol et
        checkAILimit(
            new AILimitCallback() {
                @Override
                public void onResult(boolean canUse, int remaining) {
                    // Kullanıcının üyelik tipini al
                    firestore
                        .collection(context.getString(R.string.kullanicilar))
                        .document(uid)
                        .get()
                        .addOnSuccessListener(userDoc -> {
                            try {
                                // Üyelik tipini kontrol et
                                String membershipType = userDoc.contains(
                                        "membershipType"
                                    )
                                    ? userDoc.getString("membershipType")
                                    : "free";

                                // Üyelik tipine göre maksimum limit belirle
                                long maxLimit;
                                if ("premium".equals(membershipType)) maxLimit =
                                    PREMIUM_AI_LIMIT;
                                else if (
                                    "pro".equals(membershipType)
                                ) maxLimit = PRO_AI_LIMIT;
                                else maxLimit = FREE_AI_LIMIT;

                                // Bugünkü kullanım bilgisini al
                                String today = new SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.getDefault()
                                ).format(new Date());
                                var limitRef = firestore
                                    .collection(
                                        context.getString(R.string.kullanicilar)
                                    )
                                    .document(uid)
                                    .collection("aiKullanim")
                                    .document(today);

                                limitRef
                                    .get()
                                    .addOnSuccessListener(doc -> {
                                        try {
                                            // Mevcut kullanım sayısını al
                                            Long countValue = doc.exists()
                                                ? doc.getLong("count")
                                                : null;
                                            long used = countValue != null
                                                ? countValue
                                                : 0;

                                            // Kullanım sayısını azalt (limiti artır)
                                            long newCount = Math.max(
                                                0,
                                                used - amount
                                            ); // Negatif değer olmamasını sağla

                                            limitRef
                                                .set(
                                                    Collections.singletonMap(
                                                        "count",
                                                        newCount
                                                    ),
                                                    SetOptions.merge()
                                                )
                                                .addOnSuccessListener(aVoid -> {
                                                    android.util.Log.d(
                                                        "ZarAiManager",
                                                        "AI kullanım sayısı güncellendi: " +
                                                        newCount
                                                    );
                                                    // Yeni kalan hak sayısını hesapla
                                                    long newRemaining =
                                                        Math.max(
                                                            0,
                                                            maxLimit - newCount
                                                        );
                                                    callback.onResult(
                                                        true,
                                                        (int) newRemaining
                                                    );
                                                })
                                                .addOnFailureListener(e -> {
                                                    android.util.Log.e(
                                                        "ZarAiManager",
                                                        "AI kullanım sayısı güncellenemedi: " +
                                                        e.getMessage()
                                                    );
                                                    callback.onResult(
                                                        false,
                                                        (int) remaining
                                                    );
                                                });
                                        } catch (Exception e) {
                                            android.util.Log.e(
                                                "ZarAiManager",
                                                "AI limit bilgisi işlenirken hata: " +
                                                e.getMessage()
                                            );
                                            callback.onResult(
                                                false,
                                                (int) remaining
                                            );
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        android.util.Log.e(
                                            "ZarAiManager",
                                            "AI limit bilgisi alınamadı: " +
                                            e.getMessage()
                                        );
                                        callback.onResult(
                                            false,
                                            (int) remaining
                                        );
                                    });
                            } catch (Exception e) {
                                android.util.Log.e(
                                    "ZarAiManager",
                                    "Kullanıcı bilgisi işlenirken hata: " +
                                    e.getMessage()
                                );
                                callback.onResult(false, (int) remaining);
                            }
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e(
                                "ZarAiManager",
                                "Kullanıcı bilgisi alınamadı: " + e.getMessage()
                            );
                            callback.onResult(false, (int) remaining);
                        });
                }
            }
        );
    }

    private String getTodayString() {
        return new SimpleDateFormat(
            context.getString(R.string.yyyy_mm_dd_format),
            Locale.getDefault()
        ).format(new Date());
    }
}
