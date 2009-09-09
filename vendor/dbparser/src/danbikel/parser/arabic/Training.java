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
    package danbikel.parser.arabic;

import danbikel.parser.Constants;
import danbikel.parser.Language;
import danbikel.parser.Settings;
import danbikel.parser.Nonterminal;
import danbikel.parser.Word;
import danbikel.parser.Words;
import danbikel.util.*;
import danbikel.lisp.*;
import java.util.*;
import java.io.*;

/**
 * Provides methods for language-specific processing of training parse trees.
 * Even though this subclass of {@link danbikel.parser.Training} is
 * in the default English language package, its primary purpose is simply
 * to fill in the {@link #argContexts}, {@link #semTagArgStopSet} and
 * {@link #nodesToPrune} data members using a metadata resource.  If this
 * capability is desired in another language package, this class may be
 * subclassed.
 * <p>
 * This class also re-defined the method
 * {@link danbikel.parser.lang.AbstractTraining#hasPossessiveChild(Sexp)}.
 */
public class Training extends danbikel.parser.lang.AbstractTraining {
  // constants
  private final static String className = Training.class.getName();
  private final static Symbol argContextsSym = Symbol.add("arg-contexts");
  private final static Symbol semTagArgStopListSym =
    Symbol.add("sem-tag-arg-stop-list");
  private final static Symbol nodesToPruneSym = Symbol.add("prune-nodes");
  /**
   * The symbol associated with tag map metadata.
   */
  protected final static Symbol tagMapSym = Symbol.add("tag-map");
  private final static Symbol VP = Symbol.get("VP");
  private final static Nonterminal vpNt =
    Language.treebank().parseNonterminal(VP);
  private final static Symbol X = Symbol.get("X");
  private final static Symbol punc = Symbol.get("PUNC");
  private final static Symbol nonAlpha = Symbol.get("NON_ALPHABETIC");
  private final static Symbol nonAlphaPunc =
    Symbol.get("NON_ALPHABETIC_PUNCTUATION");
  private final static Symbol period = Symbol.get(".");
  private final static Symbol quotePeriod = Symbol.get("\".");
  private final static Symbol comma = Symbol.get(",");
  private final static Nonterminal subjectNt =
    Language.treebank().parseNonterminal(Symbol.get("*-SBJ"));
  private final static Symbol vpSbj = Symbol.get("VP-SBJ");

  private final static boolean removeNounSuffix = false;
  private final static boolean removeDetPrefix  = false;
  private final static boolean removePerson     = true;
  private final static boolean removeNumber     = false;
  private final static boolean removeGender     = true;
  private final static boolean removeCase       = false;
  private final static boolean removeDefinite   = true;
  private final static boolean removePronoun    = true;
  private final static boolean removeMood       = true;

  /**
   * An array of noun markers in Arabic Treebank part-of-speech tags.
   */
  protected final static String[] nounSuffixMarkers = {"+NSUFF"};
  /**
   * An array of determiner markers in Arabic Treebank part-of-speech tags.
   */
  protected final static String[] detPrefixMarkers = {"DET+"};

