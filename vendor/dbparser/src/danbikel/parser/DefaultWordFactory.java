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

import danbikel.lisp.Symbol;
import danbikel.lisp.Sexp;

/**
 * The default&mdash;and currently only&mdash;implementation of
 * {@link WordFactory}.  This factory constructed {@link Word} instances.
 */
public class DefaultWordFactory implements WordFactory {

  /**
   * Creates a word factory for constructing {@link Word} objects.
   */
  public DefaultWordFactory() {}

  public Word get(Sexp s) {
    return new Word(s);
  }

  public Word get(Symbol word, Symbol tag) {
    return new Word(word, tag);
  }

  public Word get(Symbol word, Symbol tag, Symbol features) {
    return new Word(word, tag, features);
  }
}