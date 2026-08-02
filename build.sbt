import sbt.ThisBuild
import org.jetbrains.sbtidea.Keys.*
import org.jetbrains.sbtidea.verifier.FailureLevel
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
      With.Scala3.configure(version = Some("3.9.0-RC4")),
      With.Scalatest(V.scalatest),
      // Fetches and maintains .scalafmt.conf from the sbt-ossuminc repo on
      // `update`; the file itself is gitignored. Without this, scalafmtCheck
      // fails with "File does not exist: .scalafmt.conf".
      With.Scalafmt,
      With.coverage(0),
      With.BuildInfo,
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
        Dep.riddlLib,
        Dep.junit,
        Dep.opentest4j
      ),
      Test / parallelExecution := false,
      ThisBuild / intellijPluginName := "RIDDL4IDEA",
      ThisBuild / intellijBuild := "253.29346.240",
      // IntelliJPlatform.Idea is the unified 2025.3+ distribution (ideaIU).
      // It replaces the deprecated IdeaCommunity (ideaIC) -- a change of
      // target SDK, not a rename. Take care not to use Ultimate-only APIs.
      ThisBuild / intellijPlatform := IntelliJPlatform.Idea,
      ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
      intellijPlugins ++= Seq("com.intellij.properties".toPlugin),
      Global / intellijAttachSources := true,
      Compile / javacOptions ++= "--release" :: "21" :: Nil,
      // sbt-idea-plugin defaults pluginVerifierOptions.failureLevels to
      // FailureLevel.values().toSet, so runPluginVerifier fails on ANY notice --
      // including deprecated/experimental/internal API usage, of which this
      // plugin has plenty while the verifier's own verdict is still
      // "Compatible". That makes the task useless as a release gate. Fail only
      // on the levels that actually stop a plugin from working, and let the
      // API-usage notices be advisory (they still appear in the report and are
      // worth reviewing before a Marketplace submission).
      pluginVerifierOptions := pluginVerifierOptions.value.copy(
        failureLevels = Set(
          FailureLevel.COMPATIBILITY_PROBLEMS,
          FailureLevel.INVALID_PLUGIN,
          FailureLevel.MISSING_DEPENDENCIES,
          FailureLevel.PLUGIN_STRUCTURE_WARNINGS
        )
      ),
      Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
      Test / unmanagedResourceDirectories += baseDirectory.value / "testResources",
      runIDE / javaOptions
        .withRank(
          KeyRanks.Invisible
        ) += "-Didea.http.proxy.port=5432,-DurlSchemes=http://localhost",
      unmanagedBase := baseDirectory.value / "lib"
    )
