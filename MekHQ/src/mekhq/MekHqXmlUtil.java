package mekhq;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import megamek.common.Aero;
import megamek.common.BombType;
import megamek.common.CommonConstants;
import megamek.common.Entity;
import megamek.common.EntityListFile;
import megamek.common.FighterSquadron;
import megamek.common.IBomber;
import megamek.common.IPlayer;
import megamek.common.Jumpship;
import megamek.common.MULParser;
import megamek.common.Tank;
import megamek.common.logging.LogLevel;

public class MekHqXmlUtil {

    private static DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY;
    private static DocumentBuilderFactory UNSAFE_DOCUMENT_BUILDER_FACTORY;
    private static SAXParserFactory SAX_PARSER_FACTORY;

    /**
     * Creates a DocumentBuilder safe from XML external entities
     * attacks, and XML entity expansion attacks.
     * @return A DocumentBuilder safe to use to read untrusted XML.
     */
    public static DocumentBuilder newSafeDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DOCUMENT_BUILDER_FACTORY;
        if (null == dbf) {
            // At worst we may do this twice if multiple threads
            // hit this method. It is Ok to have more than one
            // instance of the builder factory, as long as it is
            // XXE safe.
            dbf = DocumentBuilderFactory.newInstance();

            //
            // Adapted from: https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#JAXP_DocumentBuilderFactory.2C_SAXParserFactory_and_DOM4J
            //
            // "...The JAXP DocumentBuilderFactory setFeature method allows a
            // developer to control which implementation-specific XML processor
            // features are enabled or disabled. The features can either be set
            // on the factory or the underlying XMLReader setFeature method.
            // Each XML processor implementation has its own features that
            // govern how DTDs and external entities are processed."
            //
            // "[disable] these as well, per Timothy Morgan's 2014 paper: 'XML
            // Schema, DTD, and Entity Attacks'"
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);

            // "This is the PRIMARY defense. If DTDs (doctypes) are disallowed,
            // almost all XML entity attacks are prevented"
            String FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
            dbf.setFeature(FEATURE, true);

