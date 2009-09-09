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
 * Provides a mechanism to group any two objects.  Either or both objects
 * may be <code>null</code>.
 */
public class Pair implements Serializable {
  /** The first object in the pair. */
  public Object first;
  /** The second object in the pair. */
  public Object second;

  /**
   * Constructs a new <code>Pair</code> object with both data members
   * set to <code>null</code>.
   */
  public Pair() { first = second = null; }; 
  /**
   * Constructs a new <code>Pair</code> object with the specified two
   * objects.
   */
  public Pair(Object first, Object second) {
    this.first = first; this.second = second;
  }

  /**
   * Returns <code>true</code> if and only if
   * <ul>
   * <li>the specified object is an instance of <code>Pair</code> and
   * <li>if <code>this.first</code> and <code>((Pair)obj).first</code> are
   *     either both <code>null</code> or are equal as determined by the
   *     <code>equals</code> method of <code>this.first</code>
   * <li>if <code>this.second</code> and <code>((Pair)obj).second</code> are
   *     either both <code>null</code> or are equal as determined by the
   *     <code>equals</code> method of <code>this.second</code>
   * </ul>
   */
  public boolean equals(Object obj) {
    if (!(obj instanceof Pair))
      return false;
    Pair other = (Pair)obj;
    return
      ((first == null ? other.first == null : first.equals(other.first)) &&
       (second == null ? other.second == null : second.equals(other.second)));
  }

  /**
   * Returns a hash code that is formed from the hash codes of the two
   * objects of this pair.  If an object in this pair is <code>null</code>,
   * then its effective hash code as calculated by this method is 0.
   */
  public int hashCode() {
    int code = (first == null ? 0 : first.hashCode());
    if (second != null)
      code = (code << 2) ^ second.hashCode();
    return code;
  }

  /**
   * Returns a human-readable string representation of this pair of objects,
   * of the form
   * <pre> "Pair(" + first + ", " + second + ")" </pre>
   */
  public String toString() {
    return "Pair(" + first + ", " + second + ")";
  }
}
