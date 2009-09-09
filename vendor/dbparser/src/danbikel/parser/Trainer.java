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
import danbikel.util.*;
import danbikel.util.AbstractMapToPrimitive.*;
import danbikel.parser.util.Util;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.HashMap;


/**
 * Derives all counts necessary to compute the probabilities for this parser,
 * including the top-level counts and all derived counts.  The two additional
 * facilities of this class are (1) the loading and storing of a text file
 * containing top-level event counts and (2) the loading and storing of a Java
 * object file containing all derived event counts.
 * <p/>
 * All top-level events or mappings are recorded as S-expressions with the
 * format
 * <pre>(name event count)</pre>
 * for events and
 * <pre>(name key value)</pre>
 * for mappings.
 * <p/>
 * All derived counts are stored by the internal data structures of several
 * <code>Model</code> objects, which are in turn all contained within a single
 * <code>ModelCollection</code> object.  This class provides methods to load and
 * store a Java object file containing this <code>ModelCollection</code>, as
 * well as some initial objects containing information about the
 * <code>ModelCollection</code> object (see the <tt>-scan</tt> flag in the usage
 * of {@link #main the main method of this class}).
 * <p/>
 * The various model objects capture the generation submodels of the different
 * output elements of the parser.  The smoothing levels of these submodels are
 * represented by <code>ProbabilityStructure</code> objects, passed as
 * parameters to the <code>Model</code> objects, at {@link
 * Model#Model(ProbabilityStructure) construction} time.  This architecture
 * provides a type of "plug-n-play" smoothing scheme for the various submodels
 * of this parser.
 *
 * @see #main(String[])
 * @see Model
 * @see ModelCollection
 * @see ProbabilityStructure
 */
public class Trainer implements Serializable {
  // constants
  //static boolean secretFlag;

  private final static Symbol collinsTop = Symbol.add("TOP");
  /**
   * One must set the unknownWordThreshold to 1 in order to perfectly emulate
   * Collins' trainer's output file.
   */
  private static boolean outputCollins =
    Settings.getBoolean(Settings.outputCollins);
  /** This should only be true if training on the 39832 sentences of
      the WSJ Penn Treebank, Sections 02-21 (and if emulation of Mike
      Collins' trainer is desired). */
  private static boolean useCollinsSkipArr =
    Settings.getBoolean(Settings.collinsSkipWSJSentences);

  private static boolean shareCounts =
    Settings.getBoolean(Settings.trainerShareCounts);

  private static boolean addGapInfo =
    Settings.getBoolean(Settings.addGapInfo);

  /** The sentence numbers of sentences that Mike Collins' trainer skips,
      due to a strange historical reason of a pre-processing Perl script
      of his. */
  private final static int[] collinsSkipArr =
    {167, 557, 581, 687, 698, 863, 914, 1358, 1406, 1869, 1873, 1884, 1887,
     2617, 2700, 2957, 3241, 3939, 3946, 3959, 4613, 4645, 4669, 5021, 5312,
     5401, 6151, 6161, 6165, 6173, 6340, 6342, 6347, 6432, 6704, 6850, 7162,
     7381, 7778, 7941, 8053, 8076, 8229, 10110, 10525, 10676, 11361, 11593,
     11716, 11727, 11737, 12286, 12871, 12902, 13182, 13409, 13426, 13868,
     13909, 13918, 14252, 14255, 16488, 16489, 16822, 17112, 17566, 17644,
     18414, 19663, 20105, 20213, 20308, 20653, 22565, 23053, 23226, 23483,
     24856, 24928, 24930, 25179, 25193, 25200, 26821, 26967, 27051, 27862,
     28081, 28680, 28827, 29254, 29261, 29348, 30110, 30142, 31287, 31739,
     31940, 32001, 32010, 32015, 32378, 34173, 34544, 34545, 34573, 35105,
     35247, 35390, 35865, 35868, 36281, 37653, 38403, 38545, 39182, 39197,
     39538, 39695};

  private final static String className = Trainer.class.getName();

  /**
   * The class from which an instance will be constructed in
   * {@link #main(String[])}.  This data member may be re-assigned in
   * a subclass' <code>main</code> method before invocation of this
   * class' <code>main</code> method, so that all trainer method
   * invocations done by this class' <code>main</code> method will be on
   * an instance of the subclass.
   */
  protected static Class trainerClass = Trainer.class;

  // constants for default classname prefixes for ProbabilityStructure
  // subclasses
  private static String packagePrefix =
    Settings.get(Settings.modelStructurePackage) + ".";
  private static String lexPriorModelStructureClassnamePrefix =
    packagePrefix + "LexPriorModelStructure";
  private static String nonterminalPriorModelStructureClassnamePrefix =
    packagePrefix + "NonterminalPriorModelStructure";
  private static String topNonterminalModelStructureClassnamePrefix =
    packagePrefix + "TopNonterminalModelStructure";
  private static String topLexModelStructureClassnamePrefix =
    packagePrefix + "TopLexModelStructure";
  private static String headModelStructureClassnamePrefix =
    packagePrefix + "HeadModelStructure";
  private static String gapModelStructureClassnamePrefix =
    packagePrefix + "GapModelStructure";
  private static String leftSubcatModelStructureClassnamePrefix =
    packagePrefix + "LeftSubcatModelStructure";
  private static String rightSubcatModelStructureClassnamePrefix =
    packagePrefix + "RightSubcatModelStructure";
  private static String modNonterminalModelStructureClassnamePrefix =
    packagePrefix + "ModNonterminalModelStructure";
  private static String modWordModelStructureClassnamePrefix =
    packagePrefix + "ModWordModelStructure";

  static {
    Settings.Change change = new Settings.Change() {
      public void update(Map<String, String> changedSettings) {
	outputCollins =
	  Settings.getBoolean(Settings.outputCollins);
	useCollinsSkipArr =
	  Settings.getBoolean(Settings.collinsSkipWSJSentences);
	shareCounts =
	  Settings.getBoolean(Settings.trainerShareCounts);
	addGapInfo =
	  Settings.getBoolean(Settings.addGapInfo);
	if (changedSettings.containsKey(Settings.modelStructurePackage)) {
	  packagePrefix =
	    Settings.get(Settings.modelStructurePackage) + ".";
	  lexPriorModelStructureClassnamePrefix =
	    packagePrefix + "LexPriorModelStructure";
	  nonterminalPriorModelStructureClassnamePrefix =
	    packagePrefix + "NonterminalPriorModelStructure";
	  topNonterminalModelStructureClassnamePrefix =
	    packagePrefix + "TopNonterminalModelStructure";
	  topLexModelStructureClassnamePrefix =
	    packagePrefix + "TopLexModelStructure";
	  headModelStructureClassnamePrefix =
	    packagePrefix + "HeadModelStructure";
	  gapModelStructureClassnamePrefix =
	    packagePrefix + "GapModelStructure";
	  leftSubcatModelStructureClassnamePrefix =
	    packagePrefix + "LeftSubcatModelStructure";
	  rightSubcatModelStructureClassnamePrefix =
	    packagePrefix + "RightSubcatModelStructure";
	  modNonterminalModelStructureClassnamePrefix =
	    packagePrefix + "ModNonterminalModelStructure";
	  modWordModelStructureClassnamePrefix =
	    packagePrefix + "ModWordModelStructure";
	}
      }
    };
    Settings.register(Trainer.class, change, null);
  }

  // types of events gathered by the trainer (provided as public constants
  // so that users can interpret human-readable trainer output file

  /** The label for nonterminal generation events.  This symbol has the
      print-name <tt>&quot;nonterminal&quot;</tt>. */
  public final static Symbol nonterminalEventSym = Symbol.add("nonterminal");
  /** The label for head nonterminal generation events.  This symbol has the
      print-name <tt>&quot;head&quot;</tt>. */
  public final static Symbol headEventSym = Symbol.add("head");
  /** The label for modifier nonterminal generation events.  This symbol has
      the print-name <tt>&quot;mod&quot;</tt>. */
  public final static Symbol modEventSym = Symbol.add("mod");
  /** The label for gap events.  This symbol has the
      print-name <tt>&quot;gap&quot;</tt>. */
  public final static Symbol gapEventSym = Symbol.add("gap");
  /** The label for word to part-of-speech mappings.  This symbol has the
      print-name <tt>&quot;pos&quot;</tt>. */
  public final static Symbol posMapSym = Symbol.add("pos");
  /** The label for vocabulary counts.  This symbol has the
      print-name <tt>&quot;vocab&quot;</tt>. */
  public final static Symbol vocabSym = Symbol.add("vocab");
  /** The label for word feature (unknown vocabulary) counts.  This
      symbol has the print-name <tt>&quot;word-feature&quot;</tt>. */
  public final static Symbol wordFeatureSym = Symbol.add("word-feature");
  /**
   * The label for the set of pruned preterminals.  This symbol has the
   * print-name <tt>&quot;pruned-preterm&quot;</tt>.
   *
   * @see Training#prune(Sexp)
   */
  public final static Symbol prunedPretermSym = Symbol.add("pruned-preterm");
  /**
   * The label for the set of pruned punctuation preterminals.  This
   * symbol has the print-name <tt>&quot;pruned-preterm&quot;</tt>.
   *
   * @see Training#raisePunctuation(Sexp)
   * @see Training#getPrunedPunctuation()
   */
  public final static Symbol prunedPuncSym = Symbol.add("pruned-punc");

  // integer types for mapping the above symbols to ints for a switch statement
  private final static int nonterminalEventType = 1;
  private final static int headEventType = 2;
  private final static int modEventType = 3;
  private final static int gapEventType = 4;
  private final static int posMapType = 5;
  private final static int vocabType = 6;
  private final static int wordFeatureType = 7;
  private final static int prunedPretermType = 8;
  private final static int prunedPuncType = 9;

  private final static Object[][] eventsToTypesArr = {
    {nonterminalEventSym, new Integer(nonterminalEventType)},
    {headEventSym, new Integer(headEventType)},
    {modEventSym, new Integer(modEventType)},
    {gapEventSym, new Integer(gapEventType)},
    {posMapSym, new Integer(posMapType)},
    {vocabSym, new Integer(vocabType)},
    {wordFeatureSym, new Integer(wordFeatureType)},
    {prunedPretermSym, new Integer(prunedPretermType)},
    {prunedPuncSym, new Integer(prunedPuncType)},
  };

  private final static MapToPrimitive eventsToTypes = new HashMapInt();
  static {
    for (int i = 0; i < eventsToTypesArr.length; i++) {
      Object key = eventsToTypesArr[i][0];
      int value = ((Integer)eventsToTypesArr[i][1]).intValue();
      eventsToTypes.put(key, value);
    }
  }

  // data members

  // settings
  /** The value of the {@link Settings#unknownWordThreshold} setting. */
  protected int unknownWordThreshold;
  /** The value of the {@link Settings#countThreshold} setting. */
  protected double countThreshold;
  /** The value of the {@link Settings#derivedCountThreshold} setting. */
  protected double derivedCountThreshold;
  /** The value of the {@link Settings#trainerReportingInterval} setting. */
  protected int reportingInterval;
  /** The value of the {@link Settings#numPrevMods} setting. */
  protected int numPrevMods;
  /** The value of the {@link Settings#numPrevWords} setting. */
  protected int numPrevWords;
  /** The value of the {@link Settings#keepAllWords} setting. */
  protected boolean keepAllWords;
  /** The value of the {@link Settings#keepLowFreqTags} setting. */
  protected boolean keepLowFreqTags;
  /** *The value of the {@link Settings#downcaseWords} setting. */
  protected boolean downcaseWords;

