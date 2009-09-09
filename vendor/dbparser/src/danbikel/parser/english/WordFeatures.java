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

import danbikel.lisp.*;
import danbikel.parser.Settings;
import java.util.*;
import java.io.Serializable;
import java.text.*;

/**
 * WordFeatures are orthographic and morphological features of
 * words.  Specifically, the word features encoded by the methods of this class
 * are:
 * <ol>
 * <li>capitalization
 * <li>hyphenization
 * <li>inflection
 * <li>derivation
 * <li>numeric
 * </ol>
 * The features are encoded into a single symbol of the form:
 * <tt>CcHhIiDdNn</tt>, where <tt>c</tt> encodes capitalization, <tt>h</tt>
 * encodes hyphenization, <tt>i</tt> encodes inflection, <tt>d</tt> encodes
 * derivation and <tt>n</tt> encodes the numeric feature.  For example,
 * <tt>&quot;C3H0I0D3N0&quot;</tt> encodes the features for the word
 * <tt>&quot;Geography&quot;</tt> (that is, non-sentence-initial capitalized,
 * no hyphenization, no inflection, <tt>&quot;graphy&quot;</tt> derivation and
 * non-numeric).
 */
public class WordFeatures extends danbikel.parser.lang.AbstractWordFeatures {

  private final static ParsePosition pos = new ParsePosition(0);
  private final static NumberFormat nf = NumberFormat.getNumberInstance();

  /**
   * The property obtained from the {@link Settings} class to indicate
   * whether or not to consider underscores when creating the feature vector.
   */
  public final static String useUnderscoresProperty =
    "parser.wordfeatures.english.useUnderscores";

  private static boolean useUnderscores =
    Settings.getBoolean(useUnderscoresProperty);

  private final static Symbol defaultFeatureVector = Symbol.add("C0H0I0D0N0");

  private static String[] derivationalFeatures = {
    "-backed", "-based", "graphy", "meter", "ente", "ment", "ness",
    "tion", "ael", "ary", "ate", "ble", "ent", "ess", "est", "ial",
    "ian", "ine", "ion", "ism", "ist", "ite", "ity", "ive", "ize",
    "nce", "ogy", "ous", "sis", "uan", "al", "an", "as", "er", "ez",
    "ia", "ic", "ly", "on", "or", "os", "um", "us", "a", "i", "o", "y"};

  private static char[] consonantChars = {
    'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'p', 'q', 'r', 's',
    't', 'v', 'w', 'x', 'y', 'z'
  };

  private final static int featureStrLen = 10;

  private static BitSet consonants = new BitSet(Byte.MAX_VALUE);
  static {
    for (int i = 0; i < consonantChars.length; i++)
      consonants.set(consonantChars[i]);
  }

  /**
   * Constructs a new instance of this class for deterministically mapping
   * English words to word-feature vectors.
   */
  public WordFeatures() {}

  /**
   * Returns the features of a word.
   *
   * @param word the word.
   * @param firstWord indicates whether <code>word</code> is the first word
   * of the sentence in which it occurs
   * @return the encoded feature symbol.
   */
  public Symbol features(Symbol word, boolean firstWord) {
    String wordStr = word.toString();
    String returnStr;
    if (isNumber(wordStr)) {
      returnStr = "C0H0I0D0N1";
    } else {
      returnStr =
	(new StringBuffer(featureStrLen)).
	append(capitalizationFeature(wordStr,
				     firstWord)).
	append(hyphenizationFeature(wordStr)).
	append(inflectionalFeature(wordStr)).
	append(derivationalFeature(wordStr)).
	append("N0").
	toString();
    }
    return Symbol.add(returnStr);
  }

  public Symbol defaultFeatureVector() {
    return defaultFeatureVector;
  }

  private static String capitalizationFeature(String word, boolean firstWord) {
    if (Character.isUpperCase(word.charAt(0))) {
      if (firstWord)
	return "C1";
      else if (word.equals(word.toUpperCase())) {
	for (int i = 0; i < word.length(); i++) {
	  if (Character.isDigit(word.charAt(i)))
	    return "C2";
	}
	return "C3";
      }
      else
	return "C4";
    }
    return "C0";
  }

  private static String hyphenizationFeature(String word) {
    for (int i = 0; i < word.length(); i++) {
      if (word.charAt(i) == '-')
	return "H1";
      else if (useUnderscores && (word.charAt(i) == '_'))
	return "H2";
      else if (word.charAt(i) == '$')
	return "H3";
    }
    return "H0";
  }

  private static String inflectionalFeature(String word) {
    if (word.length() > 3) {
      if (sInflection(word))
	return "I1";
      else if (word.endsWith("ed"))
	return "I2";
      else if (word.endsWith("ing"))
	return "I3";
    }
    return "I0";
  }

  private static String derivationalFeature(String word) {
    if (word.length() > 6) {
      if (sInflection(word)) {
	word = word.substring(0, word.length() - 1);
      }
      for (int i = 0; i < derivationalFeatures.length; i++) {
	String feature = derivationalFeatures[i];
	if (word.endsWith(feature)) {
	  return "D" + Integer.toString(i + 1);
	}
      }
    }
    return "D0";
  }

  private static boolean isNumber(String word) {
    pos.setIndex(0);
    pos.setErrorIndex(-1);
    nf.parse(word, pos);
    return pos.getIndex() == word.length() && pos.getErrorIndex() == -1;
  }

  private static boolean sInflection(String word) {
    if (word.length() > 2) {
      char lastChar = word.charAt(word.length() - 1);
      char nextToLast = word.charAt(word.length() - 2);
      return ((lastChar == 's') && (nextToLast != 's') &&
	      ((nextToLast == 'e') || isConsonant(nextToLast)));
    }
    else
      return false;
  }

  private static boolean isConsonant(char ch) {
    if ((int)ch >= Byte.MAX_VALUE)
      return false;
    else
      return consonants.get((byte)ch);
  }
}
