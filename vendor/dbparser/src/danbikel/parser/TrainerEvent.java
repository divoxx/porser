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
import java.io.Serializable;

/**
 * An interface to allow iteration over various kinds of events used by the
 * class {@link Trainer}.
 */
public interface TrainerEvent extends Serializable {
  /**
   * Returns the head word object associated with an event, or <code>null</code>
   * if this <code>TrainerEvent</code> has no such object.
   */
  public Word headWord();

  /**
   * Sets the head word of this event, or does nothing if this
   * <code>TrainerEvent</code> has no such object.
   *
   * @param word the word to set as the head word of this event
   */
  public void setHeadWord(Word word);

  /**
   * Returns the modifier head word object associated with an event,
   * or <code>null</code> if this <code>TrainerEvent</code> has no such object.
   */
  public Word modHeadWord();

  /**
   * Returns a deep copy of this event of the same run-time type.
   */
  public TrainerEvent copy();

  /**
   * Returns a shallow copy of this event of the same run-time type.
   */
  public TrainerEvent shallowCopy();

  /**
   * Returns the parent symbol of this event, or <code>null</code> if
   * this event has no such object.
   */
  public Symbol parent();

  /**
   * Returns the side of a modifier event.
   *
   * @exception UnsupportedOperationException if this is not a modifier event
   */
  public boolean side();
}