  /**
   * An array of person/number markers (indicating information such as
   * &ldquo;first person singular&rdquo;) in Arabic Treebank part-of-speech
   * tags.
   */
  protected final static String[] personMarkers = {
    "_1P", "_1S",
    "_2FS", "_2FP", "_2MS", "_2MP",
    "_3D", "_3FS", "_3FP", "_3MS", "_3MP",
    ":1P", ":1S",
    ":2FS", ":2FP", ":2MS", ":2MP",
    ":3D", ":3FS", ":3FP", ":3MS", ":3MP"
  };
  /**
   * An array of number markers in Arabic Treebank part-of-speech tags
   * (Arabic has forms for singular, plural and dual).
   */
  protected final static String[] numberMarkers = {"_SG", "_PL", "_DUAL","_DU"};
  /** An array of gender markers in Arabic Treebank part-of-speech tags. */
  protected final static String[] genderMarkers = {"_MASC", "_FEM"};
  /** An array of case markers in Arabic Treebank part-of-speech tags. */
  protected final static String[] caseMarkers = {"_NOM", "_ACCGEN", "_ACC"};
  /**
   * An array of definite/indefinite markers in Arabic Treebank part-of-speech
   * tags.
   */
  protected final static String[] definiteMarkers = {"_INDEF", "_DEF"};
  /** An array of pronoun markers in Arabic Treebank part-of-speech tags. */
  protected final static String[] pronounMarkers = {"_POSS", "_INDEF"};
  /** An array of verb mood markers in Arabic Treebank part-of-speech tags. */
  protected final static String[] moodMarkers = {"_MOOD:I", "_MOOD:SJ"};

  // the following two arrays must be coordinated for the contains() method
  // to work properly
  /**
   * An array of the various markers arrays.
   *
   * @see #nounSuffixMarkers
   * @see #detPrefixMarkers
   * @see #personMarkers
   * @see #numberMarkers
   * @see #genderMarkers
   * @see #caseMarkers
   * @see #definiteMarkers
   * @see #pronounMarkers
   * @see #moodMarkers
   */
  protected final static String[][] markers = {
    nounSuffixMarkers,
    detPrefixMarkers,
    personMarkers,
    numberMarkers,
    genderMarkers,
    caseMarkers,
    definiteMarkers,
    pronounMarkers,
    moodMarkers
  };
  /**
   * Indicates which of the various types of markers should be removed
   * from Arabic Treebank part-of-speech tags during preprocessing
   * (currently unused).  This array must be coordinated with {@link #markers}.
   */
  protected final static boolean[] remove = {
    removeNounSuffix,
    removeDetPrefix,
    removePerson,
    removeNumber,
    removeGender,
    removeCase,
    removeDefinite,
    removePronoun,
    removeMood
  };

  /**
   * If regularizeVerbs is <code>true</code>, it indicates that part of speech
   * tags that contain any of the patterns in the {@link #verbPatterns} array
   * should be transformed simply into the pattern itself.  For example, the
   * tag <tt>IV2D+VERB_IMPERFECT+IVSUFF_SUBJ:D_MOOD:SJ</tt> would be
   * transformed into, simply, <tt>VERB_IMPERFECT</tt>.
   */
  protected final static boolean regularizeVerbs = true;
  /**
   * The match patterns used when {@link #regularizeVerbs} is
   * <code>true</code>.
   */
  protected final static String[] verbPatterns = {
    "PASSIVE_VERB", "VERB_IMPERFECT", "VERB_PASSIVE", "VERB_PERFECT"
  };

  // data members
  /** Data member returned by the accessor method of the same name. */
  private Symbol startSym = Symbol.add("*START*");
  /** Data member returned by the accessor method of the same name. */
  private Symbol stopSym = Symbol.add("*STOP*");
  /** Data member returned by the accessor method of the same name. */
  private Word startWord = Words.get(startSym, startSym);
  /** Data member returned by the accessor method of the same name. */
  private Word stopWord = Words.get(stopSym, stopSym);
  /** Data member returned by the accessor method of the same name. */
  private Symbol topSym = Symbol.add("*TOP*");
  /** Data member returned by the accessor method of the same name. */
  private Word topWord = Words.get(topSym, topSym);
  private Nonterminal nonterminal = new Nonterminal();
  private danbikel.util.HashMap transformations = new danbikel.util.HashMap();

