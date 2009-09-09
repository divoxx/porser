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

import danbikel.util.*;
import danbikel.lisp.*;
import java.util.*;
import java.io.*;

/**
 * A &ldquo;broken&rdquo; version of {@link SubcatBag} that precisely reflects
 * the details specified in Collins&rsquo; thesis (used for
 * &ldquo;clean-room&rdquo; implementation).  <b>Catuion</b>: Changes
 * made to the way {@link SubcatBag} operates may have rendered this class
 * even more &ldquo;broken&rdquo; than originally intended.
 * <p/>
 * Provides a bag implementation of subcat requirements (a <i>bag</i> is a set
 * that allows multiple occurrences of the same item).  This list of all
 * argument nonterminals is provided by {@link Training#argNonterminals} map.
 * As a special case, this class also supports gap requirements, that is,
 * requirements equal to {@link Training#gapAugmentation}. This class also
 * provides a separate bin for miscellaneous subcat requirements, such as those
 * that can be matched via {@link danbikel.parser.lang.AbstractTraining#headSym}.
 *  All nonterminal requirements are stripped of any augmentations before being
 * counted in this subcat bag.
 * <p/>
 * The comment for the <code>toSexp</code> method describes the way in which
 * this class represents miscellaneous requirements.
 * <p/>
 * <b>Bugs</b>: <ol> <li>This class provides special-case bins for counting gap
 * and miscellaneous subcat requirements.  If this parsing package is expanded
 * to include additional elements that are possible generative requirements, and
 * these elements do not appear in {@link Training#argNonterminals}, unless it
 * is modified, this class will simply put these elements in the miscellaneous
 * bin. <li>This class cannot collect more than 127 total occurrences of
 * requirements.  This is well beyond the number of arguments ever postulated in
 * any human language, but <i>not</i> necessarily beyond the number of
 * generative requirements that might be needed by a future parsing model.  A
 * corollary of this limitation is that the number of occurrences of a
 * particular requirement may not exceed 127. </ol>
 *
 * @see Subcats
 * @see #toSexp()
 */
public class BrokenSubcatBag implements Subcat, Externalizable {
  // constants
  // index constants: make sure to update remove method if these change
  // (if it's necessary: see comment inside remove method code)
  private final static int sizeIdx = 0;
  private final static int miscIdx = 1;
  private final static int firstRealUid = 2; // must be greater than miscIdx!!!
  private final static int gapIdx = firstRealUid;
  private static int numUids;

  // static data members
  private static Map<Symbol, Integer> symbolsToInts =
    new danbikel.util.HashMap<Symbol, Integer>();
  private static Symbol stopSym = Language.training.stopSym();
  private static Symbol[] symbols;
  static {
    Symbol gapAugmentation = Language.training.gapAugmentation();
    Symbol argAugmentation = Language.training.defaultArgAugmentation();
    char delimChar = Language.treebank.canonicalAugDelimiter();

    int uid = gapIdx; // depends on gapIdx being equal to firstRealUid
    // kind of a hack: put an entry for gaps (which are "requirements"
    // that can be thrown into subcats);
    // note that uid of gap is firstRealUid (see comment inside remove method)
    symbolsToInts.put(gapAugmentation, new Integer(uid++));

    // grabbed the following by piping output of
    //   danbikel.parser.Training -q wsj-02-21.mrg
    // to
    //   danbikel.parser.util.PrintNTs -tags
    // using the "dumb" PP head-finding rule that says to pick first
    // child after head (i.e., the rule of the form "(head 1)")
    String[] argLabels = {
      ",", ":", "ADJP", "ADVP", "CD", "DT", "FRAG", "INTJ", "JJ", "JJS",
      "-LRB-", "NNP", "NP", "PP", "PRN", "PRP$", "RB", "RBS", "-RRB-", "S",
      "SBAR", "SBARQ", "SG", "SQ", "UCP", "VP", "WHNP", "X"
    };
    // add all labels into a set, in case we have two labels that map to the
    // same canonical version (such as "S" and "SG", as shown above); this is
    // just a little bit of (sometimes necessary) defensive programming
    Set<Symbol> argLabelSet = new HashSet<Symbol>();
    for (int i = 0; i < argLabels.length; i++) {
      String argLabelStr = argLabels[i] + "-A";
      Symbol canonicalArg =
	Language.training.getCanonicalArg(Symbol.get(argLabelStr));
      argLabelSet.add(canonicalArg);
    }
    for (Symbol argLabel : argLabelSet) {
      symbolsToInts.put(argLabel, new Integer(uid++));
    }
    numUids = uid;

    symbols = new Symbol[numUids];
    for (Map.Entry<Symbol, Integer> entry : symbolsToInts.entrySet()) {
      Symbol symbol = (Symbol) entry.getKey();
      uid = ((Integer) entry.getValue()).intValue();
      symbols[uid] = symbol;
    }
    symbols[miscIdx] = Symbol.get(stopSym.toString() +
				  delimChar + argAugmentation);
  }

