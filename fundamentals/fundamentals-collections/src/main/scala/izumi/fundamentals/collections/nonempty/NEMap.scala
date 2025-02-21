package izumi.fundamentals.collections.nonempty

// shameless copypaste from Scalactic

import scala.collection.{Iterable, Seq, mutable}
import scala.collection.mutable.{ArrayBuffer, Buffer}
import scala.reflect.ClassTag
import scala.collection.compat.*
import scala.language.implicitConversions

// Can't be a LinearSeq[T] because Builder would be able to create an empty one.
/**
  * A non-empty map: an ordered, immutable, non-empty collection of key-value tuples with <code>LinearSeq</code> performance characteristics.
  *
  * <p>
  * The purpose of <code>NEMap</code> is to allow you to express in a type that a <code>Map</code> is non-empty, thereby eliminating the
  * need for (and potential exception from) a run-time check for non-emptiness. For a non-empty sequence with <code>IndexedSeq</code>
  * performance, see <a href="Vector.html"><code>Vector</code></a>.
  * </p>
  *
  * <h2>Constructing <code>NEMap</code>s</h2>
  *
  * <p>
  * You can construct a <code>NEMap</code> by passing one or more elements to the <code>NEMap.apply</code> factory method:
  * </p>
  *
  * <pre class="stHighlight">
  * scala&gt; NEMap(1 -> "one", 2 -> "two", 3 -> "three")
  * res0: org.scalactic.anyvals.NEMap[Int, String] = NEMap(1 -> "one", 2 -> "two", 3 -> "three")
  * </pre>
  *
  * <h2>Working with <code>NEMap</code>s</h2>
  *
  * <p>
  * <code>NEMap</code> does not extend Scala's <code>Map</code> or <code>Traversable</code> traits because these require that
  * implementations may be empty. For example, if you invoke <code>tail</code> on a <code>Seq</code> that contains just one element,
  * you'll get an empty <code>Seq</code>:
  * </p>
  *
  * <pre class="stREPL">
  * scala&gt; Map(1 -> "one").tail
  * res6: Map[Int] = Map()
  * </pre>
  *
  * <p>
  * On the other hand, many useful methods exist on <code>Map</code> that when invoked on a non-empty <code>Seq</code> are guaranteed
  * to not result in an empty <code>Map</code>. For convenience, <code>NEMap</code> defines a method corresponding to every such <code>Map</code>
  * method. Here are an example:
  * </p>
  *
  * <pre class="stHighlight">
  * NEMap(1 -> "one", 2 -> "two", 3 -> "three").map(t => (t._1 + 1, t._2))                        // Result: NEMap(2 -> "one", 3 -> "two", 4 -> "three")
  * </pre>
  *
  * <p>
  * <code>NEMap</code> does <em>not</em> currently define any methods corresponding to <code>Map</code> methods that could result in
  * an empty <code>Map</code>. However, an implicit converison from <code>NEMap</code> to <code>Map</code>
  * is defined in the <code>NEMap</code> companion object that will be applied if you attempt to call one of the missing methods. As a
  * result, you can invoke <code>filter</code> on an <code>NEMap</code>, even though <code>filter</code> could result
  * in an empty map&mdash;but the result type will be <code>Map</code> instead of <code>NEMap</code>:
  * </p>
  *
  * <pre class="stHighlight">
  * NEMap(1 -> "one", 2 -> "two", 3 -> "three").filter(_._1 &lt; 10) // Result: Map(1 -> "one", 2 -> "two", 3 -> "three")
  * NEMap(1 -> "one", 2 -> "two", 3 -> "three").filter(_._ 1&gt; 10) // Result: Map()
  * </pre>
  *
  * <p>
  * You can use <code>NEMap</code>s in <code>for</code> expressions. The result will be an <code>NEMap</code> unless
  * you use a filter (an <code>if</code> clause). Because filters are desugared to invocations of <code>filter</code>, the
  * result type will switch to a <code>Map</code> at that point. Here are some examples:
  * </p>
  *
  * <pre class="stREPL">
  * scala&gt; import org.scalactic.anyvals._
  * import org.scalactic.anyvals._
  *
  * scala&gt; for ((i, j) &lt;- NEMap(1 -> "one", 2 -> "two", 3 -> "three")) yield (i + 1, j)
  * res0: org.scalactic.anyvals.NEMap[Int, String] = NEMap(2 -> "one", 3 -> "two", 4 -> "three")
  *
  * scala&gt; for ((i, j) &lt;- NEMap(1, 2, 3) if i &lt; 10) yield (i + 1, j)
  * res1: Map[Int, String] = Map(2 -> "one", 3 -> "two", 4 -> "three")
  * </pre>
  *
  * @tparam K the type of key contained in this <code>NEMap</code>
  * @tparam V the type of value contained in this <code>NEMap</code>
  */
final class NEMap[K, +V] private (val toMap: Map[K, V]) extends AnyVal {

  def keySet: NESet[K] = NESet.unsafeFrom(toMap.keySet)