  /**
   * The default constructor, to be invoked by {@link danbikel.parser.Language}.
   * This constructor looks for a resource named by the property
   * <code>metadataPropertyPrefix + language</code>
   * where <code>metadataPropertyPrefix</code> is the value of
   * the constant {@link #metadataPropertyPrefix} and <code>language</code>
   * is the value of <code>Settings.get(Settings.language)</code>.
   * For example, the property for English is
   * <code>&quot;parser.training.metadata.english&quot;</code>.
   */
  public Training() throws FileNotFoundException, IOException {
    startSym = Symbol.add("*START*");
    stopSym = Symbol.add("*STOP*");
    startWord = Words.get(startSym, startSym);
    stopWord = Words.get(stopSym, stopSym);
    topSym = Symbol.add("*TOP*");
    topWord = Words.get(topSym, topSym);

    String language = Settings.get(Settings.language);
    String metadataResource = Settings.get(metadataPropertyPrefix + language);
    InputStream is = Settings.getFileOrResourceAsStream(this.getClass(),
							metadataResource);
    int bufSize = Constants.defaultFileBufsize;
    SexpTokenizer metadataTok =
      new SexpTokenizer(is, Language.encoding(), bufSize);
    readMetadata(metadataTok);
  }

  /**
   * Reads the tag map metadata if the specified data type is equal to
   * {@link #tagMapSym}.
   *
   * @param dataType the data type of the specified metadata resource; if
   * the specified symbol is equal to {@link #tagMapSym} then this method
   * will read and store the associated tag map metadata
   * @param metadataLen the length of the metadata list
   * @param metadata the metadata resource
   */
  protected void readMetadataHook(Symbol dataType,
				  int metadataLen, SexpList metadata) {
    if (dataType == tagMapSym) {
      for (int i = 1; i < metadataLen; i++) {
	SexpList mapping = metadata.get(i).list();
	TagMap.add(mapping.symbolAt(0), mapping.symbolAt(1));
      }
    }
  }

  // re-defining the accessor methods for the changed start, stop and top
  // data members (and their associated Word objects)
  /**
   * Returns the symbol to indicate hidden nonterminals that precede the first
   * in a sequence of modifier nonterminals. This method overrides the default
   * implementation so as to return a symbol that does not contain a plus sign
   * (<tt>+</tt>), which is a nonterminal augmentation delimiter in the Arabic
   * Treebank.
   *
   * @return the symbol to indicate hidden nonterminals that precede the first
   *         in a sequence of modifier nonterminals.
   */
  public Symbol startSym() { return startSym; }
  /**
   * Returns the <code>Word</code> object that represents the hidden "head word"
   * of the start symbol.  This method overrides the default implementation so
   * as to return a {@link Word} containing symbols that do not contain a plus
   * sign (<tt>+</tt>), which is a nonterminal augmentation delimiter in the
   * Arabic Treebank.
   *
   * @see #startSym
   * @see danbikel.parser.Trainer
   */
  public Word startWord() { return startWord; }
  /**
   * Returns the symbol to indicate a hidden nonterminal that follows the last
   * in a sequence of modifier nonterminals. This method overrides the default
   * implementation so as to return a symbol that does not contain a plus sign
   * (<tt>+</tt>), which is a nonterminal augmentation delimiter in the Arabic
   * Treebank.
   * <p/>
   * This symbol may also be used as a special value that is guaranteed not
   * to conflict with any nonterminal in a given language's treebank.
   * <p/>
   *
   * @see danbikel.parser.Trainer
   */
  public Symbol stopSym() { return stopSym; }
  /**
   * Returns the <code>Word</code> object that represents the hidden "head word"
   * of the stop symbol.  This method overrides the default implementation so as
   * to return a {@link Word} containing symbols that do not contain a plus
   * sign (<tt>+</tt>), which is a nonterminal augmentation delimiter in the
   * Arabic Treebank.
   *
   * @see #stopSym
   * @see danbikel.parser.Trainer
   */
  public Word stopWord() { return stopWord; }
  /**
   * Returns the symbol to indicate the hidden root of all parse trees. This
   * method overrides the default implementation so as to return a symbol that
   * does not contain a plus sign (<tt>+</tt>), which is a nonterminal
   * augmentation delimiter in the Arabic Treebank.
   *
   * @see danbikel.parser.Trainer
   */
  public Symbol topSym() { return topSym; }
  /**
   * Returns the <code>Word</code> object that represents the hidden "head word"
   * of the hidden root of all parse trees.  This method overrides the default
   * implementation so as to return a {@link Word} containing symbols that do
   * not contain a plus sign (<tt>+</tt>), which is a nonterminal augmentation
   * delimiter in the Arabic Treebank.
   */
  public Word topWord() { return topWord; }


