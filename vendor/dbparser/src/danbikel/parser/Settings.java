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

import danbikel.parser.constraints.*;
import danbikel.util.Text;
import danbikel.switchboard.SwitchboardRemote;
import java.net.URL;
import java.util.*;
import java.io.*;

/**
 * Provides static settings for this package, primarily via an internal
 * {@link Properties} object.  All recognized properties of this package and
 * the supplied language packages are provided as publicly-accessible constants.
 * <p/>
 * A settings file for a particular language must provide the property
 * <code>headTablePrefix + language</code>.<br> A settings file for a
 * particular language should normally also provide the property
 * <code>fileEncodingPrefix + language</code> to override the default file
 * encoding as determined by the locale of the Java VM.  Settings files
 * for a particular language and/or Treebank may contain any other settings
 * required by a language package.
 * <p/>
 * Variable expansion is performed on property values as in Java security
 * policy files, with the additional provision that properties defined earlier
 * in a settings file can be used as variable names in subsequent lines of the
 * settings file.  See {@link Text#expandVars(Properties,StringBuffer)} for
 * what variables are allowed in the definitions of property values.
 * <p/>
 * Upon intialization, this class attempts to read default parser settings from
 * the file <tt>settings</tt> inside the default settings directory,
 * <tt>$HOME/.db-parser</tt>, where <tt>$HOME</tt> is ther user's home
 * directory, as defined by the system property <tt>user.home</tt>.  If either
 * the default settings directory or the default settings file is missing,
 * this class will use fallback default settings from a resource that is
 * bundled with this package.
 * <p/>
 * To obtain a default settings file as a template for modification, run
 * the {@link #main} method of this class.
 *
 * @see #headTablePrefix
 * @see #fileEncodingPrefix
 * @see Language#encoding
 * @see #settingsDirOverride
 * @see #settingsFileOverride
 */
public class Settings implements Serializable {

  /**
   * An interface by which to notify a class or an instance of a class after one
   * or more settings have changed.
   *
   * @see Settings#register(Class,Settings.Change,Set)
   * @see Settings#register(Settings.Change)
   */
  public static interface Change {
    /**
     * Invoked by this class to notify the requesting class that one or more
     * settings have changed.
     *
     * @param changedSettings the keys of this map are the settings that have
     *                        changed since the last time this method was
     *                        invoked, and the values are the old values for
     *                        those changed settings
     * @see Settings#register(Class,Settings.Change,Set)
     * @see Settings#register(Settings.Change)
     */
    void update(Map<String, String> changedSettings);
  }


  private Settings() {}

  // constants

  private final static String className = Settings.class.getName();

  /** The official name of this program. */
  public final static String progName = "Parsing Engine";
  /** The official version of this program. */
  public final static String version = "1.2";
  /**
   * The prefix that all properties for this parser should have
   * (to be used when finding system properties that are meant to be included
   * here).
   */
  private final static String globalPropertyPrefix = "parser.";

  /**
   * The name of the property to override the name of the default settings
   * file, which is <tt>&lt;defaultSettingsDir&gt;/settings</tt>, where
   * <tt>&lt;defaultSettingsDir&gt;</tt> is the default settings directory,
   * as described in the documentation for {@link #settingsDirOverride}.
   * <br>
   * Note that using this setting to change the default settings file
   * does <b>not</b> change the default settings directory, which is used
   * by {@link #getFileOrResourceAsStream(Class,String)}.
   */
  public final static String settingsFileOverride = "parser.settingsFile";

  /**
   * The name of the property to override the location of the default settings
   * directory, to be specified at run-time on the command line.  The default
   * settings directory is <tt>$HOME/.db-parser</tt>, where <tt>$HOME</tt> is
   * the user's home directory, as defined by the system property
   * <tt>user.home</tt>.
   * <br>
   * The value of this constant is <code>&quot;parser.settingsDir&quot;</code>.
   * <p>
   * Example UNIX usage:
   * <pre>
   *     java -Dparser.settingsDir=/tmp ...
   * </pre>
   */
  public final static String settingsDirOverride = "parser.settingsDir";

  /**
   * The property to specify the package for {@linkplain ProbabilityStructure
   * model structure classes}.
   *
   * @see #globalModelStructureNumber
   */
  public final static String modelStructurePackage =
    "parser.modelStructure.package";

  /**
   * The property to specify the language to be parsed.
   * <p>
   * The value of this constant is <code>&quot;parser.language&quot;</code>.
   *
   * @see Language
   */
  public final static String language = "parser.language";

  /**
   * The property to specify the language package to be used.
   * <p>
   * The value of this constant is
   * <code>&quot;parser.language.package&quot;</code>.
   *
   * @see Language
   */
  public final static String languagePackage = "parser.language.package";

  /**
   * The property to specify the fully-qualified name of the class that
   * extends {@link WordFeatures} in a language package.  If this property
   * is set, it will override the default, which is
   * <pre>Settings.get(Settings.languagePackage) + ".WordFeatures"</pre>
   * The value of this constant is
   * <code>&quot;parser.language.wordFeatures&quot;</code>.
   *
   * @see Language
   */
  public final static String wordFeaturesClass = "parser.language.wordFeatures";
  /**
   * The property to specify the fully-qualified name of the class that
   * extends {@link Treebank} in a language package.  If this property
   * is set, it will override the default, which is
   * <pre>Settings.get(Settings.languagePackage) + ".Treebank"</pre>
   * The value of this constant is
   * <code>&quot;parser.language.treebank&quot;</code>.
   *
   * @see Language
   */
  public final static String treebankClass = "parser.language.treebank";
  /**
   * The property to specify the fully-qualified name of the class that
   * extends {@link HeadFinder} in a language package.  If this property
   * is set, it will override the default, which is
   * <pre>Settings.get(Settings.languagePackage) + ".HeadFinder"</pre>
   * The value of this constant is
   * <code>&quot;parser.language.headFinder&quot;</code>.
   *
   * @see Language
   */
  public final static String headFinderClass = "parser.language.headFinder";
  /**
   * The property to specify the fully-qualified name of the class that
   * extends {@link Training} in a language package.  If this property
   * is set, it will override the default, which is
   * <pre>Settings.get(Settings.languagePackage) + ".Training"</pre>
   * The value of this constant is
   * <code>&quot;parser.language.training&quot;</code>.
   *
   * @see Language
   */
  public final static String trainingClass = "parser.language.training";


  /**
   * The prefix string used to specify a language's head table
   * property. For example, for English, the head table filename is available
   * by calling
   * <pre>
   *   Settings.get(Settings.headTablePrefix + "english");
   * </pre>
   * The value of this constant is <code>&quot;parser.headtable.&quot;</code>.
   *
   * @see HeadFinder
   */
  public final static String headTablePrefix = "parser.headtable.";

  /**
   * The property to specify the fully-qualified classname of the
   * <code>SubcatFactory</code> object to be used by the <code>Subcats</code>
   * static factory class.
   *
   * @see Subcat
   * @see Subcats
   * @see SubcatFactory
   */
  public final static String subcatFactoryClass = "parser.subcatFactoryClass";

  /**
   * The property to specify the fully-qualified classname of the
   * <code>WordFactory</code> object to be used by the <code>Words</code>
   * static factory class.
   *
   * @see Word
   * @see Words
   * @see WordFactory
   */
  public final static String wordFactoryClass = "parser.wordFactoryClass";

  /**
   * The property to specify the fully-qualified classname of the
   * <code>Shift</code> object to be used by the <code>Shifter</code>
   * static class.
   *
   * @see Shift
   * @see Shifter
   * @see DefaultShifter
   */
  public final static String shifterClass = "parser.shifterClass";

  /**
   * The property to specify the fully-qualified classname of the
   * <code>ConstraintSetFactory</code> object to be used by the
   * <code>ConstraintSets</code> static class.
   *
   * @see ConstraintSets
   * @see ConstraintSet
   * @see Constraint
   */
  public final static String constraintSetFactoryClass =
    "parser.constraintSetFactoryClass";

  /**
   * The property to specify whether the containsVerb predicate should have
   * an additional base case where it should simply return false for
   * NPB nodes.
   */
  public final static String baseNPsCannotContainVerbs =
    "parser.baseNPsCannotContainVerbs";

  /**
   * The prefix string used to specify a language's file encoding
   * property.  For example, for Chinese, the file encoding is available
   * by calling
   * <pre>
   *  Settings.get(Settings.fileEncodingPrefix + "chinese");
   * </pre>
   * The value of this constant is
   * <code>&quot;parser.file.encoding.&quot;</code>.
   *
   * @see Language#encoding
   */
  public final static String fileEncodingPrefix = "parser.file.encoding.";

  /**
   * The property to specify the fully-qualified class name of the
   * <code>Decoder</code> instance to be created for use by
   * <code>Parser</code> and <code>EMParser</code> classes (and any other
   * subclass of <code>Parser</code>).
   * <p>
   * The value of this constant is
   * <code>"parser.parser.decoderClass"</code>.
   *
   * @see Parser#getNewDecoder(int,DecoderServerRemote)
   */
  public final static String decoderClass = "parser.parser.decoderClass";

  /**
   * The property to specify the fully-qualified class name of the
   * <code>DecoderServerRemote</code> instance to be created for use by
   * <code>Parser</code> and <code>EMParser</code> classes (and any other
   * subclass of <code>Parser</code>).  This property is used when
   * the <code>Parser/EMParser</code> class instance is asked to create
   * and/or use its own, internal server.
   * <p>
   * The value of this constant is
   * <code>"parser.parser.decoderServerClass"</code>.
   *
   * @see Parser#getNewDecoderServer(String)
   */
  public final static String decoderServerClass =
    "parser.parser.decoderServerClass";

  /**
   * The property to specify the default {@link Model} class to be created
   * around {@link ProbabilityStructure} objects when their
   * {@link ProbabilityStructure#newModel()} method is invoked.
   * <p>
   * The value of this constant is
   * <code>"parser.probabilityStructure.defaultModelClass"</code>.
   *
   * @see ProbabilityStructure#newModel()
   */
  public final static String defaultModelClass =
    "parser.probabilityStructure.defaultModelClass";

