package edu.rit.cs4005730.dsgroup3;

import java.rmi.Remote;
import java.rmi.RemoteException;

import edu.rit.ds.Lease;

/**
 * This class implements the {@link Remote} interface for use in an RMI system designed to simulate a BitTorrent network and the effects of increasing the
 * number of seeds/peers on average transfer time.
 * 
 * @author Roshan Balaji
 * @author Stephen Ranger
 * 
 */
public interface NodeRef extends Remote {
   
   /**
    * Returns the name of this node.
    * 
    * @return the name of this node
    * @throws RemoteException
    */
   public String getName() throws RemoteException;
   
   /**
    * Returns the size of each block of data in the downloadable file in bytes or -1 if this {@link Node} has no information.
    * 
    * @return the size in bytes of each block or -1 if the value is unknown
    * @throws RemoteException
    */
   public long getBlockSize(int _index) throws RemoteException;
   
   /**
    * Returns the number of blocks the downloadable file is split into or -1 if this {@link Node} has no information.
    * 
    * @return the size in bytes of the downloadable file or -1 if the value is unknown
    * @throws RemoteException
    */
   public long getBlockCount() throws RemoteException;
   
   /**
    * Returns the maximum download rate for this {@link Node} in bytes per second.
    * 
    * @return the maximum download rate in bytes per second
    * @throws RemoteException
    */
   public double getMaximumDownloadRate() throws RemoteException;
   
   /**
    * Returns the maximum upload rate for this {@link Node} in bytes per second.
    * 
    * @return the maximum upload rate in bytes per second
    * @throws RemoteException
    */
   public double getMaximumUploadRate() throws RemoteException;
   
   /**
    * Returns the current download rate for this {@link Node} in bytes per second.
    * 
    * @return the current download rate in bytes per second
    * @throws RemoteException
    */
   public double getCurrentDownloadRate() throws RemoteException;
   
   /**
    * Returns the current upload rate for this {@link Node} in bytes per second.
    * 
    * @return the current upload rate in bytes per second
    * @throws RemoteException
    */
   public double getCurrentUploadRate() throws RemoteException;
   
   /**
    * Returns the status for each block of data as a percentage of completion between 0 and 1.
    * 
    * @return an array containing the percentage of completion for each block of data
    * @throws RemoteException
    */
   public Double[] getStatus() throws RemoteException;
   
   /**
    * Returns true if this {@link Node}Node has completed downloading the entire file or false if it has not.
    * 
    * @return true if this {@link Node} is a seed; false otherwise
    * @throws RemoteException
    */
   public boolean isSeed() throws RemoteException;
   
   /**
    * {@link Node} request specific block from another {@link Node} which will give the index of the block it is requesting and the total available bandwidth it
    * has. The {@link Node} receiving the request will then either return -1 if it does not have the block and 0 if it has no more available upload bandwidth.
    * Otherwise, it will return the amount of bandwidth it has reserved for the transfer. <br/>
    * <br/>
    * Block transfer protocol:
    * 
    * <pre>
    * Client 1 needs block 1 and has 2mbps download available
    * Client 1 asks Client 2 for block 1 with 2mbps max speed
    * Client 2 has 0.5mbps upload available, tells Client 1 it will use 0.5mbps
    * Client 2 subtracts 0.5mbps from its upload cap
    * Client 1 subtracts 0.5mbps from its download cap
    * Client 1 waits enough time for block 1 to transfer (no actual transfer)
    * Client 1 notifies Client 2 when the transfer is finished and it's bandwidth is free
    * Client 1 adds 0.5mbps to download cap
    * Client 2 adds 0.5mbps to upload cap
    * </pre>
    * 
    * @param node
    *           the requesting node
    * @param index
    *           the index of the requested block
    * @param maxDownloadAvailable
    *           the total amount of download bandwidth the requesting node has available in bytes per second
    * @return amount of upload bandwidth reserved, -1 if block is not available from this node, 0 if no available upload bandwidth
    * @throws RemoteException
    */
   public double requestBlock(final String nodeName, final int index, final double maxDownloadAvailable) throws RemoteException;
   
   /**
    * Notifies this {@link Node} that a previously requested block transfer has been completed.
    * 
    * @param node
    *           the requesting {@link Node}
    * @param index
    *           the index of the block to transfer
    * @throws RemoteException
    */
   public void finalizeBlock(final String nodeName, final int index) throws RemoteException;
   
   /**
    * Returns the total elapsed time in milliseconds that this {@link Node} has been, or had been, downloading the file.
    * 
    * @return the total transfer time in milliseconds
    * @throws RemoteException
    */
   public long getTransferTime() throws RemoteException;
   
   /**
    * Returns a total progress percentage for this Node.
    * 
    * @return the download progress percentage
    * @throws RemoteException
    */
   public double getProgress() throws RemoteException;
   
   /**
    * Adds a {@link NodeListener} to this {@link Node} that will listen for transfer progress events.
    * 
    * @param listener
    *           the listener to add
    * @throws RemoteException
    */
   public Lease addNodeListener(final NodeListener listener) throws RemoteException;
   
   /**
    * Returns the completion percentage as a value between 0 and 1 or -1 if the download of the specified block has not in progress.
    * 
    * @param index
    *           the block index
    * @return the completion status
    * @throws RemoteException
    */
   public double getBlockCompletion(int index) throws RemoteException;
   
   /**
    * Given the currently elapsed time and the current completion progress, returns the estimated time to completion in milliseconds.
    * 
    * @return estimated time to completion in seconds
    * @throws RemoteException
    */
   public long estimatedTime() throws RemoteException;
   
   /**
    * Returns the available bandwidth for download tasks.
    * 
    * @return the available download bandwidth in bytes per second
    * @throws RemoteException
    */
   public double getAvailableDownloadRate() throws RemoteException;
   
   /**
    * Returns the available bandwidth for upload tasks.
    * 
    * @return the available upload bandwidth in bytes per second
    * @throws RemoteException
    */
   public double getAvailableUploadRate() throws RemoteException;
   
   /**
    * Cancels all outstanding tasks, quits download, and deregisters itself with the NodeManager.
    * 
    * @throws RemoteException
    */
   public void dispose() throws RemoteException;
   
   /**
    * Returns the number of blocks currently being uploaded.
    * 
    * @return the number of uploading blocks
    * @throws RemoteException
    */
   public int getUploadCount() throws RemoteException;
   
   /**
    * Returns the number of blocks currently being downloaded.
    * 
    * @return the number of downloading blocks
    * @throws RemoteException
    */
   public int getDownloadCount() throws RemoteException;
}
