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

import danbikel.util.*;
import danbikel.lisp.*;
import java.util.*;
import java.io.*;

/**
 * Provides a convenient data structure for navigating a parse tree in
 * which heads have been found and percolated up through the tree.  This
 * data structure mirrors the head relations of this parser, where the
 * underlying lexicalized grammar productions are of the form
 * <blockquote>
 * P &rarr; L<sub><i>n</i></sub>L<sub><i>n</i>-1</sub>
 * <sup>&hellip;</sup> H <sup>&hellip;</sup>
 * R<sub><i>n</i>-1</sub>R<sub><i>n</i></sub>
 * </blockquote>
 * Since the order of the modifying nonterminals is from adjacent to the head
 * outward, this data structure stores modifying nonterminals in this order.
 */
public class HeadTreeNode implements Serializable, SexpConvertible {

  // constants
  private final static String className = HeadTreeNode.class.getName();

  // "mutable" constant
  private static boolean baseNPsCannotContainVerbs =
    Settings.getBoolean(Settings.baseNPsCannotContainVerbs);

  static {
    Settings.register(HeadTreeNode.class, new Settings.Change() {
      public void update(Map<String,String> changedSettings) {
	baseNPsCannotContainVerbs =
	  Settings.getBoolean(Settings.baseNPsCannotContainVerbs);
      }
    }, null);
  }

  // data members

  /** The nonterminal label of this node. */
  private Symbol label;
  /** The head word of this node. */
  private Word headWord;
  /** The index of the head word of this node. */
  private int headWordIdx;
  /** The original head word observed in the training data, before any
      downcasing or transformation to an unknown word token or a low-frequency
      word feature vector. */
  private Symbol originalHeadWord;
  /** The head child of this node. */
  private HeadTreeNode headChild;
  /** A list of the premodifiers of the head child of this node. */
  private List preMods;
  /** A list of the postmodifiers of the head child of this node. */
  private List postMods;
  /** A boolean to indicate if the current subtree contains a verb. */
  private boolean containsVerb;
  /** The index of the leftmost word in this subtree. */
  private int leftIdx;
  /** The index of the rightmost word in this subtree plus 1. */
  private int rightIdx;

  // handles onto WordFeatures, Treebank and HeadFinder objects
  private static WordFeatures wordFeatures = Language.wordFeatures();
  private static Treebank treebank = Language.treebank();
  private static HeadFinder headFinder = Language.headFinder();

  HeadTreeNode(Word headWord, HeadTreeNode headChild,
	       ArrayList preMods, ArrayList postMods) {
    this.headWord = headWord;
    this.headChild = headChild;
    this.preMods = preMods;
    this.postMods = postMods;
  }

  /**
   * Constructs a new instance from the specified parse tree.
   * @param tree the parse tree from which to construct a new
   * {@link HeadTreeNode}
   */
  public HeadTreeNode(Sexp tree) {
    this(tree, new IntCounter());
  }

  private HeadTreeNode(Sexp tree, IntCounter wordCounter) {
    if (tree.isSymbol())
      throw new IllegalArgumentException(className + ": constructor argument " +
					 "\"" + tree + "\" is of wrong type: " +
					 tree.getClass().getName());

    leftIdx = wordCounter.get();

    if (treebank.isPreterminal(tree)) {
      headChild = null;
      preMods = Collections.EMPTY_LIST;
      postMods = Collections.EMPTY_LIST;
      headWord = treebank.makeWord(tree);
      headWordIdx = wordCounter.increment();
      label = headWord.tag();
      containsVerb = treebank.isVerb(tree);
      rightIdx = wordCounter.get();
      return;
    }

    if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();

      // set label of this node
      label = treeList.get(0).symbol();

      // find head child
      int headIdx = headFinder.findHead(tree);

      // get left modifiers of head child, from left to right and
      // reverse list afterward
      int numPreMods = headIdx - 1;
      preMods = (numPreMods == 0 ? Collections.EMPTY_LIST :
		 new ArrayList(numPreMods));
      for (int i = 1; i < headIdx; i++) {
	HeadTreeNode preMod = new HeadTreeNode(treeList.get(i), wordCounter);
	preMods.add(preMod);
	containsVerb |= preMod.containsVerb;
      }
      Collections.reverse(preMods);

      // set headChild data member
      headChild = new HeadTreeNode(treeList.get(headIdx), wordCounter);
      containsVerb |= headChild.containsVerb;


      // get right modifiers of head child, from adjacent to head outward
      int numPostMods = treeListLen - headIdx - 1;
      postMods = (numPostMods == 0 ? Collections.EMPTY_LIST :
		  new ArrayList(numPostMods));
      for (int i = headIdx + 1; i < treeListLen; i++) {
	HeadTreeNode postMod = new HeadTreeNode(treeList.get(i), wordCounter);
	postMods.add(postMod);
	containsVerb |= postMod.containsVerb;
      }

      if (baseNPsCannotContainVerbs) {
	if (Language.treebank.isBaseNP(label)) {
	  containsVerb = false;
	}
      }

      rightIdx = wordCounter.get();

      // finally, set head word
      headWord = headChild.headWord;
      headWordIdx = headChild.headWordIdx;
      return;
    }

