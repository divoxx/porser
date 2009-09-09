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
import danbikel.parser.constraints.*;
import java.util.*;
import java.text.*;

/**
 * An item in a <code>CKYChart</code> for use when parsing via a probabilistic
 * version of the CKY algorithm.
 *
 * @see CKYChart
 */
public class CKYItem extends Item implements SexpConvertible {
  // "mutable" constants
  /**
   * The value of the {@link Settings#decoderOutputHeadLexicalizedLabels}
   * setting.
   */
  protected static boolean outputLexLabels =
    Settings.getBoolean(Settings.decoderOutputHeadLexicalizedLabels);

  /**
   * The value of the {@link Settings#decoderOutputInsideProbs} setting.
   */
  protected static boolean outputInsideProbs =
    Settings.getBoolean(Settings.decoderOutputInsideProbs);

  /**
   * The value of {@link Treebank#nonTreebankLeftBracket()}.
   */
  protected static char nonTreebankLeftBracket =
    Language.treebank().nonTreebankLeftBracket();

  /**
   * The value of {@link Treebank#nonTreebankRightBracket()}.
   */
  protected static char nonTreebankRightBracket =
    Language.treebank().nonTreebankRightBracket();

  /**
   * The value of {@link Treebank#nonTreebankDelimiter()}.
   */
  protected static char nonTreebankDelimiter =
    Language.treebank().nonTreebankDelimiter();
  
  /**
   * The value of the {@link Settings#baseNPsCannotContainVerbs} setting.
   */
  protected static boolean baseNPsCannotContainVerbs =
    Settings.getBoolean(Settings.baseNPsCannotContainVerbs);

  /**
   * The value of {@link Training#topSym}, cached for efficiency and
   * convenience.
   */
  protected static Symbol topSym = Language.training().topSym();

  /**
   * The value of {@link Training#stopWord()}, cached here for efficiency
   * and convenience.
   */
  protected static Word stopWord = Language.training().stopWord();

  /**
   * The value of the property {@link Settings#numPrevMods}, cached here
   * for efficiency and convenience.
   */
  protected static int numPrevMods =
    Integer.parseInt(Settings.get(Settings.numPrevMods));
  /**
   * The value of the property {@link Settings#numPrevWords}, cached here
   * for efficiency and convenience.
   */
  protected static int numPrevWords =
    Integer.parseInt(Settings.get(Settings.numPrevWords));

  private static class Change implements Settings.Change {
    public void update(Map<String, String> changedSettings) {
      outputLexLabels =
	Settings.getBoolean(Settings.decoderOutputHeadLexicalizedLabels);
      outputInsideProbs =
	Settings.getBoolean(Settings.decoderOutputInsideProbs);
      nonTreebankLeftBracket =
	Language.treebank().nonTreebankLeftBracket();
      nonTreebankRightBracket =
	Language.treebank().nonTreebankRightBracket();
      nonTreebankDelimiter =
	Language.treebank().nonTreebankDelimiter();
      baseNPsCannotContainVerbs =
	Settings.getBoolean(Settings.baseNPsCannotContainVerbs);
      topSym = Language.training().topSym();
      stopWord = Language.training().stopWord();
      numPrevMods =
	Integer.parseInt(Settings.get(Settings.numPrevMods));
      numPrevWords =
	Integer.parseInt(Settings.get(Settings.numPrevWords));
    }
  }

  static {
    Settings.register(CKYItem.class, new Change(),
		      Collections.<Class>singleton(Language.class));
  }

  // true, immutable constants
  /**
   * One of three possible cached values of this item's &ldquo;contains
   * verb&rdquo; status, indicating that the method {@link #containsVerb()}
   * has not yet been invoked on this item.
   */
  protected final static byte containsVerbUndefined = 0;
  /**
   * One of three possible cached values of this item's &ldquo;contains
   * verb&rdquo; status, indicating that the method {@link #containsVerb()}
   * has been invoked on this item and its value is <tt>true</tt> (i.e.,
   * this item has a derivation dominating a verb).
   */
  protected final static byte containsVerbTrue = 1;
  /**
   * One of three possible cached values of this item's &ldquo;contains
   * verb&rdquo; status, indicating that the method {@link #containsVerb()}
   * has been invoked on this item and its value is <tt>false</tt> (i.e.,
   * this item does not have a derivation dominating a verb).
   */
  protected final static byte containsVerbFalse = -1;

  /**
   * A base NP&ndash;aware version of {@link CKYItem} that overrides {@link
   * #equals} and {@link #hashCode()} to take into account the lack of
   * dependence on the distance metric when the root label of an item's
   * set of derivations is <tt>NPB</tt>.
   */
  public static class BaseNPAware extends CKYItem {
    public BaseNPAware() {
      super();
    }

    public Symbol headLabel() {
      if (isPreterminal())
	return null;
      return (Language.treebank.isBaseNP(headChild.label) ?
	      headChild.headChild.label : headChild.label);
    }

    /**
     * Returns whether the specified object is equal (or &ldquo;chart item
     * equivalent&rdquo;) to this item.  Unlike this method in the superclass,
     * there are special cases when
     * <ul>
     * <li>this item is does not have <tt>+TOP+</tt> as its root label and
     * <li>this item represents a base NP.
     * </ul>
     *
     * @param obj the object to be compared with this object
     * @return whether the specified object is equal (or &ldquo;chart item
     *         equivalent&rdquo;) to this item
     */
    public boolean equals(Object obj) {
      if (this == obj)
	return true;
      if (!(obj instanceof BaseNPAware))
	return false;
      BaseNPAware other = (BaseNPAware)obj;
      if (this.label != topSym && stop && other.stop) {
	return (this.isPreterminal() == other.isPreterminal() &&
		this.label == other.label &&
		this.headWord.equals(other.headWord) &&
		this.containsVerb() == other.containsVerb());
      }
      else if (Language.treebank.isBaseNP(this.label)) {
	return (this.stop == other.stop &&
		this.label == other.label &&
		//this.headWord.equals(other.headWord) &&
		this.headLabel() == other.headLabel() &&
		this.leftPrevMods.equals(other.leftPrevMods) &&
		this.rightPrevMods.equals(other.rightPrevMods) &&
		this.prevWordsEqual(other));
      }
      else {
	return (this.isPreterminal() == other.isPreterminal() &&
		this.stop == other.stop &&
		this.leftVerb == other.leftVerb &&
		this.rightVerb == other.rightVerb &&
		this.containsVerb() == other.containsVerb() &&
		this.label == other.label &&
		this.headWord.equals(other.headWord) &&
		this.headLabel() == other.headLabel() &&
		this.leftPrevMods.equals(other.leftPrevMods) &&
		this.rightPrevMods.equals(other.rightPrevMods) &&
		this.leftSubcat.equals(other.leftSubcat) &&
		this.rightSubcat.equals(other.rightSubcat));
      }
    }

