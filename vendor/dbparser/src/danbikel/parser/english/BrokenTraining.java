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
    package danbikel.parser.english;

import java.util.*;
import java.io.*;
import danbikel.parser.Constants;
import danbikel.parser.HeadFinder;
import danbikel.parser.Language;
import danbikel.parser.Treebank;
import danbikel.parser.Settings;
import danbikel.parser.Nonterminal;
import danbikel.lisp.*;

/**
 * Provides methods for language-specific processing of training parse trees.
 * This class&rsquo; primary purpose is simply to fill in the {@link
 * #argContexts}, {@link #semTagArgStopSet} and {@link #nodesToPrune} data
 * members using a metadata resource.  If this capability is desired in another
 * language package, this class may be subclassed.
 * <p/>
 * This class also re-defined the method {@link danbikel.parser.Training#addBaseNPs(Sexp)},
 * with an important change that is possibly only relevant to the Penn
 * Treebank.
 * <p/>
 * <b>Important note</b>: This class is similar to {@link Training}, except
 * that it is &ldquo;broken&rdquo; in the sense that instead of doing the
 * closest possible emulation of Collins&rsquo; parsing model, it only uses
 * details found in Collins&rsquo; published papers.  See <a
 * href="http://www.cis.upenn.edu/~dbikel/papers/collins-intricacies.pdf">Intricacies
 * of Collins&rdquo; Parsing Model</a> for details.
 */
public class BrokenTraining extends danbikel.parser.lang.AbstractTraining {
  // constants
  private final static String className = BrokenTraining.class.getName();
  private final static Symbol argContextsSym = Symbol.add("arg-contexts");
  private final static Symbol semTagArgStopListSym =
    Symbol.add("sem-tag-arg-stop-list");
  private final static Symbol nodesToPruneSym = Symbol.add("prune-nodes");
  private final static Symbol VP = Symbol.get("VP");

  // data members
  private Nonterminal nonterminal = new Nonterminal();

  /**
   * The default constructor, to be invoked by {@link danbikel.parser.Language}.
   * This constructor looks for a resource named by the property
   * <code>metadataPropertyPrefix + language</code>
   * where <code>metadataPropertyPrefix</code> is the value of
   * the constant {@link #metadataPropertyPrefix} and <code>language</code>
   * is the value of <code>Settings.get(Settings.language)</code>.
   * For example, the property for English is
   * <code>&quot;parser.training.metadata.english&quot;</code>.
   */
  public BrokenTraining() throws FileNotFoundException, IOException {
    String language = Settings.get(Settings.language);
    String metadataResource = Settings.get(metadataPropertyPrefix + language);
    InputStream is = Settings.getFileOrResourceAsStream(this.getClass(),
							metadataResource);
    int bufSize = Constants.defaultFileBufsize;
    SexpTokenizer metadataTok =
      new SexpTokenizer(is, Language.encoding(), bufSize);
    readMetadata(metadataTok);
  }


  public Sexp preProcess(Sexp tree) {
    super.preProcess(tree);
    fixSubjectlessSentences(tree);
    return tree;
  }

