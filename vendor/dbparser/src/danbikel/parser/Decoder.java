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
    package  danbikel.parser;

import java.util.HashMap;
import danbikel.util.*;
import danbikel.lisp.*;
import danbikel.parser.constraints.*;
import danbikel.parser.util.Util;

import java.io.*;
import java.util.*;
import java.rmi.*;

/**
 * Provides the methods necessary to perform CKY parsing on input sentences.
 */
public class Decoder implements Serializable, Settings.Change {

  // inner class for decoding timeouts
  /**
   * Exception to be thrown when the maximum parse time has been reached.
   *
   * @see Settings#maxParseTime
   */
  protected static class TimeoutException extends Exception {
    /**
     * Constructs a new timeout exception with no message.
     */
    TimeoutException() {
      super();
    }
    /**
     * Constructs a new timeout exception with the specified message.
     * @param s the message for this timeout exception
     */
    TimeoutException(String s) {
      super(s);
    }
  }

  // debugging constants
  // debugging code will be optimized away when the following booleans are false
  private final static boolean debug = false;
  private final static boolean debugConvertSubcatMaps = false;
  private final static boolean debugConvertHeadMap = false;
  private final static boolean debugPrunedPretermsPosMap = false;
  private final static boolean debugPrunedPunctuationPosMap = false;
  private final static boolean debugSentenceSize = true;
  private final static boolean debugMaxParseTime = true;
  private final static boolean debugSpans = false;
  private final static boolean debugInit = false;
  private final static boolean debugTop = false;
  private final static boolean debugComplete = false;
  private final static boolean debugJoin = false;
  private final static boolean debugStops = false;
  private final static boolean debugUnaries = false;
  private final static boolean debugUnariesAndStopProbs = false;
  private final static boolean debugConstraints = false;
  private final static boolean debugAnalyzeChart = false;
  private final static String debugGoldFilenameProperty =
    "parser.debug.goldFilename";
  private final static boolean debugAnalyzeBestDerivation = false;
  private final static String debugOutputChartProperty =
    "parser.debug.outputChart";
  private static boolean debugOutputChart =
    Settings.getBoolean(debugOutputChartProperty);
  private final static String debugChartFilenamePrefix = "chart";
  private final static boolean debugCommaConstraint = false;
  private final static boolean debugRemoveWord = false;
  private final static boolean debugRestorePrunedWords = false;
  private final static boolean debugBeamWidening = true;
  /**
   * This debugging option should be used only when the property
   * <tt>parser.model.precomputeProbabilities</tt> was <tt>false</tt>
   * during training (and should therefore be <tt>false</tt> during
   * decoding as well).  This is the most verbose of the debugging
   * options, so expect an output file on the order of tens of
   * megabytes, if not larger.
   */
  private final static boolean debugOutputAllCounts = false;
  private final static Symbol S = Symbol.add("S");
  private final static Symbol SA = Symbol.add("S-A");
  private final static Symbol SINV = Symbol.add("SINV");
  private final static Symbol PRN = Symbol.add("PRN");
  private final static Symbol RRB = Symbol.add("-RRB-");
  private final static Symbol NP = Symbol.add("NP");
  private final static Symbol NPB = Symbol.add("NPB");
  private final static Symbol NPA = Symbol.add("NP-A");
  private final static Symbol RRC = Symbol.add("RRC");
  private final static Symbol VP = Symbol.add("VP");
  private final static Symbol VBP = Symbol.add("VBP");
  private final static Symbol CC = Symbol.add("CC");
  private final static Symbol comma = Symbol.add(",");
  private final static Symbol FRAG = Symbol.add("FRAG");
  private final static Symbol willSym = Symbol.add("will");
  private final static Symbol mdSym = Symbol.add("MD");
  private final static Symbol PP = Symbol.add("PP");
  private final static Symbol WHADVP = Symbol.add("WHADVP");
  private final static Symbol WHNP = Symbol.add("WHNP");

  // constants
  private final static String className = Decoder.class.getName();
  /** The value of {@link Constants#LEFT} cached for better readability. */
  protected final static boolean LEFT = Constants.LEFT;
  /** The value of {@link Constants#RIGHT} cached for better readability. */
  protected final static boolean RIGHT = Constants.RIGHT;
  // cache some constants from Constants class, for more readable code
  /** The value of {@link Constants#logOfZero} cached for readability. */
  protected final static double logOfZero = Constants.logOfZero;
  /** The value of {@link Constants#logProbCertain} cached for readability. */
  protected final static double logProbCertain = Constants.logProbCertain;
  /**
   * An array of {@link Subcat} of length zero.
   *
   * @see #getPossibleSubcats(java.util.Map,HeadEvent,ProbabilityStructure,int)
   */
  protected final static Subcat[] zeroSubcatArr = new Subcat[0];

  /**
   * A writer wrapped around {@link System#err} for error messages that might
   * contain encoding-specific characters.  The encoding of the writer
   * is {@link Language#encoding()}.
   */
  protected static PrintWriter err = newErrStream();

  private static PrintWriter newErrStream() {
    Writer osw = null;
    try {
      osw = new OutputStreamWriter(System.err, Language.encoding());
    }
    catch (UnsupportedEncodingException uee) {
      System.err.println(className +
                         ": couldn't create error writer with encoding " +
                         Language.encoding() +
                         "; using default encoding instead");
      osw = new OutputStreamWriter(System.err);
    }
    return new PrintWriter(osw, true);
  }

  static {
    Settings.register(Decoder.class,
		      new Settings.Change() {
			public void update(Map<String,String> changedSettings) {
			  if (changedSettings.containsKey(Settings.language)) {
			    err = newErrStream();
			  }
			}
		      }, Collections.<Class>singleton(Language.class));
  }

  /**
   * A list containing only {@link Training#startSym()}, which is the
   * type of list that should be used when there are zero real previous
   * modifiers (to start the Markov modifier process).
   *
   * @see Trainer#newStartList()
   */
  protected final SexpList startList = Trainer.newStartList();
  /**
   * A list containing only {@link Training#startWord()}, which is the
   * type of list that should be used when there are zero real previous
   * modifiers (to start the Markov modifier process).
   *
   * @see Trainer#newStartWordList()
   */
  protected final WordList startWordList = Trainer.newStartWordList();