  /**
   * The property to specify whether or not to pre-compute probabilities
   * when training and use those pre-computed probabilities when decoding.
   * <p>
   * The value of this constant is
   * <code>"parser.model.precomputeProbabilities"</code>.
   */
  public final static String precomputeProbs =
    "parser.model.precomputeProbabilities";

  /**
   * The property to specify whether to perform deficient estimation of
   * probabilities (as per Mike Collins' bug in his thesis parser).
   * Specifically, if this property is <tt>true</tt>, then all
   * deleted-interpolation probabilities are estimated using a lambda
   * weight for the final level of back-off, as per for the formula
   * <blockquote>
   * <i>&lambda;</i><sub>1</sub>&sdot;<i>e</i><sub>1</sub> +
   * (1 &minus; <i>&lambda;</i><sub>1</sub>) &sdot;
   * (<i>&lambda;</i><sub>2</sub>&sdot;<i>e</i><sub>2</sub> +
   * (1 &minus; <i>&lambda;</i><sub>2</sub>) &sdot;
   * <i>&lambda;</i><sub>3</sub>&sdot;<i>e</i><sub>3</sub>)
   * </blockquote>
   * where <i>&lambda;<sub>i</sub></i> is the smoothing weight for backoff
   * level <i>i</i> and <i>e<sub>i</sub></i> is an estimate for backoff
   * level <i>i</i>.  If this property is <tt>false</tt>, then the formula
   * is estimated in the correct fashion:
   * <blockquote>
   * <i>&lambda;</i><sub>1</sub>&sdot;<i>e</i><sub>1</sub> +
   * (1 &minus; <i>&lambda;</i><sub>1</sub>) &sdot;
   * (<i>&lambda;</i><sub>2</sub>&sdot;<i>e</i><sub>2</sub> +
   * (1 &minus; <i>&lambda;</i><sub>2</sub>) &sdot; <i>e</i><sub>3</sub>)
   * </blockquote>
   * <p>
   * The value of this constant is
   * <code>"parser.model.collinsDeficientEstimation"</code>.
   */
  public final static String collinsDeficientEstimation =
    "parser.model.collinsDeficientEstimation";

  /**
   *
   */
  public final static String saveSmoothingParams =
    "parser.model.saveSmoothingParameters";

  /**
   * Indicates whether instances of {@link Model}, when smoothing parameters
   * from a previous training run, should not add new parameters when deriving
   * counts.
   */
  public final static String dontAddNewParams =
    "parser.model.dontAddNewParameters";

  /**
   * Indicates whether instances of {@link Model} should use smoothing
   * parameters saved to a file from a previous training run, instead of
   * deriving smoothing parameters.
   */
  public final static String useSmoothingParams =
    "parser.model.useSmoothingParameters";

  /**
   * The property to specify the directory from which {@link Model} objects
   * are to read smoothing parameters files.
   */
  public final static String smoothingParamsDir =
    "parser.model.smoothingParametersDir";

  /**
   * The property to specify whether to prune redundant parameters from
   * every {@link Model} instance.
   */
  public final static String modelDoPruning =
    "parser.model.doPruning";

  /**
   * The property to specify the pruning threshold when pruning is performed
   * (ignored if {@link #modelDoPruning} is <tt>false</tt>).  The value of this
   * property should be (the string representation of) a <tt>double</tt>.
   *
   * @see #modelDoPruning
   */
  public final static String modelPruningThreshold =
    "parser.model.pruningThreshold";

  /**
   * The property to specify the concrete type of the {@link NonterminalMapper}
   * instance used by {@link NTMapper} to map nonterminals that are
   * previously-generated modifiers of some head child nonterminal.
   */
  public final static String prevModMapperClass =
    "parser.model.prevModMapperClass";
  
  /**
   * The property to specify whether or not the <code>ModelCollection</code>
   * class should write out the large hash map containing canonical versions of
   * <code>Event</code> objects when it is serialized (that is, saved to a
   * file).  When decoding using caches instead of precomputed probabilities
   * (see {@link #precomputeProbs}), the use of the canonical events table
   * saves time by allowing the decoder to put canonical events observed
   * during training into the caches, instead of always having to create a
   * canonical events table anew during decoding.  Accordingly, when
   * <code>precomputeProbs</code> is set to <tt>false</tt>, the value of this
   * property should usually be <tt>true</tt>, except when debugging.  When
   * <code>precomputeProbs</code> is <tt>false</tt> <i><b>and</b></i> the value
   * of this property is also <tt>false</tt>, then the
   * <code>ModelCollection</code> object used during training will simply write
   * out an empty canonical events table, to be read in when the
   * <code>ModelCollection</code> object is de-serialized just prior to
   * decoding, meaning that as events are cached, they will need to be copied
   * on the fly to the canonical events table.  Finally, when
   * <code>precomputeProbs</code> is <tt>true</tt>, this property is ignored.
   * <p>
   * The value of this property should be (the string representation of) a
   * boolean (conversion is performed by the method
   * <code>Boolean.valueOf</code>).
   * <p>
   * The value of this constants is
   * <code>&quot;parser.modelCollection.writeCanonicalEvents&quot;</code>.
   *
   * @see #precomputeProbs
   * @see ModelCollection
   */
  public final static String writeCanonicalEvents =
    "parser.modelCollection.writeCanonicalEvents";

  /**
   * The property to specify whether the method
   * {@link
   * danbikel.parser.lang.AbstractHeadFinder#defaultFindHead(danbikel.lisp.Symbol,danbikel.lisp.SexpList)}
   * issues a warning whenever it needs to use the default head-finding rule.
   * The value of this property should be (the string representation of) a
   * boolean (conversion is performed by the method
   * <code>Boolean.valueOf</code>).
   * <p>
   * The value of this constant is
   * <code>"parser.headfinder.warnDefaultRule"</code>.
   */
  public final static String headFinderWarnDefaultRule =
    "parser.headfinder.warnDefaultRule";

  /**
   * The property to specify a probability that the method
   * {@link
   * danbikel.parser.lang.AbstractHeadFinder#defaultFindHead(danbikel.lisp.Symbol,danbikel.lisp.SexpList)}
   * should return a randomly-selected head-child index.  The value of
   * this property should be (the string representation of) a <tt>double</tt>
   * (conversion is performed by the method <code>Double.parseDouble</code>).
   * <br>
   * <b>N.B.</b>: Head-finding for <tt>NP</tt>s that are not <tt>NPB</tt>s is
   * unaffected by this setting, meaning that heuristics are
   * <i><b>always</b></i> used to find heads within non-<tt>NPB</tt> noun
   * phrases.  This is so that {@link Training#addBaseNPs(danbikel.lisp.Sexp)}
   * will always produce consistent results.  The issue is that adding base
   * <tt>NP</tt>s normally relies on head finding (see {@link
   * danbikel.parser.lang.AbstractTraining#needToAddNormalNPLevel(danbikel.lisp.Sexp,int,danbikel.lisp.Sexp)}).
   * <p>
   * The value of this constant is
   * <code>&quot;parser.headfinder.randomProb&quot;</code>.
   *
   * @see danbikel.parser.lang.AbstractTraining#needToAddNormalNPLevel(danbikel.lisp.Sexp,int,danbikel.lisp.Sexp)
   */
  public final static String headFinderRandomProb =
    "parser.headfinder.randomProb";

  /**
   * Property to specify whether <tt>Training.addGapInformation(Sexp)</tt>
   * threads gap information or simply leaves the training trees untouched.
   * The value of this property should be (the string representation of)
   * a boolean (conversion is performed by the method
   * <code>Boolean.valueOf</code>).
   * <p>
   * The value of this constant is
   * <code>"parser.training.addGapInfo"</code>.
   *
   * @see Training#addGapInformation(danbikel.lisp.Sexp)
   */
  public static final String addGapInfo =
    "parser.training.addGapInfo";

  /**
   * The property to specify whether <tt>Training.identifyArguments(Sexp)</tt>
   * should relabel head children as arguments.  Such relabeling is
   * unnecessary, since head children are already inherently distinct from
   * other children; however, it is performed (and is possibly a bug) in
   * Collins' parser, and so is available as a setting here in order to emulate
   * that model.
   *
   * @see Training#identifyArguments(danbikel.lisp.Sexp)
   */
  public static final String collinsRelabelHeadChildrenAsArgs =
    "parser.training.collinsRelabelHeadChildrenAsArgs";

  /**
   * The property to specify whether
   * <tt>Training.repairBaseNPs(Sexp)</tt> alters the training tree or
   * leaves it untouched.  If the value of this property is
   * <tt>false</tt>, then the tree is untouched.  The value of this
   * property should be (the string representation of) a boolean
   * (conversion is performed by the method
   * <code>Boolean.valueOf</code>).
   * <p>
   * The value of this constant is
   * <code>"parser.training.collinsRepairBaseNPs"</code>.
   *
   * @see Training#repairBaseNPs(danbikel.lisp.Sexp)
   */
  public static final String collinsRepairBaseNPs =
    "parser.training.collinsRepairBaseNPs";

  /**
   * The property to indicate whether the trainer should share counts among
   * various models' back-off levels.
   */
  public final static String trainerShareCounts =
    "parser.trainer.shareCounts";

  /**
   * The property to specify the threshold below which words are
   * considered unknown by the trainer. The value of this property
   * must be (the string representation of) an integer.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.unknownWordThreshold"</code>.
   *
   * @see Trainer
   */
  public final static String unknownWordThreshold =
    "parser.trainer.unknownWordThreshold";

  /**
   * The property to specify the threshold below which <code>TrainerEvent</code>
   * objects are discarded by the trainer.  The value of this property
   * must be (the string representation of) a floating-point number.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.countThreshold"</code>.
   *
   * @see Trainer
   */
  public final static String countThreshold =
    "parser.trainer.countThreshold";

  /**
   * The property to specify the threshold below which <code>Event</code>
   * objects are discarded by the databases contained with <code>Model</code>
   * objects.  The value of this property must be (the string representation
   * of) a floating-point number.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.derivedCountThreshold"</code>.
   *
   * @see Trainer
   */
  public final static String derivedCountThreshold =
    "parser.trainer.derivedCountThreshold";

