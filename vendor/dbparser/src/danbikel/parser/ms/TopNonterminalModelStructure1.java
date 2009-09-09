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
public class TopNonterminalModelStructure1 extends ProbabilityStructure {
  public TopNonterminalModelStructure1() {
    super();
  }

  public int maxEventComponents() { return 2; }
  public int numLevels() { return 1; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    if (backOffLevel != 0)
      throw new IllegalArgumentException();
    MutableEvent history = histories[backOffLevel];
    history.clear();
    // for p(H,t | +TOP+)
    history.add(trainerEvent.parent());
    return history;
  }
  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    MutableEvent future = futures[backOffLevel];
    future.clear();
    // for p(H,t | ...)
    future.add(((HeadEvent)trainerEvent).head());
    future.add(((HeadEvent)trainerEvent).headWord().tag());
    return future;
  }

  public ProbabilityStructure copy() {
    return new TopNonterminalModelStructure1();
  }
}
