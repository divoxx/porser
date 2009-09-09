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
import java.rmi.RemoteException;
import java.util.*;

/**
 * A wrapper object for a {@link DecoderServerRemote} instance that provides
 * probability caching.  Allmethods that return either probabilities or log
 * probabilities first check and internal probability cache before requesting a
 * probability using the {@link DecoderServerRemote} instance.  Cache size is
 * determined by the value of the setting {@link Settings#decoderLocalCacheSize}.
 *
 * @see Settings#decoderLocalCacheSize
 * @see Settings#decoderUseLocalProbabilityCache
 * @see ProbabilityCache
 */
public class CachingDecoderServer implements DecoderServerRemote {
  /**
   * The stub through which all method invocations on this object will flow.
   * Methods that return probabilities use a cache, whereas all other methods
   * flow through to this stub directly.
   */
  protected DecoderServerRemote stub;

  /**
   * The cache used for storing probabilities.
   *
   * @see Settings#decoderLocalCacheSize
   * @see Settings#decoderUseLocalProbabilityCache
   */
  protected ProbabilityCache cache;
  /**
   * The number of cache accesses over the lifetime of this object.
   */
  protected int numAccesses = 0;
  /**
   * The number of cache hits over the lifetime of this object.
   */
  protected int numHits = 0;


  /**
   * Constructs a new instance around the specified stub.
   *
   * @param stub the stub to use for flow-through calls
   */
  public CachingDecoderServer(DecoderServerRemote stub) {
    this.stub = stub;
    int cacheSize = Settings.getInteger(Settings.decoderLocalCacheSize);
    cache = new ProbabilityCache(cacheSize);
    Settings.register(new Settings.Change() {
      public void update(Map<String, String> changedSettings) {
	int cacheSize = Settings.getInteger(Settings.decoderLocalCacheSize);
	if (cacheSize > cache.size()) {
	  cache.setMaxCapacity(cacheSize);
	}
      }
    });
  }

  /**
   * Inserts the specified {@link TrainerEvent} and its associated probability
   * into this object's probability cache.
   * @param key the event whose probability is to be cached
   * @param value the probability to be cached
   */
  protected void putInCache(TrainerEvent key, double value) {
    cache.put(key.copy(), value);
  }

  /**
   * The unique identifier of this {@link DecoderServerRemote} instance.
   * @return the id of this {@link DecoderServerRemote} instance.
   * @throws RemoteException
   */
  public int id() throws RemoteException {
    return stub.id();
  }

  public boolean acceptClientsOnlyByRequest() throws RemoteException {
    return stub.acceptClientsOnlyByRequest();
  }

  public int maxClients() throws RemoteException {
    return stub.maxClients();
  }

  public boolean alive() throws RemoteException {
    return stub.alive();
  }

  public void die(boolean now) throws RemoteException {
    stub.die(now);
  }

  public String host() throws RemoteException {
    return stub.host();
  }

  public Map posMap() throws RemoteException {
    return stub.posMap();
  }

  public Map headToParentMap() throws RemoteException {
    return stub.headToParentMap();
  }

  public Map leftSubcatMap() throws RemoteException {
    return stub.leftSubcatMap();
  }

  public Map rightSubcatMap() throws RemoteException {
    return stub.rightSubcatMap();
  }

  public Map modNonterminalMap() throws RemoteException {
    return stub.modNonterminalMap();
  }

  public Map simpleModNonterminalMap() throws RemoteException {
    return stub.simpleModNonterminalMap();
  }

  public CountsTable nonterminals() throws RemoteException {
    return stub.nonterminals();
  }

  public Set prunedPreterms() throws RemoteException {
    return stub.prunedPreterms();
  }

  public Set prunedPunctuation() throws RemoteException {
    return stub.prunedPunctuation();
  }

  /**
   * Returns either the specified word untouched, or a 3-element list as would
   * be created by {@link #convertUnknownWords(SexpList)}.
   *
   * @param originalWord the original word to be (potentially) converted
   * @param index the index of the specified word
   * @return if the specified word is unknown, a 3-element list is returned,
   * as described in {@link #convertUnknownWords(SexpList)}, or, if the
   * specified word is not unknown, then it is returned untouched
   */
  public Sexp convertUnknownWord(Symbol originalWord, int index)
    throws RemoteException {
    return stub.convertUnknownWord(originalWord, index);
  }

