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
 * Specification for a <code>Subcat</code> object factory, to be used by
 * the <code>Subcats</code> static factory class.
 *
 * @see Subcats
 * @see Settings#subcatFactoryClass
 */
public interface SubcatFactory extends Serializable {
  /**
   * Return a <code>Subcat</code> object created with its default constructor.
   */
  public Subcat get();
  /**
   * Return a <code>Subcat</code> object created with its one-argument
   * constructor, using the specified list.
   *
   * @param list a list containing only <code>Symbol</code> objects; the
   * behavior is undefined if <code>list</code> contains a <code>SexpList</code>
   * object
   */
  public Subcat get(SexpList list);
}
