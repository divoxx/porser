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
import danbikel.lisp.*;

/**
 * Methods used for the construction of prior states in the Markov
 * process of creating modifier nonterminals.  Currently, prior state
 * is stored in a previous modifier list (a <code>SexpList</code>
 * object) and a previous modifier head-word list (a
 * <code>WordList</code> object), so the methods here are used to
 * "shift" a new modifier to the head of these lists, losing the last
 * element of the list (which was the least-recently-generated
 * previous modifier).  In the decoder, these previous modifier lists
 * are constructed, so there are two &quot;skip&quot; methods that
 * indicate whether to skip over certain previously-generated
 * modifiers in the construction of these lists.
 * <p>
 * <b>Implementation note</b>: In the future, the Markov process of
 * generating modifiers will be implemented in a cleaner fashion, by
 * introducing a special <code>State</code> object, which implementors
 * of this interface will manipulate.  In other words, this interface
 * will serve to specify a <i>transition function</i>, allowing
 * greater flexibility in the experimentation with different notions
 * of history in the Markov process.
 */
public interface Shift {
  /**
   * Shifts the previously-generated modifier label into the history.
   *
   * @param event the <tt>TrainerEvent</tt> whose history is to be updated
   * @param list the current history of previously-generated modifiers
   * @param prevMod the previously-generated modifier
   */
  public void shift(TrainerEvent event, SexpList list, Sexp prevMod);
  /**
   * Shifts the previously-generated modifier head word into the history.
   *
   * @param event the <tt>TrainerEvent</tt> whose history is to be updated
   * @param wordList the current history of previously-generated modifier
   *                 head words
   * @param prevWord the head word of the previously-generated modifier
   */
  public void shift(TrainerEvent event, WordList wordList, Word prevWord);

  /**
   * Returns whether the previously-generated modifier should be skipped
   * when constructing a history for the specified chart item.
   *
   * @param item the <tt>CKYItem</tt> object whose history is being constructed
   * @param prevMod the previously-generated modifier
   * @return whether or not to skip the specified previous modifier when
   * constructing the modifier history for the specified chart item
   */
  public boolean skip(Item item, Sexp prevMod);
  /**
   * Returns whether the previously-generated modifier head word should be
   * skipped when constructing a history for the specified chart item.
   *
   * @param item the <tt>CKYItem</tt> object whose history is being constructed
   * @param prevWord the head word of the previously-generated modifier
   * @return whether or not to skip the specified previous modifier's head word
   * when constructing the modifier head word history for the specified
   * chart item
   */
  public boolean skip(Item item, Word prevWord);
}
