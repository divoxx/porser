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
    package danbikel.parser.ms;

import danbikel.parser.*;
import danbikel.lisp.*;

/**
 * Provides the complete back-off structure for the submodel that generates the
 * marginal probabilities of lexical items and their parts of speech (loosely
 * but inaccurately called &ldquo;lexical priors&rdquo;).  This model is
 * &ldquo;broken&rdquo; in that it is just like {@link LexPriorModelStructure1}
 * but does not override the {@link #lambdaFudge(int)} and {@link
 * #lambdaFudgeTerm(int)} methods as would be required to replicate the model
 * Collins implemented for his thesis.
 */
public class BrokenLexPriorModelStructure extends ProbabilityStructure {
  /**
   * Constructs a new instance.
   */
  public BrokenLexPriorModelStructure() {
    super();
  }

  /** Returns 2. */
  public int maxEventComponents() { return 2; }
  /**
   * As this model simulates unconditional probabilities using
   * relative-frequency estimation, it has only one back-off level that returns
   * a dummy object.
   *
   * @return the integer 1
   */
  public int numLevels() { return 1; }

  /*
  public double lambdaFudge(int backOffLevel) { return 0.0; }
  public double lambdaFudgeTerm(int backOffLevel) { return 1.0; }
  */

  /**
   * As this model simulates unconditional probabilities using
   * relative-frequency estimation, this method returns a history
   * whose sole component is a dummy object that is the same regardless
   * of the &ldquo;future&rdquo; being estimated.
   * @param trainerEvent the maximal context event that is ignored
   * by this method
   * @param backOffLevel the back-off level that is ignored by this method
   * @return a history whose sole component is a dummy object
   *
   * @see danbikel.parser.PriorEvent#history()
   */
  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    PriorEvent priorEvent = (PriorEvent)trainerEvent;
    MutableEvent history = histories[backOffLevel];
    history.clear();
    history.add(priorEvent.history());
    return history;
  }

  /**
   * Returns an event whose two components are the word and part-of-speech
   * for which a marginal probability is being computed.
   * @param trainerEvent the maximal-context event from which to construct
   * the event containing the word/part-of-speech pair whose marginal is
   * being estimated
   * @param backOffLevel back-off level (ignored)
   * @return an event whose two components are the word and part-of-speech
   * for which a marginal probability is being computed
   */
  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    MutableEvent future = futures[backOffLevel];
    future.clear();
    PriorEvent priorEvent = (PriorEvent)trainerEvent;
    Word headWord = priorEvent.headWord();
    Symbol word =
      headWord.features() != null ? headWord.features() : headWord.word();
    future.add(word);
    future.add(headWord.tag());
    return future;
  }

  /** Returns a copy of this instance. */
  public ProbabilityStructure copy() {
    return new BrokenLexPriorModelStructure();
  }
}
