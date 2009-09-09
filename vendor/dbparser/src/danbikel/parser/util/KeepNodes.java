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

import danbikel.lisp.Sexp;
import danbikel.lisp.SexpList;
import danbikel.lisp.Symbol;
import danbikel.lisp.SexpTokenizer;
import danbikel.parser.Nonterminal;
import danbikel.parser.Language;
import danbikel.parser.Constants;

import java.util.List;
import java.util.ArrayList;
import java.io.*;

/**
 * Provides a utility method, as well as a {@link #main} method that invokes it,
 * to keep only certain nodes in a tree, deleting all others, except the root
 * node, which is never deleted.
 */
public class KeepNodes {

  public static Sexp keepNodes(Sexp tree, List<Nonterminal> nodesToKeep) {
    if (Language.treebank().isPreterminal(tree)) {
      return tree;
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      for (int i = 1; i < treeList.length(); i++) {
	SexpList child = treeList.listAt(i);
	if (!Language.treebank().isPreterminal(child)) {
	  Nonterminal childLabel =
	    Language.treebank().parseNonterminal(child.symbolAt(0));
	  if (!keepNode(childLabel, nodesToKeep)) {
	    // all of child's children become children of this tree, inserted
	    // right where the child is, and the child is removed from this tree
	    treeList.remove(i);
	    SexpList childrenOfChild = new SexpList(child);
	    childrenOfChild.remove(0);
	    treeList.addAll(i, childrenOfChild);
	    i--;
	  }
	}
	keepNodes(child, nodesToKeep);
      }
    }
    return tree;
  }

  private static boolean keepNode(Nonterminal node,
				  List<Nonterminal> nodesToKeep) {
    boolean keep = false;
    //System.out.println("Testing " + node);
    for (Nonterminal nodeToKeep : nodesToKeep) {
      if (nodeToKeep.subsumes(node)) {
	keep = true;
	break;
      }
    }
    //System.out.println(keep ? "Keeping!" : "Not keeping!");
    return keep;
  }

  private static String[] usageMsg = {
    "usage: <input file> <node to keep>+\n",
    "where\n",
    "\t<input file> may be \"-\" to specify stdin\n",
    "\t<node to keep> may be a node pattern, such as \"*-SBJ\"\n"
  };

  public static void usage() {
    for (String usage : usageMsg) {
      System.out.print(usage);
    }
    System.out.flush();
    System.exit(1);
  }


  public static void main(String[] args) {
    if (args.length < 2) {
      usage();
    }

    List<Nonterminal> nodesToKeep = new ArrayList<Nonterminal>();
    String inFile = args[0];
    for (int i = 1; i < args.length; i++) {
      Symbol nodeSym = Symbol.get(args[i]);
      nodesToKeep.add(Language.treebank().parseNonterminal(nodeSym));
    }
    try {
      String enc = Language.encoding();
      InputStream is =
	inFile.equals("-") ? System.in : new FileInputStream(inFile);
      SexpTokenizer tok =
	new SexpTokenizer(is, enc, Constants.defaultFileBufsize);
      OutputStreamWriter osw = new OutputStreamWriter(System.out, enc);
      BufferedWriter writer = new BufferedWriter(osw);
      Sexp curr = null;
      while ((curr = Sexp.read(tok)) != null) {
	writer.write(keepNodes(curr, nodesToKeep).toString());
	writer.write("\n");
      }
      writer.flush();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
