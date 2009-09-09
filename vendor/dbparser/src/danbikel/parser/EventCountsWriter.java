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

/**
 * Provides a method to write {@link CountsTable} objects containing counts of
 * {@link TrainerEvent} objects to a file or an output stream.  As an
 * implementation of {@link ObjectWriter}, instances of this class may be used
 * by the {@link Switchboard} to write such counts tables to an output file;
 * such counts table objects are the output of the {@link EMParser} class. The
 * switchboard will use instances of this class when an {@link
 * EventCountsWriterFactory} is passed as an argument to one of the {@link
 * Switchboard} constructors.
 * <p/>
 * <b>Implementation note</b>: While historically the output of one or more EM
 * parsing clients ({@link EMParser} instances) was fed to instances of this
 * class, the current approach is to use the more flexible {@link Consumer}
 * mechanism of the switchboard.  Both {@link EMParser} (in stand-alone mode)
 * and {@link StartEMSwitchboard} create an instance of a {@link
 * EventCountsConsumer} in order to take the {@link CountsTable} objects
 * produced by the {@link EMParser} and output them to an output file. However,
 * this class is neither obsolete nor is its use deprecated.  In fact, the
 * {@link EventCountsConsumer} class makes use of the static method {@link
 * #outputEvents(CountsTable,Writer)} of this class.
 * <p/>
 *
 * @see EventCountsConsumer
 * @see Switchboard#registerConsumer(Consumer)
 */
public class EventCountsWriter extends PrintWriter implements ObjectWriter {

  /**
   * Constructs a new {@link EventCountsWriter} using the specified output
   * stream to output {@link CountsTable} instances containing counts of {@link
   * TrainerEvent} objects.
   *
   * @param os the output stream to use for outputting {@link CountsTable}s of
   *           {@link TrainerEvent}s
   */
  public EventCountsWriter(OutputStream os) {
    super(new OutputStreamWriter(os));
  }

  /**
   * Constructs a new {@link EventCountsWriter} using the specified output
   * stream, encoding and buffer size to output {@link CountsTable} instances
   * containing counts of {@link TrainerEvent} objects.
   *
   * @param os       the output stream to use for outputting {@link
   *                 CountsTable}s of {@link TrainerEvent}s
   * @param encoding the encoding to use when constructing a {@link Writer}
   *                 around the specified output stream
   * @param bufSize  the buffer size to use when constructing a {@link
   *                 BufferedWriter} around the writer constructed around the
   *                 specified output stream
   * @throws IOException if there is an {@link IOException} thrown when creating
   *                     any of the {@link Writer} instances around the
   *                     specified output stream
   */
  public EventCountsWriter(OutputStream os, String encoding, int bufSize)
    throws IOException {
    super(getNewBufferedWriter(os, encoding, bufSize));
  }

  private static Writer getNewBufferedWriter(OutputStream os, String encoding,
					     int bufSize) throws IOException {
    return new BufferedWriter(new OutputStreamWriter(os, encoding),
			      bufSize);
  }

  /**
   * Constructs a new {@link EventCountsWriter} by creating a {@link Writer} for
   * the specified filename, using the specified encoding, buffer size and
   * append options.
   *
   * @param filename the filename for which to construct a {@link Writer}
   * @param encoding the encoding for the {@link Writer} that is to be
   *                 constructed for the specified file
   * @param bufSize  the buffer size for the {@link BufferedWriter} that is to
   *                 be constructed around the {@link Writer} for the specified
   *                 file
   * @param append   indicates whether to append to the specified file or to
   *                 clobber any existing file and write anew
   * @throws IOException if any of the constructors for the various {@link
   *                     Writer} objects throw an {@link IOException}
   */
  public EventCountsWriter(String filename, String encoding, int bufSize,
			  boolean append) throws IOException {
    super(getNewBufferedWriter(filename, encoding, bufSize, append));
  }

  private static Writer getNewBufferedWriter(String filename, String encoding,
					     int bufSize, boolean append)
    throws IOException {
    OutputStream fos = new FileOutputStream(filename, append);
    OutputStreamWriter osw = new OutputStreamWriter(fos, encoding);
    return new BufferedWriter(osw, bufSize);
  }

  /**
   * Writes the specified object to the file or output stream associated with
   * this {@link EventCountsWriter}.
   *
   * @param obj a {@link CountsTable} instance whose keys are {@link
   *            TrainerEvent} instances
   * @throws IOException if the underlying output writer or stream throws an
   *                     {@link IOException} while writing out the specified
   *                     object
   */
  public void writeObject(Object obj) throws IOException {
    outputEvents((CountsTable)obj, this);
    flush();
  }

  /**
   * A helper method used both by {@link #writeObject(Object)} and by the {@link
   * EventCountsConsumer} class to write a {@link CountsTable} with {@link
   * TrainerEvent} keys as text to a {@link Writer}.  The text file written is
   * both human- and machine-readable: it is crucially in the format written and
   * read by the {@link Trainer} class.
   *
   * @param events the {@link TrainerEvent} counts to be written to the
   *               specified writer
   * @param out    the writer to which to write the specified {@link
   *               TrainerEvent} counts table
   * @throws IOException if the specified output writer throws an {@link
   *                     IOException} while writing out the specified {@link
   *                     CountsTable}
   *
   * @see Trainer#writeStats(java.io.File)
   * @see Trainer#readStats(java.io.File)
   */
  public static void outputEvents(CountsTable events, Writer out)
    throws IOException {
    Iterator it = events.entrySet().iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
      TrainerEvent event = (TrainerEvent)entry.getKey();
      double count = entry.getDoubleValue();
      String name = null;
      if (event instanceof HeadEvent)
        name = Trainer.headEventSym.toString();
      else if (event instanceof ModifierEvent)
        name = Trainer.modEventSym.toString();
      if (name != null) {
        out.write("(");
        out.write(name);
        out.write(" ");
        out.write(event.toString());
        out.write(" ");
        out.write(String.valueOf(count));
        out.write(")\n");
      }
    }
  }
}