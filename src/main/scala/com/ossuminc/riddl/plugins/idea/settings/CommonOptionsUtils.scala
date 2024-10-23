package com.ossuminc.riddl.plugins.idea.settings

import com.ossuminc.riddl.language.CommonOptions
import java.nio.file.Path
import scala.concurrent.duration.FiniteDuration

object CommonOptionsUtils {
  implicit class CommonOptionsExt(var commonOptions: CommonOptions) {
    def setShowInfoMessages(showInfoMessages: Boolean): CommonOptions = {
      commonOptions = commonOptions.copy(showInfoMessages = showInfoMessages)
      commonOptions
    }
    def getShowInfoMessages: Boolean = commonOptions.showInfoMessages

    def setShowTimes(showTimes: Boolean): CommonOptions = {
      commonOptions = commonOptions.copy(showTimes = showTimes)
      commonOptions
    }
    def getShowTimes: Boolean = commonOptions.showTimes

    def setShowIncludeTimes(showIncludeTimes: Boolean): CommonOptions = {
      commonOptions = commonOptions.copy(showIncludeTimes = showIncludeTimes)
      commonOptions
    }
    def getShowIncludeTimes: Boolean = commonOptions.showIncludeTimes

    def setShowWarnings(showWarnings: Boolean): CommonOptions = {
      commonOptions = commonOptions.copy(showWarnings = showWarnings)
      commonOptions
    }
    def getShowWarnings: Boolean = commonOptions.showWarnings

    def setShowMissingWarnings(showMissingWarnings: Boolean): CommonOptions = {
      commonOptions =
        commonOptions.copy(showMissingWarnings = showMissingWarnings)
      commonOptions
    }
    def getShowMissingWarnings: Boolean = commonOptions.showMissingWarnings

    def setShowStyleWarnings(showStyleWarnings: Boolean): CommonOptions = {
      commonOptions = commonOptions.copy(showStyleWarnings = showStyleWarnings)
      commonOptions
    }
    def getShowStyleWarnings: Boolean = commonOptions.showStyleWarnings

    def setShowUsageWarnings(showUsageWarnings: Boolean): CommonOptions = {
      commonOptions = commonOptions.copy(showUsageWarnings = showUsageWarnings)
      commonOptions
    }
    def getShowUsageWarnings: Boolean = commonOptions.showUsageWarnings

    def setVerbose(verbose: Boolean): CommonOptions = {
      commonOptions = commonOptions.copy(verbose = verbose)
      commonOptions
    }
    def getVerbose: Boolean = commonOptions.verbose

    def setDryRun(dryRun: Boolean): CommonOptions = {
      commonOptions = commonOptions.copy(dryRun = dryRun)
      commonOptions
    }
    def getDryRun: Boolean = commonOptions.dryRun

    def setQuiet(quiet: Boolean): CommonOptions = {
      commonOptions = commonOptions.copy(quiet = quiet)
      commonOptions
    }
    def getQuiet: Boolean = commonOptions.quiet

    def setDebug(debug: Boolean): CommonOptions = {
      commonOptions = commonOptions.copy(debug = debug)
      commonOptions
    }
    def getDebug: Boolean = commonOptions.debug

    def setPluginsDir(pluginsDir: Option[Path]): CommonOptions = {
      commonOptions = commonOptions.copy(pluginsDir = pluginsDir)
      commonOptions
    }
    def getPluginsDir: Option[Path] = commonOptions.pluginsDir

    def setSortMessagesByLocation(
        sortMessagesByLocation: Boolean
    ): CommonOptions = {
      commonOptions =
        commonOptions.copy(sortMessagesByLocation = sortMessagesByLocation)
      commonOptions
    }
    def getSortMessagesByLocation: Boolean =
      commonOptions.sortMessagesByLocation

    def setMaxParallelParsing(maxParallelParsing: Int): CommonOptions = {
      commonOptions =
        commonOptions.copy(maxParallelParsing = maxParallelParsing)
      commonOptions
    }
    def getMaxParallelParsing: Int = commonOptions.maxParallelParsing

    def setMaxIncludeWait(maxIncludeWait: FiniteDuration): CommonOptions = {
      commonOptions = commonOptions.copy(maxIncludeWait = maxIncludeWait)
      commonOptions
    }
    def getMaxIncludeWait: FiniteDuration = commonOptions.maxIncludeWait

    def setWarningsAreFatal(warningsAreFatal: Boolean): CommonOptions = {
      commonOptions = commonOptions.copy(warningsAreFatal = warningsAreFatal)
      commonOptions
    }
    def getWarningsAreFatal: Boolean = commonOptions.warningsAreFatal
  }

