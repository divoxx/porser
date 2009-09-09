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

/**
 * A class to hold an <code>int</code> that may be incremented or decremented.
 */
public class IntCounter implements Comparable, Serializable {

  private int count;

  /**
   * Constructs an <code>IntCounter</code> with an initial value of zero.
   */
  public IntCounter() {
    count = 0;
  }

  /**
   * Constructs an <code>IntCounter</code> with the specified initial value.
   */
  public IntCounter(int count) {
    this.count = count;
  }

  /** Increments this counter by 1, returning the previous count
      (postincrement). */
  public final int increment() { return count++; }
  /**
   * Increments this counter by the specified integer. If the integer is
   * negative, then the counter will get decremented by the absolute value of
   * <code>n</code>.
   */
  public final void increment(int n) { count += n; }
  /** Gets the current count. */
  public final int get() { return count; }

  public final void set(int count) { this.count = count; }

  public int hashCode() { return count; }

  /**
   * Returns <code>true</code> if the specified object is an instance of
   * <code>IntCounter</code> and if its count is the same as that of
   * this <code>IntCounter</code> object, <code>false</code> otherwise.
   *
   * @param obj the object to compare with this object
   * @return <code>true</code> if the specified object is an instance of
   * <code>IntCounter</code> and if its count is the same as that of
   * this <code>IntCounter</code> object, <code>false</code> otherwise
   */
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof IntCounter))
      return false;
    return this.count == ((IntCounter)obj).count;
  }

  public String toString() { return String.valueOf(count); }

  public int compareTo(Object obj) {
    IntCounter other = (IntCounter)obj;
    return (this.count < other.count ? -1 :
	    (this.count == other.count ? 0 :
	     1));
  }
}
