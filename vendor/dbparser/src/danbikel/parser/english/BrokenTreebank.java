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
    package danbikel.parser.english;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import danbikel.lisp.*;
import danbikel.parser.Language;
import danbikel.parser.Nonterminal;

/**
 * Provides data and methods speciifc to the structures found in the English
 * Treebank (the Penn Treebank) or any other treebank that conforms to the
 * Treebank II annotation guidelines for part-of-speech tagging and bracketing.
 * <p/>
 * <b>Important note</b>: This class is similar to {@link Treebank}, except
 * that it is &ldquo;broken&rdquo; in the sense that instead of doing the
 * closest possible emulation of Collins&rsquo; parsing model, it only uses
 * details found in Collins&rsquo; published papers.  See <a
 * href="http://www.cis.upenn.edu/~dbikel/papers/collins-intricacies.pdf">Intricacies
 * of Collins&rdquo; Parsing Model</a>.
 */
public class BrokenTreebank extends danbikel.parser.lang.AbstractTreebank {

  // the characters that are delimiters of augmented nonterminal labels
  private final static String augmentationDelimStr = "-=|";

  // basic nodes in the English Treebank that will be transformed in a
  // preprocessing phase
  static final Symbol NP = Symbol.add("NP");
  static final Symbol S = Symbol.add("S");
  // transformed nodes
  static final Symbol baseNP = Symbol.add(NP + "B");
  static final Symbol subjectlessS = Symbol.add(S + "G");
  static final Symbol subjectAugmentation = Symbol.add("SBJ");

  // other basic nodes
  static final Symbol WHNP = Symbol.add("WHNP");
  static final Symbol CC = Symbol.add("CC");
  static final Symbol comma = Symbol.add(",");
  static final Symbol lrb = Symbol.add("-LRB-");
  static final Symbol lcb = Symbol.add("-LCB-");
  static final Symbol rrb = Symbol.add("-RRB-");
  static final Symbol rcb = Symbol.add("-RCB-");

  // null element
  static final Symbol nullElementPreterminal = Symbol.add("-NONE-");
  // possessive part of speech
  static final Symbol possessivePos = Symbol.add("POS");

  // exception nonterminals for defaultParseNonterminal
  static final Symbol[] nonterminalExceptionArr = {lrb, rrb};

  private static Symbol[][] canonicalLabelMapData = {
    {baseNP, NP},
    {subjectlessS, S},
  };
  private static HashMap canonicalLabelMap =
    new HashMap(canonicalLabelMapData.length);
  static {
    for (int i = 0; i < canonicalLabelMapData.length; i++)
      canonicalLabelMap.put(canonicalLabelMapData[i][0],
                            canonicalLabelMapData[i][1]);
  }

  private static String[] puncToRaiseElements = {",", ":"};

  private static Set puncToRaise = new HashSet();
  static {
    for (int i = 0; i < puncToRaiseElements.length; i++)
      puncToRaise.add(Symbol.add(puncToRaiseElements[i]));
  }

  private static String[] verbTagStrings = {"MD", "VB", "VBD", "VBG", "VBN",
					    "VBP", "VBZ"};
  private static Set verbTags = new HashSet();
  static {
    for (int i = 0; i < verbTagStrings.length; i++)
      verbTags.add(Symbol.add(verbTagStrings[i]));
  }

  /**
   * Constructs an English <code>Treebank</code> object.
   */
  public BrokenTreebank() {
    super();
    nonterminalExceptionSet = nonterminalExceptionArr;
  }

  /**
   * Returns <code>true</code> if <code>tree</code> represents a preterminal
   * subtree (part-of-speech tag and word).  Specifically, this method
   * returns <code>true</code> if <code>tree</code> is an instance of
   * <code>SexpList</code>, has a length of 2 and has a first list element
   * of type <code>Symbol</code>.
   */
  public final boolean isPreterminal(Sexp tree) {
    return (tree.isList() &&
	    tree.list().length() == 2 &&
	    tree.list().get(0).isSymbol() &&
	    tree.list().get(1).isSymbol());
  }

  /**
   * Returns <code>true</code> is the specified nonterminal label represents a
   * sentence in the Penn Treebank, that is, if the canonical version of
   * <code>label</code> is equal to <code>&quot;S&quot;</code>.
   * @see Training#relabelSubjectlessSentences(Sexp)
   */
  public boolean isSentence(Symbol label) { return getCanonical(label) == S; }

  public Symbol sentenceLabel() { return S; }

  /**
   * Returns the symbol that
   * {@link Training#relabelSubjectlessSentences(Sexp)}
   * will use for sentences that have no subjects.
   */
  public Symbol subjectlessSentenceLabel() { return subjectlessS; }

  public Symbol subjectAugmentation() { return subjectAugmentation; }

  /**
   * Returns <code>true</code> if the specified S-expression represents a
   * preterminal whose terminal element is the null element
   * (<code>&quot;-NONE-&quot;</code>) for the Penn Treebank.
   * @see Training#relabelSubjectlessSentences(Sexp)
   */
  public boolean isNullElementPreterminal(Sexp tree) {
    return (isPreterminal(tree) &&
	    tree.list().get(0).symbol() == nullElementPreterminal);
  }

  /**
   * Returns <code>true</code> if the specified S-expression is a preterminal
   * whose part of speech is <code>&quot;,&quot;</code> or
   * <code>&quot;:&quot;</code>.
   */
  public boolean isPuncToRaise(Sexp preterm) {
    return (isPreterminal(preterm) &&
	    puncToRaise.contains(preterm.list().first()));
  }

