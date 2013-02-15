package edu.rit.cs4005730.dsgroup3;

import edu.rit.ds.RemoteEvent;

public class NodeEvent extends RemoteEvent {
   public final NodeRef node;
   public final String  name;
   public final double  progress;
   
   public NodeEvent(final NodeRef node, final String name, final double progress) {
      this.node = node;
      this.name = name;
      this.progress = progress;
   }
}
