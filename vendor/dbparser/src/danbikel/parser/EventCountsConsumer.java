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
import danbikel.switchboard.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * An implementation of the {@link Consumer} interface (part of the Switchboard
 * framework) for counting events ({@link TrainerEvent} instances) produced as
 * part of the E-step of the EM algorithm (Inside-Outside).  Each {@link
 * NumberedObject} consumed by this consumer is expected to contain a {@link
 * CountsTable} containing the expected counts of {@link TrainerEvent}
 * instances.
 */
public class EventCountsConsumer implements Consumer, Runnable {
  // constants
  private final static String className = Consumer.class.getName();
  /**
   * The default writing interval for this consumer.
   */
  public final static int defaultWriteInterval = 500;
  private final static double countThreshold =
    Double.parseDouble(Settings.get(Settings.countThreshold));

  // data members
  private String outName;
  private Writer out;
  private CountsTable events;
  private CountsTable eventsSwap;
  private int counter;
  private int writeInterval = defaultWriteInterval;
  private boolean asynchronousWrite;
  private boolean strictWriteInterval;
  private boolean useCountThreshold;
  private volatile Thread dumper;
  // indicates to dumper thread that it is time to die
  private volatile boolean timeToDie;

  /**
   * Constructs a new event counts consumer.  Writing out consumed event counts
   * to the output file will occur asynchronously (that is, in parallel) with
   * the consumption of new event counts.  Writing to the output file will occur
   * periodicially, occurring sometime after aggregate event counts have been
   * collected from at least {@link #getWriteInterval} sentences.
   */
  public EventCountsConsumer() {
    this(true, false);
  }

  /**
   * Constructs a new event counts consumer.  If the value of the
   * <code>asynchronousWrite</code> parameter is <code>true</code>, then event
   * counts will be aggregated in  an internal <code>CountsTable</code>, and
   * after event counts have been aggregated from at least {@link
   * #getWriteInterval} sentences, the events and their aggregate counts will be
   * passed off to a separate thread for appending to the current output file.
   * This appending will happen in parallel with the consumption of new events
   * in a freshly-cleared <code>CountsTable</code>.
   *
   * @param asynchronousWrite indicates whether or not to append event counts to
   *                          the output file asynchronously (that is, in
   *                          parallel) with the consumption of new event
   *                          counts
   */
  public EventCountsConsumer(boolean asynchronousWrite) {
    this(asynchronousWrite, false);
  }

  /**
   * Constructs a new event counts consumer.  If the value of the
   * <code>asynchronousWrite</code> parameter is <code>true</code>, then event
   * counts will be aggregated in  an internal <code>CountsTable</code>, and at
   * periodic intervals, the aggregated event counts will be passed off to a
   * separate thread for appending to the current output file. This appending
   * will happen in parallel with the consumption of new events in a
   * freshly-cleared <code>CountsTable</code>. The interval between writing to
   * the output file is determined by the value of {@link #getWriteInterval}. If
   * <code>asynchronousWrite</code> is <code>true</code> and the
   * <code>strictWriteInterval</code> parameter is <code>true</code>, then it is
   * guaranteed that the internal counts table will contain aggregate counts
   * from exactly {@link #getWriteInterval} sentences before being handed off to
   * the output thread.  If the <code>strictWriteInterval</code> parameter is
   * <code>false</code>, then the internal counts table will contain events from
   * <i>at least</i> {@link #getWriteInterval} sentences.  If the
   * <code>asynchronousWrite</code> parameter is <code>false</code> then the
   * value of the <code>strictWriteInterval</code> parameter will be ignored.
   *
   * @param asynchronousWrite   indicates whether or not to append event counts
   *                            to the output file asynchronously (that is, in
   *                            parallel) with the consumption of new event
   *                            counts
   * @param strictWriteInterval if <code>asynchronousWrite</code> is
   *                            <code>true</code> and this parameter is
   *                            <code>true</code>, then it is guaranteed that
   *                            the internal counts table will contain aggregate
   *                            counts from exactly {@link #getWriteInterval}
   *                            sentences before being handed off to the output
   *                            thread; if this parameter is <code>false</code>,
   *                            then the internal counts table will contain
   *                            events from <i>at least</i> {@link
   *                            #getWriteInterval} sentences; if the
   *                            <code>asynchronousWrite</code> parameter is
   *                            <code>false</code> then the value of this
   *                            parameter will be ignored
   */
  public EventCountsConsumer(boolean asynchronousWrite,
			     boolean strictWriteInterval) {
    this.asynchronousWrite = asynchronousWrite;
    this.strictWriteInterval = strictWriteInterval;
    if (asynchronousWrite) {
      events = new CountsTableImpl();
      eventsSwap = new CountsTableImpl();
      timeToDie = false;
    }
  }

  /**
   * Gets the write interval for this consumer.
   */
  public int getWriteInterval() {
    return writeInterval;
  }

  /**
   * Sets the write interval for this consumer.
   */
  public void setWriteInterval(int writeInterval) {
    this.writeInterval = writeInterval;
  }

  /**
   * Indicates to use the count threshold specified by {@link
   * Settings#countThreshold}, which means that events below that threshold will
   * be removed.
   */
  public void useCountThreshold() {
    useCountThreshold = true;
  }

  /**
   * Indicates not to use the count threshold specified by {@link
   * Settings#countThreshold}.
   */
  public void dontUseCountThreshold() {
    useCountThreshold = false;
  }

