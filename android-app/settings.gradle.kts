pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")

        // PENTING: Library UVC perlu akses ke repo http ini
        maven("http://raw.github.com/saki4510t/libcommon/master/repository/") {
            isAllowInsecureProtocol = true
        }
    }
}

rootProject.name = "KubikCamPhotobooth"
include(":app")