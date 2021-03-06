/*
 * Copyright (c) 2008, Sun Microsystems, Inc.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of Sun Microsystems, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.sun.darkstar.example.snowman.interfaces;

import com.sun.darkstar.example.snowman.exception.MissingComponentException;

/**
 * <code>IComponent</code> defines an interface for a <code>Component</code> which
 * can be connected to other <code>IComponent</code> to form a bigger system.
 * <p>
 * <code>IComponent</code> is initialized during activation after constructing the
 * instance. This allows for a system to form its essential connections before
 * actual initialization process.
 * <p>
 * <code>IComponent</code> can be activated or deactivated. An <code>IComponent</code>
 * is functional only when it is activated.
 * 
 * @author Yi Wang (Neakor)
 * @version Creation date: 05-23-2008 15:40 EST
 * @version Modified date: 06-06-2008 17:23 EST
 */
public interface IComponent {

	/**
	 * Activate this component.
	 * @throws <code>MissingComponentException</code> when an essential <code>IComponent</code> has not been connected.
	 */
	public void activate() throws MissingComponentException;
	
	/**
	 * Validate if this component has all the essential components connected.
	 * @return True if validation is successful.
	 * @throws <code>MissingComponentException</code> when an essential <code>IComponent</code> has not been connected.
	 */
	public boolean validate() throws MissingComponentException;

	/**
	 * Initialize this component.
	 */
	public void initialize();

	/**
	 * Deactivate this component.
	 */
	public void deactivate();
	
	/**
	 * Connect this component with the given component.
	 * @param component The <code>IComponent</code> to connect to.
	 */
	public void connect(IComponent component);
	
	/**
	 * Check if this component is currently active.
	 * @return True if this <code>IComponent</code> is active. False otherwise.
	 */
	public boolean isActive();
}
