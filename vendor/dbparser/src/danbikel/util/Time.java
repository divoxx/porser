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

import java.text.*;

/**
 * A simple class for keeping track of wall-clock time.  This class
 * also contains a utility method for converting a <code>long</code>
 * representing milliseconds to a string representation of minutes,
 * seconds and milliseconds.
 *
 * @see #elapsedTime(long)
 */
public class Time {
  // number formatters (for timing output)
  private static NumberFormat longNF = NumberFormat.getInstance();
  private static NumberFormat doubleNF = NumberFormat.getInstance();
  static {
    doubleNF.setMinimumFractionDigits(3);
    doubleNF.setMaximumFractionDigits(3);
    doubleNF.setMinimumIntegerDigits(2);
    longNF.setMinimumIntegerDigits(2);
  }

  // data members
  private long startTime;

  /** Creates a <code>Time</code> object whose start time is
      <code>System.currentTimeMillis</code>. */
  public Time() {
    this(System.currentTimeMillis());
  }

  /** Creates a <code>Time</code> object with the specified start time. */
  public Time(long startTime) {
    this.startTime = startTime;
  }

  /** Resets the internal start time to be the current time. */
  public void reset() {
    this.startTime = System.currentTimeMillis();
  }

  /** Returns a string representation of the elapsed time since the start
      time of this <code>Time</code> object, using the output of
      {@link #elapsedTime(long)}. */
  public String toString() {
    return elapsedTime(System.currentTimeMillis() - startTime);
  }

  /** Returns the start time of this object. */
  public long startTime() { return startTime; }

  /**
   * Returns the number of milliseconds since the start time of this object.
   * This method is simply an alias for
   * <pre>current() - startTime()</pre>
   */
  public long elapsedMillis() { return current() - startTime; }

  /**
   * Returns the (floor of the) number of minutes since the start time of this
   * object.  This method is simply an alias for
   * <pre>elapsedMillis() / 60000</pre>.
   */
  public long elapsedMinutes() { return elapsedMillis() / 60000; }

  /**
   * Returns a string representing the length of the specified time
   * of the form <pre>MM:SS.mmm</pre>
   * where <tt>MM</tt> is the number of minutes, <tt>SS</tt> is the nubmer
   * of seconds and <tt>mmm</tt> is the number of milliseconds.
   */
  public static final String elapsedTime(long elapsedMillis) {
    long minutes = elapsedMillis / 60000;
    double seconds = (elapsedMillis / 1000.0) - (minutes * 60);
    return longNF.format(minutes) + ":" + doubleNF.format(seconds);
  }

  /** An alias for <code>System.currentTimeMillis</code>. */
  public static final long current() { return System.currentTimeMillis(); }
}
