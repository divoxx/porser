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

/**
 * Reads parse trees either from standard input or a specified file,
 * converting them to sentences and printing those sentences on standard
 * output.
 */
public class ParseToSentence {
  private static final int bufSize = Constants.defaultFileBufsize;

  private ParseToSentence() {}

  private static String[] usageMsg = {
    "usage: [-v|-help|-usage] [-tags] [filename]",
    "where",
    "\t-v|-help|-usage: prints out this message",
    "\t-tags: indicates to spit out one S-expression per word, of the form",
    "\t\t(word (tag))",
    "\tfilename is the file to be processed (standard input is assumed if",
    "\t\tthis argument is \"-\" or is not present)"
  };

  private static void usage() {
    for (int i = 0; i < usageMsg.length; i++)
      System.err.println(usageMsg[i]);
  }

  /**
   * Reads parse trees either from standard input or a specified file,
   * converting them to sentences and printing those sentences on standard
   * output.
   * <pre>
   * usage: [-v|-help|-usage] [-tags] [filename]
   *         -v|-help|-usage: prints out this message
   *         -tags: indicates to spit out one S-expression per word, of the form
   *                 (word (tag))
   *         filename is the file to be processed (standard input is assumed if
   *                 this argument is "-" or is not present)
   * </pre>
   */
  public static void main(String[] args) {
    InputStream inStream = System.in;
    boolean tags = false;
    String inFile = null;
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-help") || args[i].equals("-usage") ||
          args[i].equals("-v")) {
        usage();
        return;
      }
      else if (args[i].equals("-tags"))
        tags = true;
      else if (!args[i].equals("-"))
        inFile = args[i];
    }
    if (inFile != null) {
      try {
	inStream = new FileInputStream(inFile);
      } catch (FileNotFoundException fnfe) {
	System.err.println(fnfe);
	System.exit(-1);
      }
    }
    try {
      SexpTokenizer tok =
        new SexpTokenizer(inStream, Language.encoding(), bufSize);
      OutputStream os = System.out;
      Writer writer =
	new BufferedWriter(new OutputStreamWriter(os, Language.encoding()));
      PrintWriter pw = new PrintWriter(writer);
      Sexp curr = null;
      while ((curr = Sexp.read(tok)) != null)
        pw.println(tags ?
		   Util.collectTaggedWords(curr) :
		   Util.collectLeaves(curr));
      pw.flush();
      pw.close();
    }
    catch (Exception e) {
      System.out.println(e);
    }
  }
}
