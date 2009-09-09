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
 * This class and its associated inner class provide templates for easily
 * creating implementations of maps to primitive types, by implementing
 * all primitive-specific methods to throw an
 * <code>UnsupportedOperationException</code>.  In this way, concrete
 * subclasses need only implement methods for the primitives they support.
 */
abstract public class AbstractMapToPrimitive<K>
  extends java.util.AbstractMap<K,Object> implements MapToPrimitive<K> {

  /**
   * Provides convenient abstract implementation of the
   * <code>MapToPrimitive.Entry</code> interface: all primitive-specific
   * methods are implemented to throw an
   * <code>UnsupportedOperationException</code>.  In this way, a concrete
   * subclass need only implement methods specific to the primitives it
   * supports.
   */
  abstract public static class Entry<K>
  implements MapToPrimitive.Entry<K>, java.io.Serializable {

    // data members
    /** The key of this map entry. */
    transient protected K key;

    public K getKey() { return key; }

    /**
     * Returns <tt>0</tt>.
     */
    public int numBytes() {
      return 0;
    }

    /**
     * Gets the <code>byte</code> value associated with the key contained in
     * this entry. If an implementor associates multiple <code>byte</code>
     * values, this method retrieves the first one.
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>byte</code>s with its keys
     */
    public byte getByteValue() {
      return getByteValue(0);
    }

    /**
     * Returns the <code>byte</code> value of the specified index associated
     * with the key in this map entry.  If the implementor associates only
     * a single <code>byte</code> with its key, then the specified index must
     * be <code>0</code>.
     *
     * @param index the index of the <code>byte</code> value to retrieve
     * @throws IllegalArgumentException if
     * <pre>0 <= index < numBytes()</pre>
     * is <code>false</code>
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>byte</code>s with its key
     */
    public byte getByteValue(int index) {
      throw new UnsupportedOperationException();
    }

    /**
     * Returns <tt>0</tt>.
     */
    public int numShorts() {
      return 0;
    }

    /**
     * Gets the <code>short</code> value associated with the key contained in
     * this entry. If an implementor associates multiple <code>short</code>
     * values, this method retrieves the first one.
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>short</code>s with its keys
     */
    public short getShortValue() {
      return getShortValue(0);
    }

    /**
     * Returns the <code>short</code> value of the specified index associated
     * with the key in this map entry.  If the implementor associates only
     * a single <code>short</code> with its key, then the specified index must
     * be <code>0</code>.
     *
     * @param index the index of the <code>short</code> value to retrieve
     * @throws IllegalArgumentException if
     * <pre>0 <= index < numShorts()</pre>
     * is <code>false</code>
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>short</code>s with its key
     */
    public short getShortValue(int index) {
      throw new UnsupportedOperationException();
    }

    /**
     * Returns <tt>0</tt>.
     */
    public int numChars() {
      return 0;
    }

    /**
     * Gets the <code>char</code> value associated with the key contained in
     * this entry. If an implementor associates multiple <code>char</code>
     * values, this method retrieves the first one.
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>char</code>s with its keys
     */
    public char getCharValue() {
      return getCharValue(0);
    }

    /**
     * Returns the <code>char</code> value of the specified index associated
     * with the key in this map entry.  If the implementor associates only
     * a single <code>char</code> with its key, then the specified index must
     * be <code>0</code>.
     *
     * @param index the index of the <code>char</code> value to retrieve
     * @throws IllegalArgumentException if
     * <pre>0 <= index < numChars()</pre>
     * is <code>false</code>
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>char</code>s with its key
     */
    public char getCharValue(int index) {
      throw new UnsupportedOperationException();
    }

    /**
     * Returns <tt>0</tt>.
     */
    public int numInts() {
      return 0;
    }

    /**
     * Gets the <code>int</code> value associated with the key contained in
     * this entry. If an implementor associates multiple <code>int</code>
     * values, this method retrieves the first one.
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>int</code>s with its keys
     */
    public int getIntValue() {
      return getIntValue(0);
    }

    /**
     * Returns the <code>int</code> value of the specified index associated
     * with the key in this map entry.  If the implementor associates only
     * a single <code>int</code> with its key, then the specified index must
     * be <code>0</code>.
     *
     * @param index the index of the <code>int</code> value to retrieve
     * @throws IllegalArgumentException if
     * <pre>0 <= index < numInts()</pre>
     * is <code>false</code>
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>int</code>s with its key
     */
    public int getIntValue(int index) {
      throw new UnsupportedOperationException();
    }

    /**
     * Returns <tt>0</tt>.
     */
    public int numLongs() {
      return 0;
    }

    /**
     * Gets the <code>long</code> value associated with the key contained in
     * this entry. If an implementor associates multiple <code>long</code>
     * values, this method retrieves the first one.
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>long</code>s with its keys
     */
    public long getLongValue() {
      return getLongValue(0);
    }

    /**
     * Returns the <code>long</code> value of the specified index associated
     * with the key in this map entry.  If the implementor associates only
     * a single <code>long</code> with its key, then the specified index must
     * be <code>0</code>.
     *
     * @param index the index of the <code>long</code> value to retrieve
     * @throws IllegalArgumentException if
     * <pre>0 <= index < numLongs()</pre>
     * is <code>false</code>
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>long</code>s with its key
     */
    public long getLongValue(int index) {
      throw new UnsupportedOperationException();
    }

    /**
     * Returns <tt>0</tt>.
     */
    public int numFloats() {
      return 0;
    }

    /**
     * Gets the <code>float</code> value associated with the key contained in
     * this entry. If an implementor associates multiple <code>float</code>
     * values, this method retrieves the first one.
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>float</code>s with its keys
     */
    public float getFloatValue() {
      return getFloatValue(0);
    }

    /**
     * Returns the <code>float</code> value of the specified index associated
     * with the key in this map entry.  If the implementor associates only
     * a single <code>float</code> with its key, then the specified index must
     * be <code>0</code>.
     *
     * @param index the index of the <code>float</code> value to retrieve
     * @throws IllegalArgumentException if
     * <pre>0 <= index < numFloats()</pre>
     * is <code>false</code>
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>float</code>s with its key
     */
    public float getFloatValue(int index) {
      throw new UnsupportedOperationException();
    }

    /**
     * Returns <tt>0</tt>.
     */
    public int numDoubles() {
      return 0;
    }

    /**
     * Gets the <code>double</code> value associated with the key contained in
     * this entry. If an implementor associates multiple <code>double</code>
     * values, this method retrieves the first one.
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>double</code>s with its keys
     */
    public double getDoubleValue() {
      return getDoubleValue(0);
    }

    /**
     * Returns the <code>double</code> value of the specified index associated
     * with the key in this map entry.  If the implementor associates only
     * a single <code>double</code> with its key, then the specified index must
     * be <code>0</code>.
     *
     * @param index the index of the <code>double</code> value to retrieve
     * @throws IllegalArgumentException if
     * <pre>0 <= index < numDoubles()</pre>
     * is <code>false</code>
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>double</code>s with its key
     */
    public double getDoubleValue(int index) {
      throw new UnsupportedOperationException();
    }


    // setters

    /**
     * Sets the <code>char</code> value for the key in this entry to be
     * the specified character.
     *
     * @param index the index of the character value to set
     * @param value the byte to which to set as a value for the key of
     * this map entry
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>byte</code>s with its key
     */
    public byte set(int index, byte value) {
      throw new UnsupportedOperationException();
    }

    public void add(byte addend) {
      add(0, addend);
    }
    public void add(int index, byte addend) {
      throw new UnsupportedOperationException();
    }

    /**
     * Sets the <code>char</code> value for the key in this entry to be
     * the specified character.
     *
     * @param index the index of the character value to set
     * @param value the character to set as a value for the key of
     * this map entry
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>char</code>s with its key
     */
    public char set(int index, char value) {
      throw new UnsupportedOperationException();
    }

    /**
     * Sets the <code>short</code> value for the key in this entry to be
     * the specified character.
     *
     * @param index the index of the character value to set
     * @param value the short to set as a value for the key of
     * this map entry
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>short</code>s with its key
     */
    public short set(int index, short value) {
      throw new UnsupportedOperationException();
    }

    public void add(short addend) {
      add(0, addend);
    }
    public void add(int index, short addend) {
      throw new UnsupportedOperationException();
    }

    /**
     * Sets the <code>char</code> value for the key in this entry to be
     * the specified character.
     *
     * @param index the index of the character value to set
     * @param value the character to set as a value for the key of
     * this map entry
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>int</code>s with its key
     */
    public int set(int index, int value) {
      throw new UnsupportedOperationException();
    }

    /**
     * Increments the <code>int</code> at index 0 by the specified amount.
     * This is simply a convenience method, completely equivalent to calling
     * <code>add(0,&nbsp;addend)</code>.
     *
     * @param addend the amount by which to increment the <code>int</code>
     * value at the specified index
     */
    public void add(int addend) {
      add(0, addend);
    }

    /**
     * Increments the <code>int</code> at the specified index by the specified
     * amount.
     *
     * @param index the index of the <code>int</code> value to be incremented
     * @param addend the amount by which to increment the <code>int</code>
     * value at the specified index
     */
    public void add(int index, int addend) {
      throw new UnsupportedOperationException();
    }

    /**
     * Sets the <code>char</code> value for the key in this entry to be
     * the specified character.
     *
     * @param index the index of the character value to set
     * @param value the long to set as a value for the key of
     * this map entry
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>long</code>s with its key
     */
    public long set(int index, long value) {
      throw new UnsupportedOperationException();
    }

    public void add(long addend) {
      add(0, addend);
    }

    public void add(int index, long addend) {
      throw new UnsupportedOperationException();
    }

    /**
     * Sets the <code>float</code> value for the key in this entry to be
     * the specified character.
     *
     * @param index the index of the character value to set
     * @param value the float to set as a value for the key of
     * this map entry
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>float</code>s with its key
     */
    public float set(int index, float value) {
      throw new UnsupportedOperationException();
    }

    public void add(float addend) {
      add(0, addend);
    }

    public void add(int index, float addend) {
      throw new UnsupportedOperationException();
    }

    /**
     * Sets the <code>char</code> value for the key in this entry to be
     * the specified character.
     *
     * @param index the index of the character value to set
     * @param value the double to set as a value for the key of
     * this map entry
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>double</code>s with its key
     */
    public double set(int index, double value) {
      throw new UnsupportedOperationException();
    }

    public void add(double addend) {
      add(0, addend);
    }

    public void add(int index, double addend) {
      throw new UnsupportedOperationException();
    }

  }

  /**
   * Gets the map entry associated with the specified key, or <code>null</code>
   * if this map does not contain such a mapping.
   *
   * @param key the key for which to look up a map entry
   * @return the map entry for the specified key, or <code>null</code> if no
   * such mapping exists in this map
   */
  public abstract MapToPrimitive.Entry getEntry(K key);

  /**
   * Gets the map entry for the specified key and, as a side-effect, puts
   * the map entry at the front of the bucket list, indicating that it is
   * the most-recently used entry (useful for caches implementing a
   * bucket-LRU replacement scheme).  This is an optional operation.
   *
   * @param key the key whose map entry is to be retrieved and made the
   * MRU in its bucket inside the hash map
   * @throws UnsupportedOperationException if this map is not a hash map
   */
  public abstract MapToPrimitive.Entry getEntryMRU(K key);

  /**
   * Removes a random mapping from this map (optional operation).
   *
   * @throws UnsupportedOperationException if this operation is not supported
   */
  public void removeRandom() {
    throw new UnsupportedOperationException();
  }

  /**
   * Removes a random entry from the bucket at the specified index (optional
   * operation).
   *
   * @param bucketIndex the index of the bucket from which to remove an
   * element
   * @throws IllegalArgumentException if
   * <pre>0 &lt;= bucketIndex &lt; getCapacity()</pre>
   * is <code>false</code>
   * @throws UnsupportedOperationException if this map is not a hash map
   */
  public abstract void removeRandom(int bucketIndex);

  // put methods
  /**
   * Associates the specified <code>byte</code> value (at index 0) with the
   * specified key.
   *
   * @return the previous <code>byte</code> value associated with this key;
   * if there was no previous mapping for the specified key or if the
   * previous mapping mapped the key to <tt>0b</tt> (the default value for
   * byte instance variables), then <tt>0b</tt> is returned; the
   * <code>containsKey</code> method can be used to distinguish between
   * these two cases
   *
   * @throws UnsupportedOperationException if this map does not map keys
   * to <code>byte</code> values
   */
  public byte put(K key, byte value) {
    throw new UnsupportedOperationException();
  }
  public byte put(K key, int index, byte value) {
    throw new UnsupportedOperationException();
  }
  /**
   * Adds the specified addend to the <code>byte</code> value (at index 0)
   * associated with the specified key, or if no mapping previously existed
   * for the specified key, then this method adds a new map entry mapping
   * the key to the specified addend.  If this map maps keys to
   * multiple <code>byte</code> values, then the other <code>byte</code>s will
   * be set to their default instance-variable value, <tt>0</tt>.
   *
   * @param key the key whose <code>byte</code> value is to be incremented
   * @param addend the amount by which to increment the <code>byte</code> value
   * for the specified key
   */
  public void add(K key, byte addend) {
    add(key, 0, addend);
  }
  /**
   * Adds the specified addend to the <code>byte</code> value at the specified
   * index associated with the specified key, or if no mapping previously
   * existed for the specified key, then this method adds a new map entry
   * mapping the key to the specified addend at the specified index.  If this
   * map maps keys to multiple <code>byte</code> values, then the other
   * <code>byte</code>s will be set to their default instance-variable value,
   * <tt>0</tt>.
   *
   * @param key the key whose <code>byte</code> value is to be incremented,
   * or for which a mapping is to be added to the specified addend
   * @param index the index of the <code>byte</code> value to be incremented
   * @param addend the amount by which to increment the <code>byte</code> value
   * for the specified key
   */
  public void add(K key, int index, byte addend) {
    throw new UnsupportedOperationException();
  }
  public char put(K key, char value) {
    return put(key, 0, value);
  }
  public char put(K key, int index, char value) {
    throw new UnsupportedOperationException();
  }
  public short put(K key, short value) {
    return put(key, 0, value);
  }
  public short put(K key, int index, short value) {
    throw new UnsupportedOperationException();
  }
  /**
   * Adds the specified addend to the <code>short</code> value (at index 0)
   * associated with the specified key, or if no mapping previously existed
   * for the specified key, then this method adds a new map entry mapping
   * the key to the specified addend.  If this map maps keys to
   * multiple <code>short</code> values, then the other <code>short</code>s will
   * be set to their default instance-variable value, <tt>0</tt>.
   *
   * @param key the key whose <code>short</code> value is to be incremented
   * @param addend the amount by which to increment the <code>short</code> value
   * for the specified key
   */
  public void add(K key, short addend) {
    add(key, 0, addend);
  }
  /**
   * Adds the specified addend to the <code>short</code> value at the specified
   * index associated with the specified key, or if no mapping previously
   * existed for the specified key, then this method adds a new map entry
   * mapping the key to the specified addend at the specified index.  If this
   * map maps keys to multiple <code>short</code> values, then the other
   * <code>short</code>s will be set to their default instance-variable value,
   * <tt>0</tt>.
   *
   * @param key the key whose <code>short</code> value is to be incremented,
   * or for which a mapping is to be added to the specified addend
   * @param index the index of the <code>short</code> value to be incremented
   * @param addend the amount by which to increment the <code>short</code> value
   * for the specified key
   *
   * @throws IllegalArgumentException if the index is out of range
   */
  public void add(K key, int index, short addend) {
    throw new UnsupportedOperationException();
  }
  public int put(K key, int value) {
    return put(key, 0, value);
  }
  public int put(K key, int index, int value) {
    throw new UnsupportedOperationException();
  }
  /**
   * Adds the specified addend to the <code>int</code> value (at index 0)
   * associated with the specified key, or if no mapping previously existed
   * for the specified key, then this method adds a new map entry mapping
   * the key to the specified addend.  If this map maps keys to
   * multiple <code>int</code> values, then the other <code>int</code>s will
   * be set to their default instance-variable value, <tt>0</tt>.
   *
   * @param key the key whose <code>int</code> value is to be incremented
   * @param addend the amount by which to increment the <code>int</code> value
   * for the specified key
   */
  public void add(K key, int addend) {
    add(key, 0, addend);
  }
  /**
   * Adds the specified addend to the <code>int</code> value at the specified
   * index associated with the specified key, or if no mapping previously
   * existed for the specified key, then this method adds a new map entry
   * mapping the key to the specified addend at the specified index.  If this
   * map maps keys to multiple <code>int</code> values, then the other
   * <code>int</code>s will be set to their default instance-variable value,
   * <tt>0</tt>.
   *
   * @param key the key whose <code>int</code> value is to be incremented,
   * or for which a mapping is to be added to the specified addend
   * @param index the index of the <code>int</code> value to be incremented
   * @param addend the amount by which to increment the <code>int</code> value
   * for the specified key
   */
  public void add(K key, int index, int addend) {
    throw new UnsupportedOperationException();
  }
  public long put(K key, long addend) {
    return put(key, 0, addend);
  }
  public long put(K key, int index, long addend) {
    throw new UnsupportedOperationException();
  }
  /**
   * Adds the specified addend to the <code>long</code> value (at index 0)
   * associated with the specified key, or if no mapping previously existed
   * for the specified key, then this method adds a new map entry mapping
   * the key to the specified addend.  If this map maps keys to
   * multiple <code>long</code> values, then the other <code>long</code>s will
   * be set to their default instance-variable value, <tt>0</tt>.
   *
   * @param key the key whose <code>long</code> value is to be incremented
   * @param addend the amount by which to increment the <code>long</code> value
   * for the specified key
   */
  public void add(K key, long addend) {
    add(key, 0, addend);
  }
  /**
   * Adds the specified addend to the <code>long</code> value at the specified
   * index associated with the specified key, or if no mapping previously
   * existed for the specified key, then this method adds a new map entry
   * mapping the key to the specified addend at the specified index.  If this
   * map maps keys to multiple <code>long</code> values, then the other
   * <code>long</code>s will be set to their default instance-variable value,
   * <tt>0</tt>.
   *
   * @param key the key whose <code>long</code> value is to be incremented,
   * or for which a mapping is to be added to the specified addend
   * @param index the index of the <code>long</code> value to be incremented
   * @param addend the amount by which to increment the <code>long</code> value
   * for the specified key
   */
  public void add(K key, int index, long addend) {
    throw new UnsupportedOperationException();
  }
  public float put(K key, float value) {
    return put(key, 0, value);
  }
  public float put(K key, int index, float value) {
    throw new UnsupportedOperationException();
  }
  /**
   * Adds the specified addend to the <code>float</code> value (at index 0)
   * associated with the specified key, or if no mapping previously existed
   * for the specified key, then this method adds a new map entry mapping
   * the key to the specified addend.  If this map maps keys to
   * multiple <code>float</code> values, then the other <code>float</code>s will
   * be set to their default instance-variable value, <tt>0</tt>.
   *
   * @param key the key whose <code>float</code> value is to be incremented
   * @param addend the amount by which to increment the <code>float</code> value
   * for the specified key
   */
  public void add(K key, float addend) {
    add(key, 0, addend);
  }
  /**
   * Adds the specified addend to the <code>float</code> value at the specified
   * index associated with the specified key, or if no mapping previously
   * existed for the specified key, then this method adds a new map entry
   * mapping the key to the specified addend at the specified index.  If this
   * map maps keys to multiple <code>float</code> values, then the other
   * <code>float</code>s will be set to their default instance-variable value,
   * <tt>0</tt>.
   *
   * @param key the key whose <code>float</code> value is to be incremented,
   * or for which a mapping is to be added to the specified addend
   * @param index the index of the <code>float</code> value to be incremented
   * @param addend the amount by which to increment the <code>float</code> value
   * for the specified key
   */
  public void add(K key, int index, float addend) {
    throw new UnsupportedOperationException();
  }
  public double put(K key, double value) {
    return put(key, 0, value);
  }
  public double put(K key, int index, double value) {
    throw new UnsupportedOperationException();
  }
  /**
   * Adds the specified addend to the <code>double</code> value (at index 0)
   * associated with the specified key, or if no mapping previously existed for
   * the specified key, then this method adds a new map entry mapping the key
   * to the specified addend.  If this map maps keys to multiple
   * <code>double</code> values, then the other <code>double</code>s will be
   * set to their default instance-variable value, <tt>0</tt>.
   *
   * @param key the key whose <code>double</code> value is to be incremented
   * @param addend the amount by which to increment the <code>double</code>
   * value for the specified key
   */
  public void add(K key, double addend) {
    add(key, 0, addend);
  }
  /**
   * Adds the specified addend to the <code>double</code> value at the specified
   * index associated with the specified key, or if no mapping previously
   * existed for the specified key, then this method adds a new map entry
   * mapping the key to the specified addend at the specified index.  If this
   * map maps keys to multiple <code>double</code> values, then the other
   * <code>double</code>s will be set to their default instance-variable value,
   * <tt>0</tt>.
   *
   * @param key the key whose <code>double</code> value is to be incremented,
   * or for which a mapping is to be added to the specified addend
   * @param index the index of the <code>double</code> value to be incremented
   * @param addend the amount by which to increment the <code>double</code>
   * value for the specified key
   */
  public void add(K key, int index, double addend) {
    throw new UnsupportedOperationException();
  }

  // overridden version of java.util.AbstractMap.toString method
    /**
     * Returns a string representation of this map.  The string representation
     * consists of a list of key-value mappings in the order returned by the
     * map's <tt>entrySet</tt> view's iterator, enclosed in braces
     * (<tt>"{}"</tt>).  Adjacent mappings are separated by the characters
     * <tt>", "</tt> (comma and space).  Each key-value mapping is rendered as
     * the key followed by an equals sign (<tt>"="</tt>) followed by the
     * associated value, or an implementation-dependent, comma-separated list
     * of values, if the concrete subclass supports multiple primitive values
     * for its keys.
     *
     * This implementation creates an empty string buffer, appends a left
     * brace, and iterates over the map's <tt>entrySet</tt> view, appending
     * the string representation of each <tt>map.entry</tt> in turn.  After
     * appending each entry except the last, the string <tt>", "</tt> is
     * appended.  Finally a right brace is appended.  A string is obtained
     * from the stringbuffer, and returned.
     *
     * @return a String representation of this map.
     */
  public String toString() {
    int max = size() - 1;
    StringBuffer buf = new StringBuffer();
    java.util.Iterator i = entrySet().iterator();

    buf.append("{");
    for (int j = 0; j <= max; j++) {
        Entry e = (Entry) (i.next());
        buf.append(e);
        if (j < max)
            buf.append(", ");
    }
    buf.append("}");
    return buf.toString();
  }

}
