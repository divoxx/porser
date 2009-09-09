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
 * Provides a single static method, {@link #printLogProbDisn printLogProbDisn},
 * as well as a {@link #main(String[])} method, to print a log-probability
 * distribution for a particular event in a particular model of a model
 * collection.
 */
public class PrintDisn {
  private PrintDisn() {}

  /**
   * Prints the log-probability distribution of the specified event at the
   * specified back-off level of the specified model to the specified writer.
   *
   * @param writer   the writer to which to print a log-probability
   *                 distribution
   * @param mc       the model collection containing the specified model
   * @param model    the model from which an event's log-probability
   *                 distribution is to be printed
   * @param level    the back-off level of the specified event
   * @param hist     the event whose distribution is to be printed
   * @param futures  the set of futures for the specified history
   * @param tmpTrans a temporary storage object used during the invocation of
   *                 this method
   */
  public static void printLogProbDisn(PrintWriter writer, ModelCollection mc,
                                      Model model, int level, Event hist,
                                      Set futures, Transition tmpTrans) {
    Transition trans = tmpTrans;
    trans.setHistory(hist);
    Iterator it = futures.iterator();
    for (int idx = 0; it.hasNext(); idx++) {
      Event future = (Event)it.next();
      trans.setFuture(future);
      double logProb = model.estimateLogProbUsingPrecomputed(trans, level);
      if (logProb > Constants.logOfZero) {
        // if the future is a word symbol, then then this will get a count
        // for it; otherwise, wordFreq will simply be zero
        double wordFreq = mc.vocabCounter().count(((SexpEvent)future).toSexp());
        writer.println(future + "\t" + wordFreq + "\t" + logProb);
      }
    }
  }

  /**
   * Prints an event's log-probability distribution to an output file.
   * @param args a list of five arguments:
   * <pre>
   * usage: &lt;derived data filename&gt; &lt;back-off level&gt;
   *        &lt;structure class name&gt; &lt;event&gt; &lt;output filename&gt;
   * </pre>
   */
  public static void main(String[] args) {
    if (args.length != 5) {
      System.err.println("error: need five arguments");
      System.err.println("usage: <derived data filename> <back-off level> " +
                         "<structure class name>\n\t<event> <output filename>");
      System.exit(1);
    }


    String mcFilename = args[0];
    int level = Integer.parseInt(args[1]);
    String structureName = args[2];
    String eventStr = args[3];
    String outputFilename = args[4];


    try {
      SexpList list = Sexp.read(eventStr).list();
      boolean hasSubcat = list.length() > 1;
      SexpEvent hist = hasSubcat ? new SexpSubcatEvent() : new SexpEvent();
      hist.setSexp(list.get(0));
      if (hasSubcat)
        hist.add(Subcats.get(list.listAt(1)));

      System.err.println("history context: " + hist);

      // create output file
      OutputStream os = new FileOutputStream(outputFilename);
      OutputStreamWriter osw = new OutputStreamWriter(os, Language.encoding());
      PrintWriter writer = new PrintWriter(new BufferedWriter(osw));

      // set up data structures
      Set futures = new HashSet();
      Transition tmpTrans = new Transition(null, null);

      ModelCollection mc = Trainer.loadModelCollection(mcFilename);
      Iterator models = mc.modelList().iterator();
      while (models.hasNext()) {
        Model model = (Model)models.next();
        int numModels = model.numModels();
        for (int i = 0; i < numModels; i++) {
          Model ithModel = model.getModel(i);
          //writeModelStats(ithModel);
          String structureClassName =
            ithModel.getProbStructure().getClass().getName();
          if (structureName.equals(structureClassName)) {
            AnalyzeDisns.getFutures(futures, ithModel, level);
            System.err.println("Writing distribution to file \"" +
                               outputFilename + "\".");
            printLogProbDisn(writer, mc, ithModel, level, hist, futures,
                             tmpTrans);
          }
        }
      }
      writer.flush();
      writer.close();
    }
    catch (Exception e) {
      e.printStackTrace(System.err);
    }
  }
}
