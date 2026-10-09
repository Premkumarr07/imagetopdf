-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# AndroidX Security / Tink
-keep class androidx.security.crypto.** { *; }
-dontwarn com.google.errorprone.annotations.**

# PDF / graphics
-keep class android.graphics.pdf.** { *; }
