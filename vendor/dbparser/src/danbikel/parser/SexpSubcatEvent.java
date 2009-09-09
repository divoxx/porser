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
import java.util.*;
import java.io.*;

/**
 * Represents an event composed of zero or more <code>Sexp</code> objects
 * and zero or one <code>Subcat</code> object.
 * <p>
 * <b>Bugs</b>: The fact that no more than one <code>Subcat</code>
 * object can be added to this event type does not follow the general
 * contract of {@link Event}, which specifies that when an
 * <code>Event</code> advertises that it can collect elements of a
 * certain type, it should contain some sort of a list of such objects
 * internally, to be accessed via the {@link Event#get(int,int)}
 * method.  However, since <code>Subcat</code> objects are not simplex
 * entities but are themselves collections, this (current) limitation
 * is not entirely unreasonable.
 */
public class SexpSubcatEvent extends SexpEvent {
  // private constants
  private final static String sexpSubcatLabelStr = "sexp-subcat-event";
  private final static int sexpTypeIdx = 0;
  private final static int subcatTypeIdx = 1;
  private final static int numTypes = 2;
  private final static Class[] classes = new Class[numTypes];
  static {
    classes[sexpTypeIdx] = Sexp.class;
    classes[subcatTypeIdx] = Subcat.class;
  }

  // public constants
  /**
   * Initial symbol used in the string representation of
   * <code>SexpSubcatEvent</code> objects.
   *
   * @see #toString()
   */
  public final static Symbol sexpSubcatLabel = Symbol.add(sexpSubcatLabelStr);

  // data member
  /** The subcat data member. */
  protected Subcat subcat;

  /**
   * Constructs an event containing no components.
   */
  public SexpSubcatEvent() {
    super();
  }

