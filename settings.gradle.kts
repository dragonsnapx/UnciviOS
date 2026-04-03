import java.util.Properties

pluginManagement {
    repositories {
        mavenLocal() // To get the compiler plugin locally
        mavenCentral() // Required for plugins published outside the Gradle Plugin Portal
        gradlePluginPortal() // So other plugins can be resolved
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots")
        }
    }
}

include("desktop", "core", "tests", "server")

private fun getSdkPath(): String? {
    val localProperties = file("local.properties")
    return if (localProperties.exists()) {
        val properties = Properties()
        localProperties.inputStream().use { properties.load(it) }

        properties.getProperty("sdk.dir") ?: System.getenv("ANDROID_HOME")
    } else {
        System.getenv("ANDROID_HOME")
    }
}
if (getSdkPath() != null) include("android")
if (System.getProperty("os.name").contains("Mac")) include("ios")
