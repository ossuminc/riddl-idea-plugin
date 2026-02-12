// GitHub Packages resolver for sbt-ossuminc
resolvers += "GitHub Packages" at "https://maven.pkg.github.com/ossuminc/sbt-ossuminc"

addSbtPlugin("com.ossuminc" % "sbt-ossuminc" % "1.3.2")

// Note: sbt-idea-plugin is included in sbt-ossuminc 1.2.0 (version 5.0.4)

// Note: addSbtPlugin("org.jetbrains.scala" % "sbt-kotlin-plugin" % "3.0.3")

// This enables sbt-bloop to create bloop config files for Metals editors
// Uncomment locally if you use metals, otherwise don't slow down other
// people's builds by leaving it commented in the repo.
// addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.4.6")
