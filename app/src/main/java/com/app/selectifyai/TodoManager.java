package com.app.selectifyai;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class TodoManager {

    private final CollectionReference todoRef;

    public TodoManager() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        todoRef = FirebaseFirestore.getInstance()
                .collection("kullanicilar")
                .document(uid)
                .collection("todos");
    }

    public void yeniTodoEkle(String baslik, String oncelik) {
        yeniTodoEkle(baslik, oncelik, "Genel", "");
    }

    public void yeniTodoEkle(String baslik, String oncelik, String kategori, String aciklama) {
        Map<String, Object> map = new HashMap<>();
        map.put("baslik", baslik);
        map.put("tamamlandi", false);
        map.put("oncelik", oncelik);
        map.put("kategori", kategori);
        map.put("aciklama", aciklama);
        map.put("olusturmaTarihi", System.currentTimeMillis());
        todoRef.add(map);
    }
    // Geri al için tam todo ile ekleme
    public void yeniTodoEkle(Todo todo) {
        Map<String, Object> map = new HashMap<>();
        map.put("baslik", todo.getBaslik());
        map.put("tamamlandi", todo.isTamamlandi());
        map.put("oncelik", todo.getOncelik());
        map.put("kategori", todo.getKategori());
        map.put("aciklama", todo.getAciklama());
        map.put("olusturmaTarihi", todo.getOlusturmaTarihi());
        todoRef.add(map);
    }

    public void tamamlandiGuncelle(String id, boolean durum) {
        todoRef.document(id).update("tamamlandi", durum);
    }

    public void sil(String id) {
        todoRef.document(id).delete();
    }

    public CollectionReference getTodoRef() {
        return todoRef;
    }
}