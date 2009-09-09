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
import java.io.*;
import java.util.*;
import java.rmi.*;


/**
 * Provides the methods necessary to perform constrained CKY parsing on input
 * sentences so as to perform the E-step of the Inside-Outside EM algorithm.
 */
public class EMDecoder extends Decoder {
  // debugging constants
  // debugging code will be optimized away when the following booleans are false
  private final static boolean debug = false;
  private final static boolean debugConvertSubcatMaps = false;
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
  private final static String debugGoldFilenameProperty =
    "parser.debug.goldFilename";
  private final static boolean debugOutputChart = false;
  private final static String debugChartFilenamePrefix = "chart";
  private final static boolean debugCommaConstraint = false;
  private final static boolean debugDontPostProcess = false;
  /**
   * This debugging option should be used only when the property
   * <tt>parser.model.precomputeProbabilities</tt> was <tt>false</tt>
   * during training (and should therefore be <tt>false</tt> during
   * decoding as well).  This is the most verbose of the debugging
   * options, so expect an output file on the order of tens of
   * megabytes, if not larger.
   */
  private final static boolean debugOutputAllCounts = false;
  private final static boolean debugInsideProbs = true;
  private final static boolean debugOutputAllInsideProbs = false;

  private final static Symbol ADJP = Symbol.add("ADJP");
  private final static Symbol S = Symbol.add("S");
  private final static Symbol SA = Symbol.add("S-A");
  private final static Symbol SQ = Symbol.add("SQ");
  private final static Symbol SINV = Symbol.add("SINV");
  private final static Symbol SGA = Symbol.add("SG-A");
  private final static Symbol PRN = Symbol.add("PRN");
  private final static Symbol RRB = Symbol.add("-RRB-");
  private final static Symbol NP = Symbol.add("NP");
  private final static Symbol PP = Symbol.add("PP");
  private final static Symbol NPA = Symbol.add("NP-A");
  private final static Symbol NPB = Symbol.add("NPB");
  private final static Symbol RRC = Symbol.add("RRC");
  private final static Symbol VP = Symbol.add("VP");
  private final static Symbol VPA = Symbol.add("VP-A");
  private final static Symbol CC = Symbol.add("CC");
  private final static Symbol comma = Symbol.add(",");
  private final static Symbol FRAG = Symbol.add("FRAG");
  private final static Symbol willSym = Symbol.add("will");
  private final static Symbol mdSym = Symbol.add("MD");
  private final static Symbol PRPS = Symbol.add("PRP$");

  // constants
  private final static String className = EMDecoder.class.getName();
  /**
   * A hack to limit the number of unary productions, instead of doing
   * The Right Thing and computing infinite sums for looping derivations,
   * as described by Stolcke (1995) and Goodman (1999).
   */
  protected final static int MAX_UNARY_PRODUCTIONS = 5;
  /** The value of {@link Constants#probCertain}. */
  protected final static double probCertain = Constants.probCertain;
  /** The value of {@link Constants#probImpossible}. */
  protected final static double probImpossible = Constants.probImpossible;

  // data members
  /**
   * A temporary storage area used by {@link #addTopUnaries} for storing
   * items to be added to the chart when iterating over a cell in the chart.
   * <p/>
   * <b>Bugs</b>: It is a design error to have created this {@link Set} member
   * with the same name as the {@link Decoder#topProbItemsToAdd ArrayList}
   * member in the superclass.  The designer of this class should be
   * appropriately flogged.
   */
  protected Set topProbItemsToAdd = new HashSet();
  /**
   * The value of all sentences' inside probabilities in log-space.  Used to
   * gather training data log-likelihood at the end of each EM iteration.
   */
  protected double cummulativeInsideLogProb = 0.0;
  // data members used when debugSentenceSize is true
  private float avgSentLen = 0.0f;
  private int numSents = 0;

  /**
   * The map of events to their expected counts (cleared after every sentence).
   */
  protected CountsTable eventCounts = new CountsTableImpl();

  /** The parsing chart. */
  protected EMChart chart;

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
  public EMDecoder(int id, DecoderServerRemote server) {
    super(id, server);
    chart = new EMChart();
    super.chart = chart;
    useCommaConstraint = false;
  }

