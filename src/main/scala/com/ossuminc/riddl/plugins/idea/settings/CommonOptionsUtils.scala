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
      commonOptions = commonOptions.copy(showMissingWarnings = showMissingWarnings)
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

  val AllCommonOptions: Seq[(String, CommonOptions => Boolean => CommonOptions, CommonOptions => Boolean)] = Seq(
    ("show-times",
      (commonOptions: CommonOptions) => commonOptions.setShowTimes,
      (commonOptions: CommonOptions) => commonOptions.getShowTimes
    ), ("show-include-times",
      (commonOptions: CommonOptions) => commonOptions.setShowIncludeTimes,
      (commonOptions: CommonOptions) => commonOptions.getShowIncludeTimes
    ), ("show-warnings",
      (commonOptions: CommonOptions) => commonOptions.setShowWarnings,
      (commonOptions: CommonOptions) => commonOptions.getShowWarnings
    ), ("show-missing-warnings",
      (commonOptions: CommonOptions) => commonOptions.setShowMissingWarnings,
      (commonOptions: CommonOptions) => commonOptions.getShowMissingWarnings
    ), ("show-style-warnings",
      (commonOptions: CommonOptions) => commonOptions.setShowStyleWarnings,
      (commonOptions: CommonOptions) => commonOptions.getShowStyleWarnings
    ), ("show-usage-warnings",
      (commonOptions: CommonOptions) => commonOptions.setShowUsageWarnings,
      (commonOptions: CommonOptions) => commonOptions.getShowUsageWarnings
    ), ("show-info-messages",
      (commonOptions: CommonOptions) => commonOptions.setShowInfoMessages,
      (commonOptions: CommonOptions) => commonOptions.getShowInfoMessages
    )
  )
}