  /**
   * The method to call before counting events in a training parse tree.
   * This overridden implementation executes the following methods of this class
   * in order:
   * <ol>
   * <li> {@link #transformTags(Sexp)}
   * <li> {@link #prune(Sexp)}
   * <li> {@link #addBaseNPs(Sexp)}
   * <li> {@link #removeNullElements(Sexp)}
   * <li> {@link #raisePunctuation(Sexp)}
   * <li> {@link #identifyArguments(Sexp)}
   * <li> {@link #stripAugmentations(Sexp)}
   * </ol>
   * While every attempt has been made to make the implementations of
   * these preprocessing methods independent of one another, the order above is
   * not entirely arbitrary.  In particular:
   * <ul>
   * <li>{@link #raisePunctuation(Sexp)} should be run after
   * {@link #removeNullElements(Sexp)} because a null element that is a
   * leftmost or rightmost child can block detection of a punctuation element
   * that needs to be raised after removal of the null element (if a punctuation
   * element is the next-to-leftmost or next-to-rightmost child of an interior
   * node)
   * <li>{@link #stripAugmentations(Sexp)} should be run after all methods
   * that may depend upon the presence of nonterminal augmentations, such as
   * {@link #identifyArguments(Sexp)}
   * </ul>
   *
   * @param tree the parse tree to pre-process
   * @return <code>tree</code> having been pre-processed
   */
  public Sexp preProcess(Sexp tree) {
    transformTags(tree);
    prune(tree);
    addBaseNPs(tree);
    //repairBaseNPs(tree);
    //addGapInformation(tree);
    //relabelSubjectlessSentences(tree);
    removeNullElements(tree);
    raisePunctuation(tree);
    markSubjectVPs(tree);
    identifyArguments(tree);
    stripAugmentations(tree);
    return tree;
  }

  private void markSubjectVPs(Sexp tree) {
    if (treebank.isPreterminal(tree)) {
      return;
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      Nonterminal label = treebank.parseNonterminal(treeList.symbolAt(0));
      boolean transformed = false;
      for (int i = 1; i < treeListLen; ++i) {
	SexpList child = treeList.listAt(i);
	if (!transformed) {
	  if (vpNt.subsumes(label)) {
	    Nonterminal parsedChildLabel =
	      treebank.parseNonterminal(child.symbolAt(0));
	    if (subjectNt.subsumes(parsedChildLabel)) {
	      label.base = vpSbj;
	      treeList.set(0, label.toSymbol());
	      transformed = true;
	    }
	  }
	}
	markSubjectVPs(child);
      }
    }
  }

  /**
   * An overridden version of
   * {@link danbikel.parser.lang.AbstractTraining#createArgNonterminalsSet()}
   * that adds argument nonterminal patterns, such as <tt>*-SBJ</tt>, to the
   * set of {@linkplain #argNonterminals argument nonterminals}.
   */
  @Override
  protected void createArgNonterminalsSet() {
    argNonterminals = new HashSet();
    Iterator args = argContexts.values().iterator();
    while (args.hasNext()) {
      SexpList argList = (SexpList)args.next();
      Symbol first = argList.symbolAt(0);
      if (first != headSym && first != headPreSym && first != headPostSym) {
        int argListLen = argList.length();
        for (int i = 0; i < argListLen; i++) {
          Symbol argLabel = argList.symbolAt(i);
          Nonterminal parsedArgLabel = treebank.parseNonterminal(argLabel);
	  if (addArgAugmentation(argLabel, parsedArgLabel))
	    argLabel = parsedArgLabel.toSymbol();
	  argNonterminals.add(argLabel);
	}
      }
    }
  }