  /**
   * The property to specify the interval (in number of sentences) at which the
   * trainer emits reports to <code>System.err</code> when training.
   * The value of this property must be (the string representation of)
   * an integer.
   * <p>
   * The value of this constant is
   * <code>&quot;parser.trainer.reportingInterval&quot;</code>.
   *
   * @see Trainer
   */
  public final static String trainerReportingInterval =
    "parser.trainer.reportingInterval";

  /**
   * The property to specify whether or not the trainer keeps all words.
   * Normally, words falling below a threshold are mapped to the unknown word.
   * The value of this property should be (the string representation of) a
   * boolean (conversion is performed by the method
   * <code>Boolean.valueOf</code>).
   * <p>
   * The value of this constant is
   * <code>&quot;parser.trainer.keepAllWords&quot;</code>.
   *
   * @see #unknownWordThreshold
   * @see Trainer
   */
  public final static String keepAllWords = "parser.trainer.keepAllWords";

  /**
   * The property to specify whether the trainer includes
   * low-frequency words in its part of speech map.  Normally, such
   * low-frequency words get converted to word-feature vectors, and it
   * is <i>only</i> those vector-tag pairs that get added to the part
   * of speech map.  If this property is set to be <tt>true</tt>,
   * however, mappings from the original words to their parts of
   * speech will also be added to the part of speech map.
   * <p>
   * The value of this constant is
   * <code>&quot;parser.trainer.keepLowFreqTags&quot;</code>.
   *
   * @see #useLowFreqTags
   * @see Trainer
   */
  public final static String keepLowFreqTags = "parser.trainer.keepLowFreqTags";

  /**
   * The property to specify how many previous modifiers the trainer outputs
   * for its top-level count files.  The value of this property must be
   * (the string representation of) a non-negative integer.
   * <p>
   * The value of this constant is
   * <code>&quot;parser.trainer.numPrevMods&quot;</code>.
   * @see Trainer
   */
  public final static String numPrevMods =
    "parser.trainer.numPrevMods";

  /**
   * The property to specify how many head words of previous modifiers the
   * trainer outputs for its top-level count files.  The value of this
   * property must be (the string representation of) a non-negative integer.
   * <p>
   * The value of this constant is
   * <code>&quot;parser.trainer.numPrevWords&quot;</code>.
   * @see Trainer
   */
  public final static String numPrevWords =
    "parser.trainer.numPrevWords";

  /**
   * The property to specify the model structure number to use when
   * creating <code>ProbabilityStructure</code> objects.  The model
   * structure number will be appended to the end of the canonical
   * names of model structure class name prefixes.  For example, the
   * canonical class name prefix for the head generation model
   * structure is <code>HeadModelStructure</code>, so if this property
   * has a value of <tt>"1"</tt>, then the head model structure
   * instantiated by {@link Trainer#deriveCounts} will be the class
   * <code>HeadModelStructure1</code>.  The model structure numbers
   * for specific model structure classes can be overridden by
   * specifying one of the following, model structure-specific properties:
   * <ul>
   * <li> {@link #topNonterminalModelStructureNumber}
   * <li> {@link #topLexModelStructureNumber}
   * <li> {@link #headModelStructureNumber}
   * <li> {@link #gapModelStructureNumber}
   * <li> {@link #leftSubcatModelStructureNumber}
   * <li> {@link #rightSubcatModelStructureNumber}
   * <li> {@link #modNonterminalModelStructureNumber}
   * <li> {@link #modWordModelStructureNumber}
   * <li> {@link #lexPriorModelStructureNumber}
   * <li> {@link #nonterminalPriorModelStructureNumber}
   * </ul>
   * If the user wishes to use model structure classes outside
   * this package, the following properties may be used to specify
   * fully-qualified model structure classnames, which will override all model
   * structure number property settings (the canonical classname prefixes
   * will not be used):
   * <ul>
   * <li> {@link #topNonterminalModelStructureClass}
   * <li> {@link #topLexModelStructureClass}
   * <li> {@link #headModelStructureClass}
   * <li> {@link #gapModelStructureClass}
   * <li> {@link #leftSubcatModelStructureClass}
   * <li> {@link #rightSubcatModelStructureClass}
   * <li> {@link #modNonterminalModelStructureClass}
   * <li> {@link #modWordModelStructureClass}
   * <li> {@link #lexPriorModelStructureClass}
   * <li> {@link #nonterminalPriorModelStructureClass}
   * </ul>
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.globalModelStructureNumber"</code>.
   */
  public final static String globalModelStructureNumber =
    "parser.trainer.globalModelStructureNumber";

  /**
   * The properth to specify the model structure to use when creating
   * the <code>ProbabilityStructure</code> object for the
   * lexical prior submodel.  This number will be appended to the
   * canonical lexical prior model structure classname prefix,
   * <code>"danbikel.parser.LexPriorModelStructure"</code>, to form a
   * classname, such as
   * <code>"danbikel.parser.LexPriorModelStructure1"</code>. This
   * constant overrides the setting of the
   * <code>globalModelStructureNumber</code> property.
   * <p/>
   * The value of this constant is
   * <code>"parser.trainer.lexPriorModelStructureNumber"</code>.
   *
   * @see #globalModelStructureNumber
   * @see #lexPriorModelStructureClass
   */
  public final static String lexPriorModelStructureNumber =
    "parser.trainer.lexPriorModelStructureNumber";

  /**
   * The properth to specify the model structure to use when creating
   * the <code>ProbabilityStructure</code> object for the
   * nonterminal prior submodel.  This number will be appended to the
   * canonical nonterminal prior model structure classname prefix,
   * <code>"danbikel.parser.LexPriorModelStructure"</code>, to form a
   * classname, such as
   * <code>"danbikel.parser.NonterminalPriorModelStructure1"</code>. This
   * constant overrides the setting of the
   * <code>globalModelStructureNumber</code> property.
   * <p/>
   * The value of this constant is
   * <code>"parser.trainer.nonterminalPriorModelStructureNumber"</code>.
   *
   * @see #globalModelStructureNumber
   * @see #nonterminalPriorModelStructureClass
   */
  public final static String nonterminalPriorModelStructureNumber =
    "parser.trainer.nonterminalPriorModelStructureNumber";

  /**
   * The property to specify the model structure number to use when
   * creating the <code>ProbabilityStructure</code> object for the
   * head-generation submodel for heads whose parents are
   * {@link Training#topSym()}.  This number will be appended to the
   * canonical top nonterminal model structure classname prefix,
   * <code>"danbikel.parser.TopNonterminalModelStructure"</code>, to form a
   * classname, such as
   * <code>"danbikel.parser.TopNonterminalModelStructure1"</code>.  This
   * constant overrides the setting of the
   * <code>globalModelStructureNumber</code> property.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.topNonterminalModelStructureNumber"</code>.
   *
   * @see #globalModelStructureNumber
   * @see #topNonterminalModelStructureClass
   */
  public final static String topNonterminalModelStructureNumber =
    "parser.trainer.topNonterminalModelStructureNumber";

  /**
   * The property to specify the model structure number to use when
   * creating the <code>ProbabilityStructure</code> object for the
   * head word-generation submodel for heads of entire sentences.  This
   * number will be appended to the canonical top lexical model
   * structure classname prefix,
   * <code>"danbikel.parser.TopLexModelStructure"</code>, to form a
   * classname, such as
   * <code>"danbikel.parser.TopLexModelStructure1"</code>.  This
   * constant overrides the setting of the
   * <code>globalModelStructureNumber</code> property.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.topLexModelStructureNumber"</code>.
   *
   * @see #globalModelStructureNumber
   * @see #topLexModelStructureClass
   */
  public final static String topLexModelStructureNumber =
    "parser.trainer.topLexModelStructureNumber";

  /**
   * The property to specify the model structure number to use when
   * creating the <code>ProbabilityStructure</code> object for the
   * head-generation submodel.  This number will be appended to the
   * canonical head model structure classname prefix,
   * <code>"danbikel.parser.HeadModelStructure"</code>, to form a
   * classname, such as
   * <code>"danbikel.parser.HeadModelStructure1"</code>.  This
   * constant overrides the setting of the
   * <code>globalModelStructureNumber</code> property.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.headModelStructureNumber"</code>.
   *
   * @see #globalModelStructureNumber
   * @see #headModelStructureClass
   */
  public final static String headModelStructureNumber =
    "parser.trainer.headModelStructureNumber";
  /**
   * The property to specify the model structure number to use when
   * creating the <code>ProbabilityStructure</code> object for the
   * gap-generation submodel.  This number will be appended to the
   * canonical gap model structure classname prefix,
   * <code>"danbikel.parser.GapModelStructure"</code>, to form a
   * classname, such as
   * <code>"danbikel.parser.GapModelStructure1"</code>.  This constant
   * overrides the setting of the
   * <code>globalModelStructureNumber</code> property.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.gapModelStructureNumber"</code>.
   *
   * @see #globalModelStructureNumber
   * @see #gapModelStructureClass
   */
  public final static String gapModelStructureNumber =
    "parser.trainer.gapModelStructureNumber";
  /**
   * The property to specify the model structure number to use when
   * creating the <code>ProbabilityStructure</code> object for the
   * left-subcat-generation submodel.  This number will be appended to
   * the canonical left-subcat model structure classname prefix,
   * <code>"danbikel.parser.LeftSubcatModelStructure"</code>, to form
   * a classname, such as
   * <code>"danbikel.parser.LeftSubcatModelStructure1"</code>.  This
   * constant overrides the setting of the
   * <code>globalModelStructureNumber</code> property.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.leftSubcatModelStructureNumber"</code>.
   *
   * @see #globalModelStructureNumber
   * @see #leftSubcatModelStructureClass
   */
  public final static String leftSubcatModelStructureNumber =
    "parser.trainer.leftSubcatModelStructureNumber";
  /**
   * The property to specify the model structure number to use when
   * creating the <code>ProbabilityStructure</code> object for the
   * right-subcat-generation submodel.  This number will be appended
   * to the canonical right-subcat model structure classname prefix,
   * <code>"danbikel.parser.RightSubcatModelStructure"</code>, to form
   * a classname, such as
   * <code>"danbikel.parser.RightSubcatModelStructure1"</code>.  This
   * constant overrides the setting of the
   * <code>globalModelStructureNumber</code> property.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.rightSubcatModelStructureNumber"</code>.
   *
   * @see #globalModelStructureNumber
   * @see #rightSubcatModelStructureClass
   */
  public final static String rightSubcatModelStructureNumber =
    "parser.trainer.rightSubcatModelStructureNumber";
  /**
   * The property to specify the model structure number to use when
   * creating the <code>ProbabilityStructure</code> object for the
   * modifying nonterminal-generation submodel.  This number will be
   * appended to the canonical modifying nonterminal model structure
   * classname prefix,
   * <code>"danbikel.parser.ModNonterminalModelStructure"</code>, to
   * form a classname, such as
   * <code>"danbikel.parser.ModNonterminalModelStructure1"</code>.
   * This constant overrides the setting of the
   * <code>globalModelStructureNumber</code> property.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.modNonterminalModelStructureNumber"</code>.
   *
   * @see #globalModelStructureNumber
   * @see #modNonterminalModelStructureClass
   */
  public final static String modNonterminalModelStructureNumber =
    "parser.trainer.modNonterminalModelStructureNumber";
  /**
   * The property to specify the model structure number to use when
   * creating the <code>ProbabilityStructure</code> object for the
   * modifying word-generation submodel.  This number will be appended
   * to the canonical modifying word model structure classname prefix,
   * <code>"danbikel.parser.ModWordModelStructure"</code>, to form a
   * classname, such as
   * <code>"danbikel.parser.ModWordModelStructure1"</code>.  This
   * constant overrides the setting of the
   * <code>globalModelStructureNumber</code> property.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.modWordModelStructureNumber"</code>.
   *
   * @see #globalModelStructureNumber
   * @see #modWordModelStructureClass
   */
  public final static String modWordModelStructureNumber =
    "parser.trainer.modWordModelStructureNumber";

