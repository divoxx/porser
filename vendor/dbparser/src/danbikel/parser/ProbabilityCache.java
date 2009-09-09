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
import java.util.Random;
import java.util.Iterator;

/**
 * A cache for storing arbitrary objects with their probabilities.  This class
 * uses a hash map, and offers several replacement strategies.
 */
public class ProbabilityCache<K> extends danbikel.util.HashMapDouble<K> {

  // public constants
  /**
   * Integer to indicate to delete a random element every time the size limit of
   * this cache has been exceeded.
   *
   * @see #setStrategy
   * @see #put(Object,double)
   */
  public final static int RANDOM = 0;
  /**
   * Integer to indicate to delete the least-recently used entry in the same
   * bucket as an entry being added after the size limit of this cache has
   * been reached or exceeded.  Put another way, after the size limit of the
   * cache has been reached (the size limit is specified at construction),
   * every time an element is cached, if its bucket contains at least one item,
   * then the least-recently used item of that bucket is first deleted and
   * then the current item is added.  Thus, this replacement strategy will
   * treat the maximum capacity of this cache as a soft limit, for in instances
   * where the bucket is already empty, this strategy will simply add an item,
   * deleting nothing.  A consequence is that the average bucket size can
   * increase over time, but never by more than 1 + the average bucket size at
   * the time the maximum capacity was reached (because only empty buckets can
   * increase in size from 0 to 1).
   * <p>
   * This strategy is the default.
   * <p>
   *
   * @see #setStrategy
   * @see #put(Object,double)
   */
  public final static int BUCKET_LRU = 1;
  /**
   * Integer to indicate to delete a random half of the elements
   * every time the size limit of this cache has been reached or exceeded.
   *
   * @see #setStrategy
   * @see #put(Object,double)
   */
  public final static int HALF_LIFE = 2;
  /**
   * Integer to indicate to delete all of the elements
   * every time the size limit of this cache has been reached or exceeded.
   *
   * @see #setStrategy
   * @see #put(Object,double)
   */
  public final static int CLEAR_ALL = 3;

  // private constants
  private final static int MIN_STRATEGY_IDX = 0;
  private final static int MAX_STRATEGY_IDX = 3;
  private final static int defaultStrategy = BUCKET_LRU;

  private int maxCapacity;
  private int strategy;
  private Random rand;

  /**
   * Constructs a <code>ProbabilityCache</code> with the specified maximum
   * capacity and the default replacement strategy.
   *
   * @param maxCapacity the maximum number of elements held by this cache
   *
   * @see #BUCKET_LRU
   * @see #setStrategy(int)
   */
  public ProbabilityCache(int maxCapacity) {
    //super(minCapacity);
    super();
    setMaxCapacity(maxCapacity);
    setStrategy(defaultStrategy);
  }
  /**
   * Constructs a <code>ProbabilityCache</code> with the specified maximum
   * capacity, the specified initial capacity and the default replacement
   * strategy.
   *
   * @param maxCapacity the maximum number of elements held by this cache
   * @param initialCapacity the initial capacity of the underlying
   * hash map
   *
   * @see #BUCKET_LRU
   * @see #setStrategy(int)
   */
  public ProbabilityCache(int maxCapacity, int initialCapacity) {
    //super(minCapacity, initialCapacity);
    super(initialCapacity);
    setMaxCapacity(maxCapacity);
    setStrategy(defaultStrategy);
  }
  /**
   * Constructs a <code>ProbabilityCache</code> with the specified maximum
   * capacity, the specified initial capacity, the specified load factor and
   * the default replacement strategy.
   *
   * @param maxCapacity the maximum number of elements held by this cache
   * @param initialCapacity the initial capacity of the underlying
   * hash map
   * @param loadFactor the load factor of the underlying
   * hash map
   *
   * @see #BUCKET_LRU
   * @see #setStrategy(int)
   */
  public ProbabilityCache(int maxCapacity, int initialCapacity,
			  float loadFactor) {
    //super(inCapacity, initialCapacity, loadFactor);
    super(initialCapacity, loadFactor);
    setMaxCapacity(maxCapacity);
    setStrategy(defaultStrategy);
  }

  /**
   * Sets the strategy for replacement when the size limit of this cache has
   * been reached.
   *
   * @param strategy the integer id for the caching strategy to set for this
   *                 cache
   * @return this probability cache object
   *
   * @see #RANDOM
   * @see #BUCKET_LRU
   * @see #HALF_LIFE
   * @see #CLEAR_ALL
   */
  public ProbabilityCache setStrategy(int strategy) {
    if (strategy < MIN_STRATEGY_IDX || strategy > MAX_STRATEGY_IDX)
      throw new IllegalArgumentException();
    this.strategy = strategy;
    if (strategy == RANDOM)
      rand = new Random(System.currentTimeMillis());

    return this;
  }

  /**
   * Sets the maximum capacity for this cache.
   *
   * @param maxCapacity the new maximum capacity of this cache
   * @throws IllegalArgumentException if the specified maximum capacity
   * is zero or negative
   */
  public void setMaxCapacity(int maxCapacity) {
    if (maxCapacity <= 0)
      throw new IllegalArgumentException();
    this.maxCapacity = maxCapacity;
  }

