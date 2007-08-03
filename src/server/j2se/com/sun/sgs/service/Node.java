/*
 * Copyright 2007 Sun Microsystems, Inc. All rights reserved
 */

package com.sun.sgs.service;

/**
 * An abstraction for node information, used in conjunction
 * with the {@link WatchdogService} and {@link NodeListener}s.
 */
public interface Node {

    /**
     * Returns the node ID.
     *
     * @return the node ID
     */
    long getId();

    /** 
     * Returns this node's hostname.
     *
     * @return	this node's hostname
     */
    String getHostName();

    /**
     * Returns {@code true} if the node is known to be alive, and
     * {@code false} if the node is thought to have failed or is
     * unknown.
     *
     * @return	{@code true} if the node is alive, and {@code false}
     * 		otherwise
     */
    boolean isAlive();
}