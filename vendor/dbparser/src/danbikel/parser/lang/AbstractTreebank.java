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
    package danbikel.parser.lang;

import danbikel.util.Text;
import danbikel.lisp.*;
import danbikel.parser.*;
import java.io.Serializable;
import java.util.*;

/**
 * A collection of mostly-abstract methods to be implemented by a
 * langauge-specific subclass.  A <code>Treebank</code> implementation
 * provides data and methods specific to the structures found in a
 * particular Treebank.
*/
public abstract class AbstractTreebank implements Treebank, Serializable {
  // constants
  private static final Symbol[] zeroLenSymbolArr = new Symbol[0];

  // data members
  /**
   * A <code>BitSet</code> indexed by character (that is, whose size is
   * <code>Character.MAX_VALUE</code>), where for each character <code>c</code>
   * of the string returned by {@link #augmentationDelimiters},
   * <pre>
   * augmentationDelimSet.get(c)
   * </pre>
   * returns <code>true</code>.  The default constructor of this abstract class
   * will appropriately initialize this data member.
   */
  protected BitSet augmentationDelimSet;

  /**
   * A Symbol created from the first character of {@link
   * Treebank#augmentationDelimiters}.
   */
  protected final Symbol canonicalAugDelimSym;

  /**
   * A set of nonterminal labels (<code>Symbol</code> objects) that
   * {@link #defaultParseNonterminal(Symbol,Nonterminal)} should use when
   * determining the base nonterminal label.  If this behavior is desired,
   * this array should be assigned in in the constructor of a subclass.
   * This hook into the behavior of <code>defaultParseNonterminal</code> is
   * primarily intended for the unfortunate case when Treebank designers have
   * nonterminal labels that contain the delimiters used for augmenting
   * nonterminal labels (as is the case with the English Treebank in the
   * form of <tt>-LRB-</tt> and <tt>-RRB-</tt>).
   */
  protected Symbol[] nonterminalExceptionSet = new Symbol[0];

  /**
   * No-arg constructor, to be called by all subclasses of this abstract
   * class.  This constructor fills in the data member
   * {@link #augmentationDelimSet} based on the string returned by
   * {@link #augmentationDelimiters}.
   */
  public AbstractTreebank() {
    augmentationDelimSet = new BitSet(Character.MAX_VALUE);
    String augmentationDelimStr = augmentationDelimiters();
    for (int i = 0; i < augmentationDelimStr.length(); i++)
      augmentationDelimSet.set(augmentationDelimStr.charAt(i));
    canonicalAugDelimSym =
      Symbol.add(new String(new char[] {canonicalAugDelimiter()}));
  }

  /**
   * Returns whether <code>tree</code> represents a preterminal subtree in the
   * parse trees for this language's Treebank.  Typically, preterminals are
   * part-of-speech tags.
   */
  public abstract boolean isPreterminal(Sexp tree);

  /**
   * Gets the component of the preterminal tree that corresponds to the
   * part of speech tag.  This default implementation returns the symbol
   * that is returned by
   * <pre>
   * preterminal.list().get(0).symbol();
   * </pre>
   * If this is not appropriate for a particular Treebank, then this method
   * should be overridden.
   *
   * @param preterminal a tree that is assumed to be a preterminal
   * @return the symbol in <code>preterminal</code> that is a part of speech
   */
  public Symbol getTag(Sexp preterminal) {
    return preterminal.list().get(0).symbol();
  }

  /**
   * Constructs a <code>Word</code> object from the specified preterminal
   * subtree.  This default implementation creates a {@link Word} object from
   * the terminal and preterminal symbols (word and part-of-speech tag
   * symbols):
   * <pre>
   * Words.get(preterminal.list().get(1).symbol(),
   *	       preterminal.list().get(0).symbol());
   * </pre>
   * If a particular Treebank requires a different type of word object to be
   * constructed, or has a different preterminal tree structure, this method
   * should be overridden.
   *
   * @param preterminal a tree that is assumed to be a preterminal
   * @return the symbol in <code>preterminal</code> that is a part of speech
   */
  public Word makeWord(Sexp preterminal) {
    return Words.get(preterminal.list().get(1).symbol(),
		     preterminal.list().get(0).symbol());
  }

  /**
   * Converts a {@link Word} object into a preterminal subtree.  This default
   * implementation creates a tree whose sole nonterminal is the part of speech
   * of the specified word object and whose terminal is the word component of
   * the specified word object.
   *
   * @param word the word object from which to create a preterminal subtree
   * @return a preterminal subtree constructed from <code>word</code>
   */
  public Sexp constructPreterminal(Word word) {
    return new SexpList(2).add(word.tag()).add(word.word());
  }

