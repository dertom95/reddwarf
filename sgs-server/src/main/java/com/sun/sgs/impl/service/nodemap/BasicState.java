/*
 * Copyright 2007-2009 Sun Microsystems, Inc.
 *
 * This file is part of Project Darkstar Server.
 *
 * Project Darkstar Server is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * Project Darkstar Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sun.sgs.impl.service.nodemap;

/**
 * A class to encapsulate the basic states required for group activities.
 * The state at construction is {@link State#DISABLED}.
 */
public class BasicState {
    /** Valid states. */
    protected static enum State {
        /** Enabled, fully functional. Can be disabled or shutdown. */
	ENABLED,
        /** Disabled, can be enabled or shutdown. */
        DISABLED,
        /** Disabled, and cannot be enabled again. */
        SHUTDOWN
    }

    /** The state. Initialized to {@code DISABLED} */
    protected volatile State state = State.DISABLED;

    /**
     * Disable this component.  Multiple calls to disable are allowed.
     * Disabled components can be enabled or shutdown.
     *
     * @return {@code true} if the state has changed
     */
    protected boolean setDisabledState() {
        switch (state) {
            case ENABLED:
                state = State.DISABLED;
                return true;
            case DISABLED:
                // quietly return false (no state change)
                return false;
            case SHUTDOWN:
                throw new IllegalStateException("In shutdown state");
            default:
                throw new AssertionError();
        }
    }

    /**
     * Enable this component.  Multiple calls to enable are allowed.
     * Enabled components can be disabled or shutdown.
     * @return {@code true} if the state has changed
     */
    protected boolean setEnabledState() {
        switch (state) {
            case ENABLED:
                // quietly return false
                return false;
            case DISABLED:
                state = State.ENABLED;
                return true;
            case SHUTDOWN:
                throw new IllegalStateException("In shutdown state");
            default:
                throw new AssertionError();
        }
    }

    /**
     * Shutdown this component.  Multiple shutdown calls are allowed.
     * Once shut down, a component cannot be enabled or disabled.
     * @return {@code true} if the state has changed
     */
    protected boolean setShutdownState() {
        switch (state) {
            case ENABLED:
            case DISABLED:
                state = State.SHUTDOWN;
                break;
            case SHUTDOWN:
                // Quietly allow multiple shutdown calls
                return false;
            default:
                throw new AssertionError();
	}
        return true;
    }

    /**
     * Throws an IllegalStateException if we are in the {@code SHUTDOWN}
     * state.
     * @throws IllegalStateException if we are shut down
     */
    protected void checkForShutdownState() {
        if (state == State.SHUTDOWN) {
            throw new IllegalStateException("In shutdown state");
        }
    }

    /**
     * Throws an IllegalStateException if we are in the {@code SHUTDOWN} or
     * {@code DISABLED} state.
     * @throws IllegalStateException if we are shut down or disabled
     */
    protected void checkForDisabledOrShutdownState() {
        switch (state) {
            case DISABLED:
                throw new IllegalStateException("In disabled state");
            case SHUTDOWN:
                throw new IllegalStateException("In shutdown state");
            case ENABLED:
                // All is OK
                break;
            default:
                throw new AssertionError();
        }
    }
}
