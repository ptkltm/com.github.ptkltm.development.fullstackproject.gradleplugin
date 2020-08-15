/*
 * Copyright 2020 Patrick Leitermann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.ptkltm.development.fullstackproject.gradleplugin

import com.github.ptkltm.development.fullstackproject.gradleplugin.FullStackProjectImplementationGradlePluginTest.Companion.createSettingsGradleKts
import com.github.ptkltm.development.fullstackproject.gradleplugin.FullStackProjectImplementationGradlePluginTest.Companion.initializeGradleProject
import com.github.ptkltm.development.fullstackproject.gradleplugin.FullStackProjectImplementationGradlePluginTest.Companion.verifyFullStackProjectImplementationGradleBuild
import com.github.ptkltm.development.fullstackproject.gradleplugin.FullStackProjectImplementationGradlePluginTest.Companion.BUILD_GRADLE_FILE_CONTENT
import com.github.ptkltm.development.fullstackproject.gradleplugin.FullStackProjectImplementationGradlePluginTest.Companion.BUILD_GRADLE_FILE_NAME
import com.github.ptkltm.development.fullstackproject.gradleplugin.FullStackProjectImplementationGradlePluginTest.Companion.BUILD_GRADLE_KTS_FILE_CONTENT
import com.github.ptkltm.development.fullstackproject.gradleplugin.FullStackProjectImplementationGradlePluginTest.Companion.BUILD_GRADLE_KTS_FILE_NAME
import java.nio.file.Path
import kotlin.test.Test
import org.junit.jupiter.api.io.TempDir


/**
 * Tests for the [FullStackProjectDomainGradlePlugin].
 *
 * @author Patrick Leitermann
 */
class FullStackProjectDomainGradlePluginTest {
    /**
     * Tests the execution of the tasks 'build', 'publishAllPublicationsToMavenRootRepository', 'clean' and 'wrapper'
     * inside a fully configured FullStack project with the Gradle plugins
     * 'com.github.ptkltm.development.fullstackproject.domain', 'com.github.ptkltm.development.implementation' and
     * 'com.github.ptkltm.development.recursiveinclude'.
     *
     * During the test a sub project of an 'Implementation' project is created that applies the plugins 'java-library'
     * and 'maven-publish' to the build.gradle.kts file and defines two tasks:
     * 1. 'build' for creating a .jar in the  'build/lib' folder of the project.
     * 2. 'clean' for deleting the 'build' folder.
     *
     * The test checks if the tasks of the sub 'Implementation' project are correctly executed if the same-named tasks
     * are executed at the 'Domain' project-level.
     *
     * For more information about the test logic take a look at the method-level documentation of the
     * method [verifyFullStackProjectImplementationGradleBuild].
     *
     * @param [temporaryFolder] The temporary folder created and deleted by JUnit Jupiter.
     */
    @Test
    fun testFullStackProjectGradleBuildWithKotlinSubProjectSetup(
        @TempDir temporaryFolder: Path
    ) {
        temporaryFolder.verifyFullStackProjectGradleBuild(
            implementationSubProjectGradleName = BUILD_GRADLE_KTS_FILE_NAME,
            implementationSubProjectGradleContent = BUILD_GRADLE_KTS_FILE_CONTENT
        )
    }

    /**
     * Tests the execution of the tasks 'build', 'publishAllPublicationsToMavenRootRepository', 'clean' and 'wrapper'
     * inside a fully configured FullStack project with the Gradle plugins
     * 'com.github.ptkltm.development.fullstackproject.domain', 'com.github.ptkltm.development.implementation' and
     * 'com.github.ptkltm.development.recursiveinclude'.
     *
     * During the test a sub project of an 'Implementation' project is created that applies the plugins 'java-library'
     * and 'maven-publish' to the build.gradle file and defines two tasks:
     * 1. 'build' for creating a .jar in the  'build/lib' folder of the project.
     * 2. 'clean' for deleting the 'build' folder.
     *
     * The test checks if the tasks of the sub 'Implementation' project are correctly executed if the same-named tasks
     * are executed at the 'Domain' project-level.
     *
     * For more information about the test logic take a look at the method-level documentation of the
     * method [verifyFullStackProjectImplementationGradleBuild].
     *
     * @param [temporaryFolder] The temporary folder created and deleted by JUnit Jupiter.
     */
    @Test
    fun testFullStackProjectGradleBuildWithGroovySubProjectSetup(
        @TempDir temporaryFolder: Path
    ) {
        temporaryFolder.verifyFullStackProjectGradleBuild(
            implementationSubProjectGradleName = BUILD_GRADLE_FILE_NAME,
            implementationSubProjectGradleContent = BUILD_GRADLE_FILE_CONTENT
        )
    }