  // data members
  /** The id of the parsing client that is using this decoder. */
  protected int id;
  /** The server for this decoder. */
  protected DecoderServerRemote server;
  /** The current sentence index for this decoder (starts at 0). */
  protected int sentenceIdx = -1;
  /** The current sentence. */
  protected SexpList sentence;
  /** The length of the current sentence, cached here for convenience. */
  protected int sentLen;
  /**
   * The maximum length of sentences to be parsed.  All sentences greater
   * than this length will be skipped.
   * @see Settings#maxSentLen
   */
  protected int maxSentLen = Settings.getInteger(Settings.maxSentLen);
  /**
   * The maximum number of top-scoring parses for the various
   * <code>parse</code> methods to return.
   * @see Settings#kBest
   */
  protected int kBest =
    Math.max(1, Integer.parseInt(Settings.get(Settings.kBest)));
  /**
   * The timer (used when Settings.maxParseTime is greater than zero).
   * @see Settings#maxParseTime
   */
  protected int maxParseTime = Settings.getInteger(Settings.maxParseTime);
  /** An object for keeping track of wall-clock time. */
  protected Time time = new Time();
  /** The parsing chart. */
  protected CKYChart chart;
  /** The map from vocabulary items to their possible parts of speech. */
  protected Map posMap;
  /**
   * A cache derived from {@link #posMap} that is a map of (presumably
   * closed-class) parts of speech to random example words observed with
   * the part of speech from which they are mapped.
   */
  protected Map posToExampleWordMap;
  /** The set of possible parts of speech, derived from {@link #posMap}. */
  protected Set posSet;
  /**
   * An array of all nonterminals observed in training, that is initialized
   * and filled in at construction time.
   */
  protected Symbol[] nonterminals;
  /**
   * A map from futures of the last back-off level of the head generation model
   * to possible history contexts.
   */
  protected Map headToParentMap;
  /**
   * A map from contexts of the last back-off level of the left subcat
   * generation model to possible subcats.
   */
  protected Map leftSubcatMap;
  /**
   * A map from contexts of the last back-off level of the right subcat
   * generation model to possible subcats.
   */
  protected Map rightSubcatMap;
  /** The left subcat generation model structure. */
  protected ProbabilityStructure leftSubcatPS;
  /** The last level of back-off in the left subcat generation model
      structure. */
  protected int leftSubcatPSLastLevel;
  /** The right subcat generation model structure. */
  protected ProbabilityStructure rightSubcatPS;
  /** The last level of back-off in the right subcat generation model
      structure. */
  protected int rightSubcatPSLastLevel;
  /**
   * A map from contexts of the last back-off level of the modifying
   * nonterminal generation model to possible modifying nonterminal labels.
   */
  protected Map modNonterminalMap;
  /**
   * A map from unlexicalized parent-head-side triples to all possible
   * partially-lexicalized modifying nonterminals.
   */
  protected Map simpleModNonterminalMap;
  /** The modifying nonterminal generation model structure. */
  protected ProbabilityStructure modNonterminalPS;
  /** The last level of back-off in the modifying nonterminal generation
      model structure. */
  protected int modNonterminalPSLastLevel;
  // these next three data members are used by {@link #preProcess}
  /**
   * A map of each word pruned during training to its set of part-of-speech tags
   * observed with its pruned instances.  This map is used by {@link
   * Training#removeWord Training.removeWord}.
decod   * @see DecoderServerRemote#prunedPreterms()
   */
  protected Map prunedPretermsPosMap;
  /**
   * The set of part-of-speech tags of words pruned during training.  This set
   * is used by {@link Training#removeWord Training.removeWord}.
   * @see DecoderServerRemote#prunedPreterms()
   */
  protected Set prunedPretermsPosSet;
  /**
   * A map of each punctuation word that was pruned during training to the
   * set of its parts of speech observed with the pruned instances.
   * @see DecoderServerRemote#prunedPunctuation()
   */
  protected Map prunedPunctuationPosMap;
  // these next two data members are also kept in CKYChart, but we keep
  // them here as well, for debugging purposes
  /** The cell limit for the parsing chart (stored here for debugging). */
  protected int cellLimit = -1;
  /** The prune factor for the parsing chart (stored here for debugging). */
  protected double pruneFact = 0.0;
  /**
   * The maximum prune factor (for beam-widening).
   *
   * @see Settings#decoderMaxPruneFactor
   */
  protected double maxPruneFact = pruneFact;
  /**
   * The prune factor increment used when doing beam-widening.
   *
   * @see Settings#decoderPruneFactorIncrement
   */
  protected double pruneFactIncrement;
  /**
   * The value of {@link Settings#decoderRelaxConstraintsAfterBeamWidening},
   * cached here for readability and convenience.
   */
  protected boolean relaxConstraints =
    Settings.getBoolean(Settings.decoderRelaxConstraintsAfterBeamWidening);
  /**
   * The boolean to indicate whether to allow probability estimates equal to
   * {@link Constants#logOfZero} and to allow other hard constraints (that
   * amount to implicit log of zero probability estimates). If <tt>false</tt>,
   * all estimates equal to {@link Constants#logOfZero} are modified to be
   * {@link Constants#logProbSmall} and all other hard constraints <b>except the
   * comma-pruning constraint</b> are relaxed. This data member is <tt>true</tt>
   * by default, but is temporarily set to <tt>false</tt> by the decoder when no
   * parse is produced after all beam widening.
   */
  protected boolean hardConstraints = true;
  /** The original sentence, before preprocessing. */
  protected SexpList originalSentence = new SexpList();
  /** The original tag list, before preprocessing. */
  protected SexpList originalTags;
  /** The value of the {@link Settings#restorePrunedWords} setting. */
  protected boolean restorePrunedWords =
    Settings.getBoolean(Settings.restorePrunedWords);
  /**
   * The original sentence, but with word removed to match pre-processing.
   * This will be used to restore the original words after parsing.
   */
  protected SexpList originalWords = new SexpList();
  /** An instance of an empty subcat, for use when constructing lookup events.*/
  protected Subcat emptySubcat = Subcats.get();
  /** The boolean value of the {@link Settings#downcaseWords} setting. */
  protected boolean downcaseWords = Settings.getBoolean(Settings.downcaseWords);
  /** The boolean value of the {@link Settings#useLowFreqTags} setting. */
  protected boolean useLowFreqTags =
    Settings.getBoolean(Settings.useLowFreqTags);
  /**
   * The boolean value of the
   * {@link Settings#decoderSubstituteWordsForClosedClassTags} setting.
   */
  protected boolean substituteWordsForClosedClassTags =
    Settings.getBoolean(Settings.decoderSubstituteWordsForClosedClassTags);
  /**
   * The boolean value of the {@link Settings#decoderUseOnlySuppliedTags}
   * setting.
   */
  protected boolean useOnlySuppliedTags =
    Settings.getBoolean(Settings.decoderUseOnlySuppliedTags);
  /**
   * The boolean value of the {@link Settings#decoderUseHeadToParentMap}
   * setting.
   */
  protected boolean useHeadToParentMap =
    Settings.getBoolean(Settings.decoderUseHeadToParentMap);
  /**
   * The boolean value of the {@link Settings#useSimpleModNonterminalMap}
   * setting.
   */
  protected boolean useSimpleModNonterminalMap =
    Settings.getBoolean(Settings.useSimpleModNonterminalMap);
  /**
   * The value of {@link Training#startSym()}, cached here for efficiency
   * and convenience.
   */
  protected Symbol startSym = Language.training().startSym();
  /**
   * The value of {@link Training#startWord()}, cached here for efficiency
   * and convenience.
   */
  protected Word startWord = Language.training().startWord();
  /**
   * The value of {@link Training#stopSym()}, cached here for efficiency
   * and convenience.
   */
  protected Symbol stopSym = Language.training().stopSym();
  /**
   * The value of {@link Training#stopWord()}, cached here for efficiency
   * and convenience.
   */
  protected Word stopWord = Language.training().stopWord();
  /**
   * The value of {@link Training#topSym()}, cached here for efficiency
   * and convenience.
   */
  protected Symbol topSym = Language.training().topSym();
  /** The value of the setting {@link Settings#numPrevMods}. */
  protected int numPrevMods = Settings.getInteger(Settings.numPrevMods);
  /** The value of the setting {@link Settings#numPrevWords}. */
  protected int numPrevWords = Settings.getInteger(Settings.numPrevWords);
  // data members used by addUnariesAndStopProbs
  /** One of a pair of lists used by {@link #addUnariesAndStopProbs}. */
  protected List prevItemsAdded = new ArrayList();
  /** One of a pair of lists used by {@link #addUnariesAndStopProbs}. */
  protected List currItemsAdded = new ArrayList();
  // data members used by various methods that iterate over chart HashMaps
  /**
   * A temporary storage area used by {@link #addTopUnaries} for storing
   * items to be added to the chart when iterating over a cell in the chart.
   */
  protected List topProbItemsToAdd = new ArrayList();
  /**
   * A temporary storage area used by {@link #addUnaries} for storing
   * items to be added to the chart when iterating over a cell in the chart.
   */
  protected List unaryItemsToAdd = new ArrayList();
  /**
   * A temporary storage area used by {@link #addStopProbs} for storing
   * items to be added to the chart when iterating over a cell in the chart.
   */
  protected List stopProbItemsToAdd = new ArrayList();
  // lookup TrainerEvent objects (created once here, and constantly mutated
  // throughout decoding)
  /** A reusable {@link PriorEvent} object for look-ups in tables. */
  protected PriorEvent lookupPriorEvent = new PriorEvent(null, null);
  /** A reusable {@link HeadEvent} object for look-ups in tables. */
  protected HeadEvent lookupHeadEvent =
    new HeadEvent(null, null, null, emptySubcat, emptySubcat);
  /** A reusable {@link ModifierEvent} object for look-ups in tables. */
  protected ModifierEvent lookupModEvent =
    new ModifierEvent(null, null, null, SexpList.emptyList, null, null, null,
		      emptySubcat, false, false);
  /** A reusable {@link ModifierEvent} object for look-ups in tables. */
  protected ModifierEvent lookupLeftStopEvent =
    new ModifierEvent(null, null, null, SexpList.emptyList, null, null, null,
		      emptySubcat, false, false);
  /** A reusable {@link ModifierEvent} object for look-ups in tables. */
  protected ModifierEvent lookupRightStopEvent =
    new ModifierEvent(null, null, null, SexpList.emptyList, null, null, null,
		      emptySubcat, false, false);
  /**
   * A lookup Word object, for obtaining a canonical version.
   */
  protected Word lookupWord = Words.get(null, null, null);
  /**
   * A reflexive map of Word objects, for getting a canonical version.
   */
  protected Map canonicalWords = new danbikel.util.HashMap();
  /**
   * A reusable set for storing <code>Word</code> objects, used when seeding
   * the chart in {@link #initialize}.
   */
  protected Set wordSet = new HashSet();
  // data member used by both getPrevMods and getPrevModWords
  /**
   * A reusable list node for use by {@link #getPrevMods} and {@link
   * #getPrevModWords}.
   */
  protected SLNode tmpChildrenList = new SLNode(null, null);
  // data members used by getPrevMods
  /**
   * A reflexive map in which to store canonical versions of {@link SexpList}
   * objects that represent unlexicalized previous modifier lists.
   */
  protected Map canonicalPrevModLists = new danbikel.util.HashMap();
  /**
   * A reusable object for constructing previous modifier lists for chart
   * items.
   */
  protected SexpList prevModLookupList = new SexpList(numPrevMods);
  // data members used by getPrevModWords
  /**
   * A reusable object for constructing previous left-modifier word lists for
   * chart items.
   */
  protected WordList prevModWordLeftLookupList =
    WordListFactory.newList(numPrevMods);
  /**
   * A reusable object for constructing previous right-modifier word lists for
   * chart items.
   */
  protected WordList prevModWordRightLookupList =
    WordListFactory.newList(numPrevMods);
  // data members used by joinItems
  /** A (currently unused) reusable lookup object. */
  protected Subcat lookupSubcat = Subcats.get();
  // data members used by futurePossible (when using simpleModNonterminalMap)
  /**
   * A reusable object used for constructing parent-head-side triples when
   * employing the simpler of two methods for determining whether a particular modifier
   * is possible in the context of a particular parent-head-side combination.
   *
   * @see Settings#useSimpleModNonterminalMap
   * @see DecoderServerRemote#simpleModNonterminalMap()
   */
  protected SexpList parentHeadSideLookupList =
    new SexpList(3).add(null).add(null).add(null);
  /**
   * A reusable object used for constructing a partially-lexicalized modifier
   * nonterminal when employing the simpler of two methods for determining
   * whether a particular modifier is possible in the context of a particular
   * parent-head-side combination.
   *
   * @see Settings#useSimpleModNonterminalMap
   * @see DecoderServerRemote#simpleModNonterminalMap()
   */
  protected SexpList partiallyLexedModLookupList =
    new SexpList(2).add(null).add(null);
  // values for comma constraint-finding
  /** The boolean value of {@link Settings#decoderUseCommaConstraint}. */
  protected boolean useCommaConstraint;
  /**
   * A reusable array for storing which words are considered commas for the
   * comma-pruning constraint.  If a word at index <tt>i</tt> is such a comma,
   * then <code>commaForPruning[i]</code> will be <code>true</code> after {@link
   * #setCommaConstraintData()} has been invoked.
   *
   * @see Settings#decoderUseCommaConstraint
   * @see #setCommaConstraintData()
   */
  protected boolean[] commaForPruning;
  /**
   * A reusable array for storing which words are considered conjunctions for
   * the conjunction-pruning constraint.  If a word at index <tt>i</tt> is such
   * a conjunction, then <code>conjForPruning[i]</code> will be
   * <code>true</code> after {@link #setCommaConstraintData()} has been
   * invoked.
   *
   * @see Settings#decoderUseCommaConstraint
   * @see #setCommaConstraintData()
   */
  protected boolean[] conjForPruning;

  /** Cached value of {@link Settings#keepAllWords}, for efficiency and
      convenience. */
  protected boolean keepAllWords = Settings.getBoolean(Settings.keepAllWords);

  /**
   * Caches the ConstraintSet, if any, for the current sentence.
   */
  protected ConstraintSet constraints = null;
  /**
   * Caches the value of {@link ConstraintSet#findAtLeastOneSatisfying()},
   * if there are constraints for the current sentence; otherwise, this
   * data member will be set to <tt>false</tt>.
   *
   * @see #constraints
   */
  protected boolean findAtLeastOneSatisfyingConstraint = false;
  /**
   * Caches whether or not the ConstraintSet for the current sentence
   * requires a tree that is isomorphic to the tree of constraints.
   * Specifically, this data member will be set to <tt>true</tt> if the
   * {@link ConstraintSet#findAtLeastOneSatisfying} and
   * {@link ConstraintSet#hasTreeStructure} methods of the current
   * sentence's constraint set both return <tt>true</tt>.
   * If there is no constraint set for the current sentence, this data
   * member is set to <tt>false</tt>.
   *
   * @see #constraints
   */
  protected boolean isomorphicTreeConstraints = false;
  /**
   * Indicates whether to perform post-processing on a tree after
   * parsing, that is, whether to invoke {@link Training#postProcess(Sexp)}
   * on the tree.
   *
   * @see Settings#decoderDontPostProcess
   * @see Settings#decoderOutputInsideProbs
   */
  protected boolean dontPostProcess =
    Settings.getBoolean(Settings.decoderDontPostProcess) ||
    Settings.getBoolean(Settings.decoderOutputInsideProbs);

  // data members used when debugSentenceSize is true
  private float avgSentLen = 0.0f;
  private int numSents = 0;

  // data member to use when debugAnalyzeChart is true
  // (and when the property "parser.debug.goldFilename" has been set)
  private SexpTokenizer goldTok;

  /**
   * Constructs a new decoder that will use the specified
   * <code>DecoderServer</code> to get all information and probabilities
   * required for decoding (parsing).
   *
   * @param id the id of this parsing client
   * @param  server the <code>DecoderServerRemote</code> implementor
   * (either local or remote) that provides this decoder object with
   * information and probabilities required for decoding (parsing)
   */
  public Decoder(int id, DecoderServerRemote server) {
    this.id = id;
    this.server = server;
    boolean localCache =
      Settings.getBoolean(Settings.decoderUseLocalProbabilityCache);
    if (localCache) {
      wrapCachingServer();
    }
    constructorHelper(server);
    Settings.register(this);
  }

  private void constructorHelper(DecoderServerRemote server) {
    try {
      this.posMap = server.posMap();
      posSet = new HashSet();
      Iterator posVals = posMap.values().iterator();
      while (posVals.hasNext()) {
	SexpList posList = (SexpList)posVals.next();
	for (int i = 0; i < posList.length(); i++)
	  posSet.add(posList.get(i));
      }
      CountsTable nonterminalTable = new CountsTableImpl();
      nonterminalTable.addAll(server.nonterminals());
      // first, cache all nonterminals (and these are strictly nonterminals
      // and not parts of speech) into nonterminals array
      nonterminals = new Symbol[nonterminalTable.size()];
      Iterator it = nonterminalTable.keySet().iterator();
      for (int i = 0; it.hasNext(); i++)
	nonterminals[i] = (Symbol)it.next();
      // before setting up fastUidMap and fastArgMap, add pos tags to
      // nonterminal table, just in case pos tags can be args
      it = posSet.iterator();
      while (it.hasNext())
	nonterminalTable.add(it.next());
      Subcat sampleSubcat = Subcats.get();
      if (sampleSubcat instanceof SubcatBag)
	SubcatBag.setUpFastUidMap(nonterminalTable);
      if (sampleSubcat instanceof BrokenSubcatBag)
	BrokenSubcatBag.setUpFastUidMap(nonterminalTable);
      Language.training().setUpFastArgMap(nonterminalTable);
      if (useHeadToParentMap) {
	this.headToParentMap = new HashMap(server.headToParentMap());
	convertHeadToParentMap();
      }
      this.leftSubcatMap = new HashMap(server.leftSubcatMap());
      this.rightSubcatMap = new HashMap(server.rightSubcatMap());
      convertSubcatMaps();
      this.leftSubcatPS = server.leftSubcatProbStructure().copy();
      this.rightSubcatPS = server.rightSubcatProbStructure().copy();
      this.modNonterminalMap = server.modNonterminalMap();
      this.simpleModNonterminalMap = server.simpleModNonterminalMap();
      this.modNonterminalPS = server.modNonterminalProbStructure().copy();
      prunedPretermsPosMap = new danbikel.util.HashMap();
      prunedPretermsPosSet = new HashSet();
      Set prunedPreterms = server.prunedPreterms();
      it = prunedPreterms.iterator();
      while (it.hasNext()) {
	Word word = Language.treebank.makeWord((Sexp)it.next());
	Util.addToValueSet(prunedPretermsPosMap, word.word(), word.tag());
	prunedPretermsPosSet.add(word.tag());
      }
      if (debugPrunedPretermsPosMap)
	System.err.println("prunedPretermsPosMap: " + prunedPretermsPosMap);
      prunedPunctuationPosMap = new danbikel.util.HashMap();
      Set prunedPunctuation = server.prunedPunctuation();
      it = prunedPunctuation.iterator();
      while (it.hasNext()) {
	Word word = Language.treebank.makeWord((Sexp)it.next());
	Util.addToValueSet(prunedPunctuationPosMap, word.word(), word.tag());
      }
      if (debugPrunedPunctuationPosMap)
	System.err.println("prunedPunctuationPosMap: " +
			   prunedPunctuationPosMap);
    } catch (RemoteException re) {
      System.err.println(re);
    }

    leftSubcatPSLastLevel = leftSubcatPS.numLevels() - 1;
    rightSubcatPSLastLevel = rightSubcatPS.numLevels() - 1;

    modNonterminalPSLastLevel = modNonterminalPS.numLevels() - 1;

    if (Settings.getBoolean(Settings.decoderUseCellLimit)) {
      cellLimit = Settings.getInteger(Settings.decoderCellLimit);
    }
    boolean usePruneFact = Settings.getBoolean(Settings.decoderUsePruneFactor);
    if (usePruneFact) {
      pruneFact = Math.log(10) *
		  Double.parseDouble(Settings.get(Settings.decoderPruneFactor));
      String maxPruneFactStr = Settings.get(Settings.decoderMaxPruneFactor);
      maxPruneFact = maxPruneFactStr == null ? pruneFact :
                     Math.log(10) * Double.parseDouble(maxPruneFactStr);
      String pruneFactIncrementStr =
        Settings.get(Settings.decoderPruneFactorIncrement);
      pruneFactIncrement = Math.log(10) *
                           Double.parseDouble(pruneFactIncrementStr);
    }
    useCommaConstraint =Settings.getBoolean(Settings.decoderUseCommaConstraint);

    chart = new CKYChart(cellLimit, pruneFact);

    if (!usePruneFact)
      chart.dontDoPruning();

    if (debugAnalyzeChart) {
      setGoldTok();
    }
  }

