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
    package portuguese;

import java.io.*;
import java.util.*;
import danbikel.lisp.*;
import danbikel.parser.Treebank;
import danbikel.parser.Language;
import danbikel.parser.Settings;
import danbikel.parser.Constants;

/**
 * Reads a series of heuristics to determine the distinguished head child of a
 * context-free production (a parent and its ordered list of children in
 * a syntax tree).  Because a default head-finding rule is always supplied
 * even if one is not specified, it is guaranteed that every tree in which
 * head-finding is performed will have a unique head-lexicalization.
 */
public class HeadFinder extends danbikel.parser.lang.AbstractHeadFinder {
  Treebank treebank = Language.treebank();

  /**
   * Constructs an English head-finding object, getting the name of the head
   * table from the value of
   * <code>Settings.get(Settings.headTablePrefix&nbsp;+&nbsp;Language.getLanguage())</code>.
   * The named head table is searched for in the locations that are searched
   * by the method {@link Settings#getFileOrResourceAsStream(Class,String)}.
   * <p>
   * This constructor will be invoked upon the initialization of the
   * <code>Language</code> class.
   *
   * @see Settings#getFileOrResourceAsStream(Class, String)
   */
  public HeadFinder() throws IOException, FileNotFoundException {
    super();
  }

  /**
   * Constructs an English head-finding object with the specified head table.
   */
  public HeadFinder(Sexp headTableSexp) {
    super(headTableSexp);
  }

  /**
   * Finds the head for the grammar production <code>lhs -> rhs</code>.  This
   * method destructively modifies <code>rhs</code> to contain only
   * the canonical version of each of its symbols.  This method then calls
   * {@link #defaultFindHead(Symbol,SexpList)}, using the canonical version
   * of the <code>lhs</code> symbol for the first argument.  If the default
   * head index points to a nonterminal in a coordinating relationship, that
   * is, if the default head index is greater than 2 and the previous
   * nonterminal is a conjunction, then the index returned is the default
   * head index minus 2.
   *
   * @param tree the original subtree in which to find the head child, or
   * <code>null</code> if the subtree is not available
   * @param lhs the nonterminal label that is the left-hand side of a grammar
   * production
   * @param rhs a list of symbols that is the right-hand side of a grammar
   * production
   * @return the 1-based index of the head child in <code>rhs</code>
   *
   * @see Treebank#isConjunction(Symbol)
   */
  public int findHead(Sexp tree, Symbol lhs, SexpList rhs) {
    // destructively modify rhs, resetting all elements to be their canonicals
    int rhsSize = rhs.size();
    for (int i = 0; i < rhsSize; i++)
      rhs.set(i, treebank.getCanonical(rhs.get(i).symbol()));

    Symbol canonicalLHS = treebank.getCanonical(lhs);

    // find the default head using the canonical LHS and canonical RHS
    int defaultHead = defaultFindHead(canonicalLHS, rhs);

    // defaultFindHead returns a 1-based index, so we decrement to be 0-based
    int defaultHeadIdx = defaultHead - 1;

    // before returning, we check for the coordinated phrase case
    if (defaultHeadIdx >= 2 &&
	treebank.isConjunction(rhs.get(defaultHeadIdx - 1).symbol()) &&
        !Language.treebank().isBaseNP(lhs)) {
    /*
    if (defaultHeadIdx >= 2 &&
	treebank.isConjunction(rhs.get(defaultHeadIdx - 1).symbol())) {
    */
      // first, try to find same label farther back
      /*
      int firstOccurrenceIdx = rhs.indexOf(rhs.get(defaultHeadIdx));
      if (firstOccurrenceIdx < defaultHeadIdx) {
        defaultHead = firstOccurrenceIdx + 1;
      }
      // if that fails, if we can find a non-punctuation and non-conjunction
      // symbol to the left of the default head, set default head to be that;
      // otherwise, leave default head where it is
      else {
      */
        for (int i = defaultHeadIdx - 2; i >= 0; i--) {
          Symbol curr = rhs.symbolAt(i);
          if (!treebank.isPunctuation(curr) && !treebank.isConjunction(curr)) {
            defaultHead = i + 1;  // need to return 1-based index, so we add 1
            break;
          }
        }
      //}
      //defaultHead -= 2;
    }

    return defaultHead;
  }

  /** A test driver for this class. */
  public static void main(String[] args) {
    Class thisClass = portuguese.HeadFinder.class;

    String encoding = Language.encoding();

    String headTableFilename = null;
    String inputFilename = null;
    switch (args.length) {
    case 2:
      headTableFilename = args[0];
      inputFilename = args[1];
      break;
    case 1:
      headTableFilename = Settings.get(Settings.headTablePrefix +
				       Language.getLanguage());
      inputFilename = args[0];
      break;
    case 0:
      headTableFilename = Settings.get(Settings.headTablePrefix +
                                       Language.getLanguage());
      break;
    default:
      System.err.println("usage: [head table] <input file>");
      System.exit(1);
    }

    try {
      InputStream is = Settings.getFileOrResourceAsStream(thisClass,
							  headTableFilename);
      int bufSize = Constants.defaultFileBufsize;
      SexpTokenizer headTableTok =
	new SexpTokenizer(is, Language.encoding(), bufSize);
      Sexp headTable = Sexp.read(headTableTok);
      HeadFinder hf = new HeadFinder(headTable);

      System.err.println("\nHead-finding instructions:");

      Iterator it = hf.headFindInstructions.keySet().iterator();
      while (it.hasNext()) {
	Symbol head = (Symbol)it.next();
	System.err.print("(" + head + " ");
	HeadFindInstruction[] instructions =
	  (HeadFindInstruction[])hf.headFindInstructions.get(head);
	for (int i = 0; i < instructions.length; i++) {
	  System.err.print(" ");
	  System.err.print(instructions[i]);
	}
	System.err.println(")");
      }

      System.err.println("\n\n\nFinding heads in " +
                         (inputFilename == null ?
                          "sentences from standard input" : inputFilename) +
                         ":\n");

      InputStream inputFile =
        inputFilename == null ? System.in : new FileInputStream(inputFilename);
      BufferedReader inputReader =
	new BufferedReader(new InputStreamReader(inputFile,
						 encoding));
      SexpTokenizer inputTok = new SexpTokenizer(inputReader);
      Sexp tree = null;
      while ((tree = Sexp.read(inputTok)) != null)
	System.out.println(hf.addHeadInformation(tree));
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(fnfe);
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
  }
}
