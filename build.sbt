import sbt.ThisBuild
import org.jetbrains.sbtidea.Keys.*
import sbt.*

enablePlugins(OssumIncPlugin)

Global / onChangedBuildSource := ReloadOnSourceChanges
(Global / excludeLintKeys) ++= Set(mainClass)

lazy val developers: List[Developer] = List(
  Developer(
    "AlWein92",
    "Alex Weinstein",
    "alex.weinstein@improving.com",
    url("https://github.com/AlWein92")
  ),
  Developer(
    id = "reid-spencer",
    "Reid Spencer",
    "reid.spencer@ossuminc.com",
    url("https://github.com/reid-spencer")
  )
)

resolvers += Resolver.githubPackages("ossuminc", "riddl")

lazy val riddlIdeaPlugin: Project =
  Root(
    ghRepoName = "riddl-idea-plugin",
    ghOrgName = "ossuminc",
    orgPackage = "com.ossuminc.riddl.plugins.idea",
    orgName = "Ossum, Inc.",
    orgPage = url("https://www.ossuminc.com/"),
    startYr = 2024,
    devs = developers,
    spdx = "Apache-2.0"
  ).configure(
      With.basic,
      With.Scala3.configure(version = Some("3.4.3")),
      With.Scalatest(V.scalatest),
      With.coverage(0),
      With.build_info,
      With.GithubPublishing
    )
    .enablePlugins(JavaAppPackaging)
    .settings(
      buildInfoPackage := (ThisBuild / organization).value,
      buildInfoObject := "RiddlIDEAPluginBuildInfo",
      description := "The plugin for supporting RIDDL in IntelliJ",
      libraryDependencies ++= Seq(
        Dep.minimalJson,
        Dep.riddlCommands,
        Dep.junit,
        Dep.opentest4j
      ),
      Test / parallelExecution := false,
      ThisBuild / intellijPluginName := "RIDDL4IDEA",
      ThisBuild / intellijBuild := "253.29346.240",
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