    /**
     * Tests the build of a fully configured FullStack project with the Gradle
     * plugins 'com.github.ptkltm.development.fullstackproject.domain',
     * 'com.github.ptkltm.development.implementation' and 'com.github.ptkltm.development.recursiveinclude'.
     *
     * This methods extends the project structure of the test logic [verifyFullStackProjectImplementationGradleBuild]
     * by additional parent 'Domain' and 'Platform' folders:
     *
     * /com.github.ptkltm.dsl
     *      - Semantic Component Type: Domain
     *      - build.gradle.kts
     *          Applies the 'com.github.ptkltm.development.fullstackproject.domain' Gradle plugin.
     *      - settings.gradle.kts
     *          Applies the 'com.github.ptkltm.development.recursiveinclude' plugin.
     *
     * /com.github.ptkltm.dsl/gradlescript
     *      - Semantic Component Type: Platform
     *      - no non-directory files
     *      - contains the 'KotlinApi' Implementation project of the Platform module 'GradleScript'
     *
     * /com.github.ptkltm.dsl/gradlescript/com.github.ptkltm.dsl.gradlescript.kotlinapi
     *      - Semantic Component Type: Implementation
     *      - build.gradle.kts
     *          Applies the 'com.github.ptkltm.development.fullstackproject.implementation' Gradle plugin.
     *      - settings.gradle.kts
     *          Applies the 'com.github.ptkltm.development.recursiveinclude' plugin.
     *
     * /com.github.ptkltm.dsl/gradlescript/com.github.ptkltm.dsl.gradlescript.kotlinapi/
     * com.github.ptkltm.dsl.gradlescript.kotlinapi.model
     *      - Semantic Component Type: Implementation sub project
     *      - build.gradle(.kts)
     *          Applies the 'java-library' and 'maven-publish' plugins that creates two tasks:
     *              1. 'build' task
     *                  Creates a file named 'com.github.ptkltm.dsl.gradlescript.kotlinapi.model-1.2.3.jar' inside
     *                  the root path './com.github.ptkltm.dsl/gradlescript/com.github.ptkltm.dsl.gradlescript
     *                  .kotlinapi/com.github.ptkltm.dsl.gradlescript.kotlinapi.model/build/libs'
     *              2. 'clean' task
     *                  Deletes the folder
     *                  './com.github.ptkltm.dsl/gradlescript/com.github.ptkltm.dsl.gradlescript.kotlinapi/
     *                  com.github.ptkltm.dsl.gradlescript.kotlinapi.model/build'.
     *
     * For more information about the test logic see [verifyFullStackProjectImplementationGradleBuild].
     *
     * @receiver The path of the parent directory.
     * @param [implementationSubProjectGradleName] 'build.gradle' or 'build.gradle.kts' of the 'Implementation' sub
     * project.
     * @param [implementationSubProjectGradleContent] The content of the build.gradle(.kts) file of the 'Implementation'
     * sub project.
     */
    private
    fun Path.verifyFullStackProjectGradleBuild(
        implementationSubProjectGradleName: String,
        implementationSubProjectGradleContent: String
    ) {
        // Initializes the root 'Domain' project './com.github.ptkltm.dsl'.
        val domainDirectoryPath = initializeGradleProject(
            projectDirectoryName = "com.github.ptkltm.dsl",
            buildGradleFileName = BUILD_GRADLE_KTS_FILE_NAME,
            buildGradleContent = """
plugins {
    id("com.github.ptkltm.development.fullstackproject.domain")
}
            """.trimIndent()
        )

        // Generates a settings.gradle.kts file at the 'Domain' root project.
        domainDirectoryPath.createSettingsGradleKts()

        // /gradlescript
        val platformDirectory = domainDirectoryPath.resolve("gradlescript")

        platformDirectory.verifyFullStackProjectImplementationGradleBuild(
            implementationSubProjectGradleName = implementationSubProjectGradleName,
            implementationSubProjectGradleContent = implementationSubProjectGradleContent,
            alternativeProjectGradleExecutionDirectory = domainDirectoryPath
        )
    }
}