  // data storage for training
  /**
   * A table for storing counts of (unlexicalized) nonterminals.  The keys
   * are instances of {@link Symbol}.
   */
  protected CountsTable nonterminals = new CountsTableImpl();
  /**
   * A table for storing counts of lexicalized nonterminal prior events.
   * The keys are instances of {@link PriorEvent}.
   */
  protected CountsTable priorEvents = new CountsTableImpl();
  /**
   * A table for storing counts of head-generation events. The keys are
   * instances of {@link HeadEvent}.
   */
  protected CountsTable headEvents = new CountsTableImpl();
  /**
   * A table for storing counts of modifier-generation events.  The keys are
   * instances of {@link ModifierEvent}.
   */
  protected CountsTable modifierEvents = new CountsTableImpl();
  /**
   * A table for storing counts of gap-generation events.  The keys are
   * instances of {@link GapEvent}.
   */
  protected CountsTable gapEvents = new CountsTableImpl();
  /**
   * A table for storing counts of vocabulary items.  The keys are instances
   * of {@link Symbol}.
   */
  protected CountsTable vocabCounter = new CountsTableImpl();
  /**
   * A table for storing counts of word feature&ndash;vectors.  The keys
   * are instances of {@link Symbol}.
   */
  protected CountsTable wordFeatureCounter = new CountsTableImpl();
  /**
   * A map of words to lists of their observed part-of-speech tags.
   * The keys in this map are instances of {@link Symbol}, and the values
   * are {@link SexpList} instances that represent sets by containing
   * lists of distinct {@link Symbol} objects.
   */
  protected Map posMap = new HashMap();
  /**
   * A map of head child nonterminals to their observed parent nonterminals.
   * The keys are instances of {@link Symbol}, and the values are {@link Set}
   * instances containing {@link Symbol} objects.
   */
  protected Map headToParentMap = new HashMap();
  /**
   * A map of events from the last back-off level of the left
   * subcat&ndash;generation submodel to the set of possible left subcats.
   * The keys are instnaces of {@link Event}, and the values are {@link Set}
   * instances containing {@link Subcat} objects.
   */
  protected Map leftSubcatMap = new HashMap();
  /**
   * A map of events from the last back-off level of the right
   * subcat&ndash;generation submodel to the set of possible right subcats.
   * The keys are instnaces of {@link Event}, and the values are {@link Set}
   * instances containing {@link Subcat} objects.
   */
  protected Map rightSubcatMap = new HashMap();
  /**
   * A map of events from the last back-off level of the modifier
   * nonterminal&ndash;generation submodel to the set of possible futures
   * (typically, a future is a modifier label and its head word's part-of-speech
   * tag).  The keys are instances of {@link Event}, and the values are {@link
   * Set} instances containing {@link Event} objects.
   */
  protected Map modNonterminalMap = new HashMap();
  /**
   * A map from unlexicalized parent-head-side triples to all possible
   * partially-lexicalized modifying nonterminals.  This map provides a simpler
   * mechanism for determining whether a given modifier is possible in the
   * current parent-head context than is provided by
   * {@link #modNonterminalMap}.
   * <p/>
   * The keys are {@link SexpList} objects containing exactly three
   * {@link Symbol} elements representing the following in a production:
   * <ol>
   * <li>an unlexicalized parent nonterminal
   * <li>an unlexicalized head nonterminal
   * <li>the direction of modification, either {@link Constants#LEFT} or
   * {@link Constants#RIGHT}.
   * </ol>
   * <p/>
   * The values consist of {@link Set} objects containing {@link SexpList}
   * objects that contain exactly two {@link Symbol} elements representing a
   * partially-lexicalized modifying nonterminal:
   * <ol>
   * <li>the unlexicalized modifying nonterminal
   * <li>the part-of-speech tag of the modifying nonterminal's head word.
   * </ol>
   * <p/>
   * An example of a partially-lexicalized nonterminal in the Penn Treebank
   * is <code>NP(NNP)</code>, which is a noun phrase headed by a singular
   * proper noun.
   *
   * @see Settings#useSimpleModNonterminalMap
   */
  protected Map simpleModNonterminalMap = new HashMap();
  /**
   * A set of {@link Sexp} objects representing preterminals that were
   * pruned during training.
   *
   * @see Training#prune(Sexp)
   * @see Treebank#isPreterminal(Sexp)
   */
  protected Set prunedPreterms;
  /**
   * Returns the set of preterminals ({@link Sexp} objects) that were
   * punctuation elements that were &ldquo;raised away&rdquo; because they were
   * either at the beginning or end of a sentence.
   *
   * @see Training#raisePunctuation(Sexp)
   * @see Treebank#isPuncToRaise(Sexp)
   */
  protected Set prunedPunctuation;
  // temporary map for canonicalizing subcat objects for
  // leftSubcatMap and rightSubcatMap data members
  /**
   * A reflexive map for storing canonical versions of {@link Subcat} objects.
   */
  transient protected Map canonicalSubcatMap;
  /** The value returned by <code>Subcats.get()</code>. */
  transient protected Subcat emptySubcat = Subcats.get();

  /**
   * The set of {@link Model} objects and other resources that describe
   * an entire parsing model.
   */
  protected ModelCollection modelCollection;

  // Model objects
  /**
   * The model for marginal probabilities of lexical elements (for the
   * estimation of the joint event that is a fully lexicalized nonterminal).
   */
  protected Model lexPriorModel;
  /**
   * The model for conditional probabilities of nonterminals given the
   * lexical components (for the estimation of the joint event that is a fully
   * lexicalized nonterminal).
   */
  protected Model nonterminalPriorModel;
  /**
   * The head-generation model for heads whose parents are {@link
   * Training#topSym()}.
   */
  protected Model topNonterminalModel;
  /** The head-word generation model for heads of entire sentences. */
  protected Model topLexModel;
  /** The head-generation model. */
  protected Model headModel;
  /** The gap-generation model. */
  protected Model gapModel;
  /**
   * The model for generating subcats that fall on the left side of head
   * children.
   */
  protected Model leftSubcatModel;
  /**
   * The model for generating subcats that fall on the right side of head
   * children.
   */
  protected Model rightSubcatModel;
  /** The modifying nonterminal&ndash;generation model. */
  protected Model modNonterminalModel;
  /** The model that generates head words of modifying nonterminals. */
  protected Model modWordModel;

  /**
   * A handle onto static {@link WordFeatures} object contained static inside
   * {@link Language}.
   */
  transient protected WordFeatures wordFeatures = Language.wordFeatures;

  // handles onto some data from Training for more efficient and more readable
  // code
  /** The value of {@link Training#startSym()}. */
  protected Symbol startSym = Language.training.startSym();
  /** The value of {@link Training#stopSym()}. */
  protected Symbol stopSym = Language.training.stopSym();
  /** The value of {@link Training#topSym()}. */
  protected Symbol topSym = Language.training.topSym();
  /** The value of {@link Training#startWord()}. */
  protected Word startWord = Language.training.startWord();
  /** The value of {@link Training#stopWord()}. */
  protected Word stopWord = Language.training.stopWord();
  /** The value of {@link Training#gapAugmentation()}. */
  protected Symbol gapAugmentation = Language.training.gapAugmentation();
  /** The value of {@link Training#traceTag()}. */
  protected Symbol traceTag = Language.training.traceTag();

  // various filters used by deriveCounts, deriveSubcatMaps and
  // deriveModNonterminalMap
  /** An instance of {@link AllPass}. */
  protected Filter allPass = new AllPass();
  /**
   * A filter that only allows {@link TrainerEvent} instances where the parent
   * nonterminal is not {@link Training#topSym()}.
   */
  protected Filter nonTop = new Filter() {
    public boolean pass(Object obj) {
      return ((TrainerEvent)obj).parent() != topSym;
    }
  };
  /**
   * A filter that only allows {@link TrainerEvent} instances that do not
   * represent preterminals (where the parent is identical to the part-of-speech
   * tag of the head word).
   */
  protected Filter nonPreterm = new Filter() {
    public boolean pass(Object obj) {
      TrainerEvent event = (TrainerEvent)obj;
      return event.parent() != event.headWord().tag();
    }
  };
  /**
   * A filter that is functionally equivalent to piping objects through both
   * {@link #nonTop} and {@link #nonPreterm}.
   */
  protected Filter nonTopNonPreterm = new Filter() {
    public boolean pass(Object obj) {
      TrainerEvent event = (TrainerEvent)obj;
      return (event.parent() != topSym &&
              event.parent() != event.headWord().tag());
    }
  };
  /**
   * A filter that only allows {@link TrainerEvent} instances where the parent
   * is {@link Training#topSym()}.
   */
  protected Filter topOnly = new Filter() {
      public boolean pass(Object obj) {
	return ((TrainerEvent)obj).parent() == topSym;
      }
    };
  /**
   * A filter that disallows  {@link ModifierEvent} instances where the
   * modifier is {@link Training#stopSym()}, but allows all other objects.
   */
  protected Filter nonStop = new Filter() {
    public boolean pass(Object obj) {
      if (obj instanceof ModifierEvent) {
	ModifierEvent modEvent = (ModifierEvent)obj;
	return modEvent.modifier() != stopSym;
      }
      else {
	return true;
      }
    }
  };
  /**
   * A filter that disallows {@link ModifierEvent} instances where the modifier
   * is neither {@link Training#stopSym()} nor {@link Training#topSym()}, but
   * allows all other objects.
   */
  protected Filter nonStopAndNonTop = new Filter() {
      public boolean pass(Object obj) {
	if (obj instanceof ModifierEvent) {
	  ModifierEvent modEvent = (ModifierEvent)obj;
	  return modEvent.modifier() != stopSym && modEvent.parent() != topSym;
	}
	else {
	  return true;
	}
      }
  };

  /**
   * Class to represent a MapToPrimitive.Entry object for use by the
   * {@link Trainer#getEventIterator} method.
   */
  public static class EventEntry extends AbstractMapToPrimitive.Entry {
    /**
     * The {@link TrainerEvent} object contained by this map entry.
     */
    protected TrainerEvent event;
    /**
     * The observed count of the {@link TrainerEvent} object contained by
     * this map entry.
     */
    protected double count;

    /**
     * Constructs a new <code>EventEntry</code> instance with the specified
     * {@link TrainerEvent} and count.
     *
     * @param event the {@link TrainerEvent} of this map entry
     * @param count the observed count of the {@link TrainerEvent} of this map
     * entry
     */
    public EventEntry(TrainerEvent event, double count) {
      this.event = event;
      this.count = count;
    }

    /**
     * Returns the event key associated with this map entry.
     * @return the event key associated with this map entry
     */
    public Object getKey() {
      return event;
    }
    public double getDoubleValue(int idx) {
      return count;
    }

    /**
     * Throws an {@link UnsupportedOperationException}.
     * @return an {@link UnsupportedOperationException}.
     */
    public Object getValue() {
      throw new UnsupportedOperationException();
    }
    /**
     * Throws an {@link UnsupportedOperationException}.
     * @return an {@link UnsupportedOperationException}.
     */
    public Object setValue(Object value) {
      throw new UnsupportedOperationException();
    }
    /**
     * Throws an {@link UnsupportedOperationException}.
     * @return an {@link UnsupportedOperationException}.
     */
    public boolean replaceKey(Object newKey) {
      throw new UnsupportedOperationException();
    }
  }

  // constructor

  /**
   * Constructs a new training object, which uses values from {@link Settings}
   * for its settings.  This class is not thread-safe, and there will typically
   * be one instance of a <code>Trainer</code> object per process, constructed
   * via the {@link #main} method of this class.
   *
   * @see Settings#unknownWordThreshold
   * @see Settings#countThreshold
   * @see Settings#derivedCountThreshold
   * @see Settings#trainerReportingInterval
   * @see Settings#numPrevMods
   */
  public Trainer() {
    unknownWordThreshold =
      Integer.parseInt(Settings.get(Settings.unknownWordThreshold));
    countThreshold =
      Double.parseDouble(Settings.get(Settings.countThreshold));
    derivedCountThreshold =
      Double.parseDouble(Settings.get(Settings.derivedCountThreshold));
    reportingInterval =
      Integer.parseInt(Settings.get(Settings.trainerReportingInterval));
    numPrevMods =
      Integer.parseInt(Settings.get(Settings.numPrevMods));
    numPrevWords =
      Integer.parseInt(Settings.get(Settings.numPrevWords));

    keepAllWords = Settings.getBoolean(Settings.keepAllWords);
    keepLowFreqTags = Settings.getBoolean(Settings.keepLowFreqTags);
    downcaseWords = Settings.getBoolean(Settings.downcaseWords);

    modelCollection = newModelCollection();
  }

  /**
   * Returns a new instance of {@link ModelCollection}.  Subclasses may override
   * this method to return different sub-types of {@link ModelCollection}.
   *
   * @return a new instance of {@link ModelCollection}
   */
  protected ModelCollection newModelCollection() {
    return new ModelCollection();
  }

