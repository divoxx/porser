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
import danbikel.lisp.*;
import java.util.*;
import java.io.*;


/**
 * Presents an immutable map of a type of {@link TrainerEvent} objects to
 * observed counts, backed by a file of the form output by
 * {@link Trainer#writeStats}.  The contract of the {@link java.util.Map}
 * interface may be violated if the underlying file does not contain
 * a collection of unique <code>TrainerEvent</code> objects; however, this
 * contract violation may not be a problem for many kinds of operations,
 * such as those that rely simply on the ability to iterate over all
 * observed events.  One such operation is the additive derivation of
 * counts as implemented by the method
 * {@link Model#deriveCounts(CountsTable,Filter,double,FlexibleMap)}.
 */
public class FileBackedTrainerEventMap
  extends AbstractMapToPrimitive implements CountsTable {

  /**
   * Inner class to implement caching iterator.  While the main thread
   * is reading elements from the "grab cache", another thread is reading ahead
   * from the file to fill the "fill cache".
   */
  class CachingIterator implements Iterator, Runnable {
    final static int defaultCacheSize = 2500;

    Iterator it;
    int cacheSize;
    List grabCache;
    List fillCache;
    int grabCacheIdx;
    volatile boolean moreElements;
    volatile int timeToSwap = 0;
    Time time;

    CachingIterator(Iterator it) {
      this(it, defaultCacheSize);
    }

    CachingIterator(Iterator it, int cacheSize) {
      this.cacheSize = cacheSize;
      this.it = it;
      grabCacheIdx = cacheSize;
      grabCache = new ArrayList(cacheSize);
      fillCache = new ArrayList(cacheSize);
      moreElements = it.hasNext();
      new Thread(this, "CachingIterator for " + file).start();
    }

    synchronized void swap() {
      timeToSwap++;
      if (timeToSwap < 2) {
        //time = new Time();
        try { wait(); }
        catch (InterruptedException ie) { System.err.println(ie); }
        return;
      }
      //System.err.println("time between swap: " + time);
      //time = null;
      timeToSwap = 0;
      grabCacheIdx = 0;
      List tmpList = grabCache;
      grabCache = fillCache;
      fillCache = tmpList;
      fillCache.clear();
      // the next line matters when the last fillCache has been swapped over
      // to be the grab cache, and it is smaller than the original cache size
      cacheSize = grabCache.size();
      notifyAll();
    }

    public boolean hasNext() {
      return grabCacheIdx < cacheSize || fillCache.size() > 0 || moreElements;
    }
    public Object next() {
      if (!hasNext())
        throw new NoSuchElementException();
      // if we grabbed the last object in the grab cache, but there
      // are more objects in the fill cache (or there will be when the fill
      // thread is done), then do the swap
      if (hasNext() && grabCacheIdx == cacheSize) {
        swap();
      }
      return grabCache.get(grabCacheIdx++);
    }
    public void remove() {
      throw new UnsupportedOperationException();
    }
    public void run() {
      while (moreElements) {
        while (moreElements && fillCache.size() < cacheSize) {
          fillCache.add(it.next());
          if (!it.hasNext())
            moreElements = false;
        }
        if (fillCache.size() > 0)
          swap();
      }
    }
  }

  private File file;
  private Symbol type;
  private int size = -1;
  private LinkedList cache = new LinkedList();

  // constructors

  /**
   * Constructs a new file-backed {@link TrainerEvent} map for events of the
   * specified type and using the specified file.
   *
   * @param type     the type of {@link TrainerEvent} objects in the specified
   *                 file over which to iterate; this argument must be one of
   *                 the following values:
   *                 <ul>
   *                 <li>{@link Trainer#nonterminalEventSym}
   *                 <li>{@link Trainer#headEventSym}
   *                 <li>{@link Trainer#modEventSym}
   *                 <li>{@link Trainer#gapEventSym}
   *                 <li>{@link Trainer#posMapSym}
   *                 <li>{@link Trainer#vocabSym}
   *                 <li>{@link Trainer#wordFeatureSym}
   *                 <li>{@link Trainer#prunedPretermSym}
   *                 <li>{@link Trainer#prunedPuncSym}
   *                 </ul>
   * @param filename the file containing S-expressions representing {@link
   *                 TrainerEvent} objects, where the S-expressions are of the
   *                 form output by {@link Trainer#writeStats}
   * @throws FileNotFoundException if the specified file does not exist
   */
  public FileBackedTrainerEventMap(Symbol type, String filename)
    throws FileNotFoundException {
    this(type, new File(filename));
  }

  /**
   * Constructs a new file-backed {@link TrainerEvent} map for events of the
   * specified type and using the specified file.
   *
   * @param type     the type of {@link TrainerEvent} objects in the specified
   *                 file over which to iterate; this argument must be one of
   *                 the following values:
   *                 <ul>
   *                 <li>{@link Trainer#nonterminalEventSym}
   *                 <li>{@link Trainer#headEventSym}
   *                 <li>{@link Trainer#modEventSym}
   *                 <li>{@link Trainer#gapEventSym}
   *                 <li>{@link Trainer#posMapSym}
   *                 <li>{@link Trainer#vocabSym}
   *                 <li>{@link Trainer#wordFeatureSym}
   *                 <li>{@link Trainer#prunedPretermSym}
   *                 <li>{@link Trainer#prunedPuncSym}
   *                 </ul>
   * @param file     the file containing S-expressions representing {@link
   *                 TrainerEvent} objects, where the S-expressions are of the
   *                 form output by {@link Trainer#writeStats}
   * @throws FileNotFoundException if the specified file does not exist
   */
  public FileBackedTrainerEventMap(Symbol type, File file)
    throws FileNotFoundException {
    if (!file.exists()) {
      String msg = FileBackedTrainerEventMap.class.getName() +
                   ": couldn't find file \"" + file + "\"";
      throw new FileNotFoundException(msg);
    }
    this.file = file;
    this.type = type;
  }

  private String unmodifiableMapMsg() {
    return getClass().getName() + " implements unmodifiable map";
  }

  /**
   * Throws an {@link UnsupportedOperationException}, as this is an
   * unmodifiable map.
   * @param other ignored
   */
  public void addAll(CountsTable other) {
    throw new UnsupportedOperationException(unmodifiableMapMsg());
  }

  /**
   * Throws an {@link UnsupportedOperationException}, as this is an
   * unmodifiable map.
   * @param other ignored
   */
  public void putAll(CountsTable other) {
    throw new UnsupportedOperationException(unmodifiableMapMsg());
  }

  /**
   * Throws an {@link UnsupportedOperationException}, as this is an
   * unmodifiable map.
   *
   * @param key ignored
   */
  public void add(Object key) {
    throw new UnsupportedOperationException(unmodifiableMapMsg());
  }

  public double count(Object key) {
    MapToPrimitive.Entry entry = getEntry(key);
    return (entry == null ? 0.0 : entry.getDoubleValue());
  }

  public double count(Object key, int hashCode) {
    MapToPrimitive.Entry entry = getEntry(key, hashCode);
    return (entry == null ? 0.0 : entry.getDoubleValue());
  }

  /**
   * Throws an {@link UnsupportedOperationException}, as this is an
   * unmodifiable map.
   *
   * @param threshold ignored
   */
  public void removeItemsBelow(double threshold) {
    throw new UnsupportedOperationException(unmodifiableMapMsg());
  }

  /**
   * Throws an {@link UnsupportedOperationException}, because isn't it silly
   * to try to copy an already file-backed map to a file?
   */
  public void output(String filename, java.io.Writer writer) {
    throw new UnsupportedOperationException(getClass().getName() +
                                            "silly to copy a map that is " +
                                            "already file-backed");
  }

  /**
   * Returns an entry set view of the map entries.
   * @return an entry set view of the map entries.
   */
  public Set entrySet() {
    return new java.util.AbstractSet() {
      public int size() {
        if (size == -1) {
          // compute size once and for all
          Iterator it = entrySet().iterator();
          size = 0;
          for (; it.hasNext(); it.next(), size++)
            ;
        }
        return size;
      }
      public Iterator iterator() {
        SexpTokenizer tok = null;
        try {
          tok = new SexpTokenizer(file, Language.encoding(),
                                  Constants.defaultFileBufsize);
        }
        catch (IOException ioe) {
          throw new RuntimeException(ioe.toString());
        }
        return new CachingIterator(Trainer.getEventIterator(tok, type));
      }
    };
  }

  /**
   * Throws an {@link UnsupportedOperationException} because this is an
   * unmodifiable map.
   * @param bucketIndex ignored
   */
  public void removeRandom(int bucketIndex) {
    throw new UnsupportedOperationException(unmodifiableMapMsg());
  }

  /**
   * Uses an <i>&Omicron;</i>(<i>n</i>) algorithm to retrieve the map entry for
   * the specified key.
   *
   * @param key the key for which to retrieve a map entry
   * @return a map entry for the specified key
   */
  public MapToPrimitive.Entry getEntry(Object key) {
    Iterator it = entrySet().iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
      if (entry.getKey().equals(key))
        return entry;
    }
    return null;
  }

  /**
   * Simply invokes <code>getEntry(key)</code>, returning the map entry for the
   * specified key.
   *
   * @param key      the key for which to get a map entry
   * @param hashCode ignored
   * @return the map entry for the specified key.
   *
   * @see #getEntry(Object)
   */
  public MapToPrimitive.Entry getEntry(Object key, int hashCode) {
    return getEntry(key);
  }

  /**
   * Throws an {@link UnsupportedOperationException} because this is an
   * unmodifiable map.
   * @param key ignored
   */
  public MapToPrimitive.Entry getEntryMRU(Object key) {
    throw new UnsupportedOperationException();
  }

  /**
   * Throws an {@link UnsupportedOperationException} because this is an
   * unmodifiable map.
   * @param key ignored
   */
  public MapToPrimitive.Entry getEntryMRU(Object key, int hashCode) {
    throw new UnsupportedOperationException();
  }

}