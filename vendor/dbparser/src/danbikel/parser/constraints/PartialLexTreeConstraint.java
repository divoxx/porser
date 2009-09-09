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

import danbikel.util.*;
import danbikel.lisp.*;
import danbikel.parser.*;
import java.util.*;

/**
 * Represents a node in a parsing constraint tree, that requires an associated
 * chart item to have the same label, head word and head tag.  The crucial
 * difference between this type of constraint and {@link LexTreeConstraint}
 * is that the latter uses the {@link Word#equals} method to determine
 * lexicalized node equality, whereas this constraint explicitly compares only
 * the corresponding {@link Word#word()} and {@link Word#tag()} fields
 * of head words, making this type of constraint suitable when the head
 * word objects are subclasses of <code>Word</code> that include more
 * information (such as, for example, WordNet synsets).
 */
public class PartialLexTreeConstraint extends UnlexTreeConstraint {

  /**
   * The head word associated with this constraint.
   */
  protected Word headWord;

  /**
   * Constructs a tree of constraints given the specified parse tree.
   * @param tree
   */
  public PartialLexTreeConstraint(Sexp tree) {
    this(null, tree, new IntCounter(0), Language.headFinder());
  }

  /**
   * A helper constructor used by {@link #PartialLexTreeConstraint(Sexp)}
   * to construct an entire tree of constraints.
   * @param parent the parent of this constraint being constructed, or
   * <code>null</code> if this constraint is the root of a tree of
   * constraints
   * @param tree the root of the subtree for which to construct a
   * constraint
   * @param currWordIdx the current 0-based word index of the left-most
   * word of the specified subtree (<code>tree</code>)
   * @param headFinder the head finder to be used for lexicalizing the
   * specified tree (typically {@link danbikel.parser.Language#headFinder()})
   */
  protected PartialLexTreeConstraint(PartialLexTreeConstraint parent,
				     Sexp tree, IntCounter currWordIdx,
				     HeadFinder headFinder) {
    if (Language.treebank().isPreterminal(tree)) {
      this.parent = parent;
      children = Collections.EMPTY_LIST;
      headWord = Language.treebank().makeWord(tree);
      label = headWord.tag();
      start = end = currWordIdx.get();
      currWordIdx.increment();
    }
    else {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();

      //label = Language.treebank.getCanonical(treeList.symbolAt(0));
      label = treeList.symbolAt(0);
      start = currWordIdx.get();
      this.parent = parent;
      children = new ArrayList(treeListLen - 1);
      for (int i = 1; i < treeListLen; i++) {
	Sexp child = treeList.get(i);
	children.add(new PartialLexTreeConstraint(this, child, currWordIdx,
						  headFinder));
      }
      end = currWordIdx.get() - 1;

      // inherit head word from head child in constraint tree
      int headIdx = headFinder.findHead(tree) - 1; // convert to zero-based
      headWord = ((PartialLexTreeConstraint)children.get(headIdx)).headWord;
    }
  }

  /**
   * Returns <code>true</code> if this constraint {@linkplain
   * #isLocallySatisfiedBy is locally satisfied by} the specified item and
   * if this constraint's {@linkplain #spanMatches span matches} that of
   * the specified item.  This overridden definition is in stark contrast
   * to that of {@link UnlexTreeConstraint}, where preterminals are
   * <i>always</i> satisfied by preterminal constraints, meaning that
   * parts of speech are not constrained.
   *
   * @param item the item to be tested
   * @return <code>true</code> if this constraint {@linkplain
   * #isLocallySatisfiedBy is locally satisfied by} the specified item and if
   * this constraint's {@linkplain #spanMatches span matches} that of the
   * specified item.
   */
  protected boolean isSatisfiedByPreterminal(CKYItem item) {
    if (isLocallySatisfiedBy(item) && spanMatches(item)) {
      satisfied = true;
      return true;
    }
    else
      return false;
  }

  public boolean isLocallySatisfiedBy(Item item) {
    if (item.label() != label)
      return false;

    // COMPARE ONLY WORD AND TAG
    // (this is the "partial" of "PartialLexTreeConstraint")
    Word otherHeadWord = ((CKYItem)item).headWord();
    return (otherHeadWord.tag() == headWord.tag() &&
	    (otherHeadWord.word() == headWord.word()));
  }

  public Sexp toSexp() {
    SexpList retVal = new SexpList(children.size() + 1);

    Symbol thisNode = Symbol.add(label.toString() + headWord + "-" +
				 start + "-" + end);

    retVal.add(thisNode);
    for (int i = 0; i < children.size(); i++)
      retVal.add(((SexpConvertible)children.get(i)).toSexp());

    return retVal;
  }

  /**
   * Returns a human-readable string representation of this constraint
   * for debugging.
   * @return a human-readable string representation of this constraint
   * for debugging
   */
  public String toString() {
    return "headWord.word=" + headWord.word() + ", headWord.tag=" +
	   headWord.tag() + ", label=" + label +
	   ", span=(" + start + "," + end + "), parentLabel=" +
	   (parent == null ? "null" : parent.label.toString());
  }
}
