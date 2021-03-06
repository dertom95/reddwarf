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
package com.sun.darkstar.example.snowman.game.task;

import com.sun.darkstar.example.snowman.game.Game;
import com.sun.darkstar.example.snowman.game.task.enumn.ETask;
import com.sun.darkstar.example.snowman.interfaces.ITask;

/**
 * <code>Task</code> defines the most basic abstraction of all types of tasks.
 * Each <code>Task</code> has an unique pair of <code>ETaskType</code> and
 * <code>ETask</code> enumeration defined at construction time.
 * <p>
 * <code>Task</code> also maintains a reference to <code>Game</code> in order
 * to allow subclasses to access game data for logic execution.
 * <p>
 * Subclasses of <code>Task</code> needs to implement execution logic details.
 * 
 * @author Yi Wang (Neakor)
 * @version Creation date: 06-02-2008 16:47 EST
 * @version Modified date: 07-09-2008 13:51 EST
 */
public abstract class Task implements ITask {
	/**
	 * The <code>ETask</code> enumeration of this <code>Task</code>.
	 */
	protected final ETask enumn;
	/**
	 * The <code>Game</code> instance.
	 */
	protected final Game game;
	
	/**
	 * Constructor of <code>Task</code>.
	 * @param enumn The <code>ETask</code> of this </code>Task</code>.
	 * @param game The <code>Game</code> instance.
	 */
	public Task(ETask enumn, Game game) {
		this.enumn = enumn;
		this.game = game;
	}

	@Override
	public ETask getEnumn() {
		return this.enumn;
	}
}
