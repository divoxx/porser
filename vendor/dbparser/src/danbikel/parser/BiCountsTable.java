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
 * Provides a mapping between objects and two floating-point (<tt>double</tt>)
 * values that may be incremented or decremented.
 */
public class BiCountsTable<K> extends danbikel.util.HashMapTwoDoubles<K> {
  /**
   * Constructs an empty <code>BiCountsTable</code>.
   */
  public BiCountsTable() {
    super();
  }

  /**
   * Constructs an empty <code>BiCountsTable</code> with the specified initial
   * number of hash buckets.
   *
   * @param initialCapacity the number of hash buckets that this object
   * will initially have
   */
  public BiCountsTable(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Constructs an empty <code>BiCountsTable</code> with the specified initial
   * number of hash buckets and the specified load factor.  If the load factor,
   * which is average number of items per bucket, is exceeded at runtime, the
   * number of buckets is roughly doubled and the entire map is re-hashed, as
   * implemented by the parent class, {@link danbikel.util.HashMap}.
   *
   * @param initialCapacity the number of hash buckets that this object will
   *                        initially have
   * @param loadFactor      the load factor of this <code>HashMap</code> object
   */
  public BiCountsTable(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  /**
   * Adds 1 to the counter at the specified index for the specified key in
   * this map.  If no such key exists, then a mapping is added, with 1 as
   * the value of the counter at the specified index and 0 as the value of
   * all other counters (this map contains only two counters).
   *
   * @param key the key whose count at the specified index is to be incremented
   * @param index the index of the counter to be incremented for the specified
   * key
   */
  public void add(K key, int index) {
    add(key, index, 1.0);
  }

  /**
   * Returns the value at the specified index for the specified key.
   * @param key the key to be looked up
   * @param index the index of a value associated with the specified key
   * @return the value at the specified index for the specified key, or
   * <tt>0.0</tt> if the key is not in this map
   */
  public double count(K key, int index) {
    /*
    if (index < 0 || index > 1)
      throw new IllegalArgumentException();
    */
    MapToPrimitive.Entry<K> e = getEntry(key);
    return (e == null ? 0.0 : e.getDoubleValue(index));
  }

  /**
   * Removes items in this table whose counts are less than the specified
   * threshold.
   *
   * @param threshold the count threshold below which to remove items from
   * this table
   * @param atIndex the index at which to check an item's count to see if
   * it falls below the specified threshold; the value of this argument
   * must be either 0 or 1
   */
  public void removeItemsBelow(double threshold, int atIndex) {
    Iterator it = entrySet().iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
      if (entry.getDoubleValue(atIndex) < threshold)
        it.remove();
    }
  }

  /**
   * Outputs all the mappings of this map in as S-expressions of the form
   * <pre>(name key count0 count1)</pre>
   * where <tt>count0</tt> is the integer at index 0 and <tt>count1</t>
   * is the integer at index 1 for the key.
   *
   * @param eventName the name of this type of event, to be the first element
   * of the 4-element S-expression output by this method
   * @param writer the character stream to which this map's entries are to
   * be written
   * @throws IOException if the specified <tt>Writer</tt> throws an
   * <tt>IOException</tt> while it is being written to
   */
  public void output(String eventName, Writer writer) throws IOException {
    for (K key : keySet()) {
      writer.write("(");
      writer.write(eventName);
      writer.write(" ");
      writer.write(String.valueOf(key));
      writer.write(" ");
      writer.write(String.valueOf(count(key, 0)));
      writer.write(" ");
      writer.write(String.valueOf(count(key, 1)));
      writer.write(")\n");
    }
  }
}
