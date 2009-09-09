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

import java.util.*;
import danbikel.lisp.*;
import java.io.*;

/**
 * Specifies methods for language-specific preprocessing of training
 * parse trees.  The primary method to be invoked from an implementation
 * of this interface is {@link #preProcess(Sexp)}.  Additionally, as
 * implementations are likely to contain or have access to appropriate
 * preprocessing data and methods, this interface also specifies a crucial
 * method to be used for post-processing, to &quot;undo&quot; what it was
 * done during preprocessing after decoding.  This post-processing method is
 * {@link #postProcess(Sexp)}, and is invoked by default by the
 * <code>Decoder</code>.
 * <p>
 * A language package must include an implementation of this interface.
 *
 * @see #preProcess(Sexp)
 * @see #postProcess(Sexp)
 * @see Decoder
 */
public interface Training {
  /**
   * Indicates to set up a static map for quickly mapping argument nonterminals
   * to their non-argument variants (that is, for quickly stripping away
   * their argument augmentations).
   * <p>
   * <b>N.B.</b>: This method is necessarily thread-safe, as it is expected
   * to be invoked by every {@link Decoder} as it starts up, and since there
   * can be multiple {@link Decoder} instances within a given VM.
   * <b><i>However</i></b>, note that it is <b>inappropriate to invoke this
   * method</b> if the set of nonterminals in the specified counts table
   * is incomplete (see the documentation for the {@link SubcatBag} class
   * for an instance where this will be the case).
   *
   * @param nonterminals a counts table whose keys form a complete set of
   * all possible nonterminal labels, as is obtained from
   * {@link DecoderServerRemote#nonterminals()} (the counts to which the
   * nonterminals are mapped are not used by this method)
   */
  public void setUpFastArgMap(CountsTable nonterminals);

  /**
   * The method to call before counting events in a training parse tree.
   *
   * @param tree the parse tree to pre-process
   * @return <code>tree</code> having been pre-processed
   */
  public Sexp preProcess(Sexp tree);

  /**
   * Invoked by the {@linkplain danbikel.parser.Decoder decoder} as the first
   * step in preprocessing (prior to the invocation of {@link #preProcessTest}).
   * Returns whether the specified word should be removed from the sentence
   * before parsing.
   *
   * @param word a word in the sentence about to parsed
   * @param tag the supplied part-of-speech tag of the specified word,
   * or <tt>null</tt> if tags were not supplied
   * @param idx the index of the specified word in the specified sentence
   * @param sentence a list of {@link Symbol} objects that represent the words
   * of the sentence to be parsed
   * @param tags coordinated list of supplied part-of-speech tag lists for each
   * of the words in the specified sentence, or <tt>null</tt> if no tags
   * were supplied
   * @param originalTags the cached copy of the specified <tt>tags</tt> list,
   * used when {@link Settings#restorePrunedWords} is <tt>true</tt>
   * @param prunedPretermsPosSet the set of part-of-speech tags that were
   * pruned during training
   * @param prunedPretermsPosMap a map of words pruned during training to
   * their part-of-speech tags when they were pruned
   *
   * @return whether the specified word should be removed from the sentence
   * before parsing
   */
  public boolean removeWord(Symbol word, Symbol tag, int idx, SexpList sentence,
			    SexpList tags, SexpList originalTags,
			    Set prunedPretermsPosSet,
			    Map prunedPretermsPosMap);

  /**
   * Preprocesses the specified test sentence and its coordinated list of tags.
   *
   * @param sentence the list of words, where a known word is a symbol and
   * an unknown word is represented by a 3-element list (see
   * {@link DecoderServerRemote#convertUnknownWords})
   * @param originalWords the list of unprocessed words (all symbols)
   * @param tags the list of tag lists, where the list at index <i>i</i>
   * is the list of possible parts of speech for the word at that index
   * @return a two-element list, containing two lists, the first of which
   * is a processed version of <code>sentence</code> and the second of which
   * is a processed version of <code>tags</code>; if <code>tags</code>
   * is <code>null</code>, then the returned list will contain only
   * one element (since <code>SexpList</code> objects are not designed
   * to handle null elements)
   */
  public SexpList preProcessTest(SexpList sentence,
				 SexpList originalWords, SexpList tags);

