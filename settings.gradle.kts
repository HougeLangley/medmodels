pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }  // 插件门户镜像
        maven { url = uri("https://maven.aliyun.com/repository/google") }         // AGP 及其传递依赖镜像
        maven { url = uri("https://maven.aliyun.com/repository/public") }         // mavenCentral 镜像
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
    }
}
rootProject.name = "MedModels"
include(":app")
