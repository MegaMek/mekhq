/*
 * JumpPath,java
 * 
 * Copyright (c) 2011 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

package mekhq.campaign;

import java.util.ArrayList;

/**
 * This is an array list of planets for a jump path, from which we can derive
 * various statistics. We can also add in details about the jump path here, like if
 * the user would like to use recharge stations when available. For XML serialization, 
 * this object will need to spit out a list of planet names and then reconstruct 
 * the planets from that.
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class JumpPath {

	private ArrayList<Planet> path;
	
	public JumpPath() {
		path = new ArrayList<Planet>();
	}
	
	public JumpPath(ArrayList<Planet> p) {
		path = p;
	}
	
	public ArrayList<Planet> getPlanets() {
		return path;
	}
	
	public boolean isEmpty() {
		return path.isEmpty();
	}
	
	public Planet getFirstPlanet() {
		if(path.isEmpty()) {
			return null;
		} else {
			return path.get(0);
		}
	}
	
	public Planet getLastPlanet() {
		if(path.isEmpty()) {
			return null;
		} else {
			return path.get(path.size() - 1);
		}
	}
	
	public double getStartTime() {
		double startTime = 0.0;
		if(null != getFirstPlanet()) {
			startTime = getFirstPlanet().getTimeToJumpPoint(1.0);
		}
		return startTime;
	}
	
	public double getEndTime() {
		double endTime = 0.0;
		if(null != getLastPlanet()) {
			endTime = getLastPlanet().getTimeToJumpPoint(1.0);
		}
		return endTime;
	}
	
	public double getTotalRechargeTime() {
		int rechargeTime = 0;
		for(Planet planet : path) {
			rechargeTime += planet.getRechargeTime();
		}
		return rechargeTime/24.0;
	}
	
	public int getJumps() {
		return size()-1;
	}
	
	public double getTotalTime(boolean useStartTransit, boolean useEndTransit) {
		
		return getTotalRechargeTime() + (useStartTransit ? getStartTime():0) + (useEndTransit ? getEndTime():0);
	}
	
	public void addPlanet(Planet p) {
		path.add(p);
	}
	
	public void addPlanets(ArrayList<Planet> planets) {
		path.addAll(planets);
	}
	
	public void removeFirstPlanet() {
		if(!path.isEmpty()) {
			path.remove(0);
		}
	}
	
	public int size() {
		return path.size();
	}
	
	public Planet get(int i) {
		if(i >= size()) {
			return null;
		} else {
			return path.get(i);
		}
	}
	
	public boolean contains(Planet planet) {
		return path.contains(planet);
	}
}