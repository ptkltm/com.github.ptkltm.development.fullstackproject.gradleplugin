# FullStack Project Plugins

Gradle plugins for the automated management of a FullStack project based on convention over configuration and a
standardized project structure.

## Implementation description

FullStack application projects are compositions of multiple sub component projects that may be based on different
technologies and programming languages.

An example would be an e-commerce platform that's a product of the following five components:
- Backend written in Go
- Frontend written in Angular
- Database based on SQL
- Search Engine based on ElasticSearch
- AI-based algorithm for product recommendation based on TensorFlow and Python

Each of these elements uses another technology or programming language.
In many cases each of this projects has it's own project structure conventions and is stored independently of the other
component projects. Besides that the type of the source control management might by different.

The disadvantage of this strategy is that if a developer wants to verify the current state of the application, multiple 
sub projects need to be checked out and build independently from each other and published to a central repository 
(e. g. mavenLocal()) which might affect productivity.
Breaking changes in specific components may also occur because the components do not know about the current development
state of each other. 

The aim of this repository is the definition on a clean predictable project structure for bundling the component
projects of complex full stack projects.
Gradle, the two Gradle plugins provided by the repository
'[com.github.ptkltm.development/com.github.ptkltm.development.fullstackproject.gradleplugin](https://github.com/ptkltm/com.github.ptkltm.development.fullstackproject.gradleplugin)' and the Recursive Include Plugin provided by the
repository '[com.github.ptkltm.development/com.github.ptkltm.development.recursiveinclude.gradleplugin](https://github.com/ptkltm/com.github.ptkltm.development.recursiveinclude.gradleplugin)' will be the clue for connecting the component
projects together.

Each project is based on the following semantic components:

