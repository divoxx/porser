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

import java.io.*;

/**
 * <code>IntSymbol</code> objects associate integers with unique references.
 * This association is maintained in <code>Symbol</code>, not in
 * <code>IntSymbol</code> itself.  As a consequence, <b>the <code>new</code>
 * operator for <code>IntSymbol</code> cannot be invoked directly</b>.  Rather,
 * new symbols are created by calling {@link Symbol#add(Integer)} or {@link
 * Symbol#add(int)}.
 *
 * @see Symbol
 * @see StringSymbol
 */
public class IntSymbol extends Symbol implements Externalizable {
  private Integer symInt;

  /**
   * A public, no-arg constructor, required by the <code>Externalizable</code>
   * interface.
   * <p>
   * <b><code>IntSymbol</code> objects should not be created via this
   * constructor.</b>
   * <p>
   */
  public IntSymbol() {
  }

  /**
   * Creates an instance whose internal <code>Integer</code>
   * has the value <code>i</code>.
   *
   * @param i the integer value for this new <code>IntSymbol</code>
   */
  IntSymbol(int i) {
    symInt = new Integer(i);
  }
  /**
   * Creates an instance whose internal <code>Integer</code>
   * has the value <code>i</code>.
   *
   * @param i the <code>Integer</code> that will be wrapped by this
   * <code>IntSymbol</code> object
   */
  IntSymbol(Integer i) {
    symInt = i;
  }

  /**
   * Gets a string representation of this symbol.
   *
   * @return a string representation of this symbol, by calling
   * {@link Integer#toString()} on this object's internal <code>Integer</code>
   * object.
   */
  public String toString() {
    return symInt.toString();
  }

  /**
   * Gets the internal <code>Integer</code> object for this
   * symbol.
   *
   * @return the internal <code>Integer</code> object for this
   * symbol.
   */
  public Integer getInteger() { return symInt; }

  /**
   * Returns the key used by the internal symbol map of the class
   * <code>Symbol</code>, which, for this type of symbol, is the
   * <code>Integer</code> object returned by {@link #getInteger()}.
   *
   * @return the object returned by {@link #getInteger()}
   */
  protected Object getSymKey() { return symInt; }

  // methods to comply with Externalizable interface

  /**
   * Writes this object to an <code>ObjectOutput</code> instance.
   *
   * @param out the object stream to which to write an object of this class
   * @throws IOException if the underlying write operation throws an
   * <code>IOException</code>
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(symInt.intValue());
  }

  /**
   * Reads this object from an <code>ObjectInput</code> instance.
   *
   * @param in the object stream from which to read objects of this class
   * @throws IOException if the underlying read operation throws an
   * <code>IOException</code>
   * @throws ClassNotFoundException if the underlying read operation throws
   * an <code>ClassNotFoundException</code>
   */
  public void readExternal(ObjectInput in)
    throws IOException, ClassNotFoundException {
    symInt = new Integer(in.readInt());
  }

  /**
   * Deals with the issue of uniqueness when we are dealing with more
   * than one VM by adding the read symbol to the symbol map, if it
   * is not already there.
   *
   * @return the canonical version of this symbol
   *
   * @throws ObjectStreamException if there is a problem with the underlying
   * read operation
   */
  public Object readResolve() throws ObjectStreamException {
    return Symbol.get(symInt);
  }
}
