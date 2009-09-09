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
import java.math.BigInteger;
import java.lang.reflect.Array;
import java.io.*;

/**
 * Home-grown implementation of a hash map, in order to support the {@link
 * FlexibleMap} interface.
 */
public class HashMap<K,V> extends AbstractMap<K, V>
  implements FlexibleMap<K, V>, Cloneable, Serializable {
  // constants
  private final static float defaultLoadFactor = 0.75f;
  private final static int defaultInitialCapacity = 11;
  private final static int hashCodeBitmask = 0x7fffffff;
  protected final static BigInteger maxCapacity =
    new BigInteger(String.valueOf(Integer.MAX_VALUE));

  // inner classes
  class Entry<K,V> implements Map.Entry<K, V> {
    private K key;
    private V val;
    private int keyHash;
    private Entry<K,V> next;

    public Entry(K key, V val, Entry<K,V> next) {
      this.key = key;
      this.keyHash = keyHash(key);
      this.val = val;
      this.next = next;
    }

    public K getKey() {
      return key;
    }

    int getKeyHash() {
      return keyHash;
    }

    public V getValue() {
      return val;
    }

    public V setValue(V value) {
      V oldVal = val;
      this.val = value;
      return oldVal;
    }
  }

  class EntryIterator<K,V> implements Iterator<Map.Entry<K, V>> {
    // data members
    Entry<K,V> current = null;
    int currBucket = -1;
    Entry<K,V> lastReturned = null;
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

    public Map.Entry<K, V> next() {
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
      Entry<K,V> prev = entries[lastReturnedBucket];
      if (prev == lastReturned) {
	// if the last returned entry was the first element in its bucket
	entries[lastReturnedBucket] = lastReturned.next;
      }
      else {
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

  private class EntrySet<K,V> extends AbstractSet<Map.Entry<K, V>> {
    public Iterator<Map.Entry<K, V>> iterator() {
      return new EntryIterator<K,V>();
    }

    public int size() {
      return size;
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

  // constructors
  public HashMap() {
    this(defaultInitialCapacity, defaultLoadFactor);
  }

  public HashMap(int initialCapacity) {
    this(initialCapacity, defaultLoadFactor);
  }

  public HashMap(int initialCapacity, float loadFactor) {
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

  public HashMap(Map<? extends K, ? extends V> map) {
    this(map.size());
    this.putAll(map);
  }

  public V put(K key, V value) {
    int keyHash = keyHash(key);
    int bucketIdx = (keyHash & hashCodeBitmask) % entries.length;
    for (Entry<K,V> entry = entries[bucketIdx];
	 entry != null; entry = entry.next) {
      if (keyHash == entry.keyHash && keysEqual(entry.key, key)) {
	V oldVal = entry.val;
	entry.val = value;
	return oldVal;
      }
    }
    // key wasn't already in map, so add as new first bucket entry
    entries[bucketIdx] = new Entry<K,V>(key, value, entries[bucketIdx]);
    modCount++;
    size++;
    if (size > threshold) {
      rehash();
    }
    return null;
  }

  public V remove(Object key) {
    int keyHash = keyHash(key);
    int bucketIdx = (keyHash & hashCodeBitmask) % entries.length;
    for (Entry<K,V> entry = entries[bucketIdx], prev = null;
	 entry != null; entry = entry.next) {
      if (keyHash == entry.keyHash && keysEqual(entry.key, key)) {
	V oldVal = entry.val;
	if (prev == null) {
	  // remove first item in bucket
	  entries[bucketIdx] = entries[bucketIdx].next;
	}
	else {
	  prev.next = entry.next;
	}
	modCount++;
	size--;
	return oldVal;
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
    }
    else if (cmp > 0) {
      bigIntNewSize = maxCapacity;
    }

    int newSize = bigIntNewSize.intValue();
    Entry[] newEntries = new Entry[newSize];

    for (int i = 0; i < oldSize; i++) {
      if (oldEntries[i] == null)
	continue;
      for (Entry<K,V> entry = oldEntries[i]; entry != null; ) {
	int newBucket = (entry.keyHash & hashCodeBitmask) % newSize;
	Entry<K,V> oldNext = entry.next;
	entry.next = newEntries[newBucket];
	newEntries[newBucket] = entry;
	entry = oldNext;
      }
    }
    modCount++;
    entries = newEntries;
    threshold = loadFactor * entries.length;
  }

  public boolean containsKey(Object key) {
    int keyHash = keyHash(key);
    int bucketIdx = (keyHash & hashCodeBitmask) % entries.length;
    for (Entry<K,V> entry = entries[bucketIdx];
	 entry != null; entry = entry.next) {
      if (keyHash == entry.keyHash && keysEqual(key, entry.key))
	return true;
    }
    return false;
  }

  public V get(Object key) {
    return get(key, keyHash(key));
  }

  public V get(Object key, int keyHash) {
    int bucketIdx = (keyHash & hashCodeBitmask) % entries.length;
    for (Entry<K,V> entry = entries[bucketIdx];
	 entry != null; entry = entry.next) {
      if (keyHash == entry.keyHash && keysEqual(key, entry.key))
	return entry.val;
    }
    return null;
  }

  public Map.Entry<K,V> getEntry(Object key) {
    int keyHash = keyHash(key);
    int bucketIdx = (keyHash & hashCodeBitmask) % entries.length;
    for (Entry<K,V> entry = entries[bucketIdx];
	 entry != null; entry = entry.next) {
      if (keyHash == entry.keyHash && keysEqual(key, entry.key))
	return entry;
    }
    return null;
  }

  private final static int keyHash(Object k) {
    return k == null ? 0 : k.hashCode();
  }

  private final static boolean keysEqual(Object k1, Object k2) {
    return k1 == null ? k2 == null : k1.equals(k2);
  }

  public void clear() {
    entries = new Entry[entries.length];
    size = 0;
    modCount = 0;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * Gets the capacity of this map (optional operation).
   *
   * @return the capacity of this map (the number of buckets, in the case of a
   *         hash map)
   *
   * @throws UnsupportedOperationException if this map is not a hash map
   */
  public int getCapacity() {
    return entries.length;
  }

  /**
   * Gets the load factor of this map (optional operation).
   *
   * @return the load factor of this map
   *
   * @throws UnsupportedOperationException if this map is not a hash map
   */
  public float getLoadFactor() {
    return loadFactor;
  }

  public int getMaxBucketSize() {
    int maxBucketSize = 0;
    for (int i = 0; i < entries.length; i++) {
      int bucketSize = 0;
      for (Entry<K,V> entry = entries[i]; entry != null; entry = entry.next) {
	bucketSize++;
      }
      if (bucketSize > maxBucketSize)
	maxBucketSize = bucketSize;
    }
    return maxBucketSize;
  }

  public int getNumNonEmptyBuckets() {
    int numNonEmptyBuckets = 0;
    for (int i = 0; i < entries.length; i++) {
      if (entries[i] != null) {
	numNonEmptyBuckets++;
      }
    }
    return numNonEmptyBuckets;
  }

  public double getAverageBucketSize(boolean onlyCountNonEmptyBuckets) {
    double denom = onlyCountNonEmptyBuckets ?
		   getNumNonEmptyBuckets() : getCapacity();
    return denom == 0.0 ? 0.0 : size() / denom;
  }

  /**
   * Returns a string that represents the useful statistics of this map
   * (useful/necessary in the case of hash maps, where it is desirable to know
   * the number of collisions and average and maximum buckets sizes). The format
   * of the string is up to the implementor.
   */
  public String getStats() {
    return "size=" + size() + "; capacity=" + getCapacity() +
	   "; load factor=" + getLoadFactor() +
	   "; non-empty buckets=" + getNumNonEmptyBuckets() +
	   "; max. bucket size=" + getMaxBucketSize() +
	   "; avg. bucket size=" + getAverageBucketSize(false) +
	   "; avg. bucket size (non-empty buckets)=" +
	   getAverageBucketSize(true);
  }

  public Set<Map.Entry<K, V>> entrySet() {
    return new EntrySet<K,V>();
  }

  private void readObject(java.io.ObjectInputStream ois)
    throws IOException, ClassNotFoundException {
    ois.defaultReadObject();
    entries = new Entry[ois.readInt()];
    int numEntriesToRead = ois.readInt();
    size = 0; // this data member will get incremented by put method, below
    for (int i = 0; i < numEntriesToRead; i++) {
      K k = (K)ois.readObject();
      V v = (V)ois.readObject();
      put(k, v);
    }
  }

  private void writeObject(java.io.ObjectOutputStream oos)
    throws IOException {
    oos.defaultWriteObject();
    oos.writeInt(entries.length);
    oos.writeInt(size);
    for (int i = 0; i < entries.length; i++) {
      Entry entry = entries[i];
      for (; entry != null; entry = entry.next) {
	oos.writeObject(entry.key);
	oos.writeObject(entry.val);
      }
    }
  }

  public static void main(String[] args) {
    HashMap<String,Integer> map = new HashMap<String,Integer>();
    for (int i = 0; i < 100; i++) {
      map.put(String.valueOf(i), i);
    }
    System.out.println("map contains 99? " + map.containsKey("99"));
    System.out.println("map contains 100? " + map.containsKey("100"));
    System.out.println(map.getStats());
    map.remove("47");
    System.out.println(map.getStats());
    System.out.println(map);
    map.put("47", 48);
    map.put(null, 60);
    System.out.println("map contains null? " + map.containsKey(null));
    System.out.println(map.getStats());
    System.out.println(map);

    HashMap<String,Integer> removalMap =
      new HashMap<String, Integer>(map);

    Iterator<Map.Entry<String,Integer>> it = removalMap.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String,Integer> entry = it.next();
      it.remove();
    }
    System.out.println("removalMap stats: " + removalMap.getStats());
    System.out.println("removalMap data:");
    System.out.println(removalMap);

    Map<Integer,Integer> intMap = new HashMap<Integer, Integer>();
    intMap.put(-1, 100);

    try {
      System.out.println("writing map");
      OutputStream fos = new FileOutputStream("foo");
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(map);
      fos.close();
      System.out.println("reading map");
      InputStream fis = new FileInputStream("foo");
      ObjectInputStream ois = new ObjectInputStream(fis);
      HashMap<String,Integer> readMap =
	(HashMap<String,Integer>)ois.readObject();
      System.out.println("read map stats: " + readMap.getStats());
      System.out.println("read map data:");
      System.out.println(readMap);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
  }
}