  /**
   * The property to specify the fully-qualified name of a class that
   * extends <code>ProbabilityStructure</code>, to be instantiated by
   * {@link Trainer} for the lexical prior submodel.  Specifying this
   * property overrides the {@link #globalModelStructureNumber}
   * and {@link #lexPriorModelStructureNumber} properties.
   * <p/>
   * The value of this constant is
   * <code>"parser.trainer.lexPriorModelStructureClass"</code>.
   */
  public final static String lexPriorModelStructureClass =
    "parser.trainer.lexPriorModelStructureClass";

  /**
   * The property to specify the fully-qualified name of a class that
   * extends <code>ProbabilityStructure</code>, to be instantiated by
   * {@link Trainer} for the nonterminal prior submodel.  Specifying this
   * property overrides the {@link #globalModelStructureNumber}
   * and {@link #nonterminalPriorModelStructureNumber} properties.
   * <p/>
   * The value of this constant is
   * <code>"parser.trainer.nonterminalPriorModelStructureClass"</code>.
   */
  public final static String nonterminalPriorModelStructureClass =
    "parser.trainer.nonterminalPriorModelStructureClass";

  /**
   * The property to specify the fully-qualified name of a class that
   * extends <code>ProbabilityStructure</code>, to be instantiated by
   * {@link Trainer} for the head-generation submodel for heads whose parents
   * are {@link Training#topSym()}.  Specifying this
   * property overrides the {@link #globalModelStructureNumber} and
   * {@link #topNonterminalModelStructureNumber} properties.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.topNonterminalModelStructureClass"</code>.
   */
  public final static String topNonterminalModelStructureClass =
    "parser.trainer.topNonterminalModelStructureClass";

  /**
   * The property to specify the fully-qualified name of a class that
   * extends <code>ProbabilityStructure</code>, to be instantiated by
   * {@link Trainer} for the head word-generation submodel for head
   * words of entire sentences.  Specifying this
   * property overrides the {@link #globalModelStructureNumber} and
   * {@link #topLexModelStructureNumber} properties.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.topLexModelStructureClass"</code>.
   */
  public final static String topLexModelStructureClass =
    "parser.trainer.topLexModelStructureClass";

  /**
   * The property to specify the fully-qualified name of a class that
   * extends <code>ProbabilityStructure</code>, to be instantiated by
   * {@link Trainer} for the head-generation submodel.  Specifying this
   * property overrides the {@link #globalModelStructureNumber} and
   * {@link #headModelStructureNumber} properties.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.headModelStructureClass"</code>.
   */
  public final static String headModelStructureClass =
    "parser.trainer.headModelStructureClass";
  /**
   * The property to specify the fully-qualified name of a class that
   * extends <code>ProbabilityStructure</code>, to be instantiated by
   * {@link Trainer} for the gap-generation submodel.  Specifying this
   * property overrides the {@link #globalModelStructureNumber} and
   * {@link #gapModelStructureNumber} properties.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.gapModelStructureClass"</code>.
   */
  public final static String gapModelStructureClass =
    "parser.trainer.gapModelStructureClass";
  /**
   * The property to specify the fully-qualified name of a class that
   * extends <code>ProbabilityStructure</code>, to be instantiated by
   * {@link Trainer} for the left subcat-generation submodel.  Specifying this
   * property overrides the {@link #globalModelStructureNumber} and
   * {@link #leftSubcatModelStructureNumber} properties.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.leftSubcatModelStructureClass"</code>.
   */
  public final static String leftSubcatModelStructureClass =
    "parser.trainer.leftSubcatModelStructureClass";
  /**
   * The property to specify the fully-qualified name of a class that
   * extends <code>ProbabilityStructure</code>, to be instantiated by
   * {@link Trainer} for the right subcat-generation submodel.  Specifying this
   * property overrides the {@link #globalModelStructureNumber} and
   * {@link #rightSubcatModelStructureNumber} properties.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.rightSubcatModelStructureClass"</code>.
   */
  public final static String rightSubcatModelStructureClass =
    "parser.trainer.rightSubcatModelStructureClass";
  /**
   * The property to specify the fully-qualified name of a class that
   * extends <code>ProbabilityStructure</code>, to be instantiated by
   * {@link Trainer} for the modifying nonterminal-generation submodel.
   * Specifying this property overrides the {@link #globalModelStructureNumber}
   * and {@link #modNonterminalModelStructureNumber} properties.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.modNonterminalModelStructureClass"</code>.
   */
  public final static String modNonterminalModelStructureClass =
    "parser.trainer.modNonterminalModelStructureClass";
  /**
   * The property to specify the fully-qualified name of a class that
   * extends <code>ProbabilityStructure</code>, to be instantiated by
   * {@link Trainer} for the modifying word-generation submodel.
   * Specifying this property overrides the {@link #globalModelStructureNumber}
   * and {@link #modWordModelStructureNumber} properties.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.modWordModelStructureClass"</code>.
   */
  public final static String modWordModelStructureClass =
    "parser.trainer.modWordModelStructureClass";

  /**
   * The property to specify whether certain sentences are skipped
   * during training on sections 02 through 21 of the Penn Treebank Wall
   * Street Journal corpus in order to mimic Mike Collins' trainer on this
   * now-standard training corpus.  This property should not be set to
   * <tt>true</tt> if training on any corpus other than the Penn Treebank
   * Wall Street Journal corpus.
   * <p>
   * The value of this constants is
   * <code>"parser.trainer.collinsSkipWSJSentences"</code>.
   */
  public final static String collinsSkipWSJSentences =
    "parser.trainer.collinsSkipWSJSentences";

  /**
   * The property to specify how many events the trainer should read
   * from an observations file before deriving counts (for use only
   * when using a trainer output file; see {@link Trainer#main(String[])}).
   * The value of this property should be (the string representation of)
   * an integer.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.maxEventChunkSize"</code>.
   */
  public final static String maxEventChunkSize =
    "parser.trainer.maxEventChunkSize";

  /**
   * The property to specify whether the trainer should output the
   * head-to-parent nonterminal map that it derives from its top-level
   * observations.  This property should be (the string representation of)
   * a boolean.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.outputHeadToParentMap"</code>.
   */
  public final static String outputHeadToParentMap =
    "parser.trainer.outputHeadToParentMap";

  /**
   * The property to specify whether the trainer should output the subcat
   * maps that it derives from its top-level observations.  This property
   * should be (the string representation of) a boolean.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.outputSubcatMaps"</code>.
   */
  public final static String outputSubcatMaps =
    "parser.trainer.outputSubcatMaps";

  /**
   * The property to specify whether the trainer should output the
   * modifying nonterminal map that it derives from its top-level
   * observations.  This property should be (the string representation of)
   * a boolean.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.outputModNonterminalMap"</code>.
   */
  public final static String outputModNonterminalMap =
    "parser.trainer.outputModNonterminalMap";

  /**
   * The property to specify whether the trainer outputs top-level events
   * in the format output by Michael Collins' trainer to
   * <code>System.out</code> when training on a Treebank input file.  Note
   * that in order to (near) perfectly emulate Collins' trainer, the property
   * {@link #unknownWordThreshold} should be set to 1.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.outputCollins"</code>.
   */
  public final static String outputCollins =
    "parser.trainer.outputCollins";

  /**
   * The property to specify the fully-qualified name of the subclass
   * of {@link Item} to be used for chart items.
   * <p>
   * The value of this constant is
   * <code>"parser.chart.itemClass"</code>.
   */
  public final static String chartItemClass =
    "parser.chart.itemClass";

  /**
   * The property to specify whether the chart should add 3 in natural
   * log-space to the beam width for chart items whose root labels are either
   * <tt>NP</tt> or <tt>NP-A</tt>, as is done by Collins' parser.  That is, if
   * the normal beam width is <tt>exp(-B)</tt>, then this hack makes the beam
   * for NP/NP-A chart items expand to <tt>exp(-(B+3))</tt>.
   * <p>
   * The value of this constant is
   * <code>"parser.chart.collinsNPPruneHack"</code>.
   */
  public final static String collinsNPPruneHack =
    "parser.chart.collinsNPPruneHack";

