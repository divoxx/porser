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
 * for RMI clients such that the RMI server will be re-gotten from
 * the bootstrap registry in the event of method failure.  Subclasses
 * may modify the behavior in the event of method failure, by overriding
 * the <code>validate</code> method.
 *
 * @see #validate
 */
public class Reconnect
  implements InvocationHandler, Serializable {

  // constants
  private final static boolean debug = true;

  // data members

  // the actual stub on which to invoke remote methods
  protected Object stub;
  // registry binding name of the object to reconnect to
  protected String name;


  // constructor

  /**
   * Constructs a proxy invocation handler to reconnect with an RMI
   * server in the event of a method failure.  The reconnection is
   * performed by looking up the specified name in the bootstrap registry
   * (via <code>Naming.lookup</code>).  The default lookup behavior may
   * be changed in a subclass by overriding the {@link #validate} method.
   *
   * @param stub the remote object stub which should be validated
   * and which will be performing the actual method computation
   * @param name the name of the server in the bootstrap registry, to be
   * used as the argument to <code>Naming.lookup</code> when "re-connecting"
   * to the server
   */
  public Reconnect(Object stub, String name) {
    this.stub = stub;
    this.name = name;
  }

  public static synchronized Object proxyFor(Object stub, String name) {
    Class stubClass = stub.getClass();
    return Proxy.newProxyInstance(stubClass.getClassLoader(),
				  stubClass.getInterfaces(),
				  new Reconnect(stub, name));
  }

  public Object invoke(Object proxy,
		       Method method,
		       Object[] args) throws Throwable {
    validate();
    try {
      return method.invoke(stub, args);
    } catch (InvocationTargetException ite) {
      try {

	// try to throw embedded exception that method might have thrown...
	throw ite.getTargetException();

      } catch (RemoteException re) {
	// ...unless embedded exception was a RemoteException, which
	// means the remote method failed in some way
	stub = null;
	throw re;
      }
    }
  }

  /**
   * Ensures that the server stub on which remote methods will be executed
   * is valid, getting a server instance from the <code>rmiregistry</code>
   * if there was a previous method failure on the stub.  A subclass may
   * override this method if a different type of validation is desired.
   * For example, if for client-side failover, this method may execute
   * some other remote method to get a different server from a registry.
   */
  protected void validate() throws RemoteException {
    if (stub == null) {

      if (debug)
	System.err.println("trying to validate " + name);

      try {
	stub = Naming.lookup(name);
      }
      catch (Exception e) {
	throw new ServerException("could not find " + name + " in rmiregistry");
      }
    }
  }
}
