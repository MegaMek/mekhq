/*
 * WorkItem.java
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

package mekhq.campaign.work;

import java.io.PrintWriter;
import java.io.Serializable;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.TargetRoll;
import mekhq.MekHQApp;
import mekhq.campaign.MekHqXmlSerializable;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.team.SupportTeam;

/**
 * An abstract class representing some item that needs work will be extended for
 * repairs, replacement, reloading, etc.
 * 
 * @author Taharqa
 */
public abstract class WorkItem implements Serializable, MekHqXmlSerializable {
	private static final long serialVersionUID = -751881044717430784L;
	public static final int MODE_NORMAL = 0;
	public static final int MODE_EXTRA_ONE = 1;
	public static final int MODE_EXTRA_TWO = 2;
	public static final int MODE_RUSH_ONE = 3;
	public static final int MODE_RUSH_TWO = 4;
	public static final int MODE_RUSH_THREE = 5;
	public static final int MODE_N = 6;

	public static final int NONE = -1;

	protected String name;
	// the id of this work item
	protected int id;
	// the skill modifier for difficulty
	protected int difficulty;
	// the amount of time for the repair (this is the base time)
	protected int time;
	// time spent on the task so far for tasks that span days
	protected int timeSpent;
	// the minimum skill level in order to attempt
	protected int skillMin;
	// has this task been successfully completed?
	protected boolean completed;
	// the team assigned to this task (will be null except for
	// PersonnelWorkItems
	protected SupportTeam team;
	protected int teamId;
	protected int mode;

	public WorkItem() {
		this.name = "Unknown";
		this.skillMin = SupportTeam.EXP_GREEN;
		this.completed = false;
		this.mode = MODE_NORMAL;
		this.timeSpent = 0;
	}
	
	public abstract void reCalc();

	public String getName() {
		return name;
	}

	public void setName(String s) {
		this.name = s;
	}

	public abstract String getDisplayName();

	public int getId() {
		return id;
	}

	public void setId(int i) {
		this.id = i;
	}

	public int getDifficulty() {
		return difficulty;
	}

	public int getBaseTime() {
		return time;
	}

	public int getActualTime() {
		switch (mode) {
		case MODE_EXTRA_ONE:
			return 2 * time;
		case MODE_EXTRA_TWO:
			return 4 * time;
		case MODE_RUSH_ONE:
			return (int) Math.ceil(time / 2.0);
		case MODE_RUSH_TWO:
			return (int) Math.ceil(time / 4.0);
		case MODE_RUSH_THREE:
			return (int) Math.ceil(time / 8.0);
		default:
			return time;
		}
	}

	public int getTimeLeft() {
		return getActualTime() - getTimeSpent();
	}

	public int getTimeSpent() {
		return timeSpent;
	}

	public void addTimeSpent(int m) {
		this.timeSpent += m;
	}

	public void resetTimeSpent() {
		this.timeSpent = 0;
	}

	public int getSkillMin() {
		return skillMin;
	}

	public void setSkillMin(int i) {
		this.skillMin = i;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean b) {
		this.completed = b;
	}

	public void complete() {
		setCompleted(true);
	}

	/*
	 * public String getDesc() { return getName() + " " + getStats(); }
	 */

	public String getDesc() {
		return getName() + " (" + getDetails() + ")";
	}

	public String getDetails() {
		return "";
	}

	public String getStats() {
		String scheduled = "";
		if (isAssigned()) {
			scheduled = " (scheduled) ";
		}
		return "[" + getTimeLeft() + "m/"
				+ SupportTeam.getRatingName(getSkillMin()) + "/"
				+ getAllMods().getValueAsString() + "]" + scheduled;
	}

	public String getDescHTML() {
		String bonus = getAllMods().getValueAsString();
		if (getAllMods().getValue() > -1) {
			bonus = "+" + bonus;
		}
		bonus = "(" + bonus + ")";
		String toReturn = "<html><font size='2'";

		String scheduled = "";
		if (isAssigned()) {
			scheduled = " (scheduled) ";
		}

		if (this instanceof ReplacementItem
				&& !((ReplacementItem) this).hasPart()) {
			toReturn += " color='white'";
		}
		toReturn += ">";
		toReturn += "<b>" + getName() + "</b><br/>";
		toReturn += getDetails() + "<br/>";
		toReturn += "" + getTimeLeft() + " minutes" + scheduled;
		toReturn += ", " + SupportTeam.getRatingName(getSkillMin());
		toReturn += " " + bonus;
		if (getMode() != MODE_NORMAL) {
			toReturn += "<br/><i>" + getCurrentModeName() + "</i>";
		}
		toReturn += "</font></html>";
		return toReturn;
	}

	public TargetRoll getAllMods() {
		TargetRoll mods = new TargetRoll(getDifficulty(), "difficulty");
		if (getModeMod() != 0) {
			mods.addModifier(getModeMod(), getCurrentModeName());
		}
		return mods;
	}

