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
import java.io.*;
import java.lang.reflect.*;
import java.util.Map;

/**
 * Abstract class to represent the probability structure&mdash;the entire
 * set of of back-off levels, including the top level&mdash;for the
 * estimation of a particular parameter class in the overall parsing
 * model (using "class" in the statistical, non-Java sense of the
 * word).  Providing this abstract structure is intended to facilitate
 * the experimentation with differing smoothing or back-off schemes.
 * Various data members are provided to enable efficient construction
 * of <code>SexpEvent</code> objects to represent events in the
 * back-off scheme, but any class that implements the {@link Event}
 * interface may be used to record events in a concrete subclass of
 * this class.
 * <p/>
 * <b>Design note</b>: The probability estimates of a {@link Model} object
 * using a {@link ProbabilityStructure} will be somewhat unpredictable if
 * the history contexts at the back-off levels do not represent supersets
 * of one another.  That is, the history context at back-off level
 * <i>i</i>&nbsp;+&nbsp;1 <b><i>must</i></b> be a superset of the context
 * at back-off level <i>i</i>.
 * <p/>
 * <b>Concurrency note</b>: A separate <code>ProbabiityStructure</code> object
 * needs to be constructed for each thread that needs to use its facilities,
 * to avoid concurrent access and modification of its data members (which
 * are intended to improve efficiency and are thus not designed for
 * concurrent access via <code>synchronized</code> blocks).
 *
 * @see Model
 * @see JointModel
 * @see Trainer
 */
public abstract class ProbabilityStructure implements Serializable {
  /**
   * The size of the cache that model's of this probability structure should
   * use for events containing maximal context.
   *
   * @see #cacheSize(int)
   */
  protected transient int topLevelCacheSize;
  /**
   * Indicates whether certain events/distributions of low or no utility should
   * be pruned from the model using this probability structure. This variable
   * will be set to the boolean value of the setting <code>getClass().getName()
   * + ".doPruning"</code>. For example, if there is a concrete subclass of this
   * class named <code>com.pkg.Foo</code>, and if it is desirable to have
   * pruning performed for models using the <code>com.pkg.Foo</code> probability
   * structure, then the settings file should include the line
   * <tt>com.pkg.Foo.doPruning=true</tt>.
   */
  protected transient boolean doPruning;

  /** The value off the {@link Settings#defaultModelClass} setting. */
  protected static String defaultModelClassName =
    Settings.get(Settings.defaultModelClass);

  private static Constructor setDefaultModelConstructor() {
    Constructor defaultModelConstructor = null;
    try {
      Class defaultModelClass = Class.forName(defaultModelClassName);
      Class[] params = {ProbabilityStructure.class};
      defaultModelConstructor = defaultModelClass.getConstructor(params);
    }
    catch (ClassNotFoundException cnfe) {
      System.err.println(cnfe);
    }
    catch (NoSuchMethodException nsme) {
      System.err.println(nsme);
    }
    return defaultModelConstructor;
  }

  /**
   * The constructor of the class specified by the
   * {@link Settings#defaultModelClass} setting, taking a single
   * {@link ProbabilityStructure} as its only argument.
   */
  protected static Constructor defaultModelConstructor =
    setDefaultModelConstructor();

  static {
    Settings.Change change = new Settings.Change() {
      public void update(Map<String, String> changedSettings) {
	if (changedSettings.containsKey(Settings.defaultModelClass)) {
	  defaultModelClassName =
	    Settings.get(Settings.defaultModelClass);
	  defaultModelConstructor = setDefaultModelConstructor();
	}
      }
    };
    Settings.register(ProbabilityStructure.class, change, null);
  }



  /**
   * A reusable list to enable efficient construction of <code>SexpEvent</code>
   * objects of various sizes to represent history contexts.
   *
   * @deprecated Ever since the <code>Event</code> and
   * <code>MutableEvent</code> interfaces were re-worked to include
   * methods to add and iterate over event components and the
   * <code>SexpEvent</code> class was retrofitted to these new
   * specifications, this object became superfluous, as
   * <code>SexpEvent</code> objects can now be efficiently constructed
   * directly, by using the <code>SexpEvent.add(Object)</code> method.
   *
   * @see SexpEvent#add(Object)
   * @see #histories
   * @see #historiesWithSubcats
   */
  protected SexpList historyList;

