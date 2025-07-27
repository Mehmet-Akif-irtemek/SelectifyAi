#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_app_selectifyai_ApiKeyProvider_getEncryptedGroqKey(JNIEnv *env, jobject) {
    return env->NewStringUTF("LDggFAAaBxIYfDsMMnsZCBssGwQKBC4vHAwvMil4DRI9LA0+Oy18eHwhPx0AEiIkIDE8eSJ8DhI=");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_app_selectifyai_ApiKeyProvider_getEncryptedWeatherKey(JNIEnv *env, jobject) {
    return env->NewStringUTF("eCl+KCh4fXt7LSkufHgqenwoc3pyKCh6eSlyKXx+e3I=");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_app_selectifyai_ApiKeyProvider_getEncryptedFirebaseKey(JNIEnv *env, jobject) {
    return env->NewStringUTF("CgIxKhgyCR0CGxo/L2YBeXw+MyopewMYPSIvEQgCHiomPTscCTke");
}