  /**
   * Replaces all unknown words in the specified sentence with
   * three-element lists, where the first element is the word itself, the
   * second element is a word-feature vector, as determined by the
   * implementation of {@link WordFeatures#features(Symbol,boolean)}, and
   * the third element is {@link Constants#trueSym} if this word was never
   * observed during training or {@link Constants#falseSym} if it was
   * observed at least once during training.
   *
   * @param sentence a list of symbols representing a sentence to be parsed
   */
  public SexpList convertUnknownWords(SexpList sentence) throws RemoteException {
    return stub.convertUnknownWords(sentence);
  }

  public ProbabilityStructure leftSubcatProbStructure() throws RemoteException {
    return stub.leftSubcatProbStructure();
  }

  public ProbabilityStructure rightSubcatProbStructure() throws RemoteException {
    return stub.rightSubcatProbStructure();
  }

  public ProbabilityStructure modNonterminalProbStructure() throws RemoteException {
    return stub.modNonterminalProbStructure();
  }

  /** Returns a test probability (for debugging purposes). */
  public double testProb() throws RemoteException {
    return stub.testProb();
  }

  /**
   * Returns the prior probability of generating the nonterminal contained
   * in the specified <code>HeadEvent</code>.
   */
  public double logPrior(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double logPrior = stub.logPrior(id, event);
      putInCache(event, logPrior);
      return logPrior;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  /**
   * Returns the log of the probability of generating a new head and
   * its left and right subcat frames.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the log of the probability of generating a new head and its
   * left and right subcat frames
   */
  public double logProbHeadWithSubcats(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double logProbHeadWithSubcats = stub.logProbHeadWithSubcats(id, event);
      putInCache(event, logProbHeadWithSubcats);
      return logProbHeadWithSubcats;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  public double logProbHead(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double logProbHead = stub.logProbHead(id, event);
      putInCache(event, logProbHead);
      return logProbHead;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  public double logProbLeftSubcat(int id, TrainerEvent event)
    throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double logProbLeftSubcat = stub.logProbLeftSubcat(id, event);
      putInCache(event, logProbLeftSubcat);
      return logProbLeftSubcat;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  public double logProbRightSubcat(int id, TrainerEvent event)
    throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double logProbRightSubcat = stub.logProbRightSubcat(id, event);
      putInCache(event, logProbRightSubcat);
      return logProbRightSubcat;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  public double logProbSubcat(int id, TrainerEvent event, boolean side)
    throws RemoteException {
    return
      (side == Constants.LEFT ?
       logProbLeftSubcat(id, event) : logProbRightSubcat(id, event));
  }

  /**
   * Returns the log of the probability of generating the head nonterminal
   * of an entire sentence.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the log of the probability of generating the head nonterminal
   * of an entire sentence
   */
  public double logProbTop(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double logProbTop = stub.logProbTop(id, event);
      putInCache(event, logProbTop);
      return logProbTop;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  public double logProbMod(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double logProbMod = stub.logProbMod(id, event);
      putInCache(event, logProbMod);
      return logProbMod;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  public double logProbModNT(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double logProbModNT = stub.logProbModNT(id, event);
      putInCache(event, logProbModNT);
      return logProbModNT;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  /**
   * Returns the log of the probability of generating a gap.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the log of the probability of generating a gap
   */
  public double logProbGap(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double logProbGap = stub.logProbGap(id, event);
      putInCache(event, logProbGap);
      return logProbGap;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  // non-log prob methods
  public double probHead(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double probHead = stub.probHead(id, event);
      putInCache(event, probHead);
      return probHead;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  public double probMod(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double probMod = stub.probMod(id, event);
      putInCache(event, probMod);
      return probMod;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }
  public double probLeftSubcat(int id, TrainerEvent event)
    throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double probLeftSubcat = stub.probLeftSubcat(id, event);
      putInCache(event, probLeftSubcat);
      return probLeftSubcat;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }
  public double probRightSubcat(int id, TrainerEvent event)
    throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double probRightSubcat = stub.probRightSubcat(id, event);
      putInCache(event, probRightSubcat);
      return probRightSubcat;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }
  public double probTop(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double probTop = stub.probTop(id, event);
      putInCache(event, probTop);
      return probTop;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }
}
