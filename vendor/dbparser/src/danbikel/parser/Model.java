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
import java.io.*;
import java.util.*;
import java.text.*;

/**
 * This class computes the probability of generating an output element of this
 * parser, where an output element might be, for example, a word, a part of
 * speech tag, a nonterminal label or a subcat frame.  It derives counts from
 * top-level <code>TrainerEvent</code> objects, storing these derived counts in
 * its internal data structures.  The derived counts are necessary for the
 * smoothing of the top-level probabilities used by the parser, and the
 * particular structure of those levels of smoothing (or, less accurately,
 * back-off) are specified by the <code>ProbabilityStructure</code> argument to
 * the {@link #Model(ProbabilityStructure) constructor} and to the {@link
 * #estimateLogProb(int,TrainerEvent)} method.
 * <p/>
 * <b>N.B.</b>: While the name of this class is &ldquo;Model&rdquo;, more
 * strictly speaking it computes the probabilities for an entire class of
 * parameters used by the overall parsing model.  As such&mdash;using a looser
 * definition of the term &ldquo;model&rdquo;&mdash;this class can be considered
 * to represent a &ldquo;submodel&rdquo;, in that it contains a model of the
 * generation of a particular type of output element of this parser.
 *
 * @see ProbabilityStructure
 */
public class Model implements Serializable {
  // constants
  private final static boolean verboseDebug = false;
  /**
   * The value of this constant determines whether {@link
   * #estimateProb(ProbabilityStructure,TrainerEvent)} emits a warning when it
   * encounters a history for which there is a saved smoothing parameter but
   * was not an observed history as far as the current model is concerned.
   * When using smoothing parameters from another training run, it is typical
   * to be operating with a model trained from the same data.  In such a case,
   * the set of history contexts observed during the training run that produced
   * the smoothing parameters would be identical to the set of history contexts
   * encountered when training again on that same data.  However, there are
   * circumstances when a history context observed in the smoothing training
   * run would not be observed in the subsequent training run, such as when
   * performing EM there is, for example, a long sentence with lots of
   * structure whose total inside probability mass is less than {@link
   * Double#MIN_VALUE}.  In such a case, the {@link EMDecoder} will issue an
   * underflow warning and not emit any expected events for that sentence.  If
   * a history context was only observed in the one or more sentences that have
   * underflow problems in a particular EM iteration, then it will effectively
   * not be observed in that iteration, and therefore not in any subsequent
   * iteration.
   */
  protected final static boolean warnSmoothingHasHistoryNotInTraining = false;
  /**
   * Indicates whether to set {@link #counts} to <code>null</code> just before
   * writing this model object to an {@link ObjectOutputStream}.  Normally,
   * this boolean should be <tt>true</tt>, but setting it to <tt>false</tt> can
   * be useful for debugging purposes.
   *
   * @see AnalyzeDisns
   */
  protected final static boolean deleteCountsWhenPrecomputingProbs = true;
  /** The boolean value of {@link Settings#precomputeProbs}, cached here
      for convenience. */
  protected static boolean precomputeProbs =
    Settings.getBoolean(Settings.precomputeProbs);
  private static boolean deficientEstimation =
    Settings.getBoolean(Settings.collinsDeficientEstimation);
  /**
   * A constant that indicates whether this {@link Model} should perform
   * probability caching.  This constant is usually <code>true</code>,
   * but may redefined as <code>false</code> for debugging purposes
   * (recompilation is necessary after redefining this constant).
   */
  protected final static boolean useCache = true;
  private final static int minCacheSize = 1000;
  private final static boolean doGCBetweenCanonicalizations = false;
  /**
   * If <tt>true</tt>, indicates that {@link #backOffMap} should not be set to
   * null after probabilities have been precomputed, which means that it will
   * be saved with this {@link Model} instance (for debugging purposes);
   * otherwise, {@link #backOffMap} is set to <code>null</code> just after
   * precomputation of probabilities.  Normally, the value of this boolean
   * should be <tt>false</tt>.  The value of this boolean is only consulted
   * when {@link Settings#precomputeProbs} is <tt>true</tt>.
   */
  protected final static boolean saveBackOffMap = false;
  /**
   * Indicates whether the {@link #histBackOffMap} should be created when
   * precomputing probabilities and saved with this {@link Model}
   * for debugging purposes.  Normally, the value of this boolean should be
   * <tt>false</tt>. The value of this boolean is only consulted when
   * {@link Settings#precomputeProbs} is <tt>true</tt>.
   *
   * @see AnalyzeDisns
   */
  protected final static boolean saveHistBackOffMap = false;

  /**
   * Caches the value of {@link Settings#modelDoPruning}.
   */
  protected static boolean globalDoPruning =
    Settings.getBoolean(Settings.modelDoPruning);

  /**
   * Caches the <tt>double</tt> value of {@link Settings#modelPruningThreshold}.
   */
  protected static double pruningThreshold =
    Settings.getDouble(Settings.modelPruningThreshold);

  /**
   * Indicates whether the method {@link #pruneHistoriesAndTransitions()} will
   * output pruned events to a special pruned event log file.
   */
  protected final static boolean printPrunedEvents = true;
  /**
   * Indicates whether the method {@link #pruneHistoriesAndTransitions()} will
   * output events that were not pruned to a special pruned event log file.
   */
  protected final static boolean printUnprunedEvents = true;

  private final static int structureMapArrSize = 1000;

  private static NumberFormat doubleNF = NumberFormat.getInstance();
  static {
    doubleNF.setMinimumFractionDigits(3);
    doubleNF.setMaximumFractionDigits(3);
    Settings.Change change = new Settings.Change() {
      public void update(Map<String, String> changedSettings) {
	precomputeProbs =
	  Settings.getBoolean(Settings.precomputeProbs);
	deficientEstimation =
	  Settings.getBoolean(Settings.collinsDeficientEstimation);
	globalDoPruning =
	  Settings.getBoolean(Settings.modelDoPruning);
	pruningThreshold =
	  Settings.getDouble(Settings.modelPruningThreshold);
      }
    };
    Settings.register(Model.class, change, null);
  }

  // data members

  /** The probability structure for this model to use. */
  protected ProbabilityStructure structure;
  // the prob structures for individual clients in a multithreaded environment
  private ProbabilityStructure[] structureMapArr =
    new ProbabilityStructure[structureMapArrSize];
  private Map structureMap = new danbikel.util.HashMap();
  private IntCounter idInt = new IntCounter();
  // some handles on info available from structure object
  /**
   * A cached copy of the name of the concrete type of the {@link
   * ProbabilityStructure} instance used by this model.
   */
  protected String structureClassName;
  /**
   * The value of {@link #structureClassName} but without the package
   * qualification.
   */
  protected String shortStructureClassName;
  /**
   * A cached copy of the number of back-off levels in the {@link
   * ProbabilityStructure} used by this model.
   *
   * @see ProbabilityStructure#numLevels()
   */
  protected int numLevels;
  /**
   * A cached copy of the smoothing factors of the {@link ProbabilityStructure}
   * used by this model.  This array is of size {@link #numLevels}.
   *
   * @see ProbabilityStructure#lambdaFudge(int)
   */
  protected double[] lambdaFudge;
  /**
   * A cached copy of the smoothing terms of the {@link ProbabilityStructure}
   * used by this model.  This array is of size {@link #numLevels}.
   *
   * @see ProbabilityStructure#lambdaFudgeTerm(int)
   */
  protected double[] lambdaFudgeTerm;
  /**
   * A cached copy of the smoothing penalty factors contained in the
   * {@link ProbabilityStructure} used by this model.  This array is
   * of size equal to {@link #numLevels}.
   *
   * @see ProbabilityStructure#lambdaPenalty
   */
  protected double[] lambdaPenalty;
  /**
   * The values of {@link #lambdaPenalty} but modified such that<br>
   * <code>logOneMinusLambdaPenalty[i] = Math.log(1 - lambdaPenalty[i])</code>
   * <br>for all <code>i: 0 &le; i &lt; lambdaPenalty.size</code>.
   */
  protected double[] logOneMinusLambdaPenalty;
  /**
   * The derived event counts used to estimate probabilities of this model.
   */
  protected CountsTrio[] counts;
  private int numCanonicalizableEvents = 0;
  /** Indicates whether to report to stderr what this class is doing. */
  protected boolean verbose = true;

  // for the storage of precomputed probabilities and lambdas
  /**
   * Precomputed probabilities for each back-off level of this model.  The keys
   * of each of the {@link HashMapDouble} maps in this array are {@link
   * Transition} objects.
   */
  protected HashMapDouble[] precomputedProbs;
  /**
   * Precomputed lambdas for each back-off level of this model.  The keys of
   * each of the {@link HashMapDouble} maps in this array are {@link Event}
   * instances.
   * <p/>
   * For the modified Witten-Bell smoothing method used by this class, the
   * values of the maps of this array are actually the log of one minus the
   * lambda of a particular event at a particular back-off level, for ease of
   * computing a smoothed estimate.  That is, if <code>event</code> is some
   * history context whose associated smoothing value is
   * &lambda;<sub><i>i</i></sub>, then
   * <code>precomputedLambdas[i].get(event)</code> will be equal to
   * ln(1&nbsp;&minus;&nbsp;&lambda;<sub><i>i</i></sub>), where ln is the
   * natural log function that is implemented by <code>Math.log</code>.
   */
  protected HashMapDouble[] precomputedLambdas;
  /**
   * Records the number of &ldquo;hits&rdquo; to the caches of precomputed
   * probability estimates at the various back-off levels, to determine the
   * amount each back-off level is used while decoding.
   */
  protected transient int[] precomputedProbHits;
  /**
   * Records the number of times
   * {@link #estimateLogProbUsingPrecomputed(Transition,int)} is invoked.
   */
  protected transient int precomputedProbCalls;
  /**
   * Records the number of &ldquo;hits&rdquo; to the caches of precomputed
   * probability estimates at the various back-off levels when the caller
   * requests a probability for a context that has a base NP (<tt>NPB</tt>) as the
   * parent nonterminal.  This allows a comparison between <tt>NPB</tt>
   * hits versus overall hits.
   */
  protected transient int[] precomputedNPBProbHits;
  /**
   * Records the number of times
   * {@link #estimateLogProbUsingPrecomputed(Transition,int)} is invoked
   * requesting a probability for an event whose history context has
   * a base NP (<tt>NPB</tt>) the parent nonterminal.  This allows a comparison
   * between <tt>NPB</tt> method invocations and overall method invocations.
   */
  protected transient int precomputedNPBProbCalls;
  /**
   * A set of {@link #numLevels}<code>&nbsp;-&nbsp;1</code> maps, where map
   * <i>i</i> is a map from back-off level <i>i</i> transitions to
   * <i>i</i>&nbsp;+&nbsp;1 transitions.  These maps are only used temporarily
   * when precomputing probs (and are necessary for incremental training).
   *
   * @see #savePrecomputeData(CountsTable,Filter)
   * @see #saveBackOffMap
   */
  protected java.util.HashMap[] backOffMap;
  /**
   * A set of {@link #numLevels}<code>&nbsp;-&nbsp;1</code> maps, where map
   * <i>i</i> is a map from back-off level <i>i</i> histories to
   * <i>i</i>&nbsp;+&nbsp;1 histories.  These maps are not necessary
   * for precomputing probabilities, but can be useful when debugging.
   *
   * @see #saveHistBackOffMap
   * @see #savePrecomputeData(CountsTable,Filter)
   */
  protected java.util.HashMap[] histBackOffMap;

