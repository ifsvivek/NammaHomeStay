# =============================================================================
# Namma HomeStay — R8 / ProGuard keep rules
# =============================================================================
# The default Android optimize file ("proguard-android-optimize.txt") is also
# applied — see `proguardFiles(...)` in app/build.gradle.kts. Keep this file
# focused on what's *specific* to this project (Firestore POJOs, osmdroid,
# Compose-via-reflection cases).
#
# Keep line numbers + source files in the release stack traces.
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# -----------------------------------------------------------------------------
# Firestore POJOs
# -----------------------------------------------------------------------------
# Firebase Firestore deserialises documents into our `data class` models via
# reflection: it looks up fields by name, calls getters / accessors, and
# instantiates via the no-arg constructor. Both of those break under R8's
# default minify + obfuscate, so we keep every model class verbatim.
-keep class com.ifsvivek.nammahomestay.data.model.** { *; }
-keepclassmembers class com.ifsvivek.nammahomestay.data.model.** {
    <init>(...);
    <fields>;
}

# Firestore annotations on fields (@DocumentId, @ServerTimestamp, @PropertyName,
# @Exclude, @IgnoreExtraProperties) are read at runtime — keep them. The main
# Firestore library ships its own consumer-rules so its public API is safe;
# this is just for *our* models' annotated fields.
-keepclassmembers class * {
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.Exclude <fields>;
}

# -----------------------------------------------------------------------------
# osmdroid
# -----------------------------------------------------------------------------
# osmdroid has some reflection (tile source factories, overlays) and only ships
# minimal consumer-rules. Keeping it verbatim is the safest default.
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# -----------------------------------------------------------------------------
# Coil — image loader uses reflection-y bits internally
# -----------------------------------------------------------------------------
-dontwarn coil.**

# -----------------------------------------------------------------------------
# Kotlin / Compose safety nets (mostly covered by default rules, but explicit
# is better than mysterious release-only crashes).
# -----------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlin.Metadata { *; }
