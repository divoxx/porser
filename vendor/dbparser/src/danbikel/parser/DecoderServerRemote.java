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
import danbikel.switchboard.*;
import java.rmi.*;
import java.util.*;

/**
 * Specifies all methods necessary for a decoder client to get its settings
 * and top-level probabilities from a server object.
 */
public interface DecoderServerRemote extends Server {

  /**
   * Returns the map of vocabulary items to possible parts of speech, contained
   * in the internal <code>ModelCollection</code> object.  This map
   * is needed when decoding.
   */
  public Map posMap() throws RemoteException;

  /**
   * A mapping from head labels to possible parent labels.
   * The keys of this map are {@link Symbol} obects, and the values are
   * {@link Set} objects containing {@link Symbol} objects.
   *
   * @return a mapping from head labels to possible parent labels
   */
  public Map headToParentMap() throws RemoteException;

  /**
   * A mapping from left subcat-prediction conditioning contexts (typically
   * parent and head nonterminal labels) to all possible left subcat frames.
   * The keys should represent the conditioning contexts that are the
   * last back-off level of the subcat-prediction models.
   * The keys of this map are {@link Event} objects, and the values are
   * {@link Set Set} objects containing {@link Subcat} objects.
   */
  public Map leftSubcatMap() throws RemoteException;

  /**
   * A mapping from right subcat-prediction conditioning contexts (typically
   * parent and head nonterminal labels) to all possible right subcat frames.
   * The keys should represent the conditioning contexts that are the
   * last back-off level of the subcat-prediction models.
   * The keys of this map are {@link Event} objects, and the values are
   * {@link Set Set} objects containing {@link Subcat} objects.
   */
  public Map rightSubcatMap() throws RemoteException;

  /**
   * A map of events from the last back-off level of the modifier
   * nonterminal&ndash;generation submodel to the set of possible futures
   * (typically, a future is a modifier label and its head word's part-of-speech
   * tag).  The keys are instances of {@link Event}, and the values are {@link
   * Set} instances containing {@link Event} objects.
   */
  public Map modNonterminalMap() throws RemoteException;

  /**
   * A map from unlexicalized parent-head-side triples to all possible
   * partially-lexicalized modifying nonterminals.  This map provides a simpler
   * mechanism for determining whether a given modifier is possible in the
   * current parent-head context than is provided by
   * {@link #modNonterminalMap()}.
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
  public Map simpleModNonterminalMap() throws RemoteException;

  /**
   * A counts table of unlexicalized nonterminals, i.e., a map of
   * unlexicalized nonterminals to their respective frequencies in the
   * training data.
   *
   * @return a counts table of unlexicalized nonterminals
   * @throws RemoteException
   */
  public CountsTable nonterminals() throws RemoteException;

  /**
   * A set of {@link Sexp} objects representing preterminals that were
   * pruned during training.
   * @return a set of {@link Sexp} objects representing preterminals that were
   * pruned during training.
   * @throws RemoteException
   *
   * @see Training#prune(Sexp)
   * @see Treebank#isPreterminal(Sexp)  
   */
  public Set prunedPreterms() throws RemoteException;

  /**
   * Returns the set of preterminals (<code>Sexp</code> objects) that were
   * punctuation elements that were &ldquo;raised away&rdquo; because they were
   * either at the beginning or end of a sentence.
   *
   * @see Training#raisePunctuation(Sexp)
   * @see Treebank#isPuncToRaise(Sexp)
   */
  public Set prunedPunctuation() throws RemoteException;

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
    throws RemoteException;

  /**
   * Replaces all unknown words in the specified sentence with
   * three-element lists. Each element is a symbol, and, in order, these three
   * symbols represent
   * <ul>
   * <li>the word itself,
   * <li>a word-feature vector, as determined by the
   * implementation of {@link WordFeatures#features(Symbol,boolean)} and
   * <li>{@link Constants#trueSym} if this word was never
   * observed during training or {@link Constants#falseSym} if it was
   * observed at least once during training.
   * </ul>
   *
   * @param sentence a list of symbols representing a sentence to be parsed
   */
  public SexpList convertUnknownWords(SexpList sentence) throws RemoteException;

  /**
   * The probability structure for the submodel that generates subcats
   * on the left-hand side of head constituents.  This structure is needed
   * to derive most-general contexts (using the last level of back-off)
   * in order to determine all possible left-side subcat frames for a given
   * context, using the {@link #leftSubcatMap()}.
   */
  public ProbabilityStructure leftSubcatProbStructure() throws RemoteException;

  /**
   * The probability structure for the submodel that generates subcats
   * on the right-hand side of head constituents.  This structure is needed
   * to derive most-general contexts (using the last level of back-off)
   * in order to determine all possible left-side subcat frames for a given
   * context, using the {@link #rightSubcatMap()}.
   */
  public ProbabilityStructure rightSubcatProbStructure() throws RemoteException;

  /**
   * The probability structure for the submodel that generates modifiers
   * of head constituents.  This structure is needed to derive most-general
   * contexts (using the last level of back-off) in order to determine all
   * possible modifiers for a given context, using the
   * {@link #modNonterminalMap()}.
   */
  public ProbabilityStructure modNonterminalProbStructure() throws RemoteException;

