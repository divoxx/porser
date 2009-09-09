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

import java.io.*;
import java.util.*;
import danbikel.lisp.*;
import danbikel.parser.Treebank;
import danbikel.parser.Language;
import danbikel.parser.Settings;
import danbikel.parser.Constants;

/**
 * A class for determining the head node on the right-hand sides of Chinese
 * parse tree productions.
 */
public class HeadFinder extends danbikel.parser.lang.AbstractHeadFinder {
  Treebank treebank = Language.treebank();

  /**
   * Constructs an Chinese head-finding object, getting the name of the head
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
   * Constructs an Chinese head-finding object with the specified head table.
   */
  public HeadFinder(Sexp headTableSexp) {
    super(headTableSexp);
  }

  private Symbol unSubjectify(Symbol label) {
    String labelStr = label.toString();
    if (labelStr.endsWith("SBJ"))
      return Symbol.get(labelStr.substring(0, labelStr.length() - 3));
    else
      return label;
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
    for (int i = 0; i < rhsSize; i++) {
      Symbol canonical = treebank.getCanonical(rhs.get(i).symbol());
      //canonical = unSubjectify(canonical);
      rhs.set(i, canonical);
    }

    Symbol canonicalLHS = treebank.getCanonical(lhs);
    //canonicalLHS = unSubjectify(canonicalLHS);

    // find the default head using the canonical LHS and canonical RHS
    int defaultHead = defaultFindHead(canonicalLHS, rhs);

    // defaultFindHead returns a 1-based index, so we decrement to be 0-based
    int defaultHeadIdx = defaultHead - 1;

    return defaultHead;
  }

  /** A test driver for this class. */
  public static void main(String[] args) {
    Class thisClass = danbikel.parser.chinese.HeadFinder.class;

    String encoding = Language.encoding();

    String headTableFilename = null;
    String inputFilename = null;
    if (args.length == 2) {
      headTableFilename = args[0];
      inputFilename = args[1];
    }
    else if (args.length == 1) {
      headTableFilename = Settings.get(Settings.headTablePrefix +
				       Language.getLanguage());
      inputFilename = args[0];
    }
    else {
      System.out.println("usage: [head table] <input file>");
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

      System.out.println("\nHead-finding instructions:");

      Iterator it = hf.headFindInstructions.keySet().iterator();
      while (it.hasNext()) {
	Symbol head = (Symbol)it.next();
	System.out.print("(" + head + " ");
	HeadFindInstruction[] instructions =
	  (HeadFindInstruction[])hf.headFindInstructions.get(head);
	for (int i = 0; i < instructions.length; i++) {
	  System.out.print(" ");
	  System.out.print(instructions[i]);
	}
	System.out.println(")");
      }

      System.out.println("\n\n\nFile " + inputFilename + " with head nodes:\n");

      FileInputStream inputFile = new FileInputStream(inputFilename);
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