  private static HashMapInt<Symbol> fastUidMap = new HashMapInt<Symbol>();
  private static boolean canUseFastUidMap = false;

  public static synchronized void setUpFastUidMap(CountsTable nonterminals) {
    if (canUseFastUidMap)
      return;
    fastUidMap.put(Language.training.gapAugmentation(), gapIdx);
    for (Object ntObj : nonterminals.keySet()) {
      Symbol nt = (Symbol)ntObj;
      int uid = getUid(nt);
      fastUidMap.put(nt, uid);
    }
    canUseFastUidMap = true;
  }

  // data member
  private byte[] counts;

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

  /** Constructs an empty subcat. */
  public BrokenSubcatBag() {
    counts = new byte[numUids];
    for (int i = 0; i < counts.length; i++)
      counts[i] = 0;
  }

  /**
   * Constructs a subcat bag containing the number of occurrences of
   * the symbols of <code>list</code>.
   *
   * @param list a list of <code>Symbol</code> objects to be added to this
   * subcat bag
   */
  public BrokenSubcatBag(SexpList list) {
    this();
    addAll(list);
  }

  /**
   * Adds the specified requirement to this subcat bag.  There are
   * separate bins maintained for each of the nonterminals in the list
   * returned by {@link Training#argNonterminals}, as
   * well as a bin for gap augmentations (that is, requirements that are
   * equal to {@link Training#gapAugmentation}) and a miscellaneous bin
   * for all other requirements, such as those that can be matched via
   * {@link danbikel.parser.lang.AbstractTraining#headSym}.
   *
   * @param requirement the requirement to add to this subcat bag
   */
  public Subcat add(Symbol requirement) {
    if (validRequirement(requirement)) {
      counts[sizeIdx]++;
      counts[getUid(requirement)]++;
    }
    return this;
  }

  /**
   * Adds each of the symbols of <code>list</code> to this subcat bag,
   * effectively calling {@link #add(Symbol)} for each element of
   * <code>list</code>.
   *
   * @param list a list of <code>Symbol</code> objects to be added to this
   * subcat bag
   * @exception ClassCastException if one or more elements of <code>list</code>
   * is not an instance of <code>Symbol</code>
   */
  public boolean addAll(SexpList list) {
    int listLen = list.length();
    for (int i = 0; i < listLen; i++)
      add(list.symbolAt(i));
    return listLen > 0;
  }

  /**
   * Removes the specified requirement from this subcat bag, if possible.
   * If the specified requirement is a nonterminal, then it is only
   * removed if it is an argument nonterminal, that is, if
   * <code>Language.training().isArgumentFast(requirement)</code>
   * returns <code>true</code>, and if this subcat contained at least
   * one instance of that nonterminal.
   *
   * @return <code>true</code> if this subcat bag contained at least one
   * instance of the specified requirement and it was removed,
   * <code>false</code> otherwise
   *
   * @see Training#isArgumentFast(Symbol)
   */
  public boolean remove(Symbol requirement) {
    int uid = getUid(requirement);

    // if the uid is of an actual nonterminal (either greater than
    // firstRealUid, which is used for gap requirements, or equal to
    // miscIdx) and if the specified requirement is not marked as an
    // argument, return false
    if ((uid == miscIdx || uid > firstRealUid) &&
	!Language.training.isArgumentFast(requirement))
      return false;

    if (counts[uid] == 0)
      return false;
    else {
      counts[sizeIdx]--;
      counts[uid]--;
      return true;
    }
  }

