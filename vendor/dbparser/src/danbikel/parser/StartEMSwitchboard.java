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

import java.net.MalformedURLException;
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;

/**
 * A class for kick-starting a {@link Switchboard} instance when doing the
 * E-step of the EM algorithm (Inside-Outside) in a distributed-computing
 * environment.
 */
public class StartEMSwitchboard {

  private StartEMSwitchboard() {
  }

  // constants
  private static final String outFilenameSuffix = ".parsed";

  // static data (filled in by processArgs)
  private static int portMain = 0;  // anonymous port is default
  private static String[] requiredArgs = new String[1];
  private static String settingsFile = null;
  // currently, the only required arg is inFilenameMain
  private static String inFilenameMain = null;
  private static String outFilenameMain = null;
  private static String logFilenameMain = null;
  private static String bindingName = null;
  private static boolean reProcessMain = Switchboard.defaultReProcess;
  private static String msgFilenameMain = null;


  private static final String[] usageMsg = {
    "usage: [-sf <settings file> | --settings <settings file>]",
    "\t[-p <RMI server port>] [-n <registry name URL>]",
    "\t[-log <log filename>] [-rp] <input file> [-o <output file>]",
    "\t[-msg <messages output file>]",
    "where",
    "\t<settings file> contains the settings to be used by this switchboard",
    "\t\tand all its users (clients and servers)",
    "\t<input file> is the input file to process this run (required)",
    "\t<RMI server port> is the port on which this object accepts RMI calls",
    "\t\t(defaults to a dynamically-chosen anonymous port)",
    "\t<registry name URL> is the RMI URL to specify an rmiregistry",
    "\t\t(defauls to \"" + Switchboard.defaultBindingName + "\")",
    "\t<log filename> is the name of the log file of incremental processing",
    "\t\t(defaults to \"<input file>" + Switchboard.logFilenameSuffix + "\")",
    "\t-rp specifies to re-process sentences that were un-processed,",
    "\t\twhen recovering from a previous run",
    "\t<output file> is the output file of processed sentences",
    "\t\t(defaults to \"<input file>" + outFilenameSuffix + "\")",
    "\t<messages output file> is the output file for switchboard messages",
    "\t\t(defaults to \"directory of <output file>\" + \"" +
    Switchboard.defaultMessagesFilename + "\")",
  };

  private final static void usage() {
    for (int i = 0; i < usageMsg.length; i++)
      System.err.println(usageMsg[i]);
    System.exit(-1);
  }

  private final static void processArgs(String[] args) {
    int currRequiredArgIdx = 0;
    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("-")) {
	// process flag
	if (args[i].equals("-sf") || args[i].equals("--settings")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: no argument present after " + args[i]);
	    usage();
	  } else
	    settingsFile = args[++i];
	} else if (args[i].equals("-p")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: no argument present after -p");
	    usage();
	  } else
	    portMain = Integer.parseInt(args[++i]);
	} else if (args[i].equals("-n")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: no argument present after -n");
	    usage();
	  } else
	    bindingName = args[++i];
	} else if (args[i].equals("-log")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: no argument present after -log");
	    usage();
	  } else
	    logFilenameMain = args[++i];
	} else if (args[i].equals("-o")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: no argument present after -o");
	    usage();
	  } else
	    outFilenameMain = args[++i];
	} else if (args[i].equals("-msg")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: no argument present after -msg");
	    usage();
	  } else
	    msgFilenameMain = args[++i];
	} else if (args[i].equals("-rp"))
	  reProcessMain = true;
      } else if (currRequiredArgIdx < requiredArgs.length) {
	requiredArgs[currRequiredArgIdx++] = args[i];
      } else {
	System.err.println("error: unexpected argument: " + args[i]);
	usage();
      }
    }

    if (currRequiredArgIdx < requiredArgs.length) {
      System.err.println("error: only " + currRequiredArgIdx + " of " +
			 requiredArgs.length + " required arguments present");
      usage();
    }

    // set required args (there is currently on one)
    inFilenameMain = requiredArgs[0];

    // set all other args that have defaults, as necessary
    if (outFilenameMain == null)
      outFilenameMain = inFilenameMain + outFilenameSuffix;
    if (logFilenameMain == null)
      logFilenameMain = outFilenameMain + Switchboard.logFilenameSuffix;
    if (bindingName == null)
      bindingName = Switchboard.defaultBindingName;
    if (msgFilenameMain == null) {
      File outFile = new File(outFilenameMain);
      String outFileParent = outFile.getParent();
      if (outFileParent == null)
	outFileParent = "";
      else
	outFileParent += File.separator;
      msgFilenameMain = outFileParent + Switchboard.defaultMessagesFilename;
    }
  }

  /**
   * Kick-starts a <code>Switchboard</code> instance, using <code>Sexp</code>
   * object reader factories.
   *
   * @see SexpObjectReaderFactory
   * @see SexpNumberedObjectReaderFactory
   */
  public static void main(String[] args) {
    // uncomment the following for debugging under JBuilder
    /*
    String codebase = "file:///local/home/dbikel/jbproject/dbparser/classes/";
    System.setProperty("java.rmi.server.codebase", codebase);
    String policy =
      "/local/home/dbikel/jbproject/dbparser/policy-files/switchboard.policy";
    System.setProperty("java.security.policy", policy);
    */
    processArgs(args);
    Switchboard.setPolicyFile(Settings.getSettings());
    //Create and install a security manager
    if (System.getSecurityManager() == null)
      System.setSecurityManager(new RMISecurityManager());
    try {
      ObjectReaderFactory orf =
	new SexpObjectReaderFactory();
      /*
      ObjectReaderFactory norf =
	Switchboard.getDefaultNumberedObjectReaderFactory();
      ObjectWriterFactory owf =
	new EventCountsWriterFactory();
      ObjectWriterFactory nowf =
	Switchboard.getDefaultNumberedObjectWriterFactory();
      */

      if (settingsFile != null)
	Settings.load(settingsFile);


      Switchboard switchboard = new Switchboard(msgFilenameMain,
						portMain,
						reProcessMain,
						orf, null,
						null, null,
						bindingName);

      // events are too large to be sorted in memory, as is done by the current
      // implementation, so we force sorting to be off; future implementations
      // of Switchboard may do file-based sorting, somehow
      Settings.set(SwitchboardRemote.sortOutput, "false");

      switchboard.bind(Settings.getSettings(), Language.encoding());
      //switchboard.processFile(inFilenameMain, outFilenameMain, logFilenameMain);
      EventCountsConsumer consumer = new EventCountsConsumer(true, true);
      consumer.useCountThreshold();
      switchboard.registerConsumer(consumer);
      // we don't pass in the name of the log file, even if one is specified!!!
      switchboard.processFile(inFilenameMain, outFilenameMain, null);
      switchboard.cleanup();
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
