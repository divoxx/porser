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

import danbikel.lisp.*;
import danbikel.parser.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.Reader;
import java.io.IOException;

/**
 * Contains basic utility functions for <code>Sexp</code> objects that
 * represent parse trees.
 * @author Dan Bikel
 */
public class Util {
  // constants
  private static final Pattern underline = Pattern.compile("(.*)_(.*)");
  private static final char[] ibmOrdinaryChars = {'[', ']'};

  private Util() {}

  /**
   * Returns a <code>SexpList</code> that contains all the leaves of the
   * specified parse tree.
   *
   * @param tree the tree from which to collect leaves (words)
   * @return a list of the words contained in the specified tree
   */
  public static Sexp collectLeaves(Sexp tree) {
    SexpList leaves = new SexpList();
    collectLeaves(tree, leaves, false);
    return leaves;
  }

  /**
   * Returns a <code>SexpList</code> that contains all the words of the
   * specified parse tree as well as their part of speech tags, where each
   * word is its own <code>SexpList</code> of the form <tt>(word (tag))</tt>.
   *
   * @param tree the tree from which to collect tagged words
   * @return a list of tagged words from the specified tree
   */
  public static Sexp collectTaggedWords(Sexp tree) {
    SexpList taggedWords = new SexpList();
    collectLeaves(tree, taggedWords, true);
    return taggedWords;
  }

  public static ArrayList collectWordObjects(Sexp tree) {
    return collectWordObjects(tree, new ArrayList());
  }