  /**
   * The property to specify the maximum number of top-scoring theories to give
   * as a parse. If this property is equals to 1, then the returned item from
   * the decoder will be a single tree; if it is greater than 1, it will be a
   * list of trees (i.e., a list of lists).  This property should be (the
   * string representation of) an integer greater than or equal to 1.
   * <p>
   * The value of this constant is
   * <code>"parser.decoder.kBest"</code>.
   *
   * @see Decoder#parse(danbikel.lisp.SexpList)
   */
  public final static String kBest =
    "parser.decoder.kBest";

  /**
   * The property to specify the maximum length a sentence can be;
   * sentences greater than this length will not be parsed.
   * If the value of this property is less than <tt>1</tt>, the
   * decoder will attempt to parse all sentences. This property should
   * be (the string representation of) an integer.
   * <p>
   * The value of this constant is
   * <code>"parser.decoder.maxSentenceLength"</code>.
   * <p>
   *
   * @see Decoder
   */
  public final static String maxSentLen =
    "parser.decoder.maxSentenceLength";

  /**
   * The property to specify the maximum time, in milliseconds, that the
   * decoder will attempt to deliver a parse on a sentence.  If this
   * property is set to a value less than or equal to zero, the maximum
   * parsing time will be infinite (i.e., there will not be a time-out).
   * This property should be (the string representation of) an integer.
   * <p>
   * The value of this constant is
   * <code>"parser.decoder.maxParseTime"</code>.
   * <p>
   *
   * @see Decoder
   */
  public final static String maxParseTime =
    "parser.decoder.maxParseTime";

  /**
   * The property to specify whether to use tags collected from
   * low-frequency words by the trainer when seeding the chart, if the
   * current word is a low-frequency word observed when training.  If
   * <code>true</code>, and if {@link #keepLowFreqTags} was true
   * during training, causes the decoder to attempt to find tags
   * observed with this word in training, even if it was a
   * low-frequency word.  If <code>false</code>, the decoder will
   * simply choose the first-best tag supplied by the input sentence,
   * or, if the input does not contain pre-tagged words, will use all
   * tags observed with the word's feature vector.
   * <p>
   * The value of this constant is
   * <code>"parser.decoder.useLowFreqTags"</code>.
   *
   * @see #keepLowFreqTags
   * @see Decoder
   */
  public final static String useLowFreqTags = "parser.decoder.useLowFreqTags";

  /**
   * The property to specify whether or not the decoding algorithm should
   * prune away chart entries within a particular factor of the top-ranked
   * chart entry in a given cell.  The value of this property should be (the
   * string representation of) a boolean (conversion is performed by the method
   * <code>Boolean.valueOf</code>).
   * <p>
   * The value of this constant is
   * <code>"parser.decoder.usePruneFactor"</code>.
   * <p>
   *
   * @see Decoder
   */
  public final static String decoderUsePruneFactor =
    "parser.decoder.usePruneFactor";

  /**
   * The property to specify the factor by which the decoder should prune
   * away chart entries.  The value of this property should be a floating
   * point number that is the logarithm (base 10) of the desired factor
   * (i.e., the factor employed will effectively be
   * <code>Math.pow(value, 10.0)</code>, where <code>value</code> is the value
   * of this property).  This form of pruning will only occur if the value of
   * {@link #decoderUsePruneFactor} is <code>true</code>.
   * <p>
   * The value of this constant is
   * <code>"parser.decoder.pruneFactor"</code>.
   * <p>
   *
   * @see Decoder
   */
  public final static String decoderPruneFactor =
    "parser.decoder.pruneFactor";

  /**
   * The property to specify the maximum prune factor when performing
   * beam-widening.  Beam-widening is when the decoder tries successively
   * wider beams until there is a parse for the current sentence, or until
   * the maximum prune factor is reached.  The initial beam tried will be
   * the value of {@link #decoderPruneFactor}.  If this property is not set
   * (the value is the <tt>null</tt> object), then the maximum prune factor
   * defaults to the value of {@link #decoderPruneFactor}, and the decoder
   * will not do beam-widening. The value of this property should be a floating
   * point number that is the logarithm (base 10) of the desired factor
   * (i.e., the factor employed will effectively be
   * <code>Math.pow(value, 10.0)</code>, where <code>value</code> is the value
   * of this property).
   *
   * @see #decoderPruneFactor
   * @see #decoderPruneFactorIncrement
   */
  public final static String decoderMaxPruneFactor =
    "parser.decoder.maxPruneFactor";

  /**
   * The property to specify the increment used when the decoder does
   * beam-widening.  The value of this property should be a floating
   * point number that is the logarithm (base 10) of the desired
   * beam increment (i.e., the increment employed will effectively be
   * <code>Math.pow(value, 10.0)</code>, where <code>value</code> is the value
   * of this property).
   *
   * @see #decoderMaxPruneFactor
   */
  public final static String decoderPruneFactorIncrement =
    "parser.decoder.pruneFactorIncrement";

  /**
   * The property to specify whether the decoder should relax all hard
   * constraints (except the comma pruning rule, which is controlled by the
   * {@link #decoderUseCommaConstraint} setting) after performing all beam
   * widening.  Setting this property to <tt>true</tt> provides a means to
   * making the parser more robust, as it <i>should</i> allow the decoder to
   * always find at least <i>some</i> parse for any sentence.
   * <p/>
   * For the purposes of this setting, a <i>hard constraint</i> is any implicit
   * or explicit zero probability estimate that would cause the decoder to
   * abandon an hypothesis (derivation).  Note that setting this property to
   * <tt>true</tt> has no effect on the constraint-satisfaction system provided
   * by {@link #constraintSetFactoryClass}.
   */
  public final static String decoderRelaxConstraintsAfterBeamWidening =
    "parser.decoder.relaxConstraintsAfterBeamWidening";
  
  /**
   * The property to specify whether the decoder should impose a limit on the
   * number of chart items per cell in the chart.  The value of this property
   * should be (the string representation of) a boolean (conversion is
   * performed by the method <code>Boolean.valueOf</code>).
   * <p>
   * The value of this constant is
   * <code>"parser.decoder.useCellLimit"</code>.
   * <p>
   *
   * @see Decoder
   */
  public final static String decoderUseCellLimit =
    "parser.decoder.useCellLimit";

  /**
   * The property to specify the limit on the number of chart items the decoder
   * will have per cell in its chart.  This type of pruning will only occur
   * if the value of {@link #decoderUseCellLimit} is <code>true</code>.
   * The value of this property should be (the string representation of) an
   * integer.
   * <p>
   * The value of this constant is
   * <code>"parser.decoder.cellLimit"</code>.
   * <p>
   *
   * @see Decoder
   */
  public final static String decoderCellLimit =
    "parser.decoder.cellLimit";

  /**
   * The property to specify whether the decoder should employ a constraint
   * on the way commas can appear in and around chart items.  Specifically,
   * the constraint is that for a chart item that represents the CFG rule
   * <pre>Z --> &lt;.. X Y..&gt;</pre>
   * two of its children <tt>X</tt> and <tt>Y</tt> are separated by a comma,
   * then the last word in <tt>Y</tt> must be directly followed by a comma or
   * must be the last word in the sentence.
   */
  public final static String decoderUseCommaConstraint =
    "parser.decoder.useCommaConstraint";

  /**
   * The property to specify whether the decoder should only use the tags
   * supplied with words in an input file when seeding the chart.  Normally,
   * when a list of tags is supplied with every word in every input sentence,
   * the supplied tags are only used with unknown words; for a known word,
   * the possible tags are taken to be those with which the word was observed
   * in training.  A run-time error will occur if this setting is
   * <tt>true</tt> but the input file of sentences does not contain at least
   * one tag per word.
   * <p>
   * The value of this property should be (the string representation of)
   * a boolean (conversion is performed by the method
   * <code>Boolean.valueOf</code>).
   * <p>
   * The value of this constant is
   * <code>"parser.decoder.useOnlySuppliedTags"</code>.
   */
  public final static String decoderUseOnlySuppliedTags =
    "parser.decoder.useOnlySuppliedTags";

  /**
   * The property to specify whether the decoder should substitute a known word
   * when the only tag for an unknown word is closed-class (i.e., the tag
   * was never observed with the unknown word during training).
   * <p>
   * The value of this property should be (the string representation of)
   * a boolean (conversion is performed by the method
   * <code>Boolean.valueOf</code>).
   * <p>
   * The value of this constant is
   * <code>"parser.decoder.substituteWordsForClosedClassTags"</code>.
   */
  public final static String decoderSubstituteWordsForClosedClassTags =
    "parser.decoder.substituteWordsForClosedClassTags";

  /**
   * The property to specify whether node labels in trees output by the
   * decoder include their lexical head information, which is normally only
   * used internally by the decoder.  Even though this setting is grouped
   * with the other decoder settings, it technically affects the implementation
   * of {@link CKYItem#toSexp()}.
   * <p/>
   * The form of a lexicalized label will be
   * <pre>NT[isHead/word/tag/startIdx/endIdx/headWordIdx]</pre>
   * where
   * <ul>
   * <li><tt>NT</tt> is the original, unlexicalized nonterminal
   * <li><tt>isHead</tt> is the string <tt>true</tt> or <tt>false</tt>,
   * indicating whether this node is the head child of its parent
   * <li><tt>word</tt> is the head word
   * <li><tt>tag</tt> is the part-of-speech tag of the head word
   * <li><tt>startIdx</tt> is the index of the leftmost word of the span
   * <li><tt>endIdx</tt> is the index of the rightmost word of the span
   * <li><tt>headWordIdx</tt> is the index of the head word of the span
   * </ul>
   * where all indices are 0-based.
   * <p/>
   * The bracket and delimiter characters <tt>'['</tt>, <tt>']'</tt> and
   * <tt>'/'</tt> are determined by the
   * {@link Treebank#nonTreebankLeftBracket()},
   * {@link Treebank#nonTreebankRightBracket()} and
   * {@link Treebank#nonTreebankDelimiter()} methods, respectively.
   * <p/>
   * If {@link #decoderOutputInsideProbs} is also <code>true</code>, then
   * the form of a lexicalized label will be
   * <pre>NT[isHead/word/tag/prob]</pre>
   * where <tt>prob</tt> is a floating-point value equal to the inside tree
   * probability of the subtree rooted at the node.
   * <p/>
   * The words that were pruned before parsing and re-inserted after parsing
   * (when the {@link #restorePrunedWords} setting is <tt>true</tt>) will
   * not be output with lexical information, since the parser never
   * stochastically assigned head words to these nodes (but one could trivially
   * map these preterminals to their lexicalized versions).
   *
   * @see CKYItem#toSexp()
   */
  public final static String decoderOutputHeadLexicalizedLabels =
    "parser.decoder.outputHeadLexicalizedLabels";

