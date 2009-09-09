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

public class BrokenTopLexModelStructure extends ProbabilityStructure {
  public BrokenTopLexModelStructure() {
    super();
  }

  public int maxEventComponents() { return 3; }
  public int numLevels() { return 2; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    HeadEvent headEvent = (HeadEvent)trainerEvent;
    MutableEvent history = histories[backOffLevel];
    history.clear();
    switch (backOffLevel) {
    case 0:
      // for p(w | t, H, +TOP+)
      history.add(headEvent.headWord().tag());
      history.add(headEvent.head());
      history.add(headEvent.parent());
      break;
    /*
    case 1:
      // for p(w | t, H)
      history.add(headEvent.headWord().tag());
      history.add(Language.treebank.getCanonical(headEvent.head()));
      break;
    */
    case 1:
      // for p(w | t, +TOP+)
      history.add(headEvent.headWord().tag());
      history.add(headEvent.parent());
    }
    return history;
  }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    HeadEvent headEvent = (HeadEvent)trainerEvent;
    MutableEvent future = futures[backOffLevel];
    future.clear();
    // for p(w | ...)
    Word headWord = ((HeadEvent)trainerEvent).headWord();
    Symbol word =
      headWord.features() != null ? headWord.features() : headWord.word();
    future.add(word);
    return future;
  }

  public ProbabilityStructure copy() {
    return new BrokenTopLexModelStructure();
  }
}
