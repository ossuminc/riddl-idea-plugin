import org.jetbrains.sbtidea.Keys.*
import sbt.ThisBuild

Global / onChangedBuildSource := ReloadOnSourceChanges
(Global / excludeLintKeys) ++= Set(mainClass)

val scalaVer = "3.4.1"
Global / scalaVersion := scalaVer

lazy val riddl: Project =
  Root(
    "riddl-idea-plugin",
    "ossuminc",
    "com.ossuminc.riddl.plugins.idea",
    startYr = 2024
  )
    .configure(With.noPublishing, With.git, With.dynver)
    .aggregate(riddlIdeaPlugin)

lazy val riddlIdeaPlugin: Project = Module("src", "riddl-idea-plugin")
  .configure(
    With.noPublishing,
    With.typical,
    With.build_info,
    With.coverage(90)
  )
  .settings(
    version := "0.0.1",
    buildInfoPackage := "com.ossuminc.riddl.plugins.idea",
    buildInfoObject := "RiddlIDEAPluginBuildInfo",
    description := "The plugin for supporting RIDDL in IntelliJ",
    libraryDependencies ++= Dep.testing ++ Dep.basic,
    Test / parallelExecution := false,
    scalaVersion := scalaVer,
    ThisBuild / intellijPluginName := "RIDDL4IDEA",
    ThisBuild / intellijBuild := "231.9011.34",
    ThisBuild / intellijPlatform := IntelliJPlatform.IdeaUltimate,
    ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
    intellijPlugins ++= Seq("com.intellij.properties".toPlugin),
    Global / intellijAttachSources := true,
    Compile / javacOptions ++= "--release" :: "17" :: Nil,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Test / unmanagedResourceDirectories += baseDirectory.value / "testResources"
  )
