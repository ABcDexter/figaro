/*
 * RecursionDepthStrategy.scala
 * Strategies that refine lazily to a fixed recursion depth.
 *
 * Created By:      William Kretschmer (kretsch@mit.edu)
 * Creation Date:   Aug 07, 2017
 *
 * Copyright 2017 Avrom J. Pfeffer and Charles River Analytics, Inc.
 * See http://www.cra.com or email figaro@cra.com for information.
 *
 * See http://www.github.com/p2t2/figaro for a copy of the software license.
 */
package com.cra.figaro.algorithm.structured.strategy.refine

import com.cra.figaro.algorithm.structured._
import com.cra.figaro.language._

/**
 * Strategies that refine lazily to a fixed recursion depth, as defined in `ComponentCollection`. That is, they refine
 * any component belonging to a problem for which the recursion depth associated with that subproblem is at most the
 * depth given.
 *
 * This strategy has the advantage that it guarantees no need for backtracking, because components are never visited
 * more than once. This is made possible by the fact that the depth of a component is known at the time of expansion;
 * i.e. it cannot change. However, this strategy is not universal: it may fail when a model uses a recursive process for
 * which the associated component collection fails to increment the depth. For example, if one uses a recursive model
 * without Chain function memoization and uses a `SelectiveIncrementingCollection`, the collection will not increment
 * the depth at so-called recursive calls. A `RecursionDepthStrategy` running on such a model will fail to terminate.
 * @param problem Problem to refine.
 * @param initialComponents Components from which to begin the bottom-up refining process. Often, these are the set of
 * targets and evidence elements of a top-level problem. Refining proceeds by recursively refining the arguments of a
 * component before generating the range of the component. This continues until the maximum recursion depth is reached.
 * @param maxDepth Nonnegative maximum recursion depth for recursive subproblems. Defaults to `Int.MaxValue` for
 * expansion of the entire model (does not terminate on infinite models).
 */
class RecursionDepthStrategy(problem: Problem, override val initialComponents: Traversable[ProblemComponent[_]],
                             maxDepth: Int = Int.MaxValue) extends DepthFirstStrategy(problem.collection) {

  /**
   * Get the recursion depth associated with a problem, according to the collection.
   */
  protected def recursionDepth(problem: Problem): Int = problem match {
    case np: NestedProblem[_] => collection.problemToExpansion(np)._2
    case _ => 0
  }

  // Add the component if it is in the collection
  override def checkArg[T](element: Element[T]) = {
    if(collection.contains(element)) collection(element)
    else problem.add(element)
  }

  // Refine any component not fully-refined that is associated with a problem below the given depth
  override def shouldRefine(comp: ProblemComponent[_]): Boolean = {
    !comp.fullyRefined && recursionDepth(comp.problem) <= maxDepth
  }

  override def processChain[P, V](chainComp: ChainComponent[P, V]): Unit = {
    // Decompose the parent to get values for expansion
    val parentComp = checkArg(chainComp.chain.parent)
    refine(parentComp)
    // Observe: we do not backtrack because we never visit a component multiple times
    // Thus, it is safe to get all of the parent values and expand all of the subproblems once
    chainComp.expand()
    val subproblems = for(parentValue <- parentComp.range.regularValues) yield chainComp.subproblems(parentValue)
    // Refine the target of each existing subproblem
    for(subproblem <- subproblems) {
      refine(checkArg(subproblem.target))
    }
    // Make range based on the refinement of the subproblems
    generateRange(chainComp)
    // The range for this component is complete if the range of the parent is complete (and therefore no further
    // subproblems can be created), and the target for each subproblem has a complete range
    chainComp.fullyEnumerated =
      // It is safe to call collection(sp.target) because the target will have been added to the collection
      parentComp.fullyEnumerated && subproblems.forall { sp => collection(sp.target).fullyEnumerated }
    // If all components in the subproblems are fully refined, then the chain component is also fully refined
    chainComp.fullyRefined = chainComp.fullyEnumerated && subproblems.forall(_.fullyRefined)
  }

  override def processMakeArray[V](maComp: MakeArrayComponent[V]): Unit = {
    // Decompose the number of items component to get the maximum number of expansions
    val numItemsComp = checkArg(maComp.makeArray.numItems)
    refine(numItemsComp)
    // Observe: we do not backtrack because we never visit a component multiple times
    // Thus, it is safe to get the maximum of the parent values and expand the subproblems once
    maComp.expand()

    // All item components used in the generation of this MakeArray, newly expanded or otherwise
    val itemComps = for(item <- maComp.makeArray.items.take(maComp.maxExpanded)) yield checkArg(item)
    // Refine each of the items
    itemComps.foreach(refine)
    // Make range based on the ranges of the items
    generateRange(maComp)

    // The range for this component is complete if the number of items and each item have complete ranges
    // This also implies that the component is fully refined because there are no subproblems
    maComp.fullyEnumerated = numItemsComp.fullyEnumerated && itemComps.forall(_.fullyEnumerated)
    maComp.fullyRefined = maComp.fullyEnumerated
  }
}