  /**
   * Creates a new object capable of efficiently collecting the specified
   * number of components of the type <code>Sexp</code>.  The initial
   * capacity does not apply to components of type <code>Subcat</code>, as
   * this <code>Event</code> implementation does not support more than
   * 1 <code>Subcat</code> component.
   *
   * @param initialCapacity the pre-allocated capacity for objects of type
   * <code>Sexp</code> for this event
   */
  public SexpSubcatEvent(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Gets the class object associated with the specified type index.  The two
   * classes/types supported by this <code>Event</code> implementation are
   * <code>Sexp</code> and <code>Subcat</code>.
   */
  public Class getClass(int type) { return classes[type]; }

  /**
   * Gets the type index associated with the specified class.  The two
   * classes/types supported by this <code>Event</code> implementation are
   * <code>Sexp</code> and <code>Subcat</code>, so this method
   * is guaranteed to return either 0 or 1 for these classes, or -1 if the
   * specified class is neither of these two classes.
   */
  public int typeIndex(Class cl) {
    int typeIdx = 0;  // type indicies required to start at 0 by Event contract
    for ( ; typeIdx < classes.length; typeIdx++)
      if (cl.equals(classes[typeIdx]))
	return typeIdx;
    return -1;
  }

  /**
   * Returns 2, as this <code>Event</code> implementation supports only two
   * types.
   */
  public int numTypes() { return numTypes; }

  /**
   * Returns the number of components of this event: the number of
   * <code>Sexp</code> objects plus the number of <code>Subcat</code> objects.
   * The number of <code>Sexp</code> objects is 1 if the backing
   * <code>Sexp</code> is a symbol, or the length of the list otherwise.
   * Due to limitations of the implementation of this event type, there can be
   * at most 1 <code>Subcat</code> object.
   */
  public int numComponents() {
    return super.numComponents() + subcatCount();
  }

  /**
   * Returns the number of <code>Sexp</code> components or the number of
   * <code>Subcat</code> components that are part of this event.
   * Due to limitations of the implementation of this event type, there can be
   * at most 1 <code>Subcat</code> object.
   */
  public int numComponents(int type) {
    switch (type) {
    case sexpTypeIdx:
      return super.numComponents();
    case subcatTypeIdx:
      return subcatCount();
    default:
      return 0; // shouldn't ever reach this point!
    }
  }

  /**
   * Gets the <code>index</code><sup>th</sup> component of this event of
   * the specified type.  If the type is that for <code>Sexp</code> and
   * the backing <code>Sexp</code> is a symbol, then the specified index must
   * be 0; otherwise, the index must be within the range of the backing
   * <code>SexpList</code>.  If the type is that for <code>Subcat</code>,
   * then the specified index must be 0, as this <code>Event</code>
   * implementation only supports the storage of at most 1 <code>Subcat</code>
   * object.
   *
   * @param type the type index of the component to get
   * @param index the index of the component to get
   *
   * @exception IndexOutOfBoundsException if
   * <code>type</code>&nbsp;&lt;&nbsp;0</code> or if
   * <code>type&nbsp;&gt;=&nbsp;numTypes()</code> or if
   * <code>index&nbsp;&lt;&nbsp;0</code> or if
   * <code>index&nbsp;&gt;=&nbsp;numComponents(type)</code>
   *
   * @see #typeIndex(Class)
   */
  public Object get(int type, int index) {
    switch (type) {
    case sexpTypeIdx:
      return super.get(type, index);
    case subcatTypeIdx:
      if (subcatCount() == 0)
	throw new IndexOutOfBoundsException();
      else
	return subcat;
    default:
	throw new IndexOutOfBoundsException();
    }
  }

  private final int subcatCount() { return (subcat == null ? 0 : 1); }

  /**
   * Adds the specified component to this event.
   * <p>
   * <b>N.B.</b>: Given that this implementation only supports at most
   * 1 <code>Subcat</code> component, if the specified object is of type
   * <code>Subcat</code> and this event already has such a
   * component, then the existing subcat will be (silently!) replaced
   * by the specified subcat.  Put another way, it is an error to add
   * more than one <code>Subcat</code> object to a
   * <code>SexpSubcatEvent</code> object.
   *
   * @param obj the component to add to this event
   */
  public MutableEvent add(Object obj) {
    // important to test if obj is an instance of Subcat first, because
    // SubcatList objects are also instances of Sexp objects!
    // rule of thumb: Event objects must always test for more specific
    // run-time types than more general ones of the types they support
    if (obj instanceof Subcat)
      subcat = (Subcat)obj;
    else
      super.add(obj);
    return this;
  }

  /**
   * Adds the specified type of object to this event.  If the specified object
   * type is <code>typeIndex(Subcat.class)</code>, then the specified object
   * must be an instance of {@link Subcat} and is added as this event's subcat.
   * If the specified object type is <code>typeIndex(Sexp.class)</code>, then
   * the object must be of type {@link Sexp} and is added to this event's list
   * of {@link Sexp} elements.
   *
   * @param type the type of object to be added to this event
   * @param obj  the object to be added to this event
   * @return this {@link SexpSubcatEvent} object
   *
   * @see #typeIndex(Class) 
   */
  public MutableEvent add(int type, Object obj) {
    switch (type) {
    case sexpTypeIdx:
      super.add(obj);
      break;
    case subcatTypeIdx:
      subcat = (Subcat)obj;
      break;
    }
    return this;
  }

  /**
   * Canonicalizes the backing <code>Sexp</code> and <code>Subcat</code>
   * components of this event using the specified reflexive map.
   *
   * @param canonical a reflexive map used for canonicalization
   */
  public int canonicalize(Map canonical) {
    if (subcat != null)
      subcat = subcat.getCanonical(false, canonical);
    return super.canonicalize(canonical);
  }

  /**
   * Returns a deep copy of this event, using <code>SexpEvent.copy</code> to
   * copy the backing <code>Sexp</code>, and using <code>Event.copy</code>
   * to copy the backing <code>Subcat</code>, if there is one.
   * <p>
   * <b>Bugs</b>: The subcat is <i><b>not</b></i> deeply copied, as we
   * are using "caller-copy" semantics, that is, that any thread that wishes
   * to use a modified version of a subcat must take care to copy an existing
   * subcat before making the modification.  This convention is potentially
   * dangerous, but is in place for efficiency.
   *
   * @see SexpEvent#copy
   * @see Event#copy
   */
  public Event copy() {
    SexpSubcatEvent eventCopy = new SexpSubcatEvent();
    eventCopy.event = copySexpEvent();
    /*
    if (subcat != null)
      eventCopy.subcat = (Subcat)subcat.copy();
    */
    // WARNING!!!
    // This is potentially very dangerous, and is an efficiency hack, as we
    // have made it such that Subcat objects are only copied just prior to
    // being modified (meaning that in all other cases, we never need to copy
    // them)
    // WARNING!!!
    if (subcat != null)
      eventCopy.subcat = this.subcat;
    return eventCopy;
  }

  /** Returns the hash code of this event, based on its components. */
  public int hashCode() {
    int code = super.hashCode();
    if (subcat != null)
      code = (code << 2) ^ subcat.hashCode();
    return code;
  }

  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o instanceof SexpSubcatEvent) {
      SexpSubcatEvent other = (SexpSubcatEvent)o;
      return this.subcat.equals(other.subcat) && super.equals(o);
    }
    else
      return this.genericEquals(o);
  }

  /**
   * Returns a string representation of this object of the form
   * <tt>(</tt>{@link #sexpSubcatLabel}<tt>&nbsp;event)</tt>
   * where <tt>event</tt> is string representation of the <code>Sexp</code>
   * held by this object.
   */
  public String toString() {
    if (subcat == null)
      return super.toString();
    else
      return
	"(" + sexpSubcatLabelStr + " " + event + " " + subcat.toSexp() + ")";
  }

  /**
   * If this event has no subcat, then this method simply returns the backing
   * <code>Sexp</code>; otherwise, this method returns a <code>SexpList</code>
   * of the form <code>(sexp&nbsp;subcat)</code>, where <code>sexp</code>
   * is the <code>Sexp</code> component(s) of this event and <code>subcat</code>
   * is the result of calling <code>subcat.toSexp()</code>.
   *
   * @see SexpConvertible
   * @see Subcat#toSexp
   */
  public Sexp toSexp() {
    if (subcat == null)
      return super.toSexp();
    else
      return new SexpList(2).add(event).add(subcat.toSexp());
  }

  public void clear() {
    super.clear();
    subcat = null;
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(subcat);
  }

  public void readExternal(ObjectInput in)
    throws IOException, ClassNotFoundException {
    super.readExternal(in);
    subcat = (Subcat)in.readObject();
  }
}
