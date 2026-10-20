# Gson reads the API models by reflection, so keep their field names if minification is ever switched on.
-keep class com.teampointblank.lpms.data.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
