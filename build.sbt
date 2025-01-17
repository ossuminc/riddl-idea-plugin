import kotlin.Keys.{kotlinRuntimeProvided, kotlinVersion, kotlincJvmTarget}
import sbt.ThisBuild
import org.jetbrains.sbtidea.Keys.*
import sbt.*

enablePlugins(OssumIncPlugin)

Global / onChangedBuildSource := ReloadOnSourceChanges
(Global / excludeLintKeys) ++= Set(mainClass)

resolvers += Resolver.githubPackages("ossuminc", "riddl")

lazy val riddlIdeaPlugin: Project =
  Root("riddl-idea-plugin", "com.ossuminc.riddl.plugins.idea", startYr = 2024)
    .configure(
      With.typical,
      With.scalaTest(V.scalatest),
      With.coverage(90),
      With.riddl(V.riddl),
      With.build_info
    )
    .enablePlugins(KotlinPlugin, JavaAppPackaging)
    .settings(
      kotlinVersion := "2.0.0",
      kotlincJvmTarget := "21",
      kotlinRuntimeProvided := true,
      buildInfoPackage := "com.ossuminc.riddl.plugins.idea",
      buildInfoObject := "RiddlIDEAPluginBuildInfo",
      description := "The plugin for supporting RIDDL in IntelliJ",
      libraryDependencies ++= Seq(
        Dep.minimalJson,
        Dep.kotlin,
        Dep.riddlCommands,
        Dep.junit
      ),
      Test / parallelExecution := false,
      scalaVersion := "3.4.3",
      ThisBuild / intellijPluginName := "RIDDL4IDEA",
      ThisBuild / intellijBuild := "243.22562.218",
      ThisBuild / intellijPlatform := IntelliJPlatform.IdeaCommunity,
      ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
      intellijPlugins ++= Seq("com.intellij.properties".toPlugin),
      Global / intellijAttachSources := true,
      Compile / javacOptions ++= "--release" :: "21" :: Nil,
      Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
      Test / unmanagedResourceDirectories += baseDirectory.value / "testResources",
      runIDE / javaOptions
        .withRank(
          KeyRanks.Invisible
        ) += "-Didea.http.proxy.port=5432,-DurlSchemes=http://localhost",
      unmanagedBase := baseDirectory.value / "lib"
    )