    /**
     * Returns a hash code for this item.
     * @return a hash code for this item
     */
    public int hashCode() {
      // all three types of chart items (stopped, baseNP and others)
      // depend on label and head word
      int code = label.hashCode();
      code = (code << 2) ^ headWord.hashCode();

      // special case for stopped items
      if (label != topSym && stop) {
	code = (code << 1) | (isPreterminal() ? 1 : 0);
	return code;
      }

      // special case for baseNP items
      if (Language.treebank.isBaseNP(this.label)) {
	Symbol headLabel = headLabel();
	if (headLabel != null)
	  code = (code << 2) ^ headLabel.hashCode();
	code = (code << 2) ^ leftPrevMods.hashCode();
	code = (code << 2) ^ rightPrevMods.hashCode();
	return code;
      }

      // finish computation of hash code for all other items
      if (leftSubcat != null)
	code = (code << 2) ^ leftSubcat.hashCode();
      if (rightSubcat != null)
	code = (code << 2) ^ rightSubcat.hashCode();
      Symbol headLabel = headLabel();
      if (headLabel != null)
	code = (code << 2) ^ headLabel.hashCode();
      code = (code << 2) ^ leftPrevMods.hashCode();
      code = (code << 2) ^ rightPrevMods.hashCode();
      int booleanCode = 0;
      //booleanCode = (booleanCode << 1) | (stop ? 1 : 0);
      booleanCode = (booleanCode << 1) | (leftVerb ? 1 : 0);
      booleanCode = (booleanCode << 1) | (rightVerb ? 1 : 0);
      return code ^ booleanCode;
    }
  };

  /**
   * Overrides <code>equals</code> and <code>hashCode</code> methods
   * to take the last previous modifier into account only insofar as
   * its equality to the initial {@link Training#startSym} modifier.
   */
  public static class PrevModIsStart extends CKYItem {
    protected final static Symbol startSym = Language.training.startSym();

    public PrevModIsStart() {
      super();
    }
    /**
     * Returns <code>true</code> if and only if the specified object is
     * also an instance of a <code>CKYItem</code> and all elements of
     * this <code>CKYItem</code> are equal to those of the specified
     * <code>CKYItem</code>, except their left and right children lists
     * and their log probability values.
     */
    public boolean equals(Object obj) {
      if (this == obj)
	return true;
      if (!(obj instanceof PrevModIsStart))
	return false;
      PrevModIsStart other = (PrevModIsStart)obj;
      if (this.label != topSym && stop && other.stop) {
	return (this.isPreterminal() == other.isPreterminal() &&
		this.label == other.label &&
		this.headWord.equals(other.headWord) &&
		this.containsVerb() == other.containsVerb());
      }
      else {
	return (this.isPreterminal() == other.isPreterminal() &&
		this.stop == other.stop &&
		this.label == other.label &&
		this.leftVerb == other.leftVerb &&
		this.rightVerb == other.rightVerb &&
		this.containsVerb() == other.containsVerb() &&
		this.headWord.equals(other.headWord) &&
		this.headLabel() == other.headLabel() &&
		this.leftPrevModIsStart() == other.leftPrevModIsStart() &&
		this.rightPrevModIsStart() == other.rightPrevModIsStart() &&
		this.leftSubcat.equals(other.leftSubcat) &&
		this.rightSubcat.equals(other.rightSubcat));
      }
    }

    /**
     * Returns whether the previous modifier on the left side is the {@linkplain
     * Training#startSym() start symbol}.
     *
     * @return whether the previous modifier on the left side is the {@linkplain
     *         Training#startSym() start symbol}.
     */
    protected boolean leftPrevModIsStart() {
      return leftPrevMods.get(0) == startSym;
    }
    /**
     * Returns whether the previous modifier on the right side is the {@linkplain
     * Training#startSym() start symbol}.
     *
     * @return whether the previous modifier on the right side is the {@linkplain
     *         Training#startSym() start symbol}.
     */
    protected boolean rightPrevModIsStart() {
      return rightPrevMods.get(0) == startSym;
    }

    /**
     * Computes the hash code based on all elements used by the
     * {@link #equals} method.
     */
    public int hashCode() {
      // for both types of items (stopped and un-stopped), the equals method
      // relies on items' labels and head words
      int code = label.hashCode();
      code = (code << 2) ^ headWord.hashCode();

      // special case for stopped items
      if (label != topSym && stop) {
	code = (code << 1) | (isPreterminal() ? 1 : 0);
	return code;
      }

      // finish the hash code computation for un-stopped items
      if (leftSubcat != null)
	code = (code << 2) ^ leftSubcat.hashCode();
      if (rightSubcat != null)
	code = (code << 2) ^ rightSubcat.hashCode();
      if (headLabel() != null)
	code = (code << 2) ^ headLabel().hashCode();
      int booleanCode = 0;
      booleanCode = (booleanCode << 1) | (leftPrevModIsStart() ? 1 : 0);
      booleanCode = (booleanCode << 1) | (rightPrevModIsStart() ? 1 : 0);
      //booleanCode = (booleanCode << 1) | (stop ? 1 : 0);
      booleanCode = (booleanCode << 1) | (leftVerb ? 1 : 0);
      booleanCode = (booleanCode << 1) | (rightVerb ? 1 : 0);
      return code ^ booleanCode;
    }
  };

  /**
   * Overrides <code>equals</code> and <code>hashCode</code> methods to compare
   * the last previous modifier on each side of each chart item's head child
   * with respect to their respective equivalence classes, as determined by the
   * mapping provided by {@link NTMapper#map(Symbol)}.
   *
   * @see #mappedPrevModsEqual(CKYItem)
   * @see NonterminalMapper
   * @see NTMapper
   * @see Settings#prevModMapperClass
   */
  public static class MappedPrevModBaseNPAware extends CKYItem {
    public MappedPrevModBaseNPAware() {
      super();
    }

    /*
    public Symbol headLabel() {
      if (isPreterminal())
	return null;
      return (Language.treebank.isBaseNP(headChild.label) ?
	      headChild.headChild.label : headChild.label);
    }
    */

