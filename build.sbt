import kotlin.Keys.{kotlinRuntimeProvided, kotlinVersion, kotlincJvmTarget}
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
    With.riddl("0.55.0-30-2bf4ce43")
  )
  .enablePlugins(KotlinPlugin, JavaAppPackaging)
  .settings(
    kotlinVersion := "2.0.0",
    kotlincJvmTarget := "22",
    kotlinRuntimeProvided := true,
    buildInfoPackage := "com.ossuminc.riddl.plugins.idea",
    buildInfoObject := "RiddlIDEAPluginBuildInfo",
    description := "The plugin for supporting RIDDL in IntelliJ",
    libraryDependencies ++= Dep.testing ++ Dep.basic ++ Seq(
      Dep.kotlin,
      Dep.riddlCommands,
      Dep.pureconfig
    ),
    Test / parallelExecution := false,
    scalaVersion := "3.4.3",
    ThisBuild / intellijPluginName := "RIDDL4IDEA",
    ThisBuild / intellijBuild := "242.23726.103",
    ThisBuild / intellijPlatform := IntelliJPlatform.IdeaUltimate,
    ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
    intellijPlugins ++= Seq("com.intellij.properties".toPlugin),
    Global / intellijAttachSources := true,
    Compile / javacOptions ++= "--release" :: "21" :: Nil,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Test / unmanagedResourceDirectories += baseDirectory.value / "testResources",
    runIDE / javaOptions += "-Didea.http.proxy.port=5432,-DurlSchemes=http://localhost",
    unmanagedBase := baseDirectory.value / "lib"
  )
