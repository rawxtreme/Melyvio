import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

plugins {
    alias(libs.plugins.hilt) apply (false)
    alias(libs.plugins.kotlin.ksp) apply (false)
    alias(libs.plugins.kotlin.serialization) apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://maven.aliyun.com/repository/public") }
    }
    dependencies {
        classpath(libs.gradle)
        classpath(kotlin("gradle-plugin", libs.versions.kotlin.get()))
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            if (project.findProperty("enableComposeCompilerReports") == "true") {
                arrayOf("reports", "metrics").forEach {
                    freeCompilerArgs.add("-P")
                    freeCompilerArgs.add("plugin:androidx.compose.compiler.plugins.kotlin:${it}Destination=${project.layout.buildDirectory}/compose_metrics")
                }
            }
        }
    }
}

tasks.register("unzipAndReplaceFiles") {
    doLast {
        val zipFile = file("Melyvio-update.zip")
        val destDir = projectDir

        if (!zipFile.exists()) {
            throw GradleException("Zip file not found at: ${zipFile.absolutePath}")
        }

        println("Starting extraction to: ${destDir.absolutePath}")

        ZipInputStream(zipFile.inputStream()).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val filePath = File(destDir, entry.name)

                // Security check to prevent path traversal issues
                if (!filePath.canonicalPath.startsWith(destDir.canonicalPath + File.separator)) {
                    throw IOException("Entry is outside of the target dir: ${entry.name}")
                }

                if (entry.isDirectory) {
                    filePath.mkdirs()
                } else {
                    filePath.parentFile?.mkdirs()
                    // This seamlessly overwrites existing files without throwing locking errors
                    FileOutputStream(filePath).use { fos ->
                        zipIn.copyTo(fos)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
        println("Extraction complete! All matching files successfully replaced.")
    }
}