    /**
     * Returns <code>true</code> if and only if the specified object is
     * also an instance of a <code>CKYItem</code> and all elements of
     * this <code>CKYItem</code> are equal to those of the specified
     * <code>CKYItem</code>, except their left and right children lists
     * and their log probability values.  Unlike this method in the superclass,
     * there are special cases when
     * <ul>
     * <li>this item is does not have <tt>+TOP+</tt> as its root label and
     * <li>this item represents a base NP.
     * </ul>
     * Furthermore, only the most recent previous modifiers are compared,
     * and they are mapped to equivalence classes before being compared.
     * Mapping and comparison are performed by the
     * {@link #mappedPrevModsEqual(CKYItem)} method.
     *
     * @param obj the object to compare to this object
     * @return whether this object is equal or equivalent to the specified
     * object
     */
    public boolean equals(Object obj) {
      if (this == obj)
	return true;
      if (!(obj instanceof MappedPrevModBaseNPAware))
	return false;
      MappedPrevModBaseNPAware other = (MappedPrevModBaseNPAware)obj;
      if (this.label != topSym && stop && other.stop) {
	return (this.isPreterminal() == other.isPreterminal() &&
		this.label == other.label &&
		this.headWord.equals(other.headWord) &&
		this.containsVerb() == other.containsVerb());
      }
      else if (Language.treebank.isBaseNP(this.label)) {
	return (this.stop == other.stop &&
		this.label == other.label &&
		//this.headWord.equals(other.headWord) &&
		this.headLabel() == other.headLabel() &&
		this.leftPrevMods.equals(other.leftPrevMods) &&
		this.rightPrevMods.equals(other.rightPrevMods) &&
		this.prevWordsEqual(other));
      }
      else {
	return (this.isPreterminal() == other.isPreterminal() &&
		this.stop == other.stop &&
		this.label == other.label &&
		this.leftVerb == other.leftVerb &&
		this.rightVerb == other.rightVerb &&
		this.containsVerb() == other.containsVerb() &&
		this.headWord.equals(other.headWord) &&
		this.headLabel() == other.headLabel() &&
		this.mappedPrevModsEqual(other) &&
		this.leftSubcat.equals(other.leftSubcat) &&
		this.rightSubcat.equals(other.rightSubcat));
      }
    }

    /**
     * Returns true if the most recvent previous modifiers on both the left and
     * right sides of the head child are equivalent to the respective left and
     * right previous modifiers of the specified chart item. Two previous
     * modifiers are considered equivalent if their equivalence classes are
     * equal.  Mapping of a modifier to an equivlanece class is performed by the
     * {@link NTMapper#map(Symbol)} method.
     *
     * @param other the other chart item whose most recent previous modifiers
     *              are to be compared to those of this item
     * @return whether the most recent previous modifiers of this item are
     *         equivalent to those of the specified item
     *
     * @see NonterminalMapper
     * @see NTMapper
     * @see Settings#prevModMapperClass
     */
    protected boolean mappedPrevModsEqual(CKYItem other) {
      return
	((NTMapper.map(this.leftPrevMods.symbolAt(0)) ==
	  NTMapper.map(other.leftPrevMods.symbolAt(0))) &&

	 (NTMapper.map(this.rightPrevMods.symbolAt(0)) ==
	  NTMapper.map(other.rightPrevMods.symbolAt(0))));
    }

    /**
     * Computes the hash code based on all elements used by the
     * {@link #equals} method.
     */
    public int hashCode() {
      // all three types of chart items (stopped, baseNP and others)
      // depend on label and head word
      int code = label.hashCode();
      code = (code << 2) ^ headWord.hashCode();

      // special case for stopped items
      if (label != topSym && stop) {
	code = (code << 1) | (isPreterminal() ? 1 : 0);
	return code;
      }

      // special case for baseNP items
      if (Language.treebank.isBaseNP(this.label)) {
	Symbol headLabel = headLabel();
	if (headLabel != null)
	  code = (code << 2) ^ headLabel.hashCode();
	code = (code << 2) ^ leftPrevMods.hashCode();
	code = (code << 2) ^ rightPrevMods.hashCode();
	return code;
      }

      // finish computation of hash code for all other items
      if (leftSubcat != null)
	code = (code << 2) ^ leftSubcat.hashCode();
      if (rightSubcat != null)
	code = (code << 2) ^ rightSubcat.hashCode();
      Symbol headLabel = headLabel();
      if (headLabel != null)
	code = (code << 2) ^ headLabel.hashCode();
      Symbol mappedLeftPrevMod = NTMapper.map(leftPrevMods.symbolAt(0));
      Symbol mappedRightPrevMod = NTMapper.map(rightPrevMods.symbolAt(0));
      code = (code << 2) ^ mappedLeftPrevMod.hashCode();
      code = (code << 2) ^ mappedRightPrevMod.hashCode();
      int booleanCode = 0;
      //booleanCode = (booleanCode << 1) | (stop ? 1 : 0);
      booleanCode = (booleanCode << 1) | (leftVerb ? 1 : 0);
      booleanCode = (booleanCode << 1) | (rightVerb ? 1 : 0);
      return code ^ booleanCode;
    }
  };

  /**
   * A hack to approximate <i>k</i>-best parsing by effectively turning
   * off dynamic programming (usability depends on reducing the beam size
   * from its normal value).  Two <code>KBestHack</code> chart items
   * are only equal if they are object-equal.
   */
  public static class KBestHack extends MappedPrevModBaseNPAware {
    public KBestHack() {
      super();
    }
    /**
     * Returns the value of <code>System.identityHashCode(this)</code>.
     * @return the value of <code>System.identityHashCode(this)</code>.
     */
    public int hashCode() { return System.identityHashCode(this); }
    /**
     * Returns whether this object is object-equal to the specified object.
     * @param obj the object to be compared to this object
     * @return whether this object is object-equal to the specified object.
     */
    public boolean equals(Object obj) { return this == obj; }
  };

  // constants
  private final static int outputPrecision = 14;
  private final static int shortOutputPrecision = 3;

  // static data members
  // number formatter for string (debugging) output
  private static NumberFormat doubleNF = NumberFormat.getInstance();
  static {
    doubleNF.setMinimumFractionDigits(outputPrecision);
    doubleNF.setMaximumFractionDigits(outputPrecision);
  }
  private static NumberFormat shortDoubleNF = NumberFormat.getInstance();
  static {
    shortDoubleNF.setMinimumFractionDigits(shortOutputPrecision);
    shortDoubleNF.setMaximumFractionDigits(shortOutputPrecision);
  }


  // data members

  /** The log of the probability of the tree represented by this item. */
  protected double logTreeProb;

  /**
   * The log of the probability of the lexicalized root nonterminal label of
   * the tree represented by this item.
   */
  protected double logPrior;

  /** The label of this chart item. */
  protected Symbol label;

  /** The head word of this chart item. */
  protected Word headWord;

  /** The subcat frame representing the unmet requirements on the left
      side of the head as of the production of this chart item. */
  protected Subcat leftSubcat;

  /** The subcat frame representing the unmet requirements on the right
      side of the head as of the production of this chart item. */
  protected Subcat rightSubcat;

  /** The item representing the head child of the tree node represented by this
      chart item, or <code>null</code> if this item represents a
      preterminal. */
  protected CKYItem headChild;

  /** A list of <code>CKYItem</code> objects that are the children to the left
      of the head child, with the head-adjacent child being last. */
  protected SLNode leftChildren;

  /** A list of <code>CKYItem</code> objects that are the children to the right
      of the head child, with the head-adjacent child being last. */
  protected SLNode rightChildren;

  /** The previous modifiers generated on the left of the head child. */
  protected SexpList leftPrevMods;

  /** The previous modifiers generated on the right of the head child. */
  protected SexpList rightPrevMods;

  /** The index of the first word of the span covered by this item. */
  protected int start;