  /**
   * Preprocesses the specified test sentence and its coordinated list of
   * part-of-speech tags, leaving the original sentence untouched but providing
   * a modified version of the coordinated list of tags, where each tag has been
   * mapped using the value of the original word and the original tag using
   * {@link TagMap#transformTag(Word)}.
   *
   * @param sentence      the list of words, where a known word is a symbol and
   *                      an unknown word is represented by a 3-element list
   *                      (see {@link danbikel.parser.DecoderServerRemote#convertUnknownWords})
   * @param originalWords the list of unprocessed words (all symbols)
   * @param tags          the list of tag lists, where the list at index
   *                      <i>i</i> is the list of possible parts of speech for
   *                      the word at that index
   * @return a two-element list, containing two lists, the first of which is (in
   *         this case) an unprocessed version of <code>sentence</code> and the
   *         second of which is a processed version of <code>tags</code>; if
   *         <code>tags</code> is <code>null</code>, then the returned list will
   *         contain only one element (since <code>SexpList</code> objects are
   *         not designed to handle null elements)
   *
   * @see TagMap#transformTag(Word)
   */
  public SexpList preProcessTest(SexpList sentence,
				 SexpList originalWords, SexpList tags) {
    if (tags == null)
      return super.preProcessTest(sentence, originalWords, tags);
    SexpList processed = new SexpList(2);
    processed.add(sentence);
    int numWords = sentence.size();
    for (int i = 0; i < numWords; i++) {
      Symbol origWord = originalWords.symbolAt(i);

      SexpList currWordTags = tags.listAt(i);
      int numTags = currWordTags.size();

      for (int tagIdx = 0; tagIdx < numTags; tagIdx++) {

	Symbol origTag = currWordTags.symbolAt(tagIdx);
	Word word = danbikel.parser.Words.get(origWord, origTag);

	currWordTags.set(tagIdx, TagMap.transformTag(word));
      }

      tags.set(i, currWordTags);
    }
    processed.add(tags);
    return processed;
  }

  /**
   * If the specified tree has a root label with a print name equal to
   * <tt>&quot;X&quot;</tt>, then this method returns <code>false</code>;
   * otherwise, this method returns the value of the default implementation in
   * the superclass with the specified tree
   * (<code>super.isValidTree(tree)</code>).
   *
   * @param tree the tree to test for validitiy
   * @return <code>false</code> if the specified tree's root label is equal to
   *         <code>Symbol.add(&quot;X&quot;)</code>, or <code>super.isValidTree(tree)</code>
   *         otherwise
   */
  public boolean isValidTree(Sexp tree) {
    // we invalidate top-level "X" sentences, which are not annotated for
    // syntax because they are headers/headlines
    // we also invalidate any top-level "sentences" that are actually just
    // preterminals hanging out in no man's land, such as (-NONE- dummy)
    if (tree.isList()) {
      SexpList treeList = tree.list();
      if ((treeList.get(0).isSymbol() && treeList.symbolAt(0) == X) ||
	  (treeList.length() == 2 && treeList.get(1).isSymbol()))
	return false;
    }
    return super.isValidTree(tree);
  }

  /**
   * Helper method used by {@link TagMap#transformTag(Word)}.
   */
  protected int contains(StringBuffer searchBuf, String[] searchPatterns,
			 IntCounter patternIdx) {
    int numPatterns = searchPatterns.length;
    int idx = -1;
    for (int i = 0; i < numPatterns; i++) {
      idx = searchBuf.indexOf(searchPatterns[i]);
      if (idx != -1) {
	patternIdx.set(i);
	return idx;
      }
    }
    patternIdx.set(-1);
    return -1;
  }