  private static final int getUid(Symbol requirement) {
    if (canUseFastUidMap) {
      MapToPrimitive.Entry fastUidMapEntry = fastUidMap.getEntry(requirement);
      return fastUidMapEntry == null ? miscIdx : fastUidMapEntry.getIntValue();
    }
    Integer uidInteger =
      (Integer)symbolsToInts.get(Language.training.getCanonicalArg(requirement));
    if (uidInteger == null)
      return miscIdx;
    else
      return uidInteger.intValue();
  }

  /** Returns the number of requirements contained in this subcat bag. */
  public int size() {
    return counts[sizeIdx];
  }

  /**
   * Returns <code>true</code> if and only if there are zero requirements
   * in this subcat bag.
   */
  public boolean empty() {
    return size() == 0;
  }

  public boolean contains(Symbol requirement) {
    int uid = getUid(requirement);
    // if the uid is of an actual nonterminal (either greater than firstRealUid,
    // which is used for gap requirements, or equal to miscIdx) and if it's not
    // marked as an argument, return false
    //noinspection SimplifiableIfStatement
    if ((uid == miscIdx || uid > firstRealUid) &&
	!Language.training.isArgumentFast(requirement))
      return false;

    return counts[uid] > 0;
  }

  /**
   * Returns an itrerator over the elements of this subcat bag, returning
   * the canonical version of symbols for each the categories described in
   * {@link #add(Symbol)}; for each occurrence of a miscellaneous item
   * present in this subcat bag, the return value of {@link Training#stopSym}
   * is returned.
   */
  public Iterator iterator() {
    return new Itr();
  }

  /**
   * Returns a deep copy of this subcat bag.
   */
  public Event copy() {
    BrokenSubcatBag subcatCopy = new BrokenSubcatBag();
    subcatCopy.counts = (byte[])this.counts.clone();
    return subcatCopy;
  }

  /**
   * Computes the hash code for this subcat.
   */
  public int hashCode() {
    // do the efficient version if we can afford the 2-bit bit-shifts
    if (counts[sizeIdx] == 0)
      return 0;
    if (counts.length <= 16) {
      int code = 0;
      for (int i = counts.length - 1; i >= 0; i--)
	code = (code << 2) ^ counts[i];
      return code;
    }
    // otherwise do the slightly less efficient version
    else {
      int code = 0;
      for (int i = counts.length - 1; i >= 0; i--)
	code = (code * 31) + counts[i];
      return code;
    }
  }

