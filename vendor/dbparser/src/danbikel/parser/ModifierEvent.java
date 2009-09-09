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
import java.io.*;
import java.util.*;

/**
 * A class to represent the modifier generation event implicit in the models
 * supported by this parsing package.  The class {@link Trainer} counts
 * such events, from which other events are derived.
 */
public class ModifierEvent implements TrainerEvent, Cloneable {
  private final static Symbol leftSym = Constants.leftSym;
  private final static Symbol rightSym = Constants.rightSym;
  private final static Symbol trueSym = Constants.trueSym;

  // data members
  // N.B.: IF ANY MORE DATA MEMBERS ARE ADDED, make sure to update
  // the copy method
  private Word modHeadWord;
  private Word headWord;
  private Symbol modifier;
  private SexpList previousMods;
  private WordList previousWords;
  private Symbol parent;
  private Symbol head;
  private Subcat subcat;
  private Word prevPunc;
  private Word prevConj;
  private boolean isConjPConj;
  private boolean verbIntervening;
  private boolean headAdjacent;
  private boolean side;

  /**
   * Constructs a new object from the specified S-expression. The
   * <code>Sexp</code> must be an instance of a list with the following format:
   * <pre>
   * (modHeadWord headWord modifier previousMods parent head subcat
   *  verbIntervening side)
   * </pre>
   * where
   * <ul>
   * <li> <tt>modHeadWord</tt> is  a list of the form accepted by
   * {@link Word#Word(Sexp)}
   * <li> <tt>headWord</tt> is a list of the form accepted by
   * {@link Word#Word(Sexp)}
   * <li> <tt>modifier</tt> is the nonterminal label of the modifier
   * <li> <tt>previousMods</tt> is a list of previous modifying  nonterminal
   * labels
   * <li> <tt>parent</tt> is the parent nonterminal label
   * <li> <tt>head</tt> is the head child nonterminal label
   * <li> <tt>subcat</tt> is an ordered list of nonterminals representing
   * arguments that have yet to be generated
   * <li> <tt>verbIntervening</tt> is one of {{@link Constants#trueSym},
   * {@link Constants#falseSym}}
   * <li> <tt>side</tt> is one of {{@link Constants#leftSym},
   * {@link Constants#rightSym}}
   * </ul>
   */
  public ModifierEvent(Sexp sexp) {
    this(Words.get(sexp.list().get(0)),
	 Words.get(sexp.list().get(1)),
	 sexp.list().symbolAt(2),
	 SexpList.getCanonical(sexp.list().listAt(3)),
	 WordListFactory.newList(sexp.list().listAt(4)),
	 sexp.list().symbolAt(5),
	 sexp.list().symbolAt(6),
	 sexp.list().listAt(7),
	 sexp.list().symbolAt(8) == trueSym,
	 (sexp.list().symbolAt(9) == leftSym ?
	  Constants.LEFT : Constants.RIGHT));
  }

  /**
   * Constructs a new <code>ModifierEvent</code> object, settings its
   * data members to the values specified.
   *
   * @param modHeadWord the head word of the modifying nonterminal of this
   * modifier event
   * @param headWord the head word of the head child being modified
   * @param modifier the nonterminal label of the modifier
   * @param previousMods a list of previous modifying nonterminal labels
   * @param parent the parent nonterminal label
   * @param head the head child nonterminal label
   * @param subcat an ordered list of arguments of the head that have yet to
   * be generated
   * @param verbIntervening a boolean representing whether or a not a verb
   * has been generated anywhere in the subtrees between the head child
   * and the current modifier
   * @param side a boolean that is equal to {@link Constants#LEFT} if this
   * modifier lies on the left side of the head child or equal to
   * {@link Constants#RIGHT} if this modifier lies on the right side
   */
  public ModifierEvent(Word modHeadWord,
		       Word headWord,
		       Symbol modifier,
		       SexpList previousMods,
		       WordList previousWords,
		       Symbol parent,
		       Symbol head,
		       SexpList subcat,
		       boolean verbIntervening,
		       boolean side) {
    this(modHeadWord, headWord, modifier, previousMods, previousWords, parent,
	 head, Subcats.get(subcat), verbIntervening, side);
  }

