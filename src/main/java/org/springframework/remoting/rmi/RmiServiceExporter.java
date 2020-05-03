package org.springframework.remoting.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.support.RemoteExporter;

/**
 * RMI exporter that exposes the specified service as RMI object with the specified
 * name. Such services can be accessed via plain RMI or via RmiProxyFactoryBean.
 * Also supports exposing any non-RMI service via RMI invokers, to be accessed via
 * RmiProxyFactoryBean's automatic detection of such invokers.
 *
 * <p>With an RMI invoker, RMI communication works on the RemoteInvocationHandler
 * level, needing only one stub for any service. Service interfaces do not have to
 * extend java.rmi.Remote or throw RemoteException on all methods, but in and out
 * parameters have to be serializable.
 *
 * <p>The major advantage of RMI, compared to Hessian and Burlap, is serialization.
 * Effectively, any serializable Java object can be transported without hassle.
 * Hessian and Burlap have their own (de-)serialization mechanisms, but are
 * HTTP-based and thus much easier to setup than RMI. 
 *
 * @author Juergen Hoeller
 * @since 13.05.2003
 * @see RmiProxyFactoryBean
 * @see org.springframework.remoting.caucho.HessianServiceExporter
 * @see org.springframework.remoting.caucho.BurlapServiceExporter
 */
public class RmiServiceExporter extends RemoteExporter implements InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private String serviceName;

	private int servicePort = 0;  // anonymous port

	private int registryPort = Registry.REGISTRY_PORT;

	/**
	 * Set the name of the exported RMI service,
	 * i.e. rmi://localhost:port/NAME
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * Set the port that the exported RMI service will use.
	 * Default is 0 (anonymous port).
	 */
	public void setServicePort(int servicePort) {
		this.servicePort = servicePort;
	}

	/**
	 * Set the port of the registry for the exported RMI service,
	 * i.e. rmi://localhost:PORT/name
	 * Default is Registry.REGISTRY_PORT (1099).
	 */
	public void setRegistryPort(int registryPort) {
		this.registryPort = registryPort;
	}

	/**
	 * Register the service as RMI object.
	 * Creates an RMI registry on the specified port if none exists.
	 */
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();

		if (this.serviceName == null) {
			throw new IllegalArgumentException("serviceName is required");
		}

		Registry registry = null;
		logger.info("Looking for RMI registry at port '" + this.registryPort + "'");
		try {
			// retrieve registry
			registry = LocateRegistry.getRegistry(this.registryPort);
			registry.list();
		}
		catch (RemoteException ex) {
			logger.debug("RMI registry access threw exception", ex);
			logger.warn("Could not detect RMI registry - creating new one");
			// assume no registry found -> create new one
			registry = LocateRegistry.createRegistry(this.registryPort);
		}

		// bind wrapper to registry
		logger.info("Binding RMI service '" + this.serviceName + "' to registry at port '" + this.registryPort + "'");
		if (getService() instanceof Remote) {
			// conventional RMI service
			Remote exportedObject = UnicastRemoteObject.exportObject((Remote) getService(), this.servicePort);
			registry.rebind(this.serviceName, exportedObject);
		}
		else {
			// RMI invoker
			logger.info("RMI object '" + this.serviceName + "' is an RMI invoker");
			Remote wrapper = new RemoteInvocationWrapper(getProxyForService(), this.servicePort);
			registry.rebind(this.serviceName, wrapper);
		}
	}

}