  /** Returns a test probability (for debugging purposes). */
  public double testProb() throws RemoteException;

  /**
   * Returns the prior probability of generating the nonterminal contained
   * in the specified <code>HeadEvent</code>.
   */
  public double logPrior(int id, TrainerEvent event) throws RemoteException;

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
  public double logProbHeadWithSubcats(int id, TrainerEvent event) throws RemoteException;

  /**
   * Returns the log of the probability of generating a head child in the
   * context of a particular parent (both the head to be generated and the
   * parent are contained in the specified {@link TrainerEvent} object).
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the log of the probability of generating the new head
   */
  public double logProbHead(int id, TrainerEvent event) throws RemoteException;

  /**
   * Returns the log of the probability of generating a left subcat in the
   * context of a particular parent and head (the subcat to be generated, the
   * head and parent are all contained in the specified {@link TrainerEvent}
   * object).
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the log of the probability of generating the left subcat
   */
  public double logProbLeftSubcat(int id, TrainerEvent event)
    throws RemoteException;

  /**
   * Returns the log of the probability of generating a right subcat in the
   * context of a particular parent and head (the subcat to be generated, the
   * head and parent are all contained in the specified {@link TrainerEvent}
   * object).
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the log of the probability of generating the right subcat
   */
  public double logProbRightSubcat(int id, TrainerEvent event)
    throws RemoteException;

  /**
   * Invokes {@link #logProbLeftSubcat(int, TrainerEvent)} or
   * {@link #logProbRightSubcat(int, TrainerEvent)} depending on the value
   * of <code>side</code>.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @param side either {@link Constants#LEFT} or {@link Constants#RIGHT}
   * @return the value of {@link #logProbLeftSubcat(int, TrainerEvent)} or
   * {@link #logProbRightSubcat(int, TrainerEvent)}
   * @throws RemoteException
   */
  public double logProbSubcat(int id, TrainerEvent event, boolean side)
    throws RemoteException;

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
  public double logProbTop(int id, TrainerEvent event) throws RemoteException;

  /**
   * Returns the log of the probability of generating a fully-lexicalized
   * modifying nonterminal given a particular parent, head and other
   * components of the syntactic context.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the log of the probability of generating the lexicalized modifying
   * nonterminal
   */
  public double logProbMod(int id, TrainerEvent event) throws RemoteException;

  /**
   * Returns the log of the probability of generating a partially-lexicalized
   * modifying nonterminal given a particular parent, head and other
   * components of the syntactic context.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the log of the probability of generating a partially-lexicalized
   * modifying nonterminal
   */
  public double logProbModNT(int id, TrainerEvent event) throws RemoteException;

  /**
   * Returns the log of the probability of generating a gap.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the log of the probability of generating a gap
   */
  public double logProbGap(int id, TrainerEvent event) throws RemoteException;

  // non-log prob methods

  /**
   * Returns the probability of generating a head child in the context of a
   * particular parent (both the head to be generated and the parent are
   * contained in the specified {@link TrainerEvent} object).
   * <p/>
   * <b>N.B.</b>: This method is unsupported when {@link
   * Settings#precomputeProbs} is <code>true</code>.
   *
   * @param id    the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   *              complete context needed to compute the requested probability
   * @return the probability of generating the new head
   */
  public double probHead(int id, TrainerEvent event) throws RemoteException;

  /**
   * Returns the probability of generating a fully-lexicalized
   * modifying nonterminal given a particular parent, head and other
   * components of the syntactic context.
   * <p/>
   * <b>N.B.</b>: This method is unsupported when {@link
   * Settings#precomputeProbs} is <code>true</code>.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the probability of generating the lexicalized modifying
   * nonterminal
   */
  public double probMod(int id, TrainerEvent event) throws RemoteException;

  /**
   * Returns the probability of generating a left subcat in the
   * context of a particular parent and head (the subcat to be generated, the
   * head and parent are all contained in the specified {@link TrainerEvent}
   * object).
   * <p/>
   * <b>N.B.</b>: This method is unsupported when {@link
   * Settings#precomputeProbs} is <code>true</code>.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the probability of generating the left subcat
   */
  public double probLeftSubcat(int id, TrainerEvent event)
    throws RemoteException;

  /**
   * Returns the probability of generating a right subcat in the
   * context of a particular parent and head (the subcat to be generated, the
   * head and parent are all contained in the specified {@link TrainerEvent}
   * object).
   * <p/>
   * <b>N.B.</b>: This method is unsupported when {@link
   * Settings#precomputeProbs} is <code>true</code>.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the probability of generating the right subcat
   */
  public double probRightSubcat(int id, TrainerEvent event)
    throws RemoteException;

  /**
   * Returns the probability of generating the head nonterminal
   * of an entire sentence.
   * <p/>
   * <b>N.B.</b>: This method is unsupported when {@link
   * Settings#precomputeProbs} is <code>true</code>.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the probability of generating the head nonterminal
   * of an entire sentence
   */
  public double probTop(int id, TrainerEvent event) throws RemoteException;
}
