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
 * A map for storing arbitrary {@link Object} instances as keys with ordered
 * pairs of <code>double</code>s as values.
 */
public class HashMapTwoDoubles<K> extends HashMapDouble<K> {
  /**
   * Constructs a new, empty map with the specified initial capacity and the
   * specified load factor.
   *
   * @param initialCapacity the initial capacity of the HashMap.
   * @param loadFactor      the load factor of the HashMap
   * @throws IllegalArgumentException if the initial capacity is less than zero,
   *                                  or if the load factor is nonpositive.
   */
  public HashMapTwoDoubles(int initialCapacity, float loadFactor) {
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
  public HashMapTwoDoubles(int initialCapacity) {
    this(initialCapacity, defaultLoadFactor);
  }

  /**
   * Constructs a new, empty map with a
   * {@link HashMapPrimitive#defaultInitialCapacity default capacity} and
   * {@link #defaultLoadFactor load factor}.
   */
  public HashMapTwoDoubles() {
    this(defaultInitialCapacity, defaultLoadFactor);
  }

  /**
   * Constructs a new map with the same mappings as the given map.  The map is
   * created with a capacity of twice the number of mappings in the given map or
   * {@link HashMapPrimitive#defaultInitialCapacity the default size} (whichever
   * is greater), and {@link HashMapPrimitive#defaultLoadFactor a default load
   * factor}.
   *
   * @param t the map whose mappings are to be placed in this map.
   */
  public HashMapTwoDoubles(Map t) {
    this(Math.max(2 * t.size(), defaultInitialCapacity), defaultLoadFactor);
    putAll(t);
  }

  /**
   * Gets a map entry for this type of map, containing a key and a pair of
   * <code>double</code>s.
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
   * A map entry for this type of map, containing a key and a pair of
   * <code>double</code>s.
   */
  protected static class Entry<K> extends HashMapPrimitive.Entry<K> {
    transient protected double doubleVal0;
    transient protected double doubleVal1;

    /**
     * Constructs a new entry for this type of map with default values for the
     * data members (this default constructor here for serialization reasons).
     */
    public Entry() {
    }

    /**
     * Constructs a new entry for a map from objects to pairs of
     * <code>double</code>s.
     *
     * @param hash the hash value for the specified key
     * @param key  the key for this entry to wrap
     * @param next the next pointer in this entry&rsquo;s singly-linked list
     */
    protected Entry(int hash, K key, HashMapPrimitive.Entry<K> next) {
      super(hash, key, next);
    }

    /**
     * Constructs a new entry for a map from objects to pairs of
     * <code>double</code>s.
     *
     * @param hash   the hash value for the specified key
     * @param key    the key for this entry to wrap
     * @param value0 the first <code>double</code> value to be associated with
     *               the specified key in the map
     * @param value1 the second <code>double</code> value to be associated with
     *               the specified key in the map
     * @param next   the next pointer in this entry&rsquo;s singly-linked list
     */
    protected Entry(int hash, K key, double value0, double value1,
		    HashMapPrimitive.Entry<K> next) {
      super(hash, key, next);
      doubleVal0 = value0;
      doubleVal1 = value1;
    }

    /**
     * Returns 2, the number of <code>double</code>s associated with a key.
     *
     * @return 2, the number of <code>double</code>s associated with a key
     */
    public int numDoubles() {
      return 2;
    }

    /**
     * Returns the <code>double</code> value at the specified index associated
     * with the key in this entry.
     *
     * @param index the index of the <code>double</code> value to return
     * @return the <code>double</code> value at the specified index associated
     *         with the key in this entry
     */
    public final double getDoubleValue(int index) {
      switch (index) {
      case 0:
	return doubleVal0;
      case 1:
	return doubleVal1;
      default:
	throw new IllegalArgumentException();
      }
    }

    /**
     * Sets the <code>double</code> value at the specified index associated with
     * the key in this entry.
     *
     * @param index the index of the <code>double</code> value to set
     * @param value the value to associated with the key in this entry
     * @return the old value at the specified index associated with the key in
     *         this entry
     */
    public final double set(int index, double value) {
      double oldVal = 0;
      switch (index) {
      case 0:
	oldVal = doubleVal0;
	doubleVal0 = value;
	break;
      case 1:
	oldVal = doubleVal1;
	doubleVal1 = value;
	break;
      default:
	throw new IllegalArgumentException();
      }
      return oldVal;
    }

    /**
     * Adds the specified amount to the <code>double</code> at the specified
     * index associated with the key in this entry.
     *
     * @param index  the index of the <code>double</code> value to increase
     * @param addend the amount to add to the <code>double</code> associated
     *               with the key in this entry
     */
    public final void add(int index, double addend) {
      switch (index) {
      case 0:
	doubleVal0 += addend;
	break;
      case 1:
	doubleVal1 += addend;
	break;
      default:
	throw new IllegalArgumentException();
      }
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
      this.doubleVal1 = other.doubleVal1;
    }

    public String toString() {
      return key + "=" + doubleVal0 + "," + doubleVal1;
    }

    public int hashCode() {
      long v1 = Double.doubleToLongBits(doubleVal0);
      long v2 = Double.doubleToLongBits(doubleVal1);
      int doubleVal1Hash = (int) (v1 ^ (v1 >>> 32));
      int doubleVal2Hash = (int) (v2 ^ (v2 >>> 32));
      return ((keyHash ^ doubleVal1Hash) << 2) ^ doubleVal2Hash;
    }

    public boolean equals(Object o) {
      if (!(o instanceof Entry)) {
	return false;
      }
      Entry other = (Entry) o;
      return ((key == null ? other.key == null : key.equals(other.key)) &&
	      doubleVal0 == other.doubleVal0 &&
	      doubleVal1 == other.doubleVal1);
    }

    public Object clone() {
      return new Entry<K>(keyHash, key, doubleVal0, doubleVal1, next);
    }

    public void writeValues(java.io.ObjectOutput out) throws IOException {
      out.writeDouble(doubleVal0);
      out.writeDouble(doubleVal1);
    }

    public void readValues(java.io.ObjectInput in)
      throws IOException, ClassNotFoundException {
      doubleVal0 = in.readDouble();
      doubleVal1 = in.readDouble();
    }
  }
}