  /**
   * Returns a canonical mapping for the specified nonterminal label; if
   * <code>label</code> already is in canonical form, it is returned.
   * This method is intended to be used by implementations of
   * {@link HeadFinder#findHead(Sexp)}.
   *
   * @see HeadFinder#findHead(Sexp)
   */
  public abstract Symbol getCanonical(Symbol label);

  public abstract Symbol getCanonical(Symbol label, boolean stripAugmentations);

  /**
   * Returns <code>true</code> is the specified nonterminal label represents a
   * sentence in this language&rsquo;s Treebank.  This method is intended to
   * be used by implementations of {@link
   * Training#relabelSubjectlessSentences(Sexp)}.
   */
  public abstract boolean isSentence(Symbol label);

  /**
   * Returns the canonical label for a sentence, for de-transforming sentences
   * that were transformed via {@link
   * Training#relabelSubjectlessSentences(Sexp)}.
   */
  public abstract Symbol sentenceLabel();

  /**
   * Returns the symbol with which {@link
   * Training#relabelSubjectlessSentences(Sexp)}
   * will relabel sentences when they have no subjects.
   */
  public abstract Symbol subjectlessSentenceLabel();

  /**
   * Returns the symbol that is used to augment nonterminals to indicate matrix
   * subjects in this language&rsquo;s Treebank.
   *
   * @see Training#relabelSubjectlessSentences(Sexp)
   */
  public abstract Symbol subjectAugmentation();

  /**
   * Returns <code>true</code> if the specified S-expression represents
   * a preterminal whose terminal element is the null element for this
   * language&rsquo;s Treebank.  This method is intended to be used by implementations
   * of {@link Training#relabelSubjectlessSentences(Sexp)}.
   *
   * @see Training#relabelSubjectlessSentences(Sexp)
   */
  public abstract boolean isNullElementPreterminal(Sexp tree);

  /**
   * Returns the index of a trace for the specified null element preterminal.
   * This default implementation assumes trace indicies are marked on trace
   * terminals that can be parsed by {@link
   * #parseNonterminal(Symbol,Nonterminal)}.  If this is not true for a
   * particular Treebank, this method should be overridden.  If
   * <code>preterm</code> is not a null element preterminal (that is, a
   * preterminal for which {@link #isNullElementPreterminal(Sexp)} returns
   * <code>false</code>), the semantics of this method are undefined.  This
   * method is used by the default implementation of {@link
   * AbstractTraining#hasGap(Sexp,Sexp,ArrayList)}, which is a helper method
   * for the default implementation of {@link Training#addGapInformation(Sexp)}.
   *
   * @param preterm the null element preterminal whose trace index is to be
   * returned
   * @param nonterminal the object used as the second argument to
   * {@link #parseNonterminal(Symbol,Nonterminal)}
   * @return the index of the trace of the terminal contained in
   * <code>preterm</code>, or -1 if the null element does not have an index
   */
  public int getTraceIndex(Sexp preterm, Nonterminal nonterminal) {
    parseNonterminal(preterm.list().get(1).symbol(), nonterminal);
    return nonterminal.index;
  }

  /**
   * Returns <code>true</code> if the specified S-expression represents
   * a preterminal and a part-of-speech tag that indicates punctuation
   * to be raised when running {@link Training#raisePunctuation(Sexp)}.  If
   * punctuation raising is not desirable for a particular language
   * package, this method may be implemented simply to return
   * <code>false</code>.
   *
   * @param preterm the preterminal to test
   * @see Training#raisePunctuation(Sexp)
   */
  public abstract boolean isPuncToRaise(Sexp preterm);

  /**
   * Returns <code>true</code> if the specified part of speech tag is one
   * for which {@link #isPuncToRaise(Sexp)} would return <code>true</code>.
   *
   * @param tag the part of speech to test
   * @see #isPuncToRaise(Sexp)
   */
  public abstract boolean isPunctuation(Symbol tag);

  /**
   * Returns <code>true</code> if the specified S-expression represents
   * a preterminal that is the possessive part of speech.  This method is
   * intended to be used by implementations of {@link
   * Training#addBaseNPs(Sexp)}.
   *
   * @see Training#addBaseNPs(Sexp)
   */
  public abstract boolean isPossessivePreterminal(Sexp tree);

