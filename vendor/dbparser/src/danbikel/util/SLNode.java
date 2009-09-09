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

import java.io.Serializable;
import java.util.*;

/**
 * Represents a node in a singly-linked list.  This class provides
 * low-level and direct access to the nodes of a singly-linked list
 * and to the data objects at those nodes, and is thus intended to be
 * used when efficiency concerns outweigh data abstraction concerns.
 * For example, this class makes no attempt at providing synchronization
 * mechanisms, nor does it attempt to check for concurrent modifications,
 * as do most of the Collections framework classes.  Also, zero-length
 * lists are represented implicitly by <code>null</code>, as far as
 * this class is concerned.
 */
public class SLNode implements Serializable {
  private Object data;
  private SLNode next;

  public SLNode() {}

  /**
   * Constructs a new <code>SLNode</code> with the specified data object
   * and <code>next</code> node.
   *
   * @param data the data to associate with this node
   * @param next the next object in the singly-linked list, or
   * <code>null</code> if this is the last node in the list
   */
  public SLNode(Object data, SLNode next) {
    this.data = data;
    this.next = next;
  }

  // accessors

  /**
   * Returns the data associated with this node of the list.
   */
  public Object data() { return data; }

  /**
   * Returns the next node of this list.
   */
  public SLNode next() { return next; }

  // mutators

  public SLNode set(Object data, SLNode next) {
    this.data = data;
    this.next = next;
    return this;
  }

  public SLNode setData(Object data) {
    this.data = data;
    return this;
  }

  /**
   * Sets the next node of this list.
   *
   * @param newNext the new node to be inserted after this node
   */
  private void setNext(SLNode next) {
    this.next = next;
  }

  /**
   * Returns the length of this list.  This method begins by counting the
   * current node, traversing <code>next</code> pointers until <code>null</code>
   * is reached.
   */
  public int length() {
    int len = 1;
    SLNode curr = this;
    for ( ; curr.next() != null; len++)
      curr = curr.next();
    return len;
  }

  /**
   * Returns the length of this list.
   */
  public int size() { return length(); }

  /**
   * Returns a new <code>LinkedList</code> object containing all the data of
   * this list.
   */
  public LinkedList toList() {
    LinkedList list = new LinkedList();
    SLNode curr = this;
    while (curr != null) {
      list.add(curr.data());
      curr = curr.next();
    }
    return list;
  }

  /**
   * Returns an iterator to iterate over the elements of this list.
   * An alternative and more efficient iteration idiom is as
   * follows:
   * <pre>
   * for (SLNode curr = &lt;init&gt;; curr.next() != null; curr = curr.next()) {
   *   Object data = curr.data();
   *   // operate on data at current node...
   * }
   * </pre>
   */
  public Iterator iterator() {
    return new Iterator() {
      private SLNode curr = SLNode.this;
      private boolean nextCalled = false;
      public boolean hasNext() {
	nextCalled = true;
	return curr != null;
      }
      public Object next() {
	Object currData = curr.data();
	curr = curr.next();
	return currData;
      }
      public void remove() {
	if (!nextCalled || !hasNext())
	  throw new IllegalStateException();
	curr.setNext(curr.next().next());
	curr = curr.next();
      }
    };
  }

  /**
   * Returns a human- and <code>Sexp</code>-readable version of the
   * singly-linked list headed by this node.
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("(");
    SLNode curr = this;
    while (curr != null) {
      sb.append(curr.data);
      curr = curr.next;
    }
    sb.append(")");
    return sb.toString();
  }

  /**
   * Returns the hash code for this singly-linked list, using the same
   * algorithm specified by {@link List#hashCode()}.
   */
  /*
  public int hashCode() {
    int code = 0;
    SLNode curr = this;
    while (curr != null) {
      code = (code * 31) + (curr.data == null ? 0 : curr.data.hashCode());
      curr = curr.next;
    }
    return code;
  }
  */

  /**
   * Returns <code>true</code> if the specified object is also an instance
   * of <code>SLNode</code>, and if the singly-linked list it head has
   * the same number of nodes as the list headed by this node, and if
   * those two lists have data objects that are pairwise-equal; otherwise,
   * this method returns <code>false</code>.
   */
  /*
  public boolean equals(Object obj) {
    if (!(obj instanceof SLNode))
      return false;
    SLNode other = (SLNode)obj;
    SLNode curr = this;
    while (curr != null && other != null) {
      if ((curr.data == null && other.data != null) ||
	  (curr.data != null && other.data == null))
	return false;
      if (curr.data != null && curr.data.equals(other.data) == false)
	return false;
      curr = curr.next;
      other = other.next;
    }
    return curr == null && other == null;
  }
  */
}