  /**
   * Records observations from the training trees contained in the
   * specified S-expression tokenizer.  The observations are either mappings
   * stored in <code>Map</code> objects or items to be counted, stored in
   * <code>CountsTable</code> objects.  All the trees obtained from
   * <code>tok</code> are first preprocessed using
   * <code>Training.preProcess(Sexp)</code>.
   *
   * @param tok the S-expression tokenizer from which to obtain training
   * parse trees
   * @param auto indicates whether to automatically determine whether to
   * strip off outer parens of training parse trees before preprocessing;
   * if the value of this argument is <code>false</code>, then the value
   * of <code>stripOuterParens</code> is used
   * @param stripOuterParens indicates whether an outer layer of parentheses
   * should be stripped off of trees obtained from <code>tok</code> before
   * preprocessing and training (only used if the <code>auto</code> argument
   * is <code>false</code>)
   *
   * @see CountsTable
   * @see Training#preProcess(Sexp)
   */
  public void train(SexpTokenizer tok, boolean auto, boolean stripOuterParens)
    throws IOException {
    Sexp tree = null;
    int sentNum = 0, intervalCounter = 0;
    ArrayList headTrees = new ArrayList();
    int collinsSkipIdx = 0;
    int numSents = 0;
    System.err.println("Phase 0: reading trees and finding heads");
    for ( ; (tree = Sexp.read(tok)) != null; sentNum++, intervalCounter++) {
      //System.err.println(tree);

      if (intervalCounter == reportingInterval) {
	System.err.println(className + ": processed " + sentNum + " sentence" +
			   (sentNum > 1 ? "s" : ""));
	intervalCounter = 0;
      }

      if (useCollinsSkipArr) {
	if (collinsSkipIdx < collinsSkipArr.length &&
	    sentNum + 1 == collinsSkipArr[collinsSkipIdx]) {
	  System.err.println("Skipping sentence " + (sentNum + 1) +
			     " (to emulate Collins' trainer)");
	  collinsSkipIdx++;
	  continue;
	}
      }

      /*
      if (!tree.isList()) {
	System.err.println(className + ": error: invalid format for training " +
			   "parse tree: " + tree + " ...skipping");
	continue;
      }
      */

      // parenthesis-stripping is indicated if the training tree is a list
      // containing one element that is also a list
      if (auto)
	stripOuterParens = (tree.list().length() == 1 &&
			    tree.list().get(0).isList());
      if (stripOuterParens)
	tree = tree.list().get(0);

      String skipStr = Language.training.skip(tree);
      if (skipStr != null) {
	System.err.println(className + ": skipping tree No. " +
			   (sentNum + 1) + ": " + skipStr);
	continue;
      }

      if (outputCollins)
	tree = new SexpList(2).add(collinsTop).add(tree);

      //System.err.println(tree);

      numSents++;

      Language.training.preProcess(tree);

      //System.err.println(Util.prettyPrint(tree));

      HeadTreeNode headTree = new HeadTreeNode(tree);
      if (downcaseWords)
	downcaseWords(headTree);
      headTrees.add(headTree);
    }

    prunedPreterms = Language.training.getPrunedPreterms();
    prunedPunctuation = Language.training.getPrunedPunctuation();

    // phase 1: go through all sentences and set up vocabulary counts
    System.err.println("Phase 1: vocabulary counts");
    for (sentNum = 0; sentNum < numSents; sentNum++) {
      HeadTreeNode headTree = (HeadTreeNode)headTrees.get(sentNum);
      countVocab(headTree);
    }
    int origVocabSize = vocabCounter.size();
    System.err.println("Original vocab size is " + origVocabSize + ".");

    // phase 2: alter low-frequency words in HeadTreeNode objects
    System.err.println("Phase 2: alter low-frequency words");
    if (unknownWordThreshold > 1) {
      for (sentNum = 0; sentNum < numSents; sentNum++) {
	HeadTreeNode headTree = (HeadTreeNode)headTrees.get(sentNum);
	alterLowFrequencyWords(headTree);
      }
    }
    if (!keepAllWords)
      vocabCounter.removeItemsBelow(unknownWordThreshold);

    int numTransformed = origVocabSize - vocabCounter.size();
    int numWFVectors = wordFeatureCounter.size();
    String verbToBe = (numWFVectors > 1 ? "are" : "is");
    String plural = (numWFVectors > 1 ? "s" : "");
    System.err.println("Transformed " + numTransformed + " original vocab " +
		       "items into word feature vectors.\nThere " + verbToBe +
		       " " + numWFVectors + " distinct word feature " +
		       "vector" + plural + ".\nOriginal vocab size was " +
		       origVocabSize + "; new vocab size is " +
		       vocabCounter.size() + ".");

    /*
    Iterator vocabItems = vocabCounter.keySet().iterator();
    while (vocabItems.hasNext()) {
      Symbol word = (Symbol)vocabItems.next();
      System.out.println("count: " + word + "=" + vocabCounter.count(word));
    }
    */

    // phase 3: finally go through all sentences and collect stats
    System.err.println("Phase 3: collect stats");
    intervalCounter = 0;
    canonicalSubcatMap = new HashMap();
    for (sentNum = 0; sentNum < numSents; sentNum++, intervalCounter++) {
      HeadTreeNode headTree = (HeadTreeNode)headTrees.get(sentNum);
      if (intervalCounter == reportingInterval) {
	System.err.println(className + ": processed " + sentNum + " sentence" +
			   (sentNum > 1 ? "s" : ""));
	intervalCounter = 0;
      }
      collectStats(tree, headTree, true);
    }
    canonicalSubcatMap = null; // it has served its purpose

    System.err.println(className + ": processed " + sentNum + " sentence" +
		       (sentNum > 1 ? "s " : " ") + "in total");

    System.err.print("Creating part-of-speech map...");
    System.err.flush();
    createPosMap();
    System.err.println("done (map has " + posMap.size() + " entries).");

    if (countThreshold > 0.0) {
      System.err.println(className + ": removing all TrainerEvent objects " +
			 "with counts less than " + countThreshold);
      headEvents.removeItemsBelow(countThreshold);
      modifierEvents.removeItemsBelow(countThreshold);
      gapEvents.removeItemsBelow(countThreshold);
    }

    //countUniqueBigrams();

    //outputCollins();
  }

  private void downcaseWords(HeadTreeNode tree) {
    if (tree.isPreterminal()) {
      if (tree.headWord().tag() != traceTag) {
	Word headWord = tree.headWord();
	tree.setOriginalHeadWord(headWord.word());
	headWord.setWord(Symbol.add(headWord.word().toString().toLowerCase()));
      }
    }
    else {
      downcaseWords(tree.headChild());
      for (Iterator mods = tree.preMods().iterator(); mods.hasNext(); )
	downcaseWords((HeadTreeNode)mods.next());
      for (Iterator mods = tree.postMods().iterator(); mods.hasNext(); )
	downcaseWords((HeadTreeNode)mods.next());
    }
  }

  /**
   * Counts number of occurrences of each word in the specified tree and
   * adds the word with this count to {@link #vocabCounter}.  Specifically,
   * if the tree with which this recursive method is called represents
   * a preterminal that is not a trace and that is not already a key in
   * {@link #wordFeatureCounter}, then the <code>word</code> field
   * of the tree's {@linkplain HeadTreeNode#headWord headWord} is added
   * (with a count of 1) to {@link #vocabCounter}.
   *
   * @param tree
   */
  protected void countVocab(HeadTreeNode tree) {
    if (tree.isPreterminal()) {
      if (tree.headWord().tag() != traceTag) {
	Word headWord = tree.headWord();
	if (wordFeatureCounter.containsKey(headWord.word()) == false)
	  vocabCounter.add(headWord.word());
      }
    }
    else {
      countVocab(tree.headChild());
      for (Iterator mods = tree.preMods().iterator(); mods.hasNext(); )
	countVocab((HeadTreeNode)mods.next());
      for (Iterator mods = tree.postMods().iterator(); mods.hasNext(); )
	countVocab((HeadTreeNode)mods.next());
    }
  }


  /**
   * For every <code>Word</code> in the specified tree, if it occurred
   * less than {@link #unknownWordThreshold} times, then it is modified.
   * If {@link #keepAllWords} is <code>true</code>, then the word's
   * <code>features</code> field is set, using {@link Word#setFeatures(Symbol)};
   * otherwise, the word's <code>word</code> field is set, using
   * {@link Word#setWord(Symbol)}.<br>
   * This method also invokes {@link #addToPosMap} with {@link #posMap}
   * and the head word as arguments if {@link #keepLowFreqTags} is
   * <code>true</code>.
   *
   * @param tree the tree in which to alter word frequencies
   */
  protected void alterLowFrequencyWords(HeadTreeNode tree) {
    if (tree.isPreterminal()) {
      if (tree.headWord().tag() != traceTag) {
	Word headWord = tree.headWord();
	if (vocabCounter.count(headWord.word()) < unknownWordThreshold) {
	  if (keepLowFreqTags && !keepAllWords)
	    addToPosMap(headWord.word(), headWord.tag());
	  boolean isFirstWord = tree.leftIdx() == 0;
	  Symbol oldWord = tree.originalHeadWord();
	  if (oldWord == null)
	    oldWord = headWord.word();
	  Symbol features = wordFeatures.features(oldWord, isFirstWord);
	  // if we want to keep all words, we simply *add* to this
	  // Word's information by setting its features field; otherwise,
	  // we simply *replace* the word itself with its feature vector
	  if (keepAllWords)
	    headWord.setFeatures(features);
	  else
	    headWord.setWord(features);
	  wordFeatureCounter.add(features);
	}
      }
    }
    else {
      alterLowFrequencyWords(tree.headChild());
      for (Iterator mods = tree.preMods().iterator(); mods.hasNext(); )
	alterLowFrequencyWords((HeadTreeNode)mods.next());
      for (Iterator mods = tree.postMods().iterator(); mods.hasNext(); )
	alterLowFrequencyWords((HeadTreeNode)mods.next());
    }
  }

  /**
   * This method is a synonym for <code>addHeadEvent(event, 1.0)</code>.
   *
   * @param event the event to be added with a count of <tt>1.0</tt>
   *
   * @see #addHeadEvent(HeadEvent,double)
   */
  protected void addHeadEvent(HeadEvent event) {
    addHeadEvent(event, 1.0);
  }

  /**
   * This method is a synonym for <code>addModifierEvent(event, 1.0)</code>.
   *
   * @param event the event to be added with a count of <tt>1.0</tt>
   *
   * @see #addModifierEvent(ModifierEvent,double)
   */
  protected void addModifierEvent(ModifierEvent event) {
    addModifierEvent(event, 1.0);
  }

  /**
   * This method is a synonym for <code>addGapEvent(event, 1.0)</code>.
   *
   * @param event the event to be added with a count of <tt>1.0</tt>
   *
   * @see #addGapEvent(GapEvent,double)
   */
  protected void addGapEvent(GapEvent event) {
    addGapEvent(event, 1.0);
  }

  /**
   * Adds the specified <code>HeadEvent</code> to {@link #headEvents} with
   * the specified count.  This is a helper method used by the
   * {@link #collectStats(Sexp,HeadTreeNode,boolean) collectStats}
   * and {@link #readStats(SexpTokenizer) readStats} methods.  The purpose of
   * using this protected method is to provide a hook for subclasses.
   *
   * @param event the <code>HeadEvent</code> to be added
   * @param count the count of the event to be added
   *
   * @see CountsTable#add(Object,double)
   */
  protected void addHeadEvent(HeadEvent event, double count) {
    headEvents.add(event, count);
  }

  /**
   * Adds the specified <code>ModifierEvent</code> to {@link #modifierEvents}
   * with the specified count.  This is a helper method used by the
   * {@link #collectStats(Sexp,HeadTreeNode,boolean) collectStats},
   * {@link #collectModifierStats(HeadTreeNode,Subcat,int,boolean)
   * collectModifierStats} and
   * {@link #readStats(SexpTokenizer) readStats} methods.  The purpose of
   * using this protected method is to provide a hook for subclasses.
   *
   * @param event the <code>ModifierEvent</code> to be added
   * @param count the count of the event to be added
   *
   * @see CountsTable#add(Object,double)
   */
  protected void addModifierEvent(ModifierEvent event, double count) {
    modifierEvents.add(event, count);
  }

  /**
   * Adds the specified <code>GapEvent</code> to {@link #gapEvents} with
   * the specified count.  This is a helper method used by the
   * {@link #collectStats(Sexp,HeadTreeNode,boolean) collectStats},
   * {@link #collectModifierStats(HeadTreeNode,Subcat,int,boolean)
   * collectModifierStats} and
   * {@link #readStats(SexpTokenizer) readStats} methods.  The purpose of
   * using this protected method is to provide a hook for subclasses.
   *
   * @param event the <code>GapEvent</code> to be added
   * @param count the count of the event to be added
   *
   * @see CountsTable#add(Object,double)
   */
  protected void addGapEvent(GapEvent event, double count) {
    gapEvents.add(event, count);
  }

  /**
   * Collects the statistics from the specified tree.  Some
   * &quot;statistics&quot; are actually mappings, such as
   * part-of-speech-to-word mappings.
   *
   * @param orig the original (preprocessed) tree, used for debugging purposes
   * @param tree the tree from which to collect statistics and mappings
   * @param isRoot indicates whether <code>tree</code> is the observed root
   * of a tree (the observed root is the child of the hidden root,
   * represented by the symbol {@link Training#topSym})
   *
   */
  protected void collectStats(Sexp orig, HeadTreeNode tree, boolean isRoot) {
    SexpList startList = newStartList();
    WordList startWordList = newStartWordList();
    if (isRoot) {
      // add special head transition from +TOP+ to actual root of tree
      HeadEvent topToRoot = new HeadEvent(tree.headWord(), topSym, tree.label(),
					  emptySubcat, emptySubcat);
      addHeadEvent(topToRoot);

      // kind of a hack: we manufacture a "modification event", as
      // though the root node modifies +TOP+, so as to gather counts
      // for the last level of back-off of the modification model,
      // which is p(w | t); without this hack, we would not have counts
      // for words that are the heads of their entire sentence, since
      // they are not modifiers of anything
      ModifierEvent topMod = new ModifierEvent(tree.headWord(),
					       tree.headWord(),
					       tree.label(),
					       startList,
					       startWordList,
					       topSym,
					       tree.label(),
					       emptySubcat,
					       false, false);
      addModifierEvent(topMod);
    }
    if (tree.isPreterminal()) {
      Word word = tree.headWord();
      if (word.tag() != traceTag) {
	// we add a trivial head-generation event from a lexicalized
	// preterminal to its word, which by design has a probability of 1;
	// this enables the headEvents counts table to contain counts for all
	// lexicalized nonterminals (crucial for the last level of back-off of
	// the top lex model)
        HeadEvent pretermHeadEvent =
          new HeadEvent(word, tree.label(), word.word(),
                        emptySubcat, emptySubcat);
        addHeadEvent(pretermHeadEvent);
      }
    }
    else {
      nonterminals.add(tree.label());
      // head generation event
      Subcat leftSubcat = collectArguments(tree.preMods());
      Subcat rightSubcat = collectArguments(tree.postMods());

      Symbol parent = tree.label();
      Symbol head = tree.headChild().label();

      boolean parentHasGap = Language.training.hasGap(parent);
      boolean headHasGap = Language.training.hasGap(head);

      int leftGapIdx = -1;
      int rightGapIdx = -1;

      // take care of gap passed from parent to head child
      // this isn't *strictly* necessary, as the headEvent contains all
      // this information (via the gap augmentations on parent and head child)
      if (parentHasGap) {
	if (headHasGap) {
	  GapEvent gapEvent = new GapEvent(GapEvent.toHead, tree.headWord(),
					   parent, head);
	  addGapEvent(gapEvent);
	}
	else {
	  // find gap index, which is either on left or right side of head
	  leftGapIdx = hasGapOrTrace(tree.preMods());
	  rightGapIdx = hasGapOrTrace(tree.postMods());
	}
      }

      leftSubcat = leftSubcat.getCanonical(false, canonicalSubcatMap);
      rightSubcat = rightSubcat.getCanonical(false, canonicalSubcatMap);

      HeadEvent headEvent = new HeadEvent(tree.headWord(),
					  parent, head,
					  leftSubcat, rightSubcat);
      addHeadEvent(headEvent);

      if (outputCollins)
	outputCollins(headEvent);

      collectModifierStats(tree, leftSubcat, leftGapIdx, Constants.LEFT);
      collectModifierStats(tree, rightSubcat, rightGapIdx, Constants.RIGHT);

      // make recursive call to head child
      collectStats(orig, tree.headChild(), false);
      // make recursive calls on pre- and postmodifiers
      Iterator mods = tree.preMods().iterator();
      while (mods.hasNext())
	collectStats(orig, (HeadTreeNode)mods.next(), false);
      mods = tree.postMods().iterator();
      while (mods.hasNext())
	collectStats(orig, (HeadTreeNode)mods.next(), false);
    }
  }