  /**
   * Constructs a new <code>ModifierEvent</code> object, settings its
   * data members to the values specified.
   *
   * @param modHeadWord the head word of the modifying nonterminal of this
   * modifier event
   * @param headWord the head word of the head child being modified
   * @param modifier the nonterminal label of the modifier
   * @param previousMods a list of previous modifying nonterminal labels
   * @param parent the parent nonterminal label
   * @param head the head child nonterminal label
   * @param subcat a set of arguments on the specified side of the head that
   * have yet to be generated
   * @param verbIntervening a boolean representing whether or a not a verb
   * has been generated anywhere in the subtrees between the head child
   * and the current modifier
   * @param side a boolean that is equal to {@link Constants#LEFT} if this
   * modifier lies on the left side of the head child or equal to
   * {@link Constants#RIGHT} if this modifier lies on the right side
   */
  public ModifierEvent(Word modHeadWord,
		       Word headWord,
		       Symbol modifier,
		       SexpList previousMods,
		       WordList previousWords,
		       Symbol parent,
		       Symbol head,
		       Subcat subcat,
		       boolean verbIntervening,
		       boolean side) {
    set(modHeadWord, headWord, modifier, previousMods, previousWords, parent,
	head, subcat, verbIntervening, side);
  }

  /**
   * Constructs a new <code>ModifierEvent</code> object for use when
   * outputting training events in the format of Mike Collins&rsquo; parser,
   * settings its data members to the values specified.
   *
   * @param modHeadWord the head word of the modifying nonterminal of this
   * modifier event
   * @param headWord the head word of the head child being modified
   * @param modifier the nonterminal label of the modifier
   * @param previousMods a list of previous modifying nonterminal labels
   * @param parent the parent nonterminal label
   * @param head the head child nonterminal label
   * @param subcat a set of arguments on the specified side of the head that
   * have yet to be generated
   * @param prevPunc the previously-generated punctuation word or
   * <code>null</code> if the last modifier was neither punctuation nor
   * a conjunction
   * @param prevConj the previously-generated conjunction or <code>null</code>
   * if the last modifier was neither punctuation nor a conjunction
   * @param isConjPConj indicates whether the previously-generated modifier
   * was a conjunction that was part of a conjoined phrase as per
   * the definitions of Mike Collins&rsquo; model
   * @param verbIntervening a boolean representing whether or a not a verb
   * has been generated anywhere in the subtrees between the head child
   * and the current modifier
   * @param headAdjacent indicates whether the current modifier is adjacent
   * to the head child
   * @param side a boolean that is equal to {@link Constants#LEFT} if this
   * modifier lies on the left side of the head child or equal to
   * {@link Constants#RIGHT} if this modifier lies on the right side
   *
   * @see Settings#outputCollins
   */
  public ModifierEvent(Word modHeadWord,
		       Word headWord,
		       Symbol modifier,
		       SexpList previousMods,
		       WordList previousWords,
		       Symbol parent,
		       Symbol head,
		       Subcat subcat,
		       Word prevPunc,
		       Word prevConj,
		       boolean isConjPConj,
		       boolean verbIntervening,
		       boolean headAdjacent,
		       boolean side) {
    set(modHeadWord, headWord, modifier, previousMods, previousWords, parent,
	head, subcat, prevPunc, prevConj, isConjPConj, verbIntervening,
	headAdjacent, side);
  }

  void canonicalize(Map map) {
    modHeadWord = (Word)getCanonical(map, modHeadWord);
    headWord = (Word)getCanonical(map, headWord);

    if (prevPunc != null)
      prevPunc = (Word)getCanonical(map, prevPunc);
    if (prevConj != null)
      prevConj = (Word)getCanonical(map, prevConj);

    for (int i = 0; i < previousWords.size(); i++)
      previousWords.set(i, (Word)getCanonical(map, previousWords.getWord(i)));
    previousWords = (WordList)getCanonical(map, previousWords);

    previousMods = (SexpList)getCanonical(map, previousMods);

    subcat = (Subcat)getCanonical(map, subcat);
  }

  Object getCanonical(Map map, Object obj) {
    Object mapObj = map.get(obj);
    if (mapObj == null) {
      map.put(obj, obj);
      return obj;
    }
    else
      return mapObj;
  }