  // for temporary storage of histories (so we don't have to copy histories
  // created by deriveHistories() to create transition objects)
  /**
   * A currently-unused cache of probabilities of {@link TrainerEvent}
   * objects.
   */
  protected transient ProbabilityCache topLevelCache;
  /**
   * A cache of probability estimates at the various back-off levels of this
   * model, used when {@link #precomputeProbs} is <code>false</code>.
   */
  protected transient ProbabilityCache[] cache;
  /**
   * Records the number of cache hits for each back-off level of this mdoel.
   */
  protected transient int[] cacheHits;
  /**
   * Records the number of cache accesses for each back-off level of this model.
   */
  protected transient int[] cacheAccesses;

  /**
   * A reflexive map of canonical {@link Event} objects to save memory
   * in the various tables of this model that store such {@link Event}
   * objects.
   */
  protected transient FlexibleMap canonicalEvents;

  /**
   * The value of the smoothing parameters file for this model, as given
   * by {@link ProbabilityStructure#smoothingParametersFile()}.
   *
   * @see #useSmoothingParams
   * @see #dontAddNewParams
   * @see ProbabilityStructure#smoothingParametersFile()
   */
  protected transient String smoothingParamsFile;
  /**
   * The boolean value of the {@link Settings#saveSmoothingParams} setting.
   */
  protected transient boolean saveSmoothingParams;
  /**
   * The boolean value of the {@link Settings#dontAddNewParams} setting.
   */
  protected transient boolean dontAddNewParams;
  /**
   * The boolean value of the {@link Settings#useSmoothingParams} setting.
   */
  protected transient boolean useSmoothingParams;
  /**
   * The smoothing parameters for the history contexts ({@link Event} instances)
   * at the back-off levels of this model.
   */
  protected transient CountsTable[] smoothingParams;

  /**
   * A set of sets used to collect transitions that are to be pruned.
   *
   * @see #doPruning
   * @see #pruneHistoriesAndTransitions()
   */
  protected transient HashSet[] transitionsToPrune;
  /**
   * A set of sets used to collect histories that are to be pruned.
   *
   * @see #doPruning
   * @see #pruneHistoriesAndTransitions()
   */
  protected transient HashSet[] historiesToPrune;

  /**
   * Indicates whether the {@link #histBackOffMap} should be created when
   * precomputing probabilities.  If either {@link #doPruning} or {@link
   * #saveHistBackOffMap} is <tt>true</tt>, then this data member will be set
   * to <tt>true</tt> as well.  The value of this boolean is set automatically
   * in the constructor, and is only consulted when {@link
   * Settings#precomputeProbs} is <tt>true</tt>.
   *
   * @see AnalyzeDisns
   * @see #doPruning
   */
  protected transient boolean createHistBackOffMap;
  /**
   * The value of this data member determines whether this model will be
   * {@linkplain #pruneHistoriesAndTransitions() pruned} when {@linkplain
   * #precomputeProbs() probabilities are precomputed}.  This data
   * member&rsquo;s value is set automatically in the constructor: it is
   * <tt>true</tt> if and only if either {@link #globalDoPruning} is true or if
   * the {@link ProbabilityStructure#doPruning()} method invoked on this
   * model&rsquo;s {@linkplain #structure probability structure object} returns
   * <tt>true</tt>.
   *
   * @see #pruneHistoriesAndTransitions()
   * @see #precomputeProbs()
   */
  protected transient boolean doPruning;

  static int numCacheAdds = 0;
  static int numCanonicalHits = 0;

  /**
   * Constructs a new object for deriving all counts using the specified
   * probability structure.
   *
   * @param structure the probability structure to use when deriving counts
   */
  public Model(ProbabilityStructure structure) {
    this.structure = structure;
    structureClassName = structure.getClass().getName();
    int structureClassNameBegin =
      structureClassName.lastIndexOf('.') + 1;
    shortStructureClassName =
      structureClassName.substring(structureClassNameBegin);
    numLevels = structure.numLevels();
    counts = new CountsTrio[numLevels];
    for (int i = 0; i < counts.length; i++)
      counts[i] = new CountsTrio();
    lambdaFudge = new double[numLevels];
    lambdaPenalty = new double[numLevels];
    logOneMinusLambdaPenalty = new double[numLevels];
    for (int i = 0; i < lambdaFudge.length; i++) {
      lambdaFudge[i] = structure.lambdaFudge(i);
      lambdaPenalty[i] = structure.lambdaPenalty(i);
      logOneMinusLambdaPenalty[i] = Math.log(1 - lambdaPenalty[i]);
    }
    lambdaFudgeTerm = new double[numLevels];

    createHistBackOffMap = saveHistBackOffMap;
    if (globalDoPruning || structure.doPruning()) {
      createHistBackOffMap = true;
      doPruning = true;
    }

    if (precomputeProbs)
      setUpPrecomputedProbTables();

    setUpSmoothingParamsSettings();

    for (int i = 0; i < lambdaFudgeTerm.length; i++)
      lambdaFudgeTerm[i] = structure.lambdaFudgeTerm(i);
    if (useCache)
      setUpCaches();
    if (precomputeProbs)
      setUpPrecomputeProbStatTables();
  }

  private void setUpSmoothingParamsSettings() {
    smoothingParamsFile = structure.smoothingParametersFile();
    String smoothingParamsDir = Settings.get(Settings.smoothingParamsDir);
    if (smoothingParamsDir != null)
      smoothingParamsFile =
        smoothingParamsDir + File.separator + smoothingParamsFile;
    saveSmoothingParams = Settings.getBoolean(Settings.saveSmoothingParams) ||
                          structure.saveSmoothingParameters();
    dontAddNewParams = Settings.getBoolean(Settings.dontAddNewParams) ||
                       structure.dontAddNewParameters();
    useSmoothingParams = Settings.getBoolean(Settings.useSmoothingParams) ||
                         structure.useSmoothingParameters();

  }

  private void setUpPrecomputedProbTables() {
    precomputedProbs = new HashMapDouble[numLevels];
    for (int i = 0; i < precomputedProbs.length; i++)
      precomputedProbs[i] = new HashMapDouble();
    precomputedLambdas = new HashMapDouble[numLevels - 1];
    for (int i = 0; i < precomputedLambdas.length; i++)
      precomputedLambdas[i] = new HashMapDouble();

    backOffMap = new java.util.HashMap[numLevels - 1];
    for (int i = 0; i < backOffMap.length; i++) {
      backOffMap[i] = new java.util.HashMap();
    }
    if (createHistBackOffMap) {
      histBackOffMap = new java.util.HashMap[numLevels - 1];
      for (int i = 0; i < backOffMap.length; i++) {
	histBackOffMap[i] = new java.util.HashMap();
      }
    }
    if (doPruning) {
      transitionsToPrune = new HashSet[numLevels - 1];
      historiesToPrune = new HashSet[numLevels - 1];
      for (int i = 0; i < transitionsToPrune.length; i++) {
	transitionsToPrune[i] = new HashSet();
	historiesToPrune[i] = new HashSet();
      }
    }
  }

  private void setUpPrecomputeProbStatTables() {
    precomputedProbHits = new int[numLevels];
    precomputedNPBProbHits = new int[numLevels];
  }

  private void setUpCaches() {
    int cacheSize = Math.max(structure.cacheSize(0), minCacheSize);
    //topLevelCache = new ProbabilityCache(cacheSize, cacheSize / 4 + 1);

    cacheHits = new int[numLevels];
    cacheAccesses = new int[numLevels];

    cache = new ProbabilityCache[numLevels];
    for (int i = 0; i < cache.length; i++) {
      cacheSize = Math.max(structure.cacheSize(i), minCacheSize);
      /*
      System.err.println("setting up " + structure.getClass().getName() +
			 " cache at level " + i + "\n\tto have max. cap. of " +
			 cacheSize + " and init. cap. of " +
			 (cacheSize / 4 + 1));
      */
      cache[i] = new ProbabilityCache(cacheSize, cacheSize / 4 + 1);
    }
  }

  /**
   * Sets the {@link #canonicalEvents} member of this object.
   *
   * @param canonical the reflexive map of canonical {@link Event}
   * objects
   *
   * @see ModelCollection#internalReadObject(java.io.ObjectInputStream)
   */
  public void setCanonicalEvents(FlexibleMap canonical) {
    canonicalEvents = canonical;
  }

