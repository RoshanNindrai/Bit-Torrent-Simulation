package edu.rit.cs4005730.dsgroup3.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import edu.rit.cs4005730.dsgroup3.GeneralStatistics;
import edu.rit.cs4005730.dsgroup3.Node;
import edu.rit.cs4005730.dsgroup3.NodeEvent;
import edu.rit.cs4005730.dsgroup3.NodeListener;
import edu.rit.cs4005730.dsgroup3.NodeManager;
import edu.rit.cs4005730.dsgroup3.NodeManagerListener;
import edu.rit.cs4005730.dsgroup3.NodeManagerRef;
import edu.rit.cs4005730.dsgroup3.NodeRef;
import edu.rit.cs4005730.dsgroup3.util.RemoteUtils;
import edu.rit.ds.registry.NotBoundException;
import edu.rit.ds.registry.RegistryProxy;
import edu.rit.ds.registry.RegistryServer;

/**
 * This UserInterface will register a {@link NodeManagerListener} with any {@link NodeManager} objects found in the RegistryServer and will show status
 * information in a GUI form. It will also allow the user to tweak settings, add/remove nodes on-the-fly, and start/stop the simulation. All settings will also
 * be set via a configuration file for automatic setup.
 * 
 * @author swr9990
 * 
 */
public class UserInterface extends JTabbedPane {
   // gets NodeManager from RegistryServer and then displays status for all seeds/peers, and all status graphs/data
   
   private final String       host         = "localhost";
   private final int          port         = 56789;
   
   private final JFrame       frame;
   
   private RegistryProxy      proxy;
   private NodeManagerRef     manager;
   
   private JPanel             seedPanel, peerPanel;
   
   private int                seedIndex    = 0;
   private int                peerIndex    = 0;
   
   private final NodeListener nodeListener = new NodeListener() {
                                              @Override
                                              public void report(final long theSequenceNumber, final NodeEvent theEvent) throws RemoteException {
                                                 UserInterface.this.frame.invalidate();
                                                 UserInterface.this.frame.repaint();
                                              }
                                           };
   
   public UserInterface(final String[] args) {
      if ((args == null) || (args.length != 0)) {
         throw new IllegalArgumentException("Usage: java Start UserInterface");
      }
      
      System.out.println(this.host + ":" + this.port);
      
      RemoteUtils.startProcess(RegistryServer.class, new String[] { this.host, Integer.toString(this.port) });
      RemoteUtils.sleep(1000);
      
      try {
         this.proxy = new RegistryProxy(this.host, this.port);
         System.out.println("proxy: " + this.proxy);
      } catch (final RemoteException e) {
         throw new IllegalArgumentException("NodeManager: Invalid Registry Server Not found");
      }
      
      RemoteUtils.startProcess(NodeManager.class, new String[] { this.host, Integer.toString(this.port), RemoteUtils.NODE_MANAGER_NAME });
      RemoteUtils.sleep(1000);
      
      try {
         this.manager = (NodeManagerRef) this.proxy.lookup(RemoteUtils.NODE_MANAGER_NAME);
         System.out.println("node manager: " + this.manager);
      } catch (final RemoteException e) {
         e.printStackTrace();
      } catch (final NotBoundException e) {
         e.printStackTrace();
      }
      
      this.frame = new JFrame("BitTorrent Simulation");
      this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      
      final JPanel nodePanel = this.getNodePanel();
      final JPanel settingsPanel = this.getSettingsPanel();
      final JPanel statsPanel = this.getStatsPanel();
      
      this.addTab("Node Status", nodePanel);
      this.addTab("Settings", settingsPanel);
      this.addTab("Statistics", statsPanel);
      this.frame.getContentPane().add(this);
      
      try {
         UnicastRemoteObject.exportObject(this.nodeListener, 0);
      } catch (final RemoteException e1) {
         e1.printStackTrace();
      }
      
      // TODO: fix node manager listener and update node panel to display nodes actually in node manager
      
      final NodeRef node = RemoteUtils.createNode(this.frame, this.seedPanel, "seed" + this.seedIndex++, this.host, this.port, true, RemoteUtils.getUpload(true), RemoteUtils.getDownload(true));
      try {
         node.addNodeListener(this.nodeListener);
      } catch (final RemoteException e) {
         e.printStackTrace();
      }
      
      this.show();
   }
   
   @Override
   public void show() {
      this.frame.setLocation(200, 200);
      this.frame.pack();
      this.frame.setVisible(true);
   }
   
