/*
 * LazyStructuredVETest.scala
 * Tests for lazy structured variable elimination
 *
 * Created By:      William Kretschmer (kretsch@mit.edu)
 * Creation Date:   Jun 9, 2017
 *
 * Copyright 2017 Avrom J. Pfeffer and Charles River Analytics, Inc.
 * See http://www.cra.com or email figaro@cra.com for information.
 *
 * See http://www.github.com/p2t2/figaro for a copy of the software license.
 */
package com.cra.figaro.test.algorithm.structured.algorithm.laziness

import org.scalatest.{Matchers, WordSpec}
import com.cra.figaro.language._
import com.cra.figaro.algorithm.structured.algorithm.laziness.LazyStructuredVE
import com.cra.figaro.library.compound.If

class LazyStructuredVETest extends WordSpec with Matchers {
  // Two-level chain model
  def twoLevelChain(): Element[Boolean] = {
    val fl1 = Flip(0.1)
    val fl2 = Flip(0.2)
    val fl3 = Flip(0.3)
    val fl4 = Flip(0.4)
    val fl5 = Flip(0.5)
    val innerIf = If(fl3, fl4, fl5)
    If(fl1, fl2, innerIf)
  }
  
  // Recursive geometric distribution
  def recursiveGeometric(): Element[Int] = Chain(Flip(0.5), memoGeometricFunc)
  val memoGeometricFunc: Boolean => Element[Int] = (b: Boolean) => {
    if(b) Constant(1)
    else recursiveGeometric().map(_ + 1)
  }
  
