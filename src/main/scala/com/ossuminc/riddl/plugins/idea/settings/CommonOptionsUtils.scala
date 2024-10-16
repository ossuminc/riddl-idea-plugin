package com.ossuminc.riddl.plugins.idea.settings

import com.ossuminc.riddl.language.CommonOptions

object CommonOptionsUtils {
  implicit class CommonOptionsExt(var commonOptions: CommonOptions) {
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
    def setShowInfoMessages(showInfoMessages: Boolean): CommonOptions = {
      commonOptions = commonOptions.copy(showInfoMessages = showInfoMessages)
      commonOptions
    }
    def getShowInfoMessages: Boolean = commonOptions.showInfoMessages
  }

  case class CommonOption(
      name: String,
      getCommonOptionValue: CommonOptions => Boolean,
      setCommonOptionValue: CommonOptions => Boolean => CommonOptions
  )

  val AllCommonOptions: Seq[CommonOption] = Seq(
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
    )
  ).map(tup => CommonOption(tup._1, tup._2, tup._3))
}