  /**
   * Creates and returns a new start list.  A start list is a list of length
   * equal to the value of <tt>Settings.get(Settings.numPrevMods)</tt>, where
   * every element is the symbol <code>Language.training.startSym()</code>.
   * This is the appropriate initial list of previously "generated" modidifers
   * when beginning the Markov process of generating modifiers.
   *
   * @return a new list of start symbols
   *
   * @see Training#startSym()
   */
  public static SexpList newStartList() {
    int numPrevMods = Integer.parseInt(Settings.get(Settings.numPrevMods));
    return newStartList(numPrevMods);
  }

  private static final SexpList newStartList(int numPrevMods) {
    SexpList startList = new SexpList(numPrevMods);
    for (int i = 0; i < numPrevMods; i++)
      startList.add(Language.training.startSym());
    return startList;
  }

  public static WordList newStartWordList() {
    int numPrevWords = Integer.parseInt(Settings.get(Settings.numPrevWords));
    return newStartWordList(numPrevWords);
  }

  private static WordList newStartWordList(int numPrevWords) {
    WordList wordList = WordListFactory.newList(numPrevWords);
    for (int i = 0; i < numPrevWords; i++)
      wordList.add(Language.training.startWord());
    return wordList;
  }

  /**
   * Note the O(n) operation performed on the prevModList.
   */
  protected void collectModifierStats(HeadTreeNode tree,
				      Subcat subcat,
				      int gapIdx,
				      boolean side) {
    Symbol parent = tree.label();
    Symbol head = tree.headChild().label();
    Word headWord = tree.headWord();
    Iterator mods = (side == Constants.LEFT ?
		     tree.preMods().iterator() : tree.postMods().iterator());

    ModifierEvent prevModEvent = null;

    SexpList prevModList = newStartList(numPrevMods);
    Sexp prevMod = prevModList.first();
    WordList prevWordList = newStartWordList(numPrevWords);
    Word prevWord = prevWordList.getWord(0);
    Subcat dynamicSubcat = (Subcat)subcat.copy();
    // if there's a gap to generate, add as requirement to end of subcat list
    if (gapIdx != -1)
      dynamicSubcat.add(gapAugmentation);
    boolean verbIntervening = false;

    // collect modifier generation events
    boolean prevModHadVerb = false;
    // the next two booleans only necessary for (currently) unused method of
    // determining modifier adjacency, which is that no *words* appear between
    // head and currently-generated left- or right-edge frontier word of
    // currently-generated modifier subtree
    boolean wordsIntervening = false;
    boolean headAlreadyHasMods =
      (side == Constants.LEFT ?
       tree.headChild().leftIdx() < tree.headWordIdx() :
       tree.headChild().rightIdx() > tree.headWordIdx() + 1);

    Symbol prevRealModifier = null;
    Word prevPunc = null, prevConj = null;

    for (int modIdx = 0; mods.hasNext(); modIdx++) {
      HeadTreeNode currMod = (HeadTreeNode)mods.next();
      Symbol modifier = currMod.label();

      Shifter.shift(prevModEvent, prevModList, prevMod);
      Shifter.shift(prevModEvent, prevWordList, prevWord);

      boolean isConj = Language.treebank.isConjunction(modifier);
      boolean isConjPConj =
	isConj && side == Constants.RIGHT &&
	!Language.treebank.isBaseNP(parent) && mods.hasNext();

      if (isConjPConj && prevConj != null)
	System.err.println(className +
			   ": warning: more than one conjunction in a row: " +
			   tree.toSexp());

      boolean headAdjacent = prevRealModifier == null;

      verbIntervening |= prevModHadVerb;
      wordsIntervening = (modIdx > 0 ? true : headAlreadyHasMods);
      Subcat canonicalDynamicSubcat =
	dynamicSubcat.getCanonical(false, canonicalSubcatMap);
      ModifierEvent modEvent = new ModifierEvent(currMod.headWord(),
						 headWord,
						 modifier,
						 new SexpList(prevModList),
						 prevWordList.copy(),
						 parent,
						 head,
						 canonicalDynamicSubcat,
						 prevPunc,
						 prevConj,
						 isConjPConj,
						 verbIntervening,
						 headAdjacent,
						 side);
      addModifierEvent(modEvent);

      if (outputCollins)
	outputCollins(modEvent);

      if (modIdx == gapIdx) {
	Symbol direction = (side == Constants.LEFT ?
			    GapEvent.toLeft : GapEvent.toRight);
	GapEvent gapEvent = new GapEvent(direction, headWord, parent, head);
	addGapEvent(gapEvent);
	if (dynamicSubcat.size() == 0)
	  System.err.println(className + ": error: gap detected in " +
			     tree + " but subcat list is empty!");
	if (dynamicSubcat.contains(gapAugmentation)) {
	  dynamicSubcat = (Subcat)dynamicSubcat.copy();
	  dynamicSubcat.remove(gapAugmentation);
	}
	else {
	  System.err.println(className + ": warning: gap detected in " +
			     "modifier " + tree.label() + " but not present " +
			     "in subcat list");
	}
      }

      prevModEvent = modEvent;

      if (dynamicSubcat.contains(modifier)) {
	dynamicSubcat = (Subcat)dynamicSubcat.copy();
	dynamicSubcat.remove(modifier);
      }

      prevModHadVerb = currMod.containsVerb();

      prevWord = currMod.headWord();
      prevMod = modifier;

      if (Language.treebank.isPunctuation(modifier))
	prevPunc = currMod.headWord();
      else if (isConjPConj)
	prevConj = currMod.headWord();
      else {
	prevRealModifier = modifier;
	prevPunc = null;
	prevConj = null;
      }
    }

    if (!dynamicSubcat.empty())
      System.err.println(className + ": warning: dynamic subcat not empty: " +
			 dynamicSubcat);

    Subcat emptySubcat = dynamicSubcat.getCanonical(false, canonicalSubcatMap);

    // transition to stop symbol
    verbIntervening |= prevModHadVerb;
    // need to shift in final mod and modWord
    Shifter.shift(prevModEvent, prevModList, prevMod);
    Shifter.shift(prevModEvent, prevWordList, prevWord);
    ModifierEvent modEvent = new ModifierEvent(stopWord,
					       headWord,
					       stopSym,
					       prevModList,
					       prevWordList,
					       parent,
					       head,
					       emptySubcat,
					       null,
					       null,
					       false,
					       verbIntervening,
					       prevRealModifier == null,
					       side);
    addModifierEvent(modEvent);

    if (outputCollins)
      outputCollins(modEvent);
  }

  /**
   * Creates {@link #posMap} from the {@link #headEvents}, {@link
   * #modifierEvents} and {@link #gapEvents} counts tables.
   */
  public void createPosMap() {
    createPosMap(headEvents);
    createPosMap(modifierEvents);
    createPosMap(gapEvents);
  }

  /**
   * Adds to {@link #posMap} using information contained in the specified
   * counts table.
   * @param events the counts table of {@link TrainerEvent} instances from
   * which to derive a mapping of words to their observed parts of speech
   */
  public void createPosMap(CountsTable events) {
    Iterator it = events.keySet().iterator();
    while (it.hasNext()) {
      TrainerEvent event = (TrainerEvent)it.next();
      addToPosMap(event.headWord());
      addToPosMap(event.modHeadWord());
    }
  }

  // helper methods

  /**
   * Called by {@link #collectStats} and
   * {@link #alterLowFrequencyWords(HeadTreeNode)}.
   *
   * @param word the Word object containing word (and possibly a word-feature
   * vector) and a tag with which that word (and possibly feature vector) has
   * been observed with
   */
  protected final void addToPosMap(Word word) {
    if (isRealWord(word)) {
      addToPosMap(word.word(), word.tag());
      if (keepAllWords && word.features() != null)
        addToPosMap(word.features(), word.tag());
    }
  }

  /**
   * Called by {@link #addToPosMap(Word)}.
   *
   * @param word the word with which to associate a part of speech
   * @param tag the part-of-speech tag associated with the specified word
   */
  protected final void addToPosMap(Symbol word, Symbol tag) {
    SexpList mapSet = (SexpList)posMap.get(word);
    if (mapSet == null)
      posMap.put(word, (mapSet = new SexpList(2)).add(tag));
    else if (!mapSet.contains(tag))
      mapSet.add(tag);
  }

  /** Called by {@link #collectStats}. */
  private final static Subcat collectArguments(List modifiers) {
    Subcat args = Subcats.get();
    Iterator mods = modifiers.iterator();
    while (mods.hasNext()) {
      HeadTreeNode mod = (HeadTreeNode)mods.next();
      Symbol modLabel = mod.label();
      args.add(modLabel);
      /*
      if (Language.training.isArgument(modLabel)) {
	if (modLabel.toString().indexOf("RRB") != -1 ||
	    modLabel.toString().indexOf("LRB") != -1)
	  secretFlag = true;
      }
      */
    }
    return args;
  }

  /** Called by {@link #collectStats}. */
  private final int hasGapOrTrace(List modifiers) {
    Iterator mods = modifiers.iterator();
    for (int i = 0; mods.hasNext(); i++)
      if (hasGapOrTrace((HeadTreeNode)mods.next()))
	return i;
    return -1;
  }

  /** Called by {@link #hasGapOrTrace(List)}. */
  private final boolean hasGapOrTrace(HeadTreeNode modifier) {
    if (Language.training.hasGap(modifier.label()))
      return true;
    if (Language.treebank.isWHNP(modifier.label()) &&
	modifier.headChild().isPreterminal() &&
	modifier.headChild().headWord().tag() == traceTag)
      return true;
    return false;
  }

  /**
   * Creates all of the internal model objects used by this trainer when
   * constructing its internal {@link ModelCollection} object.
   * Each model is created by first creating its {@link ProbabilityStructure}
   * object, and then calling that object's
   * {@link ProbabilityStructure#newModel()} method to wrap itself in a
   * {@link Model} instance.  There are ten {@link Model} members of this class:
   * <ul>
   * <li>{@link #lexPriorModel}
   * <li>{@link #nonterminalPriorModel}
   * <li>{@link #topNonterminalModel}
   * <li>{@link #topLexModel}
   * <li>{@link #headModel}
   * <li>{@link #gapModel}
   * <li>{@link #leftSubcatModel}
   * <li>{@link #rightSubcatModel}
   * <li>{@link #modNonterminalModel}
   * <li>{@link #modWordModel}
   * </ul>
   * In order to determine the fully-qualified class name for the associated
   * {@link ProbabilityStructure} for each of the above models, the following
   * algorithm is used:
   * <ul>
   * <li>If the fully-qualified name is specified via its settings, then
   * an instance is used.
   * <li>Otherwise, if there is a model structure&ndash;specific number setting
   * found in {@link Settings}, then it is appended to the default model
   * structure classname and an instance is used.
   * <li>Otherwise, if the global model structure number is appended to the
   * default model structure classname prefix and an instance is used.
   * </ul>
   * Please read the documentation for the
   * {@link Settings#globalModelStructureNumber} setting for more details on
   * all the model structure&ndash;specific settings that control which
   * concrete subclasses of {@link ProbabilityStructure} are instantiated.
   */
  protected void createModelObjects() {
    String globalModelStructureNumber =
      Settings.get(Settings.globalModelStructureNumber);
    if (globalModelStructureNumber == null) {
      globalModelStructureNumber = "1";
    }
    System.err.println(className + ": using global model structure number " +
                       globalModelStructureNumber);
    try {
      lexPriorModel =
        getProbStructure(lexPriorModelStructureClassnamePrefix,
                         globalModelStructureNumber,
                         Settings.lexPriorModelStructureNumber,
                         Settings.lexPriorModelStructureClass).newModel();

      nonterminalPriorModel =
        getProbStructure(nonterminalPriorModelStructureClassnamePrefix,
                         globalModelStructureNumber,
                         Settings.nonterminalPriorModelStructureNumber,
                         Settings.nonterminalPriorModelStructureClass).newModel();

      topNonterminalModel =
        getProbStructure(topNonterminalModelStructureClassnamePrefix,
                         globalModelStructureNumber,
                         Settings.topNonterminalModelStructureNumber,
                         Settings.topNonterminalModelStructureClass).newModel();

      topLexModel =
        getProbStructure(topLexModelStructureClassnamePrefix,
                         globalModelStructureNumber,
                         Settings.topLexModelStructureNumber,
                         Settings.topLexModelStructureClass).newModel();

      headModel =
        getProbStructure(headModelStructureClassnamePrefix,
                         globalModelStructureNumber,
                         Settings.headModelStructureNumber,
                         Settings.headModelStructureClass).newModel();
      gapModel =
        getProbStructure(gapModelStructureClassnamePrefix,
                         globalModelStructureNumber,
                         Settings.gapModelStructureNumber,
                         Settings.gapModelStructureClass).newModel();
      leftSubcatModel =
        getProbStructure(leftSubcatModelStructureClassnamePrefix,
                         globalModelStructureNumber,
                         Settings.leftSubcatModelStructureNumber,
                         Settings.leftSubcatModelStructureClass).newModel();
      rightSubcatModel =
        getProbStructure(rightSubcatModelStructureClassnamePrefix,
                         globalModelStructureNumber,
                         Settings.rightSubcatModelStructureNumber,
                         Settings.rightSubcatModelStructureClass).newModel();
      modNonterminalModel =
        getProbStructure(modNonterminalModelStructureClassnamePrefix,
                         globalModelStructureNumber,
                         Settings.modNonterminalModelStructureNumber,
                         Settings.modNonterminalModelStructureClass).newModel();
      modWordModel =
        getProbStructure(modWordModelStructureClassnamePrefix,
                         globalModelStructureNumber,
                         Settings.modWordModelStructureNumber,
                         Settings.modWordModelStructureClass).newModel();
    }
    catch (ExceptionInInitializerError e) {
      System.err.println(className + ": problem initializing an instance of " +
                         "a model class: " + e);
    }
    catch (LinkageError e) {
      System.err.println(className + ": problem linking a model class: " + e);
    }
    catch (ClassNotFoundException e) {
      System.err.println(className + ": couldn't find a model class: " + e);
    }
    catch (InstantiationException e) {
      System.err.println(className + ": couldn't instantiate a model class: " +
                         e);
    }
    catch (IllegalAccessException e) {
      System.err.println(className + ": not allowed to instantiate a model " +
                         "class: " + e);
    }
  }

