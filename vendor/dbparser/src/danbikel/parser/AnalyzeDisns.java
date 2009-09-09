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
import java.util.*;
import java.io.*;

/**
 * An analysis and debugging class to analyze the probability distributions of
 * all {@link Model}s in a {@link ModelCollection}.  It is important that
 * <ul>
 * <li>{@link Model#precomputeProbs} was <tt>true</tt>,
 * <li>{@link Model#deleteCountsWhenPrecomputingProbs} was <tt>false</tt> and
 * <li>{@link Model#createHistBackOffMap} was <tt>true</tt>
 * </ul>
 * when the {@link ModelCollection} to be analyzed was created.
 *
 * @see Model#deleteCountsWhenPrecomputingProbs
 * @see Model#createHistBackOffMap
 */
public class AnalyzeDisns {

  private AnalyzeDisns() {}

  /**
   * The {@link BiCountsTable} index for retrieving the Jensen-Shannon
   * divergence from a history context (distribution) at a particular back-off
   * level to its corresponding zeroeth back-off level (maximal context) history
   * context.
   */
  public final static int toZeroIdx = 0;
  /**
   * The {@link BiCountsTable} index for retrieving the Jensen-Shannon
   * divergence from a history context (distribution) at a particular back-off
   * level to its corresponding previous back-off level (greater context)
   * history context.
   */
  public final static int toPrevIdx = 1;

  /**
   * Returns the entropy of the specified distribution.
   *
   * @param disn an array containing the probabilites of a distribution
   * @return the entropy of the specified distribution.
   */
  public static double entropy(double[] disn) {
    return entropy(disn, disn.length);
  }
  /**
   * Returns the entropy of the specified distribution.
   *
   * @param disn an array containing the probabilities of a distribution
   * @param endIdx the last index plus one in the specified array of
   * probabilities
   * @return the entropy of the specified distribution.
   */
  public static double entropy(double[] disn, int endIdx) {
    double entropy = 0.0;
    for (int i = endIdx - 1; i >= 0; i--) {
      entropy -= disn[i] * (Math.log(disn[i])/Math.log(2));
    }
    return entropy;
  }

  /**
   * Returns the entropy of the specified distribution of log-probabilities.
   *
   * @param disn an array containing the log-probabilites of a distribution
   * @return the entropy of the specified distribution of log-probabilities.
   */
  public static double entropyFromLogProbs(double[] disn) {
    return entropyFromLogProbs(disn, disn.length);
  }
  /**
   * Returns the entropy of the specified distribution of log-probabilities.
   *
   * @param disn an array containing the log-probabilities of a distribution
   * @param endIdx the last index plus one in the specified array of
   * log-probabilities
   *
   * @return  the entropy of the specified distribution of log-probabilities.
   */
  public static double entropyFromLogProbs(double[] disn, int endIdx) {
    double entropy = 0.0;
    for (int i = endIdx - 1; i >= 0; i--) {
      if (disn[i] == Constants.logOfZero)
	continue;
      entropy -= Math.exp(disn[i]) * disn[i]/Math.log(2);
    }
    return entropy;
  }

  /**
   * Returns <i>D</i>(<code>disnP</code>&nbsp;||&nbsp;<code>disnQ</code>),
   * where <i>D</i> is the Kullback-Leibler divergence (<i>relative
   * entropy</i>), and where each of the specified arguments is a distribution
   * of log-probabilities.
   *
   * @param disnP a distribution of log-probabilities
   * @param disnQ a distribution of log-probabilities
   * @return <i>D</i>(<code>disnP</code>&nbsp;||&nbsp;<code>disnQ</code>)
   */
  public static double klDistFromLogProbs(double[] disnP, double[] disnQ) {
    double klDist = 0.0;
    for (int i = disnP.length - 1; i >= 0; i--) {
      if (disnP[i] == Constants.logOfZero)
	continue;
      klDist += Math.exp(disnP[i]) * ((disnP[i] - disnQ[i])/Math.log(2));
    }
    return klDist;
  }