  /**
    * Returns a new <code>NEMap</code> containing the entries of this <code>NEMap</code> and the entries of the passed <code>NEMap</code>.
    * The entry type of the resulting <code>NEMap</code> is the most specific superclass encompassing the entry types of this and the passed <code>NEMap</code>.
    *
    * @tparam V1 the value type of the returned <code>NEMap</code>
    * @param other the <code>NEMap</code> to append
    * @return a new <code>NEMap</code> that contains all the elements of this <code>NEMap</code> and all elements of <code>other</code>.
    */
  def ++[V1 >: V](other: NEMap[K, V1]): NEMap[K, V1] = new NEMap(toMap ++ other.toMap)

  /**
    * Returns a new <code>NEMap</code> containing the entries of this <code>NEMap</code> and the entries of the passed <code>Vector</code>.
    * The entry type of the resulting <code>NEMap</code> is the most specific superclass encompassing the entry types of this <code>NEMap</code> and the passed <code>Vector</code>.
    *
    * @tparam V1 the value type of the returned <code>NEMap</code>
    * @param other the <code>Vector</code> to append
    * @return a new <code>NEMap</code> that contains all the entries of this <code>NEMap</code> and all elements of <code>other</code>.
    */
  def ++[V1 >: V](other: Vector[(K, V1)]): NEMap[K, V1] = new NEMap(toMap ++ other.toVector)

  // TODO: Have I added these extra ++, etc. methods to Vector that take a NEMap?

  /**
    * Returns a new <code>NEMap</code> containing the entries of this <code>NEMap</code> and the entries of the passed <code>TraversableOnce</code>.
    * The entry type of the resulting <code>NEMap</code> is the most specific superclass encompassing the entry types of this <code>NEMap</code>
    * and the passed <code>TraversableOnce</code>.
    *
    * @tparam V1 the value type of the returned <code>NEMap</code>
    * @param other the <code>TraversableOnce</code> to append
    * @return a new <code>NEMap</code> that contains all the elements of this <code>NEMap</code> followed by all elements of <code>other</code>.
    */
  def ++[V1 >: V](other: IterableOnce[(K, V1)]): NEMap[K, V1] =
    if (other.iterator.isEmpty) this else new NEMap(toMap ++ other.iterator.toMap)

  /**
    * Returns a new <code>NEMap</code> with the given entry added.
    *
    * <p>
    * Note that :-ending operators are right associative. A mnemonic for <code>+:</code> <em>vs.</em> <code>:+</code> is: the COLon goes on the COLlection side.
    * </p>
    *
    * @param entry the element to add to this <code>NEMap</code>
    * @return a new <code>NEMap</code> consisting of <code>element</code> followed by all elements of this <code>NEMap</code>.
    */
  def +:[V1 >: V](entry: (K, V1)): NEMap[K, V1] = new NEMap(toMap + entry)

  /**
    * As with <code>++</code>, returns a new <code>NEMap</code> containing the entries of this <code>NEMap</code> and the entries of the passed <code>NEMap</code>.
    * The entry type of the resulting <code>NEMap</code> is the most specific superclass encompassing the entry types of this and the passed <code>NEMap</code>.
    *
    * It differs from <code>++</code> in that the right operand determines the type of the resulting collection rather than the left one. Mnemonic: the COLon is on the side of the new COLlection type.
    *
    * @tparam V1 the value type of the returned <code>NEMap</code>
    * @param other the <code>NEMap</code> to add
    * @return a new <code>NEMap</code> that contains all the elements of this <code>NEMap</code> and all elements of <code>other</code>.
    */
  def ++:[V1 >: V](other: NEMap[K, V1]): NEMap[K, V1] = new NEMap(toMap ++ other.toMap)

  /**
    * As with <code>++</code>, returns a new <code>NEMap</code> containing the entries of this <code>NEMap</code> and the entries of the passed <code>Vector</code>.
    * The entry type of the resulting <code>NEMap</code> is the most specific superclass encompassing the entry types of this <code>NEMap</code> and the passed <code>Vector</code>.
    *
    * It differs from <code>++</code> in that the right operand determines the type of the resulting collection rather than the left one. Mnemonic: the COLon is on the side of the new COLlection type.
    *
    * @tparam V1 the value type of the returned <code>NEMap</code>
    * @param other the <code>Vector</code> to append
    * @return a new <code>NEMap</code> that contains all the entries of this <code>NEMap</code> and all elements of <code>other</code>.
    */
  def ++:[V1 >: V](other: Vector[(K, V1)]): NEMap[K, V1] = new NEMap(toMap ++ other.toVector)

  /**
    * Returns a new <code>NEMap</code> containing the entries of this <code>NEMap</code> and the entries of the passed <code>TraversableOnce</code>.
    * The entry type of the resulting <code>NEMap</code> is the most specific superclass encompassing the entry types of this <code>NEMap</code>
    * and the passed <code>TraversableOnce</code>.
    *
    * @tparam V1 the value type of the returned <code>NEMap</code>
    * @param other the <code>TraversableOnce</code> to append
    * @return a new <code>NEMap</code> that contains all the elements of this <code>NEMap</code> followed by all elements of <code>other</code>.
    */
  def ++:[V1 >: V](other: IterableOnce[(K, V1)]): NEMap[K, V1] =
    if (other.iterator.isEmpty) this else new NEMap(toMap ++ other.iterator.toMap)

  /**
    * Returns a new <code>NEMap</code> with the given entry added.
    *
    * @param entry the entry to add to this <code>NEMap</code>
    * @return a new <code>NEMap</code> consisting of all entries of this <code>NEMap</code> and <code>entry</code>.
    */
  def +[V1 >: V](entry: (K, V1)): NEMap[K, V1] = new NEMap(toMap + entry)

