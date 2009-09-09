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

import java.util.Map;
import java.util.Set;

/**
 * Specifies methods that allow mappings from arbitrary objects to primitive
 * types that are bundled in the map entries for efficiency.  This interface
 * specifies methods to access map entries directly via a key in the map,
 * (via the {@link #getEntry} and {@link #getEntryMRU}
 * methods) both to obtain the "canonical" key objects contained in the map,
 * as well as to get at the primitive values for those keys more efficiently.
 * Finally, this interface specifies methods useful to building caches that
 * wish to implement various cache-replacement strategies.
 * <p>
 * The methods specified both in this interface and in the
 * <code>MapToPrimitive.Entry</code> interface allow implementors to associate
 * multiple primitive types with each key, and for each primitive type,
 * there can be multiple vales of that type, accessible via index.  For
 * example, if an implementation simply stored one <code>byte</code> value
 * for each key, then it would create map entries that extended
 * <code>AbstractPrimitiveMapEntry</code> and that overrode only the
 * {@link MapToPrimitive.Entry#numBytes()},
 * {@link MapToPrimitive.Entry#getByteValue()} and
 * {@link MapToPrimitive.Entry#getByteValue(int)} methods.  The
 * <code>MapToPrimitive.Entry#numBytes()</code> method would simply return
 * <tt>1</tt>, and the other two methods would return the byte value
 * (the implementation of <code>getByteValue(int)</code> should throw an
 * exception if the argument is anything other than <tt>0</tt>, as specified
 * in the API documentation for that method).
 */
public interface MapToPrimitive<K> extends java.util.Map<K, Object> {

  /**
   * Interface that provides methods for all the possible primitive
   * types that could be associated with keys in a <code>MapToPrimitive</code>
   * map.
   */
  public interface Entry<K> extends java.util.Map.Entry<K,Object> {

    /**
     * Sets the key of this map entry to be the specified key if it is
     * equal to the current key.  If the specified key is not equal to the
     * current key (as determined by the current key's <code>equals</code>
     * method), this method does nothing and returns <code>false</code>.
     *
     * @param key a key equal to the current key as determined by
     * the current key's <code>equals</code> method
     * @return <code>true</code> if the specified key was equal to the current
     * key and was therefore substituted for the current key, <code>false</code>
     * otherwise
     */
    boolean replaceKey(K key);

    /**
     * Returns the number of <code>byte</code> values associated with the key
     * in this map entry, or <tt>0</tt>0 if the implementor does not
     * associate <code>byte</code>s with its key
     */
    int numBytes();

    /**
     * Gets the <code>byte</code> value associated with the key contained in
     * this entry. If an implementor associates multiple <code>byte</code>
     * values, this method retrieves the first one.
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>byte</code>s with its keys
     */
    byte getByteValue();

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
    byte getByteValue(int index);

    /**
     * Returns the number of <code>short</code> values associated with the key
     * in this map entry, or <tt>0</tt> if the implementor does not
     * associate <code>short</code>s with its key
     */
    int numShorts();

    /**
     * Gets the <code>short</code> value associated with the key contained in
     * this entry. If an implementor associates multiple <code>short</code>
     * values, this method retrieves the first one.
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>short</code>s with its keys
     */
    short getShortValue();

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
    short getShortValue(int index);

    /**
     * Returns the number of <code>char</code> values associated with the key
     * in this map entry, or <tt>0</tt> if the implementor does not
     * associate <code>char</code>s with its key
     */
    int numChars();

    /**
     * Gets the <code>char</code> value associated with the key contained in
     * this entry. If an implementor associates multiple <code>char</code>
     * values, this method retrieves the first one.
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>char</code>s with its keys
     */
    char getCharValue();

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
    char getCharValue(int index);

    /**
     * Returns the number of <code>int</code> values associated with the key
     * in this map entry, or <tt>0</tt> if the implementor does not
     * associate <code>int</code>s with its key
     */
    int numInts();

