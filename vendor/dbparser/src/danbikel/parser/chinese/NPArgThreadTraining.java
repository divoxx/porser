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
    package danbikel.parser.chinese;

import java.util.*;
import java.io.*;
import danbikel.lisp.*;

/**
 * This class is identical to {@link danbikel.parser.chinese.Training}, except
 * that the {@link #preProcess(Sexp)} method invokes
 * {@link danbikel.parser.lang.AbstractTraining#threadNPArgAugmentations(Sexp)}.
 */
public class NPArgThreadTraining extends Training {
  /**
   * The default constructor, to be invoked by {@link danbikel.parser.Language}.
   * This constructor looks for a resource named by the property
   * <code>metadataPropertyPrefix + language</code>
   * where <code>metadataPropertyPrefix</code> is the value of
   * the constant {@link #metadataPropertyPrefix} and <code>language</code>
   * is the value of <code>Settings.get(Settings.language)</code>.
   * For example, the property for Chinese is
   * <code>&quot;parser.training.metadata.chinese&quot;</code>.
   */
  public NPArgThreadTraining() throws FileNotFoundException, IOException {
    super();
  }

  /**
   * Performs all the preprocessing setps of the overridden {@link
   * Training#preProcess(Sexp)} method of the superclass, and then provides an
   * additional preprocessing step by invoking {@link
   * danbikel.parser.lang.AbstractTraining#threadNPArgAugmentations(Sexp)}.
   *
   * @param tree the tree to be preprocessed in-place
   * @return a preprocessed version of the specified tree
   */
  public Sexp preProcess(Sexp tree) {
    //transformSubjectNTs(tree);
    super.preProcess(tree);
    threadNPArgAugmentations(tree);
    return tree;
  }

  /**
   * Test driver for this class. Simply invokes <code>main</code> of the
   * superclass with the specified argument array.
   *
   * @see Training#main(String[])
   */
  public static void main(String[] args) {
    Training.main(args);
  }
}