  case class CommonOption[T](
      name: String,
      getCommonOptionValue: CommonOptions => ? <: T,
      setCommonOptionValue: CommonOptions => ? <: T => CommonOptions
  )

  val BooleanCommonOptions: Seq[CommonOption[Boolean]] = Seq(
    (
      "show-times",
      (commonOptions: CommonOptions) => commonOptions.getShowTimes,
      (commonOptions: CommonOptions) => commonOptions.setShowTimes
    ),
    (
      "show-include-times",
      (commonOptions: CommonOptions) => commonOptions.getShowIncludeTimes,
      (commonOptions: CommonOptions) => commonOptions.setShowIncludeTimes
    ),
    (
      "show-warnings",
      (commonOptions: CommonOptions) => commonOptions.getShowWarnings,
      (commonOptions: CommonOptions) => commonOptions.setShowWarnings
    ),
    (
      "show-missing-warnings",
      (commonOptions: CommonOptions) => commonOptions.getShowMissingWarnings,
      (commonOptions: CommonOptions) => commonOptions.setShowMissingWarnings
    ),
    (
      "show-style-warnings",
      (commonOptions: CommonOptions) => commonOptions.getShowStyleWarnings,
      (commonOptions: CommonOptions) => commonOptions.setShowStyleWarnings
    ),
    (
      "show-usage-warnings",
      (commonOptions: CommonOptions) => commonOptions.getShowUsageWarnings,
      (commonOptions: CommonOptions) => commonOptions.setShowUsageWarnings
    ),
    (
      "show-info-messages",
      (commonOptions: CommonOptions) => commonOptions.getShowInfoMessages,
      (commonOptions: CommonOptions) => commonOptions.setShowInfoMessages
    ),
    (
      "verbose",
      (commonOptions: CommonOptions) => commonOptions.getVerbose,
      (commonOptions: CommonOptions) => commonOptions.setVerbose
    ),
    (
      "dryRun",
      (commonOptions: CommonOptions) => commonOptions.getDryRun,
      (commonOptions: CommonOptions) => commonOptions.setDryRun
    ),
    (
      "quiet",
      (commonOptions: CommonOptions) => commonOptions.getQuiet,
      (commonOptions: CommonOptions) => commonOptions.setQuiet
    ),
    (
      "debug",
      (commonOptions: CommonOptions) => commonOptions.getDebug,
      (commonOptions: CommonOptions) => commonOptions.setDebug
    ),
    (
      "sortMessagesByLocation",
      (commonOptions: CommonOptions) => commonOptions.getSortMessagesByLocation,
      (commonOptions: CommonOptions) => commonOptions.setSortMessagesByLocation
    ),
    (
      "warningsAreFatal",
      (commonOptions: CommonOptions) => commonOptions.getWarningsAreFatal,
      (commonOptions: CommonOptions) => commonOptions.setWarningsAreFatal
    )
  ).map(tup => CommonOption(tup._1, tup._2, tup._3))

  val IntegerCommonOption: CommonOption[Int] =
    CommonOption(
      "max-parallel-parsing",
      (commonOptions: CommonOptions) => commonOptions.getMaxParallelParsing,
      (commonOptions: CommonOptions) => commonOptions.setMaxParallelParsing
    )

  val FiniteDurationCommonOption: CommonOption[FiniteDuration] =
    CommonOption(
      "max-include-wait",
      (commonOptions: CommonOptions) => commonOptions.getMaxIncludeWait,
      (commonOptions: CommonOptions) => commonOptions.setMaxIncludeWait
    )

  val PluginsDirOption: CommonOption[Option[Path]] = CommonOption(
    "plugins-dir",
    (commonOptions: CommonOptions) => commonOptions.getPluginsDir,
    (commonOptions: CommonOptions) => commonOptions.setPluginsDir
  )
}
