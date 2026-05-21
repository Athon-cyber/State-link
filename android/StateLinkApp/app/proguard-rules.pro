# ProGuard rules for StateLink
# No special rules needed — the app uses standard Android SDK and support libraries.
# This file exists so the build.gradle reference doesn't fail.

# Keep JSON parsing
-keepattributes Signature
-keepattributes *Annotation*

# Keep our app classes
-keep class com.statelink.app.** { *; }
