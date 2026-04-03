
import com.unciv.build.BuildConfig
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val gdxVersion: String by project
val coroutinesVersion: String by project
val roboVMVersion: String by project

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = "https://central.sonatype.com/repository/maven-snapshots")
    }
}

plugins {
    id("kotlin")
    id("com.robovmx.robovm") version "10.2.2.5-SNAPSHOT"
}

apply(plugin = "java")

repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://central.sonatype.com/repository/maven-snapshots")
}

robovm {
    
}

sourceSets {
    main {
        java.srcDir("src/")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(project(":core"))
    implementation("com.robovmx:robovm-rt:$roboVMVersion")
    implementation("com.robovmx:robovm-cocoatouch:$roboVMVersion")
//     implementation("com.badlogicgames.gdx:gdx-backend-robovm:$gdxVersion")
//     implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-ios")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
}

// Configure RoboVM
// project.extensions.configure<org.robovm.gradle.RoboVMPluginExtension>("robovm") {
//     iosSignIdentity = System.getenv("IOS_SIGN_IDENTITY")
//     iosProvisioningProfile = System.getenv("IOS_PROVISIONING_PROFILE")
// }
