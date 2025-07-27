package com.app.selectifyai;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import androidx.core.content.ContextCompat;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Helper class for location and weather operations
 */
public class LocationWeatherHelper {
    private final Context context;
    private final FusedLocationProviderClient locationClient;
    
    public interface LocationCallback {
        void onLocationReceived(String district, String weather);
        void onError(String error);
    }
    
    public LocationWeatherHelper(Context context) {
        this.context = context;
        this.locationClient = LocationServices.getFusedLocationProviderClient(context);
    }
    
    public void getLocationAndWeather(LocationCallback callback) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            callback.onError("Location permission not granted");
            return;
        }
        
        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                String district = getDistrictFromLocation(location);
                getWeatherInfo(location.getLatitude(), location.getLongitude(), weather -> {
                    callback.onLocationReceived(district, weather);
                });
            } else {
                callback.onError("Location not available");
            }
        }).addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
    
    private String getDistrictFromLocation(Location location) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(
                location.getLatitude(), location.getLongitude(), 1);
            
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                return address.getSubAdminArea();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    private void getWeatherInfo(double lat, double lon, WeatherCallback callback) {
        String apiKey = KeyDecryptor.getWeatherKey();
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat +
                "&lon=" + lon + "&appid=" + apiKey + "&units=metric&lang=tr";
        
        RequestQueue queue = Volley.newRequestQueue(context);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String city = response.getString("name");
                        JSONObject main = response.getJSONObject("main");
                        double temp = main.getDouble("temp");
                        
                        JSONArray weatherArray = response.getJSONArray("weather");
                        JSONObject weather = weatherArray.getJSONObject(0);
                        String description = weather.getString("description");
                        
                        String weatherInfo = city + ": " + description + " - " + temp + "°C";
                        callback.onWeatherReceived(weatherInfo);
                        
                    } catch (Exception e) {
                        callback.onWeatherReceived("");
                    }
                },
                error -> callback.onWeatherReceived(""));
        
        queue.add(request);
    }
    
    private interface WeatherCallback {
        void onWeatherReceived(String weather);
    }
}