# Keep this app’s code readable in Play Vitals after R8.
-keepattributes SourceFile,LineNumberTable,*Annotation*,InnerClasses,Signature,Exception
-keep class com.ramapalani.civics2025.** { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
