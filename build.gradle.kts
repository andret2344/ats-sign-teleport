/*
 * Copyright Andret Tools System (c) 2018. Copying and modifying allowed only keeping git link reference.
 */

plugins {
	idea
	java
	jacoco
	`maven-publish`
	id("org.barfuin.gradle.jacocolog") version "3.1.0"
	id("com.github.johnrengelman.shadow") version "8.1.1"
	id("kr.entree.spigradle") version "2.4.3"
}

repositories {
	mavenCentral()
}

dependencies {
	compileOnly(group = "org.spigotmc", name = "spigot-api", version = "${project.properties["spigotVersion"]}-R0.1-SNAPSHOT")
	compileOnly(group = "org.jetbrains", name = "annotations", version = "24.0.1")
	implementation(group = "org.bstats", name = "bstats-bukkit", version = "3.0.2")
	implementation(group = "org.json", name = "json", version = "20230227")

	testImplementation(group = "org.assertj", name = "assertj-core", version = "3.24.2")
	testImplementation(group = "org.mockito", name = "mockito-core", version = "5.3.1")
	testImplementation(group = "org.mockito", name = "mockito-inline", version = "5.2.0")
	testImplementation(group = "org.mockito", name = "mockito-testng", version = "0.5.0")
	testImplementation(group = "org.spigotmc", name = "spigot-api", version = "${project.properties["spigotVersion"]}-R0.1-SNAPSHOT")
	testImplementation(group = "org.testng", name = "testng", version = "7.8.0")
}

tasks {
	compileJava {
		sourceCompatibility = "17"
		targetCompatibility = "17"
		options.compilerArgs.addAll(listOf("-parameters", "-g", "-Xlint:deprecation", "-Xlint:unchecked"))
	}

	test {
		useTestNG()
		finalizedBy(jacocoTestCoverageVerification, jacocoAggregatedReport)
	}

	jacocoTestReport {
		classDirectories.setFrom(classDirectories.files.map {
			fileTree(it).matching {
				exclude("**/*Plugin.*")
			}
		})
	}

	jacocoTestCoverageVerification {
		violationRules {
			rule {
				classDirectories.setFrom(jacocoTestReport.get().classDirectories)
				limit {
					minimum = BigDecimal("1")
				}
			}
		}
	}

	javadoc {
		source = sourceSets["main"].allJava
		classpath = configurations["compileClasspath"]

		options {
			memberLevel = JavadocMemberLevel.PUBLIC
		}
	}

	spigot {
		authors = listOf("Andret")
		apiVersion = "1.17"
		description = "The plugin that allows to create signs that teleports"
		version = project.properties["version"] as String
		website = "https://www.spigotmc.org/resources/ats-andret-tools-system-sign-teleport-place-a-sign-teleporting-clicking-player.104918/"
		libraries = listOf("org.jetbrains:annotations:24.0.1")

		permissions {
			create("ats.signteleport.*") {
				children = mapOf(
						"ats.signteleport.use" to true,
						"ats.signteleport.create" to false
				)
			}
		}
	}

	build {
		dependsOn(shadowJar)
	}

	shadowJar {
		archiveFileName.set("${project.name}-${project.version}.jar")
		relocate("org.bstats", "eu.andret.ats.signteleport.bstats")
		relocate("org.json", "eu.andret.ats.signteleport.json")
	}

	publishing {
		publications {
			create<MavenPublication>("maven") {
				artifact(jar)
				artifact(javadoc)
				groupId = project.properties["group"] as String
				version = project.properties["version"] as String
				artifactId = project.properties["artifact"] as String
			}
		}
		repositories {
			maven {
				name = "GitLab"

				url = uri("https://gitlab.com/api/v4/projects/25194962/packages/maven")
				credentials(HttpHeaderCredentials::class) {
					name = "Job-Token"
					value = System.getenv("CI_JOB_TOKEN")
				}
				authentication {
					create<HttpHeaderAuthentication>("header")
				}
			}
		}
	}
}
