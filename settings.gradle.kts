/*
 * Copyright Andret Tools System (c) 2025. Copying and modifying allowed only keeping git link reference.
 */

pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
	}
}

dependencyResolutionManagement {
	repositories {
		mavenCentral()
		maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
		maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
	}
}

rootProject.name = "atsSignTeleport"
