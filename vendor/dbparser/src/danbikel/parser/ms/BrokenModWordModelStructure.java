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
 * Provides the complete back-off structure for the submodel that generates
 * the head words of modifying nonterminals.  This class is just like
 * {@link ModWordModelStructure2} but is &ldquo;broken&rdquo; in the
 * sense that it includes side information when generating histories
 * for the last back-off level, as indicated by Collins&rsquo; thesis,
 * but as was not implemented by Collins in his actual thesis parser,
 * which collapsed <i>all</i> words when computing
 * <i>p</i>(<i>w</i>&nbsp;|&nbsp;<i>t</i>).
 * <p/>
 * The specific back-off structure provided by this class is as follows.
 * If the parent <i>P</i> is <i>not</i> a base NP (<tt>NPB</tt>), then
 * the back-off structure provided by this class is
 * <ul>
 * <li><i>p</i>(<i>w<sub>i</sub></i> |
 *     <i>&gamma;</i>(<i>M</i>(<i>t</i>)<i><sub>i</sub></i>),
 *     <i>P</i>, <i>&gamma;</i>(<i>H</i>), <i>w<sub>h</sub></i>,
 *     <i>t<sub>h</sub></i>, <tt>vi</tt>,
 *     <i>&delta;</i>(<i>M<sub>i-1</sub></i>), <i>subcat<sub>side</sub></i>,
 *     <i>side</i>)
 * <li><i>p</i>(<i>w<sub>i</sub></i> |
 *     <i>&gamma;</i>(<i>M</i>(<i>t</i>)<i><sub>i</sub></i>),
 *     <i>P</i>, <i>&gamma;</i>(<i>H</i>), <i>t<sub>h</sub></i>, <tt>vi</tt>,
 *     <i>&delta;</i>(<i>M<sub>i-1</sub></i>), <i>subcat<sub>side</sub></i>,
 *     <i>side</i>)
 * <li><i>p</i>(<i>w<sub>i</sub></i> | <i>t<sub>i</sub></i>, <i>side</i>)
 * </ul>
 * If the parent <i>P</i> <i>is</i> a base NP (<tt>NPB</tt>), then
 * the back-off structure provided by this class is
 * <ul>
 * <li><i>p</i>(<i>w<sub>i</sub></i> |
 *     <i>&gamma;</i>(<i>M</i>(<i>t</i>)<i><sub>i</sub></i>), <i>P</i>,
 *     <i>&delta;</i>(<i>M(w,t)<sub>i-1</sub></i>), <i>subcat<sub>side</sub></i>,
 *     <i>side</i>)
 * <li><i>p</i>(<i>w<sub>i</sub></i> |
 *     <i>&gamma;</i>(<i>M</i>(<i>t</i>)<i><sub>i</sub></i>), <i>P</i>,
 *     <i>&delta;</i>(<i>M(t)<sub>i-1</sub></i>), <i>subcat<sub>side</sub></i>,
 *     <i>side</i>)
 * <li><i>p</i>(<i>w<sub>i</sub></i> | <i>t<sub>i</sub></i>, <i>side</i>)
 * </ul>
 * <p/>
 * Please consult one of the following two references for an explanation of
 * the notation used above.
 * <ul>
 * <li>Daniel M. Bikel. 2004.
 * <a href="http://www.cis.upenn.edu/~dbikel/papers/collins-intricacies.pdf">
 * Intricacies of Collins&rsquo; Parsing Model</a>.
 * <i><a href="http://mitpress.mit.edu/catalog/item/default.asp?ttype=4&tid=10">
 * Computational Linguistics</i></a>, <b>30</b>(4). pp. 479-511.
 * <li>Daniel Martin Bikel. 2004.
 * <a href="http://www.cis.upenn.edu/~dbikel/papers/thesis.pdf">On the
 * Parameter Space of Generative Lexicalized Statistical Parsing Models</a>.
 * Ph.D. thesis, University of Pennsylvania.
 * </ul>
 */
public class BrokenModWordModelStructure extends ProbabilityStructure {
  // data members
  private Symbol startSym = Language.training().startSym();
  private Word startWord = Language.training().startWord();
  private Symbol topSym = Language.training().topSym();

  /** Constructs a new instance. */
  public BrokenModWordModelStructure() {
    super();
  }

  /** Returns 10. */
  public int maxEventComponents() { return 10; }
  /** Returns 3. */
  public int numLevels() { return 3; }

