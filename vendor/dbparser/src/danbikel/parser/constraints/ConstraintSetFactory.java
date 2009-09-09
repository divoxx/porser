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
    package danbikel.parser.constraints;

import danbikel.parser.Settings;

/**
 * Specification for a <code>ConstraintSet</code> object factory, to be used by
 * the <code>ConstraintSets</code> static factory class.
 *
 * @see ConstraintSets
 * @see Settings#constraintSetFactoryClass
 */
public interface ConstraintSetFactory {
  /**
   * Return a <code>ConstraintSet</code> object created with its default
   * constructor.
   */
  ConstraintSet get();

  /**
   * Return a <code>ConstraintSet</code> object created with its one-argument
   * constructor.
   *
   * @param input the input from which to derive parsing constraints
   */
  ConstraintSet get(Object input);
}