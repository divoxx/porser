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
import java.io.*;
import java.util.HashSet;

/**
 * Reads in a file of gold-standard parsed sentences and a file of
 * machine-parsed sentences, replacing every occurrence of
 * <tt>null</tt> in the machine-parsed file with the original
 * sentence and then adding fake parts of speech for each word of that
 * original sentence that will not to be deleted by the scorer.
 */
public class AddFakePos {

  // constants
  private final static Symbol nullSym = Symbol.add("null");
  private final static Symbol fakePos = Symbol.add("foo");

  // static data members
  private static HashSet posPrunes = new HashSet();
  static {
    String[] prunes = {",", ":", "``", "''", "."};
    for (int i = 0; i < prunes.length; i++)
      posPrunes.add(Symbol.add(prunes[i]));
  }
  private static SexpList goldPreterms = new SexpList(200);
  private static SexpList goldWords = new SexpList(200);
  private static SexpList testPreterms = new SexpList(200);
  private static SexpList testWords = new SexpList(200);

  private AddFakePos() {}

  private final static void usage() {
    System.err.println("usage: <gold parse file> <filename> [nodes to prune]+");
    System.exit(1);
  }

  /**
   * Reads in parse trees either from a specified file or from standard input
   * and adds fake parts of speech to raw (un-parsed) sentences.
   * <pre>usage: &lt;gold parse file&gt; &lt;filename&gt;</pre>
   * where specifying <tt>-</tt> for the filename indicates to
   * read the parser output from standard input.
   */
  public static void main(String[] args) {
    InputStream goldIn = null;
    if (args.length < 2)
      usage();

    File goldFile = new File(args[0]);
    try { goldIn = new FileInputStream(goldFile); }
    catch (FileNotFoundException fnfe) {
      System.err.println("error: file \"" + args[0] + "\" does not exist");
      System.exit(1);
    }
    InputStream in = null;
    if (args[1].equals("-")) {
      in = System.in;
    }
    else {
      File file = new File(args[1]);
      try { in = new FileInputStream(file); }
      catch (FileNotFoundException fnfe) {
	System.err.println("error: file \"" + args[1] + "\" does not exist");
	System.exit(1);
      }
    }
    if (args.length > 2) {
      posPrunes.clear();
      for (int i = 2; i < args.length; i++)
	posPrunes.add(Symbol.add(args[i]));
    }
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    SexpTokenizer tok = null;
    SexpTokenizer goldTok = null;
    String enc = Language.encoding();
    try {
      tok = new SexpTokenizer(in, enc, 8192);
      goldTok = new SexpTokenizer(goldIn, enc, 8192);
    }
    catch (UnsupportedEncodingException uee) {
      System.err.println("error: encoding \"" + enc + "\" not supported");
      System.exit(1);
    }

    Sexp curr = null, goldCurr = null;
    try {
      for (int sentIdx = 1; (curr = Sexp.read(tok)) != null; sentIdx++) {
	goldCurr = Sexp.read(goldTok);
	goldCurr = Language.training().removeNullElements(goldCurr);
	goldPreterms.clear();
	goldWords.clear();
	collectPreterms(goldCurr, goldPreterms, goldWords);
	if (goldCurr == null) {
	  System.err.println("error: ran out of sentences in gold file!");
	  break;
	}
	if (curr == nullSym)
	  curr = goldWords;
	addFakePos(curr);

	/*
	// now munge pos tags of gold and curr if one is a "node for pruning"
	// and the other isn't
	curr = Language.training().removeNullElements(curr);
	testPreterms.clear();
	testWords.clear();
	collectPreterms(curr, testPreterms, testWords);
        Set mungeTagIndices = new HashSet();
	if (testPreterms.length() == goldPreterms.length()) {
	  int len = testPreterms.length();
	  for (int idx = 0; idx < len; idx++) {
	    boolean testPrune = prune(testPreterms.get(idx));
	    boolean goldPrune = prune(goldPreterms.get(idx));
	    if ((testPrune && !goldPrune) || (goldPrune && !testPrune))
	      mungeTagIndices.add(new Integer(idx));
	  }
	  
	}
	else {
	  System.err.println("error: length mismatch (gold=" +
			     goldPreterms.length() + "|test=" +
			     testPreterms.length() + " for sentence No. " +
			     sentIdx);
	}
	*/

	System.out.println(curr);
      }
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
  }

  private static void collectPreterms(Sexp tree,
				      SexpList preterms,
				      SexpList words) {
    if (Language.treebank().isPreterminal(tree)) {
      preterms.add(tree);
      Word word = Language.treebank().makeWord(tree);
      words.add(word.word());
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeLen = treeList.length();
      for (int i = 0; i < treeLen; i++)
	collectPreterms(treeList.get(i), preterms, words);
    }
  }

  private static void addFakePos(Sexp sent) {
    if (sent.isList() && sent.list().isAllSymbols()) {
      SexpList sentList = sent.list();
      int sentLen = sentList.length();
      for (int i = 0; i < sentLen; i++) {
	Sexp word = sentList.get(i);
	SexpList preterm = goldPreterms.listAt(i);
	Sexp pos = prune(preterm) ? preterm.get(0) : fakePos;
	SexpList wordList = new SexpList(2).add(pos).add(word);
	sentList.set(i, wordList);
      }
    }
  }
  private final static boolean prune(SexpList preterm) {
    return posPrunes.contains(preterm.get(0));
  }
}
