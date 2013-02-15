package edu.rit.cs4005730.dsgroup3;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import edu.rit.cs4005730.dsgroup3.NodeManagerEvent.EventType;
import edu.rit.cs4005730.dsgroup3.util.BlockingThreadPool;
import edu.rit.cs4005730.dsgroup3.util.Pair;
import edu.rit.cs4005730.dsgroup3.util.RemoteUtils;
import edu.rit.ds.Lease;
import edu.rit.ds.RemoteEventGenerator;
import edu.rit.ds.registry.NotBoundException;
import edu.rit.ds.registry.RegistryProxy;
import edu.rit.numeric.prob.UniformIntPrng;

public class Node implements NodeRef {
   // private final ReentrantLock lock = new ReentrantLock();
   
   private final String                             name;
   private final double                             upload;
   private final double                             download;
   private final int                                blockCount;
   private final Map<Integer, Double>               blockCompletion  = new ConcurrentHashMap<Integer, Double>();
   private final long                               blockSize;
   private final UniformIntPrng                     random;
   private final BlockingThreadPool                 jobs             = new BlockingThreadPool(8);
   
   private NodeManagerRef                           manager;
   private boolean                                  isSeed;
   private boolean                                  isDisposed       = false;
   private long                                     startTime;
   private long                                     endTime;
   private volatile Double                          downloadAvailable;
   private volatile Double                          uploadAvailable;
   
   private final Map<Pair<String, Integer>, Double> currentDownloads = new ConcurrentHashMap<Pair<String, Integer>, Double>();
   private final Map<Pair<String, Integer>, Double> currentUploads   = new ConcurrentHashMap<Pair<String, Integer>, Double>();
   
   private final Map<String, NodeRef>               peers            = new ConcurrentHashMap<String, NodeRef>();
   private final RemoteEventGenerator<NodeEvent>    eventGenerator   = new RemoteEventGenerator<NodeEvent>();
   
   /**
    * Creates a new Node object with the given name and the given settings
    * 
    * @param name
    *           the name for this node (does not have to be unique)
    * @param upload
    *           the upload rate in bytes per second
    * @param download
    *           the download rate in bytes per second
    * @throws RemoteException
    * @throws NotBoundException
    * @throws InterruptedException
    */
   public Node(final String name, final double upload, final double download, final long filesize, final long blockSize, final int blockCount, final boolean isSeed, final String host, final int port,
         final String nodeManagerName) throws RemoteException, NotBoundException, InterruptedException {
      this.name = name;
      this.upload = upload;
      this.download = download;
      this.blockCount = blockCount;
      this.blockSize = blockSize;
      this.isSeed = isSeed;
      this.random = new UniformIntPrng(new Random(), 0, blockCount - 1);
      
      this.downloadAvailable = download;
      this.uploadAvailable = upload;
      
      RegistryProxy myProxy = null;
      try {
         myProxy = new RegistryProxy(host, port);
         UnicastRemoteObject.exportObject(this, 0);
         
      } catch (final RemoteException e) {
         throw new RemoteException("Node: export object problem");
      }
      
      if (isSeed) {
         for (int i = 0; i < blockCount; i++) {
            this.blockCompletion.put(i, 1.0);
         }
      }
      
      try {
         this.manager = (NodeManagerRef) myProxy.lookup(nodeManagerName);
         this.manager.registerNode(this);
         
         final NodeManagerListener nodeManagerListener = new NodeManagerListener() {
            @Override
            public void report(final long sequenceNumber, final NodeManagerEvent event) throws RemoteException {
               if (event.type == EventType.NODE_ADDED) {
                  Node.this.peers.put(event.name, event.node);
               } else if (event.type == EventType.NODE_REMOVED) {
                  Node.this.peers.remove(event.name);
               }
            }
         };
         
         UnicastRemoteObject.exportObject(nodeManagerListener, 0);
         this.manager.addNodeManagerListener(nodeManagerListener);
      } catch (final RemoteException e) {
         e.printStackTrace();
      }
      
      final List<NodeRef> nodes = this.manager.getNodes();
      
      for (final NodeRef node : nodes) {
         try {
            this.peers.put(node.getName(), node);
         } catch (final RemoteException e) {
            // could not access node; ignore
         }
      }
   }
   
   @Override
   public void dispose() throws RemoteException {
      this.isDisposed = true;
      
      RemoteUtils.sleep(100);
      
      try {
         this.jobs.shutdown();
      } catch (final Exception e) {
         // dont care
      }
      
      try {
         this.manager.deregisterNode(this);
      } catch (final Exception e) {
         // dont care
      }
      
   }
   
   private int selectRandomBlock() {
      try {
         // this.lock.lock();
         int index = -1;
         int ctr = 0;
         
         while ((index == -1) && (ctr < this.blockCount)) {
            index = this.random.next();
            
            if (this.blockCompletion.containsKey(index)) {
               index = -1;
            }
            
            ctr++;
         }
         
         // took too long to select random index; pick first available
         if (index == -1) {
            for (int i = 0; i < this.blockCount; i++) {
               if (!this.blockCompletion.containsKey(i)) {
                  index = i;
                  break;
               }
            }
         }
         
         return index;
      } finally {
         // this.lock.unlock();
      }
   }
   
