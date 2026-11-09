pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(
        if (file("flutter_module/.android/include_flutter.groovy").exists()) {
            RepositoriesMode.PREFER_SETTINGS
        } else {
            RepositoriesMode.FAIL_ON_PROJECT_REPOS
        }
    )
    repositories {
        google()
        mavenCentral()
        // Flutter add-to-app engine artifacts (io.flutter:*).
        maven { url = uri("https://storage.googleapis.com/download.flutter.io") }
    }
}
rootProject.name = "TRADENEX"

// The add-to-app project is generated from flutter_fx by scripts/bootstrap_flutter_module.sh.
// Keep native-only development builds working until the module has been bootstrapped.
val flutterInclude = file("flutter_module/.android/include_flutter.groovy")
if (flutterInclude.exists()) {
    apply(from = flutterInclude)
}
include(":app")
