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
import java.io.*;

/**
 * Reads in a file of parse trees and outputs pretty-printed versions.
 */
public class PrettyPrint {
  private PrettyPrint() {}

  /**
   * Reads in parse trees either from a specified file or from standard input
   * and pretty prints those trees to standard output.
   * <pre>usage: [- | <filename>]</pre>
   * where specifying <tt>-</tt> or using no arguments at all indicates to
   * read from standard input.
   */
  public static void main(String[] args) {
    InputStream in = System.in;
    if (args.length > 0) {
      if (!args[0].equals("-")) {
        File file = new File(args[0]);
        try { in = new FileInputStream(file); }
        catch (FileNotFoundException fnfe) {
          System.err.println("error: file \"" + args[0] + "\" does not exist");
          System.exit(1);
        }
      }
    }
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    SexpTokenizer tok = null;
    String enc = System.getProperty("file.encoding");
    try { tok = new SexpTokenizer(in, enc, 8192); }
    catch (UnsupportedEncodingException uee) {
      System.err.println("error: encoding \"" + enc + "\" not supported");
      System.exit(1);
    }
    Sexp curr = null;
    try {
      while ((curr = Sexp.read(tok)) != null) {
        System.out.println(Util.prettyPrint(curr));
      }
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
  }
}