  public void newFile(String inputFilename, String outputFilename) {
    if (asynchronousWrite) {
      synchronized (this) {
	if (dumper != null) {
	  timeToDie =
	    true; // should have been set to true in processingComplete
	  notifyAll();
	}
      }
      if (dumper != null) {
	try {
	  dumper.join();
	}
	catch (InterruptedException ie) {
	  System.err.println(ie);
	}
      }
      timeToDie = false;
      events.clear();
      eventsSwap.clear();
      counter = 0;
    }

    outName = outputFilename;
    setOutputWriter();

    if (asynchronousWrite) {
      dumper = new Thread(this, "Dumper for \"" + inputFilename + "\"");
      dumper.start();
    }
  }

  void setOutputWriter() {
    try {
      OutputStream os = new FileOutputStream(outName);
      if (outName.endsWith(".gz"))
	os = new GZIPOutputStream(os);
      out = new BufferedWriter(new OutputStreamWriter(os, Language.encoding()),
			       Constants.defaultFileBufsize);
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(className +
			 ": error: couldn't create output file \"" +
			 outName + "\"");
    }
    catch (UnsupportedEncodingException uee) {
      System.err.println(className +
			 ": error: unsupported encoding: " +
			 Language.encoding());
    }
    catch (IOException ioe) {
      System.err.println(className +
			 ": error: trouble creating file \"" + outName + "\"" +
			 ioe);
    }
  }

  synchronized boolean timeToWrite() {
    return counter >= writeInterval;
  }

  public void consume(NumberedObject obj) {
    if (!obj.processed())
      return;
    if (asynchronousWrite)
      consumeForDumper(obj);
    else {
      try {
	CountsTable currCounts = (CountsTable) obj.get();
	if (useCountThreshold)
	  currCounts.removeItemsBelow(countThreshold);
	EventCountsWriter.outputEvents(currCounts, out);
	out.flush();
      }
      catch (IOException ioe) {
	System.err.println(className + ": error outputting events: " + ioe);
      }
    }
  }

  /**
   * A helper method used by {@link #consume(NumberedObject)} to perform
   * consumption of objects that are periodicially written to an output file by
   * a separate &ldquo;dumper&rdquo; thread.  This method is only used if the
   * <code>asynchronousWrite</code> argument to one of the constructors is
   * <code>true</code>.
   *
   * @param obj the object to be consumed
   */
  synchronized public void consumeForDumper(NumberedObject obj) {
    // note that this method is ONLY called if asynchronousWrite is true
    if (strictWriteInterval) {
      try {
	while (timeToWrite())
	  wait();
      }
      catch (InterruptedException ie) {
      }
    }

    CountsTable currEvents = (CountsTable) obj.get();
    Iterator it = currEvents.entrySet().iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry) it.next();
      TrainerEvent event = (TrainerEvent) entry.getKey();
      double count = entry.getDoubleValue();
      if (!useCountThreshold || count >= countThreshold)
	events.add(event, count);
    }
    counter++;
    if (timeToWrite())
      notifyAll();
  }

  /**
   * Indicates that there are no more sentences whose event counts are to be
   * consumed.  If this consumer was constructed to have asynchronous writing,
   * then any remaining aggregate counts in the internal
   * <code>CountsTable</code> will be appended to the output file before this
   * method exits.
   *
   * @param inputFilename  the input file for which event counts have been
   *                       consumed
   * @param outputFilename the output file for consumed event counts
   */
  synchronized public void processingComplete(String inputFilename,
					      String outputFilename) {
    if (asynchronousWrite) {
      timeToDie = true;
      notifyAll();
    }
  }

  void writeOutput() {
    boolean keepLooping = true;
    boolean lastLoop = false;
    while (keepLooping) {
      synchronized (this) {
	try {
	  while (!timeToDie && !timeToWrite())
	    wait();
	}
	catch (InterruptedException ie) {
	  System.err.println(ie);
	}
	// while we still have lock, do the swap and reset counter
	eventsSwap.clear();
	CountsTable tmp = eventsSwap;
	eventsSwap = events;
	events = tmp;
	counter = 0;
	if (strictWriteInterval)
	  notifyAll();  // because of the call to wait() in consumeForDumper
      }

      // we no longer have lock, so write out eventsSwap with invocations
      // of consume method happening asynchronously
      try {
	EventCountsWriter.outputEvents(eventsSwap, out);
	out.flush();
      }
      catch (IOException ioe) {
	System.err.println(className + ": error outputting events: " + ioe);
      }
      if (timeToDie) {
	if (lastLoop || events.size() == 0)
	  keepLooping = false;
	else
	  lastLoop = true;
      }
    }
    try {
      out.close();
    }
    catch (IOException ioe) {
      System.err.println(className + ": error: couldn't close file \"" +
			 outName + "\"");
    }
  }

  /**
   * Allows this object to used as a &ldquo;dumper&rdquo; thread for
   * periodically writing consumed objects to an output file.  A dumper thread
   * is only created when the <code>asynchronousWrite</code> argument of the
   * constructors that take such an argument is <code>true</code>, or if
   * {@linkplain #EventCountsConsumer() the default constructor} is used.  A
   * dumper thread is created by {@link #newFile(String, String)}.
   *
   * @see #EventCountsConsumer()
   * @see #EventCountsConsumer(boolean)
   * @see #EventCountsConsumer(boolean,boolean)
   * @see #newFile(String,String)
   */
  public void run() {
    writeOutput();
  }
}