  /**
   * Returns <code>true</code> if the canonical version of the specified label
   * is an NP for this language&rsquo;s Treebank.
   *
   * @see Training#addBaseNPs(Sexp)
   */
  public abstract boolean isNP(Symbol label);

  /**
   * Returns the symbol with which {@link Training#addBaseNPs(Sexp)} will
   * relabel core NPs.
   *
   * @see Training#addBaseNPs(Sexp)
   */
  public abstract Symbol baseNPLabel();

  /**
   * Returns whether the specified label is for a base NP.  The default
   * implementation here simply tests for object equality between the
   * specified label and the label returned by {@link #baseNPLabel}.  If
   * a particular language package can have various types of base NP labels
   * (such as those bearing node augmentations), then this method
   * should be overridden.
   *
   * @param label the label to test
   *
   * @return whether the specified label is for a base NP.
   */
  public boolean isBaseNP(Symbol label) {
    return label == baseNPLabel();
  }

  /**
   * Returns <code>true</code> if the canonical version of the specified label
   * is an NP that undergoes WH-movement in a particular Treebank.  This method
   * is used by {@link Training#addGapInformation(Sexp)}.  If a particular
   * language package does not require gap information, then this method may be
   * implemented simply to return <code>false</code>.
   *
   * @see Training#addGapInformation(Sexp)
   */
  public abstract boolean isWHNP(Symbol label);

  /**
   * Returns the symbol that {@link Training#addBaseNPs(Sexp)} should
   * add as a parent if a base NP is not dominated by an NP.
   *
   * @see Training#addBaseNPs(Sexp)
   */
  public abstract Symbol NPLabel();

  /**
   * Returns <code>true</code> if the canonical version of the specified label
   * is a conjunction tag or nonterminal in a particular Treebank.
   */
  public abstract boolean isConjunction(Symbol label);

  /**
   * Returns <code>true</code> if the specified preterminal is that of a verb.
   * This method is used by {@link HeadTreeNode} to determine if a particular
   * subtree contains a verb, which is in turn used by {@link Trainer} to
   * calculate the distance metric, which depends on whether a verb occurs
   * in the subtrees of the previous modifiers.  It is the responsibility
   * of the caller to insure that <code>preterminal</code> is a
   * <code>Sexp</code> object for which {@link #isPreterminal(Sexp)} returns
   * <code>true</code>.
   *
   * @see HeadTreeNode
   * @see Trainer
   */
  public abstract boolean isVerb(Sexp preterminal);

  /**
   * Returns <code>true</code> if the specified symbol is the part of speech
   * tag of a verb.  This method should return true for exactly the same
   * parts of speech for which {@link #isVerb(Sexp)} returns <code>true</code>,
   * and is used to calculate the distance metric while decoding.
   *
   * @see CKYItem#containsVerb()
   * @see Decoder
   */
  public abstract boolean isVerbTag(Symbol tag);

  /**
   * Returns <code>true</code> if the specified word is a comma.  This method
   * is used by the <code>Decoder</code> class when performing the comma
   * constraint on chart items.
   *
   * @param word the word to test
   * @see Settings#decoderUseCommaConstraint
   */
  public abstract boolean isComma(Symbol word);

  /**
   * Returns <code>true</code> if the specified word is a left
   * parenthesis.  This method is used by the <code>Decoder</code>
   * class when performing the comma constraint on chart items.
   *
   * @param word the word to test
   * @see Settings#decoderUseCommaConstraint
   */
  public abstract boolean isLeftParen(Symbol word);

  /**
   * Returns <code>true</code> if the specified word is a right
   * parenthesis.  This method is used by the <code>Decoder</code>
   * class when performing the comma constraint on chart items.
   *
   * @param word the word to test
   * @see Settings#decoderUseCommaConstraint
   */
  public abstract boolean isRightParen(Symbol word);

  /**
   * Returns a string whose characters are the set of delimiters for
   * complex nonterminal labels.
   *
   * @see #stripAugmentation(Symbol)
   * @see #defaultParseNonterminal(Symbol,Nonterminal)
   */
  public abstract String augmentationDelimiters();

  /**
   * Returns the first character of the string returned by
   * {@link #augmentationDelimiters}, which will be considered the
   * &quot;canonical&quot; augmentation delimiter when adding
   * new augmentations, such as the argument augmentations added by
   * implementations of {@link Training#identifyArguments(Sexp)}.
   */
  public char canonicalAugDelimiter() {
    return augmentationDelimiters().charAt(0);
  }

