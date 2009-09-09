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

import danbikel.util.Filter;
import danbikel.util.FlexibleMap;
import danbikel.util.Time;
import danbikel.lisp.*;
import java.io.*;
import java.util.*;

/**
 * Provides access to all {@link Model} objects and maps necessary for parsing.
 * By bundling all of this information together, all of the objects necessary
 * for parsing can be stored and retrieved simply by serializing and
 * de-serializing this object to a Java object file.  The types of output
 * elements that are modeled are determined by {@link ProbabilityStructure}
 * objects around which {@link Model} objects are wrapped, by the method {@link
 * ProbabilityStructure#newModel()}. This collection holds ten different {@link
 * Model} objects, each modeling a different output element of this parser
 * (nonterminal, word, subcategorization frame, etc.) becuase each wraps a
 * different type of {@link ProbabilityStructure} object. The concrete types of
 * {@link ProbabilityStructure} objects are determined by various run-time
 * settings, as described in the documentation for
 * {@link Settings#globalModelStructureNumber}.  The other counts tables,
 * maps and resources contained in this object are derived by the
 * {@link Trainer}.
 *
 * @see Settings#globalModelStructureNumber
 * @see Settings#precomputeProbs
 * @see Settings#writeCanonicalEvents
 */
public class ModelCollection implements Serializable {

  // constants
  /**
   * Indicates whether to output verbose messages to <code>System.err</code>.
   * The value of this constant is normally <code>true</code> (this <i>is</i>
   * research software, after all).
   */
  protected final static boolean verbose = true;
  /**
   * Indicates whether to invoke <code>System.gc()</code> after
   * this object has been de-serialized from a stream.
   */
  protected final static boolean callGCAfterReadingObject = false;

  // data members

  /**
   * An array containing all <code>Model</code> objects contained by this
   * model collection, set up by {@link #createModelArray()}.
   */
  protected transient Model[] modelArr;
  /** The model for lexical priors. */
  protected transient Model lexPriorModel;
  /** The model for nonoterminal priors. */
  protected transient Model nonterminalPriorModel;
  /**
   * The model for generating observed root nonterminals given the hidden
   * <tt>+TOP+</tt> nonterminal.
   */
  protected transient Model topNonterminalModel;

  /**
   * The model for generating the head word and part of speech of observed
   * root nonterminals given the hidden <tt>+TOP+</tt> nonterminal.
   */
  protected transient Model topLexModel;
  /**
   * The model for generating a head nonterminal given its (lexicalized)
   * parent.
   */
  protected transient Model headModel;
  /** The model for generating gaps. */
  protected transient Model gapModel;
  /** The model for generating subcats on the left side of the head child. */
  protected transient Model leftSubcatModel;
  /** The model for generating subcats on the right side of the head child. */
  protected transient Model rightSubcatModel;
  /**
   * The model for generating partially-lexicalized nonterminals that modify
   * the head child.
   */
  protected transient Model modNonterminalModel;
  /**
   * The model for generating head words of lexicalized nonterminals that
   * modify the head child.
   */
  protected transient Model modWordModel;
  /**
   * A table that maps observed words to their counts in the training
   * corpus.
   */
  protected transient CountsTable vocabCounter;
  /**
   * A table that maps observed word-feature vectors to their counts
   * in the training corpus.
   */
  protected transient CountsTable wordFeatureCounter;
  /**
   * A table that maps unlexicalized nonterminals to their counts in the
   * training corpus.
   */
  protected transient CountsTable nonterminals;
  /**
   * A mapping from lexical items to all of their possible parts
   * of speech.
   */
  protected transient Map posMap;

  /**
   * A mapping from head labels to possible parent labels.
   */
  protected transient Map headToParentMap;