  /**
   * Returns whether the specified tree is valid.  The particular notion
   * of validity can be language package-dependent.
   *
   * @param tree the parse tree to check for validity
   */
  public boolean isValidTree(Sexp tree);

  /**
   * Returns whether the specified tree is to be skipped when training.
   *
   * @param tree an annotated training tree
   * @return a string if the specified tree is to be skipped
   * when training, <code>null</code> otherwise
   *
   * @see Trainer#train(SexpTokenizer,boolean,boolean)
   */
  public String skip(Sexp tree);

  /**
   * Returns <code>true</code> if the specified label is a node to prune.
   */
  /*
  public boolean isNodeToPrune(Symbol label) {
    return nodesToPrune.contains(label);
  }
  */

  /**
   * Returns the set of pruned preterminals (<code>Sexp</code> objects).
   *
   * @see #prune(Sexp)
   */
  public Set getPrunedPreterms();

  /**
   * Prunes away subtrees that have a root that is an element of
   * <code>nodesToPrune</code>.
   * <p>
   * <b>Side effect</b>: An internal set of pruned preterminals will
   * be updated.  This set may be accessed via {@link #getPrunedPreterms()}.
   * <p>
   * <b>Bugs</b>: Cannot prune away entire tree if the root label of the
   * specified tree is in <code>nodesToPrune</code>.
   * <p>
   *
   * @param tree the parse tree to prune
   * @return <code>tree</code> having been pruned
   */
  public Sexp prune(Sexp tree);

  /**
   * Augments labels of nonterminals that are arguments.  This method is
   * optional, and may be overridden to simply return <code>tree</code>
   * untouched if argument identification is not desired for a particular
   * language package.
   *
   * @param tree the parse tree to modify
   * @return a reference to the modified <code>tree</code> object
   * @see Treebank#canonicalAugDelimiter
   */
  public Sexp identifyArguments(Sexp tree);

  /**
   * The symbol that is used to mark argument (required) nonterminals by
   * {@link #identifyArguments(Sexp)}.
   */
  public Symbol defaultArgAugmentation();

  /**
   * Returns <code>true</code> if and only if <code>label</code> has an
   * argument augmentation as added by {@link #identifyArguments(Sexp)}.
   */
  public boolean isArgument(Symbol label);

  /**
   * Returns <code>true</code> if and only if the specified nonterminal
   * label has an argument augmentation preceded by the canonical
   * augmentaion delimiter.  Unlike {@link #isArgument(Symbol)}, this
   * method is thread-safe.  Also, it is more efficient than
   * {@link #isArgument(Symbol)}, as it does not actually parse the
   * specified nonterminal label.
   */
  public boolean isArgumentFast(Symbol label);

  /**
   * Returns the canonical version of the specified argument nonterminal,
   * crucially including its argument augmentation.  For example,
   * in the English Penn Treebank, the canonical version of <tt>NP-CLR-A</tt>
   * would typically be <tt>NP-A</tt>, where <tt>A</tt> is the argument
   * augmentation.
   *
   * @param argLabel the argument nonterminal to be canonicalized
   * @return the canonical version of the specified argument nonterminal
   */
  public Symbol getCanonicalArg(Symbol argLabel);

  /**
   * Augments nonterminals to include gap information for WHNP's that have
   * moved and leave traces (gaps), as in the GPSG framework.  This method is
   * optional, and may simply return <code>tree</code> untouched if gap
   * information is desired for a particular language package.
   *
   * @param tree the parse tree to which to add gapping
   * @return the same <code>tree</code> that was passed in, with certain
   * nodes modified to include gap information
   */
  public Sexp addGapInformation(Sexp tree);

  /**
   * Returns <code>true</code> if and only if <code>label</code> has a
   * gap augmentation as added by {@link #addGapInformation(Sexp)}.
   */
  public boolean hasGap(Symbol label);

  /**
   * The symbol that will be used to identify nonterminals whose subtrees
   * contain a gap (a trace).  This method is used by {@link
   * #stripAugmentations(Sexp)}, so that gap augmentations that are added by
   * {@link #addGapInformation(Sexp)} do not get removed.
   */
  public Symbol gapAugmentation();