  /**
    * Returns a new <code>NEMap</code> with the given entries added.
    *
    * @param entries the entries to add to this <code>NEMap</code>
    * @return a new <code>NEMap</code> consisting of all entries of this <code>NEMap</code> and <code>entries</code>.
    */
  def +[V1 >: V](entries: (K, V1)*): NEMap[K, V1] = new NEMap(toMap ++ entries)

  /**
    * Appends all entries of this <code>NEMap</code> to a string builder. The written text will consist of a concatenation of the result of invoking <code>toString</code>
    * on of every entry of this <code>NEMap</code>, without any separator string.
    *
    * @param sb the string builder to which entries will be appended
    * @return the string builder, <code>sb</code>, to which entries were appended.
    */
  def addString(sb: StringBuilder): StringBuilder = toMap.addString(sb)

  /**
    * Appends all entries of this <code>NEMap</code> to a string builder using a separator string. The written text will consist of a concatenation of the
    * result of invoking <code>toString</code>
    * on of every element of this <code>NEMap</code>, separated by the string <code>sep</code>.
    *
    * @param sb the string builder to which entries will be appended
    * @param sep the separator string
    * @return the string builder, <code>sb</code>, to which elements were appended.
    */
  def addString(sb: StringBuilder, sep: String): StringBuilder = toMap.addString(sb, sep)

  /**
    * Appends all entries of this <code>NEMap</code> to a string builder using start, end, and separator strings. The written text will consist of a concatenation of
    * the string <code>start</code>; the result of invoking <code>toString</code> on all elements of this <code>NEMap</code>,
    * separated by the string <code>sep</code>; and the string <code>end</code>
    *
    * @param sb the string builder to which elements will be appended
    * @param start the starting string
    * @param sep the separator string
    * @param start the ending string
    * @return the string builder, <code>sb</code>, to which elements were appended.
    */
  def addString(sb: StringBuilder, start: String, sep: String, end: String): StringBuilder = toMap.addString(sb, start, sep, end)

  /**
    * Selects an value by its key in the <code>NEMap</code>.
    *
    * @return the value of this <code>NEMap</code> at key <code>k</code>.
    */
  def apply(k: K): V = toMap(k)

  /**
    * Finds the first entry of this <code>NEMap</code> for which the given partial function is defined, if any, and applies the partial function to it.
    *
    * @param pf the partial function
    * @return an <code>Option</code> containing <code>pf</code> applied to the first entry for which it is defined, or <code>None</code> if
    *    the partial function was not defined for any entry.
    */
  def collectFirst[U](pf: PartialFunction[(K, V), U]): Option[U] = toMap.collectFirst(pf)

  /**
    * Indicates whether this <code>NEMap</code> contains a binding for given key.
    *
    * @param key the key to look for
    * @return true if this <code>NEMap</code> has a binding that is equal (as determined by <code>==)</code> to <code>key</code>, false otherwise.
    */
  def contains(key: K): Boolean = toMap.contains(key)

  /**
    * Copies entries of this <code>NEMap</code> to an array. Fills the given array <code>arr</code> with entries of this <code>NEMap</code>. Copying
    * will stop once either the end of the current <code>NEMap</code> is reached, or the end of the array is reached.
    *
    * @param arr the array to fill
    */
  def copyToArray[V1 >: V](arr: Array[(K, V1)]): Unit = {
    toMap.copyToArray(arr)
    ()
  }

  /**
    * Copies entries of this <code>NEMap</code> to an array. Fills the given array <code>arr</code> with entries of this <code>NEMap</code>, beginning at
    * index <code>start</code>. Copying will stop once either the end of the current <code>NEMap</code> is reached, or the end of the array is reached.
    *
    * @param arr the array to fill
    * @param start the starting index
    */
  def copyToArray[V1 >: V](arr: Array[(K, V1)], start: Int): Unit = {
    toMap.copyToArray(arr, start)
    ()
  }

  /**
    * Copies entries of this <code>NEMap</code> to an array. Fills the given array <code>arr</code> with at most <code>len</code> entries of this <code>NEMap</code>, beginning at
    * index <code>start</code>. Copying will stop once either the end of the current <code>NEMap</code> is reached, the end of the array is reached, or
    * <code>len</code> elements have been copied.
    *
    * @param arr the array to fill
    * @param start the starting index
    * @param len the maximum number of elements to copy
    */
  def copyToArray[V1 >: V](arr: Array[(K, V1)], start: Int, len: Int): Unit = {
    toMap.copyToArray(arr, start, len)
    ()
  }

  /**
    * Copies all elements of this <code>NEMap</code> to a buffer.
    *
    * @param buf the buffer to which elements are copied
    */
  def copyToBuffer[V1 >: V](buf: Buffer[(K, V1)]): Unit = {
    buf ++= toMap
    ()
  }

  /**
    * Counts the number of elements in this <code>NEMap</code> that satisfy a predicate.
    *
    * @param p the predicate used to test elements.
    * @return the number of elements satisfying the predicate <code>p</code>.
    */
  def count(p: ((K, V)) => Boolean): Int = toMap.count(p)

  /*
    override def equals(o: Any): Boolean =
      o match {
        case NEMap: NEMap[?] => toMap == NEMap.toMap
        case _ => false
      }
   */