  /**
   * A mapping from left subcat-prediction conditioning contexts (typically
   * parent and head nonterminal labels) to all possible subcat frames.
   */
  protected transient Map leftSubcatMap;
  /**
   * A mapping from right subcat-prediction conditioning contexts (typically
   * parent and head nonterminal labels) to all possible subcat frames.
   */
  protected transient Map rightSubcatMap;
  /**
   * A mapping from the last level of back-off of modifying nonterminal
   * conditioning contexts to all possible modifying nonterminals.
   */
  protected transient Map modNonterminalMap;
  /**
   * A map from unlexicalized parent-head-side triples to all possible
   * partially-lexicalized modifying nonterminals.
   */
  protected transient Map simpleModNonterminalMap;
  /** The set of preterminals pruned during training. */
  protected transient Set prunedPreterms;
  /** The set of punctuation preterminals pruned during training. */
  protected transient Set prunedPunctuation;
  /**
   * The reflexive map used to canonicalize objects created when deriving
   * counts for all models in this model collection.
   */
  protected transient FlexibleMap canonicalEvents;

  // derived transient data
  // maps from integers to nonterminals and nonterminals to integers
  /**
   * A map from nonterminal labels (<code>Symbol</code> objects) to
   * unique integers that are indices in the
   * {@linkplain #nonterminalArr nonterminal array}.
   */
  protected Map nonterminalMap;
  /**
   * An array of all nonterminal labels, providing a mapping of unique integers
   * (indices into this array) to nonterminal labels.  The inverse map is
   * contained in {@link #nonterminalMap}.
   */
  protected Symbol[] nonterminalArr;

  /**
   * Constructs a new <code>ModelCollection</code> that initially contains
   * no data.
   */
  public ModelCollection() {}

  /**
   * Sets all the data members of this object.
   *
   * @param lexPriorModel the model for marginal probabilities of
   * lexical elements (for the estimation of the joint event that is a
   * fully lexicalized nonterminal)
   * @param nonterminalPriorModel the model for conditional probabilities of
   * nonterminals given the lexical components (for the estimation of the
   * joint event that is a fully lexicalized nonterminal)
   * @param topNonterminalModel the head-generation model for heads whose
   * parents are {@link Training#topSym()}
   * @param topLexModel the head-word generation model for heads of entire
   * sentences
   * @param headModel the head-generation model
   * @param gapModel the gap-generation model
   * @param leftSubcatModel the left subcat-generation model
   * @param rightSubcatModel the right subcat-generation mode,l
   * @param modNonterminalModel the modifying nonterminal-generation model
   * @param modWordModel the modifying word-generation model
   * @param vocabCounter a table of counts of all "known" words of the
   * training data
   * @param wordFeatureCounter a table of counts of all word features ("unknown"
   * words) of the training data
   * @param nonterminals a table of counts of all nonterminals occurring in
   * the training data
   * @param posMap a mapping from lexical items to all of their possible parts
   * of speech
   * @param leftSubcatMap a mapping from left subcat-prediction conditioning
   * contexts (typically parent and head nonterminal labels) to all possible
   * subcat frames
   * @param rightSubcatMap a mapping from right subcat-prediction conditioning
   * contexts (typically parent and head nonterminal labels) to all possible
   * subcat frames
   * @param modNonterminalMap a mapping from the last level of back-off of
   * modifying nonterminal conditioning contexts to all possible modifying
   * nonterminals
   * @param simpleModNonterminalMap a mapping from parent-head-side triples
   * to all possible partially-lexicalized modifying nonterminals
   * @param prunedPreterms the set of preterminals pruned during training
   * @param prunedPunctuation the set of punctuation preterminals pruned
   * during training
   * @param canonicalEvents the reflexive map used to canonicalize objects
   * created when deriving counts for all models in this model collection
   */
  public void set(Model lexPriorModel,
		  Model nonterminalPriorModel,
		  Model topNonterminalModel,
		  Model topLexModel,
		  Model headModel,
		  Model gapModel,
		  Model leftSubcatModel,
		  Model rightSubcatModel,
		  Model modNonterminalModel,
		  Model modWordModel,
		  CountsTable vocabCounter,
		  CountsTable wordFeatureCounter,
		  CountsTable nonterminals,
		  Map posMap,
		  Map headToParentMap,
		  Map leftSubcatMap,
		  Map rightSubcatMap,
		  Map modNonterminalMap,
                  Map simpleModNonterminalMap,
		  Set prunedPreterms,
		  Set prunedPunctuation,
		  FlexibleMap canonicalEvents) {
    this.lexPriorModel = lexPriorModel;
    this.nonterminalPriorModel = nonterminalPriorModel;
    this.topNonterminalModel = topNonterminalModel;
    this.topLexModel = topLexModel;
    this.headModel = headModel;
    this.gapModel = gapModel;
    this.leftSubcatModel = leftSubcatModel;
    this.rightSubcatModel = rightSubcatModel;
    this.modNonterminalModel = modNonterminalModel;
    this.modWordModel = modWordModel;

    createModelArray();

    this.vocabCounter = vocabCounter;
    this.wordFeatureCounter = wordFeatureCounter;
    this.nonterminals = nonterminals;
    this.posMap = posMap;
    this.headToParentMap = headToParentMap;
    this.leftSubcatMap = leftSubcatMap;
    this.rightSubcatMap = rightSubcatMap;
    this.modNonterminalMap = modNonterminalMap;
    this.simpleModNonterminalMap = simpleModNonterminalMap;
    this.prunedPreterms = prunedPreterms;
    this.prunedPunctuation = prunedPunctuation;

    createNonterminalMap();

    this.canonicalEvents = canonicalEvents;
  }

