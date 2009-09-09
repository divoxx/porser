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
 * A factory for creating <code>SubcatBag</code> objects.
 *
 * @see SubcatBag
 * @see Subcats
 * @see Settings#subcatFactoryClass
 */
public class SubcatBagFactory implements SubcatFactory {
  /** Constructs a new <code>SubcatBagFactory</code>. */
  public SubcatBagFactory() {}

  /** Returns an empty <code>SubcatBag</code>. */
  public Subcat get() { return new SubcatBag(); }

  /**
   * Returns a <code>SubcatBag</code> initialized with the requirements
   * contained in the specified list.
   *
   * @param list a list of <code>Symbol</code> objects to be added
   * as requirements to a new <code>SubcatBag</code> instance
   */
  public Subcat get(SexpList list) { return new SubcatBag(list); }
}