  /**
   * @param word the word whose part-of-speech tag is to be transformed
   * @return a transformed version of the part-of-speech tag contained in the
   *         specified {@link Word} object
   *         <p/>
   *         {@link TagMap#transformTag(Word)}
   *
   * @deprecated This method is the old mechanism by which to transform the
   *             part-of-speech tag associated with an Arabic word; it has been
   *             superseded by the method {@link TagMap#transformTag(Word)}.
   */
  protected Symbol transformTagOld(Word word) {
    // if the tag is a non-alphabetic punctuation and the word itself is
    // either a period or a comma, then the tag should be identical to the word
    if (word.tag() == nonAlpha || word.tag() == nonAlphaPunc) {
      if (word.word() == period ||
	  word.word() == quotePeriod ||
	  word.word() == comma) {
	return word.word();
      }
      else
	return word.tag();
    }
    else {
      // first, check cache of transformed tags
      Map.Entry cacheEntry = transformations.getEntry(word.tag());
      if (cacheEntry != null)
	return (Symbol)cacheEntry.getValue();

      StringBuffer tagBuf = new StringBuffer(word.tag().toString());
      int idx = -1;
      IntCounter patternIdx = new IntCounter(-1);

      if (regularizeVerbs &&
	  (idx = contains(tagBuf, verbPatterns, patternIdx)) != -1) {
	Symbol matchedPattern = Symbol.get(verbPatterns[patternIdx.get()]);
	transformations.put(word.tag(), matchedPattern);
	return matchedPattern;
      }

      for (int i = 0; i < remove.length; i++) {
	if (remove[i]) {
	  idx = contains(tagBuf, markers[i], patternIdx);
	  if (idx != -1) {
	    String matchedPattern = markers[i][patternIdx.get()];
	    tagBuf.delete(idx, idx + matchedPattern.length());
	  }
	}
      }

      Symbol transformedTag = Symbol.get(tagBuf.toString());

      transformations.put(word.tag(), transformedTag);

      return transformedTag;
    }
  }

