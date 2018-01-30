/*
 * ApplyFactory.scala
 * Methods to create factors associated with Apply elements.
 *
 * Created By:      Glenn Takata (gtakata@cra.com)
 * Creation Date:   Dec 15, 2014
 *
 * Copyright 2017 Avrom J. Pfeffer and Charles River Analytics, Inc.
 * See http://www.cra.com or email figaro@cra.com for information.
 *
 * See http://www.github.com/p2t2/figaro for a copy of the software license.
 */

package com.cra.figaro.algorithm.factored.factors.factory

import com.cra.figaro.algorithm.PointMapper
import com.cra.figaro.algorithm.factored.factors._
import com.cra.figaro.algorithm.lazyfactored.{Regular, Star}
import com.cra.figaro.language._
import com.cra.figaro.algorithm.structured.ComponentCollection
import com.cra.figaro.library.compound.BooleanOperator

/**
 * A Sub-Factory for Apply Elements
 */
object ApplyFactory {

    /**
   * Factor constructor for an Apply Element that has one input
   */
  def makeFactors[T, U](cc: ComponentCollection, apply: Apply1[T, U])(implicit mapper: PointMapper[U]): List[Factor[Double]] = {
    val arg1Var = Factory.getVariable(cc, apply.arg1)
    val resultVar = Factory.getVariable(cc, apply)
    cc.variableParents(resultVar) += arg1Var
    val applyComponent = cc(apply)
    val applyMap = applyComponent.getMap
    val applyValues = applyComponent.range
    val factor = new SparseFactor[Double](List(arg1Var), List(resultVar))
    val arg1Indices = arg1Var.range.zipWithIndex
    for {
      (arg1Val, arg1Index) <- arg1Indices
    } {
      if (arg1Val.isRegular) {
        val resultVal = mapper.map(applyMap(arg1Val.value), applyValues.regularValues)
        val resultIndex = resultVar.range.indexWhere(xval => xval.isRegular && xval.value == resultVal)
        if (resultIndex >= 0) factor.set(List(arg1Index, resultIndex), resultVar.valueSet.normalizer)
      } else if (!arg1Val.isRegular) {
        val resultNotRegularIndex = resultVar.range.indexWhere(!_.isRegular)
        if (resultNotRegularIndex >= 0) factor.set(List(arg1Index, resultNotRegularIndex), 1.0)
      }
    }
    List(factor)
  }

  /**
   * Factor constructor for an Apply Element that has two inputs
   */
  def makeFactors[T1, T2, U](cc: ComponentCollection, apply: Apply2[T1, T2, U])(implicit mapper: PointMapper[U]): List[Factor[Double]] = {
    val arg1Var = Factory.getVariable(cc, apply.arg1)
    val arg2Var = Factory.getVariable(cc, apply.arg2)
    val resultVar = Factory.getVariable(cc, apply)
    cc.variableParents(resultVar) ++= Set(arg1Var, arg2Var)
    val applyComponent = cc(apply)
    val applyMap = applyComponent.getMap
    val applyValues = cc(apply).range
    val factor = new SparseFactor[Double](List(arg1Var, arg2Var), List(resultVar))
    val arg1Indices = arg1Var.range.zipWithIndex
    val arg2Indices = arg2Var.range.zipWithIndex
    for {
      (arg1Val, arg1Index) <- arg1Indices
      (arg2Val, arg2Index) <- arg2Indices
    } {
      if (arg1Val.isRegular && arg2Val.isRegular) {
        val resultVal = mapper.map(applyMap((arg1Val.value, arg2Val.value)), applyValues.regularValues)
        val resultIndex = resultVar.range.indexWhere(xval => xval.isRegular && xval.value == resultVal)
        if (resultIndex >= 0) factor.set(List(arg1Index, arg2Index, resultIndex), resultVar.valueSet.normalizer)
      } else if ((!arg1Val.isRegular || !arg2Val.isRegular)) {
        val resultNotRegularIndex = resultVar.range.indexWhere(!_.isRegular)
        if (resultNotRegularIndex >= 0) factor.set(List(arg1Index, arg2Index, resultNotRegularIndex), 1.0)
      }

    }
    List(factor)
  }

