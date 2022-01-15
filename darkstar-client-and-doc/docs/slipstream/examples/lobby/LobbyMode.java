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

package lobby;

import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.ManagedReference;

import com.sun.sgs.app.util.ScalableHashMap;

import com.sun.slipstream.Group;
import com.sun.slipstream.ObjectWrapper;
import com.sun.slipstream.SingleSenderGroup;
import com.sun.slipstream.UnmoderatedGroup;
import com.sun.slipstream.UserPlayer;

import com.sun.slipstream.game.Event;
import com.sun.slipstream.game.EventResponse;
import com.sun.slipstream.game.GameMode;
import com.sun.slipstream.game.GameProxy;
import com.sun.slipstream.game.LeaveNotificationHandle;

import com.sun.slipstream.shared.Coordinate;

import com.sun.slipstream.shared.message.Message;

import java.io.Serializable;


/**
 * Note that this could probably be a generic utility, or at least the
 * public methods should be factored into a utility interface.
 */
public class LobbyMode implements GameMode, Serializable, ManagedObject {

    private static final long serialVersionUID = 1;

    private final String modeName;

    private final ManagedReference<? extends Group> gamesUpdateGroupRef;
    private final ManagedReference<? extends Group> chatGroupRef;

    private final ManagedReference<ScalableHashMap<String,Integer>> modeMapRef;

    /** Create an instance of a lobby. */
    public LobbyMode(String lobbyName) {
        modeName = lobbyName;
        DataManager dm = AppContext.getDataManager();
        gamesUpdateGroupRef =
            dm.createReference(new SingleSenderGroup(lobbyName +
                                                     ":UpdateGroup", false));
        chatGroupRef =
            dm.createReference(new UnmoderatedGroup(lobbyName +
                                                    ":ChatGroup", false));
        modeMapRef =
            dm.createReference(new ScalableHashMap<String,Integer>());
    }

    /* Implement GameMode */

    /** {@inheritDoc} */
    public String getName() {
        return modeName;
    }

    /** {@inheritDoc} */
    public GameProxy join(UserPlayer player, LeaveNotificationHandle handle) {
        gamesUpdateGroupRef.get().join(player);
        chatGroupRef.get().join(player);
        return new LobbyProxy(this, player, handle);
    }

    /* Public methods used to update what's available from the lobby. */

    /** used to add and update? */
    public void updateAvailableMode(String modeName, int population) {
        modeMapRef.get().put(modeName, population);
        // TODO: send updated value to gamesUpdateGroup or queue for a batch
    }

    /**  */
    public void removeAvailableMode(String modeName) {
        modeMapRef.get().remove(modeName);
        // TODO: send removal update to gamesUpdateGroup or quqeue for a batch
    }

    /* Private methods used by the proxy */

    /** Notify that a player is leaving. */
    private void leave(UserPlayer player) {
        gamesUpdateGroupRef.get().leave(player);
        chatGroupRef.get().leave(player);
    }

    static class LobbyProxy implements GameProxy, Serializable {
        private static final long serialVersionUID = 1;
        private final ManagedReference<LobbyMode> lobbyRef;
        private final ManagedReference<? extends UserPlayer> playerRef;
        private final ObjectWrapper<? extends LeaveNotificationHandle>
                                              wrappedHandle;
        LobbyProxy(LobbyMode lobbyMode, UserPlayer player,
                   LeaveNotificationHandle handle)
        {
            lobbyRef = AppContext.getDataManager().createReference(lobbyMode);
            playerRef = AppContext.getDataManager().createReference(player);
            wrappedHandle =
                new ObjectWrapper<LeaveNotificationHandle>(handle);
        }
        public void handleMessage(Message message) {
            String modeName = null; // TODO: get this from the message
            wrappedHandle.get().leave(modeName);
        }
        public EventResponse handleEvent(Event event) {
            return BasicResponse.ACCEPT;
        }
        public void loggedOut() {
            lobbyRef.get().leave(playerRef.get());
            wrappedHandle.get().leave(false);
        }
    }

}