  // accessors
  /** Returns the head word of the modifier of this modifier event. */
  public Word modHeadWord() { return modHeadWord; }
  /** Returns the head word of the head child being modified. */
  public Word headWord() { return headWord; }
  /** Returns the nonterminal label of this modifier event. */
  public Symbol modifier() { return modifier; }
  /** Returns a list of modifiers that have already been generated. */
  public SexpList previousMods() { return previousMods; }
  /**
   * Returns a list of the head words of modifiers that have already been
   * generated.
   */
  public WordList previousWords() { return previousWords; }
  /** Returns the parent nonterminal label. */
  public Symbol parent() { return parent; }
  /** Returns the head child nonterminal label. */
  public Symbol head() { return head; }
  /**
   * Returns a list of arguments of the head child that have yet to be
   * generated.
   */
  public Subcat subcat() { return subcat; }

  /** Returns the previously-generated punctuation word if this modifier
      follows one, or <code>null</code>. */
  public Word prevPunc() { return prevPunc; }

  /** Returns the previously-generated conjunction if this modifier is conjoined
      with the head of the phrase, or <code>null</code> if this modifier is
      not conjoined. */
  public Word prevConj() { return prevConj; }

  /** Returns the boolean that indicates whether the modifier is a conjunction
      that is part of a conjunction phrase. */
  public boolean isConjPConj() { return isConjPConj; }

  /**
   * Returns whether a verb has been generated in any of the subtrees generated
   * between the current modifier and the head child.
   */
  public boolean verbIntervening() { return verbIntervening; }

  /**
   * Returns whether the current modifier is adjacent to the head child.
   * This method is only appropriate when outputting Collins-format
   * trainer events.
   * @return whether the current modifier is adjacent to the head child.
   *
   * @see Settings#outputCollins
   */
  public boolean headAdjacent() { return headAdjacent; }

  /**
   * Returns the value of {@link Constants#LEFT} if this modifier lies on the
   * left side of the head child, or the value of {@link Constants#RIGHT} if
   * this modifier lies on the right side.
   */
  public boolean side() { return side; }

  /**
   * Sets the head word of the modifier.
    * @param modHeadWord the head word of the modifier
   */
  public void setModHeadWord(Word modHeadWord) {
    this.modHeadWord = modHeadWord;
  }
  /**
   * Sets the head word of the head child and parent.
   * @param headWord the head word
   */
  public void setHeadWord(Word headWord) {
    this.headWord = headWord;
  }

  // package-level access mutators
  void setModifier(Symbol modifier) {
    this.modifier = modifier;
  }
  void setPreviousMods(SexpList previousMods) {
    this.previousMods = previousMods;
  }

  /**
   * Sets the previous words list.
   * @param previousWords the previous words list
   */
  public void setPreviousWords(WordList previousWords) {
    this.previousWords = previousWords;
  }
  void setParent(Symbol parent) {
    this.parent = parent;
  }
  void setHead(Symbol head) {
    this.head = head;
  }
  void setSubcat(Subcat subcat) {
    this.subcat = subcat;
  }

  void setIsConjPConj(boolean isConjPConj) {
    this.isConjPConj = isConjPConj;
  }

  void setVerbIntervening(boolean verbIntervening) {
    this.verbIntervening = verbIntervening;
  }
  void setSide(boolean side) {
    this.side = side;
  }
  void set(Word modHeadWord,
	   Word headWord,
	   Symbol modifier,
	   SexpList previousMods,
	   WordList previousWords,
	   Symbol parent,
	   Symbol head,
	   Subcat subcat,
	   boolean verbIntervening,
	   boolean side) {
    this.modHeadWord = modHeadWord;
    this.headWord = headWord;
    this.modifier = modifier;
    this.previousMods = SexpList.getCanonical(previousMods);
    this.previousWords = previousWords;
    this.parent = parent;
    this.head = head;
    this.subcat = subcat;
    this.verbIntervening = verbIntervening;
    this.side = side;
  }

  void set(Word modHeadWord,
	   Word headWord,
	   Symbol modifier,
	   SexpList previousMods,
	   WordList previousWords,
	   Symbol parent,
	   Symbol head,
	   Subcat subcat,
	   Word prevPunc,
	   Word prevConj,
	   boolean isConjPConj,
	   boolean verbIntervening,
	   boolean headAdjacent,
	   boolean side) {
    this.modHeadWord = modHeadWord;
    this.headWord = headWord;
    this.modifier = modifier;
    this.previousMods = SexpList.getCanonical(previousMods);
    this.previousWords = previousWords;
    this.parent = parent;
    this.head = head;
    this.subcat = subcat;
    this.prevPunc = prevPunc;
    this.prevConj = prevConj;
    this.isConjPConj = isConjPConj;
    this.verbIntervening = verbIntervening;
    this.headAdjacent = headAdjacent;
    this.side = side;
  }

