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

/**
 * Specifies useful/necessary diagnostic and lookup methods that the Map
 * and HashMap APIs lack.
 */
public interface FlexibleMap<K,V> extends java.util.Map<K,V> {

  /**
   * Gets the capacity of this map (optional operation).
   *
   * @return the capacity of this map (the number of buckets, in the case
   * of a hash map)
   * @throws UnsupportedOperationException if this map is not a hash map
   */
  int getCapacity();

  /**
   * Gets the load factor of this map (optional operation).
   *
   * @return the load factor of this map
   * @throws UnsupportedOperationException if this map is not a hash map
   */
  float getLoadFactor();

  /**
   * Returns a string that represents the useful statistics of this map
   * (useful/necessary in the case of hash maps, where it is desirable to
   * know the number of collisions and average and maximum buckets sizes).
   * The format of the string is up to the implementor.
   */
  String getStats();

  /**
   * Returns the value for the specified key.  If the specified hash code
   * is not the value of <code>key.hashCode()</code>, the behavior of thiscollins
   * method is not defined.
   *
   * @param key the key whose value is to be looked up
   * @param hashCode the value of <code>key.hashCode()</code>
   */
  V get(K key, int hashCode);
}