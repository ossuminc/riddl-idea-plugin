/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.structure

import com.intellij.icons.AllIcons
import javax.swing.Icon

/** Icons for RIDDL structure view elements.
  *
  * Uses IntelliJ's built-in icons for consistency with the IDE.
  */
object RiddlStructureIcons {

  // File icon
  val FILE: Icon = AllIcons.FileTypes.Any_type

  // Container definitions
  val DOMAIN: Icon = AllIcons.Nodes.Module
  val CONTEXT: Icon = AllIcons.Nodes.Package
  val ENTITY: Icon = AllIcons.Nodes.Class
  val ADAPTOR: Icon = AllIcons.Nodes.Plugin
  val APPLICATION: Icon = AllIcons.Nodes.WebFolder
  val EPIC: Icon = AllIcons.Nodes.Favorite
  val SAGA: Icon = AllIcons.Nodes.DataSchema
  val REPOSITORY: Icon = AllIcons.Nodes.DataTables
  val PROJECTOR: Icon = AllIcons.Nodes.DataColumn
  val STREAMLET: Icon = AllIcons.Nodes.AbstractClass
  val CONNECTOR: Icon = AllIcons.Nodes.Related

  // Definition components
  val HANDLER: Icon = AllIcons.Nodes.Lambda
  val FUNCTION: Icon = AllIcons.Nodes.Function
  val STATE: Icon = AllIcons.Nodes.ObjectTypeAttribute
  val TYPE: Icon = AllIcons.Nodes.Type
  val CONSTANT: Icon = AllIcons.Nodes.Constant
  val INLET: Icon = AllIcons.Nodes.ReadAccess
  val OUTLET: Icon = AllIcons.Nodes.WriteAccess

  // Message types
  val COMMAND: Icon = AllIcons.Nodes.Annotationtype
  val EVENT: Icon = AllIcons.Nodes.ExceptionClass
  val QUERY: Icon = AllIcons.Nodes.Interface
  val RESULT: Icon = AllIcons.Nodes.AnonymousClass
  val RECORD: Icon = AllIcons.Nodes.Record

  // Default fallback
  val DEFAULT: Icon = AllIcons.Nodes.Unknown

  /** Get the appropriate icon for a RIDDL definition kind. */
  def forKind(kind: String): Icon = kind.toLowerCase match {
    case "domain"      => DOMAIN
    case "context"     => CONTEXT
    case "entity"      => ENTITY
    case "adaptor"     => ADAPTOR
    case "application" => APPLICATION
    case "epic"        => EPIC
    case "saga"        => SAGA
    case "repository"  => REPOSITORY
    case "projector"   => PROJECTOR
    case "streamlet"   => STREAMLET
    case "connector"   => CONNECTOR
    case "handler"     => HANDLER
    case "function"    => FUNCTION
    case "state"       => STATE
    case "type"        => TYPE
    case "constant"    => CONSTANT
    case "inlet"       => INLET
    case "outlet"      => OUTLET
    case "command"     => COMMAND
    case "event"       => EVENT
    case "query"       => QUERY
    case "result"      => RESULT
    case "record"      => RECORD
    case _             => DEFAULT
  }
}
