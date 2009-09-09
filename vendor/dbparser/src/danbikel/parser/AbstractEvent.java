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

/**
 * A convenience class that simply implements the <code>equals</code>
 * method, as specified by the contract in {@link
 * Event#equals(Object)}.  For efficiency, subclasses are encouraged
 * to override this method, using its result only when the two objects
 * being compared are both instances of <code>Event</code> but are not
 * of identical run-time types (this is the strategy employed by
 * <code>SexpEvent</code>).
 *
 * @see SexpEvent#equals(Object)
 */
abstract public class AbstractEvent implements Event {
  AbstractEvent() { }

  /**
   * Compares this <tt>Event</tt> object to the specified object for equality.
   * Two <tt>Event</tt> objects are equal if the following conditions are met:
   * <ul>
   * <li>they both accept the same number of types, as deteremined by their
   * {@link Event#numTypes()} methods
   * <li>they both have the same number of components, as determined by their
   * {@link Event#numComponents()} methods
   * <li>for each type, the two objects' corresponding sequences are of the same
   * size (that is, contain the same number of components, as determined by
   * their {@link Event#numComponents(int)} methods)
   * <li>for each type, the two objects' corresponding sequences of components
   * are equal, where two sequences are defined to be equal if their elements
   * are pairwise equal (as determined by the <tt>equals(Object)</tt> method
   * of one object in the pair applied to the other)
   * </ul>
   * <b>N.B.</b>: For efficiency, subclasses are encouraged to override this
   * method, using its result only when the two objects being compared are both
   * instaces of <tt>Event</tt> but are not of identical run-time types.
   *
   * @param obj the object to be tested for equality to this object
   * @return whether this <tt>Event</tt> is equal to the specified object
   */
  public boolean genericEquals(Object obj) {
    if (!(obj instanceof Event))
      return false;
    // check same number of types and same total number of components
    Event otherEvent = (Event)obj;
    if (otherEvent.numTypes() != numTypes() ||
	otherEvent.numComponents() != numComponents())
      return false;

    //System.err.println("warning: we're deep in genericEquals!");

    // check that every type of this object is supported by other object
    // (and hence, since they have the same number of types, the converse
    // will be true)
    int numTypes = numTypes();
    for (int i = 0; i < numTypes; i++)
      if (otherEvent.typeIndex(getClass(i)) == -1)
	return false;

    // they share the same types, so check each list
    for (int typeIdx = 0; typeIdx < numTypes; typeIdx++) {
      int otherTypeIdx = otherEvent.typeIndex(getClass(typeIdx));
      // first, check length of same-typed lists
      if (numComponents(typeIdx) != otherEvent.numComponents(otherTypeIdx))
	return false;
      // now, check components of our equal-length lists of the current type:
      // lists must be pairwise equal
      int numComponents = numComponents(typeIdx);
      for (int componentIdx = 0; componentIdx < numComponents; componentIdx++) {
	Object objFromThis = this.get(typeIdx, componentIdx);
	Object objFromOther = otherEvent.get(otherTypeIdx, componentIdx);
	try {
	  if (!objFromThis.equals(objFromOther))
	    return false;
	}
	catch (ClassCastException cce) {
	  return false;
	}
      }
    }
    return true;
  }
}
