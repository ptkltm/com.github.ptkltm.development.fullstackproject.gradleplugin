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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task


/**
 * Gradle plugin with the id 'com.github.ptkltm.development.fullstackproject.domain' that configures the 'Domain'
 * project being located at the root of the project structure.
 *
 * This plugin defines the tasks 'clean', 'build', 'publishAllPublicationsToMavenRootRepository' and 'wrapper'
 * that depend on the same-named tasks of the sub builds.
 *
 * Additional information can be found at the documentation of the method [apply].
 *
 * @author Patrick Leitermann
 */
class FullStackProjectDomainGradlePlugin : Plugin<Project> {
    /**
     * Singleton-like container for storing static fields, properties and methods
     * reused across all instances of [FullStackProjectDomainGradlePlugin].
     */
    internal
    companion object {
        /**
         * The name of the 'build' task.
         */
        const val BUILD_TASK_NAME = "build"

        /**
         * The name of the 'clean' task.
         */
        const val CLEAN_TASK_NAME = "clean"

        /**
         * The name of the 'mavenRoot' repository.
         */
        const val MAVEN_ROOT_REPOSITORY_NAME = "mavenRoot"

        /**
         * The '.repository' suffix for the name of the folder containing the
         * Maven repository.
         */
        const val REPOSITORY_SUFFIX = ".repository"

        /**
         * The name of the 'publishAllPublicationsToMavenRootRepository' task.
         */
        val PUBLISH_TASK_NAME = "publishAllPublicationsTo${MAVEN_ROOT_REPOSITORY_NAME.capitalize()}Repository"

        /**
         * The name of the 'wrapper' task.
         */
        private
        const val WRAPPER_TASK_NAME = "wrapper"

        /**
         * Searches for a task with a specific name, extends the task with a list of dependent tasks and applies
         * an action.
         * If no task with the name was found a task with the name is created automatically.
         *
         * Finally the name of the task is returned.
         *
         * @receiver The project containing the task and the dependencies.
         * @param [taskName] The name of the task.
         * @param [postAction] The action that's executed at the end of the task.
         * @param [dependentTasks] The tasks the configured task depends on.
         * @return The name of the task. It's the value of the parameter [taskName].
         */
        @Suppress("UNCHECKED_CAST")
        fun Project.defineIncludedTask(
            taskName: String,
            postAction: Task.() -> Unit,
            dependentTasks: List<Any>
        ): String = (tasks.find { it.name == taskName } ?: tasks.create(taskName))
                .dependsOn(dependentTasks)
                .doLast(postAction).name
    }

    /**
     * Configures the 'Domain' root project based on the included sub builds.
     *
     * The following tasks are defined that depend on all same-named tasks in the sub builds:
     * - build
     *      Executes all 'build' tasks of the included sub builds.
     * - clean
     *      Executes all 'clean' tasks of the included sub builds and deletes the build directory of the 'Domain'
     *      project.
     * - publishAllPublicationsToMavenRootRepository
     *      Executes all 'publishAllPublicationsToMavenRootRepository' tasks of the included sub builds
     *      and copies all files in a folder with the name of project followed by the suffix '.repository'
     *      inside the build directory of the 'Domain' project.
     * - wrapper
     *      Executes all 'wrapper' tasks of the included sub builds.
     *      This can be used for generating the Gradle wrappers in all 'Implementation' projects at the same time.
     *
     * The tasks 'clean' and 'build' are declared as default tasks.
     *
     * @param [project] The project being extended with the tasks 'clean', 'build',
     * 'publishAllPublicationsToMavenRootRepository' and 'wrapper'.
     */
    override fun apply(
        project: Project
    ): Unit = project.run {
        /**
         * Declares the 'clean' and 'build' tasks as default tasks.
         */
        defaultTasks(
            /**
             * Defines a 'clean' task that deletes the 'build' folder of the current project and depends on all
             * 'clean' tasks of the included builds.
             */
            defineIncludeBuildTask(
                taskName = CLEAN_TASK_NAME
            ) {
                delete(buildDir)
            },
            /**
             * Defines a 'build' task that depends on all 'build' tasks of the included builds.
             */
            defineIncludeBuildTask(
                taskName = BUILD_TASK_NAME
            ) {}
        )
        /**
         * Defines a 'publishAllPublicationsToMavenRootRepository' task that depends on all
         * 'publishAllPublicationsToMavenRootRepository' tasks of the included builds and copies all their content
         * in a folder with the name of project followed by the suffix '.repository' inside the build directory.
         */
        defineIncludeBuildTask(
            taskName = PUBLISH_TASK_NAME
        ) {
            copy {
                it.from(
                        gradle.includedBuilds.map { includedBuild ->
                            "${includedBuild.projectDir.absolutePath}${File.separatorChar}${Project
                                    .DEFAULT_BUILD_DIR_NAME}${File.separatorChar}${includedBuild
                                    .name}$REPOSITORY_SUFFIX"
                        }.filter { mavenRepositoryPath -> file(mavenRepositoryPath).exists() }
                )
                it.into("${buildDir.absolutePath}${File.separatorChar}${rootProject.name}$REPOSITORY_SUFFIX")
            }
        }
        /**
         * Defines a 'wrapper' task that depends on all 'wrapper' tasks of the included builds.
         */
        defineIncludeBuildTask(
            taskName = WRAPPER_TASK_NAME
        ) {}
    }

    /**
     * Searches for a task with a specific name, extends the task with a dependency to a list of all name-named tasks in
     * the included builds and applies an action.
     * If no task with the name was found a task with the name is created automatically.
     *
     * Finally the name of the task is returned.
     *
     * @receiver The project containing the task and the sub builds.
     * @param [taskName] The name of the task.
     * @param [postAction] The post action that's executed at the end of the task.
     * @return The name of the task. It's the value of the parameter [taskName].
     */
    private
    fun Project.defineIncludeBuildTask(
        taskName: String,
        postAction: Task.() -> Unit
    ) = defineIncludedTask(
            taskName = taskName,
            postAction = postAction,
            dependentTasks = gradle.includedBuilds.map {
                includedBuild -> includedBuild.task("${Project.PATH_SEPARATOR}$taskName") as Any
            }
        )
}
