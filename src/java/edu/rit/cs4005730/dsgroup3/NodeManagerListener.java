package edu.rit.cs4005730.dsgroup3;

import edu.rit.ds.RemoteEventListener;

/**
 * The {@link NodeManagerListener} class is designed to be added to a {@link NodeManager} so that the requestor be notified when {@link Node} objects are added
 * or removed from the {@link NodeManager}.
 * 
 * @author Roshan Balaji
 * 
 */
public interface NodeManagerListener extends RemoteEventListener<NodeManagerEvent> {
   
}