    /**
     * Gets the <code>int</code> value associated with the key contained in
     * this entry. If an implementor associates multiple <code>int</code>
     * values, this method retrieves the first one.
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>int</code>s with its keys
     */
    int getIntValue();

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
    int getIntValue(int index);

    /**
     * Returns the number of <code>long</code> values associated with the key
     * in this map entry, or <tt>0</tt> if the implementor does not
     * associate <code>long</code>s with its key
     */
    int numLongs();

    /**
     * Gets the <code>long</code> value associated with the key contained in
     * this entry. If an implementor associates multiple <code>long</code>
     * values, this method retrieves the first one.
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>long</code>s with its keys
     */
    long getLongValue();

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
    long getLongValue(int index);

    /**
     * Returns the number of <code>float</code> values associated with the key
     * in this map entry, or <tt>0</tt> if the implementor does not
     * associate <code>float</code>s with its key
     */
    int numFloats();

    /**
     * Gets the <code>float</code> value associated with the key contained in
     * this entry. If an implementor associates multiple <code>float</code>
     * values, this method retrieves the first one.
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>float</code>s with its keys
     */
    float getFloatValue();

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
    float getFloatValue(int index);

    /**
     * Returns the number of <code>double</code> values associated with the key
     * in this map entry, or <tt>0</tt> if the implementor does not
     * associate <code>double</code>s with its key
     */
    int numDoubles();

    /**
     * Gets the <code>double</code> value associated with the key contained in
     * this entry. If an implementor associates multiple <code>double</code>
     * values, this method retrieves the first one.
     *
     * @throws UnsupportedOperationException if the implementor does not
     * associate <code>double</code>s with its keys
     */
    double getDoubleValue();

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
    double getDoubleValue(int index);


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
    byte set(int index, byte value);

    /**
     * Increments the <code>byte</code> at index 0 by the specified amount.
     * This is simply a convenience method, completely equivalent to calling
     * <code>add(0,&nbsp;addend)</code>.
     *
     * @param addend the amount by which to increment the <code>byte</code>
     * value at the specified index
     */
    void add(byte addend);
    /**
     * Increments the <code>byte</code> at the specified index by the specified
     * amount.
     *
     * @param index the index of the <code>byte</code> value to be incremented
     * @param addend the amount by which to increment the <code>byte</code>
     * value at the specified index
     */
    void add(int index, byte addend);

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
    char set(int index, char value);

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
    short set(int index, short value);

    /**
     * Increments the <code>short</code> at index 0 by the specified amount.
     * This is simply a convenience method, completely equivalent to calling
     * <code>add(0,&nbsp;addend)</code>.
     *
     * @param addend the amount by which to increment the <code>short</code>
     * value at the specified index
     */
    void add(short addend);
    /**
     * Increments the <code>short</code> at the specified index by the specified
     * amount.
     *
     * @param index the index of the <code>short</code> value to be incremented
     * @param addend the amount by which to increment the <code>short</code>
     * value at the specified index
     */
    void add(int index, short addend);

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
    int set(int index, int value);

    /**
     * Increments the <code>int</code> at index 0 by the specified amount.
     * This is simply a convenience method, completely equivalent to calling
     * <code>add(0,&nbsp;addend)</code>.
     *
     * @param addend the amount by which to increment the <code>int</code>
     * value at the specified index
     */
    void add(int addend);

    /**
     * Increments the <code>int</code> at the specified index by the specified
     * amount.
     *
     * @param index the index of the <code>int</code> value to be incremented
     * @param addend the amount by which to increment the <code>int</code>
     * value at the specified index
     */
    void add(int index, int addend);

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
    long set(int index, long value);

    /**
     * Increments the <code>long</code> at index 0 by the specified amount.
     * This is simply a convenience method, completely equivalent to calling
     * <code>add(0,&nbsp;addend)</code>.
     *
     * @param addend the amount by which to increment the <code>long</code>
     * value at the specified index
     */
    void add(long addend);
    /**
     * Increments the <code>long</code> at the specified index by the specified
     * amount.
     *
     * @param index the index of the <code>long</code> value to be incremented
     * @param addend the amount by which to increment the <code>long</code>
     * value at the specified index
     */
    void add(int index, long addend);

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
    float set(int index, float value);

