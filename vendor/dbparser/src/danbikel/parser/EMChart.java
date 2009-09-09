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
import java.io.Serializable;
import java.util.*;

/**
 * Implementation of a chart for performing constrained CKY parsing so as
 * to perform the E-step of the Inside-Outside algorithm.
 */
public class EMChart extends CKYChart {
  // constants
  private final static String className = EMChart.class.getName();
  private final static boolean debugAddToChart = false;

  // inner class
  /**
   * Contains all information and items covering a particular span.
   */
  protected static class Entry extends Chart.Entry implements Serializable {
    int[] numItemsAtLevel = new int[10];
    int numLevels;
    Entry() { super(); }
  }

  // constructors
  /**
   * Constructs a new chart with the default chart size.
   */
  public EMChart() {
    super();
  }
  /**
   * Constructs a new chart with the specified chart size.
   *
   * @param size the initial size of this chart
   */
  public EMChart(int size) {
    super(size);
  }

  /**
   * Ensures that the size of the chart is at least as large as the
   * specified size.
   *
   * @param size the size to be set for this chart
   */
  public void setSize(int size) {
    if (this.size < size) {
      this.size = size;
      chart = new Entry[size][size];
    }
  }

  /**
   * Checks every map of the chart covering a span less than or equal to
   * size and clears it; if a chart entry is <code>null</code>, then a
   * new map is created.
   */
  public void clear() {
    totalItems = 0;
    if (debugNumItemsGenerated) {
      totalItemsGenerated = 0;
    }
    if (debugNumPrunedItems) {
      numPrePruned = numPruned = 0;
    }
    for (int i = 0; i < size; i++)
      for (int j = i; j < size; j++)
	if (chart[i][j] == null)
	  chart[i][j] = new Entry();
	else {
	  chart[i][j].clear();
	  ((Entry)chart[i][j]).numLevels = 0;
	  int[] levelCounts = ((Entry)chart[i][j]).numItemsAtLevel;
	  for (int k = 0; k < levelCounts.length; k++)
	    levelCounts[k] = 0;
	}
  }

  protected boolean outsideBeam(Item item, double topProb) {
    return false;
  }

  protected boolean toPrune(int start, int end, Item item) {
    return false;
  }

  /**
   * This method has been overloaded so that it simply throws an
   * <code>UnsupportedOperationException</code>, since pruning is
   * inappropriate when performing the E-step of the Inside-Outside
   * algorithm.
   */
  public void doPruning() {
    String msg = "pruning is not appropriate for EM";
    throw new UnsupportedOperationException(msg);
  }

  /**
   * Adds this item that has no antecedents to the chart.
   *
   * @param start the start of the span of this item
   * @param end the end of the span of this item
   * @param item the item to be added
   *
   * @return whether the specified item was added to the chart (<tt>false</tt>
   * if an existing, equivalent item already exists in chart and the specified
   * item's inside probability is added to the existing item's inside
   * probability)
   */
  public boolean add(int start, int end, EMItem item) {
    return add(start, end, item, null, null, null, null);
  }

  /**
   * Adds this item to the chart, recording its antecedents and the events
   * and their probabilities that allowed this item (consequent) to be produced.
   *
   * @param start the start of the span of the item to be added
   * @param end the end of the span of the item to be added
   * @param item the item to be added
   * @param ante1 the first of two possible antecedents of the item to be added
   * @param ante2 the second of two possible antecedents of the item to be
   * added; if the item has only one antecedent, the value of this parameter
   * should be <code>null</code>
   * @param event the single event that allowed this item (consequent) to be
   * produced from its antecedent(s)
   * @param prob the probability of the specified event
   *
   * @return whether the specified item was added to the chart (<tt>false</tt>
   * if an existing, equivalent item already exists in chart and the specified
   * item's inside probability is added to the existing item's inside
   * probability)
   */
  public boolean add(int start, int end, EMItem item,
                     EMItem ante1, EMItem ante2,
                     TrainerEvent event, double prob) {
    return add(start, end, item, ante1, ante2,
               new TrainerEvent[]{event}, new double[]{prob});
  }