  /**
   * A reusable list to enable efficient construction of <code>SexpEvent</code>
   * objects of various sizes to represent futures.
   *
   * @deprecated Ever since the <code>Event</code> and
   * <code>MutableEvent</code> interfaces were re-worked to include
   * methods to add and iterate over event components and the
   * <code>SexpEvent</code> class was retrofitted to these new
   * specifications, this object became superfluous, as
   * <code>SexpEvent</code> objects can now be efficiently constructed
   * directly, by using the <code>SexpEvent.add(Object)</code> method.
   *
   * @see SexpEvent#add(Object)
   * @see #futures
   * @see #futuresWithSubcats
   */
  protected SexpList futureList;

  /**
   * A reusable <code>SexpEvent</code> array to represent history
   * contexts; the array will be initialized to have the size of
   * {@link #numLevels()}.  These objects may be used as the return values of
   * <code>getHistory(TrainerEvent,int)</code>.
   * @see #getHistory(TrainerEvent,int)
   */
  protected MutableEvent[] histories;
  /**
   * A reusable <code>SexpEvent</code> array to represent futures;
   * the array will be initialized to have the size of {@link #numLevels()}.
   * These objects may be used as the return values of
   * <code>getFuture(TrainerEvent,int)</code>.
   * @see #getFuture(TrainerEvent,int)
   */
  protected MutableEvent[] futures;

  /**
   * A reusable <code>SexpSubcatEvent</code> array to represent
   * histories; the array will be initialized to have the size of
   * {@link #numLevels()}.
   * These objects may be used as the return values of
   * <code>getHistory(TrainerEvent,int)</code>.
   * @see #getHistory(TrainerEvent,int)
   */
  protected MutableEvent[] historiesWithSubcats;

  /**
   * A reusable <code>SexpSubcatEvent</code> array to represent futures;
   * the array will be initialized to have the size of
   * {@link #numLevels()}. These objects may be used as the return values of
   * <code>getFuture(TrainerEvent,int)</code>.
   * @see #getFuture(TrainerEvent,int)
   */
  protected MutableEvent[] futuresWithSubcats;

  /**
   * A reusable <code>Transition</code> array to store transitions.
   * The <code>Transition</code> objects in this array may be used as the
   * return values of {@link #getTransition(TrainerEvent,int)}.
   */
  public Transition[] transitions;

  /**
   * An array used only during the computation of top-level probabilities,
   * used to store the ML estimates of all the levels of back-off.
   *
   * @see Model#estimateLogProb(int,TrainerEvent)
   *
   */
  public double[] estimates;
  /**
   * An array used only during the computation of top-level probabilities,
   * used to store the lambdas calculated at all the levels of back-off.
   *
   * @see Model#estimateLogProb(int,TrainerEvent)
   */
  public double[] lambdas;

  /**
   * A temporary value used in the computation of top-level probabilities,
   * used in the computation of lambdas.
   *
   * @see Model#estimateLogProb(int,TrainerEvent)
   */
  public double prevHistCount;

  /**
   * Handle onto additional data object for this probability structure,
   * whose value is <code>null</code> if no other data is required for
   * the concrete probability structure.
   */
  protected Object additionalData;

