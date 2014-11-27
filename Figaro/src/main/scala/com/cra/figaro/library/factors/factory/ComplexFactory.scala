/**
 *
 */
package com.cra.figaro.library.factors.factory

import com.cra.figaro.algorithm.lazyfactored._
import com.cra.figaro.language.{ Aggregate, ElementCollection, MultiValuedReferenceElement, SingleValuedReferenceElement }
import com.cra.figaro.library.compound._
import com.cra.figaro.library.factors._
/**
 * @author gtakata
 *
 */
object ComplexFactory {

  def makeFactors[T](element: SingleValuedReferenceElement[T]): List[Factor[Double]] = {
    val (first, rest) = element.collection.getFirst(element.reference)
    rest match {
      case None =>
        val elementVar = Variable(element)
        val firstVar = Variable(first)
        val factor = new BasicFactor[Double](List(firstVar), List(elementVar))
        for {
          i <- 0 until firstVar.range.size
          j <- 0 until elementVar.range.size
        } {
          factor.set(List(i, j), (if (i == j) 1.0; else 0.0))
        }
        List(factor)
      case Some(restRef) =>
        val firstVar = Variable(first)
        val selectedFactors =
          for {
            (firstXvalue, firstIndex) <- firstVar.range.zipWithIndex
            firstCollection = firstXvalue.value.asInstanceOf[ElementCollection]
            restElement = element.embeddedElements(firstCollection)
          } yield {
            Factory.makeConditionalSelector(element, firstVar, firstIndex, Variable(restElement)) :: makeFactors(restElement)
          }
        selectedFactors.flatten
    }
  }

  def makeFactors[T](element: MultiValuedReferenceElement[T]): List[Factor[Double]] = {
    val (first, rest) = element.collection.getFirst(element.reference)
    val selectionFactors: List[List[Factor[Double]]] = {
      rest match {
        case None =>
          val elementVar = Variable(element)
          val firstVar = Variable(first)
          val factor = new BasicFactor[Double](List(firstVar), List(elementVar))
          for {
            i <- 0 until firstVar.range.size
            j <- 0 until elementVar.range.size
          } {
            factor.set(List(i, j), (if (i == j) 1.0; else 0.0))
          }
          List(List(factor))
        case Some(restRef) =>
          val firstVar = Variable(first)
          for {
            (firstXvalue, firstIndex) <- firstVar.range.zipWithIndex
          } yield {
            if (firstXvalue.isRegular) {
              firstXvalue.value match {
                case firstCollection: ElementCollection =>
                  val restElement = element.embeddedElements(firstCollection)
                  val result: List[Factor[Double]] =
                    Factory.makeConditionalSelector(element, firstVar, firstIndex, Variable(restElement)) :: Factory.make(restElement)
                  result
                case cs: Traversable[_] =>
                  // Create a multi-valued reference element (MVRE) for each collection in the value of the first name.
                  // Since the first name is multi-valued, its value is the union of the values of all these MVREs.
                  val collections = cs.asInstanceOf[Traversable[ElementCollection]].toList.distinct // Set semantics
                  val multis: List[MultiValuedReferenceElement[T]] = collections.map(element.embeddedElements(_)).toList
                  // Create the element that takes the union of the values of the all the MVREs.
                  // The combination and setMaker elements are encapsulated within this object and are created now, so we need to create factors for them. 
                  // Finally, we create a conditional selector (see ProbFactor) to select the appropriate result value when the first
                  // name's value is these MVREs.
                  val combination = element.embeddedInject(collections)
                  val setMaker = element.embeddedApply(collections)
                  val result: List[Factor[Double]] =
                    Factory.makeConditionalSelector(element, firstVar, firstIndex, Variable(setMaker)) :: Factory.make(combination) :::
                      Factory.make(setMaker)
                  result
              }
            } else StarFactory.makeStarFactor(element)
          }
      }
    }
    selectionFactors.flatten
  }

  def makeFactors[T, U](element: Aggregate[T, U]): List[Factor[Double]] = {
    val elementVar = Variable(element)
    val mvreVar = Variable(element.mvre)
    val factor = new BasicFactor[Double](List(mvreVar), List(elementVar))
    for {
      (mvreXvalue, mvreIndex) <- mvreVar.range.zipWithIndex
      (elementXvalue, elementIndex) <- elementVar.range.zipWithIndex
    } {
      if (elementXvalue.isRegular && mvreXvalue.isRegular) factor.set(List(mvreIndex, elementIndex), if (element.aggregate(mvreXvalue.value) == elementXvalue.value) 1.0; else 0.0)
    }
    // The MultiValuedReferenceElement for this aggregate is generated when values is called. 
    // Therefore, it will be included in the expansion and have factors made for it automatically, so we do not create factors for it here.
    List(factor)
  }
  
  def makeFactors[T](element: MakeList[T]): List[Factor[Double]] = {
    val parentVar = Variable(element.numItems)
    // We need to create factors for the items and the lists themselves, which are encapsulated in this MakeList
    val regularParents = parentVar.range.filter(_.isRegular).map(_.value)
    val maxItem = regularParents.reduce(_ max _)
    val itemFactors = List.tabulate(maxItem)((i: Int) => Factory.make(element.items(i)))
    val indexedResultElemsAndFactors =
      for { i <- regularParents } yield {
        val elem = element.embeddedInject(i)
        val factors = Factory.make(elem)
        (Regular(i), elem, factors)
      }
    val conditionalFactors =
      parentVar.range.zipWithIndex map (pair =>
        Factory.makeConditionalSelector(element, parentVar, pair._2, Variable(indexedResultElemsAndFactors.find(_._1 == pair._1).get._2)))
    conditionalFactors ::: itemFactors.flatten ::: indexedResultElemsAndFactors.flatMap(_._3)
  }
}