  /**
   * Populates the {@link #modelArr} with the {@link Model} objects that
   * are contained in this model collection.
   */
  protected void createModelArray() {
    modelArr = new Model[] {
      lexPriorModel,
      nonterminalPriorModel,
      topNonterminalModel,
      topLexModel,
      headModel,
      gapModel,
      leftSubcatModel,
      rightSubcatModel,
      modNonterminalModel,
      modWordModel,
    };
  }

  /**
   * Returns an unmodifiable list view of the {@link Model} objects contained
   * in this model collection.
   * @return an unmodifiable list view of the {@link Model} objects contained
   * in this model collection
   */
  public List modelList() {
    return Collections.unmodifiableList(Arrays.asList(modelArr));
  }

  /**
   * Syntactic sugar for <code>modelList().iterator()</code>.
   * @return the iterator of the list returned by {@link #modelList()}
   */
  public Iterator modelIterator() {
    return modelList().iterator();
  }

  private void createNonterminalMap() {
    nonterminalMap = new HashMap(nonterminals.size());
    nonterminalArr = new Symbol[nonterminals.size()];
    Iterator nts = nonterminals.keySet().iterator();
    for (int uid = 0; nts.hasNext(); uid++) {
      Symbol nonterminal = (Symbol)nts.next();
      nonterminalArr[uid] = nonterminal;
      nonterminalMap.put(nonterminal, new Integer(uid));
    }
  }

  /**
   * In a dangerous but effective way, this method shares counts for a back-off
   * level from one model with another model; in this case, the last level of
   * back-off from the {@link #modWordModel} is being shared (i.e., will be
   * used) as the last level of back-off for {@link #topLexModel}, as the last
   * levels of both these models typically estimate
   * <i>p</i>(<i>w</i>&nbsp;|&nbsp;<i>t</i>).
   *
   * @param verbose indicates whether to print a message to
   *                <code>System.err</code>
   *
   * @see Settings#trainerShareCounts
   */
  public void shareCounts(boolean verbose) {
    if (verbose)
      System.err.println("Sharing last level of modWordModel to be " +
			 "last level of topLexModel.");
    int modWordLastLevel =
	modWordModel.getProbStructure().numLevels() - 1;
    int topLexLastLevel =
	topLexModel.getProbStructure().numLevels() - 1;
    modWordModel.share(modWordLastLevel, topLexModel, topLexLastLevel);
  }

