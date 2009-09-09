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

/**
 * Specifies a single method to map a symbol representing a nonterminal to
 * another symbol, typically an equivalence class.
 *
 * @see NTMapper
 * @see Settings#prevModMapperClass
 */
public interface NonterminalMapper {
  /**
   * Maps the specified nonterminal label to some other symbol (typically
   * an equivalence class).
   * @param label the nonterminal label to be mapped
   * @return a mapped version of the specified nonterminal label (typically
   * an equivalence class)
   */
  public Symbol map(Symbol label);
}
