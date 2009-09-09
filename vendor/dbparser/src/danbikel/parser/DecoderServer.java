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
import danbikel.switchboard.*;
import java.net.MalformedURLException;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;

/**
 * Provides probabilities and other resources needed by decoders.
 */
public class DecoderServer
  extends AbstractServer implements DecoderServerRemote, Settings.Change {

  // data members
  /** The model collection used by this decoder server. */
  protected ModelCollection modelCollection;
  /** The value of {@link Training#stopSym()}, cached here for convenience. */
  protected Word stopWord = Language.training().stopWord();
  /** The integer value of {@link Settings#unknownWordThreshold}. */
  protected int unknownWordThreshold =
    Settings.getInteger(Settings.unknownWordThreshold);
  /** The boolean value of {@link Settings#downcaseWords}. */
  protected boolean downcaseWords = Settings.getBoolean(Settings.downcaseWords);


  /**
   * Constructs a non-exported <code>DecoderServer</code> object.
   */
  public DecoderServer(String mcFilename)
    throws ClassNotFoundException, IOException, OptionalDataException {
    setModelCollection(mcFilename);
    Settings.register(this);
  }

  /**
   * Constructs a new server with the specified timeout value for its
   * RMI sockets, to receive RMI calls on an anonymous port.
   *
   * @param timeout the timeout value, in milliseconds, to be used for the
   * client- and server-side RMI sockets of this object
   */
  public DecoderServer(int timeout) throws RemoteException {
    super(timeout);
    Settings.register(this);
  }

  /**
   * Constructs a new server with the specified timeout value for its
   * RMI sockets, to receive RMI calls on the specified port.
   *
   * @param timeout the timeout value, in milliseconds, to be used for the
   * client- and server-side RMI sockets of this object
   * @param port the port on which to receive RMI calls
   */
  public DecoderServer(int timeout, int port) throws RemoteException {
    super(timeout, port);
    Settings.register(this);
  }

  /**
   * Constructs a new server that will accept no more than the specified
   * number of clients, will optionally accept clients only by request,
   * that will use the specified timeout for its RMI sockets and
   * will accept RMI calls on the specified port.
   *
   * @param maxClients the maximum number of clients this server is
   * willing to accept
   * @param acceptClientsOnlyByRequest if <code>true</code>, then
   * this server will only accept clients that request it specifically
   * @param timeout the timeout value, in milliseconds, to be used for the
   * client- and server-side RMI sockets of this object
   * @param port the port on which to receive RMI calls
   */
  public DecoderServer(int maxClients, boolean acceptClientsOnlyByRequest,
		       int timeout, int port) throws RemoteException {
    super(maxClients, acceptClientsOnlyByRequest, timeout, port);
    Settings.register(this);
  }

  /**
   * Constructs a new server that will accept RMI calls on the specified
   * port, using the specified socket factories to create RMI sockets.
   *
   * @param port the port on which to receive RMI calls
   * @param csf the factory from which to create client-side RMI sockets
   * @param ssf the factory from which to create server-side RMI sockets
   */
  public DecoderServer(int port,
		       RMIClientSocketFactory csf,
		       RMIServerSocketFactory ssf)
    throws RemoteException {
    super(port, csf, ssf);
  }

  /**
   * Constructs a new server that will accept no more than the specified
   * number of clients, will optionally accept clients only by request,
   * will accept RMI calls on the specified port and will use the
   * specified socket factories to create its RMI sockets.
   *
   * @param maxClients the maximum number of clients this server is
   * willing to accept
   * @param acceptClientsOnlyByRequest if <code>true</code>, then
   * this server will only accept clients that request it specifically
   * @param port the port on which to receive RMI calls
   * @param csf the factory from which to create client-side RMI sockets
   * @param ssf the factory from which to create server-side RMI sockets
   */
  public DecoderServer(int maxClients,
		       boolean acceptClientsOnlyByRequest,
		       int port,
		       RMIClientSocketFactory csf,
		       RMIServerSocketFactory ssf)
    throws RemoteException {
    super(maxClients, acceptClientsOnlyByRequest, port, csf, ssf);
  }

  /**
   * Sets the model collection from the specified filename, which should
   * be the path to a Java object file.
   */
  protected void setModelCollection(String mcFilename)
    throws ClassNotFoundException, IOException, OptionalDataException {
    modelCollection = Trainer.loadModelCollection(mcFilename);
  }

  /**
   * A flow-through method for {@link ModelCollection#getModelCacheStats()}.
   * @return the value of {@link ModelCollection#getModelCacheStats()}
   */
  public String getModelCacheStats() {
    return modelCollection.getModelCacheStats();
  }

  public Sexp convertUnknownWord(Symbol originalWord, int index)
    throws RemoteException {
    CountsTable vocabCounter = modelCollection.vocabCounter();
    Sexp word = (downcaseWords ?
		 Symbol.get(originalWord.toString().toLowerCase()) :
		 originalWord);
    int freq = (int)vocabCounter.count(word);
    if (freq < unknownWordThreshold) {
      Symbol features =
	Language.wordFeatures.features(originalWord, index == 0);
      Symbol neverObserved =
	(freq == 0 ? Constants.trueSym : Constants.falseSym);
      SexpList wordAndFeatures =
	new SexpList(3).add(originalWord).add(features).add(neverObserved);
      return wordAndFeatures;
    }
    else {
      return originalWord;
    }
  }

  /**
   * Replaces all unknown words in the specified sentence with
   * three-element lists, where the first element is the word itself, the
   * second element is a word-feature vector, as determined by the
   * implementation of {@link WordFeatures#features(Symbol,boolean)}, and
   * the third element is {@link Constants#trueSym} if this word was never
   * observed during training or {@link Constants#falseSym} if it was
   * observed at least once during training.
   *
   * @param sentence a list of symbols representing a sentence to be parsed
   */
  public SexpList convertUnknownWords(SexpList sentence)
    throws RemoteException {
    CountsTable vocabCounter = modelCollection.vocabCounter();
    int sentLen = sentence.length();
    for (int i = 0; i < sentLen; i++) {
      sentence.set(i, convertUnknownWord(sentence.symbolAt(i), i));
    }
    return sentence;
  }

  /**
   * Returns the nonterminals <code>CountsTable</code> of the internal
   * <code>ModelCollection</code> object.  The set of nonterminals
   * is needed when decoding.
   */
  public CountsTable nonterminals() throws RemoteException {
    return modelCollection.nonterminals();
  }

  /**
   * Returns the map of vocabulary items to possible parts of speech, contained
   * in the internal <code>ModelCollection</code> object.  This map
   * is needed when decoding.
   */
  public Map posMap() throws RemoteException {
    return modelCollection.posMap();
  }

  public Map headToParentMap() throws RemoteException {
    return modelCollection.headToParentMap();
  }

  /**
   * Returns a map of <code>Event</code> objects to <code>Set</code> objects,
   * where each <code>Event</code> object is the last level of back-off
   * of the probability structure for left-side subcat generation and the
   * set contains all possible <code>Subcat</code> objects for that
   * most-general context.
   */
  public Map leftSubcatMap() throws RemoteException {
    return modelCollection.leftSubcatMap();
  }

  /**
   * Returns a map of <code>Event</code> objects to <code>Set</code> objects,
   * where each <code>Event</code> object is the last level of back-off
   * of the probability structure for right-side subcat generation and the
   * set contains all possible <code>Subcat</code> objects for that
   * most-general context.
   */
  public Map rightSubcatMap() throws RemoteException {
    return modelCollection.rightSubcatMap();
  }

  public Map modNonterminalMap() throws RemoteException {
    return modelCollection.modNonterminalMap();
  }

  public Map simpleModNonterminalMap() throws RemoteException {
    return modelCollection.simpleModNonterminalMap();
  }

  public Set prunedPreterms() throws RemoteException {
    return modelCollection.prunedPreterms();
  }

  public Set prunedPunctuation() throws RemoteException {
    return modelCollection.prunedPunctuation();
  }

  /**
   * The probability structure for the submodel that generates subcats
   * on the left-hand side of head constituents.  This structure is needed
   * to derive most-general contexts (using the last level of back-off)
   * in order to determine all possible left-side subcat frames for a given
   * context, using the {@link #leftSubcatMap()}.
   */
  public ProbabilityStructure leftSubcatProbStructure() throws RemoteException {
    return modelCollection.leftSubcatModel().getProbStructure();
  }

  /**
   * The probability structure for the submodel that generates subcats
   * on the right-hand side of head constituents.  This structure is needed
   * to derive most-general contexts (using the last level of back-off)
   * in order to determine all possible left-side subcat frames for a given
   * context, using the {@link #rightSubcatMap()}.
   */
  public ProbabilityStructure rightSubcatProbStructure() throws RemoteException {
    return modelCollection.rightSubcatModel().getProbStructure();
  }

  /**
   * The probability structure for the submodel that generates modifiers
   * of head constituents.  This structure is needed to derive most-general
   * contexts (using the last level of back-off) in order to determine all
   * possible modifiers for a given context, using the
   * {@link #modNonterminalMap()}.
   */
  public ProbabilityStructure modNonterminalProbStructure()
  throws RemoteException {
    return modelCollection.modNonterminalModel().getProbStructure();
  }

  /** Returns 1.0. */
  public double testProb() throws RemoteException {
    return 1.0;
  }

  /**
   * Returns the prior probability for the lexicalized nonteminal encoded in the
   * specified <code>TrainerEvent</code>, which should be an instance of
   * <code>HeadEvent</code>.  The prior probability is decomposed into two
   * parts:<br>
   * <blockquote>
   * <i>p(w,t) * p(N | w,t)</i>
   * </blockquote>
   * where <i>N</i> is a nonterminal label, <i>w</i> is a word and <i>t</i>
   * is a part-of-speech tag.
   */
  public double logPrior(int id, TrainerEvent event) {
    Model lexPriorModel = modelCollection.lexPriorModel();
    Model nonterminalPriorModel = modelCollection.nonterminalPriorModel();
    double lexPriorProb = lexPriorModel.estimateLogProb(id, event);
    if (lexPriorProb == Constants.logOfZero)
      return Constants.logOfZero;
    double nonterminalPriorProb =
      nonterminalPriorModel.estimateLogProb(id, event);
    if (nonterminalPriorProb == Constants.logOfZero)
      return Constants.logOfZero;
    return lexPriorProb + nonterminalPriorProb;
  }

  public double logProbHeadWithSubcats(int id, TrainerEvent event) {
    double headProb = modelCollection.headModel().estimateLogProb(id, event);
    if (headProb == Constants.logOfZero)
      return Constants.logOfZero;
    double leftSubcatProb =
      modelCollection.leftSubcatModel().estimateLogProb(id, event);
    if (leftSubcatProb == Constants.logOfZero)
      return Constants.logOfZero;
    double rightSubcatProb =
      modelCollection.rightSubcatModel().estimateLogProb(id, event);
    if (rightSubcatProb == Constants.logOfZero)
      return Constants.logOfZero;
    return headProb + leftSubcatProb + rightSubcatProb;
  }

  public double logProbHead(int id, TrainerEvent event) {
    return modelCollection.headModel().estimateLogProb(id, event);
  }

  public double logProbLeftSubcat(int id, TrainerEvent event) {
    return modelCollection.leftSubcatModel().estimateLogProb(id, event);
  }

  public double logProbRightSubcat(int id, TrainerEvent event) {
    return modelCollection.rightSubcatModel().estimateLogProb(id, event);
  }

  public double logProbSubcat(int id, TrainerEvent event, boolean side) {
    return (side == Constants.LEFT ?
	    modelCollection.leftSubcatModel().estimateLogProb(id, event) :
	    modelCollection.rightSubcatModel().estimateLogProb(id, event));
  }

  public double logProbTop(int id, TrainerEvent event) {
    Model topNTModel = modelCollection.topNonterminalModel();
    Model topLexModel = modelCollection.topLexModel();
    double ntProb = topNTModel.estimateLogProb(id, event);
    if (ntProb == Constants.logOfZero)
      return Constants.logOfZero;
    double lexProb = topLexModel.estimateLogProb(id, event);
    if (lexProb == Constants.logOfZero)
      return Constants.logOfZero;
    return ntProb + lexProb;
  }

  public double logProbMod(int id, TrainerEvent event) {
    Model modNTModel = modelCollection.modNonterminalModel();
    Model modWordModel = modelCollection.modWordModel();
    double modNTProb = modNTModel.estimateLogProb(id, event);
    if (modNTProb == Constants.logOfZero)
      return Constants.logOfZero;
    if (stopWord.equals(event.modHeadWord()))
      return modNTProb;
    double modWordProb = modWordModel.estimateLogProb(id, event);
    if (modWordProb == Constants.logOfZero)
      return Constants.logOfZero;
    return modNTProb + modWordProb;
  }

  public double logProbModNT(int id, TrainerEvent event) {
    Model modNTModel = modelCollection.modNonterminalModel();
    return modNTModel.estimateLogProb(id, event);
  }

  public double logProbGap(int id, TrainerEvent event) {
    return modelCollection.gapModel().estimateLogProb(id, event);
  }

  // non-log prob methods
  public double probHead(int id, TrainerEvent event) throws RemoteException {
    return modelCollection.headModel().estimateProb(id, event);
  }
  public double probMod(int id, TrainerEvent event) throws RemoteException {
    Model modNTModel = modelCollection.modNonterminalModel();
    Model modWordModel = modelCollection.modWordModel();
    double modNTProb = modNTModel.estimateProb(id, event);
    if (modNTProb == 0.0)
      return 0.0;
    if (stopWord.equals(event.modHeadWord()))
      return modNTProb;
    double modWordProb = modWordModel.estimateProb(id, event);
    if (modWordProb == 0.0)
      return 0.0;
    return modNTProb * modWordProb;
  }
  public double probLeftSubcat(int id, TrainerEvent event)
    throws RemoteException {
    return modelCollection.leftSubcatModel().estimateProb(id, event);
  }
  public double probRightSubcat(int id, TrainerEvent event)
    throws RemoteException {
    return modelCollection.rightSubcatModel().estimateProb(id, event);
  }
  public double probTop(int id, TrainerEvent event) throws RemoteException {
    Model topNTModel = modelCollection.topNonterminalModel();
    Model topLexModel = modelCollection.topLexModel();
    double ntProb = topNTModel.estimateProb(id, event);
    if (ntProb == 0.0)
      return 0.0;
    double lexProb = topLexModel.estimateProb(id, event);
    if (lexProb == 0.0)
      return 0.0;
    return ntProb * lexProb;
  }

  /**
   * Obtains the timeout from <code>Settings</code>.
   *
   * @see Settings#sbUserTimeout
   */
  protected static int getTimeout() {
    String timeoutStr = Settings.get(Settings.sbUserTimeout);
    return (timeoutStr != null ? Integer.parseInt(timeoutStr) :
	    defaultTimeout);
  }

  /**
   * Starts a decoder server and registers it with the switchboard.
   * usage: [switchboard binding name] <derived data file>
   */
  public static void main(String[] args) {
    String switchboardName = Switchboard.defaultBindingName;
    String derivedDataFilename = null;
    if (args.length != 1 && args.length != 2) {
      System.err.println("usage: [switchboard binding name] " +
			 "<derived data file>");
      System.exit(1);
    }
    if (args.length == 2) {
      switchboardName = args[0];
      derivedDataFilename = args[1];
    }
    if (args.length == 1) {
      derivedDataFilename = args[0];
    }
    //Create and install a security manager
    if (System.getSecurityManager() == null)
      System.setSecurityManager(new RMISecurityManager());
    DecoderServer decoderServer = null;
    try {
      decoderServer = new DecoderServer(DecoderServer.getTimeout());
      decoderServer.setModelCollection(derivedDataFilename);
      decoderServer.register(switchboardName);
      Settings.setSettings(decoderServer.switchboard.getSettings());
      decoderServer.startAliveThread();
      decoderServer.unexportWhenDead();
    }
    catch (RemoteException re) {
      System.err.println(re);
      if (decoderServer != null) {
	try { decoderServer.die(true); }
	catch (RemoteException re2) {
	  System.err.println("couldn't die! (" + re + ")");
	}
      }
    }
    catch (MalformedURLException mue) {
      System.err.println(mue);
    }
    catch (OptionalDataException ode) {
      System.err.println(ode);
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
    catch (ClassNotFoundException cnfe) {
      System.err.println(cnfe);
    }
  }

  public void update(Map<String, String> changedSettings) {
    stopWord = Language.training().stopWord();
    unknownWordThreshold =
      Settings.getInteger(Settings.unknownWordThreshold);
    downcaseWords = Settings.getBoolean(Settings.downcaseWords);
  }
}