  /**
   * Derives event counts for all back-off levels of all sub-models for the
   * current parsing model.  After deriving counts, the {@link
   * #modelCollectionSet(FlexibleMap)} method will be invoked.
   *
   * @see Model#deriveCounts(CountsTable,Filter, double,FlexibleMap)
   */
  public void deriveCounts() {
    deriveCounts(true);
  }

  /**
   * Derives event counts for all back-off levels of all sub-models for the
   * current parsing model.
   *
   * @param setModelCollection indicates whether to invoke {@link
   *                           #modelCollectionSet(FlexibleMap)} after deriving
   *                           counts
   * @see Model#deriveCounts(CountsTable,Filter, double,FlexibleMap)
   */
  public void deriveCounts(boolean setModelCollection) {
    deriveCounts(setModelCollection, new danbikel.util.HashMap(100003, 1.5f));
  }

  /**
   * Derives event counts for all back-off levels of all sub-models for the
   * current parsing model.
   *
   * @param setModelCollection indicates whether to invoke {@link
   *                           #modelCollectionSet(FlexibleMap)} after deriving
   *                           counts
   * @param canonical          the {@link FlexibleMap} instance to use for
   *                           creating a reflexive map of canonical versions of
   *                           event objects creating when deriving counts
   * @see Model#deriveCounts(CountsTable,Filter, double,FlexibleMap)
   */
  public void deriveCounts(boolean setModelCollection, FlexibleMap canonical) {
    deriveCounts(derivedCountThreshold, canonical);

    System.err.println("Canonical events HashMap stats: " +
                       canonical.getStats());

    if (setModelCollection) {
      precomputeProbs();
      modelCollectionSet(canonical);
    }
  }

  /**
   * Clears the {@link #priorEvents}, {@link #headEvents}, {@link
   * #modifierEvents} and {@link #gapEvents} counts tables.
   */
  protected void clearEventCounters() {
    priorEvents.clear();
    headEvents.clear();
    modifierEvents.clear();
    gapEvents.clear();
  }

  /**
   * Derives all counts for creating a {@link ModelCollection} object.
   *
   * @param derivedCountThreshold the count threshold below which to throw away
   *                              derived events
   * @param canonical             a reflexive map of canonical versions of
   *                              derived {@link Event} and {@link Transition}
   *                              objects, shared among all {@link Model}
   *                              instances of this trainer
   */
  protected void deriveCounts(double derivedCountThreshold,
                              FlexibleMap canonical) {
    System.err.print("Deriving events for prior probability computations...");
    derivePriors();
    System.err.println("done.");

    //Language.training().setUpFastArgMap(nonterminals);

    deriveModelCounts(derivedCountThreshold, canonical);

    deriveHeadToParentMap(canonical);

    deriveSubcatMaps(leftSubcatModel.getProbStructure(),
                     rightSubcatModel.getProbStructure(),
                     canonical);

    deriveModNonterminalMap(modNonterminalModel.getProbStructure(),
                            canonical);
  }

  /**
   * A helper method used by {@link #deriveCounts(double,FlexibleMap)} to derive
   * counts for all {@link Model} instances contained within a {@link
   * ModelCollection}.
   *
   * @param derivedCountThreshold the count threshold below which to throw away
   *                              derived events
   * @param canonical             a reflexive map of canonical versions of
   *                              derived {@link Event} and {@link Transition}
   *                              objects, shared among all {@link Model}
   *                              instances
   */
  protected void deriveModelCounts(double derivedCountThreshold,
                                   FlexibleMap canonical) {
    double th = derivedCountThreshold;
    lexPriorModel.deriveCounts(priorEvents, allPass, th, canonical);
    nonterminalPriorModel.deriveCounts(priorEvents, allPass, th, canonical);
    topNonterminalModel.deriveCounts(headEvents, topOnly, th, canonical);
    topLexModel.deriveCounts(headEvents, allPass, th, canonical);
    headModel.deriveCounts(headEvents, nonTopNonPreterm, th, canonical);
    gapModel.deriveCounts(gapEvents, allPass, th, canonical);
    leftSubcatModel.deriveCounts(headEvents, nonTopNonPreterm, th, canonical);
    rightSubcatModel.deriveCounts(headEvents, nonTopNonPreterm, th, canonical);
    modNonterminalModel.deriveCounts(modifierEvents, allPass, th, canonical);
    modWordModel.deriveCounts(modifierEvents, nonStop, th, canonical);
  }

  /**
   * Precomputes all probabilities and smoothing parameters for all {@link
   * Model} instances that are part of the {@link ModelCollection} of this
   * trainer.
   *
   * @see Model#precomputeProbs()
   */
  protected void precomputeProbs() {
    lexPriorModel.precomputeProbs();
    nonterminalPriorModel.precomputeProbs();
    topNonterminalModel.precomputeProbs();
    topLexModel.precomputeProbs();
    headModel.precomputeProbs();
    gapModel.precomputeProbs();
    leftSubcatModel.precomputeProbs();
    rightSubcatModel.precomputeProbs();
    modNonterminalModel.precomputeProbs();
    modWordModel.precomputeProbs();
  }

  /**
   * Sets all the data members of the {@link #modelCollection} member of this
   * trainer with the internal resources constructed by this trainer (such as
   * all the {@link Model} instances).
   *
   * @param canonical a reflexive map of canonical versions of derived {@link
   *                  Event} and {@link Transition} objects, shared among all
   *                  {@link Model} instances
   */
  protected void modelCollectionSet(FlexibleMap canonical) {
    modelCollection.set(lexPriorModel,
                        nonterminalPriorModel,
                        topNonterminalModel,
                        topLexModel,
                        headModel,
                        gapModel,
                        leftSubcatModel,
                        rightSubcatModel,
                        modNonterminalModel,
                        modWordModel,
                        vocabCounter,
                        wordFeatureCounter,
                        nonterminals,
                        posMap,
                        headToParentMap,
                        leftSubcatMap,
                        rightSubcatMap,
                        modNonterminalMap,
                        simpleModNonterminalMap,
                        prunedPreterms,
                        prunedPunctuation,
                        canonical);
    modelCollectionSetHook();

    // somewhat of a hack: we allow counts for a back-off level from
    // one model to be shared with another model; in this case, the
    // last level of back-off from the modWordModel is being
    // shared (i.e., will be used) as the last level of back-off for
    // topLexModel, as the last levels of both these models should just
    // be estimating p(w | t)
    if (shareCounts) {
      modelCollection.shareCounts(true);
    }
  }

  /**
   * A method called by {@link #deriveCounts()} just after it calls
   * {@link ModelCollection#set}.
   */
  protected void modelCollectionSetHook() { }

  /**
   * Called by {@link #deriveCounts()}.
   *
   * @param leftMS the left subcat model structure
   * @param rightMS the right subcat model structure
   */
  private void deriveSubcatMaps(ProbabilityStructure leftMS,
				ProbabilityStructure rightMS,
				FlexibleMap canonicalMap) {
    System.err.println("Deriving subcat maps.");

    //canonicalSubcatMap = new HashMap();

    int leftMSLastLevel = leftMS.numLevels() - 1;
    int rightMSLastLevel = rightMS.numLevels() - 1;

    Filter filter = nonTopNonPreterm;
    Iterator events = headEvents.keySet().iterator();
    while (events.hasNext()) {
      HeadEvent headEvent = (HeadEvent)events.next();
      if (!filter.pass(headEvent))
	continue;
      Event leftContext =
	leftMS.getHistory(headEvent, leftMSLastLevel).copy();
      leftContext.canonicalize(canonicalMap);
      Subcat canonicalLeftSubcat =
	headEvent.leftSubcat().getCanonical(false, canonicalMap);
      Util.addToValueSet(leftSubcatMap, leftContext, canonicalLeftSubcat);

      Event rightContext =
	rightMS.getHistory(headEvent, rightMSLastLevel).copy();
      rightContext.canonicalize(canonicalMap);
      Subcat canonicalRightSubcat =
	headEvent.rightSubcat().getCanonical(false, canonicalMap);
      Util.addToValueSet(rightSubcatMap, rightContext, canonicalRightSubcat);
    }

    if (Settings.getBoolean(Settings.outputSubcatMaps))
      outputSubcatMaps();

    //canonicalSubcatMap = null; // it has served its purpose
  }

  /**
   * Called by {@link #deriveCounts()}.
   *
   * @param canonicalMap the map of canonicalized objects
   */
  private void deriveHeadToParentMap(FlexibleMap canonicalMap) {
    System.err.println("Deriving head-to-parent map.");

    Filter filter = nonTopNonPreterm;
    Iterator events = headEvents.keySet().iterator();
    while (events.hasNext()) {
      HeadEvent headEvent = (HeadEvent)events.next();
      if (!filter.pass(headEvent))
	continue;
      Util.addToValueSet(headToParentMap, headEvent.head(), headEvent.parent());
    }
    if (Settings.getBoolean(Settings.outputHeadToParentMap))
      outputHeadToParentMap();
  }

  /**
   * Called by {@link #deriveCounts()}.
   *
   * @param modMS the modifying nonterminal model structure
   * @param canonicalMap the map of canonicalized objects
   */
  private void deriveModNonterminalMap(ProbabilityStructure modMS,
				       FlexibleMap canonicalMap) {
    System.err.println("Deriving modifying nonterminal maps.");

    int modMSLastLevel = modMS.numLevels() - 1;

    SexpList lookupTriple = new SexpList(3).add(null).add(null).add(null);
    SexpList lookupPair = new SexpList(2).add(null).add(null);
    SexpList parentHeadSideTriple;
    SexpList modPair;

    // we want to ignore stop-word modifiers, as well as the "fake" modifier
    // of +TOP+ that is the root of the observed tree; the nonStopAndNonTop
    // filter accomplishes this
    Filter filter = nonStopAndNonTop;
    //Filter filter = allPass;
    Iterator events = modifierEvents.keySet().iterator();
    while (events.hasNext()) {
      ModifierEvent modEvent = (ModifierEvent)events.next();
      if (!filter.pass(modEvent))
	continue;
      Event context = modMS.getHistory(modEvent, modMSLastLevel).copy();
      context.canonicalize(canonicalMap);
      Event future = modMS.getFuture(modEvent, modMSLastLevel).copy();
      future.canonicalize(canonicalMap);
      Util.addToValueSet(modNonterminalMap, context, future);

      Symbol arglessParent =
	Language.training().removeArgAugmentation(modEvent.parent());
      Symbol gaplessHead =
	(Symbol)Language.training().removeGapAugmentation(modEvent.head());
      lookupTriple.set(0, arglessParent);
      lookupTriple.set(1, gaplessHead);
      lookupTriple.set(2, Constants.sideToSym(modEvent.side()));
      parentHeadSideTriple = getCanonicalList(canonicalMap, lookupTriple);

      lookupPair.set(0, modEvent.modifier());
      lookupPair.set(1, modEvent.modHeadWord().tag());
      modPair = getCanonicalList(canonicalMap, lookupPair);

      Util.addToValueSet(simpleModNonterminalMap,
			 parentHeadSideTriple, modPair);
    }

    if (Settings.getBoolean(Settings.outputModNonterminalMap))
      outputModNonterminalMap();
  }

  /**
   * Returns a canonical version of the specified list from the specified
   * reflexive map.
   * @param map a reflexive map of {@link SexpList} objects
   * @param list the list to canonicalize
   * @return a canonical version of the specified list from the specified
   * reflexive map
   */
  public final static SexpList getCanonicalList(Map map,
                                                SexpList list) {
    SexpList canonicalList = (SexpList)map.get(list);
    if (canonicalList == null) {
      canonicalList = (SexpList)list.deepCopy();
      map.put(canonicalList, canonicalList);
    }
    return canonicalList;
  }

  // four utility methods to output subcat and mod nonterminal maps

