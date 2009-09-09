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

public class NonterminalPriorModelStructure1 extends ProbabilityStructure {
  public NonterminalPriorModelStructure1() {
    super();
  }

  public int maxEventComponents() { return 3; }
  public int numLevels() { return 2; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    PriorEvent priorEvent = (PriorEvent)trainerEvent;

    MutableEvent history = histories[backOffLevel];
    history.clear();
    switch (backOffLevel) {
    case 0:
      // for p(label | w,t)
      Word headWord = priorEvent.headWord();
      Symbol word =
	headWord.features() != null ? headWord.features() : headWord.word();
      history.add(word);
      history.add(headWord.tag());
      break;
    case 1:
      // for p(label | t)
      history.add(priorEvent.headWord().tag());
      break;
    }
    return history;
  }
  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    MutableEvent future = futures[backOffLevel];
    future.clear();
    future.add(((PriorEvent)trainerEvent).label());
    return future;
  }

  public ProbabilityStructure copy() {
    return new NonterminalPriorModelStructure1();
  }
}