  /**
   * Returns <code>true</code> if and only if the specified object is of
   * type <code>BrokenSubcatBag</code> and has the same number of requirement
   * categories and has the same counts for each of those requirement
   * categories.
   */
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof BrokenSubcatBag))
      return false;
    BrokenSubcatBag other = (BrokenSubcatBag)obj;
    if (counts.length != other.counts.length)
      return false;
    int len = counts.length;
    for (int i = 0; i < len; i++)
      if (counts[i] != other.counts[i])
	return false;
    return true;
  }

  /**
   * Returns a human-readable string representation of the
   * requirements contained in this bag.  Note that nonterminals that are
   * not in the miscellaneous bag will contain argument augmentations.
   */
  public String toString() {
    StringBuffer sb = new StringBuffer(6 * counts.length);
    sb.append("size=").append(counts[sizeIdx]).append(" ");
    for (int i = firstRealUid; i < counts.length; i++)
      sb.append(symbols[i]).append("=").append(counts[i]).append(" ");
    sb.append("misc=").append(counts[miscIdx]);
    return sb.toString();
  }


  private final class Itr implements Iterator {
    int totalCounter = BrokenSubcatBag.this.counts[sizeIdx];
    int countIdx = firstRealUid;
    int counter = BrokenSubcatBag.this.counts[countIdx];

    public boolean hasNext() {
      return totalCounter > 0;
    }

    public Object next() {
      if (totalCounter == 0)
	throw new NoSuchElementException();

      // if there are more counts at the current count index...
      if (counter > 0) {
	counter--;
	totalCounter--;
	return symbols[countIdx];
      }
      // else go hunting for the next place with non-zero counts
      countIdx++;
      while (countIdx < counts.length && counts[countIdx] == 0)
	countIdx++;
      if (countIdx == counts.length)
	countIdx = miscIdx;

      counter = counts[countIdx] - 1;
      totalCounter--;
      return symbols[countIdx];
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  };

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

  // methods to comply with the MutableEvent interface

  public MutableEvent add(Object obj) {
    return add((Symbol)obj);
  }
  public MutableEvent add(int type, Object obj) {
    return add((Symbol)obj);
  }
  /** This method does nothing and returns. */
  public void ensureCapacity(int size) { return; }
  /** This method does nothing and returns. */
  public void ensureCapacity(int type, int size) { return; }
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
  /** An alias for {@link #size()}. */
  public int numComponents() { return size(); }
  /** An alias for {@link #size()}. */
  public int numComponents(int type) { return size(); }
  /**
   * This method does nothing and returns -1, as no internal data to this
   * class can be canonicalized.
   */
  public int canonicalize(Map canonical) { return -1; }
  /** This method sets all counts of this subcat bag to zero. */
  public void clear() {
    for (int i = 0; i < counts.length; i++)
      counts[i] = 0;
  }
  /**
   * Gets the <code>index</code><sup>th</sup> components of this subcat bag.
   * <p>
   * <b>Efficiency note</b>: The time complexity of this method is linear in
   * the number of requirement types.
   *
   * @param type an unused type parameter (<code>Subcat</code> events only
   * support the type <code>Symbol</code>, so this argument is effectively
   * superfluous for this class)
   * @param index the index of the requirement to get
   * @return the <code>index</code><sup>th</sup> <code>Symbol</code> of this
   * subcat bag, as would be returned by the <code>index</code><sup>th</sup>
   * invocation of <code>next</code> from the iterator returned by
   * {@link #iterator()}
   */
  public Object get(int type, int index) {
    int totalCounter = size();
    if (index < 0 || index >= totalCounter)
      throw new IndexOutOfBoundsException();
    int countIdx = firstRealUid;
    for (; countIdx < counts.length && index >= counts[countIdx]; countIdx++)
      index -= counts[countIdx];
    if (countIdx == counts.length)
      countIdx = miscIdx;
    return symbols[countIdx];
  }

  /**
   * As per the contract of <code>Subcat</code>, this method returns a
   * <code>Sexp</code> such that an equivalent <code>BrokenSubcatBag</code>
   * object would result from the {@link #addAll(SexpList)} method being
   * invoked with this <code>Sexp</code> as its argument.
   * <p>
   * <b>N.B.</b>: For each occurrence of a miscellaneous item present
   * in this subcat bag, the returned list will contain the symbol
   * {@link Training#stopSym} augmented with the argument augmentation:
   * <pre>
   * Symbol.get({@link Training#stopSym()}.toString() +
   *            {@link Treebank#canonicalAugDelimiter()} +
   *            {@link Training#defaultArgAugmentation()});
   * </pre>
   */
  public Sexp toSexp() {
    int size = numComponents();
    SexpList list = new SexpList(size);
    for (int i = 0; i < size; i++)
      list.add((Symbol)get(0, i));
    return list;
  }

  public void writeExternal(ObjectOutput stream) throws IOException {
    stream.writeByte(size());
    stream.writeInt(counts.length - 1);
    for (int countIdx = 1; countIdx < counts.length; countIdx++) {
      stream.writeObject(symbols[countIdx]);
      stream.writeByte(counts[countIdx]);
    }
  }
  public void readExternal(ObjectInput stream)
    throws IOException, ClassNotFoundException {
    counts[sizeIdx] = stream.readByte();
    int numPairs = stream.readInt();
    for (int i = 0; i < numPairs; i++) {
      Symbol requirement = (Symbol)stream.readObject();
      counts[getUid(requirement)] = stream.readByte();
    }
  }

  public void become(Subcat other) {
    BrokenSubcatBag otherBag = (BrokenSubcatBag)other;
    System.arraycopy(otherBag.counts, 0, this.counts, 0,
		     otherBag.counts.length);
  }
}
