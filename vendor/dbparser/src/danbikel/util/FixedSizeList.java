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
    package danbikel.util;

import java.util.*;

/**
 * Specifies methods for a list of a fixed size.  As a consequence of having
 * fixed size, several of the optional operations from <code>List</code> are
 * overridden here to document explicitly that they should <i>not</i> be
 * supported.  Another consequence is that all implementors should have at
 * least one argument in their constructors that is an <code>int</code>,
 * to specify the fixed size of the list to be created, and all elements are
 * initially <code>null</code> unless otherwise set via a constructor.  A
 * final consequence is that the {@link #shift(Object)} method may be
 * efficiently implemented for lists of fixed size, by employing a circular
 * buffer (indeed, this was one of the prime motivations for the creation of
 * this interface).
 */
public interface FixedSizeList extends List {
  /**
   * Implementors should simply throw an
   * <code>UnsupportedOperationException</code>.
   */
  public void add(int index, Object element);

  /**
   * A synonym for {@link #shift(Object)}.
   *
   * @param o the object to be shifted into the first position of this list
   * @return <tt>true</tt> (as per the general contract of the
   * <code>Collection.add</code> method)
   */
  public boolean add(Object o);

  /**
   * Sets the first <i>n</i> elements of this list to be the elements of the
   * specified collection, where <i>n</i> is the minimum of the size of the
   * collection and the (fixed) size of this list.
   *
   * @param c the collection whose elements are to become the elements of
   * this list
   * @return <tt>true</tt> if this list was modified as a result of the call
   */
  public boolean addAll(Collection c);

  /**
   * Implementors should simply throw an
   * <code>UnsupportedOperationException</code>.
   */
  public boolean addAll(int index, Collection c);

  /** Sets all elements of this list to <code>null</code>. */
  public void clear();


  /**
   * Implementors should simply throw an
   * <code>UnsupportedOperationException</code>.
   */
  public Object remove(int index);

  /**
   * Implementors should simply throw an
   * <code>UnsupportedOperationException</code>.
   */
  public boolean remove(Object o);

  /**
   * Implementors should simply throw an
   * <code>UnsupportedOperationException</code>.
   */
  public boolean removeAll(Collection c);

  /**
   * Implementors should simply throw an
   * <code>UnsupportedOperationException</code>.
   */
  public boolean retainAll(Collection c);

  /**
   * Shifts the specified object to the beginning of the list, that is,
   * causes the specified object to be the value at index <tt>0</tt>,
   * causes the object at index <tt>size() - 1</tt> to be removed and
   * causes the index of all other objects to be increased by <tt>1</tt>.
   * <p>
   * <b>Implementation advice</b>: This method may be implemented to take
   * constant (that is, O(1)) time if the employed data struture is a
   * circular buffer.
   *
   * @param obj the object to be shifted into this list
   * @return <tt>true</tt> (as per the general contract of the
   * <code>Collection.add</code> method)
   */
  public boolean shift(Object obj);

}