   @Override
   public double requestBlock(final String nodeName, final int index, final double maxDownloadAvailable) throws RemoteException {
      try {
         // this.lock.lock();
         
         if ((nodeName != null) && (this.getBlockCompletion(index) == 1.0)) {
            synchronized (this.currentUploads) {
               final double uploadReserved = Math.min(maxDownloadAvailable, Math.min(this.upload / 4.0, this.uploadAvailable));
               
               if ((uploadReserved > 0) && (uploadReserved > (this.upload / 6.0))) {
                  this.decrementUploadAvailable(nodeName, index, uploadReserved);
                  return uploadReserved;
               } else {
                  return 0;
               }
            }
         } else {
            return -1;
         }
      } finally {
         // this.lock.unlock();
      }
   }
   
   @Override
   public void finalizeBlock(final String nodeName, final int index) throws RemoteException {
      try {
         // this.lock.lock();
         
         if (nodeName != null) {
            final Double value = this.currentUploads.get(Pair.getInstance(nodeName, index));
            
            if (value != null) {
               this.incrementUploadAvailable(nodeName, index);
            }
         }
      } finally {
         // this.lock.unlock();
      }
   }
   
   public void startProcess() throws RemoteException, InterruptedException {
      this.startTime = System.nanoTime();
      
      if (this.isSeed) {
         this.endTime = this.startTime;
      } else {
         while (!this.isSeed && !this.isDisposed) {
            this.getBlock();
         }
      }
   }
   
   private void getBlock() {
      final Runnable runnable = new Runnable() {
         @Override
         public void run() {
            NodeRef node = null;
            int index = -1;
            
            try {
               final Index indexReturnValue = new Index();
               node = Node.this.getSeed(indexReturnValue);
               index = indexReturnValue.getIndex();
               
               if (node != null) {
                  final Pair<String, Integer> pair = Pair.getInstance(node.getName(), index);
                  final Double uploadReserved = Node.this.currentDownloads.get(pair);
                  
                  if (uploadReserved != null) {
                     // size of block in bytes divided by transfer speed in bytes per second is the number of
                     // seconds the transfer will take
                     // multiply by the number of milliseconds in a second to get wait time
                     // divide by 100 to get duration per percent complete (tenths of a second)
                     // bytes / bytes per second * ms per second = bytes per ms
                     final double duration = (Node.this.blockSize / uploadReserved) * RemoteUtils.SECONDS_IN_MILLISECONDS;
                     final double step = RemoteUtils.getStep(duration * RemoteUtils.TIME_DILATION_RATIO);
                     final double inc = RemoteUtils.getIncrements(duration * RemoteUtils.TIME_DILATION_RATIO);
                     double value = 0;
                     
                     Node.this.setBlockCompletion(index, 0.0);
                     
                     for (int i = 0; i < inc; i++) {
                        RemoteUtils.sleep((long) step);
                        value += 1.0 / inc;
                        Node.this.setBlockCompletion(index, value);
                     }
                     
                     Node.this.setBlockCompletion(index, 1.0);
                     Node.this.incrementDownloadAvailable(pair.left, pair.right);
                     Node.this.notifyListeners();
                  }
               }
            } catch (final Exception e) {
               e.printStackTrace();
            }
            
            if (node != null) {
               try {
                  node.finalizeBlock(Node.this.name, index);
                  Node.this.incrementDownloadAvailable(node.getName(), index);
               } catch (final RemoteException e) {
                  e.printStackTrace();
               }
            }
            
            try {
               if (Node.this.getBlockCompletion(index) != 1.0) {
                  Node.this.setBlockCompletion(index, null);
               }
            } catch (final RemoteException e) {
               e.printStackTrace();
            }
            
            Node.this.checkSeedStatus();
         }
      };
      
      // will block if jobs service is full
      this.jobs.execute(runnable);
   }
   
   private void setBlockCompletion(final Integer index, final Double value) {
      try {
         // this.lock.lock();
         if ((value == null) || (value == -1)) {
            this.blockCompletion.remove(index);
         } else {
            this.blockCompletion.put(index, value);
         }
      } finally {
         // this.lock.unlock();
      }
   }
   
   private NodeRef getSeed(final Index index) {
      try {
         // this.lock.lock();
         final List<NodeRef> peers = new ArrayList<NodeRef>();
         final UniformIntPrng peerRandoms = new UniformIntPrng(new Random(), 0, this.peers.size() - 1);
         double uploadReserved = 0;
         int ctr = 0;
         NodeRef peer = null;
         
         peers.addAll(this.peers.values());
         
         while ((uploadReserved <= 0) && (ctr < 100)) {
            index.setIndex(this.selectRandomBlock());
            peer = peers.get(peerRandoms.next());
            
            try {
               if (!peer.getName().equals(this.name)) {
                  synchronized (this.currentDownloads) {
                     uploadReserved = peer.requestBlock(this.name, index.getIndex(), this.downloadAvailable);
                     ctr++;
                     
                     if (uploadReserved > 0) {
                        this.decrementDownloadAvailable(peer.getName(), index.getIndex(), uploadReserved);
                        return peer;
                     }
                  }
               }
            } catch (final RemoteException e) {
               // nothing; node is gone, just skip
               // e.printStackTrace();
               this.peers.remove(peer);
            }
         }
      } finally {
         // this.lock.unlock();
      }
      
      index.setIndex(-1);
      return null;
   }
   