  /**
   * Returns the number of unique (unlexicalized) nonterminals observed in
   * the training data.
   */
  public int numNonterminals() { return nonterminalArr.length; }
  /**
   * Returns the {@link #nonterminalMap} member.
   */
  public Map getNonterminalMap() { return nonterminalMap; }
  /**
   * Returns the {@link #nonterminalArr} member.
   */
  public Symbol[] getNonterminalArr() { return nonterminalArr; }

  // accessors
  /**
   * Returns the model for marginal probabilities of lexical elements (for the
   * estimation of the joint event that is a fully lexicalized nonterminal)
   */
  public Model lexPriorModel() { return lexPriorModel; }
  /**
   * Returns the model for conditional probabilities of nonterminals given the
   * lexical components (for the estimation of the joint event that is a fully
   * lexicalized nonterminal)
   */
  public Model nonterminalPriorModel() { return nonterminalPriorModel; }
  /**
   * Returns the head-generation model for heads whose parents are {@link
   * Training#topSym()}.
   */
  public Model topNonterminalModel() { return topNonterminalModel; }
  /**
   * Returns the head-word generation model for heads of entire
   * sentences.
   */
  public Model topLexModel() { return topLexModel; }
  /** Returns the head-generation model. */
  public Model headModel() { return headModel; }
  /** Returns the gap-generation model. */
  public Model gapModel() { return gapModel; }
  /** Returns the left subcat-generation model. */
  public Model leftSubcatModel() { return leftSubcatModel; }
  /** Returns the right subcat-generation model. */
  public Model rightSubcatModel() { return rightSubcatModel; }
  /** Returns the modifying nonterminal&ndash;generation model. */
  public Model modNonterminalModel() { return modNonterminalModel; }
  /** Returns the model that generates head words of modifying nonterminals. */
  public Model modWordModel() { return modWordModel; }
  /**
   * Returns a mapping from {@link Symbol} objects representing words to
   * their count in the training data.
   */
  public CountsTable vocabCounter() { return vocabCounter; }
  /**
   * Returns a mapping from {@link Symbol} objects that are word features
   * to their count in the training data.
   */
  public CountsTable wordFeatureCounter() { return wordFeatureCounter; }
  /**
   * Returns a mapping of (unlexicalized) nonterminals to their counts in the
   * training data.
   */
  public CountsTable nonterminals() { return nonterminals; }
  /**
   * Returns a mapping from {@link Symbol} objects representing words to
   * {@link SexpList} objects that contain the set of their possible parts of speech
   * (a list of {@link Symbol}).
   */
  public Map posMap() { return posMap; }
  /**
   * Returns a mapping from head labels to possible parent labels.
   */
  public Map headToParentMap() { return headToParentMap; }
  /**
   * Returns a mapping from left subcat-prediction conditioning contexts
   * (typically parent and head nonterminal labels) to all possible subcat
   * frames.
   */
  public Map leftSubcatMap() { return leftSubcatMap; }
  /**
   * Returns a mapping from right subcat-prediction conditioning contexts
   * (typically parent and head nonterminal labels) to all possible subcat
   * frames.
   */
  public Map rightSubcatMap() { return rightSubcatMap; }
  /**
   * Returns a mapping from the last level of back-off of modifying nonterminal
   * conditioning contexts to all possible modifying nonterminals.
   */
  public Map modNonterminalMap() { return modNonterminalMap; }
  /**
   * Returns a map from unlexicalized parent-head-side triples to all possible
   * partially-lexicalized modifying nonterminals.
   */
  public Map simpleModNonterminalMap() { return simpleModNonterminalMap; }
  /** Returns set of preterminals pruned during training. */
  public Set prunedPreterms() { return prunedPreterms; }
  /** Returns set of punctuation preterminals pruned during training. */
  public Set prunedPunctuation() { return prunedPunctuation; }

