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

import danbikel.lisp.*;
import danbikel.parser.*;
import java.util.*;

/**
 * Represents a set of constraints that correspond to a specific lexicalized
 * parse tree, for use when the bottom-up parsing algorithm needs to generate
 * only the analyses that are consistent with a particular lexicalized tree.
 * Accordingly, the individual <tt>Constraint</tt> objects in this set form
 * an isomorphic tree structure.
 *
 * @see LexTreeConstraint
 */
public class LexTreeConstraintSet extends UnlexTreeConstraintSet {

  /**
   * Constructs a new, empty set of constraints.
   */
  public LexTreeConstraintSet() {
    super();
  }

  /**
   * Constructs a new set of constraints according to a lexicalized version of
   * the specified unlexicalized tree.  Lexicalization performed by the current
   * {@linkplain danbikel.parser.Language#headFinder() head finder}.
   *
   * @param tree the lexicalized tree for which to construct a constaint set
   */
  public LexTreeConstraintSet(Sexp tree) {
    super(tree);
  }

  /**
   * Builds the constraint tree from a lexicalized version of the specified
   * unlexicalized parse tree (exicalization performed by the current
   * {@linkplain danbikel.parser.Language#headFinder() head finder}). As a
   * necessary side-effect, the {@link #root} and {@link #leaves} data members
   * will be set/populated by this method.
   *
   * @param tree the tree from which to build this constraint set
   */
  protected void buildConstraintSet(Sexp tree) {
    root = new LexTreeConstraint(tree);
    collectNodes(root);
    list.trimToSize();
    leaves.trimToSize();
  }

  /**
   * Test driver for this class.
   * @param args
   */
  public static void main(String[] args) {
    try {
      SexpTokenizer tok =
	new SexpTokenizer(System.in, Language.encoding(), 8192);
      Sexp curr = null;
      while ((curr = Sexp.read(tok)) != null) {
	LexTreeConstraintSet set = new LexTreeConstraintSet(curr);
	System.out.println(((SexpConvertible)set.root()).toSexp());
      }
    }
    catch (Exception e) {
      System.err.println(e);
    }
  }
}