  /**
    * Indicates whether a predicate holds for at least one of the entries of this <code>NEMap</code>.
    *
    * @param p the predicate used to test entries.
    * @return <code>true</code> if the given predicate <code>p</code> holds for some of the entries of this <code>NEMap</code>, otherwise <code>false</code>.
    */
  def exists(p: ((K, V)) => Boolean): Boolean = toMap.exists(p)

  /**
    * Finds the first entry of this <code>NEMap</code> that satisfies the given predicate, if any.
    *
    * @param p the predicate used to test elements
    * @return an <code>Some</code> containing the first element in this <code>NEMap</code> that satisfies <code>p</code>, or <code>None</code> if none exists.
    */
  def find(p: ((K, V)) => Boolean): Option[(K, V)] = toMap.find(p)

  /**
    * Builds a new <code>NEMap</code> by applying a function to all entries of this <code>NEMap</code> and using the entries of the resulting <code>NEMap</code>s.
    *
    * @tparam K1 the key type of the returned <code>NEMap</code>
    * @tparam V1 the value type of the returned <code>NEMap</code>
    * @param f the function to apply to each entry.
    * @return a new <code>NEMap</code> containing entries obtained by applying the given function <code>f</code> to each entry of this <code>NEMap</code> and concatenating
    *    the entries of resulting <code>NEMap</code>s.
    */
  def flatMap[K1, V1](f: ((K, V)) => NEMap[K1, V1]): NEMap[K1, V1] = {
    val buf = new ArrayBuffer[(K1, V1)]
    for (ele <- toMap)
      buf ++= f(ele).toMap
    new NEMap(buf.toMap)
  }

  /**
    * Folds the entries of this <code>NEMap</code> using the specified associative binary operator.
    *
    * <p>
    * The order in which operations are performed on entries is unspecified and may be nondeterministic.
    * </p>
    *
    * @tparam U a type parameter for the binary operator, a supertype of (K, V).
    * @param z a neutral element for the fold operation; may be added to the result an arbitrary number of
    *     times, and must not change the result (<em>e.g.</em>, <code>Nil</code> for list concatenation,
    *     0 for addition, or 1 for multiplication.)
    * @param op a binary operator that must be associative
    * @return the result of applying fold operator <code>op</code> between all the elements and <code>z</code>
    */
  def fold[U >: (K, V)](z: U)(op: (U, U) => U): U = toMap.fold(z)(op)

  /**
    * Applies a binary operator to a start value and all elements of this <code>NEMap</code>, going left to right.
    *
    * @tparam B the result type of the binary operator.
    * @param z the start value.
    * @param op the binary operator.
    * @return the result of inserting <code>op</code> between consecutive entries of this <code>NEMap</code>, going left to right, with the start value,
    *     <code>z</code>, on the left:
    *
    * <pre>
    * op(...op(op(z, x_1), x_2), ..., x_n)
    * </pre>
    *
    * <p>
    * where x<sub>1</sub>, ..., x<sub>n</sub> are the elements of this <code>NEMap</code>.
    * </p>
    */
  def foldLeft[B](z: B)(op: (B, (K, V)) => B): B = toMap.foldLeft(z)(op)

  /**
    * Applies a binary operator to all entries of this <code>NEMap</code> and a start value, going right to left.
    *
    * @tparam B the result of the binary operator
    * @param z the start value
    * @param op the binary operator
    * @return the result of inserting <code>op</code> between consecutive entries of this <code>NEMap</code>, going right to left, with the start value,
    *     <code>z</code>, on the right:
    *
    * <pre>
    * op(x_1, op(x_2, ... op(x_n, z)...))
    * </pre>
    *
    * <p>
    * where x<sub>1</sub>, ..., x<sub>n</sub> are the elements of this <code>NEMap</code>.
    * </p>
    */
  def foldRight[B](z: B)(op: ((K, V), B) => B): B = toMap.foldRight(z)(op)

  /**
    * Indicates whether a predicate holds for all entries of this <code>NEMap</code>.
    *
    * @param p the predicate used to test entries.
    * @return <code>true</code> if the given predicate <code>p</code> holds for all entries of this <code>NEMap</code>, otherwise <code>false</code>.
    */
  def forall(p: ((K, V)) => Boolean): Boolean = toMap.forall(p)

  /**
    * Applies a function <code>f</code> to all entries of this <code>NEMap</code>.
    *
    * @param f the function that is applied for its side-effect to every entry. The result of function <code>f</code> is discarded.
    */
  def foreach(f: ((K, V)) => Unit): Unit = toMap.foreach(f)

  /**
    * Partitions this <code>NEMap</code> into a map of <code>NEMap</code>s according to some discriminator function.
    *
    * @param f the discriminator function.
    * @return A map from keys to <code>NEMap</code>s such that the following invariant holds:
    *
    * <pre>
    * (NEMap.toMap partition f)(k) = xs filter (x =&gt; f(x) == k)
    * </pre>
    *
    * <p>
    * That is, every key <code>k</code> is bound to a <code>NEMap</code> of those elements <code>x</code> for which <code>f(x)</code> equals <code>k</code>.
    * </p>
    */
  def groupBy(f: ((K, V)) => K): Map[K, NEMap[K, V]] = {
    val mapKToMap = toMap.groupBy(f)
    mapKToMap.view.mapValues {
      list => new NEMap(list)
    }.toMap
  }

