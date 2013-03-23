package mekhq.campaign;

import gd.xml.ParseException;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BombType;
import megamek.common.CommonConstants;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.FighterSquadron;
import megamek.common.IArmorState;
import megamek.common.ILocationExposureStatus;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.WeaponType;
import megamek.common.XMLStreamParser;
import megamek.common.util.StringUtil;
import mekhq.MekHQ;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class MekHqXmlUtil {
	public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, String val) {
		for (int x=0; x<indent; x++)
			pw1.print("\t");
		
		pw1.print("<"+name+">");
		pw1.print(escape(val));
		pw1.println("</"+name+">");
	}

	public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, int val) {
		for (int x=0; x<indent; x++)
			pw1.print("\t");
		
		pw1.print("<"+name+">");
		pw1.print(val);
		pw1.println("</"+name+">");
	}

	public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, boolean val) {
		for (int x=0; x<indent; x++)
			pw1.print("\t");
		
		pw1.print("<"+name+">");
		pw1.print(val);
		pw1.println("</"+name+">");
	}

	public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, long val) {
		for (int x=0; x<indent; x++)
			pw1.print("\t");
		
		pw1.print("<"+name+">");
		pw1.print(val);
		pw1.println("</"+name+">");
	}

	public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, double val) {
		for (int x=0; x<indent; x++)
			pw1.print("\t");
		
		pw1.print("<"+name+">");
		pw1.print(val);
		pw1.println("</"+name+">");
	}

	public static String indentStr(int level) {
		String retVal = "";
	
		for (int x=0; x<level; x++)
			retVal += "\t";
		
		return retVal;
	}

	public static String xmlToString(Node node) throws TransformerException {
        Source source = new DOMSource(node);
        StringWriter stringWriter = new StringWriter();
        Result result = new StreamResult(stringWriter);
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.transform(source, result);

        return stringWriter.getBuffer().toString();
    }

	/**
	 * Contents copied from megamek.common.EntityListFile.saveTo(...) Modified
	 * to support saving to/from XML for our purposes in MekHQ TODO: Some of
	 * this may want to be back-ported into entity itself in MM and then
	 * re-factored out of EntityListFile.
	 * 
	 * @param tgtEnt
	 *            The entity to serialize to XML.
	 * @return A string containing the XML representation of the entity.
	 */
	public static String writeEntityToXmlString(Entity tgtEnt, int indentLvl, ArrayList<Entity> list) {
		// Holdover from EntityListFile in MM.
		// I guess they simply ignored all squadrons for writing out entities?
		if (tgtEnt instanceof FighterSquadron) {
			return "";
		}

		String retVal = "";

		// Start writing this entity to the file.
		retVal += MekHqXmlUtil.indentStr(indentLvl) + "<entity chassis=\""
				+ escape(tgtEnt.getChassis())
				+ "\" model=\"" + escape(tgtEnt.getModel())
				+ "\" type=\"" + escape(tgtEnt.getMovementModeAsString())
				+ "\" commander=\"" + String.valueOf(tgtEnt.isCommander());

		retVal += "\" externalId=\"";
		retVal += tgtEnt.getExternalIdAsString();

		if (tgtEnt.countQuirks() > 0) {
			retVal += "\" quirks=\"";
			retVal += String.valueOf(escape(tgtEnt.getQuirkList("::")));
		}
		if (tgtEnt.getC3Master() != null) {
			retVal += "\" c3MasterIs=\"";
			retVal += tgtEnt.getGame()
				.getEntity(tgtEnt.getC3Master().getId())
				.getC3UUIDAsString();
		}
		if (tgtEnt.hasC3() || tgtEnt.hasC3i()) {
			retVal += "\" c3UUID=\"";
			retVal += tgtEnt.getC3UUIDAsString();
		}

         if (null != tgtEnt.getCamoCategory()) {
             retVal += "\" camoCategory=\"";
             retVal += tgtEnt.getCamoCategory();
         }

         if (null != tgtEnt.getCamoFileName()) {
             retVal += "\" camoFileName=\"";
             retVal += tgtEnt.getCamoFileName();
         }

		retVal += "\">\n";

		// Add the crew this entity.
		// Except that crew is handled as a Person object in MekHq...
		// So lets not.
		/*
		 * final Pilot crew = tgtEnt.getCrew(); retVal +=
		 * "      <pilot name=\""; retVal += crew.getName().replaceAll("\"",
		 * "&quot;"); retVal += "\" nick=\""; retVal +=
		 * crew.getNickname().replaceAll("\"", "&quot;"); retVal +=
		 * "\" gunnery=\""; retVal += String.valueOf(crew.getGunnery());
		 * 
		 * if (null != tgtEnt.getGame() &&
		 * tgtEnt.getGame().getOptions().booleanOption("rpg_gunnery")) { retVal
		 * += "\" gunneryL=\""; retVal += String.valueOf(crew.getGunneryL());
		 * retVal += "\" gunneryM=\""; retVal +=
		 * String.valueOf(crew.getGunneryM()); retVal += "\" gunneryB=\"";
		 * retVal += String.valueOf(crew.getGunneryB()); }
		 * 
		 * retVal += "\" piloting=\""; retVal +=
		 * String.valueOf(crew.getPiloting());
		 * 
		 * if (null != tgtEnt.getGame() && tgtEnt.getGame().getOptions()
		 * .booleanOption("artillery_skill")) { retVal += "\" artillery=\"";
		 * retVal += String.valueOf(crew.getArtillery()); }
		 * 
		 * if (crew.getToughness() != 0) { retVal += "\" toughness=\""; retVal
		 * += String.valueOf(crew.getToughness()); }
		 * 
		 * if (crew.getInitBonus() != 0) { retVal += "\" initB=\""; retVal +=
		 * String.valueOf(crew.getInitBonus()); }
		 * 
		 * if (crew.getCommandBonus() != 0) { retVal += "\" commandB=\""; retVal
		 * += String.valueOf(crew.getCommandBonus()); }
		 * 
		 * if (crew.isDead() || crew.getHits() > 5) { retVal +=
		 * "\" hits=\"Dead"; } else if (crew.getHits() > 0) { retVal +=
		 * "\" hits=\""; retVal += String.valueOf(crew.getHits()); }
		 * 
		 * retVal += "\" ejected=\""; retVal +=
		 * String.valueOf(crew.isEjected());
		 * 
		 * if (crew.countOptions(PilotOptions.LVL3_ADVANTAGES) > 0) { retVal +=
		 * "\" advantages=\""; retVal += String.valueOf(crew.getOptionList("::",
		 * PilotOptions.LVL3_ADVANTAGES)); }
		 * 
		 * if (crew.countOptions(PilotOptions.MD_ADVANTAGES) > 0) { retVal +=
		 * "\" implants=\""; retVal += String.valueOf(crew.getOptionList("::",
		 * PilotOptions.MD_ADVANTAGES)); }
		 * 
		 * if (tgtEnt instanceof Mech) { if (((Mech) tgtEnt).isAutoEject()) {
		 * retVal += "\" autoeject=\"true"; } else { retVal +=
		 * "\" autoeject=\"false"; } }
		 * 
		 * if (!Pilot.ROOT_PORTRAIT.equals(crew.getPortraitCategory())) { retVal
		 * += "\" portraitCat=\""; retVal += crew.getPortraitCategory(); }
		 * 
		 * if (!Pilot.PORTRAIT_NONE.equals(crew.getPortraitFileName())) { retVal
		 * += "\" portraitFile=\""; retVal += crew.getPortraitFileName(); }
		 * 
		 * if (crew.getExternalId() != Entity.NONE) { retVal +=
		 * "\" externalId=\""; retVal += String.valueOf(crew.getExternalId()); }
		 * 
		 * retVal += "\"/>"; retVal += CommonConstants.NL;
		 */

		// If it's a tank, add a movement tag.
		// Since tank movement can be affected by damage other than equipment
		// damage...
		// And thus can't necessarily be calculated.
		if (tgtEnt instanceof Tank) {
			Tank tentity = (Tank) tgtEnt;
			retVal += getMovementString(tentity, indentLvl+1);

			if (tentity.isTurretLocked(Tank.LOC_TURRET)) {
				retVal += getTurretLockedString(tentity, indentLvl+1);
			}

			// Crits
			retVal += getTankCritString(tentity, indentLvl+1);
		}

		// add a bunch of stuff for aeros
		if (tgtEnt instanceof Aero) {
			Aero a = (Aero) tgtEnt;

			// SI
			retVal += MekHqXmlUtil.indentStr(indentLvl+1) + "<structural integrity=\""
					+ String.valueOf(a.getSI()) + "\"/>\n";

			// Heat sinks
			retVal += MekHqXmlUtil.indentStr(indentLvl+1) + "<heat sinks=\"" + String.valueOf(a.getHeatSinks())
					+ "\"/>\n";

			// Fuel
			retVal += MekHqXmlUtil.indentStr(indentLvl+1) + "<fuel left=\"" + String.valueOf(a.getFuel())
					+ "\"/>\n";

            int[] bombChoices = a.getBombChoices();
            if (bombChoices.length > 0) {
                retVal += MekHqXmlUtil.indentStr(indentLvl+1) + "<bombs>\n";
                for (int type = 0; type < BombType.B_NUM; type++) {
                    if (bombChoices[type] > 0) {
                        retVal += MekHqXmlUtil.indentStr(indentLvl+2) + "<bomb type=\"";
                        retVal += String.valueOf(type);
                        retVal += "\" load=\"";
                        retVal += String.valueOf(bombChoices[type]);
                        retVal += "\"/>\n";
                    }
                }
                retVal += MekHqXmlUtil.indentStr(indentLvl+1) + "</bombs>\n";
            }

			// TODO: dropship docking collars, bays

			// Large craft stuff
			if (a instanceof Jumpship) {
				Jumpship j = (Jumpship) a;

				// KF integrity
				retVal += MekHqXmlUtil.indentStr(indentLvl+1) + "<KF integrity=\""
						+ String.valueOf(j.getKFIntegrity()) + "\"/>\n";

				// KF sail integrity
				retVal += MekHqXmlUtil.indentStr(indentLvl+1) + "<sail integrity=\""
						+ String.valueOf(j.getSailIntegrity()) + "\"/>\n";
			}

			// Crits
			retVal += getAeroCritString(a, indentLvl+1);
		}

		// Add the locations of this entity (if any are needed).
		String loc = getLocString(tgtEnt, indentLvl+1);
		
		if (null != loc) {
			retVal += loc;
		}

		// Write the C3i Data if needed
		if (tgtEnt.hasC3i()) {
			retVal += MekHqXmlUtil.indentStr(indentLvl+1) + "<c3iset>";
			retVal += CommonConstants.NL;
			Iterator<Entity> c3iList = list.iterator();
			while (c3iList.hasNext()) {
				final Entity C3iEntity = c3iList.next();

				if (C3iEntity.onSameC3NetworkAs(tgtEnt, true)) {
					retVal += MekHqXmlUtil.indentStr(indentLvl+2) + "<c3i_link link=\"";
					retVal += C3iEntity.getC3UUIDAsString();
					retVal += "\"/>";
					retVal += CommonConstants.NL;
				}
			}
			retVal += MekHqXmlUtil.indentStr(indentLvl+1) + "</c3iset>";
			retVal += CommonConstants.NL;
		}

		// Finish writing this entity to the file.
		retVal += MekHqXmlUtil.indentStr(indentLvl) + "</entity>";

		// Okay, return whatever we've got!
		return retVal;
	}

	/**
	 * Contents copied from megamek.common.EntityListFile.getAeroCritString(...)
	 * Modified to support saving to/from XML for our purposes in MekHQ
	 * 
	 * @param a
	 *            The Aero unit to generate a crit string for.
	 * @return The generated crit string.
	 */
	private static String getAeroCritString(Aero a, int indentLvl) {
		String retVal = MekHqXmlUtil.indentStr(indentLvl) + "<acriticals";
		String critVal = "";

		// crits
		if (a.getAvionicsHits() > 0) {
			critVal = critVal.concat(" avionics=\"");
			critVal = critVal.concat(Integer.toString(a.getAvionicsHits()));
			critVal = critVal.concat("\"");
		}
		
		if (a.getSensorHits() > 0) {
			critVal = critVal.concat(" sensors=\"");
			critVal = critVal.concat(Integer.toString(a.getSensorHits()));
			critVal = critVal.concat("\"");
		}
		
		if (a.getEngineHits() > 0) {
			critVal = critVal.concat(" engine=\"");
			critVal = critVal.concat(Integer.toString(a.getEngineHits()));
			critVal = critVal.concat("\"");
		}
		
		if (a.getFCSHits() > 0) {
			critVal = critVal.concat(" fcs=\"");
			critVal = critVal.concat(Integer.toString(a.getFCSHits()));
			critVal = critVal.concat("\"");
		}
		
		if (a.getCICHits() > 0) {
			critVal = critVal.concat(" cic=\"");
			critVal = critVal.concat(Integer.toString(a.getCICHits()));
			critVal = critVal.concat("\"");
		}
		
		if (a.getLeftThrustHits() > 0) {
			critVal = critVal.concat(" leftThrust=\"");
			critVal = critVal.concat(Integer.toString(a.getLeftThrustHits()));
			critVal = critVal.concat("\"");
		}
		
		if (a.getRightThrustHits() > 0) {
			critVal = critVal.concat(" rightThrust=\"");
			critVal = critVal.concat(Integer.toString(a.getRightThrustHits()));
			critVal = critVal.concat("\"");
		}
		
		if (!a.hasLifeSupport()) {
			critVal = critVal.concat(" lifeSupport=\"none\"");
		}
		
		if (a.isGearHit()) {
			critVal = critVal.concat(" gear=\"none\"");
		}

		if (!critVal.equals("")) {
			// then add beginning and end
			retVal = retVal.concat(critVal);
			retVal = retVal.concat("/>\n");
		} else {
			return critVal;
		}

		return retVal;
	}

	/**
	 * Contents copied from
	 * megamek.common.EntityListFile.getTurretLockedString(...) Modified to
	 * support saving to/from XML for our purposes in MekHQ
	 * 
	 * @param e
	 *            The tank to generate a turret-locked string for.
	 * @return The generated string.
	 */
	private static String getTurretLockedString(Tank e, int indentLvl) {
		String retval = MekHqXmlUtil.indentStr(indentLvl) + "<turretlock direction=\"";
		retval = retval.concat(Integer.toString(e.getSecondaryFacing()));
		retval = retval.concat("\"/>\n");

		return retval;
	}

	/**
	 * Contents copied from megamek.common.EntityListFile.getMovementString(...)
	 * Modified to support saving to/from XML for our purposes in MekHQ
	 * 
	 * @param e
	 *            The tank to generate a movement string for.
	 * @return The generated string.
	 */
	private static String getMovementString(Tank e, int indentLvl) {
		String retVal = MekHqXmlUtil.indentStr(indentLvl) + "<movement speed=\"";
		boolean im = false;
		
		// This can throw an NPE for no obvious reason.
		// Okay, fine.  If the tank doesn't even *have* an object related to this...
		// Lets assume it's fully mobile, as any other fact hasn't been recorded.
		try {
			 im = e.isImmobile();
		} catch (NullPointerException ex) {
			// Ignore - just don't completely fail out.
		}

		if (im) {
			retVal = retVal.concat("immobile");
		} else {
			retVal = retVal.concat(Integer.toString(e.getOriginalWalkMP()));
		}
		
		retVal = retVal.concat("\"/>\n");

		// save any motive hits
		retVal = retVal.concat(MekHqXmlUtil.indentStr(indentLvl) + "<motive damage=\"");
		retVal = retVal.concat(Integer.toString(e.getMotiveDamage()));
		retVal = retVal.concat("\" penalty=\"");
		retVal = retVal.concat(Integer.toString(e.getMotivePenalty()));
		retVal = retVal.concat("\"/>\n");

		return retVal;
	}
	
	/**
	 * Contents copied from megamek.common.EntityListFile.getTankCritString(...)
	 * Modified to support saving to/from XML for our purposes in MekHQ
	 * 
	 * @param e
	 *            The tank to generate a movement string for.
	 * @return The generated string.
	 */
	 private static String getTankCritString(Tank e, int indentLvl) {

	     String retVal = MekHqXmlUtil.indentStr(indentLvl) + "<tcriticals";
	     String critVal = "";

	     // crits
	     if (e.getSensorHits() > 0) {
		 critVal = critVal.concat(" sensors=\"");
		 critVal = critVal.concat(Integer.toString(e.getSensorHits()));
		 critVal = critVal.concat("\"");
	     }
	     if (e.isEngineHit()) {
		 critVal = critVal.concat(" engine=\"");
		 critVal = critVal.concat("hit");
		 critVal = critVal.concat("\"");
	     }

	     /* crew are handled as a Person object in MekHq...
	     if (e.isDriverHit()) {
		 critVal = critVal.concat(" driver=\"");
		 critVal = critVal.concat("hit");
		 critVal = critVal.concat("\"");
	     }

	     if (e.isCommanderHit()) {
		 critVal = critVal.concat(" commander=\"");
		 critVal = critVal.concat("hit");
		 critVal = critVal.concat("\"");
	     }
	     */

	     if (!critVal.equals("")) {
		 // then add beginning and end
		 retVal = retVal.concat(critVal);
		 retVal = retVal.concat("/>\n");
	     } else {
		 return critVal;
	     }

	     return retVal;

	}


	/**
	 * Contents copied from megamek.common.EntityListFile.getMovementString(...)
	 * Modified to support saving to/from XML for our purposes in MekHQ
	 * 
	 * @param entity
	 *            The entity to generate a location string for.
	 * @return The generated string.
	 */
	private static String getLocString(Entity entity, int indentLvl) {
		boolean isMech = entity instanceof Mech;
        boolean haveSlot = false;
		StringBuffer output = new StringBuffer();
		StringBuffer thisLoc = new StringBuffer();
		boolean isDestroyed = false;

		// Walk through the locations for the entity,
		// and only record damage and ammo.
		for (int loc = 0; loc < entity.locations(); loc++) {
			
			//if the location is blown off, remove it so we can get the real values
        	boolean blownOff = entity.isLocationBlownOff(loc);
        	
			// Record destroyed locations.
			if (!(entity instanceof Aero)
					&& !(entity instanceof megamek.common.Infantry && !(entity instanceof megamek.common.BattleArmor))
					&& entity.getOInternal(loc) != IArmorState.ARMOR_NA
					&& entity.getInternalForReal(loc) <= 0) {
				isDestroyed = true;
			}

			// Record damage to armor and internal structure.
			// Destroyed locations have lost all their armor and IS.
			//if (!isDestroyed) {
				if (entity.getOArmor(loc) != entity.getArmorForReal(loc)) {
					thisLoc.append(MekHqXmlUtil.indentStr(indentLvl+1) + "<armor points=\"");
					thisLoc.append(formatArmor(entity.getArmorForReal(loc)));
					thisLoc.append("\"/>\n");
				}

				if (entity.getOInternal(loc) != entity.getInternalForReal(loc)) {
					thisLoc.append(MekHqXmlUtil.indentStr(indentLvl+1) + "<armor points=\"");
					thisLoc.append(formatArmor(entity.getInternalForReal(loc)));
					thisLoc.append("\" type=\"Internal\"/>\n");
				}

				if (entity.hasRearArmor(loc)
						&& entity.getOArmor(loc, true) != entity.getArmorForReal(loc,
								true)) {
					thisLoc.append(MekHqXmlUtil.indentStr(indentLvl+1) + "<armor points=\"");
					thisLoc.append(formatArmor(entity.getArmorForReal(loc, true)));
					thisLoc.append("\" type=\"Rear\"/>\n");
				}
				if(entity.getLocationStatus(loc) == ILocationExposureStatus.BREACHED) {
                    thisLoc.append(MekHqXmlUtil.indentStr(indentLvl+1) + "<breached/>\n");
                }
                if(blownOff) {
                	thisLoc.append(MekHqXmlUtil.indentStr(indentLvl+1) + "<blownOff/>\n");
                }
			//}

			// Walk through the slots in this location.
			for (int loop = 0; loop < entity.getNumberOfCriticals(loc); loop++) {
				// Get this slot.
				CriticalSlot slot = entity.getCritical(loc, loop);

				// Did we get a slot?
				if (null == slot) {
					// Nope. Record missing actuators on Biped Mechs.
					if (isMech && !entity.entityIsQuad()
							&& (loc == Mech.LOC_RARM || loc == Mech.LOC_LARM)
							&& (loop == 2 || loop == 3)) {
						thisLoc.append(MekHqXmlUtil.indentStr(indentLvl+1) + "<slot index=\"");
						thisLoc.append(String.valueOf(loop + 1));
						thisLoc.append("\" type=\"Empty\"/>\n");
						haveSlot = true;
					}
				} else {
					// Yup. If the equipment isn't a system, get it.
					Mounted mount = null;
					
					if (CriticalSlot.TYPE_EQUIPMENT == slot.getType()) {
						mount = entity.getEquipment(slot.getIndex());
					}

					// Destroyed locations on Mechs that contain slots
					// that are missing but not hit or destroyed must
					// have been blown off.
					if (!isDestroyed && isMech && slot.isMissing()
							&& !slot.isHit() && !slot.isDestroyed()) {
						thisLoc.append(formatSlot(String.valueOf(loop + 1),
								mount, slot.isHit(), slot.isDestroyed(),
								slot.isRepairable(), slot.isMissing(), indentLvl+1));
						haveSlot = true;
					} else if (slot.isDamaged()) { // Record damaged slots.
						thisLoc.append(formatSlot(String.valueOf(loop + 1),
								mount, slot.isHit(), slot.isDestroyed(),
								slot.isRepairable(), slot.isMissing(), indentLvl+1));
						haveSlot = true;
					} else if (null != mount && mount.countQuirks() > 0) { // record any quirks
						thisLoc.append(formatSlot(String.valueOf(loop + 1),
								mount, slot.isHit(), slot.isDestroyed(),
								slot.isRepairable(), slot.isMissing(), indentLvl+1));
						haveSlot = true;
					} else if (mount != null
							&& mount.getType() instanceof AmmoType) {
						// Record ammunition slots in undestroyed locations.
						// N.B. the slot CAN\"T be damaged at this point.
						thisLoc.append(MekHqXmlUtil.indentStr(indentLvl+1) + "<slot index=\"");
						thisLoc.append(String.valueOf(loop + 1));
						thisLoc.append("\" type=\"");
						thisLoc.append(mount.getType().getInternalName());
						thisLoc.append("\" shots=\"");
						thisLoc.append(String.valueOf(mount.getBaseShotsLeft()));
						thisLoc.append("\"/>\n");
						haveSlot = true;
					} else if (mount != null
							&& mount.getType() instanceof WeaponType
							&& (mount.getType()).hasFlag(WeaponType.F_ONESHOT)) {
						// Record the munition type of oneshot launchers
						thisLoc.append(formatSlot(String.valueOf(loop + 1),
								mount, slot.isHit(), slot.isDestroyed(),
								slot.isRepairable(), slot.isMissing(), indentLvl+1));
						haveSlot = true;
					}
				} // End have-slot
			} // Check the next slot in this location

			// Tanks don't have slots, and Protomechs only have
			// system slots, so we have to handle the ammo specially.
			if (entity instanceof Tank || entity instanceof Protomech) {
				for (Mounted mount : entity.getAmmo()) {
					// Is this ammo in the current location?
					if (mount.getLocation() == loc) {
						thisLoc.append(formatSlot("N/A", mount, false, false,
								false, false, indentLvl+1));
						haveSlot = true;
					}

				} // Check the next ammo.
				// TODO: Handle slotless equipment.
				// TODO: Handle tank crits.
			} // End is-tank-or-proto

			// Did we record information for this location?
			if (thisLoc.length() > 0) {
				// Add this location to the output string.
				output.append(MekHqXmlUtil.indentStr(indentLvl) + "<location index=\"");
				output.append(String.valueOf(loc));		
				output.append("\"> ");
				output.append(entity.getLocationName(loc));
				
				if (blownOff) {
					output.append(" has been blown off.");
				}
				output.append("\n");
				output.append(thisLoc.toString());
				output.append(MekHqXmlUtil.indentStr(indentLvl) + "</location>\n");

				// Reset the location buffer.
				thisLoc = new StringBuffer();
				blownOff = false;
			} 
			// Reset the "location is destroyed" flag.
			isDestroyed = false;
		} // Handle the next location

		// If there is no location string, return a null.
		if (output.length() == 0) {
			return null;
		}

		// Convert the output into a String and return it.
		return output.toString();
	} // End private static String getLocString( Entity )

	/**
	 * Contents copied from megamek.common.EntityListFile.getMovementString(...)
	 * Modified to support saving to/from XML for our purposes in MekHQ
	 * 
	 * Produce a string describing this armor value. Valid output values are any
	 * integer from 0 to 100, N/A, or Destroyed.
	 * 
	 * @param points
	 *            - the <code>int</code> value of the armor. This value may be
	 *            any valid value of entity armor (including NA, DOOMED, and
	 *            DESTROYED).
	 * @return a <code>String</code> that matches the armor value.
	 */
	private static String formatArmor(int points) {
		// Is the armor destroyed or doomed?
		if (points == IArmorState.ARMOR_DOOMED
				|| points == IArmorState.ARMOR_DESTROYED) {
			return "Destroyed";
		}

		// Was there armor to begin with?
		if (points == IArmorState.ARMOR_NA) {
			return "N/A";
		}

		// Translate the int to a String.
		return String.valueOf(points);
	}

	/**
	 * Contents copied from megamek.common.EntityListFile.getMovementString(...)
	 * Modified to support saving to/from XML for our purposes in MekHQ
	 * 
	 * Produce a string describing the equipment in a critical slot.
	 * 
	 * @param index
	 *            - the <code>String</code> index of the slot. This value should
	 *            be a positive integer or "N/A".
	 * @param mount
	 *            - the <code>Mounted</code> object of the equipment. This value
	 *            should be <code>null</code> for a slot with system equipment.
	 * @param isHit
	 *            - a <code>boolean</code> that identifies this slot as having
	 *            taken a hit.
	 * @param isDestroyed
	 *            - a <code>boolean</code> that identifies the equipment as
	 *            having been destroyed. Note that a single slot in a multi-slot
	 *            piece of equipment can be destroyed but not hit; it is still
	 *            available to absorb additional critical hits.
	 * @return a <code>String</code> describing the slot.
	 */
	private static String formatSlot(String index, Mounted mount,
			boolean isHit, boolean isDestroyed, boolean isRepairable, boolean isMissing, 
			int indentLvl) {
		StringBuffer output = new StringBuffer();

		output.append(MekHqXmlUtil.indentStr(indentLvl) + "<slot index=\"");
		output.append(index);
		output.append("\" type=\"");
		
		if (mount == null) {
			output.append("System");
		} else {
			output.append(mount.getType().getInternalName());
			
			if (mount.isRearMounted()) {
				output.append("\" isRear=\"true");
			}
			
			if (mount.getType() instanceof AmmoType) {
				output.append("\" shots=\"");
				output.append(String.valueOf(mount.getBaseShotsLeft()));
			}
			
			if (mount.getType() instanceof WeaponType
					&& (mount.getType()).hasFlag(WeaponType.F_ONESHOT)) {
				output.append("\" munition=\"");
				output.append(mount.getLinked().getType().getInternalName());
			}
			
			if (mount.countQuirks() > 0) {
				output.append("\" quirks=\"");
				output.append(String.valueOf(mount.getQuirkList("::")));
			}
		}
		
		if (isHit) {
			output.append("\" isHit=\"");
			output.append(String.valueOf(isHit));
		}
		
		if (!isRepairable && (isHit || isDestroyed)) {
			output.append("\" isRepairable=\"");
			output.append(String.valueOf(isRepairable));
		}
		if (isMissing) {
            output.append("\" isMissing=\"");
            output.append(String.valueOf(isMissing));
        }
		output.append("\" isDestroyed=\"");
		output.append(String.valueOf(isDestroyed));
		output.append("\"/>\n");

		// Return a String.
		return output.toString();
	}

	public static Entity getEntityFromXmlString(Node xml)
			throws UnsupportedEncodingException, ParseException,
			TransformerException {
		MekHQ.logMessage("Executing getEntityFromXmlString(Node)...", 4);

		return getEntityFromXmlString(MekHqXmlUtil.xmlToString(xml));
	}

	public static Entity getEntityFromXmlString(String xml)
			throws UnsupportedEncodingException, ParseException {
		MekHQ.logMessage("Executing getEntityFromXmlString(String)...", 4);

		Entity retVal = null;

		XMLStreamParser prs = new XMLStreamParser(new ByteArrayInputStream(
				xml.getBytes("UTF-8")));
		Vector<Entity> ents = prs.getEntities();

		if (ents.size() > 1)
			throw new IllegalArgumentException(
					"More than one entity contained in XML string!  Expecting a single entity.");
		else if (ents.size() != 0)
			retVal = ents.get(0);
		
		MekHQ.logMessage("Returning "+retVal+" from getEntityFromXmlString(String)...", 4);

		return retVal;
	}

    /** Escaping code for XML borrowed from org.json.XML
      * Full license and code available https://github.com/douglascrockford/JSON-java/blob/master/XML.java
      * @param string The string to be encoded
      * @return An encoded copy of the string
     **/
    public static String escape(String string) {
        if (StringUtil.isNullOrEmpty(string)) {
            return string;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0, length = string.length(); i < length; i++) {
            char c = string.charAt(i);
            switch (c) {
            case '&':
                sb.append("&amp;");
                break;
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            case '"':
                sb.append("&quot;");
                break;
            case '\'':
                sb.append("&apos;");
                break;
            default:
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Unescape...well, it reverses escaping...
    **/
    public static String unEscape(String string) {
      return string.replaceAll( "&amp;", "&" ).replaceAll( "&lt;", "<" ).replaceAll( "&gt;", ">" ).replaceAll( "&quot;", "\"" ).replaceAll( "&apos", "\'" );
    }
    
    public static String getEntityNameFromXmlString(Node node) {
    	NamedNodeMap attrs = node.getAttributes();
		String chassis = attrs.getNamedItem("chassis").getTextContent();
		String model = attrs.getNamedItem("model").getTextContent();
		return chassis + " " + model;
    }
}
