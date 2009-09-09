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

/**
 * Specifies the methods for getting a word's features in vector form, as
 * represented by the print-name of a symbol.
 * <p>
 * A language package must provide an implementation of this interface.
 *
 * @see danbikel.parser.lang.AbstractWordFeatures
 */
public interface WordFeatures {
  /**
   * Returns a symbol representing the orthographic and/or morphological
   * features of the specified word.
   *
   * @param word the word whose features are to be computed
   * @param firstWord whether <code>word</code> is the first word in the
   * sentence (useful when computing capitalization features for certain
   * languages, such as English)
   * @return a symbol representing the orthographic and/or morphological
   * features of <code>word</code>
   */
  public Symbol features(Symbol word, boolean firstWord);

  /**
   * The symbol that represents the case where none of the features fires
   * for a particular word.
   */
  public Symbol defaultFeatureVector();
}
