/*
 * VEBPStrategy.scala
 * An abstract class that defines how to solve a problem
 *
 * Created By:      Brian Ruttenberg (bruttenberg@cra.com)
 * Creation Date:   July 1, 2015
 *
 * Copyright 2015 Avrom J. Pfeffer and Charles River Analytics, Inc.
 * See http://www.cra.com or email figaro@cra.com for information.
 *
 * See http://www.github.com/p2t2/figaro for a copy of the software license.
 */
package com.cra.figaro.algorithm.structured.strategy.solve

import com.cra.figaro.algorithm.structured.{NestedProblem, Problem, solver}
import com.cra.figaro.algorithm.factored.factors.Factor
import com.cra.figaro.algorithm.factored.factors.Variable
import com.cra.figaro.algorithm.factored.VariableElimination

/**
 * A solving strategy that chooses between VE and BP based on a score of the elminiation order
 */
class VEBPStrategy(problem: Problem, raisingCriteria: RaisingCriteria, val scoreThreshold: Double, val iterations: Int)
  extends RaisingStrategy(problem, raisingCriteria) {

  override def eliminate(toEliminate: Set[Variable[_]], toPreserve: Set[Variable[_]], factors: List[Factor[Double]]): (List[Factor[Double]], Map[Variable[_], Factor[_]]) = {
    val (score, order) = VariableElimination.eliminationOrder(factors, toPreserve)
    if (score > scoreThreshold) {
      solver.marginalBeliefPropagation(iterations)(problem, toEliminate, toPreserve, factors)
    } else {
      solver.marginalVariableElimination(problem, toEliminate, toPreserve, factors)
    }
  }

  override def recurse(subproblem: NestedProblem[_]) = {
    if(subproblem.solved) None
    else Some(new VEBPStrategy(subproblem, raisingCriteria, scoreThreshold, iterations))
  }
  
}