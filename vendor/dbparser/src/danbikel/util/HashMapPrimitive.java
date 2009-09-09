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

import java.io.*;
import java.util.*;
import java.math.BigInteger;

/**
 * A home-grown hash map from objects to indexed sequences of primitives.
 * Concrete implementations need only provide a map entry implementation and
 * provide a concrete implementation of the {@link #getNewEntry(int, Object,
 * HashMapPrimitive.Entry)} method.
 */
abstract public class HashMapPrimitive<K>
  extends AbstractMapToPrimitive<K>
  implements FlexibleMap<K, Object>, Serializable {
  // constants
  /**
   * The default load factor, 0.75f.
   */
  protected final static float defaultLoadFactor = 0.75f;
  /**
   * The default initial capacity, 11.
   */
  protected final static int defaultInitialCapacity = 11;
  /**
   * The hash code bit mask, <code>0x7fffffff</code>.
   */
  protected final static int hashCodeBitmask = 0x7fffffff;
  /**
   * The maximum capacity (number of buckets) of this hash map, {@link
   * Integer#MAX_VALUE}.
   */
  protected final static BigInteger maxCapacity =
    new BigInteger(String.valueOf(Integer.MAX_VALUE));

  // inner classes
  /**
   * A still-abstract extension of the {@link AbstractMapToPrimitive.Entry} that
   * adds a next pointer and an <code>int</code> to cache the hash value of the
   * key held by this entry.
   */
  abstract public static class Entry<K> extends AbstractMapToPrimitive.Entry<K>
    implements Externalizable {
    /**
     * The hash value of the key of this entry, cached here for efficiency.
     */
    transient protected int keyHash;
    /**
     * The next pointer of this entry&rsquo;s singly-linked list.
     */
    transient protected Entry<K> next;

    /**
     * Constructs a new entry, with all default values for data members (this
     * no-argument constructor necessary for serialization).
     */
    public Entry() {
    }

    /**
     * Constructs a new entry with the specified values for its data members.
     *
     * @param keyHash the hash value of the specified key
     * @param key     the key of this hash map entry
     * @param next    the next pointer of this entry&rsquo;s singly-linked list
     *                (can be <code>null</code>)
     */
    protected Entry(int keyHash, K key, Entry<K> next) {
      this.keyHash = keyHash;
      this.key = key;
      this.next = next;
    }

    /**
     * Returns a new copy of this type of map entry.
     */
    protected abstract Object clone();

    /**
     * Replaces the key of this entry with the specified key.  A replacement
     * will only occur if the specified key has the same hash value and is equal
     * to the existing key of this entry (as determined by the existing
     * key&rsquo;s <code>equals</code> method).
     *
     * @param key the new key of this entry
     * @return whether or not the replacement occurred
     */
    public boolean replaceKey(K key) {
      if (this.key != key && this.key.equals(key)) {
	this.key = key;
	return true;
      }
      return false;
    }

    /**
     * Throws an {@link UnsupportedOperationException}.  This default
     * implementation may be overridden by subclasses, but should be used with
     * care, as auto-boxing might need to happen.
     *
     * @return nothing
     *
     * @throws UnsupportedOperationException under all circumstances
     */
    public Object getValue() {
      throw new UnsupportedOperationException();
    }

    /**
     * Throws an {@link UnsupportedOperationException}.
     *
     * @param value ignored
     * @return nothing
     *
     * @throws UnsupportedOperationException under all circumstances
     */
    public Object setValue(Object value) {
      throw new UnsupportedOperationException();
    }

    public abstract boolean equals(Object o);

    public abstract int hashCode();

    public abstract String toString();

    /**
     * Copies the values from the specified entry to this entry.
     *
     * @throws ClassCastException if the specified entry is not of the same
     *                            run-time type as this entry
     */
    public abstract void copyValuesFrom(Entry copyFrom);

    public void writeExternal(java.io.ObjectOutput out) throws IOException {
      out.writeObject(key);
      writeValues(out);
    }

    public void readExternal(java.io.ObjectInput in)
      throws IOException, ClassNotFoundException {
      key = (K) in.readObject();
      keyHash = keyHash(key);
      readValues(in);
    }

    protected abstract void writeValues(java.io.ObjectOutput out)
      throws IOException;

    protected abstract void readValues(java.io.ObjectInput in)
      throws IOException, ClassNotFoundException;
  }

  private class EntryIterator<K> implements Iterator<Map.Entry<K, Object>> {
    // data members
    Entry<K> current = null;
    int currBucket = -1;
    Entry<K> lastReturned = null;
    int lastReturnedBucket = -1;
    int expectedModCount = modCount;

    EntryIterator() {
      advance();
    }

    void advance() {
      if (size == 0 || currBucket == entries.length)
	return;
      // if we have a current entry and its next field is non-null, advance
      if (current != null && current.next != null) {
	current = current.next;
	return;
      }
      // otherwise, go to next bucket and search until we find a non-empty one
      for (currBucket = currBucket + 1;
	   currBucket < entries.length; currBucket++) {
	if (entries[currBucket] != null) {
	  current = entries[currBucket];
	  return;
	}
      }
      current = null;
    }

    public boolean hasNext() {
      if (modCount != expectedModCount) {
	throw new ConcurrentModificationException();
      }
      return current != null;
    }

    public MapToPrimitive.Entry<K> next() {
      if (modCount != expectedModCount)
	throw new ConcurrentModificationException();
      lastReturned = current;
      lastReturnedBucket = currBucket;
      advance();
      return lastReturned;
    }

    public void remove() {
      if (lastReturned == null) {
	throw new IllegalStateException();
      }
      if (modCount != expectedModCount) {
	throw new ConcurrentModificationException();
      }
      Entry<K> prev = entries[lastReturnedBucket];
      if (prev == lastReturned) {
	// if the last returned entry was the first element in its bucket
	entries[lastReturnedBucket] = lastReturned.next;
      } else {
	// otherwise, find previous element in singly-linked list
	while (prev.next != lastReturned) {
	  prev = prev.next;
	}
	prev.next = lastReturned.next;
      }
      size--;
      modCount++;
      expectedModCount++;
    }
  }

  // data members
  private transient int size = 0;
  private float loadFactor;
  /**
   * The value of loadFactor * capacity.
   */
  private float threshold;
  private transient Entry[] entries;
  private transient int modCount = 0;
  private transient volatile EntrySet<K> entrySet = null;

  // constructors
  /**
   * Constructs a new map from objects to primitive values, using the default
   * initial capacity and the default load factor.
   *
   * @see #defaultInitialCapacity
   * @see #defaultLoadFactor
   */
  public HashMapPrimitive() {
    this(defaultInitialCapacity, defaultLoadFactor);
  }

  /**
   * Constructs a new map from objects to primitive values, using the default
   * load factory
   *
   * @param initialCapacity the initial capacity of this map
   */
  public HashMapPrimitive(int initialCapacity) {
    this(initialCapacity, defaultLoadFactor);
  }

  /**
   * Constructs a new map from objects to primitive values.
   *
   * @param initialCapacity the initial capacity of this map
   * @param loadFactor      the load factor of this map
   */
  public HashMapPrimitive(int initialCapacity, float loadFactor) {
    BigInteger capacity = new BigInteger(String.valueOf(initialCapacity));
    if (!capacity.isProbablePrime(100)) {
      capacity = capacity.nextProbablePrime();
    }
    capacity =
      capacity.compareTo(maxCapacity) < 0 ? capacity : maxCapacity;
    entries = new Entry[capacity.intValue()];
    this.loadFactor = loadFactor;
    threshold = loadFactor * entries.length;
  }

  public HashMapPrimitive(Map<? extends K, Object> map) {
    this(map.size());
    this.putAll(map);
  }

  /**
   * In order to make use of java.util.AbstractMap&rsquo;s keySet() and values()
   * views which are built on top of the provided entrySet view, we need the set
   * to be over Map.Entry<K,Object>.  However, we crucially use covariant return
   * types in EntryIterator<K> to have the next() method return a subtype of
   * Map.Entry<K,Object>, which is MapToPrimitive.Entry<K>.
   */
  private class EntrySet<K> extends AbstractSet<Map.Entry<K, Object>> {
    public Iterator<Map.Entry<K, Object>> iterator() {
      return new EntryIterator<K>();
    }

    public int size() {
      return size;
    }
  }

  /**
   * The method used when constructing map entries for concrete subclasses.
   *
   * @param hash the hash value of the key of the new entry
   * @param key  the key of the new entry
   * @param next the next pointer of the new entry&rsquo;s singly-linked list
   * @return the newly-constructed entry
   */
  abstract protected HashMapPrimitive.Entry<K> getNewEntry(int hash, K key,
							   Entry<K> next);

  /**
   * Removes the entry for the specified key.  Unlike the API specified by
   * {@link Map#remove(Object)}, this method does not return the value
   * associated with the key, because that value could be one or more primitive
   * values.
   *
   * @param key the key of the entry to remove
   * @return <code>null</code>
   */
  public Object remove(Object key) {
    int keyHash = keyHash(key);
    int bucketIdx = (keyHash & hashCodeBitmask) % entries.length;
    for (Entry<K> entry = entries[bucketIdx], prev = null;
	 entry != null; entry = entry.next) {
      if (keyHash == entry.keyHash && keysEqual(entry.key, key)) {
	if (prev == null) {
	  // remove first item in bucket
	  entries[bucketIdx] = entries[bucketIdx].next;
	} else {
	  prev.next = entry.next;
	}
	modCount++;
	size--;
	return null;
      }
      prev = entry;
    }
    return null;
  }

  private void rehash() {
    int oldSize = entries.length;
    Entry[] oldEntries = entries;

    BigInteger bigIntNewSize = new BigInteger(String.valueOf(oldSize * 2));
    bigIntNewSize = bigIntNewSize.nextProbablePrime();

    int cmp = bigIntNewSize.compareTo(maxCapacity);
    if (cmp == 0) {
      return; // if we're already at maximum capacity, return
    } else if (cmp > 0) {
      bigIntNewSize = maxCapacity;
    }

    int newSize = bigIntNewSize.intValue();
    Entry[] newEntries = new Entry[newSize];

    for (int i = 0; i < oldSize; i++) {
      if (oldEntries[i] == null)
	continue;
      for (Entry<K> entry = oldEntries[i]; entry != null;) {
	int newBucket = (entry.keyHash & hashCodeBitmask) % newSize;
	Entry<K> oldNext = entry.next;
	entry.next = newEntries[newBucket];
	newEntries[newBucket] = entry;
	entry = oldNext;
      }
    }
    entries = newEntries;
    threshold = loadFactor * entries.length;
  }

  public boolean containsKey(Object key) {
    int keyHash = keyHash(key);
    int bucketIdx = (keyHash & hashCodeBitmask) % entries.length;
    for (Entry<K> entry = entries[bucketIdx];
	 entry != null; entry = entry.next) {
      if (keyHash == entry.keyHash && keysEqual(key, entry.key))
	return true;
    }
    return false;
  }

  /**
   * Throws an {@link UnsupportedOperationException}.
   * @param key ignored
   * @param value ignored
   * @return nothing
   *
   * @throws UnsupportedOperationException under all circumstances
   */
  public Object put(K key, Object value) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the map entry associated with the specified key, or <code>null</code>
   * if this map does not contain such a mapping.
   *
   * @param key the key for which to look up a map entry
   * @return the map entry for the specified key, or <code>null</code> if no
   *         such mapping exists in this map
   */
  public MapToPrimitive.Entry<K> getEntry(K key) {
    return getEntryInternal(key, keyHash(key));
  }

  /**
   * Returns the entry associated with the specified key, or, if no such entry
   * exists, creates one and returns it.
   *
   * @param key the key for which to return or create a map entry
   * @return the entry associated with the specified key, or, if no such entry
   *         exists, creates one and returns it
   */
  protected MapToPrimitive.Entry<K> getOrCreateEntry(K key) {
    int keyHash = keyHash(key);
    Entry<K> entry = getEntryInternal(key, keyHash);
    if (entry == null) {
      int bucketIdx = (keyHash & hashCodeBitmask) % entries.length;
      // add the new entry as the first entry in the bucket's singly-linked list
      entry = getNewEntry(keyHash, key, entries[bucketIdx]);
      entries[bucketIdx] = entry;
      modCount++;
      size++;
      if (size > threshold) {
	rehash();
      }
    }
    return entry;
  }

  /**
   * Returns the entry associated with the specified key, or <code>null</code>
   * if no such entry exists.  It is an error to invoke this method with a hash
   * code that is not equal to <code>key.hashCode()</code>.
   *
   * @param key      the key for which to retrieve its map entry
   * @param hashCode the hash code for the specified key
   * @return the entry associated with the specified key, or <code>null</code>
   *         if no such entry exists
   */
  public MapToPrimitive.Entry<K> getEntry(K key, int hashCode) {
    return getEntryInternal(key, hashCode);
  }

  private HashMapPrimitive.Entry<K> getEntryInternal(K key,
						     int hashCode) {
    int bucketIdx = (hashCode & hashCodeBitmask) % entries.length;
    for (Entry<K> entry = entries[bucketIdx];
	 entry != null; entry = entry.next) {
      if (hashCode == entry.keyHash && keysEqual(key, entry.key))
	return entry;
    }
    return null;
  }

  /**
   * Gets the map entry for the specified key and, as a side-effect, puts the
   * map entry at the front of the bucket list, indicating that it is the
   * most-recently used entry (useful for caches implementing a bucket-LRU
   * replacement scheme).  This is an optional operation.
   *
   * @param key the key whose map entry is to be retrieved and made the MRU in
   *            its bucket inside the hash map
   * @throws UnsupportedOperationException if this map is not a hash map
   */
  public MapToPrimitive.Entry<K> getEntryMRU(K key) {
    return getEntryMRU(key, keyHash(key));
  }

  public MapToPrimitive.Entry<K> getEntryMRU(K key, int hashCode) {
    int bucketIdx = (hashCode & hashCodeBitmask) % entries.length;
    for (Entry<K> entry = entries[bucketIdx], prev = null;
	 entry != null; entry = entry.next) {
      if (hashCode == entry.keyHash && keysEqual(key, entry.key)) {
	if (prev != null) {
	  // entry isn't first in list, so make it so
	  prev.next = entry.next;
	  entry.next = entries[bucketIdx];
	  entries[bucketIdx] = entry;
	}
	modCount++;
	return entry;
      }
      prev = entry;
    }
    return null;
  }

  /**
   * Adds the specified entry to the beginning of the singly-linked list at its
   * bucket index (indicating it is the most-recently used entry).
   * <b>Warning</b>: This method does not check whether the specified entry's
   * key already is in the map; subclasses should only invoke this method when
   * it has already been ascertained that the entry&rsquo;s key is
   * <b><i>not</i></b> in the map.
   *
   * @param entry the entry to add as most-recently used in its bucket
   */
  protected void addEntryMRU(Entry entry) {
    int bucketIdx = (entry.keyHash & hashCodeBitmask) % entries.length;
    entry.next = entries[bucketIdx];
    entries[bucketIdx] = entry;
    modCount++;
  }

  protected MapToPrimitive.Entry removeLRU(K key) {
    return removeLRU(keyHash(key));
  }

  /**
   * Removes the last entry at the specified bucket index, if that bucket
   * contains at least one entry.
   *
   * @param hashCode the hashCode of an object whose bucket is to be emptied of
   *                 its least-recently-used entry
   * @return the removed entry, or <code>null</code> if no entry was removed
   */
  protected MapToPrimitive.Entry removeLRU(int hashCode) {
    int bucketIdx = (hashCode & hashCodeBitmask) % entries.length;
    Entry<K> entry = entries[bucketIdx];
    if (entry == null) {
      return null;
    }
    for (Entry<K> prev = null; entry != null; entry = entry.next) {
      if (entry.next == null) {
	if (prev == null) {
	  entries[bucketIdx] = null;
	} else {
	  prev.next = null;
	}
	modCount++;
	return entry;
      }
      prev = entry;
    }
    throw new RuntimeException("removeLRU fatal error!");
  }

  /**
   * Removes a random entry from the bucket at the specified index (optional
   * operation).
   *
   * @param bucketIndex the index of the bucket from which to remove an element
   * @throws IllegalArgumentException      if
   *                                       <pre>0 &lt;= bucketIndex &lt;
   *                                       <p/>
   *
   *                                       getCapacity()</pre>
   *                                       is <code>false</code>
   * @throws UnsupportedOperationException if this map is not a hash map
   */
  public void removeRandom(int bucketIndex) {
    Entry<K> entry = entries[bucketIndex];
    // just remove first element in bucket, if it exists
    if (entry != null) {
      remove(entry.key);
    }
  }

  private void writeObject(ObjectOutputStream s)
    throws IOException {
    s.defaultWriteObject();
    s.writeInt(entries.length);
    s.writeInt(size);
    for (int i = entries.length - 1; i >= 0; i--) {
      Entry entry = entries[i];
      while (entry != null) {
	s.writeObject(entry);
	entry = entry.next;
      }
    }
  }

  private void readObject(ObjectInputStream s)
    throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    int numBuckets = s.readInt();
    entries = new Entry[numBuckets];
    size = 0;
    int numEntriesToRead = s.readInt();
    while (numEntriesToRead-- > 0) {
      Object entry = s.readObject();
      addEntry((Entry) entry);
    }
  }

  private void addEntry(Entry<K> entryToAdd) {
    Entry<K> entry = getEntryInternal(entryToAdd.key, entryToAdd.keyHash);
    if (entry != null) {
      entry.copyValuesFrom(entryToAdd);
    } else {
      int bucketIdx = (entryToAdd.keyHash & hashCodeBitmask) % entries.length;
      entryToAdd.next = entries[bucketIdx];
      entries[bucketIdx] = entryToAdd;
      modCount++;
      size++;
      if (size > threshold) {
	rehash();
      }
    }
  }

  // methods to comply with FlexibleMap interface
  public int getCapacity() {
    return entries.length;
  }

  public float getLoadFactor() {
    return loadFactor;
  }

  public String getStats() {
    int maxBucketSize = 0, numNonZeroBuckets = 0;
    for (int index = entries.length - 1; index >= 0; index--) {
      Entry<K> entry = entries[index];

      int numItemsInBucket = 0;
      //noinspection StatementWithEmptyBody
      for (; entry != null; entry = entry.next, numItemsInBucket++)
	;
      if (numItemsInBucket > maxBucketSize)
	maxBucketSize = numItemsInBucket;
      if (numItemsInBucket > 0)
	numNonZeroBuckets++;
    }
    return "size: " + size() + "; load factor: " + getLoadFactor() +
	   ";\n\tNo. of buckets: " + getCapacity() +
	   " (max.: " + maxBucketSize +
	   "; avg.: " + (size() / (float) numNonZeroBuckets) +
	   "; non-zero: " + numNonZeroBuckets + ")";
  }

  public Object get(K key, int hashCode) {
    throw new UnsupportedOperationException();
  }

  // byte-specific methods
  public byte put(K key, int index, byte value) {
    MapToPrimitive.Entry<K> entry = getOrCreateEntry(key);
    return entry.set(index, value);
  }

  public void add(K key, int index, byte addend) {
    MapToPrimitive.Entry<K> entry = getOrCreateEntry(key);
    entry.add(index, addend);
  }

  // char-specific methods
  public char put(K key, int index, char value) {
    MapToPrimitive.Entry<K> entry = getOrCreateEntry(key);
    return entry.set(index, value);
  }

  // short-specific methods
  public short put(K key, int index, short value) {
    MapToPrimitive.Entry<K> entry = getOrCreateEntry(key);
    return entry.set(index, value);
  }

  public void add(K key, int index, short addend) {
    MapToPrimitive.Entry<K> entry = getOrCreateEntry(key);
    entry.add(index, addend);
  }

  // int-specific methods
  public int put(K key, int index, int value) {
    MapToPrimitive.Entry<K> entry = getOrCreateEntry(key);
    return entry.set(index, value);
  }

  public void add(K key, int index, int addend) {
    MapToPrimitive.Entry<K> entry = getOrCreateEntry(key);
    entry.add(index, addend);
  }

  // long-specific methods
  public long put(K key, int index, long value) {
    MapToPrimitive.Entry<K> entry = getOrCreateEntry(key);
    return entry.set(index, value);
  }

  public void add(K key, int index, long addend) {
    MapToPrimitive.Entry<K> entry = getOrCreateEntry(key);
    entry.add(index, addend);
  }

  // float-specific methods
  public float put(K key, int index, float value) {
    MapToPrimitive.Entry<K> entry = getOrCreateEntry(key);
    return entry.set(index, value);
  }

  public void add(K key, int index, float addend) {
    MapToPrimitive.Entry<K> entry = getOrCreateEntry(key);
    entry.add(index, addend);
  }

  // double-specific methods
  public double put(K key, int index, double value) {
    MapToPrimitive.Entry<K> entry = getOrCreateEntry(key);
    return entry.set(index, value);
  }

  public void add(K key, int index, double addend) {
    MapToPrimitive.Entry<K> entry = getOrCreateEntry(key);
    entry.add(index, addend);
  }

  // static helper methods (needed to deal with the null key)
  private static int keyHash(Object k) {
    return k == null ? 0 : k.hashCode();
  }

  private static boolean keysEqual(Object k1, Object k2) {
    return k1 == null ? k2 == null : k1.equals(k2);
  }

  public Set<Map.Entry<K, Object>> entrySet() {
    if (entrySet == null)
      entrySet = new EntrySet<K>();
    return entrySet;
  }
}
