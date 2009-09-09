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
 * Provides the identity mapping function.
 */
public class IdentityNTMapper implements NonterminalMapper {
  /**
   *  Constructs a new instance of this identity mapper.
   */
  IdentityNTMapper() {}
  
  /**
   * Returns the specified label unchanged (the identity mapping function).
   * @param label the label to be mapped
   * @return the specified label unchanged (the identity mapping function).
   */
  public Symbol map(Symbol label) {
    return label;
  }
}
