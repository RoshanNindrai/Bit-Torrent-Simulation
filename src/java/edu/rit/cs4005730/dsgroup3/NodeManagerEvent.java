package edu.rit.cs4005730.dsgroup3;

import edu.rit.ds.RemoteEvent;

public class NodeManagerEvent extends RemoteEvent {
   public enum EventType {
      NODE_ADDED, NODE_REMOVED;
   }
   
   public final EventType type;
   public final NodeRef    node;
   public final String     name;
   
   public NodeManagerEvent(final EventType type, final NodeRef node, final String name) {
      this.type = type;
      this.node = node;
      this.name = name;
   }
}
