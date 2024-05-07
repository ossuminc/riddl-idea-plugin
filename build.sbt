import kotlin.Keys.*

enablePlugins(OssumIncPlugin)

import org.jetbrains.sbtidea.Keys.*
import sbt.ThisBuild

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
    With.aliases
  )
  .enablePlugins(KotlinPlugin)
  .settings(
    // NOTE: check community/.idea/libraries/kotlin_stdlib.xml in intellij monorepo when updating intellijVersion
    // NOTE: keep versions in sync with ultimate/.idea/kotlinc.xml and community/.idea/kotlinc.xml
    kotlinVersion := "1.9.22",
    kotlincJvmTarget := "17",
    kotlinRuntimeProvided := true,
    buildInfoPackage := "com.ossuminc.riddl.plugins.idea",
    buildInfoObject := "RiddlIDEAPluginBuildInfo",
    description := "The plugin for supporting RIDDL in IntelliJ",
    libraryDependencies ++= Dep.testing ++ Dep.basic ++ Dep.riddl,
    Test / parallelExecution := false,
    scalaVersion := "3.4.1",
    ThisBuild / intellijPluginName := "RIDDL4IDEA",
    ThisBuild / intellijBuild := "241.15989.150",
    ThisBuild / intellijPlatform := IntelliJPlatform.IdeaUltimate,
    ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
    intellijPlugins ++= Seq("com.intellij.properties".toPlugin),
    Global / intellijAttachSources := true,
    Compile / javacOptions ++= "--release" :: "17" :: Nil,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Test / unmanagedResourceDirectories += baseDirectory.value / "testResources",
    intellijRuntimePlugins := Seq(
      "org.jetbrains.kotlin".toPlugin
    )
  )
