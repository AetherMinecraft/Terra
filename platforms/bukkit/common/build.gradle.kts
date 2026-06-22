repositories {

}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

dependencies {
    shadedApi(project(":common:implementation:base"))

    compileOnly("io.papermc.paper", "paper-api", Versions.Bukkit.paper)

    compileOnly("org.mvplugins.multiverse.core", "multiverse-core", Versions.Bukkit.multiverse)

    shadedApi("io.papermc", "paperlib", Versions.Bukkit.paperLib)

    shadedApi("com.google.guava", "guava", Versions.Libraries.Internal.guava)

    shadedApi("org.incendo", "cloud-paper", Versions.Bukkit.cloud)
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release.set(25)
    }
}