  /**
    * Partitions entries into fixed size <code>NEMap</code>s.
    *
    * @param size the number of entries per group
    * @return An iterator producing <code>NEMap</code>s of size <code>size</code>, except the last will be truncated if the entries don't divide evenly.
    */
  def grouped(size: Int): Iterator[NEMap[K, V]] = {
    val itOfMap = toMap.grouped(size)
    itOfMap.map {
      list => new NEMap(list)
    }
  }

  /**
    * Returns <code>true</code> to indicate this <code>NEMap</code> has a definite size, since all <code>NEMap</code>s are strict collections.
    */
  def hasDefiniteSize: Boolean = true

  // override def hashCode: Int = toMap.hashCode

  /**
    * Selects the first element of this <code>NEMap</code>.
    *
    * @return the first element of this <code>NEMap</code>.
    */
  def head: (K, V) = toMap.head

  def tail: Map[K, V] = toMap.tail

  /**
    * Tests whether this <code>NEMap</code> contains given key.
    *
    * @param key the key to test
    * @return true if this <code>NEMap</code> contains a binding for the given <code>key</code>, <code>false</code> otherwise.
    */
  def isDefinedAt(key: K): Boolean = toMap.isDefinedAt(key)

  /**
    * Returns <code>false</code> to indicate this <code>NEMap</code>, like all <code>NEMap</code>s, is non-empty.
    *
    * @return false
    */
  def isEmpty: Boolean = false

  /**
    * Returns <code>true</code> to indicate this <code>NEMap</code>, like all <code>NEMap</code>s, can be traversed repeatedly.
    *
    * @return true
    */
  def isTraversableAgain: Boolean = true

  /**
    * Creates and returns a new iterator over all elements contained in this <code>NEMap</code>.
    *
    * @return the new iterator
    */
  def iterator: Iterator[(K, V)] = toMap.iterator

  /**
    * Selects the last entry of this <code>NEMap</code>.
    *
    * @return the last entry of this <code>NEMap</code>.
    */
  def last: (K, V) = toMap.last

  /**
    * Builds a new <code>NEMap</code> by applying a function to all entries of this <code>NEMap</code>.
    *
    * @tparam K1 the key type of the returned <code>NEMap</code>.
    * @tparam V1 the value type of the returned <code>NEMap</code>.
    * @param f the function to apply to each element.
    * @return a new <code>NEMap</code> resulting from applying the given function <code>f</code> to each element of this <code>NEMap</code> and collecting the results.
    */
  def map[K1, V1](f: ((K, V)) => (K1, V1)): NEMap[K1, V1] =
    new NEMap(toMap.map(f))

  /**
    * Finds the largest entry.
    *
    * @return the largest entry of this <code>NEMap</code>.
    */
  def max[U >: (K, V)](implicit cmp: Ordering[U]): (K, V) = toMap.max(cmp)

  /**
    * Finds the largest result after applying the given function to every entry.
    *
    * @return the largest result of applying the given function to every entry of this <code>NEMap</code>.
    */
  def maxBy[U](f: ((K, V)) => U)(implicit cmp: Ordering[U]): (K, V) = toMap.maxBy(f)(cmp)

  /**
    * Finds the smallest entry.
    *
    * @return the smallest entry of this <code>NEMap</code>.
    */
  def min[U >: (K, V)](implicit cmp: Ordering[U]): (K, V) = toMap.min(cmp)

  /**
    * Finds the smallest result after applying the given function to every entry.
    *
    * @return the smallest result of applying the given function to every entry of this <code>NEMap</code>.
    */
  def minBy[U](f: ((K, V)) => U)(implicit cmp: Ordering[U]): (K, V) = toMap.minBy(f)(cmp)

  /**
    * Displays all entries of this <code>NEMap</code> in a string.
    *
    * @return a string representation of this <code>NEMap</code>. In the resulting string, the result of invoking <code>toString</code> on all entries of this
    *     <code>NEMap</code> follow each other without any separator string.
    */
  def mkString: String = toMap.mkString

  /**
    * Displays all entries of this <code>NEMap</code> in a string using a separator string.
    *
    * @param sep the separator string
    * @return a string representation of this <code>NEMap</code>. In the resulting string, the result of invoking <code>toString</code> on all entries of this
    *     <code>NEMap</code> are separated by the string <code>sep</code>.
    */
  def mkString(sep: String): String = toMap.mkString(sep)

  /**
    * Displays all entries of this <code>NEMap</code> in a string using start, end, and separator strings.
    *
    * @param start the starting string.
    * @param sep the separator string.
    * @param end the ending string.
    * @return a string representation of this <code>NEMap</code>. The resulting string begins with the string <code>start</code> and ends with the string
    *     <code>end</code>. Inside, In the resulting string, the result of invoking <code>toString</code> on all entries of this <code>NEMap</code> are
    *     separated by the string <code>sep</code>.
    */
  def mkString(start: String, sep: String, end: String): String = toMap.mkString(start, sep, end)

  /**
    * Returns <code>true</code> to indicate this <code>NEMap</code>, like all <code>NEMap</code>s, is non-empty.
    *
    * @return true
    */
  def nonEmpty: Boolean = true

