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
    package danbikel.util.proxy;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.rmi.Naming;


/**
 * An invocation handler for which proxy instances may be constructed
 * for RMI clients such that if a remote method fails, the method
 * will be re-invoked repeatedly until success.  The number of retries
 * of the remote method is specified at construction time.
 */
public class Retry
  implements InvocationHandler, Serializable {
  /** Indicates to re-try the remote object indefinitely.
      <p>The value of this constant is <tt>-1</tt>. */
  public final static int retryIndefinitely = -1;

  // constants
  private final static boolean debug = true;

  // data members

  // the actual stub on which to invoke remote methods
  private Object stub;
  // the retry count
  private int retries;
  // the sleep time between retries
  private long sleep;

  // constructors

  /**
   * Constructs a proxy invocation handler to retry an RMI server in
   * the event of a method failure.  Note that the total number of
   * tries will be one more than the value of this parameter.
   *
   * @param stub the remote object stub which should be validated
   * and which will be performing the actual method computation
   * @param retries the number of times the method will be re-tried
   * on the RMI server before giving up
   * to re-try indefinitely
   * @param sleep the number of milliseconds to sleep in between tries
   *
   * @throws IllegalArgumentException if <code>retries</code> is neither
   * non-negative nor {@link #retryIndefinitely}
   */
  public Retry(Object stub, int retries, long sleep) {
    if (!(retries >= 0 || retries == retryIndefinitely))
      throw new IllegalArgumentException();
    this.stub = stub;
    this.retries = retries;
    this.sleep = sleep;
  }


  public static synchronized Object proxyFor(Object stub,
					     int retries, long sleep) {
    Class stubClass = stub.getClass();
    return Proxy.newProxyInstance(stubClass.getClassLoader(),
				  stubClass.getInterfaces(),
				  new Retry(stub, retries, sleep));
  }

  /**
   * Returns whether to keep trying a remote method in the face of failure.
   * This method is guaranteed to be called before each attempt at a
   * method invocation.  By overriding this method, subclasses may
   * perform arbitrary tests to determine whether to stop re-trying
   * the RMI server.  The default implementation simply returns
   * <code>true</code>.
   *
   * @return <code>true</code> under all circumstances
   */
  protected boolean keepTrying() {
    return true;
  }

  public Object invoke(Object proxy,
		       Method method,
		       Object[] args) throws Throwable {
    RemoteException re = null;
    int numTries = retries + 1;
    boolean infiniteRetries = retries == retryIndefinitely;
    for (int i = 0; infiniteRetries || i < numTries; i++) {
      if (!keepTrying())
	break;

      if (debug)
	if (i > 0)
	  System.err.println(getClass().getName() + ": retrying call of " +
			     method + " on " + stub.getClass() +
			     "\n\t(retry No. " + i + ")");

      try {
	Object retVal = method.invoke(stub, args);
	if (debug)
	  if (i > 0)
	    System.err.println(getClass().getName() +
			       ": successfully called method\n\t" + method);
	return retVal;
      } catch (InvocationTargetException ite) {
	try {

	// try to throw embedded exception that method might have thrown...
	throw ite.getTargetException();

	// ...unless embedded exception was either of the two
	// exceptions below, which means the remote method failed in
	// some way
	} catch (java.rmi.ConnectException ce) {
	  re = ce;

	  if (debug)
	    System.err.println(getClass().getName() + ": received exception " +
			       "(" + re + ")");

	  Thread.sleep(sleep);

	} catch (java.rmi.ConnectIOException cioe) {
	  re = cioe;

	  if (debug)
	    System.err.println(re);

	  Thread.sleep(sleep);
	}
      }
    } // end for

    // we tried and failed, so throw the last exception that was thrown
    throw re;
  }
}