  /**
   * Derives all counts from the specified counts table, using the
   * probability structure specified in the constructor.
   *
   * @param trainerCounts a map from {@link TrainerEvent} objects to
   * their counts (as <code>double</code>s) from which to derive counts
   * @param filter used to filter out <code>TrainerEvent</code> objects
   * whose derived counts should not be derived for this model
   * @param threshold a (currently unused) count cut-off threshold
   * @param canonical a reflexive map used to canonicalize objects
   * created when deriving counts
   */
  public void deriveCounts(CountsTable trainerCounts, Filter filter,
			   double threshold, FlexibleMap canonical) {
    deriveCounts(trainerCounts, filter, threshold, canonical, false);
  }

  /**
   * Derives all counts from the specified counts table, using the
   * probability structure specified in the constructor.
   *
   * @param trainerCounts a map from {@link TrainerEvent} objects to
   * their counts (as <code>double</code>s) from which to derive counts
   * @param filter used to filter out <code>TrainerEvent</code> objects
   * whose derived counts should not be derived for this model
   * @param threshold a (currently unused) count cut-off threshold
   * @param canonical a reflexive map used to canonicalize objects
   * created when deriving counts
   * @param deriveOtherModelCounts an unused parameter, as this class
   * does not contain other, internal <code>Model</code> instances
   */
  public void deriveCounts(CountsTable trainerCounts, Filter filter,
			   double threshold, FlexibleMap canonical,
			   boolean deriveOtherModelCounts) {
    if (useSmoothingParams || dontAddNewParams)
      readSmoothingParams();
    setCanonicalEvents(canonical);
    //deriveHistories(trainerCounts, filter, canonical);

    Time time = null;
    if (verbose)
      time = new Time();

    Transition trans = new Transition(null, null);

    Iterator entries = trainerCounts.entrySet().iterator();
    while (entries.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)entries.next();
      TrainerEvent event = (TrainerEvent)entry.getKey();
      double count = entry.getDoubleValue();
      if (!filter.pass(event))
	continue;

      // store all transitions for all levels
      for (int level = 0; level < numLevels; level++) {
        Event history = structure.getHistory(event, level);
        if (useSmoothingParams || dontAddNewParams)
          if (smoothingParams[level].containsKey(history) == false)
            continue;
        history = canonicalizeEvent(history, canonical);
        counts[level].history().add(history, CountsTrio.hist, count);

        Event future = structure.getFuture(event, level);
        trans.setFuture(canonicalizeEvent(future, canonical));
        trans.setHistory(history);

        if (verboseDebug)
          System.err.println(shortStructureClassName +
                             "(" + level + "): " + trans +
                             "; count=" + (float)count);

        if (counts[level].transition().count(trans) == 0)
          counts[level].history().add(trans.history(), CountsTrio.diversity);
        counts[level].transition().add(getCanonical(trans, canonical), count);
      }
    }

    if (verbose)
      System.err.println("Derived transitions for " + structureClassName +
			 " in " + time + ".");

    /*
    if (threshold > 1) {
      pruneHistoriesAndTransitions(threshold);
    }
    */

    //deriveDiversityCounts();

    /*
    if (precomputeProbs)
      precomputeProbs(trainerCounts, filter);
    */
   if (precomputeProbs)
     savePrecomputeData(trainerCounts, filter);