  /**
   * Returns a left-bracket character that is not an existing metacharacter
   * in the current treebank, for use when the
   * {@link Settings#decoderOutputHeadLexicalizedLabels} is <tt>true</tt>.
   * The default implementation here returns <tt>'['</tt>.
   *
   * @return a left-bracket character that is not an existing metacharacter
   * in the current treebank
   */
  public char nonTreebankLeftBracket() {
    return '[';
  }

  /**
   * Returns a right-bracket character that is not an existing metacharacter in
   * the current treebank, for use when constructing lexicalized nonterminals
   * when the {@link Settings#decoderOutputHeadLexicalizedLabels} is
   * <tt>true</tt>. The default implementation here returns <tt>']'</tt>.
   *
   * @return a right-bracket character that is not an existing metacharacter in
   *         the current treebank
   */
  public char nonTreebankRightBracket() {
    return ']';
  }
  public char nonTreebankDelimiter() {
    return '/';
  }

  /**
   * Returns the <code>Symbol</code> created by stripping off all
   * augmentations, that is all characters after and including the first
   * character that appears in the string returned by
   * {@link #augmentationDelimiters}.
   *
   * @param label the potentially-complex nonterminal label to be stripped
   * @return a version of <code>label</code> with all augmentations removed
   */
  public Symbol stripAugmentation(Symbol label) {
    String labelStr = label.toString();
    int labelStrLen = labelStr.length();
    int augmentationIdx = -1;

    for (int i = 0; i < labelStrLen; i++) {
      if (augmentationDelimSet.get(labelStr.charAt(i))) {
	augmentationIdx = i;
	break;
      }
    }
    return ((augmentationIdx == -1) ? label :
	    Symbol.add(labelStr.substring(0, augmentationIdx)));
  }

  /**
   * Returns <code>label</code>, but stripped of any index augmentation.  This
   * method assumes that the index will always be the final augmentation in a
   * complex nonterminal label.<br><b>N.B.</b>: This method will create a new
   * <code>Nonterminal</code> object, to be filled in by {@link
   * #stripIndex(Symbol,Nonterminal)}.
   *
   * @param label the nonterminal to be stripped of any possible index
   * @return a <code>Symbol</code> that is identical to <code>label</code>,
   * except that all characters after and including the final delimiter
   * are removed if the final augmentation is composed entirely of digits
   */
  public Symbol stripIndex(Symbol label) {
    return stripIndex(label, new Nonterminal());
  }

  /**
   * Identical to {@link #stripIndex(Symbol)}, except that instead of creating
   * a new <code>Nonterminal</code> object for use by {@link
   * #parseNonterminal(Symbol,Nonterminal)}, this method simply passes the
   * specified <code>nonterminal</code> object.  In a sequential run, this
   * method provides maximum efficiency, as only one <code>Nonterminal</code>
   * object need be created at the beginning of the run.
   */
  public Symbol stripIndex(Symbol label, Nonterminal nonterminal) {
    parseNonterminal(label, nonterminal);
    if (nonterminal.index == -1)
      return label;
    else {
      // the last element in the augmentations list was the delimiter
      // between the pre-index part of the nonterminal and the index
      nonterminal.augmentations.remove(nonterminal.augmentations.size() - 1);
      nonterminal.index = -1; // "remove" the index
      return Symbol.add(nonterminal.toString());
    }
  }

  /**
   * Returns a symbol identical to the specified <code>label</code>, except
   * all augmentations other than the index will be removed.  If
   * <code>label</code> had no index to begin with, then this method
   * is functionally identical to {@link #stripAugmentation(Symbol)}.
   * @param label the nonterminal label to strip of non-index augmentations
   */
  public Symbol stripAllButIndex(Symbol label) {
    return stripAllButIndex(label, new Nonterminal());
  }

  /**
   * Identical to {@link #stripAllButIndex(Symbol)}, except that instead of
   * creating a new <code>Nonterminal</code> object for use by
   * {@link #parseNonterminal(Symbol,Nonterminal)}, this method
   * uses the specified <code>nonterminal</code> object.  In a sequential
   * run, this method provides maximum efficiency, as only one
   * <code>Nonterminal</code> object need be created at the beginning
   * of the run.
   */
  public Symbol stripAllButIndex(Symbol label, Nonterminal nonterminal) {
    parseNonterminal(label, nonterminal);
    if (nonterminal.index == -1)
      return nonterminal.base;
    else
      return Symbol.add(nonterminal.base.toString() +
			nonterminal.augmentations.last() +
			nonterminal.index);
  }

