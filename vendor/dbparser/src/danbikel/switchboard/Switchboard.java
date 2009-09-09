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
    package danbikel.switchboard;

import danbikel.util.TimeoutSocketFactory;
import danbikel.util.Time;
import danbikel.util.IntPair;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

/**
 * The switchboard serves as the central "hub" in a distributed RMI
 * object-processing run, accepting registrations of clients and servers, and
 * assigning clients to servers.  Also, the switchboard is an "object
 * server", doling out objects to clients when requested.
 * When clients are assigned servers via the {@link #getServer(int)} method,
 * the switchboard will always return the most lightly-loaded server,
 * as determined by the load ratio of that server (the number of its
 * clients divided by the maximum number of clients it is willing to accept).
 * The switchboard may be used either for its primary switchboard facilities
 * (doling out servers to clients), its object server facilities, or both.
 * <p>
 * <b>N.B.</b>: Unlike <code>UnicastRemoteObject</code> RMI servers, a
 * <code>Switchboard</code> object needs to be explicitly exported,
 * via its {@link #export} method.
 *
 * @see SwitchboardUser
 * @see ObjectReader
 * @see NumberedObject
 */
public class Switchboard
  extends RemoteServer
  implements SwitchboardRemote {

  // public constants

  /**
   * Allows external links to {@link UnicastRemoteObject} and its members
   * not to break when generating javadoc API documentation.
   */
  public static final UnicastRemoteObject javadocHack = null;

  // allow users access to constructor defaults

  /** The default rmiregistry binding name, <tt>"/Switchboard"</tt>,
      which indicates to bind the switchboard under the specified name
      on localhost using the default rmiregistry port (1099). */
  public static final String defaultBindingName = "/Switchboard";

  /** The default suffix to add to input file names to form an output
      file when none is explicitly specified. The value of this constant
      is <tt>".proc"</tt>. */
  public static final String outFilenameSuffix = ".proc";

  /**
   * The default suffix to add to input file names to form a log file name when
   * none is explicitly specified.  The value of this constant is
   * <tt>".log"</tt>.
   */
  public static final String logFilenameSuffix = ".log";

  /**
   * The default filename to use for printing out messages.  If this
   * default name is used, the messages file will be created in the
   * current working directory.  The value of this constant is
   * <tt>"switchboard.msgs"</tt>.
   */
  public static final String defaultMessagesFilename = "switchboard.msgs";

  /**
   * The default re-processing option, which is <code>false</code>.
   * This value may be passed to various constructors that take a re-processing
   * option.
   */
  public static final boolean defaultReProcess = false;

  /**
   * The default as to whether clients should be killed upon the death
   * of their server.
   * <p>
   * The value of this constant is <code>false</code>.
   */
  public static final boolean defaultServerDeathKillClients = false;

  /**
   * The default maximum number of retries the switchboard should make
   * to determine whether a user is alive in the face of failure of
   * that user's {@link SwitchboardUser#alive alive} method.
   * <p>
   * The value of this constant is <code>0</code>.
   */
  public static final int defaultKeepAliveMaxRetries = 0;

  /**
   * The default interval, in milliseconds, between calls to a switchboard
   * user's {@link SwitchboardUser#alive alive} method.
   * <p>
   * The value of this constant is <code>5000</code>.
   */
  public static final int defaultKeepAliveInterval = 5000;

  /**
   * The default sorting behavior for creating the output file from the
   * log file, which is to perform a sort.
   */
  public static final boolean defaultSortOutput = true;

  /**
   * The default port on which to receive RMI calls, which is 0, indicating
   * an anonymous port.  Unlike the RMI API, we encourage the use of this
   * constant rather than direct referral to a "magic number".  This value
   * may be passed to the <code>Switchboard</code> constructors that take
   * a port.
   */
  public static final int defaultPort = 0;

  // static methods for default factories

  /**
   * Returns the default <code>ObjectReaderFactory</code> for un-numbered
   * objects, which uses an <code>ObjectInputStream</code> from which to read
   * objects.
   *
   * @return the default <code>ObjectReaderFactory</code> for un-numbered
   *         objects
   */
  public static final ObjectReaderFactory getDefaultObjectReaderFactory() {
    if (defaultORF == null)
      defaultORF = new DefaultObjectReaderFactory();
    return defaultORF;
  }

  /**
   * Returns the default <code>ObjectReaderFactory</code> for numbered objects,
   * which uses an <code>ObjectInputStream</code> from which to read objects.
   *
   * @return the default <code>ObjectReaderFactory</code> for numbered objects
   */
  public static final ObjectReaderFactory getDefaultNumberedObjectReaderFactory() {
    if (defaultNORF == null)
      defaultNORF = getDefaultObjectReaderFactory(); // same as for un-numbered!
    return defaultNORF;
  }

  /**
   * Returns the default <code>ObjectWriterFactory</code> for un-numbered
   * objects, which uses an <code>ObjectOutputStream</code> to write objects.
   *
   * @return the default <code>ObjectWriterFactory</code> for un-numbered
   *         objects
   */
  public static final ObjectWriterFactory getDefaultObjectWriterFactory() {
    if (defaultOWF == null)
      defaultOWF = new DefaultObjectWriterFactory();
    return defaultOWF;
  }

  /**
   * Returns the default <code>ObjectWriterFactory</code> for numbered objects,
   * which uses an <code>ObjectOutputStream</code> to write objects.
   *
   * @return the default <code>ObjectWriterFactory</code> for numbered objects
   */
  public static final ObjectWriterFactory getDefaultNumberedObjectWriterFactory() {
    if (defaultNOWF == null)
      defaultNOWF = getDefaultObjectWriterFactory(); // same as for un-numbered!
    return defaultNOWF;
  }


  // private constants
  @SuppressWarnings({"UnusedDeclaration"})
  private static final String msgFilenameSuffix = ".msg";
  private static final String className = Switchboard.class.getName();
  private static final String msgFileHeader =
  "----------------------------------------";
  private static final boolean debug = false;
  private static final int firstFileId = 0;
  /**
   * Indicates the maximum number of files that are open for processing. Note
   * that since dumper threads operate asynchronously, it is likely that the
   * actual number of open files will be greater than this constant at any given
   * time, equaling the size of the {@link #unProcessedFiles} map plus any files
   * that are being actively processed, or any files that have been completely
   * processed but have not yet finishd being dumped.
   * <p/>
   * Note that a &ldquo;file&rdquo;, in the form of an {@link IOData} object,
   * actually represents up to two open streams: either an input file reader
   * stream and a log output stream, or, in the dumping phase, a log reader
   * stream and an output file writer stream.  (The reason there is never a time
   * when all four streams are open is because the {@link
   * Switchboard.IOData#dumpOutput()} method always tries to close the first two
   * streams before opening the latter two.) The hope is that even two times the
   * value of this constant is still far less than the maximum number of open
   * files allowed by the environment in which the JVM is running.
   */
  private static final int maxNumOpenFiles = 100;

  // static data members

  // default object reader factories
  private static ObjectReaderFactory defaultORF;
  private static ObjectReaderFactory defaultNORF;
  // default object writer factories
  private static ObjectWriterFactory defaultOWF;
  private static ObjectWriterFactory defaultNOWF;

  // default buffer size
  /**
   * The default buffer size for all streams created by the switchboard.
   * This will be the buffer size of the messages file.  The
   * <code>setBufSize</code> method may be used if a different
   * buffer size is desired for all other streams created by the switchboard.
   * <p>
   * The value of this data member is <code>8192</code>.
   * <p>
   *
   * @see #setBufSize(int)
   */
  protected static int defaultBufSize = 8192;


  // inner classes

  private class IOData implements Runnable {
    // data members

    // unique ID number for this set of IO files and data
    int id;
    // input file
    String inName;
    ObjectReader in;
    // output (processed) file
    String outName;
    ObjectWriter out;
    // log file
    String logName;
    ObjectWriter log;

    // next object to be returned by nextObject method
    NumberedObject nextNumberedObject;

    // state of object processing
    private int currObjectNum = 0;
    private int numObjectsProcessed = 0;
    @SuppressWarnings({"UnusedDeclaration"})
    private int numObjectsProcessedThisRun = 0;
    private boolean moreObjects = true;
    /** Indicates whether this file has been opened for processing. */
    private boolean open = false;
    private boolean clobber;

    // constructors

    IOData(int id, String inName, String outName, String logName) {
      this(id, inName, outName, logName, true);
    }

    /**
     * Constructs a new <code>IOData</code> object, using the specified file ID
     * and input, output and log filenames.
     *
     * @param id      the unique identification number of this file
     * @param inName  the input file name
     * @param outName the output file name
     * @param logName the log file name
     * @param clobber specifies whether to disregard prior existence of the
     *                specified output file and clobber it; if
     *                <code>false</code> and the specified output file exists,
     *                then this constructor exits early, setting only enough
     *                internal state so that the {@link #done} method returns
     *                <code>true</code>
     */
    IOData(int id, String inName, String outName, String logName,
	   boolean clobber) {
      this.id = id;
      this.inName = inName;
      this.outName = outName;
      this.logName = logName;
      this.clobber = clobber;
    }

    void open() throws IOException {
      open = true;
      // if clobber is false and output file exists, then file processing is
      // effectively done (because it was done in a prior run)
      if (outName != null) {
	File outFile = new File(outName);
	if (!clobber && outFile.exists()) {
	  moreObjects = false;
	  String msg = className + ": existence of output file \"" + outName +
	    "\" with noclobber on:\n\t\"" + inName + "\" assumed to have " +
	    "been completed in previous run";
	  log(msg);
	  return;
	}
      }

      String msg = className + ": enqueuing input file \"" + inName + "\" " +
	"(ID number " + id + ") for processing";
      log(msg);

      in = objReaderFactory.get(inName, encoding, bufSize);

      boolean append = true;

      File logFile = null;
      if (logName != null) {
	logFile = new File(logName);
	if (logFile.exists()) {
	  msg = className + ": log file\n\t\"" + logName + "\"\n\t" +
	    "exists; will attempt to recover";
	  log(msg);
	  recover();
	  if (in == null) {
	    throw new IOException("couldn't open file \"" + inName +
				  "\" for processing");
	  }
	}
      }

      // read first object to set nextObject data member
      readNextObject();

      //if (clobber && done()) {
      if (done()) {
	msg = className + ": processing completed in previous run" +
	      (outName == null ? "" : "; creating output file");
	log(msg);
	if (logName != null && outName != null)
	  dumpOutput();
      }
      else {
	if (logName != null) {
	  msg = className + ": appending to log file \"" + logName + "\"";
	  log(msg);

	  boolean emptyFile = logFile.length() == 0;
	  OutputStream logOS = new FileOutputStream(logName, append);
	  log = numObjWriterFactory.get(logOS, encoding, bufSize,
					append, emptyFile);
	}
      }
    }

    // in the future, might implement a second constructor that takes
    // both file names and streams, including both an output *and*
    // an input stream for the log file, since it will have to be read
    // in when dumping processed objects to the output file

    /**
     * Simply tells consumer the names of the file it is about to be consuming,
     * along with the name of the output file, via its
     * {@link Consumer#newFile(String,String)} method.
     *
     * @param c the consumer to register
     *
     * @throws RemoteException if there is a problem invoking this remote method
     */
    void registerConsumer(Consumer c) throws RemoteException {
      c.newFile(inName, outName);
    }

    void readNextObject() {
      readNextObject(-1);
    }

    /**
     * Reads the next object from the input file, constructs a NumberedObject
     * (with the appropriate number) and sets the {@link #nextNumberedObject}
     * data member.
     *
     * @param clientId the ID number of the client calling this method
     */
    void readNextObject(int clientId) {
      Object nextObject = null;
      try { nextObject = in.readObject(); }
      catch (IOException ioe) {
	if (verbose) {
	  if (clientId == -1) {
	    logFailure("nextObject");
	  }
	  else {
	    logFailure("nextObject", clientId);
	  }
	}
      }
      if (nextObject == null) {
	moreObjects = false;
	nextNumberedObject = null;
	/*
	if (verbose) {
	  String msg = "IOData.readNextObject: no more objects";
	  if (clientId == -1)
	    log(msg);
	  else
	    log(msg, clientId);
	}
	*/
      }
      else {
	/*
	if (verbose) {
	  String msg =
	    "IOData.readNextObject: read object " + currObjectNum +
	    " from file No. " + id;
	  if (clientId == -1)
	    log(msg);
	  else
	    log(msg, clientId);
	}
	*/
	nextNumberedObject =
	  new NumberedObject(currObjectNum++, id, false, nextObject);
      }
    }

    /**
     * Gets the next numbered object from the input file or stream.
     *
     * @param clientId the ID of the client requesting the object (used
     * by this method only for error-reporting purposes)
     * @return the next object to be processed from the file represented by
     * this <code>IOData</code> object, or <code>null</code> if there are
     * no more objects to process (all objects in the file have already
     * been doled out to clients)
     */
    @SuppressWarnings({"UnusedDeclaration"})
    synchronized NumberedObject nextObject(int clientId) {
      /*
      if (!moreObjects) {
	notifyIfDone();
	return null;
      }
      */
      NumberedObject retval = this.nextNumberedObject;
      if (retval != null)
	readNextObject();
      if (verbose) {
        /*
	if (retval == null) {
	  log("IOData.nextObject: no more objects in file No. " + id, clientId);
	}
	else {
	  log("IOData.nextObject: returning object " + retval.number() +
	      " from file No. " + id, clientId);
	}
       */
      }
      return retval;
    }

    synchronized void writeToLog(NumberedObject numObj) throws RemoteException {
      synchronized (consumers) {
	if (consumers.size() > 0) {
	  for (Consumer consumer : consumers) {
	    consumer.consume(numObj);
	  }
	}
      }
      if (logName != null) {
	try {
	  log.writeObject(numObj);
	}
	catch (IOException ioe) {
	  logFailure("putObject: error writing " + numObj + " to log file! (" +
		     ioe + ")");
	}
      }
      numObjectsProcessed++;
      numObjectsProcessedThisRun++;
    }

    /**
     * Notifies any thread waiting on this object if object processing is done,
     * and notifies all consumers that object processing is done.
     */
    synchronized void notifyIfDone() {
      if (this.done()) {
	this.notifyAll();
      }
    }

    /**
     * Returns <code>true</code> when object processing is done.
     *
     * @return <code>true</code> when object processing is done
     */
    synchronized boolean done() {
      return (open && !moreObjects && numObjectsProcessed == currObjectNum);
    }

    /**
     * Returns whether or not there are more objects to read from the input
     * file.  If this method returns <code>false</code>, it indicates that all
     * objects have been read from the input file, and have either been
     * processed and written to the log file, or are currently being processed.
     *
     * @return whether or not there are more objects to read from the input
     *         file
     */
    synchronized boolean moreObjectsToRead() {
      return moreObjects;
    }

    /**
     * Waits on this object until object processing is done, then
     * reads through the log file and spits out the processed objects in
     * the correct order to the output file.
     */
    void dumpOutput() {
      boolean interrupted = false;

      synchronized (this) {
	try {
	  while (!this.done())
	    this.wait();
	} catch (InterruptedException ie) {
	  String msg = "dump output on file " + id + " interrupted";
	  log(msg);
	  interrupted = true;
	}
      }

      closeReader(in, inName);
      closeWriter(log, logName);

      if (interrupted)
	return;

      if (Switchboard.this.verbose)
	System.out.println(Switchboard.className +
			   ": processing on \"" + inName + "\" finished; " +
			   "dumping output to \"" + outName + "\"");
      ObjectReader numObjReader = null;
      try {
	// read in all objects from log, sorting them on the fly into a TreeSet
	numObjReader =
	  numObjReaderFactory.get(logName, encoding, bufSize);

	SortedSet<NumberedObject> allObjects = null;
	if (sortOutput) {
	  allObjects = new TreeSet<NumberedObject>();
	  Object curr = null;
	  while ((curr = numObjReader.readObject()) != null) {
	    NumberedObject currNumObj = (NumberedObject)curr;
	    allObjects.add(currNumObj);
	  }
	}

	try {
	  out = objWriterFactory.get(outName, encoding, bufSize, false);
	}
	catch (IOException ioe) {
	  String errMsg = Switchboard.className +
	    ": error opening final output file \"" +
	    outName + "\"! (" + ioe + ")";
	  if (verbose)
	    logFailure(errMsg);
	  System.err.println(errMsg);
	  return;
	}

	if (sortOutput) {
	  for (NumberedObject numObj : allObjects) {
	    out.writeObject(numObj.get());
	  }
	}
	else {
	  // write out objects in the order in which they appear in log file
	  Object curr = null;
	  while ((curr = numObjReader.readObject()) != null) {
	    NumberedObject currNumObj = (NumberedObject)curr;
	    out.writeObject(currNumObj.get());
	  }
	}
      }
      catch (IOException ioe) {
	String errMsg = Switchboard.className +
	  ": error outputting final output file! " +
	  "(" + ioe + ")";
	if (verbose)
	  logFailure(errMsg);
	System.err.println(errMsg);
      }
      // even if there was an error during writing the final output file,
      // we want to attempt to close both of the remaining open streams
      // (which wouldn't happen if the closures were in the above try block,
      // because an error during output would cause control flow to pass to the
      // catch clause without reaching the close statements)
      closeReader(numObjReader, logName);
      closeWriter(out, outName);
    }

    private void closeReader(ObjectReader reader, String name) {
      if (reader == null) {
	return;
      }
      try {
	reader.close();
      }
      catch (IOException ioe) {
	String errMsg = Switchboard.className +
	  ": error closing input stream for file \"" + name + "\"" +
	  "(" + ioe + ")";
	if (verbose)
	  logFailure(errMsg);
	System.err.println(errMsg);
      }
    }

    private void closeWriter(ObjectWriter writer, String name) {
      if (writer == null) {
	return;
      }
      try {
	writer.close();
      }
      catch (IOException ioe) {
	String errMsg = Switchboard.className +
	  ": error closing output stream for file \"" + name + "\"" +
	  "(" + ioe + ")";
	if (verbose)
	  logFailure(errMsg);
	System.err.println(errMsg);
      }
    }

    /**
     * Attempts to recover as much work as possible from a prior processing
     * run on the current input file, by using a log file, if available.
     */
    protected void recover() {
      // open reader to log file
      ObjectReader numObjReader = null;
      try {
	numObjReader = numObjReaderFactory.get(logName, encoding, bufSize);
      }
      catch (UnsupportedEncodingException uee) {
	System.err.println(uee);
	System.exit(1);
      }
      catch (FileNotFoundException fnfe) {
	System.err.println(className + ": error: couldn't open log file \"" +
			   logName + "\" for recovery");
	return;
      }
      catch (IOException ioe) {
	System.err.println(className + ": error: couldn't open log file \"" +
			   logName + "\" for recovery");
	return;
      }

      // read through all numbered objects in log, sticking numbers
      // into a map, mapping integers to booleans, so that we have
      // an accounting of all objects logged during prior run,
      // mapped to their processed status
      Object currObj = null;
      int maxNum = -1;
      SortedMap<Integer, Boolean> numObjs = new TreeMap<Integer, Boolean>();
      try {
	while ((currObj = numObjReader.readObject()) != null) {
	  NumberedObject numObj = (NumberedObject)currObj;
	  Integer currObjNum = new Integer(numObj.number());
	  Boolean currObjProcessed = new Boolean(numObj.processed());
	  if (numObjs.containsKey(currObjNum)) {
	    String warning = "recovery warning: object " + numObj.uid() +
	      " already seen; using subsequent entry";
	    log(warning);
	    System.err.println(warning);
	  }
	  numObjs.put(currObjNum, currObjProcessed);

	  if (maxNum < numObj.number())
	    maxNum = numObj.number();

	  if (debug)
	    System.err.println("recovered object number " + currObjNum);
	}
      }
      catch (IOException ioe) {
	String errMsg = className + ": error: trouble reading log file \"" +
	  logName + "\" (" + ioe + ")";
	logFailure(errMsg);
	System.err.println(errMsg);
      }

      // now read through maxNum objects of input file, pushing every
      // object that was not logged in previous run onto toProcess;
      // if reProcess flag is true, also enqueue objects whose processed
      // status is false (i.e., unprocessed) onto beginning of toProcess deque
      currObj = null;
      int numEnqueued = 0;
      LinkedList<NumberedObject> queue = new LinkedList<NumberedObject>();
      boolean exceptionThrown = false;
      try {
	for (int num = 0; num <= maxNum; num++) {
	  currObj = in.readObject();

	  if (currObj == null)
	    throw new IOException("ran out of objects during recovery");

	  boolean pushObject = false;
	  Boolean processed = (Boolean)numObjs.get(new Integer(num));
	  boolean objectWasNotLogged = processed == null;
	  if (objectWasNotLogged) {
	    String msg = "undone object: " + num;
	    if (verbose)
	      System.out.println(msg);
	    log(msg);
	    pushObject = true;
	  }
	  else if (reProcess && !processed.booleanValue()) {
	    String msg = "object " + num + " unprocessed from previous run; " +
	      "treating as undone";
	    if (verbose)
	      System.out.println(msg);
	    log(msg);
	    pushObject = true;
	  }

	  if (pushObject) {
	    NumberedObject numObj = new NumberedObject(num, id, false, currObj);
	    queue.addFirst(numObj); // enqueue this object
	    numEnqueued++;
	  }
	} // end for

	currObjectNum = maxNum + 1;

	// try to read one more object, just in case processing was complete
	currObj = in.readObject();
	if (currObj == null)
	  moreObjects = false;
	else {
	  NumberedObject numObj = new NumberedObject(currObjectNum++, id,
						     false, currObj);
	  queue.addFirst(numObj);
	  numEnqueued++;
	}

	numObjectsProcessed = currObjectNum - numEnqueued;
      }
      catch (IOException ioe) {
	String errMsg = className + ": error: trouble reading from input " +
	  "file \"" + inName + "\" during recovery (" + ioe + "); " +
	  "recovery failed";
	System.err.println(errMsg);
	logFailure(errMsg);
	exceptionThrown = true;
	unrecover();
      }

      if (!exceptionThrown) {
	toProcess.addAll(0, queue);
      }

      String msg = "number of objects previously processed: " +
	numObjectsProcessed;
      if (debug)
	System.err.println(msg);
      log(msg);

      try { numObjReader.close(); }
      catch (IOException ioe) {
	String errMsg = className + ": error: couldn't close log file " +
	  "during recovery (" + ioe + ")";
	System.err.println(errMsg);
	logFailure(errMsg);
      }
    }

    /**
     * Resets everything to where it would have been if {@link #recover} method
     * had not been invoked.
     */
    private void unrecover() {
      String errMsg;
      moreObjects = true;
      numObjectsProcessed = 0;
      currObjectNum = 0;
      try {
	in.close();
      }
      catch (IOException ioe) {
	errMsg = className + ": error: couldn't close input file \"" +
		 inName + "\" after error during recovery";
	System.err.println(errMsg);
	logFailure(errMsg);
      }
      try {
	in = objReaderFactory.get(inName, encoding, bufSize);
      }
      catch (IOException ioe) {
	errMsg = className + ": error: couldn't re-open input " +
		 "file \"" + inName + "\" after error during recovery";
	System.err.println(errMsg);
	logFailure(errMsg);
	in = null;
      }
    }

    /** Allows this class to be used as the basis of a dumper thread,
	which gets started from within
	{@link Switchboard#processFile(String,String,String,boolean)}. */
    public void run() {
      dumpOutput();
    }

    public String toString() {
      return "\"" + inName + "\" (ID=" + id + ")";
    }
  }


  /** Used to keep track of switchboard users' liveness. */
  private class KeepAlive implements Runnable {
    private SwitchboardUserData userData;

    private KeepAlive(SwitchboardUserData userData) {
      this.userData = userData;
    }

    /** This method continually asks a switchboard user if it is still
	alive.  If the user doesn't respond, this method will keep
	trying to reach the user up to keepAliveMaxRetries times (with
	keepAliveInterval between tries).  If switchboard user still
	doesn't respond, it is considered dead. */
    public void run() {
      int retries = keepAliveMaxRetries;
      int numTriesRemaining = retries + 1;
      SwitchboardUser user = userData.switchboardUser();
      boolean returned = true;

      Exception lastExceptionThrown = null;
      while (!Thread.interrupted() && userData.alive && numTriesRemaining > 0) {
	try {
	  userData.alive = user.alive();
	  returned = true;
	  numTriesRemaining = retries + 1;  // alive method returned, so reset
	  try { Thread.sleep(keepAliveInterval); }
	  catch (InterruptedException ie) {}
	}
	catch (RemoteException re) {
	  numTriesRemaining--;
	  lastExceptionThrown = re;
	  returned = false;
	  try { Thread.sleep(keepAliveInterval); }
	  catch (InterruptedException ie) {}
	}
      } // end while (stop trying to find out if alive and assume dead)

      if (verbose)
	logSwitchboardUserDeath(userData, returned,
				(retries + 1 - numTriesRemaining),
				lastExceptionThrown);
      userData.cleanup();
    }
  }

  /** Base class for client and server data objects used by the switchboard.
      Provides convenient abstraction for KeepAlive to call cleanup method. */
  private abstract class SwitchboardUserData {
    protected int id;
    protected String host;
    protected volatile boolean alive;

    protected SwitchboardUserData(int id, String host) {
      this.id = id;
      this.host = host;
      alive = true;
    }
    protected abstract void cleanup();
    protected abstract SwitchboardUser switchboardUser();
  }

  private class ClientData extends SwitchboardUserData {
    // data members
    private int serverId = -1;
    // a map from object uid's to NumberedObject objects
    private Map<IntPair, NumberedObject> objectsInProgress;
    private Client client;

    // constructor
    private ClientData(int id, String host, Client client) {
      super(id, host);
      this.client = client;
      objectsInProgress = new HashMap<IntPair, NumberedObject>();
    }

    public boolean equals(Object obj) {
      return obj instanceof ClientData && this.id == ((ClientData)obj).id;
    }

    public void addObject(NumberedObject obj) {
      objectsInProgress.put(obj.uid(), obj);
    }

    protected SwitchboardUser switchboardUser() { return client; }

    protected void cleanup() {
      cleanup(true);
    }

    protected void cleanup(boolean removeFromServer) {
      // KeepAlive thread will stop after at most one more call to
      // SwitchboardUser.alive method of the switchboard user encapsulated
      // by this data object
      alive = false;
      client = null;

      synchronized (Switchboard.this) {
	Integer thisIdInteger = new Integer(this.id);
	Integer serverIdInteger = new Integer(this.serverId);

	// unregister this client
	if (Switchboard.this.clients.remove(thisIdInteger) != null) {

	  // remove this clent from server's set of clients, if possible
	  if (removeFromServer) {
	    ServerData serverData =
	      (ServerData)Switchboard.this.servers.get(serverIdInteger);
	    if (serverData != null)
	      serverData.clients.remove(thisIdInteger);
	  }

	  // push all objects handed out to the client back on toProcess deque
	  for (NumberedObject numObj : objectsInProgress.values()) {
	    if (verbose)
	      log("cleanup: pushing object No. " + numObj.uid(), id);
	    Switchboard.this.toProcess.addLast(numObj);
	  }
	}
      } // end synchronized
    }

    public String toString() {
      return "client No. " + id;
    }
  }

  private class ServerData extends SwitchboardUserData implements Comparable {
    // constants
    final static int defaultMaxClients = 100000;

    // data members
    private Set<Integer> clients;
    private int maxClients = defaultMaxClients;
    private Server server;
    private boolean acceptClientsOnlyByRequest = false;

    // constructors
    private ServerData(int id, String host, Server server) {
      super(id, host);
      this.server = server;
      clients = new HashSet<Integer>();
    }
    @SuppressWarnings({"UnusedDeclaration"})
    private ServerData(int id, String host, Server server,
		       int maxClients) {
      this(id, host, server);
      this.maxClients = maxClients;
    }
    private ServerData(int id, String host, Server server,
		       int maxClients, boolean acceptClientsOnlyByRequest) {
      this(id, host, server);
      this.maxClients = maxClients;
      this.acceptClientsOnlyByRequest = acceptClientsOnlyByRequest;
    }

    public boolean equals(Object obj) {
      return obj instanceof ServerData && this.id == ((ServerData)obj).id;
    }

    /**
     * Orders <code>ServerData</code> objects by their load, which is
     * defined to be the number of current clients divided by their maximum
     * number of clients.
     */
    public final int compareTo(Object obj) {
      ServerData other = (ServerData)obj;
      double thisLoadFactor = this.loadFactor();
      double otherLoadFactor = other.loadFactor();
      return (thisLoadFactor < otherLoadFactor ? -1 :
	      (thisLoadFactor == otherLoadFactor && this.equals(other) ? 0 :
	       1));
    }

    private final int numClients() { return clients.size(); }
    private final double loadFactor() {
      return numClients() / (double)maxClients;
    }

    protected SwitchboardUser switchboardUser() { return server; }

    protected void cleanup() {
      // KeepAlive thread will stop after at most one more call to
      // SwitchboardUser.alive method of the switchboard user encapsulated
      // by this data object
      alive = false;
      server = null;

      synchronized (Switchboard.this) {
	// unregister this server
	Integer thisIdInteger = new Integer(this.id);
	Switchboard.this.sortedServers.remove(this);
	if (Switchboard.this.servers.remove(thisIdInteger) != null) {

	  // try to tell clients to re-register or die and then
	  // have them cleanup after themselves (which involves attempting to
	  // unregister themselves)
	  for (Object client : clients) {
	    Integer clientIdInteger = (Integer) client;
	    ClientData clientData =
	      (ClientData) Switchboard.this.clients.get(clientIdInteger);
	    if (clientData != null && clientData.client != null &&
		clientData.alive) {
	      try {
		if (serverDeathKillClients)
		  clientData.client.die(true);
		else {
		  log("telling client " + clientData.id + " that server " +
		      id + " is down");
		  clientData.client.serverDown(id);
		  clientData.serverId = -1;
		}
	      }
	      catch (RemoteException re) {
	      }
	    }
	    // don't remove from this server data object's set of
	    // clients, since we are iterating over it!
	    // N.B.: We no longer ask clients to clean up after themselves,
	    // because clients should now just ask for a new server
	    // (hence the following line of code is commented out)
	    //clientData.cleanup(false);
	  } // end iterating through server's clients
	} // end if server is good
      } // end synchronized
    }

    public String toString() {
      return "server No. " + id;
    }
  }


  // data members

  // I/O
  /** The current file encoding. */
  protected String encoding;
  private ObjectReaderFactory objReaderFactory;
  private ObjectReaderFactory numObjReaderFactory;
  private ObjectWriterFactory objWriterFactory;
  private ObjectWriterFactory numObjWriterFactory;
  private PrintWriter msgs;       // message output
  private int bufSize = defaultBufSize;
  private boolean verbose = true; // whether to print extensive messages
  private List<Consumer> consumers = new ArrayList<Consumer>();


  // global state of object processing
  /** Stack of objects to be processed. */
  private LinkedList<NumberedObject> toProcess =
    new LinkedList<NumberedObject>();
  private int maxFileId = 0;
  /**
   * A map from file id&rsto {@link IOData} objects for all files that have not
   * yet been opened for processing.
   * <p/>
   * <b>N.B.</b>: A file that has not been opened may have been completed in a
   * previous run (<i>i.e.</i>, it may be that its log file indicates that all
   * objects were previously processed).
   */
  private SortedMap<Integer, IOData> unopenedFiles =
    new TreeMap<Integer, IOData>();
  /**
   * A map from file id&rsquo;s to {@link IOData} objects for all files that are
   * not yet done (are either opened but unprocessed or have not yet had all
   * their objects processed).
   */
  private NavigableMap<Integer, IOData> files = new TreeMap<Integer, IOData>();
  /**
   * A map from file id&rsquo;s to {@link IOData} objects for all files that
   * have been opened for processing have not yet begun to be processed in this
   * run.  This map will never grow beyond {@link #maxNumOpenFiles} in size. In
   * this way, this data member acts like a buffer of files whose streams have
   * been opened but that have not begun to be processed.
   *
   * @see #gotoNextFile(int)
   * @see #openFileForProcessing(danbikel.switchboard.Switchboard.IOData)
   */
  private Map<Integer, IOData> unProcessedFiles =
    new HashMap<Integer, IOData>();
  private volatile IOData currFile = null;
  private ThreadGroup dumpers = new ThreadGroup("Dumpers");
  private volatile int totalNumObjectsProcessed = 0;
  /** When recovering, report stats for number of objs. processed this run. */
  private int totalNumObjectsProcessedThisRun = 0;
  private boolean reProcess = false;

  // switchboard data
  private String bindingName = null;
  /** The port of this UnicastRemoteObject, where 0 indicates an
      anonymous port was used. */
  private int port;
  private int nextClientId = 0;
  /** A map of client id&rsquo;s  to {@link ClientData} objects. */
  private Map<Integer, ClientData> clients = new HashMap<Integer, ClientData>();
  private int nextServerId = 0;
  /** A map of server id&rsquo;s to {@link ServerData} objects. */
  private Map<Integer, ServerData> servers = new HashMap<Integer, ServerData>();
  /** A set of all the {@link ServerData} objects that are values in
      <code>servers</code>, ordered by their load, with the most
      lightly-loaded being first in the order. */
  private SortedSet<ServerData> sortedServers = new TreeSet<ServerData>();
  private ThreadGroup keepAlives = new ThreadGroup("KeepAlives");

  // keep-alive data and other switchboard-specific settings
  private int keepAliveMaxRetries;
  private int keepAliveInterval;
  private boolean serverDeathKillClients;
  private boolean sortOutput;

  // other data
  /** A timer object used to collect stats on object processing; set
      to new object (or set its start time) upon first call to {@link
      #nextObject(int)}. */
  private Time timer;
  private Properties settings = null;

  private TimeoutSocketFactory tsf;


  // constructors

  /**
   * Constructs a <code>Switchboard</code> with all default settings.
   * The defaults are as follows:
   * <ul>
   * <li> the messages filename will be {@link #defaultMessagesFilename}
   * <li> the output to the messages file will not be verbose
   * (use the {@link #setVerbose(boolean)} method to change this setting)
   * <li> the default file encoding will be used, which is the value
   * of the System property <code>"file.encoding"</code>
   * (use {@link #setEncoding(String)} to change)
   * <li> RMI calls will be received on an anonymous port
   * <li> if recovering from a previous run, previously unprocessed objects
   * will <i>not</i> be re-processed
   * <li> the default <code>ObjectReaderFactory</code> will be used for
   * reading un-numbered objects (see {@link #getDefaultObjectReaderFactory})
   * <li> the default <code>ObjectReaderFactory</code> will be used for
   * reading numbered objects (see
   * {@link #getDefaultNumberedObjectReaderFactory})
   * <li> the default <code>ObjectWriterFactory</code> will be used for
   * writing un-numbered objects (see {@link #getDefaultObjectWriterFactory})
   * <li> the default <code>ObjectWriterFactory</code> will be used for
   * writing numbered objects (see
   * {@link #getDefaultNumberedObjectWriterFactory})
   * <li> the default registry binding name will be used, which is
   * {@link #defaultBindingName}
   * </ul>
   * <p>
   * Additionally, the settings controlled via the {@link
   * #setSettings(Properties)} method will initially be set to their
   * defaults: {@link #defaultKeepAliveInterval}, {@link
   * #defaultKeepAliveMaxRetries} and {@link
   * #defaultServerDeathKillClients}.
   * @throws RemoteException if there is an underlying exception thrown
   * by this constructor
   */
  public Switchboard() throws RemoteException {
    this(defaultMessagesFilename);
  }

  /**
   * Constructs a <code>Switchboard</code> object using the specified
   * filename for the messages file.
   * <p>
   * The following default settings will be used:
   * <ul>
   * <li> the output to the messages file will not be verbose
   * (use the {@link #setVerbose(boolean)} method to change this setting)
   * <li> the default file encoding will be used, which is the value
   * of the System property <code>"file.encoding"</code>
   * (use {@link #setEncoding(String)} to change)
   * <li> RMI calls will be received on an anonymous port
   * <li> if recovering from a previous run, previously unprocessed objects
   * will <i>not</i> be re-processed
   * <li> the default <code>ObjectReaderFactory</code> will be used for
   * reading un-numbered objects (see {@link #getDefaultObjectReaderFactory})
   * <li> the default <code>ObjectReaderFactory</code> will be used for
   * reading numbered objects (see
   * {@link #getDefaultNumberedObjectReaderFactory})
   * <li> the default <code>ObjectWriterFactory</code> will be used for
   * writing un-numbered objects (see {@link #getDefaultObjectWriterFactory})
   * <li> the default <code>ObjectWriterFactory</code> will be used for
   * writing numbered objects (see
   * {@link #getDefaultNumberedObjectWriterFactory})
   * <li> the default registry binding name will be used, which is
   * {@link #defaultBindingName}
   * </ul>
   * <p>
   * Additionally, the settings controlled via the {@link
   * #setSettings(Properties)} method will initially be set to their
   * defaults: {@link #defaultKeepAliveInterval}, {@link
   * #defaultKeepAliveMaxRetries} and {@link
   * #defaultServerDeathKillClients}.
   *
   * @param msgsFilename the filename of the messages output writer,
   * to be appended to as as long as this switchboard is running
   * @throws RemoteException if there is an underlying exception thrown
   * by this constructor
   */
  public Switchboard(String msgsFilename) throws RemoteException {
    this(getMessagesPrintWriter(msgsFilename), defaultPort);
  }

  /**
   * Constructs a <code>Switchboard</code> with the specified message file
   * writer.
   * <p>
   * The following default settings will be used:
   * <ul>
   * <li> the output to the messages file will not be verbose
   * (use the {@link #setVerbose(boolean)} method to change this setting)
   * <li> the default file encoding will be used, which is the value
   * of the System property <code>"file.encoding"</code>
   * (use {@link #setEncoding(String)} to change)
   * <li> RMI calls will be received on an anonymous port
   * <li> if recovering from a previous run, previously unprocessed objects
   * will <i>not</i> be re-processed
   * <li> the default <code>ObjectReaderFactory</code> will be used for
   * reading un-numbered objects (see {@link #getDefaultObjectReaderFactory})
   * <li> the default <code>ObjectReaderFactory</code> will be used for
   * reading numbered objects (see
   * {@link #getDefaultNumberedObjectReaderFactory})
   * <li> the default <code>ObjectWriterFactory</code> will be used for
   * writing un-numbered objects (see {@link #getDefaultObjectWriterFactory})
   * <li> the default <code>ObjectWriterFactory</code> will be used for
   * writing numbered objects (see
   * {@link #getDefaultNumberedObjectWriterFactory})
   * <li> the default registry binding name will be used, which is
   * {@link #defaultBindingName}
   * </ul>
   * <p>
   * Additionally, the settings controlled via the {@link
   * #setSettings(Properties)} method will initially be set to their
   * defaults: {@link #defaultKeepAliveInterval}, {@link
   * #defaultKeepAliveMaxRetries} and {@link
   * #defaultServerDeathKillClients}.
   *
   * @param msgs the messages output writer, to be appended to as
   * as long as this switchboard is running
   * @throws RemoteException if there is an underlying exception thrown
   * by this constructor
   */
  public Switchboard(PrintWriter msgs) throws RemoteException {
    this(msgs, defaultPort);
  }

  /**
   * Constructs a <code>Switchboard</code> object with the specified
   * messages filename and port on which to receive RMI calls.
   * <p>
   * The following default settings will be used:
   * <ul>
   * <li> the output to the messages file will not be verbose
   * (use the {@link #setVerbose(boolean)} method to change this setting)
   * <li> the default file encoding will be used, which is the value
   * of the System property <code>"file.encoding"</code>
   * (use {@link #setEncoding(String)} to change)
   * <li> if recovering from a previous run, previously unprocessed objects
   * will <i>not</i> be re-processed
   * <li> the default <code>ObjectReaderFactory</code> will be used for
   * reading un-numbered objects (see {@link #getDefaultObjectReaderFactory})
   * <li> the default <code>ObjectReaderFactory</code> will be used for
   * reading numbered objects (see
   * {@link #getDefaultNumberedObjectReaderFactory})
   * <li> the default <code>ObjectWriterFactory</code> will be used for
   * writing un-numbered objects (see {@link #getDefaultObjectWriterFactory})
   * <li> the default <code>ObjectWriterFactory</code> will be used for
   * writing numbered objects (see
   * {@link #getDefaultNumberedObjectWriterFactory})
   * <li> the default registry binding name will be used, which is
   * {@link #defaultBindingName}
   * </ul>
   * <p>
   * Additionally, the settings controlled via the {@link
   * #setSettings(Properties)} method will initially be set to their
   * defaults: {@link #defaultKeepAliveInterval}, {@link
   * #defaultKeepAliveMaxRetries} and {@link
   * #defaultServerDeathKillClients}.
   *
   * @param msgsFilename the filename of the messages output writer,
   * to be appended to as as long as this switchboard is running
   * @param port the port for this <code>UnicastRemoteObject</code> to
   * listen for RMI calls
   * @throws RemoteException if there is an underlying exception thrown
   * by this constructor
   */
  public Switchboard(String msgsFilename, int port) throws RemoteException {
    this(getMessagesPrintWriter(msgsFilename), port);
  }

  /**
   * Constructs a <code>Switchboard</code> object with the specified
   * message file writer and the specified port on which to receive RMI calls.
   * <p>
   * The following default settings will be used:
   * <ul>
   * <li> the output to the messages file will not be verbose
   * (use the {@link #setVerbose(boolean)} method to change this setting)
   * <li> the default file encoding will be used, which is the value
   * of the System property <code>"file.encoding"</code>
   * (use {@link #setEncoding(String)} to change)
   * <li> if recovering from a previous run, previously unprocessed objects
   * will <i>not</i> be re-processed
   * <li> the default <code>ObjectReaderFactory</code> will be used for
   * reading un-numbered objects (see {@link #getDefaultObjectReaderFactory})
   * <li> the default <code>ObjectReaderFactory</code> will be used for
   * reading numbered objects (see
   * {@link #getDefaultNumberedObjectReaderFactory})
   * <li> the default <code>ObjectWriterFactory</code> will be used for
   * writing un-numbered objects (see {@link #getDefaultObjectWriterFactory})
   * <li> the default <code>ObjectWriterFactory</code> will be used for
   * writing numbered objects (see
   * {@link #getDefaultNumberedObjectWriterFactory})
   * <li> the default registry binding name will be used, which is
   * {@link #defaultBindingName}
   * </ul>
   * <p>
   * Additionally, the settings controlled via the {@link
   * #setSettings(Properties)} method will initially be set to their
   * defaults: {@link #defaultKeepAliveInterval}, {@link
   * #defaultKeepAliveMaxRetries} and {@link
   * #defaultServerDeathKillClients}.
   *
   * @param msgs the messages output writer, to be appended to as
   * as long as this switchboard is running
   * @param port the port for this <code>UnicastRemoteObject</code> to
   * listen for RMI calls
   * @throws RemoteException if there is an underlying exception thrown
   * by this constructor
   */
  public Switchboard(PrintWriter msgs, int port)
    throws RemoteException {
    this(msgs, port, defaultReProcess);
  }

  /**
   * Constructs a <code>Switchboard</code> with the specified message
   * filename, port and re-process option.
   * <p>
   * The following default settings will be used:
   * <ul>
   * <li> the output to the messages file will not be verbose
   * (use the {@link #setVerbose(boolean)} method to change this setting)
   * <li> the default file encoding will be used, which is the value
   * of the System property <code>"file.encoding"</code>
   * (use {@link #setEncoding(String)} to change)
   * <li> the default <code>ObjectReaderFactory</code> will be used for
   * reading un-numbered objects (see {@link #getDefaultObjectReaderFactory})
   * <li> the default <code>ObjectReaderFactory</code> will be used for
   * reading numbered objects (see
   * {@link #getDefaultNumberedObjectReaderFactory})
   * <li> the default <code>ObjectWriterFactory</code> will be used for
   * writing un-numbered objects (see {@link #getDefaultObjectWriterFactory})
   * <li> the default <code>ObjectWriterFactory</code> will be used for
   * writing numbered objects (see
   * {@link #getDefaultNumberedObjectWriterFactory})
   * <li> the default registry binding name will be used, which is
   * {@link #defaultBindingName}
   * </ul>
   * <p>
   * Additionally, the settings controlled via the {@link
   * #setSettings(Properties)} method will initially be set to their
   * defaults: {@link #defaultKeepAliveInterval}, {@link
   * #defaultKeepAliveMaxRetries} and {@link
   * #defaultServerDeathKillClients}.
   *
   * @param msgsFilename the filename of the messages output writer,
   * to be appended to as as long as this switchboard is running
   * @param port the port for this <code>UnicastRemoteObject</code> to
   * listen for RMI calls
   * @param reProcess indicates whether to attempt to re-process objects
   * that were not processed previously, if recovering from a previous run
   * using an existing log file
   * @throws RemoteException if there is an underlying exception thrown
   * by this constructor
   */
  public Switchboard(String msgsFilename, int port, boolean reProcess)
    throws RemoteException {
    this(getMessagesPrintWriter(msgsFilename), port, reProcess);
  }

  /**
   * Constructs a <code>Switchboard</code> object with the specified
   * message file writer, port and re-process option.
   * <p>
   * The following default settings will be used:
   * <ul>
   * <li> the output to the messages file will not be verbose
   * (use the {@link #setVerbose(boolean)} method to change this setting)
   * <li> the default file encoding will be used, which is the value
   * of the System property <code>"file.encoding"</code>
   * (use {@link #setEncoding(String)} to change)
   * <li> the default <code>ObjectReaderFactory</code> will be used for
   * reading un-numbered objects (see {@link #getDefaultObjectReaderFactory})
   * <li> the default <code>ObjectReaderFactory</code> will be used for
   * reading numbered objects (see
   * {@link #getDefaultNumberedObjectReaderFactory})
   * <li> the default <code>ObjectWriterFactory</code> will be used for
   * writing un-numbered objects (see {@link #getDefaultObjectWriterFactory})
   * <li> the default <code>ObjectWriterFactory</code> will be used for
   * writing numbered objects (see
   * {@link #getDefaultNumberedObjectWriterFactory})
   * <li> the default registry binding name will be used, which is
   * {@link #defaultBindingName}
   * </ul>
   * <p>
   * Additionally, the settings controlled via the {@link
   * #setSettings(Properties)} method will initially be set to their
   * defaults: {@link #defaultKeepAliveInterval}, {@link
   * #defaultKeepAliveMaxRetries} and {@link
   * #defaultServerDeathKillClients}.
   *
   * @param msgs the messages output writer, to be appended to as
   * as long as this switchboard is running
   * @param port the port for this <code>UnicastRemoteObject</code> to
   * listen for RMI calls
   * @param reProcess indicates whether to attempt to re-process objects
   * that were not processed previously, if recovering from a previous run
   * using an existing log file
   * @throws RemoteException if there is an underlying exception thrown
   * by this constructor
   */
  public Switchboard(PrintWriter msgs, int port, boolean reProcess)
    throws RemoteException {
    this(msgs, port,
	 reProcess,
	 getDefaultObjectReaderFactory(),
	 getDefaultNumberedObjectReaderFactory(),
	 getDefaultObjectWriterFactory(),
	 getDefaultNumberedObjectWriterFactory());
  }

  /**
   * Constructs a <code>Switchboard</code> with the specified message
   * filename, port, re-process option and object reader/writer factories.
   * <p>
   * The following default settings will be used:
   * <ul>
   * <li> the output to the messages file will not be verbose
   * (use the {@link #setVerbose(boolean)} method to change this setting)
   * <li> the default file encoding will be used, which is the value
   * of the System property <code>"file.encoding"</code>
   * (use {@link #setEncoding(String)} to change)
   * <li> the default registry binding name will be used, which is
   * {@link #defaultBindingName}
   * </ul>
   * <p>
   * Additionally, the settings controlled via the {@link
   * #setSettings(Properties)} method will initially be set to their
   * defaults: {@link #defaultKeepAliveInterval}, {@link
   * #defaultKeepAliveMaxRetries} and {@link
   * #defaultServerDeathKillClients}.
   *
   * @param msgsFilename the filename of the messages output writer,
   * to be appended to as as long as this switchboard is running
   * @param port the port for this <code>UnicastRemoteObject</code> to
   * listen for RMI calls
   * @param reProcess indicates whether to attempt to re-process objects
   * that were not processed previously, if recovering from a previous run
   * using an existing log file
   * @param objReaderFactory the factory from which to get objects
   * that will be used to read objects from the specified input file
   * @param numObjReaderFactory the factory from which to get objects
   * that will be used to read numbered objects from the log file<br>
   * <b>N.B.</b>: The object readers produced by this factory <i>must</i>
   * only return <code>NumberedObject</code> instances from their
   * underlying streams.
   * @param objWriterFactory the factory from which to get objects
   * that will be used to write objects to the specified output file
   * @param numObjWriterFactory the factory from which to get objects
   * that will be used to write numbered objects to the log file
   * @throws RemoteException if there is an underlying exception thrown
   * by this constructor
   */
  public Switchboard(String msgsFilename,
		     int port, boolean reProcess,
		     ObjectReaderFactory objReaderFactory,
		     ObjectReaderFactory numObjReaderFactory,
		     ObjectWriterFactory objWriterFactory,
		     ObjectWriterFactory numObjWriterFactory)
    throws RemoteException {
    this(getMessagesPrintWriter(msgsFilename),
	 port, reProcess,
	 objReaderFactory, numObjReaderFactory,
	 objWriterFactory, numObjWriterFactory);
  }

  /**
   * Constructs a <code>Switchboard</code> with the specified message file
   * writer, port, re-process option and object reader/writer factories.
   * <p>
   * The following default settings will be used:
   * <ul>
   * <li> the output to the messages file will not be verbose
   * (use the {@link #setVerbose(boolean)} method to change this setting)
   * <li> the default file encoding will be used, which is the value
   * of the System property <code>"file.encoding"</code>
   * (use {@link #setEncoding(String)} to change)
   * <li> the default registry binding name will be used, which is
   * {@link #defaultBindingName}
   * </ul>
   * <p>
   * Additionally, the settings controlled via the {@link
   * #setSettings(Properties)} method will initially be set to their
   * defaults: {@link #defaultKeepAliveInterval}, {@link
   * #defaultKeepAliveMaxRetries} and {@link
   * #defaultServerDeathKillClients}.
   *
   * @param msgs the messages output writer, to be appended to as
   * as long as this switchboard is running
   * @param port the port for this <code>UnicastRemoteObject</code> to
   * listen for RMI calls
   * @param reProcess indicates whether to attempt to re-process objects
   * that were not processed previously, if recovering from a previous run
   * using an existing log file
   * @param objReaderFactory the factory from which to get objects
   * that will be used to read objects from the specified input file
   * @param numObjReaderFactory the factory from which to get objects
   * that will be used to read numbered objects from the log file<br>
   * <b>N.B.</b>: The object readers produced by this factory <i>must</i>
   * only return <code>NumberedObject</code> instances from their
   * underlying streams.
   * @param objWriterFactory the factory from which to get objects
   * that will be used to write objects to the specified output file
   * @param numObjWriterFactory the factory from which to get objects
   * that will be used to write numbered objects to the log file
   * @throws RemoteException if there is an underlying exception thrown
   * by this constructor
   */
  public Switchboard(PrintWriter msgs,
		     int port, boolean reProcess,
		     ObjectReaderFactory objReaderFactory,
		     ObjectReaderFactory numObjReaderFactory,
		     ObjectWriterFactory objWriterFactory,
		     ObjectWriterFactory numObjWriterFactory)
    throws RemoteException {
    this(msgs, port, reProcess,
	 objReaderFactory, numObjReaderFactory,
	 objWriterFactory, numObjWriterFactory,
	 defaultBindingName);
  }

  /**
   * Constructs a <code>Switchboard</code> with the specified message file
   * writer, port, re-process option, object reader/writer factories and
   * registry binding name.
   * <p>
   * The following default settings will be used:
   * <ul>
   * <li> the output to the messages file will not be verbose
   * (use the {@link #setVerbose(boolean)} method to change this setting)
   * <li> the default file encoding will be used, which is the value
   * of the System property <code>"file.encoding"</code>
   * (use {@link #setEncoding(String)} to change)
   * </ul>
   * <p>
   * Additionally, the settings controlled via the {@link
   * #setSettings(Properties)} method will initially be set to their
   * defaults: {@link #defaultKeepAliveInterval}, {@link
   * #defaultKeepAliveMaxRetries} and {@link
   * #defaultServerDeathKillClients}.
   *
   * @param msgsFilename the filename of the messages output writer,
   * to be appended to as as long as this switchboard is running
   * @param port the port for this <code>UnicastRemoteObject</code> to
   * listen for RMI calls
   * @param reProcess indicates whether to attempt to re-process objects
   * that were not processed previously, if recovering from a previous run
   * using an existing log file
   * @param objReaderFactory the factory from which to get objects
   * that will be used to read objects from the specified input file
   * @param numObjReaderFactory the factory from which to get objects
   * that will be used to read numbered objects from the log file<br>
   * <b>N.B.</b>: The object readers produced by this factory <i>must</i>
   * only return <code>NumberedObject</code> instances from their
   * underlying streams.
   * @param objWriterFactory the factory from which to get objects
   * that will be used to write objects to the specified output file
   * @param numObjWriterFactory the factory from which to get objects
   * that will be used to write numbered objects to the log file
   * @param bindingName the name under which this switchboard will be
   * bound using <code>Naming.bind</code> or <code>Naming.rebind</code>,
   * so that this switchboard can unexport itself when it is finished
   * @throws RemoteException if there is an underlying exception thrown
   * by this constructor
   */
  public Switchboard(String msgsFilename,
		     int port, boolean reProcess,
		     ObjectReaderFactory objReaderFactory,
		     ObjectReaderFactory numObjReaderFactory,
		     ObjectWriterFactory objWriterFactory,
		     ObjectWriterFactory numObjWriterFactory,
		     String bindingName)
    throws RemoteException {
    this(getMessagesPrintWriter(msgsFilename),
	 port, reProcess,
	 objReaderFactory, numObjReaderFactory,
	 objWriterFactory, numObjWriterFactory,
	 bindingName);
  }

  /**
   * Constructs a <code>Switchboard</code> object with the specified
   * message file writer, port on which to receive RMI
   * calls, re-process option, object reader factories (one for
   * un-numbered objects, another for numbered objects) and registry binding
   * name.
   * <p>
   * The following default settings will be used:
   * <ul>
   * <li> the output to the messages file will not be verbose
   * (use the {@link #setVerbose(boolean)} method to change this setting)
   * <li> the default file encoding will be used, which is the value
   * of the System property <code>"file.encoding"</code>
   * (use {@link #setEncoding(String)} to change)
   * </ul>
   * <p>
   * Additionally, the settings controlled via the {@link
   * #setSettings(Properties)} method will initially be set to their
   * defaults: {@link #defaultKeepAliveInterval}, {@link
   * #defaultKeepAliveMaxRetries} and {@link
   * #defaultServerDeathKillClients}.
   *
   * @param msgs the messages output writer, to be appended to as
   * as long as this switchboard is running
   * @param port the port for this <code>UnicastRemoteObject</code> to
   * listen for RMI calls
   * @param reProcess indicates whether to attempt to re-process objects
   * that were not processed previously, if recovering from a previous run
   * using an existing log file
   * @param objReaderFactory the factory from which to get objects
   * that will be used to read objects from the specified input file
   * @param numObjReaderFactory the factory from which to get objects
   * that will be used to read numbered objects from the log file<br>
   * <b>N.B.</b>: The object readers produced by this factory <i>must</i>
   * only return <code>NumberedObject</code> instances from their
   * underlying streams.
   * @param objWriterFactory the factory from which to get objects
   * that will be used to write objects to the specified output file
   * @param numObjWriterFactory the factory from which to get objects
   * that will be used to write numbered objects to the log file
   * @param bindingName the name under which this switchboard will be
   * bound using <code>Naming.bind</code> or <code>Naming.rebind</code>,
   * so that this switchboard can unexport itself when it is finished
   * @throws RemoteException if there is an underlying exception thrown
   * by this constructor
   */
  public Switchboard(PrintWriter msgs,
		     int port, boolean reProcess,
		     ObjectReaderFactory objReaderFactory,
		     ObjectReaderFactory numObjReaderFactory,
		     ObjectWriterFactory objWriterFactory,
		     ObjectWriterFactory numObjWriterFactory,
		     String bindingName)
    throws RemoteException {
    this.port = port;
    this.encoding = System.getProperty("file.encoding");
    this.objReaderFactory = objReaderFactory;
    this.numObjReaderFactory = numObjReaderFactory;
    this.objWriterFactory = objWriterFactory;
    this.numObjWriterFactory = numObjWriterFactory;
    this.port = port;
    this.reProcess = reProcess;
    this.bindingName = bindingName;

    // set the following to their defaults (see setSettings method)
    this.keepAliveInterval = defaultKeepAliveInterval;
    this.keepAliveMaxRetries = defaultKeepAliveMaxRetries;
    this.serverDeathKillClients = defaultServerDeathKillClients;
    this.sortOutput = defaultSortOutput;

    this.msgs = msgs;

    msgs.println(msgFileHeader);
    msgs.println(className + ": starting up at " + new Date());

    tsf = new TimeoutSocketFactory(0, 0);
    //dumpers.setDaemon(true);
  }

  // constructor helper
  private static PrintWriter getMessagesPrintWriter(String msgsFilename) {
    boolean append = true;
    boolean autoFlush = true;

    try {
      OutputStreamWriter outStreamWriter =
	new OutputStreamWriter(new FileOutputStream(msgsFilename, append));
      return new PrintWriter(new BufferedWriter(outStreamWriter,
						defaultBufSize),
			     autoFlush);
    }
    catch (IOException ioe) {
      System.err.println(className +
			 ": error: couldn't create messages file: " +
			 msgsFilename);
      System.exit(1);
    }
    return null;
  }

  /**
   * Calls {@link #setPolicyFile(String)} with the value of the
   * {@link SwitchboardRemote#switchboardPolicyFile} property obtained from
   * the specified <code>Properties</code> object.
   *
   * @param props the <code>Properties</code> object from which to
   * obtain the value of the {@link SwitchboardRemote#switchboardPolicyFile}
   * property
   */
  public static void setPolicyFile(Properties props) {
    setPolicyFile(props.getProperty(SwitchboardRemote.switchboardPolicyFile));
  }

  /**
   * Calls {@link #setPolicyFile(Class,String)} with the specified class
   * and the value of the {@link SwitchboardRemote#switchboardPolicyFile}
   * property obtained from the specified <code>Properties</code> object.
   *
   * @param cl the class for which to set the policy file
   * @param props the <code>Properties</code> object from which to
   * obtain the value of the {@link SwitchboardRemote#switchboardPolicyFile}
   * property
   */
  public static void setPolicyFile(Class cl, Properties props) {
    setPolicyFile(cl,
		  props.getProperty(SwitchboardRemote.switchboardPolicyFile));
  }

  /**
   * Sets the system property <tt>"java.security.policy"</tt> to be the
   * URL of the specified resource obtained from the
   * <code>SwitchboardRemote</code> class.
   *
   * @param resource the resource to obtain from {@link SwitchboardRemote}
   * that will be the value of the system property
   * <tt>"java.security.policy"</tt>
   */
  public static void setPolicyFile(String resource) {
    setPolicyFile(SwitchboardRemote.class, resource);
  }

  /**
   * Sets the system property <tt>"java.security.policy"</tt> to be the
   * URL of the specified resource obtained from the specified class.
   *
   * @param cl the class for which to set the policy file
   * @param resource the resource to obtain from the specified class
   * that will be the value of the system property
   * <tt>"java.security.policy"</tt>
   */
  public static void setPolicyFile(Class cl, String resource) {
    if (System.getProperty("java.security.policy") == null) {
      java.net.URL policyURL = cl.getResource(resource);
      System.setProperty("java.security.policy", policyURL.toString());
    }
  }

  // registration methods

  /**
   * Sets the specified settings and langauge encoding and then
   * exports this switchboard and binds it to the bootstrap RMI
   * registry.
   * <p>
   * This is a convenience method that calls {@link
   * #setSettings(Properties)}, {@link #setEncoding(String)}, {@link
   * #export} and then <code>Naming.rebind</code>.  By using this
   * method, a class that kick-starts a switchboard need only
   * construct one, call this method and then (optionally) repeatedly
   * call <code>processFile</code> until
   * there are no more files to process, finally calling
   * <code>cleanup</code>, as shown in the following sample code:
   * <pre>
   * public class StartSwitchboard {
   *   public static void main(String args[]) {
   *     try {
   *       Switchboard sb = new Switchboard();
   *       sb.bind(<font color=green>null</font>, System.getProperty("file.encoding"));
   *       <font color=red>// process files specified on the command line</font>
   *       for (int i = 0; i < args.length; i++)
   *         sb.processFile(args[i]);
   *       sb.cleanup();
   *     } catch (Exception e) {
   *       System.err.println(e);
   *     }
   *   }
   * }
   * </pre>
   * The above code processes each file in sequence, where each call
   * to <code>processFile</code> returns only when the file has been completely
   * processed.  In order to simply enqueue all files for processing and then
   * be notified when all processing is complete, the user should call
   * {@link #processFile(String,String,String,boolean)} with a value
   * of <code>false</code> for the final argument, indicating not to wait.
   * After all files have been enqueued, the user may call to
   * {@link #cleanupWhenAllFilesAreDone}, which will not return until all
   * enqueued files have been processed.
   *
   * @param settings the settings that should be set before this object is
   * exported and bound, or <code>null</code> if {@link #setSettings} should
   * not be called with the specified value
   * @param encoding the language encoding that should be set before this
   * object is exported and bound, or <code>null</code> if {link #setEncoding}
   * should not be called with the specified value
   *
   * @see #processFile(String)
   * @see #processFile(String,String,String,boolean)
   * @see #cleanup
   * @see #cleanupWhenAllFilesAreDone
   * @see #setSettings(Properties)
   * @see #setEncoding(String)
   * @see #export
   * @throws MalformedURLException if the binding name is a malformed URL
   * @throws RemoteException if there is an underlying exception thrown by
   *                         this method
   */
  public void bind(Properties settings, String encoding)
    throws RemoteException, MalformedURLException {
    if (settings != null)
      setSettings(settings);
    if (encoding != null)
      setEncoding(encoding);
    export();
    Naming.rebind(bindingName, this);
  }


  /**
   * Exports this object using
   * {@link UnicastRemoteObject#exportObject(Remote,int,
    *       RMIClientSocketFactory,RMIServerSocketFactory)
   * UnicastRemoteObject.exportObject}.
   * @throws RemoteException if this method throws an exception
   */
  public void export() throws RemoteException {
    /*
    UnicastRemoteObject.exportObject(this, port,
				     (RMIClientSocketFactory)tsf,
				     (RMIServerSocketFactory)tsf);
    */
    UnicastRemoteObject.exportObject(this, port);
  }

  // I/O methods

  /**
   * Processes the specified input file and creates an output file
   * with the specified name; a log file will be used to keep track
   * of incremental work.  The name of the output file will be
   * <code>inFilename&nbsp;+&nbsp;{@link #outFilenameSuffix}</code>.
   * The name of the log file will be
   * <code>inFilename&nbsp;+&nbsp;{@link #logFilenameSuffix}</code>.
   * <p>
   * Objects will be read from the input file, doled out to clients
   * upon request, and when the clients have processed all the objects,
   * the output file will be created, putting the processed objects
   * in the same order in which their unprocessed counterparts existed
   * in the input file.  This method will return as soon as
   * object-processing is complete (but possibly before the output file
   * has been created).
   *
   * @param inFilename the name of the input file to be processed
   *
   * @see #processFile(String,String)
   */
  public void processFile(String inFilename) {
    processFile(inFilename,
		inFilename + outFilenameSuffix,
		inFilename + logFilenameSuffix);
  }

  /**
   * Processes the specified input file and creates an output file
   * with the specified name; a log file will be used to keep track
   * of incremental work. The name of the log file will be
   * <code>inFilename&nbsp;+&nbsp;{@link #logFilenameSuffix}</code>.
   * <p>
   * Objects will be read from the input file, doled out to clients
   * upon request, and when the clients have processed all the objects,
   * the output file will be created, putting the processed objects
   * in the same order in which their unprocessed counterparts existed
   * in the input file.  This method will return as soon as
   * object-processing is complete (but possibly before the output file
   * has been created).
   *
   * @param inFilename the name of the input file to be processed
   * @param outFilename the name of the output file to be created
   *
   * @see #processFile(String,String,String)
   */
  public void processFile(String inFilename, String outFilename) {
    processFile(inFilename, outFilename, outFilename + logFilenameSuffix);
  }

  /**
   * Processes the specified input file and creates an output file
   * with the specified name; a log file with the specified name will
   * be used to keep track of incremental work.
   * <p>
   * Objects will be read from the input file, doled out to clients
   * upon request, and when the clients have processed all the objects,
   * the output file will be created, putting the processed objects
   * in the same order in which their unprocessed counterparts existed
   * in the input file.  This method will return as soon as
   * object-processing is complete (but possibly before the output file
   * has been created).
   *
   * @param inFilename the name of the input file to be processed
   * @param outFilename the name of the output file to be created
   * @param logFilename the name of the log file (which is used to
   * keep track of incremental work during input file processing)
   *
   * @see #processFile(String,String,String,boolean)
   */
  public void processFile(String inFilename, String outFilename,
			  String logFilename) {
    processFile(inFilename, outFilename, logFilename, true);
  }

  /**
   * Processes the specified input file and creates an output file
   * with the specified name; a log file with the specified name will
   * be used to keep track of incremental work.
   * <p>
   * Objects will be read from the input file, doled out to clients
   * upon request, and when the clients have processed all the objects,
   * the output file will be created, putting the processed objects
   * in the same order in which their unprocessed counterparts existed
   * in the input file.
   *
   * @param inFilename the name of the input file to be processed
   * @param outFilename the name of the output file to be created
   * @param logFilename the name of the log file (which is used to
   * keep track of incremental work during input file processing)
   * @param wait if <code>true</code>, indicates whether this method
   * should wait until object processing is complete or, if
   * <code>false</code>, return immediately
   */
  public void processFile(String inFilename, String outFilename,
			  String logFilename, boolean wait) {
    IOData fileToProcess =
      new IOData(maxFileId++, inFilename, outFilename, logFilename);
    synchronized (this) {
      try {
	if (unProcessedFiles.size() < maxNumOpenFiles) {
	  openFileForProcessing(fileToProcess);
	}
	else {
	  unopenedFiles.put(fileToProcess.id, fileToProcess);
	}
      }
      catch (IOException ioe) {
	System.err.println(ioe);
	logFailure("processFile (" + ioe + ")");
	return;
      }
    }
    if (wait) {
      synchronized (fileToProcess) {
	try {
	  while (!fileToProcess.done())
	    fileToProcess.wait();
	}
	catch (InterruptedException ie) {
	  System.err.println(ie);
	  logFailure("processFile: thread interrupted (" + ie + ")");
	}
      }
    }
    // if wait is true, this method returns when processing is done
    // (but dumping may still be happening asynchronously upon return
    // of this method)
  }

  private void openFileForProcessing(IOData fileToProcess)
    throws IOException {

    fileToProcess.open();

    String logFilename = fileToProcess.logName;
    String outFilename = fileToProcess.outName;

    if (!fileToProcess.done()) {
      files.put(fileToProcess.id, fileToProcess);
      unProcessedFiles.put(fileToProcess.id, fileToProcess);
    }
    // start dumper thread if the input file does not appear to have
    // been previously processed and output to the output file;
    // also, add the new IOData object to the files map
    if (logFilename != null && outFilename != null &&
	!fileToProcess.done()) {
      Thread dumper = new Thread(dumpers,
				 fileToProcess,
				 "Dumper (file " + fileToProcess.id + ")");
      dumper.start();
    }
  }


  /**
   * Sets the internal settings of this switchboard to the specified
   * <code>Properties</code> object.  This object is returned by the {@link
   * #getSettings} method, to allow clients and servers to all have the same
   * settings.
   * <p/>
   * If the specified <code>Properties</code> object contains any of the
   * switchboard-specific properties specified in <code>SwitchboardRemote</code>,
   * the internal settings of this <code>Switchboard</code> object are set
   * accordingly.
   *
   * @param settings the settings for this Switchboard instance to dole out to
   *                 its users
   * @see #getSettings()
   * @see SwitchboardRemote#socketTimeout
   * @see SwitchboardRemote#keepAliveInterval
   * @see SwitchboardRemote#keepAliveMaxRetries
   * @see SwitchboardRemote#serverDeathKillClients
   */
  public void setSettings(Properties settings) {
    this.settings = settings;
    setSocketTimeout();
    setKeepAliveInterval();
    setKeepAliveMaxRetries();
    setServerDeathKillClients();
    setSortOutput();
    setDisableHttp();
  }

  private void setSocketTimeout() {
    int timeout = 0;
    if (settings != null) {
      String socketTimeoutStr =
	settings.getProperty(SwitchboardRemote.socketTimeout);
      if (socketTimeoutStr != null)
	timeout = Integer.parseInt(socketTimeoutStr);
    }
    tsf.setTimeout(timeout, timeout);
  }

  private void setKeepAliveInterval() {
    if (settings != null) {
      String keepAliveIntervalStr =
	settings.getProperty(SwitchboardRemote.keepAliveInterval);
      if (keepAliveIntervalStr != null)
	keepAliveInterval = Integer.parseInt(keepAliveIntervalStr);
    }
  }

  private void setKeepAliveMaxRetries() {
    if (settings != null) {
      String retriesStr =
	settings.getProperty(SwitchboardRemote.keepAliveMaxRetries);
      if (retriesStr != null)
	keepAliveMaxRetries = Integer.parseInt(retriesStr);
    }
  }

  private void setServerDeathKillClients() {
    if (settings != null) {
      String serverDeathKillClientsStr =
	settings.getProperty(SwitchboardRemote.serverDeathKillClients);
      if (serverDeathKillClientsStr != null)
	serverDeathKillClients =
	  Boolean.valueOf(serverDeathKillClientsStr).booleanValue();
    }
  }

  private void setSortOutput() {
    if (settings != null) {
      String sortOutputStr = settings.getProperty(SwitchboardRemote.sortOutput);
      if (sortOutputStr != null)
	sortOutput = Boolean.valueOf(sortOutputStr).booleanValue();
    }
  }

  private void setDisableHttp() {
    if (settings != null) {
      String disableHttpStr =
	settings.getProperty(SwitchboardRemote.switchboardDisableHttp);
      if (disableHttpStr != null) {
	System.setProperty("java.rmi.server.disableHttp", disableHttpStr);
      }
    }
  }

  /**
   * Sets the default character encoding (applicable if any
   * <code>ObjectReader</code> or <code>ObjectWriter</code> instances
   * created by the switchboard's factories are character-based).
   * @param enc the character encoding to set
   */
  public void setEncoding(String enc) {
    if (verbose)
      log("using encoding " + enc);
    encoding = enc;
  }

  /**
   * Sets the buffer size to be used for all streams by the switchboard
   * (except the messages output file, which uses the value of
   * {@link #defaultBufSize}).
   * @param bufSize the buffer size to set
   */
  public void setBufSize(int bufSize) {
    this.bufSize = bufSize;
  }

  /**
   * Gets the value for the specified settings from the switchboard's
   * internal <code>Properties</code> object.
   */
  public String getSetting(String settingName) throws RemoteException {
    if (settings == null)
      return null;
    else
      return settings.getProperty(settingName);
  }

  /**
   * Gets the internal <code>Properties</code> object used for various
   * settings of this switchboard.
   */
  public Properties getSettings() throws RemoteException { return settings; }

  public int register(Client client) throws RemoteException {
    String host = null;
    try { host = client.host(); }
    catch (RemoteException re) {
      throw new RegistrationException(className + ": problem invoking a " +
				      "client method", re.detail);
    }
    synchronized (this) {
      int clientId = nextClientId++;
      ClientData data = new ClientData(clientId, host, client);
      clients.put(new Integer(clientId), data);
      new Thread(keepAlives,
		 new KeepAlive(data), "KeepAlive for " + data).start();

      if (verbose)
	log("register(Client): registered client running on host " + host,
	    clientId);

      return clientId;
    }
  }

  public int register(Server server) throws RemoteException {
    String host = null;
    int maxClients = -2;
    boolean acceptClientsOnlyByRequest = false;
    try {
      host = server.host();
      maxClients = server.maxClients();
      acceptClientsOnlyByRequest = server.acceptClientsOnlyByRequest();
    }
    catch (RemoteException re) {
      throw new RegistrationException(className + ": problem invoking " +
				      "a server method", re.detail);
    }
    /*
    if (host == null) {
      throw new RegistrationException(className +
				      ": host method returned null");
    }
    */
    if (!(maxClients > 0 || maxClients == Server.acceptUnlimitedClients))
      throw new RegistrationException(className +
				      ": illegal value for maxClients");
    if (maxClients == Server.acceptUnlimitedClients)
      maxClients = ServerData.defaultMaxClients;
    synchronized (this) {
      int serverId = nextServerId++;
      ServerData data = new ServerData(serverId, host, server, maxClients,
				       acceptClientsOnlyByRequest);
      servers.put(new Integer(serverId), data);
      sortedServers.add(data);
      new Thread(keepAlives,
		 new KeepAlive(data), "KeepAlive for " + data).start();

      if (verbose)
	log("register(Server): registered server running on host " + host,
	    -1, serverId);

      return serverId;
    }
  }

  public synchronized Server getServer(int clientId) throws RemoteException {
    if (servers.size() == 0)
      return null;

    ClientData clientData = checkValidClient("getServer(int)", clientId);

    // go through servers, starting with most lightly-loaded, to find
    // one able to accept a client
    for (ServerData serverData : sortedServers) {
      // since sorted by load, if we come across a fully-loaded server,
      // we're done
      if (ineligibleServer(serverData))
	return null;

      updateServerData(serverData);
      if (serverData.acceptClientsOnlyByRequest)
	continue;

      assignClientToServer(clientData, serverData);

      return serverData.server;
    }

    // all servers were either fully loaded or are accepting clients only
    // by request
    return null;
  }

  public synchronized Server getServer(int clientId, int serverId)
    throws RemoteException {

    ClientData clientData = checkValidClient("getServer(int,int)", clientId);

    // grab requested server for client
    ServerData serverData = (ServerData)servers.get(new Integer(serverId));
    if (ineligibleServer(serverData)) {
      if (verbose)
	logFailure("getServer(int,int)", clientId, serverId);
      if (serverData == null)
	throw new UnrecognizedServerException(className +
					      ": invalid server ID: " +
					      serverId);
      else
	return null;
    }

    updateServerData(serverData);

    assignClientToServer(clientData, serverData);

    return serverData.server;
  }

  private void updateServerData(ServerData data) throws RemoteException {
    Server server = data.server;
    data.acceptClientsOnlyByRequest = server.acceptClientsOnlyByRequest();
    int newMaxClients = server.maxClients();
    if (data.maxClients != newMaxClients) {
      sortedServers.remove(data);
      data.maxClients = newMaxClients;
      sortedServers.add(data);
    }
  }

  private void assignClientToServer(ClientData client, ServerData server) {
    // remove server from sorted set while it is being manipulated
    sortedServers.remove(server);

    // add client to set of server's clients
    server.clients.add(new Integer(client.id));

    // re-insert server (possibly into a new position in the ordering)
    sortedServers.add(server);

    // remove client from its old server, if it had one
    ServerData oldServer =
      (ServerData)servers.get(new Integer(client.serverId));
    if (oldServer != null) {
      sortedServers.remove(oldServer);
      oldServer.clients.remove(new Integer(client.id));
      sortedServers.add(oldServer);
    }

    // set client's data object to show that it has been assigned serverId
    client.serverId = server.id;

    if (verbose)
      log("assigned client to server", client.id, server.id);
  }

  // object server methods

  /**
   * Helper method for {@link #nextObject(int)} that finds the next file in the
   * <code>files</code> map, sets the <code>currFile</code> data member and
   * informs consumers of a new file, if one is found.
   *
   * @param clientId the id of the client invoking this method
   * @throws RemoteException if this method throws an underlying exception
   */
  @SuppressWarnings({"UnusedDeclaration"})
  void gotoNextFile(int clientId) throws RemoteException {
    if (unProcessedFiles.size() == 0)
      return;

    int currFileId = currFile == null ? firstFileId - 1 : currFile.id;

    Map.Entry<Integer, IOData> nextEntry = files.higherEntry(currFileId);
    // grab the next-highest file id
    Integer nextFileId = nextEntry == null ? null : nextEntry.getKey();
    // make currFile be the next file
    currFile = nextEntry == null ? null : nextEntry.getValue();

    // if we found a new file, currFile will be non-null, so tell all
    // the consumers about it
    if (currFile != null) {
      synchronized (consumers) {
	if (consumers.size() > 0) {
	  for (Consumer consumer : consumers) {
	    currFile.registerConsumer(consumer);
	  }
	}
      }

      if (!unProcessedFiles.containsKey(nextFileId)) {
	logFailure("uh-oh: " + nextFileId +
		   " should be in unProcessedFiles map, but is not" +
		   " (unProcessedFiles.keySet=" + unProcessedFiles.keySet() +
		   ")");
      }
      unProcessedFiles.remove(nextFileId);

      // now that we've removed a file from the set of unprocessed files,
      // fill up that buffer with as many of the unopened files as we can
      // (opening them as we go, since openFileForProcessing opens a file and,
      // if it has not been completely processed, sticks it in both the files
      // and unProcessedFiles maps)
      Iterator<Map.Entry<Integer, IOData>> unopenedFileIt =
	unopenedFiles.entrySet().iterator();
      while (unProcessedFiles.size() < maxNumOpenFiles &&
	     unopenedFileIt.hasNext()) {
	Map.Entry<Integer, IOData> unopenedFileEntry = unopenedFileIt.next();
	try {
	  openFileForProcessing(unopenedFileEntry.getValue());
	}
	catch (IOException ioe) {
	  System.err.println(ioe);
	  logFailure("processFile (" + ioe + ")");
	}
	unopenedFileIt.remove();
      }

      log("starting processing on file " + currFile);
    }
  }

  NumberedObject getObjectFromCurrFileAndAssignToClient(ClientData clientData) {
    NumberedObject numObj = null;
    if (currFile != null) {
      numObj = currFile.nextObject(clientData.id);
      if (numObj != null)
	clientData.objectsInProgress.put(numObj.uid(), numObj);
    }
    return numObj;
  }

  public synchronized NumberedObject nextObject(int clientId)
    throws RemoteException {

    ClientData clientData = checkValidClient("nextObject", clientId);

    NumberedObject numObj = null;
    if (toProcess.size() == 0) {
      // we've got to grab something from a file
      // if we couldn't grab a next object from the current file
      // (possibly because we haven't even started processing the very first
      // file, when currFile == null), go to the next enqueued file and try to
      // read from it
      if ((currFile == null || !currFile.moreObjectsToRead()) &&
	  unProcessedFiles.size() > 0) {
	gotoNextFile(clientId);
      }
      numObj = getObjectFromCurrFileAndAssignToClient(clientData);
    }
    else {
      numObj = (NumberedObject)toProcess.removeLast();
      clientData.objectsInProgress.put(numObj.uid(), numObj);
      if (verbose)
	log("nextObject: got object " + numObj.uid() + " off of deque",
	    clientId);
    }

    // start timer if this is the first object of switchboard's life
    if (timer == null)
      timer = new Time();

    return numObj;
  }

  /**
   * Registers the specified consumer of processed objects with this
   * switchboard.  It is guaranteed that the consumer's
   * {@link Consumer#newFile(String,String)} method will be invoked
   * before this registration method returns.  Note that it is possible
   * that all objects from the current file could have been processed
   * before this method returns.  In order to guarantee that one or more
   * consumers post-process all objects of a particular input file,
   * the consumers should be registered, via this method, <i>before</i>
   * files are processed via one of the <code>processFile</code> methods.
   *
   * @param consumer the consumer to be registered
   *
   * @see Consumer#newFile(String,String)
   * @see #processFile(String)
   * @see #processFile(String,String)
   * @see #processFile(String,String,String)
   * @see #processFile(String,String,String,boolean)
   */
  public void registerConsumer(Consumer consumer) throws RemoteException {
    synchronized (consumers) {
      consumers.add(consumer);
    }
    synchronized (this) {
      if (currFile != null)
	currFile.registerConsumer(consumer);
    }
  }

  public void putObject(int clientId,
			NumberedObject obj,
			long millis) throws RemoteException {
    ClientData clientData = null;
    IntPair objectId = null;

    IOData file = null;

    synchronized (this) {
      clientData = checkValidClient("putObject", clientId);
      objectId = obj.uid();
      if (clientData.objectsInProgress.containsKey(objectId)) {
	clientData.objectsInProgress.remove(objectId);
      }
      else {
	String errMsg =
	  "putObject: client " + clientId +
	  " not known to be processing object No. " + objectId;
	if (verbose)
	  logFailure(errMsg, clientId);
	throw new RemoteException(errMsg);
      }

      // grab IOData object for this NumberedObject
      // (we don't use files map if we don't have to)
      if (currFile != null && obj.fileId() == currFile.id)
	file = currFile;
      else
	file = files.get(obj.fileId());

      if (file == null) {
	String errMsg =
	  "putObject: client " + clientId + " processed object " + objectId +
	  " from an unknown file";
	if (verbose)
	  logFailure(errMsg, clientId);
	throw new RemoteException(errMsg);
      }
    }

    file.writeToLog(obj);

    synchronized (msgs) {
      int numClients = clients.size();
      totalNumObjectsProcessed++;
      totalNumObjectsProcessedThisRun++;
      if (verbose)
	log("processed object " + objectId +
	    " in " + Time.elapsedTime(millis), clientId);
      log("elapsed for " + totalNumObjectsProcessed +
	  " objs./" + numClients + " client" +
	  (numClients > 1 ? "s" : "") + ": " +
	  timer + "; avg. processing time: " +
	  Time.elapsedTime((Time.current() - timer.startTime()) /
			   totalNumObjectsProcessedThisRun));
    }

    if (file.done()) {
      //log("client " + clientId + " has completed " + file);
      synchronized (consumers) {
	if (consumers.size() > 0) {
	  for (Consumer consumer : consumers) {
	    consumer.processingComplete(file.inName, file.outName);
	  }
	}
      }
      //log("client " + clientId + " trying to grab lock");
      synchronized (this) {
	files.remove(file.id);
	//System.err.println("notifying all");
	this.notifyAll();
      }
      log("finished processing file " + file);
    }
    file.notifyIfDone();
  }

  public void waitUntilAllFilesAreDone() {
    synchronized (this) {
      try {
	while (!files.isEmpty()) {
	  this.wait();
	}
      }
      catch (InterruptedException ie) {
	System.err.println(ie);
	logFailure("waitUntilAllFilesAreDone: thread interrupted " +
		   "(" + ie + ")");
      }
    }
  }

  public void cleanupWhenAllFilesAreDone() {
    waitUntilAllFilesAreDone();
    cleanup();
  }

  /**
   * Cleans up by telling all switchboard users to die, since object
   * processing is complete.
   */
  public void cleanup() {
    if (!files.isEmpty())
      log(className + ": warning: cleaning up while processsing is incomplete");

    //dumpers.interrupt();
    //keepAlives.interrupt();

    boolean now = true;

    synchronized (this) {
      if (verbose)
	if (clients.size() > 0)
	  log("cleaning up clients");

      for (ClientData clientData : clients.values()) {
	SwitchboardUserData userData = (SwitchboardUserData) clientData;
	SwitchboardUser user = userData.switchboardUser();

	if (verbose)
	  log("trying to kill client No. " + userData.id);

	try {
	  user.die(now);
	}
	catch (RemoteException re) {
	}
      }

      if (verbose)
	if (servers.size() > 0)
	  log("cleaning up servers");

      for (ServerData serverData : servers.values()) {
	SwitchboardUserData userData = (SwitchboardUserData) serverData;
	SwitchboardUser user = userData.switchboardUser();

	if (verbose)
	  log("trying to kill server No. " + userData.id);

	try {
	  user.die(now);
	}
	catch (RemoteException re) {
	}
      }
    }
    /*
    System.err.println("No. of clients left in database: " + clients.size());
    System.err.println("No. of servers left in database: " + servers.size());
    */

    try { Naming.unbind(bindingName); }
    catch (MalformedURLException mue) {
      System.err.println(mue);
    }
    catch (RemoteException re) {
      System.err.println("trouble unbinding switchboard (" + re + ")");
    }
    catch (NotBoundException nbe) {
      System.err.println("trouble unbinding switchboard (" + nbe + ")");
    }
    try { UnicastRemoteObject.unexportObject(this, false); }
    catch (NoSuchObjectException nsoe) {
      System.err.println(nsoe);
    }

    System.err.println(className + ": unbound and unexported self");
  }

  /**
   * Returns the verbosity status of the switchboard.
   *
   * @return the verbosity status of the switchboard
   */
  public boolean getVerbose() {
    return verbose;
  }

  /**
   * Sets the verbosity status of the switchboard.
   *
   * @param verbose whether this switchboard should be verbose in its log
   *                messages
   */
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  private ClientData checkValidClient(String errMsg, int clientId)
    throws RemoteException {
    Integer clientIdInteger = new Integer(clientId);
    ClientData clientData = (ClientData)clients.get(clientIdInteger);
    if (clientData == null) {
      if (verbose)
	logFailure(errMsg, clientId);
      throw new UnrecognizedClientException(className +
					    ": invalid client ID: " +
					    clientId);
    }
    return clientData;
  }

  private void log(String msg) {
    synchronized (msgs) {
      msgs.println(msg);
    }
  }
  private void log(String msg, int clientId) {
    synchronized (msgs) {
      msgs.print("client No. ");
      msgs.print(clientId);
      msgs.print(": ");
      msgs.println(msg);
    }
  }
  private void log(String msg, int clientId, int serverId) {
    synchronized (msgs) {
      if (clientId >= 0) {
	msgs.print("client No. ");
	msgs.print(clientId);
	msgs.print(", ");
      }
      msgs.print("server No. ");
      msgs.print(serverId);
      msgs.print(": ");
      msgs.println(msg);
    }
  }
  private void logFailure(String msg) {
    synchronized (msgs) {
      msgs.print("failure: ");
      msgs.println(msg);
    }
  }
  private void logFailure(String msg, int clientId) {
    synchronized (msgs) {
      msgs.print("client No. ");
      msgs.print(clientId);
      msgs.print(": failure: ");
      msgs.println(msg);
    }
  }
  private void logFailure(String msg, int clientId, int serverId) {
    synchronized (msgs) {
      if (clientId >= 0) {
	msgs.print("client No. ");
	msgs.print(clientId);
	msgs.print(", ");
      }
      msgs.print("server No. ");
      msgs.print(serverId);
      msgs.print(": failure: ");
      msgs.println(msg);
    }
  }


  private void logSwitchboardUserDeath(SwitchboardUserData data,
				       boolean natural,
				       int numTriesMade,
				       Exception lastExceptionThrown) {
    synchronized (msgs) {
      msgs.print(data.toString());
      msgs.print(" has died ");
      if (natural) {
	msgs.println("naturally");
      }
      else {
	msgs.println("an unnatural death: made " + numTriesMade + " contact " +
		     "attempts (last exception thrown: " +
		     lastExceptionThrown + ")");
      }
    }
  }

  public int getKeepAliveInterval() throws RemoteException {
    return keepAliveInterval;
  }
  public int getKeepAliveMaxRetries() throws RemoteException {
    return keepAliveMaxRetries;
  }

  /**
   * Returns <code>true</code> if the specified server data object is
   * <code>null</code>, or if the underlying server has a maximum number of
   * clients greater than 0 and is fully loaded.
   *
   * @param server the server to test
   * @return <code>true</code> if the specified server data object is
   *         <code>null</code>, or if the underlying server has a maximum number
   *         of clients greater than 0 and is fully loaded
   */
  private final boolean ineligibleServer(ServerData server) {
    return (server == null ||
	    (server.maxClients > 0 && server.loadFactor() == 1.0));
  }

  @SuppressWarnings({"UnusedDeclaration"})
  private final boolean invalidClient(ClientData client) {
    return client == null;
  }
}
