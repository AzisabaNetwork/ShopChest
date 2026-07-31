plugins {
    `java-library`
    `maven-publish`
//    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("com.gradleup.shadow") version "9.4.1"
}

group = "de.epiceric"
version = "1.21.11+1.13.8"
description = "ShopChest"

val projectUrl = project.findProperty("projectUrl")?.toString().orEmpty()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))

    withSourcesJar()
//    withJavadocJar()
}

// paperweight.reobfArtifactConfiguration.set(io.papermc.paperweight.userdev.ReobfArtifactConfiguration.REOBF_PRODUCTION)

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }

    maven {
        url = uri("https://nexus.hc.to/content/repositories/pub_releases/")
    }

    maven {
        url = uri("https://repo.codemc.org/repository/maven-public/")
    }

    maven {
        url = uri("https://maven.enginehub.org/repo/")
    }

    maven {
        url = uri("https://www.uskyblock.ovh/maven/uskyblock/")
    }

    maven {
        url = uri("https://jitpack.io")
    }

    maven {
        url = uri("https://repo.inventivetalent.org/content/groups/public/")
    }

    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        url = uri("https://minevolt.net/repo")
    }

    maven {
        url = uri("https://repo.azisaba.net/repository/maven-public/")
    }

    maven("https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
//    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    api(libs.org.codemc.worldguardwrapper.worldguardwrapper)
    api(libs.org.bstats.bstats.bukkit)
    api(libs.com.zaxxer.hikaricp)
    api(libs.org.slf4j.slf4j.jdk14)
    api(libs.org.inventivetalent.reflectionhelper)
    compileOnly(libs.com.github.milkbowl.vaultapi) {
        exclude("org.bukkit", "bukkit")
    }
    compileOnly(libs.io.papermc.paper.api)
    compileOnly(files("../Townia/build/libs/Townia-1.0-SNAPSHOT.jar"))
    compileOnly(libs.com.github.techfortress.griefprevention)
    compileOnly(libs.com.palmergames.bukkit.towny.towny)
    compileOnly(libs.org.projectlombok.lombok)
    compileOnly(fileTree("libs"))
    compileOnly("io.lumine:Mythic-Dist:5.12.0")
}

tasks {
    processResources {
        from(
            sourceSets.main
                .get()
                .resources.srcDirs,
        ) {
            include("**")
            val tokenReplacementMap =
                mapOf(
                    "name" to project.name,
                    "version" to project.version,
                    "description" to project.description,
                    "url" to projectUrl,
                )
            filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to tokenReplacementMap)
        }
        filteringCharset = "UTF-8"
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(projectDir) { include("LICENSE") }
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    shadowJar {
        enableAutoRelocation = true
        relocationPrefix = "de.epiceric.shopchest.libs"
    }
}

publishing {
    repositories {
        maven {
            name = "repo"
            credentials(PasswordCredentials::class)
            url =
                uri(
                    if (project.version.toString().endsWith("SNAPSHOT")) {
                        project.findProperty("deploySnapshotURL")
                            ?: System.getProperty("deploySnapshotURL", "https://repo.azisaba.net/repository/maven-snapshots/")
                    } else {
                        project.findProperty("deployReleasesURL")
                            ?: System.getProperty("deployReleasesURL", "https://repo.azisaba.net/repository/maven-releases/")
                    },
                )
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
