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

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir


/**
 * Tests for the [FullStackProjectImplementationGradlePlugin].
 *
 * @author Patrick Leitermann
 */
class FulStackProjectImplementationGradlePluginTest {
    /**
     * Singleton-like container for storing static fields, properties and methods
     * reused across all instances of [FulStackProjectImplementationGradlePluginTest].
     */
    internal
    companion object {
        /**
         * The name of a Gradle build script with the Groovy syntax (build.gradle).
         */
        const val BUILD_GRADLE_FILE_NAME = Project.DEFAULT_BUILD_FILE

        /**
         * The name of a Gradle build script with the Kotlin syntax (build.gradle.kts).
         */
        const val BUILD_GRADLE_KTS_FILE_NAME = "$BUILD_GRADLE_FILE_NAME.kts"

        /**
         * The content of the build.gradle file.
         */
        val BUILD_GRADLE_KTS_FILE_CONTENT = """
plugins {
    `java-library`
    `maven-publish`
}

publishing {
    publications {
        create("mavenJava", MavenPublication::class) {
            from(components["java"])
        }
    }
}
        """.trimIndent()

        /**
         * The content of the build.gradle.kts file.
         */
        val BUILD_GRADLE_FILE_CONTENT = """
plugins {
    id 'java-library'
    id 'maven-publish'
}

publishing {
    publications {
        maven(MavenPublication) {
            from components.java
        }
    }
}
        """.trimIndent()

        /**
         * The name of the Gradle settings file with the Kotlin syntax (settings.gradle.kts).
         */
        private
        const val SETTINGS_GRADLE_KTS_FILE_NAME = "${Settings.DEFAULT_SETTINGS_FILE}.kts"

        /**
         * The name of the 'build' task.
         */
        private
        const val BUILD_TASK_NAME = "build"

        /**
         * The name of the 'clean' task.
         */
        private
        const val CLEAN_TASK_NAME = "clean"

        /**
         * The name of the 'publishAllPublicationsToMavenRootRepository' task.
         */
        private
        const val PUBLISH_TASK_NAME = "publishAllPublicationsToMavenRootRepository"

        /**
         * The name of the 'wrapper' task.
         */
        private
        const val WRAPPER_TASK_NAME = "wrapper"

        /**
         * The version of the project.
         */
        private
        const val PROJECT_VERSION = "1.2.3"

        /**
         * The content of the settings.gradle.kts file.
         */
        private
        val SETTINGS_GRADLE_KTS_FILE_CONTENT = {
                val pluginPrefix = "com.github.ptkltm.development.fullstackproject"
                """
pluginManagement { 
    repositories {
        flatDir {
            dirs = setOf(file("${File("").absoluteFile}/${'$'}{Project.DEFAULT_BUILD_DIR_NAME}/libs"))
        }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith(prefix = "$pluginPrefix")) {
                useModule(
                    "$pluginPrefix.gradleplugin:" +
                    "$pluginPrefix.gradleplugin:" +
                    "0.1.0"
                )
            }
        }
    }
}
buildscript {
    repositories { 
        jcenter()
    }
    dependencies { 
        classpath(
            group = "com.github.ptkltm.development.recursiveinclude.gradleplugin", 
            name = "com.github.ptkltm.development.recursiveinclude.gradleplugin",
            version = "0.4.0"
        )
    }
}

apply {
    plugin("com.github.ptkltm.development.recursiveinclude")
}
        """.trimIndent().toByteArray()
        }()

        /**
         * Reusable generic method for the test of the logic of an 'Implementation' project with the Gradle
         * plugins 'com.github.ptkltm.development.implementation' and 'com.github.ptkltm.development.recursiveinclude'.
         *
         * The test code is based on multiple steps:
         *
         * 1. Generates a temporary Gradle project based on the following structure:
         *
         * /com.github.ptkltm.dsl.gradlescript.kotlinapi
         *      - Semantic Component Type: Implementation
         *      - build.gradle.kts
         *          Applies the 'com.github.ptkltm.development.fullstackproject.implementation' Gradle plugin.
         *      - settings.gradle.kts
         *          Applies the 'com.github.ptkltm.development.recursiveinclude' plugin.
         *
         * /com.github.ptkltm.dsl.gradlescript.kotlinapi/com.github.ptkltm.dsl.gradlescript.kotlinapi.model
         *      - Semantic Component Type: Implementation sub project
         *      - build.gradle(.kts)
         *          Applies the 'java-library' and 'maven-publish' plugins that creates two tasks:
         *              1. 'build' task
         *                  Creates a file named 'com.github.ptkltm.dsl.gradlescript.kotlinapi.model-1.2.3.jar' inside
         *                  the root path './com.github.ptkltm.dsl.gradlescript.kotlinapi/com.github.ptkltm.dsl.´
         *                  gradlescript.kotlinapi.model/build/libs'
         *              2. 'clean' task
         *                  Deletes the folder
         *                  './com.github.ptkltm.dsl.gradlescript.kotlinapi/com.github.ptkltm.dsl.gradlescript
         *                  .kotlinapi.model/build'.
         *
         *
         * 2. Executes the 'build' task at the root directory.
         *
         * 3. Checks that the execution of the 'build' task was successful.
         *
         * 4. Verifies that a file 'com.github.ptkltm.dsl.gradlescript.kotlinapi.model-1.2.3.jar' was generated in the
         * root path './com.github.ptkltm.dsl.gradlescript.kotlinapi/com.github.ptkltm.dsl.gradlescript.kotlinapi
         * .model/build/lib'.
         *
         * 5. Executes the task 'publishAllPublicationsToMavenRootRepository'.
         *
         * 6. Tests that the execution of the 'publishAllPublicationsToMavenRootRepository' task was successful.
         *
         * 7. Checks that a file called 'com.github.ptkltm.dsl.gradlescript.kotlinapi.model-1.2.3.jar' is now
         * contained in the temporary repository of the root project.
         *
         * 8. Executes the 'clean' task at the root directory.
         *
         * 9. Verify that the execution of the 'clean' task was successful.
         *
         * 10. Verifies that the directory './com.github.ptkltm.dsl.gradlescript.kotlinapi/
         * com.github.ptkltm.dsl.gradlescript.kotlinapi.model/build' was deleted.
         *
         * 11. Executes the 'wrapper' task at the root directory.
         *
         * 12. Verify that the execution of the 'wrapper' task was successful.
         *
         * 13. Checks that the files 'gradle-wrapper.jar' and 'gradle-wrapper.properties' are generated in the
         * folder 'gradle/wrapper'.
         *
         * @receiver The path of the parent directory.
         * @param [implementationSubProjectGradleName] 'build.gradle' or 'build.gradle.kts' of the 'Implementation'
         * sub project.
         * @param [implementationSubProjectGradleContent] The content of the build.gradle(.kts) file of the
         * 'Implementation' sub project.
         * @param [alternativeProjectGradleExecutionDirectory] Alternative Gradle execution path. By default Gradle is
         * executed at the root path of the 'Implementation' project.
         * @sample [alternativeProjectGradleExecutionDirectory] An example for an alternative Gradle execution directory
         * would be the path to the root of the 'Domain' project.
         */
        fun Path.verifyFullStackProjectImplementationGradleBuild(
            implementationSubProjectGradleName: String,
            implementationSubProjectGradleContent: String,
            alternativeProjectGradleExecutionDirectory: Path? = null
        ) {
            val implementationRootProjectName = "com.github.ptkltm.dsl.gradlescript.kotlinapi"

            // 1. Generates the temporary file structure.

            // Initializes an 'Implementation' project
            // './com.github.ptkltm.dsl.gradlescript.kotlinapi'.
            val implementationRootProjectDirectoryPath = initializeGradleProject(
                projectDirectoryName = implementationRootProjectName,
                buildGradleFileName = BUILD_GRADLE_KTS_FILE_NAME,
                buildGradleContent = """
plugins {
    id("com.github.ptkltm.development.fullstackproject.implementation")
}

version = "$PROJECT_VERSION"
                """.trimIndent()
            )

            implementationRootProjectDirectoryPath.createSettingsGradleKts()

            val implementationSubProjectName = "$implementationRootProjectName.model"

            // Initializes an 'Implementation' sub project
            // './com.github.ptkltm.dsl.gradlescript.kotlinapi/com.github.ptkltm.dsl.gradlescript.kotlinapi.model'.
            val implementationSubProjectDirectoryPath = implementationRootProjectDirectoryPath.initializeGradleProject(
                projectDirectoryName = implementationSubProjectName,
                buildGradleFileName = implementationSubProjectGradleName,
                buildGradleContent = implementationSubProjectGradleContent
            )

            val gradleExecutionDirectoryPath = alternativeProjectGradleExecutionDirectory
                    ?: implementationRootProjectDirectoryPath

            // Initialize the GradleRunner with the root project's directory
            // './com.github.ptkltm.dsl.gradlescript.kotlinapi' or an alternative path.
            val gradleRunner = GradleRunner.create().withProjectDir(
                gradleExecutionDirectoryPath.toFile()
            )

            // 2. Executes the 'build' task at the root directory.
            val buildResult = gradleRunner.withArguments(BUILD_TASK_NAME).build()

            // 3. Checks that the execution of the 'build' task was successful.
            assertEquals(
                expected = TaskOutcome.SUCCESS,
                actual = buildResult.task("${Project.PATH_SEPARATOR}$BUILD_TASK_NAME")?.outcome,
                message = "The execution of the '$BUILD_TASK_NAME' task was not successful."
            )

            val generatedJarName = "$implementationSubProjectName-$PROJECT_VERSION.jar"

            // 4. Verifies that a file 'com.github.ptkltm.dsl.gradlescript.kotlinapi.model-1.2.3.jar' was generated in
            // the root path './com.github.ptkltm.dsl.gradlescript.kotlinapi/
            // com.github.ptkltm.dsl.gradlescript.kotlinapi.model/build/lib'.
            "libs${File.separatorChar}$generatedJarName".let {
                assertTrue(
                    actual = File("${implementationSubProjectDirectoryPath.toAbsolutePath()}${File
                            .separatorChar}${Project.DEFAULT_BUILD_DIR_NAME}${File.separatorChar}$it").exists(),
                    message = "The file '$it' doesn't exist."
                )
            }

            // 5. Executes the task 'publishAllPublicationsToMavenRootRepository'.
            val publishResult = gradleRunner.withArguments(PUBLISH_TASK_NAME).build()

            // 6. Tests that the execution of the 'publishAllPublicationsToMavenRootRepository' task was successful.
            assertEquals(
                expected = TaskOutcome.SUCCESS,
                actual = publishResult.task("${Project.PATH_SEPARATOR}$PUBLISH_TASK_NAME")?.outcome,
                message = "The execution of the '$PUBLISH_TASK_NAME' task was not successful."
            )

            // 7. Checks that a file called 'com.github.ptkltm.dsl.gradlescript.kotlinapi.model-1.2.3.jar' is now
            // contained in the temporary repository of the root project.
            "${gradleExecutionDirectoryPath.fileName}.repository${File.separatorChar}com${File
                    .separatorChar}github${File.separatorChar}ptkltm${File.separatorChar}dsl${File
                    .separatorChar}gradlescript${File.separatorChar}kotlinapi${File
                    .separatorChar}$implementationSubProjectName${File.separatorChar}$PROJECT_VERSION${File
                    .separatorChar}$generatedJarName".let {
                assertTrue(
                    actual = File("${gradleExecutionDirectoryPath.toAbsolutePath()}${File
                        .separatorChar}${Project.DEFAULT_BUILD_DIR_NAME}${File
                        .separatorChar}$it").exists(),
                    message = "The file '$it' doesn't exist."
                )
            }