  /**
   * Returns the reflexive map used to canonicalize objects created when
   * deriving counts for all models in this model collection.
   */
  public FlexibleMap canonicalEvents() { return canonicalEvents; }

  // utility method
  /**
   * Invokes {@link Model#getCacheStats()} on each {@link Model} contained in
   * this model collection, and returns the results as a single {@link String}.
   *
   * @return a single string that is the concatenation of all of the {@link
   *         Model#getCacheStats()} strings for the models in this collection
   */
  public String getModelCacheStats() {
    StringBuffer sb = new StringBuffer(300 * modelArr.length);
    int numModels = modelArr.length;
    for (int i = 0; i < numModels; i++) {
      sb.append(modelArr[i].getCacheStats());
    }
    return sb.toString();
  }

  // I/O methods

  /**
   * Writes this object to the specified stream.
   *
   * @param s the stream to which to write this object
   * @throws IOException if there is a problem writing to the specified stream
   */
  protected void internalWriteObject(java.io.ObjectOutputStream s)
    throws IOException {

    s.defaultWriteObject();

    Time tempTimer = null;
    if (verbose)
      tempTimer = new Time();

    if (verbose) {
      System.err.print("Writing out canonicalEvents...");
      tempTimer.reset();
    }
    boolean precomputeProbs = Settings.getBoolean(Settings.precomputeProbs);
    if (precomputeProbs)
      canonicalEvents = null;
    else {
      boolean writeCanonicalEvents =
	Settings.getBoolean(Settings.writeCanonicalEvents);
      if (!writeCanonicalEvents) {
	if (verbose)
	  System.err.print("emptying...");
	canonicalEvents.clear();
      }
    }
    s.writeObject(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out lexPriorModel...");
      tempTimer.reset();
    }
    s.writeObject(lexPriorModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out nonterminalPriorModel...");
      tempTimer.reset();
    }
    s.writeObject(nonterminalPriorModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out topNonterminalModel...");
      tempTimer.reset();
    }
    s.writeObject(topNonterminalModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out topLexModel...");
      tempTimer.reset();
    }
    s.writeObject(topLexModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out headModel...");
      tempTimer.reset();
    }
    s.writeObject(headModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out gapModel...");
      tempTimer.reset();
    }
    s.writeObject(gapModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out leftSubcatModel...");
      tempTimer.reset();
    }
    s.writeObject(leftSubcatModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out rightSubcatModel...");
      tempTimer.reset();
    }
    s.writeObject(rightSubcatModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out modNonterminalModel...");
      tempTimer.reset();
    }
    s.writeObject(modNonterminalModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out modWordModel...");
      tempTimer.reset();
    }
    s.writeObject(modWordModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out vocabCounter...");
      tempTimer.reset();
    }
    s.writeObject(vocabCounter);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out wordFeatureCounter...");
      tempTimer.reset();
    }
    s.writeObject(wordFeatureCounter);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out nonterminals...");
      tempTimer.reset();
    }
    s.writeObject(nonterminals);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out posMap...");
      tempTimer.reset();
    }
    s.writeObject(posMap);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out headToParentMap...");
      tempTimer.reset();
    }
    s.writeObject(headToParentMap);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out leftSubcatMap...");
      tempTimer.reset();
    }
    s.writeObject(leftSubcatMap);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out rightSubcatMap...");
      tempTimer.reset();
    }
    s.writeObject(rightSubcatMap);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out modNonterminalMap...");
      tempTimer.reset();
    }
    s.writeObject(modNonterminalMap);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out simpleModNonterminalMap...");
      tempTimer.reset();
    }
    s.writeObject(simpleModNonterminalMap);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out prunedPreterms...");
      tempTimer.reset();
    }
    s.writeObject(prunedPreterms);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out prunedPunctuation...");
      tempTimer.reset();
    }
    s.writeObject(prunedPunctuation);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
  }

  private void writeObject(java.io.ObjectOutputStream s)
    throws IOException {
    Time totalTime = null;
    if (verbose)
      totalTime = new Time();

    internalWriteObject(s);
    /*
    if (verbose)
      System.err.println("Total time writing out ModelCollection object: " +
			 totalTime + ".");
    */
  }

  /**
   * Reads an instance of this class from the specified stream.
   * @param s the stream from which to read an instance of this class
   * @throws IOException if there is a problem reading from the specified
   * stream
   * @throws ClassNotFoundException if any of the concrete types that
   * are in the specified stream cannot be found
   */
  protected void internalReadObject(java.io.ObjectInputStream s)
    throws IOException, ClassNotFoundException {

    Time tempTimer = null;
    if (verbose)
      tempTimer = new Time();

    if (verbose) {
      System.err.print("Reading canonicalEvents...");
      tempTimer.reset();
    }
    canonicalEvents = (FlexibleMap)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading lexPriorModel...");
      tempTimer.reset();
    }
    lexPriorModel = (Model)s.readObject();
    lexPriorModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading nonterminalPriorModel...");
      tempTimer.reset();
    }
    nonterminalPriorModel = (Model)s.readObject();
    nonterminalPriorModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading topNonterminalModel...");
      tempTimer.reset();
    }
    topNonterminalModel = (Model)s.readObject();
    topNonterminalModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading topLexModel...");
      tempTimer.reset();
    }
    topLexModel = (Model)s.readObject();
    topLexModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading headModel...");
      tempTimer.reset();
    }
    headModel = (Model)s.readObject();
    headModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading gapModel...");
      tempTimer.reset();
    }
    gapModel = (Model)s.readObject();
    gapModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading leftSubcatModel...");
      tempTimer.reset();
    }
    leftSubcatModel = (Model)s.readObject();
    leftSubcatModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading rightSubcatModel...");
      tempTimer.reset();
    }
    rightSubcatModel = (Model)s.readObject();
    rightSubcatModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading modNonterminalModel...");
      tempTimer.reset();
    }
    modNonterminalModel = (Model)s.readObject();
    modNonterminalModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading modWordModel...");
      tempTimer.reset();
    }
    modWordModel = (Model)s.readObject();
    modWordModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading vocabCounter...");
      tempTimer.reset();
    }
    vocabCounter = (CountsTable)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading wordFeatureCounter...");
      tempTimer.reset();
    }
    wordFeatureCounter = (CountsTable)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading nonterminals...");
      tempTimer.reset();
    }
    nonterminals = (CountsTable)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading posMap...");
      tempTimer.reset();
    }
    posMap = (Map)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading headToParentMap...");
      tempTimer.reset();
    }
    headToParentMap = (Map)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading leftSubcatMap...");
      tempTimer.reset();
    }
    leftSubcatMap = (Map)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading rightSubcatMap...");
      tempTimer.reset();
    }
    rightSubcatMap = (Map)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading modNonterminalMap...");
      tempTimer.reset();
    }
    modNonterminalMap = (Map)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading simpleModNonterminalMap...");
      tempTimer.reset();
    }
    simpleModNonterminalMap = (Map)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading prunedPreterms...");
      tempTimer.reset();
    }
    prunedPreterms = (Set)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading prunedPunctuation...");
      tempTimer.reset();
    }
    prunedPunctuation = (Set)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
  }

  private void readObject(java.io.ObjectInputStream s)
    throws IOException, ClassNotFoundException {
    Time totalTime = null;
    if (verbose)
      totalTime = new Time();

    s.defaultReadObject();

    internalReadObject(s);

    createModelArray();

    if (verbose)
      System.err.println("Total time reading ModelCollection object: " +
			 totalTime + ".");

    if (callGCAfterReadingObject) {
      if (verbose)
	System.err.print("gc...");
      System.gc();
      if (verbose)
	System.err.println("done");
    }
  }
}
