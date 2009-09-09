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
 * Represents an event composed of one or more <code>Sexp</code>
 * objects.  For efficiency reasons, this class treats as equal a
 * backing <code>Sexp</code> that is a symbol and a backing
 * <code>Sexp</code> that is a list whose sole element is the same
 * symbol.  As per the guidelines of <code>MutableEvent</code>, this
 * class implements the <code>ensureCapacity</code> methods, so that a
 * single <code>SexpEvent</code> object may be re-used for efficiency.
 */
public class SexpEvent extends AbstractEvent
  implements MutableEvent, SexpConvertible, Externalizable {
  // private constants
  private final static String sexpLabelStr = "sexp-event";

  // public constants
  /**
   * Initial symbol used in the string representation of <code>SexpEvent</code>
   * objects.
   *
   * @see #toString()
   */
  public final static Symbol sexpLabel = Symbol.add(sexpLabelStr);

  // data member
  /**
   * The event stored by this instance.
   */
  protected Sexp event;

  /**
   * Constructs a <code>SexpEvent</code> from a list of the form
   * <code>(sexpLabel&nbsp;sexp)</code>
   * where <code>sexplabel</code> is the symbol {@link #sexpLabel} and
   * <code>sexp</code> is the event S-expression.
   */
  SexpEvent(SexpList list) {
    if (list.length() > 2 ||
	list.first().isList() || list.symbolAt(0) != sexpLabel)
      throw new RuntimeException();
    this.event = list.get(1);
  }

  /**
   * Constructs a <code>SexpEvent</code> that contains no data.
   */
  public SexpEvent() {}

  /**
   * Constructs a <code>SexpEvent</code> that contains no data, but
   * whose {@link #ensureCapacity(int)} method has been called with
   * the specified value.
   */
  public SexpEvent(int initialCapacity) {
    this();
    ensureCapacity(initialCapacity);
  }

  /**
   * Creates a new <code>SexpEvent</code> using the specified S-expression.
   *
   * @param event the <code>Sexp</code> representing an arbitrary event.
   */
  public SexpEvent(Sexp event) {
    this.event = event;
  }

  /**
   * Returns <code>Sexp.class</code> if the specified type is 0.
   *
   * @exception IndexOutOfBoundsException if the specified type is not 0
   */
  public Class getClass(int type) {
    if (type == 0)
      return Sexp.class;
    else
      throw new IndexOutOfBoundsException();
  }

  /**
   * Returns 0 if the specified class is <code>Sexp.class</code>, -1
   * otherwise.
   */
  public int typeIndex(Class cl) {
    if (cl.equals(Sexp.class))
      return 0;
    else
      return -1;
  }

  /**
   * Returns 1, as this <code>Event</code> implementation supports only one
   * type.
   */
  public int numTypes() { return 1; }

  /**
   * Returns the number of components of this event: 1 if the backing
   * <code>Sexp</code> is a symbol, or the length of the list otherwise.
   */
  public int numComponents() {
    return (event.isSymbol() ? 1 : event.list().length());
  }

  /**
   * An alias for {@link #numComponents}, as this <code>Event</code>
   * implementation supports only one type.
   */
  public int numComponents(int type) { return numComponents(); }

  /**
   * Returns the <code>index</code><sup>th</sup> component of this event.
   * If the backing <code>Sexp</code> is a symbol, then <code>index</code>
   * must be 0; otherwise, it must be not be outside the acceptable index range
   * of the backing <code>SexpList</code>.
   *
   * @param type the type index of the component to be retrieved (which must
   * be zero, since <code>SexpEvent</code> objects only have one possible type)
   * @param index the index of the component to be retrieved
   *
   * @exception IndexOutOfBoundsException if
   * <code>type</code>&nbsp;!=&nbsp;0</code> or if
   * <code>index&nbsp;&lt;&nbsp;0</code> or if
   * <code>index&nbsp;&gt;=&nbsp;numComponents(type)</code>
   */
  public Object get(int type, int index) {
    if (type != 0)
      throw new IndexOutOfBoundsException();
    if (event.isSymbol()) {
      if (index == 0)
	return event;
      else
	throw new IndexOutOfBoundsException();
    }
    else
      return event.list().get(index);
  }

  /**
   * Returns a deep copy of this event, which really just means
   * creating a new instance with a deep copy of the backing
   * <code>Sexp</code>, using the <code>Sexp.deepCopy</code> method.  As a
   * special exception, if the underlying <code>Sexp</code> of this
   * event is a list with only one element that is a symbol, the copy
   * will contain a <code>Sexp</code> that is that symbol (the
   * "canonical" version of lists containing a single symbol, as
   * far as this class is concerned).  This transforms a time-efficient
   * lookup object using a pre-allocated underlying list into a
   * space-efficient object suitable for persistent storage.
   *
   * @see #copySexpEvent
   * @see #equals(Object)
   * @see #ensureCapacity(int)
   */
  public Event copy() {
    return new SexpEvent(copySexpEvent());
  }

  /**
   * Returns a deep copy of the underlying <code>Sexp</code> of this event.
   * If the underlying <code>Sexp</code> is a single-element list containing
   * a symbol, the symbol is returned instead of a copy of the list.
   */
  protected final Sexp copySexpEvent() {
    if (eventIsListOfSingleSymbol(event))
      return event.list().get(0);
    else
      return event.deepCopy();
  }

  /**
   * Returns the hash code of the backing <code>Sexp</code> object.
   * As a special case, if the backing <code>Sexp</code> is a list with a
   * single symbol, the hash code returned is that of the single symbol.
   * This behavior is consistent with this class' semantics of treating
   * a symbol and a list containing that symbol as equivalent.
   */
  public int hashCode() {
    if (event == null)
      return 0;
    else if (eventIsListOfSingleSymbol(event))
      return event.list().get(0).hashCode();
    else
      return event.hashCode();
  }

  private final static boolean eventIsListOfSingleSymbol(Sexp event) {
    return (event.isList() && event.list().size() == 1 &&
	    event.list().get(0).isSymbol());
  }

  /**
   * Returns <tt>true</tt> if <tt>o</tt> is an instance of
   * <tt>Event</tt> and if the backing <tt>Sexp</tt> of <tt>o</tt>
   * is equal to the backing <tt>Sexp</tt> of this object; also, this
   * method treats backing <code>Sexp</code> of the two objects equal
   * when one is a symbol and the other is a list of length 1
   * containing that symbol.
   * <p>
   * As per the general contract of {@link Event#equals(Object)}, the
   * specified object need not be of type <code>SexpEvent</code>, but merely
   * support the same types as this <code>Event</code>, namely <code>Sexp</code>
   * objects, and store the same number of equal <code>Sexp</code> objects in
   * the same order.
   */
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Event))
      return false;
    else if (o instanceof SexpEvent) {
      Sexp otherEvent = ((SexpEvent)o).event;
      if (event.isList()) {
	if (otherEvent.isList())
	  return (event == otherEvent || event.equals(otherEvent));
	else
	  return event.list().size() == 1 && event.list().get(0) == otherEvent;
      }
      else {
	if (otherEvent.isList())
	  return (otherEvent.list().size() == 1 &&
		  event == otherEvent.list().get(0));
	else
	  return event == otherEvent;
      }
    }
    else {
      return this.genericEquals(o);
    }
  }

  /**
   * Returns a string representation of this object of the form
   * <tt>(</tt>{@link #sexpLabel}<tt>&nbsp;event)</tt>
   * where <tt>event</tt> is string representation of the <code>Sexp</code>
   * held by this object.
   */
  public String toString() {
    return "(" + sexpLabelStr + " " + event + ")";
  }

  /**
   * Sets the backing <tt>Sexp</tt> of this object to be the specified
   * <tt>Sexp</tt>.
   */
  public void setSexp(Sexp event) {
    this.event = event;
  }

  /** Returns the <code>Sexp</code> contained in this event. */
  public Sexp toSexp() { return event; }

  /**
   * Adds the specified object, which must be a {@link Sexp} instance,
   * to this event.
   * @param obj a {@link Sexp} instance to add to this event
   * @return this {@link SexpEvent} object
   */
  public MutableEvent add(Object obj) {
    Sexp newComponent = (Sexp)obj;
    if (event == null)
      event = newComponent;
    else if (event.isList()) {
      event.list().add(newComponent);
    }
    else {
      SexpList newEvent = new SexpList(4).add(event).add(newComponent);
      event = newEvent;
    }
    return this;
  }

  /**
   * Identical to <code>add(obj)</code>.
   * @param type an ignored parameter
   * @param obj an event to add to this complex event object
   * @return this {@link SexpEvent} object
   *
   * @see #add(Object)
   */
  public MutableEvent add(int type, Object obj) {
    return add(obj);
  }

  /**
   * Since events are typically read-only, this method will allow for
   * canonicalization (or "unique-ifying") of the information
   * contained in this event.  Use of this method is intended to
   * conserve memory by removing duplicate copies of event information
   * in different event objects.
   *
   * @param canonical a reflexive map of objecs representing event
   * information: for each unique key-value pair, the value is a
   * reference to the key
   *
   * @return 1 if the backing <code>Sexp</code> was a list and was therefore
   * canonicalized, 0 if it was a list but was not canonicalized (and had to
   * be added to <code>canonical</code>) or -1 if this event was a
   * <code>Symbol</code> and was therefore not even eligible for
   * canonicalization
   */
  public int canonicalize(Map canonical) {
    if (event.isSymbol()) // only lists are eligible for canonicalization
      return -1;
    Sexp oldEvent = event;
    event = event.getCanonical(canonical);
    return (oldEvent != event) ? 1 : 0;
  }

  /**
   * If <code>size</code> is greater than <tt>1</tt>, this method
   * ensures that the underlying <code>Sexp</code> is a
   * <code>SexpList</code> (creating a new <code>SexpList</code> if
   * necessary) and pre-allocates space in that <code>SexpList</code>.
   * For efficiency when creating lookup <code>SexpEvent</code>
   * objects, once the underlying <code>Sexp</code> is a
   * <code>SexpList</code>, it will remain as such, even if cleared
   * and only one symbol is added.  However, when the
   * <code>copy</code> method is invoked, if the underlying
   * <code>Sexp</code> of this <code>SexpEvent</code> object is a list
   * with a single symbol <tt>sym</tt>, the returned
   * <code>SexpEvent</code> object will have a "canonicalized" underlying
   * <code>Sexp</code> that is <tt>sym</tt> (that is, the list of a single
   * symbol will be turned into the symbol itself).  This scheme allows
   * a lookup object using a pre-allocated list for time efficiency to
   * be transformed into a space-efficient object suitable for persistent
   * storage.
   *
   * @param size the amount of space to pre-allocate for the underlying
   * list of <code>Sexp</code> components
   * @see #copy
   */
  public void ensureCapacity(int size) {
    if (event == null || event.isSymbol()) {
      if (size > 1)
	event = new SexpList(size);
    }
    else
      event.list().ensureCapacity(size);
  }

  /**
   * Since there is only one type supported by this class, this method
   * simply calls {@link #ensureCapacity(int) ensureCapacity(size)}.
   */
  public void ensureCapacity(int type, int size) { ensureCapacity(size); }

  /**
   * Clears the data stored in this complex event object.
   */
  public void clear() {
    if (event.isSymbol())
      event = null;
    else
      event.list().clear();
  }

  /**
   * Writes a representation of this object to the specified object output
   * stream.
   * @param out the stream to which to write a representation of this object
   * @throws IOException if there is a problem writing to the specified
   * output stream
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(event);
  }

  /**
   * Reconstructs the {@link SexpEvent} object that was serialized using
   * {@link #writeExternal(ObjectOutput)}.
   *
   * @param in the input stream from which to reconstruct a {@link SexpEvent}
   * object
   * @throws IOException if there is a problem reading from the specified
   * input stream
   * @throws ClassNotFoundException if the concrete type of the serialized
   * object read from the specified input stream cannot be found 
   */
  public void readExternal(ObjectInput in)
    throws IOException, ClassNotFoundException {
    event = (Sexp)in.readObject();
  }
}