  "One time lazy structured variable elimination" when {
    "given a simple two-level chain with no evidence" should {
      "produce the correct depth 1 approximation" in {
      	Universe.createNew()
      	val outerIf = twoLevelChain()
        val alg = LazyStructuredVE(1, outerIf)
      	alg.start()
      	alg.probabilityBounds(outerIf, true)._1 should be (0.02 +- 0.000000001)
      	alg.probabilityBounds(outerIf, false)._1 should be (0.08 +- 0.000000001)
      	alg.probabilityBounds(outerIf, true)._2 should be (0.92 +- 0.000000001)
      	alg.probabilityBounds(outerIf, false)._2 should be (0.98 +- 0.000000001)
      }
        
      "produce the correct depth 2 perfect answer" in {
      	Universe.createNew()
      	val outerIf = twoLevelChain()
        val alg = LazyStructuredVE(2, outerIf)
      	alg.start()
      	val pInner = 0.3 * 0.4 + 0.7 * 0.5
      	val pOuter = 0.1 * 0.2 + 0.9 * pInner
      	alg.probabilityBounds(outerIf, true)._1 should be (pOuter +- 0.000000001)
      	alg.probabilityBounds(outerIf, false)._1 should be ((1 - pOuter) +- 0.000000001)
      	alg.probabilityBounds(outerIf, true)._2 should be (pOuter +- 0.000000001)
      	alg.probabilityBounds(outerIf, false)._2 should be ((1 - pOuter) +- 0.000000001)
        // The lower and upper bounds are equal, so we should also get the exact answer using the probability method
        alg.probability(outerIf, true) should be (pOuter +- 0.000000001)
        alg.probability(outerIf, false) should be ((1 - pOuter) +- 0.000000001)

        alg.kill()
      } 
    }

    "given a simple two-level chain with a condition" should {
      "produce the correct depth 1 approximation" in {
      	Universe.createNew()
      	// This is designed so that depth 1 catches the check and its Flip arguments, but not the inner Flips of the two level chain.
      	val outerIf = twoLevelChain()
      	val apply = Apply(outerIf, (b: Boolean) => b)
      	val check = If(apply, Flip(0.6), Flip(0.3))
      	check.observe(true)
        val alg = LazyStructuredVE(1, apply)
      	alg.start()
      	val trueWeight = 0.1 * 0.2 * 0.6
      	val falseWeight = 0.1 * 0.8 * 0.3
      	val starWeightLower = 0
      	val starWeightUpper = 0.9
      	val lowerZ = trueWeight + falseWeight + starWeightLower
      	val upperZ = trueWeight + falseWeight + starWeightUpper
      	val trueLower = trueWeight / upperZ
      	val falseLower = falseWeight / upperZ
      	alg.probabilityBounds(apply, true)._1 should be (trueLower +- 0.000000001)
      	alg.probabilityBounds(apply, false)._1 should be (falseLower +- 0.000000001)
      	alg.probabilityBounds(apply, true)._2 should be ((1 - falseLower) +- 0.000000001)
      	alg.probabilityBounds(apply, false)._2 should be ((1 - trueLower) +- 0.000000001)
        
        alg.kill()
      }
      
      "produce the correct depth 2 perfect answer" in {
      	Universe.createNew()
      	val outerIf = twoLevelChain()
      	val apply = Apply(outerIf, (b: Boolean) => b)
      	val check = If(apply, Flip(0.6), Flip(0.3))
      	check.observe(true)
        val alg = LazyStructuredVE(2, apply)
      	alg.start()
      	val pInner = 0.3 * 0.4 + 0.7 * 0.5 
      	val pOuterT = 0.1 * 0.2 + 0.9 * pInner
      	val pOuterF = 1.0 - pOuterT
      	val qOuterT = pOuterT * 0.6
      	val qOuterF = pOuterF * 0.3
      	val pOuter = qOuterT / (qOuterF + qOuterT)
      	alg.probabilityBounds(apply, true)._1 should be (pOuter +- 0.000000001)
      	alg.probabilityBounds(apply, false)._1 should be ((1 - pOuter) +- 0.000000001)
      	alg.probabilityBounds(apply, true)._2 should be (pOuter +- 0.000000001)
      	alg.probabilityBounds(apply, false)._2 should be ((1 - pOuter) +- 0.000000001)
        // The lower and upper bounds are equal, so we should also get the exact answer using the probability method
        alg.probability(apply, true) should be (pOuter +- 0.000000001)
        alg.probability(apply, false) should be ((1 - pOuter) +- 0.000000001)
        
        alg.kill()
      } 
    }

    "given a simple two-level chain with a constraint" should {
      "produce the correct depth 1 approximation" in {
      	Universe.createNew()
      	val outerIf = twoLevelChain()
      	outerIf.addConstraint((b: Boolean) => if (b) 0.6; else 0.3)
      	val alg = LazyStructuredVE(1, outerIf)
      	alg.start()
      	val trueWeight = 0.1 * 0.2 * 0.6
      	val falseWeight = 0.1 * 0.8 * 0.3
      	val starWeightLower = 0
      	val starWeightUpper = 0.9
      	val lowerZ = trueWeight + falseWeight + starWeightLower
      	val upperZ = trueWeight + falseWeight + starWeightUpper
      	val trueLower = trueWeight / upperZ
      	val falseLower = falseWeight / upperZ
      	alg.probabilityBounds(outerIf, true)._1 should be (trueLower +- 0.000000001)
      	alg.probabilityBounds(outerIf, false)._1 should be (falseLower +- 0.000000001)
      	alg.probabilityBounds(outerIf, true)._2 should be ((1 - falseLower) +- 0.000000001)
      	alg.probabilityBounds(outerIf, false)._2 should be ((1 - trueLower) +- 0.000000001)
        
        alg.kill()
      }
      
      "produce the correct depth 2 perfect answer" in {
      	Universe.createNew()
      	val outerIf = twoLevelChain()
      	outerIf.addConstraint((b: Boolean) => if (b) 0.6; else 0.3)
        val alg = LazyStructuredVE(2, outerIf)
      	alg.start()
      	val pInner = 0.3 * 0.4 + 0.7 * 0.5 
      	val pOuterT = 0.1 * 0.2 + 0.9 * pInner
      	val pOuterF = 1.0 - pOuterT
      	val qOuterT = pOuterT * 0.6
      	val qOuterF = pOuterF * 0.3
      	val pOuter = qOuterT / (qOuterF + qOuterT)
      	alg.probabilityBounds(outerIf, true)._1 should be (pOuter +- 0.000000001)
      	alg.probabilityBounds(outerIf, false)._1 should be ((1 - pOuter) +- 0.000000001)
      	alg.probabilityBounds(outerIf, true)._2 should be (pOuter +- 0.000000001)
      	alg.probabilityBounds(outerIf, false)._2 should be ((1 - pOuter) +- 0.000000001)
        // The lower and upper bounds are equal, so we should also get the exact answer using the probability method
        alg.probability(outerIf, true) should be (pOuter +- 0.000000001)
        alg.probability(outerIf, false) should be ((1 - pOuter) +- 0.000000001)
        
        alg.kill()
      } 
    }
    
    "given a recursive geometric without evidence" should {
      "produce the correct depth 6 probability bounds for each value" in {
        Universe.createNew()
        val geometric = recursiveGeometric()
        val alg = LazyStructuredVE(6, geometric)
        alg.start()
        // Unassigned probability at this expansion
        val pUnexpanded = math.pow(0.5, 7)

        val allBounds = alg.allProbabilityBounds(geometric)
        val regularValues = allBounds.map(_._3).toSet
        regularValues should equal((1 to 7).toSet)
        for((lower, upper, value) <- allBounds) {
          // True probability of this value
          val pValue = math.pow(0.5, value)
          lower should be (pValue +- 0.000000001)
          upper should be ((pValue + pUnexpanded) +- 0.000000001)
        }
        alg.kill()
      }

      "produce the correct depth 6 expectation bounds for a bounded function" in {
        Universe.createNew()
        val geometric = recursiveGeometric()
        val alg = LazyStructuredVE(6, geometric)
        alg.start()
        // Unassigned probability at this expansion
        val pUnexpanded = math.pow(0.5, 7)

        val (lower, upper) = alg.expectationBounds(geometric, (i: Int) => 1.0 / i, Some(0.0), Some(1.0))
        val baseExpectation = (1 to 7).map(i => math.pow(0.5, i) / i).sum
        lower should be (baseExpectation +- 0.000000001)
        upper should be ((baseExpectation + pUnexpanded) +- 0.000000001)
        alg.kill()
      }
    }
    
    "given a recursive geometric with a condition" should {
      "produce the correct depth 6 probability bounds for each value" in {
        Universe.createNew()
        val geometric = recursiveGeometric()
        geometric.addCondition(_ % 2 == 0)
        val alg = LazyStructuredVE(6, geometric)
        alg.start()
        // Unassigned probability at this expansion
        val pUnexpanded = math.pow(0.5, 7)
        val normalizer = math.pow(0.5, 2) + math.pow(0.5, 4) + math.pow(0.5, 6) + pUnexpanded

        val allBounds = alg.allProbabilityBounds(geometric)
        val regularValues = allBounds.map(_._3).toSet
        regularValues should equal((1 to 7).toSet)
        for((lower, upper, value) <- allBounds) {
          if(value % 2 == 0) {
            // True prior probability of this value
            val pValue = math.pow(0.5, value)
            lower should be ((pValue / normalizer) +- 0.000000001)
            upper should be (((pValue + pUnexpanded) / normalizer) +- 0.000000001)
          }
          else {
            lower should be (0.0 +- 0.000000001)
            upper should be ((pUnexpanded / normalizer) +- 0.000000001)
          }
        }
        alg.kill()
      }

      "produce the correct depth 6 expectation bounds for a bounded function" in {
        Universe.createNew()
        val geometric = recursiveGeometric()
        geometric.addCondition(_ % 2 == 0)
        val alg = LazyStructuredVE(6, geometric)
        alg.start()
        // Unassigned probability at this expansion
        val pUnexpanded = math.pow(0.5, 7)
        val normalizer = math.pow(0.5, 2) + math.pow(0.5, 4) + math.pow(0.5, 6) + pUnexpanded

        val (lower, upper) = alg.expectationBounds(geometric, (i: Int) => 1.0 / i, Some(0.0), Some(1.0))
        val baseExpectation = (2 to 6 by 2).map(i => math.pow(0.5, i) / (i * normalizer)).sum
        lower should be (baseExpectation +- 0.000000001)
        upper should be ((baseExpectation + (pUnexpanded / normalizer)) +- 0.000000001)
        alg.kill()
      }
    }
    
    "given a recursive geometric with a constraint" should {
      "produce the correct depth 6 probability bounds for each value" in {
        Universe.createNew()
        val geometric = recursiveGeometric()
        geometric.addConstraint(1.0 / _)
        val alg = LazyStructuredVE(6, geometric)
        alg.start()
        // Unassigned probability at this expansion
        val pUnexpanded = math.pow(0.5, 7)
        val normalizer = (1 to 7).map(i => math.pow(0.5, i) / i).sum + pUnexpanded

        val allBounds = alg.allProbabilityBounds(geometric)
        val regularValues = allBounds.map(_._3).toSet
        regularValues should equal((1 to 7).toSet)
        for((lower, upper, value) <- allBounds) {
          val pValue = math.pow(0.5, value) / value
          lower should be ((pValue / normalizer) +- 0.000000001)
          upper should be (((pValue + pUnexpanded) / normalizer) +- 0.000000001)
        }
        alg.kill()
      }

      "produce the correct depth 6 expectation bounds for a bounded function" in {
        Universe.createNew()
        val geometric = recursiveGeometric()
        geometric.addConstraint(1.0 / _)
        val alg = LazyStructuredVE(6, geometric)
        alg.start()
        // Unassigned probability at this expansion
        val pUnexpanded = math.pow(0.5, 7)
        val normalizer = (1 to 7).map(i => math.pow(0.5, i) / i).sum + pUnexpanded

        val (lower, upper) = alg.expectationBounds(geometric, (i: Int) => 1.0 / i, Some(0.0), Some(1.0))
        val baseExpectation = (1 to 7).map(i => math.pow(0.5, i) / (i * i * normalizer)).sum
        lower should be (baseExpectation +- 0.000000001)
        upper should be ((baseExpectation + (pUnexpanded / normalizer)) +- 0.000000001)
        alg.kill()
      }
    }
  }