|Semantic Component|Description|Example for naming convention (always case-sensitive)|Example for relative location (always based on lower-case)|
|---|---|---|---|
|Domain|A Domain may encapsulate multiple FullStack projects (Platforms).|Com.Github.Ptkltm.Development|/**com.github.ptkltm.development**|
|Platform|Each Platform represents a FullStack project and is able to contain multiple 'Implementation' projects.|RecursiveInclude|/com.github.ptkltm.development/**recursiveinclude**|
|Implementation|The technology the domain-specific model/algorithm is implemented with.|GradlePlugin|/com.github.ptkltm.development/recursiveinclude/**com.github.ptkltm.dsl.recursiveinclude.gradleplugin**|
|Implementation sub projects|Sub projects of the 'Implementation' project.| |/com.github.ptkltm.development/recursiveinclude/com.github.ptkltm.dsl.recursiveinclude.gradleplugin/**com.github.ptkltm.dsl.recursiveinclude.gradleplugin.model**|

Based on this information all FullStack projects make use of the following three Gradle plugins:

- **com.github.ptkltm.development.fullstackproject.domain**

     The plugin 'com.github.ptkltm.development.fullstackproject.domain' has to be applied to the build.gradle(.kts) file
     of the 'Domain' project at the root-level of the project structure.
     It's responsible for defining four tasks - 'clean', 'build', 'publishAllPublicationsToMavenRootRepository' and
     'wrapper' that depend on the same-named tasks in all sub builds ('Implementation' projects).
    
    The 'build' and 'wrapper' tasks have no additional logic than executing all dependent tasks but 'clean' and 
    'publishAllPublicationsToMavenRootRepository' perform additional logic:
    
    - '**clean**' deletes the build directory of the 'Domain' project.
    - '**publishAllPublicationsToMavenRootRepository**' copies the artifacts of all 'Implementation' Maven repositories
    in an additional directory inside the build directory of the 'Domain' project with the name of the domain project
    followed by the '.repository' suffix.
    
        This task is defined for getting a temporary project-specific Maven repository without being coupled to
        the Maven local repository that's used across the whole system.
        
        If other projects outside the FullStack project want to use the latest development state they'll be able
        to define a Maven repository with a relative path to the repository inside the build directory of the 'Domain'
        project and get a clean state of the latest build artifacts.

- **com.github.ptkltm.development.fullstackproject.implementation**

    'com.github.ptkltm.development.fullstackproject.implementation' has to be applied to the build.gradle(.kts) file
    of all 'Implementation' projects. It sets the group name of the 'Implementation' project and all it's sub projects
    to the project name and defines the tasks 'clean', 'build' and  'publishAllPublicationsToMavenRootRepository' being
    dependent on the same-named tasks in the sub projects.
    
    After the evaluation of the sub projects each sub project with no explicitly set version will get the version of the
    root project automatically.
    
    Apart from that all projects containing the 'maven-publish' Gradle plugin will be extended by a Maven publication
    with the name 'mavenRoot'.
    
    The 'build' task only executes the 'build' tasks of all sub projects but 'clean' and 
    'publishAllPublicationsToMavenRootRepository' provide additional logic:
    
    - '**clean**' deletes the build directory of the 'Implementation' root project.
    - '**publishAllPublicationsToMavenRootRepository**' copies the artifacts of the Maven publications of all
    sub projects of an 'Implementation' in an additional directory inside the build directory of the 'Implementation'
    root project with the name of the root 'Implementation' project followed by the '.repository' suffix.

- **com.github.ptkltm.development.recursiveinclude**

    The Recursive Include Plugin has the ability to include sub builds and sub projects.

    It's located at another repository because it can be used independently of the other plugins.
In the context of FullStack projects it's applied at the settings.gradle(.kts) files of the 'Domain' and all
'Implementation' projects - at the 'Domain' project-level it includes all ('Implementation') sub builds via [Gradle's
composite build feature](https://docs.gradle.org/current/userguide/composite_builds.html) and at the 'Implementation' level it automatically includes all sub projects of the
'Implementation' project it's applied to.

## Current Version

[ ![Download](https://api.bintray.com/packages/ptkltm/com.github.ptkltm.development/com.github.ptkltm.development.fullstackproject.gradleplugin/images/download.svg) ](https://bintray.com/ptkltm/com.github.ptkltm.development/com.github.ptkltm.development.fullstackproject.gradleplugin/_latestVersion)

## Usage

The plugins with the ids 'com.github.ptkltm.development.fullstackproject.domain' and
'com.github.ptkltm.development.fullstackproject.implementation' are hosted at 
[Bintray](https://bintray.com/ptkltm/com.github.ptkltm.development/com.github.ptkltm.development.fullstackproject.gradleplugin) 
and can be applied via the following configurations to either a **settings.gradle.kts** or a **settings.gradle** file.

- Configuration of the **build.gradle(.kts)** file in 'Domain' type projects:

    - **build.gradle.kts** file (if the Kotlin syntax is used)

    ```kotlin
    buildscript {
        repositories {
            jcenter()
        }
        dependencies {
            classpath(
                group = "com.github.ptkltm.development.fullstackproject.gradleplugin",
                name = "com.github.ptkltm.development.fullstackproject.gradleplugin",
                version = "0.1.0"
            )
        }
    }
    
    apply(plugin = "com.github.ptkltm.development.fullstackproject.domain")
    ```

    - **build.gradle** file (if the Groovy syntax is used)

    ```groovy
    buildscript {
        repositories {
            jcenter()
        }
        dependencies {
            classpath 'com.github.ptkltm.development.fullstackproject.gradleplugin:com.github.ptkltm.development.fullstackproject.gradleplugin:0.1.0'
        }
    }
    
    apply plugin: 'com.github.ptkltm.development.fullstackproject.domain'
    ```

- Configuration of the **build.gradle(.kts)** file in all 'Implementation' type projects:

    - **build.gradle.kts** file (if the Kotlin syntax is used)

    ```kotlin
    buildscript {
        repositories {
            jcenter()
        }
        dependencies {
            classpath(
                group = "com.github.ptkltm.development.fullstackproject.gradleplugin",
                name = "com.github.ptkltm.development.fullstackproject.gradleplugin",
                version = "0.1.0"
            )
        }
    }
    
    apply(plugin = "com.github.ptkltm.development.fullstackproject.implementation")
    ```

    - **build.gradle** file (if the Groovy syntax is used)

    ```groovy
    buildscript {
        repositories {
            jcenter()
        }
        dependencies {
            classpath 'com.github.ptkltm.development.fullstackproject.gradleplugin:com.github.ptkltm.development.fullstackproject.gradleplugin:0.1.0'
        }
    }
    
    apply plugin: 'com.github.ptkltm.development.fullstackproject.implementation'
    ```
  
- Configuration of the **settings.gradle(.kts)** file in all 'Domain' and 'Implementation' type projects:

    - **settings.gradle.kts** file (if the Kotlin syntax is used)

    ```kotlin
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
    
    apply(plugin = "com.github.ptkltm.development.recursiveinclude")
    ```

    - **settings.gradle** file (if the Groovy syntax is used)

    ```groovy
    buildscript {
        repositories {
            jcenter()
        }
        dependencies {
            classpath 'com.github.ptkltm.development.recursiveinclude.gradleplugin:com.github.ptkltm.development.recursiveinclude.gradleplugin:0.4.0'
        }
    }
    
    apply plugin: 'com.github.ptkltm.development.recursiveinclude'
    ```

## Building the source code

- `git clone https://github.com/ptkltm/com.github.ptkltm.development.fullstackproject.gradleplugin.git`
- `cd com.github.ptkltm.development.fullstackproject.gradleplugin`
- `./gradlew` (on Linux or macOS) / `gradlew.bat` (on Windows)

## License information

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.