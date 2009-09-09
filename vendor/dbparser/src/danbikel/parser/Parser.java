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
import danbikel.switchboard.*;
import danbikel.parser.constraints.*;
import danbikel.parser.util.*;
import java.util.*;
import java.net.*;
import java.security.*;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;
import java.lang.reflect.*;

/**
 * A parsing client.  This class parses sentences by implementing the
 * {@link AbstractClient#process(Object)} method of its {@link
 * AbstractClient superclass}.  All top-level probabilities are
 * computed by a <code>DecoderServerRemote</code> object, which is either local
 * or is a stub whose methods are invoked via RMI.  The actual
 * parsing is implemented in the <code>Decoder</code> class.
 *
 * @see AbstractClient
 * @see DecoderServerRemote
 * @see DecoderServer
 * @see Decoder
 */
public class Parser
  extends AbstractClient implements ParserRemote, Runnable, Settings.Change {

  // private constants
  private final static boolean debug = false;
  private final static boolean debugCacheStats = true;
  private final static String className = Parser.class.getName();
  private final static boolean flushAfterEverySentence = true;
  private static String decoderClassName =
    Settings.get(Settings.decoderClass);
  private static String decoderServerClassName =
    Settings.get(Settings.decoderServerClass);

  static {
    Settings.Change change =
      new Settings.Change() {
	public void update(Map<String, String> changedSettings) {
	  decoderClassName = Settings.get(Settings.decoderClass);
	  decoderServerClassName = Settings.get(Settings.decoderServerClass);
	}
      };
    Settings.register(Parser.class, change, null);
  }

  protected static String invocationTargetExceptionMsg =
    "ERROR: IT IS LIKELY THAT YOU HAVE ATTEMPTED TO LOAD AN OLD VERSION OF\n" +
    "\tA SERIALIZED JAVA OBJECT; PLEASE RE-TRAIN TO PRODUCE A NEW\n" +
    "\tDERIVED DATA FILE";
  
  /**
   * An array of types containing a single element,
   * <code>String.class</code>. Used for fetching constructors of classes
   * that take a single argument of type <code>String</code>.
   *
   * @see #getNewParser(String)
   * @see #getNewDecoderServer(String)
   */
  protected final static Class[] stringTypeArr = {String.class};
  /**
   * An array of types containing a single element,
   * <code>Integer.TYPE</code>. Used when fetching the approrpriate constructor
   * of this class in the static &ldquo;named constructor&rdquo; {@link
   * #getNewParser(int)}.
   */
  protected final static Class[] intTypeArr = {Integer.TYPE};
  /**
   * An array of types used for fetching the constructor of {@link Decoder}
   * that takes two arguments of type <code>int</code> and of type
   * <code>DecoderServerRemote</code>.
   *
   * @see #getNewDecoder(int,DecoderServerRemote)
   */
  protected final static Class[] newDecoderTypeArr =
    {Integer.TYPE,DecoderServerRemote.class};

  // protected constants
  /** Cached value of {@link Settings#keepAllWords}, for efficiency and
      convenience. */
  protected boolean keepAllWords = Settings.getBoolean(Settings.keepAllWords);

  // public constants
  /**
   * The suffix to attach to input files by default when creating their
   * associated output files.
   */
  public final static String outputFilenameSuffix = ".parsed";

  // protected static data
  /**
   * The subclass of <code>Parser</code> to be constructed by
   * the {@link #main(String[])} method of this class.
   */
  protected static Class parserClass = Parser.class;

  // data members
  /** The server for the internal {@link Decoder} to use when parsing. */
  protected DecoderServerRemote server;
  /** The current sentence being processed. */
  protected SexpList sent;
  /** The internal {@link Decoder} that performs the actual parsing. */
  protected Decoder decoder;
  /**
   * Indicates whether the {@link DecoderServerRemote} instance is local
   * or remote (an RMI stub).
   */
  protected boolean localServer = true;
  // the next two data members are only used when not in stand-alone mode
  // (i.e., when using the switchboard), and when the user has specified
  // an input filename and, optionally, an output filename
  /**
   * The name of the input file to be processed (only used when this parser is
   * in stand-alone mode, not using the
   * {@link danbikel.switchboard.Switchboard}.
   */
  protected String internalInputFilename = null;
  /**
   * The name of the output file to be processed (only used when this parser is
   * in stand-alone mode, not using the
   * {@link danbikel.switchboard.Switchboard}.
   */
  protected String internalOutputFilename = null;

  /**
   * A {@link PrintWriter} object wrapped around {@link System#err} for
   * printing in the proper character encoding.
   */
  protected PrintWriter err;

  public void update(Map<String, String> changedSettings) {
    if (changedSettings.containsKey(Settings.decoderClass)) {
      decoder = getNewDecoder(id, server);
    }
  }

  /**
   * Constructs a new {@link Parser} instance that will construct an internal
   * {@link DecoderServer} for its {@link Decoder} to use when parsing.
   *
   * @param derivedDataFilename the name of the derived data file to pass to the
   *                            constructor of the {@link DecoderServer} class
   *                            when creating an internal instance
   * @throws RemoteException
   * @throws ClassNotFoundException if {@link #getNewDecoderServer(String)}
   *                                throws this exception
   * @throws NoSuchMethodException  if {@link #getNewDecoderServer(String)}
   *                                throws this excception
   * @throws java.lang.reflect.InvocationTargetException
   *                                if {@link #getNewDecoderServer(String)}
   *                                throws this excception
   * @throws IllegalAccessException if {@link #getNewDecoderServer(String)}
   *                                throws this excception
   * @throws InstantiationException if {@link #getNewDecoderServer(String)}
   *                                throws this excception
   */
  public Parser(String derivedDataFilename)
    throws RemoteException, ClassNotFoundException,
	   NoSuchMethodException, java.lang.reflect.InvocationTargetException,
	   IllegalAccessException, InstantiationException {
    server = getNewDecoderServer(derivedDataFilename);
    decoder = getNewDecoder(0, server);
    setUpErrWriter();
    Settings.register(this);
  }

  /**
   * Constructs a new parsing client where the internal {@link Decoder} will
   * use the specified server.
   * @param server the server for the {@link Decoder} to use
   * @throws RemoteException
   */
  public Parser(DecoderServerRemote server) throws RemoteException {
    this.server = server;
    decoder = getNewDecoder(0, server);
    setUpErrWriter();
    Settings.register(this);
  }

  /**
   * Constructs a new parsing client with the specified timeout value for its
   * sockets (not needed with recent RMI implementations from Sun).
   * @param timeout the timeout value for RMI client and server sockets
   * @throws RemoteException
   */
  public Parser(int timeout) throws RemoteException {
    super(timeout);
    setUpErrWriter();
    Settings.register(this);
  }
  /**
   * Constructs a new parsing client with the specified timeout value for its
   * sockets (not needed with recent RMI implementations from Sun) and with
   * the specified listening port for receiving remote method invocations.
   *
   * @param timeout the timeout value for RMI client and server sockets
   * @param port the port for this RMI server
   * @throws RemoteException
   */
  public Parser(int timeout, int port) throws RemoteException {
    super(timeout, port);
    setUpErrWriter();
    Settings.register(this);
  }
  /**
   * Constructs a new parsing client with the specified RMI port and
   * client and server socket factories.
   *
   * @param port the port on which to receive remote method invocations
   * @param csf the client socket factory for this RMI client
   * @param ssf the server socket factory for this RMI server
   * @throws RemoteException
   */
  public Parser(int port,
		RMIClientSocketFactory csf, RMIServerSocketFactory ssf)
    throws RemoteException {
    super(port, csf, ssf);
    setUpErrWriter();
    Settings.register(this);
  }

  private void setUpErrWriter() {
    OutputStreamWriter errosw = null;
    try {
      errosw = new OutputStreamWriter(System.err, Language.encoding());
    }
    catch (UnsupportedEncodingException uee) {
      System.err.println(className + ": error: couldn't create err output " +
			 "stream using encoding " + Language.encoding() +
			 "(reason: " + uee + ")");
      System.err.println("\tusing default encoding instead");
      errosw = new OutputStreamWriter(System.err);
    }
    err = new PrintWriter(errosw, true);
  }

  /*
  protected Decoder getNewDecoder(int id, DecoderServerRemote server) {
    return new Decoder(id, server);
  }
  */
  protected Decoder getNewDecoder(int id, DecoderServerRemote server) {
    Decoder decoder = null;
    try {
      Class decoderClass = Class.forName(decoderClassName);
      
      Constructor cons = decoderClass.getConstructor(newDecoderTypeArr);
      Object[] argArr = new Object[]{new Integer(id), server};
      decoder = (Decoder)cons.newInstance(argArr);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    return decoder;
  }

  // helper method for the "stand-alone" constructor
  /**
   * Gets a new decoder server for when creating a stand-alone parsing client
   * (i.e., a parsing client that creates its own {@link DecoderServerRemote}
   * instance).
   *
   * @param derivedDataFilename the name of the derived data file to pass to the
   *                            constructor of {@link DecoderServer} that takes
   *                            this name as an argument
   * @return a new decoder server for when creating a stand-alone parsing client
   *
   * @throws ClassNotFoundException if the class specified by {@link
   *                                Settings#decoderServerClass} cannot be
   *                                found
   * @throws NoSuchMethodException  if the class specified by {@link
   *                                Settings#decoderServerClass} has no
   *                                constructor that takes a single {@link
   *                                String}
   * @throws java.lang.reflect.InvocationTargetException
   *                                if the constructor of the class specified by
   *                                {@link Settings#decoderServerClass} (the
   *                                invocation target) throws an exception
   *                                (which will be wrapped in this type of
   *                                exception)
   * @throws IllegalAccessException if this class is not allowed to access the
   *                                single-string constructor of the class
   *                                specified by {@link Settings#decoderServerClass}
   * @throws InstantiationException if there's a problem instantiating a new
   *                                instance of the class specified by {@link
   *                                Settings#decoderServerClass}
   */
  protected static DecoderServer getNewDecoderServer(String derivedDataFilename)
    throws ClassNotFoundException,
	   NoSuchMethodException, java.lang.reflect.InvocationTargetException,
	   IllegalAccessException, InstantiationException {

    Class decoderServerClass = Class.forName(decoderServerClassName);

    DecoderServer server = null;

    Constructor cons = decoderServerClass.getConstructor(stringTypeArr);
    Object[] argArr = new Object[]{derivedDataFilename};
    server = (DecoderServer)cons.newInstance(argArr);

    return server;
  }

  /**
   * Unless it is time to die, this method continually tries the switchboard
   * until it can assign this client a server.
   *
   * @throws RemoteException
   */
  protected void getServer() throws RemoteException {
    // the following check is necessary, as this method will be called
    // by reRegister, which is called when Switchboard failure is
    // detected by AbstractClient.processObjects
    if (localServer)
      return;

    super.getServer();
    server = (DecoderServerRemote)super.server;
  }

  protected void tolerateFaults(int retries,
				int sleepTime,
				boolean failover) {
    // the following check is necessary, as this method will be called
    // by reRegister, which is called when Switchboard failure is
    // detected by AbstractClient.processObjects
    if (localServer)
      return;

    super.tolerateFaults(retries, sleepTime, failover);
    server = (DecoderServerRemote)super.server;
  }


  /**
   * We override {@link AbstractClient#cleanup()} here so that it
   * sets the internal {@link #server} data member to <code>null</code>
   * only when not debugging cache stats (which is provided as an internal,
   * compile-time option via a private data member).  The default behavior
   * of this method, as defined in the superclass' implementation, is simply
   * to set the server data member to be <code>null</code>.
   */
  /*
  protected void cleanup() {
    if (debugCacheStats == false)
      server = null;
  }
  */

  private SexpList test(SexpList sent) throws RemoteException {
    double prob = server.testProb();
    if (debug)
      System.err.println(className + ": server returned prob. of " + prob);
    return sent;
  }

  /**
   * Parses the specified sentence, which can be in one of three formats.
   * <ul>
   * <li>Format 1 is where each word is supplied with a list of possible
   * part-of-speech tags, and is described in detail in the documentation for
   * the method {@link #sentContainsWordsAndTags(SexpList)}.
   * <li>Format 2 is where the list contains only symbols that are the words of
   * the sentence to be parsed.
   * <li>Format 3 is where the specified list is in the format of a valid parse
   * tree.  In this case, the tree will be used to get parsing constraints.
   * Typically, then, the parse trees passed are underspecified.  For example,
   * the tree<br>
   * <tt>(S (JJ Bad) (NNP John) (VP (VBD sat) (RB quietly)))</tt><br>
   * encodes the constraint that an <tt>S</tt> spans the entire sentence
   * and a <tt>VP</tt> spans the last two words, but has nothing else to say
   * about any constituents that may cover the first two words. The type of
   * constraints used is determined by the
   * {@link Settings#constraintSetFactoryClass} setting.
   * </ul>
   *
   * @param sent the sentence to be parsed
   * @return the parsed version of the specified sentence, or <code>null</code>
   *         if no parse could be produced using the current model
   *
   * @throws RemoteException
   */
  public Sexp parse(SexpList sent) throws RemoteException {
    if (sentContainsWordsAndTags(sent))
      return decoder.parse(getWords(sent), getTagLists(sent));
    else if (sent.isAllSymbols())
      return decoder.parse(sent);
    else if (Language.training.isValidTree(sent)) {
      return decoder.parse(getWordsFromTree(sent),
			   getTagListsFromTree(sent),
			   getConstraintsFromTree(sent));
    }
    else {
      err.println(className + ": error: sentence \"" + sent +
			 "\" has a bad format:\n\tmust either be all symbols " +
			 "or a list of lists of the form (<word> (<tag>*))");
      return null;
    }
  }

  /**
   * Converts certain words (leaves) in the specified tree to their associated
   * word-feature vectors.  If the boolean value of {@link
   * Settings#keepAllWords} is <code>true</code>, then only words that were
   * never observed in training are converted; otherwise, only words that were
   * observed less than the {@linkplain Settings#unknownWordThreshold the
   * unknown word threshold} are converted.  This method intentionally performs
   * the same conversion of words as would be performed by {@link
   * DecoderServerRemote#convertUnknownWords(danbikel.lisp.SexpList)}. In fact,
   * it uses
   * {@link DecoderServerRemote#convertUnknownWord(danbikel.lisp.Symbol,int)}
   * as a helper method.
   *
   * @param tree        the tree whose low-frequency or unobserved words are to
   *                    be converted to feature vectors
   * @param currWordIdx the threaded word index of the sentence
   * @return the specified tree, having been modified in-place
   */
  protected Sexp convertUnknownWords(Sexp tree, IntCounter currWordIdx) {
    if (Language.treebank().isPreterminal(tree)) {
      Word wordObj = Language.treebank().makeWord(tree);

      // change word to unknown, if necessary
      Sexp wordInfo = null;
      try {
	wordInfo = server.convertUnknownWord(wordObj.word(), currWordIdx.get());
      }
      catch (RemoteException re) {
	System.err.println(re);
      }
      Symbol word = null;
      boolean wordIsUnknown = wordInfo.isList();
      if (wordIsUnknown) {
	SexpList wordInfoList = wordInfo.list();
	Symbol features = wordInfoList.symbolAt(1);
	boolean neverObserved = wordInfoList.symbolAt(2) == Constants.trueSym;

	if (keepAllWords) {
	  word = neverObserved ? features : wordInfoList.symbolAt(0);
	}
	else {
	  word = features;
	}

	wordObj.setWord(word);
      }

      tree = Language.treebank().constructPreterminal(wordObj);

      currWordIdx.increment();
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++) {
	treeList.set(i, convertUnknownWords(treeList.get(i), currWordIdx));
      }
    }

    return tree;
  }

  /**
   * After {@linkplain #convertUnknownWords(Sexp,IntCounter) converting
   * unknown words} in the specified parse tree, this method constructs
   * a constraint set using the method {@link ConstraintSets#get(Object)}.
   *
   * @param tree the tree from which to construct a set of parsing constraints
   * @return a set of parsing constraints for use when parsing the specified
   * tree
   */
  protected ConstraintSet getConstraintsFromTree(Sexp tree) {
    // since we're about to destructively modify tree, let's deeply copy it
    tree = tree.deepCopy();
    convertUnknownWords(tree, new IntCounter(0));
    return ConstraintSets.get(tree);
  }

  /**
   * A method to determine if the sentence to be parsed is in the format
   * where part-of-speech tags are supplied along with the words.  The
   * format is defined by the following BNF specification:
   * <p/>
   * <table>
   * <tr><td valign=top>(&nbsp;&lt;wordTagList&gt;*&nbsp;)
   *                    where</td></tr>
   * <tr><td valign=top>&lt;wordTagList&gt;</td><td>::=</td>
   *     <td valign=top>(&nbsp;&lt;word&gt;&nbsp;&lt;tagList&gt;&nbsp;)</td>
   * </tr>
   * <tr><td valign=top>&lt;word&gt;</td><td>::=</td>
   *     <td valign=top>a symbol representing a word in the sentence to be
   *                    parsed</td>
   * </tr>
   * <tr><td valign=top>&lt;tagList&gt;</td><td valign=top>::=</td>
   *     <td valign=top>(&nbsp;&lt;tag&gt;+&nbsp;)</td>
   * </tr>
   * <tr><td valign=top>&lt;tag&gt;</td><td valign=top>::=</td>
   *     <td valign=top>a symbol that represents a possible part-of-speech tag
   *                    for the word with which it is associated</td>
   * </tr>
   * </table>
   * <p/>
   * An example of this format is
   * <pre>
   * ((John (NNP)) (sat (VBD VB)) (. (.)))
   * </pre>
   * Typically, only a single tag is supplied with each word.
   * @param sent the sentence to be tested
   * @return whether the specified list is in the format where a list of tags
   * is supplied with each word of the sentence to be parsed
   */
  protected boolean sentContainsWordsAndTags(SexpList sent) {
    int size = sent.size();
    for (int i = 0; i < size; i++) {
      if (!wordTagList(sent.get(i)))
	return false;
    }
    return true;
  }

  /**
   * Returns whether the specified S-expression is a list of length two
   * whose first element is a symbol and whose second element is a list
   * of one or more symbols.
   *
   * @param sexp the S-expression to be tested
   * @return whether the specified S-expression is a list of length two
   * whose first element is a symbol and whose second element is a list
   * of one or more symbols
   */
  protected boolean wordTagList(Sexp sexp) {
    if (sexp.isSymbol())
      return false;
    SexpList list = sexp.list();
    // this is a word-tag list is the first element is a symbol (the word)
    // and the second element is a list containing all symbols (the list
    // of possible tags)
    return (list.size() == 2 && list.get(0).isSymbol() &&
	    list.get(1).isList() && list.get(1).list().isAllSymbols());
  }

  /**
   * Returns a new list containing only the words of the sentence to be
   * parsed when the sentence is in the format described in the comment
   * for the {@link #sentContainsWordsAndTags(SexpList)} method.
   *
   * @param sent the sentence whose words are to be extracted into a new list
   * @return a new list containing only the words of the sentence to be
   * parsed when the sentence is in the format described in the comment
   * for the {@link #sentContainsWordsAndTags(SexpList)} method
   */
  protected SexpList getWords(SexpList sent) {
    int size = sent.size();
    SexpList wordList = new SexpList(size);
    for (int i = 0; i < size; i++)
      wordList.add(sent.get(i).list().get(0));
    return wordList;
  }

  /**
   * Returns a new list containing the word symbols from the specified tree.
   * @param tree the tree from which to extract a list of its words
   * @return a new list containing the word symbols from the specified tree
   */
  protected SexpList getWordsFromTree(Sexp tree) {
    return getWordsFromTree(new SexpList(), tree);
  }

  /**
   * Gets the words of the sentence to be parsed from the specified parse
   * tree.
   * @param wordList the list to which the words of the specified tree
   * are to be added
   * @param tree the tree from which to extract word symbols
   * @return the specified <code>wordList</code> object, having been modified
   * so that each word symbol from the specified tree was added to its end
   */
  protected SexpList getWordsFromTree(SexpList wordList, Sexp tree) {
    if (Language.treebank.isPreterminal(tree)) {
      Word word = Language.treebank.makeWord(tree);
      wordList.add(word.word());
    }
    else {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++)
	getWordsFromTree(wordList, treeList.get(i));
    }
    return wordList;
  }

  /**
   * Collects a list of symbols that are the part-of-speech tags (preterminals)
   * of the specified tree.
   *
   * @param tree the tree from which to extract a list of part-of-speech tags
   * @return a new list of symbols that are the part-of-speech tags
   *         (preterminals) of the specified tree.
   */
  protected SexpList getTagListsFromTree(Sexp tree) {
    Sexp taggedSentence = Util.collectTaggedWords(tree);
    return getTagLists(taggedSentence.list());
  }

  /**
   * Returns a new list of the tag lists for each word when the specified
   * sentence is in the format described in the comments for the {@link
   * #sentContainsWordsAndTags(SexpList)}.
   *
   * @param sent the sentence from which to extract tag lists
   * @return a new list of the tag lists for each word when the specified
   *         sentence is in the format described in the comments for the {@link
   *         #sentContainsWordsAndTags(SexpList)}
   */
  protected SexpList getTagLists(SexpList sent) {
    int size = sent.size();
    SexpList tagLists = new SexpList(size);
    for (int i = 0; i < size; i++)
      tagLists.add(sent.get(i).list().get(1));
    return tagLists;
  }

  /**
   * Parses the specified object, which must be a {@link SexpList}.
   *
   * @param obj the {@link SexpList} to parse
   * @return a parsed version of the sentence contained in the specified {@link
   *         SexpList}, or <code>null</code> if no parse was possible under the
   *         current model
   *
   * @throws RemoteException
   *
   * @see #parse(SexpList)
   */
  protected Object process(Object obj) throws RemoteException {
    if (decoder == null) {
      decoder = getNewDecoder(id, server);
    }
    sent = (SexpList)obj;
    return parse(sent);
  }

  /**
   * Prints the sentence currently being parsed to <code>System.err</code>
   * as an emergency backup (in case processing took a long time and
   * it is highly undesirable to lose the work).
   */
  protected void switchboardFailure() {
    err.println("Switchboard failure: client " + id + " did not get chance " +
		"to push most recently-parsed sentence to Switchboard; " +
		"parsed sentence:\n" + sent);
    if (Settings.getBoolean(Settings.clientDeathUponSwitchboardDeath)) {
      System.err.println("client " + id + " committing suicide because " +
			 Settings.clientDeathUponSwitchboardDeath +
			 " setting is true");
      try {
	die(true);
      }
      catch (RemoteException re) {
	re.printStackTrace();
      }
    }
  }

  /**
   * Obtains the timeout from <code>Settings</code>.
   *
   * @see Settings#sbUserTimeout
   */
  protected static int getTimeout() {
    return Settings.getIntProperty(Settings.sbUserTimeout, defaultTimeout);
  }

  /**
   * Returns the integer value of {@link Settings#serverMaxRetries}, or the
   * specified fallback default value if that property does not exist.
   *
   * @param defaultValue the fallback default value to return if {@link
   *                     Settings#serverMaxRetries} does not exist
   * @return the integer value of {@link Settings#serverMaxRetries}, or the
   *         specified fallback default value if that property does not exist
   */
  protected static int getRetries(int defaultValue) {
    return Settings.getIntProperty(Settings.serverMaxRetries, defaultValue);
  }

  /**
   * Returns the integer value of {@link Settings#serverRetrySleep}, or the
   * specified fallback default value if that property does not exist.
   *
   * @param defaultValue the fallback default to return if {@link
   *                     Settings#serverRetrySleep} does not exist
   * @return the integer value of {@link Settings#serverRetrySleep}, or the
   *         specified fallback default value if that property does not exist.
   */
  protected static int getRetrySleep(int defaultValue) {
    return Settings.getIntProperty(Settings.serverRetrySleep, defaultValue);
  }

  /**
   * Returns the boolean value of {@link Settings#serverFailover}, or the
   * specified fallback default value if that property does not exist.
   *
   * @param defaultValue the fallback default value to return if {@link
   *                     Settings#serverFailover} does not exist
   * @return the boolean value of {@link Settings#serverFailover}, or the
   *         specified fallback default value if that property does not exist
   */
  protected static boolean getFailover(boolean defaultValue) {
    return Settings.getBooleanProperty(Settings.serverFailover, defaultValue);
  }

  /**
   * Runs this parsing client within its enclosing thread: if {@link
   * #internalInputFilename} is <code>null</code>, then this method invokes
   * {@link #processObjectsThenDie()}; otherwise, this method processes {@link
   * #internalInputFilename} and outputs to {@link #internalOutputFilename} by
   * invoking {@link #processInputFile(String,String)}.
   */
  public void run() {
    if (internalInputFilename == null) {
      try {
	processObjectsThenDie();
      }
      catch (RemoteException re) {
	System.err.println(re);
	try { die(true); }
	catch (RemoteException re2) {
	  System.err.println("client " + id + " couldn't die! (" + re + ")");
	}
      }
    }
    else {
      try {
	processInputFile(internalInputFilename, internalOutputFilename);
	die(false);
      }
      catch (IOException ioe) {
	System.err.println(ioe);
	try { die(true); }
	catch (RemoteException re) {
	  System.err.println("client " + id + " couldn't die! (" + re + ")");
	}
      }
    }
  }

  /**
   * Sets the {@link #internalInputFilename} and {@link #internalOutputFilename}
   * members of this parsing client to the specified values.
   *
   * @param inputFilename the name of the input file to process
   * @param outputFilename the name of the output file to create
   */
  protected void setInternalFilenames(String inputFilename,
				      String outputFilename) {
    internalInputFilename = inputFilename;
    internalOutputFilename = outputFilename;
  }

  /**
   * Parses the sentences contained in the specified input file, outputting the
   * results to the specified output file.
   *
   * @param inputFilename  the input file to process
   * @param outputFilename the output file to create
   * @throws IOException if there is a problem creating the input file stream or
   *                     writing to the created output file stream
   */
  protected void processInputFile(String inputFilename, String outputFilename)
    throws IOException {

    if (decoder == null) {
      decoder = getNewDecoder(id, server);
    }

    InputStream in = null;
    if (inputFilename.equals("-")) {
      in = System.in;
    }
    else {
      File inFile = getFile(inputFilename);
      if (inFile == null)
	return;
      in = new FileInputStream(inFile);
    }

    int bufSize = Constants.defaultFileBufsize;
    OutputStream outputStream = (outputFilename.equals("-") ?
				 (OutputStream)System.out :
				 new FileOutputStream(outputFilename));
    OutputStreamWriter osw =
      new OutputStreamWriter(outputStream, Language.encoding());
    BufferedWriter out = new BufferedWriter(osw, bufSize);
    Sexp sent = null;
    SexpTokenizer tok = new SexpTokenizer(in, Language.encoding(), bufSize);
    Time totalTime = new Time();
    Time time = new Time();
    for (int i = 1; ((sent = Sexp.read(tok)) != null); i++) {
      err.println("processing sentence No. " + i + ": " + sent);
      time.reset();
      Sexp parsedSent = parse(sent.list());
      err.println("elapsed time: " + time);
      err.println("cummulative average elapsed time: " +
			 Time.elapsedTime(totalTime.elapsedMillis() / i));
      out.write(String.valueOf(parsedSent));
      out.write("\n");
      if (flushAfterEverySentence)
	out.flush();
    }
    err.println("\ntotal elapsed time: " + totalTime);
    err.println("\nHave a nice day!");
    out.flush();
  }

  // main stuff
  /**
   * The bound RMI name of the {@link Switchboard} specified on the command line
   * (defaults to {@link Switchboard#defaultBindingName}).
   */
  protected static String switchboardName = Switchboard.defaultBindingName;
  /** The derived data filename specified on the command line. */
  protected static String derivedDataFilename = null;
  /** The input filename specified on the command line. */
  protected static String inputFilename = null;
  /** The output filename specified on the command line. */
  protected static String outputFilename = null;
  /** The settings file to use specified on the command line. */
  protected static String settingsFilename = null;
  /** The number of parsing client to create in this JVM. */
  protected static int numClients = 1;
  /**
   * Indicates whether this is a stand-alone client, or is using a switchboard,
   * as specified on the command line.
   */
  protected static boolean standAlone = false;
  /**
   * Indicates whether the user specified on the command line for this client
   * to grab its settings from the switchboard.
   */
  protected static boolean grabSBSettings = true;

  private static final String[] usageMsg = {
    "usage: [-nc <numClients> | --num-clients <numClients>]",
    "\t[-sf <settings file> | --settings <settings file>]",
    "\t[--no-sb-settings]",
    "\t[-is <derived data file> | --internal-server <derived data file>] ",
    "\t[ [-sa <sentence input file> | --stand-alone <sentence input file> ",
    "\t       [-out <parse output file>] ] |",
    "\t  [<switchboard binding name>] ",
    "\t       [-in <sentence input file>] [-out <parse output file>] ]",
    "where",
    "\t<numClients> is the number of parser clients to start when using",
    "\t\tthe switchboard (ignored when in stand-alone mode)",
    "\t<settings file> is the name of a settings file to load locally",
    "\t--no-sb-settings indicates not to grab settings from the switchboard",
    "\t--internal-server|-is specifies to create an internal decoder server",
    "\t\tinstead of requesting one from the switchboard",
    "\t--stand-alone|-sa specifies not to use the switchboard, but to parse",
    "\t\tthe specified <sentence input file> with an internal",
    "\t\tdecoder server (must be used with --internal-server option)",
    "\t<sentence input file> is the sentence input file, or '-' for stdin",
    "\t<parse outputfile> is the name of the output file, or '-' for stdout",
    "\t-in specifies a sentence input file when using the switchboard,",
    "\t\twhich means the switchboard's object server facility will",
    "\t\tnot be used (only one client may be started in this mode)"
  };

  private static final void usage() {
    for (int i = 0; i < usageMsg.length; i++)
      System.err.println(usageMsg[i]);
  }

  private static final boolean processArgs(String[] args) {
    for (int i = 0; i < args.length; i++) {
      if (args[i].charAt(0) == '-') {
	// process switch
	if (args[i].equals("-help") || args[i].equals("-usage")) {
	  usage();
	  System.exit(0);
	}
	if (args[i].equals("-sf") || args[i].equals("--settings")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: " + args[i] + " requires a filename");
	    usage();
	    return false;
	  }
	  settingsFilename = args[++i];
	}
	else if (args[i].equals("--no-sb-settings")) {
	  grabSBSettings = false;
	}
	else if (args[i].equals("-sa") || args[i].equals("--stand-alone")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: " + args[i] + " requires a filename");
	    usage();
	    return false;
	  }
	  standAlone = true;
	  inputFilename = args[++i];
	}
	else if (args[i].equals("-is") || args[i].equals("--internal-server")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: " + args[i] + " requires a filename");
	    usage();
	    return false;
	  }
	  derivedDataFilename = args[++i];
	}
	else if (args[i].equals("-in")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: " + args[i] + " requires a filename");
	    usage();
	    return false;
	  }
	  inputFilename = args[++i];
	}
	else if (args[i].equals("-out")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: " + args[i] + " requires a filename");
	    usage();
	    return false;
	  }
	  outputFilename = args[++i];
	}
	else if (args[i].equals("-nc") || args[i].equals("--num-clients")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: " + args[i] + " requires an integer");
	    usage();
	    return false;
	  }
	  try {
	    numClients = Integer.parseInt(args[++i]);
	  }
	  catch (NumberFormatException nfe) {
	    System.err.println("error: " + args[i] + " requires an integer");
	    usage();
	    return false;
	  }
	}
	else {
	  System.err.println("unrecognized command-line switch: " + args[i]);
	  usage();
	  return false;
	}
      }
      else {
	switchboardName = args[i];
      }
    }

    if (!standAlone && numClients < 1) {
      System.err.println("error: number of clients must be greater than zero");
      usage();
      return false;
    }

    if (standAlone && derivedDataFilename == null) {
      System.err.println("error: must use --internal-server with -sa");
      usage();
      return false;
    }

    if (standAlone && inputFilename != null && numClients > 1) {
      System.err.println(
      "error: can't start more than one parsing client thread when internally"+
      "\n\tprocessing an input file; use switchboard's object server facility");
      usage();
      return false;
    }

    // we guarantee that if user specifies an input filename, the static
    // data member outputFilename will also be non-null
    if (inputFilename != null && outputFilename == null) {
      if (inputFilename.equals("-"))
	outputFilename = "stdin.parsed";
      else
	outputFilename = inputFilename + ".parsed";
    }

    return true;
  }

  /**
   * Checks the specified settings and issues warnings to
   * <code>System.err</code> when a current setting differs.
   *
   * @param sbSettings the settings to compare with the current run-time
   * settings
   */
  protected static void checkSettings(Properties sbSettings) {
    Iterator it = sbSettings.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry)it.next();
      String sbProp = (String)entry.getKey();
      String sbVal = (String)entry.getValue();
      String localVal = Settings.get(sbProp);
      if (sbVal.equals(localVal) == false) {
	System.err.println(className + ": warning: value of property \"" +
			   sbProp + "\" is\n\t\t\"" + localVal + "\"\n\t" +
			   "in settings obtained locally but is\n\t\t\"" +
			   sbVal +
			   "\"\n\tin settings obtained from switchboard");
      }
    }
  }

  /**
   * Grabs the settings from the {@link Switchboard} instance and sets
   * to be the current run-time settings.
   * @param sb the switchboard from which to grab settings for this client
   * @throws RemoteException
   */
  public static void setSettingsFromSwitchboard(SwitchboardRemote sb)
    throws RemoteException {
    Properties sbSettings = sb.getSettings();
    if (derivedDataFilename != null)
      checkSettings(sbSettings);
    Settings.setSettings(sbSettings);
  }

  /**
   * Returns a new parsing client constructed via its single-{@link String}
   * constructor using the specified derived data filename as the argument. The
   * run-time type of the returned parsing client will be equal to the value of
   * {@link #parserClass} member.
   *
   * @param derivedDataFilename the derived data filename with which to
   *                            construct a new parsing client instance
   * @return a new parsing client with an internal decoder using the derived
   * data contained in the specified file
   *
   * @throws NoSuchMethodException     if the class specified by {@link
   *                                   #parserClass} does not have a constructor
   *                                   that accepts a single {@link String} as
   *                                   its argument
   * @throws InvocationTargetException if the constructor of the class specified
   *                                   by {@link #parserClass} throws an
   *                                   exception
   * @throws IllegalAccessException    if the constructor of the class specified
   *                                   by {@link #parserClass} cannot be
   *                                   accessed
   * @throws InstantiationException    if there is a problem instantiating a new
   *                                   instance of the class specified by {@link
   *                                   #parserClass}
   */
  protected static Parser getNewParser(String derivedDataFilename)
    throws NoSuchMethodException, InvocationTargetException,
	   IllegalAccessException, InstantiationException {
    Parser parser = null;
    Constructor cons = parserClass.getConstructor(stringTypeArr);
    parser = (Parser)cons.newInstance(new Object[]{derivedDataFilename});
    return parser;
  }

  /**
   * Returns a new parsing client constructed via its single-<code>int</code>
   * constructor using the specified timeout value as the argument. The
   * run-time type of the returned parsing client will be equal to the value of
   * {@link #parserClass} member.
   *
   * @param timeout the timeout value for the client- and server-side sockets
   * of this RMI object
   * @return a new parsing client that uses the switchboard
   *
   * @throws NoSuchMethodException     if the class specified by {@link
   *                                   #parserClass} does not have a constructor
   *                                   that accepts a single {@link String} as
   *                                   its argument
   * @throws InvocationTargetException if the constructor of the class specified
   *                                   by {@link #parserClass} throws an
   *                                   exception
   * @throws IllegalAccessException    if the constructor of the class specified
   *                                   by {@link #parserClass} cannot be
   *                                   accessed
   * @throws InstantiationException    if there is a problem instantiating a new
   *                                   instance of the class specified by {@link
   *                                   #parserClass}
   */
  protected static Parser getNewParser(int timeout)
    throws NoSuchMethodException, InvocationTargetException,
	   IllegalAccessException, InstantiationException {
    Parser parser = null;
    Constructor cons = parserClass.getConstructor(intTypeArr);
    parser = (Parser)cons.newInstance(new Object[]{new Integer(timeout)});
    return parser;
  }

  /**
   * Returns a new {@link File} object for the specified filename, or
   * <code>null</code> if the specified file does not exist. An error
   * message will be output to <code>System.err</code> if the specified
   * file does not exist.
   * @param filename the filename for which to create a new {@link File}
   * object
   * @return a new {@link File} object for the specified filename, or
   * <code>null</code> if the specified file does not exist.
   */
  public static File getFile(String filename) {
    return getFile(filename, true);
  }

  /**
   * Returns a new {@link File} object for the specified filename, or
   * <code>null</code> if the specified file does not exist.
   * file does not exist.
   * @param filename the filename for which to create a new {@link File}
   * object
   * @param verbose indicates whether to output an error message to
   * <code>System.err</code> if the specified file does not exist
   * @return a new {@link File} object for the specified filename, or
   * <code>null</code> if the specified file does not exist.
   */
  public static File getFile(String filename, boolean verbose) {
    File file = new File(filename);
    if (file.exists()) {
      return file;
    }
    else {
      if (verbose)
	System.err.println(className + ": error: file \"" + filename +
			   "\" does not exist");
      return null;
    }
  }

  /**
   * Contacts the switchboard, registers this parsing client and
   * gets sentences from the switchboard, parses them and returns them,
   * until the switchboard indicates there are no more sentences to
   * process.  Multiple such clients may be created.  Execute the command
   * <pre>
   * java danbikel.parser.Parser -help
   * </pre>
   * for complete usage information.
   * <p/>
   * <b>Input file formats</b>: Input files processed by this class
   * when it is in its stand-alone mode must contain a series of S-expressions,
   * where each S-expression represents a sentence to be parsed.  There are
   * three acceptable input formats for these S-expressions, described in
   * the documentation for the {@link #parse(SexpList)} method.
   */
  public static void main(String[] args) {
    if (!processArgs(args))
      return;
    Parser parser = null;
    DecoderServer server = null;
    ThreadGroup clientThreads = new ThreadGroup("parser clients");
    if (standAlone) {
      try {
	if (settingsFilename != null) {
	  if (getFile(settingsFilename) == null)
	    return;
	  Settings.load(settingsFilename);
	}
	if (getFile(derivedDataFilename) == null)
	  return;
	//parser = new Parser(derivedDataFilename);
	parser = getNewParser(derivedDataFilename);
	parser.processInputFile(inputFilename, outputFilename);
      }
      catch (InstantiationException ie) {
	System.err.println(ie);
      }
      catch (IllegalAccessException iae) {
	System.err.println(iae);
      }
      catch (java.lang.reflect.InvocationTargetException ite) {
	System.err.println(invocationTargetExceptionMsg);
	ite.printStackTrace();
      }
      catch (NoSuchMethodException nsme) {
	System.err.println(nsme);
      }
      catch (RemoteException re) {
	System.err.println(re);
      }
      catch (IOException ioe) {
	System.err.println(ioe);
      }
    }
    else {
      setPolicyFile(Settings.getSettings());
      // create and install a security manager
      if (System.getSecurityManager() == null)
	System.setSecurityManager(new RMISecurityManager());
      // define fallback-default values for the following three
      // fault-tolerance settings
      int defaultRetries = 1, defaultRetrySleep = 1000;
      boolean defaultFailover = true;
      try {
	//DecoderServer server = null;
	if (derivedDataFilename != null) {
	  if (getFile(derivedDataFilename) == null)
	    return;
	  server = getNewDecoderServer(derivedDataFilename);
	}

	for (int i = 0; i < numClients; i++) {
	  try {
	    // first, try to get the switchboard so as to get settings
	    // BEFORE creating a Parser instance, so that any static data
	    // will be correct before the static initializers of Parser
	    // (and any dependent classes) are run
	    SwitchboardRemote sb =
	      AbstractSwitchboardUser.getSwitchboard(switchboardName,
						     defaultRetries);
	    if (grabSBSettings && sb != null) {
	      setSettingsFromSwitchboard(sb);
	      if (settingsFilename != null)
		Settings.load(settingsFilename);
	    }
	    //parser = new Parser(Parser.getTimeout());
	    parser = getNewParser(Parser.getTimeout());
	    parser.register(switchboardName);
	    if (grabSBSettings && sb == null) {
	      setSettingsFromSwitchboard(parser.switchboard);
	      if (settingsFilename != null) {
		Settings.load(settingsFilename);
	      }
	    }
	    if (derivedDataFilename != null) {
	      parser.server = server;
	    }
	    else {
	      parser.localServer = false;
	      parser.getFaultTolerantServer(getRetries(defaultRetries),
					    getRetrySleep(defaultRetrySleep),
					    getFailover(defaultFailover));
	    }
	    if (inputFilename != null) {
	      parser.setInternalFilenames(inputFilename, outputFilename);
	    }
	    new Thread(clientThreads, parser,
		       "Parse Client " + parser.id).start();
	  }
	  catch (RemoteException re) {
	    System.err.println(re);
	    if (parser != null) {
	    try { parser.die(true); }
	      catch (RemoteException re2) {
		System.err.println("client " + parser.id +
				   " couldn't die! (" + re + ")");
	      }
	    }
	  }
	}
      }
      catch (InstantiationException ie) {
	System.err.println(ie);
      }
      catch (IllegalAccessException iae) {
	System.err.println(iae);
      }
      catch (java.lang.reflect.InvocationTargetException ite) {
	System.err.println(invocationTargetExceptionMsg);
	ite.printStackTrace();
      }
      catch (NoSuchMethodException nsme) {
	System.err.println(nsme);
      }
      catch (MalformedURLException mue) {
	System.err.println(mue);
      }
      catch (IOException ioe) {
	System.err.println(ioe);
      }
      catch (ClassNotFoundException cnfe) {
	System.err.println(cnfe);
      }
    }
    if (debug)
      System.err.println(className + ": main ending!");

    // we only print cache stats when in stand-alone mode or when
    // there is an internal server
    if (debugCacheStats) {
      if (standAlone){
	System.err.println(((DecoderServer)parser.server).getModelCacheStats());
      }
      else if (derivedDataFilename != null) {
	while (clientThreads.activeCount() > 0) {
	  try {
	    Thread.currentThread().sleep(1000);
	  }
	  catch (InterruptedException ie) {
	    System.err.println(ie);
	  }
	}
	System.err.println(server.getModelCacheStats());
      }
      else {
	System.err.println(className + ": warning: not printing model cache " +
			   "stats because decoder server is a remote object");
      }
      /*
      parser = null;
      server = null;
      System.gc();
      System.runFinalization();
      */
    }
  }
}
