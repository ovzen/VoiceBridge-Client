// Top-level build file where you can add configuration options common to all sub-projects/modules.
//plugins {
//    alias(libs.plugins.android.application) apply false
//    alias(libs.plugins.kotlin.compose) apply false
//}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
//plugins {
  //  id("com.android.application") version "8.3.2" apply false
  //  id("com.android.library") version "8.3.2" apply false
 //   id("org.jetbrains.kotlin.android") version "1.9.24" apply false
//}
// build.gradle.kts (Project: VoiceBridgeClient)
// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Top-level build file
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}