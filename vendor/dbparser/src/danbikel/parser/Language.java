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

import java.io.*;
import java.lang.reflect.*;
import java.util.Map;

import danbikel.lisp.*;

/**
 * Provides objects that perform functions specific to a particular language
 * and/or Treebank.  When the static method {@link #setLanguage} is called,
 * several objects from a language package are created and stored statically by
 * this class.  This scheme means that <code>Language.setLanguage</code> should
 * be called before any language-specific resources or methods are required,
 * typically early in the execution of a program.  A language package must
 * provide implementations for the following interfaces:
 * <ul>
 * <li>{@link WordFeatures}
 * <li>{@link Treebank}
 * <li>{@link HeadFinder}
 * <li>{@link Training}
 * </ul>
 * Upon initialization, this class will set the language to be the default
 * language, which is English, using classes from the default language package,
 * <code>danbikel.parser.english</code>, using the method {@link #setLanguage}.
 *
 * @see #setLanguage
 * @see Settings
 * @see Settings#language
 * @see Settings#languagePackage */
public class Language implements Serializable {
  private Language() {}

  /** The <code>Wordfeatures</code> object for the current language. */
  static WordFeatures wordFeatures;
  /** The <code>HeadFinder</code> object for the current language. */
  static HeadFinder headFinder;
  /** The <code>Treebank</code> object for the current language. */
  static Treebank treebank;
  /** The <code>Training</code> object for the current language. */
  static Training training;


  // accesssors for the above static objects (for classes outside this package)
  /** Gets the <code>WordFeatures</code> object for the current language. */
  public final static WordFeatures wordFeatures() { return wordFeatures; }
  /** Gets the <code>HeadFinder</code> object for the current language. */
  public final static HeadFinder headFinder() { return headFinder; }
  /** Gets the <code>Treebank</code> object for the current language. */
  public final static Treebank treebank() { return treebank; }
  /** Gets the <code>Training</code> object for the current language. */
  public final static Training training() { return training; }

  // a "mutable" constant
  private static String fileEncodingProperty =
    Settings.fileEncodingPrefix + Settings.get(Settings.language);

  static String encoding;
  static {
    encoding = Settings.get(fileEncodingProperty);
    if (encoding == null)
      encoding = System.getProperty("file.encoding");
  }

  /**
   * Gets the file encoding for the current language.<br>
   * If the value
   * <code>Settings.get(Settings.fileEncodingPrefix + Settings.language)</code>
   * is non-<code>null</code>, then it is used as the file encoding; otherwise,
   * the file encoding is to be the value of<br>
   * <code>System.getProperty(&quot;file.encoding&quot;)</code>.
   *
   * @see Settings#fileEncodingPrefix
   * @see Settings#language
   */
  public final static String encoding() { return encoding; }

  private static String lang;
  private static String langPackage;

  static {
    setLanguage();
    Settings.Change change = new Settings.Change() {
      public void update(Map<String, String> changedSettings) {
	if (changedSettings.containsKey(Settings.language) ||
	    changedSettings.containsKey(Settings.languagePackage)) {
	  fileEncodingProperty =
	    Settings.fileEncodingPrefix +
	    Settings.get(Settings.language);
	  encoding = Settings.get(fileEncodingProperty);
	  if (encoding == null)
	    encoding = System.getProperty("file.encoding");
	  setLanguage();
	  System.err.println(Language.class.getName() +
			     ": language has changed to " + getLanguage() +
			     ";\n\tnew encoding: " + encoding +
			     ";\n\tnew language classes:" +
			     "\n\t\t" + treebank().getClass().getName() +
			     "\n\t\t" + training().getClass().getName() +
			     "\n\t\t" + headFinder().getClass().getName() +
			     "\n\t\t" + wordFeatures().getClass().getName());
	}
      }
    };
    Settings.register(Language.class, change, null);
  }

  /**
   * Sets the language and language package using the values obtained from the
   * {@link Settings} class.  The language to be set is determined by the value
   * of the <code>parser.language</code> property stored in {@link Settings}.
   * The language package to be set is determined by the value of the
   * <code>parser.language.package</code> property stored in
   * <code>Settings</code>.
   * <p>
   * A language package is required to provide concrete subclasses of
   * the following abstract classes:
   * <ol>
   * <li>{@link WordFeatures}
   * <li>{@link Treebank}
   * <li>{@link HeadFinder}
   * <li>{@link Training}
   * </ol>
   * This method will create one object of each of the required language package
   * classes using the classes' respective default constructors.
   * The objects are created in the order listed above, so any
   * dependencies in a language package must be from later-instantiated to
   * earlier-instantiated classes.
   * <p>
   * The class names of the concrete classes in a language package are
   * assumed to be identical to those listed above, prepended with the
   * string
   * <pre>Settings.get(Settings.languagePackage)&nbsp;+&nbsp;"."</pre>
   * If a particular concrete subclass has a different name from the
   * abstract class it extends, the appropriate {@link Settings}
   * property must be set containing the <i>fully-qualified</i> version
   * of the class name:
   * <ul>
   * <li>{@link Settings#wordFeaturesClass}
   * <li>{@link Settings#treebankClass}
   * <li>{@link Settings#headFinderClass}
   * <li>{@link Settings#trainingClass}
   * </ul> */
  public static void setLanguage() {
    setLanguagePackage(Settings.get(Settings.language),
		       Settings.get(Settings.languagePackage));
  }

  private static void setLanguagePackage(String language,
					 String languagePackage) {
    // store the current language setting
    lang = language;
    langPackage = languagePackage;

    // finally, set static language components based on specified language
    try {

      // initialize WordFeatures object
      String wordFeaturesClass = Settings.get(Settings.wordFeaturesClass);
      if (wordFeaturesClass == null)
	wordFeaturesClass = languagePackage + ".WordFeatures";
      wordFeatures =
	(WordFeatures)Class.forName(wordFeaturesClass).newInstance();

      // initialize Treebank object
      String treebankClass = Settings.get(Settings.treebankClass);
      if (treebankClass == null)
	treebankClass = languagePackage + ".Treebank";
      treebank =
	(Treebank)Class.forName(treebankClass).newInstance();

      // initialize HeadFinder object
      String headFinderClass = Settings.get(Settings.headFinderClass);
      if (headFinderClass == null)
	headFinderClass = languagePackage + ".HeadFinder";
      headFinder =
	(HeadFinder)Class.forName(headFinderClass).newInstance();

      // initialize Training object
      String trainingClass = Settings.get(Settings.trainingClass);
      if (trainingClass == null)
	trainingClass = languagePackage + ".Training";
      training =
	(Training)Class.forName(trainingClass).newInstance();
    }
    catch (InstantiationException ie) {
      System.err.println(ie);
    }
    catch (IllegalAccessException iae) {
      System.err.println(iae);
    }
    catch (ClassNotFoundException cnfe) {
      System.err.println(cnfe);
    }
  }

  /** Gets the name of the current language. */
  public final static String getLanguage() { return lang; }
  /** Gets the name of the current language package. */
  public final static String getLanguagePackage() { return langPackage; }
}