   private void checkSeedStatus() {
      double progress = 0;
      
      try {
         progress = this.getProgress();
      } catch (final RemoteException e) {
         // shouldn't happen; called locally
         e.printStackTrace();
      }
      
      if (progress == 1.0) {
         this.isSeed = true;
         this.endTime = System.nanoTime();
         try {
            this.manager.addSeed(this);
         } catch (final RemoteException e) {
            e.printStackTrace();
         }
      }
   }
   
   private void decrementDownloadAvailable(final String name, final int index, final double download) {
      this.downloadAvailable -= download;
      this.setBlockCompletion(index, -0.5);
      this.currentDownloads.put(Pair.getInstance(name, index), download);
   }
   
   private void incrementDownloadAvailable(final String name, final int index) {
      final Double download = this.currentDownloads.remove(Pair.getInstance(name, index));
      
      if (download != null) {
         this.downloadAvailable += download;
      }
   }
   
   private void decrementUploadAvailable(final String name, final int index, final double upload) {
      this.uploadAvailable -= upload;
      this.currentUploads.put(Pair.getInstance(name, index), upload);
   }
   
   private void incrementUploadAvailable(final String name, final int index) {
      final Double upload = this.currentUploads.remove(Pair.getInstance(name, index));
      
      if (upload != null) {
         this.uploadAvailable += upload;
      }
   }
   
   @Override
   public String getName() throws RemoteException {
      return this.name;
   }
   
   @Override
   public long getBlockSize(final int _index) throws RemoteException {
      return this.blockSize;
   }
   
   @Override
   public long getBlockCount() throws RemoteException {
      return this.blockCount;
   }
   
   @Override
   public double getMaximumDownloadRate() throws RemoteException {
      return this.download;
   }
   
   @Override
   public double getMaximumUploadRate() throws RemoteException {
      return this.upload;
   }
   
   @Override
   public double getCurrentDownloadRate() throws RemoteException {
      return this.download - this.downloadAvailable;
   }
   
   @Override
   public double getCurrentUploadRate() throws RemoteException {
      return this.upload - this.uploadAvailable;
   }
   
   @Override
   public double getAvailableDownloadRate() throws RemoteException {
      return this.downloadAvailable;
   }
   
   @Override
   public double getAvailableUploadRate() throws RemoteException {
      return this.uploadAvailable;
   }
   
   @Override
   public Double[] getStatus() throws RemoteException {
      try {
         // this.lock.lock();
         return this.blockCompletion.values().toArray(new Double[this.blockCompletion.size()]);
      } finally {
         // this.lock.unlock();
      }
   }
   
   @Override
   public boolean isSeed() throws RemoteException {
      return this.isSeed;
   }
   
   @Override
   public long getTransferTime() throws RemoteException {
      final long currentTime = ((this.isSeed) ? this.endTime : System.nanoTime());
      final long duration = (this.startTime > 0) ? (currentTime - this.startTime) : 0; // ensures that 0 is returned before the startTime is set
      return (long) (duration / RemoteUtils.MILLISECOND_IN_NANOSECONDS / RemoteUtils.TIME_DILATION_RATIO);
   }
   
   @Override
   public double getProgress() throws RemoteException {
      try {
         // this.lock.lock();
         double total = 0;
         
         for (final Double value : this.blockCompletion.values()) {
            total += (value < 0) ? 0 : value;
         }
         
         return total / this.blockCount;
      } finally {
         // this.lock.unlock();
      }
   }
   
   @Override
   public long estimatedTime() throws RemoteException {
      final long duration = (((this.isSeed) ? this.endTime : System.nanoTime()) - this.startTime) / RemoteUtils.MILLISECOND_IN_NANOSECONDS;
      return (long) (((100.0 * duration) / RemoteUtils.TIME_DILATION_RATIO) / this.getProgress());
   }
   
   @Override
   public Lease addNodeListener(final NodeListener listener) throws RemoteException {
      return this.eventGenerator.addListener(listener);
   }
   
   private void notifyListeners() {
      double progress;
      try {
         progress = this.getProgress();
         this.eventGenerator.reportEvent(new NodeEvent(this, this.name, progress));
      } catch (final RemoteException e) {
         // don't care; it's local
      }
   }
   
   @Override
   public double getBlockCompletion(final int index) throws RemoteException {
      try {
         // this.lock.lock();
         final Double value = this.blockCompletion.get(index);
         
         return (value == null) ? -1 : value;
      } finally {
         // this.lock.unlock();
      }
   }
   
   @Override
   public int getUploadCount() throws RemoteException {
      return this.currentUploads.size();
   }
   
   @Override
   public int getDownloadCount() throws RemoteException {
      return this.currentDownloads.size();
   }
}