            // 8. Executes the 'clean' task at the root directory.
            val cleanResult = gradleRunner.withArguments(CLEAN_TASK_NAME).build()

            // 9. Verify that the execution of the 'clean' task was successful.
            assertEquals(
                expected = TaskOutcome.SUCCESS,
                actual = cleanResult.task("${Project.PATH_SEPARATOR}$CLEAN_TASK_NAME")?.outcome,
                message = "The execution of the '$CLEAN_TASK_NAME' task was not successful."
            )

            // 10. Verifies that the directory './com.github.ptkltm.dsl.gradlescript.kotlinapi/
            // com.github.ptkltm.dsl.gradlescript.kotlinapi.model/build' was deleted.
            assertFalse(
                actual = File("${implementationSubProjectDirectoryPath.toAbsolutePath()}${File
                        .separatorChar}${Project.DEFAULT_BUILD_DIR_NAME}").exists(),
                message = "The directory '${Project.DEFAULT_BUILD_DIR_NAME}' was not deleted."
            )

            // 11. Executes the 'wrapper' task at the root directory.
            val wrapperResult = gradleRunner.withArguments(WRAPPER_TASK_NAME).build()

            // 12. Verify that the execution of the 'wrapper' task was successful.
            assertEquals(
                expected = TaskOutcome.SUCCESS,
                actual = wrapperResult.task("${Project.PATH_SEPARATOR}$WRAPPER_TASK_NAME")?.outcome,
                message = "The execution of the '$WRAPPER_TASK_NAME' task was not successful."
            )

