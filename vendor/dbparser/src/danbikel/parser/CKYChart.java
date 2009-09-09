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

/**
 * Implementation of a chart for probabilistic Cocke-Kasami-Younger
 * (CKY) parsing.
 */
public class CKYChart extends Chart {
  // "mutable" constants
  private static boolean collinsNPPruneHack =
    Settings.getBoolean(Settings.collinsNPPruneHack);

  static {
    Settings.register(CKYChart.class,
		      new Settings.Change() {
			public void update(Map<String,String> changedSettings) {
			  collinsNPPruneHack =
			    Settings.getBoolean(Settings.collinsNPPruneHack);
			}
		      },
		      null);
  }

  private final static int pruneClampSmall = 100;
  private final static int pruneClampBig = 120;
  @SuppressWarnings({"MismatchedReadAndWriteOfArray"})
  private final static double[] variablePruneFact = new double[200];

  private final static double variablePruneFn(int span) {
    if (span < 5)
      return 4.0;
    else
      return Math.log(10) * Math.max(2.0, (-0.08 * span + 3.8));
  }
  static {
    for (int i = 0; i < variablePruneFact.length; i++) {
      variablePruneFact[i] = variablePruneFn(i);
    }
  }

  // additional data member
  private double smallPruneFact;
  private double smallerPruneFact;

  // constructors

  /**
   * Constructs a new chart with the default chart size.
   */
  public CKYChart() {
    super();
    smallPruneFact = Math.max(2.0, pruneFact - 2);
    smallerPruneFact = Math.max(1.0, pruneFact - 3);
  }
  /**
   * Constructs a new chart with the specified chart size.
   *
   * @param size the initial size of this chart
   */
  public CKYChart(int size) {
    super(size);
    smallPruneFact = Math.max(2.0, pruneFact - 2);
    smallerPruneFact = Math.max(1.0, pruneFact - 3);
  }

  /**
   * Constructs a new chart with a default initial chart size, and with
   * the specified cell limit and prune factor.
   *
   * @param cellLimit the limit to the number of items per cell
   * @param pruneFact that log of the prune factor
   *
   * @see #cellLimit
   * @see #pruneFact
   */
  public CKYChart(int cellLimit, double pruneFact) {
    super(cellLimit, pruneFact);
    smallPruneFact = Math.max(2.0, pruneFact - 2);
    smallerPruneFact = Math.max(1.0, pruneFact - 3);
  }

  /**
   * Constructs a new chart with the specified initial chart size, cell limit
   * and prune factor.
   *
   * @param size the initial size of this chart
   * @param cellLimit the limit to the number of items per cell
   * @param pruneFact that log of the prune factor
   *
   * @see #cellLimit
   * @see #pruneFact
   */
  public CKYChart(int size, int cellLimit, double pruneFact) {
    super(size, cellLimit, pruneFact);
    smallPruneFact = Math.max(2.0, pruneFact - 2);
    smallerPruneFact = Math.max(1.0, pruneFact - 3);
  }

  /**
   * Used by the decoder when it abandons a parse forest for a given sentence
   * and is about to try again with a larger beam (beam-widening).
   *
   * @see Settings#decoderMaxPruneFactor
   * @see Settings#decoderPruneFactorIncrement
   */
  public void clearNonPreterminals() {
    for (int i = 0; i < size; i++) {
      for (int j = i; j < size; j++) {
        if (chart[i][j] == null)
          chart[i][j] = new Entry();
        else {
          if (i == j) {
            // remove non preterminal items
            Iterator it = chart[i][j].map.keySet().iterator();
            while (it.hasNext()) {
              CKYItem item = (CKYItem)it.next();
              if (!item.isPreterminal())
                it.remove();
            }
            chart[i][j].setTopInfo();
          }
          else {
            chart[i][j].clear();
          }
        }
      }
    }
  }

  protected boolean outsideBeam(Item item, double topProb) {
    CKYItem currItem = (CKYItem)item;
    Symbol label = (Symbol)currItem.label();
    if (currItem.isPreterminal())
      return false;

    int span = currItem.end() - currItem.start();

    // if this is an NP or NP-A with more than one child, then we use wider beam
    /*
    if ((currItem.leftChildren() != null || currItem.rightChildren() != null) &&
	Language.treebank.isNP(label))
    */
    if (collinsNPPruneHack &&
	(currItem.leftChildren() != null || currItem.rightChildren() != null) &&
	Language.treebank.stripAugmentation(label) ==
	Language.treebank.NPLabel()) {
      return item.logProb() < topProb - pruneFact - 3;
    }
    /*
    else if (currItem.stop()) {
      // much smaller beam for stopped items
      int span = currItem.end() - currItem.start();
      if (span < variablePruneFact.length)
        return item.logProb() < topProb - variablePruneFact[span];
      else
        return item.logProb() < topProb - variablePruneFn(span);
    }
    */
    else if (relax && currItem.stop()) {
      // much wider beam for stopped items
      //return item.logProb() < topProb - pruneFact - 200;
      return false;
    }
    else if (span > pruneClampBig) {
      return item.logProb() < topProb - smallerPruneFact;
    }
    else if (span > pruneClampSmall) {
      // use very tight beam--as low as 10^2.0--when pruning spans that are
      // greater than pruneClampSmall (i.e., >100)
      return item.logProb() < topProb - smallPruneFact;
    }
    else
      return item.logProb() < topProb - pruneFact;
  }

  protected void setUpItemPool() {
    String chartItemClassname = Settings.get(Settings.chartItemClass);
    Class chartItemClass = null;
    try { chartItemClass = Class.forName(chartItemClassname); }
    catch (ClassNotFoundException cnfe) {
      throw new RuntimeException("Couldn't find class " +
				 chartItemClassname + "; check " +
				 Settings.chartItemClass + " property");
    }
    itemPool = new ObjectPool(chartItemClass, 50000);
  }

  /**
   * Returns a new chart item from the internal pool of reusable items.
   * @return a new chart item from the internal pool of reusable items.
   */
  public CKYItem getNewItem() {
    // return new CKYItem();
    /*
    CKYItem newItem = (CKYItem)itemPool.get();
    if (newItem.garbage())
      throw new RuntimeException();
    return newItem;
    */
    return (CKYItem)itemPool.get();
  }

  protected void reclaimItemCollection(Collection c) {
    if (c.size() > 0) {
      for (Object itemObj : c) {
	Item item = (Item)itemObj;
	item.clear();
      }
      itemPool.putBackAll(c);
    }
  }

  /**
   * Returns <code>true</code> if the specified item has received its
   * stop probabilities (that is, if <code>item.stop() == true</code>).
   *
   * @param item the item to be tested
   */
  protected boolean cellLimitShouldApplyTo(Item item) {
    //return ((CKYItem)item).stop();
    return true;
  }
}
