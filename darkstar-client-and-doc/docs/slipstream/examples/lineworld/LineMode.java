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

import com.sun.slipstream.Group;
import com.sun.slipstream.ObjectWrapper;
import com.sun.slipstream.UnmoderatedGroup;
import com.sun.slipstream.UserPlayer;

import com.sun.slipstream.game.CollisionEvent;
import com.sun.slipstream.game.Event;
import com.sun.slipstream.game.EventResponse;
import com.sun.slipstream.game.GameMode;
import com.sun.slipstream.game.GameProxy;
import com.sun.slipstream.game.LeaveNotificationHandle;
import com.sun.slipstream.game.LeftRegionEvent;

import com.sun.slipstream.region.Region;
import com.sun.slipstream.region.RegionFactory;
import com.sun.slipstream.region.RegionProxy;

import com.sun.slipstream.shared.Coordinate;

import com.sun.slipstream.shared.message.Message;
import com.sun.slipstream.shared.message.MovementMessage;

import java.io.Serializable;

import region.OneDimensionalRegion;

import lobby.BasicResponse;


/**  */
public class LineMode implements GameMode, Serializable, ManagedObject {

    private static final long serialVersionUID = 1;

    private final String name;

    private final int length;

    private final ManagedReference<? extends Group> chatGroupRef;

    private final ManagedReference<? extends Region> regionRef;

    private final static Coordinate START = new Coordinate(0, 0, 0);

    /**  */
    public LineMode(String name, int length, int mobCount,
                    RegionFactory factory)
    {
        this.name = name;
        this.length = length;
        chatGroupRef = AppContext.getDataManager().
            createReference(new UnmoderatedGroup(name + "Chat", false));
        regionRef = AppContext.getDataManager().
            createReference(new OneDimensionalRegion(name + "Region", length));
        // TODO: add the MOBs to the line...which will probably require that
        // a MOB have a way to interact with its mode
    }

    /* Implement GameMode */

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    public GameProxy join(UserPlayer player, LeaveNotificationHandle handle) {
        chatGroupRef.get().join(player);
        return new UserLineModeProxy(player, regionRef.get(), this, handle);
    }

    /* Private utilities */

    /**  */
    private void notifyLeft(UserPlayer player) {
        chatGroupRef.get().leave(player);
    }

    /** Private implementation of GameProxy for UserPlayers. */
    static class UserLineModeProxy implements GameProxy, Serializable {
        private static final long serialVersionUID = 1;
        private final ManagedReference<? extends UserPlayer> playerRef;
        private final ObjectWrapper<? extends RegionProxy> wrappedRegionProxy;
        private final ManagedReference<LineMode> modeRef;
        private final ObjectWrapper<? extends LeaveNotificationHandle>
                                              wrappedHandle;
        /**  */
        UserLineModeProxy(UserPlayer player, Region region, LineMode mode,
                          LeaveNotificationHandle handle)
        {
            DataManager dm = AppContext.getDataManager();
            playerRef = dm.createReference(player);
            wrappedRegionProxy =
                new ObjectWrapper<RegionProxy>(region.join(player, START));
            modeRef = dm.createReference(mode);
            wrappedHandle =
                new ObjectWrapper<LeaveNotificationHandle>(handle);
        }
        /** {@inheritDoc} */
        public void handleMessage(Message message) {
            if (message.getMessageId() != MovementMessage.STANDARD_ID) {
                return;
            }
            MovementMessage msg = (MovementMessage) message;
            boolean moved =
                wrappedRegionProxy.get().move(msg.getLocation(), msg.getSpeed());
            if (! moved) {
                // TODO: send back a message that the movement failed
            }
        }
        /** {@inheritDoc} */
        public EventResponse handleEvent(Event event) {
            switch (event.getEventId()) {
            case CollisionEvent.ID:
                return BasicResponse.DENY;
            case LeftRegionEvent.ID:
                handleLeft(true);
            }
            return BasicResponse.ACCEPT;
        }
        /** {@inheritDoc} */
        public void loggedOut() {
            handleLeft(false);
        }
        /** Private handler for when the player leaves this mode. */
        private void handleLeft(boolean stillPlaying) {
            wrappedRegionProxy.get().release();
            modeRef.get().notifyLeft(playerRef.get());
            wrappedHandle.get().leave(stillPlaying);
        }
    }

}
