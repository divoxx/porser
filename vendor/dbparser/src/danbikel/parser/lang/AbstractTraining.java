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
    package danbikel.parser.lang;

import danbikel.lisp.*;
import danbikel.parser.*;
import java.util.*;
import java.io.*;

/**
 * Provides methods for language-specific preprocessing of training
 * parse trees.  The primary method to be invoked from this class is
 * {@link #preProcess(Sexp)}.  Additionally, as this class contains or
 * has access to appropriate preprocessing data and methods, it also
 * contains a crucial method to be used for post-processing, to
 * &quot;undo&quot; what it has done during preprocessing after
 * decoding.  This post-processing method is {@link
 * #postProcess(Sexp)}, and is invoked by default by the
 * <code>Decoder</code>.
 * <p>
 * <b>Concurrency note</b>: As training is typically a sequential
 * process, with very few noted exceptions, <i>none of the default
 * implementations of the methods of this abstract base class is
 * thread-safe</i>.  If thread-safe guarantees are desired, the
 * methods of this class should be overridden.
 *
 * @see #preProcess(Sexp)
 * @see #postProcess(Sexp)
 * @see Decoder
 */
public abstract class AbstractTraining implements Training, Serializable {

  // constants

  /**
   * Indicates to relabel head children as arguments.  Such relabeling is
   * unnecessary, since head children are already inherently distinct.  This
   * flag should be true when emulating the behavior of Mike Collins' parser.
   */
  protected static boolean relabelHeadChildrenAsArgs =
    Settings.getBoolean(Settings.collinsRelabelHeadChildrenAsArgs);

  private static final String className = Training.class.getName();

  /**
   * Caches the boolean value of the property {@link Settings#addGapInfo}.
   */
  protected static boolean addGapInfo =
    Settings.getBoolean(Settings.addGapInfo);

  /**
   * Caches the boolean value of the property
   * {@link Settings#collinsRepairBaseNPs}.
   */
  protected static boolean repairBaseNPs =
    Settings.getBoolean(Settings.collinsRepairBaseNPs);

  // static map for stripping away argument augmentation from nonterminal labels
  private static danbikel.util.HashMap fastArgMap = new danbikel.util.HashMap();
  private static boolean canUseFastArgMap = false;

  /**
   * Static set for storing argument nonterminals.
   */
  protected static Set argNonterminals = null;
  /**
   * The symbol to indicate the list of argument-finding rules from a metadata
   * resource.
   */
  protected final static Symbol argContextsSym = Symbol.add("arg-contexts");
  /**
   * The symbol to indicate the list of node augmentations that prevent
   * a node from being relabeled
   */
  protected final static Symbol semTagArgStopListSym =
    Symbol.add("sem-tag-arg-stop-list");
  /**
   * The symbol to indicate the list of nodes to prune.
   *
   * @see #nodesToPrune
   * @see #prune(Sexp)
   */
  protected final static Symbol nodesToPruneSym = Symbol.add("prune-nodes");
  /**
   * The prefix of the property of the metadata resource required by the
   * default constructor of concrete subclasses.  The value of this constant
   * is <code>&quot;parser.training.metadata.&quot;</code>.
   */
  protected final static String metadataPropertyPrefix =
    "parser.training.metadata.";

  public void setUpFastArgMap(CountsTable nonterminals) {
    staticSetUpFastArgMap(nonterminals);
  }

  /**
   * Indicates to set up a static map for quickly mapping argument nonterminals
   * to their non-argument variants (that is, for quickly stripping away
   * their argument augmentations).
   * <p>
   * <b>N.B.</b>: This method is necessarily thread-safe, as it is expected
   * to be invoked by every {@link Decoder} as it starts up, and since there
   * can be multiple {@link Decoder} instances within a given VM.
   *
   * @param nonterminals a counts table whose keys form a complete set of
   * all possible nonterminal labels, as is obtained from
   * {@link DecoderServerRemote#nonterminals()} (the counts to which the
   * nonterminals are mapped are not used by this method)
   */
  protected static synchronized void staticSetUpFastArgMap(CountsTable nonterminals) {
    if (canUseFastArgMap)
      return;
    for (Object o : nonterminals.keySet()) {
      Symbol nt = (Symbol)o;
      if (Language.training().isArgument(nt))
	fastArgMap.put(nt, Language.training().removeArgAugmentation(nt));
    }
    canUseFastArgMap = true;
  }

  static {
    Settings.Change change = new Settings.Change() {
      public void update(Map<String, String> changedSettings) {
	relabelHeadChildrenAsArgs =
	  Settings.getBoolean(Settings.collinsRelabelHeadChildrenAsArgs);
	addGapInfo =
	  Settings.getBoolean(Settings.addGapInfo);
	repairBaseNPs =
	  Settings.getBoolean(Settings.collinsRepairBaseNPs);
      }
    };
    Settings.register(AbstractTraining.class, change, null);
  }

  // data members

  private Nonterminal nonterminal = new Nonterminal();
  private Nonterminal nonterminal2 = new Nonterminal();
  @SuppressWarnings({"UnusedDeclaration"})
  private Nonterminal addGapData = new Nonterminal();

  /** Holds the value of {@link danbikel.parser.Language#treebank()}. */
  protected Treebank treebank = Language.treebank();
  /** Holds the value of {@link danbikel.parser.Language#headFinder()}. */
  protected HeadFinder headFinder = Language.headFinder();

  /**
   * The symbol that will be used to identify nonterminals whose subtrees
   * contain a gap (a trace).  This method is used by {@link
   * #stripAugmentations(Sexp)}, so that gap augmentations that are added by
   * {@link #addGapInformation(Sexp)} do not get removed.  The default value is
   * the symbol returned by <code>Symbol.add(&quot;g&quot;)</code>.  If this
   * default value conflicts with an augmentation already used in a particular
   * Treebank, this value should be reassigned in the constructor of a
   * subclass.
   */
  protected Symbol gapAugmentation = Symbol.add("g");

  /**
   * The string consisting of the canonical augmentation delimiter
   * concatenated with the gap augmentation, to be used in
   * identifying nonterminals that contain gap augmentations.
   *
   * @see Treebank#canonicalAugDelimiter
   * @see #gapAugmentation
   */
  protected String delimAndGapStr =
    treebank.canonicalAugDelimiter() + gapAugmentation.toString();
  /**
   * The length of {@link #delimAndGapStr}, cached here for efficiency
   * and convenience.
   */
  protected int delimAndGapStrLen = delimAndGapStr.length();

  /**
   * The symbol that will be used to identify argument nonterminals.  This
   * method is used by {@link #stripAugmentations(Sexp)}, so that argument
   * augmentations that are added by {@link #identifyArguments(Sexp)} do not
   * get removed.  The default value is the symbol returned by
   * <code>Symbol.add(&quot;A&quot;)</code>.  If this default value conflicts
   * with an augmentation already used in a particular Treebank, this value
   * should be reassigned in the constructor of a subclass.
   */
  protected Symbol defaultArgAugmentation = Symbol.add("A");

  /**
   * A list representing the set of all argument augmentations.  By default,
   * this data member will be initialized to a new list containing only the
   * {@linkplain #defaultArgAugmentation default argument augmentation}.
   * Subclasses should add additional augmentations to this list in their
   * constructors, or by invoking the {@link #createArgAugmentationsList}
   * method after filling in the {@link #argContexts} map.
   *
   * @see #argContexts
   * @see #createArgAugmentationsList()
   */
  protected SexpList argAugmentations =
    new SexpList().add(defaultArgAugmentation);

  /**
   * The symbol that gets assigned as the part of speech for null
   * preterminals that represent traces that have undergone WH-movement, as
   * relabeled by the default implementation of {@link
   * #addGapInformation(Sexp)}.  The default value is the return value of
   * <code>Symbol.add(&quot;*TRACE*&quot;)</code>.  If this maps to an actual
   * part of speech tag or nonterminal label in a particular Treebank, this
   * data member should be reassigned in the constructor of a subclass.
   */
  protected Symbol traceTag = Symbol.add("*TRACE*");

  // re-define all six of the data members below if '+' is an
  // augmentatoin delimeter in the treebank(s) intended to be usd byte
  // the concrete language package

  /** Data member returned by the accessor method of the same name. */
  private Symbol startSym = Symbol.add("+START+");
  /** Data member returned by the accessor method of the same name. */
  private Symbol stopSym = Symbol.add("+STOP+");
  /** Data member returned by the accessor method of the same name. */
  private Word startWord = Words.get(startSym, startSym);
  /** Data member returned by the accessor method of the same name. */
  private Word stopWord = Words.get(stopSym, stopSym);
  /** Data member returned by the accessor method of the same name. */
  private Symbol topSym = Symbol.add("+TOP+");
  /** Data member returned by the accessor method of the same name. */
  private Word topWord = Words.get(topSym, topSym);

  /**
   * A Symbol created from the first character of {@link
   * Treebank#augmentationDelimiters}.
   */
  protected final Symbol canonicalAugDelimSym;

  /**
   * Data member to store the set of nodes to prune for the default
   * implementation of {@link #prune(Sexp)}.  The set should only contain
   * objects of type <code>Symbol</code>, and the elements of this set
   * should be added in the constructor of a subclass.
   *
   * @see #prune(Sexp)
   */
  protected Set nodesToPrune;

  /**
   * Data member to store the set of words to prune for the default
   * implementation of {@link #prune(Sexp)}.  The set should only contain
   * objects of type <code>Symbol</code>, and the elements of this set should be
   * added in the constructor of a subclass.  The default implementation will
   * only prune a preterminal if both the part-of-speech tag is in {@link
   * #nodesToPrune} <i><b>and</b></i> if the word is in this
   * <code>wordsToPrune</code> set.
   *
   * @see #prune(Sexp)
   */
  protected Set wordsToPrune;

  /**
   * The set of preterminals (<code>Sexp</code> objects) that have been pruned
   * away.
   */
  protected Set prunedPreterms = new HashSet();

  /**
   * Data member used to store the map required by the default implementation
   * of the method {@link #identifyArguments(Sexp)}.  This data member maps
   * parent nonterminals to lists of children nonterminals, to indicate that
   * the children are candidates for being labeled as arguments in the presence
   * of that parent.  A children list may also be a list of the form
   * <pre>
   * (head &lt;offset&gt;)
   * </pre>
   * indicating to match a node <code>&lt;offset&gt;</code> away from the head
   * child of the parent that was mapped to this children list.  The keys and
   * values of this map should be added in the constructor of a subclass.
   * The keys of this map must be of type {@link Symbol}, and the values of
   * this map must be of type {@link SexpList}.
   * <p>
   * Optionally, after this data member has been filled in by the constructor
   * of a subclass, the method {@link #createArgAugmentationsList()} should
   * be invoked to automatically fill in the {@link #argAugmentations} list.
   *
   * @see #identifyArguments(Sexp)
   * @see #argAugmentations
   * @see #createArgAugmentationsList()
   */
  protected Map argContexts;
  /**
   * Data member used to store the set required by the method {@link
   * #identifyArguments(Sexp)}.  The set contains semantic tags (which is
   * English Treebank parlance) that prohibit a candidate argument child from
   * being relabeled as an argument.  The objects in this set must all be of
   * type <code>Symbol</code>.  The members of this set should be added in the
   * constructor of a subclass.
   *
   * @see #identifyArguments(Sexp)
   */
  protected Set semTagArgStopSet;
  /**
   * The symbol that is a possible mapping in {@link #argContexts} to indicate
   * to choose a child relative to the head as an argument.  For example, an
   * argument context might be <code>PP</code> mapping to <code>(head
   * 1))</code>, meaning that the child that is 1 position to the right of the
   * head child of a PP should be relabeled as an argument.  The value of this
   * data member is the symbol returned by
   * <code>Symbol.add(&quot;head&quot;)</code>.  In the unlikely event that
   * this value conflicts with a nonterminal in a particular Treebank, this
   * data member should be reassigned in the constructor of a subclass.
   *
   * @see #identifyArguments(Sexp)
   */
  protected static final Symbol headSym = Symbol.add("head");