  /** The index of the last word of the span covered by this item. */
  protected int end;

  /** The constraint associated with this chart item. */
  protected Constraint constraint;

  /** The total number of possible parses represented by this chart item. */
  protected int numParses = 1;

  /** The boolean indicating whether a verb intervenes between the head child
      and the currently-generated left-modifying child. */
  protected boolean leftVerb;

  /** The boolean indicating whether a verb intervenes between the head child
      and the currently-generated right-modifying child. */
  protected boolean rightVerb;

  /** The boolean indicating whether this item has received its stop
      probabilities. */
  protected boolean stop;

  /**
   * The boolean indicating whether this item has been eliminated from the
   * chart because another, equivalent item was added (meaning that this item
   * could not be immediately reclaimed, since the caller of
   * <code>Chart.add</code> may have a handle onto this item).
   */
  protected boolean garbage = false;

  /**
   * The cached value of the result of the {@link #containsVerb()} method
   * invoked on this chart item, initially set to {@link
   * #containsVerbUndefined}.
   */
  protected byte containsVerb = containsVerbUndefined;

  // constructors

  /** Default constructor. Data members set to default values. */
  public CKYItem() {}

  /**
   * Constructs a CKY chart item with the specified data.
   *
   * @param label         the unlexicalized root label of this chart item
   * @param headWord      the head word of this chart item
   * @param leftSubcat    the subcat on the left side of this item's head child
   * @param rightSubcat   the subcat on the right side of this item's head
   *                      child
   * @param headChild     the head child item of this chart item
   * @param leftChildren  the modifiers on the left side of this item's head
   *                      child
   * @param rightChildren the modifiers on the right side of this item's head
   *                      child
   * @param leftPrevMods  a list of the previous modifiers on the left side of
   *                      this item's head child
   * @param rightPrevMods a list of the previous modifiers on the right side of
   *                      this item's head child
   * @param start         the index of the first word spanned by this item
   * @param end           the index of the last word spanned by this item
   * @param leftVerb      whether a verb has been generated anywhere in the
   *                      surface string of the modifiers on the left side of
   *                      this item's head child
   * @param rightVerb     whether a verb has been generated anywhere in the
   *                      surface string of the modifiers on the right side of
   *                      this item's head child
   * @param stop          whether this item has received its stop probabilities
   * @param logTreeProb   the log of the probability of generating all of this
   *                      item's child items (head child and left and right
   *                      modifier children)
   * @param logPrior      the log of the marginal probability of this item's
   *                      lexicalized root label
   * @param logProb       the log of the probability of this chart item (its
   *                      <i>score</i>)
   */
  public CKYItem(Symbol label,
		 Word headWord,
		 Subcat leftSubcat,
		 Subcat rightSubcat,
		 CKYItem headChild,
		 SLNode leftChildren,
		 SLNode rightChildren,
		 SexpList leftPrevMods,
		 SexpList rightPrevMods,
		 int start,
		 int end,
		 boolean leftVerb,
		 boolean rightVerb,
		 boolean stop,
		 double logTreeProb,
		 double logPrior,
		 double logProb) {
    super(logProb);
    this.logTreeProb = logTreeProb;
    this.logPrior = logPrior;
    this.label = label;
    this.headWord = headWord;
    this.leftSubcat = leftSubcat;
    this.rightSubcat = rightSubcat;
    this.headChild = headChild;
    this.leftChildren = leftChildren;
    this.rightChildren = rightChildren;
    this.leftPrevMods = leftPrevMods;
    this.rightPrevMods = rightPrevMods;
    this.start = start;
    this.end = end;
    this.leftVerb = leftVerb;
    this.rightVerb = rightVerb;
    this.stop = stop;
  }


  /**
   * Sets all of the data members of this chart item.
   *
   * @param label         the unlexicalized root label of this chart item
   * @param headWord      the head word of this chart item
   * @param leftSubcat    the subcat on the left side of this item's head child
   * @param rightSubcat   the subcat on the right side of this item's head
   *                      child
   * @param headChild     the head child item of this chart item
   * @param leftChildren  the modifiers on the left side of this item's head
   *                      child
   * @param rightChildren the modifiers on the right side of this item's head
   *                      child
   * @param leftPrevMods  a list of the previous modifiers on the left side of
   *                      this item's head child
   * @param rightPrevMods a list of the previous modifiers on the right side of
   *                      this item's head child
   * @param start         the index of the first word spanned by this item
   * @param end           the index of the last word spanned by this item
   * @param leftVerb      whether a verb has been generated anywhere in the
   *                      surface string of the modifiers on the left side of
   *                      this item's head child
   * @param rightVerb     whether a verb has been generated anywhere in the
   *                      surface string of the modifiers on the right side of
   *                      this item's head child
   * @param stop          whether this item has received its stop probabilities
   * @param logTreeProb   the log of the probability of generating all of this
   *                      item's child items (head child and left and right
   *                      modifier children)
   * @param logPrior      the log of the marginal probability of this item's
   *                      lexicalized root label
   * @param logProb       the log of the probability of this chart item (its
   *                      <i>score</i>)
   */
  public void set(Symbol label,
		  Word headWord,
		  Subcat leftSubcat,
		  Subcat rightSubcat,
		  CKYItem headChild,
		  SLNode leftChildren,
		  SLNode rightChildren,
		  SexpList leftPrevMods,
		  SexpList rightPrevMods,
		  int start,
		  int end,
		  boolean leftVerb,
		  boolean rightVerb,
		  boolean stop,
		  double logTreeProb,
		  double logPrior,
		  double logProb) {
    this.logProb = logProb;
    this.logTreeProb = logTreeProb;
    this.logPrior = logPrior;
    this.label = label;
    this.headWord = headWord;
    this.leftSubcat = leftSubcat;
    this.rightSubcat = rightSubcat;
    this.headChild = headChild;
    this.leftChildren = leftChildren;
    this.rightChildren = rightChildren;
    this.leftPrevMods = leftPrevMods;
    this.rightPrevMods = rightPrevMods;
    this.start = start;
    this.end = end;
    this.leftVerb = leftVerb;
    this.rightVerb = rightVerb;
    this.stop = stop;
    containsVerb = containsVerbUndefined;
    this.numParses = 1;
    garbage = false;
  }

  public Constraint getConstraint() { return constraint; }
  public void setConstraint(Constraint constraint) {
    this.constraint = constraint;
  }

  /** Returns the symbol that is the label of this chart item. */
  public Object label() { return label; }

  /** Returns the head word of this chart item. */
  public Word headWord() { return headWord; }

  /** Returns the left subcat of this chart item. */
  public Subcat leftSubcat() { return leftSubcat; }

  /** Returns the right subcat of this chart item. */
  public Subcat rightSubcat() { return rightSubcat; }

  /** Returns the head child item of this item. */
  public CKYItem headChild() { return headChild; }

