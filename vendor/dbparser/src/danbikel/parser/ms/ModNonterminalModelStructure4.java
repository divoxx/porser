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
 * Representation of the complete back-off structure of the generation model
 * for modifying nonterminals/part-of-speech tags (the modifying nonterminals
 * are partially lexicalized with the parts of speech of their respective
 * head words).
 * <p>
 * <b>It is a horrendous bug that all of these <code>ProbabilityStructure</code>
 * classes do not copy various lists from the <code>TrainerEvent</code> objects
 * before removing gap augmentations from their elements.</b>
 * <p>
 */
public class ModNonterminalModelStructure4 extends ProbabilityStructure {
  // data members
  private static Symbol startSym = Language.training().startSym();
  private static Word startWord = Language.training().startWord();
  private Symbol topSym = Language.training().topSym();

  public ModNonterminalModelStructure4() {
    super();
  }

  public int maxEventComponents() { return 8; }
  public int numLevels() { return 4; }

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
      // for p(M(t)_i | P, H, w, t, verbIntervening, map(M_i-1), subcat, side)
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
      // for p(M(t)_i | P, H, t, verbIntervening, map(M_i-1), subcat, side)
      hist.add(0, parent);
      hist.add(0, Language.training().removeGapAugmentation(modEvent.head()));
      hist.add(0, modEvent.headWord().tag());
      hist.add(0, verbInterveningSym);
      hist.add(0, mappedPrevModSym);
      hist.add(1, modEvent.subcat());
      hist.add(0, side);
      break;
    case 2:
      // for p(M(t)_i | P, H, verbIntervening, map(M_i-1), subcat, side)
      hist.add(0, parent);
      hist.add(0, Language.training().removeGapAugmentation(modEvent.head()));
      hist.add(0, verbInterveningSym);
      hist.add(0, mappedPrevModSym);
      hist.add(1, modEvent.subcat());
      hist.add(0, side);
      break;
    case 3:
      // for p(M(t)_i | P, subcat)
      hist.add(0, parent);
      hist.add(1, modEvent.subcat());
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
      // for p(M(t)_i | P, M(w,t)_i-1, side)
      hist.add(Language.training().removeGapAugmentation(modEvent.parent()));
      hist.add(prevModLabel);
      hist.add(prevModWord.word());
      hist.add(prevModWord.tag());
      hist.add(side);
      break;
    case 1:
      // for p(M(t)_i | P, M(t)_i-1, side)
      hist.add(Language.training().removeGapAugmentation(modEvent.parent()));
      hist.add(prevModLabel);
      hist.add(prevModWord.tag());
      hist.add(side);
      break;
    case 2:
      // for p(M(t)_i | P, M_i-1, side)
      hist.add(Language.training().removeGapAugmentation(modEvent.parent()));
      hist.add(prevModLabel);
      hist.add(side);
      break;
    case 3:
      // for p(M(t)_i | P)
      hist.add(Language.training().removeGapAugmentation(modEvent.parent()));
      break;
    }
    return hist;
  }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    ModifierEvent modEvent = (ModifierEvent)trainerEvent;
    MutableEvent future = futures[backOffLevel];
    future.clear();
    future.add(modEvent.modifier());
    future.add(modEvent.modHeadWord().tag());
    return future;
  }

  public boolean doCleanup() { return true; }

  /**
   * In order to gather statistics for words that appear as the head of
   * the entire sentence when estimating p(w | t), the trainer "fakes" a
   * modifier event, as though the root node of the observed tree was seen
   * to modify the magical +TOP+ node.  For back-off levels 0 and 1, we
   * will never use the derived counts whose history contexts contain +TOP+.
   * This method allows for the removal of these "unnecessary" counts,
   * which will never be used when decoding.
   */
  public boolean removeHistory(int backOffLevel, Event history) {
    return history.get(0, 0) == topSym;
  }

  public ProbabilityStructure copy() {
    return new ModNonterminalModelStructure4();
  }
}
