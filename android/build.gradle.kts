buildscript {
    
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.4.0") // ou votre version actuelle du plugin Android Gradle
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22") // ou votre version actuelle du plugin Kotlin Gradle
        // Ajoutez ici les dépendances de classpath supplémentaires si vous en avez
    }
}

// Assurez-vous que ce bloc 'plugins' existe. S'il n'est pas là, ajoutez-le.
plugins {
   
    id("com.google.gms.google-services") version "4.4.2" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory = rootProject.layout.buildDirectory.dir("../../build").get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
