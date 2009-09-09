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

import danbikel.lisp.*;
import java.io.Serializable;

/**
 * Representation of all possible data present in a complex
 * nonterminal annotation: the base label, any augmentations and any index.
 * Not following the traditional object-oriented design principles, this class
 * is used strictly for representing the data present in a complex nonterminal,
 * but that data is easily manipulated by several methods of {@link Treebank},
 * such as <code>Treebank.parseNonterminal</code> and
 * <code>Treebank.addAugmentation</code>.
 *
 * @see Treebank#parseNonterminal(Symbol,Nonterminal)
 * @see Treebank#addAugmentation(Nonterminal,Symbol)
 * @see Treebank#removeAugmentation(Nonterminal,Symbol)
 * @see Treebank#containsAugmentation(Symbol,Symbol)
 * @see Treebank#getTraceIndex(Sexp,Nonterminal)
 * @see Treebank#stripAllButIndex(Symbol,Nonterminal)
 * @see Treebank#stripIndex(Symbol,Nonterminal)
 */
public class Nonterminal implements Serializable {
  /** The unaugmented base nonterminal. */
  public Symbol base;
  /** A list of symbols representing any augmentations and delimiters. */
  public SexpList augmentations;
  /** The index of the augmented nonterminal, or -1 if none was present. */
  public int index;

  /**
   * Default constructor sets the <code>base</code> data member to be
   * <code>null</code>, the <code>augmentations</code> data member to be
   * a list with no elements and the <code>index</code> data member to be
   * -1.
   */
  public Nonterminal() {
    this(null, new SexpList(), -1);
  }

  /** Sets the data members of this new object to the specified values */
  public Nonterminal(Symbol base, SexpList augmentations, int index) {
    this.base = base;
    this.augmentations = augmentations;
    this.index = index;
  }

  /**
   * Returns whether this nonterminal subsumes the specified nonterminal.  A
   * nonterminal <i>X</i> subsumes another nonterminal <i>Y</i> if the base
   * (unaugmented) nonterminal labels are identical and if <i>X</i> contains a
   * subset of the nonterminal augmentations of <i>Y</i>.  For example,
   * <tt>NP</tt> subsumes <tt>NP-SBJ</tt> and <tt>NP-TMP</tt> subsumes
   * <tt>NP-TMP-CLR-1</tt>.
   *
   * @param other the nonterminal to test for subsumption with this nonterminal
   * @return whether this nonterminal subsumes the specified nonterminal
   */
  public boolean subsumes(Nonterminal other) {
    Symbol thisCanonicalBase = Language.treebank().getCanonical(base);
    Symbol otherCanonicalBase = Language.treebank().getCanonical(other.base);
    if (thisCanonicalBase != Constants.kleeneStarSym &&
	thisCanonicalBase != otherCanonicalBase)
      return false;
    // now check that each augmentation in this nonterminal exists as an
    // augmentation in the other nonterminal
    int thisAugLen = augmentations.length();
    for (int i = 0; i < thisAugLen; i++) {
      Sexp thisAug = augmentations.get(i);
      if (!Language.treebank().isAugDelim(thisAug) &&
	  !other.augmentations.contains(thisAug))
	return false;
    }
    return true;
  }

  /**
   * Returns a string representation of the nonterminal, identical the
   * original nonterminal that was parsed to create this object.
   */
  public String toString() {
    if (base == null || augmentations == null)
      return "null";
    int len = base.toString().length();
    int augLen = augmentations.length();
    // efficiency: don't need StringBuffer if all this has is a base
    if (augLen == 0 && index == -1)
      return base.toString();
    for (int i = 0; i < augLen; i++)
      len += augmentations.symbolAt(i).toString().length();
    StringBuffer sb = new StringBuffer(len + 3);
    sb.append(base);
    for (int i = 0; i < augLen; i++)
      sb.append(augmentations.get(i));
    if (index != -1)
      sb.append(index);
    return sb.toString();
  }

  /** Returns the symbol representing this complex nonterminal. */
  public Symbol toSymbol() {
    if (augmentations.length() == 0 && index == -1)
      return base;
    else
      return Symbol.get(toString());
  }
}

