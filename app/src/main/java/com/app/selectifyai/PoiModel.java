package com.app.selectifyai;

import org.json.JSONException;
import org.json.JSONObject;

public class PoiModel {
    private final String ad;
    private final String tur;
    private final double mesafe;
    private final double lat;
    private final double lon;

    public PoiModel(String ad, String tur, double mesafe, double lat, double lon) {
        this.ad = ad;
        this.tur = tur;
        this.mesafe = mesafe;
        this.lat = lat;
        this.lon = lon;
    }

    public String getAd() {
        return ad;
    }

    public String getTur() {
        return tur;
    }

    public double getMesafe() {
        return mesafe;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("ad", ad);
        obj.put("tur", tur);
        obj.put("mesafe", mesafe);
        obj.put("lat", lat);
        obj.put("lon", lon);
        return obj;
    }

    public static PoiModel fromJson(JSONObject obj) {
        return new PoiModel(
                obj.optString("ad"),
                obj.optString("tur"),
                obj.optDouble("mesafe"),
                obj.optDouble("lat"),
                obj.optDouble("lon")
        );
    }
}