  /**
   * Factor constructor for an Apply Element that has three inputs
   */
  def makeFactors[T1, T2, T3, U](cc: ComponentCollection, apply: Apply3[T1, T2, T3, U])(implicit mapper: PointMapper[U]): List[Factor[Double]] = {
    val arg1Var = Factory.getVariable(cc, apply.arg1)
    val arg2Var = Factory.getVariable(cc, apply.arg2)
    val arg3Var = Factory.getVariable(cc, apply.arg3)
    val resultVar = Factory.getVariable(cc, apply)
    cc.variableParents(resultVar) ++= Set(arg1Var, arg2Var, arg3Var)
    val applyComponent = cc(apply)
    val applyMap = applyComponent.getMap
    val applyValues = cc(apply).range
    val factor = new SparseFactor[Double](List(arg1Var, arg2Var, arg3Var), List(resultVar))
    val arg1Indices = arg1Var.range.zipWithIndex
    val arg2Indices = arg2Var.range.zipWithIndex
    val arg3Indices = arg3Var.range.zipWithIndex
    for {
      (arg1Val, arg1Index) <- arg1Indices
      (arg2Val, arg2Index) <- arg2Indices
      (arg3Val, arg3Index) <- arg3Indices
    } {
      if (arg1Val.isRegular && arg2Val.isRegular && arg3Val.isRegular) {
        val resultVal = mapper.map(applyMap((arg1Val.value, arg2Val.value, arg3Val.value)), applyValues.regularValues)
        val resultIndex = resultVar.range.indexWhere(xval => xval.isRegular && xval.value == resultVal)
        if (resultIndex >= 0) factor.set(List(arg1Index, arg2Index, arg3Index, resultIndex), resultVar.valueSet.normalizer)
      } else if ((!arg1Val.isRegular || !arg2Val.isRegular || !arg3Val.isRegular)) {
        val resultNotRegularIndex = resultVar.range.indexWhere(!_.isRegular)
        if (resultNotRegularIndex >= 0) factor.set(List(arg1Index, arg2Index, arg3Index, resultNotRegularIndex), 1.0)
      }
    }
    List(factor)
  }

  /**
   * Factor constructor for an Apply Element that has four inputs
   */
  def makeFactors[T1, T2, T3, T4, U](cc: ComponentCollection, apply: Apply4[T1, T2, T3, T4, U])(implicit mapper: PointMapper[U]): List[Factor[Double]] = {
    val arg1Var = Factory.getVariable(cc, apply.arg1)
    val arg2Var = Factory.getVariable(cc, apply.arg2)
    val arg3Var = Factory.getVariable(cc, apply.arg3)
    val arg4Var = Factory.getVariable(cc, apply.arg4)
    val resultVar = Factory.getVariable(cc, apply)
    cc.variableParents(resultVar) ++= Set(arg1Var, arg2Var, arg3Var, arg4Var)
    val applyComponent = cc(apply)
    val applyMap = applyComponent.getMap
    val applyValues = cc(apply).range
    val factor = new SparseFactor[Double](List(arg1Var, arg2Var, arg3Var, arg4Var), List(resultVar))
    val arg1Indices = arg1Var.range.zipWithIndex
    val arg2Indices = arg2Var.range.zipWithIndex
    val arg3Indices = arg3Var.range.zipWithIndex
    val arg4Indices = arg4Var.range.zipWithIndex
    for {
      (arg1Val, arg1Index) <- arg1Indices
      (arg2Val, arg2Index) <- arg2Indices
      (arg3Val, arg3Index) <- arg3Indices
      (arg4Val, arg4Index) <- arg4Indices
    } {
      if (arg1Val.isRegular && arg2Val.isRegular && arg3Val.isRegular && arg4Val.isRegular) {
        val resultVal = mapper.map(applyMap((arg1Val.value, arg2Val.value, arg3Val.value, arg4Val.value)), applyValues.regularValues)
        val resultIndex = resultVar.range.indexWhere(xval => xval.isRegular && xval.value == resultVal)
        if (resultIndex >= 0) factor.set(List(arg1Index, arg2Index, arg3Index, arg4Index, resultIndex), resultVar.valueSet.normalizer)
      } else if ((!arg1Val.isRegular || !arg2Val.isRegular || !arg3Val.isRegular || !arg4Val.isRegular)) {
        val resultNotRegularIndex = resultVar.range.indexWhere(!_.isRegular)
        if (resultNotRegularIndex >= 0) factor.set(List(arg1Index, arg2Index, arg3Index, arg4Index, resultNotRegularIndex), 1.0)
      }
    }
    List(factor)
  }