  /**
    * The result of multiplying all the entries of this <code>NEMap</code>.
    *
    * <p>
    * This method can be invoked for any <code>NEMap[T]</code> for which an implicit <code>Numeric[T]</code> exists.
    * </p>
    *
    * @return the product of all elements
    */
  def product[U >: (K, V)](implicit num: Numeric[U]): U = toMap.product(num)

  /**
    * Reduces the entries of this <code>NEMap</code> using the specified associative binary operator.
    *
    * <p>
    * The order in which operations are performed on entries is unspecified and may be nondeterministic.
    * </p>
    *
    * @tparam U a type parameter for the binary operator, a supertype of T.
    * @param op a binary operator that must be associative.
    * @return the result of applying reduce operator <code>op</code> between all the elements of this <code>NEMap</code>.
    */
  def reduce[U >: (K, V)](op: (U, U) => U): U = toMap.reduce(op)

  /**
    * Applies a binary operator to all entries of this <code>NEMap</code>, going left to right.
    *
    * @tparam U the result type of the binary operator.
    * @param op the binary operator.
    * @return the result of inserting <code>op</code> between consecutive entries of this <code>NEMap</code>, going left to right:
    *
    * <pre>
    * op(...op(op(x_1, x_2), x_3), ..., x_n)
    * </pre>
    *
    * <p>
    * where x<sub>1</sub>, ..., x<sub>n</sub> are the elements of this <code>NEMap</code>.
    * </p>
    */
  def reduceLeft[U >: (K, V)](op: (U, (K, V)) => U): U = toMap.reduceLeft(op)

  /**
    * Applies a binary operator to all entries of this <code>NEMap</code>, going left to right, returning the result in a <code>Some</code>.
    *
    * @tparam U the result type of the binary operator.
    * @param op the binary operator.
    * @return a <code>Some</code> containing the result of <code>reduceLeft(op)</code>
    * </p>
    */
  def reduceLeftOption[U >: (K, V)](op: (U, (K, V)) => U): Option[U] = toMap.reduceLeftOption(op)

  def reduceOption[U >: (K, V)](op: (U, U) => U): Option[U] = toMap.reduceOption(op)

  /**
    * Applies a binary operator to all entries of this <code>NEMap</code>, going right to left.
    *
    * @tparam U the result of the binary operator
    * @param op the binary operator
    * @return the result of inserting <code>op</code> between consecutive entries of this <code>NEMap</code>, going right to left:
    *
    * <pre>
    * op(x_1, op(x_2, ... op(x_{n-1}, x_n)...))
    * </pre>
    *
    * <p>
    * where x<sub>1</sub>, ..., x<sub>n</sub> are the entries of this <code>NEMap</code>.
    * </p>
    */
  def reduceRight[U >: (K, V)](op: ((K, V), U) => U): U = toMap.reduceRight(op)

  /**
    * Applies a binary operator to all entries of this <code>NEMap</code>, going right to left, returning the result in a <code>Some</code>.
    *
    * @tparam U the result of the binary operator
    * @param op the binary operator
    * @return a <code>Some</code> containing the result of <code>reduceRight(op)</code>
    */
  def reduceRightOption[U >: (K, V)](op: ((K, V), U) => U): Option[U] = toMap.reduceRightOption(op)

  /**
    * Checks if the given <code>NEMap</code> contains the same entries in the same order as this <code>NEMap</code>.
    *
    * @param that the <code>NEMap</code> with which to compare
    * @return <code>true</code>, if both this and the given <code>NEMap</code> contain the same entries
    *     in the same order, <code>false</code> otherwise.
    */
  def sameElements[V1 >: V](that: NEMap[K, V1]): Boolean = toMap == that.toMap

  /**
    * Computes a prefix scan of the entries of this <code>NEMap</code>.
    *
    * <p>
    * @note The neutral element z may be applied more than once.
    * </p>
    *
    * @param z a neutral element for the scan operation; may be added to the result an arbitrary number of
    *     times, and must not change the result (<em>e.g.</em>, <code>Nil</code> for list concatenation,
    *     0 for addition, or 1 for multiplication.)
    * @param op a binary operator that must be associative
    * @return a new <code>NEMap</code> containing the prefix scan of the elements in this <code>NEMap</code>
    */
  def scan[V1 >: V](z: (K, V1))(op: ((K, V1), (K, V1)) => (K, V1)): NEMap[K, V1] = new NEMap(toMap.scan(z)(op).toMap)

  /**
    * Groups entries in fixed size blocks by passing a &ldquo;sliding window&rdquo; over them (as opposed to partitioning them, as is done in grouped.)
    *
    * @param size the number of entries per group
    * @return an iterator producing <code>NEMap</code>s of size <code>size</code>, except the last and the only element will be truncated
    *     if there are fewer entries than <code>size</code>.
    */
  def sliding(size: Int): Iterator[NEMap[K, V]] = toMap.sliding(size).map(new NEMap(_))

  /**
    * Groups entries in fixed size blocks by passing a &ldquo;sliding window&rdquo; over them (as opposed to partitioning them, as is done in grouped.),
    * moving the sliding window by a given <code>step</code> each time.
    *
    * @param size the number of entries per group
    * @param step the distance between the first entries of successive groups
    * @return an iterator producing <code>NEMap</code>s of size <code>size</code>, except the last and the only element will be truncated
    *     if there are fewer elements than <code>size</code>.
    */
  def sliding(size: Int, step: Int): Iterator[NEMap[K, V]] = toMap.sliding(size, step).map(new NEMap(_))