  /**
   * Throws an <code>UnsupportedOperationException</code>, as the only
   * way to add keys to this specialized cache is through the
   * <code>put(Object,double)</code> method.
   *
   * @see #put(Object,double)
   */
  public Object put(Object key, Object value) {
    throw new UnsupportedOperationException();
  }

  /**
   * Adds the specified key with the specified probability to this cache.
   * As a side effect, if the maximum capacity of this cache has been
   * reached or exceeded at the time this method is invoked, then
   * one or more cached elements may be removed, depending on the
   * cache strategy being used.
   *
   * @param key the key to add to this cache
   * @param probability the probability of the specified key to be cached
   * @return the old value associated with <code>key</code>, or
   * <code>null</code> if there was no mapping for this key
   *
   * @see #setStrategy(int)
   */
  //public synchronized double put(Object key, double probability) {
  public double put(K key, double probability) {
    if (size() >= maxCapacity) {
      switch (strategy) {
      case RANDOM:
	removeRandom();
	break;
      case BUCKET_LRU:
	//return super.putAndRemove(key, new Double(probability));
	return putAndRemove(key, probability);
      case HALF_LIFE:
	clearHalf();
	break;
      case CLEAR_ALL:
	clear();
	break;
      }
    }
    //return super.put(key, new Double(probability));
    return super.put(key, probability);
  }

  /*
  public synchronized double put(Object key, int hashCode, double probability) {
    if (size() >= maxCapacity) {
      switch (strategy) {
      case RANDOM:
	removeRandom();
	break;
      case BUCKET_LRU:
	//return super.putAndRemove(key, new Double(probability));
	return putAndRemove(key, hashCode, probability);
      case HALF_LIFE:
	clearHalf();
	break;
      case CLEAR_ALL:
	clear();
	break;
      }
    }
    //return super.put(key, new Double(probability));
    return super.put(key, hashCode, probability);
  }
  */

  //public final synchronized double putAndRemove(Object key, double probability) {
  /**
   * A synonym for <code>putAndRemove(key, key.hashCode(), probability)</code>.
   * @param key the key to be inserted into this cache
   * @param probability the probability of the key
   * @return the old probability of the specified key, or {@link Double#NaN}
   * if the key did not exist in this map
   */
  public final double putAndRemove(K key, double probability) {
    return putAndRemove(key, key.hashCode(), probability);
  }

  //public final synchronized double putAndRemove(Object key, int hash,
  /**
   * Puts the specified key into the cache with the specified probability,
   * removing the least-recently used key from this key's bucket if the bucket
   * is not currently empty.
   *
   * @param key the key to be inserted into this cache
   * @param hash the hash value of the specified key
   * @param probability the probability of the specified key
   * @return the old probability of the specified key, or {@link Double#NaN}
   * if the key did not exist in this map
   */
  public final double putAndRemove(K key, int hash, double probability) {
    MapToPrimitive.Entry entry = getEntryMRU(key, hash);
    if (entry != null) {
      double oldProb = entry.getDoubleValue();
      entry.set(0, probability);
      return oldProb;
    }
    else {
      removeLRU(hash);
      HashMapPrimitive.Entry newEntry = getNewEntry(hash, key, null);
      newEntry.set(0, probability);
      addEntryMRU(newEntry);
    }
    return Double.NaN;
  }

  /**
   * Throws an <code>UnsupportedOperationException</code>, as the only
   * way to get values from this specialized cache is through the
   * <code>getProb(Object)</code> method.
   *
   * @see #getProb(Object)
   */
  public Object get(Object key) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the <code>Double</code> containing the probability of the specified
   * key, or <code>null</code> if the specified key is not in this
   * cache.
   *
   * @param key the key to look up in this cache
   * @return the probability of the specified key or <code>null</code>
   * if it is not in this cache
   */
  //public synchronized MapToPrimitive.Entry getProb(Object key) {
  public MapToPrimitive.Entry getProb(K key) {
    return (strategy == BUCKET_LRU ?
	    super.getEntryMRU(key) :
	    super.getEntry(key));
  }

  /*
  public synchronized MapToPrimitive.Entry getProb(Object key, int hashCode) {
    return (strategy == BUCKET_LRU ?
	    super.getEntryMRU(key, hashCode) :
	    super.getEntry(key, hashCode));
  }
  */

  public synchronized boolean containsKey(Object key) {
    return super.containsKey(key);
  }

  public void removeRandom() {
    int randIdx = rand.nextInt(getCapacity());
    removeRandom(randIdx);
  }

  private void clearHalf() {
    /*
    System.err.print("yea!  we're clearing half from a cache of size " +
		     size() + "...");
    */
    Iterator it = keySet().iterator();
    while (it.hasNext()) {
      it.next();
      if (it.hasNext()) {
	it.next();
	it.remove();
      }
    }
    // System.err.println("done");
  }
}
