/*
 *
 * Copyright (c) 2007-2010, Oracle and/or its affiliates.
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
 *
 */

package lineworld;

import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.ManagedReference;

import com.sun.slipstream.Item;
import com.sun.slipstream.UserPlayer;

import com.sun.slipstream.game.BasicUserPlayer;
import com.sun.slipstream.game.Game;
import com.sun.slipstream.game.GameMode;
import com.sun.slipstream.game.GameProxy;
import com.sun.slipstream.game.LeaveNotificationHandle;
import com.sun.slipstream.game.NameMappingUtil;

import com.sun.slipstream.region.RegionFactory;

import java.io.Serializable;

import java.util.Collection;
import java.util.Properties;

import lobby.LobbyMode;


/**
 * The first game written using slipstream, LineWorld is a very simple game
 * that allows movement along one of several sets of lines. Players start
 * in a lobby and then choose a starting line. This class handles setup of
 * these two types of modes (lobby and line), manages transition between
 * modes, and defines the game-specific aspects of a {@code UserPlayer}.
 * This is typically all that an implementation of {@code Game} is expected
 * to do.
 */
public class LineWorld implements Game, Serializable {

    private static final long serialVersionUID = 1;

    private static final String LOBBY_NAME = "lineworld:mode:lobby";
    private static final String LINES_NS = "lineworld:mode:lines:";

    /** Creates an instance of the Line World Game. */
    public LineWorld(Properties p, RegionFactory factory) {
        LobbyMode lobby = new LobbyMode("lobby");
        DataManager dm = AppContext.getDataManager();
        // NOTE: in a "real" game this would come from some configuration,
        // but for the sake of the demo, we'll just create a few lines..
        for (int i = 0; i < 10; i++) {
            String name = "line" + i;
            dm.setBinding(LINES_NS + name, new LineMode(name, 400, 5, factory));
            lobby.updateAvailableMode(name, 0);
        }
        dm.setBinding(LOBBY_NAME, lobby);
    }

    /* Implement Game. */

    /** {@inheritDoc} */
    public UserPlayer getUserPlayer(String name) {
        UserPlayer player = NameMappingUtil.getUserPlayer(name);
        if (player == null) {
            player = new LWUserPlayer(name);
            NameMappingUtil.addUserPlayer(player);
        }
        return player;
    }

    /** {@inheritDoc} */
    public GameProxy join(UserPlayer player) {
        return ((LobbyMode) (AppContext.getDataManager().
                             getBinding(LOBBY_NAME))).
            join(player, new LWLeaveNotificationHandle((LWUserPlayer) player));
    }

    /**
     * Implementation of LeaveNotificationHandle for all transitions. Note
     * that this implements ManagedObject only so that instances can remove
     * themselves after notification, to ensure that any given handle is
     * only notified once.
     * <p>
     * TODO: if the lobby gets updated to handle population counts, this
     * handle should report the joins and leaves.
     */
    static class LWLeaveNotificationHandle
        implements LeaveNotificationHandle, ManagedObject, Serializable
    {
        private static final long serialVersionUID = 1;
        private final ManagedReference<LWUserPlayer> playerRef;
        /** Creates an instance of LWLeaveNotificationHandle for the player. */
        LWLeaveNotificationHandle(LWUserPlayer player) {
            playerRef = AppContext.getDataManager().createReference(player);
        }
        /** {@inheritDoc} */
        public void leave(boolean joinNewMode) {
            AppContext.getDataManager().removeObject(this);
            // the player must be returning to the lobby
            LobbyMode mode = (LobbyMode) (AppContext.getDataManager().
                                          getBinding(LOBBY_NAME));
            playerRef.get().changeMode(mode);
        }
        /** {@inheritDoc} */
        public void leave(String newModeName) {
            AppContext.getDataManager().removeObject(this);
            // the player is joining a specific line
            LineMode mode = (LineMode) (AppContext.getDataManager().
                                        getBinding(LINES_NS + newModeName));
            playerRef.get().changeMode(mode);
        }
    }

    /** Game-specific implementation of UserPlayer. */
    static class LWUserPlayer extends BasicUserPlayer implements Serializable {
        private static final long serialVersionUID = 1;
        /** Creates an instance of LWUserPlayer with the given name. */
        LWUserPlayer(String name) {
            super(name);
        }
        /** Utility to update the player's current mode. */
        void changeMode(GameMode newMode) {
            setGameProxy(newMode.
                         join(this, new LWLeaveNotificationHandle(this)));
        }
    }

}
