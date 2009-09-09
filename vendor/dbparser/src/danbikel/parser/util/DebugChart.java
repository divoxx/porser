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
    package danbikel.parser.util;

import danbikel.util.*;
import danbikel.lisp.*;
import danbikel.parser.*;

import java.io.*;
import java.util.*;

/**
 * A class to print to <code>System.err</code> the constituents of a
 * gold-standard parse tree that were found by the parser, according to its
 * output chart file.
 */
public class DebugChart {
  // data members
  private static Symbol traceTag = Language.training().traceTag();

  private static HashSet best = new HashSet();
  private static SortedSet sortedItems = new TreeSet();

  public static Filter allPass = new AllPass();
  public static Filter onlyStopped = new Filter() {
    public boolean pass(Object obj) {
      return ((CKYItem) obj).stop();
    }
  };

  private DebugChart() {
  }

  /**
   * Prints out to <code>System.err</code> which constituents of the specified
   * gold-standard parse tree were found by the parser, according to its output
   * chart file.  The specified filename must point to a valid Java object file
   * that contains two objects: a <code>Chart</code> object and a
   * <code>SexpList</code> object (in that order), where the
   * <code>SexpList</code> object is the list of the original words in the
   * parsed sentence (which is the original sentence, but with potentially
   * certain words removed after preprocessing).  This method is intended to be
   * used for off-line debugging (i.e., after a parsing run during which chart
   * object files were created).
   *
   * @param chartFilename the filename of a parser chart file, which is a Java
   *                      object file containing two serialized objects: a
   *                      <code>Chart</code> object and a <code>SexpList</code>
   *                      object
   * @param goldTree      the gold-standard parse tree, as found in the original
   *                      <tt>combined</tt> file directory of the Penn Treebank,
   *                      except with its outer parentheses removed
   */
  public static void findConstituents(String chartFilename,
				      Sexp goldTree) {
    try {
      ObjectInputStream ois =
	new ObjectInputStream(new FileInputStream(chartFilename));
      Chart chart = (Chart) ois.readObject();
      CKYItem topRankedItem = (CKYItem) ois.readObject();
      SexpList sentence = (SexpList) ois.readObject();
      SexpList origWords = (SexpList) ois.readObject();
      Sexp newGoldTree = Language.training().preProcess(goldTree);
      boolean downcaseWords = Settings.getBoolean(Settings.downcaseWords);
      if (downcaseWords) {
	downcaseWords(newGoldTree);
      }
      replaceWords(downcaseWords, newGoldTree, sentence);
      System.err.println(Util.prettyPrint(newGoldTree));
      HeadTreeNode headTree = new HeadTreeNode(newGoldTree);
      Set best = collectBest(topRankedItem);
      findConstituents(chart, best, headTree);
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(fnfe);
    }
    catch (StreamCorruptedException sce) {
      System.err.println(sce);
    }
    catch (ClassNotFoundException cnfe) {
      System.err.println(cnfe);
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
  }

  /**
   * Prints out to <code>System.err</code> which constituents of the specified
   * gold-standard parse tree were found by the parser, according to the
   * specified chart.  This method is intended to be used for on-line parser
   * debugging (that is, debugging during a decoding run).  The only functional
   * difference between this method and {@link #findConstituents(String,Sexp)}
   * is that this method does not print out the original words of the sentence,
   * as that can be separately accomplished during decoding by setting
   * <code>Decoder.debugInit</code> to <code>true</code>.
   *
   * @param chart    the chart of the parser, after processing the sentence to
   *                 be analyzed
   * @param goldTree the gold-standard parse tree, as found in the original
   *                 <tt>combined</tt> directory of the Penn Treebank, except
   *                 with its outer parentheses removed
   */
  public static void findConstituents(boolean downcaseWords,
				      Chart chart,
				      CKYItem topRankedItem,
				      SexpList sentence,
				      Sexp goldTree) {
    findConstituents("", downcaseWords, chart, topRankedItem,
		     sentence, goldTree);
  }

  /**
   * Prints out to <code>System.err</code> which constituents of the specified
   * gold-standard parse tree were found by the parser, according to the
   * specified chart.  This method is intended to be used for on-line parser
   * debugging (that is, debugging during a decoding run).  The only functional
   * difference between this method and {@link #findConstituents(String,Sexp)}
   * is that this method does not print out the original words of the sentence,
   * as that can be separately accomplished during decoding by setting
   * <code>Decoder.debugInit</code> to <code>true</code>.
   *
   * @param prefix   the prefix string to be output before other information on
   *                 lines that don't begin with a tab character
   * @param chart    the chart of the parser, after processing the sentence to
   *                 be analyzed
   * @param goldTree the gold-standard parse tree, as found in the original
   *                 <tt>combined</tt> directory of the Penn Treebank, except
   *                 with its outer parentheses removed
   */
  public static void findConstituents(String prefix,
				      boolean downcaseWords,
				      Chart chart,
				      CKYItem topRankedItem,
				      SexpList sentence,
				      Sexp goldTree) {
    Sexp newGoldTree = Language.training().preProcess(goldTree);
    if (downcaseWords) {
      downcaseWords(newGoldTree);
    }
    replaceWords(downcaseWords, newGoldTree, sentence);
    HeadTreeNode headTree = new HeadTreeNode(newGoldTree);
    Set best = collectBest(topRankedItem);
    findConstituents(prefix, chart, best, headTree);
  }

  public static void replaceWords(boolean downcaseWords,
				  Sexp tree, SexpList sentence) {
    replaceWords(downcaseWords, tree, sentence, 0);
  }

  private static int replaceWords(boolean downcaseWords,
				  Sexp tree, SexpList sentence, int wordIdx) {
    Treebank treebank = Language.treebank();
    if (treebank.isPreterminal(tree)) {
      return wordIdx;
    }
    if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++) {
	Sexp currChild = treeList.get(i);
	if (treebank.isPreterminal(currChild)) {
	  Sexp sentenceElt = sentence.get(wordIdx);
	  boolean isUnknownWord = sentenceElt.isList();
	  if (isUnknownWord) {
	    Word word = Language.treebank().makeWord(currChild);
	    Symbol sentWord = sentenceElt.list().symbolAt(0);
	    if (downcaseWords) {
	      sentWord = Symbol.get(sentWord.toString().toLowerCase());
	    }
	    Symbol wordFeature = sentenceElt.list().symbolAt(1);
	    if (sentWord == word.word()) {
	      word.setWord(wordFeature);
	      treeList.set(i, treebank.constructPreterminal(word));
	    }
	    else {
	      System.err.println("chart debug error: didn't replace " +
				 word.word() + " with feature from " +
				 sentenceElt);
	    }
	  }
	  wordIdx++;
	}
	else {
	  wordIdx = replaceWords(downcaseWords, currChild, sentence, wordIdx);
	}
      }
    }
    return wordIdx;
  }

  public static void printBestDerivationStats(String prefix,
					      Chart chart,
					      int sentLen,
					      Symbol topSym,
					      double nonTopHighestLogProb,
					      CKYItem bestDerivationItem) {
    if (bestDerivationItem == null) {
      return;
    }
    // first, get distance (as a log prob addend, or probability factor)
    // of the current item of the best derivation to the top-ranked item
    // for the current item's span
    CKYItem bdi = bestDerivationItem;
    int start = bdi.start();
    int end = bdi.end();
    boolean nonTopSentSpanItem =
      (bdi.label() != topSym) && ((end + 1 - start) == sentLen);
    double highestLogProb =
      nonTopSentSpanItem ? nonTopHighestLogProb :
	chart.getTopLogProb(start, end);
    double distance = highestLogProb - bdi.logProb();
    distance /= Math.log(10); // put distance in log base 10
    // next, get the current best-derivation item's rank among all items
    // covering its span
    int rank = -1;
    if (bdi == chart.getTopItem(start, end)) {
      rank = 0;
    }
    else {
      sortedItems.clear();
      // add all items to sorted items
      Iterator it = chart.get(start, end);
      while (it.hasNext())
	sortedItems.add(it.next());
      rank = sortedItems.tailSet(bdi).size() - 1;
    }

    System.err.println(prefix + itemToString(bdi) + ", span=" +
		       (end + 1 - start) +
		       ", dist=" + distance + ", rank=" + rank + " of " +
		       chart.numItems(start, end));

    // recurse on head child
    printBestDerivationStats(prefix, chart, sentLen, topSym,
			     nonTopHighestLogProb, bdi.headChild());
    // recurse on left children
    for (SLNode lc = bdi.leftChildren(); lc != null; lc = lc.next())
      printBestDerivationStats(prefix, chart, sentLen, topSym,
			       nonTopHighestLogProb, (CKYItem) lc.data());
    // recurse on right children
    for (SLNode rc = bdi.rightChildren(); rc != null; rc = rc.next())
      printBestDerivationStats(prefix, chart, sentLen, topSym,
			       nonTopHighestLogProb, (CKYItem) rc.data());
  }

  // helper methods
  public static void findConstituents(Chart chart, Set best,
				      HeadTreeNode tree) {
    findConstituents("", chart, best, tree);
  }

  public static void findConstituents(String prefix,
				      Chart chart, Set best,
				      HeadTreeNode tree) {
    if (!tree.isPreterminal()) {
      int start = tree.leftIdx();
      // head tree nodes specify right index as index of rightmost word PLUS 1,
      // but we simply want index of rightmost word
      int end = tree.rightIdx() - 1;
      Symbol label = (Symbol) tree.label();
      Iterator it = chart.get(start, end);
      CKYItem found = null;
      CKYItem foundNoStop = null;
      CKYItem unlexicalizedFound = null;
      CKYItem unlexicalizedFoundNoStop = null;
      while (it.hasNext()) {
	CKYItem item = (CKYItem) it.next();
	if (item.label().equals(label)) {
	  if (item.stop()) {
	    boolean haventPreviouslyFound = unlexicalizedFound == null;
	    if (haventPreviouslyFound || best.contains(item)) {
	      unlexicalizedFound = item;
	    }
	  }
	  else {
	    unlexicalizedFoundNoStop = item;
	  }
	  if (item.headWord().equals(tree.headWord())) {
	    if (item.stop()) {
	      System.err.println(prefix + "found " +
				 itemToString(item, best));
	      boolean haventPreviouslyFound = found == null;
	      boolean currItemInBest = best.contains(item);
	      if (haventPreviouslyFound || currItemInBest) {
		found = item;
	      }
	      // if we've found a lexicalized nonterminal that covers this
	      // span and is part of the best derivation, end search!
	      if (currItemInBest) {
		break;
	      }
	    }
	    else {
	      foundNoStop = item;
	    }
	  }
	}
      }

      if (found == null) {
	System.err.print(prefix + "didn't find " +
			 headTreeNodeToString(tree));
	boolean foundOther =
	  foundNoStop != null ||
	  unlexicalizedFound != null || unlexicalizedFoundNoStop != null;
	if (foundOther) {
	  System.err.println(" but found:" +
			     (unlexicalizedFound != null ?
			      "\n\t" +
			      itemToString(unlexicalizedFound, best) : "") +
			     (foundNoStop != null ?
			      "\n\t" +
			      itemToString(foundNoStop, best) : "") +
			     (unlexicalizedFound == null &&
			      unlexicalizedFoundNoStop != null ?
			      "\n\t" + itemToString(unlexicalizedFoundNoStop,
						    best) :
			       ""));
	}
	else {
	  System.err.println();
	}
      }

      // recurse on head child
      findConstituents(prefix, chart, best, tree.headChild());
      // recurse on pre- and post-mods
      it = tree.preMods().iterator();
      while (it.hasNext())
	findConstituents(prefix, chart, best, (HeadTreeNode) it.next());
      it = tree.postMods().iterator();
      while (it.hasNext())
	findConstituents(prefix, chart, best, (HeadTreeNode) it.next());
    }
  }

  public static Set collectBest(CKYItem topRanked) {
    best.clear();
    if (topRanked == null) {
      return best;
    }
    return collectBest(topRanked, best);
  }

  private static Set collectBest(CKYItem curr, Set best) {
    if (curr.isPreterminal()) {
      best.add(curr);
    }
    else {
      // collect this item
      best.add(curr);
      // recurse on head child
      collectBest(curr.headChild(), best);
      // recurse on left children
      for (SLNode lc = curr.leftChildren(); lc != null; lc = lc.next())
	collectBest((CKYItem) lc.data(), best);
      // recurse on right children
      for (SLNode rc = curr.rightChildren(); rc != null; rc = rc.next())
	collectBest((CKYItem) rc.data(), best);
    }
    return best;
  }

  /**
   * Returns a string of the form <tt>[start,end,label&lt;headWord&gt;,
   * &lt;headChild&gt;]</tt> where <tt>&lt;headWord&gt;</tt> is the head word
   * and where <tt>&lt;headChild&gt;</tt> is either a string of the form
   * <tt>[start,end,label]</tt> or <tt>null</tt> if the specified
   * <code>HeadTreeNode</code> is a preterminal.
   *
   * @return a string representation of the specified node
   */
  public static String headTreeNodeToString(HeadTreeNode node) {
    return ("[" + node.leftIdx() + "," + (node.rightIdx() - 1) + "," +
	    node.label() + node.headWord() + ", " +
	    (node.isPreterminal() ? "[null]" :
	      "[" +
	      node.headChild().leftIdx() + "," +
	      (node.headChild().rightIdx() - 1) + "," +
	      node.headChild().label() +
	      "]") +
		   "]");

  }

  public static String itemToString(CKYItem item, Set best) {
    int numChildren = item.numLeftChildren() + item.numRightChildren();
    if (!item.isPreterminal()) {
      numChildren++;
    }
    return ("[" + item.start() + "," + item.end() + "," +
	    item.label() + item.headWord() + ",stop=" +
	    (item.stop() ? "t" : "f") + ", best=" +
	    (best.contains(item) ? "t" : "f") + ", " +
	    (item.isPreterminal() ? "[null]" :
	      "[" +
	      item.headChild().start() + "," +
	      item.headChild().end() + "," +
	      item.headChild().label() +
	      "]") +
		   ",numKids=" + numChildren +
		   "]");
  }

  public static String itemToString(CKYItem item) {
    int numChildren = item.numLeftChildren() + item.numRightChildren();
    if (!item.isPreterminal()) {
      numChildren++;
    }
    return ("[" + item.start() + "," + item.end() + "," +
	    item.label() + item.headWord() + ",stop=" +
	    (item.stop() ? "t" : "f") + ", " +
	    (item.isPreterminal() ? "[null]" :
	      "[" +
	      item.headChild().start() + "," +
	      item.headChild().end() + "," +
	      item.headChild().label() +
	      "]") +
		   ",numKids=" + numChildren +
		   "]");
  }

  public static void downcaseWords(Sexp tree) {
    Treebank treebank = Language.treebank();
    if (treebank.isPreterminal(tree)) {
      return;
    }
    if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++) {
	Sexp currChild = treeList.get(i);
	if (treebank.isPreterminal(currChild)) {
	  Word word = treebank.makeWord(currChild);
	  word.setWord(Symbol.add(word.word().toString().toLowerCase()));
	  treeList.set(i, treebank.constructPreterminal(word));
	}
	else {
	  downcaseWords(currChild);
	}
      }
    }
  }

  public static void downcaseWords(HeadTreeNode tree) {
    if (tree.isPreterminal()) {
      if (tree.headWord().tag() != traceTag) {
	Word headWord = tree.headWord();
	tree.setOriginalHeadWord(headWord.word());
	headWord.setWord(Symbol.add(headWord.word().toString().toLowerCase()));
      }
    }
    else {
      downcaseWords(tree.headChild());
      for (Iterator mods = tree.preMods().iterator(); mods.hasNext();)
	downcaseWords((HeadTreeNode) mods.next());
      for (Iterator mods = tree.postMods().iterator(); mods.hasNext();)
	downcaseWords((HeadTreeNode) mods.next());
    }
  }

  /**
   * Removes preterminals from the specified tree that are not found in the
   * specified list of words.
   *
   * @param words   the words of the sentence that was parsed (meaning that some
   *                of the words of the original sentence may have been pruned)
   * @param tree    the parse tree whose preterminals are to match
   *                <code>words</code>
   * @param wordIdx the threaded word index; to be <tt>0</tt> for all
   *                non-recursive calls
   * @return the modified tree
   */
  public static Sexp removePreterms(SexpList words, Sexp tree, int wordIdx) {
    if (Language.treebank().isPreterminal(tree)) {
      return tree;
    }
    if (tree.isList()) {
      SexpList treeList = tree.list();
      for (int i = 1; i < treeList.length(); i++) {
	Sexp currChild = treeList.get(i);
	if (Language.treebank().isPreterminal(currChild)) {
	  Word treeWord = Language.treebank().makeWord(currChild);
	  if (treeWord.word() == words.get(wordIdx)) {
	    wordIdx++;
	  }
	  else {
	    treeList.remove(i--);
	  }
	}
	else {
	  removePreterms(words, currChild, wordIdx);
	}
      }
    }
    return tree;
  }

  /**
   * Removes interior nodes of the specified tree that are not preterminals and
   * that have no children.
   *
   * @param tree the tree from which to remove childless interior nodes
   * @return the modified tree
   */
  public static Sexp removeChildlessNodes(Sexp tree) {
    if (Language.treebank().isPreterminal(tree)) {
      return tree;
    }
    if (tree.isList()) {
      SexpList treeList = tree.list();
      for (int i = 1; i < treeList.length(); i++) {
	Sexp currChild = treeList.get(i);
	if (!Language.treebank().isPreterminal(tree) &&
	    currChild.isList() && currChild.list().length() == 1) {
	  treeList.remove(i--);
	}
	else {
	  removeChildlessNodes(currChild);
	}
      }
    }
    return tree;
  }

  /**
   * Prints the derivation rooted at the specified chart item to
   * <tt>System.err</tt>.
   */
  public static void printDerivation(CKYItem item) {
    printDerivation(item, allPass);
  }

  public static void printDerivation(CKYItem item, Filter filter) {
    printDerivation(item, filter, 0);
  }

  private static void printDerivation(CKYItem item, Filter filter, int level) {
    if (filter.pass(item)) {
      for (int i = 0; i < level; i++)
	System.err.print("  ");
      System.err.println(item);
      if (item.headChild() != null) {
	printDerivation(item.headChild(), filter, level + 1);
      }
      SLNode leftChildren = item.leftChildren();
      while (leftChildren != null) {
	printDerivation((CKYItem) leftChildren.data(), filter, level + 1);
	leftChildren = leftChildren.next();
      }
      SLNode rightChildren = item.rightChildren();
      while (rightChildren != null) {
	printDerivation((CKYItem) rightChildren.data(), filter, level + 1);
	rightChildren = rightChildren.next();
      }
    }
  }
}