            // 13. Checks that the files 'gradle-wrapper.jar' and 'gradle-wrapper.properties' were generated in the
            // folder 'gradle/wrapper'.
            listOf("gradle-wrapper.jar", "gradle-wrapper.properties").forEach {
                assertTrue(
                    actual = File("${gradleExecutionDirectoryPath.toAbsolutePath()}${File
                            .separatorChar}gradle${File.separatorChar}wrapper${File.separatorChar}$it").exists(),
                    message = "The file '$it' doesn't exist in the folder 'gradle${File.separatorChar}wrapper'."
                )
            }
        }

        /**
         * Initializes a Gradle project with a settings.gradle(.kts) and a build.gradle(.kts) file.
         *
         * @receiver The path of the parent directory.
         * @param [projectDirectoryName] The name of the project directory and the project.
         * @param [buildGradleFileName] build.gradle (for Groovy syntax) or settings.gradle.kts (for Kotlin syntax).
         * @param [buildGradleContent] The content of the build.gradle(.kts) file.
         * @return The path to the initialized project.
         */
        fun Path.initializeGradleProject(
            projectDirectoryName: String,
            buildGradleFileName: String,
            buildGradleContent: String
        ): Path {
            val projectPath = resolve(projectDirectoryName)
            Files.createDirectories(projectPath)

            Files.write(
                projectPath.resolve(buildGradleFileName),
                buildGradleContent.toByteArray()
            )

            return projectPath
        }

        /**
         * Creates a settings.gradle.kts file.
         *
         * @receiver The path of the project containing the settings.gradle.kts file.
         */
        fun Path.createSettingsGradleKts() {
            Files.write(
                resolve(
                    SETTINGS_GRADLE_KTS_FILE_NAME
                ),
                SETTINGS_GRADLE_KTS_FILE_CONTENT
            )
        }
    }

    /**
     * Tests the execution of the tasks 'clean', 'build' and 'publishAllPublicationsToMavenRootRepository' inside a
     * single 'Implementation' project with the Gradle plugins 'com.github.ptkltm.development.implementation' and
     * 'com.github.ptkltm.development.recursiveinclude'.
     *
     * During the test a sub project of an 'Implementation' project is created that applies the plugins 'java-library'
     * and 'maven-publish' to the build.gradle.kts file and defines two tasks:
     * 1. 'build' for creating a .jar in the  'build/lib' folder of the project.
     * 2. 'clean' for deleting the 'build' folder.
     *
     * The test checks if the tasks of the 'Implementation' sub project are correctly executed if the same-named task
     * is executed at the 'Implementation' project-level.
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
        temporaryFolder.verifyFullStackProjectImplementationGradleBuild(
            implementationSubProjectGradleName = BUILD_GRADLE_KTS_FILE_NAME,
            implementationSubProjectGradleContent = BUILD_GRADLE_KTS_FILE_CONTENT
        )
    }

    /**
     * Tests the execution of the tasks 'clean', 'build' and 'publishAllPublicationsToMavenRootRepository' inside a
     * single 'Implementation' project with the Gradle plugins 'com.github.ptkltm.development.implementation' and
     * 'com.github.ptkltm.development.recursiveinclude'.
     *
     * During the test a sub project of an 'Implementation' project is created that applies the plugins 'java-library'
     * and 'maven-publish' to the build.gradle file and defines two tasks:
     * 1. 'build' for creating a .jar in the  'build/lib' folder of the project.
     * 2. 'clean' for deleting the 'build' folder.
     *
     * The test checks if the tasks of the sub 'Implementation' project are correctly executed if the same-named task
     * is executed at the 'Implementation' project-level.
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
        temporaryFolder.verifyFullStackProjectImplementationGradleBuild(
            implementationSubProjectGradleName = BUILD_GRADLE_FILE_NAME,
            implementationSubProjectGradleContent = BUILD_GRADLE_FILE_CONTENT
        )
    }
}