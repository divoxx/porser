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
import java.io.*;
import java.util.*;

/**
 * Implements subcats where requirements need to be met in the order
 * in which they are added to this subcat (the strictest form of a subcat).
 * The only loosening of restrictions in this implementation of a subcat
 * is that nonterminals are stripped of any gap augmentations before added
 * to this subcat frame.
 */
public class SubcatList extends SexpList implements Subcat, Externalizable {

  // N.B.: the internal list holds requirements (Symbol objects) in the reverse
  // order in which they were added, so that calls to remove are extremely
  // efficient


  /**
   * Construct a new empty subcat list.
   */
  public SubcatList() {
    super();
  }

  /**
   * Constructs a new subcat list from the requirements in the specified
   * {@link SexpList}.
   * @param list the list of requirements from which to construct this
   * subcat list
   */
  public SubcatList(SexpList list) {
    super(list);
    reverse();
  }

  private SubcatList(SexpList list, boolean reverse) {
    super(list);
    if (reverse)
      reverse();
  }

  public MutableEvent add(Object obj) { return add((Symbol)obj); }
  public MutableEvent add(int type, Object obj) { return add((Symbol)obj); }

  /**
   * A method to check if the specified requirement is valid. For this
   * class, a requirement is valid if it is either
   * {@link Training#gapAugmentation} or a symbol for which
   * {@link Training#isArgumentFast(Symbol)} returns <code>true</code>.
   * A subclass may override this method to allow for new or different
   * valid requirements.
   *
   * @param requirement the requirement to test
   * @return whether the specified requirement is valid
   */
  protected boolean validRequirement(Symbol requirement) {
    return
      requirement == Language.training.gapAugmentation() ||
      Language.training.isArgumentFast(requirement);
  }

  /**
   * Adds the specified requirement to this subcat list.  If
   * <code>requirement</code> is a nonterminal containing a gap
   * augmentation, the augmentation is removed before the requirement is added.
   * <p>
   * <b>Efficiency note</b>: The requirement is added to the beginning
   * of the internal list of this object--an O(n) operation--so as to
   * make the running time of {@link #remove(Symbol)} O(1).  The preferred
   * method of construction of this type of subcat is via the
   * {@link #addAll(SexpList)} method, which operates in time linear to
   * the size of the specified list.
   *
   * @param requirement the requirement to add
   * @return this <code>Subcat</code> object
   */
  public Subcat add(Symbol requirement) {
    if (validRequirement(requirement))
      super.add(0, Language.training.removeGapAugmentation(requirement));
    return this;
  }

  private void addSuper(Sexp requirement) {
    if (validRequirement((Symbol)requirement))
      super.add(Language.training.removeGapAugmentation(requirement));
  }

  /**
   * Adds the requirements (<code>Symbol</code> objects) of <code>list</code>
   * to this subcat list.  Functionally, the order of elements in
   * <code>list</code> is preserved, in that its first element will be the
   * first requirement of this subcat (provided that this subcat is empty
   * when this method is invoked).
   */
  public boolean addAll(SexpList list) {
    int oldSize = size();
    if (this.size() == 0) {
      // just add elements of list in reverse order to the backing list
      for (int i = list.size() - 1; i >= 0; i--)
	addSuper(list.get(i));
    }
    else {
      // this list needs to become (the valid requirements of)
      // the reverse of the specified list concatenated with the old
      // elements

      // copy current (soon to be old) elements
      SexpList old = (SexpList)this.deepCopy();
      // clear this list
      clear();
      // add reverse of specified new requirement list, checking for validity
      for (int i = list.size() - 1; i >= 0; i--)
	addSuper(list.get(i));
      // now add old elements in order, without checking anything
      int oldLen = old.length();
      for (int i = 0; i < oldLen; i++)
	super.add(old.get(i));
    }
    return oldSize == size();
  }

  /**
   * Removes the specified requirement from this subcat list, if possible.
   *
   * @param requirement the requirement to remove from this subcat list
   * @return <code>true</code> if the specified requirement was the next
   * requirement in this subcat and was removed, <code>false</code> otherwise
   */
  public boolean remove(Symbol requirement) {
    requirement = (Symbol)Language.training.removeGapAugmentation(requirement);
    if (requirement != last().symbol())
      return false;
    else {
      remove(size() - 1);
      return true;
    }
  }

  public boolean empty() { return size() == 0; }

  public boolean contains(Symbol requirement) {
    return contains((Sexp)requirement);
  }

  /**
   * Returns <code>true</code> if and only if the specified object is an
   * instance of <code>SubcatList</code> whose underlying list is equal
   * to that of this object, as determined by {@link SexpList#equals(Object)}.
   */
  public boolean equals(Object obj) {
    return obj instanceof SubcatList && super.equals(obj);
  }

  public Event copy() {
    return new SubcatList(this, false);
  }


  public Subcat getCanonical(boolean copyInto, Map<Subcat, Subcat> map) {
    Subcat mapElt = (Subcat)map.get(this);
    if (mapElt == null) {
      Subcat putInMap = copyInto ? (Subcat)this.copy() : this;
      map.put(putInMap, putInMap);
      return putInMap;
    }
    else {
      return mapElt;
    }
  }

  // override get method to preserve semantics of this subcat: last element
  // of the internal list is actually the first requirement
  public Sexp get(int index) {
    return super.get(size() - 1 - index);
  }

  // methods to comply with MutableEvent interface

  /**
   * This method returns the one class that <code>Subcat</code> objects
   * need to support: <code>Symbol.class</code>.
   */
  public Class getClass(int type) { return Symbol.class; }
  /**
   * Returns <tt>0</tt> if the specified class is equal to
   * <code>Symbol.class</code>, <tt>-1</tt> otherwise.
   */
  public int typeIndex(Class cl) {
    if (cl.equals(Symbol.class))
      return 0;
    else
      return -1;
  }
  /**
   * Returns 1 (<code>Subcat</code> objects only support <code>Symbol</code>
   * objects).
   */
  public int numTypes() { return 1; }

  /** An alias for {@link SexpList#size}. */
  public int numComponents() { return size(); }
  /** An alias for {@link SexpList#size}. */
  public int numComponents(int type) { return size(); }
  /** An alias for {@link SexpList#get(int)}. */
  public Object get(int type, int index) { return get(index); }
  /**
   * This method does nothing and returns -1, as no internal data to this
   * class can be canonicalized.
   */
  public int canonicalize(Map canonical) { return -1; }
  /**
   * As <code>Subcat</code> objects only support requirements of a single
   * type (<code>Symbol</code>), this method is an alias for
   * {@link SexpList#ensureCapacity(int)}.
   */
  public void ensureCapacity(int type, int size) {
    super.ensureCapacity(size);
  }

  public Sexp toSexp() {
    int size = numComponents();
    SexpList list = new SexpList(size);
    for (int i = size - 1; i >= 0; i--)
      list.add((Symbol)get(0, i));
    return list;
  }

  public void become(Subcat other) {
    SubcatList otherList = (SubcatList)other;
    clear();
    super.addAll(otherList);
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    // if there were additional data members in this subclass of
    // SexpList, we'd write them out here
  }

  public void readExternal(ObjectInput in)
    throws IOException, ClassNotFoundException {
    super.readExternal(in);
    // if there were additional data members in this subclass of
    // SexpList, we'd read them in here
  }
}
