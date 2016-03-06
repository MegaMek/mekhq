package mekhq;

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
import megamek.common.BombType;
import megamek.common.CommonConstants;
import megamek.common.Entity;
import megamek.common.EntityListFile;
import megamek.common.FighterSquadron;
import megamek.common.IPlayer;
import megamek.common.Jumpship;
import megamek.common.MULParser;
import megamek.common.Tank;
import megamek.common.util.StringUtil;

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

         if (null != tgtEnt.getCamoCategory()
        		 && tgtEnt.getCamoCategory() != IPlayer.NO_CAMO
        		 && !tgtEnt.getCamoCategory().isEmpty()) {
             retVal += "\" camoCategory=\"";
             retVal += String.valueOf(escape(tgtEnt.getCamoCategory()));
         }

         if (null != tgtEnt.getCamoFileName()
        		 && tgtEnt.getCamoFileName() != IPlayer.NO_CAMO
        		 && !tgtEnt.getCamoFileName().isEmpty()) {
             retVal += "\" camoFileName=\"";
             retVal += String.valueOf(escape(tgtEnt.getCamoFileName()));
         }

		retVal += "\">\n";

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
                    	String typeName = BombType.getBombInternalName(type);
                        retVal += MekHqXmlUtil.indentStr(indentLvl+2) + "<bomb type=\"";
                        retVal += typeName;
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
		String loc = EntityListFile.getLocString(tgtEnt, indentLvl+1);

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

	public static Entity getEntityFromXmlString(Node xml)
			throws UnsupportedEncodingException, TransformerException {
		MekHQ.logMessage("Executing getEntityFromXmlString(Node)...", 4);

		return getEntityFromXmlString(MekHqXmlUtil.xmlToString(xml));
	}

	public static Entity getEntityFromXmlString(String xml)
			throws UnsupportedEncodingException {
		MekHQ.logMessage("Executing getEntityFromXmlString(String)...", 4);

		Entity retVal = null;

		MULParser prs = new MULParser(new ByteArrayInputStream(xml.getBytes("UTF-8")));
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
