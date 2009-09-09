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
 * Representation of the complete back-off structure of the subcat-generation
 * model for either side of the head child.
 */
public class GapModelStructure1 extends ProbabilityStructure {
  public GapModelStructure1() { super(); }

  public int maxEventComponents() { return 4; }
  public int numLevels() { return 3; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    GapEvent gapEvent = (GapEvent)trainerEvent;

    Sexp noGapHead =
      Language.training().removeGapAugmentation(gapEvent.head());
    Sexp noGapParent =
      Language.training().removeGapAugmentation(gapEvent.parent());

    MutableEvent history = histories[backOffLevel];
    history.clear();
    switch (backOffLevel) {
    case 0:
      // for p(Gap | H, P, w, t)
      history.add(noGapHead);
      history.add(noGapParent);
      history.add(gapEvent.headWord().word());
      history.add(gapEvent.headWord().tag());
      break;
    case 1:
      // for p(Gap | H, P, t)
      history.add(noGapHead);
      history.add(noGapParent);
      history.add(gapEvent.headWord().tag());
      break;
    case 2:
      // for p(Gap | H, P)
      history.add(noGapHead);
      history.add(noGapParent);
      break;
    }
    return history;
  }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    MutableEvent future = futures[backOffLevel];
    future.clear();
    future.add(((GapEvent)trainerEvent).direction());
    return future;
  }

  public ProbabilityStructure copy() {
    return new GapModelStructure1();
  }
}