   /*
   if (structure.doCleanup())
     cleanup();
   */
  }

  /**
   * A method invoked after probabilities have been precomputed by {@link
   * #precomputeProbs()} to clean up (that is, remove) objects from the various
   * counts tables that are no longer needed, as determined by {@link
   * ProbabilityStructure#removeHistory(int,Event)} and {@link
   * ProbabilityStructure#removeTransition(int,Transition)}.
   * <p/>
   * If {@link #precomputeProbs} is <code>true</code>, then this method will
   * remove entries from the maps of the {@link #precomputedProbs} array.  If
   * {@link #precomputeProbs} is <code>false</code> or if {@link
   * #deleteCountsWhenPrecomputingProbs} is <code>false</code>, then this method
   * will remove entries from the maps in the {@link #counts} array.
   * <p/>
   *
   * @see ProbabilityStructure#removeHistory(int,Event)
   * @see ProbabilityStructure#removeTransition(int,Transition)
   */
  protected void cleanup() {
    int numHistoriesRemoved = 0;
    int numTransitionsRemoved = 0;
    Time time = null;
    if (verbose)
      time = new Time();
    if (precomputeProbs) {
      int lastLevel = numLevels - 1;
      for (int level = 0; level < numLevels; level++) {
	Iterator it = precomputedProbs[level].keySet().iterator();
	while (it.hasNext()) {
	  if (structure.removeTransition(level, (Transition)it.next())) {
	    it.remove();
	    numTransitionsRemoved++;
	  }
	}
	if (level < lastLevel) {
	  it = precomputedLambdas[level].keySet().iterator();
	  while (it.hasNext()) {
	    if (structure.removeHistory(level, (Event)it.next())) {
	      it.remove();
	      numHistoriesRemoved++;
	    }
	  }
	}
      }
    }
    if (!(precomputeProbs && deleteCountsWhenPrecomputingProbs)) {
      for (int level = 0; level < numLevels; level++) {
	BiCountsTable histories = counts[level].history();
	Iterator it = histories.keySet().iterator();
	while (it.hasNext())
	  if (structure.removeHistory(level, (Event)it.next())) {
	    it.remove();
	    numHistoriesRemoved++;
	  }
	CountsTable transitions = counts[level].transition();
	it = transitions.keySet().iterator();
	while (it.hasNext())
	  if (structure.removeTransition(level, (Transition)it.next())) {
	    it.remove();
	    numTransitionsRemoved++;
	  }
      }
    }
    if (verbose)
      System.err.println("Cleaned up in " + time + "; removed " +
			 numHistoriesRemoved + " histories and " +
			 numTransitionsRemoved + " transitions.");
  }

  /**
   * This method first canonicalizes the information in the specified event
   * (a Sexp or a Subcat and a Sexp), then it returns a canonical version
   * of the event itself, copying it into the map if necessary.
   */
  protected final static Event canonicalizeEvent(Event event,
						 FlexibleMap canonical) {
    Event canonicalEvent = (Event)canonical.get(event);
    if (canonicalEvent == null) {
      canonicalEvent = event.copy();
      canonicalEvent.canonicalize(canonical);
      canonical.put(canonicalEvent, canonicalEvent);
    }
    return canonicalEvent;
  }

  /**
   * This method assumes trans already contains a canonical history and a
   * canonical future.  If an equivalent transition is found in the canonical
   * map, it is returned; otherwise, a new Transition object is created
   * with the canonical future and canonical history contained in the specified
   * transition, and that new Transition object is added to the canonical map
   * and returned.
   */
  protected final static Transition getCanonical(Transition trans,
						 FlexibleMap canonical) {
    Transition canonicalTrans = (Transition)canonical.get(trans);
    if (canonicalTrans == null) {
      canonicalTrans = new Transition(trans.future(), trans.history());
      canonical.put(canonicalTrans, canonicalTrans);
    }
    return canonicalTrans;
  }

  /**
   * Estimates the log-probability of a conditional event.  The history
   * (conditioning context) and future of this conditional event are contained
   * in the specified maximal-context event.
   *
   * @param id    the id of the caller (typically a
   *              {@link danbikel.switchboard.Switchboard} client ID)
   * @param event the maximal-context event containing both the history
   *              (conditioning context) and future of the conditional event
   *              whose probability is to be estimated
   * @return an estimate of the log-probaiblity of a conditional event
   */
  public double estimateLogProb(int id, TrainerEvent event) {
    ProbabilityStructure clientStructure = getClientProbStructure(id);
    return (precomputeProbs ?
	    estimateLogProbUsingPrecomputed(clientStructure, event) :
	    Math.log(estimateProb(clientStructure, event)));
  }

  /**
   * Estimates the probability of a conditional event.  The history
   * (conditioning context) and future of this conditional event are contained
   * in the specified maximal-context event.
   *
   * @param id    the id of the caller (typically a
   *              {@link danbikel.switchboard.Switchboard} client ID)
   * @param event the maximal-context event containing both the history
   *              (conditioning context) and future of the conditional event
   *              whose probability is to be estimated
   * @return an estimate of the probaiblity of a conditional event
   */
  public double estimateProb(int id, TrainerEvent event) {
    if (precomputeProbs)
      throw
	new UnsupportedOperationException("precomputed probs are in log-space");
    ProbabilityStructure clientStructure = getClientProbStructure(id);
    return estimateProb(clientStructure, event);
  }

  private final ProbabilityStructure getClientProbStructure(int id) {
    ProbabilityStructure clientStructure = null;

    if (id < structureMapArrSize) {
      clientStructure = structureMapArr[id];
      if (clientStructure == null)
	structureMapArr[id] = (clientStructure = structure.copy());
    }
    else {
      IntCounter localIdInt = idInt;
      synchronized (structureMap) {
	if (structureMap.size() > 1) {
	  localIdInt.set(id);
	  clientStructure = (ProbabilityStructure)structureMap.get(localIdInt);
	  if (clientStructure == null) {
	    clientStructure = structure.copy();
	    structureMap.put(new IntCounter(id), clientStructure);
	  }
	}
      }
    }
    return clientStructure;
  }

  /**
   * Estimates the log prob using precomputed probabilities and smoothing values
   * (lambdas).  This method is invoked by the public method {@link
   * #estimateLogProb(int,TrainerEvent)} if {@link #precomputeProbs} is
   * <code>true</code>.
   */
  protected double estimateLogProbUsingPrecomputed(ProbabilityStructure
						     structure,
						   TrainerEvent event) {
    boolean npbParent = Language.treebank.isBaseNP(event.parent());

    precomputedProbCalls++;
    if (npbParent)
      precomputedNPBProbCalls++;

    MapToPrimitive.Entry transEntry = null, lambdaEntry = null;
    double logLambda = 0.0;
    int lastLevel = numLevels - 1;
    /*
    Transition lastLevelTrans = structure.getTransition(event, lastLevel);
    if (precomputedProbs[lastLevel].getEntry(lastLevelTrans) == null)
      return Constants.logOfZero;
    */
    for (int level = 0; level < numLevels; level++) {
      Transition transition = structure.getTransition(event, level);
      transEntry = precomputedProbs[level].getEntry(transition);
      if (transEntry != null) {
	precomputedProbHits[level]++;
	if (npbParent)
	  precomputedNPBProbHits[level]++;
	return logLambda + transEntry.getDoubleValue();
      }
      else if (level < lastLevel) {
	Event history = transition.history();
	lambdaEntry = precomputedLambdas[level].getEntry(history);
	logLambda += (lambdaEntry == null ? logOneMinusLambdaPenalty[level] :
		      lambdaEntry.getDoubleValue());
      }
    }
    return Constants.logOfZero;
  }

  /**
   * Estimates the log prob of the specified transition using precomputed
   * probabilities and lambdas and {@link #histBackOffMap} (debugging method).
   * <b>N.B.</b>: The history contained within the specified transition
   * <i>must</i> have been observed during training (but not necessarily with
   * the particular future contained in the specified transition).
   *
   * @param transition the transition for which to get a smoothed
   * log-probability estimate
   * @param atLevel the back-off level of the specified transition
   *
   * @see #histBackOffMap
   * @see #createHistBackOffMap
   */
  protected double estimateLogProbUsingPrecomputed(Transition transition,
						   int atLevel) {
    MapToPrimitive.Entry transEntry = null, lambdaEntry = null;
    double logLambda = 0.0;
    int lastLevel = numLevels - 1;
    for (int level = atLevel; level < numLevels; level++) {
      transEntry = precomputedProbs[level].getEntry(transition);
      if (transEntry != null)
	return logLambda + transEntry.getDoubleValue();
      else if (level < lastLevel) {
	Event history = transition.history();
	lambdaEntry = precomputedLambdas[level].getEntry(history);
	logLambda += (lambdaEntry == null ? logOneMinusLambdaPenalty[level] :
		      lambdaEntry.getDoubleValue());
	Event backOffHist = (Event)histBackOffMap[level].get(history);
	if (backOffHist == null)
	  System.err.println(shortStructureClassName +
			     ": couldn't get back-off history from " + history);
	transition = new Transition(transition.future(), backOffHist);
      }
    }
    return Constants.logOfZero;
  }

  /**
   * Returns the smoothed probability estimate of a transition contained in the
   * specified <code>TrainerEvent</code> object.
   *
   * @param probStructure a <code>ProbabilityStructure</code> object that is
   *                      either {@link #structure} or a copy of it, used for
   *                      temporary storage during the computation performed by
   *                      this method
   * @param event         the <code>TrainerEvent</code> containing a transition
   *                      from a history to a future whose smoothed probability
   *                      is to be computed
   * @return the smoothed probability estimate of a transition contained in the
   *         specified <code>TrainerEvent</code> object
   */
  protected double estimateProb(ProbabilityStructure probStructure,
				TrainerEvent event) {
    ProbabilityStructure structure = probStructure;
    if (Debug.level >= 20) {
      System.err.println(structureClassName + "\n\t" + event);
    }

    /*
    if (useCache) {
      Double cacheProb = topLevelCache.getProb(event);
      if (cacheProb != null)
	return cacheProb.doubleValue();
    }
    */
    int highestCachedLevel = numLevels;
    int lastLevel = numLevels - 1;

    double[] lambdas = structure.lambdas;
    double[] estimates = structure.estimates;
    //structure.prevHistCount = 0;
    for (int level = 0; level < numLevels; level++) {
      Transition transition = structure.getTransition(event, level);
      Event history = transition.history();

      // check cache here!!!!!!!!!!!!
      if (useCache) {
	cacheAccesses[level]++;
	if (Debug.level >= 21) {
	  System.err.print("level " + level + ": getting cached  P");
	}
	MapToPrimitive.Entry cacheProbEntry = cache[level].getProb(transition);
	if (cacheProbEntry != null) {
	  if (Debug.level >= 21) {
	    System.err.println(cacheProbEntry);
	  }
	  estimates[level] = cacheProbEntry.getDoubleValue();
	  cacheHits[level]++;
	  highestCachedLevel = level;
	  break;
	}
	else {
	  if (Debug.level >= 21) {
	    System.err.print(transition);
	    System.err.println("=null");
	  }
	}
      }
      CountsTrio trio = counts[level];
      MapToPrimitive.Entry histEntry = trio.history().getEntry(history);
      double historyCount = (histEntry == null ? 0.0 :
			     histEntry.getDoubleValue(CountsTrio.hist));
      double transitionCount = trio.transition().count(transition);
      double diversityCount = (histEntry == null ? 0.0 :
			       histEntry.getDoubleValue(CountsTrio.diversity));

      double lambda, estimate; //, adjustment = 1.0;
      double fudge = lambdaFudge[level];
      double fudgeTerm = lambdaFudgeTerm[level];
      if (useSmoothingParams) {
        MapToPrimitive.Entry smoothingParamEntry =
          smoothingParams[level].getEntry(history);
        if (smoothingParamEntry != null && historyCount > 0) {
          lambda = smoothingParamEntry.getDoubleValue();
	  estimate = transitionCount / historyCount;
	}
	else {
	  if (warnSmoothingHasHistoryNotInTraining &&
	      smoothingParamEntry != null)
	    System.err.println(structureClassName +
			       ": warning: smoothing parameter exists for a " +
			       "history not seen in training: " + history);
	  lambda = lambdaPenalty[level];
	  estimate = 0;
	}
      }
      else if (historyCount == 0) {
	lambda = lambdaPenalty[level];
	estimate = 0;
      }
      else {
	/*
	if (structure.prevHistCount <= historyCount)
	  adjustment = 1 - structure.prevHistCount / historyCount;
	*/
	lambda = ((!deficientEstimation && level == lastLevel) ? 1.0 :
		  historyCount /
		  (historyCount + fudgeTerm + fudge * diversityCount));
	  //adjustment * (historyCount / (historyCount + fudge * diversityCount));
	estimate = transitionCount / historyCount;
      }

      if (Debug.level >= 20) {
	for (int i = 0; i < level; i++)
	  System.err.print("  ");
	/*
	System.err.println(transitionCount + "    " +
			   historyCount + "    " + diversityCount + "    " +
			   adjustment + "    " + estimate);
	*/
	System.err.println(transitionCount + "    " +
			   historyCount + "    " + diversityCount + "    " +
			   lambda + "    " + estimate);
      }

      lambdas[level] = lambda;
      estimates[level] = estimate;
      //structure.prevHistCount = historyCount;
    }
    double prob = Constants.probImpossible;
    if (useCache) {
      prob = (highestCachedLevel == numLevels ?
	      Constants.probImpossible : estimates[highestCachedLevel]);
    }
    for (int level = highestCachedLevel - 1; level >= 0; level--) {
      double lambda = lambdas[level];
      double estimate = estimates[level];
      prob = lambda * estimate + ((1 - lambda) * prob);
      if (useCache  && prob > Constants.probImpossible) {
	Transition transition = structure.transitions[level];
	if (Debug.level >= 21) {
	  System.err.println("adding P" + transition + " = " + prob +
			     " to cache at level " + level);
	}

	putInCache:
	if (!cache[level].containsKey(transition)) {
	  numCacheAdds++;
	  Transition canonTrans = (Transition)canonicalEvents.get(transition);
	  if (canonTrans != null) {
	    cache[level].put(canonTrans, prob);
	    numCanonicalHits++;
	  }
	  else {
	    // don't want to copy history and future if we don't have to
	    Event transHist = transition.history();
	    Event transFuture = transition.future();
	    Event hist = (Event)canonicalEvents.get(transHist);
	    if (hist == null) {
	      //break putInCache;
	      hist = transHist.copy();
	      //System.err.println("no hit: " + hist);
	    }
	    else {
	      numCanonicalHits++;
	      //System.err.println("hit: " + hist);
	    }
	    Event future = (Event)canonicalEvents.get(transFuture);
	    if (future == null) {
	      //break putInCache;
	      future = transFuture.copy();
	      //System.err.println("no hit: " + future);
	    }
	    else {
	      numCanonicalHits++;
	      //System.err.println("hit: " + future);
	    }
	    cache[level].put(new Transition(future, hist), prob);
	    //cache[level].put(transition.copy(), prob);
	  }
	}
      }
    }

    /*
    if (useCache && prob > Constants.logOfZero) {
      if (!topLevelCache.containsKey(event))
	topLevelCache.put(event.copy(), prob);
    }
    */
    return prob;
  }

  protected double estimateProbOld(ProbabilityStructure structure,
				   TrainerEvent event,
				   int level, double prevHistCount) {
    if (level == numLevels)
      return 0.0;
    // check cache here!!!!!!!!!!!!!!!!!!!!!!!

    Transition transition = structure.getTransition(event, level);
    Event history = transition.history();
    CountsTrio trio = counts[level];
    MapToPrimitive.Entry histEntry = trio.history().getEntry(history);
    double historyCount = (histEntry == null ? 0.0 :
			   histEntry.getDoubleValue(CountsTrio.hist));
    double transitionCount = trio.transition().count(transition);
    double diversityCount = (histEntry == null ? 0.0 :
			     histEntry.getDoubleValue(CountsTrio.diversity));

    double lambda, estimate, adjustment = 1.0;
    double fudge = structure.lambdaFudge(level);
    double fudgeTerm = structure.lambdaFudgeTerm(level);
    if (historyCount == 0.0) {
      lambda = lambdaPenalty[level];
      estimate = 0.0;
    }
    else {
      if (prevHistCount <= historyCount)
	adjustment = 1 - prevHistCount / historyCount;
      lambda =
	adjustment * (historyCount /
		      (historyCount + fudgeTerm + fudge * diversityCount));
      estimate = transitionCount / historyCount;
    }
    double backOffProb = estimateProbOld(structure,
				      event, level + 1, historyCount);
    double probVal = (lambda * estimate) + ((1 - lambda) * backOffProb);

    // cache probVal here!!!!!!!!!!!!!!!!!!!!!

    return probVal;
  }

  /**
   * Called by
   * {@link #deriveCounts(CountsTable,Filter,double,FlexibleMap)}, for each
   * type of transition observed, this method derives the number of
   * unique transitions from the history context to the possible
   * futures.  This number of unique transitions, called the
   * <i>diversity</i> of a random variable, is used in a modified
   * version of Witten-Bell smoothing.
   *
   * @deprecated This method used to be called by {@link
   * #deriveCounts(CountsTable,Filter,double,FlexibleMap,boolean)}, but
   * diversity counts are now derived directly by that method.
   */
  protected void deriveDiversityCounts() {
    // derive diversity counts for all levels
    Time time = null;
    if (verbose)
      time = new Time();

    for (int level = 0; level < numLevels; level++) {
      Iterator it = counts[level].transition().keySet().iterator();
      while (it.hasNext()) {
	Transition transition = (Transition)it.next();
	counts[level].history().add(transition.history(), CountsTrio.diversity);
      }
    }

    if (verbose)
      System.err.println("Derived diversity counts for " + structureClassName +
			 " in " + time + ".");
  }

  /**
   * Derives all history-context counts from the specified counts table, using
   * this <code>Model</code> object's probability structure.
   *
   * @param trainerCounts a map from {@link TrainerEvent} objects to
   * their counts (as <code>double</code>s) from which to derive counts
   * @param filter used to filter out <code>TrainerEvent</code> objects
   * whose derived counts should not be derived for this model
   * @param canonical a reflexive map used to canonicalize objects
   * created when deriving counts
   *
   * @deprecated This method used to be called by {@link
   * #deriveCounts(CountsTable,Filter,double,FlexibleMap,boolean)}, but
   * histories are now derived directly by that method.
   */
  protected void deriveHistories(CountsTable trainerCounts, Filter filter,
				 FlexibleMap canonical) {
    Time time = null;
    if (verbose)
      time = new Time();
    Iterator entries = trainerCounts.entrySet().iterator();
    while (entries.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)entries.next();
      TrainerEvent event = (TrainerEvent)entry.getKey();
      double count = entry.getDoubleValue();
      if (!filter.pass(event))
	continue;
      // store all histories for all back-off levels
      for (int level = 0; level < numLevels; level++) {
	Event history = structure.getHistory(event, level);
        if (useSmoothingParams || dontAddNewParams)
          if (smoothingParams[level].containsKey(history) == false)
            continue;
	history = canonicalizeEvent(history, canonical);
	counts[level].history().add(history, CountsTrio.hist, count);
      }
    }

    if (verbose)
      System.err.println("Derived histories for " + structureClassName +
			 " in " + time + ".");
  }

  private void pruneHistoriesAndTransitions(int threshold) {
    for (int level = 0; level < numLevels; level++) {
      Iterator it = counts[level].transition().entrySet().iterator();
      while (it.hasNext()) {
	MapToPrimitive.Entry transEntry = (MapToPrimitive.Entry)it.next();
	Transition trans = (Transition)transEntry.getKey();
	double transCount = transEntry.getDoubleValue();
	if (transCount < threshold) {
	  MapToPrimitive.Entry histEntry =
	    counts[level].history().getEntry(trans.history());
	  histEntry.add(CountsTrio.hist, -transCount);
	  if (histEntry.getDoubleValue(CountsTrio.hist) <= 0.0) {
	    if (histEntry.getDoubleValue(CountsTrio.hist) < 0.0)
	      System.err.println("yikes!!!");
	    counts[level].history().remove(histEntry.getKey());
	  }
	  it.remove();
	}
      }
    }
  }

  /**
   * Inserts the {@link Transition} objects representing conditional events for
   * all back-off levels of this model into the specified array, with
   * <code>trans[0] = zeroLevelTrans</code>.  Higher-numbered back-off level
   * events (i.e., events with increasingly coarser history contexts) are gotten
   * from the specified zeroeth-level {@link Transition} by use of the
   * {@link #backOffMap}.
   *
   * @param zeroLevelTrans the {@link Transition} object representing
   * a conditional event with a maximal-context history
   * @param trans the array in which to insert {@link Transition} objects
   * for all levels of back-off of this model
   * @return the specified {@link Transition} array, having been modified
   * to include {@link Transition} objects for all levels of back-off
   * of this model
   */
  protected Transition[] getTransitions(Transition zeroLevelTrans,
					Transition[] trans) {
    trans[0] = zeroLevelTrans;
    for (int i = 1; i < numLevels; i++) {
      int prevLevel = i - 1;
      trans[i] = (Transition)backOffMap[prevLevel].get(trans[prevLevel]);
    }
    return trans;
  }

  /**
   * Prune every history and transition with a back-off level less than
   * the last level in which the last level history has a diversity of 1
   * (meaning that the probability is 1, so no need to store a history
   * and transition).
   */
  protected void pruneHistoriesAndTransitionsOld() {
    Transition[] trans = new Transition[numLevels];
    int lastLevel = numLevels - 1;
    Iterator it = counts[0].transition().entrySet().iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry transEntry = (MapToPrimitive.Entry)it.next();
      Transition zeroLevelTrans = (Transition)transEntry.getKey();
      getTransitions(zeroLevelTrans, trans);

      MapToPrimitive histMap = counts[lastLevel].history();
      Event lastLevelHist = trans[lastLevel].history();
      MapToPrimitive.Entry lastLevelHistEntry =
	(MapToPrimitive.Entry)histMap.getEntry(lastLevelHist);

      if (lastLevelHistEntry.getDoubleValue(CountsTrio.diversity) == 1) {
	for (int level = 0; level < lastLevel; level++) {
	  // delete from precomputedProbs and precomputedLambdas but not
	  // from smoothingParams
	  if (precomputeProbs) {
	    precomputedProbs[level].remove(trans[level]);
	    precomputedLambdas[level].remove(trans[level].history());
	    /*
	    if (saveSmoothingParams)
	      smoothingParams[level].remove(trans[level].history());
	    */
	  }
	  if (!(precomputeProbs && deleteCountsWhenPrecomputingProbs)) {
	    counts[level].transition().remove(trans[level]);
	    counts[level].history().remove(trans[level].history());
	  }
	}
      }
    }
  }

  /**
   * Schedule for pruning every history and transition whose MLE is equal to
   * that of back-off level's transition.
   */
  protected void computeHistoriesAndTransitionsToPrune() {
    if (!doPruning)
      return;
    Transition[] trans = new Transition[numLevels];
    int lastLevel = numLevels - 1;

    MapToPrimitive.Entry currTransEntry, nextTransEntry;
    MapToPrimitive.Entry currHistEntry, nextHistEntry;

    for (int level = 0; level < lastLevel; level++) {
      Iterator it = counts[level].transition().entrySet().iterator();
      while (it.hasNext()) {
	MapToPrimitive.Entry transEntry = (MapToPrimitive.Entry)it.next();

	Transition currTrans = (Transition)transEntry.getKey();
	Event currHist = currTrans.history();
	Transition nextTrans = (Transition)backOffMap[level].get(currTrans);
	Event nextHist = nextTrans.history();

	CountsTrio currTrio = counts[level];
	CountsTrio nextTrio = counts[level + 1];


	currTransEntry =
	  (MapToPrimitive.Entry)currTrio.transition().getEntry(currTrans);
	currHistEntry =
	  (MapToPrimitive.Entry)currTrio.history().getEntry(currHist);
	nextTransEntry =
	  (MapToPrimitive.Entry)nextTrio.transition().getEntry(nextTrans);
	nextHistEntry =
	  (MapToPrimitive.Entry)nextTrio.history().getEntry(nextHist);

	double currTransCount = currTransEntry.getDoubleValue();
	double currHistCount = currHistEntry.getDoubleValue(CountsTrio.hist);
	double nextTransCount = nextTransEntry.getDoubleValue();
	double nextHistCount = nextHistEntry.getDoubleValue(CountsTrio.hist);

	double currMLE = currTransCount / currHistCount;
	double nextMLE = nextTransCount / nextHistCount;

	if (currMLE == nextMLE) {
	  /*
	  System.err.println(structureClassName +
			     ": pruning " + currTrans + " at level " +
			     level);
	  */

	  transitionsToPrune[level].add(currTrans);

	  currHistEntry.add(CountsTrio.hist, -currTransCount);
	  currHistEntry.add(CountsTrio.diversity, -1.0);
	  double newDiversity =
	    currHistEntry.getDoubleValue(CountsTrio.diversity);
	  double newHistCount = currHistEntry.getDoubleValue(CountsTrio.hist);
	  if ((newDiversity == 0.0 && newHistCount != 0.0) ||
	      (newHistCount == 0.0 && newDiversity != 0.0))
	    System.err.println(structureClassName + ": uh-oh: pruned " +
			       "last transition from " +
			       currHist + " but diversity=" + newDiversity +
			       " where history=" + newHistCount);
	  if (newDiversity == 0.0) {
	    //System.err.println(structureClassName + ": removing " + currHist);
	    historiesToPrune[level].add(currHist);
	  }
	}
      }
    }
  }

  /*
  protected void pruneHistoriesAndTransitions() {
    if (!doPruning)
      return;
    int totalTransPruned  = 0;
    int totalHistPruned = 0;
    boolean deletingCounts =
      precomputeProbs && deleteCountsWhenPrecomputingProbs;
    int lastLevel = numLevels - 1;
    for (int level = 0; level < lastLevel; level++) {
      totalTransPruned += transitionsToPrune[level].size();
      Iterator it = transitionsToPrune[level].iterator();
      while (it.hasNext()) {
	Transition trans = (Transition)it.next();
	precomputedProbs[level].remove(trans);
	if (!deletingCounts)
	  counts[level].transition().remove(trans);
      }
      totalHistPruned += historiesToPrune[level].size();
      it = historiesToPrune[level].iterator();
      while (it.hasNext()) {
	Event hist = (Event)it.next();
	precomputedLambdas[level].remove(hist);
	if (!deletingCounts)
	  counts[level].history().remove(hist);
      }
    }
    if (verbose)
      System.err.println("Pruned " + totalHistPruned + " histories and " +
			 totalTransPruned + " transitions.");
  }
  */

  /**
   * Analyzes the distributions of this model in order to prune history and
   * transition (i.e., conditional) events from the various counts tables.  The
   * analysis is designed so that the histories and transitions that are pruned
   * are likely to have a minimal if any impact on overall parsing performance.
   * <p/>
   * As a side effect, the events that are pruned or not pruned are output to a
   * file named <code>{@link #structureClassName} +
   * &quot;.prune-log&quot;</code>, if {@link #printPrunedEvents} or
   * {@link #printUnprunedEvents}, respectively, are <code>true</code>.  This
   * allows further analysis of the model.
   * <p/>
   *
   * @see AnalyzeDisns
   * @see #printPrunedEvents
   * @see #printUnprunedEvents
   */
  protected void pruneHistoriesAndTransitions() {
    if (!doPruning || numLevels < 2)
      return;

    // set up PrintWriter for printing log of pruned and unpruned events
    PrintWriter log = null;
    if (printPrunedEvents || printUnprunedEvents) {
      String prunedFilename = structureClassName + ".prune-log";
      String enc = Language.encoding();
      try {
	OutputStream os = new FileOutputStream(prunedFilename);
	OutputStreamWriter prunedosw = new OutputStreamWriter(os, enc);
	log = new PrintWriter(prunedosw);
      }
      catch (Exception e) {
	System.err.println(e);
      }
    }

    int toZeroIdx = AnalyzeDisns.toZeroIdx;
    int toPrevIdx = AnalyzeDisns.toPrevIdx;

    CountsTable[] entropy = AnalyzeDisns.newEntropyCountsTables(this);
    BiCountsTable[] js = AnalyzeDisns.newJSCountsTables(this);

    Time time = null;
    if (verbose)
      time = new Time();

    AnalyzeDisns.computeEntropyAndJSStats(this, entropy, js);

    if (verbose)
      System.err.println("Computed entropy and JS divergence values for " +
			 structureClassName + " in " + time + ".");

    int totalTrans = 0;
    int totalTransPruned  = 0;
    int totalHist = 0;
    int totalHistPruned = 0;

    int lastLevel = numLevels - 1;
    /*
    Iterator it = precomputedProbs[0].keySet().iterator();
    while (it.hasNext()) {
      Transition trans = (Transition)it.next();
      Event hist = trans.history();
    */
    Set historiesExamined = new HashSet();
    Iterator it = precomputedLambdas[0].keySet().iterator();
    while (it.hasNext()) {
      Event hist = (Event)it.next();
      for (int level = 0; level < lastLevel; level++) {
	if (historiesExamined.contains(hist))
	  break; // we've already explored this portion of the back-off tree
	else
	  historiesExamined.add(hist);
	Event backOffHist = (Event)histBackOffMap[level].get(hist);
        // if js divergence from backOffHist's disn to hist's disn is
        // less than 10% of entropy of hist's entropy, then prune hist
	double jsToPrev = js[level + 1].count(backOffHist, toPrevIdx);
	double histEntropy = entropy[level].count(hist);
	if (jsToPrev / histEntropy < pruningThreshold) {
	  historiesToPrune[level].add(hist);
	  if (printPrunedEvents)
	    log.println("pruning " + hist + " at level " + level + " because " +
			jsToPrev + " / " + histEntropy + " = " +
			(jsToPrev / histEntropy) + " < " + pruningThreshold);
	}
	else {
	  if (printUnprunedEvents)
	    log.println("did not prune " + hist + " at level " + level +
			" because " + jsToPrev + " / " + histEntropy + " = " +
			(jsToPrev / histEntropy) + " >= " + pruningThreshold);
	  break; // if curr hist is kept, don't try to prune coarser versions
	}
	hist = backOffHist;
      }
    }
    // the number of histories examined will generally be less than
    // total number of pruneable (non-lastLevel) histories, because of
    // break statement above
    double totalHistExamined = historiesExamined.size();
    historiesExamined = null;

    log.flush();
    log.close();

    // now that we've collected all histories to be pruned, actually do the
    // pruning
    boolean deletingCounts =
      precomputeProbs && deleteCountsWhenPrecomputingProbs;
    for (int level = 0; level < lastLevel; level++) {
      // go through every Transition object in precomputedProbs[level]
      // and, if its history context was scheduled for pruning,
      // remove transition both from precomputedProbs *and* from
      // counts[level].transition() (if applicable)
      totalTrans += precomputedProbs[level].size();
      it = precomputedProbs[level].keySet().iterator();
      while (it.hasNext()) {
	Transition trans = (Transition)it.next();
	if (historiesToPrune[level].contains(trans.history())) {
	  it.remove();
	  if (!deletingCounts)
	    counts[level].transition().remove(trans);
	  totalTransPruned++;
	}
      }
      // now go through every history scheduled for pruning and
      // remove it from precomputedLambdas[level] and, if appropriate,
      // from counts[level].history()
      totalHist += precomputedLambdas[level].size();
      totalHistPruned += historiesToPrune[level].size();
      it = historiesToPrune[level].iterator();
      while (it.hasNext()) {
	Event hist = (Event)it.next();
	precomputedLambdas[level].remove(hist);
	if (!deletingCounts)
	  counts[level].history().remove(hist);
      }
    }
    for (int level = 0; level < lastLevel; level++)
      historiesToPrune[level].clear();

    if (verbose) {
      double histPercent = 100 * (totalHistPruned / (double)totalHist);
      double possibleHistPercent = 100 * (totalHistPruned / totalHistExamined);
      double transPercent = 100 * (totalTransPruned / (double)totalTrans);
      System.err.println(structureClassName + ": pruned " + totalHistPruned +
			 " of " + totalHist + " histories (" +
			 doubleNF.format(histPercent) + "%) and of " +
			 (int)totalHistExamined + " pruneable histories (" +
			 doubleNF.format(possibleHistPercent) + "%) and " +
			 totalTransPruned + " of " + totalTrans +
			 " transitions (" + doubleNF.format(transPercent) +
			 "%) in " + time + ".");
    }

  }

  /**
   * Sets up the smoothing parameter arrays and maps.
   *
   * @see #smoothingParams
   */
  protected void initializeSmoothingParams() {
    smoothingParams = new CountsTableImpl[numLevels];
    for (int i = 0; i < numLevels; i++)
      smoothingParams[i] = new CountsTableImpl();
  }

  /**
   * Saves the back-off chain for each event derived from each
   * <code>TrainerEvent</code> in the key set of the specified counts table.
   * This method is called by {@link
   * #deriveCounts(CountsTable,Filter,double,FlexibleMap,boolean)} when
   * {@link Settings#precomputeProbs} is <code>true</code>.
   *
   * @param trainerCounts a counts table containing some or all of the
   * <code>TrainerEvent</code> objects collected during training
   * @param filter a filter specifying which <code>TrainerEvent</code>
   * objects to ignore in the key set of the specified counts table
   *
   * @see #deriveCounts(CountsTable,Filter,double,FlexibleMap,boolean)
   * @see #backOffMap
   */
  protected void savePrecomputeData(CountsTable trainerCounts, Filter filter) {
    Transition oldTrans;
    Transition currTrans = null;
    Iterator keys = trainerCounts.keySet().iterator();
    while (keys.hasNext()) {
      TrainerEvent event = (TrainerEvent)keys.next();
      if (!filter.pass(event))
        continue;
      for (int level = 0; level < numLevels; level++) {
	oldTrans = currTrans;
        Transition transition = structure.getTransition(event, level);
        Event history = transition.history();

        CountsTrio trio = counts[level];
        MapToPrimitive.Entry histEntry = trio.history().getEntry(history);
        MapToPrimitive.Entry transEntry = trio.transition().getEntry(transition);
        // take care of case where history was removed due to its low count
        // or because we are using smoothing parameters gotten from a file
        if (histEntry == null)
          continue;

        currTrans = (Transition)transEntry.getKey();
        if (level > 0) {
          backOffMap[level - 1].put(oldTrans, currTrans);
	  if (createHistBackOffMap) {
	    // sanity check
	    /*
	    Event backOffHist =
	      (Event)histBackOffMap[level - 1].get(oldTrans.history());
	    if (backOffHist != null && !backOffHist.equals(currTrans.history()))
	      System.err.println("UH-OH: " + oldTrans.history() + " --> " +
				 backOffHist + " but now --> " +
				 currTrans.history());
	    */
	    histBackOffMap[level - 1].put(oldTrans.history(),
					  currTrans.history());
	  }
	}
      }
    }
  }

  /**
   * Precomputes all probabilities and smoothing values for events seen during
   * all previous invocations of {@link
   * #deriveCounts(CountsTable,Filter,double,FlexibleMap,boolean)}.
   *
   * @see #precomputeProbs(MapToPrimitive.Entry,double[],double[],
   * Transition[],Event[],int) precomputeProbs(MapToPrimitive.Entry, ...)
   * @see #storePrecomputedProbs(double[],double[],Transition[],Event[],int)
   * storePrecomputedProbs
   */
  public void precomputeProbs() {
    //computeHistoriesAndTransitionsToPrune();

    if (!precomputeProbs)
      return;

    Time time = null;
    if (verbose)
      time = new Time();

    if (saveSmoothingParams && smoothingParams == null)
      initializeSmoothingParams();
    else if (useSmoothingParams)
      readSmoothingParams(); // only reads from file if smoothingParams != null

    // go through all transitions at each level of counts array, grabbing
    // histories and getting to next level via backOffMap

    Transition[] transitions = new Transition[numLevels];
    Event[] histories = new Event[numLevels];

    int lastLevel = numLevels - 1;

    Iterator topLevelTrans = counts[0].transition().entrySet().iterator();
    while (topLevelTrans.hasNext()) {
      MapToPrimitive.Entry transEntry =
	(MapToPrimitive.Entry)topLevelTrans.next();
      double[] lambdas = structure.lambdas;
      double[] estimates = structure.estimates;
      precomputeProbs(transEntry, lambdas, estimates, transitions, histories,
		      lastLevel);
      storePrecomputedProbs(lambdas, estimates, transitions, histories,
			    lastLevel);
    }
    
    pruneHistoriesAndTransitions();

    if (!saveBackOffMap)
      backOffMap = null; // no longer needed!
    if (!saveHistBackOffMap)
      histBackOffMap = null;
    if (structure.doCleanup())
      cleanup();
    if (saveSmoothingParams) {
      writeSmoothingParams();
      smoothingParams = null;
    }
    if (verbose)
      System.err.println("Precomputed probabilities for " +
			 structureClassName + " in " + time + ".");
  }

  /**
   * Precomputes the probabilities and smoothing values for the
   * {@link Transition} object contained as a key within the specified
   * map entry, where the value is the count of the transition.
   *
   * @param transEntry a map entry mapping a <code>Transition</code>
   * object to its count (a <code>double</code>)
   * @param lambdas an array in which to store the smoothing value for
   * each of the back-off levels
   * @param estimates an array in which to store the maximum-likelihood
   * estimate at each of the back-off levels
   * @param transitions an array in which to store the <code>Transition</code>
   * instance for each of the back-off levels
   * @param histories an array in which to store the history, an
   * <code>Event</code> instance, for each of the back-off levels
   * @param lastLevel the last back-off level (the value equal to
   * {@link #numLevels}<code>&nbsp;-&nbsp;1</code>)
   *
   * @see #precomputeProbs()
   */
  protected void precomputeProbs(MapToPrimitive.Entry transEntry,
				 double[] lambdas,
				 double[] estimates,
				 Transition[] transitions,
				 Event[] histories,
				 int lastLevel) {
    for (int level = 0; level < numLevels; level++) {
      Transition currTrans = (Transition)transEntry.getKey();
      Event history = currTrans.history();
      MapToPrimitive.Entry histEntry =
	(MapToPrimitive.Entry)counts[level].history().getEntry(history);

      if (histEntry == null)
	System.err.println("yikes! something is very wrong");

      transitions[level] = currTrans;
      histories[level] = (Event)histEntry.getKey();

      double historyCount = histEntry.getDoubleValue(CountsTrio.hist);
      double transitionCount = transEntry.getDoubleValue();
      double diversityCount = histEntry.getDoubleValue(CountsTrio.diversity);

      double fudge = lambdaFudge[level];
      double fudgeTerm = lambdaFudgeTerm[level];
      double lambda = ((!deficientEstimation && level == lastLevel) ? 1.0 :
		       historyCount /
		       (historyCount + fudgeTerm + fudge * diversityCount));
      if (useSmoothingParams) {
	MapToPrimitive.Entry smoothingParamEntry =
	  smoothingParams[level].getEntry(history);
	if (smoothingParamEntry != null)
	  lambda = smoothingParamEntry.getDoubleValue();
	else
	  System.err.println("uh-oh: couldn't get smoothing param entry " +
			     "for " + history);
      }
      double estimate = transitionCount / historyCount;
      lambdas[level] = lambda;
      estimates[level] = estimate;

      if (level < lastLevel) {
	Transition nextLevelTrans  =
	  (Transition)backOffMap[level].get(currTrans);
	transEntry = counts[level + 1].transition().getEntry(nextLevelTrans);
      }
    }
  }

  /**
   * Stores the specified smoothing values (lambdas) and smoothed probability
   * estimates in the {@link #precomputedProbs} and {@link #smoothingParams}
   * map arrays.
   *
   * @param lambdas an array containing the smoothing value for each of the
   * back-off levels
   * @param estimates an array containing the maximum-likelihood estimate at
   * each of the back-off levels
   * @param transitions an array containing the <code>Transition</code>
   * instance for each of the back-off levels
   * @param histories an array in which to store the history, an
   * <code>Event</code> instance, for each of the back-off levels
   * @param lastLevel the last back-off level (the value equal to
   * {@link #numLevels}<code>&nbsp;-&nbsp;1</code>)
   *
   * @see #precomputeProbs()
   */
  protected void storePrecomputedProbs(double[] lambdas,
				       double[] estimates,
				       Transition[] transitions,
				       Event[] histories,
				       int lastLevel) {
    double prob = 0.0;
    for (int level = lastLevel; level >= 0; level--) {
      double lambda = lambdas[level];
      double estimate = estimates[level];
      prob = lambda * estimate + ((1 - lambda) * prob);
      if (transitions[level] != null)
	precomputedProbs[level].put(transitions[level], Math.log(prob));
      if (level < lastLevel && histories[level] != null)
	precomputedLambdas[level].put(histories[level], Math.log(1 - lambda));
      if (saveSmoothingParams)
	smoothingParams[level].put(histories[level], lambda);
    }
  }

  /**
   * Stores precomputed probabilities and smoothing values for events derived
   * from the maximal-context <code>TrainerEvent</code> instances and their
   * counts contained in the specified counts table.
   *
   * @param trainerCounts a map of <code>TrainerEvent</code> instances
   * to their observed counts
   * @param filter a filter indicating which of the <code>TrainerEvent</code>
   * objects in the specified counts table should be ignored by this method
   * as it iterates over all entires in the counts table
   *
   * @deprecated This method has been superseded by {@link #precomputeProbs()}.
   */
  protected void precomputeProbs(CountsTable trainerCounts, Filter filter) {
    Time time = null;
    if (verbose)
      time = new Time();
    if (saveSmoothingParams && smoothingParams == null)
      initializeSmoothingParams();
    else if (useSmoothingParams)
      readSmoothingParams(); // only reads from file if smoothingParams != null
    Transition[] transitions = new Transition[numLevels];
    Event[] histories = new Event[numLevels];
    Iterator keys = trainerCounts.keySet().iterator();
    while (keys.hasNext()) {
      TrainerEvent event = (TrainerEvent)keys.next();
      if (!filter.pass(event))
	continue;
      precomputeProbs(event, transitions, histories);
    }
    if (saveSmoothingParams) {
      writeSmoothingParams();
      smoothingParams = null;
    }
    //counts = null;
    if (verbose)
      System.err.println("Precomputed probabilities and lambdas in " + time +
			 ".");
  }

  /**
   * Precomputes probabilities for the specified event, using the specified
   * arrays as temporary storage during this method invocation.
   *
   * @param event the <code>TrainerEvent</code> object from which probabilities
   * are to be precomputed
   * @param transitions temporary storage to be used during an invocation
   * of this method
   * @param histories temporary storage to be used during an invocation
   * of this method
   *
   * @deprecated This method is called by {@link
   * #precomputeProbs(CountsTable,Filter)}, which is also deprecated.
   */
  protected void precomputeProbs(TrainerEvent event,
				 Transition[] transitions, Event[] histories) {
    double[] lambdas = structure.lambdas;
    double[] estimates = structure.estimates;
    int lastLevel = numLevels - 1;
    for (int level = 0; level < numLevels; level++) {
      Transition transition = structure.getTransition(event, level);
      Event history = transition.history();

      CountsTrio trio = counts[level];
      MapToPrimitive.Entry histEntry = trio.history().getEntry(history);
      MapToPrimitive.Entry transEntry = trio.transition().getEntry(transition);

      // take care of case where history was removed due to its low count
      // or because we are using smoothing parameters gotten from a file
      if (histEntry == null) {
	estimates[level] = lambdas[level] = 0.0;
	histories[level] = null;
	transitions[level] = null;
	continue;
      }

      // the keys of the map entries are guaranteed to be canonical
      histories[level] = (Event)histEntry.getKey();
      transitions[level] =
	transEntry == null ? null : (Transition)transEntry.getKey();

      double historyCount = histEntry.getDoubleValue(CountsTrio.hist);
      double transitionCount =
	transEntry == null ? 0.0 : transEntry.getDoubleValue();
      double diversityCount = histEntry.getDoubleValue(CountsTrio.diversity);

      double fudge = lambdaFudge[level];
      double fudgeTerm = lambdaFudgeTerm[level];
      double lambda = ((!deficientEstimation && level == lastLevel) ? 1.0 :
		       historyCount /
		       (historyCount + fudgeTerm + fudge * diversityCount));
      if (useSmoothingParams) {
        MapToPrimitive.Entry smoothingParamEntry =
          smoothingParams[level].getEntry(history);
        if (smoothingParamEntry != null)
          lambda = smoothingParamEntry.getDoubleValue();
      }
      double estimate = transitionCount / historyCount;
      lambdas[level] = lambda;
      estimates[level] = estimate;
    }

    double prob = 0.0;
    for (int level = lastLevel; level >= 0; level--) {
      double lambda = lambdas[level];
      double estimate = estimates[level];
      prob = lambda * estimate + ((1 - lambda) * prob);
      if (transitions[level] != null)
	precomputedProbs[level].put(transitions[level], Math.log(prob));
      if (level < lastLevel && histories[level] != null)
	precomputedLambdas[level].put(histories[level], Math.log(1 - lambda));
      if (saveSmoothingParams)
        smoothingParams[level].put(histories[level], lambda);
    }
  }

  // I/O methods for smoothing parameters file

  /**
   * Reads all necessary smoothing parameters from {@link #smoothingParamsFile}
   * instead of deriving values for smoothing parameters.  Verbose output is
   * produced.
   *
   * @see #useSmoothingParams
   * @see #dontAddNewParams
   * @see #readSmoothingParams(boolean)
   */
  protected void readSmoothingParams() {
    readSmoothingParams(true);
  }

  /**
   * Reads all necessary smoothing parameters from {@link #smoothingParamsFile}
   * instead of deriving values for smoothing parameters.  Verbose output is
   * produced if the specified argument is <code>true</code>.
   *
   * @param verboseOutput indicates whether or not this method should output
   *                      verbose messages to <code>System.err</code>.
   * @see #useSmoothingParams
   * @see #dontAddNewParams
   */
  protected void readSmoothingParams(boolean verboseOutput) {
    if (smoothingParams != null)
      return;
    try {
      if (verboseOutput)
	System.err.println("Reading smoothing parameters from \"" +
			   smoothingParamsFile + "\" for " +
			   structureClassName + ".");
      int bufSize = Constants.defaultFileBufsize;
      BufferedInputStream bis =
        new BufferedInputStream(new FileInputStream(smoothingParamsFile),
                                bufSize);
      ObjectInputStream ois = new ObjectInputStream(bis);
      smoothingParams = (CountsTable[])ois.readObject();
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(fnfe);
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
    catch (ClassNotFoundException cnfe) {
      System.err.println(cnfe);
    }
  }

  /**
   * Writes the smoothing parameters of this model to the file named by
   * {@link #smoothingParamsFile}.  Verbose output to <code>System.err</code>
   * is produced.
   */
  protected void writeSmoothingParams() {
    writeSmoothingParams(true);
  }

  /**
   * Writes the smoothing parameters of this model to the file named by {@link
   * #smoothingParamsFile}.
   *
   * @param verboseOutput indicates whether to output verbose messages to
   *                      <code>System.err</code>
   */
  protected void writeSmoothingParams(boolean verboseOutput) {
    try {
      if (verboseOutput)
	System.err.println("Writing smoothing parameters to \"" +
			   smoothingParamsFile + "\" for " +
			   structureClassName + ".");
      int bufSize = Constants.defaultFileBufsize;
      BufferedOutputStream bos =
        new BufferedOutputStream(new FileOutputStream(smoothingParamsFile),
                                 bufSize);
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(smoothingParams);
      oos.close();
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(fnfe);
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
  }

  /**
   * Indicates to use counts or precomputed probabilities from the specified
   * back-off level of this model when estimating probabilities for the
   * specified back-off level of another model.<br>
   * <b>N.B.</b>: Note that invoking this method <b>destructively alters the
   * specified <code>Model</code></b>.
   *
   * @param backOffLevel the back-off level of this model that is to be
   * shared by another model
   * @param otherModel the other model that will share a particular
   * back-off level with this mdoel (that is, use the counts or precomputed
   * probabilities from this model)
   * @param otherModelBackOffLevel the back-off level of the other model
   * that is to be made the same as the specified back-off level
   * of this model (that is, use the counts or precomputed probabilities
   * from this model)
   */
  public void share(int backOffLevel,
                    Model otherModel, int otherModelBackOffLevel) {
    if (precomputeProbs) {
      otherModel.precomputedProbs[otherModelBackOffLevel] =
	this.precomputedProbs[backOffLevel];
      if (backOffLevel < numLevels - 1 &&
	  otherModelBackOffLevel < otherModel.numLevels - 1)
	otherModel.precomputedLambdas[otherModelBackOffLevel] =
	  this.precomputedLambdas[backOffLevel];
    }
    else {
      otherModel.counts[otherModelBackOffLevel] = this.counts[backOffLevel];
    }
  }

  /**
   * Since events are typically read-only, this method will allow for
   * canonicalization (or "unique-ifying") of the information
   * contained in the events contained in this object.  Use of this
   * method is intended to conserve memory by removing duplicate
   * copies of event information in different event objects.
   */
  public void canonicalize() {
    canonicalize(new danbikel.util.HashMap());
  }

  /**
   * Since events are typically read-only, this method will allow for
   * canonicalization (or "unique-ifying") of the information
   * contained in the events contained in this object using the
   * specified map.  Use of this method is intended to conserve memory
   * by removing duplicate copies of event information in different
   * event objects.
   *
   * @param map a map of canonical information structures of the
   * <code>Event</code> objects contained in this object; this parameter
   * allows multiple <code>Model</code> objects to have their
   * events structures canonicalized with respect to each other
   */
  public void canonicalize(FlexibleMap map) {
    setCanonicalEvents(map);
    int prevMapSize = map.size();
    for (int level = 0; level < numLevels; level++) {
      CountsTrio trio = counts[level];
      canonicalize(trio.history(), map);
      //canonicalize(trio.unique(), map);
      canonicalize(trio.transition(), map);
    }

    if (verbose) {
      System.err.println("Canonicalized Sexp objects; " +
			 "now canonicalizing Event objects");
    }

    for (int level = 0; level < numLevels; level++) {
      CountsTrio trio = counts[level];
      canonicalizeEvents(trio.history(), map);
      //canonicalizeEvents(trio.unique(), map);
      canonicalizeEvents(trio.transition(), map);
    }
    int increase = map.size() - prevMapSize;

    if (verbose) {
      System.err.print("Canonicalized " + numCanonicalizableEvents +
		       " events; map grew by " + increase + " elements to " +
		       map.size());
      if (doGCBetweenCanonicalizations) {
	System.err.print(" (gc ... ");
	System.err.flush();
      }
      else
	System.err.println();
    }
    if (doGCBetweenCanonicalizations) {
      System.gc();
      if (verbose)
	System.err.println("done).");
    }

    // reset for use by this method if it is called again
    numCanonicalizableEvents = 0;
  }

  private void canonicalize(MapToPrimitive table, Map map) {
    Iterator it = table.keySet().iterator();
    while (it.hasNext()) {
      Object histOrTrans = it.next();
      if (histOrTrans instanceof Event) {
	if (((Event)histOrTrans).canonicalize(map) != -1)
	  numCanonicalizableEvents++;
      }
      else {
	Transition trans = (Transition)histOrTrans;
	if (trans.history().canonicalize(map) != -1)
	  numCanonicalizableEvents++;
	if (trans.future().canonicalize(map) != -1)
	  numCanonicalizableEvents++;
      }
    }
  }

  private void canonicalizeEvents(MapToPrimitive table, Map map) {
    int numKeysReplaced = 0;
    Iterator it = table.entrySet().iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
      Object histOrTrans = entry.getKey();
      Object mapHistOrTrans = map.get(histOrTrans);
      boolean alreadyInMap = mapHistOrTrans != null;
      if (alreadyInMap) {
	if (entry.replaceKey(mapHistOrTrans))
	  numKeysReplaced++;
      }
      else
	map.put(histOrTrans, histOrTrans);
    }
    numCanonicalizableEvents += table.size();
    /*
    System.err.println(structureClassName +
		       ": No. of canonicalized Event objects: " +
		       numKeysReplaced);
    */
  }

  // accessors
  /**
   * Returns the type of <code>ProbabilityStructure</code> object used
   * during the invocation of
   * {@link #deriveCounts(CountsTable,Filter,double,FlexibleMap)}.
   *
   * <p>A copy of this object should be created and stored for each
   * parsing client thread, for use when the clients need to call the
   * probability-computation methods of this class.  This scheme
   * allows the reusable data members inside the
   * <code>ProbabilityStructure</code> objects to be used by multiple
   * clients without any concurrency problems, thereby maintaining
   * their efficiency and thread-safety.
   */
  public ProbabilityStructure getProbStructure() { return structure; }

  /**
   * Returns <tt>1</tt>, as this object does not contain any other,
   internal <code>Model</code> instances.
   */
  public int numModels() {
    return 1;
  }

  /**
   * Returns this model object.
   * @param idx an unused parameter, as this object does not contain any other,
   * internal <code>Model</code> instances.
   * @return this model object
   */
  public Model getModel(int idx) {
    return this;
  }

  // mutators
  /**
   * Causes this class to be verbose in its output to <code>System.err</code>
   * during the invocation of its methods, such as
   * {@link #deriveCounts(CountsTable,Filter,double,FlexibleMap)}.
   */
  public void beVerbose() { verbose = true;}
  /**
   * Causes this class not to output anything to <code>System.err</code>
   * during the invocation of its methods, such as
   * {@link #deriveCounts(CountsTable,Filter,double,FlexibleMap)}.
   */
  public void beQuiet() { verbose = false; }

  private void readObject(ObjectInputStream in)
  throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    if (useCache && !precomputeProbs)
      setUpCaches();
    if (precomputeProbs)
      setUpPrecomputeProbStatTables();
    setUpSmoothingParamsSettings();
    if (useSmoothingParams) {
      System.err.print("reading smoothing parameters...");
      readSmoothingParams(false);
    }
  }

  private void writeObject(java.io.ObjectOutputStream s)
    throws IOException {
    if (precomputeProbs && deleteCountsWhenPrecomputingProbs)
      counts = null;
    s.defaultWriteObject();
  }

  /**
   * Returns a human-readable string containing all the precomputed or
   * non-precomputed probability cache statistics for the life of this {@link
   * Model} object.
   *
   * @return a human-readable string containing all the precomputed or
   *         non-precomputed probability cache statistics for the life of this
   *         {@link Model} object.
   */
  public String getCacheStats() {
    StringBuffer sb = new StringBuffer(300);
    if (precomputeProbs) {
      sb.append("precomputed prob data for ").
	 append(structure.getClass().getName()).append(":\n");
      sb.append("\ttotal No. of calls: ").append(precomputedProbCalls).
	 append("\n");
      int total = 0;
      int sum = 0;
      for (int level = 0; level < precomputedProbHits.length; level++) {
	int hitsThisLevel = precomputedProbHits[level];
	total += hitsThisLevel;
	if (level > 0)
	  sum += hitsThisLevel * level;
	sb.append("\tlevel ").append(level).append(": ").append(hitsThisLevel).
	   append("\n");
      }
      sb.append("\taverage hit level: ").append((float)sum/total).append("\n");

      sb.append("\ttotal No. of NPB calls: ").append(precomputedNPBProbCalls).
	 append("\n");
      total = 0;
      sum = 0;
      for (int level = 0; level < precomputedNPBProbHits.length; level++) {
	int hitsThisLevel = precomputedNPBProbHits[level];
	total += hitsThisLevel;
	if (level > 0)
	  sum += hitsThisLevel * level;
	sb.append("\tNPB level ").append(level).append(": ").
	   append(hitsThisLevel).append("\n");
      }
      sb.append("\taverage NPB hit level: ").append((float)sum/total).
	 append("\n");
    }
    else {
      sb.append("cache data for ").append(structure.getClass().getName()).
	 append(":\n");
      for (int level = 0; level < cacheHits.length; level++) {
	sb.append("\tlevel ").append(level).append(": ").
	   append(cacheHits[level]).append("/").append(cacheAccesses[level]).append("/").
	   append((float)cacheHits[level]/cacheAccesses[level]).
	   append(" (hits/accesses/hit rate)\n");
	sb.append("\t\t").
	   append(cache[level].getStats().
		  replace('\n', ' ').replace('\t', ' ')).
	   append("\n");
      }
    }
    return sb.toString();
  }
}