  /**
   * Outputs the head map internal to this <code>Trainer</code> object
   * to <code>System.err</code>.
   */
  public void outputHeadToParentMap() {
    outputMap(headToParentMap, "head-to-parent-map");
  }

  /**
   * Outputs the subcat maps internal to this <code>Trainer</code> object
   * to <code>System.err</code>.
   */
  public void outputSubcatMaps() {
    outputMaps(leftSubcatMap, "left-subcat", rightSubcatMap, "right-subcat");
  }

  /**
   * Outputs the modifier map internal to this <code>Trainer</code> object
   * to <code>System.err</code>.
   */
  public void outputModNonterminalMap() {
    outputMap(modNonterminalMap, "mod-map");
    outputMap(simpleModNonterminalMap, "simple-mod-map");
  }

  /** Outputs the specified map to <code>System.err</code> */
  public static void outputMap(Map map, String mapName) {
    try {
      BufferedWriter systemErr =
	new BufferedWriter(new OutputStreamWriter(System.err,
						  Language.encoding()),
			   Constants.defaultFileBufsize);
      try {
	outputMap(map, mapName, systemErr);
	systemErr.flush();
      }
      catch (IOException ioe) {
	System.err.println(ioe);
      }
    }
    catch (UnsupportedEncodingException uee) {
      System.err.println(uee);
    }
  }

  /** Outputs both the specified maps to <code>System.err</code>. */
  public static void outputMaps(Map leftMap, String leftMapName,
				Map rightMap, String rightMapName) {
    try {
      BufferedWriter systemErr =
	new BufferedWriter(new OutputStreamWriter(System.err,
						  Language.encoding()),
			   Constants.defaultFileBufsize);
      try {
	outputMaps(leftMap, leftMapName, rightMap, rightMapName, systemErr);
	systemErr.flush();
      }
      catch (IOException ioe) {
	System.err.println(ioe);
      }
    }
    catch (UnsupportedEncodingException uee) {
      System.err.println(uee);
    }
  }

  /** Outputs the specified named map to the specified writer. */
  public static void outputMap(Map map, String mapName, Writer writer)
  throws IOException {
    SymbolicCollectionWriter.writeMap(map, Symbol.add(mapName), writer);
  }

  /** Outputs both the specified maps to the specified writer. */
  public static void outputMaps(Map leftMap, String leftMapName,
				Map rightMap, String rightMapName,
				Writer writer)
  throws IOException {
    SymbolicCollectionWriter.writeMap(leftMap,
				      Symbol.add(leftMapName), writer);
    SymbolicCollectionWriter.writeMap(rightMap,
				      Symbol.add(rightMapName), writer);
  }



