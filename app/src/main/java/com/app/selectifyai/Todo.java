package com.app.selectifyai;

import androidx.annotation.NonNull;

/**
 * Todo görev modeli. Firestore'dan veriyi temsil eder.
 */
public class Todo {

    private String id;
    private String baslik;
    private boolean tamamlandi;
    private String oncelik;
    private String kategori;
    private long olusturmaTarihi;
    private String aciklama;

    /**
     * Boş yapıcı Firestore tarafından kullanılmak üzere gereklidir.
     */
    public Todo() {
        // Firestore için gerekli
    }

    /**
     * Tam yapıcı metod.
     * @param id Firestore doküman ID'si
     * @param baslik Görev metni
     * @param tamamlandi Görev tamamlandı mı
     */
    public Todo(@NonNull String id, @NonNull String baslik, boolean tamamlandi) {
        this.id = id;
        this.baslik = baslik;
        this.tamamlandi = tamamlandi;
        this.olusturmaTarihi = System.currentTimeMillis();
    }

    // Getter'lar
    public String getId() {
        return id;
    }

    public String getBaslik() {
        return baslik;
    }

    public boolean isTamamlandi() {
        return tamamlandi;
    }

    public String getOncelik() {
        return oncelik;
    }

    public String getKategori() {
        return kategori;
    }

    public long getOlusturmaTarihi() {
        return olusturmaTarihi;
    }

    public String getAciklama() {
        return aciklama;
    }

    // Setter'lar
    public void setId(String id) {
        this.id = id;
    }

    public void setBaslik(String baslik) {
        this.baslik = baslik;
    }

    public void setTamamlandi(boolean tamamlandi) {
        this.tamamlandi = tamamlandi;
    }

    public void setOncelik(String oncelik) {
        this.oncelik = oncelik;
    }

    public void setKategori(String kategori) {
        this.kategori = kategori;
    }

    public void setOlusturmaTarihi(long olusturmaTarihi) {
        this.olusturmaTarihi = olusturmaTarihi;
    }

    public void setAciklama(String aciklama) {
        this.aciklama = aciklama;
    }
}