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
 * Provides a convenient default implementation for most of the methods of
 * <code>List</code> and <code>FixedSizeList</code>.
 */
public abstract class AbstractFixedSizeList
  extends AbstractList implements FixedSizeList {

  /**
   * No-arg constructor, for use only for Serialization of derived, concrete
   * classes.
   */
  protected AbstractFixedSizeList() { }

  /**
   * Initializes this new list to have the specified number of elements.
   * As per the contract of <code>FixedSizeList</code>, all elements will
   * initially be <code>null</code>.
   *
   * @param size the number of elements that this fixed-size list will have
   */
  protected AbstractFixedSizeList(int size) {
    initialize(size);
  }

  /**
   * Initializes this new list to contain all elements of the specified
   * collection.
   *
   * @param c a collection whose elements are to become the elements of this
   * fixed-size list
   */
  protected AbstractFixedSizeList(Collection c) {
    initialize(c.size());
    addAll(c);
  }

  /**
   * Initializes this list to be of the specified size.  As per the general
   * contract of {@link FixedSizeList}, all elements will initially be
   * <code>null</code>.  Implementors should take care not to invoke this
   * method subsequent to construction of a fixed-size list, as such an
   * invocation would violate the general contract of {@link FixedSizeList}.
   *
   * @param size the size of this list
   */
  abstract protected void initialize(int size);

  public boolean add(Object obj) {
    return shift(obj);
  }

  public boolean addAll(Collection c) {
    Iterator it = c.iterator();
    int size = size();
    int i = 0;
    for ( ; i < size && it.hasNext(); i++) {
      set(i, it.next());
    }
    return i > 0;
  }

  public boolean addAll(int index, Collection c) {
    throw new UnsupportedOperationException();
  }

  public boolean removeAll(Collection c) {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the specified object to be at the specified index in this fixed-size
   * list.
   * @param index the index at which to set the specified object
   * @param element the object to be set at the specified index
   * @return the object that was formerly at the specified index
   */
  abstract public Object set(int index, Object element);

  abstract public boolean shift(Object obj);

  /**
   * Compres this <code>FixedSizeList</code> to the specified object
   * for equality.  This implementation assumes that the <code>get(int)</code>
   * and <code>size()</code> methods take constant time.
   *
   * @param o the object to test for equality with this object
   * @return whether the specified object is equal to this object
   */
  public boolean equals(Object o) {
    if (o == this)
      return true;
    if (!(o instanceof FixedSizeList))
      return false;
    FixedSizeList other = (FixedSizeList)o;
    if (this.size() != other.size())
      return false;
    int size = size();
    for (int i = 0; i < size; i++)
      if (!this.get(i).equals(other.get(i)))
        return false;
    return true;
  }

  /**
   * Generates a hash code for this list.  This implementation assumes that
   * the <code>get(int)</code> and <code>size()</code> methods take constant
   * time.
   *
   * @return the hash code of this fixed-size list
   */
  public int hashCode() {
    int size = size();
    int code = 0;
    if (size < 10) {
      Object first = get(0);
      code = first == null ? 0 : first.hashCode();
      for (int i = 1; i < size; i++) {
        Object curr = get(i);
        code = (code << 2) ^ (curr == null ? 0 : curr.hashCode());
      }
    }
    else {
      code = 1;
      for (int i = 0; i < size; i++) {
        Object curr = get(i);
        code = 31*code + (curr == null ? 0 : curr.hashCode());
      }
    }
    return code;
  }
}