  /**
   * Returns a <code>Nonterminal</code> object to represent all the
   * components of a complex nonterminal annotation: the base label, any
   * augmentations and any index.  If there are no augmentations, the
   * <code>augmentations</code> field of the returned object will contain
   * a list with zero elements; if there is no index, the
   * value of index will be -1.  A final requirement of the contract of this
   * method is to represent all the delimiters in the list of augmentations;
   * this requirement is met, for example, by the helper method {@link
   * #defaultParseNonterminal(Symbol,Nonterminal)}.<br>
   * <b>Efficiency note</b>: This method creates and returns a new
   * <code>Nonterminal</code> object with every invocation.
   *
   * @param label a (possibly complex) nonterminal label from a Treebank
   * @return a <code>Nonterminal</code> object representing any and
   * all components of the specified complex nonterminal
   * @see Nonterminal
   */
  public Nonterminal parseNonterminal(Symbol label) {
    Nonterminal n = new Nonterminal();
    parseNonterminal(label, n);
    return n;
  }

  /**
   * Identical to {@link #parseNonterminal(Symbol)}, except that instead of
   * returning a newly-created <code>Nonterminal</code> object, this
   * method merely modifies the specified <code>Nonterminal</code> object.
   * This method may be used for efficiency: in a particular, sequential
   * training run, only one <code>Nonterminal</code> need be created,
   * repeatedly passed in to this method for modification.
   *
   * @param label a (possibly complex) nonterminal label from a Treebank
   * @param nonterminal the representation of any and all components present
   * in <code>label</code>
   */
  public abstract Nonterminal parseNonterminal(Symbol label,
					       Nonterminal nonterminal);


  /**
   * Fills in the specified <code>Nonterminal</code> object to represent
   * all the components of a complex nonterminal annotation: the base label,
   * any augmentations and any index.  If there are no augmentations, the
   * <code>augmentations</code> field of the returned object will contain a
   * list with no elements; if there is no index, the value of index will be
   * -1.  Augmentation delimiters are the characters in the string returned by
   * {@link #augmentationDelimiters}.<br><b>N.B.</b>: This method assumes that
   * the index, if one exists for the specified nonterminal, will always be the
   * final augmentation in the label.<br>This method is intended to be used by
   * implementations of {@link #parseNonterminal(Symbol,Nonterminal)}.
   *
   * @param label a (possibly complex) nonterminal label from a Treebank
   * @see Nonterminal
   */
  public void defaultParseNonterminal(Symbol label, Nonterminal nonterminal) {
    String augmentationDelimStr = augmentationDelimiters();

    String labelStr = label.toString();

    nonterminal.base = null;
    nonterminal.augmentations.clear();
    nonterminal.index = -1;

    for (int i = 0; i < nonterminalExceptionSet.length; i++) {
      Symbol exception = nonterminalExceptionSet[i];
      if (labelStr.startsWith(exception.toString())) {
	nonterminal.base = exception;
	labelStr = labelStr.substring(exception.toString().length());
	if (labelStr.equals(""))
	  return;
	break;
      }
    }

    // for efficiency, if we can't find an augmentation delimiter, then
    // just set the base and return
    int labelStrLen = labelStr.length();
    int lastAugDelimIdx = -1;
    for (int idx = labelStrLen - 1; idx >= 0; idx--) {
      if (augmentationDelimSet.get(labelStr.charAt(idx))) {
	lastAugDelimIdx = idx;
	break;
      }
    }
    if (lastAugDelimIdx == -1) {
      nonterminal.base = label;
      return;
    }

    // at least one augmentation delimiter exists in labelStr, so use
    // freshly-constructed StringTokenizer to grab all remaining tokens
    boolean delimsAreTokens = true;
    StringTokenizer st = new StringTokenizer(labelStr,
					     augmentationDelimStr,
					     delimsAreTokens);
    int numTokens = st.countTokens();

    if (numTokens > 0) {
      // take care of the base, if we haven't already
      if (nonterminal.base == null) {
	nonterminal.base = Symbol.add(st.nextToken());
	numTokens--;
      }
      // take care of any remaining augmentations
      nonterminal.augmentations.ensureCapacity(numTokens);
      for (int i = 0; i < numTokens; i++)
	nonterminal.augmentations.add(Symbol.add(st.nextToken()));
    }

    // finally, take care of index
    if (nonterminal.augmentations.size() > 0) {
      String lastAugStr = nonterminal.augmentations.last().symbol().toString();
      if (Text.isAllDigits(lastAugStr)) {
	nonterminal.index = Integer.parseInt(lastAugStr);
	int lastAugIdx = nonterminal.augmentations.size() - 1;
	nonterminal.augmentations.remove(lastAugIdx);
      }
    }
  }

