/*
 * Copyright (c) 2020 The MegaMek Team.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.online.forces;

public class RemoteTOE {
    public static final RemoteTOE EMPTY_TOE = new RemoteTOE(RemoteForce.emptyForce());

	private final RemoteForce forces;

    public RemoteTOE(RemoteForce forces) {
        this.forces = forces;
    }

    public RemoteForce getForces() {
        return forces;
    }
}
