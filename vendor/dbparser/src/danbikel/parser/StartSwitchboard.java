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

import danbikel.switchboard.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

/**
 * A class to kick-start a {@link Switchboard} instance for parsing in a
 * distributed-computing environment.
 */
public class StartSwitchboard {

  private StartSwitchboard() {}

  // constants
  private static final String outFilenameSuffix = ".parsed";

  // static data (filled in by processArgs)
  private static int portMain = 0;  // anonymous port is default
  private static String[] requiredArgs = new String[0];
  private static String settingsFile = null;
  // currently, the only required arg is inFilenameMain
  // THIS HAS BEEN CHANGED
  private static int numFiles = 0;
  private static String[] inFilenameMain = null;
  private static String[] outFilenameMain = null;
  private static String[] logFilenameMain = null;
  private static String bindingName = null;
  private static boolean reProcessMain = Switchboard.defaultReProcess;
  private static String msgFilenameMain = null;


  private static final String[] usageMsg = {
    "usage: [-v|-help|-usage]",
    "\t[-sf <settings file> | --settings <settings file>]",
    "\t[-p <RMI server port>] [-n <registry name URL>]",
    "\t[ [-log <log filename>+] | [-logdir <log dir>] ]",
    "\t[<input file or dir>+]",
    "\t[ [-o <output file>+] | [-odir <output file dir>] ]",
    "\t[-msg <messages output file>] [-rp] [--]",
    "where",
    "\t-v,-help,-usage: prints this message",
    "\t<settings file> contains the settings to be used by this switchboard",
    "\t\tand all its users (clients and servers)",
    "\t<input file or dir>+ is the list of input files to process this run",
    "\t\t(a directory in the list indicates to non-recursively process",
    "\t\tall files in that directory)",
    "\t<RMI server port> is the port on which this object accepts RMI calls",
    "\t\t(defaults to a dynamically-chosen anonymous port)",
    "\t<registry name URL> is the RMI URL to specify an rmiregistry",
    "\t\t(defauls to \"" + Switchboard.defaultBindingName + "\")",
    "\t<log filename>+ is the list of log files of incremental processing",
    "\t\t(this list is coordinated with the list of input files;",
    "\t\tthe ith input file's log file defaults to",
    "\t\t\"<input file>_i" + Switchboard.logFilenameSuffix + "\")",
    "\t-rp specifies to re-process sentences that were un-processed,",
    "\t\twhen recovering from a previous run",
    "\t<output file>+ is the list of output file names of processed sentences",
    "\t\t(this list is coordinated with the list of input files;",
    "\t\tthe ith input file's output file defaults to",
    "\t\t\"<input file>_i" + outFilenameSuffix + "\")",
    "\t<messages output file> is the output file for switchboard messages",
    "\t\t(defaults to",
    "\t\t\"directory of first <output file>\" + \"" +
       Switchboard.defaultMessagesFilename + "\")",
    "\t-- ends a list of items (such as log files)"
  };

  /**
   * Removes any and all directory components of the specified filename.
   * @param filename the filename from which to get the tail
   * @return the tail of the specified filename
   */
  protected final static String getTail(String filename) {
    int lastSepIdx = filename.lastIndexOf(File.separator) + 1;
    return filename.substring(lastSepIdx);
  }

  private final static void usage() {
    for (int i = 0; i < usageMsg.length; i++)
      System.err.println(usageMsg[i]);
    System.exit(-1);
  }

