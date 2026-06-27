# ============================================================================
# Mushotoku – R8/ProGuard Keep-Regeln (Release)
#
# Wird zusammen mit proguard-android-optimize.txt (Standard-AGP-Datei) und den
# von den Bibliotheken mitgelieferten consumer-Regeln angewendet. Ziel: maximale
# Verkleinerung/Optimierung durch R8 (full mode), ohne die per JNI/Reflection
# erreichten Klassen zu entfernen.
#
# Hinweis: native <methods> werden bereits durch proguard-android-optimize.txt
# erhalten (-keepclasseswithmembernames ... native <methods>). Die folgenden
# Regeln sichern zusätzlich die JNI-Brückenklassen, die der native Code per
# Namen auflöst, sowie die kotlinx.serialization-Generate.
# ============================================================================

# --- SQLCipher (net.zetetic:sqlcipher-android) ------------------------------
# Die AAR liefert eigene consumer-Regeln; diese Keeps sind eine Absicherung,
# falls der native Code Klassen-/Methodennamen per JNI auflöst.
-keep class net.sqlcipher.** { *; }
-keep interface net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# --- Argon2 (com.lambdapioneer.argon2kt) ------------------------------------
# JNI-Brücke zur nativen libargon2-Implementierung; Namen dürfen nicht
# verändert werden, sonst schlägt der native Lookup fehl.
-keep class com.lambdapioneer.argon2kt.** { *; }
-dontwarn com.lambdapioneer.argon2kt.**

# --- kotlinx.serialization --------------------------------------------------
# Das Serialization-Compiler-Plugin erzeugt die Serializer ohne Reflection;
# diese Regeln sind die offiziell empfohlene Absicherung gegen zu aggressives
# Shrinking generierter $$serializer-Klassen und Companion-Serializer.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# @Serializable-Modelle der App: Companion + generierten Serializer behalten.
-keepclassmembers @kotlinx.serialization.Serializable class com.mushotoku.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class com.mushotoku.app.**
-keep class com.mushotoku.app.**$$serializer { *; }
