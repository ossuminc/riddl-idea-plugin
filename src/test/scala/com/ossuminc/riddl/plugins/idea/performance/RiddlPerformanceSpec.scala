/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.performance

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import com.ossuminc.riddl.language.parsing.{RiddlParserInput, TopLevelParser}
import com.ossuminc.riddl.utils.pc

/** Performance tests for RIDDL parsing with large files.
  *
  * These tests verify that the lexer and parser can handle large RIDDL files
  * within acceptable time limits. They use only RIDDL APIs (not IntelliJ)
  * to avoid the Java 21 requirement.
  */
class RiddlPerformanceSpec extends AnyWordSpec with Matchers {

  /** Generate a large RIDDL domain with many entities. */
  private def generateLargeDomain(entityCount: Int): String = {
    val entities = (1 to entityCount).map { i =>
      s"""  entity Entity$i is {
         |    state EntityState$i of Entity${i}State
         |    handler Entity${i}Handler is {
         |      on command Create$i {
         |        send event Created$i to outlet Entity${i}Events
         |      }
         |      on event Updated$i {
         |        set field to @Updated$i.newValue
         |      }
         |    }
         |  }
         |
         |  type Entity${i}State is {
         |    id: Id(Entity$i),
         |    name: String,
         |    createdAt: DateTime,
         |    updatedAt: DateTime?,
         |    status: String
         |  }
         |""".stripMargin
    }.mkString("\n")

    s"""domain LargeDomain is {
       |  context MainContext is {
       |$entities
       |  }
       |}
       |""".stripMargin
  }

  /** Generate a deeply nested RIDDL structure. */
  private def generateNestedDomain(depth: Int): String = {
    def generateNested(level: Int): String =
      if level >= depth then
        s"""${"  " * level}entity DeepEntity is {
           |${"  " * level}  state DeepState of DeepStateType
           |${"  " * level}}
           |""".stripMargin
      else
        val inner = generateNested(level + 1)
        s"""${"  " * level}context Level$level is {
           |$inner
           |${"  " * level}}
           |""".stripMargin

    s"""domain NestedDomain is {
       |${generateNested(1)}
       |}
       |""".stripMargin
  }

  "RIDDL Tokenization" should {
    "tokenize a small file quickly (< 200ms including JIT warmup)" in {
      val riddl =
        """domain Test is {
          |  context Ctx is {
          |    entity User is {
          |      state UserState of UserStateType
          |    }
          |  }
          |}
          |""".stripMargin

      val startTime = System.currentTimeMillis()
      val result = TopLevelParser.parseToTokens(RiddlParserInput(riddl, "test"))
      val elapsed = System.currentTimeMillis() - startTime

      result.isRight.shouldBe(true)
      // First invocation includes JIT compilation overhead
      elapsed should be < 200L
    }

    "tokenize a medium file (50 entities) within 500ms" in {
      val riddl = generateLargeDomain(50)
      riddl.length should be > 5000

      val startTime = System.currentTimeMillis()
      val result = TopLevelParser.parseToTokens(RiddlParserInput(riddl, "test"))
      val elapsed = System.currentTimeMillis() - startTime

      result.isRight.shouldBe(true)
      elapsed should be < 500L
      info(s"Tokenized ${riddl.length} chars (50 entities) in ${elapsed}ms")
    }

    "tokenize a large file (200 entities) within 2 seconds" in {
      val riddl = generateLargeDomain(200)
      riddl.length should be > 20000

      val startTime = System.currentTimeMillis()
      val result = TopLevelParser.parseToTokens(RiddlParserInput(riddl, "test"))
      val elapsed = System.currentTimeMillis() - startTime

      result.isRight.shouldBe(true)
      elapsed should be < 2000L
      info(s"Tokenized ${riddl.length} chars (200 entities) in ${elapsed}ms")
    }

    "tokenize deeply nested structures (20 levels) within 500ms" in {
      val riddl = generateNestedDomain(20)

      val startTime = System.currentTimeMillis()
      val result = TopLevelParser.parseToTokens(RiddlParserInput(riddl, "test"))
      val elapsed = System.currentTimeMillis() - startTime

      result.isRight.shouldBe(true)
      elapsed should be < 500L
      info(s"Tokenized ${riddl.length} chars (20 levels deep) in ${elapsed}ms")
    }
  }

  "RIDDL Parsing" should {
    "parse a small file quickly (< 200ms)" in {
      val riddl =
        """domain Test is {
          |  context Ctx is {
          |    entity User is {
          |      state UserState of UserStateType is {
          |        id: Id(User)
          |      }
          |    }
          |  }
          |}
          |""".stripMargin

      val startTime = System.currentTimeMillis()
      val result = TopLevelParser.parseNebula(RiddlParserInput(riddl, "test"))
      val elapsed = System.currentTimeMillis() - startTime

      elapsed should be < 200L
    }

    "parse a medium file (50 entities) within 2 seconds" in {
      val riddl = generateLargeDomain(50)

      val startTime = System.currentTimeMillis()
      val result = TopLevelParser.parseNebula(RiddlParserInput(riddl, "test"))
      val elapsed = System.currentTimeMillis() - startTime

      elapsed should be < 2000L
      info(s"Parsed ${riddl.length} chars (50 entities) in ${elapsed}ms")
    }

    "parse deeply nested structures (15 levels) within 1 second" in {
      val riddl = generateNestedDomain(15)

      val startTime = System.currentTimeMillis()
      val result = TopLevelParser.parseNebula(RiddlParserInput(riddl, "test"))
      val elapsed = System.currentTimeMillis() - startTime

      elapsed should be < 1000L
      info(s"Parsed ${riddl.length} chars (15 levels deep) in ${elapsed}ms")
    }
  }

  "Memory Usage" should {
    "handle very large files without excessive memory" in {
      // Generate a 500-entity domain (approximately 100KB)
      val riddl = generateLargeDomain(500)
      riddl.length should be > 50000

      // Force GC before measuring
      System.gc()
      Thread.sleep(100)
      val beforeMemory = Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()

      val result = TopLevelParser.parseToTokens(RiddlParserInput(riddl, "test"))

      System.gc()
      Thread.sleep(100)
      val afterMemory = Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()

      val memoryUsed = (afterMemory - beforeMemory) / 1024 / 1024 // MB

      result.isRight.shouldBe(true)
      // Should use less than 100MB for tokenization
      memoryUsed should be < 100L
      info(s"Memory used for tokenizing 500 entities (~${riddl.length / 1024}KB): ~${memoryUsed}MB")
    }
  }

  "Repeated Tokenization" should {
    "show consistent performance across multiple runs" in {
      val riddl = generateLargeDomain(100)
      val iterations = 5
      val times = (1 to iterations).map { _ =>
        val startTime = System.currentTimeMillis()
        TopLevelParser.parseToTokens(RiddlParserInput(riddl, "test"))
        System.currentTimeMillis() - startTime
      }

      val avgTime = times.sum.toDouble / times.length
      val maxDeviation = times.map(t => math.abs(t - avgTime)).max

      // All runs should be within 2x of average (accounting for JIT warmup)
      times.foreach { t =>
        t should be < (avgTime * 2).toLong
      }

      info(s"Average time: ${avgTime.round}ms, max deviation: ${maxDeviation.round}ms")
    }
  }
}