  /**
   * Returns the history event corresponding to the specified back-off level.
   * If the parent <i>P</i> is <i>not</i> a base NP (<tt>NPB</tt>), then
   * the back-off structure provided by this class is
   * <ul>
   * <li><i>p</i>(<i>w<sub>i</sub></i> |
   *     <i>&gamma;</i>(<i>M</i>(<i>t</i>)<i><sub>i</sub></i>),
   *     <i>P</i>, <i>&gamma;</i>(<i>H</i>), <i>w<sub>h</sub></i>,
   *     <i>t<sub>h</sub></i>, <tt>vi</tt>,
   *     <i>&delta;</i>(<i>M<sub>i-1</sub></i>), <i>subcat<sub>side</sub></i>,
   *     <i>side</i>)
   * <li><i>p</i>(<i>w<sub>i</sub></i> |
   *     <i>&gamma;</i>(<i>M</i>(<i>t</i>)<i><sub>i</sub></i>),
   *     <i>P</i>, <i>&gamma;</i>(<i>H</i>), <i>t<sub>h</sub></i>, <tt>vi</tt>,
   *     <i>&delta;</i>(<i>M<sub>i-1</sub></i>), <i>subcat<sub>side</sub></i>,
   *     <i>side</i>)
   * <li><i>p</i>(<i>w<sub>i</sub></i> | <i>t<sub>i</sub></i>, <i>side</i>)
   * </ul>
   * If the parent <i>P</i> <i>is</i> a base NP (<tt>NPB</tt>), then
   * the back-off structure provided by this class is
   * <ul>
   * <li><i>p</i>(<i>w<sub>i</sub></i> |
   *     <i>&gamma;</i>(<i>M</i>(<i>t</i>)<i><sub>i</sub></i>), <i>P</i>,
   *     <i>&delta;</i>(<i>M(w,t)<sub>i-1</sub></i>), <i>subcat<sub>side</sub></i>,
   *     <i>side</i>)
   * <li><i>p</i>(<i>w<sub>i</sub></i> |
   *     <i>&gamma;</i>(<i>M</i>(<i>t</i>)<i><sub>i</sub></i>), <i>P</i>,
   *     <i>&delta;</i>(<i>M(t)<sub>i-1</sub></i>), <i>subcat<sub>side</sub></i>,
   *     <i>side</i>)
   * <li><i>p</i>(<i>w<sub>i</sub></i> | <i>t<sub>i</sub></i>, <i>side</i>)
   * </ul>
   *
   * @param trainerEvent the maximal-context event from which to derive
   * the history contexts used by the probability structure provided by
   * this class
   * @param backOffLevel the back-off level for which to return a history
   * context
   * @return the history context for the specified back-off level
   */
  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    ModifierEvent modEvent = (ModifierEvent)trainerEvent;

    if (Language.treebank().isBaseNP(modEvent.parent()))
      return getBaseNPHistory(modEvent, backOffLevel);

    Symbol side = Constants.sideToSym(modEvent.side());

    MutableEvent hist = historiesWithSubcats[backOffLevel];
    hist.clear();
    Symbol verbInterveningSym =
      Constants.booleanToSym(modEvent.verbIntervening());
    Symbol mappedPrevModSym =
      NTMapper.map(modEvent.previousMods().symbolAt(0));
    Symbol parent =
      Language.training().removeArgAugmentation(modEvent.parent());

