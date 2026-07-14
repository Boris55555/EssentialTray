# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/aasimestari/AndroidStudioProjects/EssentialTray/app/build/intermediates/default_proguard_files/global/proguard-android-optimize.txt-8.7.3
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep rules here:

# Keep Compose related classes if needed (usually handled by the compiler plugin)
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
