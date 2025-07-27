plugins {
    alias(libs.plugins.android.application) apply false
    // Google Services plugin’i burada tanımlı olsun:
    id("com.google.gms.google-services") version "4.4.0" apply false
}