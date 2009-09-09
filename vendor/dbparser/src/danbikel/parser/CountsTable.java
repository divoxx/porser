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

import java.io.*;

/**
 * Specifies a mapping between objects and floating-point (<code>double</code>)
 * counts that may be incremented or decremented.
 */
public interface CountsTable<K> extends danbikel.util.MapToPrimitive<K> {
  /**
   * Adds all the counts from the specified table to this table, adding any
   * new keys in the specified map to this map, if necessary.
   *
   * @param other the other counts table whose counts are to be added
   * to this table
   */
  public void addAll(CountsTable<K> other);

  /**
   * Puts the specified map of key objects to their counts into this
   * counts table.  If a key from the specified map already exists in this
   * map, it is simply replaced.
   *
   * @param other another counts table whose counts are to be put into
   * this table
   */
  public void putAll(CountsTable<K> other);

  /**
   * Adds the specified key with a count of <code>1.0</code>.
   *
   * @param key the key to be added to this counts table
   */
  public void add(K key);

  /**
   * Returns the count of the specified key, or <code>0</code> if this
   * counts table does not contain a count for the specified key.
   *
   * @param key the key whose count is to be gotten
   * @return the count of the specified key, or <code>0</code> if this
   * counts table does not contain a count for the specified key
   */
  public double count(K key);

  /**
   * Returns the count of the specified key with the specified hash code, or
   * <code>0</code> if this counts table does not contain a count for the
   * specified key.
   *
   * @param key the key whose count is to be gotten
   * @param hashCode the hash code of the specified key
   * @return the count of the specified key with the specified hash code, or
   * <code>0</code> if this counts table does not contain a count for the
   * specified key
   */
  public double count(K key, int hashCode);

  /**
   * Removes items in this table whose counts are less than the specified
   * threshold.
   *
   * @param threshold the count threshold below which to remove items from
   * this table
   */
  public void removeItemsBelow(double threshold);

  /**
   * Outputs all the mappings of this map in as S-expressions of the form
   * <pre>(name key value)</pre>
   * @param eventName the name of the events contained in this {@link CountsTable}
   * to output as the first symbol in the three-element list
   * @param writer the writer to which to output the elements of this
   * counts table as three-element S-expression lists
   *
   * @throws IOException if there is a problem writing to the specified
   * writer
   */
  public void output(String eventName, Writer writer) throws IOException;
}
