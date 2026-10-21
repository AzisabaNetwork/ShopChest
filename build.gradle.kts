plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "9.4.1"
}

group = "de.epiceric"
version = "1.13.4+1.15.2"
description = "A Bukkit plugin letting players create custom chest shops."

val projectUrl = providers.gradleProperty("projectUrl").orElse("")

java {
    // Spigot 1.15.2 runs on Java 8+, so keep the bytecode level at 8.
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://jitpack.io")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.inventivetalent.org/content/groups/public/")
    maven("https://repo.glaremasters.me/repository/towny/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.azisaba.net/repository/maven-public/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.15.2-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { exclude("org.bukkit", "bukkit") }
    compileOnly("com.github.TechFortress:GriefPrevention:16.17.1")
    compileOnly("com.palmergames.bukkit.towny:Towny:0.96.1.0")
    compileOnly(files("lib/IslandWorld-8.5.jar"))
    compileOnly("net.azisaba:LifeCore:6.3.2")

    implementation("org.codemc.worldguardwrapper:worldguardwrapper:1.2.0-SNAPSHOT")
    implementation("org.bstats:bstats-bukkit:2.2.1")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.slf4j:slf4j-jdk14:1.7.36")
    implementation("org.inventivetalent:reflectionhelper:1.18.4-SNAPSHOT") { exclude("junit", "junit") }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(8)
        options.compilerArgs.addAll(listOf("-Xmaxerrs", "1000", "-Xlint:-options"))
    }

    processResources {
        val props = mapOf(
            "name" to project.name,
            "version" to project.version.toString(),
            "url" to projectUrl.get(),
        )
        inputs.properties(props)
        filteringCharset = "UTF-8"
        // Only plugin.yml contains placeholders; other resources (e.g. language files) must stay untouched.
        filesMatching("plugin.yml") { expand(props) }
    }

    shadowJar {
        archiveClassifier.set("")
        val prefix = "de.epiceric.shopchest.dependencies"
        relocate("org.bstats", "$prefix.bstats")
        relocate("org.codemc.worldguardwrapper", "$prefix.worldguardwrapper")
        relocate("com.zaxxer.hikari", "$prefix.hikari")
        relocate("org.slf4j", "$prefix.slf4j")
        relocate("org.inventivetalent.reflection", "$prefix.reflectionhelper")
    }

    jar {
        // The shaded jar is the deliverable; make the plain jar distinguishable.
        archiveClassifier.set("slim")
    }

    assemble { dependsOn(shadowJar) }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "ShopChest"
            from(components["java"])
        }
    }
}
