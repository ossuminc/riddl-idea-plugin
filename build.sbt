import kotlin.Keys.*
import sbt.ThisBuild
import org.jetbrains.sbtidea.Keys.*

enablePlugins(OssumIncPlugin)

Global / onChangedBuildSource := ReloadOnSourceChanges
(Global / excludeLintKeys) ++= Set(mainClass)

lazy val riddlIdeaPlugin: Project = Root(
  "riddl-idea-plugin",
  "ossuminc",
  "com.ossuminc.riddl.plugins.idea",
  startYr = 2024
)
  .configure(
    With.typical,
    With.build_info,
    With.coverage(90),
    With.aliases,
    With.riddl(forJS = false, "0.52.2-13-89c9057a")
  )
  .enablePlugins(KotlinPlugin, JavaAppPackaging)
  .settings(
    kotlinVersion := "2.0.0",
    kotlincJvmTarget := "22",
    kotlinRuntimeProvided := true,
    buildInfoPackage := "com.ossuminc.riddl.plugins.idea",
    buildInfoObject := "RiddlIDEAPluginBuildInfo",
    description := "The plugin for supporting RIDDL in IntelliJ",
    libraryDependencies ++= Dep.testing ++ Dep.basic :+ Dep.kotlin,
    Test / parallelExecution := false,
    scalaVersion := "3.4.0",
    ThisBuild / intellijPluginName := "RIDDL4IDEA",
    ThisBuild / intellijBuild := "242.23339.11",
    ThisBuild / intellijPlatform := IntelliJPlatform.IdeaUltimate,
    ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
    intellijPlugins ++= Seq("com.intellij.properties".toPlugin),
    Global / intellijAttachSources := true,
    Compile / javacOptions ++= "--release" :: "21" :: Nil,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Test / unmanagedResourceDirectories += baseDirectory.value / "testResources",
    runIDE / javaOptions += "-Didea.http.proxy.port=5432,-DurlSchemes=http://localhost",
    unmanagedBase := baseDirectory.value / "lib",
  )
