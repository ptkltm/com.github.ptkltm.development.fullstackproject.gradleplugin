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

import com.github.ptkltm.development.fullstackproject.gradleplugin.FullStackProjectDomainGradlePlugin.Companion.defineIncludedTask
import com.github.ptkltm.development.fullstackproject.gradleplugin.FullStackProjectDomainGradlePlugin.Companion.BUILD_TASK_NAME
import com.github.ptkltm.development.fullstackproject.gradleplugin.FullStackProjectDomainGradlePlugin.Companion.CLEAN_TASK_NAME
import com.github.ptkltm.development.fullstackproject.gradleplugin.FullStackProjectDomainGradlePlugin.Companion.MAVEN_ROOT_REPOSITORY_NAME
import com.github.ptkltm.development.fullstackproject.gradleplugin.FullStackProjectDomainGradlePlugin.Companion.PUBLISH_TASK_NAME
import com.github.ptkltm.development.fullstackproject.gradleplugin.FullStackProjectDomainGradlePlugin.Companion.REPOSITORY_SUFFIX
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.PublishingExtension
import java.io.File


/**
 * Gradle plugin with the id 'com.github.ptkltm.development.fullstackproject.implementation' that configures the
 * 'Implementation' project being located in a platform-specific folder inside the 'Domain' project.
 *
 * This plugin defines the tasks 'clean', 'build', 'publishAllPublicationsToMavenRootRepository' that depend on the
 * same-named tasks of the sub projects of the 'Implementation' project.
 *
 * Additional information can be found in the documentation of the method [apply].
 *
 * @author Patrick Leitermann
 */
class FullStackProjectImplementationGradlePlugin : Plugin<Project> {

    /**
     * Configures the 'Implementation' root project based on the included sub projects.
     *
     * Sets the group of the root project and all it's sub projects to the name of the root project and the version of
     * all sub projects to the version of the root project is no sub project version is set explicitly.
     *
     * Apart from that if one of the projects contains the 'maven-publish' plugin, a new Maven publication called
     * 'mavenRoot' is added by default with a repository url to a folder with the name of the 'Implementation' project
     * followed by '.repository' inside the build directory of the root project.
     *
     * The following tasks are defined that depend on all same-named tasks in the sub projects:
     * - build
     *      Executes all 'build' tasks of the included sub projects.
     * - clean
     *      Executes all 'clean' tasks of the included sub projects and deletes the build directory of the
     *      'Implementation' project.
     * - publishAllPublicationsToMavenRootRepository
     *      Executes all 'publishAllPublicationsToMavenRootRepository' tasks of the included sub projects
     *      and copies all files in a folder with the name of the project followed by the suffix '.repository'
     *      inside the build directory of the root 'Implementation' project.
     *
     * The tasks 'clean' and 'build' are declared as default tasks.
     *
     * @param [project] The project being extended with the tasks 'clean', 'build' and
     * 'publishAllPublicationsToMavenRootRepository'.
     */
    override fun apply(
        project: Project
    ) = project.run {
        /**
         * Sets the name of the group to the name of the root project.
         */
        group = project.rootProject.name

        /**
         * Contains the configuration of all included sub projects.
         */
        subprojects {
            /**
             * Executed after the current sub project is evaluated.
             */
            it.afterEvaluate { evaluatedSubProject ->
                /**
                 * Sets the group of the evaluated sub project to the group of the root project.
                 */
                evaluatedSubProject.group = group

                /**
                 * If no version was explicitly set in the sub project the project version of the 'Implementation'
                 * project is used by default.
                 */
                if (Project.DEFAULT_VERSION == evaluatedSubProject.version) {
                    evaluatedSubProject.version = version
                }
            }
        }

        /**
         * Name of the maven repository folder being created inside the build folder of the root project.
         * The name is based on the name of the root project followed by the '.repository' suffix.
         */
        val mavenRootRepositoryFolderName = "${rootProject.name}$REPOSITORY_SUFFIX"

        /**
         * The path to the maven repository in the build directory of the root project.
         */
        val mavenRootRepositoryUrl = uri("${rootProject.buildDir}/$mavenRootRepositoryFolderName")

        /**
         * Closure for the configuration of the root project and all it's sub projects.
         */
        allprojects {
            /**
             * After the evaluation of a project that contains the 'maven-publish' plugin, the 'publishing' extension
             * is extended by a maven repository with the name 'mavenRoot' and a repository url to a folder with the
             * name of the 'Implementation' root project followed by the suffix '.repository' inside the build
             * directory of the root project.
             */
            it.afterEvaluate { targetProject ->
                if (targetProject.plugins.hasPlugin("maven-publish")) {
                    targetProject.extensions.configure(
                        PublishingExtension.NAME,
                        Action<PublishingExtension> { publishingExtension ->
                            publishingExtension.repositories.maven { maven ->
                                maven.apply {
                                    name = MAVEN_ROOT_REPOSITORY_NAME
                                    url = mavenRootRepositoryUrl
                                }
                            }
                        }
                    )
                }
            }
        }

        /**
         * Closure containing the configuration being executed after the current root project was evaluated.
         */
        afterEvaluate { evaluatedProject ->
            /**
             * Declares the 'clean' and 'build' tasks as default tasks.
             */
            evaluatedProject.defaultTasks(
                /**
                 * Defines a 'clean' task that deletes the 'build' folder of the current project and depends on all
                 * 'clean' tasks of the included sub projects.
                 */
                evaluatedProject.defineIncludedSubProjectTask(
                    taskName = CLEAN_TASK_NAME,
                    postAction = Action {
                        evaluatedProject.delete(evaluatedProject.buildDir)
                    }
                ),
                /**
                 * Defines a 'build' task that depends on all 'build' tasks of the included sub projects.
                 */
                evaluatedProject.defineIncludedSubProjectTask(
                    taskName = BUILD_TASK_NAME,
                    postAction = Action { }
                )
            )
            /**
             * Defines a 'publishAllPublicationsToMavenRootRepository' task that depends on all
             * 'publishAllPublicationsToMavenRootRepository' tasks of the included sub projects and copies all their
             * content in a folder with the name of project followed by the suffix '.repository' inside the build
             * directory.
             */
            evaluatedProject.defineIncludedSubProjectTask(
                taskName = PUBLISH_TASK_NAME,
                postAction = Action {
                    copy {
                        it.from(
                            subprojects.map {
                                subProject -> "${subProject.buildDir.absolutePath}${File
                                    .separatorChar}${subProject.name}$REPOSITORY_SUFFIX"
                            }.filter { mavenRepositoryPath -> file(mavenRepositoryPath).exists() }
                        )
                        it.into("${buildDir.absolutePath}${File.separatorChar}$mavenRootRepositoryFolderName")
                    }
                }
            )
        }
    }

    /**
     * Searches for a task with a specific name, extends the task with a dependency to a list of all name-named tasks in
     * the included sub projects and applies a post action.
     * If no task with the name was found a task with the name is created automatically.
     *
     * Finally the name of the task is returned.
     *
     * @receiver The project containing the task and the sub projects.
     * @param [taskName] The name of the task.
     * @param [postAction] The action that's executed at the end of the task.
     * @param [T] The type of the task.
     * @return The name of the task. It's the value of the parameter [taskName].
     */
    private
    fun <T : Task> Project.defineIncludedSubProjectTask(
        taskName: String,
        postAction: Action<T>
    ) = defineIncludedTask(
            taskName = taskName,
            postAction = postAction,
            dependentTasks = subprojects.map {
                subProject -> "${subProject.path}${Project.PATH_SEPARATOR}$taskName"
            }
        )
}
