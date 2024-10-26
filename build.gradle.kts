plugins {
    id("java")
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.papermc.paperweight.userdev") version "1.7.3"
    id("com.gradleup.shadow") version "8.3.3"
}

version = "1.0-SEXO"

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.unnamed.team/repository/unnamed-releases/")
    maven("https://oss.sonatype.org/content/groups/public/")
}

dependencies {
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")

    implementation("dev.triumphteam:triumph-gui:3.1.10")
    implementation("me.fixeddev:commandflow-bukkit:0.6.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    runServer {
        minecraftVersion("1.21.1")
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("project" to project)
        }
    }

    shadowJar {
        relocate("dev.triumphgui", "me.krzu.gui")
    }
}