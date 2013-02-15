package edu.rit.cs4005730.dsgroup3;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import edu.rit.ds.Lease;

public interface NodeManagerRef extends Remote {
   public void registerNode(final NodeRef node) throws RemoteException;
   
   public void deregisterNode(final NodeRef node) throws RemoteException;
   
   public List<NodeRef> getSeeds() throws RemoteException;
   
   public List<NodeRef> getNodes() throws RemoteException;
   
   public Lease addNodeManagerListener(final NodeManagerListener listener) throws RemoteException;
   
   public void addSeed(NodeRef seed) throws RemoteException;
   
   public double getCurrentBandwidthUtilization() throws RemoteException;
   
   public GeneralStatistics getStats() throws RemoteException;
}