  /**
   * Marks certain nodes as arguments by appending a suffix to their
   * respective labels.  Collins&rsquo; implementation of his parsing model
   * has very specific conditions under which a nonterminal may be identified
   * as an argument; this implementation ignores some of those conditions,
   * making this one of the reasons this class is &ldquo;broken&rdquo;.
   * @param tree the tree in which to identify argument nodes
   * @return the specified tree modified so that argument nodes are
   * identified via a nonterminal augmentation (suffix)
   */
  public Sexp identifyArguments(Sexp tree) {
    if (treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();

      // first, make recursive call if not already at max recursion level
      for (int childIdx = 1; childIdx < treeListLen; childIdx++)
	identifyArguments(treeList.get(childIdx));

      Symbol parent =
	treebank.getCanonical(tree.list().first().symbol());
      SexpList candidatePatterns = (SexpList)argContexts.get(parent);

      int headIdx = headFinder.findHead(treeList);
      if (candidatePatterns != null) {
	Symbol headChild = (headIdx == 0 ? null :
			    treeList.get(headIdx).list().first().symbol());

	/*
	if (isCoordinatedPhrase(tree, headIdx))
	  return tree;
	*/

	// either the candidate pattern list has the form (head <int>) or
	// (head-left <list>) or (head-right <list>) or it's a list of actual
	// nonterminal labels
	if (candidatePatterns.first().symbol() == headSym ||
	    candidatePatterns.first().symbol() == headPreSym ||
	    candidatePatterns.first().symbol() == headPostSym) {
	  int argIdx = -1;
	  Symbol child = null;
	  boolean foundArg = false;

	  if (candidatePatterns.first().symbol() == headSym) {
	    String offsetStr = candidatePatterns.get(1).toString();
	    int headOffset = Integer.parseInt(offsetStr);
	    // IF there is a head and IF that head is not equal to parent (i.e.,
	    // if it's not a situation like (PP (PP ...) (CC and) (PP ...)) ) and
	    // if the headIdx plus the headOffset is still valid, then we've got
	    // an argument
	    argIdx = headIdx + headOffset;
	    if (headIdx > 0 &&
		treebank.getCanonical(headChild) != parent &&
		argIdx > 0 && argIdx < treeListLen) {
	      child = treeList.getChildLabel(argIdx);

	      // if the arg is actually a conjunction or punctuation,
	      // OR ANY KIND OF PRETERMINAL,
	      // if possible, find the first child to the left or right of argIdx that
	      // is not a preterminal
	      /*
	      if (treebank.isPreterminal(child) ||
		  treebank.isConjunction(child) || treebank.isPunctuation(child)) {
		if (headOffset > 0) {
		  for (int i = argIdx + 1; i < treeListLen; i++) {
		    if (!treebank.isPreterminal(treeList.get(i))) {
		      argIdx = i;
		      child = treeList.getChildLabel(argIdx);
		      break;
		    }
		  }
		}
		else {
		  for (int i = argIdx - 1; i >= 0; i--) {
		    if (!treebank.isPreterminal(treeList.get(i))) {
		      argIdx = i;
		      child = treeList.getChildLabel(argIdx);
		      break;
		    }
		  }
		}
	      }
	      foundArg = !treebank.isPreterminal(treeList.get(argIdx));
	      */
	      foundArg = true;
	    }
	  }
	  else {
	    SexpList searchInstruction = candidatePatterns;
	    int searchInstructionLen = searchInstruction.length();
	    boolean leftSide =
	      searchInstruction.symbolAt(0) == headPreSym;
	    boolean leftToRight =
	      searchInstruction.symbolAt(1) == Constants.firstSym;
	    boolean negativeSearch =
	      searchInstruction.symbolAt(2) == Constants.notSym;
	    int searchSetStartIdx = negativeSearch ? 3 : 2;
	    if (headIdx > 0 && treebank.getCanonical(headChild) != parent) {
	    //if (headIdx > 0) {
	      int increment = leftToRight ? 1 : -1;
	      int startIdx = -1, endIdx = -1;
	      if (leftSide) {
		startIdx = leftToRight ?  1            :  headIdx - 1;
		endIdx   = leftToRight ?  headIdx - 1  :  1;
	      }
	      else {
		startIdx = leftToRight ? headIdx + 1      :  treeListLen - 1;
		endIdx   = leftToRight ? treeListLen - 1  :  headIdx + 1;
	      }
	      // start looking one after (or before) the head index for
	      // first occurrence of a symbol in the search set, which is
	      // comprised of the symbols at indices 2..searchInstructionLen
	      // in the list searchInstruction
	      SEARCH:
	      for (int i = startIdx;
		   leftToRight ? i <= endIdx : i >= endIdx; i += increment) {
		Symbol currChild = treeList.getChildLabel(i);
		Symbol noAugChild = treebank.stripAugmentation(currChild);
		for (int j = searchSetStartIdx; j < searchInstructionLen; j++) {
		  Symbol searchSym = searchInstruction.symbolAt(j);
		  //if (noAugChild == searchSym) {
		  if (currChild == searchSym) {
		    if (negativeSearch)
		      continue SEARCH;
		    else {
		      argIdx = i;
		      child = currChild;
		      foundArg = true;
		      break SEARCH;
		    }
		  }
		}
		// if no match for any nt in search set and this is
		// negative search, we've found what we're looking for
		if (negativeSearch) {
		  argIdx = i;
		  child = currChild;
		  foundArg = true;
		  break SEARCH;
		}
	      }
	    }
	  }

	  if (foundArg) {
	    Nonterminal parsedChild =
	      treebank.parseNonterminal(child, nonterminal);
	    treebank.addAugmentation(parsedChild, defaultArgAugmentation);
	    treeList.setChildLabel(argIdx, parsedChild.toSymbol());
	  }
	}
	else {
	  // the candidate pattern list is a list of actual nonterminal labels

	  // the following line means we only find arguments in situations
	  // where the head child's label is different from the parent label
	  if (treebank.getCanonical(headChild) != parent) {
	  //if (true) {
	    relabelArgChildren(treeList, headIdx, candidatePatterns);
	    /*
	    for (int childIdx = 1; childIdx < treeListLen; childIdx++) {
	      if (!relabelHeadChildrenAsArgs)
		if (childIdx == headIdx)
		  continue;
	      Symbol child = treeList.getChildLabel(childIdx);
	      int candidateChildIdx =
		candidatePatterns.indexOf(treebank.getCanonical(child));
	      if (candidateChildIdx != -1) {
		Nonterminal parsedChild =
		  treebank.parseNonterminal(child, nonterminal);
		SexpList augmentations = parsedChild.augmentations;
		int augLen = augmentations.length();
		boolean isArg = true;
		for (int i = 0; i < augLen; i++) {
		  if (semTagArgStopSet.contains(augmentations.get(i))) {
		    isArg = false;
		    break;
		  }
		}
		if (isArg) {
		  treebank.addAugmentation(parsedChild, argAugmentation);
		  treeList.setChildLabel(childIdx, parsedChild.toSymbol());
		}
	      }
	    }
	    */
	  }
	}
      }
      else {
	// the current parent had no arg-finding rule, so we need to use
	// the default rule, if one exists
	candidatePatterns = (SexpList)argContexts.get(Constants.kleeneStarSym);
	if (candidatePatterns != null) {
	  relabelArgChildren(treeList, headIdx, candidatePatterns);
	}
      }
    }
    return tree;
  }

