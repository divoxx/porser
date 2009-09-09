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
import java.io.Serializable;
import java.util.Map;

/**
 * Static factory for <code>Subcat</code> objects.  This scheme allows
 * the type of <code>Subcat</code> object to be determined at run-time.
 * The type of subcat factory used is deteremined by the value of the
 * property {@link Settings#subcatFactoryClass}.
 *
 * @see SubcatFactory
 * @see Settings#subcatFactoryClass
 * @see Subcat
 */
public class Subcats implements Serializable {
  private static final String className = Subcats.class.getName();


  private Subcats() {}

  private static SubcatFactory factory = getFactory();

  private static SubcatFactory getFactory() {
    SubcatFactory factory;
    String subcatFactStr = Settings.get(Settings.subcatFactoryClass);
    if (subcatFactStr != null) {
      try {
	factory = (SubcatFactory)Class.forName(subcatFactStr).newInstance();
      }
      catch (Exception e) {
	System.err.println(className + ": error creating " +
			   "instance of " + subcatFactStr + ":\n\t" + e +
			   "\n\tusing SubcatBagFactory instead");
	factory = new SubcatBagFactory();
      }
    }
    else {
      System.err.println(className + ": error: the property " +
			 Settings.subcatFactoryClass + " was not set;\n\t" +
			 "using SubcatBagFactory");
      factory = new SubcatBagFactory();
    }
    return factory;
  }

  static {
    Settings.Change change = new Settings.Change() {
      public void update(Map<String, String> changedSettings) {
	if (changedSettings.containsKey(Settings.subcatFactoryClass)) {
	  factory = getFactory();
	}
      }
    };
    Settings.register(Subcats.class, change, null);
  }

  /**
   * Return a <code>Subcat</code> object created with its default constructor.
   */
  public static Subcat get() {
    return factory.get();
  }
  /**
   * Return a <code>Subcat</code> object created with its one-argument
   * constructor, using the specified list.
   *
   * @param list a list containing only <code>Symbol</code> objects; the
   * behavior is undefined if <code>list</code> contains a <code>SexpList</code>
   * object
   */
  public static Subcat get(SexpList list) {
    return factory.get(list);
  }
}