   private JPanel getNodePanel() {
      this.seedPanel = new JPanel() {
         @Override
         public void paint(final Graphics g) {
            UserInterface.sortNodePanels(this);
            
            super.paint(g);
         }
      };
      this.seedPanel.setLayout(new BoxLayout(this.seedPanel, BoxLayout.PAGE_AXIS));
      this.peerPanel = new JPanel() {
         @Override
         public void paint(final Graphics g) {
            // UserInterface.sortNodePanels(this);
            
            NodePanel panel;
            
            // TODO: temp until remote events work
            for (int i = 0; i < UserInterface.this.peerPanel.getComponentCount(); i++) {
               panel = (NodePanel) UserInterface.this.peerPanel.getComponent(i);
               
               try {
                  if (panel.getNode().isSeed()) {
                     UserInterface.this.peerPanel.remove(panel);
                     UserInterface.this.seedPanel.add(panel);
                     UserInterface.this.seedPanel.invalidate();
                     UserInterface.this.peerPanel.invalidate();
                     UserInterface.this.frame.repaint();
                  }
               } catch (final RemoteException e) {
                  e.printStackTrace();
               }
            }
            
            super.paint(g);
         }
      };
      
      this.peerPanel.setLayout(new BoxLayout(this.peerPanel, BoxLayout.PAGE_AXIS));
      
      final JScrollPane seedScroll = new JScrollPane(this.seedPanel);
      seedScroll.setPreferredSize(new Dimension(325, 600));
      final JScrollPane peerScroll = new JScrollPane(this.peerPanel);
      peerScroll.setPreferredSize(new Dimension(325, 600));
      
      final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      panel.setPreferredSize(new Dimension(700, 650));
      
      final JSpinner seedSpinner = new JSpinner(new SpinnerNumberModel(RemoteUtils.SEED_SPINNER_DEFAULT, 1, 1000, 1));
      final JButton seedButton = new JButton("Add Seeds");
      final JSpinner peerSpinner = new JSpinner(new SpinnerNumberModel(RemoteUtils.PEER_SPINNER_DEFAULT, 1, 1000, 1));
      final JButton peerButton = new JButton("Add Peers");
      
      seedButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent e) {
            final int count = (Integer) seedSpinner.getValue();
            
            new Thread() {
               @Override
               public void run() {
                  for (int i = 0; i < count; i++) {
                     final Node node = RemoteUtils.createNode(UserInterface.this.frame, UserInterface.this.seedPanel, "seed" + UserInterface.this.seedIndex++, UserInterface.this.host,
                           UserInterface.this.port, true);
                     try {
                        node.addNodeListener(UserInterface.this.nodeListener);
                     } catch (final RemoteException e) {
                        e.printStackTrace();
                     }
                  }
                  
                  System.out.println("seeds added: " + count);
               }
            }.start();
         }
      });
      
      peerButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent e) {
            final int count = (Integer) peerSpinner.getValue();
            
            new Thread() {
               @Override
               public void run() {
                  for (int i = 0; i < count; i++) {
                     final Node node = RemoteUtils.createNode(UserInterface.this.frame, UserInterface.this.peerPanel, "peer" + UserInterface.this.peerIndex++, UserInterface.this.host,
                           UserInterface.this.port, false);
                     try {
                        node.addNodeListener(UserInterface.this.nodeListener);
                     } catch (final RemoteException e) {
                        e.printStackTrace();
                     }
                  }
                  
                  System.out.println("peers added: " + count);
               }
            }.start();
         }
      });
      
      panel.add(seedSpinner);
      panel.add(seedButton);
      panel.add(peerSpinner);
      panel.add(peerButton);
      
      final JPanel nodePanel = new JPanel(new GridLayout(1, 2));
      nodePanel.add(seedScroll);
      nodePanel.add(peerScroll);
      
      panel.add(nodePanel);
      
      return panel;
   }
   
   private JPanel getStatsPanel() {
      final int FIELD_WIDTH = 10;
      final JTextField seedCountLbl = new JTextField(FIELD_WIDTH);
      final JTextField peerCountLbl = new JTextField(FIELD_WIDTH);
      final JTextField meanDownloadTimeLbl = new JTextField(FIELD_WIDTH);
      final JTextField peersFinishedLbl = new JTextField(FIELD_WIDTH);
      final StringBuilder dataString = new StringBuilder();
      
      final JPanel panel = new JPanel(new GridBagLayout()) {
         @Override
         public void paint(final Graphics g) {
            // get data from the stats object on the node manager
            GeneralStatistics stats = null;
            try {
               stats = UserInterface.this.manager.getStats();
            } catch (final RemoteException e) {
               e.printStackTrace();
            }
            
            if (stats != null) {
               // set the labels
               seedCountLbl.setText("" + stats.getInitialSeedCount());
               peerCountLbl.setText("" + stats.getInitialPeerCount());
               meanDownloadTimeLbl.setText("" + (stats.getMeanTimeToCompletion() / 1000.0) + " seconds");
               peersFinishedLbl.setText("" + stats.getIsComplete());
               
               // construct the copyable CSV string
               dataString.setLength(0);
               dataString.append("" + stats.getInitialSeedCount());
               dataString.append("," + stats.getInitialPeerCount());
               dataString.append("," + stats.getMeanTimeToCompletion());
               dataString.append("," + stats.getIsComplete());
            } else {
               // failure
               seedCountLbl.setText("ERROR");
               peerCountLbl.setText("ERROR");
               meanDownloadTimeLbl.setText("ERROR");
               peersFinishedLbl.setText("ERROR");
               dataString.setLength(0);
            }
            super.paint(g);
         }
      };
      
      final GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridwidth = 1;
      
      // create labels
      c.gridx = 0;
      c.gridy = 0;
      c.weightx = 0;
      JLabel lbl = new JLabel("Seed Count ");
      lbl.setHorizontalAlignment(JLabel.RIGHT);
      panel.add(lbl, c);
      c.gridy++;
      lbl = new JLabel("Peer Count ");
      lbl.setHorizontalAlignment(JLabel.RIGHT);
      panel.add(lbl, c);
      c.gridy++;
      lbl = new JLabel("Mean Download Time ");
      lbl.setHorizontalAlignment(JLabel.RIGHT);
      panel.add(lbl, c);
      c.gridy++;
      lbl = new JLabel("Downloads Complete ");
      lbl.setHorizontalAlignment(JLabel.RIGHT);
      panel.add(lbl, c);
      
      // create value text fields
      c.gridy = 0;
      c.gridx = 1;
      seedCountLbl.setEditable(false);
      panel.add(seedCountLbl, c);
      c.gridy++;
      peerCountLbl.setEditable(false);
      panel.add(peerCountLbl, c);
      c.gridy++;
      meanDownloadTimeLbl.setEditable(false);
      panel.add(meanDownloadTimeLbl, c);
      c.gridy++;
      peersFinishedLbl.setEditable(false);
      panel.add(peersFinishedLbl, c);
      c.gridy++;
      
      // create the copy button
      c.gridx = 0;
      c.gridwidth = 2;
      final JButton copyButton = new JButton("Copy Data To Clipboard");
      panel.add(copyButton, c);
      copyButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent e) {
            final StringSelection stringSelection = new StringSelection(dataString.toString());
            final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, stringSelection);
         }
      });
      
      return panel;
   }
   
   private JPanel getSettingsPanel() {
      final JPanel panel = new JPanel(new GridBagLayout());
      final GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      
      c.gridx = 0;
      c.gridy = 0;
      c.gridwidth = 1;
      c.weightx = 0.21;
      final JLabel maxDownload = new JLabel("Max Download");
      panel.add(maxDownload, c);
      
      c.gridx = 1;
      c.gridy = 0;
      c.gridwidth = 3;
      c.weightx = 0.75;
      final JSpinner maxDownloadField = new JSpinner(new SpinnerNumberModel(Long.valueOf(RemoteUtils.DOWNLOAD_MAX), Long.valueOf(RemoteUtils.MINIMUM_SPEED_BPS),
            Long.valueOf(RemoteUtils.MAXIMUM_SPEED_BPS), Long.valueOf(256 * RemoteUtils.KB_IN_BYTES)));
      ((JSpinner.DefaultEditor) maxDownloadField.getEditor()).getTextField().setHorizontalAlignment(JTextField.LEFT);
      ((JSpinner.DefaultEditor) maxDownloadField.getEditor()).getTextField().setFormatterFactory(new TransferSpeedFormatFactory());
      panel.add(maxDownloadField, c);
      
      c.gridx = 0;
      c.gridy = 1;
      c.gridwidth = 1;
      c.weightx = 0.25;
      final JLabel minDownload = new JLabel("Min Download");
      panel.add(minDownload, c);
      
      c.gridx = 1;
      c.gridy = 1;
      c.gridwidth = 3;
      c.weightx = 0.75;
      final JSpinner minDownloadField = new JSpinner(new SpinnerNumberModel(Long.valueOf(RemoteUtils.DOWNLOAD_MIN), Long.valueOf(RemoteUtils.MINIMUM_SPEED_BPS),
            Long.valueOf(RemoteUtils.MAXIMUM_SPEED_BPS), Long.valueOf(256 * RemoteUtils.KB_IN_BYTES)));
      ((JSpinner.DefaultEditor) minDownloadField.getEditor()).getTextField().setHorizontalAlignment(JTextField.LEFT);
      ((JSpinner.DefaultEditor) minDownloadField.getEditor()).getTextField().setFormatterFactory(new TransferSpeedFormatFactory());
      panel.add(minDownloadField, c);
      
      c.gridx = 0;
      c.gridy = 2;
      c.gridwidth = 1;
      c.weightx = 0.25;
      final JLabel maxUpload = new JLabel("Max Upload");
      panel.add(maxUpload, c);
      
      c.gridx = 1;
      c.gridy = 2;
      c.gridwidth = 3;
      c.weightx = 0.75;
      final JSpinner maxUploadField = new JSpinner(new SpinnerNumberModel(Long.valueOf(RemoteUtils.UPLOAD_MAX), Long.valueOf(RemoteUtils.MINIMUM_SPEED_BPS),
            Long.valueOf(RemoteUtils.MAXIMUM_SPEED_BPS), Long.valueOf(256 * RemoteUtils.KB_IN_BYTES)));
      ((JSpinner.DefaultEditor) maxUploadField.getEditor()).getTextField().setHorizontalAlignment(JTextField.LEFT);
      ((JSpinner.DefaultEditor) maxUploadField.getEditor()).getTextField().setFormatterFactory(new TransferSpeedFormatFactory());
      panel.add(maxUploadField, c);
      
      c.gridx = 0;
      c.gridy = 3;
      c.gridwidth = 1;
      c.weightx = 0.25;
      final JLabel minUpload = new JLabel("Min Upload");
      panel.add(minUpload, c);
      
      c.gridx = 1;
      c.gridy = 3;
      c.gridwidth = 3;
      c.weightx = 0.75;
      final JSpinner minUploadField = new JSpinner(new SpinnerNumberModel(Long.valueOf(RemoteUtils.UPLOAD_MIN), Long.valueOf(RemoteUtils.MINIMUM_SPEED_BPS),
            Long.valueOf(RemoteUtils.MAXIMUM_SPEED_BPS), Long.valueOf(256 * RemoteUtils.KB_IN_BYTES)));
      ((JSpinner.DefaultEditor) minUploadField.getEditor()).getTextField().setHorizontalAlignment(JTextField.LEFT);
      ((JSpinner.DefaultEditor) minUploadField.getEditor()).getTextField().setFormatterFactory(new TransferSpeedFormatFactory());
      panel.add(minUploadField, c);
      
      c.gridx = 0;
      c.gridy = 4;
      c.gridwidth = 1;
      c.weightx = 0.25;
      final JLabel filesize = new JLabel("Filesize");
      panel.add(filesize, c);
      
      c.gridx = 1;
      c.gridy = 4;
      c.gridwidth = 3;
      c.weightx = 0.75;
      final JSpinner fileSizeField = new JSpinner(new SpinnerNumberModel(Long.valueOf(RemoteUtils.FILESIZE), Long.valueOf(RemoteUtils.KB_IN_BYTES), Long.valueOf(Long.MAX_VALUE),
            Long.valueOf(10 * RemoteUtils.FILESIZE_STEP)));
      ((JSpinner.DefaultEditor) fileSizeField.getEditor()).getTextField().setHorizontalAlignment(JTextField.LEFT);
      ((JSpinner.DefaultEditor) fileSizeField.getEditor()).getTextField().setFormatterFactory(new FileSizeFormatFactory());
      panel.add(fileSizeField, c);
      
      c.gridx = 0;
      c.gridy = 5;
      c.gridwidth = 1;
      c.weightx = 0.25;
      final JLabel blocksize = new JLabel("Block Size");
      panel.add(blocksize, c);
      
      c.gridx = 1;
      c.gridy = 5;
      c.gridwidth = 3;
      c.weightx = 0.75;
      final JSpinner blockSizeField = new JSpinner(new SpinnerNumberModel(Long.valueOf(RemoteUtils.BLOCK_SIZE), Long.valueOf(RemoteUtils.KB_IN_BYTES), Long.valueOf(256 * RemoteUtils.MB_IN_BYTES),
            Long.valueOf(256 * RemoteUtils.KB_IN_BYTES)));
      ((JSpinner.DefaultEditor) blockSizeField.getEditor()).getTextField().setHorizontalAlignment(JTextField.LEFT);
      ((JSpinner.DefaultEditor) blockSizeField.getEditor()).getTextField().setFormatterFactory(new FileSizeFormatFactory());
      panel.add(blockSizeField, c);
      
      c.gridx = 0;
      c.gridy = 6;
      c.gridwidth = 1;
      c.weightx = 0.25;
      final JLabel timeDilationLabel = new JLabel("Time Dilation");
      panel.add(timeDilationLabel, c);
      
      c.gridx = 1;
      c.gridy = 6;
      c.gridwidth = 3;
      c.weightx = 0.75;
      final JSpinner timeDilationSpinner = new JSpinner(new SpinnerNumberModel(RemoteUtils.TIME_DILATION_RATIO, 0.01, 10.0, 0.1));
      ((JSpinner.DefaultEditor) timeDilationSpinner.getEditor()).getTextField().setHorizontalAlignment(JTextField.LEFT);
      panel.add(timeDilationSpinner, c);
      
      c.gridx = 0;
      c.gridy = 7;
      c.gridwidth = 4;
      c.weightx = 1.0;
      c.insets = new Insets(50, 10, 10, 10);
      final JButton submit = new JButton("Submit Changes");
      panel.add(submit, c);
      
      submit.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent e) {
            final long maxDownload = Math.max(RemoteUtils.KB_IN_BYTES, ((Number) maxDownloadField.getValue()).longValue());
            final long minDownload = Math.max(RemoteUtils.KB_IN_BYTES, ((Number) minDownloadField.getValue()).longValue());
            final long maxUpload = Math.max(RemoteUtils.KB_IN_BYTES, ((Number) maxUploadField.getValue()).longValue());
            final long minUpload = Math.max(RemoteUtils.KB_IN_BYTES, ((Number) minUploadField.getValue()).longValue());
            
            final long fileSize = Math.max(RemoteUtils.KB_IN_BYTES, ((Number) fileSizeField.getValue()).longValue());
            final long blockSize = Math.max(RemoteUtils.KB_IN_BYTES, ((Number) blockSizeField.getValue()).longValue());
            final double timeDilation = ((Number) timeDilationSpinner.getValue()).doubleValue();
            
            RemoteUtils.DOWNLOAD_MAX = maxDownload;
            RemoteUtils.DOWNLOAD_MIN = minDownload;
            RemoteUtils.UPLOAD_MAX = maxUpload;
            RemoteUtils.UPLOAD_MIN = minUpload;
            
            RemoteUtils.FILESIZE = fileSize;
            RemoteUtils.BLOCK_SIZE = blockSize;
            RemoteUtils.BLOCK_COUNT = (int) Math.ceil(((double) RemoteUtils.FILESIZE / RemoteUtils.BLOCK_SIZE));
            
            RemoteUtils.TIME_DILATION_RATIO = timeDilation;
            
            try {
               final Collection<NodeRef> nodes = UserInterface.this.manager.getNodes();
               
               for (final NodeRef node : nodes) {
                  node.dispose();
               }
               
               UserInterface.this.seedPanel.removeAll();
               UserInterface.this.peerPanel.removeAll();
               
               UserInterface.this.seedIndex = 0;
               UserInterface.this.peerIndex = 0;
               
               final NodeRef node = RemoteUtils.createNode(UserInterface.this.frame, UserInterface.this.seedPanel, "seed" + UserInterface.this.seedIndex++, UserInterface.this.host,
                     UserInterface.this.port, true, RemoteUtils.getUpload(true), RemoteUtils.getDownload(true));
               
               node.addNodeListener(UserInterface.this.nodeListener);
            } catch (final RemoteException re) {
               
            }
         }
      });
      
      return panel;
   }
   
   private static void sortNodePanels(final JPanel panel) {
      final Component[] components = panel.getComponents();
      panel.removeAll();
      
      Arrays.sort(components, new Comparator<Component>() {
         @Override
         public int compare(final Component o1, final Component o2) {
            if ((o1 instanceof NodePanel) && (o2 instanceof NodePanel)) {
               try {
                  // negate so it goes highest to lowest
                  final int value = -Integer.valueOf(((NodePanel) o1).getNode().getUploadCount()).compareTo(Integer.valueOf(((NodePanel) o2).getNode().getUploadCount()));
                  
                  return value;
               } catch (final RemoteException e) {
                  return 0;
               }
            } else {
               return 0;
            }
         }
      });
      
      for (final Component c : components) {
         panel.add(c);
      }
   }
   
   public static void main(final String[] args) {
      new UserInterface(args);
   }
}