  /**
    * The size of this <code>NEMap</code>.
    *
    * <p>
    * @note <code>length</code> and <code>size</code> yield the same result, which will be <code>&gt;</code>= 1.
    * </p>
    *
    * @return the number of elements in this <code>NEMap</code>.
    */
  def size: Int = toMap.size

  /**
    * Returns <code>"NEMap"</code>, the prefix of this object's <code>toString</code> representation.
    *
    * @return the string <code>"NEMap"</code>
    */
  def stringPrefix: String = "NEMap"

  /**
    * The result of summing all the elements of this <code>NEMap</code>.
    *
    * <p>
    * This method can be invoked for any <code>NEMap[T]</code> for which an implicit <code>Numeric[T]</code> exists.
    * </p>
    *
    * @return the sum of all elements
    */
  def sum[U >: (K, V)](implicit num: Numeric[U]): U = toMap.sum(num)

  /**
    * Converts this <code>NEMap</code> into a collection of type <code>Col</code> by copying all entries.
    *
    * @tparam C1 the collection type to build.
    * @return a new collection containing all entries of this <code>NEMap</code>.
    */
  def to[C1](factory: Factory[(K, V), C1]): C1 = factory.fromSpecific(iterator)
  /**
    * Converts this <code>NEMap</code> to an array.
    *
    * @return an array containing all entries of this <code>NEMap</code>. A <code>ClassTag</code> must be available for the entry type of this <code>NEMap</code>.
    */
  def toArray[U >: (K, V)](implicit classTag: ClassTag[U]): Array[U] = toMap.toArray

  /**
    * Converts this <code>NEMap</code> to a <code>Vector</code>.
    *
    * @return a <code>Vector</code> containing all entries of this <code>NEMap</code>.
    */
  def toVector: Vector[(K, V)] = toMap.toVector

  /**
    * Converts this <code>NEMap</code> to a mutable buffer.
    *
    * @return a buffer containing all entries of this <code>NEMap</code>.
    */
  def toBuffer[U >: (K, V)]: mutable.Buffer[U] = toMap.toBuffer

  /**
    * Converts this <code>NEMap</code> to an immutable <code>IndexedSeq</code>.
    *
    * @return an immutable <code>IndexedSeq</code> containing all entries of this <code>NEMap</code>.
    */
  def toIndexedSeq: collection.immutable.IndexedSeq[(K, V)] = toMap.toVector

  /**
    * Converts this <code>NEMap</code> to an iterable collection.
    *
    * @return an <code>Iterable</code> containing all entries of this <code>NEMap</code>.
    */
  def toIterable: Iterable[(K, V)] = toMap

  /**
    * Returns an <code>Iterator</code> over the entries in this <code>NEMap</code>.
    *
    * @return an <code>Iterator</code> containing all entries of this <code>NEMap</code>.
    */
  def toIterator: Iterator[(K, V)] = toMap.iterator

  /**
    * Converts this <code>NEMap</code> to an immutable <code>IndexedSeq</code>.
    *
    * @return an immutable <code>IndexedSeq</code> containing all entries of this <code>NEMap</code>.
    */
  def toSeq: collection.immutable.Seq[(K, V)] = collection.immutable.Seq.empty[(K, V)] ++ toMap.toSeq

  /**
    * Converts this <code>NEMap</code> to a set.
    *
    * @return a set containing all entries of this <code>NEMap</code>.
    */
  def toSet[U >: (K, V)]: Set[U] = toMap.toSet

  /**
    * Returns a string representation of this <code>NEMap</code>.
    *
    * @return the string <code>"NEMap"</code> followed by the result of invoking <code>toString</code> on
    *   this <code>NEMap</code>'s elements, surrounded by parentheses.
    */
  override def toString: String = "NEMap(" + toMap.mkString(", ") + ")"

  /**
    * Converts this <code>NEMap</code> of pairs into two <code>Iterable</code>s of the first and second half of each pair.
    *
    * @tparam L the type of the first half of the element pairs
    * @tparam R the type of the second half of the element pairs
    * @param asPair an implicit conversion that asserts that the element type of this <code>NEMap</code> is a pair.
    * @return a pair of <code>NEMap</code>s, containing the first and second half, respectively, of each element pair of this <code>NEMap</code>.
    */
  def unzip[L, R](implicit asPair: ((K, V)) => (L, R)): (scala.collection.immutable.Iterable[L], scala.collection.immutable.Iterable[R]) = toMap.unzip

  /**
    * Converts this <code>NEMap</code> of triples into three <code>NEMap</code>s of the first, second, and and third entry of each triple.
    *
    * @tparam L the type of the first member of the entry triples
    * @tparam R the type of the second member of the entry triples
    * @tparam R the type of the third member of the entry triples
    * @param asTriple an implicit conversion that asserts that the entry type of this <code>NEMap</code> is a triple.
    * @return a triple of <code>NEMap</code>s, containing the first, second, and third member, respectively, of each entry triple of this <code>NEMap</code>.
    */
  def unzip3[L, M, R](
    implicit asTriple: ((K, V)) => (L, M, R)
  ): (scala.collection.immutable.Iterable[L], scala.collection.immutable.Iterable[M], scala.collection.immutable.Iterable[R]) = toMap.unzip3