  /**
   * Usually called implicitly, this constructor initializes the
   * internal, reusable {@link #historyList} to have an initial capacity of
   * the return value of <code>maxEventComponents</code>.
   *
   * @see #historyList
   * @see #futureList
   * @see #maxEventComponents
   */
  protected ProbabilityStructure() {
    histories = new SexpEvent[numLevels()];
    for (int i = 0; i < histories.length; i++)
      histories[i] = new SexpEvent(maxEventComponents());
    futures = new SexpEvent[numLevels()];
    for (int i = 0; i < futures.length; i++)
      futures[i] = new SexpEvent(maxEventComponents());
    historiesWithSubcats = new SexpSubcatEvent[numLevels()];
    for (int i = 0; i < historiesWithSubcats.length; i++)
      historiesWithSubcats[i] = new SexpSubcatEvent(maxEventComponents());
    futuresWithSubcats = new SexpSubcatEvent[numLevels()];
    for (int i = 0; i < futuresWithSubcats.length; i++)
      futuresWithSubcats[i] = new SexpSubcatEvent(maxEventComponents());

    transitions = new Transition[numLevels()];
    for (int i = 0; i < transitions.length; i++)
      transitions[i] = new Transition(null, null);

    ///////////////////////////////////////////////////////////////////////////
    // no longer needed
    //historyList = new SexpList(maxEventComponents());
    //futureList = new SexpList(maxEventComponents());
    ///////////////////////////////////////////////////////////////////////////

    estimates = new double[numLevels()];
    lambdas = new double[numLevels()];

    topLevelCacheSize = getTopLevelCacheSize();
    doPruning = Settings.getBoolean(getClass().getName() + ".doPruning");
  }

  /**
   * This method converts the value of the setting named
   * <code>getClass().getName()&nbsp;+&nbsp;".topLevelCacheSize"</code>
   * to an integer and returns it.  This method is used within the
   * constructor of this abstract class to set the value of the
   * {@link #topLevelCacheSize} data member.  Subclasses should override
   * this method if such a setting may not be available or if a different
   * mechanism for determining the top-level cache size is desired.
   *
   * @see Settings#get(String)
   */
  protected int getTopLevelCacheSize() {
    String topLevelCacheSizeStr =
      Settings.get(getClass().getName() + ".topLevelCacheSize");
    /*
    System.err.println(getClass().getName() + ": setting top-level cache " +
		       "size to be " + topLevelCacheSizeStr);
    */
    return (topLevelCacheSizeStr == null ?
	    0 : Integer.parseInt(topLevelCacheSizeStr));
  }

  /**
   * Returns whether models using this probability structure should prune
   * parameters.
   *
   * @return whether models using this probability structure should prune
   *         parameters.
   *
   * @see #doPruning
   */
  public boolean doPruning() { return doPruning; }

  /**
   * Returns a default name of the smoothing parameters file, which is the
   * value of <code>getClass().getName()&nbsp;+&nbsp;".smoothingParams"</code>.
   *
   * @return a default name of the smoothing parameters file, which is the
   * value of <code>getClass().getName()&nbsp;+&nbsp;".smoothingParams"</code>
   *
   * @see #smoothingParametersFile()
   */
  protected String defaultSmoothingParamsFilename() {
    return getClass().getName() + ".smoothingParams";
  }

  /**
   * Returns the name of the smoothing parameters file, either to be created
   * if {@link #saveSmoothingParameters()} returns <code>true</code>, or
   * read from and used if either {@link #dontAddNewParameters()} or
   * {@link #useSmoothingParameters()} return <code>true</code>.
   * <p>
   * The name of the smoothing file returned by this method is
   * the value of the setting
   * <code>getClass().getName()&nbsp;+&nbsp;".smoothingParametersFile"</code>,
   * or the value returned by {@link #defaultSmoothingParamsFilename()} if
   * this property is not set.
   *
   * @return the name of the smoothing parameters file, either to be
   * created or read from and used in a training run
   *
   * @see #saveSmoothingParameters()
   * @see #dontAddNewParameters()
   * @see #useSmoothingParameters()
   */
  public String smoothingParametersFile() {
    String smoothingParamsFile =
      Settings.get(getClass().getName() + ".smoothingParametersFile");
    if (smoothingParamsFile == null)
      smoothingParamsFile = defaultSmoothingParamsFilename();
    return smoothingParamsFile;
  }

  /**
   * Indicates that this probability structure's associated {@link Model}
   * object should save the smoothing parameters to the file named by
   * {@link #smoothingParametersFile()} when precomputing probabilities during
   * training.  If the {@link Settings#precomputeProbs} setting is
   * <code>false</code> then the value of this property is ignored.
   * <p>
   * The default implementation here gets the boolean value of the setting
   * <code>getClass().getName()&nbsp;+&nbsp;".saveSoothingParameters"</code>,
   * as determined by {@link Boolean#valueOf(String)}.
   *
   * @return whether or not this probability structure's associated
   * {@link Model} object should save the smoothing parameters to the file
   * named by {@link #smoothingParametersFile()}
   *
   * @see #smoothingParametersFile()
   */
  protected boolean saveSmoothingParameters() {
    return
      Settings.getBoolean(getClass().getName() + ".saveSmoothingParameters");
  }

