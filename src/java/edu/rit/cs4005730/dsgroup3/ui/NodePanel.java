package edu.rit.cs4005730.dsgroup3.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.rmi.RemoteException;
import java.text.DecimalFormat;

import javax.swing.JLabel;

import edu.rit.cs4005730.dsgroup3.Node;
import edu.rit.cs4005730.dsgroup3.util.RemoteUtils;

public class NodePanel extends JLabel {
   private static final long   serialVersionUID = 1L;
   
   private final DecimalFormat dFormatter       = new DecimalFormat("#.##");
   private final Node          node;
   
   public NodePanel(final Node node) {
      this.node = node;
      
      // TODO: update so events are sent correctly
      // try {
      // node.addNodeListener(new NodeListener() {
      // @Override
      // public void transferProgress(final long elapsed, final double percentComplete) throws RemoteException {
      //
      // }
      // });
      // } catch (final RemoteException e) {
      // e.printStackTrace();
      // }
      
      this.setPreferredSize(new Dimension(300, 100));
      this.setSize(new Dimension(300, 100));
      this.setMinimumSize(new Dimension(300, 100));
      this.setMaximumSize(new Dimension(300, 100));
      this.setAlignmentX(Component.CENTER_ALIGNMENT);
   }
   
   @Override
   public void paint(final Graphics g) {
      final int width = this.getWidth();
      final int height = this.getHeight();
      g.setColor(Color.LIGHT_GRAY);
      g.fillRect(0, 0, width, height);
      
      g.setColor(Color.BLACK);
      
      try {
         g.drawString("Max UL: " + RemoteUtils.formatSpeed(this.node.getMaximumUploadRate()), 5, 42);
         g.drawString("Max DL: " + RemoteUtils.formatSpeed(this.node.getMaximumDownloadRate()), 155, 42);
         g.drawString("UL: " + RemoteUtils.formatSpeed(this.node.getAvailableUploadRate()), 5, 56);
         g.drawString("DL: " + RemoteUtils.formatSpeed(this.node.getAvailableDownloadRate()), 155, 56);
         g.drawString("UpCount: " + this.node.getUploadCount(), 5, 70);
         g.drawString("DownCount: " + this.node.getDownloadCount(), 155, 70);
         g.drawString("Progress: " + this.dFormatter.format(this.node.getProgress() * 100.0) + " %", 5, 84);
         g.drawString("Elapsed: " + this.dFormatter.format(this.node.getTransferTime() / 1000.0) + " seconds", 155, 84);
         g.drawString("Name: " + this.node.getName(), 5, 98);
      } catch (final RemoteException e) {
         // ignore
      }
      
      g.drawRect(0, 0, width - 1, height - 1);
      
      final int startX = 5;
      final int barWidth = width - 15;
      final int startY = 5;
      final int barHeight = 25;
      int blockCount = 0;
      try {
         blockCount = (int) this.node.getBlockCount();
      } catch (final RemoteException e) {
         e.printStackTrace();
      }
      final double step = Math.ceil(barWidth / (double) blockCount);
      int i = 0;
      
      // for (int i = 0; i < this.blocks.length; i++) {
      // x = startX + (i * step * 2);
      for (double x = startX; (x < (startX + barWidth)) && (i < blockCount); x += step, i++) {
         try {
            g.setColor(getColor(this.node.getBlockCompletion(i)));
         } catch (final RemoteException e) {
            e.printStackTrace();
         }
         g.fillRect((int) x, startY, (int) step, barHeight);
      }
   }
   
   private static Color getColor(final double value) {
      final float fValue = (float) value;
      
      if (fValue == -1) {
         return Color.black;
      } else if (fValue == -0.5) {
         return Color.orange;
      } else if (fValue == 0) {
         return Color.red.darker();
      } else if (fValue >= 1) {
         return Color.blue;
      } else {
         try {
            return new Color(0f, fValue, 0f);
         } catch (final Exception e) {
            // wtf?
            System.err.println("Green Value Invalid: " + fValue);
            return Color.white;
         }
      }
   }
   
   public Node getNode() {
      return this.node;
   }
}
