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
 * <code>StringSymbol</code> objects associate strings with unique references.
 * This association is maintained in <code>Symbol</code>, not in
 * <code>StringSymbol</code> itself. As a consequence, <b>the new operator for
 * <code>StringSymbol</code> cannot be invoked directly</b>.  Rather,
 * new symbols should be created by calling {@link Symbol#add(String)}.
 *
 * @see Symbol
 * @see IntSymbol
 */

public class StringSymbol extends Symbol implements Externalizable {
  private String printName;

  /**
   * A public, no-arg constructor, required by the <code>Externalizable</code>
   * interface.
   * <p>
   * <b><code>StringSymbol</code> objects should not be created via this
   * constructor.</b>
   * <p>
   */
  public StringSymbol() {
  }

  /**
   * Creates a new StringSymbol object.  <b>Warning: the new operator cannot be
   * invoked directly.</b> Rather, new symbols are created by calling
   * {@link Symbol#add(String)}.
   *
   * @param printName the string containing the symbol's print name.
   */
  StringSymbol(String printName) {
    this.printName = printName;
  }

  /**
   * Returns the print name of this Symbol.
   *
   * @return the print name of this Symbol.
   */
  public String toString() {
    return printName;
  }

  /**
   * Returns <code>null</code>, since this extension of <code>Symbol</code>
   * only stores strings.
   *
   * @return <code>null</code>
   */
  public Integer getInteger() { return null; }

  /**
   * Returns the key used by the internal symbol map of the class
   * <code>Symbol</code>, which, for this type of symbol, is the
   * <code>String</code> object returned by {@link #toString}.
   *
   * @return the object returned by {@link #toString}.
   * <
   */
  protected Object getSymKey() { return printName; }


  // methods to comply with Externalizable interface

  /**
   * Writes this object to an <code>ObjectOutput</code> instance.
   *
   * @param out the object stream to which to write an object of this class
   * @throws IOException if the underlying write operation throws an
   * <code>IOException</code>
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(printName);
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
    printName = (String)in.readObject();
  }


  /**
   * Deals with the issue of uniqueness when we are dealing with more
   * than one VM by adding the read symbol to the symbol map, if it
   * is not already there.
   *
   * @return the canonical instance of the <code>StringSymbol</code> that
   * was read from an <code>ObjectInput</code> instance
   *
   * @throws ObjectStreamException if there is a problem with the underlying
   * read operation
   */
  public Object readResolve() throws ObjectStreamException {
    return Symbol.get(printName);
  }
}