  /**
   * Adds this item to the chart, recording its antecedents and the events
   * and their probabilities that allowed this item (consequent) to be produced.
   *
   * @param start the start of the span of the item to be added
   * @param end the end of the span of the item to be added
   * @param item the item to be added
   * @param ante1 the first of two possible antecedents of the item to be added
   * @param ante2 the second of two possible antecedents of the item to be
   * added; if the item has only one antecedent, the value of this parameter
   * should be <code>null</code>
   * @param events the events that allowed this item (consequent) to be
   * produced from its antecedent(s)
   * @param probs an array of probabilities of the same size and coindexed
   * with the specified array of events, where each probability
   * is that for its coindexed event
   *
   * @return whether the specified item was added to the chart (<tt>false</tt>
   * if an existing, equivalent item already exists in chart and the specified
   * item's inside probability is added to the existing item's inside
   * probability)
   */
  public boolean add(int start, int end, EMItem item,
                     EMItem ante1, EMItem ante2,
                     TrainerEvent[] events, double[] probs) {
    if (debugNumItemsGenerated) {
      totalItemsGenerated++;
    }
    if (item.insideProb() == Constants.probImpossible)
      return false;
    if (Double.isNaN(item.insideProb())) {
      System.err.println(className + ": warning: inside prob. is NaN (" +
			 start + "," + end + "): " + item + "\n\tante1: " +
			 ante1 + "\n\tante2: " + ante2);
      System.err.println(events.length + " events");
      for (int i = 0; i < events.length; i++)
	System.err.println("event=" + events[i] + "; prob=" + probs[i]);
    }

    boolean added = false;

    Entry chartEntry = (Entry)chart[start][end];
    MapToPrimitive items = chartEntry.map;
    MapToPrimitive.Entry itemEntry = items.getEntry(item);
    boolean itemExists = itemEntry != null;
    EMItem existingItem = null;
    int unaryLevel = item.unaryLevel();
    if (itemExists) {
      existingItem = (EMItem)itemEntry.getKey();
      EMItem.AntecedentPair currList = existingItem.antecedentPairs();
      EMItem.AntecedentPair newList  =
	new EMItem.AntecedentPair(ante1, ante2, events, probs, currList);
      existingItem.setAntecedentPairs(newList);

      if (debugAddToChart)
        System.err.println(className + ": increasing existing item\n\t" +
                           existingItem + "\ninside prob of " +
                           existingItem.insideProb() + " by " +
                           item.insideProb() + " of\n\t" + item +
                           " ante1=@" + System.identityHashCode(ante1) +
                           " ante2=@" + System.identityHashCode(ante2));

      existingItem.increaseInsideProb(item.insideProb());
      if (unaryLevel != existingItem.unaryLevel())
	System.err.println(className + ": warning: re-deriving an item with " +
			   "a different unary level!");
      item.setGarbage(true);
    }
    else {
      if (ante1 != null) {
	EMItem.AntecedentPair newPair =
	  new EMItem.AntecedentPair(ante1, ante2, events, probs, null);
	item.setAntecedentPairs(newPair);
      }
      if (unaryLevel > chartEntry.numLevels) {
	System.err.println(className + ": error: trying to add item with " +
			   "unary level that is too large (largest seen so " +
			   " far: " + chartEntry.numLevels +
			   "; current item's unary level: " + unaryLevel + ")");
	System.err.println("\titem[" + start + "," + end + "]: " + item);
      }
      else if (unaryLevel == chartEntry.numLevels) {
	// increase capacity of counter array, if necessary
	int currCapacity = chartEntry.numItemsAtLevel.length;
	if (unaryLevel == currCapacity) {
	  int[] oldArr = chartEntry.numItemsAtLevel;
	  chartEntry.numItemsAtLevel = new int[currCapacity * 2];
	  System.arraycopy(oldArr, 0, chartEntry.numItemsAtLevel,
			   0, currCapacity);
	}
	chartEntry.numLevels++;
	chartEntry.numItemsAtLevel[unaryLevel]++;
      }
      else {
	chartEntry.numItemsAtLevel[unaryLevel]++;
      }
      if (debugAddToChart)
        System.err.println(className + ": adding\n\t" + item);
      items.put(item, item.insideProb());
      totalItems++;
      added = true;
    }
    return added;
  }

  protected void setUpItemPool() {
    itemPool = new ObjectPool(EMItem.class, 50000);
  }

  /**
   * Returns a new {@link EMItem}.
   * @return a new {@link EMItem}
   */
  public EMItem getNewEMItem() {
    return (EMItem)super.getNewItem();
  }

  /**
   * Returns the number of unary levels for the chart entry at the specified
   * span.
   *
   * @param start the start of the span for which to retrieve the number of
   *              unary levels
   * @param end   the end of the span for which to retrieve the number of unary
   *              levels
   * @return the number of unary levels for the chart entry at the specified
   *         span.
   */
  public int numUnaryLevels(int start, int end) {
    return ((Entry)chart[start][end]).numLevels;
  }

  /**
   * Returns an array of integers containing the number of chart items at each
   * of the unary levels of the specified span.  The size of the array is the
   * integer returne by {@link #numUnaryLevels(int, int)} for the same span.
   *
   * @param start the start of the span for which to retrieve the unary level
   *              counts
   * @param end   the start of the span for which to retrieve the unary level
   *              counts
   * @return an array of integers containing the number of chart items at each
   *         of the unary levels of the specified span
   */
  public int[] unaryLevelCounts(int start, int end) {
    return ((Entry)chart[start][end]).numItemsAtLevel;
  }
}
