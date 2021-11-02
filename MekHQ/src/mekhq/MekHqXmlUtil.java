/*
 * Copyright (c) 2013-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import megamek.common.annotations.Nullable;
import megamek.utils.MegaMekXmlUtil;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import megamek.common.Aero;
import megamek.common.BombType;
import megamek.common.CommonConstants;
import megamek.common.Entity;
import megamek.common.EntityListFile;
import megamek.common.FighterSquadron;
import megamek.common.IBomber;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.MULParser;
import megamek.common.Tank;

public class MekHqXmlUtil extends MegaMekXmlUtil {
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
        retVal.append(MekHqXmlUtil.indentStr(indentLvl)).append("<entity chassis=\"")
                .append(escape(tgtEnt.getChassis())).append("\" model=\"").append(escape(tgtEnt.getModel()))
                .append("\" type=\"").append(escape(tgtEnt.getMovementModeAsString())).append("\" commander=\"")
                .append(tgtEnt.isCommander()).append("\" externalId=\"").append(tgtEnt.getExternalIdAsString());

        if (tgtEnt.countQuirks() > 0) {
            retVal.append("\" quirks=\"").append(escape(tgtEnt.getQuirkList("::")));
        }
        if (tgtEnt.getC3Master() != null) {
            retVal.append("\" c3MasterIs=\"")
                    .append(tgtEnt.getGame().getEntity(tgtEnt.getC3Master().getId()).getC3UUIDAsString());
        }
        if (tgtEnt.hasC3() || tgtEnt.hasC3i() || tgtEnt.hasNavalC3()) {
            retVal.append("\" c3UUID=\"").append(tgtEnt.getC3UUIDAsString());
        }

        if (!tgtEnt.getCamouflage().hasDefaultCategory()) {
            retVal.append("\" camoCategory=\"").append(escape(tgtEnt.getCamouflage().getCategory()));
        }

        if (!tgtEnt.getCamouflage().hasDefaultFilename()) {
            retVal.append("\" camoFileName=\"").append(escape(tgtEnt.getCamouflage().getFilename()));
        }

        if (tgtEnt.getDeployRound() > 0) {
            retVal.append(String.format("\" %s=\"%d", MULParser.DEPLOYMENT, tgtEnt.getDeployRound()));
        }

        if (tgtEnt instanceof Infantry) {
            retVal.append(String.format("\" %s=\"%d", MULParser.INF_SQUAD_NUM, ((Infantry) tgtEnt).getSquadN()));
        }

        retVal.append(String.format("\" %s=\"%d", MULParser.ALTITUDE, tgtEnt.getAltitude()));

        if (tgtEnt.isOffBoard()) {
            retVal.append("\" offboard=\"");
            retVal.append(String.valueOf(tgtEnt.isOffBoard()));
            retVal.append("\" offboard_distance=\"");
            retVal.append(String.valueOf(tgtEnt.getOffBoardDistance()));
            retVal.append("\" offboard_direction=\"");
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
            retVal.append(MekHqXmlUtil.indentStr(indentLvl + 1)).append("<structural integrity=\"").append(a.getSI())
                    .append("\"/>\n");

            // Heat sinks
            retVal.append(MekHqXmlUtil.indentStr(indentLvl + 1)).append("<heat sinks=\"").append(a.getHeatSinks())
                    .append("\"/>\n");

            // Fuel
            retVal.append(MekHqXmlUtil.indentStr(indentLvl + 1)).append("<fuel left=\"").append(a.getFuel())
                    .append("\"/>\n");

            // TODO: dropship docking collars, bays

            // Large craft stuff
            if (a instanceof Jumpship) {
                Jumpship j = (Jumpship) a;

                // KF integrity
                retVal.append(MekHqXmlUtil.indentStr(indentLvl + 1)).append("<KF integrity=\"")
                        .append(j.getKFIntegrity()).append("\"/>\n");

                // KF sail integrity
                retVal.append(MekHqXmlUtil.indentStr(indentLvl + 1)).append("<sail integrity=\"")
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
            retVal.append(MekHqXmlUtil.indentStr(indentLvl + 1)).append("<nc3set>");
            retVal.append(CommonConstants.NL);
            Iterator<Entity> nc3List = list.iterator();
            while (nc3List.hasNext()) {
                final Entity nc3Entity = nc3List.next();

                if (nc3Entity.onSameC3NetworkAs(tgtEnt, true)) {
                    retVal.append(MekHqXmlUtil.indentStr(indentLvl + 2)).append("<nc3_link link=\"");
                    retVal.append(nc3Entity.getC3UUIDAsString());
                    retVal.append("\"/>");
                    retVal.append(CommonConstants.NL);
                }
            }
            retVal.append(MekHqXmlUtil.indentStr(indentLvl + 1)).append("</nc3set>");
            retVal.append(CommonConstants.NL);
        }

        // Write the C3i Data if needed
        if (tgtEnt.hasC3i()) {
            retVal.append(MekHqXmlUtil.indentStr(indentLvl + 1)).append("<c3iset>");
            retVal.append(CommonConstants.NL);
            Iterator<Entity> c3iList = list.iterator();
            while (c3iList.hasNext()) {
                final Entity C3iEntity = c3iList.next();

                if (C3iEntity.onSameC3NetworkAs(tgtEnt, true)) {
                    retVal.append(MekHqXmlUtil.indentStr(indentLvl + 2)).append("<c3i_link link=\"");
                    retVal.append(C3iEntity.getC3UUIDAsString());
                    retVal.append("\"/>");
                    retVal.append(CommonConstants.NL);
                }
            }
            retVal.append(MekHqXmlUtil.indentStr(indentLvl + 1)).append("</c3iset>");
            retVal.append(CommonConstants.NL);
        }

        // Finish writing this entity to the file.
        retVal.append(MekHqXmlUtil.indentStr(indentLvl)).append("</entity>");

        // Okay, return whatever we've got!
        return retVal.toString();
    }

    private static String getBombChoiceString(IBomber bomber, int indentLvl) {
        StringBuilder retVal = new StringBuilder();

        int[] bombChoices = bomber.getBombChoices();
        if (bombChoices.length > 0) {
            retVal.append(MekHqXmlUtil.indentStr(indentLvl + 1)).append("<bombs>\n");
            for (int type = 0; type < BombType.B_NUM; type++) {
                if (bombChoices[type] > 0) {
                    String typeName = BombType.getBombInternalName(type);
                    retVal.append(MekHqXmlUtil.indentStr(indentLvl + 2)).append("<bomb type=\"");
                    retVal.append(typeName);
                    retVal.append("\" load=\"");
                    retVal.append(bombChoices[type]);
                    retVal.append("\"/>\n");
                }
            }
            retVal.append(MekHqXmlUtil.indentStr(indentLvl + 1)).append("</bombs>\n");
        }

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
        String retval = MekHqXmlUtil.indentStr(indentLvl) + "<turretlock direction=\"";
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
        String retVal = MekHqXmlUtil.indentStr(indentLvl) + "<movement speed=\"";
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
        retVal = retVal.concat(MekHqXmlUtil.indentStr(indentLvl) + "<motive damage=\"");
        retVal = retVal.concat(Integer.toString(e.getMotiveDamage()));
        retVal = retVal.concat("\" penalty=\"");
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

    /** @deprecated use {@link #parseSingleEntityMul(Element)} instead */
    @Deprecated
    public static Entity getEntityFromXmlString(Node xml) {
        return parseSingleEntityMul((Element) xml);
    }

    /**
     * Parses the given node as if it was a .mul file and returns the first entity it contains.
     * <p>
     * In theme with {@link MULParser}, this method fails silently and returns {@code null} if the input
     * can't be parsed; if it can be parsed and contains more than one entity, an
     * {@linkplain IllegalArgumentException} is thrown.
     *
     * @param element the xml tag to parse
     *
     * @return the first entity parsed from the given element, or {@code null} if anything is wrong with
     *         the input
     *
     * @throws IllegalArgumentException if the given element parses to multiple entities
     */
    public static Entity parseSingleEntityMul(Element element) {
        MekHQ.getLogger().trace("Executing getEntityFromXmlString(Node)...");

        MULParser prs = new MULParser();
        prs.parse(element);
        List<Entity> entities = prs.getEntities();

        switch (entities.size()) {
            case 0:
                return null;
            case 1:
                final Entity entity = entities.get(0);
                MekHQ.getLogger().trace("Returning " + entity + " from getEntityFromXmlString(String)...");
                return entity;
            default:
                throw new IllegalArgumentException(
                        "More than one entity contained in XML string! Expecting a single entity.");
        }
    }

    public static String getEntityNameFromXmlString(Node node) {
        NamedNodeMap attrs = node.getAttributes();
        String chassis = attrs.getNamedItem("chassis").getTextContent();
        String model = attrs.getNamedItem("model").getTextContent();
        return chassis + " " + model;
    }
}
