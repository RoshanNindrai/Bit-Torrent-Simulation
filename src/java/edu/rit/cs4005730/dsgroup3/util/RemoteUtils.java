package edu.rit.cs4005730.dsgroup3.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

import edu.rit.cs4005730.dsgroup3.Node;
import edu.rit.cs4005730.dsgroup3.ui.FileSizeFormat;
import edu.rit.cs4005730.dsgroup3.ui.NodePanel;
import edu.rit.cs4005730.dsgroup3.ui.TransferSpeedFormat;
import edu.rit.ds.registry.NotBoundException;
import edu.rit.numeric.prob.UniformDoublePrng;

public class RemoteUtils {

   public static final long                 MB_IN_BYTES                = 1024 * 1024;
   public static final long                 KB_IN_BYTES                = 1024;
   public static final long                 MINIMUM_SPEED_BPS          = KB_IN_BYTES;
   public static final long                 MAXIMUM_SPEED_BPS          = 1024 * MB_IN_BYTES;
   public static final String               NODE_MANAGER_NAME          = "nodeManager";
   public static final long                 MILLISECOND_IN_NANOSECONDS = 1000000;
   public static final long                 SECONDS_IN_MILLISECONDS    = 1000;
   public static final int                  SEED_SPINNER_DEFAULT       = 1;
   public static final int                  PEER_SPINNER_DEFAULT       = 8;

   public static long                       UPLOAD_MAX                 = 1 * MB_IN_BYTES;
   public static long                       UPLOAD_MIN                 = 256 * KB_IN_BYTES;
   public static long                       DOWNLOAD_MAX               = 15 * MB_IN_BYTES;
   public static long                       DOWNLOAD_MIN               = 5 * MB_IN_BYTES;
   public static long                       FILESIZE                   = 100 * MB_IN_BYTES;
   public static final long                 FILESIZE_STEP              = 1 * MB_IN_BYTES;
   public static long                       BLOCK_SIZE                 = 256 * KB_IN_BYTES;
   public static int                        BLOCK_COUNT                = (int) Math.ceil(((double) FILESIZE / BLOCK_SIZE));
   public static double                     TIME_DILATION_RATIO        = 0.1;

   private static final UniformDoublePrng   RANDOM                     = new UniformDoublePrng(new Random(), 0.0, 1.0);
   private static final TransferSpeedFormat speedFormatter             = new TransferSpeedFormat();
   private static final FileSizeFormat      sizeFormatter              = new FileSizeFormat();

   private RemoteUtils() {
      // only static helper methods
   }

   public static void startProcess(final Class<?> clazz, final String[] args) {
      new Thread() {
         @Override
         public void run() {
            try {
               final Constructor<?> constructor = clazz.getDeclaredConstructor(String[].class);
               constructor.newInstance(new Object[] { args });
            } catch (final SecurityException e) {
               e.printStackTrace();
            } catch (final NoSuchMethodException e) {
               e.printStackTrace();
            } catch (final IllegalArgumentException e) {
               e.printStackTrace();
            } catch (final InstantiationException e) {
               e.printStackTrace();
            } catch (final IllegalAccessException e) {
               e.printStackTrace();
            } catch (final InvocationTargetException e) {
               e.printStackTrace();
            }

            try {
               Thread.currentThread().join();
            } catch (final InterruptedException e) {
               e.printStackTrace();
            }
         }
      }.start();
   }

   public static void sleep(final long millis) {
      try {
         Thread.sleep(millis);
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }
   }

   public static Node createNode(final JFrame frame, final JPanel panel, final String name, final String host, final int port, final boolean isSeed) {
      return createNode(frame, panel, name, host, port, isSeed, RemoteUtils.getUpload(false), RemoteUtils.getDownload(false));
   }

   public static Node createNode(final JFrame frame, final JPanel panel, final String name, final String host, final int port, final boolean isSeed, final double upload, final double download) {
      Node node = null;

      try {
         node = new Node(name, upload, download, RemoteUtils.FILESIZE, RemoteUtils.BLOCK_SIZE, RemoteUtils.BLOCK_COUNT, isSeed, host, port, NODE_MANAGER_NAME);
      } catch (final RemoteException e) {
         e.printStackTrace();
      } catch (final NotBoundException e) {
         e.printStackTrace();
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }

      final Node n = node;
      final Thread current = Thread.currentThread();

      new Thread() {
         @Override
         public void run() {
            try {
               if (n != null) {
                  panel.add(new NodePanel(n));
                  panel.invalidate();
                  frame.repaint();

                  n.startProcess();
               }
               current.join();
            } catch (final RemoteException e1) {
               e1.printStackTrace();
            } catch (final InterruptedException e1) {
               e1.printStackTrace();
            }
         }
      }.start();

      return node;
   }

   public static double getUpload(final boolean getMax) {
      return getMax ? UPLOAD_MAX : ((UPLOAD_MAX - UPLOAD_MIN) * RANDOM.next()) + RemoteUtils.UPLOAD_MIN;
   }

   public static double getDownload(final boolean getMax) {
      return getMax ? DOWNLOAD_MAX : ((DOWNLOAD_MAX - DOWNLOAD_MIN) * RANDOM.next()) + RemoteUtils.DOWNLOAD_MIN;
   }

   public static double getStep(final double duration) {
      if (duration < 10) {
         return duration;
      } else if (duration < 100) {
         return duration / 10.0;
      } else {
         return duration / 100.0;
      }
   }

   public static int getIncrements(final double duration) {
      if (duration < 10) {
         return 1;
      } else if (duration < 100) {
         return 10;
      } else {
         return 100;
      }
   }

   public static String formatSpeed(final double bytesPerSecond) {
      return speedFormatter.format(bytesPerSecond);
   }

   public static String formatSize(final double bytes) {
      return sizeFormatter.format(bytes);
   }

   public static double parseSpeed(final String speed) {
      try {
         return speedFormatter.parse(speed).doubleValue();
      } catch (final ParseException e) {
         return 0;
      }
   }

   public static long parseSize(final String text) {
      try {
         return sizeFormatter.parse(text).longValue();
      } catch (final ParseException e) {
         return 0;
      }
   }
}
