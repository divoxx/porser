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
    package danbikel.util;

//import bsh.Interpreter;
import java.io.*;
import java.util.*;

/**
 * Static class that stores the current debugging level
 * (default is zero, for no debugging output), debugging options, and other
 * utility functions for debugging.  Its main function also allows the use
 * of <a href="http://www.beanshell.org">BeanShell</a> to perform on-the-fly
 * debugging tests.
 */
public class Debug implements Serializable {
  // data members
  /**
   * A counter with which to count stuff.
   */
  public static int counter = 0;  // to count stuff
  /**
   * The current debugging level (default is <tt>0</tt>).
   */
  public static int level = 0;

  /**
   * Sets the debugging level to be the specified level.
   *
   * @param newLevel the new debugging level
   */
  public static void setLevel(int newLevel) {
    level = newLevel;
  }

  /**
   * Fills the specified string array with the whitespace-delimited tokens
   * contained in the specified filler argument.
   * <p>
   * This method should probably be in {@link danbikel.util.Text}.
   * <p>
   * @param arr the array to be filled
   * @param filler a string that will be tokenized based on whitespace and whose
   * tokens will be used to fill the specified array
   * @throws IndexOutOfBoundsException if the number of whitespace-delimited
   * tokens in <code>filler</code> is greater than the size of <code>arr</code>
   */
  public static void fillStringArray(String[] arr, String filler) {
    StringTokenizer st = new StringTokenizer(filler);
    for (int i = 0; st.hasMoreTokens(); i++)
      arr[i] = st.nextToken();
  }
/*
  public static void main(String[] args) {
    Interpreter i = new Interpreter();
    try {
      i.eval("server(" + args[0] + ")");
    } catch (bsh.EvalError e) { System.err.println(e); }
  }
*/
}
