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

import java.io.*;
import java.util.HashMap;

import danbikel.lisp.*;

/**
 * Specifies the methods for the head-finding component of a language package.
 * A head finder determines the distinguished head child for any context-free
 * production.
 * <p/>
 * A language package must include an implementation of this interface.
 *
 * @see danbikel.parser.lang.AbstractHeadFinder
 */
public interface HeadFinder {
  /**
   * Finds the head for the production at the root of the specified subtree. The
   * general contract of this method is to extract the root nonterminal label of
   * the specified tree, create a list of the child nonterminal labels and call
   * {@link #findHead(Sexp,Symbol,SexpList)}.
   *
   * @param tree the subtree for whose root production to find the head
   * @return the 1-based index of the head child of the production at the root
   *         of the specified subtree
   *
   * @see #findHead(Sexp,Symbol,SexpList)
   */
  public int findHead(Sexp tree);

  /**
   * Finds the head for the grammar production <code>lhs &rarr; rhs</code>. This
   * method may destructively modify <code>rhs</code>.
   *
   * @param tree the original subtree in which to find the head child, or
   *             <code>null</code> if the subtree is not available
   * @param lhs  the nonterminal label that is the left-hand side of a grammar
   *             production
   * @param rhs  a list of symbols that is the right-hand side of a grammar
   *             production
   * @return the 1-based index of the head child in <code>rhs</code>
   */
  public int findHead(Sexp tree, Symbol lhs, SexpList rhs);

  /**
   * Perform head-finding in <code>tree</code>, augmenting nodes that are the
   * head child of their parent by appending {@link #headSuffix()}. This method
   * is useful for head-finding debugging.
   *
   * @return a reference to the modified <code>tree</code> object
   */
  public Sexp addHeadInformation(Sexp tree);

  /**
   * The suffix to append to nodes that are the head children of their
   * respective parents when marking heads via {@link #addHeadInformation(Sexp)}.
   *
   * @return the string that is to be appended to the print name of nodes that
   *         are the head children of their respective parents
   */
  public String headSuffix();
}
