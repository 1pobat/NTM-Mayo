import java.security.MessageDigest

plugins {
    java
    id("net.neoforged.moddev") version "2.0.148"
}

group = "dev.hbmmayo"
version = "0.1.0-dev"
base { archivesName = "hbmmayo" }
java { toolchain.languageVersion = JavaLanguageVersion.of(25) }
repositories {
    mavenCentral()
    if (providers.gradleProperty("withClientDependencies").isPresent) {
        maven("https://repo.warfactory.co/releases")
    }
}

// Resolved relative to THIS build.gradle.kts, not the shell working directory.
val ntmJar = layout.projectDirectory.file("ntmjar/ntm-next.jar")
dependencies {
    compileOnly(files(ntmJar))
    runtimeOnly(files(ntmJar))
    if (providers.gradleProperty("withClientDependencies").isPresent) {
        runtimeOnly("dev.engine_room:crankshaft-neoforge:1.6.0+mc26.2")
    }
    compileOnly("io.github.llamalad7:mixinextras-common:0.5.5")
}
neoForge {
    version = "26.2.0.89"
    runs {
        configureEach {
            jvmArgument("--add-modules=jdk.incubator.vector")
            jvmArgument("--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED")
        }
        create("client") { client() }
        create("server") { server(); programArgument("--nogui") }
    }
    mods { create("hbmmayo") { sourceSet(sourceSets.main.get()) } }
}
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
val coreChecks by tasks.registering(JavaExec::class) {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "dev.hbmmayo.core.CoreChecks"
}
tasks.check { dependsOn(coreChecks) }

val verifyNtm by tasks.registering {
    inputs.file(ntmJar)
    doLast {
        check(ntmJar.asFile.isFile) { "Missing local dependency: ${ntmJar.asFile}" }
        val actual = MessageDigest.getInstance("SHA-256")
            .digest(ntmJar.asFile.readBytes()).joinToString("") { "%02x".format(it) }
        check(actual == "923a0a2bea1deded10a71cc7273cb687d728b27fe1c2ba06774150f09c28a273") {
            "Unverified NTM NEXT binary: $actual. Expected 1.0.3+1b8c6c2d."
        }
    }
}
tasks.compileJava { dependsOn(verifyNtm) }
tasks.jar {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