  /**
   * The symbol that is a possible mapping {@link #argContexts} to indicate
   * to choose a child relative to the left side of the head as an argument.
   * For example, an argument context might be <code>VP</code> mapping to
   * <code>(head-left left MD VBD)</code>, meaning that the children to the left
   * of the head child should be searched from left to right, and the first
   * child found that is a member of the set <tt>{MD, VBD}</tt> should be
   * considered a possible argument of the head.
   */
  protected static final Symbol headPreSym = Symbol.add("head-pre");
  /**
   * The symbol that is a possible mapping {@link #argContexts} to indicate
   * to choose a child relative to the right side of the head as an argument.
   * For example, an argument context might be <code>PP</code> mapping to
   * <code>(head-right left PP NP WHNP ADJP)</code>, meaning that the children
   * to the right of the head child should be searched from left to right, and
   * the first child found that is a member of the set
   * <tt>{PP, NP, WHNP, ADJP}</tt> should be considered a possible argument
   * of the head.
   */
  protected static final Symbol headPostSym = Symbol.add("head-post");

  /**
   * The value of {@link Treebank#baseNPLabel}, cached for efficiency and
   * convenience.
   */
  protected final Symbol baseNP = treebank.baseNPLabel();
  /**
   * The value of {@link Treebank#NPLabel}, cached for efficiency and
   * convenience.
   */
  protected final Symbol NP = treebank.NPLabel();

  // data members used by raisePunctuation
  private SexpList addToRaise = new SexpList();
  private SexpList raise = new SexpList();
  /**
   * The set of preterminals (<code>Sexp</code> objects) that were "raised
   * away" by {@link #raisePunctuation(Sexp)} because they appeared either at
   * the beginning or the end of a sentence.
   */
  protected Set prunedPunctuation = new HashSet();

  // data member used by hasGap
  private ArrayList hasGapIndexStack = new ArrayList();

  /**
   * A synchronized map from nonterminals to their non-argument versions, used
   * byte {@link #isArgumentFast(Symbol)} (unless {@link
   * #setUpFastArgMap(CountsTable)} has been invoked).
   */
  private Map fastArgCache = Collections.synchronizedMap(new HashMap());

  /**
   * A synchronized map from nonterminals to their canonical-argument versions,
   * used byte {@link #getCanonicalArg(Symbol)}.
   */
  private Map canonicalArgCache = Collections.synchronizedMap(new HashMap());

  /**
   * Default constructor for this abstract base class; sets {@link
   * #argContexts} to a new <code>Map</code> object, sets {@link
   * #semTagArgStopSet} to a new <code>Set</code> object and initializes {@link
   * #canonicalAugDelimSym}.  Subclass constructors are responsible for filling
   * in the data for {@link #argContexts} and {@link #semTagArgStopSet}.
   */
  protected AbstractTraining() {
    argContexts = new HashMap();
    semTagArgStopSet = new HashSet();
    nodesToPrune = new HashSet();
    wordsToPrune = new HashSet();
    canonicalAugDelimSym =
      Symbol.add(new String(new char[] {treebank.canonicalAugDelimiter()}));
  }

  /**
   * The method to call before counting events in a training parse tree.
   * This default implementation executes the following methods of this class
   * in order:
   * <ol>
   * <li> {@link #prune(Sexp)}
   * <li> {@link #addBaseNPs(Sexp)}
   * <li> {@link #repairBaseNPs(Sexp)}
   * <li> {@link #addGapInformation(Sexp)}
   * <li> {@link #relabelSubjectlessSentences(Sexp)}
   * <li> {@link #removeNullElements(Sexp)}
   * <li> {@link #raisePunctuation(Sexp)}
   * <li> {@link #identifyArguments(Sexp)}
   * <li> {@link #stripAugmentations(Sexp)}
   * </ol>
   * While every attempt has been made to make the default implementations of
   * these preprocessing methods independent of one another, the order above is
   * not entirely arbitrary.  In particular:
   * <ul>
   * <li>{@link #addGapInformation(Sexp)} should be run after methods that
   * introduce new nodes, which in this case is {@link #addBaseNPs(Sexp)}, as
   * these new nodes may need to be used to thread the gap feature
   * <li>{@link #relabelSubjectlessSentences(Sexp)} should be run after
   * {@link #addGapInformation(Sexp)} because only those sentences whose
   * empty subjects are <i>not</i> the result of WH-movement should be
   * relabeled
   * <li>{@link #removeNullElements(Sexp)} should be run after any
   * methods that depend on the presence of null elements, such as
   *   <ul>
   *   <li>{@link #relabelSubjectlessSentences(Sexp)} because a sentence cannot
   *   be determined to be subjectless unless a null element is present as
   *   a child of a subject-marked node
   *   <br>and<br>
   *   <li>{@link #addGapInformation(Sexp)} because the determination of
   *   the location of a trace requires the presence of indexed null elements
   *   </ul>
   * <li>{@link #raisePunctuation(Sexp)} should be run after
   * {@link #removeNullElements(Sexp)} because a null element that is a
   * leftmost or rightmost child can block detection of a punctuation element
   * that needs to be raised after removal of the null element (if a punctuation
   * element is the next-to-leftmost or next-to-rightmost child of an interior
   * node)
   * <li>{@link #stripAugmentations(Sexp)} should be run after all methods
   * that may depend upon the presence of nonterminal augmentations: {@link
   * #identifyArguments(Sexp)}, {@link #relabelSubjectlessSentences(Sexp)} and
   * {@link #addGapInformation(Sexp)}
   * </ul>
   *
   * @param tree the parse tree to pre-process
   * @return <code>tree</code> having been pre-processed
   */
  public Sexp preProcess(Sexp tree) {
    prune(tree);
    addBaseNPs(tree);
    repairBaseNPs(tree);
    addGapInformation(tree);
    relabelSubjectlessSentences(tree);
    removeNullElements(tree);
    raisePunctuation(tree);
    identifyArguments(tree);
    stripAugmentations(tree);
    //fixSubjectlessSentences(tree);  this method now in English lang. package
    return tree;
  }

  public boolean removeWord(Symbol word, Symbol tag, int idx, SexpList sentence,
			    SexpList tags, SexpList originalTags,
			    Set prunedPretermsPosSet,
			    Map prunedPretermsPosMap) {
    /*
    return (tag != null ?
	    prunedPretermsPosSet.contains(tag) :
	    prunedPretermsPosMap.containsKey(word));
    */
    return prunedPretermsPosMap.containsKey(word);
  }

  /**
   * Preprocesses the specified test sentence and its coordinated list of tags.
   * The default implementation here does nothing.
   *
   * @param sentence the list of words, where a known word is a symbol and
   * an unknown word is represented by a 3-element list (see
   * {@link DecoderServerRemote#convertUnknownWords})
   * @param originalWords the list of unprocessed words (all symbols)
   * @param tags the list of tag lists, where the list at index <i>i</i>
   * is the list of possible parts of speech for the word at that index
   * @return a two-element list, containing two lists, the first of which
   * is a processed version of <code>sentence</code> and the second of which
   * is a processed version of <code>tags</code>; if <code>tags</code>
   * is <code>null</code>, then the returned list will contain only
   * one element (since <code>SexpList</code> objects are not designed
   * to handle null elements)
   */
  public SexpList preProcessTest(SexpList sentence,
				 SexpList originalWords, SexpList tags) {
    if (tags == null)
      return new SexpList(1).add(sentence);
    else
      return new SexpList(2).add(sentence).add(tags);
  }

  /**
   * Returns <code>true</code> if <code>tree</code> is a preterminal (the base
   * case) or is a list with the first element of type <code>Symbol</code> (the
   * node label) and subsequent elements are valid trees (the recursive case).
   * If a language package requires a different definition of training parse
   * tree validity, this method should be overridden.  However, changing the
   * definition of tree validity should be done with care, as the default
   * implementations of the tree-processing methods in this class require trees
   * that correspond to the definition of validity implemented by this method.
   * This method also ensures that not all words or preterminals in the tree
   * are to be pruned.
   *
   * @param tree the parse tree to check for validity
   * @see #isAllNodesToPrune(Sexp)
   * @see Treebank#isPreterminal(Sexp)
   */
  public boolean isValidTree(Sexp tree) {
    return isValidTreeInternal(tree) && !isAllNodesToPrune(tree);
  }

