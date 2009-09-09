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

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

/**
 * A simple stack implementation created from an <code>ArrayList</code>.
 */
public class Stack extends ArrayList {
  /** Constructs an empty stack. */
  public Stack() {
    super();
  }
  /**
   * Constructs a stack with the specified collection, whose bottom
   * element is the first returned by <code>c.iterator()</code> and
   * whose top element is the last.
   */
  public Stack(Collection c) {
    super(c);
  }
  /**
   * Constructs a stack with the specified initial capacity.
   */
  public Stack(int initialCapacity) {
    super(initialCapacity);
  }

  /** Returns <code>true</code> if this stack contains no elements. */
  public boolean empty() { return size() == 0; }
  /** Pushes the specified object onto the stack. */
  public void push(Object obj) {
    add(obj);
  }
  /**
   * Pops the top element off the stack.
   *
   * @return the top element that was popped off the stack
   * @throws IndexOutOfBoundsException if this stack was empty prior
   * to calling this method
   */
  public Object pop() {
    return remove(size() - 1);
  }
}
