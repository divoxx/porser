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
    package danbikel.parser;

import danbikel.lisp.*;

/**
 * Contains static constants for use by this package.
 */
public class Constants {
  /** Private constructor: this class is only to be used for static data. */
  private Constants() {}

  /** The constant representing the left side or the left-to-right direction. */
  public static final boolean LEFT = false;
  /** The constant representing the right side or the right-to-left
      direction. */
  public static final boolean RIGHT = true;
  /** The symbol constant representing the left side or the
      left-to-right direction. */
  public static final Symbol leftSym = Symbol.add("left");
  /** The symbol constant representing the right side or the right-to-left
      direction. */
  public static final Symbol rightSym = Symbol.add("right");

  /**
   * The symbol constant indicating to match the first child node in the
   * natural walk of a parent's children in a syntax tree (left-to-right,
   * in English).
   */
  public static final Symbol firstSym = Symbol.add("first");
  /**
   * The symbol constant indicating to match the last child node in the
   * reverse of the natural walk of a parent's children in a syntax tree
   * (right-to-left, in English).
   */
  public static final Symbol lastSym = Symbol.add("last");

  /** A symbol constant to represent truth. */
  public static final Symbol trueSym = Symbol.add("true");
  /** A symbol constant to represent falsity. */
  public static final Symbol falseSym = Symbol.add("false");

  /** A symbol constant to represent negation.  */
  public static final Symbol notSym = Symbol.add("not");
  /** A symbol constant to represent Kleene star. */
  public static final Symbol kleeneStarSym = Symbol.add("*");

  /**
   * A constant to represent the logarithm of zero, equal to
   * <code>Double#NEGATIVE_INFINITY</code>.
   */
  public static final double logOfZero = Double.NEGATIVE_INFINITY;

  /**
   * The constant to represent the logarithm of 10E-19, which is a very
   * small probability.
   */
  public static final double logProbSmall = -19 * Math.log(10);

  /**
   * A constant to represent the logarithm of a probability of 1.0
   * (equal to 0.0).
   */
  public static final double logProbCertain = 0.0;

  /**
   * A constant to represent the probability of an impossible event
   * (equal to 0.0).
   */
  public static final double probImpossible = 0.0;

  /**
   * A constant to represent the probability of 1.0 (equal to 1.0).
   */
  public static final double probCertain = 1.0;

  /**
   * The default file buffer size, which may be passed as an argument to
   * <code>BufferedReader</code> and <code>BufferedWriter</code> constructors.
   */
  public static final int defaultFileBufsize = 81920;

  /**
   * Converts a boolean value into a symbol representation.
   */
  public final static Symbol booleanToSym(boolean value) {
    return (value ? Constants.trueSym : Constants.falseSym);
  }

  /**
   * Converts the boolean value of {@link #LEFT} into {@link #leftSym}
   * and converts the boolean value of {@link #RIGHT} into {@link #rightSym}.
   */
  public final static Symbol sideToSym(boolean side) {
    return side == LEFT ? leftSym : rightSym;
  }
}
