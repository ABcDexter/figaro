/*
 * LazyStructuredMarginalMAP.scala
 * Lazy SFI algorithms that compute extended marginal MAP values.
 *
 * Created By:      William Kretschmer (kretsch@mit.edu)
 * Creation Date:   Jan 09, 2017
 *
 * Copyright 2017 Avrom J. Pfeffer and Charles River Analytics, Inc.
 * See http://www.cra.com or email figaro@cra.com for information.
 *
 * See http://www.github.com/p2t2/figaro for a copy of the software license.
 */
package com.cra.figaro.experimental.marginalmap

import com.cra.figaro.algorithm._
import com.cra.figaro.algorithm.lazyfactored.{Extended, Star}
import com.cra.figaro.language._
import akka.pattern.ask

trait LazyStructuredMarginalMAP extends StructuredMarginalMAPAlgorithm {
  /**
   * Return an estimate of the most likely extended value of the given element. Returns a regular value if the bounds
   * are strong enough to prove the existence of a MAP, and * otherwise.
   * Particular algorithms must implement this method.
   */
  def computeMostLikelyExtendedValue[T](target: Element[T]): Extended[T]

  /**
   * Helper method implemented in OneTime and Anytime versions of this algorithm.
   */
  protected def doMostLikelyExtendedValue[T](target: Element[T]): Extended[T]

  /**
   * Return an estimate of the most likely extended value of the given element. Returns a regular value if the bounds
   * are strong enough to prove the existence of a MAP, and * otherwise.
   * Throws NotATargetException if the target is not a MAP element.
   * Throws AlgorithmInactiveException if the algorithm is inactive.
   */
  def mostLikelyExtendedValue[T](target: Element[T]): Extended[T] = {
    check(target)
    doMostLikelyExtendedValue(target)
  }
}

trait OneTimeLazyStructuredMarginalMAP extends LazyStructuredMarginalMAP with OneTimeStructuredMarginalMAP {
  override protected def doMostLikelyExtendedValue[T](target: Element[T]): Extended[T] = {
    computeMostLikelyExtendedValue(target)
  }
}

trait AnytimeLazyStructuredMarginalMAP extends LazyStructuredMarginalMAP with AnytimeStructuredMarginalMAP {
  /**
   * A message instructing the handler to compute the extended MPE of the target.
   */
  case class ComputeMostLikelyExtendedValue[T](target: Element[T]) extends Service

  /**
   * A message from the handler containing the extended MPE of the target.
   */
  case class MostLikelyExtendedValue[T](extended: Extended[T]) extends Response

  override def handle(service: Service): Response =
    service match {
      case ComputeMostLikelyValue(target) =>
        MostLikelyValue(mostLikelyValue(target))
      case ComputeMostLikelyExtendedValue(target) =>
        MostLikelyExtendedValue(computeMostLikelyExtendedValue(target))
    }

  override protected def doMostLikelyExtendedValue[T](target: Element[T]): Extended[T] = {
    awaitResponse(runner ? Handle(ComputeMostLikelyExtendedValue(target)), messageTimeout.duration) match {
      case MostLikelyExtendedValue(extended) => extended.asInstanceOf[Extended[T]]
      case _ => Star()
    }
  }
}