  /**
   * Returns the left modifier item list of this item, or <code>null</code> if
   * there are no left modifier items.
   */
  public SLNode leftChildren() { return leftChildren; }

  /** Returns the number of children in the left modifier item list. */
  public int numLeftChildren() {
    return leftChildren == null ? 0 : leftChildren.size();
  }

  /**
   * Returns the right modifier item list of this item, or <code>null</code> if
   * there are no right modifier items.
   */
  public SLNode rightChildren() { return rightChildren; }

  /** Returns the number of children in the right modifier item list. */
  public int numRightChildren() {
    return rightChildren == null ? 0 : rightChildren.size();
  }

  /**
   * Returns a list of previously-generated unlexicalized modifiers on the left
   * side of the head child in this item's set of derivations.  The list will
   * be of length equal to the value of {@link Settings#numPrevMods}.
   *
   * @see Settings#numPrevMods
   */
  public SexpList leftPrevMods() { return leftPrevMods; }

  /**
   * Returns a list of previously-generated unlexicalized modifiers on the right
   * side of the head child in this item's set of derivations.  The list will
   * be of length equal to the value of {@link Settings#numPrevMods}.
   *
   * @see Settings#numPrevMods
   */
  public SexpList rightPrevMods() { return rightPrevMods; }

  /**
   * Returns the start word index of the span of this chart item.  Note that the
   * number of words spanned by this item is
   * <code>(end()&nbsp;-&nbsp;start())&nbsp;+&nbsp;1</code>.
   *
   * @return the start word index of the span of this chart item.
   */
  public int start() { return start; }

  /**
   * Returns the end word index of the span of this chart item.  Note that the
   * number of words spanned by this item is
   * <code>(end()&nbsp;-&nbsp;start())&nbsp;+&nbsp;1</code>.
   *
   * @return the end word index of the span of this chart item.
   */
  public int end() { return end; }

  /**
   * Recursively computes the index of the head word of this derivation.
   * <p/>
   * <b>Implementation advice</b>: Because chart items do not store or cache the
   * index returned by this method, it should be used only for debugging.
   *
   * @return the index of the head word of this derivation
   */
  public int headWordIdx() {
    if (start() == end()) {
      return start();
    }
    else {
      return headChild.headWordIdx();
    }
  }

  /**
   * Returns whether a verb has been generated anywhere in the surface strings
   * of the left modifiers of the head child.
   * @return whether a verb has been generated anywhere in the surface strings
   * of the left modifiers of the head child.
   */
  public boolean leftVerb() { return leftVerb; }

  /**
   * Returns whether a verb has been generated anywhere in the surface strings
   * of the right modifiers of the head child.
   * @return whether a verb has been generated anywhere in the surface strings
   * of the right modifiers of the head child.
   */
  public boolean rightVerb() { return rightVerb; }

  /**
   * Returns whether this item has received its stop probabilities.
   * @return whether this item has received its stop probabilities.
   *
   * @see Training#stopSym()
   */
  public boolean stop() { return stop; }

  /**
   * Returns the log probability, or <i>score</i>, of this chart item.  This is
   * equal to <code>logTreeProb() + logPrior()</code>.
   * @return the log probability, or score, of this chart item.
   *
   * @see #logTreeProb()
   * @see #logPrior()
   */
  public double logProb() { return logProb; }

  /**
   * Returns the probability of generating all the children of this chart
   * item (head child and left and right modifier children).
   * @return the probability of generating all the children of this chart
   * item (head child and left and right modifier children).
   *
   * @see #logProb()
   * @see #logPrior()
   */
  public double logTreeProb() { return logTreeProb; }

  /**
   * Returns the marginal probability of generating the lexicalized root
   * label of this item's set of derivations (strictly speaking, this is not
   * a &ldquo;prior&rdquo; as the name of this method would suggest).
   * @return the marginal probability of generating the lexicalized root
   * label of this item's set of derivations
   *
   * @see #logProb()
   * @see #logTreeProb()
   */
  public double logPrior() { return logPrior; }

  /**
   * Returns the root nonterminal label of the derivation of this item's
   * head child.
   * @return the root nonterminal label of the derivation of this item's
   * head child.
   */
  public Symbol headLabel() {
    if (isPreterminal())
      return null;
    return headChild.label;
  }

  /**
   * Returns whether this item has been eliminated from the chart because
   * another, equivalent item was added (meaning that this item could not be
   * immediately reclaimed, since the caller of <code>Chart.add</code> may have
   * a handle onto this item).
   */
  public boolean garbage() { return garbage; }

  /** The total number of possible parses represented by this chart item. */
  public int numParses() { return numParses; }

  // side-sensitive accessors
  /**
   * Returns the subcat on the specified side of this item's head child.
   * @param side the side whose subcat is to be gotten
   * @return the subcat on the specified side of this item's head child.
   */
  public Subcat subcat(boolean side) {
    return side == Constants.LEFT ? leftSubcat : rightSubcat;
  }
  /**
   * Returns the modifier (children) list of the specified side of this item's
   * head child, or <code>null</code> if the specified side has no modifiers.
   *
   * @param side the side whose children list is to be gotten
   * @return the modifier (children) list of the specified side of this item's
   * head child, or <code>null</code> if the specified side has no modifiers.
   */
  public SLNode children(boolean side) {
    return side == Constants.LEFT ? leftChildren : rightChildren;
  }
  /**
   * Returns the previous modifiers on the specified side of this item's
   * head child.
   * @param side the side whose previous modifier list is to be gotten
   * @return the previous modifiers on the specified side of this item's
   * head child.
   */
  public SexpList prevMods(boolean side) {
    return side == Constants.LEFT ? leftPrevMods : rightPrevMods;
  }
  /**
   * Returns whether a verb has been generated anywhere in the surface string on
   * the specified side of this item's head child.
   *
   * @param side the side of this item's head child to be tested
   * @return whether a verb has been generated anywhere in the surface string on
   *         the specified side of this item's head child.
   */
  public boolean verb(boolean side) {
    return side == Constants.LEFT ? leftVerb : rightVerb;
  }

  /*
  public boolean containsVerb() {
    return leftVerb || rightVerb || Language.treebank.isVerbTag(headWord.tag());
  }
  */

  /**
   * Returns whether a verb has been generated anywhere in the surface string in
   * the set of derivations of this chart item.
   * <p/>
   * <b>Implementation note</b>: The return value of this method is cached
   * locally in this chart item.
   *
   * @return whether a verb has been generated anywhere in the surface string in
   *         the set of derivations of this chart item.
   */
  public boolean containsVerb() {
    if (containsVerb == containsVerbUndefined)
      containsVerb =
	containsVerbRecursive() ? containsVerbTrue : containsVerbFalse;
    return containsVerb == containsVerbTrue;
  }

