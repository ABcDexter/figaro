/**
 *
 */
package com.cra.figaro.library.factors.factory

import com.cra.figaro.language._
import com.cra.figaro.library.atomic.discrete._
import com.cra.figaro.library.factors._
import com.cra.figaro.algorithm.lazyfactored._
/**
 * @author gtakata
 *
 */
object DistributionFactory {
  def makeFactors(flip: AtomicFlip): List[Factor[Double]] = {
    val flipVar = Variable(flip)
    if (flipVar.range.exists(!_.isRegular)) {
      assert(flipVar.range.size == 1) // Flip's range must either be {T,F} or {*}
      StarFactory.makeStarFactor(flip)
    } else {
      val factor = new BasicFactor[Double](List(), List(flipVar))
      val i = flipVar.range.indexOf(Regular(true))
      factor.set(List(i), flip.prob)
      factor.set(List(1 - i), 1.0 - flip.prob)
      List(factor)
    }
  }

  def makeFactors(flip: CompoundFlip): List[Factor[Double]] = {
    val flipVar = Variable(flip)
    if (flipVar.range.exists(!_.isRegular)) {
      assert(flipVar.range.size == 1) // Flip's range must either be {T,F} or {*}
      StarFactory.makeStarFactor(flip)
    } else {
      val probVar = Variable(flip.prob)
      val factor = new BasicFactor[Double](List(probVar), List(flipVar))
      val parentVals = probVar.range
      val i = flipVar.range.indexOf(Regular(true))
      for { j <- 0 until parentVals.size } {
        if (parentVals(j).isRegular) {
          val value = parentVals(j).value
          factor.set(List(j, i), value)
          factor.set(List(j, 1 - i), 1.0 - value)
        } else {
          factor.set(List(j, 0), 0.0)
          factor.set(List(j, 1), 0.0)
        }
      }
      List(factor)
    }
  }
  
  def makeFactors(flip: ParameterizedFlip): List[Factor[Double]] = {
    val flipVar = Variable(flip)
    val factor = new BasicFactor[Double](List(),List(flipVar))
    val prob = flip.parameter.MAPValue
    val i = flipVar.range.indexOf(Regular(true))
    factor.set(List(i), prob)
    factor.set(List(1 - i), 1.0 - prob)
    List(factor)
  }
  
  def makeFactors(binomial: AtomicBinomial): List[Factor[Double]] = {
      val binVar = Variable(binomial)
      val factor = new BasicFactor[Double](List(), List(binVar))
      for { (xvalue, index) <- binVar.range.zipWithIndex } {
        factor.set(List(index), binomial.density(xvalue.value))
      }
    List(factor)
  }

}