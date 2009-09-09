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
import java.io.*;

/**
 * A map from arbitrary keys to <code>double</code> values.
 */
public class HashMapDouble<K> extends HashMapPrimitive<K>
  implements Cloneable, java.io.Serializable {
  /**
   * Constructs a new, empty map with the specified initial capacity and the
   * specified load factor.
   *
   * @param initialCapacity the initial capacity of the HashMap.
   * @param loadFactor      the load factor of the HashMap
   * @throws IllegalArgumentException if the initial capacity is less than zero,
   *                                  or if the load factor is nonpositive.
   */
  public HashMapDouble(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  /**
   * Constructs a new, empty map with the specified initial capacity and {@link
   * HashMapPrimitive#defaultLoadFactor default load factor}.
   *
   * @param initialCapacity the initial capacity of the HashMap.
   * @throws IllegalArgumentException if the initial capacity is less than
   *                                  zero.
   */
  public HashMapDouble(int initialCapacity) {
    this(initialCapacity, defaultLoadFactor);
  }

  /**
   * Constructs a new, empty map with a
   * {@link HashMapPrimitive#defaultInitialCapacity default capacity} and
   * {@link HashMapPrimitive#defaultLoadFactor load factor}.
   */
  public HashMapDouble() {
    this(defaultInitialCapacity, defaultLoadFactor);
  }

  /**
   * Constructs a new map with the same mappings as the given map.  The map is
   * created with a capacity of twice the number of mappings in the given map or
   * {@link HashMapPrimitive#defaultInitialCapacity the default size} (whichever
   * is greater), and a {@link HashMapPrimitive#defaultLoadFactor default load
   * factor}.
   *
   * @param t the map whose mappings are to be placed in this map.
   */
  public HashMapDouble(Map t) {
    this(Math.max(2 * t.size(), defaultInitialCapacity), defaultLoadFactor);
    putAll(t);
  }

  /**
   * Gets a map entry for this type of map, containing a key and a
   * <code>double</code>.
   *
   * @param hash the hash value of the specified key
   * @param key  the key for this map entry to wrap
   * @param next the next pointer for this entry in its singly-linked list (can
   *             be null)
   * @return a new {@link HashMapPrimitive.Entry} for this type of map
   */
  protected HashMapPrimitive.Entry<K>
  getNewEntry(int hash, K key,
	      HashMapPrimitive.Entry<K> next) {
    return new Entry<K>(hash, key, next);
  }

  /**
   * A map entry for this type of map, containing a key and a
   * <code>double</code>.
   */
  protected static class Entry<K> extends HashMapPrimitive.Entry<K> {
    transient protected double doubleVal0;

    /**
     * Constructs a new entry for this type of map with default values for the
     * data members (this default constructor here for serialization reasons).
     */
    public Entry() {
    }

    /**
     * Constructs a new entry for a map from objects to <code>double</code>s.
     *
     * @param hash the hash value for the specified key
     * @param key  the key for this entry to wrap
     * @param next the next pointer in this entry&rsquo;s singly-linked list
     */
    protected Entry(int hash, K key, HashMapPrimitive.Entry<K> next) {
      super(hash, key, next);
    }

    /**
     * Constructs a new entry for a map from objects to <code>double</code>s.
     *
     * @param hash  the hash value for the specified key
     * @param key   the key for this entry to wrap
     * @param value the <code>double</code> value to be associated with the
     *              specified key in the map
     * @param next  the next pointer in this entry&rsquo;s singly-linked list
     */
    protected Entry(int hash, K key, double value,
		    HashMapPrimitive.Entry next) {
      super(hash, key, next);
      doubleVal0 = value;
    }

    /**
     * Returns a {@link Double} whose value is the <code>double</code> in this
     * entry.
     * <p/>
     * <b>Warning</b>: This method uses auto-boxing, and is intended primarily
     * for debugging purposes.
     *
     * @return a {@link Double} whose value is the <code>double</code> in this
     *         entry
     */
    public Object getValue() {
      return doubleVal0;
    }

    /**
     * Returns 1, the number of <code>double</code>s associated with a key.
     *
     * @return 1, the number of <code>double</code>s associated with a key
     */
    public int numDoubles() {
      return 1;
    }

    /**
     * Returns the <code>double</code> value associated with the key in this
     * entry.
     *
     * @param index an ignored parameter
     * @return the <code>double</code> value associated with the key in this
     *         entry
     */
    public double getDoubleValue(int index) {
      return doubleVal0;
    }

    /**
     * Sets the <code>double</code> value associated with the key in this
     * entry.
     *
     * @param index an ignored parameter
     * @param value the value to associated with the key in this entry
     * @return the old value associated with the key in this entry
     */
    public double set(int index, double value) {
      double oldVal = doubleVal0;
      doubleVal0 = value;
      return oldVal;
    }

    /**
     * Adds the specified amount to the <code>double</code> associated with the
     * key in this entry.
     *
     * @param index  an ignored parameter
     * @param addend the amount to add to the <code>double</code> associated
     *               with the key in this entry
     */
    public void add(int index, double addend) {
      doubleVal0 += addend;
    }

    /**
     * Makes the data membes in this entry be identical to those in the
     * specified entry by performing a shallow copy.
     *
     * @param copyFrom the entry from which to copy
     */
    public void copyValuesFrom(HashMapPrimitive.Entry copyFrom) {
      Entry other = (Entry) copyFrom;
      this.doubleVal0 = other.doubleVal0;
    }

    public String toString() {
      return key + "=" + doubleVal0;
    }

    public int hashCode() {
      long v = Double.doubleToLongBits(doubleVal0);
      return keyHash ^ (int) (v ^ (v >>> 32));
    }

    public boolean equals(Object o) {
      if (!(o instanceof Entry)) {
	return false;
      }
      Entry other = (Entry) o;
      return ((key == null ? other.key == null : key.equals(other.key)) &&
	      doubleVal0 == other.doubleVal0);
    }

    public Object clone() {
      return new Entry(keyHash, key, doubleVal0, next);
    }

    public void writeValues(java.io.ObjectOutput out) throws IOException {
      out.writeDouble(doubleVal0);
    }

    public void readValues(java.io.ObjectInput in)
      throws IOException, ClassNotFoundException {
      doubleVal0 = in.readDouble();
    }
  }
}
