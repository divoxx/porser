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
    package danbikel.parser.util;

import danbikel.lisp.*;
import danbikel.parser.*;

import java.util.*;

/**
 * A class to convert {@link TrainerEvent} instances to {@link String} instances
 * of the form produced by
 * <a href="http://www.ai.mit.edu/people/mcollins/code.html">Mike
 * Collins&rsquo;</a> parser.
 *
 * @see Settings#outputCollins
 */
public class TrainerEventToCollins {
  public static Symbol topSym = Language.training().topSym();
  public static Symbol startSym = Language.training().startSym();
  public static Symbol stopSym = Language.training().stopSym();
  public static Symbol npArg = Symbol.add("NP-A");
  public static Symbol sbarArg = Symbol.add("SBAR-A");
  public static Symbol sArg = Symbol.add("S-A");
  public static Symbol vpArg = Symbol.add("VP-A");
  public static Symbol miscArg =
    Symbol.add(stopSym.toString() +
	       Language.treebank().canonicalAugDelimiter() +
	       Language.training().defaultArgAugmentation());


  public static String subcatToCollins(Subcat subcat, boolean withGap) {
    int nps = 0, sbars = 0, ss = 0, vps = 0, miscs = 0;
    Iterator it = subcat.iterator();
    while (it.hasNext()) {
      Symbol requirement = (Symbol) it.next();
      if (requirement == npArg) {
	nps++;
      }
      else if (requirement == sArg) {
	ss++;
      }
      else if (requirement == sbarArg) {
	sbars++;
      }
      else if (requirement == vpArg) {
	vps++;
      }
      else if (requirement == miscArg) {
	miscs++;
      }
    }
    StringBuffer sb = new StringBuffer(withGap ? 6 : 5);
    sb.append(nps).append(ss).append(sbars).append(vps).append(miscs);
    if (withGap) {
      sb.append("0");
    }
    return sb.toString();
  }

  public static String modEventToCollins(ModifierEvent modEvent) {

    boolean parentIsBaseNP = Language.treebank().isBaseNP(modEvent.parent());

    // conjunctions are treated specially when they are not dominated by
    // NPB and when they are part of a coordinated phrase (i.e., actually
    // conjoining two phrases, as opposed to starting a phrase, such as "But..."
    // hence, when such a "true" coordinating conjunction is the modifier
    // we return null; we also return null when the modifier is punctuation,
    // since punctuation is ALWAYS treated specially, even when parent is NPB

    // no need to test for parent not being base NP, since we now do that
    // in Trainer
    //boolean nonBaseNPConjPConj = !parentIsBaseNP && modEvent.isConjPConj();

    if (modEvent.isConjPConj() ||
	Language.treebank().isPunctuation(modEvent.modifier())) {
      return null;
    }

    StringBuffer sb = new StringBuffer(80);
    sb.append("2 ");
    Word modHeadWord = modEvent.modHeadWord();
    boolean modIsStop = modHeadWord.tag() == stopSym;
    if (modIsStop) {
      sb.append("#STOP# #STOP# ");
    }
    else {
      sb.append(modHeadWord.word()).append(" ");
      sb.append(modHeadWord.tag()).append(" ");
    }

    Symbol prevMod = modEvent.previousMods().symbolAt(0);
    boolean prevModIsStart = prevMod == startSym;

    Word headWord = modEvent.headWord();
    if (parentIsBaseNP && !prevModIsStart) {
      headWord = modEvent.previousWords().getWord(0);
    }

    sb.append(headWord.word()).append(" ");
    sb.append(headWord.tag()).append(" ");
    String modifier = modIsStop ? "#STOP#" : modEvent.modifier().toString();
    sb.append(modifier).append(" ");
    sb.append(modEvent.parent()).append(" ");

    Symbol head = modEvent.head();
    if (parentIsBaseNP && !prevModIsStart) {
      head = modEvent.previousMods().symbolAt(0);
    }

    sb.append(head).append(" ");
    sb.append(subcatToCollins(modEvent.subcat(), true)).append(" ");

    // append distance triple
    sb.append(modEvent.side() == Constants.LEFT ? "1" : "0");
    boolean adjacent = parentIsBaseNP || modEvent.headAdjacent();
    sb.append(adjacent ? "1" : "0");
    boolean verbIntervening =
      parentIsBaseNP ? false : modEvent.verbIntervening();
    sb.append(verbIntervening ? "1" : "0");

    sb.append(" ");

    // append coordination information
    // note that conjunctions are only treated specially when they are NOT
    // dominated by NPB
    Word prevConj = modEvent.prevConj();
    if (parentIsBaseNP || prevConj == null) {
      sb.append("0");
    }
    else {
      sb.append("1 ");
      sb.append(prevConj.word()).append(" ").append(prevConj.tag());
    }

    sb.append(" ");

    Word prevPunc = modEvent.prevPunc();
    if (prevPunc == null) {
      sb.append("0");
    }
    else {
      sb.append("1 ");
      sb.append(prevPunc.word()).append(" ").append(prevPunc.tag());
    }

    return sb.toString();
  }

  public static String headEventToCollins(HeadEvent headEvent) {
    StringBuffer sb = new StringBuffer(80);
    sb.append("3 ");
    Word headWord = headEvent.headWord();
    sb.append(headWord.word()).append(" ").append(headWord.tag()).append(" ");
    Symbol parent = headEvent.parent();
    sb.append(parent == topSym ? "TOP" :
      headEvent.parent().toString()).append(" ");
    sb.append(headEvent.head()).append(" ");
    sb.append(subcatToCollins(headEvent.leftSubcat(), false)).append(" ");
    sb.append(subcatToCollins(headEvent.rightSubcat(), false));
    return sb.toString();
  }

  public static String trainerEventToCollins(TrainerEvent event) {
    // top events are handled by manually adding TOP to each training sentence
    // so that we can correctly generate #STOP# events to either side of sole
    // "head child" of TOP (just like Mike's trainer does)
    if (event.parent() == topSym) {
      return null;
    }
    String collinsStr = null;
    if (event instanceof HeadEvent) {
      collinsStr = headEventToCollins((HeadEvent) event);
    }
    else if (event instanceof ModifierEvent) {
      collinsStr = modEventToCollins((ModifierEvent) event);
    }
    return collinsStr;
  }
}
