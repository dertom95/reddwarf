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
package com.sun.darkstar.example.snowman.game.entity.influence;

import com.sun.darkstar.example.snowman.common.entity.influence.enumn.EInfluence;
import com.sun.darkstar.example.snowman.common.interfaces.IDynamicEntity;

/**
 * <code>SlipperyInfluence</code> slows down the target in movement speed.
 * 
 * @author Yi Wang (Neakor)
 * @version Creation date: 06-09-2008 12:10 EST
 * @version Modified date: 06-18-2008 12:45 EST
 */
public class SlipperyInfluence extends Influence {
	/**
	 * The reduction percentage in movement speed.
	 */
	private final float reduceSpeed;

	/**
	 * Constructor of <code>SlipperyInfluence</code>.
	 */
	public SlipperyInfluence() {
		super(EInfluence.Slippery);
		// TODO Set values.
		this.reduceSpeed = 0;
	}
	
	@Override
	public void perform(IDynamicEntity entity) {
		// TODO Auto-generated method stub

	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Class getClassTag() {
		return SlipperyInfluence.class;
	}
}
