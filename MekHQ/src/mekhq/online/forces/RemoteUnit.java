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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mekhq.online.ForceUnit;

public class RemoteUnit {
    private final UUID id;
    private final String name;
    private final String commander;

    public RemoteUnit(UUID id, String name, String commander) {
        this.id = id;
        this.name = name;
        this.commander = commander;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCommander() {
        return commander;
    }

    public static RemoteUnit build(ForceUnit unit) {
        return new RemoteUnit(UUID.fromString(unit.getId()), unit.getName(), unit.getCommander());
    }

	public static List<RemoteUnit> build(List<ForceUnit> units) {
        List<RemoteUnit> remoteUnits = new ArrayList<>(units.size());
		for (ForceUnit unit : units) {
            remoteUnits.add(build(unit));
        }
        return remoteUnits;
	}
}