  public boolean isPunctuation(Symbol tag) {
    //return puncToRaise.contains(stripAugmentation(tag));
    return puncToRaise.contains(tag);
  }

  /**
   * Returns <code>true</code> if the specified S-expression represents
   * a preterminal that is the possessive part of speech.  This method is
   * intended to be used by implementations of {@link
   * Training#addBaseNPs(Sexp)}.
   */
  public boolean isPossessivePreterminal(Sexp tree) {
    return (isPreterminal(tree) &&
	    tree.list().get(0).symbol() == possessivePos);
  }

  /**
   * Returns <code>true</code> if the canonical version of the specified label
   * is an NP for for English Treebank.
   *
   * @see Training#addBaseNPs(Sexp)
   */
  public boolean isNP(Symbol label) {
    return getCanonical(label) == NP;
  }

  /**
   * Returns the symbol with which {@link Training#addBaseNPs(Sexp)} will
   * relabel base NPs.
   *
   * @see Training#addBaseNPs
   */
  public Symbol baseNPLabel() { return baseNP; }

  /**
   * Returns <code>true</code> if the canonical version of the specified label
   * is a WHNP in the English Treebank.
   *
   * @see Training#addGapInformation(Sexp)
   */
  public boolean isWHNP(Symbol label) { return getCanonical(label) == WHNP; }

  /**
   * Returns the symbol that {@link Training#addBaseNPs(Sexp)} should
   * add as a parent if a base NP is not dominated by an NP.
   */
  public Symbol NPLabel() { return NP; }

  /**
   * Returns <code>true</code> if <code>label</code> is equal to the symbol
   * whose print name is <code>&quot;CC&quot;</code>.
   */
  public boolean isConjunction(Symbol label) {
    //return getCanonical(label) == CC;
    return label == CC;
  }

  /**
   * Returns <code>true</code> if <code>preterminal</code> represents a
   * terminal with one of the following parts of speech: <tt>VB, VBD, VBG,
   * VBN, VBP</tt> or <tt>VBZ</tt>.  It is an error to call this method
   * with a <code>Sexp</code> object for which {@link #isPreterminal(Sexp)}
   * returns <code>false</code>.<br>
   *
   * @param preterminal the preterminal to test
   * @return <code>true</code> if <code>preterminal</code> is a verb
   */
  public boolean isVerb(Sexp preterminal) {
    return isVerbTag(preterminal.list().get(0).symbol());
  }

  public boolean isVerbTag(Symbol tag) {
    return verbTags.contains(tag);
  }

  public boolean isComma(Symbol word) {
    return word == comma;
  }

  /**
   * This method returns <code>false</code> under all circumstances, as though
   * we didn't know about Mike's hack for the comma-pruning rule that says that
   * commas that occur after left parens and before right parens (or the end
   * of the sentence) are not considered commas for the purposes of comma
   * pruning.
   *
   * @param word the word to test
   * @return <code>false</code> regardless of the specified word
   */
  public boolean isLeftParen(Symbol word) {
    return false;
  }

  /**
   * This method returns <code>false</code> under all circumstances, as though
   * we didn't know about Mike's hack for the comma-pruning rule that says that
   * commas that occur after left parens and before right parens (or the end
   * of the sentence) are not considered commas for the purposes of comma
   * pruning.
   *
   * @param word the word to test
   * @return <code>false</code> regardless of the specified word
   */
  public boolean isRightParen(Symbol word) {
    return false;
  }

  /**
   * Returns a canonical mapping for the specified nonterminal label; if
   * <code>label</code> already is in canonical form, it is returned.
   * The canonical mapping refers to transformations performed on nonterminals
   * during the training process.  Before obtaining a label's canonical form,
   * it is also stripped of all Treebank augmentations, meaning that only
   * the characters before the first occurrence of '-', '=' or '|' are kept.
   *
   * @return a <code>Symbol</code> with the same print name as
   * <code>label</code>, except that all training transformations and Treebank
   * augmentations have been undone and stripped
   */
  public final Symbol getCanonical(Symbol label) {
    for (int i = 0; i < nonterminalExceptionSet.length; i++)
      if (label == nonterminalExceptionSet[i])
	return label;
    Symbol strippedLabel = stripAugmentation(label);
    Symbol mapEntry = (Symbol)canonicalLabelMap.get(strippedLabel);
    return ((mapEntry == null) ? strippedLabel : mapEntry);
  }

  public final Symbol getCanonical(Symbol label, boolean stripAugmentations) {
    if (stripAugmentations)
      return getCanonical(label);
    else {
      Symbol mapEntry = (Symbol)canonicalLabelMap.get(label);
      return (mapEntry == null ? label : mapEntry);
    }
  }

  /**
   * Calls {@link Treebank#defaultParseNonterminal(Symbol, Nonterminal)} with
   * the specified arguments.
   * @param label to the nonterminal label to parse
   * @param nonterminal the <code>Nonterminal</code> object to fill with
   * the components of <code>label</code>
   */
  public Nonterminal parseNonterminal(Symbol label, Nonterminal nonterminal) {
    defaultParseNonterminal(label, nonterminal);
    return nonterminal;
  }

  /**
   * Returns a string of the three characters that serve as augmentation
   * delimiters in the Penn Treebank: <code>&quot;-=|&quot;</code>.
   */
  public String augmentationDelimiters() { return augmentationDelimStr; }
}