  /**
   * Provides an efficient, thread-safe method for testing whether the
    * specified nonterminal contains the specified augmentation (without
   * parsing the nonterminal).
   * <p>
   * <b>N.B.</b>: This method assumes that the augmentation is preceded
   * by the canonical augmentation delimiter.  To search for an augmentation
   * preceded by <i>any</i> of the possible augmentaion delimiters (as defined
   * by {@link #augmentationDelimiters}), use
   * <pre>
   * parseNonterminal(nonterminal).augmentations.contains(augmentation)
   * </pre>
   */
  public boolean containsAugmentation(Symbol nonterminal,
				      Symbol augmentation) {
    if (!(nonterminal instanceof StringSymbol))
	return false;
    String augStr = augmentation.toString();
    StringBuffer sb = new StringBuffer(augStr.length() + 1);
    sb.append(canonicalAugDelimiter()).append(augStr);
    return (nonterminal.toString().indexOf(sb.toString(), 1) != -1);
  }

  /**
   * Adds the specified augmentation to the end of the (possibly empty)
   * augmentation list of the specified <code>Nonterminal</code> object.
   * This method takes care to add the canonical augmentation delimiter
   * before adding the augmentation itself, and also takes care to add
   * these two elements before a final delimiter between the main augmentations
   * and the index, if one exists.
   *
   * @param nonterminal the nonterminal to which to add an augmentation
   * @param augmentation the augmentation to add to <code>nonterminal</code>'s
   * augmentation list
   */
  public void addAugmentation(Nonterminal nonterminal,
			      Symbol augmentation) {
    SexpList augmentations = nonterminal.augmentations;
    int addIdx = augmentations.size();
    if (addIdx > 0) {
      if (isAugDelim(nonterminal.augmentations.last()))
	addIdx--;
    }
    augmentations.add(addIdx++, canonicalAugDelimSym);
    augmentations.add(addIdx, augmentation);
  }

  /**
   * Removes the specified augmentation from the augmentation list of the
   * specified <code>Nonterminal</code> object, and the previous augmentation
   * delimiter.  If the specified augmentation is <i>not</i> preceded by
   * an augmentation delimiter, meaning it is the base label itself, then it
   * is not removed.
   *
   * @param nonterminal the nonterminal from which to remove an augmentation
   * @param augmentation the augmentation to remove from
   * <code>nonterminal</code>
   * @return <code>true</code> if <code>augmentation</code> and
   * a preceding augmentation delimiter was removed from
   * <code>nonterminal</code>'s augmentation list, or <code>false</code>
   * otherwise
   */
  public boolean removeAugmentation(Nonterminal nonterminal,
				    Symbol augmentation) {
    SexpList augmentations = nonterminal.augmentations;
    int augIdx = augmentations.indexOf(augmentation);
    if (augIdx > 0 &&
	isAugDelim(augmentations.get(augIdx - 1))) {
      augmentations.remove(augIdx--);
      augmentations.remove(augIdx);
      return true;
    }
    return false;
  }

  public Sexp removeAugmentation(Sexp sexp,
				 Nonterminal nonterminal,
				 Symbol augmentation) {
    if (sexp.isList())
      return listRemoveAugmentation(sexp.list(), nonterminal, augmentation);
    else
      return symRemoveAugmentation(sexp.symbol(), nonterminal, augmentation);
  }

  private final Symbol symRemoveAugmentation(Symbol label,
					     Nonterminal nonterminal,
					     Symbol augmentation) {
    if (label.toString().indexOf(augmentation.toString()) != -1) {
      parseNonterminal(label, nonterminal);
      removeAugmentation(nonterminal, augmentation);
      return Symbol.add(nonterminal.toString());
    }
    else
      return label;
  }

  private final SexpList listRemoveAugmentation(SexpList list,
						Nonterminal nonterminal,
						Symbol augmentation) {
    int listLen = list.length();
    for (int i = 0; i < listLen; i++)
      list.set(i, removeAugmentation(list.get(i), nonterminal, augmentation));
    return list;
  }

  public final boolean isAugDelim(Sexp sexp) {
    String symStr = sexp.toString();
    return (symStr.length() == 1 && augmentationDelimSet.get(symStr.charAt(0)));
  }
}