  /**
   * A debugging method for analyzing a particular event in the modifier
   * word model.
   * @param mc the model collection from which to access the modifier word
   * model
   * @param eventStr the string to be converted to an S-expression that
   * represents the {@link TrainerEvent} to be analyzed
   * @throws IOException
   */
  public static void analyzeModWordDisn(ModelCollection mc, String eventStr)
    throws IOException {

    System.err.println("analyzing dis'n for " + eventStr);

    Model mwm = mc.modWordModel();
    Set allWords = new HashSet();
    allWords.addAll(mc.vocabCounter().keySet());
    allWords.addAll(mc.wordFeatureCounter().keySet());

    System.err.println("num words (incl. word feature vectors): " +
		       allWords.size());

    ProbabilityStructure structure = mwm.getProbStructure();
    int numLevels = structure.numLevels();
    TrainerEvent event = new ModifierEvent(Sexp.read(eventStr));
    Word modWord = event.modHeadWord();

    double total = 0.0;
    double[] tmpProbs = new double[allWords.size()];
    /*
    double[][] tmpLevelProbs = new double[numLevels][];
    for (int i = 0; i < numLevels; i++) {
      tmpLevelProbs[i] = new double[allWords.size()];
    }
    */
    int probIdx = 0;
    Iterator it = allWords.iterator();
    while (it.hasNext()) {
      modWord.setWord((Symbol)it.next());
      double prob = mwm.estimateProb(structure, event);
      int levelForProb = numLevels;
      for (int i = 0; i < numLevels; i++){
	if (structure.estimates[i] > 0.0) {
	  levelForProb = i;
	  break;
	}
      }
      if (prob > 0.0) {
	Symbol word = event.modHeadWord().word();
	// wordFreq should *really* be the count(w,t) observed in training,
	// *NOT* the simple c(w)
	double wordFreq = mc.vocabCounter().count(word);
	System.out.println(wordFreq + "\t" + prob + "\t" + levelForProb +
			   "\t" + word);
	tmpProbs[probIdx++] = prob;
	total += prob;
      }
    }

    double probs[] = new double[probIdx];
    System.arraycopy(tmpProbs, 0, probs, 0, probIdx);

    System.err.println("total prob. mass is " + total);

    System.err.println("entropy is " + entropy(probs) + " bits");

    /*
      Arrays.sort(probs);
      for (int i = probs.length - 1; i >= 0; i--)
      System.out.println(probs[i]);
    */
  }