  /**
   * A helper method for {@link #containsVerb()} that returns whether a verb has
   * been generated anywhere in the surface string of the derivations of this
   * chart item.
   *
   * @return whether a verb has been generated anywhere in the surface string of
   *         the derivations of this chart item.
   *
   * @see Settings#baseNPsCannotContainVerbs
   */
  protected boolean containsVerbRecursive() {
    if (baseNPsCannotContainVerbs && Language.treebank.isBaseNP(this.label))
      return false;
    else if (leftVerb || rightVerb)
      return true;
    // it is crucial to check the head child BEFORE checking the head word
    // (which is, of course, inherited from the head child), since
    // the containsVerb predicate can be "blocked" by NPB's
    else if (headChild != null)
      return headChild.containsVerb();
    else
      return Language.treebank.isVerbTag(headWord.tag());
  }

  /**
   * Returns the value of {@link #start()} if the specified side is
   * {@link Constants#LEFT} or the value of {@link #end()} otherwise.
   * @param side the side of the span whose index is to be gotten
   * @return the value of {@link #start()} if the specified side is
   * {@link Constants#LEFT} or the value of {@link #end()} otherwise.
   */
  public int edgeIndex(boolean side) {
    return side == Constants.LEFT ? start : end;
  }

  // mutators

  /**
   * Sets the label of this chart item.
   *
   * @param label a <code>Symbol</code> object that is to be the label of
   * this chart item
   * @throws ClassCastException if <code>label</code> is not an instance of
   * <code>Symbol</code>
   */
  public void setLabel(Object label) {
    this.label = (Symbol)label;
  }

  /**
   * Sets the left subcat of this chart item to the specified value.
   * @param leftSubcat the left subcat to be set for this chart item
   */
  public void setLeftSubcat(Subcat leftSubcat) {
    this.leftSubcat = leftSubcat;
  }

  /**
   * Sets the right subcat of this chart item to the specified value.
   * @param rightSubcat the right subcat to be set for this chart item
   */
  public void setRightSubcat(Subcat rightSubcat) {
    this.rightSubcat = rightSubcat;
  }

  /**
   * Sets the log of the probability of generating all of this item's children
   * to the specified value.
   *
   * @param logTreeProb the log of the probability of generating all of this
   *                    item's children the log of the probability of generating
   *                    all of this item's children
   * @see #logTreeProb()
   */
  public void setLogTreeProb(double logTreeProb) {
    this.logTreeProb = logTreeProb;
  }

  /**
   * Sets the log of the probability of this chart item (its <i>score</i>) to
   * the specified value.
   *
   * @param logProb the log of the probability of this chart item (its
   *                <i>score</i>)
   *
   * @see #logProb()
   */
  public void setLogProb(double logProb) { this.logProb = logProb; }

  /**
   * Sets the log of the marginal probability of the lexicalized root
   * nonterminal label of this chart item to the specified value.
   *
   * @param logPrior the log of the marginal probability of the lexicalized root
   *                 nonterminal label of this chart item
   *
   * @see #logPrior()
   */
  public void setLogPrior(double logPrior) { this.logPrior = logPrior; }

  // side-sensitive mutators
  /**
   * Sets the subcat on the specified side of this chart item's head child.
   * @param side the side of this chart item's head child on which
   * to set the subcat, either {@link Constants#LEFT} or
   * {@link Constants#RIGHT}
   * @param subcat the subcat to set on the specified side of this item's
   * head child
   */
  public void setSubcat(boolean side, Subcat subcat) {
    if (side == Constants.LEFT)
      this.leftSubcat = subcat;
    else
      this.rightSubcat = subcat;
  }

  /**
   * Sets the modifier (children) list on the specified side of this chart
   * item.
   * @param side the side of this item's head child on which to set
   * the modifier list, either {@link Constants#LEFT} or
   * {@link Constants#RIGHT}
   * @param children the modifier list to set for this item
   */
  public void setChildren(boolean side, SLNode children) {
    if (side == Constants.LEFT)
      this.leftChildren = children;
    else
      this.rightChildren = children;
  }

  /**
   * Sets the previous modifier list on the specified side of this chart
   * item's head child.
   * @param side the side of this chart item's head child on which to set
   * the previous modifier list, either {@link Constants#LEFT} or
   * {@link Constants#RIGHT}
   * @param prevMods the list of previous modifiers to set on the specified
   * side of this chart item's head child
   */
  public void setPrevMods(boolean side, SexpList prevMods) {
    if (side == Constants.LEFT)
      this.leftPrevMods = prevMods;
    else
      this.rightPrevMods = prevMods;
  }

  /**
   * Sets the index of the leftmost or rightmost word spanned by this chart
   * item.
   *
   * @param side  the side of the span whose index is to be set, either {@link
   *              Constants#LEFT} or {@link Constants#RIGHT}
   * @param index the index to be set on the specified side
   *
   * @see #start()
   * @see #end()
   */
  public void setEdgeIndex(boolean side, int index) {
    if (side == Constants.LEFT)
      this.start = index;
    else
      this.end = index;
  }

  /**
   * Sets whether a verb has been generated anywhere in the surface string on
   * the specified side of this item's head child.
   *
   * @param side the side on whose verb-generated value is to be set, either
   *             {@link Constants#LEFT} or {@link Constants#RIGHT}
   * @param verb whether a verb has been generated anywhere in the surface
   *             string on the specified side of this item's head child
   */
  public void setVerb(boolean side, boolean verb) {
    if (side == Constants.LEFT)
      this.leftVerb = verb;
    else
      this.rightVerb = verb;
    containsVerb = containsVerbUndefined;
  }

  /**
   * Sets all the side-specific information for one side of this chart item.
   * This method is a synonym for executing the following code:
   * <pre>
   * setSubcat(side, subcat);
   * setChildren(side, children);
   * setPrevMods(side, prevMods);
   * setEdgeIndex(side, edgeIndex);
   * setVerb(side, verb);
   * </pre>
   *
   * @param side      the side of this item's head child on which to set
   *                  informatioon for this chart item, either {@link
   *                  Constants#LEFT} or {@link Constants#RIGHT}
   * @param subcat    the subcat to be set on the specified side of this item's
   *                  head child
   * @param children  the children list to be set on the specified side of this
   *                  item's head child
   * @param prevMods  the previous modifier list to be set on the specified side
   *                  of this item's head child
   * @param edgeIndex the edge index of the span of this item for either the
   *                  leftmost or rightmost side
   * @param verb      whether a verb has been generated anywhere in the surface
   *                  string on the specified side of this item's head child
   */
  public void setSideInfo(boolean side,
			  Subcat subcat,
			  SLNode children,
			  SexpList prevMods,
			  int edgeIndex,
			  boolean verb) {
    setSubcat(side, subcat);
    setChildren(side, children);
    setPrevMods(side, prevMods);
    setEdgeIndex(side, edgeIndex);
    setVerb(side, verb);
  }

  /**
   * Sets whether this item has been eliminated from the chart because another,
   * equivalent item was added (meaning that this item could not be immediately
   * reclaimed, since the caller of <code>Chart.add</code> may have a handle
   * onto this item).
   */
  public void setGarbage(boolean garbage) {
    this.garbage = garbage;
  }

