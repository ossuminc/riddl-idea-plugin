// GitHub Packages resolver for sbt-ossuminc
resolvers += "GitHub Packages" at "https://maven.pkg.github.com/ossuminc/sbt-ossuminc"

addSbtPlugin("com.ossuminc" % "sbt-ossuminc" % "1.4.0")

// sbt-ossuminc 1.4.0 re-exports sbt-idea-plugin 5.1.0. Declaring it directly
// here evicts that in favour of the 6.x line. Keep this until sbt-ossuminc
// re-exports 6.x itself, at which point this override can be dropped.
addSbtPlugin("org.jetbrains.scala" % "sbt-idea-plugin" % "6.0.0-RC3")

// Note: addSbtPlugin("org.jetbrains.scala" % "sbt-kotlin-plugin" % "3.0.3")

// This enables sbt-bloop to create bloop config files for Metals editors
// Uncomment locally if you use metals, otherwise don't slow down other
// people's builds by leaving it commented in the repo.
// addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.4.6")