  /**
   * Returns <code>true</code> if the specified object is an instance of
   * a <code>ModifierEvent</code> object containing data members which are all
   * pairwise-equal with the data members of this <code>ModifierEvent</code>
   * object, according to each data member's <code>equals(Object)</code> method.
   */
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ModifierEvent))
      return false;
    ModifierEvent other = (ModifierEvent)o;
    boolean modHeadWordsEqual =
      (modHeadWord == null ? other.modHeadWord == null :
       modHeadWord.equals(other.modHeadWord));
    boolean headWordsEqual =
      (headWord == null ? other.headWord == null :
       headWord.equals(other.headWord));
    boolean prevPuncEqual =
      (prevPunc == null ? other.prevPunc == null :
       prevPunc.equals(other.prevPunc));
    boolean prevConjEqual =
      (prevConj == null ? other.prevConj == null :
       prevConj.equals(other.prevConj));
    return (modHeadWordsEqual &&
	    headWordsEqual &&
	    prevPuncEqual &&
	    prevConjEqual &&
	    modifier == other.modifier &&
	    previousMods.equals(other.previousMods) &&
	    previousWords.equals(other.previousWords) &&
	    parent == other.parent &&
	    head == other.head &&
	    subcat.equals(other.subcat) &&
	    isConjPConj == other.isConjPConj &&
	    verbIntervening == other.verbIntervening &&
	    headAdjacent == other.headAdjacent &&
	    side == other.side);
  }

  /**
   * Returns an S-expression of the form accepted by
   * {@link ModifierEvent#ModifierEvent(Sexp)}.
   */
  public String toString() {
    return "(" + modHeadWord + " " + headWord +
      " " + modifier + " " + previousMods + " " + previousWords.toSexp() +
      " " + parent + " " + head + " " +
      subcat.toSexp() + " " + verbIntervening + " " +
      (side == Constants.LEFT ? leftSym : rightSym) + ")";
  }

  /**
   * Returns the hash code of this object, calculated from the hash codes
   * of all its data members.
   */
  public int hashCode() {
    int code = 0;
    if (modHeadWord != null)
      code = modHeadWord.hashCode();
    if (headWord != null)
      code = (code << 2) ^ headWord.hashCode();
    if (prevPunc != null)
      code = (code << 2) ^ prevPunc.hashCode();
    if (prevConj != null)
      code = (code << 2) ^ prevConj.hashCode();
    code = (code << 2) ^ modifier.hashCode();
    code = (code << 2) ^ previousMods.hashCode();
    code = (code << 2) ^ previousWords.hashCode();
    code = (code << 2) ^ parent.hashCode();
    code = (code << 2) ^ head.hashCode();
    code = (code << 2) ^ subcat.hashCode();
    int booleansCode = (((verbIntervening ? 1 : 0) << 1) |
			(side ? 1 : 0));
    // doesn't matter if we leave out the head adjacent bit
    // and the isConjPConj bit
    code = (code << 2) | booleansCode;
    return code;
  }

  /** Returns a deep copy of this object. */
  public Object clone() {
    return copy();
  }
  /** Returns a deep copy of this object. */
  public TrainerEvent copy() {
    return new ModifierEvent(modHeadWord.copy(),
			     headWord.copy(),
			     modifier,
			     new SexpList(previousMods),
			     previousWords.copy(),
			     parent,
			     head,
			     (Subcat)subcat.copy(),
			     prevPunc,
			     prevConj,
			     isConjPConj,
			     verbIntervening,
			     headAdjacent,
			     side);
  }

  /**
   * Returns a shallow copy of this event.
   * @return a shallow copy of this event
   */
  public TrainerEvent shallowCopy() {
    return new ModifierEvent(modHeadWord, headWord, modifier,
			     previousMods, previousWords, parent, head,
			     subcat, prevPunc, prevConj, isConjPConj,
			     verbIntervening, headAdjacent, side);
  }

  private void readObject(ObjectInputStream in)
  throws IOException, ClassNotFoundException {
    in.defaultReadObject();
  }
  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
  }
}