  /**
   * Indicates that the specified item is an antecedent to this item, allowing
   * a subclass instance to store a list of antecedents, or do other
   * computation based on its antecedents.  The default implementation here
   * multiplies the {@link #numParses} data member of this item by the
   * number of parses of the specified antecedent.
   *
   * @param antecedent an antecedent of this item
   */
  public void hasAntecedent(Item antecedent) {
    numParses *= ((CKYItem)antecedent).numParses;
  }

  /**
   * Indicates that the specified item is equivalent to this item, allowing a
   * subclass to do arbitrary computation when the decoder produces such an item
   * (this method is guaranteed to be called by the decoder when that happens).
   * The default implementation here simply adds the number of parses contained
   * in the specified item to the number of parses contained in the derivation
   * set of this item.
   *
   * @param equivalentItem the item that is equivalent to this item
   */
  public void hasEquivalentItem(Item equivalentItem) {
    numParses += ((CKYItem)equivalentItem).numParses;
  }

  /** Returns <code>true</code> if this item represents a preterminal. */
  public boolean isPreterminal() { return headChild == null; }

  /**
   * Returns <code>true</code> if and only if the specified object is
   * also an instance of a <code>CKYItem</code> and all elements of
   * this <code>CKYItem</code> are equal to those of the specified
   * <code>CKYItem</code>, except their left and right children lists
   * and their log probability values.
   */
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof CKYItem))
      return false;
    CKYItem other = (CKYItem)obj;
    if (this.label != topSym && stop && other.stop) {
      return (this.isPreterminal() == other.isPreterminal() &&
	      this.label == other.label &&
	      this.headWord.equals(other.headWord) &&
	      this.containsVerb() == other.containsVerb());
    }
    else {
      return (this.isPreterminal() == other.isPreterminal() &&
	      this.stop == other.stop &&
	      this.label == other.label &&
	      this.leftVerb == other.leftVerb &&
	      this.rightVerb == other.rightVerb &&
	      this.containsVerb() == other.containsVerb() &&
	      this.headWord.equals(other.headWord) &&
	      this.headLabel() == other.headLabel() &&
	      this.leftPrevMods.equals(other.leftPrevMods) &&
	      this.rightPrevMods.equals(other.rightPrevMods) &&
	      this.leftSubcat.equals(other.leftSubcat) &&
	      this.rightSubcat.equals(other.rightSubcat));
    }
  }

  /**
   * Returns whether the previous word lists of this chart item are equal
   * to those of the specified item.  This method is a synonym for
   * <pre>
   * return prevWordsEqual(Constants.LEFT, other) &&
   *        prevWordsEqual(Constants.RIGHT, other);
   * </pre>
   * @param other the chart item whose previous word lists are to be compared
   * to this item's
   * @return whether the previous word lists of this chart item are equal
   * to those of the specified item.
   *
   * @see #prevWordsEqual(boolean, CKYItem)
   */
  protected boolean prevWordsEqual(CKYItem other) {
    return prevWordsEqual(Constants.LEFT, other) &&
	   prevWordsEqual(Constants.RIGHT, other);
  }

  /**
   * Returns whether the head words of modifier children on the specified
   * side of this item are equal to those on the specified side of the
   * specified other item.
   * <br>
   * <b>Implementation note</b>: This complicated method would not be
   * necessary if we stored appropriate WordList objects within chart items
   * (where "appropriate" means "created by the decoder using the Shifter
   * to skip items that need to be skipped").
   *
   * @param side the side on which to compare head words of modifier children
   * @param other the other chart item with which to compare modifier
   * head words
   * @return whether the previously-generated head words of modifier items
   * of this chart item are equal to those of the specified other chart item
   */
  protected boolean prevWordsEqual(boolean side, CKYItem other) {
    int counter = numPrevWords;

    SLNode thisCurr = children(side);
    SLNode otherCurr = other.children(side);

    while (counter > 0 && thisCurr != null && otherCurr != null) {

      CKYItem thisMod = (CKYItem)thisCurr.data();
      CKYItem otherMod = (CKYItem)otherCurr.data();

      Word thisPrevModHead = thisMod.headWord();
      Word otherPrevModHead = otherMod.headWord();

      boolean skipThis = Shifter.skip(this, thisPrevModHead);
      boolean skipOther = Shifter.skip(other, otherPrevModHead);

      // if neither mod is to be skipped, do comparison of head words
      boolean doCompare = !skipThis && !skipOther;
      if (doCompare) {
	// here's where we actually compare head words
	if (thisPrevModHead.equals(otherPrevModHead)) {
	  counter--;
	}
	else {
	  return false;
	}
      }

      if (skipThis || doCompare)
	thisCurr = thisCurr.next();
      if (skipOther || doCompare)
	otherCurr = otherCurr.next();
    }

    if (counter > 0) {
      // if we haven't finished checking numPrevWords, then end of at least one
      // of child lists was reached, SO...
      // if it is NOT the case that ends of *both* children lists were reached,
      // return false (since lists were not of equal length)
      if (!(thisCurr == null && otherCurr == null)) {
	return false;
      }
      // otherwise, simply return whether head child's head words are equal
      else {
	return this.headWord.equals(other.headWord);
      }
    }

    // it must be that counter == 0, so we've checked numPrevWords words
    return true;
  }

  /**
   * Computes the hash code based on all elements used by the
   * {@link #equals} method.
   */
  public int hashCode() {
    int code = label.hashCode();
    code = (code << 2) ^ headWord.hashCode();
    if (label != topSym && stop) {
      code = (code << 1) | (isPreterminal() ? 1 : 0);
      return code;
    }
    if (leftSubcat != null)
      code = (code << 2) ^ leftSubcat.hashCode();
    if (rightSubcat != null)
      code = (code << 2) ^ rightSubcat.hashCode();
    Symbol headLabel = headLabel();
    if (headLabel != null)
      code = (code << 2) ^ headLabel.hashCode();
    code = (code << 2) ^ leftPrevMods.hashCode();
    code = (code << 2) ^ rightPrevMods.hashCode();
    int booleanCode = 0;
    //booleanCode = (booleanCode << 1) | (stop ? 1 : 0);
    booleanCode = (booleanCode << 1) | (leftVerb ? 1 : 0);
    booleanCode = (booleanCode << 1) | (rightVerb ? 1 : 0);
    return code ^ booleanCode;
  }

  /**
   * Returns the S-expression representation of the tree rooted at this
   * chart item.
   *
   * @return the S-expression representation of the tree rooted at this
   * chart item.
   *
   * @see #toSexpInternal(boolean)
   */
  public Sexp toSexp() {
    return toSexpInternal(false);
  }

  /**
   * Returns the S-expression representation of the tree rooted at this
   * chart item (helper method invoked by {@link #toSexp()}).
   *
   * @param isHeadChild indicates whether the caller of this method is the
   * head child of its parent
   *
   * @return the S-expression representation of the tree rooted at this
   * chart item.
   *
   * @see #outputLexLabels
   * @see #outputInsideProbs
   */
  protected Sexp toSexpInternal(boolean isHeadChild) {
    if (isPreterminal()) {
      Sexp preterm = Language.treebank.constructPreterminal(headWord);
      if (outputLexLabels || outputInsideProbs) {
	Symbol pretermLabel = preterm.list().symbolAt(0);
	preterm.list().set(0, getLabel(pretermLabel, isHeadChild));
      }
      return preterm;
    }
    else {
      int len = numLeftChildren() + numRightChildren() + 2;
      SexpList list = new SexpList(len);
      // first, add label of this node
      list.add(getLabel(label, isHeadChild));
      // then, add left subtrees in order
      for (SLNode curr = leftChildren; curr != null; curr = curr.next())
	list.add(((CKYItem)curr.data()).toSexpInternal(false));
      // next, add head child's subtree
      list.add(headChild.toSexpInternal(true));
      // finally, add right children in reverse order
      if (rightChildren != null) {
	LinkedList rcList = rightChildren.toList();
	ListIterator it = rcList.listIterator(rcList.size());
	while (it.hasPrevious()) {
	  CKYItem item = (CKYItem)it.previous();
	  list.add(item.toSexpInternal(false));
	}
      }
      return list;
    }
  }

  /**
   * Helper method used by {@link #toSexpInternal(boolean)}, to provide a
   * layer of abstraction so that the label can include, e.g., head
   * information.
   *
   * @return the (possibly head-lexicalized) root label of the derivation of
   * this item
   *
   * @see #outputLexLabels
   * @see #outputInsideProbs
   */
  protected Symbol getLabel(Symbol label, boolean isHeadChild) {
    if (outputLexLabels) {
      Nonterminal nt = Language.treebank.parseNonterminal(label);
      char lbracket = nonTreebankLeftBracket;
      char rbracket = nonTreebankRightBracket;
      char sep = nonTreebankDelimiter;
      String insideProb =
	outputInsideProbs ? sep + shortDoubleNF.format(logTreeProb) : "";
      String newLabel =
	nt.base.toString() +
	lbracket + Boolean.toString(isHeadChild) +
	sep + headWord.word() + sep + headWord.tag() +
	sep + start() + sep + end() + sep + headWordIdx() +
	insideProb + rbracket;
      nt.base =	Symbol.add(newLabel);
      return nt.toSymbol();
    }
    else if (outputInsideProbs) {
      Nonterminal nt = Language.treebank.parseNonterminal(label);
      char lbracket = nonTreebankLeftBracket;
      char rbracket = nonTreebankRightBracket;
      String newLabel =
	nt.base.toString() +
	lbracket + shortDoubleNF.format(logTreeProb) + rbracket;
      nt.base = Symbol.add(newLabel);
      return nt.toSymbol();
    }
    else
      return label;
  }

  /**
   * Returns a string containing all the information contained locally
   * in this chart item (for debugging purposes).
   * @return a string containing all the information contained locally
   * in this chart item (for debugging purposes).
   */
  public String toString() {
    return toSexp().toString() + "\t\t; head=" + headWord +
      "; lc=" + leftSubcat.toSexp() + "; rc=" + rightSubcat.toSexp() +
      "; leftPrev=" + leftPrevMods + "; rightPrev=" + rightPrevMods +
      "; lv=" + shortBool(leftVerb) + "; rv=" + shortBool(rightVerb) +
      "; hasVerb=" + shortContainsVerb(containsVerb) +
      "; stop=" + shortBool(stop) +
      "\t; tree=" + doubleNF.format(logTreeProb) +
      "; prior=" + doubleNF.format(logPrior) +
      "; prob=" + doubleNF.format(logProb) +
      "; numParses=" + numParses +
      " (@" + System.identityHashCode(this) + ")";
  }

  /**
   * Returns the string <tt>&quot;t&quot;</tt> if the specified boolean is
   * <code>true</code> and the string <tt>&quot;f&quot;</tt> if the specified
   * boolean is <code>false</code>.
   *
   * @param bool the boolean to be converted to a short string
   * @return the string <tt>&quot;t&quot;</tt> if the specified boolean is
   *         <code>true</code> and the string <tt>&quot;f&quot;</tt> if the
   *         specified boolean is <code>false</code>.
   */
  protected final static String shortBool(boolean bool) {
    return bool ? "t" : "f";
  }

  /**
   * Returns the string <tt>&quot;undef&quot;</tt> if the cached value of this
   * item's &ldquo;contains verb&rdquo; status is {@link
   * #containsVerbUndefined}; otherwise, returns the string that would
   * result in executing <code>shortBool(containsVerb())</code>.
   *
   * @param containsVerbValue
   * @return the string <tt>&quot;undef&quot;</tt> if the cached value of this
   *         item's &ldquo;contains verb&rdquo; status is {@link
   *         #containsVerbUndefined}; otherwise, returns the value of
   *         <code>shortBool(containsVerb())</code>.
   */
  protected final static String shortContainsVerb(byte containsVerbValue) {
    return (containsVerbValue == containsVerbUndefined ? "undef" :
	    (containsVerbValue == containsVerbTrue ? "t" : "f"));
  }

  /**
   * Assigns data members of specified <code>CKYItem</code> to this item,
   * effectively performing a destructive shallow copy of the specified
   * item into this item.
   *
   * @param other the item whose data members are to be assigned to this
   * instance
   * @return this item
   */
  public CKYItem setDataFrom(CKYItem other) {
    this.label = other.label;
    this.headWord = other.headWord;
    this.leftSubcat = other.leftSubcat;
    this.rightSubcat = other.rightSubcat;
    this.headChild =  other.headChild;
    this.leftChildren = other.leftChildren;
    this.rightChildren = other.rightChildren;
    this.leftPrevMods = other.leftPrevMods;
    this.rightPrevMods = other.rightPrevMods;
    this.start = other.start;
    this.end = other.end;
    this.leftVerb = other.leftVerb;
    this.rightVerb = other.rightVerb;
    this.stop = other.stop;
    this.logTreeProb = other.logTreeProb;
    this.logProb = other.logProb;
    this.logPrior = other.logPrior;
    this.constraint = other.constraint;
    this.numParses = other.numParses;
    containsVerb = containsVerbUndefined;
    garbage = false;
    return this;
  }

  /**
   * Sets the number of parses represented by this chart item to 1.
   * @return this chart item
   */
  public Item clear() { numParses = 1; return this; }

  /*
  public CKYItem shallowCopy() {
    return new CKYItem(label, headWord,
		       leftSubcat, rightSubcat,
		       headChild, leftChildren, rightChildren,
		       (SexpList)leftPrevMods.deepCopy(),
		       (SexpList)rightPrevMods.deepCopy(),
		       start, end,
		       leftVerb, rightVerb, stop,
		       logTreeProb, logPrior, logProb);
  }
  */
}
