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
import java.util.Collections;
import java.util.List;

import mekhq.online.Force;

public class RemoteForce {
    private static final RemoteForce EMPTY_FORCE = new RemoteForce(-1, "", Collections.emptyList(), Collections.emptyList());

    private final int id;
    private final String name;
    private final List<RemoteForce> subForces;
    private final List<RemoteUnit> units;

    public RemoteForce(int id, String name, List<RemoteForce> subForces, List<RemoteUnit> units) {
        this.id = id;
        this.name = name;
        this.subForces = Collections.unmodifiableList(subForces);
        this.units = Collections.unmodifiableList(units);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<RemoteForce> getSubForces() {
        return subForces;
    }

    public List<RemoteUnit> getUnits() {
        return units;
    }

    public static RemoteForce build(Force force) {
        return new RemoteForce(force.getId(), force.getName(), build(force.getSubForcesList()), RemoteUnit.build(force.getUnitsList()));
    }

    public static List<RemoteForce> build(List<Force> subForces) {
        List<RemoteForce> remoteForces = new ArrayList<>(subForces.size());
        for (Force force : subForces) {
            remoteForces.add(build(force));
        }
        return remoteForces;
    }

	public List<Object> getAllChildren() {
        List<Object> children = new ArrayList<>(subForces.size() + units.size());
        children.addAll(subForces);
        children.addAll(units);
        return children;
	}

	public static RemoteForce emptyForce() {
		return EMPTY_FORCE;
	}
}