  /**
    * A copy of this <code>NEMap</code> with one single replaced entry.
    *
    * @param key the key of the replacement
    * @param value the replacing value
    * @return a copy of this <code>NEMap</code> with the value at <code>key</code> replaced by the given <code>value</code>.
    */
  def updated[V1 >: V](key: K, value: V1): NEMap[K, V1] =
    new NEMap(toMap.updated(key, value))

  /**
    * Returns a <code>NEMap</code> formed from this <code>NEMap</code> and an iterable collection by combining corresponding
    * entries in pairs. If one of the two collections is shorter than the other, placeholder entries will be used to extend the
    * shorter collection to the length of the longer.
    *
    * @tparam O the type of the second half of the returned pairs
    * @tparam V1 the subtype of the value type of this <code>NEMap</code>
    * @param other the <code>Iterable</code> providing the second half of each result pair
    * @param thisElem the element to be used to fill up the result if this <code>NEMap</code> is shorter than <code>that</code> <code>Iterable</code>.
    * @param otherElem the element to be used to fill up the result if <code>that</code> <code>Iterable</code> is shorter than this <code>NEMap</code>.
    * @return a new <code>NEMap</code> containing pairs consisting of corresponding entries of this <code>NEMap</code> and <code>that</code>. The
    *     length of the returned collection is the maximum of the lengths of this <code>NEMap</code> and <code>that</code>. If this <code>NEMap</code>
    *     is shorter than <code>that</code>, <code>thisElem</code> values are used to pad the result. If <code>that</code> is shorter than this
    *     <code>NEMap</code>, <code>thatElem</code> values are used to pad the result.
    */
  def zipAll[O, V1 >: V](other: collection.Iterable[O], thisElem: (K, V1), otherElem: O): NEMap[(K, V1), O] =
    new NEMap(toMap.zipAll(other, thisElem, otherElem).toMap)

  /**
    * Zips this <code>NEMap</code>  with its indices.
    *
    * @return A new <code>NEMap</code> containing pairs consisting of all elements of this <code>NEMap</code> paired with their index. Indices start at 0.
    */
  def zipWithIndex[V1 >: V]: NEMap[(K, V1), Int] = new NEMap(toMap.zipWithIndex.toMap)
}

/**
  * Companion object for class <code>NEMap</code>.
  */
object NEMap {

  /**
    * Constructs a new <code>NEMap</code> given at least one element.
    *
    * @tparam K the type of the key contained in the new <code>NEMap</code>
    * @tparam V the type of the value contained in the new <code>NEMap</code>
    * @param firstElement the first element (with index 0) contained in this <code>NEMap</code>
    * @param otherElements a varargs of zero or more other elements (with index 1, 2, 3, ...) contained in this <code>NEMap</code>
    */
  @inline def apply[K, V](firstElement: (K, V), otherElements: (K, V)*): NEMap[K, V] = new NEMap(otherElements.toMap + firstElement)

  /**
    * Variable argument extractor for <code>NEMap</code>s.
    *
    * @tparam K the type of the key contained in the <code>NEMap</code>
    * @tparam V the type of the value contained in the <code>NEMap</code>
    * @param NEMap: the <code>NEMap</code> containing the elements to extract
    * @return an <code>Seq</code> containing this <code>NEMap</code>s elements, wrapped in a <code>Some</code>
    */
  @inline def unapplySeq[K, V](NEMap: NEMap[K, V]): Some[Seq[(K, V)]] = Some(NEMap.toSeq)

  /**
    * Optionally construct a <code>NEMap</code> containing the elements, if any, of a given <code>Seq</code>.
    *
    * @tparam K the type of the key contained in the new <code>NEMap</code>
    * @tparam V the type of the value contained in the new <code>NEMap</code>
    * @param seq the <code>Seq</code> with which to construct a <code>NEMap</code>
    * @return a <code>NEMap</code> containing the elements of the given <code>Seq</code>, if non-empty, wrapped in
    *     a <code>Some</code>; else <code>None</code> if the <code>Seq</code> is empty
    */
  @inline def from[K, V](seq: Seq[(K, V)]): Option[NEMap[K, V]] =
    seq.headOption match {
      case None => None
      case Some(first) => Some(new NEMap(scala.collection.immutable.Map.empty[K, V] ++ seq.tail.toMap + first))
    }

  @inline def from[K, V](map: scala.collection.Map[K, V]): Option[NEMap[K, V]] = {
    if (map.isEmpty) None else Some(new NEMap(scala.collection.immutable.Map.empty[K, V] ++ map))
  }

  @inline def from[K, V](map: scala.collection.immutable.Map[K, V]): Option[NEMap[K, V]] = {
    if (map.isEmpty) None else Some(new NEMap(map))
  }

  @inline def unsafeFrom[K, V](set: scala.collection.immutable.Map[K, V]): NEMap[K, V] = {
    require(set.nonEmpty)
    new NEMap(set)
  }

  implicit final class OptionOps[K, +V](private val option: Option[NEMap[K, V]]) extends AnyVal {
    @inline def fromNEMap: Map[K, V] = if (option.isEmpty) Map.empty else option.get.toMap
  }

  @inline implicit def asIterable[K, V](ne: NEMap[K, V]): IterableOnce[(K, V)] = ne.toIterable

}