  /**
   * Indicates whether this probability structure's associated {@link Model}
   * object should not add new parameters when deriving counts by consulting
   * the smoothing parameters from {@link #smoothingParametersFile()}.
   * Specifically, for each history context derived from a {@link TrainerEvent},
   * a derived count will only be added for that history context if it has a
   * non-zero smoothing parameter, as determined by the information contained
   * in {@link #smoothingParametersFile}.  Effectively, when this method
   * returns <code>true</code>, it indicates to use the smoothing parameters
   * contained in {@link #smoothingParametersFile()} only to determine
   * which histories have non-zero smoothing values.
   * If {@link #useSmoothingParameters} returns <code>true</code>, then the
   * <i>all</i> the smoothing parameters contained in
   * {@link #smoothingParametersFile()} will be used, meaning that
   * no new parameters will be added, making the return value of this
   * method irrelevant (because it will implicitly be true).
   * <p>
   * The default implementation here gets the boolean value of the setting
   * <code>getClass().getName()&nbsp;+&nbsp;".dontAddNewParameters"</code>,
   * as determined by {@link Boolean#valueOf(String)}.
   *
   * @return whether this probability structure's associated {@link Model}
   * object should not add new parameters when deriving counts by consulting
   * the smoothing parameters from {@link #smoothingParametersFile()}
   *
   * @see #smoothingParametersFile
   * @see #useSmoothingParameters()
   */
  protected boolean dontAddNewParameters() {
    return
      Settings.getBoolean(getClass().getName() + ".dontAddNewParameters");
  }

  /**
   * Indicates whether this probability structure's associated {@link Model}
   * object should use the smoothing parameters contained in the file
   * {@link #smoothingParametersFile()} when deriving counts and precomputing
   * probabilities.  Note that when this method returns <code>true</code>, no
   * new parameters will be added to the model when deriving counts, thus
   * making the return value of {@link #dontAddNewParameters()} irrelevant.
   * <p>
   * The default implementation here gets the boolean value of the setting
   * <code>getClass().getName()&nbsp;+&nbsp;".dontAddNewParameters"</code>,
   * as determined by {@link Boolean#valueOf(String)}.
   *
   * @return whether this probability structure's associated {@link Model}
   * object should use the smoothing parameters contained in the file
   * {@link #smoothingParametersFile()} when deriving counts and precomputing
   * probabilities
   *
   * @see #smoothingParametersFile
   * @see #dontAddNewParameters
   */
  protected boolean useSmoothingParameters() {
    return
      Settings.getBoolean(getClass().getName() + ".useSmoothingParameters");
  }

  /**
   * Allows subclasses to specify the maximum number of event components,
   * so that the constructor of this class may pre-allocate space in its
   * internal, reusable <code>MutableEvent</code> objects (used for efficient
   * event construction).  The default implementation simply returns 1.
   *
   * @return 1 (subclasses should override this method)
   * @see MutableEvent#ensureCapacity
   */
  protected int maxEventComponents() { return 1; }

  /**
   * Returns a newly-constructed <code>Model</code> object for this
   * probability structure. The default implementation here returns
   * an instance of <code>Model</code>.  If a concrete
   * <code>ProbabilityStructure</code> class overrides
   * {@link #jointModel()}, it should use this method to return an
   * instance of a class that is suitable for handling multiple
   * <code>ProbabilityStructure</code> objects, such as {@link JointModel}.
   *
   * @see #jointModel()
   * @see Model
   * @see JointModel
   */
  public Model newModel() {
    Model newModel = null;
    if (defaultModelConstructor == null)
      newModel = new Model(this);
    else {
      try {
        newModel =
          (Model)defaultModelConstructor.newInstance(new Object[] {this});
      }
      catch (InstantiationException ie) {
        System.err.println(ie);
      }
      catch (IllegalAccessException iae) {
        System.err.println(iae);
      }
      catch (InvocationTargetException ite) {
        ite.printStackTrace();
      }
    }
    return newModel;
  }