            DOCUMENT_BUILDER_FACTORY = dbf;
        }

        return dbf.newDocumentBuilder();
    }

    /**
     * USE WITH CARE. Creates a DocumentBuilder safe from XML external entities
     * attacks, but unsafe from XML entity expansion attacks.
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
     * @return a SAX {@linkplain XMLReader} that is safe from external entities and entity expansion attacks.
     *
     * @see "https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#JAXB_Unmarshaller"
     */
    @SuppressWarnings("nls")
    public static XMLReader createSafeXMLReader() {
        if (SAX_PARSER_FACTORY == null) {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            try {
                spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
                spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
                throw new AssertionError("SAX implementation does not recognize or support the features we want to disable", e);
            } catch (ParserConfigurationException e) {
                throw new AssertionError(e); // Only if we messed up the CFG above
            }
            SAX_PARSER_FACTORY = spf;
        }
        try {
            return SAX_PARSER_FACTORY.newSAXParser().getXMLReader();
        } catch (ParserConfigurationException e) {
            throw new AssertionError(e); // Only if we messed up the CFG above
        } catch (SAXException e) {
            throw new AssertionError(e); // Whatever - just blow up. :-)
                                         // As of 2018-11, Xerces does not throw generic SAXExceptions.
                                         // Yes, SAX was designed when checked exception were all the rage.
        }
    }

    /**
     * @return a {@linkplain Source} for the provided input stream that is safe
     * from external entities and entity expansion attacks.
     *
     * @see "https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#JAXB_Unmarshaller"
     */
    public static Source createSafeXmlSource(InputStream inputStream) {
        return new SAXSource(createSafeXMLReader(), new InputSource(inputStream));
    }

    public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, String val) {
        for (int x=0; x<indent; x++) {
            pw1.print("\t");
        }

        pw1.print("<"+name+">");
        pw1.print(escape(val));
        pw1.println("</"+name+">");
    }

    public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, int val) {
        for (int x=0; x<indent; x++) {
            pw1.print("\t");
        }

        pw1.print("<"+name+">");
        pw1.print(val);
        pw1.println("</"+name+">");
    }

    public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, boolean val) {
        for (int x=0; x<indent; x++) {
            pw1.print("\t");
        }

        pw1.print("<"+name+">");
        pw1.print(val);
        pw1.println("</"+name+">");
    }

    public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, long val) {
        for (int x=0; x<indent; x++) {
            pw1.print("\t");
        }

        pw1.print("<"+name+">");
        pw1.print(val);
        pw1.println("</"+name+">");
    }

    public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, double val) {
        for (int x=0; x<indent; x++) {
            pw1.print("\t");
        }

        pw1.print("<"+name+">");
        pw1.print(val);
        pw1.println("</"+name+">");
    }

    public static String indentStr(int level) {
        String retVal = "";

        for (int x=0; x<level; x++) {
            retVal += "\t";
        }

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

         if ((null != tgtEnt.getCamoCategory())
                 && (tgtEnt.getCamoCategory() != IPlayer.NO_CAMO)
                 && !tgtEnt.getCamoCategory().isEmpty()) {
             retVal += "\" camoCategory=\"";
             retVal += String.valueOf(escape(tgtEnt.getCamoCategory()));
         }

         if ((null != tgtEnt.getCamoFileName())
                 && (tgtEnt.getCamoFileName() != IPlayer.NO_CAMO)
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

        // If the entity carries bombs, write those out
        if(tgtEnt instanceof IBomber) {
            retVal += getBombChoiceString((IBomber) tgtEnt, indentLvl);
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

    private static String getBombChoiceString(IBomber bomber, int indentLvl) {
        String retVal = "";

        int[] bombChoices = bomber.getBombChoices();
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

    /** @deprecated use {@link #parseSingleEntityMul(Element)} instead */
    @Deprecated
    public static Entity getEntityFromXmlString(Node xml) {
        return parseSingleEntityMul((Element) xml);
    }

    /**
     * Parses the given node as if it was a .mul file and returns the first
     * entity it contains.
     * <p>
     * In theme with {@link MULParser}, this method fails silently and returns
     * {@code null} if the input can't be parsed; if it can be parsed and
     * contains more than one entity, an {@linkplain IllegalArgumentException}
     * is thrown.
     * 
     * @param element
     *        the xml tag to parse
     * 
     * @return the first entity parsed from the given element, or {@code null}
     *         if anything is wrong with the input
     *
     * @throws IllegalArgumentException
     *         if the given element parses to multiple entities
     */
    public static Entity parseSingleEntityMul(Element element) {
        MekHQ.getLogger().log(MekHqXmlUtil.class, "getEntityFromXmlString(Element)", LogLevel.TRACE, "Executing getEntityFromXmlString(Node)..."); //$NON-NLS-2$

        MULParser prs = new MULParser();
        prs.parse(element);
        List<Entity> entities = prs.getEntities();

        switch (entities.size()) {
            case 0:
                return null;
            case 1:
                Entity entity = entities.get(0);
                MekHQ.getLogger().log(MekHqXmlUtil.class, "getEntityFromXmlString(Element)", LogLevel.TRACE, "Returning " + entity + " from getEntityFromXmlString(String)..."); //$NON-NLS-1$
                return entity;
            default:
                throw new IllegalArgumentException("More than one entity contained in XML string!  Expecting a single entity.");
        }
    }

    /** Escapes a string to store in an XML element.
      * @param string The string to be encoded
      * @return An encoded copy of the string
      */
    public static String escape(String string) {
        return StringEscapeUtils.escapeXml10(string);
    }

    /**
     * Unescape...well, it reverses escaping...
     */
    public static String unEscape(String string) {
      return StringEscapeUtils.unescapeXml(string);
    }

    public static String getEntityNameFromXmlString(Node node) {
        NamedNodeMap attrs = node.getAttributes();
        String chassis = attrs.getNamedItem("chassis").getTextContent();
        String model = attrs.getNamedItem("model").getTextContent();
        return chassis + " " + model;
    }
}
