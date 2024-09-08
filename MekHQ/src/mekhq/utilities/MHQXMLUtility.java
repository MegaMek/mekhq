/*
 * Copyright (c) 2013-2022 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.utilities;

import megamek.codeUtilities.StringUtility;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.utilities.xml.MMXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

public class MHQXMLUtility extends MMXMLUtility {
    private static DocumentBuilderFactory UNSAFE_DOCUMENT_BUILDER_FACTORY;

    /**
     * USE WITH CARE. Creates a DocumentBuilder safe from XML external entities attacks, but unsafe from
     * XML entity expansion attacks.
     *
     * @return A DocumentBuilder less safe to use to read untrusted XML.
     */
    public static DocumentBuilder newUnsafeDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = UNSAFE_DOCUMENT_BUILDER_FACTORY;
        if (null == dbf) {
            // At worst we may do this twice if multiple threads
            // hit this method. It is Ok to have more than one
            // instance of the builder factory, as long as it is
            // XXE safe.

            //
            // For further background, see newSafeDocumentBuilder()
            //
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);

            //
            // "If you can't completely disable DTDs, then at least do the
            // following:"
            //

            // Disable external entities
            String FEATURE = "http://xml.org/sax/features/external-general-entities";
            dbf.setFeature(FEATURE, false);

            // Disable external parameters
            FEATURE = "http://xml.org/sax/features/external-parameter-entities";
            dbf.setFeature(FEATURE, false);

            // Disable external DTDs as well
            FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
            dbf.setFeature(FEATURE, false);

            UNSAFE_DOCUMENT_BUILDER_FACTORY = dbf;
        }

        return dbf.newDocumentBuilder();
    }

    /**
     * TODO : This is dumb and we should just use EntityListFile.writeEntityList.
     * TODO : Some of this may want to be back-ported into entity itself in MM and then
     * TODO : re-factored out of EntityListFile.
     *
     * Contents copied from megamek.common.EntityListFile.saveTo(...) Modified
     * to support saving to/from XML for our purposes in MekHQ
     *
     * @param tgtEnt The entity to serialize to XML.
     * @return A string containing the XML representation of the entity.
     */
    public static String writeEntityToXmlString(Entity tgtEnt, int indentLvl, List<Entity> list) {
        // Holdover from EntityListFile in MM.
        // I guess they simply ignored all squadrons for writing out entities?
        if (tgtEnt instanceof FighterSquadron) {
            return "";
        }

        StringBuilder retVal = new StringBuilder();

        // Start writing this entity to the file.
        retVal.append(MHQXMLUtility.indentStr(indentLvl)).append("<" + MULParser.ELE_ENTITY + " " + MULParser.ATTR_CHASSIS + "=\"")
                .append(escape(tgtEnt.getFullChassis())).append("\" " + MULParser.ATTR_MODEL + "=\"").append(escape(tgtEnt.getModel()))
                .append("\" " + MULParser.ATTR_TYPE + "=\"").append(escape(tgtEnt.getMovementModeAsString())).append("\" " + MULParser.ATTR_COMMANDER + "=\"")
                .append(tgtEnt.isCommander()).append("\" " + MULParser.ATTR_EXT_ID + "=\"").append(tgtEnt.getExternalIdAsString());

        if (tgtEnt.countQuirks() > 0) {
            retVal.append("\" " + MULParser.ATTR_QUIRKS + "=\"").append(escape(tgtEnt.getQuirkList("::")));
        }
        if (tgtEnt.getC3Master() != null) {
            retVal.append("\" " + MULParser.ATTR_C3MASTERIS + "=\"")
                    .append(tgtEnt.getGame().getEntity(tgtEnt.getC3Master().getId()).getC3UUIDAsString());
        }
        if (tgtEnt.hasC3() || tgtEnt.hasC3i() || tgtEnt.hasNavalC3()) {
            retVal.append("\" " + MULParser.ATTR_C3UUID + "=\"").append(tgtEnt.getC3UUIDAsString());
        }

        if (!tgtEnt.getCamouflage().hasDefaultCategory()) {
            retVal.append("\" " + MULParser.ATTR_CAMO_CATEGORY + "=\"").append(escape(tgtEnt.getCamouflage().getCategory()));
        }

        if (!tgtEnt.getCamouflage().hasDefaultFilename()) {
            retVal.append("\" " + MULParser.ATTR_CAMO_FILENAME + "=\"").append(escape(tgtEnt.getCamouflage().getFilename()));
        }

        if (tgtEnt.getDeployRound() > 0) {
            retVal.append(String.format("\" %s=\"%d", MULParser.ATTR_DEPLOYMENT, tgtEnt.getDeployRound()));
        }

        if (tgtEnt instanceof Infantry) {
            retVal.append(String.format("\" %s=\"%d", MULParser.ATTR_INF_SQUAD_NUM, ((Infantry) tgtEnt).getSquadCount()));
        }

        retVal.append(String.format("\" %s=\"%d", MULParser.ATTR_ALTITUDE, tgtEnt.getAltitude()));

        if (tgtEnt.isOffBoard()) {
            retVal.append("\" " + MULParser.ATTR_OFFBOARD + "=\"");
            retVal.append(String.valueOf(tgtEnt.isOffBoard()));
            retVal.append("\" " + MULParser.ATTR_OFFBOARD_DISTANCE + "=\"");
            retVal.append(String.valueOf(tgtEnt.getOffBoardDistance()));
            retVal.append("\" " + MULParser.ATTR_OFFBOARD_DIRECTION + "=\"");
            retVal.append(String.valueOf(tgtEnt.getOffBoardDirection().getValue()));
        }

        retVal.append("\">\n");

        // If it's a tank, add a movement tag.
        // Since tank movement can be affected by damage other than equipment
        // damage...
        // And thus can't necessarily be calculated.
        if (tgtEnt instanceof Tank) {
            Tank tentity = (Tank) tgtEnt;
            retVal.append(getMovementString(tentity, indentLvl + 1));

            if (tentity.isTurretLocked(Tank.LOC_TURRET)) {
                retVal.append(getTurretLockedString(tentity, indentLvl + 1));
            }

            // Crits
            retVal.append(getTankCritString(tentity, indentLvl + 1));
        }

        // add a bunch of stuff for aeros
        if (tgtEnt instanceof Aero) {
            Aero a = (Aero) tgtEnt;

            // SI
            retVal.append(MHQXMLUtility.indentStr(indentLvl + 1)).append("<" + MULParser.ELE_SI + " " + MULParser.ATTR_INTEGRITY + "=\"").append(a.getSI())
                    .append("\"/>\n");

            // Heat sinks
            retVal.append(MHQXMLUtility.indentStr(indentLvl + 1)).append("<" + MULParser.ELE_HEAT + " " + MULParser.ATTR_SINK + "=\"").append(a.getHeatSinks())
                    .append("\"/>\n");

            // Fuel
            retVal.append(MHQXMLUtility.indentStr(indentLvl + 1)).append("<" + MULParser.ELE_FUEL + " " + MULParser.ATTR_LEFT + "=\"").append(a.getFuel())
                    .append("\"/>\n");

            // TODO: dropship docking collars, bays

            // Large craft stuff
            if (a instanceof Jumpship) {
                Jumpship j = (Jumpship) a;

                // KF integrity
                retVal.append(MHQXMLUtility.indentStr(indentLvl + 1)).append("<" + MULParser.ELE_KF + " " + MULParser.ATTR_INTEGRITY + "=\"")
                        .append(j.getKFIntegrity()).append("\"/>\n");

                // KF sail integrity
                retVal.append(MHQXMLUtility.indentStr(indentLvl + 1)).append("<" + MULParser.ELE_SAIL + " " + MULParser.ATTR_INTEGRITY + "=\"")
                        .append(j.getSailIntegrity()).append("\"/>\n");
            }

            // Crits
            retVal.append(getAeroCritString(a, indentLvl + 1));
        }

        // If the entity carries bombs, write those out
        if (tgtEnt instanceof IBomber) {
            retVal.append(getBombChoiceString((IBomber) tgtEnt, indentLvl));
        }

        // Add the locations of this entity (if any are needed).
        String loc = EntityListFile.getLocString(tgtEnt, indentLvl + 1);

        if (null != loc) {
            retVal.append(loc);
        }

        // Write the Naval C3 Data if needed
        if (tgtEnt.hasNavalC3()) {
            retVal.append(MHQXMLUtility.indentStr(indentLvl + 1)).append("<" + MULParser.ELE_NC3 + ">\n");
            Iterator<Entity> nc3List = list.iterator();
            while (nc3List.hasNext()) {
                final Entity nc3Entity = nc3List.next();

                if (nc3Entity.onSameC3NetworkAs(tgtEnt, true)) {
                    retVal.append(MHQXMLUtility.indentStr(indentLvl + 2)).append("<" + MULParser.ELE_NC3LINK + " " + MULParser.ATTR_LINK + "=\"");
                    retVal.append(nc3Entity.getC3UUIDAsString());
                    retVal.append("\"/>\n");
                }
            }
            retVal.append(MHQXMLUtility.indentStr(indentLvl + 1)).append("</" + MULParser.ELE_NC3 + ">\n");
        }

        // Write the C3i Data if needed
        if (tgtEnt.hasC3i()) {
            retVal.append(MHQXMLUtility.indentStr(indentLvl + 1)).append("<" + MULParser.ELE_C3I + ">\n");

            Iterator<Entity> c3iList = list.iterator();
            while (c3iList.hasNext()) {
                final Entity C3iEntity = c3iList.next();

                if (C3iEntity.onSameC3NetworkAs(tgtEnt, true)) {
                    retVal.append(MHQXMLUtility.indentStr(indentLvl + 2))
                            .append("<" + MULParser.ELE_C3ILINK + " " + MULParser.ATTR_LINK + "=\"")
                            .append(C3iEntity.getC3UUIDAsString())
                            .append("\"/>\n");
                }
            }
            retVal.append(MHQXMLUtility.indentStr(indentLvl + 1)).append("</" + MULParser.ELE_C3I + ">\n");
        }

        // Finish writing this entity to the file.
        retVal.append(MHQXMLUtility.indentStr(indentLvl)).append("</" + MULParser.ELE_ENTITY + ">");

        // Okay, return whatever we've got!
        return retVal.toString();
    }

    private static void compileBombChoices(int[] bombChoices, StringBuilder retVal, int indentLvl, boolean isInternal) {
        if (bombChoices.length > 0) {
            retVal.append(MHQXMLUtility.indentStr(indentLvl + 1)).append("<" + MULParser.ELE_BOMBS + ">\n");
            for (int type = 0; type < BombType.B_NUM; type++) {
                if (bombChoices[type] > 0) {
                    String typeName = BombType.getBombInternalName(type);
                    retVal.append(MHQXMLUtility.indentStr(indentLvl + 2)).append("<" + MULParser.ELE_BOMB + " " + MULParser.ATTR_TYPE + "=\"");
                    retVal.append(typeName);
                    retVal.append("\" " + MULParser.ATTR_LOAD + "=\"");
                    retVal.append(bombChoices[type]);
                    retVal.append((isInternal) ? "\" " + MULParser.ATTR_INTERNAL + "=\"true" : "\" " + MULParser.ATTR_INTERNAL + "=\"false");
                    retVal.append("\"/>\n");
                }
            }
            retVal.append(MHQXMLUtility.indentStr(indentLvl + 1)).append("</" + MULParser.ELE_BOMBS + ">\n");
        }

    }
    private static String getBombChoiceString(IBomber bomber, int indentLvl) {
        StringBuilder retVal = new StringBuilder();

        int[] bombChoices = bomber.getIntBombChoices();
        compileBombChoices(bombChoices, retVal, indentLvl, true);
        bombChoices = bomber.getExtBombChoices();
        compileBombChoices(bombChoices, retVal, indentLvl, false);

        return retVal.toString();
    }

    /**
     * Contents copied from megamek.common.EntityListFile.getAeroCritString(...) Modified to support
     * saving to/from XML for our purposes in MekHQ
     *
     * @param a The Aero unit to generate a crit string for.
     * @return The generated crit string.
     */
    private static String getAeroCritString(Aero a, int indentLvl) {
        String retVal = MHQXMLUtility.indentStr(indentLvl) + "<" + MULParser.ELE_AEROCRIT + "";
        String critVal = "";

        // crits
        if (a.getAvionicsHits() > 0) {
            critVal = critVal.concat(" " + MULParser.ATTR_AVIONICS + "=\"");
            critVal = critVal.concat(Integer.toString(a.getAvionicsHits()));
            critVal = critVal.concat("\"");
        }

        if (a.getSensorHits() > 0) {
            critVal = critVal.concat(" " + MULParser.ATTR_SENSORS + "=\"");
            critVal = critVal.concat(Integer.toString(a.getSensorHits()));
            critVal = critVal.concat("\"");
        }

        if (a.getEngineHits() > 0) {
            critVal = critVal.concat(" " + MULParser.ATTR_ENGINE + "=\"");
            critVal = critVal.concat(Integer.toString(a.getEngineHits()));
            critVal = critVal.concat("\"");
        }

        if (a.getFCSHits() > 0) {
            critVal = critVal.concat(" " + MULParser.ATTR_FCS + "=\"");
            critVal = critVal.concat(Integer.toString(a.getFCSHits()));
            critVal = critVal.concat("\"");
        }

        if (a.getCICHits() > 0) {
            critVal = critVal.concat(" " + MULParser.ATTR_CIC + "=\"");
            critVal = critVal.concat(Integer.toString(a.getCICHits()));
            critVal = critVal.concat("\"");
        }

        if (a.getLeftThrustHits() > 0) {
            critVal = critVal.concat(" " + MULParser.ATTR_LEFT_THRUST + "=\"");
            critVal = critVal.concat(Integer.toString(a.getLeftThrustHits()));
            critVal = critVal.concat("\"");
        }

        if (a.getRightThrustHits() > 0) {
            critVal = critVal.concat(" " + MULParser.ATTR_RIGHT_THRUST + "=\"");
            critVal = critVal.concat(Integer.toString(a.getRightThrustHits()));
            critVal = critVal.concat("\"");
        }

        if (!a.hasLifeSupport()) {
            critVal = critVal.concat(" " + MULParser.ATTR_LIFE_SUPPORT + "=\"" + MULParser.VALUE_NONE + "\"");
        }

        if (a.isGearHit()) {
            critVal = critVal.concat(" " + MULParser.ATTR_GEAR + "=\"" + MULParser.VALUE_NONE + "\"");
        }

        if (!critVal.isBlank()) {
            // then add beginning and end
            retVal = retVal.concat(critVal);
            retVal = retVal.concat("/>\n");
        } else {
            return critVal;
        }

        return retVal;
    }

    /**
     * Contents copied from megamek.common.EntityListFile.getTurretLockedString(...) Modified to support
     * saving to/from XML for our purposes in MekHQ
     *
     * @param e The tank to generate a turret-locked string for.
     * @return The generated string.
     */
    private static String getTurretLockedString(Tank e, int indentLvl) {
        String retval = MHQXMLUtility.indentStr(indentLvl) + "<" + MULParser.ELE_TURRETLOCK + " " + MULParser.ATTR_DIRECTION + "=\"";
        retval = retval.concat(Integer.toString(e.getSecondaryFacing()));
        retval = retval.concat("\"/>\n");

        return retval;
    }

    /**
     * Contents copied from megamek.common.EntityListFile.getMovementString(...) Modified to support
     * saving to/from XML for our purposes in MekHQ
     *
     * @param e The tank to generate a movement string for.
     * @return The generated string.
     */
    private static String getMovementString(Tank e, int indentLvl) {
        String retVal = MHQXMLUtility.indentStr(indentLvl) + "<movement speed=\"";
        boolean im = false;

        // This can throw an NPE for no obvious reason.
        // Okay, fine. If the tank doesn't even *have* an object related to this...
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
        retVal = retVal.concat(MHQXMLUtility.indentStr(indentLvl) + "<" + MULParser.ELE_MOTIVE + " " + MULParser.ATTR_MDAMAGE + "=\"");
        retVal = retVal.concat(Integer.toString(e.getMotiveDamage()));
        retVal = retVal.concat("\" " + MULParser.ATTR_MPENALTY + "=\"");
        retVal = retVal.concat(Integer.toString(e.getMotivePenalty()));
        retVal = retVal.concat("\"/>\n");

        return retVal;
    }

    /**
     * Contents copied from megamek.common.EntityListFile.getTankCritString(...) Modified to support
     * saving to/from XML for our purposes in MekHQ
     *
     * @param e The tank to generate a movement string for.
     * @return The generated string.
     */
    private static String getTankCritString(Tank e, int indentLvl) {

        String retVal = MHQXMLUtility.indentStr(indentLvl) + "<" + MULParser.ELE_TANKCRIT + "";
        String critVal = "";

        // crits
        if (e.getSensorHits() > 0) {
            critVal = critVal.concat(" " + MULParser.ATTR_SENSORS + "=\"");
            critVal = critVal.concat(Integer.toString(e.getSensorHits()));
            critVal = critVal.concat("\"");
        }
        if (e.isEngineHit()) {
            critVal = critVal.concat(" " + MULParser.ATTR_ENGINE + "=\"");
            critVal = critVal.concat( MULParser.VALUE_HIT);
            critVal = critVal.concat("\"");
        }

        /*
         * crew are handled as a Person object in MekHq... if (e.isDriverHit()) { critVal =
         * critVal.concat(" driver=\""); critVal = critVal.concat("hit"); critVal = critVal.concat("\""); }
         *
         * if (e.isCommanderHit()) { critVal = critVal.concat(" commander=\""); critVal =
         * critVal.concat("hit"); critVal = critVal.concat("\""); }
         */

        if (!critVal.isBlank()) {
            // then add beginning and end
            retVal = retVal.concat(critVal);
            retVal = retVal.concat("/>\n");
        } else {
            return critVal;
        }

        return retVal;
    }

    /**
     * FIXME : I should have never been in MekHQ... move me to MegaMek
     * MHQXMLUtility.writeEntityToXmlString does not include the crew,
     * as crew is handled by the Person class in MekHQ. This utility
     * function will insert a pilot tag (and also a deployment attribute,
     * which is also not added by the MHQXMLUtility method).
     */
    public static void writeEntityWithCrewToXML(PrintWriter pw, int indentLvl, Entity tgtEnt,
                                                List<Entity> list) {
        String retVal = MHQXMLUtility.writeEntityToXmlString(tgtEnt, indentLvl, list);

        StringBuilder crew = new StringBuilder(MHQXMLUtility.indentStr(indentLvl + 1));
        crew.append("<" + MULParser.ELE_CREW + " " + MULParser.ATTR_CREWTYPE + "=\"").append(tgtEnt.getCrew().getCrewType().toString().toLowerCase())
                .append("\" " + MULParser.ATTR_SIZE + "=\"").append(tgtEnt.getCrew().getSize());
        if (tgtEnt.getCrew().getInitBonus() != 0) {
            crew.append("\" " + MULParser.ATTR_INITB + "=\"").append(tgtEnt.getCrew().getInitBonus());
        }
        if (tgtEnt.getCrew().getCommandBonus() != 0) {
            crew.append("\" " + MULParser.ATTR_COMMANDB + "=\"").append(tgtEnt.getCrew().getCommandBonus());
        }
        if (tgtEnt instanceof Mek) {
            crew.append("\" " + MULParser.ATTR_AUTOEJECT + "=\"").append(((Mek) tgtEnt).isAutoEject());
        }
        crew.append("\" " + MULParser.ATTR_EJECTED + "=\"").append(tgtEnt.getCrew().isEjected()).append("\">\n");

        for (int pos = 0; pos < tgtEnt.getCrew().getSlotCount(); pos++) {
            crew.append(MHQXMLUtility.indentStr(indentLvl + 2)).append("<" + MULParser.ELE_CREWMEMBER + " " + MULParser.ATTR_SLOT + "=\"")
                    .append(pos).append("\" " + MULParser.ATTR_NAME + "=\"").append(MHQXMLUtility.escape(tgtEnt.getCrew().getName(pos)))
                    .append("\" " + MULParser.ATTR_NICK + "=\"").append(MHQXMLUtility.escape(tgtEnt.getCrew().getNickname(pos)))
                    .append("\" " + MULParser.ATTR_GENDER + "=\"").append(tgtEnt.getCrew().getGender(pos).name())
                    .append("\" " + MULParser.ATTR_GUNNERY + "=\"").append(tgtEnt.getCrew().getGunnery(pos))
                    .append("\" " + MULParser.ATTR_PILOTING + "=\"").append(tgtEnt.getCrew().getPiloting(pos));

            if (tgtEnt.getCrew().getToughness(pos) != 0) {
                crew.append("\" " + MULParser.ATTR_TOUGH + "=\"").append(tgtEnt.getCrew().getToughness(pos));
            }

            if (tgtEnt.getCrew().getCrewFatigue(pos) != 0) {
                crew.append("\" " + MULParser.ATTR_FATIGUE + "=\"").append(tgtEnt.getCrew().getCrewFatigue(pos));
            }

            if (tgtEnt.getCrew().isDead(pos) || tgtEnt.getCrew().getHits(pos) >= Crew.DEATH) {
                crew.append("\" " + MULParser.ATTR_HITS + "=\"" + MULParser.VALUE_DEAD + "");
            } else if (tgtEnt.getCrew().getHits(pos) > 0) {
                crew.append("\" " + MULParser.ATTR_HITS + "=\"").append(tgtEnt.getCrew().getHits(pos));
            }

            crew.append("\" " + MULParser.ATTR_EXT_ID + "=\"").append(tgtEnt.getCrew().getExternalIdAsString(pos));

            String extraData = tgtEnt.getCrew().writeExtraDataToXMLLine(pos);
            if (!StringUtility.isNullOrBlank(extraData)) {
                crew.append(extraData);
            }

            crew.append("\"/>\n");
        }
        crew.append(MHQXMLUtility.indentStr(indentLvl + 1)).append("</" + MULParser.ELE_CREW + ">\n");

        pw.println(retVal.replaceFirst(">", ">\n" + crew + "\n"));
    }

    /**
     * Parses the given node as if it was a .mul file and returns the first entity it contains.
     *
     * In theme with {@link MULParser}, this method fails silently and returns {@code null} if the
     * input can't be parsed; if it can be parsed and contains more than one entity, an
     * {@linkplain IllegalArgumentException} is thrown.
     *
     * @param element the xml tag to parse
     * @param campaign the Campaign to parse using, which may be null to ignore the game and game
     *                 options
     * @return the first entity parsed from the given element, or {@code null} if anything is wrong
     * with the input
     * @throws IllegalArgumentException if the given element parses to multiple entities
     */
    public static @Nullable Entity parseSingleEntityMul(final Element element,
                                                        final @Nullable Campaign campaign)
            throws IllegalArgumentException {
        final List<Entity> entities = new MULParser(element, ((campaign == null) ? null : campaign.getGameOptions()))
                .getEntities();

        switch (entities.size()) {
            case 0:
                return null;
            case 1:
                final Entity entity = entities.get(0);
                if (campaign != null) {
                    entity.setGame(campaign.getGame());
                }
                LogManager.getLogger().trace("Returning " + entity + " from getEntityFromXmlString(String)...");
                return entity;
            default:
                throw new IllegalArgumentException(
                        "More than one entity contained in XML string! Expecting a single entity.");
        }
    }

    public static String getEntityNameFromXmlString(Node node) {
        NamedNodeMap attrs = node.getAttributes();
        String chassis = attrs.getNamedItem(MULParser.ATTR_CHASSIS ).getTextContent();
        String model = attrs.getNamedItem(MULParser.ATTR_MODEL).getTextContent();
        return chassis + " " + model;
    }

    //region Simple XML Tag
    /**
     * This writes a Money or a Money array to file
     * @param pw the PrintWriter to use
     * @param indent the indent to write at
     * @param name the name of the XML tag
     * @param values the Money or Money[] to write to XML
     */
    public static void writeSimpleXMLTag(final PrintWriter pw, final int indent, final String name,
                                         final Money... values) {
        if (values.length > 0) {
            final StringJoiner stringJoiner = new StringJoiner(",");
            for (final Money value : values) {
                if (value != null) {
                    stringJoiner.add(value.toXmlString());
                }
            }

            if (!stringJoiner.toString().isBlank()) {
                pw.println(indentStr(indent) + '<' + name + '>' + stringJoiner + "</" + name + '>');
            }
        }
    }
    //endregion Simple XML Tag
}