  /**
   * The symbol that gets reassigned as the part of speech for null
   * preterminals that represent traces that have undergone WH-movement, as
   * relabeled by the default implementation of {@link
   * #addGapInformation(Sexp)}.
   */
  public Symbol traceTag();

  /**
   * Relabels sentences that have no subjects with the nonterminal label
   * returned by {@link Treebank#subjectlessSentenceLabel}.  This method is
   * optional, and may be overridden to simply return <code>tree</code>
   * untouched if subjectless sentence relabeling is not desired for a
   * particular language package.
   *
   * @param tree the parse tree in which to relabel subjectless sentences
   * @return the same <code>tree</code> that was passed in, with
   * subjectless sentence nodes relabeled
   * @see Treebank#isSentence(Symbol)
   * @see Treebank#subjectAugmentation
   * @see Treebank#isNullElementPreterminal(Sexp)
   * @see Treebank#subjectlessSentenceLabel
   */
  public Sexp relabelSubjectlessSentences(Sexp tree);

  /**
   * Strips any augmentations off all of the nonterminal labels of
   * <code>tree</code>.  The set of nonterminal labels does <i>not</i> include
   * preterminals, which are typically parts of speech.  If a particular
   * language's Treebank augments preterminals, this method should be
   * overridden in a language package's subclass. The only augmentations that
   * will not be removed are those that are added by {@link
   * #identifyArguments(Sexp)}, so as to preserve the transformations of that
   * method.  This method should only be called subsequent to the invocations
   * of methods that require augmentations, such as {@link
   * #relabelSubjectlessSentences(Sexp)}.
   *
   * @param tree the tree all of the nonterminals of which are to be stripped
   * of all augmentations except those added by <code>identifyArguments</code>
   * @return a reference to <code>tree</code>
   */
  public Sexp stripAugmentations(Sexp tree);

  /**
   * Raises punctuation to the highest possible point in a parse tree,
   * resulting in a tree where no punctuation is the first or last child of a
   * non-leaf node.  One consequence is that all punctuation is removed from
   * the beginning and end of the sentence.  The punctuation affected is
   * defined by the implementation of the method {@link
   * Treebank#isPuncToRaise(Sexp)}.
   * <p>
   * <b>Side effect</b>: All preterminals removed from the beginning and end
   * of the sentence are stored in an internal set, which can be accessed
   * via {@link #getPrunedPunctuation()}.
   * <p>
   * Example of punctuation raising:
   * <pre>
   * (S (NP
   *      (NPB Pierre Vinken)
   *      (, ,)
   *      (ADJP 61 years old)
   *      (, ,))
   *    (VP joined (NP (NPB the board))) (. .))
   * </pre>
   * becomes
   * <pre>
   * (S (NP
   *      (NPB Pierre Vinken)
   *      (, ,)
   *      (ADJP 61 years old))
   *    (, ,)
   *    (VP joined (NP (NPB the board))))
   * </pre>
   * This method appropriately deals with the case of having multiple
   * punctuation elements to be raised on the left or right side of the list of
   * children for a nonterminal.  For example, in English, if this method
   * were passed the tree
   * <pre>
   * (S
   *   (NP (DT The) (NN dog) (, ,) (NNP Barky) (. .) (. .) (. .))
   *   (VP (VB was) (ADJP (JJ stupid)))
   *   (. .) (. .) (. .))
   * </pre>
   * the result would be
   * <pre>
   * (S
   *   (NP (DT The) (NN dog) (, ,) (NNP Barky))
   *   (. .) (. .) (. .)
   *   (VP (VB was) (ADJP (JJ stupid))))
   * </pre>
   * <p>
   * <b>Bugs</b>: In the pathological case where all the children of a node
   * are punctuation to raise, this method simply emits a warning to
   * <code>System.err</code> and does not attempt to raise them (which would
   * cause an interior node to become a leaf).
   * <p>
   * @param tree the parse tree to destructively modify by raising punctuation
   * @return a reference to the modified <code>tree</code> object
   */
  public Sexp raisePunctuation(Sexp tree);

  /**
   * Returns the set of preterminals (<code>Sexp</code> objects) that were
   * punctuation elements that were "raised away" because they were either at
   * the beginning or end of a sentence.
   *
   * @see #raisePunctuation(Sexp)
   */
  public Set getPrunedPunctuation();

