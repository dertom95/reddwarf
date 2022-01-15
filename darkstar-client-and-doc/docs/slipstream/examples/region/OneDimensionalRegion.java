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

package region;

import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.ManagedReference;

import com.sun.slipstream.Item;
import com.sun.slipstream.Player;
import com.sun.slipstream.SingleSenderGroup;

import com.sun.slipstream.game.LeftRegionEvent;

import com.sun.slipstream.message.MessageManager;

import com.sun.slipstream.message.common.MovementMessageSpec;

import com.sun.slipstream.region.Region;
import com.sun.slipstream.region.RegionProxy;

import com.sun.slipstream.shared.Coordinate;

import com.sun.slipstream.shared.message.Message;

import java.io.Serializable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import region.Line.LineEntry;


/**
 * Simple implementation of {@code Region} supporting a line. For now
 * the speed is ignored and only contiguous moves are allowed. Any number of
 * players may be allowed on a given space, with the game logic deciding how
 * this gets handled. There is no notion of "collision" at the end-points
 * to allow anyone to join or leave a line. The only special notifications
 * the game logic are when a player goes off the line.
 */
public class OneDimensionalRegion implements Region, Serializable {

    // the length of the region
    final long length;

    // the single group used for this line
    final ManagedReference<SingleSenderGroup> groupRef;

    // the list that represents the region
    private final ManagedReference<Line> lineRef;

    /** TODO: maybe another version that takes a "visible" radius? */
    public OneDimensionalRegion(String regionName, long regionLength) {
        this.length = regionLength;
        groupRef = AppContext.getDataManager().
            createReference(new SingleSenderGroup(regionName, true));
        lineRef = AppContext.getDataManager().createReference(new Line(length));
    }

    /** {@inheritDoc} */
    public RegionProxy join(Player player, Coordinate location) {
        LineEntry entry = lineRef.get().add(player, (long) location.x);
        if (entry == null) {
            return null;
        }
        groupRef.get().join(player);
        return new OneDRegionProxy(this, player, entry);
    }

    /** {@inheritDoc} */
    public RegionProxy join(Region region, Coordinate location) {
        throw new UnsupportedOperationException("Cannot use sub-regions");
    }

    /** {@inheritDoc} */
    public RegionProxy join(Item item, Coordinate location) {
        //lineRef.get().add(item, location.x);
        //groupRef.get().send(null /* TODO: define addItem message */);
        return null;
    }

    /** A private implementation of {@code RegionProxy}. */
    private static class OneDRegionProxy implements RegionProxy, Serializable {
        private final ManagedReference<OneDimensionalRegion> regionRef;
        private final ManagedReference<Player> playerRef;
        private ManagedReference<? extends LineEntry> currentPointRef;
        private long currentLocation;
        OneDRegionProxy(OneDimensionalRegion region, Player player,
                        LineEntry startingPoint)
        {
            DataManager dataManager = AppContext.getDataManager();
            regionRef = dataManager.createReference(region);
            playerRef = dataManager.createReference(player);
            currentPointRef = dataManager.createReference(startingPoint);
            currentLocation = startingPoint.position;
        }
        public void release() {
            currentPointRef.get().remove(playerRef);
            regionRef.get().groupRef.get().leave(playerRef.get());
        }
        public Coordinate getLocation() {
            return new Coordinate(currentLocation, 0, 0);
        }
        // TODO: maybe allow arbitrary size moves?
        public boolean move(Coordinate location) {
            if (Math.abs(currentLocation - location.x) != 1) {
                return false;
            }
            LineEntry newEntry = null;
            if (location.x < currentLocation) {
                newEntry = currentPointRef.get().moveLeft(playerRef);
            } else {
                newEntry = currentPointRef.get().moveRight(playerRef);
            }
            if (newEntry != null) {
                if (newEntry.position == currentLocation) {
                    // we didn't actually move
                    return false;
                }
                currentLocation = (long)location.x;
                currentPointRef = AppContext.getDataManager().
                    createReference(newEntry);
                MovementMessageSpec spec = new MovementMessageSpec(location, 0);
                Message msg = AppContext.getManager(MessageManager.class).
                    createMessage(spec);
                regionRef.get().groupRef.get().send(msg);
            } else {
                // this means that they left the region
                playerRef.get().getGameProxy().
                    handleEvent(new LeftRegionEvent(location));
            }
            return true;
        }
        public boolean move(Coordinate location, float speed) {
            if (speed != 0) {
                return false;
            }
            return move(location);
        }
        public void sendMessage(Message message, float radius) {
            OneDimensionalRegion region = regionRef.get();
            if (radius < region.length) {
                for (Player player : getPlayers((long)radius)) {
                    if (player.wantsToHearMessages()) {
                        player.send(message);
                    }
                }
            } else {
                region.groupRef.get().send(message);
            }
        }
        public void sendToVisible(Message message) {
            regionRef.get().groupRef.get().send(message);
        }
        public Collection<? extends Player> getPlayers(float radius) {
            return currentPointRef.get().getPlayers((long)radius);
        }
        public Collection<? extends Item> getItems(float radius) {
            return currentPointRef.get().getItems((long)radius);
        }
        public Region getRegion() {
            return regionRef.get();
        }
    }

}
