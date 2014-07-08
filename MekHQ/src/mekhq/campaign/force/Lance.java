/**
 * 
 */
package mekhq.campaign.force;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.Infantry;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * Used by Against the Bot to track additional information about each force
 * on the TO&E that has at least one unit assigned. Extra info includes whether
 * the force counts as a lance (or star or level II) eligible for assignment
 * to a mission role and what the assignment is on which contract.
 * 
 * @author Neoancient
 *
 */

public class Lance implements Serializable, MekHqXmlSerializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1197697940987478509L;
	
	public static final int ROLE_UNASSIGNED = 0;
	public static final int ROLE_FIGHT = 1;
	public static final int ROLE_DEFEND = 2;
	public static final int ROLE_SCOUT = 3;
	public static final int ROLE_TRAINING = 4;
	public static final int ROLE_NUM = 5;
	
	public static final String[] roleNames = {
		"Unassigned", "Fight", "Defend", "Scout", "Training"
	};
	
	public static final int STR_IS = 4;
	public static final int STR_CLAN = 5;
	public static final int STR_CS = 6;
	
	public static final long ETYPE_GROUND = Entity.ETYPE_MECH |
			Entity.ETYPE_TANK | Entity.ETYPE_INFANTRY | Entity.ETYPE_PROTOMECH;

	private int forceId;
	private int missionId;
	private int role;
	private UUID commanderId;
	
	public Lance() {}

	public Lance(int fid, Campaign c) {
		forceId = fid;
		role = ROLE_UNASSIGNED;
		missionId = -1;
		for (Mission m : c.getSortedMissions()) {
			if (!m.isActive()) {
				break;
			}
			if (m instanceof AtBContract) {
				if (null == ((AtBContract)m).getParentContract()) {
					missionId = m.getId();
				} else {
					missionId = ((AtBContract)m).getParentContract().getId();
				}
			}
		}
		commanderId = findCommander(forceId, c);
	} 
	
	public int getForceId() {
		return forceId;
	}
	
	public int getMissionId() {
		return missionId;
	}
	
	public AtBContract getContract(Campaign c) {
		return (AtBContract)c.getMission(missionId);
	}
	
	public void setContract(AtBContract c) {
		if (null == c) {
			missionId = -1;
		} else {
			missionId = c.getId();
		}
	}
	
	public int getRole() {
		return role;
	}
	
	public void setRole(int role) {
		this.role = role;
	}
	
	public UUID getCommanderId() {
		return commanderId;
	}
	
	public Person getCommander(Campaign c) {
		return c.getPerson(commanderId);
	}
	
	public void setCommander(UUID id) {
		commanderId = id;
	}
	
	public void setCommander(Person p) {
		commanderId = p.getId();
	}
	
	public void refreshCommander(Campaign c) {
		commanderId = findCommander(forceId, c);
	}
	
	public int getSize(Campaign c) {
		if (c.getFaction().isClan()) {
			return (int)Math.ceil(getEffectivePoints(c));
		}
		return c.getForce(forceId).getUnits().size();
	}
	
	public double getEffectivePoints(Campaign c) {
		/* Used to check against force size limits; for this purpose we
		 * consider a 'Mech and a Point of BA to be a single Point so that
		 * a Nova that has 10 actual Points is calculated as 5 effective
		 * Points. We also count Points of vehicles with 'Mechs and
		 * conventional infantry with BA to account for CHH vehicle Novas.
		 */
		double armor = 0.0;
		double infantry = 0.0;
		double other = 0.0;
		for (UUID id : c.getForce(forceId).getUnits()) {
			if ((c.getUnit(id).getEntity().getEntityType() & Entity.ETYPE_MECH) != 0) {
				armor += 1;
			} else if ((c.getUnit(id).getEntity().getEntityType() & Entity.ETYPE_AERO) != 0) {
				other += 0.5;
			} else if ((c.getUnit(id).getEntity().getEntityType() & Entity.ETYPE_TANK) != 0) {
				armor += 0.5;
			} else if ((c.getUnit(id).getEntity().getEntityType() & Entity.ETYPE_PROTOMECH) != 0) {
				other += 0.2;
			} else if ((c.getUnit(id).getEntity().getEntityType() & Entity.ETYPE_INFANTRY) != 0) {
				infantry += ((Infantry)c.getUnit(id).getEntity()).isSquad()?0.2:1;
			}
		}
		return Math.max(armor, infantry) + other;
	}
	
	public int getWeightClass(Campaign c) {
		/* Clan units only count half the weight of ASF and vehicles
		 * (2/Point). IS units only count half the weight of vehicles
		 * if the option is enabled, possibly dropping the lance to a lower
		 * weight class and decreasing the enemy force against vehicle/combined
		 * lances.
		 */
		double weight = 0.0;
		for (UUID id : c.getForce(forceId).getUnits()) {
			if ((c.getUnit(id).getEntity().getEntityType() & Entity.ETYPE_MECH) != 0 ||
					(c.getUnit(id).getEntity().getEntityType() & Entity.ETYPE_PROTOMECH) != 0 ||
					(c.getUnit(id).getEntity().getEntityType() & Entity.ETYPE_INFANTRY) != 0) {
				weight += c.getUnit(id).getEntity().getWeight();
			} else if ((c.getUnit(id).getEntity().getEntityType() & Entity.ETYPE_TANK) != 0) {
				if (c.getFaction().isClan() || c.getCampaignOptions().getAdjustPlayerVehicles()) {
					weight += c.getUnit(id).getEntity().getWeight() * 0.5;				
				} else {
					weight += c.getUnit(id).getEntity().getWeight();				
				}
			} else if ((c.getUnit(id).getEntity().getEntityType() & Entity.ETYPE_AERO) != 0) {
				if (c.getFaction().isClan()) {
					weight += c.getUnit(id).getEntity().getWeight() * 0.5;				
				} else {
					weight += c.getUnit(id).getEntity().getWeight();				
				}
			}
		}
		weight = weight * 4.0 / c.getCampaignOptions().getLanceStructure();
		if (weight < 40) {
			return EntityWeightClass.WEIGHT_ULTRA_LIGHT;
		}
		if (weight <= 130) {
			return EntityWeightClass.WEIGHT_LIGHT;
		}
		if (weight <= 200) {
			return EntityWeightClass.WEIGHT_MEDIUM;
		}
		if (weight <= 280) {
			return EntityWeightClass.WEIGHT_HEAVY;
		}
		if (weight <= 390) {
			return EntityWeightClass.WEIGHT_ASSAULT;
		}
		return EntityWeightClass.WEIGHT_SUPER_HEAVY;
	}

	public boolean isEligible(Campaign c) {
		/* Check that the number of units and weight are within the limits
		 * and that the force contains at least one ground unit. */
		if (c.getCampaignOptions().getLimitLanceNumUnits()) {
			int size = getSize(c);
			if (size < c.getCampaignOptions().getLanceStructure() - 1 ||
					size > c.getCampaignOptions().getLanceStructure() + 2) {
				return false;
			}
		}
		
		if (c.getCampaignOptions().getLimitLanceWeight() &&
				getWeightClass(c) > EntityWeightClass.WEIGHT_ASSAULT) {
			return false;
		}
		
		for (UUID id : c.getForce(forceId).getUnits()) {
			if ((c.getUnit(id).getEntity().getEntityType() & ETYPE_GROUND) != 0) {
				return true;
			}
		}
		
		return false;
	}
	
	/* Code to find unit commander from ForceViewPanel */
	
	public static UUID findCommander(int forceId, Campaign c) {
		ArrayList<Person> people = new ArrayList<Person>();
		for(UUID uid : c.getForce(forceId).getAllUnits()) {
			Unit u = c.getUnit(uid);
			if(null != u) {
				Person p = u.getCommander();
				if(null != p) {
					people.add(p);
				}
			}
		}
		//sort person vector by rank
		Collections.sort(people, new Comparator<Person>(){		 
			public int compare(final Person p1, final Person p2) {
				return ((Comparable<Integer>)p2.getRankNumeric()).compareTo(p1.getRankNumeric());
			}
		});
		if(people.size() > 0) {
			return people.get(0).getId();
		}
		return null;
	}

	public static Date getBattleDate(GregorianCalendar c) {
		GregorianCalendar calendar = (GregorianCalendar)c.clone();
		calendar.add(Calendar.DATE, Compute.randomInt(7));
		return calendar.getTime();
	}
	
	public AtBScenario checkForBattle(Campaign c) {
		int noBattle;
		int roll;
		int battleTypeMod = (AtBContract.MORALE_NORMAL - getContract(c).getMoraleLevel()) * 5; 
		battleTypeMod += getContract(c).getBattleTypeMod();
		switch (role) {
		case ROLE_FIGHT:
			noBattle = (int)(60.0 / c.getCampaignOptions().getIntensity() + 0.5);
			roll = Compute.randomInt(40 + noBattle) + battleTypeMod;
			if (roll < 1) {
				return new AtBScenario(c, this,
						AtBScenario.BASEATTACK, false,
						getBattleDate(c.getCalendar()));
			} else if (roll < 9) {
				return new AtBScenario(c, this,
						AtBScenario.BREAKTHROUGH, true,
						getBattleDate(c.getCalendar()));
			} else if (roll < 9 + noBattle) {
				return null;
			} else if (roll < 17 + noBattle) {
				return new AtBScenario(c, this,
						AtBScenario.STANDUP, true,
						getBattleDate(c.getCalendar()));
			} else if (roll < 25 + noBattle) {
				return new AtBScenario(c, this,
						AtBScenario.STANDUP, false,
						getBattleDate(c.getCalendar()));
			} else if (roll < 33 + noBattle) {
				return new AtBScenario(c, this,
						AtBScenario.CHASE, false,
						getBattleDate(c.getCalendar()));
			} else if (roll < 41 + noBattle) {
				return new AtBScenario(c, this,
						AtBScenario.HOLDTHELINE, true,
						getBattleDate(c.getCalendar()));
			} else {
				return new AtBScenario(c, this,
						AtBScenario.BASEATTACK, true,
						getBattleDate(c.getCalendar()));
			}
		case Lance.ROLE_SCOUT:
			noBattle = (int)(40.0 / c.getCampaignOptions().getIntensity() + 0.5);
			roll = Compute.randomInt(60 + noBattle) + battleTypeMod;
			if (roll < 1) {
				return new AtBScenario(c, this,
						AtBScenario.BASEATTACK, false,
						getBattleDate(c.getCalendar()));
			} else if (roll < 11) {
				return new AtBScenario(c, this,
						AtBScenario.CHASE, true,
						getBattleDate(c.getCalendar()));
			} else if (roll < 21) {
				return new AtBScenario(c, this,
						AtBScenario.HIDEANDSEEK, true,
						getBattleDate(c.getCalendar()));
			} else if (roll < 31) {
				return new AtBScenario(c, this,
						AtBScenario.PROBE, true,
						getBattleDate(c.getCalendar()));
			} else if (roll < 41) {
				return new AtBScenario(c, this,
						AtBScenario.PROBE, true,
						getBattleDate(c.getCalendar()));
			} else if (roll < 41 + noBattle) {
				return null;
			} else if (roll < 51 + noBattle) {
				return new AtBScenario(c, this,
						AtBScenario.EXTRACTION, true,
						getBattleDate(c.getCalendar()));
			} else {
				return new AtBScenario(c, this,
						AtBScenario.RECONRAID, true,
						getBattleDate(c.getCalendar()));
			}
		case Lance.ROLE_DEFEND:
			noBattle = (int)(80.0 / c.getCampaignOptions().getIntensity() + 0.5);
			roll = Compute.randomInt(20 + noBattle) + battleTypeMod;
			if (roll < 1) {
				return new AtBScenario(c, this,
						AtBScenario.BASEATTACK, false,
						getBattleDate(c.getCalendar()));
			} else if (roll < 5) {
				return new AtBScenario(c, this,
						AtBScenario.HOLDTHELINE, false,
						getBattleDate(c.getCalendar()));
			} else if (roll < 9) {
				return new AtBScenario(c, this,
						AtBScenario.RECONRAID, false,
						getBattleDate(c.getCalendar()));
			} else if (roll < 13) {
				return new AtBScenario(c, this,
						AtBScenario.EXTRACTION, false,
						getBattleDate(c.getCalendar()));
			} else if (roll < 13 + noBattle) {
				return null;
			} else if (roll < 97 + noBattle) {
				return new AtBScenario(c, this,
						AtBScenario.HIDEANDSEEK, true,
						getBattleDate(c.getCalendar()));
			} else {
				return new AtBScenario(c, this,
						AtBScenario.BREAKTHROUGH, false,
						getBattleDate(c.getCalendar()));
			}
		case Lance.ROLE_TRAINING:
			noBattle = (int)(90.0 / c.getCampaignOptions().getIntensity() + 0.5);
			roll = Compute.randomInt(10 + noBattle) + battleTypeMod;
			if (roll < 1) {
				return new AtBScenario(c, this,
						AtBScenario.BASEATTACK, false,
						getBattleDate(c.getCalendar()));
			} else if (roll < 3) {
				return new AtBScenario(c, this,
						AtBScenario.HOLDTHELINE, false,
						getBattleDate(c.getCalendar()));
			} else if (roll < 5) {
				return new AtBScenario(c, this,
						AtBScenario.BREAKTHROUGH, true,
						getBattleDate(c.getCalendar()));
			} else if (roll < 7) {
				return new AtBScenario(c, this,
						AtBScenario.CHASE, true,
						getBattleDate(c.getCalendar()));
			} else if (roll < 9) {
				return new AtBScenario(c, this,
						AtBScenario.HIDEANDSEEK, false,
						getBattleDate(c.getCalendar()));
			} else if (roll < 9 + noBattle) {
				return null;
			} else {
				return new AtBScenario(c, this,
						AtBScenario.CHASE, false,
						getBattleDate(c.getCalendar()));
			}
		default:
			return null;
		}
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<lance type=\""
				+this.getClass().getName()
				+"\">");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<forceId>"
				+ forceId
				+"</forceId>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<missionId>"
				+ missionId
				+"</missionId>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<role>"
				+ role
				+"</role>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<commanderId>"
				+ commanderId
				+"</commanderId>");
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</lance>");
		
	}
	
	public static Lance generateInstanceFromXML(Node wn) {
		Lance retVal = null;
		NamedNodeMap attrs = wn.getAttributes();
		Node classNameNode = attrs.getNamedItem("type");
		String className = classNameNode.getTextContent();
		try {
			retVal = (Lance) Class.forName(className).newInstance();
			NodeList nl = wn.getChildNodes();
			
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				
				if (wn2.getNodeName().equalsIgnoreCase("forceId")) {
					retVal.forceId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("missionId")) {
					retVal.missionId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("role")) {
					retVal.role = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("commanderId")) {
					retVal.commanderId = UUID.fromString(wn2.getTextContent());
				}
			}
		} catch (Exception ex) {
			MekHQ.logError(ex);
		}
		return retVal;
	}
 }