    // shouldn't ever reach this point!
    System.err.println(className + ": something very bad happened");
  }

  /**
   * Returns whether this node represents a preterminal.
   * @return whether this node represents a preterminal.
   */
  public boolean isPreterminal() { return headChild == null; }


  // accessors

  /** Gets the nonterminal label for this node. */
  public Symbol label() { return label; }
  /** Gets the head word for this node. */
  public Word headWord() { return headWord; }
  /** Gets the index of the head word for this node. */
  public int headWordIdx() { return headWordIdx; }
  /** Gets the original version of the head word. */
  public Symbol originalHeadWord() { return originalHeadWord; }
  /** Indicates whether this subtree contains a verb. */
  public boolean containsVerb() { return containsVerb; }
  /** Gets the head child of this node. */
  public HeadTreeNode headChild() { return headChild; }
  /** Gets the list of premodifiers of the head child of this node. */
  public List preMods() { return preMods; }
  /** Gets the list of postmodifiers of the head child of this node. */
  public List postMods() { return postMods; }
  /** Gets the index of the leftmost word in this subtree. */
  public int leftIdx() { return leftIdx; }
  /** Gets the index of the rightmost word in this subtree plus 1. */
  public int rightIdx() { return rightIdx; }


  // mutators
  /**
   * Sets the original version of the head word of this node, before any
   * downcasing or other transformations were applied during training.
   * @param originalHeadWord the original head word to set for this node
   */
  public void setOriginalHeadWord(Symbol originalHeadWord) {
    this.originalHeadWord = originalHeadWord;
  }

  public Sexp toSexp() {
    if (isPreterminal()) {
      return Language.treebank.constructPreterminal(headWord());
    }
    else {
      SexpList list = new SexpList(preMods.size() + postMods.size() + 2);
      // first, add the label of this node
      list.add(label());
      // then, add premodifiers in reverse
      ListIterator it = preMods.listIterator(preMods.size());
      while (it.hasPrevious()) {
	HeadTreeNode node = (HeadTreeNode)it.previous();
	list.add(node.toSexp());
      }
      // next, add head child subtree
      list.add(headChild.toSexp());
      // finally, add postmodifiers in order
      it = postMods.listIterator();
      while (it.hasNext()) {
	HeadTreeNode node = (HeadTreeNode)it.next();
	list.add(node.toSexp());
      }
      return list;
    }
  }

  public Sexp toSexp(boolean addHeadWords) {
    if (isPreterminal()) {
      return Language.treebank.constructPreterminal(headWord());
    }
    else {
      SexpList list = new SexpList(preMods.size() + postMods.size() + 2);
      // first, add the label of this node
      Symbol label = addHeadWords ? getHeadWordLabel() : label();
      list.add(label);
      // then, add premodifiers in reverse
      ListIterator it = preMods.listIterator(preMods.size());
      while (it.hasPrevious()) {
	HeadTreeNode node = (HeadTreeNode)it.previous();
	list.add(node.toSexp(addHeadWords));
      }
      // next, add head child subtree
      list.add(headChild.toSexp(addHeadWords));
      // finally, add postmodifiers in order
      it = postMods.listIterator();
      while (it.hasNext()) {
	HeadTreeNode node = (HeadTreeNode)it.next();
	list.add(node.toSexp(addHeadWords));
      }
      return list;
    }
  }

  protected Symbol getHeadWordLabel() {
    Treebank treebank = Language.treebank();
    return
      Symbol.get(label().toString() + treebank.nonTreebankLeftBracket() +
		 headWord().word() + treebank.nonTreebankDelimiter() +
		 headWord().tag() + treebank.nonTreebankRightBracket());
  }

  /**
   * Returns a string representation of the tree rooted at this node.
   * @return a string representation of the tree rooted at this node
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    toString(sb, 0);
    return sb.toString();
  }

  private void toString(StringBuffer sb, int level) {
    StringBuffer levelSpaceBuf = new StringBuffer(level * 2);
    for (int i = 0; i < level; i++)
      levelSpaceBuf.append("  ");
    String levelSpace = levelSpaceBuf.toString();
    String levelPlusOneSpace = levelSpace + "  ";

    sb.append(levelSpace);

    if (isPreterminal()) {
      sb.append("(headWord ").append(headWord).append(")");
      return;
    }

    sb.append("(");
    sb.append("(label ");
    if (label == null)
      sb.append("null");
    else
      sb.append(label);
    sb.append(") ");
    sb.append("(containsVerb ");
    sb.append(containsVerb);
    sb.append(") ");
    sb.append("(headWord ").append(headWord).append(")\n");
    sb.append(levelPlusOneSpace);
    sb.append("(headChild");
    if (headChild == null)
      sb.append(" null");
    else {
      sb.append("\n");
      headChild.toString(sb, level + 2);
    }
    sb.append(")\n");
    sb.append(levelPlusOneSpace);
    sb.append("(preMods");
    int numPreMods = preMods.size();
    sb.append(numPreMods > 0 ? "\n" : " ()");
    for (int i = 0; i < numPreMods; i++) {
      ((HeadTreeNode)preMods.get(i)).toString(sb, level + 2);
      if (i < numPreMods - 1)
	sb.append("\n");
    }
    sb.append(")\n");
    sb.append(levelPlusOneSpace);
    sb.append("(postMods");
    int numPostMods = postMods.size();
    sb.append(numPostMods > 0 ? "\n" : " ()");
    for (int i = 0; i < numPostMods; i++) {
      ((HeadTreeNode)postMods.get(i)).toString(sb, level + 2);
      if (i < numPostMods - 1)
	sb.append("\n");
    }
    sb.append("))");
  }

  public static void main(String[] args) {
    InputStream is = null;
    try {
      if (args.length == 0 || args[0].equals("-"))
	is = System.in;
      else
	is = new FileInputStream(args[0]);
      SexpTokenizer tok =
	new SexpTokenizer(is, Language.encoding(),
			  Constants.defaultFileBufsize);
      Sexp curr = null;
      while ((curr = Sexp.read(tok)) != null)
	System.out.println(new HeadTreeNode(curr));
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(fnfe);
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
  }
}
