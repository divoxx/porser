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

import java.io.File;
import java.util.Properties;

/**
 * A set of static utility functions that operate on <code>String</code>
 * or <code>StringBuffer</code> objects.
 */
public class Text {

  /**
   * Used so that external links to elements of the <code>Character</code>
   * class will work.
   */
  public static Character javadocHack = new Character('d');


  private Text() {}

  /**
   * Expands the variables in the specified <code>StringBuffer</code>.
   * The syntax for a variable is <code>${var-name}</code>.
   * <code>var-name</code> can either be the name of a property in
   * <code>props</code>, the name of a System property or <code>/</code>,
   * which will expand to <code>File.separator</code>, allowing for
   * platform-independent specification of paths.  Variables cannot be nested,
   * as this method simply looks for the first occurrence of <code>${</code>
   * and the next occurence of <code>}</code>.  This type of variable
   * expansion is identical to the type allowed in Java security
   * policy files, except that the specified <code>Properties</code>
   * object is first consulted before that of the <code>System</code>
   * object.  If a variable does not exist as a property, then this
   * method returns <code>false</code> and does not alter the
   * <code>StringBuffer</code> object.
   *
   * @param props the <code>Properties</code> object, or <code>null</code>
   * if only System properties are to be used for expansion
   * @param sb the <code>StringBuffer</code> in which to attempt to expand
   * variables
   * @return <code>true</code> if successful, <code>false</code> if
   * there were expansion problems
   */
  public static final boolean expandVars(Properties props, StringBuffer sb) {
    boolean success = true;
    for (int currIdx = 0; currIdx < sb.length(); ) {
      int begin = find(sb, "${", currIdx);
      if (begin != -1) {
	int end = find(sb, "}", begin + 2);
	if (end != -1) {
	  String var = sb.substring(begin + 2, end);
	  String expanded = (var.equals("/") ? File.separator : null);
	  if (props != null && expanded == null)
	    expanded = props.getProperty(var);
	  if (expanded == null)
	    expanded = System.getProperty(var);
	  if (expanded == null) {
	    success = false;
	    currIdx = end + 1;
	  }
	  else {
	    sb.replace(begin, end + 1, expanded);
	    currIdx = begin + expanded.length();
	  }
	}
	else
	  break; // there was no end to the current variable, so leave loop
      }
      else
	break; // there were no more variables
    }
    return success;
  }

  /**
   * Expands the variables in the specified <code>StringBuffer</code>.  The
   * syntax for a variable is <code>${var-name}</code>. <code>var-name</code>
   * can either be the name of a System property, or <code>/</code>, which will
   * expand to <code>File.separator</code>, allowing for platform-independent
   * specification of paths.  Variables cannot be nested, as this method simply
   * looks for the first occurrence of <code>${</code> and the next occurence
   * of <code>}</code>.  This type of variable expansion is identical to the
   * type allowed in Java security policy files.  If a variable does not exist
   * as a property, then this method returns <code>false</code> and does not
   * alter the <code>StringBuffer</code> object.  Calling this method is
   * identical to calling
   * <pre>Text.expandVars(null, sb)</pre>
   *
   *
   * @param sb the <code>StringBuffer</code> in which to attempt to expand
   * variables
   * @return <code>true</code> if successful, <code>false</code> if
   * there were expansion problems
   */
  public static final boolean expandVars(StringBuffer sb) {
    return expandVars(null, sb);
  }

  /**
   * Finds the first occurrence of <code>toFind</code> in the characters
   * contained in the string buffer <code>sb</code>.
   *
   * @param sb the string buffer to search
   * @param toFind the string to search for
   * @return the index of the first occurrence of <code>toFind</code>
   * or -1 if <code>toFind</code> does not occur in <code>sb</code>
   */
  public static final int find(StringBuffer sb, String toFind) {
    return find(sb, toFind, 0);
  }

  /**
   * Finds the first occurrence of <code>toFind</code> at or after
   * <code>startIdx</code> in the characters of the string buffer
   * <code>sb</code>.
   *
   * @param sb the string buffer to search
   * @param toFind the string to search for
   * @param startIdx the index in <code>sb</code> at which to start
   * looking for <code>toFind</code>
   */
  public static final int find(StringBuffer sb, String toFind,
			       int startIdx) {
    int toFindIdx = 0;
    int toFindLen = toFind.length();
    int sbLen = sb.length();

    for (int i = startIdx; i < sbLen; i++) {
      if (sb.charAt(i) == toFind.charAt(toFindIdx))
	toFindIdx++;
      else
	toFindIdx = 0;
      if (toFindIdx == toFindLen)
	return (i - toFindLen + 1);
    }
    return -1;
  }

  public final static boolean isAllWhitespace(String s) {
    int sLen = s.length();
    for (int i = 0; i < sLen; i++)
      if (!Character.isWhitespace(s.charAt(i)))
	return false;
    return true;
  }

  /**
   * Returns <code>true</code> if <code>s</code> is composed only of
   * characters for which {@link Character#isDigit} returns <code>true</code>;
   * returns <code>false</code> otherwise.
   * @param s the string to test
   * @return <code>true</code> if <code>s</code> contains only digits
   */
  public final static boolean isAllDigits(String s) {
    int sLen = s.length();
    for (int i = 0; i < sLen; i++)
      if (!Character.isDigit(s.charAt(i)))
	return false;
    return true;
  }

  /**
   * Returns <code>true</code> if <code>s</code> is composed only of
   * characters for which {@link Character#isLetterOrDigit} returns
   * <code>true</code>; returns <code>false</code> otherwise.
   *
   * @param s the string to test
   * @return <code>true</code> if <code>s</code> contains only letters or
   * digits
   */
  public final static boolean isAllLettersOrDigits(String s) {
    int sLen = s.length();
    for (int i = 0; i < sLen; i++)
      if (!Character.isLetterOrDigit(s.charAt(i)))
	return false;
    return true;
  }
}
