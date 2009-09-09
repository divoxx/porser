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
 * A generic object pool.
 */

public class ObjectPool {
  // data members
  private Class type;
  private Object[] pool;
  private int head;


  public ObjectPool(Class type) {
    this(type, 0);
  }
  public ObjectPool(Class type, int initialSize) {
    if (initialSize < 0)
      throw new IllegalArgumentException();
    this.type = type;
    pool = new Object[initialSize];
    head = 0;
  }

  public Object get() {
    if (head > 0)
      return pool[--head];
    else
      return newInstance();
  }

  public void putBack(Object obj) {
    ensureCapacity(head + 1);
    pool[head++] = obj;
  }

  public void putBackPool(ObjectPool other) {
    ensureCapacity(this.head + other.head);
    System.arraycopy(other.pool, 0, this.pool, this.head, other.head);
  }

  public void putBackAll(Collection c) {
    ensureCapacity(this.head + c.size());
    Iterator it = c.iterator();
    while (it.hasNext())
      pool[head++] = it.next();
  }

  private void ensureCapacity(int newSize) {
    if (pool.length >= newSize)
      return;
    int newArrSize = pool.length * 2;
    if (newArrSize < newSize)
      newArrSize = newSize;
    Object[] newArr = new Object[newArrSize];
    System.arraycopy(pool, 0, newArr, 0, pool.length);
    pool = newArr;
  }

  private final Object newInstance() {
    try {
      return type.newInstance();
    }
    catch (Exception e) {
      System.err.println(e);
      return null;
    }
  }

  public int size() { return head; }
  public int capacity() { return pool.length; }
}