	public boolean isNeeded() {
		return true;
	}

	/**
	 * check whether this work item is currently fixable some conditions will
	 * make a work item impossible to fix
	 * 
	 * @return a <code>String</code> indicating the reason for non-fixability,
	 *         null if fixable
	 */
	public String checkFixable() {
		return null;
	}

	/**
	 * Resolve this work item (i.e. repair it, replace it, reload it, etc)
	 */
	public abstract void fix();

	public String succeed() {
		fix();
		complete();
		return " <font color='green'><b>task completed.</b></font>";
	}

	/**
	 * fail this work item
	 * 
	 * @param rating
	 *            - an <code>int</code> of the skill rating of the currently
	 *            assigned team
	 */
	public String fail(int rating) {
		// increment the minimum skill level required
		setSkillMin(rating + 1);
		resetTimeSpent();
		setTeam(null);
		return " <font color='red'><b>task failed.</b></font>";
	}

	public SupportTeam getTeam() {
		return team;
	}

	public void setTeam(SupportTeam t) {
		this.team = t;
	}

	public boolean isAssigned() {
		return (null != team);
	}

	public int getModeMod() {
		switch (mode) {
		case MODE_EXTRA_ONE:
			return -1;
		case MODE_EXTRA_TWO:
			return -2;
		default:
			return 0;
		}
	}

	public static String getModeName(int mode) {
		switch (mode) {
		case MODE_EXTRA_ONE:
			return "Extra time";
		case MODE_EXTRA_TWO:
			return "Extra time (x2)";
		case MODE_RUSH_ONE:
			return "Rush Job (1/2)";
		case MODE_RUSH_TWO:
			return "Rush Job (1/4)";
		case MODE_RUSH_THREE:
			return "Rush Job (1/8)";
		default:
			return "Normal";
		}
	}

	public String getCurrentModeName() {
		return getModeName(mode);
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int i) {
		this.mode = i;
	}

	/**
	 * checks whether the passed WorkItem is the same. This is used to check
	 * whether a reloaded unit's WorkItems should inherit some conditions from
	 * prior WorkItems
	 * 
	 * @param task
	 * @return
	 */
	public abstract boolean sameAs(WorkItem task);

	public String getToolTip() {
		return "<html>" + getStats() + "</html>";
	}

	public abstract void writeToXml(PrintWriter pw1, int indent, int id);

	protected void writeToXmlBegin(PrintWriter pw1, int indent, int id) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<workItem id=\"" + id
				+ "\" type=\"" + this.getClass().getName() + "\">");
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<name>" + name
				+ "</name>");
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<id>" + id + "</id>");
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<difficulty>"
				+ difficulty + "</difficulty>");
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<time>" + time
				+ "</time>");
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<timeSpent>"
				+ timeSpent + "</timeSpent>");
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<skillMin>"
				+ skillMin + "</skillMin>");
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<completed>"
				+ completed + "</completed>");
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<mode>" + mode
				+ "</mode>");

		// If the team's defined, save out its team ID...
		// But we aren't guaranteed it's defined.
		if (team != null) {
			pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<teamId>"
					+ team.getId() + "</teamId>");
		}
	}

	protected void writeToXmlEnd(PrintWriter pw1, int indent, int id) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</workItem>");
	}

	public int getSupportTeamId() {
		return teamId;
	}

	public void setSupportTeam(SupportTeam supportTeam) {
		team = supportTeam;
	}

	public static WorkItem generateInstanceFromXML(Node wn) {
		WorkItem retVal = null;
		NamedNodeMap attrs = wn.getAttributes();
		Node classNameNode = attrs.getNamedItem("type");
		String className = classNameNode.getTextContent();
		
		try {
			// Instantiate the correct child class, and call its parsing function.
			retVal = (WorkItem) Class.forName(className).newInstance();
			retVal.loadFieldsFromXmlNode(wn);
			
			// Okay, now load Part-specific fields!
			NodeList nl = wn.getChildNodes();
			
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				
				if (wn2.getNodeName().equalsIgnoreCase("name")) {
					retVal.name = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("difficulty")) {
					retVal.difficulty = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("id")) {
					retVal.id = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("mode")) {
					retVal.mode = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("skillMin")) {
					retVal.skillMin = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("teamId")) {
					retVal.teamId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("time")) {
					retVal.time = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("timeSpent")) {
					retVal.timeSpent = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("completed")) {
					if (wn2.getTextContent().equalsIgnoreCase("true"))
						retVal.completed = true;
					else
						retVal.completed = false;
				}
			}
		} catch (Exception ex) {
			// Errrr, apparently either the class name was invalid...
			// Or the listed name doesn't exist.
			// Doh!
			MekHQApp.logError(ex);
		}
		
		return retVal;
	}
	
	protected abstract void loadFieldsFromXmlNode(Node wn);
}