  /**
   * The property to specify whether node labels in trees output by the
   * decoder include the inside probability of their subtree, which is normally only
   * used internally by the decoder.  Even though this setting is grouped
   * with the other decoder settings, it technically affects the implementation
   * of {@link CKYItem#toSexp()}.
   * <p/>
   * The form of a lexicalized label will be
   * <pre>NT[prob]</pre>
   * where
   * <ul>
   * <li><tt>NT</tt> is the original, unlexicalized nonterminal
   * <li><tt>prob</tt> is a <code>double</code> value, indicating the inside
   *                   probability of the subtree
   * </ul>
   * The bracket characters <tt>'['</tt> and <tt>']'</tt> are determined by the
   * {@link Treebank#nonTreebankLeftBracket()} and
   * {@link Treebank#nonTreebankRightBracket()} methods, respectively.
   * <p/>
   * This setting also &ldquo;plays nice&rdquo; with (<i>i.e.</i>, can be
   * simultaneously true with) the {@link #decoderOutputHeadLexicalizedLabels}
   * setting.
   * <p/>
   * This setting requires that post-processing <i>not</i> be performed, so
   * when this setting is true, it will effectively be as though the user
   * also specified <tt>false</tt> for the value of the
   * {@link #decoderDontPostProcess} setting.
   *
   * @see CKYItem#toSexp()
   * @see #decoderOutputHeadLexicalizedLabels
   * @see #decoderDontPostProcess
   */
  public final static String decoderOutputInsideProbs =
    "parser.decoder.outputInsideProbabilities";

  /**
   * The property to specify whether the decoder should perform post-processing
   * on a tree after parsing, that is, whether to invoke
   * {@link Training#postProcess(danbikel.lisp.Sexp)} on that parse tree.
   * <p/>
   * Note that this setting will effectively be <tt>true</tt> if
   * {@link #decoderOutputInsideProbs} is <tt>true</tt>; that is, the actual
   * value of this setting will be ignored if {@link #decoderOutputInsideProbs}
   * is <tt>true</tt>, and will be treated as though the user specified
   * <tt>true</tt> for this setting, as well.
   *
   * @see #decoderOutputInsideProbs
   */
  public final static String decoderDontPostProcess =
    "parser.decoder.dontPostProcess";

  /**
   * The property to specify whether the decoder should wrap its
   * <code>DecoderServerRemote</code> instance with an instance of
   * <code>CachingDecoderServer</code>, which caches probability
   * lookups.  The value of this property should be (the string representation
   * of) a boolean (conversion is performed by the method
   * <code>Boolean.valueOf</code>).
   *
   * @see DecoderServerRemote
   * @see CachingDecoderServer
   */
  public final static String decoderUseLocalProbabilityCache =
    "parser.decoder.useLocalProbabilityCache";

  /**
   * The property to specify the size of the cache used by the
   * <code>CachingDecoderServer</code> instance used by the decoder
   * when the {@link #decoderUseLocalProbabilityCache} property
   * is <code>true</code>.  The value of this property is ignored when
   * {@link #decoderUseLocalProbabilityCache} is <code>false</code>.
   * The value of this property should be (the string representation of)
   * an integer.
   */
  public final static String decoderLocalCacheSize =
    "parser.decoder.localProbabilityCacheSize";

  /**
   * The property to specify whether the decoder should use the
   * head-to-parent map derived during training.  Use of this map
   * potentially increases efficiency in the decoding process by
   * causing the decoder to grow theories upward using only parents
   * that occurred in training for a given chart item's root label.
   * However, use of this map also decreases generality, for if
   * the last back-off level of the head-generation model is more general
   * than <tt>p(H | P)</tt> (if it is, for example, <tt>p(H)</tt>), then
   * potentially any notnerminal can be the parent of any head nonterminal,
   * meaning that the decoder should pursue all nonterminals as parents,
   * which is its behavior when the value of this property is
   * <tt>false</tt>.
   */
  public final static String decoderUseHeadToParentMap =
    "parser.decoder.useHeadToParentMap";

  /**
   * The property to specify that the decoder restores all pruned words.  If
   * this property is <code>true</code> and the decoder produces a parse for
   * a sentence, then it is guaranteed that the number of tokens of the input
   * sentence will be equal to the number of terminals of the parsed output
   * sentence.  The value of this property should be the (string representation
   * of) a boolean (conversion is performed by the method
   * <code>Boolean.valueOf</code>).
   *
   * @see Training#prune(danbikel.lisp.Sexp)
   */
  public final static String restorePrunedWords =
    "parser.decoder.restorePrunedWords";

  /**
   * The property to specify whether the decoder uses {@link
   * ModelCollection#simpleModNonterminalMap}.  This is the simpler of two
   * mechanisms by which the decoder determines whether even to attempt to
   * compute the probability of a modifying nonterminal in the context of some
   * parent and head child and other syntactic context (this computation can be
   * expensive, hence the decoder has two, less-expensive mechanisms to try to
   * avoid such computation where possible).  The simple modifying nonterminal
   * map maps parent/head/side triples to possible modifying nonterminals that
   * occurred in training.  For example, if an <tt>NP</tt> occurred to the left
   * of a <tt>VP</tt> whose parent was <tt>S</tt> in training, then the simple
   * modifying nonterminal map would contain the mapping <tt>S,VP,left -->
   * NP</tt>.  Note that before such mappings are created, any argument
   * augmentations on the parent and any gap augmentation on the head are
   * removed.  Note also that if the last level of back-off of the
   * modifying-nonterminal generation model structure uses an even more reduced
   * context than such a triple, then the simple modifying nonterminal map
   * should <i>not</i> be used and this setting should be <tt>false</tt>.
   * (Using the simple modifying nonterminal map <i>is</i> appropriate,
   * however, with {@link danbikel.parser.ms.ModNonterminalModelStructure2},
   * as well as several other modifying nonterminal model structures in
   * the <tt>danbikel.parser.ms</tt> package.)
   */
  public final static String useSimpleModNonterminalMap =
    "parser.decoder.useSimpleModNonterminalMap";

  /**
   * The property to specify whether words are downcased during training
   * and decoding.  The value of this property should be (the string
   * representation of) a boolean (conversion is performed by the method
   * <code>Boolean.valueOf</code>).
   * <p>
   * The value of this constant is <code>"parser.downcaseWords"</code>.
   */
  public final static String downcaseWords = "parser.downcaseWords";

  /**
   * The property to specify how long, in milliseconds, the SO_TIMEOUT
   * value should be for the switchboard's RMI-client (caller) sockets.
   *
   * @see SwitchboardRemote#socketTimeout
   */
  public final static String sbSocketTimeout = SwitchboardRemote.socketTimeout;

  /**
   * The property to specify how often clients and servers should ping
   * the "keep-alive" socket connected to the switchboard.  The value
   * of this property should be (the string representation of) an
   * integer, representing milliseconds between pings.
   *
   * @see SwitchboardRemote#keepAliveInterval
   */
  public final static String keepAliveInterval =
    SwitchboardRemote.keepAliveInterval;
  /**
   * The property to specify at most how many times the switchboard attempts to
   * contact clients and servers before considering them dead (after an
   * initial failure, thus making 0 a legal value for this property).
   *
   * @see SwitchboardRemote#keepAliveMaxRetries
   */
  public final static String keepAliveMaxRetries =
    SwitchboardRemote.keepAliveMaxRetries;

  /**
   * The property to specify whether the switchboard should kill all
   * of a server's clients when it detects that the server has died.
   * This property should have the value <tt>"false"</tt> when servers
   * are stateless. The value of this property should be (the string
   * representation of) a boolean (conversion is performed by the
   * method <code>Boolean.valueOf</code>).
   *
   * @see SwitchboardRemote#serverDeathKillClients
   */
  public final static String serverDeathKillClients =
    SwitchboardRemote.serverDeathKillClients;

  /**
   * The property to specify how long (in milliseconds) sockets stay alive
   * on the client (switchboard) side for RMI calls to switchboard user objects
   * (subclasses of {@link danbikel.switchboard.AbstractSwitchboardUser
   * AbstractSwitchboardUser}).
   * <p>
   * The value of this constant is
   * <code>"parser.switchboardUser.timeout"</code>.
   */
  public final static String sbUserTimeout = "parser.switchboardUser.timeout";

  /**
   * The property to specify at most how many times switchboard users
   * should try to acquire the switchboard from the bootstrap registry before
   * giving up, either when first starting up or in the event of a switchboard
   * crash.  If the value of this property is identical to the value of
   * {@link danbikel.switchboard.AbstractSwitchboardUser#infiniteTries}, the
   * parsing clients and decoder servers will indefinitely keep trying to
   * re-acquire the switchboard in the event of its failure.
   * <p>
   * The value of this constant is
   * <code>"parser.switchboardUser.sbMaxRetries"</code>.
   */
  public final static String sbUserSBMaxRetries =
   "parser.switchboardUser.sbMaxRetries";

  /**
   * The property to specify whether parsing clients should have
   * server failover; that is, whether they should request a new server
   * from the switchboard if their current server fails.
   * <p>
   * The value of this constant is
   * <code>"parser.switchboardUser.client.serverFailover"</code>.
   * <p>
   *
   * @see danbikel.switchboard.Failover
   */
  public final static String serverFailover =
    "parser.switchboardUser.client.serverFailover";

