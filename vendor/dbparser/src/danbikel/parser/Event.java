/* *           Copyright (c) 2004, Daniel M. Bikel.
 *                         All rights reserved.
 * 
 *                Developed at the University of Pennsylvania
 *                Institute for Research in Cognitive Science
 *                    3401 Walnut Street
 *                    Philadelphia, Pennsylvania 19104
 * 			
 * 
 * For research or educational purposes only.  Do not redistribute.  For
 * complete license details, please read the file LICENSE that accompanied
 * this software.
 * 
 * DISCLAIMER
 * 
 * Daniel M. Bikel makes no representations or warranties about the suitability of
 * the Software, either express or implied, including but not limited to the
 * implied warranties of merchantability, fitness for a particular purpose, or
 * non-infringement. Daniel M. Bikel shall not be liable for any damages suffered
 * by Licensee as a result of using, modifying or distributing the Software or its
 * derivatives.
 * 
 */
    package danbikel.parser;

import java.io.Serializable;
import java.util.*;

/**
 * Provides the specification for arbitrary event types, to be used
 * when collecting counts for and computing probabilities of arbitrary
 * events.  <code>Event</code> objects have to satisfy the rather
 * conflicting design goals of providing an abstraction for events
 * composed of arbitrarily-typed components, efficiency of access,
 * efficiency of storage and notational felicity when composing an
 * event.  Therefore, the general contract of <code>Event</code>
 * objects is that they provide the means to store <i>sequences</i> of
 * different types of components, such that two <code>Event</code>s
 * are guaranteed to be equal if components of the same type have been
 * added in the same order and are pairwise equal.  Put another way,
 * <code>Event</code> objects can be thought of abstractly as a
 * set of lists, with one list for each type of object they are
 * capable of collecting, where two <code>Event</code> objects
 * are equal if all of their corresponding lists are equal.  This contract
 * achieves notational convenience when composing an event, because
 * {@link MutableEvent#add(Object)} will add the specified object to the
 * appropriate list.  For example, if two <code>MutableEvent</code> objects
 * <code>event1</code> and <code>event2</code> are composed via the following
 * code
 * <pre>
 * event1.add(new Integer(2)).add("foo").add(new Integer(3));
 * event2.add("foo").add(new Integer(2)).add(new Integer(3));
 * </pre>
 * it is guaranteed that <code>event1.equals(event2)</code> will return
 * <code>true</code>.
 * <p>
 * <code>Event</code> objects must explicitly publish the run-time types of
 * objects they are capable of collecting, and these types may include
 * <code>Object</code>, as well as primitive types.  This publishing is
 * accomplished via the method {@link #getClass(int)}, which maps integers
 * in the range [0,{@link #numTypes}) to <code>Class</code> objects.
 */
public interface Event extends Serializable {
  /**
   * Maps the specified integer to a type that this <code>Event</code> is
   * capable of collecting.  Type indices are required to fill
   * the range [0,&nbsp;{@link #numTypes}) for all types supported by an
   * <code>Event</code> implementation.
   *
   * @return the type (<code>Class</code>) associated with the specified
   * type index
   */
  public Class getClass(int type);

  /**
   * Returns the integer constant associated with the specified event component
   * type, or <tt>-1</tt> if this event does not support the specified class.
   */
  public int typeIndex(Class cl);

  /**
   * Returns the number of component types capable of being collected by
   * this <code>Event</code> implementation.
   */
  public int numTypes();

  /**
   * Gets the total number of components of this event.
   */
  public int numComponents();

  /**
   * Gets the number of components of this event of a particular type.
   */
  public int numComponents(int type);

  /**
   * Gets the <code>index</code><sup>th</sup> component of the
   * specified type from this event.  If an implementation of this
   * interface supports primitive type values, then these values
   * should be wrapped in their corresponding wrapper classes.  For example,
   * if an implementation collects <code>int</code> values, then
   * these values should be returned by this method wrapped in
   * <code>Integer</code> objects.
   *
   * @exception IndexOutOfBoundsException if
   * <code>type</code>&nbsp;&lt;&nbsp;0</code> or if
   * <code>type&nbsp;&gt;=&nbsp;numTypes()</code> or if
   * <code>index&nbsp;&lt;&nbsp;0</code> or if
   * <code>index&nbsp;&gt;=&nbsp;numComponents(type)</code>
   */
  public Object get(int type, int index);

  /**
   * Returns a deep copy of this event of the same run-time type.
   */
  public Event copy();

  /**
   * Since events are typically read-only, this method will allow for
   * canonicalization (or "unique-ifying") of the information
   * contained in this event.  Use of this method is intended to
   * conserve memory by removing duplicate copies of event information
   * in different event objects.
   *
   * @param canonical a reflexive map of objecs representing event
   * information: for each unique key-value pair, the value is a
   * reference to the key
   *
   * @return 1 if this event was canonicalized, 0 if it was not
   * canonicalized (and had to be added to <code>canonical</code>) or
   * -1 if this event was not even eligible for canonicalization
   */
  public int canonicalize(Map canonical);

  /**
   * Returns the hash code for this event.
   * @return the hash code for this event
   */
  public int hashCode();

  /**
   * Returns <code>true</code> if and only if the following three
   * conditions are met:
   * <ul>
   * <li> the specified object is an instance of <code>Event</code>
   * <li> the specified object supports the same and only the same types
   * of components as this <code>Event</code> object
   * <li> the abstract lists of each type for this object and the
   * specified object are of equal length and are pairwise equal in
   * their items
   */
  public boolean equals(Object obj);
}
