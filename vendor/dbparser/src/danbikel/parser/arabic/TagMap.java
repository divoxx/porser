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
    package danbikel.parser.arabic;

import danbikel.parser.Constants;
import danbikel.parser.HeadFinder;
import danbikel.parser.Language;
import danbikel.parser.Treebank;
import danbikel.parser.Settings;
import danbikel.parser.Nonterminal;
import danbikel.parser.Word;
import danbikel.util.*;
import danbikel.lisp.*;
import java.util.*;
import java.io.*;

/**
 * Provides tag mapping function for collapsing Arabic part-of-speech tags to
 * Penn Treebank equivalents.  The original mapping was provided by Ann Bies of
 * the <a href="http://www.ircs.upenn.edu/">Institute for Research in Cognitive
 * Science</a> at the <a href="http://www.upenn.edu/">University of
 * Pennsylvania</a>.
 */
public class TagMap {
  private TagMap() {}

  // constants
  private final static String className = TagMap.class.getName();

  private final static boolean warnUnmapped = true;

  private final static Symbol punc = Symbol.get("PUNC");
  private final static Symbol period = Symbol.get(".");
  private final static Symbol quotePeriod = Symbol.get("\".");
  private final static Symbol comma = Symbol.get(",");

  private static Map tagMap = new java.util.HashMap();

  /**
   * This method is used by {@link Training#readMetadata} to set up the
   * main map used by this class.
   */
  static void add(Symbol from, Symbol to) {
    //System.err.println("TagMap: adding mapping from " + from + " to " + to);
    tagMap.put(from, to);
  }

  /**
   * Helper method used by {@link Training#transformTags(Sexp)}.
   */
  protected static Symbol transformTag(Word word) {
    if (word.tag() == punc) {
      /*
      if (word.word() == period ||
	  word.word() == quotePeriod ||
	  word.word() == comma) {
      */
      if (word.word() == comma) {
	return word.word();
      }
      else {
	return word.tag();
      }
    }
    Symbol mappedTag = (Symbol)tagMap.get(word.tag());
    if (mappedTag == null) {
      if (warnUnmapped)
	System.err.println(className + ": warning: not mapping " + word.tag());
      return word.tag();
    }
    else {
      return mappedTag;
    }
  }
}