  "given a model with evidence on a partially expanded Chain" should {
    "produce the correct probability bounds for the parent" in {
      Universe.createNew()
      val parent = Select(0.4 -> 4, 0.3 -> 3, 0.2 -> 2, 0.1 -> 1)
      val chain = Chain(parent, (i: Int) => If(Flip(i / 10.0), Constant(i >= 3), If(Flip(0.5), Constant(true), Flip(0.5))))
      chain.observe(true)
      val alg = LazyStructuredVE(2, parent)
      alg.start()

      // Lower factor after elimination: 0.16 -> 4, 0.09 -> 3, 0.0 -> 2, 0.0 -> 1 (sum = 0.25)
      // Upper factor after elimination: 0.4 -> 4, 0.3 -> 3, 0.16 -> 2, 0.09 -> 1 (sum = 0.95)
      val bound4 = (0.16 / 0.95, 1 - 0.09 / 0.95, 4)
      val bound3 = (0.09 / 0.95, 1 - 0.16 / 0.95, 3)
      val bound2 = (0.0, 0.16 / 0.25, 2)
      val bound1 = (0.0, 0.09 / 0.25, 1)
      val actualBounds = List(bound4, bound3, bound2, bound1).sortBy(_._3)
      val computedBounds = alg.allProbabilityBounds(parent).toList.sortBy(_._3)
      for(((lowerA, upperA, valueA), (lowerC, upperC, valueC)) <- actualBounds.zip(computedBounds)) {
        valueC should equal(valueA)
        lowerC should be(lowerA +- 0.000000001)
        upperC should be(upperA +- 0.000000001)
      }
      alg.kill()
    }

    "produce the correct expectation bounds for the parent" in {
      Universe.createNew()
      val parent = Select(0.4 -> 4, 0.3 -> 3, 0.2 -> 2, 0.1 -> 1)
      val chain = Chain(parent, (i: Int) => If(Flip(i / 10.0), Constant(i >= 3), twoLevelChain()))
      chain.observe(true)
      val alg = LazyStructuredVE(2, parent)
      alg.start()

      // "Distributions" found by putting as much probability on the lowest and highest values, given the bounds above
      val lowerDist = List(0.09/0.25 -> 1, 1.0 - (0.09/0.25 + 0.25/0.95) -> 2, 0.09/0.95 -> 3, 0.16/0.95 -> 4)
      val upperDist = List(1 - 0.09/0.95 -> 4, 0.09/0.95 -> 3)
      val lowerActual = lowerDist.map(p => p._1 * p._2).sum
      val upperActual = upperDist.map(p => p._1 * p._2).sum
      val (lowerComputed, upperComputed) = alg.expectationBounds(parent, (i: Int) => i)
      lowerComputed should be(lowerActual +- 0.000000001)
      upperComputed should be(upperActual +- 0.000000001)
    }

    "return whichever of the two bounds is tightest" in {
      Universe.createNew()
      val parent = Flip(0.75)
      val chain: Element[Boolean] =
        If(parent,
          If(Flip(0.1 / 0.75),
            Constant(true),
            twoLevelChain()),
          If(Flip(0.84),
            Flip(0.2 / 0.21),
            twoLevelChain()))
      chain.observe(true)
      val alg = LazyStructuredVE(2, parent)
      alg.start()

      // Lower factor after elimination: 0.1 -> true, 0.2 -> false
      // Upper factor after elimination: 0.75 -> true, 0.24 -> false
      // Recall that there are two bounds: the bound itself, and 1 minus the sum of the opposite bounds
      // Both cases are tested here
      alg.probabilityBounds(parent, true)._1 should be(1.0 - 0.24 / (0.1 + 0.2) +- 0.000000001)
      alg.probabilityBounds(parent, true)._2 should be(1.0 - 0.2 / (0.75 + 0.24) +- 0.000000001)
      alg.probabilityBounds(parent, false)._1 should be(0.2 / (0.75 + 0.24) +- 0.000000001)
      alg.probabilityBounds(parent, false)._2 should be(0.24 / (0.1 + 0.2) +- 0.000000001)
      alg.kill()
    }
  }
}