  boolean isValidTreeInternal(Sexp tree) {
    if (tree.isSymbol())
      return false;
    if (treebank.isPreterminal(tree))
      return true;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      if (treeList.first().isSymbol() == false)
	return false;
      int treeListLen = treeList.length();
      if (treeListLen == 1)
	return false;
      for (int i = 1; i < treeListLen; i++)
	if (isValidTreeInternal(treeList.get(i)) == false)
	  return false;
    }
    return true;
  }

  /**
   * Returns whether all words or preterminals of this tree are to be pruned.
   * For example, if the part-of-speech tag "." is a {@linkplain #nodesToPrune
   * node to prune} and the word "rubbish" is a {@linkplain #wordsToPrune
   * word to prune} and the specified tree is <tt>(S (NN rubbish) (. .))</tt>
   * then this method will return <tt>true</tt>.  If <tt>S</tt>
   * is a {@linkplain #nodesToPrune node to prune}, then this method
   * will return <tt>true</tt> for the tree <tt>(S (NN rubbish) (. .))</tt>.
   *
   * @param tree the tree to inspect
   * @return whether all nodes of this tree are to be pruned.
   *
   * @see #prune(Sexp)
   */
  public boolean isAllNodesToPrune(Sexp tree) {
    boolean retval = true;
    if (treebank.isPreterminal(tree)) {
      SexpList curr = tree.list();
      retval = (nodesToPrune.contains(curr.get(0)) ||
                wordsToPrune.contains(curr.get(1)));
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      Symbol rootLabel = treeList.symbolAt(0);
      if (nodesToPrune.contains(rootLabel)) {
        retval = true;
      }
      else {
        for (int i = 1; i < treeList.length(); i++) {
          SexpList curr = treeList.listAt(i);
          retval &= isAllNodesToPrune(curr);
          if (retval == false)
            break;
        }
      }
    }
    return retval;
  }

  /**
   * Returns whether the specified tree is to be skipped when training.
   * The default implementation here simply returns the negation of the
   * return value of {@link #isValidTree(Sexp)}.
   *
   * @param tree an annotated training tree
   * @return a string if the specified tree is to be skipped
   * when training, <code>null</code> otherwise
   *
   * @see Trainer#train(SexpTokenizer,boolean,boolean)
   */
  public String skip(Sexp tree) {
    return isValidTree(tree) ? null : "invalid tree";
  }

  /**
   * Transforms nonterminals marked with a subject augmentation so that their
   * unaugmented base label is the concatenation of the original base label
   * plus the subject augmentation.  For example, transforms <tt>NP-SBJ-1</tt>
   * to <tt>NP-SBJ</tt>.
   *
   * @param tree the tree in which to transform subject nonterminals
   * @return the specified tree but with subject nonterminals transformed
   * to remove separators between their base labels and their subject
   * augmentations
   */
  public Sexp transformSubjectNTs(Sexp tree) {
    if (treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();

      for (int childIdx = 1; childIdx < treeListLen; childIdx++)
        transformSubjectNTs(treeList.get(childIdx));

      Symbol parent = tree.list().first().symbol();
      Nonterminal parsedParent =
        Language.treebank().parseNonterminal(parent, nonterminal);
      Symbol sbjAug = Language.treebank().subjectAugmentation();
      if (parsedParent.augmentations.contains(sbjAug)) {
        Symbol newParent = Symbol.add(parsedParent.base + sbjAug.toString());
        parsedParent.base = newParent;
        tree.list().set(0, parsedParent.toSymbol());
      }
    }
    return tree;
  }


  /**
   * Returns <code>true</code> if the specified label is a node to prune.
   */
  /*
  public boolean isNodeToPrune(Symbol label) {
    return nodesToPrune.contains(label);
  }
  */

  /**
   * Returns the set of pruned preterminals (<code>Sexp</code> objects).
   *
   * @see #prune(Sexp)
   */
  public Set getPrunedPreterms() { return prunedPreterms; }

  /**
   * Prunes away subtrees that have a root that is an element of
   * <code>nodesToPrune</code>.
   * <p>
   * <b>Side effect</b>: An internal set of pruned preterminals will
   * be updated.  This set may be accessed via {@link #getPrunedPreterms()}.
   * <p>
   * <b>Bugs</b>: Cannot prune away entire tree if the root label of the
   * specified tree is in <code>nodesToPrune</code>.
   * <p>
   *
   * @param tree the parse tree to prune
   * @return <code>tree</code> having been pruned
   *
   * @see #nodesToPrune
   */
  public Sexp prune(Sexp tree) {
    if (treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      for (int i = 1; i < treeList.length(); i++) {
        SexpList curr = treeList.listAt(i);
        if (nodesToPrune.contains(curr.get(0)) ||
            (treebank.isPreterminal(curr) &&
             wordsToPrune.contains(curr.get(1)))) {
          collectPreterms(prunedPreterms, treeList.get(i));
          treeList.remove(i--);
        }
        else {
          prune(treeList.get(i));
          // if pruning nodes from the current child of treeList leaves
          // a childless child, then remove the current child
          if (treeList.get(i).isList() &&
              treeList.get(i).list().length() == 1) {
            treeList.remove(i--);
          }
        }
      }
    }
    return tree;
  }

  /**
   * Adds all preterminal subtrees to the specified set.
   * @param preterms the set to which preterminal subtrees of the specified
   * tree are to be added
   * @param tree the tree from which to collect preterminal subtrees
   */
  protected final void collectPreterms(Set preterms, Sexp tree) {
    if (treebank.isPreterminal(tree)) {
      preterms.add(tree);
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++)
	collectPreterms(preterms, treeList.get(i));
    }
  }

  /**
   * Augments labels of nonterminals that are arguments.  This method is
   * optional, and may be overridden to simply return <code>tree</code>
   * untouched if argument identification is not desired for a particular
   * language package.
   * <p>
   * Note that children in a coordinated phrase are never relabeled as
   * arguments, as determined by subtrees for which
   * {@link #isCoordinatedPhrase(Sexp,int)} returns <code>true</code>.
   *
   * @param tree the parse tree to modify
   * @return a reference to the modified <code>tree</code> object
   * @see Treebank#canonicalAugDelimiter
   */
  public Sexp identifyArguments(Sexp tree) {
    if (treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();

      // first, make recursive call if not already at max recursion level
      for (int childIdx = 1; childIdx < treeListLen; childIdx++)
	identifyArguments(treeList.get(childIdx));

      // collect *all* candidate pattern lists in argContexts that apply to the
      // current parent: run through each key in argContexts map, and see if it
      // subsumes current parent; if so, add to allCandidatePatterns, which is
      // a list of lists
      Symbol parent = tree.list().first().symbol();
      Nonterminal parsedParent =
	treebank.parseNonterminal(parent, nonterminal);
      SexpList allCandidatePatterns = new SexpList();
      Iterator entries = argContexts.entrySet().iterator();
      while (entries.hasNext()) {
	Map.Entry entry = (Map.Entry)entries.next();
	Symbol key = (Symbol)entry.getKey();
	SexpList currPatternList = (SexpList)entry.getValue();
	Nonterminal parsedKey = treebank.parseNonterminal(key, nonterminal2);
	if (parsedKey.subsumes(parsedParent))
	  allCandidatePatterns.add(currPatternList);
      }

      int headIdx = headFinder.findHead(treeList);

      for (int patternIdx = 0; patternIdx < allCandidatePatterns.length();
	   patternIdx++) {
	SexpList candidatePatterns = allCandidatePatterns.listAt(patternIdx);

	Symbol headChild = (headIdx == 0 ? null :
			    treeList.get(headIdx).list().first().symbol());

	if (isCoordinatedPhrase(tree, headIdx))
	  return tree;

	// either the candidate pattern list has the form (head <int>) or
	// (head-left <list>) or (head-right <list>) or it's a list of actual
	// nonterminal labels
	if (candidatePatterns.first().symbol() == headSym ||
	    candidatePatterns.first().symbol() == headPreSym ||
	    candidatePatterns.first().symbol() == headPostSym) {
	  int argIdx = -1;
	  Symbol child = null;
	  boolean foundArg = false;

	  if (candidatePatterns.first().symbol() == headSym) {
	    String offsetStr = candidatePatterns.get(1).toString();
	    int headOffset = Integer.parseInt(offsetStr);
	    // IF there is a head and IF that head is not equal to parent (i.e.,
	    // if it's not a situation like (PP (PP ...) (CC and) (PP ...)) ) and
	    // if the headIdx plus the headOffset is still valid, then we've got
	    // an argument
	    argIdx = headIdx + headOffset;
	    if (headIdx > 0 &&
		treebank.getCanonical(headChild) != parent &&
		argIdx > 0 && argIdx < treeListLen) {
	      child = treeList.getChildLabel(argIdx);

	      // if the arg is actually a conjunction or punctuation,
	      // OR ANY KIND OF PRETERMINAL,
	      // if possible, find the first child to the left or right of
	      // argIdx that is not a preterminal
	      if (treebank.isPreterminal(child) ||
		  treebank.isConjunction(child) || treebank.isPunctuation(child)) {
		if (headOffset > 0) {
		  for (int i = argIdx + 1; i < treeListLen; i++) {
		    if (!treebank.isPreterminal(treeList.get(i))) {
		      argIdx = i;
		      child = treeList.getChildLabel(argIdx);
		      break;
		    }
		  }
		}
		else {
		  for (int i = argIdx - 1; i >= 0; i--) {
		    if (!treebank.isPreterminal(treeList.get(i))) {
		      argIdx = i;
		      child = treeList.getChildLabel(argIdx);
		      break;
		    }
		  }
		}
	      }
	      foundArg = !treebank.isPreterminal(treeList.get(argIdx));
	    }
	  }
	  else {
	    SexpList searchInstruction = candidatePatterns;
	    int searchInstructionLen = searchInstruction.length();
	    boolean leftSide =
	      searchInstruction.symbolAt(0) == headPreSym;
	    boolean leftToRight =
	      searchInstruction.symbolAt(1) == Constants.firstSym;
	    boolean negativeSearch =
	      searchInstruction.symbolAt(2) == Constants.notSym;
	    int searchSetStartIdx = negativeSearch ? 3 : 2;
	    if (headIdx > 0 && treebank.getCanonical(headChild) != parent) {
	    //if (headIdx > 0) {
	      int increment = leftToRight ? 1 : -1;
	      int startIdx = -1, endIdx = -1;
	      if (leftSide) {
		startIdx = leftToRight ?  1            :  headIdx - 1;
		endIdx   = leftToRight ?  headIdx - 1  :  1;
	      }
	      else {
		startIdx = leftToRight ? headIdx + 1      :  treeListLen - 1;
		endIdx   = leftToRight ? treeListLen - 1  :  headIdx + 1;
	      }
	      // start looking one after (or before) the head index for
	      // first occurrence of a symbol in the search set, which is
	      // comprised of the symbols at indices 2..searchInstructionLen
	      // in the list searchInstruction
	      SEARCH:
	      for (int i = startIdx;
		   leftToRight ? i <= endIdx : i >= endIdx; i += increment) {
		Symbol currChild = treeList.getChildLabel(i);
		Symbol noAugChild = treebank.stripAugmentation(currChild);
		for (int j = searchSetStartIdx; j < searchInstructionLen; j++) {
		  Symbol searchSym = searchInstruction.symbolAt(j);
		  //if (noAugChild == searchSym) {
		  if (currChild == searchSym) {
		    if (negativeSearch)
		      continue SEARCH;
		    else {
		      argIdx = i;
		      child = currChild;
		      foundArg = true;
		      break SEARCH;
		    }
		  }
		}
		// if no match for any nt in search set and this is
		// negative search, we've found what we're looking for
		if (negativeSearch) {
		  argIdx = i;
		  child = currChild;
		  foundArg = true;
		  break SEARCH;
		}
	      }
	    }
	  }

	  if (foundArg) {
	    Nonterminal parsedChild =
	      treebank.parseNonterminal(child, nonterminal);
            addArgAugmentation(child, parsedChild);
	    treeList.setChildLabel(argIdx, parsedChild.toSymbol());
	  }
	}
	else {
	  // the candidate pattern list is a list of actual nonterminal labels

	  // the following line means we only find arguments in situations
	  // where the head child's label is different from the parent label
	  if (treebank.getCanonical(headChild) !=
	      treebank.getCanonical(parent)) {
	  //if (true) {
	    relabelArgChildren(treeList, headIdx, candidatePatterns);
	  }
	}
      }
    }
    return tree;
  }

  /**
   * Relabels as arguments all immediately-dominated children in the specified
   * subtree accoding to the specified argument-finding patterns.
   *
   * @param treeList          the subtree in which to relabel arguments
   * @param headIdx           the index of the child of the specified subtree
   *                          that is the head
   * @param candidatePatterns the set of argument-finding rules
   */
  protected void relabelArgChildren(SexpList treeList,
				    int headIdx,
				    SexpList candidatePatterns) {
    int treeListLen = treeList.length();
    for (int childIdx = 1; childIdx < treeListLen; childIdx++) {
      if (!relabelHeadChildrenAsArgs)
	if (childIdx == headIdx)
	  continue;
      Symbol child = treeList.getChildLabel(childIdx);
      Nonterminal parsedChild =
	treebank.parseNonterminal(child, nonterminal);

      boolean foundCandidate = false;
      int numCandidatePatterns = candidatePatterns.length();
      for (int i = 0; !foundCandidate && i < numCandidatePatterns; i++) {
	Symbol candidatePattern = candidatePatterns.symbolAt(i);
	Nonterminal parsedCandidatePattern =
	  treebank.parseNonterminal(candidatePattern, nonterminal2);
	if (parsedCandidatePattern.subsumes(parsedChild))
	  foundCandidate = true;
      }
      if (foundCandidate) {
	SexpList augmentations = parsedChild.augmentations;
	int augLen = augmentations.length();
	boolean isArg = true;
	for (int i = 0; i < augLen; i++) {
	  if (semTagArgStopSet.contains(augmentations.get(i))) {
	    isArg = false;
	    break;
	  }
	}
	if (isArg) {
	  /*
	  if (childIdx == headIdx)
	    System.out.println(childIdx + "th child (" + parsedChild + ") of " + treeList +
			       " is a head AND an arg!");
	  */
          addArgAugmentation(child, parsedChild);
	  treeList.setChildLabel(childIdx, parsedChild.toSymbol());
	}
      }
    }
  }

  /**
   * Adds the default argument augmentation to the specified nonterminal
   * if the specified label is not already an argument.
   *
   * @param label the label that has been parsed into the specified
   * {@link Nonterminal} object
   * @param nonterminal the parsed version of the specified label
   * @return <tt>true</tt> if the specified {@link Nonterminal} object
   * was modified, <tt>false</tt> otherwise
   */
  protected boolean addArgAugmentation(Symbol label, Nonterminal nonterminal) {
    if (!isArgument(label, nonterminal, false)) {
      treebank.addAugmentation(nonterminal, defaultArgAugmentation);
      return true;
    }
    return false;
  }

  /**
   * The symbol that is used to mark argument (required) nonterminals by
   * {@link #identifyArguments(Sexp)}.
   */
  public Symbol defaultArgAugmentation() { return defaultArgAugmentation; }

  /**
   * Returns the canonical version of the specified argument nonterminal. The
   * default implementation here takes the base of the (possibly compelx)
   * nonterminal label and converts it via {@link Treebank#getCanonical(Symbol)}.
   * For example, in the English Penn Treebank, <tt>S</tt> nonterminals that
   * dominate trees with no subjects get converted to <tt>SG</tt>; if one of
   * these is identified as an argument, it will be converted to <tt>SG-A</tt>;
   * this method will return <tt>S-A</tt>, since <tt>S</tt> is the canonical
   * version of <tt>SG</tt>.  This method is needed by the class {@link
   * SubcatBag}.
   * <br>
   * <b>Implementation note</b>: This method uses an internal
   * cache to perform argument label canonicalizations in O(1) expected time.
   * For cache misses the actual canonicalization is handled by the {@link
   * #getCanonicalArg(Symbol,Nonterminal)} method.
   *
   * @param label the argument nonterminal label to be canonicalized
   * @return the canonical version of the sepcified argument label
   *
   * @see #getCanonicalArg(Symbol, Nonterminal)
   */
  public Symbol getCanonicalArg(Symbol label) {
    Symbol canonicalArgLabel = (Symbol)canonicalArgCache.get(label);
    if (canonicalArgLabel == null) {
      canonicalArgLabel = getCanonicalArg(label, new Nonterminal());
      canonicalArgCache.put(label, canonicalArgLabel);
    }
    return canonicalArgLabel;
  }

  /**
   * Returns the canonical version of the specified argument nonterminal. The
   * default implementation here takes the base of the (possibly compelx)
   * nonterminal label and converts it via {@link Treebank#getCanonical(Symbol)}.
   *  For example, in the English Penn Treebank, <tt>S</tt> nonterminals that
   * dominate trees with no subjects get converted to <tt>SG</tt>; if one of
   * these is identified as an argument, it will be converted to <tt>SG-A</tt>;
   * this method will return <tt>S-A</tt>, since <tt>S</tt> is the canonical
   * version of <tt>SG</tt>.  This method is needed by the class {@link
   * SubcatBag}.
   * @param label the argument nonterminal label whose canonical version is
   * to be returned
   * @param nonterminal the {@link Nonterminal} instance to be used
   * @return the canonical version of the specified argument label
   */
  public Symbol getCanonicalArg(Symbol label, Nonterminal nonterminal) {
    Nonterminal parsedLabel =
      treebank.parseNonterminal(label, nonterminal);
    // strip all augmentations except gap/arg augmentations
    stripAugmentations(label, parsedLabel, false);
    // canonicalize base (unaugmented) label
    parsedLabel.base = treebank.getCanonical(parsedLabel.base);
    return parsedLabel.toSymbol();
  }

  /**
   * Returns <code>true</code> if and only if <code>label</code> has an
   * argument augmentation as added by {@link #identifyArguments(Sexp)}.
   */
  public boolean isArgument(Symbol label) {
    return isArgument(label, nonterminal);
  }

  /**
   * Returns <code>true</code> if the specified nonterminal label has an
   * argument augmentation.  This method is a synonym for
   * <tt>isArgument(label, nonterminal, true)</tt>.
   * <br>
   * <b>Implementation note</b>: This method is <i>not</i> thread-safe;
   * for a thread-safe method, please use {@link #isArgumentFast(Symbol)}.
   * @param label the label to be tested
   * @param nonterminal the {@link Nonterminal} instance to be used for
   * storing the parsed version of the specified nonterminal label
   * @return whether the specified nonterminal label has an argument
   * augmentation
   *
   * @see #isArgument(Symbol,Nonterminal,boolean)
   */
  protected boolean isArgument(Symbol label, Nonterminal nonterminal) {
    return isArgument(label, nonterminal, true);
  }

  /**
   * Returns <code>true</code> if the specified nonterminal label has an
   * argument augmentation.
   * <br>
   * <b>Implementation note</b>: This method is <i>not</i> thread-safe;
   * for a thread-safe method, please use {@link #isArgumentFast(Symbol)}.
   * @param label the label to be tested
   * @param nonterminal the {@link Nonterminal} instance to be used for
   * storing the parsed version of the specified nonterminal label
   * @param parseLabel indicates whether to parse the specified label
   * before checking whether it is an argument
   * @return whether the specified nonterminal label has an argument
   * augmentation
   */
  protected boolean isArgument(Symbol label,
                               Nonterminal nonterminal,
                               boolean parseLabel) {
    Nonterminal parsedLabel =
      parseLabel ? treebank.parseNonterminal(label, nonterminal) : nonterminal;
    // if any of this nonterminal's augmentations is a member of the set
    // of argument augmentations (as contained in the SexpList argAugmentations)
    // then return true; else, return false
    SexpList augmentations = parsedLabel.augmentations;
    for (int i = 0; i < augmentations.length(); i++) {
      if (argAugmentations.contains(augmentations.get(i)))
        return true;
    }
    return false;
  }

  /**
   * Returns <code>true</code> if the specified nonterminal label has an
   * argument augmentation.<br>
   * <b>Implementation note</b>: Unlike {@link #isArgument(Symbol)}, this
   * method is thread-safe.  Also, after {@link #setUpFastArgMap(CountsTable)}
   * has been invoked, this method is much more efficient than
   * {@link #isArgument(Symbol)}, as it uses an internal cache for O(1)
   * expected time operation.
   *
   * @see #setUpFastArgMap(CountsTable)
   */
  public boolean isArgumentFast(Symbol label) {
    if (canUseFastArgMap)
      return fastArgMap.containsKey(label);

    Symbol nonArgLabel = (Symbol)fastArgCache.get(label);
    if (nonArgLabel == null) {
      nonArgLabel = removeArgAugmentation(label);
      fastArgCache.put(label, nonArgLabel);
    }
    return  label != nonArgLabel;
  }

  /**
   * Augments nonterminals to include gap information for WHNP's that have
   * moved and leave traces (gaps), as in the GPSG framework.  This method is
   * optional, and may simply return <code>tree</code> untouched if gap
   * information is desired for a particular language package.  The default
   * implementation of this method checks the setting of the property {@link
   * Settings#addGapInfo}: if this property is <code>false</code>, then
   * <code>tree</code> is returned untouched; otherwise, this method simply
   * calls {@link #hasGap(Sexp,Sexp,ArrayList)}.
   *
   * @param tree the parse tree to which to add gapping
   * @return the same <code>tree</code> that was passed in, with certain
   * nodes modified to include gap information
   * @see #hasGap(Sexp, Sexp, ArrayList)
   */
  public Sexp addGapInformation(Sexp tree) {
    if (!addGapInfo)
      return tree;
    hasGapIndexStack.clear();
    hasGap(tree, tree, hasGapIndexStack);
    return tree;
  }


  /**
   * Returns -1 if <code>tree</code> has no gap (trace), or the index of the
   * trace otherwise.  If <code>tree</code> is a null preterminal with an
   * indexed terminal (a trace) that matches the index at the top of
   * <code>indexStack</code>, then that index is popped off the stack, the
   * preterminal label is changed to be {@link #traceTag}, and the index of the
   * trace is returned.  If a child of <code>tree</code> has a gap but another
   * child is a WHNP that is coindexed, then the gap is &quot;filled&quot;, and
   * this method returns -1; otherwise, this method augments the label of
   * <code>tree</code> with {@link #gapAugmentation} and returns the gap index
   * of the child.
   * <p>
   * Put informally, this method does a depth-first search of <code>tree</code>,
   * pushing the indices of any indexed WHNP nodes onto <code>indexStack</code>
   * and popping off those indices when the corresponding null element is found
   * someplace deeper in the tree.  The stack is necessary to allow for
   * the nesting of gaps in a tree.
   * <p>
   * <b>Algorithm</b>:
   * <pre>
   * <font color=red>// base case</font>
   * <b>if</b> tree is a null-element preterminal with an index that matches top of
   *    indexStack
   * <b>then</b>
   *   modify preterminal to be traceTag;
   *   <b>return</b> pop(indexStack);
   * <b>endif</b>
   *
   * <b>int</b> numWHNPChildren = 0;
   * <b>Sexp</b> whnpChild = <b>null</b>;
   * <b>foreach</b> child <b>of</b> tree <b>do</b>
   *   <b>if</b> child is a WHNP with an index augmentation <b>then</b>
   *     <b>if</b> numWHNPChildren == 0 <b>then</b>
   *       whnpChild = child;
   *     <b>endif</b>
   *     numWHNPChildren++;
   *   <b>endif</b>
   * <b>end</b>
   *
   * <b>if</b> numWHNPChildren &gt; 0 <b>then</b>
   *   push(index of whnpChild, indexStack);
   * <b>endif</b>
   *
   * <b>int</b> numTracesToBeLinked = 0, traceIndex = -1;
   * <b>foreach</b> child <b>of</b> tree <b>do</b>
   *   <b>int</b> gapIndex = hasGap(child, root, indexStack); <font color=red>// recursive call</font>
   *   <b>if</b> gapIndex != -1 <b>then</b>
   *     <b>if</b> numTracesToBeLinked == 0 <b>then</b>
   *       traceIndex = gapIndex;
   *     <b>endif</b>
   *     numTracesToBeLinked++;
   *   <b>endif</b>
   * <b>end</b>
   *
   * <b>if</b> numTracesToBeLinked &gt; 0 <b>then</b>
   *   add gap augmentation to the current parent (the root of <b>tree</b>);
   *   <b>if</b> numWHNPChildren &gt; 0 <b>and</b> index of whnpChild == traceIndex <b>then</b>
   *     <font color=red>// a trace from a child subtree has been hooked up with the current WHNP child</font>
   *     <b>return</b> -1;
   *   <b>else</b>
   *     <b>return</b> traceIndex;
   *   <b>endif</b>
   * <b>else</b>
   *   <b>if</b> numWHNPChildren &gt; 0 <b>then</b>
   *     <b>print</b> warning that a moved WHNP node doesn't have a coindexed trace
   *       in any of its parent's other child subtrees;
   *   <b>endif</b>
   *   <b>return</b> -1;
   * <b>endif</b>
   * </pre>
   * A warning will also be issued if there are crossing WHNP-trace
   * dependencies.
   * <p>
   * This method is called by the default implementation of {@link
   * #addGapInformation}.
   * <p>
   *
   * @param tree the tree to gapify
   * @param root always the root of the tree we're gapifying, for error and
   * warning reporting
   * @param indexStack a stack of <code>Integer</code> objects (where the top
   * of the stack is the highest-indexed object), representing the pending
   * requests to find traces to match with coindexed WHNP's discovered higher
   * up in the tree (earlier in the DFS)
   * @return -1 if <code>tree</code> has no gap, or the index of the trace
   * otherwise
   *
   * @see #gapAugmentation
   * @see #traceTag
   * @see #addGapInformation(Sexp)
   * @see Treebank#isWHNP Treebank.isWHNP(Symbol)
   * @see Treebank#isNullElementPreterminal(Sexp)
   * @see Treebank#getTraceIndex(Sexp, Nonterminal)
   */
  protected int hasGap(Sexp tree, Sexp root, ArrayList indexStack) {
    // System.out.println(tree);

    /*
    if (tree.isSymbol())
      return -1;
    */
    if (treebank.isPreterminal(tree)) {
      if (treebank.isNullElementPreterminal(tree) &&
	  indexStack.size() > 0) {
	int traceIdx = treebank.getTraceIndex(tree, nonterminal);
	Integer indexFound = new Integer(traceIdx);
	Integer lookingFor = (Integer)indexStack.get(indexStack.size() - 1);
	if (traceIdx != -1) {
	  if (indexFound.equals(lookingFor)) {
	    indexStack.remove(indexStack.size() - 1);
	    tree.list().set(0, traceTag);
	    tree.list().set(1, traceTag); // set word to be trace as well
	    return indexFound.intValue();
	  }
	  else if (indexStack.contains(indexFound)) {
	    System.err.println(className + ": warning: crossing " +
			       "WH-movement for tree\n\t" + root);
	  }
	}
      }
      // if either a non-null element preterminal, or the indexStack was empty,
      // or we found a trace whose index didn't match what we were looking for
      return -1;
    }
    if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      Symbol parent = treeList.get(0).symbol();

      int numWHNPChildren = 0;
      int whnpIdx = -1;
      for (int i = 1; i < treeListLen; i++) {
	Symbol child = treeList.getChildLabel(i);
	Sexp firstChildOfChild = treeList.get(i).list().get(1);
	if (treebank.isWHNP(child) &&
	    !treebank.isNullElementPreterminal(firstChildOfChild)) {
	  Nonterminal parsedChild =
	    treebank.parseNonterminal(child, nonterminal);
	  if (parsedChild.index != -1) {
	    if (numWHNPChildren == 0)
	      whnpIdx = parsedChild.index; // only grab leftmost WHNP index
	    numWHNPChildren++;
	  }
	}
      }

      if (numWHNPChildren > 1)
	System.err.println(className + ": warning: multiple WHNP nodes have " +
			   "moved to become children of the same parent (" +
			   parent + ") for tree\n\t" + root);

      if (numWHNPChildren > 0)
	indexStack.add(new Integer(whnpIdx));

      int numTracesToBeLinked = 0;
      int traceIdx = -1, currGapIdx = -1;

      // recursive calls to all children
      for (int i = 1; i < treeListLen; i++) {
	currGapIdx = hasGap(treeList.get(i), root, indexStack);
	if (currGapIdx != -1) {
	  if (numTracesToBeLinked == 0)
	    traceIdx = currGapIdx;
	  numTracesToBeLinked++;
	}
      }

      // don't need to issue warning if numTracesToBeLinked > 1, since
      // we check for crossing WHNP movement in base case above

      if (numTracesToBeLinked > 0) {
	Nonterminal parsedParent =
	  treebank.parseNonterminal(parent, nonterminal);
	treebank.addAugmentation(parsedParent, gapAugmentation);
	treeList.set(0, parsedParent.toSymbol());

	if (numWHNPChildren > 0 && whnpIdx == traceIdx)
	  return -1;
	else
	  return traceIdx;
      }
      else {
	if (numWHNPChildren > 0)
	  System.err.println(className + ": warning: a moved WHNP node with " +
			     "parent " + parent + " and index " + whnpIdx +
			     " doesn't have co-indexed trace in any of its "+
			     "parent's other children in tree\n\t" + root);
	return -1;
      }
    }
    return -1; // should never reach this point
  }

  /**
   * Returns <code>true</code> if and only if <code>label</code> has a
   * gap augmentation as added by {@link #addGapInformation(Sexp)}.
   */
  public boolean hasGap(Symbol label) {
    Nonterminal parsedLabel = treebank.parseNonterminal(label, nonterminal);
    return parsedLabel.augmentations.contains(gapAugmentation);
  }

  /**
   * The symbol that will be used to identify nonterminals whose subtrees
   * contain a gap (a trace).  This method is used by {@link
   * #stripAugmentations(Sexp)}, so that gap augmentations that are added by
   * {@link #addGapInformation(Sexp)} do not get removed.  The default value is
   * the symbol returned by <code>Symbol.add(&quot;g&quot;)</code>.  If this
   * default value conflicts with an augmentation already used in a particular
   * Treebank, the value of the data member {@link #gapAugmentation} should be
   * reassigned in the constructor of a subclass.
   */
  public Symbol gapAugmentation() { return gapAugmentation; }

  /**
   * The symbol that gets reassigned as the part of speech for null
   * preterminals that represent traces that have undergone WH-movement, as
   * relabeled by the default implementation of {@link
   * #addGapInformation(Sexp)}.  The default value is the return value of
   * <code>Symbol.add(&quot;*TRACE*&quot;)</code>.  If this maps to an actual
   * part of speech tag or nonterminal label in a particular Treebank, the
   * data member {@link #traceTag} should be reassigned in the constructor
   * of a subclass.
   */
  public Symbol traceTag() { return traceTag; }

  /**
   * Relabels sentences that have no subjects with the nonterminal label
   * returned by {@link Treebank#subjectlessSentenceLabel}.  This method is
   * optional, and may be overridden to simply return <code>tree</code>
   * untouched if subjectless sentence relabeling is not desired for a
   * particular language package.
   * <p>
   * The default implementation here assumes that a subjectless sentence is a
   * node for which {@link Treebank#isSentence(Symbol)} returns
   * <code>true</code> and has a child with an augmentation for which {@link
   * Treebank#subjectAugmentation} returns <code>true</code>, and that this
   * child represents a subtree that is a series of unary productions, ending in
   * a subtree for which {@link Treebank#isNullElementPreterminal(Sexp)}
   * returns <code>true</code>.  Informally, this method looks for sentence
   * nodes that have a child marked as a subject, where that child has a null
   * element as its first (and presumably only) child.  For example, in the
   * English Treebank, this would mean one of the following contexts:
   * <pre>
   * (S (PREMOD ...) (NP-SBJ (-NONE- *T*)) ... )
   * </pre>
   * or
   * <pre>
   * (S (PREMOD ...) (NP-SBJ (NPB (-NONE- *T*))) ... )
   * </pre>
   * where <tt>(PREMOD ...)</tt> represents zero or more premodifying phrases
   * and where <tt>NPB</tt> represents a node inserted by a method such as
   * {@link #addBaseNPs(Sexp)}.  Note that the subtree rooted by <tt>NPB</tt>
   * satisfies the condition of being a subtree that is the result of a
   * series of unary productions (one of them, in this case) ending
   * in a null element preterminal.  (This seemingly over-complicated condition
   * is necessary for this method to run properly after <code>tree</code>
   * has been processed by {@link #addBaseNPs(Sexp)}.)
   * <p>
   * If a subclass of this class in a language package requires more
   * extensive or different checking for the &quot;subjectlessness&quot; of a
   * sentence, this method should be overridden.
   * <p>
   *
   * @param tree the parse tree in which to relabel subjectless sentences
   * @return the same <code>tree</code> that was passed in, with
   * subjectless sentence nodes relabeled
   * @see Treebank#isSentence(Symbol)
   * @see Treebank#subjectAugmentation
   * @see Treebank#isNullElementPreterminal(Sexp)
   * @see Treebank#subjectlessSentenceLabel
   */
  public Sexp relabelSubjectlessSentences(Sexp tree) {
    if (treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();

      // first, make recursive call
      for (int i = 1; i < treeList.length(); i++)
	relabelSubjectlessSentences(treeList.get(i));

      Symbol parent = treeList.symbolAt(0);
      if (treebank.isSentence(parent)) {
	for (int i = 1; i < treeListLen; i++) {
	  SexpList child = treeList.listAt(i);
	  Symbol childLabel = treeList.getChildLabel(i);
	  Nonterminal parsedChild =
	    treebank.parseNonterminal(childLabel, nonterminal);
	  Symbol subjectAugmentation = treebank.subjectAugmentation();

	  if (parsedChild.augmentations.contains(subjectAugmentation) &&
	      unaryProductionsToNull(child.get(1))) {
	    // we've got ourselves a subjectless sentence!
	    Nonterminal parsedParent = treebank.parseNonterminal(parent,
								 nonterminal);
	    parsedParent.base = treebank.subjectlessSentenceLabel();
	    treeList.set(0, parsedParent.toSymbol());
	    break;
	  }
	}
      }
    }
    return tree;
  }

  /**
   * Returns whether the specified subtree consists solely of unary productions
   * going to a null element terminal.
   *
   * @param tree the subtree to test
   * @return whether the specified subtree consists solely of unary productions
   *         going to a null element terminal.
   */
  protected final boolean unaryProductionsToNull(Sexp tree) {
    if (treebank.isNullElementPreterminal(tree))
      return true;
    else if (tree.isList()) {
      // walk down tree: as soon as a node has more than one child (i.e.,
      // is a list of length > 2) return false; otherwise, continue walking
      // until we hit a preterminal, at which point exit loop and test
      // if null element
      Sexp curr = tree;
      for ( ; !(treebank.isPreterminal(curr)); curr = curr.list().get(1))
	if (curr.list().length() != 2)
	  return false;
      return treebank.isNullElementPreterminal(curr);
    }
    else
      return false;
  }

  /**
   * Strips any augmentations off all of the nonterminal labels of
   * <code>tree</code>.  The set of nonterminal labels does <i>not</i> include
   * preterminals, which are typically parts of speech.  If a particular
   * language's Treebank augments preterminals, this method should be
   * overridden in a language package's subclass. The only augmentations that
   * will not be removed are those that are added by {@link
   * #identifyArguments(Sexp)}, so as to preserve the transformations of that
   * method.  This method should only be called subsequent to the invocations
   * of methods that require augmentations, such as {@link
   * #relabelSubjectlessSentences(Sexp)}.
   *
   * @param tree the tree all of the nonterminals of which are to be stripped
   * of all augmentations except those added by <code>identifyArguments</code>
   * @return a reference to <code>tree</code>
   */
  public Sexp stripAugmentations(Sexp tree) {
    if (treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      Symbol label = tree.list().first().symbol();
      tree.list().set(0, stripAugmentations(label));
      int treeListLen = tree.list().length();
      for (int i = 1; i < treeListLen; i++)
	stripAugmentations(tree.list().get(i));
    }
    return tree;
  }

  /**
   * Parses the specified nonterminal label and removes all augmentations.
   *
   * @param label the label from which to strip all augmentations
   * @return the specified label having been stripped of augmentations
   */
  protected Symbol stripAugmentations(Symbol label) {
    stripAugmentations(label, nonterminal, true);
    return nonterminal.toSymbol();
  }

  /**
   * Fills in the specified {@link Nonterminal} object with the specified
   * nonterminal label but without any augmentations.
   *
   * @param label the label from which to strip augmentations
   * @param nonterminal the {@link Nonterminal} object to use for
   * storage when optionally parsing the specified label and removing
   * all augmentations
   * @param parseLabel indicates whether to call
   * {@link Treebank#parseNonterminal(Symbol)}; if <tt>false</tt>, this
   * method assumes that the specified {@link Nonterminal} object
   * already contains the results of parsing the specified nonterminal
   * label (if this is not the case, then the behavior of this method
   * is undefined)
   */
  protected void stripAugmentations(Symbol label, Nonterminal nonterminal,
                                    boolean parseLabel) {
    if (parseLabel)
      treebank.parseNonterminal(label, nonterminal);
    SexpList augmentations = nonterminal.augmentations;
    for (int i = 0; i < augmentations.length(); i++) {
      Sexp thisAug = augmentations.get(i);
      if (!Language.treebank().isAugDelim(thisAug) &&
          thisAug != gapAugmentation &&
          !argAugmentations.contains(thisAug)) {
        i--; // move index to delimeter that preceded this arg augmentation
        augmentations.remove(i);  // remove delimeter
        augmentations.remove(i);  // remove arg augmentation
        i--; // decrement index again to offset increment in for loop
      }
    }
    // if there's an index, make sure to strip delimeter that precedes it
    if (nonterminal.index >= 0)
      augmentations.remove(augmentations.size() - 1);
    nonterminal.index = -1; // effectively strips off index
  }

  /**
   * Raises punctuation to the highest possible point in a parse tree,
   * resulting in a tree where no punctuation is the first or last child of a
   * non-leaf node.  One consequence is that all punctuation is removed from
   * the beginning and end of the sentence.  The punctuation affected is
   * defined by the implementation of the method {@link
   * Treebank#isPuncToRaise(Sexp)}.
   * <p>
   * <b>Side effect</b>: All preterminals removed from the beginning and end
   * of the sentence are stored in an internal set, which can be accessed
   * via {@link #getPrunedPunctuation()}.
   * <p>
   * Example of punctuation raising:
   * <pre>
   * (S (NP
   *      (NPB Pierre Vinken)
   *      (, ,)
   *      (ADJP 61 years old)
   *      (, ,))
   *    (VP joined (NP (NPB the board))) (. .))
   * </pre>
   * becomes
   * <pre>
   * (S (NP
   *      (NPB Pierre Vinken)
   *      (, ,)
   *      (ADJP 61 years old))
   *    (, ,)
   *    (VP joined (NP (NPB the board))))
   * </pre>
   * This method appropriately deals with the case of having multiple
   * punctuation elements to be raised on the left or right side of the list of
   * children for a nonterminal.  For example, in English, if this method
   * were passed the tree
   * <pre>
   * (S
   *   (NP (DT The) (NN dog) (, ,) (NNP Barky) (. .) (. .) (. .))
   *   (VP (VB was) (ADJP (JJ stupid)))
   *   (. .) (. .) (. .))
   * </pre>
   * the result would be
   * <pre>
   * (S
   *   (NP (DT The) (NN dog) (, ,) (NNP Barky))
   *   (. .) (. .) (. .)
   *   (VP (VB was) (ADJP (JJ stupid))))
   * </pre>
   * <p>
   * <b>Bugs</b>: In the pathological case where all the children of a node
   * are punctuation to raise, this method simply emits a warning to
   * <code>System.err</code> and does not attempt to raise them (which would
   * cause an interior node to become a leaf).
   * <p>
   * @param tree the parse tree to destructively modify by raising punctuation
   * @return a reference to the modified <code>tree</code> object
   */
  public Sexp raisePunctuation(Sexp tree) {
    /*
    leftRaise.clear();
    rightRaise.clear();
    raisePunctuation(tree, leftRaise, rightRaise);
    */
    raise.clear();
    raisePunctuation(tree, raise, Constants.LEFT);
    for (int i = 0; i < raise.length(); i++)
      prunedPunctuation.add(raise.get(i));
    raise.clear();
    raisePunctuation(tree, raise, Constants.RIGHT);
    for (int i = 0; i < raise.length(); i++)
      prunedPunctuation.add(raise.get(i));
    return tree;
  }

  private void raisePunctuation(Sexp tree, SexpList raise, boolean direction) {
    if (tree.isSymbol() || treebank.isPreterminal(tree))
      return;
    if (allChildrenPuncToRaise(tree)) {
      System.err.println(Training.class.getName() +
			 ": warning: all children are punctuation to raise\n" +
			 "\t" + tree);
      return;
    }

    // if tree is a list with at least two children (i.e., of length 3)
    if (tree.isList()) {
      SexpList treeList = tree.list();

      boolean leftToRight = direction == Constants.LEFT;
      int startIdx = (leftToRight ? 1 : treeList.length() - 1);
      int endIdx = (leftToRight ? treeList.length() - 1 : 1);
      int increment = (leftToRight ? 1 : -1);
      for (int i = startIdx;
	   (leftToRight && i <= endIdx) || (!leftToRight && i >= endIdx);
	   i += increment) {
	// we're occasionally removing items from tree, so recalculate these
	startIdx = (leftToRight ? 1 : treeList.length() - 1);
	endIdx = (leftToRight ? treeList.length() - 1 : 1);
	Sexp currChild = treeList.get(i);

	raisePunctuation(currChild, raise, direction);

	// if it's the last child that we're visiting
	if (i == endIdx) {
	  // while there's ending punctuation, remove it and add it
	  // to the raise queue (in queue order, hence the call to
	  // reverse, since we're grabbing nodes from the outside in)
	  addToRaise.clear();
	  while (treeList.length() > 1 &&
		 treebank.isPuncToRaise(treeList.get(endIdx))) {
	    addToRaise.add(treeList.remove(endIdx));
	    endIdx = (leftToRight ? treeList.length() - 1 : 1);
	  }
	  // set i to the new last element index (not strictly necessary)
	  if (raise.addAll(addToRaise.reverse()))
	    i = endIdx;
	}
	// if it's not the last child we're visiting at this level and
	// there are raise requests enqueued, we can oblige by
	// inserting the punctuation just after the current node
	else if (raise.length() != 0) {
	  // since items naturally shift rightward when we use the add method,
	  // to add "after" current node in a right-to-left traversal requires
	  // keeping add index the same (i.e., an increment of zero)
	  int insertIncrement = (leftToRight ? increment : 0);
	  i += insertIncrement;
	  for (int raiseIdx = 0; raiseIdx < raise.length(); raiseIdx++) {
	    treeList.add(i, raise.get(raiseIdx));
	    if (leftToRight)
	      i += insertIncrement;
	  }
	  endIdx = (leftToRight ? treeList.length() - 1 : 1);
	  i -= insertIncrement; // offset increment for enclosing for loop
	  raise.clear();
	}
      }
    }
  }

  private static boolean allChildrenPuncToRaise(Sexp tree) {
    int treeLen = tree.list().length();
    for (int i = 1; i < treeLen; i++)
      if (Language.treebank().isPuncToRaise(tree.list().get(i)) == false)
	return false;
    return true;
  }

  /**
   * Returns the set of preterminals (<code>Sexp</code> objects) that were
   * punctuation elements that were "raised away" because they were either at
   * the beginning or end of a sentence.
   *
   * @see #raisePunctuation(Sexp)
   */
  public Set getPrunedPunctuation() { return prunedPunctuation; }

  /**
   * Adds and/or relabels base NPs, which are defined in this default
   * implementation to be NPs that do not dominate other non-possessive NPs,
   * where a possessive NP is defined to be an NP that itself dominates
   * a possessive preterminal, as determined by the implementation of the
   * method {@link Treebank#isPossessivePreterminal(Sexp)}.  If an NP
   * is relabeled as a base NP but is not dominated by another NP, then
   * a new NP is interposed, for the sake of consistency.  For example,
   * if the specified tree is the English Treebank tree
   * <pre>
   * (S (NP-SBJ (DT The) (NN dog)) (VP (VBD sat)))
   * </pre>
   * then this method will transform it to be
   * <pre>
   * (S (NP-SBJ (NPB (DT The) (NN dog))) (VP (VBD sat)))
   * </pre>
   * Note that the <tt>SBJ</tt> augmentation is transferred to the
   * enclosing NP.
   *
   * @param tree the parse tree in which to add and/or relabel base NPs
   * @return a reference to the modified version of <code>tree</code>
   *
   * @see #hasPossessiveChild(Sexp)
   * @see Treebank#isNP(Symbol)
   * @see Treebank#baseNPLabel
   * @see Treebank#NPLabel
   */
  public Sexp addBaseNPs(Sexp tree) {
    return addBaseNPs(null, -1, tree);
  }

  private Sexp addBaseNPs(Sexp grandparent, int parentIdx, Sexp tree) {
    if (treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      if (tree.list().first().isList())
	System.err.println("error: tree " + tree + " has bad format");
      if (treebank.isNP(tree.list().first().symbol())) {
	Symbol parent = treeList.first().symbol();
	boolean parentIsBaseNP = true;
	for (int i = 1; i < treeListLen; i++) {
	  // if a child is an NP that is NOT a possessive NP (i.e., the child
	  // does not itself have a child that is a possessive preterminal)
	  // then parent is NOT a baseNP
	  if (treebank.isNP(treeList.getChildLabel(i)) &&
	      hasPossessiveChild(treeList.get(i)) == false) {
	    parentIsBaseNP = false;
	    break;
	  }
	}
	if (parentIsBaseNP) {
	  Nonterminal parsedParent =
	    treebank.parseNonterminal(parent, nonterminal);
	  if (parsedParent.augmentations.length() == 0 &&
	      parsedParent.index == -1)
	    treeList.set(0, baseNP);
	  else {
	    parsedParent.base = baseNP;
	    treeList.set(0, parsedParent.toSymbol());
	  }
	  // if the grandparent is not an NP, we need to add a normal NP level
	  // transferring any augmentations to the new parent from the current
	  if (needToAddNormalNPLevel(grandparent, parentIdx, tree)) {
	    if (grandparent != null) {
	      SexpList newParent = new SexpList(2);
	      treeList.set(0, baseNP);
	      parsedParent.base = NP;
	      newParent.add(parsedParent.toSymbol()).add(tree);
	      grandparent.list().set(parentIdx, newParent);
	    }
	    else {
	      // make parent back into NP
	      parsedParent.base = NP;
	      treeList.set(0, parsedParent.toSymbol());
	      // now, take all parent's children and add them as
	      // children of a new node that will be the new base NP
	      SexpList baseNPNode = new SexpList(treeListLen);
	      baseNPNode.add(baseNP);
	      for (int j = 1; j < treeListLen; j++)
		baseNPNode.add(treeList.get(j));
	      for (int j = treeListLen - 1; j >= 1; j--)
		treeList.remove(j);
	      // finally, add baseNPNode as sole child of current parent
	      treeList.add(baseNPNode);
	    }
	  }
	}
      }
      for (int i = 1; i < treeListLen; i++)
	addBaseNPs(tree, i, treeList.get(i));
    }
    return tree;
  }

  /**
   * Changes the specified tree so that when the last child of an
   * NPB is an S, the S gets raised to be a sibling immediately following
   * the NPB.  That is, situations such as
   * <pre>
   * (NP
   *   (NPB
   *     (DT an)
   *     (NN effort)
   *     (S ...)))
   * </pre>
   * get transformed to
   * <pre>
   * (NP
   *   (NPB
   *     (DT an)
   *     (NN effort))
   *   (S ...))
   * </pre>
   *
   * @param tree the tree whose base NPs are to be repaired
   * @return a modified version of the specified tree
   */
  public Sexp repairBaseNPs(Sexp tree) {
    if (repairBaseNPs)
      repairBaseNPs(null, -1, tree);
    return tree;
  }

  /**
   * Changes the specified tree so that when the last child of an
   * NPB is an S, the S gets raised to be a sibling immediately following
   * the NPB.  That is, situations such as
   * <pre>
   * (NP
   *   (NPB
   *     (DT an)
   *     (NN effort)
   *     (S ...)))
   * </pre>
   * get transformed to
   * <pre>
   * (NP
   *   (NPB
   *     (DT an)
   *     (NN effort))
   *   (S ...))
   * </pre>
   *
   * @param grandparent the grandparent of the specified tree, or
   * <code>null</code> if the specified tree is the root
   * @param parentIdx the index of the specified tree in the
   * the specified grandparent's list of children
   * @param tree the tree in which to repair base NPs
   */
  protected Sexp repairBaseNPs(Sexp grandparent, int parentIdx, Sexp tree) {
    if (treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      Symbol lastChild = treeList.getChildLabel(treeListLen - 1);
      boolean thereAreAtLeastTwoChildren = treeListLen > 2;
      if (thereAreAtLeastTwoChildren &&
	  treebank.isBaseNP(treeList.first().symbol()) &&
	  isTypeOfSentence(lastChild)) {
	if (grandparent != null) {
	  // add the final S of this base NP as a sibling immediately
	  // following this base NP
	  grandparent.list().add(parentIdx + 1,
				 treeList.remove(--treeListLen));
	}
	else {
	  System.err.println(className + ": repairBaseNPs: " +
			     "warning: found NPB without parent");
	}
      }
      for (int i = 1; i < treeList.length(); i++)
	repairBaseNPs(tree, i, treeList.get(i));
    }
    return tree;
  }

  /**
   * Adds any argument augmentations on an NP to its head child, continuing
   * recursively until reaching a preterminal.
   */
  protected Sexp threadNPArgAugmentations(Sexp tree) {
    if (treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      Symbol parent  = treeList.symbolAt(0);
      if (treebank.isNP(parent) && isArgumentFast(parent)) {
	int headIdx = headFinder.findHead(treeList);
	Symbol headChildLabel = treeList.getChildLabel(headIdx);
	if (treebank.isNP(headChildLabel) && !isArgumentFast(headChildLabel)) {
	  // smash head child's tree's root label to have same arg augmentation
	  // as current parent
	  Nonterminal parsedParent =
	    treebank.parseNonterminal(parent, nonterminal);
	  Nonterminal parsedHeadChild =
	    treebank.parseNonterminal(headChildLabel, nonterminal2);
	  // go through each aug of parsedParent: if it is an arg aug,
	  // add to aug list of head child
	  SexpList parentAugs = parsedParent.augmentations;
	  SexpList headAugs = parsedHeadChild.augmentations;
	  for (int augIdx = 0; augIdx < parentAugs.length(); augIdx++) {
	    if (argAugmentations.contains(parentAugs.get(augIdx))) {
	      // add both delimiter and arg aug to headChild's aug list
	      headAugs.add(parentAugs.get(augIdx - 1));
	      headAugs.add(parentAugs.get(augIdx));
	    }
	  }
	  // finally, smash label of head child subtree
	  treeList.listAt(headIdx).set(0, parsedHeadChild.toSymbol());
	}
      }
      for (int i = 1; i < treeListLen; i++)
	threadNPArgAugmentations(treeList.get(i));
    }
    return tree;
  }

  /**
   * A helper method used by {@link #repairBaseNPs(Sexp,int,Sexp)}.
   * While the default implementation here simply returns the result of
   * calling {@link Treebank#isSentence(Symbol)} with the specified label,
   * subclasses may override this method if different semantics are required
   * for identifying sentences that occur as siblings of base NPs.
   *
   * @param label the nonterminal label to test
   * @return <code>true</code> if the specified nonterminal represents a
   * sentence, <code>false</code> otherwise
   */
  protected boolean isTypeOfSentence(Symbol label) {
    return treebank.isSentence(label);
  }

  /**
   * Returns <code>true</code> if a unary NP needs to be added above the
   * specified base NP.
   *
   * @param grandparent the parent of the &quot;parent&quot; that is a
   * base NP
   * @param parentIdx the index of the child of <code>grandparent</code>
   * that is the base NP (that is,
   * <pre>grandparent.list().get(parentIdx) == tree</pre>
   * @param tree the base NP, whose parent is <code>grandparent</code>
   */
  protected boolean needToAddNormalNPLevel(Sexp grandparent,
					   int parentIdx, Sexp tree) {
    if (grandparent == null)
      return true;

    SexpList grandparentList = grandparent.list();
    if (!treebank.isNP(grandparentList.symbolAt(0)))
      return true;
    int headIdx = headFinder.findHead(grandparent);
    return (isCoordinatedPhrase(grandparent, headIdx) ||
	    (headIdx != parentIdx && grandparentList.symbolAt(0) != baseNP));
  }

  /**
   * Returns <code>true</code> if a non-head child of the specified
   * tree is a conjunction, and that conjunction is either post-head
   * but non-final, or immediately pre-head but non-initial (where
   * &quot;immediately pre-head&quot; means &quot;at the first index
   * less than <code>headIdx</code> that is not punctuation, as determined
   * by {@link Treebank#isPunctuation(Symbol)}).  A child is a
   * conjunction if its label is one for which
   * {@link Treebank#isConjunction(Symbol)} returns <code>true</code>.
   *
   * @param tree the (sub)tree to test
   * @param headIdx the index of the head child of the specified tree */
  protected boolean isCoordinatedPhrase(Sexp tree, int headIdx) {
    SexpList treeList = tree.list();
    int treeListLen = treeList.length();

    int conjIdx = -1;
    // first, search everything post-head except final child,
    // since conj must not be final for tree to be a true coordinated phrase
    int lastChildIdx = treeListLen - 1;
    for (int i = headIdx + 1; i < lastChildIdx; i++) {
      if (treebank.isConjunction(treeList.getChildLabel(i))) {
	conjIdx = i;
	break;
      }
    }
    // if first search didn't succeed, search immediately pre-head
    if (conjIdx == -1) {
      int i = headIdx - 1;
      // skip all punctuation immediately preceding head
      while (i > 1 && treebank.isPunctuation(treeList.getChildLabel(i)))
	i--;
      // conj must not be initial for tree to be a coordinated phrase
      if (i > 1 && treebank.isConjunction(treeList.getChildLabel(i)))
	conjIdx = i;
    }

    return conjIdx != -1;
  }


  /**
   * Returns <code>true</code> if <code>tree</code> contains a child for which
   * {@link Treebank#isPossessivePreterminal(Sexp)} returns
   * <code>true</code>, <code>false</code> otherwise.  This is a helper method
   * used by the default implementation of {@link #addBaseNPs(Sexp)}.
   * Possessive children are often more even-tempered than possessive parents.
   *
   * @param tree the parse subtree to check for possessive preterminal
   * children
   */
  protected boolean hasPossessiveChild(Sexp tree) {
    if (treebank.isPreterminal(tree))
      return false;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++)
	if (treebank.isPossessivePreterminal(treeList.get(i)))
	  return true;
    }
    return false;
  }

  /**
   * Removes all null elements, that is, those nodes of <code>tree</code> for
   * which {@link Treebank#isNullElementPreterminal(Sexp)} returns
   * <code>true</code>.  Additionally, if the removal of a null element leaves
   * an interior node that is childless, then this interior node is removed as
   * well.  For example, if we have the following sentence in English
   * <pre> (S (NP-SBJ (-NONE- *T*)) (VP ...)) </pre>
   * it will be transformed to be
   * <pre> (S (VP ...)) </pre>
   * <b>N.B.</b>: This method should only be invoked <i>after</i> preprocessing
   * with {@link #relabelSubjectlessSentences(Sexp)} and {@link
   * #addGapInformation(Sexp)}, as these methods (and possibly others, if
   * overridden) rely on the presence of null elements.
   *
   * @see Treebank#isNullElementPreterminal(Sexp)
   */
  public Sexp removeNullElements(Sexp tree) {
    if (treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      for (int i = 1; i < treeList.length(); i++) {
	if (treebank.isNullElementPreterminal(treeList.get(i))) {
	  treeList.remove(i--); // postdecrement i to offset for loop increment
	}
	else {
	  removeNullElements(treeList.get(i));
	  // if removing null preterminals from the current child yields
	  // a childless child, then remove the current child
	  if (treeList.get(i).isList() &&
	      treeList.get(i).list().length() == 1)
	    treeList.remove(i--);
	}
      }
    }
    return tree;
  }

  // some data accessor methods

  /**
   * Returns the symbol to indicate hidden nonterminals that precede the first
   * in a sequence of modifier nonterminals.  The default value is the return
   * value of <code>Symbol.add(&quot;+START+&quot;)</code>; if this value
   * conflicts with an actual nonterminal in a particular Treebank, then this
   * method should be overridden.
   *
   * @see Trainer
   */
  public Symbol startSym() { return startSym; }
  /**
   * Returns the <code>Word</code> object that represents the hidden "head
   * word" of the start symbol.
   *
   * @see #startSym
   * @see Trainer
   */
  public Word startWord() { return startWord; }
  /**
   * Returns the symbol to indicate a hidden nonterminal that follows the last
   * in a sequence of modifier nonterminals.  The default value is the return
   * value of <code>Symbol.add(&quot;+STOP+&quot;)</code>; if this value
   * conflicts with an actual nonterminal in a particular Treebank, then this
   * method should be overridden.
   * <p>
   * This symbol may also be used as a special value that is guaranteed not
   * to conflict with any nonterminal in a given language's treebank.
   * <p>
   *
   * @see Trainer
   */
  public Symbol stopSym() { return stopSym; }
  /**
   * Returns the <code>Word</code> object that represents the hidden "head
   * word" of the stop symbol.
   *
   * @see #stopSym
   * @see Trainer
   */
  public Word stopWord() { return stopWord; }
  /**
   * Returns the symbol to indicate the hidden root of all parse trees.  The
   * default value is the return value of
   * <code>Symbol.add(&quot;+TOP+&quot;)</code>; if this value conflicts with
   * an actual nonterminal in a particular Treebank, then this method should be
   * overridden.
   *
   * @see Trainer
   */
  public Symbol topSym() { return topSym; }

  /**
   * Returns the <code>Word</code> object that represents the hidden "head
   * word" of the hidden root of all parse trees.
   */
  public Word topWord() { return topWord; }


  /**
   * Returns the symbol used in the {@link #argContexts} map to identify
   * an offset from the head child.
   */
  public Symbol headSym() { return headSym; }

  /**
   * The symbol that is a possible mapping {@link #argContexts} to indicate
   * to choose a child relative to the left side of the head as an argument.
   * For example, an argument context might be <code>VP</code> mapping to
   * <code>(head-left left MD VBD)</code>, meaning that the children to the left
   * of the head child should be searched from left to right, and the first
   * child found that is a member of the set <tt>{MD, VBD}</tt> should be
   * considered a possible argument of the head.
   */
  public Symbol headPreSym() { return headPreSym; }

  /**
   * The symbol that is a possible mapping {@link #argContexts} to indicate to
   * choose a child relative to the right side of the head as an argument. For
   * example, an argument context might be <code>PP</code> mapping to
   * <code>(head-right left PP NP WHNP ADJP)</code>, meaning that the children
   * to the right of the head child should be searched from left to right, and
   * the first child found that is a member of the set <tt>{PP, NP, WHNP,
   * ADJP}</tt> should be considered a possible argument of the head.
   */
  public Symbol headPostSym() { return headPostSym; }

  /**
   * A helper method that runs through every nonterminal "pattern" for each
   * context in {@link #argContexts}, parses the pattern using {@link
   * Treebank#parseNonterminal}, runs through the resulting list of
   * augmentations and adds each augmentation symbol to the {@link
   * #argAugmentations} list.
   */
  protected void createArgAugmentationsList() {
    Iterator args = argContexts.values().iterator();
    while (args.hasNext()) {
      SexpList argList = (SexpList)args.next();
      Symbol first = argList.symbolAt(0);
      if (first != headSym && first != headPreSym && first != headPostSym) {
        int argListLen = argList.length();
        for (int i = 0; i < argListLen; i++) {
          Symbol argLabel = argList.symbolAt(i);
          Nonterminal parsedArgLabel = treebank.parseNonterminal(argLabel);
          SexpList augmentations = parsedArgLabel.augmentations;
          for (int augIdx = 0; augIdx < augmentations.length(); augIdx++) {
            Sexp thisAug = augmentations.get(augIdx);
            if (!treebank.isAugDelim(thisAug))
              argAugmentations.add(thisAug);
          }
        }
      }
    }
  }

  /**
   * Sets the {@link #argNonterminals} data member to be the static set
   * of argument nonterminals.  The default implementation here scans the
   * {@link #argContexts} list, and adds every nonterminal "pattern" for a
   * given context to the set.  If the nonterminal to be added is not
   * already an argument as determined by {@link #isArgument}, then the
   * {@link Treebank#canonicalAugDelimiter} and {@link #defaultArgAugmentation}
   * are appended before it is added to the set. This default implementation,
   * therefore, does not necessarily return a complete set of all possible arg
   * nonterminals, but merely those that are explicitly named in the
   * argument-finding contexts. As this method is primarily intended to be
   * used by {@link SubcatBag} when setting up its static resources for
   * categorizing argument nonterminals, this implementation is sufficient,
   * as all nonterminals that are not explicitly named will be thrown into
   * the miscellaneous category.
   */
  protected void createArgNonterminalsSet() {
    argNonterminals = new HashSet();
    Iterator args = argContexts.values().iterator();
    while (args.hasNext()) {
      SexpList argList = (SexpList)args.next();
      Symbol first = argList.symbolAt(0);
      if (first != headSym && first != headPreSym && first != headPostSym) {
        int argListLen = argList.length();
        for (int i = 0; i < argListLen; i++) {
          Symbol argLabel = argList.symbolAt(i);
          Nonterminal parsedArgLabel = treebank.parseNonterminal(argLabel);
          // if this is not an arg label indicating to look for a semantic tag
          if (parsedArgLabel.base != Constants.kleeneStarSym) {
            if (addArgAugmentation(argLabel, parsedArgLabel))
              argLabel = parsedArgLabel.toSymbol();
            argNonterminals.add(argLabel);
          }
        }
      }
    }
  }

  /**
   * Returns a static set of possible argument nonterminals.  The default
   * implementation here invokes {@link #createArgNonterminalsSet()} if
   * the {@link #argNonterminals} data member has not been initialized
   * (that is, if it is <code>null</code>).
   *
   * @return a static set of possible argument nonterminals
   */
  public synchronized Set argNonterminals() {
    if (argNonterminals == null) {
      createArgNonterminalsSet();
      argNonterminals = Collections.unmodifiableSet(argNonterminals);
    }
    return argNonterminals;
  }

  // a couple of utility methods for removing gap/arg augmentations
  // very efficiently

  public Symbol removeArgAugmentation(Symbol label) {
    if (canUseFastArgMap) {
      Map.Entry entry = fastArgMap.getEntry(label);
      return entry == null ? label : (Symbol)entry.getValue();
    }

    Symbol nonArgLabel = (Symbol)fastArgCache.get(label);
    if (nonArgLabel == null) {
      nonArgLabel = removeArgAugmentation(label, new Nonterminal());
      fastArgCache.put(label, nonArgLabel);
    }
    return nonArgLabel;
  }

  /**
   * Parses label into the specified {@link Nonterminal} object and then
   * removes all argument augmentations.
   *
   * @param label the label from which to remove argument augmentations
   * @param nonterminal the object to use as temporary storage during
   * execution of this method
   * @return the symbol that is the specified label removed of its
   * argument augmentations
   */
  protected Symbol removeArgAugmentation(Symbol label,
                                         Nonterminal nonterminal) {
    Nonterminal parsedLabel = treebank.parseNonterminal(label, nonterminal);
    boolean removedSomething = false;
    SexpList augmentations = parsedLabel.augmentations;
    for (int i = 0; i < augmentations.length(); i++) {
      if (argAugmentations.contains(augmentations.get(i))) {
        i--; // move index to delimeter that preceded this arg augmentation
        augmentations.remove(i);  // remove delimeter
        augmentations.remove(i);  // remove arg augmentation
        removedSomething = true;
        i--; // decrement index again to offset increment in for loop
      }
    }
    return removedSomething ? parsedLabel.toSymbol() : label;
  }

  /**
   * If the specified S-expression is a list, this method modifies the
   * list to contain only symbols without gap augmentations;
   * otherwise, this method removes the gap augmentation (if one exists)
   * in the specified symbol and returns that new symbol.  Note that
   * the presence of gap augmentations is determined by matching for
   * {@link #delimAndGapStr}, which means that symbols consisting solely
   * of the gap augmentation itself ({@link #gapAugmentation}) will
   * be unaffected.
   *
   * @param sexp a symbol or list of symbols from which to remvoe any
   * gap augmentations
   * @return a symbol or list of symbols with no gap augmentations
   */
  public Sexp removeGapAugmentation(Sexp sexp) {
    if (!addGapInfo)
      return sexp;
    if (sexp.isSymbol())
      return symRemoveGapAugmentation(sexp.symbol());
    else
      return listRemoveGapAugmentation(sexp.list());
  }

  private Symbol symRemoveGapAugmentation(Symbol label) {
    int gapAugIdx = label.toString().indexOf(delimAndGapStr);
    if (gapAugIdx != -1) {
      String labelStr = label.toString();
      /*
      StringBuffer sb = new StringBuffer(labelStr.length() + 1);
      sb.append(labelStr);
      sb.delete(gapAugIdx, gapAugIdx + delimAndGapStrLen);
      */
      StringBuffer sb =
	new StringBuffer(labelStr.length() - delimAndGapStrLen).
	  append(labelStr.substring(0, gapAugIdx)).
	  append(labelStr.substring(gapAugIdx + delimAndGapStrLen));
      return Symbol.add(sb.toString());
    }
    else
      return label;
  }

  private SexpList listRemoveGapAugmentation(SexpList list) {
    int listLen = list.length();
    for (int i = 0; i < listLen; i++)
      list.set(i, symRemoveGapAugmentation(list.symbolAt(i)));
    return list;
  }

  public void postProcess(Sexp tree) {
    removeOnlyChildBaseNPs(tree);
    canonicalizeNonterminals(tree);
  }

  /**
   * Handle case where an NP dominates a base NP and has no other children
   * (the base NP is an "only child" of the dominating NP).  This method
   * will effectively remove the base NP node, hooking up all its children
   * as the children of the parent NP.
   */
  protected void removeOnlyChildBaseNPs(Sexp tree) {
    Treebank treebank = Language.treebank();
    if (treebank.isPreterminal(tree))
      return;
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++) {
	Sexp currChild = treeList.get(i);
	boolean currChildRemoved = false;
	boolean hasOnlyChild = treeListLen == 2;
	if (hasOnlyChild &&
	    !treebank.isPreterminal(currChild) && currChild.isList()) {
	  Symbol parentLabel = treebank.stripAugmentation(treeList.symbolAt(0));
	  if (parentLabel == treebank.NPLabel()) {
	    Symbol childLabel =
	      treebank.stripAugmentation(currChild.list().symbolAt(0));
	    if (treebank.isBaseNP(childLabel)) {
	      // we've got an NP dominating an only child base NP!
	      // first, remove baseNP from parent
	      currChildRemoved = true;
	      treeList.remove(i);
	      // next, make all baseNP's children become parent's children
	      SexpList childList = currChild.list();
	      int childListLen = childList.length();
	      for (int j = 1; j < childListLen; j++)
		treeList.add(childList.get(j));
	      // need to re-calculate cached length
	      treeListLen = treeList.length();
	    }
	  }
	}
	if (!currChildRemoved)
	  removeOnlyChildBaseNPs(currChild);
      }
    }
  }

  /**
   * Modifies each nonterminal in the specified tree to be its canonical
   * version.
   * @param tree the tree whose nonterminals are to be converted to their
   * canonical versions
   *
   * @see Treebank#getCanonical(Symbol)
   */
  protected void canonicalizeNonterminals(Sexp tree) {
    if (treebank.isPreterminal(tree)) {
      SexpList treeList = tree.list();
      Symbol currLabel = treeList.symbolAt(0);
      treeList.set(0, treebank.getCanonical(currLabel));
      return;
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      Symbol currLabel = treeList.symbolAt(0);
      treeList.set(0, treebank.getCanonical(currLabel));
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++)
	canonicalizeNonterminals(treeList.get(i));
    }
  }

  // main stuff
  private static String filename = null;
  private static boolean quiet = false;

  private static void processArgs(String[] args) {
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-v") ||
	  args[i].equals("-help") ||
	  args[i].equals("-usage"))
	usage();
      else if (args[i].equals("-q"))
	quiet = true;
      else
	filename = args[i];
    }
  }

  private static String[] usageMsg = {
    "usage: [-v|-help|-usage] [-q] [<filename> | -]",
    "where",
    "\t-v,-help,-usage: prints this message",
    "\t<filename> is the name of an input file",
    "\t-: indicates to use standard input",
    "\t-q: indicates to be quiet when encountering invalid sentences"
  };

  private static void usage() {
    for (int i = 0; i < usageMsg.length; i++)
      System.err.println(usageMsg[i]);
    System.exit(1);
  }

  /**
   * Test driver for this class.  Currently, this method reads in a file
   * containing parse trees, invokes the {@link #preProcess(Sexp)} method on
   * them, and then outputs the resulting trees to standard out.
   * Usage: &lt;filename&gt;, where &lt;filename&gt; contains S-expressions
   * representing trees.
   */
  public static void main(String[] args) {
    processArgs(args);
    int treeNum = 1;
    Sexp curr = null;
    try {
      InputStream in =
	filename == null ? System.in : new FileInputStream(filename);
      SexpTokenizer tok = new SexpTokenizer(in, Language.encoding(),
					    Constants.defaultFileBufsize);
      OutputStreamWriter errosw =
	new OutputStreamWriter(System.err, Language.encoding());
      PrintWriter err = new PrintWriter(errosw, true);
      OutputStreamWriter osw =
	new OutputStreamWriter(System.out, Language.encoding());
      PrintWriter out = new PrintWriter(new BufferedWriter(osw), true);
      int line = tok.lineno();
      Training training = (Training)Language.training();
      curr = null;
      for (treeNum = 1; (curr = Sexp.read(tok)) != null; treeNum++) {
	if (curr.isList()) {
	  // automatically determine whether to strip outer parens
	  Sexp tree = curr;
	  if (curr.list().length() == 1 && curr.list().get(0).isList())
	    tree = tree.list().get(0);
	  //err.println("tree No. " + treeNum);
	  String skipStr = training.skip(tree);
	  if (skipStr == null) {
	    out.println(training.preProcess(tree));
	  }
	  else if (!quiet)
	    err.println("tree No. " + treeNum + " from line " + line +
			" invalid: " + skipStr + "\n\t" + tree);
	}
	else if (!quiet)
	  err.println("S-expression No. " + treeNum + " from line " +
		      line + ": not list: " + curr);
	line = tok.lineno();
      }
    }
    catch (UnsupportedEncodingException uee) {
      System.err.println(uee);
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(fnfe);
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
  }

  /**
   * A hook for subclasses to have their own custom metadata types.
   * The default version here does nothing.
   *
   * @param dataType the symbol representing the data type for this
   * metadata entry
   * @param metadataLen the length of the list of the specified
   * metadata entry
   * @param metadata the list of a metadata entry to be processed
   * by a subclass, if the data type is recognized
   */
  protected void readMetadataHook(Symbol dataType,
				  int metadataLen, SexpList metadata) {
  }

  /**
   * Reads metadata to fill in {@link #argContexts} and
   * {@link #semTagArgStopSet}.  Does no format
   * checking on the S-expressions of the metadata resource.
   *
   * @param metadataTok tokenizer for stream of S-expressions containing
   * metadata for this class
   */
  protected void readMetadata(SexpTokenizer metadataTok) throws IOException {
    Sexp metadataSexp = null;
    while ((metadataSexp = Sexp.read(metadataTok)) != null) {
      SexpList metadata = metadataSexp.list();
      int metadataLen = metadata.length();
      Symbol dataType = metadata.first().symbol();
      if (dataType == argContextsSym) {
	for (int i = 1; i < metadataLen; i++) {
	  SexpList context = metadata.get(i).list();
	  argContexts.put(context.get(0), context.get(1));
	}
      }
      else if (dataType == semTagArgStopListSym) {
	SexpList semTagArgStopList = metadata.get(1).list();
	for (int i = 0; i < semTagArgStopList.length(); i++)
	  semTagArgStopSet.add(semTagArgStopList.get(i));
      }
      else if (dataType == nodesToPruneSym) {
	SexpList nodesToPruneList = metadata.get(1).list();
	for (int i = 0; i < nodesToPruneList.length(); i++)
	  nodesToPrune.add(nodesToPruneList.get(i));
      }
      else {
	// unrecognized data type: call hook for subclass to (potentially)
	// recognize
	readMetadataHook(dataType, metadataLen, metadata);
      }
    }
    createArgAugmentationsList();
  }

  /** Debugging method to print the metadata used by this class. */
  public void printMetadata() {
    Iterator argContextsIt = argContexts.keySet().iterator();
    while (argContextsIt.hasNext()) {
      Sexp parent = (Sexp)argContextsIt.next();
      System.err.println("parent: " + parent + "\t" +
			 "children: " + argContexts.get(parent));
    }
    Iterator argStopSetIt = semTagArgStopSet.iterator();
    System.err.print("(");
    if (argStopSetIt.hasNext())
      System.err.print(argStopSetIt.next());
    while (argStopSetIt.hasNext()) {
      System.err.print(' ');
      System.err.print(argStopSetIt.next());
    }
    System.err.println(")");
  }
}