  /**
   * Factor constructor for an Apply Element that has five inputs
   */
  def makeFactors[T1, T2, T3, T4, T5, U](cc: ComponentCollection, apply: Apply5[T1, T2, T3, T4, T5, U])(implicit mapper: PointMapper[U]): List[Factor[Double]] = {
    val arg1Var = Factory.getVariable(cc, apply.arg1)
    val arg2Var = Factory.getVariable(cc, apply.arg2)
    val arg3Var = Factory.getVariable(cc, apply.arg3)
    val arg4Var = Factory.getVariable(cc, apply.arg4)
    val arg5Var = Factory.getVariable(cc, apply.arg5)
    val resultVar = Factory.getVariable(cc, apply)
    cc.variableParents(resultVar) ++= Set(arg1Var, arg2Var, arg3Var, arg4Var, arg5Var)
    val applyComponent = cc(apply)
    val applyMap = applyComponent.getMap
    val applyValues = cc(apply).range
    val factor = new SparseFactor[Double](List(arg1Var, arg2Var, arg3Var, arg4Var, arg5Var), List(resultVar))
    val arg1Indices = arg1Var.range.zipWithIndex
    val arg2Indices = arg2Var.range.zipWithIndex
    val arg3Indices = arg3Var.range.zipWithIndex
    val arg4Indices = arg4Var.range.zipWithIndex
    val arg5Indices = arg5Var.range.zipWithIndex
    for {
      (arg1Val, arg1Index) <- arg1Indices
      (arg2Val, arg2Index) <- arg2Indices
      (arg3Val, arg3Index) <- arg3Indices
      (arg4Val, arg4Index) <- arg4Indices
      (arg5Val, arg5Index) <- arg5Indices
    } {
      if (arg1Val.isRegular && arg2Val.isRegular && arg3Val.isRegular && arg4Val.isRegular && arg5Val.isRegular) {
        val resultVal = mapper.map(applyMap((arg1Val.value, arg2Val.value, arg3Val.value, arg4Val.value, arg5Val.value)), applyValues.regularValues)
        val resultIndex = resultVar.range.indexWhere(xval => xval.isRegular && xval.value == resultVal)
        if (resultIndex >= 0) factor.set(List(arg1Index, arg2Index, arg3Index, arg4Index, arg5Index, resultIndex), resultVar.valueSet.normalizer)
      } else if ((!arg1Val.isRegular || !arg2Val.isRegular || !arg3Val.isRegular || !arg4Val.isRegular || !arg5Val.isRegular)) {
        val resultNotRegularIndex = resultVar.range.indexWhere(!_.isRegular)
        if (resultNotRegularIndex >= 0) factor.set(List(arg1Index, arg2Index, arg3Index, arg4Index, arg5Index, resultNotRegularIndex), 1.0)
      }
    }
    List(factor)
  }

  def makeBooleanFactors(cc: ComponentCollection, bool: BooleanOperator): List[Factor[Double]] = {
    val arg1Var = Factory.getVariable(cc, bool.arg1)
    val arg2Var = Factory.getVariable(cc, bool.arg2)
    val resultVar = Factory.getVariable(cc, bool)
    cc.variableParents(resultVar) ++= Set(arg1Var, arg2Var)
    val factor = new SparseFactor[Double](List(arg1Var, arg2Var), List(resultVar))
    val arg1Indices = arg1Var.range.zipWithIndex
    val arg2Indices = arg2Var.range.zipWithIndex
    for {
      (arg1Val, arg1Index) <- arg1Indices
      (arg2Val, arg2Index) <- arg2Indices
    } {
      val resultVal = bool.extendedFn(arg1Val, arg2Val)
      val resultIndex = resultVar.range.indexOf(resultVal)
      factor.set(List(arg1Index, arg2Index, resultIndex), 1.0)
    }
    List(factor)
  }
}