  /**
   * Adds and/or relabels base NPs in the specified tree.
   *
   * @param tree the parse tree in which to add and/or relabel base NPs
   * @return a reference to the modified version of <code>tree</code>
   *
   * @see Treebank#isNP(Symbol)
   * @see Treebank#baseNPLabel
   * @see Treebank#NPLabel
   */
  public Sexp addBaseNPs(Sexp tree);

  /**
   * Changes the specified tree so that when the last child of an
   * NPB is an S, the S gets raised to be a sibling immediately following
   * the NPB.  That is, situations such as
   * <pre>
   * (NP
   *   (NPB
   *     (DT an)
   *     (NN effort)
   *     (S ...)))
   * </pre>
   * get transformed to
   * <pre>
   * (NP
   *   (NPB
   *     (DT an)
   *     (NN effort))
   *   (S ...))
   * </pre>
   */
  public Sexp repairBaseNPs(Sexp tree);


  /**
   * Removes all null elements, that is, those nodes of <code>tree</code> for
   * which {@link Treebank#isNullElementPreterminal(Sexp)} returns
   * <code>true</code>.  Additionally, if the removal of a null element leaves
   * an interior node that is childless, then this interior node is removed as
   * well.  For example, if we have the following sentence in English
   * <pre> (S (NP-SBJ (-NONE- *T*)) (VP ...)) </pre>
   * it will be transformed to be
   * <pre> (S (VP ...)) </pre>
   * <b>N.B.</b>: This method should only be invoked <i>after</i> preprocessing
   * with {@link #relabelSubjectlessSentences(Sexp)} and {@link
   * #addGapInformation(Sexp)}, as these methods (and possibly others, if
   * overridden) rely on the presence of null elements.
   *
   * @see Treebank#isNullElementPreterminal(Sexp)
   */
  public Sexp removeNullElements(Sexp tree);

  // some data accessor methods

  /**
   * Returns the symbol to indicate hidden nonterminals that precede the first
   * in a sequence of modifier nonterminals.
   *
   * @see Trainer
   */
  public Symbol startSym();
  /**
   * Returns the <code>Word</code> object that represents the hidden "head
   * word" of the start symbol.
   *
   * @see #startSym
   * @see Trainer
   */
  public Word startWord();

  /**
   * Returns the symbol to indicate a hidden nonterminal that follows the last
   * in a sequence of modifier nonterminals.
   * <p>
   * This symbol may also be used as a special value that is guaranteed not
   * to conflict with any nonterminal in a given language's treebank.
   * <p>
   *
   * @see Trainer
   */
  public Symbol stopSym();

  /**
   * Returns the <code>Word</code> object that represents the hidden "head
   * word" of the stop symbol.
   *
   * @see #stopSym
   * @see Trainer
   */
  public Word stopWord();

  /**
   * Returns the symbol to indicate the hidden root of all parse trees.
   *
   * @see Trainer
   */
  public Symbol topSym();

  /**
   * Returns the <code>Word</code> object that represents the hidden "head
   * word" of the hidden root of all parse trees.
   */
  public Word topWord();

  /**
   * Returns a static set of possible argument nonterminals.
   *
   * @return a static set of possible argument nonterminals
   */
  public Set argNonterminals();

  // a couple of utility methods for removing gap/arg augmentations
  // very efficiently

  /**
   * Removes any argument augmentations from the specified nonterminal label.
   * @param label the label whose argument augmentations are to be removed
   * @return a new label with no argument augmentations
   */
  public Symbol removeArgAugmentation(Symbol label);

  /**
   * If the specified S-expression is a list, this method modifies the
   * list to contain only symbols without gap augmentations;
   * otherwise, this method removes the gap augmentation (if one exists)
   * in the specified symbol and returns that new symbol.
   *
   * @param sexp a symbol or list of symbols from which to remvoe any
   * gap augmentations
   * @return a symbol or list of symbols with no gap augmentations
   */
  public Sexp removeGapAugmentation(Sexp sexp);

  /**
   * Post-processes a parse tree after decoding, eseentially undoing
   * the steps performed in {@linkplain #preProcess(Sexp) preprocessing}.
   * @param tree the tree to be post-processed
   */
  public void postProcess(Sexp tree);
}
