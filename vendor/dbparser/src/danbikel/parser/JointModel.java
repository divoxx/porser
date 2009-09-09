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

/**
 * Provides a mechanism for grouping related <code>Model</code> objects in order
 * to estimate the probability of some joint event. A probability estimate
 * delivered by this class is the product of all the individually-smoothed
 * probability estimates delivered by this class and all its contained
 * <code>Model</code> objects.  Crucially, this means that this class and all
 * contained <code>Model</code> objects must be <i>coherent</i>, in the sense
 * that all internal estimates of the elements of a joint event will be derived
 * from the same <code>TrainerEvent</code> object.  Typically, this class will
 * provide the means to estimate a joint event via the chain rule, where it is
 * desirable that all the separate estimates comprising the product be
 * independently smoothed.
 * <p/>
 * Note that a joint event may be estimated via a standard <code>Model</code>
 * instance, by simply having the {@link ProbabilityStructure#getFuture} method
 * return an event that is a collection of elements (for example, a nonterminal
 * symbol and a part-of-speech tag symbol).  The crucial feature enabled by this
 * class is to have the probability estimates for each element of a joint event
 * to be smoothed individually.
 * <p/>
 * <b>Implementation note</b>: An instance of this class (itself an instance of
 * <code>Model</code>) will contain an internal collection of other
 * <code>Model</code> objects whose probability structures are determined via
 * the {@link ProbabilityStructure#jointModel()} method.  The internal
 * <code>Model</code> objects used by this class can be accessed via the {@link
 * #getModel(int)} method.  Note that any of these internal <code>Model</code>
 * instances may actually also be <code>JointModel</code> instances (although
 * for efficiency reasons, such a structure should be avoided in general, if
 * possible).
 *
 * @see ProbabilityStructure#jointModel()
 */
public class JointModel extends Model {
  private final static String className = Model.class.getName();

  protected int numOtherModels;
  protected Model[] otherModels;


  public JointModel(ProbabilityStructure structure) {
    super(structure);
    ProbabilityStructure[] structures = structure.jointModel();
    numOtherModels = structures.length;
    otherModels = new Model[numOtherModels];
    for (int i = 0; i < numOtherModels; i++) {
      otherModels[i] = structures[i].newModel();
    }
  }

  /**
   * Canonicalizes the objects of this <code>Model</code>, as well as all
   * internal <code>Model</code> instances.
   *
   * @param map the map to be used for canonicalization
   */
  public void canonicalize(FlexibleMap map) {
    super.canonicalize(map);
    for (int i = 0; i < numOtherModels; i++) {
      otherModels[i].canonicalize(map);
    }
  }

  /**
   * Sets the {@link #canonicalEvents} member of this object to be the
   * specified {@link FlexibleMap}, as well as setting the same member
   * of all internal {@link Model} objects.
   *
   * @param canonical the reflexive map of canonical {@link Event}
   * objects
   */ 
  public void setCanonicalEvents(FlexibleMap canonical) {
    super.setCanonicalEvents(canonical);
    for (int i = 0; i < numOtherModels; i++) {
      otherModels[i].setCanonicalEvents(canonical);
    }
  }

  /**
   * Derives counts for this <code>Model</code>, as well as for all
   * internal <code>Model</code> instances.
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
    deriveCounts(trainerCounts, filter, threshold, canonical, true);
  }

  /**
   * Derives counts for this <code>Model</code> and optionally for
   * all internal <code>Model</code> instances.
   *
   * @param trainerCounts a map from {@link TrainerEvent} objects to
   * their counts (as <code>double</code>s) from which to derive counts
   * @param filter used to filter out <code>TrainerEvent</code> objects
   * whose derived counts should not be derived for this model
   * @param threshold a (currently unused) count cut-off threshold
   * @param canonical a reflexive map used to canonicalize objects
   * created when deriving counts
   * @param deriveOtherModelCounts indicates whether to derive
   * counts for the internal <code>Model</code> instances contained
   * in this joint model
   */
  public void deriveCounts(CountsTable trainerCounts, Filter filter,
			    double threshold, FlexibleMap canonical,
			    boolean deriveOtherModelCounts) {
    super.deriveCounts(trainerCounts, filter, threshold, canonical, false);
    if (deriveOtherModelCounts) {
      for (int i = 0; i < numOtherModels; i++) {
	otherModels[i].deriveCounts(trainerCounts, filter, threshold,
				    canonical);
      }
    }
  }

  /**
   * Precomputes probabilities and smoothing values for this <code>Model</code>
   * and for all internal <code>Model</code> instances.
   */
  public void precomputeProbs() {
    super.precomputeProbs();
    for (int i = 0; i < numOtherModels; i++) {
      otherModels[i].precomputeProbs();
    }
  }

  /**
   * Estimates a conditional probability in log-space from the specified
   * maximal-context trainer event.  The estimate will use sub-contexts of
   * the specified trainer event.  The estimate returned will be the sum
   * of the log probabilities returned by this and all contained
   * <code>Model</code> instances.
   *
   * @param id the id of the decoding client calling this method
   * @param event the maximal-context event from which to produce a
   * conditional probability estimate of some element(s) of that
   * context
   * @return a log probability estimate of some joint event
   */
  public double estimateLogProb(int id, TrainerEvent event) {
    double logProb = super.estimateLogProb(id, event);
    if (logProb <= Constants.logOfZero)
      return Constants.logOfZero;
    for (int i = 0; i < numOtherModels; i++) {
      double otherModelLogProb = otherModels[i].estimateLogProb(id, event);
      if (otherModelLogProb <= Constants.logOfZero)
	return Constants.logOfZero;
      else
	logProb += otherModelLogProb;
    }
    return logProb;
  }