  private void setGoldTok() {
    String goldFilename = Settings.get(debugGoldFilenameProperty);
    if (goldFilename != null) {
      try {
	goldTok = new SexpTokenizer(goldFilename, Language.encoding(),
				    Constants.defaultFileBufsize);
      }
      catch (Exception e) {
	throw new RuntimeException(e.toString());
      }
    }
  }

  /**
   * Wraps the normal {@link DecoderServerRemote} instance in a caching
   * version.
   *
   * @see Settings#decoderUseLocalProbabilityCache
   * @see CachingDecoderServer
   */
  protected void wrapCachingServer() {
    server = new CachingDecoderServer(server);
  }

  /**
   * Converts the values of the read-only {@link #headToParentMap} from {@link
   * Set} objects to arrays of {@link Symbol}, that is, arrays of type
   * <code>Symbol[]</code>. This is an optimization so that there is no need to
   * create a new iterator object for each traversal of the set.
   */
  protected void convertHeadToParentMap() {
    if (debugConvertHeadMap)
      System.err.print(className + ": converting head map...");
    Iterator entries = headToParentMap.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry)entries.next();
      Set parents = (Set)entry.getValue();
      Symbol[] newValue = new Symbol[parents.size()];
      parents.toArray(newValue);
      entry.setValue(newValue);
    }
    if (debugConvertHeadMap)
      System.err.println("done.");
  }

  /**
   * This helper method used by constructor converts the values of the subcat
   * maps from <code>Set</code> objects (containing <code>Subcat</code>
   * objects) to <code>Subcat</code> arrays, that is, objects of type
   * <code>Subcat[]</code>.  This allows possible subcats for given contexts
   * to be iterated over without the need to create <code>Iterator</code>
   * objects during decoding.
   */
  protected void convertSubcatMaps() {
    if (debugConvertSubcatMaps)
      System.err.print(className + ": converting subcat maps...");
    convertSubcatMap(leftSubcatMap);
    convertSubcatMap(rightSubcatMap);
    if (debugConvertSubcatMaps)
      System.err.println("done.");
  }

  /**
   * Helper method used by {@link #convertSubcatMaps()}.
   *
   * @param subcatMap the subcat map whose values are to be converted
   */
  protected void convertSubcatMap(Map subcatMap) {
    Iterator entries = subcatMap.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry)entries.next();
      Set subcats = (Set)entry.getValue();
      Subcat[] newValue = new Subcat[subcats.size()];
      subcats.toArray(newValue);
      entry.setValue(newValue);
    }
  }

  /**
   * Returns whether the specified word was raised as part of the
   * punctuation-raising procedure performed during training.
   *
   * @param word the word to be tested
   * @return whether the specified word was raised as part of the
   *         punctuation-raising procedure performed during training.
   *
   * @see Training#raisePunctuation(Sexp)
   * @see #prunedPunctuationPosMap
   */
  protected boolean isPuncRaiseWord(Sexp word) {
    return prunedPunctuationPosMap.containsKey(word);
  }

  /**
   * A helper method used by {@link #preProcess} that removes words from
   * the specified sentence and {@link #originalWords} lists, and also
   * from the specified tags list, if it is not <code>null</code>.
   *
   * @param sentence the sentence from which to remove a word
   * @param tags the list of tag lists that is coordinated with the specified
   * sentence from which an item is to be removed
   * @param i the index of the word to be removed
   */
  protected void removeWord(SexpList sentence, SexpList tags, int i) {
    if (debugRemoveWord)
      err.print(className + ": removing word " + i + " " + sentence.get(i));
    sentence.remove(i);
    originalWords.remove(i);
    if (tags != null) {
      if (debugRemoveWord)
        err.println(" with tag " + tags.get(i));
      tags.remove(i);
    }
    if (debugRemoveWord)
      err.println();
  }

  /**
   * Performs all preprocessing to the specified coordinated lists of
   * words and part-of-speech tags of the sentence that is about to be parsed.
   * @param sentence a list of words in a sentence to be parsed
   * @param tags a list of part-of-speech tags in a sentence to be parsed,
   * coordinated with the specified list of words
   * @throws RemoteException
   */
  protected void preProcess(SexpList sentence, SexpList tags)
  throws RemoteException {
    // preserve original sentence
    originalSentence.clear();
    originalSentence.addAll(sentence);

    // preserve original tags, if non-null
    if (tags != null)
      originalTags = new SexpList(tags);
    else
      originalTags = null;

    originalWords.clear();
    originalWords.addAll(sentence);

    // eliminate pruned words
    for (int i = sentence.length() - 1; i >= 0; i--) {
      Symbol word = (downcaseWords ?
		     Symbol.get(sentence.get(i).toString().toLowerCase()) :
		     sentence.symbolAt(i));
      Symbol tag = tags == null ? null : tags.listAt(i).first().symbol();
      if (Language.training().removeWord(word, tag, i, sentence,
					 tags, originalTags,
					 prunedPretermsPosSet,
					 prunedPretermsPosMap))
	removeWord(sentence, tags, i);
    }

    SexpList convertedSentence = server.convertUnknownWords(sentence);
    // we cannot just say
    //   sentence = server.convertUnknownWords(sentence);
    // because the server might return a copy of the sentence list (this is
    // guaranteed to happen if the server is remote), and we need
    // to modify the object that was passed as an arg to this function, because
    // that's what the caller of preProcess expects; the alternative would be
    // for this method to work with the (potentially) different list object
    // returned by the server and then return that list object, but then this
    // method should probably also return the tags list as well, making things
    // difficult, since we only get to return a single object
    if (convertedSentence != sentence) {
      sentence.clear();
      sentence.addAll(convertedSentence);
    }


    // downcase words
    int sentLen = sentence.length();
    if (downcaseWords) {
      for (int i = 0; i < sentLen; i++) {
	if (sentence.get(i).isList()) // skip unknown words
	  continue;
	Symbol downcasedWord =
	  Symbol.add(sentence.symbolAt(i).toString().toLowerCase());
	sentence.set(i, downcasedWord);
      }
    }


    // remove intitial and final punctuation "words"
    for (int i = 0; i < sentence.length() - 1; i++) {
      if (sentence.get(i).isList())
	break;
      if (isPuncRaiseWord(sentence.get(i))) {
	removeWord(sentence, tags, i);
	i--;
      }
      else
	break;
    }
    for (int i = sentence.length() - 1; i > 0; i--) {
      if (sentence.get(i).isList())
	break;
      if (isPuncRaiseWord(sentence.get(i))) {
	removeWord(sentence, tags, i);
      }
      else
	break;
    }

    // finally, perform any language-specific pre-processing of the words
    // and tags
    SexpList langSpecific =
      Language.training.preProcessTest(sentence, originalWords, tags);
    sentence = langSpecific.listAt(0);
    if (tags != null)
      tags = langSpecific.listAt(1);
  }

  /**
   * Performs post-processing on a sentence that has been parsed.
   * @param tree the parse tree of a sentence that has been parsed.
   *
   * @see Settings#restorePrunedWords
   * @see Training#postProcess(Sexp)
   */
  protected void postProcess(Sexp tree) {
    restoreOriginalWords(tree, 0);
    if (restorePrunedWords)
      restorePrunedWords(tree);
    if (dontPostProcess)
      return;
    else
      Language.training.postProcess(tree);
  }

  /**
   * Restores the original words in the current sentence.
   *
   * @param tree the sentence for which to restore the original words,
   * cached during execution of {@link #preProcess}
   * @param wordIdx a threaded word index
   * @return the current value of the monotonically-increasing word index,
   * after replacing all words in the current subtree
   */
  protected int restoreOriginalWords(Sexp tree, int wordIdx) {
    Treebank treebank = Language.treebank;
    if (treebank.isPreterminal(tree))
      ;
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++) {
	Sexp currChild = treeList.get(i);
	if (treebank.isPreterminal(currChild)) {
	  Word word = treebank.makeWord(currChild);
	  word.setWord(originalWords.symbolAt(wordIdx++));
	  treeList.set(i, treebank.constructPreterminal(word));
	}
	else
	  wordIdx = restoreOriginalWords(currChild, wordIdx);
      }
    }
    return wordIdx;
  }

  /**
   * Restores pruned words to a parsed sentence.
   * @param tree the parse tree of a sentence that has been parsed
   *
   * @see #postProcess(Sexp)
   * @see Settings#restorePrunedWords
   */
  protected void restorePrunedWords(Sexp tree) {
    int wordIdx = restorePrunedWordsRecursive(tree, 0);
    while (wordIdx < originalSentence.length()) {
      Symbol newWord = originalSentence.symbolAt(wordIdx);
      Symbol newTag =
	originalTags == null ?
	newWord : originalTags.listAt(wordIdx).first().symbol();
      Word newWordObj = Words.get(newWord, newTag);
      if (debugRestorePrunedWords)
	System.err.println(className + ": restoring pruned word " + newWordObj +
			   " at index " + wordIdx);
      tree.list().add(Language.treebank().constructPreterminal(newWordObj));
      wordIdx++;
    }
  }

  /**
   * The recursive helper method for {@link #restorePrunedWords(Sexp)}.  This
   * method restores all words except those pruned from the very end of the
   * original sentence.
   * @param tree the tree whose pruned words are to be restored
   * @param wordIdx the current word idx (threaded through this recursive function)
   * @return the word index of the last word in the specified tree whose
   * pruned words were restored
   */
  protected int restorePrunedWordsRecursive(Sexp tree, int wordIdx) {
    Treebank treebank = Language.treebank;
    if (treebank.isPreterminal(tree))
      ;
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      for (int i = 1; i < treeList.length(); i++) {
	Sexp currChild = treeList.get(i);
	if (treebank.isPreterminal(currChild)) {
	  Word word = treebank.makeWord(currChild);
	  while (word.word() != originalSentence.get(wordIdx)) {
	    Symbol newWord = originalSentence.symbolAt(wordIdx);
	    Symbol newTag =
	      originalTags == null ?
	      newWord : originalTags.listAt(wordIdx).first().symbol();
	    Word newWordObj = Words.get(newWord, newTag);
	    if (debugRestorePrunedWords)
	      System.err.println(className + ": restoring pruned word " +
				 newWordObj + " at index " + wordIdx);
	    // add new word as left-sibling of current word
	    treeList.add(i, treebank.constructPreterminal(newWordObj));
	    i++;
	    wordIdx++;
	  }
	  wordIdx++;
	}
	else
	  wordIdx = restorePrunedWordsRecursive(currChild, wordIdx);
      }
    }
    return wordIdx;
  }

  /**
   * Caches the locations of commas to be used for the comma constraint in the
   * boolean array {@link #commaForPruning}.  Also, sets up an array
   * (initialized to be entirely false) of booleans to cache the locations of
   * conjunctions, determined within {@link #initialize(SexpList,SexpList)}
   * (hence, the initialization of the {@link #conjForPruning} array is not
   * complete until after {@link #initialize(SexpList,SexpList)} has finished
   * executing).
   */
  protected void setCommaConstraintData() {
    if (commaForPruning == null || sentLen > commaForPruning.length)
      commaForPruning = new boolean[sentLen];
    boolean withinParens = false;
    for (int i = 0; i < sentLen; i++) {
      Symbol word = getSentenceWord(i);
      if (Language.treebank.isLeftParen(word))
	withinParens = true;
      else if (Language.treebank.isRightParen(word))
	withinParens = false;
      commaForPruning[i] = !withinParens && Language.treebank.isComma(word);
    }

    if (conjForPruning == null || sentLen > conjForPruning.length)
      conjForPruning = new boolean[sentLen];
    for (int i = 0; i < sentLen; i++)
      conjForPruning[i] = false;
  }

  /**
   * Returns a known word that was observed with the specified part of speech
   * tag.
   *
   * @param tag a part of speech tag for which an example word is to be found
   * @return a word that was observed with the specified part of speech tag.
   */
  protected Symbol getExampleWordForTag(Symbol tag) {
    // first, check cache.
    Symbol word = (Symbol)posToExampleWordMap.get(tag);
    if (word != null)
      return word;

    // run through posMap and find first known word that was observed with the
    // specified tag
    Iterator it = posMap.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry)it.next();
      word = (Symbol)entry.getKey();
      SexpList tags = (SexpList)entry.getValue();
      if (tags.contains(tag)) {
	if (downcaseWords)
	  word = Symbol.get(word.toString().toLowerCase());
	posToExampleWordMap.put(tag, word);
	return word;
      }
    }
    return null;
  }

  /**
   * Gets the set of possible part-of-speech tags for a word in the sentence
   * to be parsed.  The set returned is a list of symbols.
   * @param tags the list of supplied part-of-speech tags with the current
   * sentence, or <code>null</code> if no tags were supplied
   * @param wordIdx the index of the word whose possible tags
   * are to be gotten
   * @param word the word at the specified index whose possible tags
   * are to be gotten
   * @param wordIsUnknown whether the specified word is unknown, as far
   * as the {@link DecoderServerRemote} is concerned
   * @param origWord the original word before any mapping to a word-feature vector
   * @param tmpSet a temporary set used during the invocation of this method
   * @return the set of possible part-of-speech tags for a word in the sentence
   * to be parsed, as a list of symbols
   */
  protected SexpList getTagSet(SexpList tags, int wordIdx, Symbol word,
			       boolean wordIsUnknown, Symbol origWord,
			       HashSet tmpSet) {
    SexpList tagSet = null;

    int i = wordIdx;

    if (useOnlySuppliedTags) {
      tagSet = tags.listAt(i);
      // if word is known and has never been observed with any supplied tags,
      // issue a warning
      if (!wordIsUnknown) {
	boolean allSuppliedTagsUnobserved = true;
	SexpList observedTagSet = (SexpList)posMap.get(word);
	for (int tagIdx = 0; tagIdx < tagSet.length(); tagIdx++) {
	  if (observedTagSet.contains(tagSet.symbolAt(tagIdx))) {
	    allSuppliedTagsUnobserved = false;
	    break;
	  }
	}
	if (allSuppliedTagsUnobserved) {
	  System.err.println(className +
			     ": warning: useOnlySuppliedTags=true but known " +
			     "word \"" + word + "\" (idx=" + wordIdx + ") " +
			     "in sentence " +
			     (sentenceIdx + 1) + " has never been " +
			     "observed with any of supplied tags " + tagSet);
	  //tagSet = observedTagSet;
	}
      }
    }
    else if (wordIsUnknown) {
      if (useLowFreqTags && posMap.containsKey(origWord)) {
	tagSet = (SexpList)posMap.get(origWord);
	if (tags != null)
	  tagSet = setUnion(tagSet, tags.listAt(i), tmpSet);
      }
      else if (tags != null)
	tagSet = tags.listAt(i);
      else
	tagSet = (SexpList)posMap.get(word);
    }
    else {
      tagSet = (SexpList)posMap.get(word);
    }

    if (tagSet == null) {
      Symbol defaultFeatures = Language.wordFeatures.defaultFeatureVector();
      tagSet = (SexpList)posMap.get(defaultFeatures);
    }
    if (tagSet == null) {
      tagSet = SexpList.emptyList;
      System.err.println(className + ": warning: no tags for default " +
			 "feature vector " + word +
			 " (word index=" + wordIdx + ")");
    }
    return tagSet;
  }

  /**
   * Adds a chart item for every possible part of speech for the specified
   * word at the specified index in the current sentence.
   *
   * @param word the current word
   * @param wordIdx the index of the current word in the current sentence
   * @param features the word-feature vector for the current word
   * @param neverObserved indicates whether the current word was never observed
   * during training (a truly unknown word)
   * @param tagSet a list containing all possible part of speech tags for
   * the current word
   * @param constraints the constraint set for this sentence
   * @throws RemoteException if any calls to the underlying
   * {@link DecoderServerRemote} object throw a <code>RemoteException</code>
   *
   * @see Chart#add(int,int,Item)
   */
  protected void seedChart(Symbol word, int wordIdx, Symbol features,
			   boolean neverObserved, SexpList tagSet,
			   boolean wordIsUnknown, Symbol origWord,
			   ConstraintSet constraints) throws RemoteException {
    int i = wordIdx;
    int numAddedAtWordIdx = 0;
    int numTags = tagSet.length();
    for (int tagIdx = 0; tagIdx < numTags; tagIdx++) {
      Symbol tag = tagSet.symbolAt(tagIdx);
      if (!posSet.contains(tag))
	System.err.println(className + ": warning: part of speech tag " +
			   tag + " not seen during training");
      if (useCommaConstraint)
	if (Language.treebank.isConjunction(tag))
	  conjForPruning[i] = true;
      Word headWord = neverObserved ?
		      Words.get(word, tag, features) :
		      getCanonicalWord(lookupWord.set(word, tag, features));
      CKYItem item = chart.getNewItem();
      PriorEvent priorEvent = lookupPriorEvent;
      priorEvent.set(headWord, tag);
      double logPrior = server.logPrior(id, priorEvent);

      if (substituteWordsForClosedClassTags &&
	  numTags == 1 && wordIsUnknown && logPrior <= logOfZero) {
	Symbol exampleWord = getExampleWordForTag(tag);
	headWord = getCanonicalWord(lookupWord.set(exampleWord, tag, features));
	priorEvent.set(headWord, tag);
	logPrior = server.logPrior(id, priorEvent);
      }
      // if relaxConstraints is true and
      // if this is our last chance to add a tag for the current word and we
      // haven't added any chart items yet and the logPrior is logOfZero,
      // add a chart item with logProb == logPrior == logProbSmall
      else if (relaxConstraints && logPrior <= logOfZero &&
	       tagIdx == numTags - 1 && numAddedAtWordIdx == 0)
	logPrior = Constants.logProbSmall;

      double logProb = logPrior; // technically, logPrior + logProbCertain
      item.set(tag, headWord,
	       emptySubcat, emptySubcat, null, null,
	       null,
	       startList, startList,
	       i, i,
	       false, false, true,
	       Constants.logProbCertain, logPrior, logProb);

      if (findAtLeastOneSatisfyingConstraint) {
	Constraint constraint = constraints.constraintSatisfying(item);
	if (constraint != null) {
	  if (debugConstraints)
	    System.err.println("assigning satisfied constraint " +
			       constraint + " to item " + item);
	  item.setConstraint(constraint);
	}
	else {
	  if (debugConstraints)
	    System.err.println("no satisfying constraint for item " + item);
	  continue;
	}
      }
      boolean added = chart.add(i, i, item);
      if (added)
	numAddedAtWordIdx++;
    } // end for each tag
  }

  /**
   * Initializes the chart for parsing the specified sentence.  Specifically,
   * this method will add a chart item for each possible part of speech for
   * each word.
   *
   * @param sentence the sentence to parse, which must be a list containing
   * only symbols as its elements
   */
  protected void initialize(SexpList sentence) throws RemoteException {
    initialize(sentence, null);
  }

  /**
   * Initializes the chart for parsing the specified sentence, using the
   * specified coordinated list of part-of-speech tags when assigning parts
   * of speech to unknown words.
   *
   * @param sentence the sentence to parse, which must be a list containing
   * only symbols as its elements
   * @param tags a list that is the same length as <code>sentence</code> that
   * will be used when seeding the chart with the parts of speech for unknown
   * words; each element <i>i</i> of <code>tags</code> should itself be a
   * <code>SexpList</code> containing all possible parts of speech for the
   * <i>i</i><sup>th</sup> word in <code>sentence</code>; if the value of this
   * argument is <code>null</code>, then for each unknown word (or feature
   * vector), all possible parts of speech observed in the training data for
   * that unknown word will be used
   */
  protected void initialize(SexpList sentence, SexpList tags)
  throws RemoteException {

    preProcess(sentence, tags);

    if (debugInit) {
      System.err.println(className + ": sentence to parse: " + sentence);
    }

    this.sentence = sentence;
    sentLen = sentence.length();

    if (useCommaConstraint)
      setCommaConstraintData();

    HashSet tmpSet = new HashSet();

    for (int i = 0; i < sentLen; i++) {
      boolean wordIsUnknown = sentence.get(i).isList();
      boolean neverObserved = false;
      Symbol word = null, features = null;
      if (wordIsUnknown) {
	SexpList wordInfo = sentence.listAt(i);
	neverObserved = wordInfo.symbolAt(2) == Constants.trueSym;
	if (keepAllWords) {
	  features = wordInfo.symbolAt(1);
	  word = neverObserved ? features : wordInfo.symbolAt(0);
	}
	else {
	  // we *don't* set features, since when keepAllWords is false,
	  // we simply replace unknown words with their word feature vectors
	  word = wordInfo.symbolAt(1);
	}
      }
      else {
	// word is a known word, so just grab it
	word = sentence.symbolAt(i);
      }

      Symbol origWord = (wordIsUnknown ?
			 sentence.listAt(i).symbolAt(0) : sentence.symbolAt(i));

      SexpList tagSet =
	  getTagSet(tags, i, word, wordIsUnknown, origWord, tmpSet);

      seedChart(word, i, features, neverObserved, tagSet,
		wordIsUnknown, origWord, constraints);

      addUnariesAndStopProbs(i, i);
    } // end for each word index
  }

  /**
   * Gets the canonical {@link Word} object for the specified object.
   * @param lookup the {@link Word} object to be canonicalized
   * @return the canonical {@link Word} object for the specified object.
   *
   * @see #canonicalWords
   */
  protected Word getCanonicalWord(Word lookup) {
    Word canonical = (Word)canonicalWords.get(lookup);
    if (canonical == null) {
      canonical = lookup.copy();
      canonicalWords.put(canonical, canonical);
    }
    return canonical;
  }

  /**
   * Returns a new list that is the union of the two specified lists.
   * @param l1 the first list whose element are to be in the union
   * @param l2 the second list whose element are to be in the union
   * @param tmpSet a temporary set to be used during the invocation of this
   * method
   * @return a new list that is the union of the two specified lists.
   */
  protected SexpList setUnion(SexpList l1, SexpList l2, Set tmpSet) {
    tmpSet.clear();
    for (int i = 0; i < l1.length(); i++)
      tmpSet.add(l1.get(i));
    for (int i = 0; i < l2.length(); i++)
      tmpSet.add(l2.get(i));
    SexpList union = new SexpList(tmpSet.size());
    Iterator it = tmpSet.iterator();
    while (it.hasNext())
      union.add((Sexp)it.next());
    return union;
  }

  /**
   * Parses the specified sentence.
   *
   * @param sentence a list of symbols representing words of a sentence to be
   *                 parsed
   * @return a parse tree for the specified sentence, or <code>null</code> if no
   *         parse could be found or if a {@link TimeoutException} is thrown
   *
   * @throws RemoteException if the internal {@link DecoderServerRemote}
   *                         instance throws an exception, or some other
   *                         exception is thrown
   */
  protected Sexp parse(SexpList sentence) throws RemoteException {
    return parse(sentence, null);
  }

  /**
   * Parses the specified sentence using the supplied list of part-of-speech
   * tags.
   * @param sentence a list of symbols representing the words of a sentence
   * to be parsed
   * @param tags a list of part-of-speech tags (symbols) coordinated with
   * the specified list of words
   * @return a parse tree for the specified sentence, or <code>null</code> if no
   *         parse could be found or if a {@link TimeoutException} is thrown
   * @throws RemoteException if the internal {@link DecoderServerRemote}
   *                         instance throws an exception, or some other
   *                         exception is thrown
   */
  protected Sexp parse(SexpList sentence, SexpList tags)
    throws RemoteException {
    return parse(sentence, tags, null);
  }

  /**
   * Parses the specified sentence using the supplied list of part-of-speech
   * tags and the supplied set of parsing constraints.
   * @param sentence a list of symbols representing the words of a sentence
   * to be parsed
   * @param tags a list of part-of-speech tags (symbols) coordinated with
   * the specified list of words
   * @param constraints a set of parsing constraints for the specified sentence
   * @return a parse tree for the specified sentence, or <code>null</code> if no
   *         parse could be found or if a {@link TimeoutException} is thrown
   * @throws RemoteException if the internal {@link DecoderServerRemote}
   *                         instance throws an exception, or some other
   *                         exception is thrown
   */
  protected Sexp parse(SexpList sentence, SexpList tags,
		       ConstraintSet constraints)
    throws RemoteException {

    if (debugOutputAllCounts)
      Debug.level = 21;

    sentenceIdx++;
    time.reset();
    if (maxSentLen > 0 && sentence.length() > maxSentLen) {
      if (debugSentenceSize)
	System.err.println(className + ": current sentence length " +
			   sentence.length() + " is greater than max. (" +
			   maxSentLen + ")");
      return null;
    }

    if (constraints == null) {
      findAtLeastOneSatisfyingConstraint = isomorphicTreeConstraints = false;
    }
    else {
      this.constraints = constraints;
      findAtLeastOneSatisfyingConstraint =
	constraints.findAtLeastOneSatisfying();
      isomorphicTreeConstraints =
	findAtLeastOneSatisfyingConstraint && constraints.hasTreeStructure();
      if (debugConstraints)
	System.err.println(className + ": constraints: " + constraints);
    }

    chart.setSizeAndClear(sentence.length());
    initialize(sentence, tags);

    if (debugSentenceSize) {
      System.err.println(className + ": current sentence length: " + sentLen +
			 " word" + (sentLen == 1 ? "" : "s"));
      numSents++;
      avgSentLen = ((numSents - 1)/(float)numSents) * avgSentLen +
		   (float)sentLen / numSents;
      System.err.println(className + ": cummulative average length: " +
			 avgSentLen + " words");
    }

    if (sentLen == 0) {         // preprocessing could have removed all words!
      chart.postParseCleanup(); // get rid of seed items from initialize()
      sentence.clear();
      sentence.addAll(originalSentence); // restore original sentence
      return null;
    }

    double currPruneFact = pruneFact;

    CKYItem topRankedItem = null;
    double prevTopLogProb = Constants.logOfZero;
    CKYItem topRankedSentenceSpanItem = null; // for debugging ONLY
    int numSortedItems = 0;
    CKYItem[] sortedItems = null;

    // take care of round-off error, making sure we reach maxPruneFact
    // in loop
    double pruneFactLimit = maxPruneFact + pruneFactIncrement / 2;

    hardConstraints = true;
    chart.dontRelax();

    // BEGIN BEAM-WIDENING CODE
    for (int iteration = 1;
         topRankedItem == null;
	 currPruneFact += pruneFactIncrement, iteration++) {
      boolean triedWidestBeam = currPruneFact > pruneFactLimit;
      if (triedWidestBeam) {
	if (!relaxConstraints || !hardConstraints) {
	  // if we're not supposed to ever relax constraints, or we've already
	  // tried parsing with relaxed constraints, and it *still*
	  // didn't work, so reset hardConstraints data member and give up
	  hardConstraints = true;
	  chart.dontRelax();
	  break;
	}
	else {
	  // temporarily relax constraints and try parsing one last time
	  // at widest beam setting
	  currPruneFact -= pruneFactIncrement;
	  hardConstraints = false;
	  chart.relax();
	  if (debugBeamWidening)
	    System.err.println(className + ": couldn't parse with widest " +
			       "beam, so trying one last time with " +
			       "relaxed constraints");
	}
      }
      if (debugBeamWidening)
        System.err.println(className + ": trying with prune factor of " +
                           (currPruneFact / Math.log(10)));
      chart.setPruneFactor(currPruneFact);
      if (iteration > 1) {
        chart.clearNonPreterminals();
        for (int i = 0; i < sentLen; i++)
          addUnariesAndStopProbs(i,i);
      }
    // END BEAM-WIDENING CODE
    try {
      for (int span = 2; span <= sentLen; span++) {
        if (debugSpans)
          System.err.println(className + ": span: " + span);
        int split = sentLen - span + 1;
        for (int start = 0; start < split; start++) {
          int end = start + span - 1;
          if (debugSpans)
            System.err.println(className + ": start: " + start +
                               "; end: " + end);
          complete(start, end);
        }
      }
    }
    catch (TimeoutException te) {
      if (debugMaxParseTime) {
        System.err.println(te.getMessage());
      }
    }
    topRankedSentenceSpanItem = (CKYItem)chart.getTopItem(0, sentLen - 1);
    prevTopLogProb = chart.getTopLogProb(0, sentLen - 1);
    if (debugTop) {
      System.err.println(className + ": highest probability item for " +
                         "sentence-length span (0," + (sentLen - 1) + "): " +
                         prevTopLogProb);
      System.err.println("\t" + topRankedSentenceSpanItem);
    }
    chart.resetTopLogProb(0, sentLen - 1);
    addTopUnaries(sentLen - 1);

    // the chart mixes two types of items that cover the entire span
    // of the sentnece: those that have had their +TOP+ probability multiplied
    // in (with topSym as their label) and those that have not; if the
    // top-ranked item also has topSym as its label, we're done; otherwise,
    // we look through all items that cover the entire sentence and get
    // the highest-ranked item whose label is topSym (NO WE DO NOT, since
    // we reset the top-ranked item just before adding top unaries)
    CKYItem potentialTopItem = (CKYItem)chart.getTopItem(0, sentLen - 1);
    if (potentialTopItem != null && potentialTopItem.label() == topSym)
      topRankedItem = potentialTopItem;
    sortedItems = null;
    numSortedItems = 0;
    if (kBest > 1 && topRankedItem != null) {
      int numSentSpanItems = chart.numItems(0, sentLen - 1);
      sortedItems = new CKYItem[numSentSpanItems];
      Iterator sentSpanIt = chart.get(0, sentLen - 1);
      while (sentSpanIt.hasNext()) {
	CKYItem item = (CKYItem)sentSpanIt.next();
	if (item.label() == topSym)
	  sortedItems[numSortedItems++] = item;
      }
      Arrays.sort(sortedItems, 0, numSortedItems);
    }

    // BEGIN BEAM-WIDENING CODE
    }
    // END BEAM-WIDENING CODE

    if (debugTop)
      System.err.println(className + ": top-ranked +TOP+ item: " +
			 topRankedItem);


    if (debugConstraints) {
      Iterator it = constraints.iterator();
      while (it.hasNext()) {
	Constraint c = (Constraint)it.next();
	System.err.println(className + ": constraint " + c + " has" +
			   (c.hasBeenSatisfied() ? " " : " NOT ") +
			   "been satisfied");
      }
    }

    /*
    if (topRankedItem == null) {
     double highestProb = logOfZero;
     Iterator it = chart.get(0, sentLen - 1);
     while (it.hasNext()) {
       CKYItem item = (CKYItem)it.next();
       if (item.label() != topSym)
	 continue;
       if (item.logProb() > highestProb) {
	 topRankedItem = item;
	 highestProb = item.logProb();
       }
     }
    }
    */

    if (debugAnalyzeChart) {
      Sexp goldTree = null;
      try {
	goldTree = Sexp.read(goldTok);
	if (goldTree != null) {
	  String prefix = "chart-debug (" + sentenceIdx + "): ";
	  danbikel.parser.util.DebugChart.findConstituents(prefix,
							   downcaseWords,
							   chart, topRankedItem,
							   sentence,
							   goldTree);
	}
	else
	  System.err.println(className + ": couldn't read gold parse tree " +
			     "for chart analysis of sentence " + sentenceIdx);
      }
      catch (IOException ioe) {
	System.err.println(className + ": couldn't read gold parse tree " +
			   "for chart analysis of sentence " + sentenceIdx);
      }
    }

    if (debugAnalyzeBestDerivation) {
      String prefix = "derivation-debug for sent. " + sentenceIdx + " (len=" +
	sentLen + "): ";
      danbikel.parser.util.DebugChart.printBestDerivationStats(prefix,
							       chart,
							       sentLen,
							       topSym,
							       prevTopLogProb,
							       topRankedItem);
    }

    if (debugOutputChart) {
      try {
	String chartFilename =
	  debugChartFilenamePrefix + "-" + id + "-" + sentenceIdx + ".obj";
	System.err.println(className +
			   ": outputting chart to Java object file " +
			   "\"" + chartFilename + "\"");
	BufferedOutputStream bos =
	  new BufferedOutputStream(new FileOutputStream(chartFilename),
				   Constants.defaultFileBufsize);
	ObjectOutputStream os = new ObjectOutputStream(bos);
	os.writeObject(chart);
	os.writeObject(topRankedItem);
	os.writeObject(sentence);
	os.writeObject(originalWords);
	os.close();
      }
      catch (IOException ioe) {
	System.err.println(ioe);
      }
    }

    chart.postParseCleanup();

    if (topRankedItem == null) {
      sentence.clear();
      sentence.addAll(originalSentence); // restore original sentence
      return null;
    }
    else if (kBest == 1) {
      Sexp tree = topRankedItem.headChild().toSexp();
      postProcess(tree);
      return tree;
    }
    else {
      SexpList treeList = new SexpList();
      int counter = 0;
      for (int itemIdx = numSortedItems - 1;
	   itemIdx >= 0 && counter < kBest; counter++, itemIdx--) {
	Sexp tree = sortedItems[itemIdx].headChild().toSexp();
	postProcess(tree);
	treeList.add(tree);
      }
      return treeList;
    }
  }

  /**
   * Adds hiden root nonterminal probabilities.  That is, for each derivation
   * spanning the entire sentence from index 0 to the specified end index, this
   * method produces new chart items in which the probability of producing that
   * derivation given {@link Training#topSym()} has been multiplied to the
   * existing item's score.
   *
   * @param end the index of the last word of the sentence being parsed
   * @throws RemoteException
   */
  protected void addTopUnaries(int end) throws RemoteException {
    topProbItemsToAdd.clear();
    Iterator sentSpanItems = chart.get(0, end);
    while (sentSpanItems.hasNext()) {
      CKYItem item = (CKYItem)sentSpanItems.next();
      if (item.stop()) {

	HeadEvent headEvent = lookupHeadEvent;
	headEvent.set(item.headWord(), topSym, (Symbol)item.label(),
		      emptySubcat, emptySubcat);
	double topLogProb = server.logProbTop(id, headEvent);
	if (topLogProb <= logOfZero) {
	  if (hardConstraints)
	    continue;
	  else
	    topLogProb = Constants.logProbSmall;
	}
	double logProb = item.logTreeProb() + topLogProb;

	if (debugTop)
	  System.err.println(className +
			     ": item=" + item + "; topLogProb=" + topLogProb +
			     "; item.logTreeProb()=" + item.logTreeProb() +
			     "; logProb=" + logProb);

	if (findAtLeastOneSatisfyingConstraint) {
	  if (debugConstraints)
	    System.err.println(className +
			       ": sentence-spanning item has constraint " +
			       item.getConstraint());
	}
	if (isomorphicTreeConstraints) {
	  Constraint parent = item.getConstraint().getParent();
	  if (debugConstraints)
	    System.err.println(className + ": parent constraint is " + parent);
	  if (!(parent == null || parent == constraints.root()))
	    continue;
	}

	CKYItem newItem = chart.getNewItem();
	newItem.set(topSym, item.headWord(),
		    emptySubcat, emptySubcat, item,
		    null, null, startList, startList, 0, end,
		    false, false, true, logProb, Constants.logProbCertain,
		    logProb);

        newItem.hasAntecedent(item);
	topProbItemsToAdd.add(newItem);
      }
    }
    Iterator toAdd = topProbItemsToAdd.iterator();
    while (toAdd.hasNext()) {
      CKYItem item = (CKYItem)toAdd.next();
      boolean added = chart.add(0, end, item);
      if (debugTop) {
	if (!added) {
	  System.err.println(className + ": couldn't add item " + item);
	}
      }
    }
  }

  /**
   * Constructs all possible items spanning the specified indices and adds them
   * to the chart.  This involves joining modificands (items to be modified)
   * with modifiers when the modificand has not yet received its stop
   * probabilities and when the spans of both modificand and modifier cover the
   * specified span.
   *
   * @param start the index of the first word in the span for which all chart
   *              items are to be created and added to the chart
   * @param end   the index of the last word in the span for which all chart
   *              items are to be created and added to the chart
   * @throws RemoteException
   * @throws TimeoutException if the boolean value of {@link Settings#maxParseTime}
   *                          is greater than zero has been reached while
   *                          parsing
   * @see #joinItems(CKYItem,CKYItem,boolean)
   */
  protected void complete(int start, int end)
    throws RemoteException, TimeoutException {
    for (int split = start; split < end; split++) {

      if (maxParseTime > 0 && time.elapsedMillis() > maxParseTime) {
	throw new TimeoutException(className + ": ran out of time (>" +
				   maxParseTime + "ms) on sentence " +
				   sentenceIdx);
      }

      if (useCommaConstraint && commaConstraintViolation(start, split, end)) {
	if (debugCommaConstraint) {
	  System.err.println(className +
			     ": constraint violation at (start,split,end+1)=(" +
			     start + "," + split + "," + (end + 1) +
			     "); word at end+1 = " + getSentenceWord(end + 1));
	}
	// EVEN IF there is a constraint violation, we still try to find
	// modificands that have not yet received their stop probabilities
	// whose labels are baseNP, to see if we can add a premodifier
	// (so that we can build baseNPs to the left even if they contain
	// commas)
	// TECHNICALLY, we should really try to build constituents on both
	// the LEFT *and* RIGHT, but since base NPs are typically right-headed,
	// this hack works well (at least, in English and Chinese), but it is
	// a hack nonetheless
	boolean modifierSide = Constants.LEFT;
	int modificandStartIdx =  split + 1;
	int modificandEndIdx =    end;
	int modifierStartIdx  =   start;
	int modifierEndIdx =      split;
	if (debugComplete && debugSpans)
	  System.err.println(className + ": modifying [" +
			     modificandStartIdx + "," + modificandEndIdx +
			     "]" + " with [" + modifierStartIdx + "," +
			     modifierEndIdx + "]");

	// for each possible modifier that HAS received its stop probabilities,
	// try to find a modificand that has NOT received its stop probabilities
	if (chart.numItems(modifierStartIdx, modifierEndIdx) > 0 &&
	    chart.numItems(modificandStartIdx, modificandEndIdx) > 0) {
	  Iterator modifierItems = chart.get(modifierStartIdx, modifierEndIdx);
	  while (modifierItems.hasNext()) {
	    CKYItem modifierItem = (CKYItem)modifierItems.next();
	    if (modifierItem.stop()) {
	      Iterator modificandItems =
		chart.get(modificandStartIdx, modificandEndIdx);
	      while (modificandItems.hasNext()) {
		CKYItem modificandItem = (CKYItem)modificandItems.next();
		if (!modificandItem.stop() &&
		    Language.treebank.isBaseNP((Symbol)modificandItem.label())){
		  if (debugComplete)
		    System.err.println(className +
				       ".complete: trying to modify\n\t" +
				       modificandItem + "\n\twith\n\t" +
				       modifierItem);
		  joinItems(modificandItem, modifierItem, modifierSide);
		}
	      }
	    }
	  }
	}
	continue;
      }

      boolean modifierSide;
      for (int sideIdx = 0; sideIdx < 2; sideIdx++) {
	modifierSide = sideIdx == 0 ? Constants.RIGHT : Constants.LEFT;
	boolean modifyLeft = modifierSide == Constants.LEFT;

	int modificandStartIdx = modifyLeft ?  split + 1  :  start;
	int modificandEndIdx =   modifyLeft ?  end        :  split;

	int modifierStartIdx =   modifyLeft ?  start      :  split + 1;
	int modifierEndIdx =     modifyLeft ?  split      :  end;

	if (debugComplete && debugSpans)
	  System.err.println(className + ": modifying [" +
			     modificandStartIdx + "," + modificandEndIdx +
			     "]" + " with [" + modifierStartIdx + "," +
			     modifierEndIdx + "]");

	// for each possible modifier that HAS received its stop probabilities,
	// try to find a modificand that has NOT received its stop probabilities
	if (chart.numItems(modifierStartIdx, modifierEndIdx) > 0 &&
	    chart.numItems(modificandStartIdx, modificandEndIdx) > 0) {
	  Iterator modifierItems = chart.get(modifierStartIdx, modifierEndIdx);
	  while (modifierItems.hasNext()) {
	    CKYItem modifierItem = (CKYItem)modifierItems.next();
	    if (modifierItem.stop()) {
	      Iterator modificandItems =
		chart.get(modificandStartIdx, modificandEndIdx);
	      while (modificandItems.hasNext()) {
		CKYItem modificandItem = (CKYItem)modificandItems.next();
		if (!modificandItem.stop() &&
		    derivationOrderOK(modificandItem, modifierSide)) {
		/*
		if (!modificandItem.stop()) {
		*/
		  if (debugComplete)
		    System.err.println(className +
				       ".complete: trying to modify\n\t" +
				       modificandItem + "\n\twith\n\t" +
				       modifierItem);
		  joinItems(modificandItem, modifierItem, modifierSide);
		}
	      }
	    }
	  }
	}
      }
    }
    addUnariesAndStopProbs(start, end);
    chart.prune(start, end);
  }

  /**
   * Enforces that modificand receives all its right modifiers before receiving
   * any left modifiers, by ensuring that right-modification only happens
   * when a modificand has no left-children (this is both necessary and
   * sufficient to enforce derivation order).  Also, in the case of
   * left-modification, this method checks to make sure that the right subcat
   * is empty (necessary but <i>not</i> sufficient to enforce derivation order).
   * This method is called by {@link #complete(int,int)}.
   */
  protected boolean derivationOrderOK(CKYItem modificand, boolean modifySide) {
    return (modifySide == Constants.LEFT ?
	    modificand.rightSubcat().empty() :
	    modificand.leftChildren() == null);
  }

  /**
   * Joins two chart items, one representing the modificand that has not
   * yet received its stop probabilities, the other representing the modifier
   * that has received its stop probabilities.
   *
   * @param modificand the chart item representing a partially-completed
   * subtree, to be modified on <code>side</code> by <code>modifier</code>
   * @param modifier the chart item representing a completed subtree that
   * will be added as a modifier on <code>side</code> of
   * <code>modificand</code>'s subtree
   * @param side the side on which to attempt to add the specified modifier
   * to the specified modificand
   */
  protected void joinItems(CKYItem modificand, CKYItem modifier,
			   boolean side)
  throws RemoteException {
    Symbol modLabel = (Symbol)modifier.label();

    Subcat thisSideSubcat = (Subcat)modificand.subcat(side);
    Subcat oppositeSideSubcat = modificand.subcat(!side);
    boolean thisSideSubcatContainsMod = thisSideSubcat.contains(modLabel);
    if (!thisSideSubcatContainsMod &&
	Language.training.isArgumentFast(modLabel))
      return;

    if (isomorphicTreeConstraints) {
      if (modificand.getConstraint().isViolatedByChild(modifier)) {
	if (debugConstraints)
	  System.err.println("constraint " + modificand.getConstraint() +
			     " violated by child item(" +
			    modifier.start() + "," + modifier.end() + "): " +
			    modifier);
	return;
      }
    }

    /*
    SexpList thisSidePrevMods = getPrevMods(modificand,
					    modificand.prevMods(side),
					    modificand.children(side));
    */
    /*
    SexpList thisSidePrevMods = modificand.prevMods(side);
    */
    tmpChildrenList.set(modifier, modificand.children(side));

    SexpList thisSidePrevMods = getPrevMods(modificand, tmpChildrenList);
    SexpList oppositeSidePrevMods = modificand.prevMods(!side);

    WordList previousWords = getPrevModWords(modificand, tmpChildrenList, side);

    int thisSideEdgeIndex = modifier.edgeIndex(side);
    int oppositeSideEdgeIndex = modificand.edgeIndex(!side);

    boolean thisSideContainsVerb =
      modificand.verb(side) || modifier.containsVerb();
    boolean oppositeSideContainsVerb = modificand.verb(!side);

    ModifierEvent modEvent = lookupModEvent;
    modEvent.set(modifier.headWord(),
		 modificand.headWord(),
		 modLabel,
		 thisSidePrevMods,
		 previousWords,
		 (Symbol)modificand.label(),
		 modificand.headLabel(),
		 modificand.subcat(side),
		 modificand.verb(side),
		 side);

    boolean debugFlag = false;
    if (debugJoin) {
      Symbol modificandLabel = (Symbol)modificand.label();
      boolean modificandLabelP = modificandLabel == S;
      boolean modLabelP = modLabel == NPA;
      debugFlag = ((side == Constants.LEFT &&
		    modificandLabelP && modLabelP &&
		    modificand.start() == 1 && modificand.end() == 2)
		  ||
		  (side == Constants.RIGHT &&
		   modificandLabelP &&
		   modificand.start() == 0 && modificand.end() == 2));
      /*
      if (debugFlag)
	Debug.level = 21;
      */
    }

    if (!futurePossible(modEvent, side, debugFlag))
      if (hardConstraints)
	return;

    if (debugJoin) {
    }

    int lowerIndex = Math.min(thisSideEdgeIndex, oppositeSideEdgeIndex);
    int higherIndex = Math.max(thisSideEdgeIndex, oppositeSideEdgeIndex);

    double logModProb = server.logProbMod(id, modEvent);

    if (logModProb <= logOfZero) {
      if (hardConstraints) {
	if (debugFlag) {
	  System.err.println(className +
			     ".join: couldn't join because logProbMod=logOfZero");
	}
	Debug.level = 0;
	return;
      }
      else
	logModProb = Constants.logProbSmall;
    }

    double logTreeProb =
      modificand.logTreeProb() + modifier.logTreeProb() + logModProb;

    double logPrior = modificand.logPrior();
    double logProb = logTreeProb + logPrior;

    if (debugJoin) {
      if (debugFlag) {
	System.err.println(className + ".join: trying to extend modificand\n" +
			   modificand + "\nwith modifier\n" + modifier);
	System.err.println("where logModProb=" + logModProb);
      }
    }

    // if this side's subcat contains the the current modifier's label as one
    // of its requirements, make a copy of it and remove the requirement
    if (thisSideSubcatContainsMod) {
      thisSideSubcat = (Subcat)thisSideSubcat.copy();
      thisSideSubcat.remove(modLabel);
    }

    SLNode thisSideChildren = new SLNode(modifier, modificand.children(side));
    SLNode oppositeSideChildren = modificand.children(!side);

    CKYItem newItem = chart.getNewItem();
    newItem.set((Symbol)modificand.label(), modificand.headWord(),
		null, null, modificand.headChild(), null, null, null, null,
		lowerIndex, higherIndex, false, false, false,
		logTreeProb, logPrior, logProb);

    tmpChildrenList.set(null, thisSideChildren);
    SexpList thisSideNewPrevMods = getPrevMods(modificand, tmpChildrenList);

    newItem.setSideInfo(side,
			thisSideSubcat, thisSideChildren,
			thisSideNewPrevMods, thisSideEdgeIndex,
			thisSideContainsVerb);
    newItem.setSideInfo(!side,
			oppositeSideSubcat, oppositeSideChildren,
			oppositeSidePrevMods, oppositeSideEdgeIndex,
			oppositeSideContainsVerb);

    if (isomorphicTreeConstraints) {
      if (debugConstraints)
	System.err.println("assigning partially-satisfied constraint " +
			   modificand.getConstraint() + " to " + newItem);
      newItem.setConstraint(modificand.getConstraint());
    }
    newItem.hasAntecedent(modificand);
    newItem.hasAntecedent(modifier);

    boolean added = chart.add(lowerIndex, higherIndex, newItem);
    if (!added) {
      chart.reclaimItem(newItem);
      if (debugFlag)
	System.err.println(className + ".join: couldn't add item");
    }

    if (debugJoin) {
      Debug.level = 0;
    }
  }

  private boolean futurePossible(ModifierEvent modEvent, boolean side,
                                 boolean debug) {
    if (useSimpleModNonterminalMap)
      return futurePossibleSimple(modEvent, side, debug);
    else
      return futurePossibleComplex(modEvent, side, debug);
  }
  private boolean futurePossibleSimple(ModifierEvent modEvent, boolean side,
                                   boolean debug) {
    // first try simpleModNonterminalMap
    Symbol arglessParent =
      Language.training().removeArgAugmentation(modEvent.parent());
    Symbol gaplessHead =
      (Symbol)Language.training().removeGapAugmentation(modEvent.head());
    parentHeadSideLookupList.set(0, arglessParent);
    parentHeadSideLookupList.set(1, gaplessHead);
    parentHeadSideLookupList.set(2, Constants.sideToSym(side));

    Set possiblePartiallyLexedMods =
      (Set)simpleModNonterminalMap.get(parentHeadSideLookupList);
    if (possiblePartiallyLexedMods == null) {
      if (debug)
        System.err.println(className + ".futurePossible: simplified history " +
                           "context " + parentHeadSideLookupList +
                           " not seen in training");
      return false;
    }
    else {
      partiallyLexedModLookupList.set(0, modEvent.modifier());
      partiallyLexedModLookupList.set(1, modEvent.modHeadWord().tag());

      boolean retval =
        possiblePartiallyLexedMods.contains(partiallyLexedModLookupList);
      if (debug && retval == false)
        System.err.println(className + ".futurePossible: future " +
                           partiallyLexedModLookupList + " not seen with " +
                           "simplified history context " +
                           parentHeadSideLookupList + " in training");
      return retval;
    }
  }

  private boolean futurePossibleComplex(ModifierEvent modEvent, boolean side,
                                        boolean debug) {
    ProbabilityStructure modPS = modNonterminalPS;
    int lastLevel = modNonterminalPSLastLevel;
    Event historyContext = modPS.getHistory(modEvent, lastLevel);
    Set possibleFutures = (Set)modNonterminalMap.get(historyContext);
    if (possibleFutures != null) {
      Event currentFuture = modPS.getFuture(modEvent, lastLevel);
      if (possibleFutures.contains(currentFuture)) {
	/*
	if (debug)
	  System.err.println(className + ".futurePossible: future " +
			     currentFuture + " FOUND for history context " +
			     historyContext);
	*/
	return true;
      }
    }
    else {
      //no possible futures for history context
    }

    if (debug) {
      Event currentFuture = modPS.getFuture(modEvent, lastLevel);
      if (possibleFutures == null)
	System.err.println(className + ".futurePossible: history context " +
			   historyContext + " not seen in training");
      else if (!possibleFutures.contains(currentFuture))
	System.err.println(className + ".futurePossible: future " +
			   currentFuture + " not found for history context " +
			   historyContext);
    }

    return false;
  }

  private Set possibleFutures(ModifierEvent modEvent, boolean side) {
    ProbabilityStructure modPS = modNonterminalPS;
    int lastLevel = modNonterminalPSLastLevel;
    boolean onLeft = side == Constants.LEFT;
    Event historyContext = modPS.getHistory(modEvent, lastLevel);
    Set possibleFutures = (Set)modNonterminalMap.get(historyContext);
    return possibleFutures;
  }

  /**
   * Finds all possible parent-head (or <i>unary</i>) productions using the root
   * node of each existing chart item within the specified span as the head,
   * creates new items based on these existing items, multiplying in the
   * parent-head probability; then, using these new items, this method also
   * creates additional new items in which stop probabilities have been
   * multiplied; all new items are added to the chart. Stop probabilities are
   * the probabilities associated with generating {@link Training#stopSym()} as
   * a modifier on either side of a production.
   *
   * @param start the index of the first word in the span
   * @param end   the index of the last word in the span
   * @throws RemoteException
   * @see #addUnaries(CKYItem, java.util.List)
   * @see #addStopProbs(CKYItem, java.util.List)
   */
  protected void addUnariesAndStopProbs(int start, int end)
  throws RemoteException {
    prevItemsAdded.clear();
    currItemsAdded.clear();
    stopProbItemsToAdd.clear();

    Iterator it = chart.get(start, end);
    while (it.hasNext()) {
      CKYItem item = (CKYItem)it.next();
      if (item.stop() == false)
	stopProbItemsToAdd.add(item);
      else if (item.isPreterminal())
	prevItemsAdded.add(item);
    }

    if (stopProbItemsToAdd.size() > 0) {
      it = stopProbItemsToAdd.iterator();
      while (it.hasNext())
	addStopProbs((CKYItem)it.next(), prevItemsAdded);
    }

    int i = -1;
    //for (i = 0; i < 5 && prevItemsAdded.size() > 0; i++) {
    for (i = 0; prevItemsAdded.size() > 0; i++) {
      Iterator prevItems = prevItemsAdded.iterator();
      while (prevItems.hasNext()) {
	CKYItem item = (CKYItem)prevItems.next();
	if (!item.garbage())
	  addUnaries(item, currItemsAdded);
      }

      exchangePrevAndCurrItems();
      currItemsAdded.clear();

      prevItems = prevItemsAdded.iterator();
      while (prevItems.hasNext()) {
	CKYItem item = (CKYItem)prevItems.next();
	if (!item.garbage())
	  addStopProbs(item, currItemsAdded);
      }
      exchangePrevAndCurrItems();
      currItemsAdded.clear();
    }
    if (debugUnariesAndStopProbs) {
      System.err.println(className +
			 ": added unaries and stop probs " + i + " times");
    }
  }

  private final void exchangePrevAndCurrItems() {
    List exchange;
    exchange = prevItemsAdded;
    prevItemsAdded = currItemsAdded;
    currItemsAdded = exchange;
  }

  /**
   * Finds all possible parent-head (or <i>unary</i>) productions using the root
   * node of the specified chart item as the head, creates new items based on
   * the specified item, multiplying in the parent-head probability.  All new
   * items are added to the chart; those that are successfully added are also
   * stored in the specified <code>itemsAdded</code> list.
   *
   * @param item       the item for which unary productions are to be added
   * @param itemsAdded an empty list in which all new chart items will be
   *                   stored
   * @return the specified <code>itemsAdded</code> list having been modified
   *
   * @throws RemoteException
   */
  protected List addUnaries(CKYItem item, List itemsAdded)
  throws RemoteException {
    // get possible parent nonterminals
    Symbol[] nts;
    if (useHeadToParentMap) {
      nts = (Symbol[])headToParentMap.get(item.label());
      // this item's root label was ONLY seen as a modifier
      if (nts == null)
	return itemsAdded;
    }
    else
      nts = nonterminals;

    unaryItemsToAdd.clear();
    CKYItem newItem = chart.getNewItem();
    // set some values now, most to be filled in by code below
    newItem.set(null, item.headWord(), null, null, item,
		null, null, startList, startList,
		item.start(), item.end(),
		false, false, false, 0.0, 0.0, 0.0);
    newItem.hasAntecedent(item);
    Symbol headSym = (Symbol)item.label();
    HeadEvent headEvent = lookupHeadEvent;
    headEvent.set(item.headWord(), null, headSym, emptySubcat, emptySubcat);
    PriorEvent priorEvent = lookupPriorEvent;
    priorEvent.set(item.headWord(), null);
    int numNTs = nts.length;
    // foreach possible parent nonterminal
    for (int ntIndex = 0; ntIndex < numNTs; ntIndex++) {
      Symbol parent = nts[ntIndex];
      headEvent.setParent(parent);
      Subcat[] leftSubcats = getPossibleSubcats(leftSubcatMap, headEvent,
						leftSubcatPS,
						leftSubcatPSLastLevel);
      Subcat[] rightSubcats = getPossibleSubcats(rightSubcatMap, headEvent,
						 rightSubcatPS,
						 rightSubcatPSLastLevel);
      if (debugUnaries) {
	if (item.start() == 13 && item.end() == 20 &&
	    headSym == VP && parent == SA) {
	  System.err.println(className + ".addUnaries: trying to build on " +
			     headSym + " with " + parent);
	}
      }

      int numLeftSubcats = leftSubcats.length;
      int numRightSubcats = rightSubcats.length;
      if (numLeftSubcats > 0 && numRightSubcats > 0) {
	// foreach possible right subcat
	for (int rightIdx = 0; rightIdx < numRightSubcats; rightIdx++) {
	  Subcat rightSubcat = (Subcat)rightSubcats[rightIdx];
	  // foreach possible left subcat
	  for (int leftIdx = 0; leftIdx < numLeftSubcats; leftIdx++) {
	    Subcat leftSubcat = (Subcat)leftSubcats[leftIdx];

	    newItem.setLabel(parent);
	    newItem.setLeftSubcat(leftSubcat);
	    newItem.setRightSubcat(rightSubcat);

	    headEvent.setLeftSubcat(leftSubcat);
	    headEvent.setRightSubcat(rightSubcat);

	    if (debugUnaries) {
	      if (item.start() == 13 && item.end() == 20 &&
		  headSym == VP && parent == SA) {
		System.err.println(className + ".addUnaries: trying to " +
				   "build on " + headSym + " with " + parent +
				   " and left subcat " + leftSubcat +
				   " and right subcat " + rightSubcat);
	      }
	    }

	    if (isomorphicTreeConstraints) {
	      // get head child's constraint's parent and check that it is
	      // locally satisfied by newItem
	      if (item.getConstraint() == null) {
		System.err.println("uh-oh: no constraint for item " + item);
	      }
	      Constraint headChildParent = item.getConstraint().getParent();
	      if (headChildParent != null &&
		  headChildParent.isLocallySatisfiedBy(newItem)) {
		if (debugConstraints)
		  System.err.println("assigning locally-satisfied constraint " +
				     headChildParent + " to " + newItem);
		newItem.setConstraint(headChildParent);
	      }
	      else {
		if (debugConstraints)
		  System.err.println("constraint " + headChildParent +
				     " is not locally satisfied by item " +
				     newItem);
		continue;
	      }
	    }
	    else if (findAtLeastOneSatisfyingConstraint) {
	      Constraint constraint = constraints.constraintSatisfying(newItem);
	      if (constraint == null)
		continue;
	      else
		newItem.setConstraint(constraint);
	    }

	    double logProbLeftSubcat =
	      (numLeftSubcats == 1 ? logProbCertain :
	       server.logProbLeftSubcat(id, headEvent));
	    double logProbRightSubcat =
	      (numRightSubcats == 1 ? logProbCertain :
	       server.logProbRightSubcat(id, headEvent));
	    double logProbHead = server.logProbHead(id, headEvent);
	    if (logProbHead <= logOfZero) {
	      if (hardConstraints)
		continue;
	      else
	        logProbHead = Constants.logProbSmall;
	    }
	    double logTreeProb =
	      item.logTreeProb() +
	      logProbHead + logProbLeftSubcat + logProbRightSubcat;

	    priorEvent.setLabel(parent);
	    double logPrior = server.logPrior(id, priorEvent);

	    if (logPrior <= logOfZero) {
	      if (hardConstraints)
		continue;
	      else
		logPrior = Constants.logProbSmall;
	    }

	    double logProb = logTreeProb + logPrior;

	    if (debugUnaries) {
	      if (item.start() == 13 && item.end() == 20 &&
		  headSym == VP && parent == SA &&
		  leftSubcat.size() == 0 && rightSubcat.size() == 0) {
		String msg =
		  className + ".addUnaries: logprobs: lc=" +
		  logProbLeftSubcat + "; rc=" +
		  logProbRightSubcat + "; head=" + logProbHead +
		  "; tree=" + logTreeProb + "; prior=" + logPrior;
		System.err.println(msg);
	      }
	    }

	    if (logProb <= logOfZero) {
	      if (hardConstraints)
		continue;
	      else
		logProb = Constants.logProbSmall;
	    }

	    newItem.setLogTreeProb(logTreeProb);
	    newItem.setLogPrior(logPrior);
	    newItem.setLogProb(logProb);

	    CKYItem newItemCopy = chart.getNewItem();
	    newItemCopy.setDataFrom(newItem);
	    unaryItemsToAdd.add(newItemCopy);
	  }
	} // end foreach possible left subcat
      }
    }
    if (unaryItemsToAdd.size() > 0) {
      Iterator toAdd = unaryItemsToAdd.iterator();
      while (toAdd.hasNext()) {
	CKYItem itemToAdd = (CKYItem)toAdd.next();
	boolean added = chart.add(itemToAdd.start(), itemToAdd.end(), itemToAdd);
	if (added)
	  itemsAdded.add(itemToAdd);
	else
	  chart.reclaimItem(itemToAdd);
      }
    }
    chart.reclaimItem(newItem);

    return itemsAdded;
  }

  /**
   * Gets all possible {@link Subcat}s for the context contained in the
   * specified {@link HeadEvent}.
   *
   * @param subcatMap the map of contexts to sets of possible {@link Subcat}
   *                  objects (each set is an array of {@link Subcat})
   * @param headEvent the head event for whose context possible subcats are to
   *                  be gotten
   * @param subcatPS  the probability structure for generating subcats
   * @param lastLevel the last level of back-off for the specified subcat
   *                  probability structure
   * @return all possible {@link Subcat}s for the context contained in the
   *         specified {@link HeadEvent}
   */
  protected final Subcat[] getPossibleSubcats(Map subcatMap, HeadEvent headEvent,
					    ProbabilityStructure subcatPS,
					    int lastLevel) {
    Event lastLevelHist = subcatPS.getHistory(headEvent, lastLevel);
    Subcat[] subcats = (Subcat[])subcatMap.get(lastLevelHist);
    return subcats == null ? zeroSubcatArr : subcats;
  }

  /**
   * Adds stop probabilities to the specified item and adds these items to the
   * chart; as a side effect, all items successfully added to the chart are also
   * stored in the specified <code>itemsAdded</code> list.  Stop probabilities
   * are the probabilities associated with generating {@link Training#stopSym()}
   * as a modifier on either side of a production.
   *
   * @param item       the item for which stop probabilites are to be added,
   *                   creating a new &ldquo;stopped&rdquo; item
   * @param itemsAdded a list into which chart items added by this method are to
   *                   be stored
   * @return the specified <code>itemsAdded</code> list, modified by this
   *         method
   *
   * @throws RemoteException
   */
  protected List addStopProbs(CKYItem item, List itemsAdded)
    throws RemoteException {
    if (!(item.leftSubcat().empty() && item.rightSubcat().empty()))
      return itemsAdded;

    /*
    SexpList leftPrevMods =
      getPrevMods(item, item.leftPrevMods(), item.leftChildren());
    SexpList rightPrevMods =
      getPrevMods(item, item.rightPrevMods(), item.rightChildren());
    */

    // technically, we should getPrevMods for both lists here, but there
    // shouldn't be skipping of previous mods because of generation of stopSym
    SexpList leftPrevMods = item.leftPrevMods();
    SexpList rightPrevMods = item.rightPrevMods();

    tmpChildrenList.set(null, item.leftChildren());
    WordList leftPrevWords = getPrevModWords(item, tmpChildrenList,
					     Constants.LEFT);
    tmpChildrenList.set(null, item.rightChildren());
    WordList rightPrevWords = getPrevModWords(item, tmpChildrenList,
					      Constants.RIGHT);

    ModifierEvent leftMod = lookupLeftStopEvent;
    leftMod.set(stopWord, item.headWord(), stopSym, leftPrevMods,
		leftPrevWords,
		(Symbol)item.label(), item.headLabel(), item.leftSubcat(),
		item.leftVerb(), Constants.LEFT);
    ModifierEvent rightMod = lookupRightStopEvent;
    rightMod.set(stopWord, item.headWord(), stopSym, rightPrevMods,
		 rightPrevWords,
		 (Symbol)item.label(), item.headLabel(),
		 item.rightSubcat(), item.rightVerb(), Constants.RIGHT);

    if (debugStops) {
      if (item.start() == 0 && item.end() == 17 && item.label() == NPA) {
	System.err.println(className + ".addStopProbs: trying to add stop " +
			   "probs to item " + item);
      }
    }

    if (isomorphicTreeConstraints) {
      if (!item.getConstraint().isSatisfiedBy(item)) {
	if (debugConstraints)
	  System.err.println("constraint " + item.getConstraint() +
			     " is not satisfied by item " + item);
	return itemsAdded;
      }
    }

    double leftLogProb = server.logProbMod(id, leftMod);
    if (leftLogProb <= logOfZero) {
      if (hardConstraints)
	return itemsAdded;
      else
	leftLogProb = Constants.logProbSmall;
    }
    double rightLogProb = server.logProbMod(id, rightMod);
    if (rightLogProb <= logOfZero) {
      if (hardConstraints)
	return itemsAdded;
      else
	rightLogProb = Constants.logProbSmall;
    }
    double logTreeProb =
      item.logTreeProb() + leftLogProb + rightLogProb;

    double logPrior = item.logPrior();
    double logProb = logTreeProb + logPrior;

    if (debugStops) {
      if (item.start() == 0 && item.end() == 17 && item.label() == NPA) {
	System.err.println(className + ".addStopProbs: adding stops to item " +
			   item);
	System.err.println("\trightLogProb=" + rightLogProb +
			   "; leftLogProb=" + leftLogProb);
      }
    }

    if (logProb <= logOfZero) {
      if (hardConstraints)
	return itemsAdded;
      else
	logProb = Constants.logProbSmall;
    }

    CKYItem newItem = chart.getNewItem();
    newItem.set((Symbol)item.label(), item.headWord(),
		item.leftSubcat(), item.rightSubcat(),
		item.headChild(),
		item.leftChildren(), item.rightChildren(),
		item.leftPrevMods(), item.rightPrevMods(),
		item.start(), item.end(), item.leftVerb(),
		item.rightVerb(), true, logTreeProb, logPrior, logProb);

    if (isomorphicTreeConstraints) {
      if (debugConstraints)
	System.err.println("assigning satisfied constraint " +
			   item.getConstraint() + " to " + newItem);
      newItem.setConstraint(item.getConstraint());
    }

    newItem.hasAntecedent(item);
    boolean added = chart.add(item.start(), item.end(), newItem);
    if (added)
      itemsAdded.add(newItem);
    else {
      if (debugStops) {
	if (item.start() == 0 && item.end() == 17 && item.label() == NPA) {
	  System.err.println(className + ".addStopProbs: couldn't add item");
	}
      }
      chart.reclaimItem(newItem);
    }

    return itemsAdded;
  }

  /**
   * Creates a new previous-modifier list given the specified current list
   * and the last modifier on a particular side.
   *
   * @param item the item for which a previous-modifier list is to be
   * constructed
   * @param modChildren the last node of modifying children on a particular
   * side of the head of a chart item
   * @return the list whose first element is the label of the specified
   * modifying child and whose subsequent elements are those of the
   * specified <code>itemPrevMods</code> list, without its final element
   * (which is "bumped off" the edge, since the previous-modifier list
   * has a constant length)
   */
  protected SexpList getPrevMods(CKYItem item, SLNode modChildren) {
    if (modChildren == null)
      return startList;
    prevModLookupList.clear();
    SexpList prevMods = prevModLookupList;
    int i = 0; // index in prev mod list we are constructing
    // as long as there are children and we haven't reached the numPrevMods
    // limit, set elements of prevModList, starting at index 0
    for (SLNode curr = modChildren; curr != null && i < numPrevMods; ) {
      Symbol prevMod = (curr.next() == null ? startSym :
			(Symbol)((CKYItem)curr.next().data()).label());
      if (!Shifter.skip(item, prevMod)) {
	prevMods.add(prevMod);
	i++;
      }
      curr = curr.next();
    }

    // if, due to skipping, we haven't finished setting prevModList, finish here
    if (i == 0)
      return startList;
    for (; i < numPrevMods; i++)
      prevMods.add(startSym);

    SexpList canonical = (SexpList)canonicalPrevModLists.get(prevMods);
    if (canonical == null) {
      canonical = (SexpList)prevMods.deepCopy();
      canonicalPrevModLists.put(canonical, canonical);
    }
    return canonical;
  }

  /**
   * Creates a new previous-modifier word list given the specified current list
   * and the last modifier on a particular side.
   *
   * @param item        the item for which a previous-modifier list is to be
   *                    constructed
   * @param modChildren the last node of modifying children on a particular side
   *                    of the head of a chart item
   * @param side        the side of the specified item's head child on which the
   *                    specified modifier children occur
   * @return the list whose first element is the head word of the specified
   *         modifying child and whose subsequent elements are those of the
   *         specified <code>itemPrevMods</code> list, without its final element
   *         (which is "bumped off" the edge, since the previous-modifier list
   *         has a constant length)
   */
  protected WordList getPrevModWords(CKYItem item, SLNode modChildren,
				     boolean side) {
    if (modChildren == null)
      return startWordList;
    WordList wordList =
      side == LEFT ? prevModWordLeftLookupList : prevModWordRightLookupList;
    int i = 0; // the index of the previous mod head wordlist
    // as long as there are children and we haven't reached the numPrevWords
    // limit, set elements of wordList, starting at index 0 (i = 0, initially)
    for (SLNode curr = modChildren; curr!=null && i < numPrevWords;) {
      Word prevWord = (curr.next() == null ? startWord :
		       (Word)((CKYItem)curr.next().data()).headWord());
      if (!Shifter.skip(item, prevWord))
	wordList.set(i++, prevWord);
      curr = curr.next();
    }
    // if we ran out of children, but haven't finished setting all numPrevWords
    // elements of word list, set remainder of word list with startWord
    if (i == 0)
      return startWordList;
    for ( ; i < numPrevWords; i++)
      wordList.set(i, startWord);

    return wordList;
  }

  /**
   * There is a comma contraint violation if the word at the split point
   * is a comma and there exists a word following <code>end</code> and that
   * word is not a comma and when it is not the case that the word at
   * <code>end</code> is <i>not</i> a conunction.  The check for a conjunction
   * is to allow chart items representing partial derivations of the form
   * <blockquote>
   * <tt>P</tt>&nbsp;&rarr;&nbsp;<i>&alpha;&nbsp;&beta;&nbsp;&gamma;</i>
   * </blockquote>
   * where
   * <ul>
   * <li><i>&alpha;</i> is a sequence of nonterminals,
   * <li><i>&beta;</i> is a single nonterminal that is the comma preterminal,
   *     as defined by {@link Treebank#isComma(Symbol)} and
   * <li><i>&gamma;</i> is a single nonterminal that is a conjunction
   *     preterminal, as defined by {@link Treebank#isConjunction(Symbol)}.
   * </ul>
   * <p/>
   * In the English Penn Treebank, the concrete form of this partial
   * derivation would be
   * <blockquote>
   * <tt>P</tt>&nbsp;&rarr;&nbsp;<i>&alpha;</i>&nbsp;<tt>,&nbsp;CC</tt>
   * </blockquote>
   * <p/>
   * This addition to Mike Collins&rsquo; definition of the comma constraint
   * was necessary because, unlike in Collins' parser, commas and conjunctions
   * are generated in two separate steps.
   */
  protected final boolean commaConstraintViolation(int start,
						   int split,
						   int end) {
    /*
    return (Language.treebank.isComma(getSentenceWord(split)) &&
	    end < sentLen - 1 &&
	    !Language.treebank.isComma(getSentenceWord(end + 1)));
    */
    return (commaForPruning[split] &&
	    end < sentLen - 1 &&
	    !commaForPruning[end + 1] &&
	    !conjForPruning[end]);
  }

  private final Symbol getSentenceWord(int index) {
    return (index >= sentLen ? null :
	    (sentence.get(index).isSymbol() ? sentence.symbolAt(index) :
	     sentence.listAt(index).symbolAt(1)));

  }

  public void update(Map<String, String> changedSettings) {
    maxSentLen = Settings.getInteger(Settings.maxSentLen);
    kBest =
      Math.max(1, Settings.getInteger(Settings.kBest));
    maxParseTime = Settings.getInteger(Settings.maxParseTime);
    relaxConstraints =
      Settings.getBoolean(Settings.decoderRelaxConstraintsAfterBeamWidening);
    restorePrunedWords =
      Settings.getBoolean(Settings.restorePrunedWords);
    downcaseWords = Settings.getBoolean(Settings.downcaseWords);
    useLowFreqTags =
      Settings.getBoolean(Settings.useLowFreqTags);
    substituteWordsForClosedClassTags =
      Settings.getBoolean(Settings.decoderSubstituteWordsForClosedClassTags);
    useOnlySuppliedTags =
      Settings.getBoolean(Settings.decoderUseOnlySuppliedTags);
    useHeadToParentMap =
      Settings.getBoolean(Settings.decoderUseHeadToParentMap);
    useSimpleModNonterminalMap =
      Settings.getBoolean(Settings.useSimpleModNonterminalMap);
    numPrevMods = Settings.getInteger(Settings.numPrevMods);
    numPrevWords = Settings.getInteger(Settings.numPrevWords);
    keepAllWords = Settings.getBoolean(Settings.keepAllWords);
    dontPostProcess =
      Settings.getBoolean(Settings.decoderDontPostProcess) ||
      Settings.getBoolean(Settings.decoderOutputInsideProbs);
    constructorHelper(server);
    startSym = Language.training().startSym();
    startWord = Language.training().startWord();
    stopSym = Language.training().stopSym();
    stopWord = Language.training().stopWord();
    topSym = Language.training().topSym();
  }
}