  protected void seedChart(Symbol word, int wordIdx, Symbol features,
			   boolean neverObserved, SexpList tagSet,
			   boolean wordIsUnknown, Symbol origWord,
			   ConstraintSet constraints) throws RemoteException {
    int i = wordIdx;
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
      EMItem item = chart.getNewEMItem();
      item.set(tag, headWord,
	       emptySubcat, emptySubcat, null, null,
	       null,
	       startList, startList,
	       i, i,
	       false, false, true, 0,
	       probCertain);

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
      chart.add(i, i, item);
    } // end for each tag
  }

  /**
   * Constrain-parses the specified sentence and computes expected top-level
   * (maximal context) event counts.  This method performs the E-step of the
   * Inside-Outside EM algorithm (with a little bit of M thrown in, in that
   * events are aggregated over the entire sentence).
   *
   * @param sentence a list of symbols representing the words of a sentence
   * @return a counts table of the top-level (maximal context) events generated
   *         while constrain-parsing this sentence
   *
   * @throws RemoteException
   */
  protected CountsTable parseAndCollectEventCounts(SexpList sentence)
    throws RemoteException {
    return parseAndCollectEventCounts(sentence, null);
  }

  /**
   * Constrain-parses the specified sentence and computes expected top-level
   * (maximal context) event counts.  This method performs the E-step of the
   * Inside-Outside EM algorithm (with a little bit of M thrown in, in that
   * events are aggregated over the entire sentence).
   *
   * @param sentence a list of symbols representing the words of a sentence
   * @param tags     a list of symbols that represent the part-of-speech tags of
   *                 the words of the specified sentence (coordinated with the
   *                 specified list of words)
   * @return a counts table of the top-level (maximal context) events generated
   *         while constrain-parsing this sentence
   *
   * @throws RemoteException
   */
  protected CountsTable parseAndCollectEventCounts(SexpList sentence,
						   SexpList tags)
    throws RemoteException {
    return parseAndCollectEventCounts(sentence, tags, null);
  }

  /**
   * Constrain-parses the specified sentence and computes expected top-level
   * (maximal context) event counts.  This method performs the E-step of the
   * Inside-Outside EM algorithm (with a little bit of M thrown in, in that
   * events are aggregated over the entire sentence).
   *
   * @param sentence    a list of symbols representing the words of a sentence
   * @param tags        a list of symbols that represent the part-of-speech tags
   *                    of the words of the specified sentence (coordinated with
   *                    the specified list of words)
   * @param constraints a set of parsing constraints
   * @return a counts table of the top-level (maximal context) events generated
   *         while constrain-parsing this sentence
   *
   * @throws RemoteException
   */
  protected CountsTable parseAndCollectEventCounts(SexpList sentence,
						   SexpList tags,
						   ConstraintSet constraints)
    throws RemoteException {

    sentenceIdx++;

    if (debugOutputAllCounts)
      Debug.level = 21;

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

    addTopUnaries(sentLen - 1);

    // go through chart and compute outside probs
    computeOutsideProbs();

    // create map of TrainerEvent objects to their expected counts
    computeEventCounts();

    if (eventCounts.size() == 0) {
      System.err.println(className + ": warning: zero event counts for " +
			 "sentence " + sentence);
    }

    // the chart mixes two types of items that cover the entire span
    // of the sentnece: those that have had their +TOP+ probability multiplied
    // in (with topSym as their label) and those that have not; if the
    // top-ranked item also has topSym as its label, we're done; otherwise,
    // we look through all items that cover the entire sentence and get
    // the highest-ranked item whose label is topSym (NO WE DO NOT, since
    // we reset the top-ranked item just before adding top unaries)
    /*
    EMItem topRankedItem = null;
    EMItem potentialTopItem = (EMItem)chart.getTopItem(0, sentLen - 1);
    if (potentialTopItem != null && potentialTopItem.label() == topSym)
      topRankedItem = potentialTopItem;

    if (debugTop)
      System.err.println(className + ": top-ranked +TOP+ item: " +
			 topRankedItem);
    */

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
     double highestProb = Constants.logOfZero;
     Iterator it = chart.get(0, sentLen - 1);
     while (it.hasNext()) {
       EMItem item = (EMItem)it.next();
       if (item.label() != topSym)
	 continue;
       if (item.logProb() > highestProb) {
	 topRankedItem = item;
	 highestProb = item.logProb();
       }
     }
    }
    */

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
	//os.writeObject(topRankedItem);
	os.writeObject(null);
	os.writeObject(sentence);
	os.writeObject(originalWords);
	os.close();
      }
      catch (IOException ioe) {
	System.err.println(ioe);
      }
    }

    chart.postParseCleanup();

    /*
    if (topRankedItem == null) {
      sentence.clear();
      sentence.addAll(originalSentence); // restore original sentence
      return null;
    }
    else {
      Sexp tree = topRankedItem.headChild().toSexp();
      postProcess(tree);
      return tree;
    }
    */

    return eventCounts;
  }

  /**
   * Computes outside probabilities for the entire chart.  This step depends
   * on having computed inside probabilities via constrain-parsing first.
   */
  protected void computeOutsideProbs() {
    for (int span = sentLen; span > 0; span--) {
      int split = sentLen - span + 1;
      for (int start = 0; start < split; start++) {
	int end = start + span - 1;
	computeOutsideProbs(start, end);
      }
    }
  }

  /**
   * Computes outside probabilities for all derivations in the specified span.
   * @param start the index of the first word in the span whose chart items'
   * outside probabilities are to be computed
   * @param end  the index of the last word in the span whose chart items'
   * outside probabilities are to be computed
   */
  protected void computeOutsideProbs(int start, int end) {
    // first, do a topological sort on items
    int[] levelCounts = chart.unaryLevelCounts(start, end);
    int numLevels = chart.numUnaryLevels(start, end);
    EMItem[][] sortedItems = new EMItem[numLevels][];
    // keep an array of indices to keep track of where we are in each
    // of the numLevels arrays of EMItem when we are filling them up
    int[] index = new int[numLevels];
    for (int i = 0; i < numLevels; i++)
      sortedItems[i] = new EMItem[levelCounts[i]];
    Iterator items = chart.get(start, end);
    while (items.hasNext()) {
      EMItem item = (EMItem)items.next();
      int level = item.unaryLevel();
      sortedItems[level][index[level]++] = item;
    }
    // let's do a sanity check
    for (int i = 0; i < numLevels; i++)
      if (levelCounts[i] != index[i])
	System.err.println(className + ": error: expected " + levelCounts[i] +
			   " items at [" + start + "," + end + "], " +
			   "unary level " + i + " but only found " + index[i]);

    // now, starting with items at the highest level, compute outside probs
    for (int level = numLevels - 1; level >= 0; level--) {
      int numItems = sortedItems[level].length;
      for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
	EMItem item = sortedItems[level][itemIdx];
	// handle base case
	if (item.label() == topSym)
          item.setOutsideProb(probCertain);
	if (item.outsideProb() > probImpossible) {
	  EMItem.AntecedentPair pair = item.antecedentPairs();
	  for ( ; pair != null; pair = pair.next()) {
	    EMItem ante1 = pair.first();
	    EMItem ante2 = pair.second();
	    double eventProbMass = 1.0; // set to multiplicative identity
	    double[] probs = pair.probs();
	    for (int i = 0; i < probs.length; i++)
	      eventProbMass *= probs[i];
	    double ante2InsideProb =
	      ante2 == null ? probCertain : ante2.insideProb();
	    double ante1OutsideProbMass =
	      item.outsideProb() * eventProbMass * ante2InsideProb;
	    ante1.increaseOutsideProb(ante1OutsideProbMass);
	    if (ante2 != null) {
	      double ante2OutsideProbMass =
		item.outsideProb() * eventProbMass * ante1.insideProb();
	      ante2.increaseOutsideProb(ante2OutsideProbMass);
	    }
	  }
	}
      }
    }
  }

  /**
   * Returns a counts table with the expected couunt of all top-level events
   * produced when constrain-parsing the current sentence.
   * @return a counts table with the expected couunt of all top-level events
   * produced when constrain-parsing the current sentence.
   */
  protected CountsTable computeEventCounts() {
    eventCounts.clear();
    double sentenceProb = 0.0; // set initially to additive identity
    // sum over inside probs of all +TOP+ items to get sentenceProb
    Iterator sentSpanItems = chart.get(0, sentLen - 1);
    while (sentSpanItems.hasNext()) {
      EMItem item = (EMItem)sentSpanItems.next();
      if (item.label() == topSym)
	sentenceProb += item.insideProb();
    }

    double sentenceProbInverse = 1 / sentenceProb;

    if (Double.isInfinite(sentenceProbInverse)) {
      System.err.println(className + ": warning: skipping sentence because " +
			 "1 / sentenceProb is infinite (underflow); " +
			 "sentenceProb=" + sentenceProb);
      return eventCounts;
    }

    if (debugInsideProbs) {
      double sentenceLogProb = Math.log(sentenceProb);
      cummulativeInsideLogProb += sentenceLogProb;
      System.err.println(className + ": sentence inside prob: " + sentenceProb);
      System.err.println(className + ": sentence inside logProb: " +
			 sentenceLogProb);
      System.err.println(className + ": cummulative inside log prob: " +
			 cummulativeInsideLogProb);
    }

    for (int span = sentLen; span > 0; span--) {
      int split = sentLen - span + 1;
      for (int start = 0; start < split; start++) {
	int end = start + span - 1;
	computeEventCounts(start, end, sentenceProbInverse, eventCounts);
      }
    }
    return eventCounts;
  }

  /**
   * Computes expected counts for top-level (maximal context) events produced for the specified span
   * when decoding the current sentence; stores these events and their expected
   * counts in the specified {@link CountsTable} object.
   *
   * @param start the index of the first word in the span whose expected event
   * counts are to be computed
   * @param end the index of the last word in the span whose expected event
   * counts are to be computed
   * @param sentenceProbInverse the inverse of the total inside probability
   * of the current sentence under the current model
   * @param counts the table in which to store expected event counts
   */
  protected void computeEventCounts(int start, int end,
				    double sentenceProbInverse,
				    CountsTable counts) {

    Iterator items = chart.get(start, end);
    while (items.hasNext()) {
      EMItem item = (EMItem)items.next();
      // foreach antecedent singleton/pair
      //   foreach event that yielded the current item (consequent)
      //      expected count of event =
      //        sentenceProbInverse * eventProb *
      //        ante1.insideProb() * ante2.insideProb() * item.outsideProb(),
      if (item.outsideProb() > probImpossible) {
        if (debugOutputAllInsideProbs) {
          System.err.println(className + ": span=(" + start + "," + end +
                             "), " + item);
        }
	EMItem.AntecedentPair pair = item.antecedentPairs();
	for ( ; pair != null; pair = pair.next()) {
	  EMItem ante1 = pair.first();
	  EMItem ante2 = pair.second();
	  double ante2InsideProb =
	    ante2 == null ? probCertain : ante2.insideProb();
	  double[] prob = pair.probs();
	  double eventProb = 1.0;       // set to multiplicative identity
	  for (int i = 0; i < prob.length; i++)
	    eventProb *= prob[i];
	  TrainerEvent[] event = pair.events();
          for (int i = 0; i < event.length; i++) {
            double expectedCount =
              sentenceProbInverse * eventProb *
              ante1.insideProb() * ante2InsideProb * item.outsideProb();
            /*
            if (item.stop()) {
              System.err.println("item: " + item);
              System.err.println("sentenceProbInverse=" + sentenceProbInverse +
                                 "; eventProb=" + eventProb +
                                 "; ante1Inside=" +
                                 ante1.insideProb() + "; ante2Inside=" +
                                 ante2InsideProb + "; item.outside=" +
                                 item.outsideProb() + "; expectedCount=" +
                                 expectedCount);
              String name = event[i] instanceof HeadEvent ? "head" : "mod";
              System.err.println("(" + name + " " + event[i] + " " +
                                 expectedCount + ")");
            }
            */
	    if (event[i].parent() == topSym)
	      addSynthesizedTopModEvent(event[i], expectedCount, counts);

            if (ante1.isPreterminal())
              addPretermHeadEvent(ante1, expectedCount, counts);
	    if (ante2 != null && ante2.isPreterminal())
              addPretermHeadEvent(ante2, expectedCount, counts);

	    if (ante1.isPreterminal() && ante2 != null && ante2.isPreterminal())
	      System.err.println("WARNING: two preterminal antecedents");

	    if (Double.isInfinite(expectedCount)) {
	      System.err.println("WARNING: adding infinite count for " +
				 event[i] + " of item " + item);
	    }

	    counts.add(event[i], expectedCount);
	  }
	}
      }
    }
  }

  /**
   * Adds an event as though a tree's non-hidden root is a modifier of
   * <tt>+TOP+</tt> (in addition to being a head child).  Without these
   * &ldquo;fake&rdquo; events, the last level of the modifier-word model (such
   * as {@link danbikel.parser.ms.ModWordModelStructure2}) would not contain
   * counts for words that are the head of the entire sentence (since they are
   * not generated as modifiers of anything). This enables the (now deprecated)
   * count-sharing scheme to work, whereby the last back-off level of {@link
   * danbikel.parser.ms.TopLexModelStructure1} would use the
   * <i>p</i>(<i>w</i>|<i>t</i>) counts from the last level of {@link
   * danbikel.parser.ms.ModWordModelStructure2}.
   *
   * @param event the {@link HeadEvent} instance for an observed tree root,
   * from which a {@link ModifierEvent} is to be produced
   * @param expectedCount the expected count of the specified head-generation
   * event
   *
   * @see Settings#trainerShareCounts
   */
  protected void addSynthesizedTopModEvent(TrainerEvent event,
					   double expectedCount,
					   CountsTable counts) {
    HeadEvent topHead = (HeadEvent)event;
    ModifierEvent topMod =
      new ModifierEvent(topHead.headWord(), topHead.headWord(),
			topHead.head(), startList, startWordList,
			topSym, topHead.head(), emptySubcat,
			false, false);
    counts.add(topMod, expectedCount);
  }

  /**
   * Whenever a preterminal is generated, either as a head child or a modifier
   * of some other item, a trivial head-generation event is added, generating
   * the word from the lexicalized preterminal, which by design always
   * generates its head word with probability 1.  These extra events allow
   * the trainer to gather counts for all lexicalized nonterminals using
   * only the head-generation event counts table, thereby eliminating the
   * somewhat risky scheme of &ldquo;count sharing&rdquo;.
   *
   * @see Settings#trainerShareCounts
   */
  protected void addPretermHeadEvent(EMItem item, double expectedCount,
				     CountsTable counts) {
    Word headWord = item.headWord();
    HeadEvent pretermHeadEvent =
      new HeadEvent(headWord, (Symbol)item.label(), headWord.word(),
		    emptySubcat, emptySubcat);
    counts.add(pretermHeadEvent, expectedCount);
  }

  protected void addTopUnaries(int end) throws RemoteException {
    int level = chart.numUnaryLevels(0, end); // this will be a new unary level

    // first, collect all stopped items that span the entire sentence in
    // our own HashSet object, so that we aren't iterating over the chart's
    // sentence-span cell while we're trying to add to it
    topProbItemsToAdd.clear();
    Iterator sentSpanItems = chart.get(0, end);
    while (sentSpanItems.hasNext()) {
      EMItem item = (EMItem)sentSpanItems.next();
      if (item.stop()) {
	topProbItemsToAdd.add(item);
      }
    }
    sentSpanItems = topProbItemsToAdd.iterator();
    while (sentSpanItems.hasNext()) {
      EMItem item = (EMItem)sentSpanItems.next();
      HeadEvent headEvent = lookupHeadEvent;
      headEvent.set(item.headWord(), topSym, (Symbol)item.label(),
		    emptySubcat, emptySubcat);

      if (debugTop) {
	Debug.level = 21;
      }

      double topProb = server.probTop(id, headEvent);
      double insideProb = item.insideProb() * topProb;

      if (debugTop)
	System.err.println(className +
			   ": item=" + item + "; topProb=" + topProb +
			   "; item.insideProb()=" + item.insideProb() +
			   "; insideProb=" + insideProb);

      if (topProb == probImpossible)
	continue;

      EMItem newItem = chart.getNewEMItem();
      newItem.set(topSym, item.headWord(),
		  emptySubcat, emptySubcat, item,
		  null, null, startList, startList, 0, end,
		  false, false, true, level, insideProb);
      chart.add(0, end, newItem, item, null,
		headEvent.shallowCopy(), topProb);
    }
  }

  protected void complete(int start, int end)
      throws RemoteException, TimeoutException {
    for (int split = start; split < end; split++) {

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
	    EMItem modifierItem = (EMItem)modifierItems.next();
	    if (modifierItem.stop()) {
	      Iterator modificandItems =
		chart.get(modificandStartIdx, modificandEndIdx);
	      while (modificandItems.hasNext()) {
		EMItem modificandItem = (EMItem)modificandItems.next();
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
	    EMItem modifierItem = (EMItem)modifierItems.next();
	    if (modifierItem.stop()) {
	      Iterator modificandItems =
		chart.get(modificandStartIdx, modificandEndIdx);
	      while (modificandItems.hasNext()) {
		EMItem modificandItem = (EMItem)modificandItems.next();
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

    // no pruning when doing EM, so we comment out the following line
    //chart.prune(start, end);
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
  protected void joinItems(EMItem modificand, EMItem modifier,
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
      boolean modificandLabelP = modificandLabel == NPB;
      boolean modLabelP = modLabel == PP;
      int spanStart = Math.min(modificand.start(), modifier.start());
      int spanEnd = Math.max(modificand.end(), modifier.end());
      debugFlag = spanStart == 10 && spanEnd == 13;
      if (debugFlag) {
	System.err.println(className + ".join: trying to extend modificand\n" +
			   modificand + "\nwith modifier\n" + modifier);
	Debug.level = 21;
      }
    }

    if (!futurePossible(modEvent, side, debugFlag)) {
      if (debugFlag)
	Debug.level = 0;
      return;
    }

    if (debugJoin) {
    }

    int lowerIndex = Math.min(thisSideEdgeIndex, oppositeSideEdgeIndex);
    int higherIndex = Math.max(thisSideEdgeIndex, oppositeSideEdgeIndex);

    double modProb = server.probMod(id, modEvent);
    if (modProb == probImpossible) {
      if (debugFlag)
	Debug.level = 0;
      return;
    }
    double insideProb =
      modificand.insideProb() * modifier.insideProb() * modProb;

    if (debugJoin) {
      if (debugFlag) {
	System.err.println(className + ".join: trying to extend modificand\n" +
			   modificand + "\nwith modifier\n" + modifier +
			   "\n\twith modProb=" + modProb + " for event " +
			   modEvent + " for a combined inside prob of " +
			   insideProb);
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

    EMItem newItem = chart.getNewEMItem();
    newItem.set((Symbol)modificand.label(), modificand.headWord(),
		null, null, modificand.headChild(), null, null, null, null,
		lowerIndex, higherIndex, false, false, false, 0,
		insideProb);

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

    ModifierEvent modEventCopy = (ModifierEvent)modEvent.shallowCopy();
    modEventCopy.setPreviousWords(modEvent.previousWords().copy());

    chart.add(lowerIndex, higherIndex, newItem, modificand, modifier,
	      modEventCopy, modProb);

    if (debugJoin) {
      Debug.level = 0;
    }
  }

  private boolean futurePossible(ModifierEvent modEvent, boolean side,
				 boolean debug) {
    ProbabilityStructure modPS = modNonterminalPS;
    int lastLevel = modNonterminalPSLastLevel;
    boolean onLeft = side == Constants.LEFT;
    Event historyContext = modPS.getHistory(modEvent, lastLevel);
    Set possibleFutures = (Set)modNonterminalMap.get(historyContext);
    if (possibleFutures != null) {
      Event currentFuture = modPS.getFuture(modEvent, lastLevel);
      if (possibleFutures.contains(currentFuture))
	return true;
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

  protected void addUnariesAndStopProbs(int start, int end)
  throws RemoteException {
    prevItemsAdded.clear();
    currItemsAdded.clear();
    stopProbItemsToAdd.clear();

    int level = 1;

    Iterator it = chart.get(start, end);
    while (it.hasNext()) {
      EMItem item = (EMItem)it.next();
      if (item.stop() == false)
	stopProbItemsToAdd.add(item);
      else if (item.isPreterminal())
	prevItemsAdded.add(item);
    }

    if (stopProbItemsToAdd.size() > 0) {
      it = stopProbItemsToAdd.iterator();
      while (it.hasNext())
	addStopProbs((EMItem)it.next(), prevItemsAdded, level);
      level++;
    }

    int i = -1;
    for (i = 0; i < MAX_UNARY_PRODUCTIONS && prevItemsAdded.size() > 0; i++) {
    //for (i = 0; prevItemsAdded.size() > 0; i++) {
      Iterator prevItems = prevItemsAdded.iterator();
      while (prevItems.hasNext()) {
	EMItem item = (EMItem)prevItems.next();
	if (!item.garbage())
	  addUnaries(item, currItemsAdded, level);
      }
      level++;

      exchangePrevAndCurrItems();
      currItemsAdded.clear();

      prevItems = prevItemsAdded.iterator();
      while (prevItems.hasNext()) {
	EMItem item = (EMItem)prevItems.next();
	if (!item.garbage())
	  addStopProbs(item, currItemsAdded, level);
      }
      level++;

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

  protected List addUnaries(EMItem item, List itemsAdded, int level)
  throws RemoteException {
    EMItem newItem = chart.getNewEMItem();
    // set some values now, most to be filled in by code below
    newItem.set(null, item.headWord(), null, null, item,
		null, null, startList, startList,
		item.start(), item.end(),
		false, false, false, level, 0.0);
    Symbol headSym = (Symbol)item.label();
    HeadEvent headEvent = lookupHeadEvent;
    headEvent.set(item.headWord(), null, headSym, emptySubcat, emptySubcat);
    // foreach (possible) nonterminal
    Symbol[] nts = (useHeadToParentMap ?
		    (Symbol[])headToParentMap.get(item.label()) : nonterminals);
    int numNTs = nts.length;
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
	if (item.start() == 19 && item.end() == 27 && parent == SA) {
	  System.err.println(className +
			     ".addUnaries: trying to buid on " + headSym +
			     " with " + parent);
	  Debug.level = 21;
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

	    double probLeftSubcat =
	      (numLeftSubcats == 1 ? probCertain :
	       server.probLeftSubcat(id, headEvent));
	    double probRightSubcat =
	      (numRightSubcats == 1 ? probCertain :
	       server.probRightSubcat(id, headEvent));
	    double probHead = server.probHead(id, headEvent);
	    if (probHead == probImpossible)
	      continue;
	    double eventProbMass = probHead * probLeftSubcat * probRightSubcat;
	    double insideProb = item.insideProb() * eventProbMass;

	    if (debugUnaries) {
	    }

	    if (insideProb == probImpossible)
	      continue;

	    newItem.setInsideProb(insideProb);

	    EMItem newItemCopy = chart.getNewEMItem();
	    newItemCopy.setDataFrom(newItem);
	    boolean added = chart.add(newItemCopy.start(), newItemCopy.end(),
                                      newItemCopy,
                                      item, null,
                                      headEvent.shallowCopy(), eventProbMass);
            if (added)
              itemsAdded.add(newItemCopy);
	  }
	} // end foreach possible left subcat
      }
    }

    chart.reclaimItem(newItem);

    if (debugUnaries)
      Debug.level = 0;

    return itemsAdded;
  }

  protected List addStopProbs(EMItem item, List itemsAdded, int level)
    throws RemoteException {
    if (!(item.leftSubcat().empty() && item.rightSubcat().empty()))
      return itemsAdded;

    /*
    SexpList leftPrevMods =
      getPrevMods(item, item.leftPrevMods(), item.leftChildren());
    SexpList rightPrevMods =
      getPrevMods(item, item.rightPrevMods(), item.rightChildren());
    */

    // technically, we should invoke getPrevMods for both lists here, but there
    // shouldn't be skipping of previous mods because of generation of stopSym
    SexpList leftPrevMods = item.leftPrevMods();
    SexpList rightPrevMods = item.rightPrevMods();

    tmpChildrenList.set(null, item.leftChildren());
    WordList leftPrevWords = getPrevModWords(item, tmpChildrenList, LEFT);
    tmpChildrenList.set(null, item.rightChildren());
    WordList rightPrevWords = getPrevModWords(item, tmpChildrenList, RIGHT);

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
      if (item.start() == 19 && item.end() == 27 &&
          (item.label() == SA || item.label() == NPA)) {
	System.err.println(className +
			   ".addStopProbs: trying to add stops to item " +
			   item);
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

    double leftProb = server.probMod(id, leftMod);
    if (leftProb == probImpossible)
      return itemsAdded;
    double rightProb = server.probMod(id, rightMod);
    if (rightProb == probImpossible)
      return itemsAdded;
    double insideProb =
      item.insideProb() * leftProb * rightProb;

    if (debugStops) {
      if (item.start() == 19 && item.end() == 27 &&
          (item.label() == SA || item.label() == NPA)) {
	System.err.println(className + ".addStopProbs: adding stops to item " +
			   item);
        Debug.level = 21;
        server.probMod(id, leftMod);
        server.probMod(id, rightMod);
        Debug.level = 0;
        System.err.println("\tnew inside prob: " + insideProb);
      }
    }

    if (insideProb == probImpossible)
      return itemsAdded;

    EMItem newItem = chart.getNewEMItem();
    newItem.set((Symbol)item.label(), item.headWord(),
		item.leftSubcat(), item.rightSubcat(),
		item.headChild(),
		item.leftChildren(), item.rightChildren(),
		item.leftPrevMods(), item.rightPrevMods(),
		item.start(), item.end(), item.leftVerb(),
		item.rightVerb(), true, level, insideProb);

    if (isomorphicTreeConstraints) {
      if (debugConstraints)
	System.err.println("assigning satisfied constraint " +
			   item.getConstraint() + " to " + newItem);
      newItem.setConstraint(item.getConstraint());
    }

    // we can get away with using only a shallow copy, except we need to
    // make an explicit (shallow) copy of the WordList object, since the
    // one returned by getPrevModWords is one of only two possible "lookup"
    // objects, prevModWordLeftLookupList or prevModWordRightLookupList
    ModifierEvent leftModCopy = (ModifierEvent)leftMod.shallowCopy();
    leftModCopy.setPreviousWords(leftMod.previousWords().copy());
    ModifierEvent rightModCopy = (ModifierEvent)rightMod.shallowCopy();
    rightModCopy.setPreviousWords(rightMod.previousWords().copy());

    boolean added = chart.add(item.start(), item.end(), newItem,
                              item, null,
                              new TrainerEvent[]{leftModCopy, rightModCopy},
                              new double[]      {leftProb,    rightProb});
    if (added)
      itemsAdded.add(newItem);

    return itemsAdded;
  }

  /**
   * Creates a new previous-modifier list given the specified current list
   * and the last modifier on a particular side.
   *
   * @param modChildren the last node of modifying children on a particular
   * side of the head of a chart item
   * @return the list whose first element is the label of the specified
   * modifying child and whose subsequent elements are those of the
   * specified <code>itemPrevMods</code> list, without its final element
   * (which is "bumped off" the edge, since the previous-modifier list
   * has a constant length)
   */
  private final SexpList getPrevMods(EMItem item, SLNode modChildren) {
    if (modChildren == null)
      return startList;
    prevModLookupList.clear();
    SexpList prevMods = prevModLookupList;
    int i = 0; // index in prev mod list we are constructing
    // as long as there are children and we haven't reached the numPrevMods
    // limit, set elements of prevModList, starting at index 0
    for (SLNode curr = modChildren; curr != null && i < numPrevMods; ) {
      Symbol prevMod = (curr.next() == null ? startSym :
			(Symbol)((EMItem)curr.next().data()).label());
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
      prevMods = (SexpList)prevMods.deepCopy();
      canonicalPrevModLists.put(prevMods, prevMods);
      canonical = prevMods;
    }
    return canonical;
  }

  private final WordList getPrevModWords(EMItem item, SLNode modChildren,
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
		       (Word)((EMItem)curr.next().data()).headWord());
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

  private final Symbol getSentenceWord(int index) {
    return (index >= sentLen ? null :
	    (sentence.get(index).isSymbol() ? sentence.symbolAt(index) :
	     sentence.listAt(index).symbolAt(1)));

  }
}
