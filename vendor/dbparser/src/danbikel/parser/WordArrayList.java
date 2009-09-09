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

class WordArrayList extends FixedSizeArrayList implements WordList {
  public WordArrayList(int size) {
    super(size);
  }
  public WordArrayList(Collection c) {
    super(c);
  }

  public Word getWord(int index) {
    return (Word)get(index);
  }

  /**
   * Returns a copy of this list.
   * <p>
   * <b>Warning</b>: The returned copy is <i>not</i> a deep copy.  That is,
   * the <code>Word</code> objects in the returned list are not copies of the
   * <code>Word</code> objects from the this list.
   */
  public WordList copy() {
    int size = data.length;
    WordList newList = new WordArrayList(size);
    for (int i = 0; i < size; i++)
      //newList.set(i, getWord(i).copy());
      newList.set(i, getWord(i));
    return newList;
  }

  public Sexp toSexp() {
    int size = size();
    if (size == 0)
      return SexpList.emptyList;
    SexpList newList = new SexpList(size);
    for (int i = 0; i < size; i++)
      newList.add(getWord(i).toSexp());
    return newList;
  }
}