  /**
   * Unlike Mike's definition of a sentence for the purpose of relabeling
   * subjectless sentences, which includes any label that starts with 'S',
   * we strictly require here that the label strictly be S, or S with
   * some augmentations.  We also do *not* override the default definition
   * of relabelSubjectlessSentences(Sexp), since we are pretending we are
   * not aware that Mike defines subjectless sentences more strictly
   * than is conveyed by his thesis.
   * These are a couple of ways in which this class is &ldquo;broken&rdquo;.
   */
  protected boolean isTypeOfSentence(Symbol label) {
    return treebank.stripAugmentation(label) == treebank.sentenceLabel();
  }

  /**
   * This method has been written to do nothing to the specified tree.
   * This is one way in which this class is &ldquo;broken&rdquo;
   */
  public Sexp fixSubjectlessSentences(Sexp tree) {
    return tree;
  }

  /**
   * De-transforms NPs that were transformed by the
   * {@link danbikel.parser.Training#repairBaseNPs(Sexp)} method.  This
   * method is currently unused.
   * @param tree the tree whose NPs are to be de-transformed
   * @return a modified version of the specified tree
   */
  protected Sexp unrepairBaseNPs(Sexp tree) {
    if (tree.isSymbol())
      return tree;
    if (treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      // if we find a base NP followed by any type of S, unhook the S
      // from its parent and put as new final child of base NP subtree
      boolean thereAreAtLeastTwoChildren = treeList.length() > 2;
      if (thereAreAtLeastTwoChildren) {
	for (int i = 1; i < treeList.length() - 1; i++) {
	  Symbol currLabel = treeList.getChildLabel(i);
	  Symbol nextLabel = treeList.getChildLabel(i + 1);
	  if (currLabel == baseNP && isTypeOfSentence(nextLabel)) {
	    SexpList npbTree = treeList.listAt(i);
	    Sexp sentence = treeList.remove(i + 1); // unhook S from its parent
	    npbTree.add(sentence);
	    break;
	  }
	}
      }

      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++)
	unrepairBaseNPs(treeList.get(i));
    }
    return tree;
  }

  public void postProcess(Sexp tree) {
    //unrepairBaseNPs(tree);
    super.postProcess(tree);
  }

  /**
   * The following method has been overridden so that the two unpublished
   * conditions under which one needs to add a normal NP level are overlooked.
   * This is one reason why this class is &ldquo;broken&rdquo;.
   */
  protected boolean needToAddNormalNPLevel(Sexp grandparent,
					   int parentIdx, Sexp tree) {
    if (grandparent == null)
      return true;

    SexpList grandparentList = grandparent.list();
    if (!treebank.isNP(grandparentList.symbolAt(0)))
      return true;
    else
      return false;
    /*
    int headIdx = headFinder.findHead(grandparent);
    return (isCoordinatedPhrase(grandparent, headIdx) ||
	    (headIdx != parentIdx && grandparentList.symbolAt(0) != baseNP));
    */
  }

  /** Test driver for this class. */
  public static void main(String[] args) {
    String filename = null;
    boolean raisePunc = false, idArgs = false, subjectlessS = false;
    boolean stripAug = false, addBaseNPs = false;

    for (int i = 0; i < args.length; i++) {
      if (args[i].charAt(0) == '-') {
	if (args[i].equals("-r"))
	  raisePunc = true;
	else if (args[i].equals("-i"))
	  idArgs = true;
	else if (args[i].equals("-s"))
	  subjectlessS = true;
	else if (args[i].equals("-a"))
	  stripAug = true;
	else if (args[i].equals("-n"))
	  addBaseNPs = true;
      }
      else
	filename = args[i];
    }

    if (filename == null) {
      System.err.println("usage: [-risan] <filename>\n" +
			 "where\n\t" +
			 "-r: raise punctuation\n\t" +
			 "-i: identify arguments\n\t" +
			 "-s: relabel subjectless sentences\n\t" +
			 "-a: strip augmentations\n\t" +
			 "-n: add/relabel base NPs");
      System.exit(1);
    }

    Training training = (Training)Language.training();
    training.printMetadata();

    try {
      SexpTokenizer tok = new SexpTokenizer(filename, Language.encoding(),
					    Constants.defaultFileBufsize);
      Sexp curr = null;
      while ((curr = Sexp.read(tok)) != null) {
	if (raisePunc)
	  System.out.println(training.raisePunctuation(curr));
	if (idArgs)
	  System.out.println(training.identifyArguments(curr));
	if (subjectlessS)
	  System.out.println(training.relabelSubjectlessSentences(curr));
	if (stripAug)
	  System.out.println(training.stripAugmentations(curr));
	if (addBaseNPs)
	  System.out.println(training.addBaseNPs(curr));
      }
      System.out.println("\n\n");
    }
    catch (UnsupportedEncodingException uee) {
      System.err.println(uee);
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(fnfe);
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
  }
}
