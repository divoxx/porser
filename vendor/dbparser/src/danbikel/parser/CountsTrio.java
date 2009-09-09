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

import java.io.Serializable;

/**
 * Class for grouping the three counts tables necessary for counting
 * transitions, histories and unique transitions (or <i>diversity</i> counts
 * for the history events).
 * <p>
 * <b>Implementation note</b>: The three tables are stored internally as
 * two objects, a <code>CountsTable</code> and a <code>BiCountsTable</code>
 * object.  These two objects are available from the {@link #transition()}
 * and {@link #history()} accessor methods, respectively.
 */
public class CountsTrio implements Serializable {
  // public constants
  /**
   * The constant to be used as an index when adding or retrieving history
   * counts from the <code>BiCountsTable</code> returned by {@link #history()}.
   */
  public final static int hist = 0;
  /**
   * The constant to be used as an index when adding or retrieving diversity
   * counts from the <code>BiCountsTable</code> returned by {@link #history()}.
   */
  public final static int diversity = 1;


  // data members
  CountsTable trans;
  BiCountsTable histAndDiversity;

  CountsTrio() {
    trans = new CountsTableImpl();
    histAndDiversity = new BiCountsTable();
  }
  /**
   * Gets the <code>CountsTable</code> for transitions.
   * @return the transitions <code>CountsTable</code>.
   */
  public CountsTable transition() { return trans; }
  /**
   * Gets the <code>CountsTable</code> for histories counts and diversity
   * statistics.
   * @return the histories <code>CountsTable</code>.
   */
  public BiCountsTable history() { return histAndDiversity; }
}
