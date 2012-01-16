/*
 * MedicalTeam.java
 * 
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

package mekhq.campaign.team;

import java.io.PrintWriter;

import mekhq.campaign.personnel.Person;

import org.w3c.dom.Node;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MedicalTeam extends SupportTeam {
	private static final long serialVersionUID = -1809295642059806908L;

    public MedicalTeam() {
    	this(null, 0);
    }
    
	public MedicalTeam(String name, int rating) {
        super(name, rating);
        this.fullSize = 5;
        this.currentSize = 5;
        reCalc();
    }
	
	@Override
	public void reCalc() {
		// Do nothing.
	}
  
    public int getPatients() {
       int patients = 0;
       return patients;
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		writeToXmlEnd(pw1, indent);
	}
	
	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		// Do nothing.
	}
}
