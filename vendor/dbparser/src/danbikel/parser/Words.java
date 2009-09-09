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

import java.util.Map;

/**
 * Provides static methods to create {@link Word} instances via an internal
 * {@link WordFactory} instance.  The static methods of this class have
 * identical signatures to those of the {@link WordFactory} interface.  The
 * concrete type of the internal {@link WordFactory} instance is determined by
 * the value of the {@link Settings#wordFactoryClass} setting.
 *
 * @see WordFactory
 * @see Settings#wordFactoryClass
 */
public class Words {

  private final static String className = Words.class.getName();

  private static WordFactory factory = getFactory();

  private static WordFactory getFactory() {
    WordFactory factory;
    String wordFactStr = Settings.get(Settings.wordFactoryClass);
    if (wordFactStr != null) {
      try {
	factory = (WordFactory)Class.forName(wordFactStr).newInstance();
      }
      catch (Exception e) {
	System.err.println(className + ": error creating " +
			   "instance of " + wordFactStr + ":\n\t" + e +
			   "\n\tusing DefaultWordFactory instead");
	factory = new DefaultWordFactory();
      }
    }
    else {
      System.err.println(className + ": error: the property " +
			 Settings.wordFactoryClass + " was not set;\n\t" +
			 "using DefaultWordFactory");
      factory = new DefaultWordFactory();
    }
    return factory;
  }

  static {
    Settings.Change change = new Settings.Change() {
      public void update(Map<String, String> changedSettings) {
	if (changedSettings.containsKey(Settings.wordFactoryClass)) {
	  factory = getFactory();
	}
      }
    };
    Settings.register(Words.class, change, null);
  }

  private Words() { }

  /**
   * Returns a new {@link Word} instance constructed from the specified
   * S-expression.
   *
   * @param s the S-expression from which to construct a new {@link Word}
   *          instance
   * @return a new {@link Word} instance constructed from the specified
   *         S-expression
   *
   * @see WordFactory#get(Sexp)
   * @see Word#Word(Sexp)
   */
  public static Word get(Sexp s) {
    return factory.get(s);
  }

  /**
   * Returns a new {@link Word} instance constructed from the specified word and
   * tag symbols.
   *
   * @param word the word itself
   * @param tag  the word's part of speech
   * @return a new {@link Word} instance constructed from the specified word and
   *         tag symbols
   *
   * @see WordFactory#get(Symbol,Symbol)
   * @see Word#Word(Symbol,Symbol)
   */
  public static Word get(Symbol word, Symbol tag) {
    return factory.get(word, tag);
  }

  /**
   * Returns a new {@link Word} instance constructed from the specified word,
   * tag and feature-vector symbols.
   *
   * @param word     the word itself
   * @param tag      the word's part of speech
   * @param features the word's feature vector (see {@link WordFeatures})
   * @return a new {@link Word} instance constructed from the specified word and
   *         tag symbols
   *
   * @see WordFactory#get(Symbol,Symbol,Symbol)
   * @see Word#Word(Symbol,Symbol,Symbol)
   */
  public static Word get(Symbol word, Symbol tag, Symbol features) {
    return factory.get(word, tag, features);
  }
}