  private static ArrayList collectWordObjects(Sexp tree, ArrayList words) {
    if (Language.treebank().isPreterminal(tree)) {
      if (!Language.treebank().isNullElementPreterminal(tree)) {
	Word word = Language.treebank().makeWord(tree);
	words.add(word);
      }
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 0; i < treeListLen; i++) {
	collectWordObjects(treeList.get(i), words);
      }
    }
    return words;
  }

  private static void collectLeaves(Sexp tree, SexpList leaves,
				    boolean withTags) {
    if (Language.treebank().isPreterminal(tree)) {
      if (!Language.treebank().isNullElementPreterminal(tree)) {
	Word word = Language.treebank().makeWord(tree);
	Sexp leaf = null;
	if (withTags) {
	  SexpList tagList = new SexpList(1).add(word.tag());
	  leaf = new SexpList(2).add(word.word()).add(tagList);
	}
	else
	  leaf = word.word();

	leaves.add(leaf);
      }
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 0; i < treeListLen; i++) {
	collectLeaves(treeList.get(i), leaves, withTags);
      }
    }
  }

  /**
   * Adds the nonterminals in the specified tree to the specified set.
   *
   * @param counts the counts table to which to add the nonterminals present in
   * the specified tree
   * @param tree the tree from which to collect nonterminals
   * @param includeTags indicates whether to treat part of speech tags
   * as nonterminals
   * @return the specified counts table, modified to contain the counts of the
   * nonterminals present in the specified tree
   */
  public static CountsTable collectNonterminals(CountsTable counts, Sexp tree,
						boolean includeTags) {
    return collectNonterminals(counts, tree, includeTags, false);
  }

  /**
   * Adds the part of speech tags in the specified tree to the specified set.
   *
   * @param counts the counts table to which to add the tags present in the
   * specified tree
   * @param tree the tree from which to collect part of speech tags
   * @return the specified counts table, modified to contain the counts of the
   * part of speech tags present in the specified tree
   */
  public static CountsTable collectTags(CountsTable counts, Sexp tree) {
    return collectNonterminals(counts, tree, true, true);
  }

  private static CountsTable collectNonterminals(CountsTable counts, Sexp tree,
						 boolean includeTags,
						 boolean onlyTags) {
    if (Language.treebank().isPreterminal(tree)) {
      if (includeTags) {
	Word word = Language.treebank().makeWord(tree);
	counts.add(word.tag());
      }
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      if (!onlyTags)
	counts.add(treeList.get(0));
      for (int i = 0; i < treeListLen; i++) {
	collectNonterminals(counts, treeList.get(i), includeTags, onlyTags);
      }
    }
    return counts;
  }

  /**
   * Returns a string containing the pretty-printed version of the specified
   * parse tree.
   *
   * @param tree the tree to pretty-print
   * @return a string containing the pretty-printed version of the specified
   * parse tree.
   */
  public static String prettyPrint(Sexp tree) {
    StringBuffer sb = new StringBuffer();
    prettyPrint(tree, sb, 0);
    return sb.toString();
  }
  private static void prettyPrint(Sexp tree, StringBuffer sb, int level) {
    for (int i = 0; i < level; i++)
      sb.append("  ");
    if (Language.treebank().isPreterminal(tree)) {
      sb.append(tree.toString());
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      sb.append("(").append(treeList.symbolAt(0));
      boolean prevChildWasSmall = true;
      for (int i = 1; i < treeListLen; i++) {
	Sexp child = treeList.get(i);
	if (prevChildWasSmall &&
	    (child.isSymbol() || Language.treebank().isPreterminal(child))) {
	  sb.append(" ").append(child);
	  prevChildWasSmall = true;
	}
	else {
	  sb.append("\n");
	  prettyPrint(treeList.get(i), sb, level + 1);
	  prevChildWasSmall = false;
	}
      }
      sb.append(")");
    }
    else
      sb.append(tree);
  }

  /**
   * Adds <code>value</code> to the set that is the vale of <code>key</code>
   * in <code>map</code>; creates this set if a mapping doesn't already
   * exist for <code>key</code>.
   *
   * @param map the map to be updated
   * @param key the key in <code>map</code> whose value set is to be updated
   * @param value the value to be added to <code>key</code>'s value set
   */
  public final static <K, V> void addToValueSet(Map<K, Set<V>> map,
						K key,
						V value) {
    Set<V> valueSet = map.get(key);
    if (valueSet == null) {
      valueSet = new HashSet<V>();
      map.put(key, valueSet);
    }
    valueSet.add(value);
  }

  /**
   * Returns a new {@link SexpTokenizer} instance where the &ldquo;ordinary
   * characters&rsquo; (metacharacters) are '[' and ']'.
   *
   * @param inStream the character stream from which to read IBM-format
   *                 S-expressions/trees
   * @param comments whether semicolon-delimited line comments are allowed
   * @return a new {@link SexpTokenizer} instance capable of reading IBM-format
   *         S-expressions/trees
   */
  public static SexpTokenizer ibmTokenizer(Reader inStream, boolean comments) {
    return new SexpTokenizer(inStream, comments, ibmOrdinaryChars);
  }

  /**
   * Returns the S-expression for the IBM-format parse tree in the stream to be
   * tokenized by the specified {@link SexpTokenizer} instance. It is exptected
   * that the {@link SexpTokenizer} instnace was constructed using the {@link
   * #ibmTokenizer(java.io.Reader, boolean)} static factory method, or
   * equivalently uses '[' and ']' as metacharacters.
   *
   * @param tok the S-expression tokenizer where '[' and ']' are metacharacters
   * @return the S-expression for the IBM-format tree contained in the stream
   *         wrapped by the specified tokenizer
   *
   * @throws IOException if there is a problem reading from the underlying
   *                     character stream wrapped by the specified tokenizer
   */
  public static Sexp readIbmTree(SexpTokenizer tok) throws IOException {
    Sexp raw = Sexp.read(tok, '[', ']');
    return raw == null ? null : ibmToPenn(raw);
  }

  /**
   * A utility method that converts the specified IBM-format tree to a
   * Penn Treebank&ndash;format tree.
   * @param sexp the IBM-format tree to convert
   * @return a &ldquo;standard&rdquo; Penn Treebank&ndash;format tree
   */
  public static Sexp ibmToPenn(Sexp sexp) {
    if (sexp.isSymbol()) {
      String str = sexp.toString();
      Matcher m = underline.matcher(str);
      if (m.matches()) {
	SexpList list = new SexpList(2);
	list.add(Symbol.get(m.group(2)));
	list.add(Symbol.get(m.group(1)));
	return list;
      }
    }
    else {
      SexpList treeList = sexp.list();
      // in case extra info on final, redundant label, assign it to primary
      // label slot
      treeList.set(0, treeList.last());
      treeList.remove(treeList.length() - 1);
      for (int i = 0; i < treeList.length(); ++i) {
        /*
        if (treeList.get(i) == null) {
          System.err.println("problem printing " + i + "th tree from subtree " +
                             Util.prettyPrint(sexp));
        }
        */
        treeList.set(i, ibmToPenn(treeList.get(i)));
      }
    }
    return sexp;
  }
}