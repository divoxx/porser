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
    package danbikel.switchboard;

import danbikel.util.IntPair;
import danbikel.lisp.*;
import java.io.Serializable;

/**
 * A class to bundle an object with an integer that represents the
 * order in which the object was discovered in its input file or
 * stream (the object's <i>number</i>), as well as a flag to indicate
 * whether the object has been processed.  Objects of this type are
 * used by the switchboard.
 *
 * @see Switchboard
 */
public class NumberedObject implements Comparable, Serializable {
  // data members
  private int number;
  private int fileId;
  private IntPair uid = null;
  private Object obj;
  private boolean processed;

  /** Constructs a new <code>NumberedObject</code> object with the
      specified object, processed flag and object number. */
  public NumberedObject(int number, boolean processed, Object obj) {
    this.number = number;
    this.processed = processed;
    this.obj = obj;
  }

  NumberedObject(int number, int fileId, boolean processed, Object obj) {
    this(number, processed, obj);
    setFileId(fileId);
  }

  /** Returns the number of the underlying object. */
  public int number() { return number; }
  /** Returns the underlying object. */
  public Object get() { return obj; }
  /** Returns whether the underlying object has been processed. */
  public boolean processed() { return processed; }

  /** Returns the fildId of this numbered object. */
  int fileId() { return fileId; }

  /** Sets the underlying object to be the specified object. */
  public void set(Object obj) { this.obj = obj; }
  /** Sets the processed flag, indicating whether this object was
      successfully processed. */
  public void setProcessed(boolean processed) { this.processed = processed; }
  /** Sets the processed flag to <code>true</code>. */
  public void setProcessed() {
    this.processed = true;
  }

  /** Sets the file ID number of this <code>NumberedObject</code>. */
  void setFileId(int fileId) { this.fileId = fileId; }

  IntPair uid() {
    if (uid == null)
      uid = new IntPair(fileId, number);
    return uid;
  }

  /**
   * This allows ordering of objects by their number.
   *
   * @return a negative integer if the underlying object's number is
   * less than that of the specified <code>NumberedObject</code>, 0 if
   * they have the same number (should not typically happen) or a
   * positive integer if the underlying object's number is greater
   * than that of the specified <code>NumberedObject</code>
   *
   * @throws ClassCastException if the specified object is not an instace of
   * <code>NumberedObject</code>
   */
  public int compareTo(Object obj) {
    NumberedObject other = (NumberedObject)obj;
    return this.number - other.number;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof NumberedObject))
      return false;
    NumberedObject other = (NumberedObject)obj;
    return this.uid() == other.uid();
  }

  /**
   * Returns a string of the form
   * <tt>(number&nbsp;processed&nbsp;objectStr)</tt>, where
   * <ul>
   * <li> <tt>processed</tt> is the output of
   * <code>String.valueOf(boolean)</code> when passed the processed
   * flag of this object
   * <li> <tt>objectStr</tt> is the result of calling the underlying object's
   * <code>toString</code> method
   * </ul>
   * This is the format that is written by the <code>TextObjectWriter</code>
   * objects created by the <code>TextObjectWriterFactory</code>.
   *
   * @see TextObjectWriter
   * @see Switchboard
   * @see Switchboard#Switchboard(String,int,
   *                              boolean,
   *                              ObjectReaderFactory,ObjectReaderFactory,
   *                              ObjectWriterFactory,ObjectWriterFactory)
   *      Switchboard.Switchboard(...)
   */
  public String toString() {
    return "(" + number + " " + processed + " " + obj + ")";
  }
}
