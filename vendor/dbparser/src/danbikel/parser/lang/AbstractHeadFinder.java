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
    package danbikel.parser.lang;

import danbikel.lisp.*;
import danbikel.parser.*;
import java.io.*;
import java.util.*;

/**
 * Provides a default abstract implementation of the {@link HeadFinder}
 * interface.  Subclasses are encouraged to make use of the {@link
 * #defaultFindHead(Symbol,SexpList)} method, which finds a head according to
 * head rules that are gotten from a resource specified in the parser's
 * settings file.
 */
public abstract class AbstractHeadFinder implements HeadFinder, Serializable {
  // constants
  /**
   * The fallback default in case the property for specifying the path to
   * the head table file or resource is not set.
   */
  protected static final String fallbackDefaultHeadTableResource =
    "data/head-rules.lisp";
  /**
   * Constant to indicate a left-to-right scan (makes for more
   * readable code for this class and its subclasses).
   */
  protected static final boolean LEFT = Constants.LEFT;
  /**
   * Constant to indicate a right-to-left scan (makes for more
   * readable code for this class and its subclasses).
   */
  protected static final boolean RIGHT = Constants.RIGHT;
  /**
   * The wildcard character used in default head-finding instructions.
   * @see #readHeadTable
   */
  protected static final Symbol defaultSym = Constants.kleeneStarSym;
  /**
   * The character from a head table's head-finding instruction that indicates
   * a left-to-right scan.
   *
   * @see #readHeadTable
   */
  protected static final Symbol leftSym = Symbol.add("l");
  /**
   * The character from a head table's head-finding instruction that indicates
   * a right-to-left scan.
   *
   * @see #readHeadTable
   */
  protected static final Symbol rightSym = Symbol.add("r");
  /**
   * The augmentation for new pre-head nodes added by {@link
   * #addHeadInformation}.
   */
  public static final String preHeadSuffix = "-PRE";
  /**
   * The augmentation for new post-head nodes added by {@link
   * #addHeadInformation}.
   */
  public static final String postHeadSuffix = "-POST";
  /**
   * The augmentation for new head nodes added by {@link #addHeadInformation}.
   */
  public static final String headSuffix = "-HEAD";

  /**
   * Data structure for specifying a way to search for a head in a
   * grammar production: a set of symbols to scan for and the direction
   * of that scan.
   */
  protected final static class HeadFindInstruction implements Serializable {
    /**
     * The set of symbols to scan for.
     */
    protected Symbol[] scanSet;
    /**
     * The direction in which to scan: a value equal to {@link Constants#LEFT}
     * indicates a left-to-right scan, and a value equal to
     * {@link Constants#RIGHT} indicates a right-to-left scan.
     */
    protected boolean direction;

    /**
     * Constructs a new <code>HeadFindInstruction</code> object.
     * @param direction the direction in which to scan: a value equal to
     * {@link Constants#LEFT} indicates a left-to-right scan, and a value
     * equal to {@link Constants#RIGHT} indicates a right-to-left scan
     * @param scanSet the set of symbols to scan for
     */
    protected HeadFindInstruction(boolean direction, Symbol[] scanSet) {
      this.direction = direction;
      this.scanSet = scanSet;
    }