  /**
   * Returns an array of other <code>ProbabilityStructure</code> objects
   * for use in a <code>JointModel</code> instance, or <code>null</code>
   * if this probability structure should not be composed with a
   * <code>JointModel</code> instance.  This default implementation returns
   * <code>null</code>.
   *
   * @return an array of other <code>ProbabilityStructure</code>
   * objects, or <code>null</code> if this probability structure should
   * not be composed with a <code>JointModel</code> instance
   *
   * @see JointModel
   */
  public ProbabilityStructure[] jointModel() {
    return null;
  }

  /**
   * Returns the number of back-off levels.
   */
  abstract public int numLevels();

  /**
   * Returns the level that corresponds to the prior for
   * that which is being predicted (the future); if there is no such
   * level, this method returns -1 (the default implementation returns -1).
   */
  public int priorLevel() { return -1; }

  /**
   * Returns the "fudge factor" for the lambda computation for
   * <code>backOffLevel</code>.  The default implementation returns
   * <code>5.0</code>.
   *
   * @param backOffLevel the back-off level for which to return a "fudge
   * factor"
   */
  public double lambdaFudge(int backOffLevel) { return 5.0; }

  /**
   * Returns the "fudge term" for the lambda computation for
   * <code>backOffLevel</code>.  The default implementation returns
   * <code>0.0</code>.
   */
  public double lambdaFudgeTerm(int backOffLevel) { return 0.0; }

  /**
   * Returns the smoothing value to be used with back-off levels whose
   * histories never occurred in training, meaning that 1 minus this
   * value will be the total probability mass for the smoothed estimate
   * at the specified back-off level (resulting in a degenerate model
   * unless this value is zero).  From another perspective, this
   * method returns the confidence that the raw maximum-likelihood
   * estimate for this back-off level should be zero given that
   * this history was never seen during training.
   * <p/>
   * By default this method returns <tt>0.0</tt> for all back-off levels.
   */
  public double lambdaPenalty(int backOffLevel) { return 0.0; }

  /**
   * Extracts the history context for the specified back-off level
   * from the specified trainer event.
   *
   * @param trainerEvent the event for which a history context is desired
   * for the specified back-off level
   * @param backOffLevel the back-off level for which to get a history context
   * from the specified trainer event
   * @return an <code>Event</code> object that represents the history context
   * for the specified back-off level
   */
  abstract public Event getHistory(TrainerEvent trainerEvent,
				   int backOffLevel);

  /**
   * Extracts the future for the specified level of back-off from the specified
   * trainer event.  Typically, futures remain the same regardless of back-off
   * level.
   *
   * @param trainerEvent the event from which a future is to be extracted
   * @param backOffLevel the back-off level for which to get the future event
   * @return an <code>Event</code> object that represents the future
   * for the specified back-off level
   */
  abstract public Event getFuture(TrainerEvent trainerEvent,
				  int backOffLevel);


  /**
   * Returns the reusable transition object for the specified back-off level,
   * with its history set to the result of calling
   * <code>getHistory(trainerEvent, backOffLevel)</code> and its
   * future the result of <code>getFuture(trainerEvent, backOffLevel)</code>.
   *
   * @param trainerEvent the event from which a transition is to be extracted
   * @param backOffLevel the back-off level for which to get the transition
   * @return the reusable transition object containing the history and future
   * of the specified back-off level
   */
  public Transition getTransition(TrainerEvent trainerEvent,
				  int backOffLevel) {
    Transition transition = transitions[backOffLevel];
    transition.setHistory(getHistory(trainerEvent, backOffLevel));
    transition.setFuture(getFuture(trainerEvent, backOffLevel));
    return transition;
  }

  Transition getTransition(TrainerEvent trainerEvent, Event history,
			   int backOffLevel) {
    Transition transition = transitions[backOffLevel];
    transition.setHistory(history);
    transition.setFuture(getFuture(trainerEvent, backOffLevel));
    return transition;
  }

