# AstronKt

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.rand0m-cloud.astronkt/core)

AstronKt is a Kotlin framework for working with the [Astron](https://github.com/Astron/Astron) project, a server
architecture inspired by the in-house technology Disney developed for their MMOs. AstronKt simplifies integrating
Astron’s server communication into your Kotlin projects.

**Note:** AstronKt is not ready for production use.

## Project Structure

AstronKt is divided into four main components:

1. **core**  
   Handles the core logic, including processing network messages and propagating updates between the server and clients.

2. **dclassmacro**  
   A tool for parsing dclass file specifications, generating Kotlin glue code to seamlessly interact with the core
   library. It is designed to evolve into a general-purpose dclass file parser, capable of outputting formats like
   JSON and generated Kotlin glue code.

3. **plugin**  
   A Gradle plugin that adds a build step for automatically generating glue code based on dclass files, making
   integration into your project simple.

4. **explorer** *(Not ready for public use)*  
   An advanced tool designed to communicate with an Astron cluster at the wire level. This component is still in
   development and not ready for public use.

## Usage

To use AstronKt in your project, include the following dependency from Maven Central:

```kotlin
implementation("io.github.rand0m-cloud.astronkt:core:<current_version>")
```

**Note:** AstronKt requires a coroutine main dispatcher, such as `kotlin-coroutine-swing`.

## Plugin Usage

To use the AstronKt Gradle plugin, add the following to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.rand0m-cloud.astronkt.plugin") version "<current_version>"
}

dClassPluginConfig {
    files = listOf("game.dc")
}
```

## Sample Project

The repository includes a `sample-project` directory, which contains example code to help you get started with AstronKt.

## Future Plans

* The dclassmacro component is expected to expand with support for more output formats beyond Kotlin, such as JSON, to
  broaden its utility.
* Seperate the core library into its client and server components.
* Add testing for the core library and functionality testing