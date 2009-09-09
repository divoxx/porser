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
    package danbikel.lisp;

import java.util.*;
import java.lang.ref.*;

/**
 * <code>Symbol</code> objects associate strings or integers with
 * unique references.  This abstract base class, from which
 * <code>StringSymbol</code> and <code>IntSymbol</code> are derived,
 * allows for <code>Symbol</code> objects that contain either an int
 * or a <code>String</code>.  <p> <b>N.B.</b>: The values in the
 * internal map maintained by this class are actually weak references
 * to unique <code>Symbol</code> objects.  This allows the reclamation
 * of symbols that are unreachable via strong (or soft) references.
 * In this way, the programmer need never be concerned about the symbol
 * map monotonically growing simply because the program continues to
 * instantiate new symbols.
 *
 * @see StringSymbol
 * @see IntSymbol
*/

abstract public class Symbol extends Sexp {

  private final static boolean selfCleaning = true;

  /**
   * This class extends <code>WeakReference</code> to include a single
   * data member of type <code>Object</code> in order to store the
   * symbol map key of the <code>Symbol</code> object encapsulated by
   * this weak reference, in case the garbage collector should clear the
   * weak reference before it is encountered on its reference queue.
   * Thus, when the weak references are dequeued, the appropriate
   * key-value pairs can be removed from the symbol map.
   */
  static private class WeakSymReference extends WeakReference {
    /**
     * The key of the symbol object encapsulated by this reference
     * (either a String or an Integer object), since the actual Symbol
     * object may have been tossed by the GC.
     */
    private Object symKey;

    private WeakSymReference(Symbol sym) {
      super(sym);
      symKey = sym.getSymKey();
    }

    private WeakSymReference(Symbol sym, ReferenceQueue q) {
      super(sym, q);
      symKey = sym.getSymKey();
    }

    /* A WeakSymReference is equal to another WeakSymReference iff
       they both refer to objects that are, in turn, equal according
       to their own equals methods */
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof WeakSymReference)) return false;
      Object t = this.get();
      Object u = ((WeakSymReference)o).get();
      if ((t == null) || (u == null)) return false;
      if (t == u) return true;
      return t.equals(u);
    }

    private Object getSymKey() { return symKey; }
  }

  private static Map map = new HashMap(200001);
  private static ReferenceQueue queue = new ReferenceQueue();

  /**
   * Gets the <code>Integer</code> object associated with this Symbol.
   *
   * @return the <code>Integer</code> object associated with this Symbol
   * if it's an instance of <code>IntSymbol</code>, else <code>null</code>.
   */
  abstract public Integer getInteger();
  /**
   * Gets the string representation of this symbol.
   *
   * @return the print representation of this symbol.
   */
  abstract public String toString();

  /**
   * Gets the unique key for this symbol used by the internal symbol map.
   *
   * @return the unique key for this symbol used by the internal symbol map.
   */
  abstract protected Object getSymKey();

  public final Sexp deepCopy() { return this; }

  /**
   * Returns <code>false</code>, as no instance of a subclass of this abstract
   * base class can be a list.
   *
   * @return <code>false</code>, as no instance of a subclass of this abstract
   * base class can be a list.
   */
  public final boolean isList() { return false; }

  /**
   * Returns <code>true</code>, as any instance of a subclass of this abstract
   * base class is a symbol.
   *
   * @return <code>true</code>, as any instance of a subclass of this abstract
   * base class is a symbol.
   */
  public final boolean isSymbol() { return true; }

  /**
   * Returns the unique <code>Symbol</code> whose string key is
   * <code>str</code>.  If the symbol does not already exist in the internal
   * symbol map, it is added and returned.
   *
   * @param str the print name of the string symbol to get
   * @return the unique <code>Symbol</code> whose string key is
   * <code>str</code>.
   */
  public final static Symbol get(String str) {
    synchronized (map) {
      if (selfCleaning)
	processQueue(str);
      WeakSymReference symRef = (WeakSymReference)map.get(str);
      if (symRef == null || symRef.get() == null) {
	Symbol newSym = new StringSymbol(str);
	symRef = new WeakSymReference(newSym, queue);
	map.put(str, symRef);
	return (Symbol)symRef.get();
      }
      else
	return (Symbol)symRef.get();
    }
  }

  /**
   * A synonym for {@link #get(String)}.
   *
   * @param str the print name of the string symbol to get
   * @return the unique <code>Symbol</code> whose string key is
   * <code>str</code>.
   */
  public final static Symbol add(String str) {
    return get(str);
  }

  /**
   * Returns the unique <code>Symbol</code> whose integer value is that of
   * <code>intKey</code>.  If the symbol does not already exist in the internal
   * symbol map, it is added and returned.
   *
   * @param intKey the integer value of the symbol to get
   * @return the unique <code>Symbol</code> whose integer value is that of
   * <code>intKey</code>.
   */
  public final static Symbol get(Integer intKey) {
    synchronized (map) {
      if (selfCleaning)
	processQueue(intKey);
      WeakSymReference symRef = (WeakSymReference)map.get(intKey);
      if (symRef == null || symRef.get() == null) {
	Symbol newSym = new IntSymbol(intKey);
	symRef = new WeakSymReference(newSym, queue);
	map.put(intKey, symRef);
	return (Symbol)symRef.get();
      }
      else
	return (Symbol)symRef.get();
    }
  }

  /**
   * A synonym for {@link #get(Integer)}.
   *
   * @param intKey the integer value of the symbol to get
   * @return the unique <code>Symbol</code> whose integer value is that of
   * <code>intKey</code>.
   */
  public final static Symbol add(Integer intKey) {
    return get(intKey);
  }

  /**
   * A convenience method for {@link #add(Integer)}: the specified
   * <code>int</code> is first wrapped in an <code>Integer</code> object and
   * then added to the internal symbol map.
   *
   * @param intValue the integer value to be wrapped with an
   * <code>Integer</code> and added to the symbol map
   * @return the unique <code>Symbol</code> whose integer value is that of
   * <code>intValue</code>.
   */
  public final static Symbol add(int intValue) {
    return add(new Integer(intValue));
  }
  /**
   * A convenience method for {@link #add(Integer)}: the specified
   * <code>int</code> is first wrapped in an <code>Integer</code> object and
   * then added to the internal symbol map.
   *
   * @param intValue the integer value to be wrapped with an
   * <code>Integer</code> and added to the symbol map
   * @return the unique <code>Symbol</code> whose integer value is that of
   * <code>intValue</code>.
   */
  public final static Symbol get(int intValue) {
    return get(new Integer(intValue));
  }

  /**
   * Polls the internal reference queue of <code>WeakSymReference</code>
   * objects, removing their key-value pairs from the symbol map.
   *
   * @param symToGet the key of the symbol that is currently being
   * looked up, or <code>null</code> if this method is called by a
   * non-symbol-accessor method (such as {@link #clean}) (this parameter
   * is used only for debugging purposes)
   */
  private static void processQueue(Object symToGet) {
    WeakSymReference weakRef;
    while ((weakRef = (WeakSymReference)queue.poll()) != null) {
      map.remove(weakRef.getSymKey());
    }
  }

  /**
   * Cleans the internal symbol map by removing all symbols to which there are
   * no hard or soft references.
   */
  public final static void clean() {
    synchronized (map) {
      System.gc();
      processQueue(null);
    }
  }
}