  /**
   * The property to specify at most how many times parsing clients should
   * re-try their servers in the event of a method failure before giving up.
   * If the value of this property is identical to the value of
   * {@link danbikel.util.proxy.Retry#retryIndefinitely}, clients will
   * keep re-trying indefinitely.
   * <p>
   * The value of this constant is
   * <code>"parser.switchboardUser.client.serverMaxRetries"</code>.
   * <p>
   *
   * @see danbikel.switchboard.AbstractSwitchboardUser.SBUserRetry
   * @see danbikel.util.proxy.Retry
   */
  public final static String serverMaxRetries =
    "parser.switchboardUser.client.serverMaxRetries";

  /**
   * The property to specify how many milliseconds to sleep between server
   * re-tries.
   * <p> The value of this constant is
   * <code>"parser.switchboardUser.client.serverRetrySleep"</code>.
   * <p>
   *
   * @see #serverMaxRetries
   */
  public final static String serverRetrySleep =
    "parser.switchboardUser.client.serverRetrySleep";

  /**
   * The property to specify whether parsing clients should commit suicide
   * when they have detected that the Switchboard has died.  If the value
   * of this setting is <code>false</code>, then parsing clients will be
   * tolerant to Switchboard death, meaning they will continue to attempt
   * to re-register with a Switchboard at the same RMI location as the
   * Switchboard that died.
   */
  public final static String clientDeathUponSwitchboardDeath =
    "parser.switchboardUser.client.dieUponSwitchboardDeath";

  // constants relating to the location of the settings directory and files
  private final static String settingsDirName = ".db-parser";
  private final static String defaultSettingsDirName =
    System.getProperty("user.home") + File.separator + settingsDirName;
  private final static String defaultSettingsFilename = "settings";
  private final static String defaultSettingsFileHeader =
    " This default settings file created automatically by\n" +
    "#     " + progName + " v" + version;
  private final static String regularSettingsFileHeaderPrefix =
    " " + progName + " v" + version + " settings\n#";

  // data members

  private static Properties settings = new Properties() {
    /**
     * This overridden definition ensures that variables are always
     * expanded when new mappings are added.  This means that when properties
     * are loaded from a file, variable expansion happens correctly:
     * convenience variables (which are like macro definitions) defined in the
     * properties file are entered into the property object as they are
     * discovered, allowing mappings on subsequent lines of the file that rely
     * on those definitions to be properly expanded.
     *
     * @param key the key to be mapped to the specified value in this map
     * @param value the value to be added to this map for the specified key
     * @return the previous value of the specified key in this hashtable,
     *         or <code>null</code> if it did not have one
     */
    public synchronized Object put(Object key, Object value) {
      StringBuffer valueBuffer = new StringBuffer((String)value);
      Text.expandVars(this, valueBuffer);
      String expandedValue = valueBuffer.toString();
      String previous = (String)super.put(key, expandedValue);
      if (previous == null || !previous.equals(expandedValue)) {
	changedSettings.put((String)key, previous);
      }
      return previous;
    }
  };

  /**
   * A map where the keys are the settings that have changed, and whose values
   * are the old values of those changed settings.  This map is guaranteed
   * to contain all settings changed since the last call to
   * {@link #processChangeRequests()}.
   */
  private static Map<String, String> changedSettings =
    new HashMap<String, String>();

  // inner class for classCmp data member
  /**
   * A comparator that encodes the <i>comes before</i> relation between two
   * {@link Class} instances, such that if this comparator, which is also a
   * {@link Map}, contains a map entry for {@link Class} {@code c1}, then the
   * {@link Set Set&lt;Class&gt;} that is the value of that entry contains all
   * {@link Class} instances that are strictly less than {@code c1} in the
   * ordering encoded by this comparator.  If there is no mapping for {@code
   * c1}, then it has no <i>comes before</i> requirements. If two classes {@code
   * c1} and {@code c2} are not in the other&rsquo;s <i>comes before</i>
   * requirements, then they are <i>incommensurate</i> in the partial ordering
   * encoded by this comparator, and an arbitrary ordering is imposed based on
   * the hash code of the two class objects.
   */
  private static class ClassCmp extends HashMap<Class, Set<Class>>
    implements Comparator<Class> {

    public int compare(Class c1, Class c2) {
      Set<Class> beforeC1 = get(c1);
      Set<Class> beforeC2 = get(c2);
      if (beforeC2 != null && beforeC2.contains(c1)) {
	return -1;
      } else if (beforeC1 != null && beforeC1.contains(c2)) {
	return 1;
      }
      return c1.hashCode() - c2.hashCode();
    }

  }

  private static ClassCmp classCmp = new ClassCmp();

  /**
   * A sorted map from {@link Class} objects to {@link Change} instances, to
   * keep track of classes that need to be notified of settings changes
   * (primarily used by classes that cache settings as static data members).
   * <p/>
   * <b>Implementation note</b>: This is a map and not a list or set because we
   * want to spit out an error message if the same class registers more than
   * once.
   */
  private static SortedMap<Class, Change> classChangeRequests =
    new TreeMap<Class, Change>(classCmp);

  private static Set<Change> instanceChangeRequests =
    Collections.newSetFromMap(new WeakHashMap<Change, Boolean>());

  // static handles onto the settings dir and settings file
  private static File settingsDir = null;
  private static File settingsFile = null;

  static {
    String settingsDirName = System.getProperty(settingsDirOverride);
    if (settingsDirName == null)
      settingsDirName = defaultSettingsDirName;

    settingsDir = new File(settingsDirName);

    if (!settingsDir.exists()) {
      if (System.getProperty(settingsDirOverride) != null)
	System.err.println(className + ": warning: settings directory " +
			   settingsDir + "doesn't exist");
    }
    else if (!settingsDir.isDirectory())
      System.err.println(className + ": warning: " + settingsDir +
			 " exists but is not a directory");

    String settingsFilename = System.getProperty(settingsFileOverride);
    if (settingsFilename == null)
      settingsFile =
	new File(settingsDir + File.separator + defaultSettingsFilename);
    else
      settingsFile = new File(settingsFilename);

    // load fallback defaults from resource, then try to grab settings
    // from settings file in default location, then grab system settings
    // with prefix "parser."
    try {
      load(new BufferedInputStream(getDefaultsResource()));
      InputStream defaultSettingsStream = getSettingsStream();
      if (defaultSettingsStream != null)
	load(new BufferedInputStream(defaultSettingsStream));
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(className + ": error: couldn't load bootstrap " +
			 "default settings");
      System.err.println(className + ": please check the integrity of the " +
			 "distribution of");
      System.err.println(className + ": this package");
      throw new RuntimeException(fnfe.toString());
    }
    catch (IOException ioe) {
      System.err.println(className + ": error: couldn't load bootstrap " +
			 "default settings");
      throw new RuntimeException(ioe.toString());
    }
    for (Object o : System.getProperties().keySet()) {
      String property = (String)o;
      if (property.startsWith(globalPropertyPrefix)) {
	setInternal(property, System.getProperty(property));
      }
    }
    // we could invoke processChangeRequests here, but we don't bother, since
    // this is the bootstrap loading of settings, and should therefore come
    // before other classes are loaded that are dependent on Settings
  }

  /**
   * Registers a class to receive an update when one or more settings have
   * changed.
   *
   * @param cl     the class registering to receive notifications of changes
   * @param change the change request for the specified class
   * @param before the set of classes that must be updated before this class, or
   *               <code>null</code> or {@link java.util.Collections#emptySet()
   *               Collections.&lt;Class&gt;emptySet()} if no such set exists
   */
  public static void register(Class cl,
			      Change change, Set<Class> before) {
    synchronized (settings) {
      if (classChangeRequests.containsKey(cl)) {
	System.err.println(className + ".register: warning: class " +
			   cl.getName() +
			   "already registered; overriding with " +
			   "new registration");
      }
      if (before != null && !before.isEmpty()) {
	classCmp.put(cl, before);
      }
      classChangeRequests.put(cl, change);
    }
  }

  /**
   * Registers a particular object implementing the {@link Change} interface
   * to receive an update when one or more settings have changed.  Typically,
   * only large, long-lived objects should register for notifications.
   * 
   * @param change the change request for the specified object
   */
  public static void register(Change change) {
    synchronized (settings) {
      if (instanceChangeRequests.contains(change)) {
	System.err.println(className + ".register: warning: object " +
			   change + "already registered; overriding with " +
			   "new registration");
      }
      instanceChangeRequests.add(change);
    }
  }

  private static void processChangeRequests() {
    synchronized (settings) {
      if (changedSettings.isEmpty()) {
	return;
      }
      for (Map.Entry<Class, Change> entry : classChangeRequests.entrySet()) {
	Change c = entry.getValue();
	c.update(changedSettings);
      }
      for (Change c : instanceChangeRequests) {
	c.update(changedSettings);
      }
    }
    changedSettings.clear();
  }

  /**
   * Returns the double value of the specified setting, as determined by
   * {@link Double#parseDouble(String)}.
   *
   * @param setting the setting whose value is to be gotten
   * @return the double value of the specified setting
   */
  public static double getDouble(String setting) {
    return Double.parseDouble(get(setting));
  }

  /**
   * Returns the integer value of the specified setting, as determined by
   * {@link Integer#parseInt(String)}.
   *
   * @param setting the setting whose value is to be gotten
   * @return the integer value of the specified setting
   */
  public static int getInteger(String setting) {
    return Integer.parseInt(get(setting));
  }

  /**
   * Returns the boolean value of the specified setting, as determined by
   * {@link Boolean#valueOf(String)}.
   *
   * @param setting the setting whose value is to be gotten
   * @return the boolean value of the specified setting
   */
  public static boolean getBoolean(String setting) {
    return Boolean.valueOf(get(setting)).booleanValue();
  }

  /**
   * Loads the properties from the file of the specified filename, using
   * {@link #load(File)}.
   *
   * @param filename the name of the file containing properties to load
   * @throws IOException if {@link #load(File)} throws an <tt>IOException</tt>
   */
  public static void load(String filename) throws IOException {
    System.err.println("Reading settings from\n\t" + filename);
    load(new File(filename));
  }

  /**
   * Loads the properties from the specified file, using
   * {@link #load(InputStream)}.
   *
   * @param file the file containing properties to load
   * @throws IOException if creating a <tt>FileInputStream</tt> throws
   * an <tt>IOException</tt> or if the call to {@link #load(InputStream)}
   * throws an <tt>IOException</tt>
   */
  public static void load(File file) throws IOException {
    try {
      BufferedInputStream bis =
	new BufferedInputStream(new FileInputStream(file));
      load(bis);
      bis.close();
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(fnfe);
    }
  }

