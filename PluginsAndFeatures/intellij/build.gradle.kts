import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.model.internal.core.ModelNodes.withType
import org.jetbrains.intellij.IntelliJPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
import java.net.URI

plugins {
    base
    java
    kotlin("jvm") version "1.2.41" apply false
    id("org.jetbrains.intellij") version "0.3.1" apply false
}

// TODO
//import org.apache.tools.ant.filters.*
//        processResources {
//            filesMatching('**/ApplicationInsights.xml') {
//                filter(ReplaceTokens, tokens: ["applicationinsights.key": project.property("applicationinsights.key")])
//            }
//        }

// TODO
//apply plugin: 'checkstyle'
//apply plugin: 'findbugs'
//
//checkstyle {
//    toolVersion = '7.7'
//    configFile = new File('config/checkstyle/checkstyle.xml')
//    showViolations = false
//}
//
//findbugs {
//    toolVersion = "3.0.1"
//    ignoreFailures = true
//}

//TODO
//task cucumberPackJar(type: Jar) {
//    appendix = 'pathing'
//
//    doFirst {
//        manifest {
//            attributes "Class-Path": configurations.cucumberRuntime.files.collect {
//            it.toURL().toString().replaceFirst(/file:\/+/, '/')
//        }.join(' ')
//        }
//    }
//}
//
//task cucumber() {
//    dependsOn assemble, compileTestJava, cucumberPackJar
//    doLast {
//        javaexec {
//            main = "cucumber.api.cli.Main"
//            classpath = files(sourceSets.main.output, sourceSets.test.output, cucumberPackJar.archivePath)
//            args = [
//                '--plugin', 'pretty',
//                '--glue', 'com.microsoft.azure.hdinsight.spark.common',
//                'Test/resources']
//        }
//    }
//}
//
//test.dependsOn cucumber

allprojects {
    apply<KotlinPlatformJvmPlugin>()
    apply<IntelliJPlugin>()

    group = "com.microsoft.azuretools"
    version = "1.0"

    repositories {
        maven { url = file("../../.repository").toURI() } // to snap to the private maven repo on Jenkins if any
        mavenLocal()
        mavenCentral()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks {
        withType<JavaCompile> {
            options.encoding = "UTF-8"
        }

        withType<Test> {
            testLogging {
                showStandardStreams = true
                exceptionFormat = TestExceptionFormat.FULL
            }
        }
    }

    val compile by configurations.getting
    compile.exclude(module = "slf4j-api")

    val testCompile by configurations.getting
    testCompile.exclude(module = "htmlunit")

    // TODO
//    configurations {
//        cucumberRuntime {
//            extendsFrom testRuntime
//        }
//    }

    dependencies {
        compile(kotlin("stdlib"))

        compile(fileTree("../AddLibrary/AzureLibraries/com.microsoft.azuretools.sdk/dependencies") {
            include("applicationinsights-management-1.0.3.jar")
        })
        compile("com.microsoft.sqlserver:mssql-jdbc:6.1.0.jre8")
        compile("commons-io:commons-io:2.5")
        compile("com.microsoft.azure:azure-client-runtime:1.1.1") { isForce = true }
        compile("com.microsoft.azure:azure-client-authentication:1.1.1") { isForce = true }
        compile("com.microsoft.azuretools:azuretools-core:3.5.0") {
            exclude(group = "com.microsoft.azure", module = "azure-client-authentication")
            exclude(group = "com.microsoft.azure", module = "azure-client-runtime")
        }

        compile("com.microsoft.azuretools:azure-explorer-common:3.5.0") {
            exclude(group = "com.microsoft.azure", module = "azure-client-authentication")
            exclude(group = "com.microsoft.azure", module = "azure-client-runtime")
        }
        compile("com.microsoft.azuretools:hdinsight-node-common:3.5.0") {
            exclude(group = "com.microsoft.azure", module = "azure-client-authentication")
            exclude(group = "com.microsoft.azure", module = "azure-client-runtime")
        }
        compile("com.spotify:docker-client:8.6.2")

        testCompile("junit:junit:4.12")
        testCompile("info.cukes:cucumber-junit:1.2.5")
        testCompile("info.cukes:cucumber-java:1.2.5")
        testCompile("org.mockito:mockito-core:2.7.22")
        testCompile("org.assertj:assertj-swing-junit:3.5.0")

        testCompile("com.github.tomakehurst:wiremock:2.8.0")
        testCompile("org.powermock:powermock-module-junit4:1.7.0RC4")
        testCompile("org.powermock:powermock-api-mockito2:1.7.0RC4")
        testCompile("javax.servlet:javax.servlet-api:3.1.0")
    }
}

tasks {
    task<Wrapper>("wrapper") {
        gradleVersion = "4.7"
        distributionType = Wrapper.DistributionType.ALL
    }
}