  /**
   * Does an in-place transformation of the part-of-speech tags in the specified
   * tree.
   *
   * @param tree the tree whose part-of-speech tags are to be mapped
   * @return the specified tree having been modified to contain transformed
   *         part-of-speech tags
   */
  protected Sexp transformTags(Sexp tree) {
    if (Language.treebank().isPreterminal(tree) &&
	!Language.treebank().isNullElementPreterminal(tree)) {
      Word word = Language.treebank().makeWord(tree);
      Symbol newTag = TagMap.transformTag(word);
      tree.list().set(0, newTag);
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++)
	transformTags(treeList.get(i));
    }
    return tree;
  }

  /**
   * We override this method so that it always returns <code>false</code>,
   * so that the default implementation of <code>addBaseNPs(Sexp)<code>
   * never considers an <tt>NP</tt> to be a possessive <tt>NP</tt>.  Thus,
   * the behavior of <code>addBaseNPs</code> is much simpler: all and only
   * <tt>NP</tt>s that do not dominate other NPs will be relabeled
   * <tt>NPB</tt>.
   *
   * @param tree the tree to be tested
   * @return <code>false</code>, regardless of the value of the specified tree
   */
  protected boolean hasPossessiveChild(Sexp tree) {
    return false;
  }

  /**
   * For arabic, we do <i>not</i> want to transform preterminals
   * (parts of speech) to their canonical forms, so this method is overridden.
   *
   * @param tree the tree for which nonterminals, but not parts of speech,
   * are to be transformed into their canonical forms
   */
  protected void canonicalizeNonterminals(Sexp tree) {
    if (Language.treebank().isPreterminal(tree)) {
      return;
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      Symbol currLabel = treeList.symbolAt(0);
      treeList.set(0, Language.treebank().getCanonical(currLabel));
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++)
	canonicalizeNonterminals(treeList.get(i));
    }
  }

  private final static String[] usageMsg = {
    "usage: [-tpnria] [-combine] <filename>\n" +
    "where\n\t" +
    "-t: transform tags\n\t" +
    "-p: prune trees\n\t" +
    "-b: add/relabel base NPs\n\t" +
    "-n: remove null elements\n\t" +
    "-r: raise punctuation\n\t" +
    "-i: identify arguments\n\t" +
    "-a: strip augmentations\n\t" +
    "-combine: by default, the tree will be printed to " +
    "System.out after each\n\t\ttransformation; " +
    "if this flag is present, all transformations will "+
    "be\n\t\tapplied and only the final, fully-" +
    "transformed tree will be printed"
  };

  private static void usage() {
    for (int i = 0; i < usageMsg.length; i++) {
      System.err.println(usageMsg[i]);
    }
    System.exit(1);
  }

  /** Test driver for this class. */
  public static void main(String[] args) {
    String filename = null;
    boolean transformTags = false;
    boolean prune = false;
    boolean addBaseNPs = false;
    boolean removeNullElements = false;
    boolean raisePunc = false;
    boolean idArgs = false;
    boolean stripAug = false;
    boolean combineTransformations = false;

    for (int i = 0; i < args.length; i++) {
      // for each arg that begins with a '-', examine each of its letters
      // and set booleans accordingly, except for "-combine" flag
      if (args[i].charAt(0) == '-') {
	if (args[i].equals("-combine"))
	  combineTransformations = true;
	else {
	  for (int charIdx = 1; charIdx < args[i].length(); charIdx++) {
	    char curr = args[i].charAt(charIdx);
	    switch (curr) {
	    case 't':
	      transformTags = true;
	      break;
	    case 'p':
	      prune = true;
	      break;
	    case 'b':
	      addBaseNPs = true;
	      break;
	    case 'n':
	      removeNullElements = true;
	      break;
	    case 'r':
	      raisePunc = true;
	      break;
	    case 'i':
	      idArgs = true;
	      break;
	    case 'a':
	      stripAug = true;
	      break;
	    default:
	      System.err.println("illegal flag: " + curr);
	      usage();
	    }
	  }
	}
      }
      else
	filename = args[i];
    }

    if (filename == null) {
      usage();
    }

    Training training = (Training)Language.training();
    //training.printMetadata();

    try {
      SexpTokenizer tok = new SexpTokenizer(filename, Language.encoding(),
					    Constants.defaultFileBufsize);
      Sexp curr = null;
      while ((curr = Sexp.read(tok)) != null) {
	if (transformTags) {
	  curr = training.transformTags(curr);
	  if (!combineTransformations)
	    System.out.println(curr);
	}
	if (prune) {
	  curr = training.prune(curr);
	  if (!combineTransformations)
	    System.out.println(curr);
	}
	if (addBaseNPs) {
	  curr =training.addBaseNPs(curr);
	  if (!combineTransformations)
	    System.out.println(curr);
	}
	if (removeNullElements) {
	  curr = training.removeNullElements(curr);
	  if (!combineTransformations)
	    System.out.println(curr);
	}
	if (raisePunc) {
	  curr = training.raisePunctuation(curr);
	  if (!combineTransformations)
	    System.out.println(curr);
	}
	if (idArgs) {
	  curr =training.identifyArguments(curr);
	  if (!combineTransformations)
	    System.out.println(curr);
	}
	if (stripAug) {
	  curr = training.stripAugmentations(curr);
	  if (!combineTransformations)
	    System.out.println(curr);
	}
	if (combineTransformations)
	  System.out.println(curr);
      }
    }
    catch (UnsupportedEncodingException uee) {
      System.err.println(uee);
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(fnfe);
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
  }
}
