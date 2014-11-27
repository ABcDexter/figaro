package com.cra.figaro.library.process

import com.cra.figaro.language._

class ContainerElement[Index, Value](val element: Element[Container[Index, Value]])
{
  /**
   * Creates an element whose value is the value at the corresponding index of the value of the process element.
   */
  def apply(i: Index): Element[Value] = {
    Chain(element, (c: Container[Index, Value]) => c(i))
  }

  /**
   * Creates an element whose value is the value at the corresponding index in the value of the container element,
   * if the index is in range, None otherwise.
   */
  def get(i: Index): Element[Option[Value]] = {
    Chain(element, (c: Container[Index, Value]) => c.get(i))
  }

  /**
   * Map the given function pointwise through the value of the container element.
   */
  def map[Value2](f: Value => Value2): ContainerElement[Index, Value2] = {
    new ContainerElement(Apply(element, (c: Container[Index, Value]) => c.map(f)))
  }

  /**
   * Chain the given function pointwise through the value of the container element.
   */
  def chain[Value2](f: Value => Element[Value2]): ContainerElement[Index, Value2] = {
    new ContainerElement(Apply(element, (c: Container[Index, Value]) => c.chain(f)))
  }

  /**
   * Produce the element over values obtained by selecting a particular container and folding through its values.
   */
  def foldLeft[Value2](start: Value2)(f: (Value2, Value) => Value2): Element[Value2] = {
    CachingChain(element, (c: Container[Index, Value]) => c.foldLeft(start)(f))
  }

  /**
   * Produce the element over values obtained by selecting a particular container and folding through its values.
   */
  def foldRight[Value2](start: Value2)(f: (Value, Value2) => Value2): Element[Value2] = {
    CachingChain(element, (c: Container[Index, Value]) => c.foldRight(start)(f))
  }

  /**
   * Produce the element over values obtained by selecting a particular container and reducing through its values.
   */
  def reduce(f: (Value, Value) => Value): Element[Value] = {
    CachingChain(element, (c: Container[Index, Value]) => c.reduce(f))
  }

  /**
   * Aggregate the results of applying an operator to each element.
   */
  def aggregate[Value2](start: => Value2)(seqop: (Value2, Value) => Value2, combop: (Value2, Value2) => Value2): Element[Value2] = {
    foldLeft(start)((v1: Value2, v2: Value) => combop(v1, seqop(v1, v2)))
  }

  /**
   * Returns an element representing the number of elements in the container whose values satisfy the predicate.
   */
  def count(f: (Value) => Boolean): Element[Int] = {
    foldLeft(0)((i: Int, v: Value) => if (f(v)) i + 1 else i)
  }

  /**
   * Returns an element representing whether the value of any element in the container satisfies the predicate.
   */
  def exists(pred: Value => Boolean): Element[Boolean] = {
    foldLeft(false)((b: Boolean, v: Value) => pred(v) || b)
  }

  /**
   * Returns an element representing whether the values of all elements in the container satisfy the predicate.
   */
  def forall(pred: Value => Boolean): Element[Boolean] = {
    foldLeft(true)((b: Boolean, v: Value) => pred(v) && b)
  }

  /**
   * Returns an element representing the length of the container.
   */
  def length: Element[Int] = {
    foldLeft(0)((i: Int, v: Value) => i + 1)
  }

  /**
   * Concatenate the value of this container element with the value of another container element.
   */
  def concat[Index2](that: ContainerElement[Index2, Value]): ContainerElement[Int, Value] = {
    new ContainerElement(Apply(this.element, that.element, (c1: Container[Index, Value], c2: Container[Index2, Value]) => c1.concat(c2)))
  }
}