  /**
   * Loads the properties from the specified input stream, using
   * {@link Properties#load(InputStream)}.
   *
   * @param is the input stream containing properties to load
   * @throws IOException if there is a problem reading from the specified stream
   */
  public static void load(InputStream is) throws IOException {
    settings.load(is);
    processChangeRequests();
  }

  /**
   * Stores the properties of this class to the specified output stream,
   * using {@link Properties#store(OutputStream,String)}.
   *
   * @param os the output stream to which to write the properties contained in
   * this class
   * @throws IOException if there is a problem writing to the specified stream
   */
  public static void store(OutputStream os) throws IOException {
    settings.store(os, defaultSettingsFileHeader);
  }

  /**
   * Stores the properties of this class to the specified output stream,
   * using {@link Properties#store(OutputStream,String)}.
   *
   * @param os the output stream to which to write the properties contained in
   * this class
   * @param header the header text to put at the beginning of the
   * properties file
   * @throws IOException if there is an exception writing to the specified
   * stream
   */
  public static void store(OutputStream os, String header) throws IOException {
    settings.store(os, regularSettingsFileHeaderPrefix + header);
  }

  /**
   * Stores the properties of this class to the specified output stream.
   * @param os the output stream to which to write the properties contained
   * in this class
   * @throws IOException if there is an exception writing to the specified
   * stream
   */
  public static void store(ObjectOutputStream os) throws IOException {
    os.writeObject(settings);
  }

  /**
   * Stores a sorted list of the settings and values of this class to
   * the specified output stream
   * @param os the output stream to which to write a sorted list of the
   * settings and values contained in this class
   * @throws IOException if there is a problem writing to the specified
   * output stream
   */
  public static void storeSorted(OutputStream os) throws IOException {
    storeSorted(settings, os);
  }

  /**
   * Stores a sorted list of the specified property-value pairs to the
   * specified output stream
   * @param props a container of property-value pairs
   * @param os an output stream to which to write a sorted list of the
   * property-value pairs contained in the specified {@link Properties}
   * object
   * @throws IOException if there is a problem writing to the
   * specified output stream
   */
  public static void storeSorted(Properties props, OutputStream os)
    throws IOException {
    storeSorted(props, os, defaultSettingsFileHeader);
  }

  /**
   * Stores a sorted list of the property-value pairs contained in this class
   * to the specified output stream using the specified header.
   * @param os the output stream to which to store a sorted list of the
   * property-value pairs contained in this class
   * @param header the header to write to the specified output stream
   * before writing the sorted list of property-value pairs
   * @throws IOException if there is a problem writing to the specified
   * output stream
   */
  public static void storeSorted(OutputStream os, String header)
    throws IOException {
    storeSorted(settings, os, header);
  }

  /**
   * Stores a sorted list of the specified container of property-value pairs
   * to the specified output stream using the specified header.
   * @param props a container of property-value pairs
   * @param os the output stream to which to store a sorted list of the
   * specified property-value pairs
   * @param header the header to write to the specified output stream
   * before writing the sorted list of property-value pairs
   * @throws IOException if there is a problem writing to the specified
   * output stream
   */
  public static void storeSorted(Properties props,
				 OutputStream os, String header)
    throws IOException {
    PrintStream ps = new PrintStream(os);
    SortedMap propMap = new TreeMap();
    //noinspection unchecked
    propMap.putAll(props);
    ps.println("#" + header);
    for (Object o : propMap.entrySet()) {
      Map.Entry entry = (Map.Entry)o;
      ps.println(entry.getKey() + " = " + entry.getValue());
    }
    ps.flush();
  }

  /**
   * Sets the property <code>name</code> to <code>value</code>, using {@link
   * Properties#setProperty(String,String)}.  As a side effect, all classes that
   * have registered to receive settings changes will be notified of this
   * change, via invocations of {@link Settings.Change#update(Map)} on the
   * {@link Change} instances that were passed either to the {@link
   * #register(Class,Settings.Change,Set)} method or to the {@link
   * #register(Settings.Change)} method.
   *
   * @param name  the name of the property to set
   * @param value the value to which to set the property <code>name</code>
   */
  public static void set(String name, String value) {
    setInternal(name, value);
    processChangeRequests();
  }

  /**
   * Updates the specified setting on the internal {@link Properties} object
   * without processing change requests.
   *
   * @param name  the name of the setting to update
   * @param value the new value for the specified setting
   */
  private static void setInternal(String name, String value) {
    settings.setProperty(name, value);
  }

  /**
   * Gets the value of the specified property.
   *
   * @param name the name of the property to get
   * @return the value of the specified property
   */
  public static String get(String name) {
    return settings.getProperty(name);
  }

  /**
   * Returns a deep copy of the internal <code>Properties</code> object.
   * @return a deep copy of the internal <code>Properties</code> object
   */
  public static Properties getSettings() {
    Properties settingsCopy = new Properties();
    settingsCopy.putAll(settings);
    return settingsCopy;
  }

  /**
   * Allows any class to set the settings of this class directly using the
   * specified <code>Properties</code> object.
   *
   * @param newSettings the new settings to be contained by this class
   */
  public static void setSettings(Properties newSettings) {
    for (Map.Entry<Object, Object> objectObjectEntry : newSettings.entrySet()) {
      Map.Entry entry = (Map.Entry) objectObjectEntry;
      setInternal((String) entry.getKey(), (String) entry.getValue());
    }
    processChangeRequests();
  }

  /**
   * Attempts to locate the file or resource with the specified name in
   * one of three places:
   * <ol>
   * <li> as a file path relative to the default settings directory, or
   * <li> as a file path relative to the current working directory, or
   *      relative to nothing, if <code>name</code> is an absolute path
   * <li> as a resource gotten from the class loader of the specified class
   * </ol>
   * The default settings directory is described in the documentation
   * for {@link #settingsDirOverride}.
   *
   * @param cl the class that needs the file or resource
   * @param name the name of the file or resource
   * @return an <code>InputStream</code> of the specified file or resource,
   * or <code>null</code> if the file or resource could not be found
   * @throws FileNotFoundException if the settings file cannot be found
   */
  public final static InputStream getFileOrResourceAsStream(Class cl,
							    String name)
    throws FileNotFoundException {

    File file1 = new File(Settings.settingsDir + File.separator + name);
    File file2 = new File(name);
    File file = (file1.exists() ? file1 :
		 (file2.exists() ? file2 : null));
    if (file != null)
      return new FileInputStream(file);
    else {
      // try to access as resource
      URL resourceURL = cl.getResource(name);
      InputStream is = null;
      if (resourceURL != null) {
	try { is = resourceURL.openStream(); }
	catch (IOException ioe) { is = null; }
      }
      if (is == null) {
	System.err.println(cl.getName() + ": warning: couldn't " +
			   "find \"" +
			   name + "\" in\n\t" +
			   file1 + "\n\t" + file2 + "\n" +
			   "or as the resource\n\t" +
			   resourceURL);
	throw new FileNotFoundException(name);
      }
      System.err.println("Reading from resource " + resourceURL);
      return is;
    }
  }

  /**
   * Gets the fallback defaults from resource, thowing exception if resource
   * unavailable (which is a very bad situation).
   *
   * @return the defaults resource for this class
   *
   * @throws FileNotFoundException if the defaults resource stream cannot be
   *                               found
   */
  public final static InputStream getDefaultsResource()
    throws FileNotFoundException {
    // try to access as resource
    URL defaultsURL = Settings.class.getResource("default-settings.properties");
    InputStream is = null;
    if (defaultsURL != null) {
      try {
	is = defaultsURL.openStream();
      }
      catch (IOException ioe) {
	is = null;
      }
    }
    if (is == null) {
      System.err.println(className + ": error: couldn't " +
			 "find the \"default-settings.properties\" resource");
      throw new FileNotFoundException("no default settings");
    }
    System.err.println("Reading default settings from resource\n\t" +
		       defaultsURL);
    return is;
  }

  /**
   * Gets a new <code>InputStream</code> for the file
   * <tt>$HOME/.db-parser/settings</tt> if it exists, returning
   * <code>null</code> otherwise.  The location of the file may be changed
   * using {@link #settingsDirOverride} or {@link #settingsFileOverride},
   * where the latter definition takes precedence over the former.
   *
   * @return the stream of the settings file
   * @throws FileNotFoundException if the settings file cannot be found
   */
  private final static InputStream getSettingsStream()
    throws FileNotFoundException {
    if (settingsFile.exists()) {
      System.err.println("Reading settings from\n\t" + settingsFile);
      return new FileInputStream(settingsFile);
    }
    else
      return null;
  }

  /**
   * Returns the integer value of specified property, or the specified default
   * value if the specified property does not exist.
   *
   * @param property     the property or setting whose value is to be retrieved
   * @param defaultValue the fallback default value for the specified property
   * @return the integer value of specified property, or the specified default
   *         value if the specified property does not exist.
   */
  public static int getIntProperty(String property, int defaultValue) {
    String valueStr = get(property);
    return (valueStr == null) ? defaultValue : getInteger(property);
  }

  /**
   * Returns the boolean value of specified property, or the specified default
   * value if the specified property does not exist.
   *
   * @param property     the property or setting whose value is to be retrieved
   * @param defaultValue the fallback default value for the specified property
   * @return the boolean value of specified property, or the specified default
   *         value if the specified property does not exist.
   */
  public static boolean getBooleanProperty(String property,
					   boolean defaultValue) {
    String propStr = get(property);
    return (propStr == null) ? defaultValue : getBoolean(property);
  }

  /**
   * Prints the default settings contained in the resource supplied with this
   * parsing software.
   *
   * @param args ignored
   */
  public static void main(String [] args) {
    try {
      InputStream is = Settings.getDefaultsResource();
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      for (String line = null; (line = reader.readLine()) != null; )
	System.out.println(line);
    }
    catch (IOException ioe) { System.err.println(ioe); }
  }
}
