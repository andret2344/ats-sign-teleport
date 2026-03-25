/*
 * Copyright Andret Tools System (c) 2026. Copying and modifying allowed only keeping git link reference.
 */

plugins {
	java
	jacoco
	`maven-publish`
	id("org.barfuin.gradle.jacocolog") version "4.0.1"
	id("com.gradleup.shadow") version "9.4.0"
}

dependencies {
	compileOnly(libs.spigot.api)
	compileOnly(libs.jetbrains.annotations)
	implementation(libs.bstats.bukkit)

	testImplementation(libs.assertj.core)
	testImplementation(libs.mockito.core)
	testImplementation(libs.mockito.junit.jupiter)
	testImplementation(libs.spigot.api)
	testImplementation(libs.junit.jupiter)
	testRuntimeOnly(libs.junit.platform.launcher)
}

tasks {
	compileJava {
		sourceCompatibility = JavaVersion.VERSION_25.toString()
		targetCompatibility = JavaVersion.VERSION_25.toString()
		options.compilerArgs.addAll(listOf("-parameters", "-g", "-Xlint:deprecation", "-Xlint:unchecked"))
	}

	test {
		useJUnitPlatform()
		finalizedBy(jacocoTestCoverageVerification, jacocoTestReport)
	}

	jacocoTestReport {
		reports {
			xml.required.set(true)
			html.required.set(false)
		}
		classDirectories.setFrom(
			sourceSets.main.get().output.classesDirs.asFileTree.matching {
				exclude("**/*Plugin.*")
			}
		)
	}

	jacocoTestCoverageVerification {
		dependsOn(jacocoTestReport)
		classDirectories.setFrom(jacocoTestReport.get().classDirectories)
		violationRules {
			rule {
				limit {
					minimum = "1".toBigDecimal()
				}
			}
		}
	}

	build {
		dependsOn(shadowJar)
	}

	shadowJar {
		archiveFileName.set("${project.name}-${project.version}.jar")
		relocate("org.bstats", "eu.andret.ats.signteleport.bstats")
	}

	publishing {
		publications {
			create<MavenPublication>("maven") {
				artifact(jar)
				groupId = project.properties["group"].toString()
				version = project.properties["version"].toString()
				artifactId = project.properties["artifact"].toString()
			}
		}
		repositories {
			maven {
				name = "GitHubPackages"
				url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: ""}")
				credentials {
					username = System.getenv("GITHUB_ACTOR")
					password = System.getenv("GITHUB_TOKEN")
				}
			}
		}
	}
}