  /**
   * A debugging method that outputs all histories of the specified model
   * to {@link System#out}.
   * @param model the model whose histories are to be output
   */
  public static void outputHistories(Model model) {
    Iterator histIt = model.counts[0].history().entrySet().iterator();
    while (histIt.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)histIt.next();
      System.out.println(entry.getDoubleValue(CountsTrio.hist) + "\t" +
			 entry.getKey());
    }
  }

  /**
   * Returns all possible futures for the specified model at the specified
   * back-off level, using the specified set for storage (the specified
   * set is first cleared before futures are stored).
   *
   * @param futures the set in which to store futures
   * @param model the model from which to collect possible futures
   * @param level the back-off level at which to collect possible futures
   * (should normally be irrelevant)
   * @return the specified <code>Set</code> having been destructively
   * modified to contain possible futures for the specified model at the
   * specified back-off level
   */
  public static Set getFutures(Set futures, Model model, int level) {
    futures.clear();
    Iterator it = model.counts[level].transition().entrySet().iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
      futures.add(((Transition)entry.getKey()).future());
    }
    return futures;
  }

  /**
   * Returns the smoothed log-probability distribution for the specified
   * history at the specified back-off level in the specified model.
   *
   * @param model the model from which to get a distribution of smoothed
   * log-probability estimates
   * @param level the back-off level of the specified history
   * @param hist the history for which a distribution is to be gotten
   * @param futures the set of possible futures for the specified history
   * @param disn the array in which to store all smoothed log-probability
   * estimates
   * @param tmpTrans a temporary {@link Transition} object, to be used
   * during the estimation of smoothed log-probabilities
   * @return the specified array of <code>double</code>, having been modified
   * to contain a distribution of log-probabilities at indices <code>0</code>
   * through <code>futures.size() - 1</code>
   *
   * @throws ArrayIndexOutOfBoundsException if the specified array of
   * <code>double</code> (the <code>disn</code> parameter) is of length less
   * than <code>futures.size()</code>
   */
  public static double[] getLogProbDisn(Model model, int level, Event hist,
					Set futures, double[] disn,
					Transition tmpTrans) {
    double logProbs[] = disn;
    Transition trans = tmpTrans;
    trans.setHistory(hist);
    Iterator it = futures.iterator();
    for (int idx = 0; it.hasNext(); idx++) {
      trans.setFuture((Event)it.next());
      logProbs[idx] = model.estimateLogProbUsingPrecomputed(trans, level);
    }
    return logProbs;
  }

  /**
   * A method to compute a model's entropy statistics for all estimated
   * distributions.
   * @param model the model whose entropy statistics are to be computed
   * @return an array mapping every history of every back-off level to its
   * entropy
   */
  public static CountsTable[] computeModelEntropies(Model model) {
    ProbabilityStructure structure = model.getProbStructure();
    int numLevels = structure.numLevels();
    CountsTable[] entropy = new CountsTable[numLevels];
    for (int i = 0; i < entropy.length; i++)
      entropy[i] = new CountsTableImpl();
    return computeModelEntropies(model, entropy);
  }

  /**
   * A method to compute a model's entropy statistics for all estimated
   * distributions.
   *
   * @param model   the model whose entropy statistics are to be computed
   * @param entropy an array of length
   *                <code>model.getProbStructure().numLevels()</code>
   *                in which to store entropy statistics for every history of
   *                every back-off level of the specified model
   * @return an array mapping every history of every back-off level to its
   *         entropy
   */
  public static CountsTable[] computeModelEntropies(Model model,
						    CountsTable[] entropy) {
    ProbabilityStructure structure = model.getProbStructure();

    // set up temporary data structures for getFutures and getLogProbDisn
    // methods
    int disnLen = 1000;
    double[] disn = new double[disnLen];
    Transition tmpTrans = new Transition(null,null);
    Set futuresSet = new LinkedHashSet();

    // foreach back-off level
    //   foreach history context at level
    //     map history context to its entropy
    int numLevels = structure.numLevels();
    for (int level = 0; level < numLevels; level++) {
      Set futures = getFutures(futuresSet, model, level);
      disnLen = futures.size();
      if (disnLen > disn.length)
	disn = new double[disnLen];
      Event hist = null;
      Set entrySet = model.counts[level].history().entrySet();
      Iterator it = entrySet.iterator();
      while (it.hasNext()) {
	MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
        hist = (Event)entry.getKey();
        double count  = entry.getDoubleValue(CountsTrio.hist);
        double diversity = entry.getDoubleValue(CountsTrio.diversity);
        double[] logProbDisn = getLogProbDisn(model, level, hist,
					      futures, disn, tmpTrans);
        int numNonZeroProbs = 0;
        for (int i = logProbDisn.length - 1; i >= 0; i--)
	  if (logProbDisn[i] != Constants.logOfZero)
	    numNonZeroProbs++;
	entropy[level].put(hist, entropyFromLogProbs(logProbDisn, disnLen));
      }
    }

    return entropy;
  }

  /**
   * Creates a file named after the probability structure class of the
   * specified model and writes information about every distribution contained
   * in that model.  Specifically, each line will contain the following six
   * elements about a particular history context at a particular back-off
   * level, where the elements are separated by tab characters:
   * <ul>
   * <li>the number of the history's back-off level (0 is the back-off level
   * with the most context)
   * <li>the history context, represented as an S-expression
   * <li>the frequency of this history context in training
   * <li>the diversity of this history context in training
   * <li>the number of non-zero estimates in the distribution of
   * smoothed probability estimates for this history context
   * <li>the entropy of the distribution for this history context
   * </ul>
   *
   * @param model the model whose distributions are to be analyzed
   */
  public static void writeModelStats(Model model) throws IOException {
    ProbabilityStructure structure = model.getProbStructure();
    String structureClassName = structure.getClass().getName();
    String filename = structureClassName + ".disns";
    System.err.println("Writing distribution stats to \"" + filename + "\".");
    // create file for this model
    OutputStream os = new FileOutputStream(filename);
    OutputStreamWriter osw = new OutputStreamWriter(os, Language.encoding());
    PrintWriter writer =  new PrintWriter(new BufferedWriter(osw));

    // set up temporary data structures for getFutures and getLogProbDisn
    // methods
    int disnLen = 1000;
    double[] disn = new double[disnLen];
    Transition tmpTrans = new Transition(null,null);
    Set futuresSet = new LinkedHashSet();

    // foreach back-off level
    //   foreach history at level
    //     print level hist count diversity numNonZeroSmoothedProbs entropy
    int numLevels = structure.numLevels();
    for (int level = 0; level < numLevels; level++) {
      Set futures = getFutures(futuresSet, model, level);
      disnLen = futures.size();
      if (disnLen > disn.length)
	disn = new double[disnLen];
      Event hist = null;
      Set entrySet = model.counts[level].history().entrySet();
      Iterator it = entrySet.iterator();
      while (it.hasNext()) {
	MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
        hist = (Event)entry.getKey();
        double count  = entry.getDoubleValue(CountsTrio.hist);
        double diversity = entry.getDoubleValue(CountsTrio.diversity);
        double[] logProbDisn = getLogProbDisn(model, level, hist,
					      futures, disn, tmpTrans);
        int numNonZeroProbs = 0;
        for (int i = logProbDisn.length - 1; i >= 0; i--)
	  if (logProbDisn[i] != Constants.logOfZero)
	    numNonZeroProbs++;
	writer.println(level + "\t" + hist + "\t" + count + "\t" +
		       diversity + "\t" + numNonZeroProbs + "\t" +
		       entropyFromLogProbs(logProbDisn, disnLen));
      }
    }
    writer.flush();
    writer.close();
  }

  /**
   * Returns an array of {@link CountsTable} instances in which to store the
   * entropy of every history at every back-off level.  The array will
   * necessarily be of length <code>model.getProbStructure().numLevels()</code>.
   *
   * @param model the model for which entropies are to be computed
   * @return an array of {@link CountsTable} instances in which to store the
   * entropy of every history at every back-off level
   */
  public static CountsTable[] newEntropyCountsTables(Model model) {
    ProbabilityStructure structure = model.getProbStructure();
    int numLevels = structure.numLevels();
    int lastLevel = numLevels - 1;
    CountsTable[] entropy = new CountsTable[numLevels];
    for (int i = 0; i < lastLevel; i++)
      entropy[i] = new CountsTableImpl();
    return entropy;
  }

  /**
   * Returns an array of {@link BiCountsTable} instances in which to store the
   * JS divergence of every history at every back-off level, both to the
   * previous back-off level and to the zeroeth back-off level.  The array will
   * necessarily be of length <code>model.getProbStructure().numLevels()</code>.
   *
   * @param model the model for which entropies are to be computed
   * @return an array of {@link BiCountsTable} instances in which to store the
   *         JS divergence of every history at every back-off level
   *
   * @see #toPrevIdx
   * @see #toZeroIdx
   */
  public static BiCountsTable[] newJSCountsTables(Model model) {
    ProbabilityStructure structure = model.getProbStructure();
    int numLevels = structure.numLevels();
    BiCountsTable[] js = new BiCountsTable[numLevels];
    for (int level = 1; level < numLevels; level++)
      js[level] = new BiCountsTable();
    return js;
  }

  /**
   * A method invoked by {@link Model} when {@link
   * Settings#modelDoPruning} is <tt>true</tt>: entropy values and
   * JS divergence values are used in the parameter-pruning method.
   *
   * @param model the model whose entropies and JS divergence statistics
   * are to be computed
   * @param entropy the array of counts tables in which to store the
   * entropies of the specified model's distributions
   * @param js the array of {@link BiCountsTable} objects in which to store
   * the JS divergence statistics of the specified model's distributions
   */
  public static void computeEntropyAndJSStats(Model model,
					      CountsTable[] entropy,
					      BiCountsTable[] js) {
    ProbabilityStructure structure = model.getProbStructure();
    int numLevels = structure.numLevels();
    int lastLevel = numLevels - 1;

    // set up temporary data structures for getFutures and getLogProbDisn
    // methods
    Transition tmpTrans = new Transition(null,null);
    Set futures = getFutures(new LinkedHashSet(), model, 0);
    double[] zeroLevelDisn = new double[futures.size()];
    double[] currDisn = null;
    double[] prevDisn = null;
    Map[] disnCache = new Map[numLevels];
    for (int i = 0; i < numLevels; i++)
      disnCache[i] = new java.util.LinkedHashMap(16, 0.75f, true) {
	  protected boolean removeEldestEntry(Map.Entry eldest) {
	    return size() > 5000;
	  }
	};

    Event hist = null;
    double count = 0.0, oldCount = 0.0, zeroLevelCount = 0.0;
    Set entrySet = model.counts[0].history().entrySet();
    Iterator it = entrySet.iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
      hist = (Event)entry.getKey();
      boolean prevLevelHistFirstSeen = true;
      count = entry.getDoubleValue(CountsTrio.hist);
      zeroLevelCount = oldCount = count;
      // put disn for hist_0 into zeroLevelDisn
      getLogProbDisn(model, 0, hist, futures, zeroLevelDisn, tmpTrans);
      // store entropy for zeroeth level hist
      entropy[0].put(hist, entropyFromLogProbs(zeroLevelDisn));
      for (int level = 1; level < numLevels; level++) {
	// get this level's hist from previous level's using histBackOffMap
	hist = (Event)model.histBackOffMap[level - 1].get(hist);
	MapToPrimitive.Entry currHistEntry =
	  model.counts[level].history().getEntry(hist);
	count = currHistEntry.getDoubleValue(CountsTrio.hist);
	double probExtraContextToZero = zeroLevelCount / count;
	double probExtraContextToPrev = oldCount / count;
	// put curr disn for this hist into currDisn
	// first, check cache
	double[] cached = (double[])disnCache[level].get(hist);
	if (cached != null) {
	  currDisn = cached;
	}
	else {
	  currDisn = new double[futures.size()];
	  getLogProbDisn(model, level, hist, futures, currDisn, tmpTrans);
	  disnCache[level].put(hist, currDisn);
	}
	// store entropy for current level's hist, if we haven't already
	// computed it
	if (level < lastLevel && entropy[level].getEntry(hist) != null) {
	  entropy[level].put(hist, entropyFromLogProbs(currDisn));
	}
	double klDistToZero  = klDistFromLogProbs(zeroLevelDisn, currDisn);
	double klDistToPrev =
	  level > 1 && prevLevelHistFirstSeen ?
	  klDistFromLogProbs(prevDisn, currDisn) : klDistToZero;

	boolean currLevelHistUnseen = js[level].getEntry(hist) == null;

	js[level].add(hist, toZeroIdx, probExtraContextToZero * klDistToZero);
	// only add to js dist to prev when prev was first seen, since we only
	// want to sum over unique "expansions" to more-specific histories for
	// the current level hist
	if (prevLevelHistFirstSeen)
	  js[level].add(hist, toPrevIdx, probExtraContextToPrev * klDistToPrev);

	// currDisn becomes prevDisn and count becomes oldCount
	prevDisn = currDisn;
	oldCount = count;

	prevLevelHistFirstSeen = currLevelHistUnseen;
      }
    }
  }

  /**
   * Creates two files named after the probability structure of the specified
   * model, and writes Kullback-Leibler divergences (relative entropies)
   * between the zeroeth-level back-off distributions and the other back-off
   * distributions to one file and writes Jensen-Shannon divergences between
   * zeroeth-level back-off distributions and the other back-off distributions
   * to the other file.  The KL divergence file will have the extension
   * <tt>&quot;.kl&quot;</tt> and the JS divergence file will have the
   * extension <tt>&quot;.js&quot;</tt>.
   * <p>
   * Specifically, the KL divergence file will contain one line for each
   * zeroeth-level (maximal-context) history with the following elements,
   * separated by tab characters:
   * <ul>
   * <li><tt>hist_0</tt>
   * <li><b>foreach</b> back-off level <i>i</i>&nbsp;&gt;&nbsp;0
   *   <ul>
   *     <li><tt>hist_i</tt>
   *     <li><i>c</i>(<tt>hist_0</tt>)/<i>c</i>(<tt>hist_i</tt>)
   *     <li><i>D</i>(<tt>hist_0</tt>&nbsp;||&nbsp;<tt>hist_i</tt>)
   *     <li><i>c</i>(<tt>hist_i-1</tt>)/<i>c</i>(<tt>hist_i</tt>)
   *     <li><i>D</i>(<tt>hist_i-1</tt>&nbsp;||&nbsp;<tt>hist_i</tt>)
   *   </ul>
   * </ul>
   * where <tt>hist_i</tt> is an S-expression of the history at back-off level
   * <i>i</i>, <i>D</i> is the relative entropy function and <i>c</i> is
   * the count of a history context int training, meaning that the quantity
   * <i>c</i>(<tt>hist_i-1</tt>)/<i>c</i>(<tt>hist_i</tt>) is the probability
   * of seeing the extra context in <tt>hist_i-1</tt> compared to
   * <tt>hist_i</tt>.
   * <p>
   * For example, if a model has three back-off levels (a zeroeth,
   * maximal-context level and two more levels, each with less context), then
   * each line will contain 11 elements separated by tab characters, where
   * the first element is the S-expression of the zeroeth back-off level
   * history and with five elements for each of the other two back-off levels.
   * <p>
   * The JS divergence file will contain one line for each non-zeroeth-level
   * history with the following four elements, separated by tab characters:
   * <ul>
   * <li><tt>i</tt> (the back-off level of <tt>hist_i</tt>)
   * <li><tt>hist_i</tt>
   * <li><i>JS</i>(<tt>hist_0</tt>&nbsp;||&nbsp;<tt>hist_i</tt>)
   * <li><i>JS</i>(<tt>hist_i-1</tt>&nbsp;||&nbsp;<tt>hist_i</tt>)
   * </ul>
   * where <i>JS</i> is the Jensen-Shannon divergence function.
   *
   * @param model the model whose distributions are to be analyzed
   */
  public static void writeKLDistStats(Model model) throws IOException {
    ProbabilityStructure structure = model.getProbStructure();
    String structureClassName = structure.getClass().getName();
    String klFilename = structureClassName + ".kl";
    System.err.println("Writing KL divergence stats to \"" + klFilename +
		       "\".");
    OutputStream os = new FileOutputStream(klFilename);
    OutputStreamWriter osw = new OutputStreamWriter(os, Language.encoding());
    PrintWriter writer = new PrintWriter(new BufferedWriter(osw));

    int numLevels = structure.numLevels();

    // set up temporary data structures for getFutures and getLogProbDisn
    // methods
    Transition tmpTrans = new Transition(null,null);
    Set futures = getFutures(new LinkedHashSet(), model, 0);
    double[] zeroLevelDisn = new double[futures.size()];
    double[] currDisn = null;
    double[] prevDisn = null;
    Map[] disnCache = new Map[numLevels];
    for (int i = 0; i < numLevels; i++)
      disnCache[i] = new java.util.LinkedHashMap(16, 0.75f, true) {
	  protected boolean removeEldestEntry(Map.Entry eldest) {
	    return size() > 5000;
	  }
	};

    // set up temporary data structures for computing Jensen-Shannon
    // divergences
    BiCountsTable[] js = new BiCountsTable[numLevels];
    for (int level = 1; level < numLevels; level++)
      js[level] = new BiCountsTable();

    // foreach history "hist" at back-off level 0
    //   print hist
    //   foreach back-off level i greater than 0 (less context)
    //     print hist_i p(hist_0   | hist_i) D(hist_0 || hist_i)
    //                  p(hist_i-1 | hist_i) D(hist_i-1 || hist_i)
    //       (where D(p||q) is relative entropy)
    //   print newline
    Event hist = null;
    double count = 0.0, oldCount = 0.0, zeroLevelCount = 0.0;
    Set entrySet = model.counts[0].history().entrySet();
    Iterator it = entrySet.iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
      hist = (Event)entry.getKey();
      boolean prevLevelHistFirstSeen = true;
      count = entry.getDoubleValue(CountsTrio.hist);
      zeroLevelCount = oldCount = count;
      writer.print(hist);
      // put disn for hist_0 into zeroLevelDisn
      getLogProbDisn(model, 0, hist, futures, zeroLevelDisn, tmpTrans);
      for (int level = 1; level < numLevels; level++) {
	// get this level's hist from previous level's using histBackOffMap
	hist = (Event)model.histBackOffMap[level - 1].get(hist);
	MapToPrimitive.Entry currHistEntry =
	  model.counts[level].history().getEntry(hist);
	count = currHistEntry.getDoubleValue(CountsTrio.hist);
	double probExtraContextToZero = zeroLevelCount / count;
	double probExtraContextToPrev = oldCount / count;
	// put curr disn for this hist into currDisn
	// first, check cache
	double[] cached = (double[])disnCache[level].get(hist);
	if (cached != null) {
	  currDisn = cached;
	}
	else {
	  currDisn = new double[futures.size()];
	  getLogProbDisn(model, level, hist, futures, currDisn, tmpTrans);
	  disnCache[level].put(hist, currDisn);
	}
	double klDistToZero  = klDistFromLogProbs(zeroLevelDisn, currDisn);
	double klDistToPrev =
	  level > 1 && prevLevelHistFirstSeen ?
	  klDistFromLogProbs(prevDisn, currDisn) : klDistToZero;

	boolean currLevelHistUnseen = js[level].getEntry(hist) == null;

	js[level].add(hist, toZeroIdx, probExtraContextToZero * klDistToZero);
	// only add to js dist to prev when prev was first seen, since we only
	// want to sum over unique "expansions" for the current level hist
	if (prevLevelHistFirstSeen)
	  js[level].add(hist, toPrevIdx, probExtraContextToPrev * klDistToPrev);

	writer.print("\t" + hist + "\t" +
		     probExtraContextToZero + "\t" + klDistToZero + "\t" +
		     probExtraContextToPrev + "\t" + klDistToPrev);

	// currDisn becomes prevDisn and count becomes oldCount
	prevDisn = currDisn;
	oldCount = count;

	prevLevelHistFirstSeen = currLevelHistUnseen;
      }
      writer.println();
    }
    writer.flush();
    writer.close();

    String jsFilename = structureClassName + ".js";
    System.err.println("Writing Jensen-Shannon divergence stats to \"" +
		       jsFilename + "\".");
    os = new FileOutputStream(jsFilename);
    osw = new OutputStreamWriter(os, Language.encoding());
    writer = new PrintWriter(new BufferedWriter(osw));
    for (int level = 1; level < numLevels; level++) {
      Iterator entries = js[level].entrySet().iterator();
      while (entries.hasNext()) {
	MapToPrimitive.Entry entry = (MapToPrimitive.Entry)entries.next();
	hist = (Event)entry.getKey();
	double jsToZero = entry.getDoubleValue(toZeroIdx);
	double jsToPrev = entry.getDoubleValue(toPrevIdx);
	writer.println(level + "\t" + hist + "\t" + jsToZero + "\t" + jsToPrev);
      }
    }
    writer.flush();
    writer.close();
  }

  /**
   * Analyzes and saves information about every distribution in every
   * {@link Model} contained in a {@link ModelCollection}.  It is important that
   * <ul>
   * <li>{@link Model#precomputeProbs} was <tt>true</tt>,
   * <li>{@link Model#deleteCountsWhenPrecomputingProbs} was <tt>false</tt> and
   * <li>{@link Model#createHistBackOffMap} was <tt>true</tt>
   * </ul>
   * when the {@link ModelCollection} to be analyzed was created.
   * <p>
   * usage: <tt>&lt;derived data file&gt;</tt><br>
   * where <tt>&lt;derived data file&gt;</tt> was produced by {@link Trainer}.
   *
   * @param args an array containing at least one element that is the name
   * of a model collection (derived data file) as produced by a {@link Trainer}
   * instance
   *
   * @see Trainer
   * @see Trainer#writeModelCollection(ObjectOutputStream,String,String)
   * @see Trainer#loadModelCollection(String)
   */
  public static void main(String[] args) {
    String mcFilename = args[0];
    // collect any additional args into a set
    Set structureNames = new LinkedHashSet();
    for (int argIdx = 1; argIdx < args.length; argIdx++)
      structureNames.add(args[argIdx]);
    try {
      ModelCollection mc = Trainer.loadModelCollection(mcFilename);
      Model mwm = mc.modWordModel();

      // output all histories
      //outputHistories(mwm);
      /*
      String modEventStr = args.length > 1 ? args[1] :
	"((foo VB) (to TO) VP-A (+START+) " +
	"((+START+ +START+)) VP TO (VP-A) false right)";

      analyzeModWordDisn(mc, modEventStr);
      */

      // for each Model in ModelCollection (using ModelCollection.modelList)
      //   for each Model within each Model (using Model.getModel)
      //     writeModelStats(Model)
      Iterator models = mc.modelList().iterator();
      while (models.hasNext()) {
	Model model = (Model)models.next();
	int numModels = model.numModels();
	for (int i = 0; i < numModels; i++) {
	  Model ithModel = model.getModel(i);
	  writeModelStats(ithModel);
	  String structureClassName =
	    ithModel.getProbStructure().getClass().getName();
	  if (structureNames.contains(structureClassName))
	    writeKLDistStats(ithModel);
	}
      }
    }
    catch (Exception e) {
      e.printStackTrace(System.err);
    }
  }
}
