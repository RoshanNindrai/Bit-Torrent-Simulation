package edu.rit.cs4005730.dsgroup3;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import edu.rit.cs4005730.dsgroup3.NodeManagerEvent.EventType;
import edu.rit.ds.Lease;
import edu.rit.ds.RemoteEventGenerator;
import edu.rit.ds.registry.AlreadyBoundException;
import edu.rit.ds.registry.RegistryProxy;

/**
 * Allows for the registration of remote {@link NodeRef} objects as well as the lookup of all other {@link NodeRef} objects in the system.
 * 
 * @author Roshan Balaji
 * @author Stephen Ranger
 * @author Nate Smith
 * 
 */

public class NodeManager implements NodeManagerRef {
   // final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
   private final Map<String, NodeRef>                   seeds          = new ConcurrentHashMap<String, NodeRef>();
   private final List<NodeRef>                          peers          = new CopyOnWriteArrayList<NodeRef>();
   private final GeneralStatistics                      stats          = new GeneralStatistics();
   private final RegistryProxy                          myProxy;
   private final RemoteEventGenerator<NodeManagerEvent> eventGenerator = new RemoteEventGenerator<NodeManagerEvent>();
   
   public NodeManager(final String args[]) throws RemoteException, AlreadyBoundException {
      super();
      // Verify command line arguments.
      if (args.length != 3) {
         throw new IllegalArgumentException("NodeManager: <host> <port> <name>");
      }
      final String host = args[0];
      final String name = args[2];
      int port;
      try {
         port = Integer.parseInt(args[1]);
      } catch (final NumberFormatException exc) {
         throw new IllegalArgumentException("NodeManager: Invalid port: \"" + args[1] + "\"");
      }
      
      // Get a proxy for the Registry Server.
      try {
         this.myProxy = new RegistryProxy(host, port);
      } catch (final RemoteException e) {
         throw new RemoteException("NodeManager: Invalid Registry Server Not found");
      }
      // Export.
      
      UnicastRemoteObject.exportObject(this, 0);
      
      // Rebind into the Registry Server.
      
      try {
         
         this.myProxy.bind(name, this);
         
      } catch (final RemoteException exc) {
         try {
            UnicastRemoteObject.unexportObject(this, true);
            throw new RemoteException("NodeManager: Invalid Registry Server Not found");
         } catch (final NoSuchObjectException exc2) {
         }
         throw exc;
      } catch (final AlreadyBoundException exc) {
         try {
            UnicastRemoteObject.unexportObject(this, true);
            throw new AlreadyBoundException("NodeManager: The object is already Bounded");
         } catch (final NoSuchObjectException exc2) {
         }
         throw new IllegalArgumentException("NodeManager(): <NodeManagername> = \"" + name + "\" already exists");
      }
   }
   
   /**
    * Adds a new {@link NodeRef} to the registration system. If the Node already exists, the Node will not be added again.
    * 
    * @param NodeRef
    *           the {@link NodeRef} to add
    * @throws RemoteException
    */
   @Override
   public void registerNode(final NodeRef _node) throws RemoteException {
      try {
         // this.lock.writeLock().lock();
         if (!this.peers.contains(_node)) {
            if (_node.isSeed()) {
               this.seeds.put(_node.getName(), _node);
            }
            
            this.peers.add(_node);
         }
      } finally {
         // this.lock.writeLock().unlock();
      }
      
      // update whenever new nodes are added (this can trigger resets if necessary)
      this.stats.updateStats(this.peers, this.seeds);
      this.notifyListeners(EventType.NODE_ADDED, _node);
   }
   
   /**
    * Removes a {@link Node} from the registration system. If the {@link Node} is not found, the call will be ignored.
    * 
    * @param node
    *           the {@link Node} to remove
    * @throws RemoteException
    */
   @Override
   public void deregisterNode(final NodeRef _node) throws RemoteException {
      try {
         // this.lock.writeLock().lock();
         if (_node.isSeed()) {
            this.seeds.remove(_node.getName());
         }
         
         this.peers.remove(_node);
      } finally {
         // this.lock.writeLock().unlock();
      }
      
      this.notifyListeners(EventType.NODE_REMOVED, _node);
   }
   
   /**
    * Returns a list of all Seed {@link NodeRef} objects.
    * 
    * @return a list of all Seed nodes
    * @throws RemoteException
    */
   @Override
   public List<NodeRef> getSeeds() throws RemoteException {
      try {
         // this.lock.readLock().lock();
         final List<NodeRef> copyOf = new ArrayList<NodeRef>();
         copyOf.addAll(this.seeds.values());
         
         return copyOf;
      } finally {
         // this.lock.readLock().unlock();
      }
   }
   
   /**
    * Returns a list of all {@link NodeRef} objects currently registered.
    * 
    * @return a list of all registered nodes
    * @throws RemoteException
    */
   @Override
   public List<NodeRef> getNodes() throws RemoteException {
      try {
         // this.lock.readLock().lock();
         final List<NodeRef> copyOf = new ArrayList<NodeRef>();
         copyOf.addAll(this.peers);
         
         return copyOf;
      } finally {
         // this.lock.readLock().unlock();
      }
   }
   
   /**
    * Adds a {@link NodeManagerListener} to this {@link NodeManager} to listen for nodeAdded and nodeRemoved events. If this listener has already been added,
    * this call will be ignored.
    * 
    * @param listener
    *           the listener to add
    * @throws RemoteException
    */
   @Override
   public Lease addNodeManagerListener(final NodeManagerListener listener) throws RemoteException {
      return this.eventGenerator.addListener(listener);
   }
   
   private void notifyListeners(final EventType type, final NodeRef node) throws RemoteException {
      final String name = node.getName();
      this.eventGenerator.reportEvent(new NodeManagerEvent(type, node, name));
   }
   
   @Override
   public void addSeed(final NodeRef _seed) throws RemoteException {
      try {
         // this.lock.writeLock().lock();
         if (!this.seeds.containsKey(_seed.getName())) {
            this.seeds.put(_seed.getName(), _seed);
         }
      } finally {
         // this.lock.writeLock().unlock();
      }
      this.stats.updateStats(this.peers, this.seeds);
   }
   
   @Override
   public double getCurrentBandwidthUtilization() throws RemoteException {
      try {
         // this.lock.readLock().lock();
         double bandwidth = 0;
         
         for (final NodeRef node : this.peers) {
            bandwidth += node.getCurrentDownloadRate();
         }
         
         return bandwidth;
      } finally {
         // this.lock.readLock().unlock();
      }
   }
   
   @Override
   public GeneralStatistics getStats() throws RemoteException {
      return this.stats;
   }
}
