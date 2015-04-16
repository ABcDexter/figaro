/*
 * TTestResult.scala 
 * T Test
 * 
 * Created By:      Michael Reposa (mreposa@cra.com), Glenn Takata (gtakata@cra.com), Brian Ruttenberg (bruttenberg@cra.com)
 * Creation Date:   Mar 19, 2015
 * 
 * Copyright 2013 Avrom J. Pfeffer and Charles River Analytics, Inc.
 * See http://www.cra.com or email figaro@cra.com for information.
 * 
 * See http://www.github.com/p2t2/figaro for a copy of the software license.
 */

package com.cra.figaro.ndtest

import scala.collection.mutable.ListBuffer
import org.apache.commons.math3.stat.descriptive.SummaryStatistics

class TTestResult(val name: String, val target: Double, val alpha: Double = .05) extends NDTestResult {
  val statistics = new SummaryStatistics()

  def update(value: Any) {
    value match {
      case x: Double => statistics.addValue(x)
      case x: Int => statistics.addValue(x.toDouble)
      case x: Float => statistics.addValue(x.toDouble)
      case _ => println(value + " improper value for t-test")
    }
  }

  def check: Boolean = {
    val tester = new org.apache.commons.math3.stat.inference.TTest
    val result = if (statistics.getVariance <= 0) {
      println("  !NDTest: " + name + " has zero variance")
      target == statistics.getMean
    } else {
      // Apache Commons Math T Test
      // Returns false if the test passed and true if the test fails, so reverse this for return value   
      !tester.tTest(target.asInstanceOf[scala.Double], statistics, alpha.asInstanceOf[scala.Double])
    }

    if (!result) {
      val mean = statistics.getMean
      val variance = statistics.getVariance
      println(f"$name failed with mean $mean%.4f  and variance $variance%.6f")
    }
    result
  }
}