  /**
   * Indicates whether the <code>Model</code> class needs to invoke
   * its cleanup method at the end of its {@link
   * Model#deriveCounts(CountsTable, danbikel.util.Filter ,double, danbikel.util.FlexibleMap)
   * deriveCounts} method.  The default implementation here returns
   * <code>false</code>.
   *
   * @see #removeHistory(int,Event)
   * @see #removeFuture(int,Event)
   * @see #removeTransition(int,Transition)
   * @see Model#deriveCounts(CountsTable,danbikel.util.Filter,double,danbikel.util.FlexibleMap)
   * @see Model#cleanup()
   */
  public boolean doCleanup() {
    return false;
  }

  /**
   * Indicates that {@link Model#cleanup()}, which is invoked at the end
   * of {@link Model#deriveCounts(CountsTable,danbikel.util.Filter,double,danbikel.util.FlexibleMap)
   * Model.deriveCounts},
   * can safely remove the specified event from the <code>Model</code>
   * object's internal counts tables, as the event is not applicable
   * to any of the probabilities for which the model will produce an estimate.
   * <p>
   * The default implementation simply returns <code>false</code>.
   *
   * @see Model#deriveCounts(CountsTable,danbikel.util.Filter,double,danbikel.util.FlexibleMap)
   * @see Model#cleanup()
   */
  public boolean removeHistory(int backOffLevel, Event history) {
    return false;
  }
  /**
   * Indicates that {@link Model#cleanup()}, which is invoked at the end
   * of {@link Model#deriveCounts(CountsTable,danbikel.util.Filter,double,danbikel.util.FlexibleMap)},
   * can safely remove the specified event from the <code>Model</code>
   * object's internal counts tables, as the event is not applicable
   * to any of the probabilities for which the model will produce an estimate.
   * <p>
   * The default implementation simply returns <code>false</code>.
   *
   * @see Model#deriveCounts(CountsTable,danbikel.util.Filter,double,danbikel.util.FlexibleMap)
   * @see Model#cleanup()
   */
  public boolean removeFuture(int backOffLevel, Event future) {
    return false;
  }
  /**
   * Returns <code>true</code> if the specified transition contains
   * either a history or future for which {@link
   * #removeHistory(int,Event)} or {@link #removeFuture(int,Event)}
   * returns <code>true</code>, respectively.
   *
   * @see Model#deriveCounts(CountsTable,danbikel.util.Filter,double,danbikel.util.FlexibleMap)
   * @see Model#cleanup()
   */
  public boolean removeTransition(int backOffLevel, Transition transition) {
    return (removeHistory(backOffLevel, transition.history()) ||
	    removeFuture(backOffLevel, transition.future()));
  }

  /**
   * Returns the recommended cache size for the specified back-off level
   * of the model that uses this probability structure.  This default
   * implementation simply returns <code>topLevelCacheSize / 2^level</code>.
   *
   * @see #topLevelCacheSize
   */
  public int cacheSize(int level) {
    int size = topLevelCacheSize;
    for (int i = 0; i < level; i++)
      size /= 2;
    return size;
  }

  /**
   * Returns the value of the {@link #additionalData} member.
   * @return the value of the {@link #additionalData} member.
   */
  public Object getAdditionalData() { return additionalData; }
  /**
   * Sets the value of the {@link #additionalData} member.
   * @param data an additional data object associated with this probability
   * structure
   */
  public void setAdditionalData(Object data) { additionalData = data; }

  /**
   * Returns a deep copy of this object.  Currently, all data members
   * of <code>ProbabilityStructure</code> objects are used solely as
   * temporary storage during certain method invocations; therefore,
   * this copy method should simply return a new instance of the runtime
   * type of this <code>ProbabilityStructure</code> object, with
   * freshly-created data members that are <i>not</i> deep copies of
   * the data members of this object.  The general contract of the
   * copy method is slightly violated here, but without undue harm,
   * given the lack of persistent data of these types of objects. If a
   * concrete subclass has specific requirements for its data members
   * to be deeply copied, this method should be overridden.
   */
  public abstract ProbabilityStructure copy();

  private void readObject(ObjectInputStream in)
  throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    topLevelCacheSize = getTopLevelCacheSize();
  }
}
