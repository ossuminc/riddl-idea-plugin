/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea

import com.intellij.ide.plugins.{IdeaPluginDependency, IdeaPluginDescriptor}
import com.intellij.openapi.extensions.PluginId

import java.nio.file.Path
import com.ossuminc.riddl.plugins.idea.RiddlIDEAPluginBuildInfo

import java.net.URISyntaxException
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Collections

final class RiddlPluginDescriptor extends IdeaPluginDescriptor {

  // Members declared in com.intellij.openapi.extensions.PluginDescriptor

  override def getPluginId: PluginId = PluginId.getId("com.ossuminc.riddl.plugins.idea")

  def getCategory: String = "Design Language"

  def getChangeNotes: String =
    """Pre-release"""

  def getDescription: String =
    """Provides support for RIDDL, an open-source system design language.
      |The usual language support is provided: coloring, syntax checking
      |and highlighting, validation, etc.
    """.stripMargin

  def getName: String = "RIDDL"

  def getPluginClassLoader: ClassLoader = this.getClass.getClassLoader

  def getPluginPath: java.nio.file.Path =
    try {
      Path.of(this.getClass.getProtectionDomain.getCodeSource.getLocation.toURI);
    } catch {
      case (e: URISyntaxException) =>
        return Path.of("")
    }

  def getProductCode: String = null

  def getReleaseDate: java.util.Date =
    java.util.Date.from(java.time.Instant.parse(RiddlIDEAPluginBuildInfo.buildInstant))

  def getReleaseVersion: Int = 1

  def getResourceBundleBaseName: String = null

  def getSinceBuild: String = "241.8102.112"

  def getUntilBuild: String = null

  def getUrl: String = "https://ossum.tech/riddl/index.html#riddl-idea-plugin"

  def getVendor: String = RiddlIDEAPluginBuildInfo.organizationName

  def getVendorEmail: String = "info@ossuminc.com"

  def getVendorUrl: String = RiddlIDEAPluginBuildInfo.organizationHomepage

  override def getVersion: String = RiddlIDEAPluginBuildInfo.version

  def isEnabled: Boolean = true

  def isLicenseOptional: Boolean = false

  def setEnabled(x$0: Boolean): Unit = ()

  // Members declared in com.intellij.ide.plugins.IdeaPluginDescriptor
  def getDependencies: java.util.List[IdeaPluginDependency] =
    Collections.emptyList()

  def getDescriptorPath: String = "resources/META-INF/plugin.xml"

  def getOptionalDependentPluginIds: Array[PluginId] = Array.empty
}
