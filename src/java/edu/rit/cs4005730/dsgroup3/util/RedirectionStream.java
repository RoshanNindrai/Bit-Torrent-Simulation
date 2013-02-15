package edu.rit.cs4005730.dsgroup3.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RedirectionStream {
   
   public RedirectionStream(final InputStream fin, final OutputStream fout) {
      new Thread() {
         @Override
         public void run() {
            int value = 0;
            try {
               while ((value = fin.read()) != -1) {
                  fout.write(value);
               }
            } catch (final IOException e) {
               // ignore; stream just closed
            }
         }
      }.start();
   }
}