  private final static void processArgs(String[] args) {

    ArrayList inFilenames = new ArrayList();
    ArrayList outFilenames = new ArrayList();
    ArrayList logFilenames = new ArrayList();

    boolean useLogDir = false;
    boolean useOutDir = false;

    int currRequiredArgIdx = 0;
    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("-")) {
	// process flag
	if (args[i].equals("-v") || args[i].equals("-help") ||
	    args[i].equals("-usage")) {
	  usage();
	}
	else if (args[i].equals("-sf") || args[i].equals("--settings")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: no argument present after " + args[i]);
	    usage();
	  }
	  else
	    settingsFile = args[++i];
	}
	else if (args[i].equals("-p")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: no argument present after -p");
	    usage();
	  }
	  else
	    portMain = Integer.parseInt(args[++i]);
	}
	else if (args[i].equals("-n")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: no argument present after -n");
	    usage();
	  }
	  else
	    bindingName = args[++i];
	}
	else if (args[i].equals("-log")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: no argument present after -log");
	    usage();
	  }
	  else {
	    ++i;
	    while (i < args.length && !args[i].startsWith("-")) {
	      logFilenames.add(args[i++]);
	    }
	    --i;
	  }
	}
	else if (args[i].equals("-logdir")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: no argument present after -logdir");
	    usage();
	  }
	  else if (useLogDir) {
	    System.err.println("error: cannot specify multiple log directories");
	    usage();
	  }
	  else if (logFilenames.size() > 0) {
	    System.err.println("error: cannot use -logdir with -log");
	    usage();
	  }
	  else {
	    useLogDir = true;
	    logFilenames.add(args[++i]);
	  }
	}
	else if (args[i].equals("-o")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: no argument present after -o");
	    usage();
	  }
	  else if (useOutDir) {
	    System.err.println("error: cannot use -o with -odir");
	    usage();
	  }
	  else {
	    ++i;
	    while (i < args.length && !args[i].startsWith("-")) {
	      outFilenames.add(args[i++]);
	    }
	    --i;
	  }
	}
	else if (args[i].equals("-odir")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: no argument present after -odir");
	    usage();
	  }
	  else if (useOutDir) {
	    System.err.println("error: cannot specify multiple output directories");
	    usage();
	  }
	  else if (outFilenames.size() > 0) {
	    System.err.println("error: cannot use -odir with -o");
	    usage();
	  }
	  else {
	    useOutDir = true;
	    outFilenames.add(args[++i]);
	  }
	}
	else if (args[i].equals("-msg")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: no argument present after -msg");
	    usage();
	  }
	  else
	    msgFilenameMain = args[++i];
	}
	else if (args[i].equals("-rp"))
	  reProcessMain = true;
	else if (args[i].equals("--"))
	  ;
      }
      else {
	inFilenames.add(args[i]);
      }
      /*
      else if (currRequiredArgIdx < requiredArgs.length) {
	requiredArgs[currRequiredArgIdx++] = args[i];
      }
      else {
	System.err.println("error: unexpected argument: " + args[i]);
	usage();
      }
      */
    }

    if (currRequiredArgIdx < requiredArgs.length) {
      System.err.println("error: only " + currRequiredArgIdx + " of " +
			 requiredArgs.length + " required arguments present");
      usage();
    }

    /*
    // set required args (there is currently on one)
    inFilenameMain = requiredArgs[0];
    */

    // go through inFilenames, replacing any file of files (.fof) with
    // the list of files it contains
    for (int i = 0; i < inFilenames.size(); i++) {
      File curr = new File((String)inFilenames.get(i));
      if (curr.toString().endsWith(".fof")) {
	inFilenames.remove(i);
	System.err.println("grabbing all files from \"" + curr + "\"");
	List<File> filesInFof = readFof(curr);
	for (File fileInFof : filesInFof) {
	  if (fileInFof.isFile()) {
	    inFilenames.add(i++, fileInFof.getAbsolutePath());
	  }
	}
      }
    }
    // go through inFilenames, replacing any directories with list of files
    // within those directories
    for (int i = 0; i < inFilenames.size(); i++) {
      File curr = new File((String)inFilenames.get(i));
      if (curr.exists() && curr.isDirectory()) {
	inFilenames.remove(i);
	System.err.println("grabbing all files in directory " + curr);
	File[] filesInCurr = curr.listFiles();
	for (int fileIdx = 0; fileIdx < filesInCurr.length; fileIdx++)
          if (filesInCurr[fileIdx].isFile())
            inFilenames.add(i++, filesInCurr[fileIdx].getAbsolutePath());
      }
    }

    numFiles = inFilenames.size();

    // set all other args that have defaults, as necessary

    if (!useOutDir && outFilenames.size() > 0 &&
	outFilenames.size() > numFiles) {
      System.err.println("warning: more output file names specified than " +
			 "input file names; only using first " + numFiles);
      // trim outFilenames list
      for (int i = numFiles; i < outFilenames.size(); i++)
	outFilenames.remove(i);
    }
    if (!useLogDir && logFilenames.size() > 0 &&
	logFilenames.size() > numFiles) {
      System.err.println("warning: more log file names specified than " +
			 "input file names; only using first " + numFiles);
      // trim logFilenames list
      for (int i = numFiles; i < logFilenames.size(); i++)
	logFilenames.remove(i);
    }
    if (numFiles > 0) {
      // first, create three equi-sized arrays
      inFilenameMain = new String[numFiles];
      outFilenameMain = new String[numFiles];
      logFilenameMain = new String[numFiles];

      // fill arrays
      inFilenames.toArray(inFilenameMain);
      if (!useOutDir)
	outFilenames.toArray(outFilenameMain);
      if (!useLogDir)
	logFilenames.toArray(logFilenameMain);

      // next, go through and add default items to arrays if need be
      for (int i = 0; i < numFiles; i++) {
	if (!useOutDir && outFilenameMain[i] == null) {
	  outFilenameMain[i] = inFilenameMain[i] + outFilenameSuffix;
	}
	if (!useLogDir && logFilenameMain[i] == null) {
	  logFilenameMain[i] =
	    outFilenameMain[i] + Switchboard.logFilenameSuffix;
	}
      }

      if (useOutDir) {
	String outDir = (String)outFilenames.get(0);
	for (int i = 0; i < numFiles; i++) {
	  outFilenameMain[i] =
	    outDir + File.separator + getTail(inFilenameMain[i]) +
	    outFilenameSuffix;
	}
      }
      if (useLogDir) {
	String logDir = (String)logFilenames.get(0);
	for (int i = 0; i < numFiles; i++) {
	  logFilenameMain[i] =
	    logDir + File.separator + getTail(outFilenameMain[i]) +
	    Switchboard.logFilenameSuffix;
	}
      }
    }
    if (bindingName == null)
      bindingName = Switchboard.defaultBindingName;

    // if user did not specify a messages file, put default-named messages
    // file in parent of outFilenameMain if it exists, or else
    // in the current working directory, as determined by
    // System.getProperty("user.dir")
    if (msgFilenameMain == null) {
      String msgFileDirname = null;
      if (outFilenameMain != null) {
	File outFile = new File(outFilenameMain[0]);
	String outFileParent = outFile.getParent();
	if (outFileParent == null) {
	  outFileParent = "";
	}
	else {
	  outFileParent += File.separator;
	}
	msgFileDirname = outFileParent;
      }
      if (msgFileDirname == null)
	msgFileDirname = System.getProperty("user.dir") + File.separator;
      msgFilenameMain = msgFileDirname + Switchboard.defaultMessagesFilename;
    }
  }

  private static List<File> readFof(File fof) {
    List<File> filesInFof = new ArrayList<File>();
    try {
      BufferedReader br = new BufferedReader(new FileReader(fof));
      String line;
      while ((line = br.readLine()) != null) {
	filesInFof.add(new File(line));
      }
    } catch (Exception e) {
      System.err.println("Error reading \"" + fof + "\".  Skipping.");
    }
    return filesInFof;
  }

  /**
   * Kick-starts a <code>Switchboard</code> instance, using
   * <code>Sexp</code> object reader factories.
   *
   * @see SexpObjectReaderFactory
   * @see SexpNumberedObjectReaderFactory
   */
  public static void main(String[] args) {
    processArgs(args);
    Switchboard.setPolicyFile(Settings.getSettings());
    //Create and install a security manager
    if (System.getSecurityManager() == null)
      System.setSecurityManager(new RMISecurityManager());
    try {
      ObjectReaderFactory orf =
	new SexpObjectReaderFactory();
      ObjectReaderFactory norf =
	new SexpNumberedObjectReaderFactory();
      // we pass the same object writer factory for writing both
      // numbered and un-numbered sentences, which is the text-based
      // one that the danbikel.switchboard package provides for us
      ObjectWriterFactory owf =
	new TextObjectWriterFactory();

      if (settingsFile != null)
	Settings.load(settingsFile);

      Switchboard switchboard = new Switchboard(msgFilenameMain,
						portMain,
						reProcessMain,
						orf, norf,
						owf, owf,
						bindingName);

      switchboard.bind(Settings.getSettings(), Language.encoding());
      if (inFilenameMain != null) {
	for (int i = 0; i < numFiles; i++) {
	  switchboard.processFile(inFilenameMain[i], outFilenameMain[i],
				  logFilenameMain[i], false);
	}
	switchboard.cleanupWhenAllFilesAreDone();
      }
      else {
	System.err.print("Waiting for 300 seconds...");
	try {
	  Thread.sleep(300000);
	} catch (InterruptedException e) {
	  e.printStackTrace();
	}
	System.err.println("done.\nCleaning up.");
	switchboard.cleanup();
      }
    }
    catch (RemoteException re) {
      System.err.println(re);
    }
    catch (MalformedURLException mue) {
      System.err.println(mue);
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
  }
}