    /**
     * Increments the <code>float</code> at index 0 by the specified amount.
     * This is simply a convenience method, completely equivalent to calling
     * <code>add(0,&nbsp;addend)</code>.
     *
     * @param addend the amount by which to increment the <code>float</code>
     * value at the specified index
     */
    void add(float addend);
    /**
     * Increments the <code>float</code> at the specified index by the specified
     * amount.
     *
     * @param index the index of the <code>float</code> value to be incremented
     * @param addend the amount by which to increment the <code>float</code>
     * value at the specified index
     */
    void add(int index, float addend);

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
    double set(int index, double value);

    /**
     * Increments the <code>double</code> at index 0 by the specified amount.
     * This is simply a convenience method, completely equivalent to calling
     * <code>add(0,&nbsp;addend)</code>.
     *
     * @param addend the amount by which to increment the <code>double</code>
     * value at the specified index
     */
    void add(double addend);
    /**
     * Increments the <code>double</code> at the specified index by the specified
     * amount.
     *
     * @param index the index of the <code>double</code> value to be incremented
     * @param addend the amount by which to increment the <code>double</code>
     * value at the specified index
     */
    void add(int index, double addend);
  }

  /**
   * Gets the map entry associated with the specified key, or <code>null</code>
   * if this map does not contain such a mapping.  This is the preferred way
   * to obtain a value for a key, as nullity serves as a check for whether
   * a mapping exists for the specified key.
   *
   * @param key the key for which to look up a map entry
   * @return the map entry for the specified key, or <code>null</code> if no
   * such mapping exists in this map
   */
  Entry getEntry(K key);

  Entry getEntry(K key, int hashCode);

  /**
   * Returns the map entry for the specified key and, as a side-effect, puts the
   * map entry at the front of the bucket list, indicating that it is the
   * most-recently used entry (useful for caches implementing a bucket-LRU
   * replacement scheme).  This is an optional operation.
   *
   * @param key the key whose map entry is to be retrieved and made the MRU in
   *            its bucket inside the hash map
   * @return the map entry for the specified key and, as a side-effect, puts the
   *         map entry at the front of the bucket list, indicating that it is
   *         the most-recently used entry
   *
   * @throws UnsupportedOperationException if this map is not a hash map
   */
  Entry getEntryMRU(K key);

  Entry getEntryMRU(K key, int hashCode);

  /**
   * Removes a random mapping from this map (optional operation).
   *
   * @throws UnsupportedOperationException if this operation is not supported
   */
  void removeRandom();

  /**
   * Removes a random from the bucket at the specified index (optional
   * operation).
   *
   * @param bucketIndex the index of the bucket from which to remove an
   * element
   * @throws IllegalArgumentException if
   * <pre>0 <= bucketIndex < getCapacity()</pre>
   * is <code>false</code>
   * @throws UnsupportedOperationException if this map is not a hash map
   */
  void removeRandom(int bucketIndex);

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
  byte put(K key, byte value);
  byte put(K key, int index, byte value);
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
  void add(K key, byte addend);
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
  void add(K key, int index, byte addend);
  char put(K key, char value);
  char put(K key, int index, char value);
  short put(K key, short value);
  short put(K key, int index, short value);
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
  void add(K key, short addend);
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
  void add(K key, int index, short addend);
  int put(K key, int value);
  int put(K key, int index, int value);
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
  void add(K key, int addend);
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
  void add(K key, int index, int addend);
  long put(K key, long addend);
  long put(K key, int index, long addend);
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
  void add(K key, long addend);
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
  void add(K key, int index, long addend);
  float put(K key, float value);
  float put(K key, int index, float value);
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
  void add(K key, float addend);
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
  void add(K key, int index, float addend);
  double put(K key, double value);
  double put(K key, int index, double value);
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
  void add(K key, double addend);
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
  void add(K key, int index, double addend);
}
