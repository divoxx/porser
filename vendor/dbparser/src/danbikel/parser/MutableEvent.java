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

import java.util.*;

/**
 * Provides additional methods to those of <code>Event</code> that
 * permit modification of the event object.  For efficiency, it is
 * recommended that implementations of this interface implement the
 * optional <code>ensureCapacity</code> methods, which will allow
 * efficient re-use of one <code>MutableEvent</code> object for
 * lookups in a collection of <code>Event</code> objects.
 */
public interface MutableEvent extends Event {
  /**
   * Adds the specified object to this event.  The specified object
   * must be of a type that this event is capable of collecting; that is,
   * <pre>
   * this.typeIndex(obj.getClass())
   * </pre>
   * must not return <tt>-1</tt>.
   * <p>
   * If an implementation of this interface collects components that
   * are primitive type values, then these values should be wrapped
   * in their corresponding wrapper classes.  For example, if an
   * implementation of this interface accepts <code>int</code> values,
   * they should be passed as <code>Integer</code> objects to this method.
   * At present, an <code>Event</code> implementation cannot be
   * designed accept both a primitive type and its associated wrapper
   * class' type (this is, of course, not a serious limitation).
   *
   * @return this object
   * @exception ClassCastException if this event does not support the
   * run-time type of the specified object
   */
  public MutableEvent add(Object obj);

  /**
   * Adds the specified object of the specified type to this event.  The
   * specified object must be of the type specified; that is, the expression
   * <pre>
   * this.typeIndex(obj.getClass()) == type
   * </pre>
   * must be <tt>true</tt>.
   * <p>
   * If an implementation of this interface collects components that
   * are primitive type values, then these values should be wrapped
   * in their corresponding wrapper classes.  For example, if an
   * implementation of this interface accepts <code>int</code> values,
   * they should be passed as an <code>Integer</code> objects to this method.
   * At present, an <code>Event</code> implementation cannot be
   * designed accept both a primitive type and its associated wrapper
   * class' type (this is, of course, not a serious limitation).
   *
   * @return this object
   * @exception ClassCastException if this event does not support the
   * run-time type of the specified object or if the specified object
   * is not of the specified type
   */
  public MutableEvent add(int type, Object obj);

  /**
   * Pre-allocates space for all abstract lists in this event (optional
   * operation).
   *
   * @param size the size to pre-allocate for all abstract lists of this event
   */
  public void ensureCapacity(int size);
  /**
   * Pre-allocates space for the abstract list of the specified type
   * (optional operation).
   *
   * @param type the type of underlying abstract list for which to pre-allocate
   * space
   * @param size the size to pre-allocate for the specified type of abstract
   * list
   */
  public void ensureCapacity(int type, int size);

  /**
   * Clears all components from this event.
   */
  public void clear();
}
