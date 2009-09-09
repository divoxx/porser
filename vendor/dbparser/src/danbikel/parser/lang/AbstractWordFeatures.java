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
    package danbikel.parser.lang;

import danbikel.lisp.*;
import danbikel.parser.*;
import java.io.Serializable;

/**
 * Provides a default abstract implementation of the {@link WordFeatures}
 * interface.
 */
public abstract class AbstractWordFeatures
  implements WordFeatures, Serializable {
  /**
   * The unique symbol to represent unknown words.  The default value
   * is the return value of <code>Symbol.add(&quot;+unknown+&quot;)</code>;
   * if this maps to an actual word in a particular language or Treebank,
   * this data member should be reassigned in a subclass.
   */
  protected static Symbol unknownWordSym = Symbol.add("+unknown+");

  /**
   * Default constructor, to be called by subclasses (usually implicitly).
   */
  protected AbstractWordFeatures() {
  }

  /**
   * Returns a symbol representing the orthographic and/or morphological
   * features of the specified word. This default implementation simply returns
   * the unknown word symbol.
   *
   * @param word the word whose features are to be computed
   * @param firstWord whether <code>word</code> is the first word in the
   * sentence (useful when computing capitalization features for certain
   * languages, such as English)
   * @return a symbol representing the orthographic and/or morphological
   * features of <code>word</code>
   *
   * @see #unknownWordSym
   */
  public Symbol features(Symbol word, boolean firstWord) {
    return unknownWordSym;
  }

  /**
   * The symbol that represents the case where none of the features fires
   * for a particular word.
   */
  public abstract Symbol defaultFeatureVector();
}