    switch (backOffLevel) {
    case 0:
      // for p(w_i | M(t)_i, P, H, w, t, verbIntervening, map(M_i-1),  subcat,
      //             side)
      hist.add(0, Language.training().removeGapAugmentation(modEvent.modifier()));
      hist.add(0, modEvent.modHeadWord().tag());
      hist.add(0, parent);
      hist.add(0, Language.training().removeGapAugmentation(modEvent.head()));
      hist.add(0, modEvent.headWord().word());
      hist.add(0, modEvent.headWord().tag());
      hist.add(0, verbInterveningSym);
      hist.add(0, mappedPrevModSym);
      hist.add(1, modEvent.subcat());
      hist.add(0, side);
      break;
    case 1:
      // for p(w_i | M(t)_i, P, H, t, verbIntervening, map(M_i-1), subcat, side)
      hist.add(0, Language.training().removeGapAugmentation(modEvent.modifier()));
      hist.add(0, modEvent.modHeadWord().tag());
      hist.add(0, parent);
      hist.add(0, Language.training().removeGapAugmentation(modEvent.head()));
      hist.add(0, modEvent.headWord().tag());
      hist.add(0, verbInterveningSym);
      hist.add(0, mappedPrevModSym);
      hist.add(1, modEvent.subcat());
      hist.add(0, side);
      break;
    case 2:
      // for p(w_i | t_i, side)
      hist = histories[backOffLevel]; // efficiency hack: don't need subcat
      hist.clear();
      hist.add(0, modEvent.modHeadWord().tag());
      hist.add(0, side);
      break;
    }
    return hist;
  }

  private Event getBaseNPHistory(ModifierEvent modEvent, int backOffLevel) {
    MutableEvent hist = histories[backOffLevel];

    Symbol side = Constants.sideToSym(modEvent.side());

    Symbol prevModLabel =
      (modEvent.previousMods().get(0) == startSym ?
       modEvent.head() : modEvent.previousMods().symbolAt(0));
    Word prevModWord =
      (modEvent.previousWords().getWord(0).equals(startWord) ?
       modEvent.headWord() : modEvent.previousWords().getWord(0));
    hist.clear();
    switch (backOffLevel) {
    case 0:
      // for p(w_i | M(t)_i, P, M(w,t)_i-1, side)
      hist.add(Language.training().removeGapAugmentation(modEvent.modifier()));
      hist.add(modEvent.modHeadWord().tag());
      hist.add(Language.training().removeGapAugmentation(modEvent.parent()));
      hist.add(prevModLabel);
      hist.add(prevModWord.word());
      hist.add(prevModWord.tag());
      hist.add(side);
      break;
      /*
    case 1:
      // for p(w_i | M(t)_i, P, M(t)_i-1, side)
      hist.add(Language.training().removeGapAugmentation(modEvent.modifier()));
      hist.add(modEvent.modHeadWord().tag());
      hist.add(Language.training().removeGapAugmentation(modEvent.parent()));
      hist.add(prevModLabel);
      hist.add(prevModWord.tag());
      hist.add(side);
      break;
      */
    case 1:
      // for p(w_i | M(t)_i, P, M_i-1, side)
      hist.add(Language.training().removeGapAugmentation(modEvent.modifier()));
      hist.add(modEvent.modHeadWord().tag());
      hist.add(Language.training().removeGapAugmentation(modEvent.parent()));
      hist.add(prevModLabel);
      hist.add(side);
      break;
    case 2:
      // for p(w_i | t_i, side)
      hist.add(modEvent.modHeadWord().tag());
      hist.add(side);
      break;
    }
    return hist;
  }

  /**
   * Returns an event whose sole component is the word being generated as the
   * head of some modifier nonterminal.
   *
   * @param trainerEvent the maximal-context event for which to get a future
   * @param backOffLevel the level of back-off for which a probability is being
   *                     computed
   * @return an event whose sole component is the word being generated as the
   *         head of some modifier nonterminal
   */
  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    MutableEvent future = futures[backOffLevel];
    future.clear();
    Word modHead = ((ModifierEvent)trainerEvent).modHeadWord();
    Symbol word =
      modHead.features() != null ? modHead.features() : modHead.word();
    future.add(word);
    return future;
  }

  /**
   * Returns <tt>true</tt>, indicating that the {@link Model} that owns an
   * instance of this class ought to call its {@link Model#cleanup()} method at
   * the end of execution of its {@link Model#deriveCounts deriveCounts}
   * method.
   *
   * @return <tt>true</tt>
   */
  public boolean doCleanup() { return true; }

  /**
   * In order to gather statistics for words that appear as the head of the
   * entire sentence when estimating <i>p&Hat;</i>(<i>w</i>&nbsp;|&nbsp;<i>t</i>),
   * the trainer &ldquo;fakes&rdquo; a modifier event, as though the root node
   * of the observed tree was seen to modify the magical <tt>+TOP+</tt> node.
   * For back-off levels 0 and 1, we will never use the derived counts whose
   * history contexts contain <tt>+TOP+</tt>. This method allows for the removal
   * of these &ldquo;unnecessary&rdquo; counts, which will never be used when
   * decoding.
   *
   * @param backOffLevel the back-off level of the history context being tested
   *                     for removal
   * @param history      the history context being tested for removal
   */
  public boolean removeHistory(int backOffLevel, Event history) {
    // this method assumes the parent component of histories for
    // back-off levels 0 and 1 will be at index 2.  IF THIS CHANGES,
    // this method will need to change accordingly.
    switch (backOffLevel) {
    case 0:
      return history.get(0, 2) == topSym;
    case 1:
      return history.get(0, 2) == topSym;
    case 2:
      return false;
    }
    return false;
  }

  /** Returns a copy of this object. */
  public ProbabilityStructure copy() {
    ProbabilityStructure psCopy = new BrokenModWordModelStructure();
    psCopy.setAdditionalData(this.additionalData);
    return psCopy;
  }
}