  /**
   * Runs through all headEvents, collecting lexicalized nonterminal
   * occurrences.  Called by {@link #deriveCounts}.
   */
  private void derivePriors() {
    Iterator it = headEvents.entrySet().iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
      HeadEvent event = (HeadEvent)entry.getKey();
      double count = entry.getDoubleValue();
      if (isRealWord(event.headWord()) && event.parent() != topSym) {
	priorEvents.add(new PriorEvent(event.headWord(), event.parent()),
			count);
      }
    }
  }

  /** Called by {@link #deriveCounts}. */
  private ProbabilityStructure getProbStructure(String classPrefix,
						String globalStructureNumber,
						String structureNumberProperty,
						String structureClassProperty)
    throws LinkageError, ExceptionInInitializerError, ClassNotFoundException,
    InstantiationException, IllegalAccessException {
    String structureNumber = Settings.get(structureNumberProperty);
    String structureClass = Settings.get(structureClassProperty);
    String className = null;
    if (structureClass != null)
      className = structureClass;
    else if (structureNumber != null)
      className = classPrefix + structureNumber;
    else
      className = classPrefix + globalStructureNumber;
    return (ProbabilityStructure)Class.forName(className).newInstance();
  }

  // widely-used utility method
  /** Returns <code>true</code> if <code>word</code> is not <code>null</code>
      and does not have {@link Training#traceTag()} as its part of speech
      nor have {@link Training#stopSym()} as its actual word. */
  private final boolean isRealWord(Word word) {
    return (word != null &&
	    word.tag() != traceTag &&
	    word.word() != stopWord.word() && word.word() != startWord.word());
  }


  // some utility methods for adding to maps


  /**
   * Adds <code>value</code> to the set of values to which
   * <code>key</code> is mapped (if <code>value</code> is not already in
   * that set) and increments the count of that value by 1.
   *
   * @param map the map of keys to sets of values, where each value has its
   * own count (<code>map</code> is actually a map of keys to maps of values
   * to counts)
   * @param key the key in <code>map</code> to associate with a set of values
   * with counts
   * @param value the value to add to the set of <code>key</code>'s values,
   * whose count is to be incremented by 1
   */
  public final static void addToValueCounts(Map map,
					    Object key,
					    Object value) {
    addToValueCounts(map, key, value, 1);
  }

  /**
   * Adds <code>value</code> to the set of values to which
   * <code>key</code> is mapped (if <code>value</code> is not already
   * in that set) and increments the count of that value by
   * <code>count</code>.
   *
   * @param map the map of keys to sets of values, where each value has its
   * own count (<code>map</code> is actually a map of keys to maps of values
   * to counts)
   * @param key the key in <code>map</code> to associate with a set of values
   * with counts
   * @param value the value to add to the set of <code>key</code>'s values,
   * whose count is to be incremented by <code>count</code>
   * @param count the amount by which to increment <code>value</code>'s count
   */
  public final static void addToValueCounts(Map map,
					    Object key,
					    Object value,
					    int count) {
    CountsTable valueCounts = (CountsTable)map.get(key);
    if (valueCounts == null) {
      valueCounts = new CountsTableImpl(2);
      map.put(key, valueCounts);
    }
    valueCounts.add(value, count);
  }

  // I/O methods

  /**
   * Writes the statistics and mappings collected by
   * {@link #train(SexpTokenizer,boolean,boolean)} to a human-readable text
   * file, by constructing a <code>Writer</code> around a stream around the
   * specified file and calling {@link #writeStats(Writer)}.
   *
   * @see #train(SexpTokenizer,boolean,boolean)
   * @see #writeStats(Writer)
   */
  public void writeStats(File file) throws IOException {
    Writer writer =
      new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),
						Language.encoding()),
			 Constants.defaultFileBufsize);
    writeStats(writer);
  }

  /**
   * A hook for subclasses to write out any additional top-level events,
   * or top-level events of a different, newly-defined type.  This default
   * implementation does nothing.
   *
   * @param writer
   * @throws IOException
   */
  public void writeStatsHook(Writer writer) throws IOException {

  }

  /**
   * Writes the statistics and mappings collected by
   * {@link #train(SexpTokenizer,boolean,boolean)} to a human-readable text
   * file.<br>
   * This method calls {@link #writeStatsHook(Writer)} just before terminating.
   *
   * @see #train(SexpTokenizer,boolean,boolean)
   * @see SymbolicCollectionWriter#writeMap(Map,Symbol,Writer)
   * @see CountsTable#output(String,Writer)
   */
  public void writeStats(Writer writer) throws IOException {
    nonterminals.output(nonterminalEventSym.toString(), writer);
    headEvents.output(headEventSym.toString(), writer);
    modifierEvents.output(modEventSym.toString(), writer);
    gapEvents.output(gapEventSym.toString(), writer);
    vocabCounter.output(vocabSym.toString(), writer);
    wordFeatureCounter.output(wordFeatureSym.toString(), writer);
    SymbolicCollectionWriter.writeMap(posMap, posMapSym, writer);
    SymbolicCollectionWriter.writeSet(prunedPreterms, prunedPretermSym, writer);
    SymbolicCollectionWriter.writeSet(prunedPunctuation, prunedPuncSym, writer);
    writeStatsHook(writer);
  }

  /**
   * Reads the statistics and observations from an output file in the format
   * created by {@link #writeStats(Writer)}.  Observations are one of several
   * types, all recorded as S-expressions where the first element is one of the
   * following symbols:
   * <ul>
   * <li>{@link #nonterminalEventSym}
   * <li>{@link #headEventSym}
   * <li>{@link #modEventSym}
   * <li>{@link #gapEventSym}
   * <li>{@link #posMapSym}
   * <li>{@link #vocabSym}
   * <li>{@link #wordFeatureSym}
   * </ul>
   *
   * @param file the file containing the S-expressions representing
   * top-level observations and their counts
   */
  public void readStats(File file)
    throws FileNotFoundException, UnsupportedEncodingException, IOException {
    readStats(getStandardSexpStream(file));
  }

  /**
   * Returns a new {@link SexpTokenizer} wrapped around the specified file
   * using the encoding specified by {@link Language#encoding()} and
   * a buffer size equal to {@link Constants#defaultFileBufsize}.
   * @param file the file around which to construct a {@link SexpTokenizer}
   * @return a new {@link SexpTokenizer} wrapped around the specified file
   * using the encoding specified by {@link Language#encoding()} and
   * a buffer size equal to {@link Constants#defaultFileBufsize}
   * @throws FileNotFoundException if the specified file cnanot be found
   * @throws UnsupportedEncodingException if the encoding specified by
   * {@link Language#encoding()} is unsupported
   * @throws IOException if there is a problem opening a stream for the
   * specified file
   */
  public static SexpTokenizer getStandardSexpStream(File file)
    throws FileNotFoundException, UnsupportedEncodingException, IOException {
    return new SexpTokenizer(file, Language.encoding(),
                             Constants.defaultFileBufsize);
  }

  /**
   * A hook for subclasses to read an event of a newly-defined type (called
   * by {@link #readStats(SexpTokenizer)}).  This method is responsible for
   * printing out any error messages if the specified event is improperly
   * formatted or is not recognized.  New event types must still have
   * the same general S-expression format requirements as the existing event
   * types of this class: they must be lists of length 2 or 3.<br>
   * The default implementation here simply prints an error message to
   * <code>System.err</code> indicating that the specified event is
   * an unrecognized event type.
   *
   * @param event the event to be read
   */
  public void readStatsHook(SexpList event) {
    Symbol name = event.symbolAt(0);
    System.err.println(className + ": error: unrecognized event type " + name +
		       "; event=" + event);
  }

  /**
   * Returns an iterator over {@link TrainerEvent} objects that were written
   * out in S-expression form.
   * @param tokenizer the S-expression reader from which to read
   * {@link TrainerEvent} objects that were serialized as S-expression
   * strings
   * @param type the type of {@link TrainerEvent} objects to retrive; the value
   * of this argument may be one of
   * <ul>
   * <li>{@link #nonterminalEventSym}
   * <li>{@link #headEventSym}
   * <li>{@link #modEventSym}
   * <li>{@link #gapEventSym}
   * <li>{@link #posMapSym}
   * <li>{@link #vocabSym}
   * <li>{@link #wordFeatureSym}
   * <li>{@link #prunedPretermSym}
   * <li>{@link #prunedPuncSym}
   * </ul>
   * @return an iterator over {@link TrainerEvent} objects that were written
   * out in S-expression form
   */
  public static Iterator getEventIterator(final SexpTokenizer tokenizer,
                                          final Symbol type) {
    return new Iterator() {
      SexpTokenizer tok = tokenizer;
      int intType = eventsToTypes.getEntry(type).getIntValue();
      int num = 0;
      int counter = 0;

      SexpList next = getNext();

      /**
       * Gets the Sexp of the next event that is of the correct type.
       * @return the Sexp of the next event that is of the correct type.
       */
      SexpList getNext() {
        if (!addGapInfo && type == gapEventSym)
          return null;
        Sexp curr;
        try {
          while ((curr = Sexp.read(tok)) != null) {
            SexpList event = curr.list();
            Symbol name = event.symbolAt(0);
            MapToPrimitive.Entry entry = eventsToTypes.getEntry(name);
            if (entry != null && entry.getIntValue() == intType) {
              num++;
	      /*
              counter++;
              if (counter == 10000) {
                System.err.println("num=" + num);
                counter = 0;
              }
	      */
              return event;
            }
          }
        }
        catch (IOException ioe) {
          System.err.println("TrainerEvent iterator: " + ioe);
        }
        //System.err.println("num=" + num);
        return null;
      }

      public boolean hasNext() {
        if (!addGapInfo && type == gapEventSym)
          return false;
        else
          return next != null;
      }
      public Object next() {
        if (next == null)
          return new NoSuchElementException();
        TrainerEvent event = null;
        switch (intType) {
          case headEventType:
            event = new HeadEvent(next.get(1));
            break;
          case modEventType:
            event = new ModifierEvent(next.get(1));
            break;
          case gapEventType:
            event = new GapEvent(next.get(1));
            break;
        }
        double count = Double.parseDouble(next.get(2).toString());
        next = getNext();
        return new EventEntry(event, count);
      }
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /**
   * Reads the observations and their counts contained in the specified
   * S-expression tokenization stream.  The S-expressions contained in the
   * specified stream are expected to be in the format output by
   * {@link #writeStats(Writer)}.  Observations are one of several
   * types, all recorded as S-expressions where the first element is one of the
   * following symbols:
   * <ul>
   * <li>{@link #nonterminalEventSym}
   * <li>{@link #headEventSym}
   * <li>{@link #modEventSym}
   * <li>{@link #gapEventSym}
   * <li>{@link #posMapSym}
   * <li>{@link #vocabSym}
   * <li>{@link #wordFeatureSym}
   * </ul>
   *
   * @param tok the S-expression tokenization stream from which to read
   * top-level counts
   * @throws IOException if the underlying stream throws an <tt>IOException</tt>
   */
  public void readStats(SexpTokenizer tok) throws IOException {
    readStats(tok, 0);
  }

  /**
   * Reads at most the specified number of observations and their counts
   * contained in the specified S-expression tokenization stream.  The
   * S-expressions contained in the specified stream are expected to be
   * in the format output by {@link #writeStats(Writer)}.  Observations are
   * one of several types, all recorded as S-expressions where the first
   * element is one of the following symbols:
   * <ul>
   * <li>{@link #nonterminalEventSym}
   * <li>{@link #headEventSym}
   * <li>{@link #modEventSym}
   * <li>{@link #gapEventSym}
   * <li>{@link #posMapSym}
   * <li>{@link #vocabSym}
   * <li>{@link #wordFeatureSym}
   * </ul>
   *
   * @param tok the S-expression tokenization stream from which to read
   * top-level counts
   * @param maxEventsToRead the maximum number of events to read from the
   * specified stream; if the value of this parameter is less than <tt>1</tt>,
   * then all observations are read from the underlying stream, and the
   * behavior of this method is identical to {@link #readStats(SexpTokenizer)}
   * @throws IOException if the underlying stream throws an <tt>IOException</tt>
   */
  public void readStats(SexpTokenizer tok, int maxEventsToRead)
    throws IOException {
    Map canonicalMap = new danbikel.util.HashMap(100003, 1.5f);
    Sexp curr = null;
    int i = 1;
    for ( ; (maxEventsToRead < 1 || i <= maxEventsToRead) &&
            (curr = Sexp.read(tok)) != null; i++) {
      if (curr.isSymbol() ||
	  (curr.isList() &&
	   curr.list().length() != 2 && curr.list().length() != 3)) {
	System.err.println(className + ": error: S-expression No. " + i +
			   " is not in the correct format:\n\t" + curr);
	continue;
      }
      SexpList event = curr.list();
      Symbol name = event.symbolAt(0);
      double count = -1.0;
      MapToPrimitive.Entry entry = eventsToTypes.getEntry(name);
      if (entry == null) {
	readStatsHook(event);
	continue;
      }
      switch (entry.getIntValue()) {
      case nonterminalEventType:
	count = Double.parseDouble(event.symbolAt(2).toString());
	nonterminals.add(event.get(1), count);
	break;
      case headEventType:
	count = Double.parseDouble(event.symbolAt(2).toString());
	if (count >= countThreshold) {
	  HeadEvent headEvent = new HeadEvent(event.get(1));
	  headEvent.canonicalize(canonicalMap);
	  addHeadEvent(headEvent, count);
	}
	break;
      case modEventType:
	count = Double.parseDouble(event.symbolAt(2).toString());
	if (count >= countThreshold) {
	  ModifierEvent modEvent = new ModifierEvent(event.get(1));
	  modEvent.canonicalize(canonicalMap);
	  addModifierEvent(modEvent, count);
	}
	break;
      case gapEventType:
	count = Double.parseDouble(event.symbolAt(2).toString());
	if (count >= countThreshold) {
	  GapEvent gapEvent = new GapEvent(event.get(1));
	  gapEvent.canonicalize(canonicalMap);
	  addGapEvent(gapEvent, count);
	}
	break;
      case posMapType:
	//posMap.put(event.get(1), event.get(2));
	Symbol word = event.symbolAt(1);
	SexpList posList = event.listAt(2);
	int numPos = posList.length();
	for (int posIdx = 0; posIdx < numPos; posIdx++)
	  addToPosMap(word, posList.symbolAt(posIdx));
	break;
      case vocabType:
	count = Double.parseDouble(event.symbolAt(2).toString());
	vocabCounter.add(event.get(1), count);
	break;
      case wordFeatureType:
	count = Double.parseDouble(event.symbolAt(2).toString());
	wordFeatureCounter.add(event.get(1), count);
	break;
      case prunedPretermType:
	prunedPreterms = new HashSet();
	SexpList pretermList = event.listAt(1);
	int pretermListLen = pretermList.length();
	for (int j = 0; j < pretermListLen; j++)
	  prunedPreterms.add(pretermList.get(j));
	break;
      case prunedPuncType:
	prunedPunctuation = new HashSet();
	SexpList puncList = event.listAt(1);
	int puncListLen = puncList.length();
	for (int j = 0; j < puncListLen; j++)
	  prunedPunctuation.add(puncList.get(j));
	break;
      }
    }
    System.err.println("Read " + (i - 1) + " events.");
  }

  /**
   * A hook that gets called by {@link #main} after all observations are
   * collected via any calls to {@link #readStats(File)},
   * {@link #readStats(SexpTokenizer)} and
   * {@link #train(SexpTokenizer,boolean,boolean)}.  The default implementation
   * does nothing.
   */
  public void doneCollectingObservations() {
  }

  /**
   * Writes the internal {@link ModelCollection} object to the specified output
   * file, writing a header containing the names of the training input file and
   * training output file.
   *
   * @param objectOutputFilename   the output file to which to write the
   *                               internal {@link ModelCollection} object
   *                               constructed by this trainer
   * @param trainingInputFilename  the name of the input file of training parse
   *                               trees from which events and counts were
   *                               collected
   * @param trainingOutputFilename the name of the training output file of
   *                               top-level (maximal context) events
   * @throws FileNotFoundException if the specified output filename cannot be
   *                               created
   * @throws IOException           if there is a problem writing to the stream
   *                               of the specified output file
   */
  public void writeModelCollection(String objectOutputFilename,
				   String trainingInputFilename,
				   String trainingOutputFilename)
    throws FileNotFoundException, IOException {
    OutputStream os = new FileOutputStream(objectOutputFilename);
    if (objectOutputFilename.endsWith(".gz"))
      os = new GZIPOutputStream(os);
    int bufSize = Constants.defaultFileBufsize;
    BufferedOutputStream bos = new BufferedOutputStream(os, bufSize);
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    writeModelCollection(oos,
			 trainingInputFilename, trainingOutputFilename);
  }

  /**
   * Writes the internal {@link ModelCollection} object to the specified output
   * stream, writing a header containing the names of the training input file
   * and training output file.
   *
   * @param oos                    the output stream to which to write the
   *                               internal {@link ModelCollection} object
   *                               constructed by this trainer
   * @param trainingInputFilename  the name of the input file of training parse
   *                               trees from which events and counts were
   *                               collected
   * @param trainingOutputFilename the name of the training output file of
   *                               top-level (maximal context) events
   * @throws IOException if there is a problem writing to the stream of the
   *                     specified output file
   */
  public void writeModelCollection(ObjectOutputStream oos,
				   String trainingInputFilename,
				   String trainingOutputFilename)
    throws IOException {
    Settings.store(oos);
    oos.writeObject(trainingInputFilename);
    oos.writeObject(trainingOutputFilename);
    oos.writeObject(modelCollection);
    oos.close();
  }

  /**
   * Sets the internal {@link #modelCollection} data member of this class to the
   * object of that type loaded from the specified file.
   *
   * @param objectInputFilename the object from which to load a {@link
   *                            ModelCollection}
   * @throws ClassNotFoundException if the concrete type of {@link
   *                                ModelCollection} read from the specified
   *                                file cannot be found
   * @throws IOException            if there is a problem reading from the
   *                                stream of the specified file
   * @throws OptionalDataException  if there is a problem reading primitive data
   *                                associated with the {@link ModelCollection}
   *                                object read from the specified file
   */
  public void setModelCollection(String objectInputFilename)
    throws ClassNotFoundException, IOException, OptionalDataException {
    modelCollection = loadModelCollection(objectInputFilename);
  }

  /**
   * Loads the {@link ModelCollection} from the specified file.
   *
   * @param objectInputFilename the name of the Java serialized object file from
   *                            which to load a {@link ModelCollection}
   *                            instance; the file must contain a series of
   *                            header objects as produced by {@link
   *                            #writeModelCollection(String,String,String)}
   * @return the {@link ModelCollection} object contained in the specified file
   *
   * @throws ClassNotFoundException if the concrete type of the {@link
   *                                ModelCollection} or any of the header
   *                                objects in the specified file cannot be
   *                                found
   * @throws IOException            if there is a problem reading from the
   *                                specified file
   * @throws OptionalDataException  if there is a problem reading primitive data
   *                                associated with an object from the object
   *                                input stream created from the specified
   *                                file
   */
  public static ModelCollection loadModelCollection(String objectInputFilename)
    throws ClassNotFoundException, IOException, OptionalDataException {
    InputStream is = new FileInputStream(objectInputFilename);
    if (objectInputFilename.endsWith(".gz"))
      is = new GZIPInputStream(is);
    int bufSize = Constants.defaultFileBufsize * 10;
    BufferedInputStream bfi = new BufferedInputStream(is, bufSize);
    ObjectInputStream ois = new ObjectInputStream(bfi);
    System.err.println("\nLoading derived counts from object file \"" +
		       objectInputFilename + "\":");
    return loadModelCollection(ois);
  }

  /**
   * Sets the internal {@link #modelCollection} member of this class to the
   * instance loaded from the specified input stream.
   *
   * @param ois an object input stream containing a series of header objects and
   *            ultimately a {@link ModelCollection} instance
   * @throws ClassNotFoundException if the concrete type of the {@link
   *                                ModelCollection} or any of the header
   *                                objects in the specified input stream cannot
   *                                be found
   * @throws IOException            if there is a problem reading from the
   *                                specified input stream
   * @throws OptionalDataException  if there is a problem reading primitive data
   *                                associated with an object from the specified
   *                                object input stream
   */
  public void setModelCollection(ObjectInputStream ois)
    throws ClassNotFoundException, IOException, OptionalDataException {
    modelCollection = loadModelCollection(ois);
  }

  /**
   * Loads the {@link ModelCollection} from the specified file.
   *
   * @param ois the object input stream from which to load a {@link
   *            ModelCollection} instance; the stream must contain a series of
   *            header objects, as produced by
   *            {@link #writeModelCollection(String,String,String)}
   * @return the {@link ModelCollection} object contained in the specified file
   *
   * @throws ClassNotFoundException if the concrete type of the {@link
   *                                ModelCollection} or any of the header
   *                                objects in the specified file cannot be
   *                                found
   * @throws IOException            if there is a problem reading from the
   *                                specified file
   * @throws OptionalDataException  if there is a problem reading primitive data
   *                                associated with an object from the object
   *                                input stream created from the specified
   *                                file
   */
  public static ModelCollection loadModelCollection(ObjectInputStream ois)
    throws ClassNotFoundException, IOException, OptionalDataException {
    scanModelCollectionObjectFile(ois, System.err);
    return (ModelCollection)ois.readObject();
  }

  /**
   * Scans the object file and prints out the information contained in its
   * header objects.  The specified object file must contain serialized objects
   * of the type and in the order produced by {@link #writeModelCollection(String,String,String)}.
   *
   * @param scanObjectFilename the object whose header is to be scanned
   * @param os                 the output stream to which to print information
   * @throws ClassNotFoundException if any of the concrete types of the header
   *                                objects in the specified file cannot be
   *                                found
   * @throws IOException            if there is a problem reading from the
   *                                stream created from the specified file
   * @throws OptionalDataException  if there is a problem of extra primtive data
   *                                when deserializing an object from the object
   *                                input stream created from the specified
   *                                file
   */
  public static void scanModelCollectionObjectFile(String scanObjectFilename,
						   OutputStream os)
    throws ClassNotFoundException, IOException, OptionalDataException {
    InputStream is = new FileInputStream(scanObjectFilename);
    if (scanObjectFilename.endsWith(".gz"))
      is = new GZIPInputStream(is);
    int bufSize = Constants.defaultFileBufsize;
    BufferedInputStream bis = new BufferedInputStream(is, bufSize);
    ObjectInputStream ois = new ObjectInputStream(bis);
    System.err.println("\nInformation from object file \"" +
		       scanObjectFilename + "\":");
    scanModelCollectionObjectFile(ois, os);
  }

  /**
   * Scans the object file and prints out the information contained in its
   * header objects.  The specified object file must contain serialized objects
   * of the type and in the order produced by {@link #writeModelCollection(String,String,String)}.
   *
   * @param ois the object input stram whose header objects are to be scanned
   * @param os  the output stream to which to print information
   * @throws ClassNotFoundException if any of the concrete types of the header
   *                                objects in the specified stream cannot be
   *                                found
   * @throws IOException            if there is a problem reading from the
   *                                specified stream
   * @throws OptionalDataException  if there is a problem of extra primtive data
   *                                when deserializing an object from the
   *                                specified object input stream
   */
  public static void scanModelCollectionObjectFile(ObjectInputStream ois,
						   OutputStream os)
    throws ClassNotFoundException, IOException, OptionalDataException {
    PrintStream ps = new PrintStream(os);
    ps.println("Settings in effect during training");
    ps.println("------------------------------");
    Properties props = (Properties)ois.readObject();
    Settings.storeSorted(props, os, " " + Settings.progName + " v" +
			 Settings.version);
    ps.println("------------------------------");
    ps.println("Settings different during training than now");
    ps.println("------------------------------");
    Iterator propIt = props.entrySet().iterator();
    while (propIt.hasNext()) {
      Map.Entry entry = (Map.Entry)propIt.next();
      String setting = (String)entry.getKey();
      String trainingValue = (String)entry.getValue();
      String currValue = Settings.get(setting);
      if (currValue == null || !currValue.equals(trainingValue))
        ps.println(setting + "\n\twas " + trainingValue +
                   "\n\tis " + currValue);
    }
    ps.println("------------------------------");
    String trainingInputFilename = (String)ois.readObject();
    if (trainingInputFilename != null)
      ps.println("training input file: \"" + trainingInputFilename + "\".");
    String trainingOutputFilename = (String)ois.readObject();
    if (trainingOutputFilename != null)
      ps.println("training output file: \"" + trainingOutputFilename + "\".");
  }

  // main method stuff

  /**
   * The usage for the main method of this class. Please run
   * <code>java danbikel.parser.Trainer -help</code> to display the complete
   * usage of this class.
   */
  protected final static String[] usageMsg = {
    "usage: [-help] [-sf <settings file> | --settings <settings file>]",
    "\t[-it | --incremental-training]",
    "\t[-l <input file> [-l <trainer event input file>] ]",
    "\t[-scan <derived data scan file>]",
    "\t[-i <training file>] [-o <output file>]",
    "\t[-od <derived data output file>] [-ld <derived data input file>]",
    "\t[ --strip-outer-parens | --dont-strip-outer-parens | -auto ]",
    "where",
    "\t-help prints this usage message",
    "\t<settings file> is an optionally-specified settings file",
    "\t-it|--incremental-training indicates to read and derive counts",
    "\t\tincrementally when both an <input file> and a",
    "\t\t<derived data output file> are specified",
    "\t<training file> is a Treebank file containing training parse trees",
    "\t<output file> is the events output file (use \"-\" for stdout)",
    "\t<input file> is an <output file> from a previous run to load",
    "\t\tor, if <trainer event input file> is specified using a second",
    "\t\t\"-l\", the <input file> is expected to contain everything from a",
    "\t\tprevious training run except counts for head, modifier",
    "\t\tand gap events",
    "\t<derived data {scan,input,output} file> are Java object files",
    "\t\tcontaining information about and all derived counts from a",
    "\t\ttraining run",
    "\t-scan indicates to scan the first few informational objects of",
    "\t\t<derived data scan file> and print them out to stderr",
    "\t-od indicates to derive counts from observations from <training file>",
    "\t\tand output them to <derived data output file>",
    "\t-ld indicates to load derived counts from <derived data input file>",
    "\t--strip-outer-parens indicates to strip the outer parens off training",
    "\t\ttrees (appropriate for Treebank II-style annotated parse trees)",
    "\t--dont-strip-outer-parens indicates not to strip the outer parens off",
    "\t\ttraining parse trees",
    "\t-auto indicates to determine automatically whether to strip outer",
    "\t\tparens off training parse trees (default)"
  };

  private static void usage() {
    for (int i = 0; i < usageMsg.length; i++)
      System.err.println(usageMsg[i]);
    System.exit(1);
  }

  /**
   * Incrementally updates derived model counts by reading chunks of {@link
   * TrainerEvent} objects from the specified input file.  The number of {@link
   * TrainerEvent} objects read at a time (the chunk size) is determined by the
   * value of the {@link Settings#maxEventChunkSize}.
   *
   * @param trainer       the {@link Trainer} instance for which incremental
   *                      training is to be performed
   * @param inputFilename the file containing observations to be read by the
   *                      {@link #readStats(SexpTokenizer,int)} method
   * @throws FileNotFoundException        if the specified file cannot be found
   * @throws UnsupportedEncodingException if the encoding used to read
   *                                      characters from the specified file is
   *                                      not supported
   * @throws IOException                  if there is a problem reading from the
   *                                      specified file
   */
  protected static void incrementallyTrain(Trainer trainer,
                                           String inputFilename)
    throws FileNotFoundException, UnsupportedEncodingException, IOException {
    int eventChunkSize = Settings.getInteger(Settings.maxEventChunkSize);
    SexpTokenizer inputFileTok =
      getStandardSexpStream(new File(inputFilename));
    FlexibleMap canonical = new danbikel.util.HashMap(100003, 1.5f);
    while (inputFileTok.ttype != StreamTokenizer.TT_EOF) {
      trainer.readStats(inputFileTok, eventChunkSize);
      trainer.deriveCounts(false, canonical);
      trainer.clearEventCounters();
    }
  }


  /**
   * Takes arguments according to the usage as specified in {@link #usageMsg}.
   * Please run <code>java danbikel.parser.Trainer -help</code> to display the
   * complete usage of this class.
   */
  public static void main(String[] args) {
    boolean stripOuterParens = false, auto = true;
    boolean incrementalTraining = false;
    String trainingFilename = null, outputFilename = null, inputFilename = null;
    String trainerEventInputFilename = null;
    String settingsFilename = null, objectOutputFilename = null;
    String objectInputFilename = null, scanObjectFilename = null;
    // process arguments
    for (int i = 0; i < args.length; i++) {
      if (args[i].charAt(0) == '-') {
	if (args[i].equals("-i")) {
	  if (i + 1 == args.length)
	    usage();
	  trainingFilename = args[++i];
	}
	else if (args[i].equals("-o")) {
	  if (i + 1 == args.length)
	    usage();
	  outputFilename = args[++i];
	}
	else if (args[i].equals("-od")) {
	  if (i + 1 == args.length)
	    usage();
	  objectOutputFilename = args[++i];
	}
	else if (args[i].equals("-ld")) {
	  if (i + 1 == args.length)
	    usage();
	  objectInputFilename = args[++i];
	}
	else if (args[i].equals("-scan")) {
	  if (i + 1 == args.length)
	    usage();
	  scanObjectFilename = args[++i];
	}
	else if (args[i].equals("-sf") || args[i].equals("--settings")) {
	  if (i + 1 == args.length)
	    usage();
	  settingsFilename = args[++i];
	}
	else if (args[i].equals("-l")) {
	  if (i + 1 == args.length)
	    usage();
          if (inputFilename == null)
            inputFilename = args[++i];
          else
            trainerEventInputFilename = args[++i];
	}
	else if (args[i].equals("--strip-outer-parens")) {
	  stripOuterParens = true;
	  auto = false;
	}
	else if (args[i].equals("--dont-strip-outer-parens")) {
	  stripOuterParens = false;
	  auto = false;
	}
	else if (args[i].equals("-auto"))
	  auto = true;
        else if (args[i].equals("-it") ||
                 args[i].equals("--incremental-training"))
          incrementalTraining = true;
	else if (args[i].equals("-help"))
	  usage();
	else {
	  System.err.println("\nunrecognized flag: " + args[i]);
	  usage();
	}
      }
    }
    if (scanObjectFilename == null && objectInputFilename == null &&
	trainingFilename == null && outputFilename == null &&
	inputFilename == null)
      usage();

    if (trainingFilename != null && trainerEventInputFilename != null) {
      System.err.println("\nerror: cannot specify both a trainer event input " +
                         "file and an\n\tobservation input file\n");
      usage();
    }

    if (incrementalTraining &&
        !(inputFilename != null && objectOutputFilename != null)) {
      System.err.println("\nerror: must specify both <input file> and " +
                         "<derived data output file>\n\twith -it option\n");
      usage();
    }

    if (trainerEventInputFilename != null && objectOutputFilename == null) {
      System.err.println("\nerror: must specify a <derived data output " +
                         "file> when specifying <trainer event input file> " +
                         "with second \"-l\"\n");
      usage();
    }

    try {
      if (settingsFilename != null)
	Settings.load(settingsFilename);
    }
    catch (IOException ioe) {
      System.err.println("\nwarning: problem loading settings file \"" +
			 settingsFilename + "\"\n");
    }

    String encoding = Language.encoding();

    Trainer trainer = null;
    try {
      trainer = (Trainer)trainerClass.newInstance();
    }
    catch (Exception e) {
      System.err.println(e);
      System.exit(1);
    }

    try {
      if (scanObjectFilename != null) {
	try {
	  trainer.scanModelCollectionObjectFile(scanObjectFilename,
						System.err);
	}
	catch (ClassNotFoundException cnfe) {
	  System.err.println(cnfe);
	}
	catch (OptionalDataException ode) {
	  System.err.println(ode);
	}
      }

      Time overallTime = new Time();
      Time trainingTime = new Time();

      if (objectOutputFilename != null)
        trainer.createModelObjects();

      if (inputFilename != null) {
        Time time = new Time();
        System.err.println("Loading observations from \"" + inputFilename +
                           "\".");
        if (incrementalTraining) {
          // user can only specify to do incremental training when user
          // wants to derive counts
          incrementallyTrain(trainer, inputFilename);
        }
        else {
          trainer.readStats(new File(inputFilename));
        }
        if (trainerEventInputFilename != null) {
          // user may only specify a trainer event input filename along with
          // an object output file, so we know that user must want to derive
          // counts from this trainer event input file
          incrementallyTrain(trainer, trainerEventInputFilename);
        }
        System.err.println("Finished reading observations in " + time + ".");
      }

      if (trainingFilename != null) {
	System.err.println("Training from trees in \"" +
			   trainingFilename + "\".");
	Time time = new Time();
	trainer.train(new SexpTokenizer(trainingFilename, encoding,
					Constants.defaultFileBufsize),
		      auto, stripOuterParens);
	System.err.println("Observation collection completed in " + time + ".");
      }


      if (inputFilename != null || trainingFilename != null) {
	trainer.doneCollectingObservations();
      }

      if (outputFilename != null) {
	OutputStream os =
	  (outputFilename.equals("-") ?
	   (OutputStream)System.out : new FileOutputStream(outputFilename));
	if (outputFilename.endsWith(".gz"))
	  os = new GZIPOutputStream(os);
	Writer writer = new BufferedWriter(new OutputStreamWriter(os, encoding),
					   Constants.defaultFileBufsize);
	System.err.println("Writing observations to output file \"" +
			   outputFilename + "\".");
	Time time = new Time();
	trainer.writeStats(writer);
	writer.close();
	System.err.println("Finished writing observations in " + time + ".");
      }

      if (trainingFilename != null && outputFilename != null &&
	  inputFilename != null) {
	System.err.println("Training completed in " + trainingTime + ".");
	System.err.print("Cleaning symbol table...");
	System.err.flush();
	Symbol.clean();
	System.err.println("done.");
      }

      if (objectOutputFilename != null) {
	System.err.println("Deriving counts.");
	Time time1 = new Time();
	trainer.deriveCounts();
	System.err.println("Finished deriving counts in " + time1 + ".\n" +
			   "Writing out all derived counts to object file \"" +
			   objectOutputFilename + "\".");
	Time time2 = new Time();
	trainer.writeModelCollection(objectOutputFilename,
				     trainingFilename, outputFilename);
	System.err.println("Finished outputting derived counts in " +
			   time2 + ".");
      }

      if (objectInputFilename != null) {
	try {
	  System.err.println("Loading derived counts from \"" +
			     objectInputFilename + "\".");
	  Time time = new Time();
	  trainer.setModelCollection(objectInputFilename);
	  System.err.println("Finished loading derived counts in "+ time +".");
	  System.err.print("gc ... ");
	  System.err.flush();
	  System.gc();
	  System.err.println("done");
	  Runtime runtime = Runtime.getRuntime();
	  System.err.println("Memory usage: " +
			     (runtime.totalMemory() - runtime.freeMemory())+
			     " bytes.");
	}
	catch (ClassNotFoundException cnfe) {
	  System.err.println(cnfe);
	}
	catch (OptionalDataException ode) {
	  System.err.println(ode);
	}
      }

      System.err.println("\nTotal elapsed time: " + overallTime + ".");
      System.err.println("\nHave a nice day!");
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

  // unused utility method

  /**
   * Converts a map whose key-value pairs are of type Object-Set, where the
   * sets contain Sexp objects, to be key-value pairs of type Object-SexpList,
   * where the newly-created SexpList objects contain all the Sexp's that were
   * in each of the sets.
   */
  private final static void convertValueSetsToSexpLists(Map map) {
    Iterator mapIterator = map.keySet().iterator();
    while (mapIterator.hasNext()) {
      Object key = mapIterator.next();
      Set set = (Set)map.get(key);
      SexpList list = new SexpList(set.size());
      Iterator setIterator = set.iterator();
      while (setIterator.hasNext()) {
	list.add((Sexp)setIterator.next());
      }
      map.put(key, list);
    }
  }

  private void countUniqueBigrams() {
    HashSet bigramSet = new HashSet(modifierEvents.size());
    Iterator modEvents = modifierEvents.keySet().iterator();
    while (modEvents.hasNext()) {
      TrainerEvent event = (TrainerEvent)modEvents.next();
      bigramSet.add(new SymbolPair(event.headWord().word(),
				   event.modHeadWord().word()));
    }
    System.err.println("num unique bigrams " +
		       "(including traces, start and stop words): " +
		       bigramSet.size());
  }

  private void outputCollins() {
    System.err.println("Outputting Collins-format head events.");
    outputCollins(headEvents, nonPreterm);
    System.err.println("Outputting Collins-format modifier events.");
    outputCollins(modifierEvents, allPass);
  }

  private void outputCollins(CountsTable eventCounts, Filter filter) {
    Iterator modEvents = eventCounts.entrySet().iterator();
    while (modEvents.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)modEvents.next();
      TrainerEvent event = (TrainerEvent)entry.getKey();
      if (!filter.pass(event))
        continue;
      int count = (int)entry.getDoubleValue();
      outputCollins(event, count);
    }
  }

  private void outputCollins(TrainerEvent event) {
    outputCollins(event, 1);
  }
  private void outputCollins(TrainerEvent event, int count) {
    String collinsStr =
      danbikel.parser.util.TrainerEventToCollins.trainerEventToCollins(event);
    if (collinsStr != null)
      for (int i = 0; i < count; i++)
	System.out.println(collinsStr);
  }
}