    /**
     * Converts this object to a human-readable string representation.
     * @return a human-readable string representation of this object
     */
    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append('(').append(direction == LEFT ? leftSym : rightSym);
      for (int i = 0; i < scanSet.length; i++)
	buf.append(' ').append(scanSet[i]);
      buf.append(')');
      return buf.toString();
    }
  }

  // data members
  private Nonterminal nt1 = new Nonterminal();
  private Nonterminal nt2 = new Nonterminal();

  /**
   * The map of parent nonterminals to their arrays of
   * {@link AbstractHeadFinder.HeadFindInstruction}.  When a head is being
   * found, each <code>HeadFindInstruction</code> is applied in order until
   * one succeeds.
   *
   * @see #readHeadTable
   */
  protected HashMap headFindInstructions = new HashMap();

  /**
   * The value of {@link Settings#headFinderWarnDefaultRule}, cached here
   * for readability and convenience.
   */
  protected boolean warnDefaultRule =
    Settings.getBoolean(Settings.headFinderWarnDefaultRule);

  /**
   * The probability that the head child of a production will be chosen
   * at random.
   *
   * @see Settings#headFinderRandomProb
   */
  protected double probRandom =
    Double.parseDouble(Settings.get(Settings.headFinderRandomProb));
  /**
   * Set to <tt>true</tt> if {@link #probRandom} is greater than 0.0; otherwise,
   * set to <tt>false</tt>.
   */
  protected boolean useRand = probRandom > 0.0;
  /**
   * This class&rsquo; random number generator.
   */
  protected Random rand;

  /**
   * Constructs a head-finding object, getting the name of the head
   * table from the value of
   * <code>Settings.get(Settings.headTablePrefix&nbsp;+&nbsp;language)</code>,
   * where <code>language</code> is the value of
   * <code>Settings.get(Settings.language)</code>.
   * The named head table is searched for in the locations that are searched
   * by the method {@link Settings#getFileOrResourceAsStream(Class,String)}.
   * See {@link #readHeadTable} for a BNF description of the syntax of head
   * table files.
   * <p>
   * This constructor will necessarily be invoked whenever the default
   * constructor of a language-specific <code>HeadFinder</code> is invoked,
   * which is done upon initialization of the <code>Language</code> class.
   *
   * @see #readHeadTable
   * @see Language
   * @see Settings#getFileOrResourceAsStream
   */
  protected AbstractHeadFinder() throws IOException, FileNotFoundException {
    String headTableProperty =
      Settings.headTablePrefix + Language.getLanguage();
    String headTableResource = Settings.get(headTableProperty);
    if (headTableResource == null) {
      System.err.println(getClass().getName() + ": warning: the property \"" +
			 headTableProperty + "\"" +
			 " was not set;\n\tusing fallback default " +
			 "\"" + fallbackDefaultHeadTableResource + "\"");
      headTableResource = fallbackDefaultHeadTableResource;
    }
    InputStream is = Settings.getFileOrResourceAsStream(this.getClass(),
							headTableResource);
    int bufSize = Constants.defaultFileBufsize;
    SexpTokenizer headTableTok =
      new SexpTokenizer(is, Language.encoding(), bufSize);
    Sexp headTable = Sexp.read(headTableTok);
    readHeadTable(headTable);
    if (useRand)
      rand = new Random(System.currentTimeMillis());
  }

  /**
   * Constructs a head-finding object with the specified head table.
   */
  public AbstractHeadFinder(Sexp headTableSexp) {
    readHeadTable(headTableSexp);
    if (useRand)
      rand = new Random(System.currentTimeMillis());
  }

  /**
   * Provides a default mechanism to use the head table to find a head in the
   * specified grammar production.
   * <p>
   * <b>N.B.</b>: The list <code>rhs</code>
   * is, as are all <code>SexpList</code> objects, 0-indexed; however, the
   * return value of this method is meant to map to the children of a node in an
   * S-expression tree, where child indices begin at 1.  Therefore,
   * this method returns a 1-based index.
   * @param lhs the left-hand side of a grammar production
   * @param rhs a list of symbols representing the right-hand side of a
   * grammar production
   * @return the 1-based index of the nonterminal in <code>rhs</code> that
   * is the head, according to the head table of this head-finding object
   *
   * @see Settings#headFinderWarnDefaultRule
   */
  protected int defaultFindHead(Symbol lhs, SexpList rhs) {
    if (useRand && rand.nextDouble() <= probRandom &&
        !(Language.treebank().isNP(lhs) &&
          !Language.treebank().isBaseNP(lhs))) {
      // return a randomly-selected head index
      int rhsLen = rhs.length();
      return rand.nextInt(rhsLen) + 1;
    }

    HeadFindInstruction[] instructions =
      (HeadFindInstruction[])headFindInstructions.get(lhs);

    if (instructions == null) {
      if (warnDefaultRule)
	System.err.println(getClass().getName() + ": warning: couldn't find " +
			   "rule for " + lhs + " -> " + rhs);
      instructions =
	(HeadFindInstruction[])headFindInstructions.get(defaultSym);

      if (instructions == null)
	System.err.println(getClass().getName() + ": error: couldn't find " +
			   "rule for " + lhs + " -> " + rhs +
			   "\n\tand there is no default rule");
    }

    int headIdx = -1;
    int rightmostIdx = rhs.size() - 1;
    boolean direction = RIGHT;
    for (int i = 0; i < instructions.length; i++) {
      direction = instructions[i].direction;
      Symbol[] scanSet = instructions[i].scanSet;
      if (scanSet.length == 0) {
	headIdx = (direction == LEFT ? 0 : rightmostIdx);
	break;
      }
      headIdx = scan(direction, rhs, scanSet);
      if (headIdx >= 0)
	break;
    }
    // THIS IS A HACK TO HANDLE CASE WHEN ALL OF THE HEAD-FINDING INSTRUCTIONS
    // FAIL: WE PICK THE CHILD ACCORDING TO THE SCAN DIRECTION SPECIFIED
    // BY THE FINAL HEAD-FINDING INSTRUCTION
    if (headIdx < 0)
      headIdx = (direction == LEFT ? 0 : rightmostIdx);
    return headIdx + 1;
  }

  /**
   * Finds the head for the production at the root of the specified subtree.
   * The general contract of this method is to extract the root nonterminal
   * label of the specified tree, create a list of the child nonterminal
   * labels and call {@link #findHead(Sexp,Symbol,SexpList)}.
   * <br><b>Efficiency note</b>: This default implementation creates a new
   * <code>SexpList</code> containing the labels of the children of the root of
   * <code>tree</code> and then calls {@link #findHead(Sexp,Symbol,SexpList)}.
   *
   * @param tree the subtree for whose root production to find the head
   * @return the 1-based index of the head child of the production at the
   * root of the specified subtree
   *
   * @see #findHead(Sexp,Symbol,SexpList)
   */
  public int findHead(Sexp tree) {
    SexpList treeList = tree.list();
    int treeListLength = treeList.length();

    SexpList children = new SexpList(treeListLength - 1);
    for (int i = 1; i < treeListLength; i++)
      children.add(treeList.getChildLabel(i));

    return findHead(tree, treeList.first().symbol(), children);
  }

  /**
   * Finds the head for the grammar production <code>lhs -> rhs</code>.  This
   * method may destructively modify <code>rhs</code>.
   * @param tree the original subtree in which to find the head child, or
   * <code>null</code> if the subtree is not available
   * @param lhs the nonterminal label that is the left-hand side of a grammar
   * production
   * @param rhs a list of symbols that is the right-hand side of a grammar
   * production
   * @return the 1-based index of the head child in <code>rhs</code>
   */
  public abstract int findHead(Sexp tree, Symbol lhs, SexpList rhs);

  /**
   * Perform head-finding in <code>tree</code>, augmenting nodes that
   * are the head children of their respective parents.
   * This method is useful for head-finding debugging.
   *
   * @return a reference to the modified <code>tree</code> object
   *
   * @see #headSuffix()
   */
  public Sexp addHeadInformation(Sexp tree) {
    if (tree.isSymbol() ||
	Language.treebank().isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      /*
      SexpList treeList = tree.list();
      int treeListLength = treeList.length();
      Symbol parent = treeList.first().symbol();
      SexpList result = new SexpList();
      result.add(parent);

      SexpList children = new SexpList(treeListLength - 1);
      for (int i = 1; i < treeListLength; i++)
	children.add(treeList.getChildLabel(i));

      int headIdx = findHead(tree, treeList.first().symbol(), children);

      if (headIdx == 0)
	throw new RuntimeException(getClass().getName() + ": error: " +
				   "couldn't find head for " + tree);

      if (headIdx > 1) {
	SexpList preHead = new SexpList(headIdx);
	preHead.add(Symbol.add(parent + preHeadSuffix));
	for (int i = 1; i < headIdx; i++)
	  preHead.add(addHeadInformation(treeList.get(i)));
	result.add(preHead);
      }

      SexpList head = new SexpList(2);
      head.add(Symbol.add(parent + headSuffix));
      head.add(addHeadInformation(treeList.get(headIdx)));
      result.add(head);

      if (headIdx < treeListLength - 1) {
	SexpList postHead = new SexpList(treeListLength - headIdx - 1);
	postHead.add(Symbol.add(parent + postHeadSuffix));
	for (int i = headIdx + 1; i < treeListLength; i++)
	  postHead.add(addHeadInformation(treeList.get(i)));
	result.add(postHead);
      }
      return result;
      */
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();

      // first, find heads for all nodes in this subtree
      for (int i = 1; i < treeListLen; i++)
	addHeadInformation(treeList.get(i));

      // now, modify this subtree's head child's label
      Symbol parent = treeList.first().symbol();
      SexpList children = new SexpList(treeListLen - 1);
      for (int i = 1; i < treeListLen; i++)
	children.add(treeList.getChildLabel(i));

      int headIdx = findHead(tree, treeList.first().symbol(), children);

      if (headIdx == 0)
	throw new RuntimeException(getClass().getName() + ": error: " +
				   "couldn't find head for " + tree);

      Symbol oldHead = treeList.getChildLabel(headIdx);
      treeList.setChildLabel(headIdx, Symbol.add(oldHead + headSuffix()));
      return tree;
    }
    return null;
  }

  /**
   * Returns the string <code>&quot;-HEAD&quot;</code>. If this conflicts
   * with an existing nonterminal augmentation for a particular treebank or
   * an augmentation added by {@link Training} during preprocessing, this
   * method should be overridden.
   *
   * @return the string <code>&quot;-HEAD&quot;</code>
   */
  public String headSuffix() { return headSuffix; }

  /**
   * Reads the head table contained in the specified S-expression.
   * The format for the head table is as follows:
   * <pre>(&lt;headrule&gt;+)</pre>
   * where
   * <blockquote>
   * <table border=1>
   * <tr><td><tt>&lt;headrule&gt;</tt></td><td><tt>::=</tt></td>
   *     <td><tt>(&lt;parent&gt; &lt;instruction&gt;+)</tt></td></tr>
   * <tr><td><tt>&lt;parent&gt;</tt></td><td><tt>::=</tt></td>
   *     <td>the parent whose head child is to be found
   *         or the symbol <tt>&quot;&#042;&quot;</tt><br>
   *         (which indicates a default rule)</td></tr>
   * <tr><td><tt>&lt;instruction&gt;</tt></td><td><tt>::=</tt></td>
   *     <td><tt>(&lt;direction&gt; &lt;scanelement&gt;&#042;)</tt></td></tr>
   * <tr><td><tt>&lt;direction&gt;</tt></td><td><tt>::=</tt></td>
   *     <td><tt>l | r</tt><br>
   *         where <tt>l</tt> indicates left-to-right,
   *         <tt>r</tt> right-to-left</td></tr>
   * <tr><td><tt>&lt;scanelement&gt;</tt></td><td><tt>::=</tt></td>
   *     <td>a child nonterminal label to scan for in the specified
   *         direction</td></tr>
   * </table>
   * </blockquote>
   * For a particular <tt>&lt;parent&gt;</tt>, each
   * <tt>&lt;instruction&gt;</tt> is applied in the order in which it
   * appears in the head table, looking for the first element of the right-hand
   * side of a grammar production that is a member of the <tt>scanelement</tt>
   * list, and the head is the result of the first instruction that succeeds.
   * If the <code>scanelement</code> list is empty, then the rule indicates to
   * choose the first element in its scan (a default instruction).  If none
   * of the head-finding instructions succeeds for a particular parent,
   * an implicit default instruction is added that is of the form <tt>(d)</tt>,
   * where <tt>d</tt> is the direction specified by the last instruction for
   * the parent.
   * <p>
   * <b>Bugs</b>: We eventually need to replace default instructions such as
   * <tt>(l)</tt> with a syntax that includes the wildcard character, such as
   * <tt>(l &#042;)</tt>, which will be more consistent with the default
   * rule, where the wildcard matches any parent.  Also, we should possibly
   * require default instructions, instead of allowing them to be implicit.
   *
   * @see #findHead(Sexp,Symbol,SexpList)
   */
  protected void readHeadTable(Sexp headTableSexp) {
    if (!headTableSexp.isList())
      throw new IllegalArgumentException(getClass() + ": non-list head table");
    SexpList headTable = headTableSexp.list();
    for (int headIdx = 0; headIdx < headTable.length(); headIdx++) {
      SexpList instrListSexp = (SexpList)headTable.get(headIdx);
      Symbol head = (Symbol)instrListSexp.get(0);
      // instructions is a list of ordered head-finding instructions
      HeadFindInstruction[] instructions =
	new HeadFindInstruction[instrListSexp.length() - 1];
      // construct an array of HeadFindInstruction for the current instruction
      // list for this head
      for (int instrIdx = 1;
	   instrIdx < instrListSexp.length();
	   instrIdx++) {
	SexpList instrSexp = (SexpList)instrListSexp.get(instrIdx);
	Symbol directionSym = (Symbol)instrSexp.get(0);
	boolean direction = (directionSym == leftSym ? LEFT : RIGHT);
	Symbol[] scanSet = new Symbol[instrSexp.length() - 1];
	for (int scanSetIdx = 1;
	     scanSetIdx < instrSexp.length();
	     scanSetIdx++)
	  scanSet[scanSetIdx - 1] = (Symbol)instrSexp.get(scanSetIdx);
	instructions[instrIdx - 1] = new HeadFindInstruction(direction,
							     scanSet);
      }
      headFindInstructions.put(head, instructions);
    }
  }

  /**
   * Scans the RHS of a production from left to right, returning the index
   * of the first nonterminal that is in the <code>matchTags</code> array.
   *
   * @param rhs a list of symbols representing the right-hand side (RHS) of
   * a grammar production
   * @param matchTags an array of symbols representing the set of nonterminal
   * labels to try to match in <code>rhs</code>
   * @return the index of the first nonterminal in <code>rhs</code> that is in
   * the set of nonterminals contained in <code>matchTags</code> when scanning
   * from left to right, or -1 if there is no such matching nonterminal
   */
  protected int scanLeftToRight(SexpList rhs, Symbol[] matchTags) {
    for (int i = 0; i < rhs.size(); i++) {
      Symbol tag = (Symbol)rhs.get(i);
      if (tagMatches(tag, matchTags))
	return i;
    }
    return -1;
  }

  /**
   * Scans the RHS of a production from right to left, returning the index
   * of the first nonterminal that is in the <code>matchTags</code> array.
   *
   * @param rhs a list of symbols representing the right-hand side (RHS) of
   * a grammar production
   * @param matchTags an array of symbols representing the set of nonterminal
   * labels to try to match in <code>rhs</code>
   * @return the index of the first nonterminal in <code>rhs</code> that is in
   * the set of nonterminals contained in <code>matchTags</code> when scanning
   * from right to left, or -1 if there is no such matching nonterminal
   */
  protected int scanRightToLeft(SexpList rhs, Symbol[] matchTags) {
    for (int i = rhs.size() - 1; i >= 0; i--) {
      Symbol tag = (Symbol)rhs.get(i);
      if (tagMatches(tag, matchTags))
	return i;
    }
    return -1;
  }

  /**
   * Scans the RHS of a production in the specified direction.
   *
   * @param direction the direction of scan: {@link #LEFT} for left-to-right,
   * {@link #RIGHT} for right-to-left.
   * @param rhs a list of symbols representing the right-hand side (RHS) of
   * a grammar production
   * @param matchTags an array of symbols representing the set of nonterminal
   * labels to try to match in <code>rhs</code>
   * @return the index of the first nonterminal in <code>rhs</code> that is
   * in the set of nonterminals contained in <code>matchTags</code>, or -1
   * if there is no such matching nonterminal
   */
  protected int scan(boolean direction, SexpList rhs, Symbol[] matchTags) {
    if (direction == LEFT)
      return scanLeftToRight(rhs, matchTags);
    else
      return scanRightToLeft(rhs, matchTags);
  }

  /**
   * A helper method that returns <code>true</code> if any of the nonterminals
   * in <code>matchTags</code> is <code>tag</code> and returns
   * <code>false</code> otherwise.
   *
   * @param tag the tag to match
   * @param matchTags an array of symbols
   */
  protected boolean tagMatches(Symbol tag, Symbol matchTags[]) {
    Nonterminal tagNT = null;
    for (int j = 0; j < matchTags.length; j++) {
      if (tag == matchTags[j])
        return true;
      else {
        // let's see if the current match tag subsumes the tag in question
        if (tagNT == null)
          tagNT = Language.treebank().parseNonterminal(tag, nt1);
        Nonterminal matchTagNT =
          Language.treebank().parseNonterminal(matchTags[j], nt2);
        if (matchTagNT.subsumes(tagNT))
          return true;
      }
    }
    return false;
  }
}
