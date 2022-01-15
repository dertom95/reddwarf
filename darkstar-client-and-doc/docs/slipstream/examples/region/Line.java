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
import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.ManagedReference;

import com.sun.slipstream.Item;
import com.sun.slipstream.Player;

import com.sun.slipstream.game.CollisionEvent;

import com.sun.slipstream.shared.Coordinate;

import java.io.Serializable;

import java.util.HashSet;


class Line implements Serializable, ManagedObject {

    private final long length;
    private final ManagedReference<? extends LineEntry> leftRef;
    private final ManagedReference<? extends LineEntry> rightRef;

    Line(long length) {
        this.length = length;
        LineEntry left = new LeftEndEntry();
        LineEntry right = new RightEndEntry(length - 1);
        leftRef = AppContext.getDataManager().createReference(left);
        rightRef = AppContext.getDataManager().createReference(right);
        left.rightRef = rightRef;
        right.leftRef = leftRef;
    }

    LineEntry add(Player player, long position) {
        if ((position < 0) || (position >= length)) {
            throw new IllegalArgumentException("illegal starting location");
        }
        LineEntry entry = null;
        if (position == 0) {
            entry = leftRef.getForUpdate();
            entry.players.add(toRef(player));
        } else if (position == length - 1) {
            entry = rightRef.getForUpdate();
            entry.players.add(toRef(player));
        } else {
            entry = leftRef.get().getEntryOnLeft(position);
            if ((entry.players.isEmpty()) || (entry.allowCollision(player))) {
                entry.players.add(toRef(player));
            } else {
                entry = null;
            }
        }
        return entry;
    }

    void add(Item item, long position) {
        // TODO
    }

    static ManagedReference<? extends Player> toRef(Player player) {
        return AppContext.getDataManager().createReference(player);
    }

    abstract static class LineEntry implements ManagedObject, Serializable {
        long position;
        ManagedReference<? extends LineEntry> leftRef = null;
        ManagedReference<? extends LineEntry> rightRef = null;
        final HashSet<ManagedReference<? extends Player>> players =
                new HashSet<ManagedReference<? extends Player>>();

        protected LineEntry(long position) {
            this.position = position;
        }

        LineEntry getEntryOnLeft(long leftPosition) {
            assert leftPosition < position;
            LineEntry leftEntry = leftRef.get();
            if (leftEntry.position == leftPosition) {
                // the current enty to the left is what we want
                AppContext.getDataManager().markForUpdate(leftEntry);
                return leftEntry;
            } else if (leftEntry.position > leftPosition) {
                // the entry to the left is larger than what we want, so
                // we have to keep looking...should only be used for add()
                return leftEntry.getEntryOnLeft(leftPosition);
            } else {
                // there is no entry for the requested position
                if (isEmpty()) {
                    // re-use this entry
                    AppContext.getDataManager().markForUpdate(this);
                    position = leftPosition;
                    return this;
                } else {
                    LineEntry newEntry = new InteriorEntry(leftPosition);
                    newEntry.leftRef = leftRef;
                    newEntry.rightRef = AppContext.getDataManager().
                        createReference(this);
                    ManagedReference<? extends LineEntry> newRef =
                        AppContext.getDataManager().createReference(newEntry);
                    leftEntry.rightRef = newRef;
                    AppContext.getDataManager().markForUpdate(this);
                    leftRef = newRef;
                    return newEntry;
                }
            }
        }

        // Maybe pass a ref to all these methods since we'll already have it?

        LineEntry moveLeft(ManagedReference<? extends Player> playerRef) {
            AppContext.getDataManager().markForUpdate(this);
            if (! players.remove(playerRef)) {
                return null;
            }
            LineEntry leftEntry = getEntryOnLeft(position - 1);
            if ((leftEntry.players.isEmpty()) ||
                (leftEntry.allowCollision(playerRef.get())))
            {
                leftEntry.players.add(playerRef);
                return leftEntry;
            } else {
                // should this return null? something else?
                return this;
            }
        }

        LineEntry moveRight(ManagedReference<? extends Player> playerRef) {
            AppContext.getDataManager().markForUpdate(this);
            if (! players.remove(playerRef)) {
                return null;
            }
            LineEntry rightEntry =
                rightRef.getForUpdate().getEntryOnLeft(position + 1);
            if ((rightEntry.players.isEmpty()) ||
                (rightEntry.allowCollision(playerRef.get())))
            {
                rightEntry.players.add(playerRef);
                return rightEntry;
            } else {
                // should this return null? something else?
                return this;
            }
        }

        void remove(ManagedReference<? extends Player> playerRef) {
            AppContext.getDataManager().markForUpdate(this);
            players.remove(playerRef);
            if (isEmpty()) {
                rightRef.getForUpdate().leftRef = leftRef;
                leftRef.getForUpdate().rightRef = rightRef;
                AppContext.getDataManager().removeObject(this);
            }
        }

        void remove(Item item) {
            // TODO
        }

        HashSet<? extends Player> getPlayers(long radius) {
            long rightEnd = position + radius;
            long leftEnd = position - radius;
            HashSet<Player> set = new HashSet<Player>();
            LineEntry entry = this;
            while (entry.position <= rightEnd) {
                for (ManagedReference<? extends Player> playerRef :
                         entry.players)
                {
                    set.add(playerRef.get());
                }
                if (entry.rightRef == null) {
                    break;
                }
                entry = entry.rightRef.get();
            }
            if (leftRef != null) {
                entry = leftRef.get();
                while (entry.position >= leftEnd) {
                    for (ManagedReference<? extends Player> playerRef :
                             entry.players)
                    {
                        set.add(playerRef.get());
                    }
                    if (entry.leftRef == null) {
                        break;
                    }
                    entry = entry.leftRef.get();
                }
            }
            return set;
        }

        HashSet<? extends Item> getItems(long radius) {
            // TODO
            return null;
        }

        protected abstract boolean isEmpty();

        protected abstract boolean allowCollision(Player player);

    }

    private static class LeftEndEntry extends LineEntry
        implements Serializable
    {
        LeftEndEntry() {
            super(0);
        }
        LineEntry getEntryOnLeft(long leftPosition) {
            return null;
        }
        LineEntry moveLeft(Player player) {
            return null;
        }
        protected boolean isEmpty() {
            return false;
        }
        protected boolean allowCollision(Player player) {
            return true;
        }
    }

    private static class RightEndEntry extends LineEntry
        implements Serializable
    {
        RightEndEntry(long position) {
            super(position);
        }
        LineEntry moveRight(Player player) {
            return null;
        }
        protected boolean isEmpty() {
            return false;
        }
        protected boolean allowCollision(Player player) {
            return true;
        }
    }

    private static class InteriorEntry extends LineEntry
        implements Serializable
    {
        InteriorEntry(long position) {
            super(position);
        }
        protected boolean isEmpty() {
            return players.isEmpty();
        }
        protected boolean allowCollision(Player player) {
            CollisionEvent event =
                new CollisionEvent(new Coordinate(position, 0, 0));
            return player.getGameProxy().handleEvent(event).eventAccepted();
        }
    }

}
