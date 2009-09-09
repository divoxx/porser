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

/**
 * Provides a nonterminal mapping scheme that, when applied to
 * previously-generated modifiers, allows for emulation of Michael Collins'
 * modifier-generation model.
 * <p/>
 * <b>N.B.</b>: The {@link #mapPrevMod(Symbol)} static method has been
 * deprecated. Classes should use the more flexible mechanism provided by the
 * {@link NTMapper} class.
 *
 * @see #map(Symbol)
 * @see NTMapper
 * @see Settings#prevModMapperClass
 */
public class Collins implements NonterminalMapper {
  /**
   * The start symbol equivalence class.
   */
  public final static Symbol startSym = Language.training().startSym();
  /**
   * The conjunction equivalence class.
   */
  public final static Symbol conjSym = Symbol.add("CC");
  /**
   * The punctuation equivalence class.
   */
  public final static Symbol puncSym = Symbol.add(",");
  /**
   * The miscellaneous equivalence class.
   */
  public final static Symbol miscSym = Language.training().stopSym();

  /**
   * Maps the specified previous modifier to one of four equivalence classes.
   *
   * @param prevMod the previous modifier to be mapped
   * @return a mapped version of the specified previous modifier
   *
   * @deprecated Classes should now use {@link NTMapper#map(Symbol)}, since the
   *             {@link NTMapper} class provides the flexibility of using an
   *             internal {@link NonterminalMapper} instance created at run-time
   *             according to the {@link Settings#prevModMapperClass} setting.
   *
   * @see NTMapper
   * @see NonterminalMapper
   * @see Settings#prevModMapperClass
   */
  public static Symbol mapPrevMod(Symbol prevMod) {
    if (prevMod == startSym)
      return startSym;
    else if (Language.treebank.isConjunction(prevMod))
      return conjSym;
    else if (Language.treebank.isPunctuation(prevMod))
      return puncSym;
    else
      return miscSym;
  }

  /**
   * Maps the specified nonterminal to one of four equivalence classes.
   * The mapping is determined by the following sequence of cases:
   * <blockquote>
   * <table border>
   * <tr><td rowspan=4><code>map(nonterminal) =</code></td>
   *     <td>{@link #startSym}</td>
   *     <td>if <code>nonterminal == Language.training().startSym()</code></td>
   * </tr>
   * <tr><td>{@link #conjSym}</td>
   *     <td>if <code>Language.treebank().isConjunction(nonterminal)</code></td>
   * </tr>
   * <tr><td>{@link #puncSym}</td>
   *     <td>if <code>Language.treebank().isPunctuation(nonterminal)</code></td>
   * </tr>
   * <tr><td>{@link #miscSym}</td>
   *     <td>otherwise</td>
   * </tr>
   * </table>
   * </blockquote>
   *
   * @param nonterminal the nonterminal to be mapped
   * @return the specified nonterminal mapped to one of four possible
   * equivalence classes
   *
   * @see Training#startSym()
   * @see Treebank#isConjunction(Symbol)
   * @see Treebank#isPunctuation(Symbol)
   */
  public Symbol map(Symbol nonterminal) {
    if (nonterminal == startSym)
      return startSym;
    else if (Language.treebank.isConjunction(nonterminal))
      return conjSym;
    else if (Language.treebank.isPunctuation(nonterminal))
      return puncSym;
    else
      return miscSym;
  }
}
