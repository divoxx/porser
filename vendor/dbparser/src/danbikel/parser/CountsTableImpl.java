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
    package danbikel.parser;

import danbikel.util.*;
import java.io.*;
import java.util.*;

/**
 * Provides a mapping between objects and floating-point (<tt>double</tt>)
 * counts that may be incremented or decremented.
 */
public class CountsTableImpl<K>
  extends danbikel.util.HashMapDouble<K> implements CountsTable<K> {
  /**
   * Constructs an empty <code>CountsTable</code>.
   */
  public CountsTableImpl() {
    super();
  }

  /**
   * Constructs an empty <code>CountsTable</code> with the specified initial
   * number of hash buckets.
   *
   * @param initialCapacity the number of hash buckets that this object
   * will initially have
   */
  public CountsTableImpl(int initialCapacity) {
    super(initialCapacity);
  }
  /**
   * Constructs an empty <code>CountsTable</code> with the specified initial
   * number of hash buckets and the specified load factor.  If the load factor,
   * which is average number of items per bucket, is exceeded at runtime, the
   * number of buckets is roughly doubled and the entire map is re-hashed, as
   * implemented by the parent class, {@link danbikel.util.HashMap}.
   *
   * @param initialCapacity the number of hash buckets that this object will
   *                        initially have
   * @param loadFactor      the load factor of this <code>HashMap</code> object
   */
  public CountsTableImpl(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public void addAll(CountsTable<K> other) {
    for (Map.Entry<K,Object> entryObj : other.entrySet()) {
      MapToPrimitive.Entry<K> entry = (MapToPrimitive.Entry<K>)entryObj;
      this.add(entry.getKey(), entry.getDoubleValue());
    }
  }

  public void putAll(CountsTable<K> other) {
    for (Map.Entry<K,Object> entryObj : other.entrySet()) {
      MapToPrimitive.Entry<K> entry = (MapToPrimitive.Entry<K>)entryObj;
      this.put(entry.getKey(), entry.getDoubleValue());
    }
  }

  public void add(K key) {
    add(key, 0, 1.0);
  }

  public double count(K key) {
    MapToPrimitive.Entry<K> e = getEntry(key);
    return (e == null ? 0 : e.getDoubleValue(0));
  }

  public double count(K key, int hashCode) {
    MapToPrimitive.Entry e = getEntry(key, hashCode);
    return (e == null ? 0 : e.getDoubleValue(0));
  }

  /**
   * Removes items in this table whose counts are less than the specified
   * threshold.
   *
   * @param threshold the count threshold below which to remove items from
   * this table
   */
  public void removeItemsBelow(double threshold) {
    Iterator it = entrySet().iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
      if (entry.getDoubleValue() < threshold)
	it.remove();
    }
  }

  /**
   * Outputs all the mappings of this map in as S-expressions of the form
   * <pre>(name key value)</pre>
   */
  public void output(String eventName, Writer writer) throws IOException {
    for (K key : keySet()) {
      writer.write("(");
      writer.write(eventName);
      writer.write(" ");
      writer.write(String.valueOf(key));
      writer.write(" ");
      writer.write(String.valueOf(count(key)));
      writer.write(")\n");
    }
  }
}
