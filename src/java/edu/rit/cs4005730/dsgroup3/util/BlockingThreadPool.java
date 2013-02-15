package edu.rit.cs4005730.dsgroup3.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BlockingThreadPool extends ThreadPoolExecutor {
   private final Semaphore semaphore;
   
   public BlockingThreadPool(final int poolSize) {
      super(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(Runtime.getRuntime().availableProcessors() - 1));
      this.semaphore = new Semaphore(Runtime.getRuntime().availableProcessors() - 1);
   }
   
   @Override
   public void execute(final Runnable task) {
      boolean waiting = true;
      do {
         try {
            // blocks until acquired or throws interrupted exception
            this.semaphore.acquire();
            waiting = false;
         } catch (final InterruptedException e) {
            // acquire was interrupted; lets attempt again
            // don't care, just keep trying
            try {
               Thread.sleep(100);
            } catch (final InterruptedException e1) {
               e1.printStackTrace();
            }
         }
      } while (waiting);
      
      try {
         super.execute(task);
      } catch (final RuntimeException e) {
         this.semaphore.release();
         throw e;
      } catch (final Error e) {
         this.semaphore.release();
         throw e;
      }
   }
   
   @Override
   protected void afterExecute(final Runnable r, final Throwable t) {
      this.semaphore.release();
   }
}