  /**
   * Estimates the log-probability of the specified event under this {@link
   * Model} without adding the log-probabilities of the internal {@link Model}
   * objects.
   *
   * @param id    the id of the caller requesting the log-probability
   * @param event the event containing the history context and future from which
   *              to estimate a conditional log-probability
   * @return the log-probability of the specified event under this {@link Model}
   *         without adding the log-probabilities of the internal {@link Model}
   *         objects
   */
  public double estimateNonJointLogProb(int id, TrainerEvent event) {
    double logProb = super.estimateLogProb(id, event);
    if (logProb <= Constants.logOfZero)
      return Constants.logOfZero;
    return logProb;
  }

  /**
   * Estimates a conditional probability from the specified
   * maximal-context trainer event.  The estimate will use sub-contexts of
   * the specified trainer event.  The estimate returned will be the product
   * of the probabilities returned by this and all contained
   * <code>Model</code> instances.
   *
   * @param id the id of the decoding client calling this method
   * @param event the maximal-context event from which to derive a
   * conditional probability estimate
   * @return a conditional probability estimate of some joint event given
   * some history, where both the joint event and history context are
   * derived from the specified maximal-context event
   */
  public double estimateProb(int id, TrainerEvent event) {
    double prob = super.estimateProb(id, event);
    if (prob <= Constants.probImpossible)
      return Constants.probImpossible;
    for (int i = 0; i < numOtherModels; i++) {
      double otherModelProb = otherModels[i].estimateProb(id, event);
      if (otherModelProb <= Constants.probImpossible)
	return Constants.probImpossible;
      else
	prob *= otherModelProb;
    }
    return prob;
  }

  /**
   * Estimates the probability of the specified event under this {@link Model}
   * without multiplying the probabilities of the internal {@link Model}
   * objects.
   *
   * @param id    the id of the caller requesting the probability
   * @param event the event containing the history context and future from which
   *              to estimate a conditional probability
   * @return the probability of the specified event under this {@link Model}
   *         without multiplying the probabilities of the internal {@link Model}
   *         objects
   */
  public double estimateNonJointProb(int id, TrainerEvent event) {
    double prob = super.estimateProb(id, event);
    if (prob <= Constants.probImpossible)
      return Constants.probImpossible;
    return prob;
  }

  /**
   * Returns the number of models used to produce a joint probability estimate,
   * including this <code>Model</code> instance.
   *
   * @return the number of models used to produce a joint probability estimate,
   * including this <code>Model</code> instance.
   */
  public int numModels() { return numOtherModels + 1; }

  /**
   * Returns this or any of the internal <code>Model</code> instances used
   * to produce joint probability estimates.
   *
   * @param idx the index of the <code>Model</code> to return
   * @return the <code>Model</code> at the specified index; if the specified
   * index is <tt>0</tt>, then this <code>Model</code> instance is returned;
   * otherwise, one of the internal <code>Model</code> instances is returned
   *
   * @throws ArrayIndexOutOfBoundsException if the specified index is
   * greater than <tt>{@link #numModels()} - 1</tt>
   */
  public Model getModel(int idx) {
    return idx == 0 ? this : otherModels[idx - 1];
  }

  /**
   * Returns the primary probability structure of this joint model, which is
   * that used by this <code>Model</code> instance (as opposed to one of the
   * internal <code>Model</code> instances).
   *
   * @return the primary probability structure of this joint model, which is
   * that used by this <code>Model</code> instance
   */
  public ProbabilityStructure getProbStructure() {
    return super.getProbStructure();
  }

  /**
   * Returns a probability structure of this joint model, which is either
   * that used by this <code>Model</code> instance, or a structure used by
   * one of the internal <code>Model</code> instances.
   *
   * @param idx the index of the probability structure to return
   * @return if the specified index is <tt>0</tt>, then the probability
   * structure of this <code>Model</code> instance is returned; otherwise,
   * the probability structure of one of the internal <code>Model</code>
   * instances is returned
   *
   * @throws ArrayIndexOutOfBoundsException if the specified index is
   * greater than <tt>{@link #numModels()} - 1</tt>
   */
  public ProbabilityStructure getProbStructure(int idx) {
    return (idx == 0 ?
	    super.getProbStructure() : otherModels[idx - 1].getProbStructure());
  }

  /**
   * Returns a string representing the cache statistics for this and all
   * other, internal <code>Model</code> objects.
   *
   * @return a string representing the cache statistics for this and all
   * other, internal <code>Model</code> objects.
   */
  public String getCacheStats() {
    StringBuffer sb = new StringBuffer(300 * numModels());
    sb.append(super.getCacheStats());
    for (int i = 0; i < numOtherModels; i++)
      sb.append(otherModels[i].getCacheStats());
    return sb.toString();
  }
}
