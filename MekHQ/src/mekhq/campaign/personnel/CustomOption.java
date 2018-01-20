/**
 * 
 */
package mekhq.campaign.personnel;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.logging.LogLevel;
import megamek.common.options.IOption;
import megamek.common.options.PilotOptions;
import mekhq.MekHQ;

/**
 * Parses custom SPA file and passes data to the PersonnelOption constructor so the custom
 * abilities are included.
 * 
 * @author Neoancient
 *
 */
public class CustomOption {
    
    private String name;
    private String group;
    private int type;
    private Object defaultVal;
    
    private CustomOption(String key) {
        this.name = key;
        group = PilotOptions.LVL3_ADVANTAGES;
        type = IOption.BOOLEAN;
        defaultVal = Boolean.FALSE;
    }
    
    public String getName() {
        return name;
    }
    
    public String getGroup() {
        return group;
    }
    
    public int getType() {
        return type;
    }
    
    public Object getDefault() {
        return defaultVal;
    }
    
    public static List<CustomOption> getCustomAbilities() {
        final String METHOD_NAME = "getCustomAbilities()"; //$NON-NLS-1$
        List<CustomOption> retVal = new ArrayList<>();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document xmlDoc = null;


        try {
            FileInputStream fis = new FileInputStream("data/universe/customspa.xml");
            // Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(fis);
        } catch (Exception ex) {
            MekHQ.getLogger().log(CustomOption.class, METHOD_NAME, ex);
        }

        Element spaEle = xmlDoc.getDocumentElement();
        NodeList nl = spaEle.getChildNodes();

        // Get rid of empty text nodes and adjacent text nodes...
        // Stupid weird parsing of XML.  At least this cleans it up.
        spaEle.normalize();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);

            if (wn.getParentNode() != spaEle)
                continue;

            int xc = wn.getNodeType();

            if (xc == Node.ELEMENT_NODE) {
                // This is what we really care about.
                // All the meat of our document is in this node type, at this
                // level.
                // Okay, so what element is it?
                String xn = wn.getNodeName();

                if (xn.equalsIgnoreCase("option")) {
                    CustomOption option = CustomOption.generateInstanceFromXML(wn);
                    if (null != option) {
                        retVal.add(option);
                    }
                }
            }
        }
        return retVal;
    }

    public static CustomOption generateInstanceFromXML(Node wn) {
        final String METHOD_NAME = "generateInstanceFromXML(Node)"; //$NON-NLS-1$

        CustomOption retVal = null;

        String key = wn.getAttributes().getNamedItem("name").getTextContent();
        if (null == key) {
            MekHQ.getLogger().log(CustomOption.class, METHOD_NAME, LogLevel.ERROR,
                    "Custom ability does not have a 'name' attribute.");
            return null;
        }
        
        try {
            retVal = new CustomOption(key);
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("group")) {
                    retVal.group = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("type")) {
                    retVal.type = Integer.parseInt(wn2.getTextContent());
                }
            }

            switch (retVal.type) {
                case IOption.BOOLEAN:
                    retVal.defaultVal = Boolean.FALSE;
                    break;
                case IOption.INTEGER:
                    retVal.defaultVal = new Integer(0);
                    break;
                case IOption.FLOAT:
                    retVal.defaultVal = new Float(0.0f);
                    break;
                case IOption.STRING:
                case IOption.CHOICE:
                default:
                    retVal.defaultVal = "";
                    break;
            }
        } catch (Exception ex) {
            MekHQ.getLogger().log(CustomOption.class, METHOD_NAME, LogLevel.ERROR,
                    "Error parsing custom ability " + retVal.name);
        }
        
        return retVal;
    }

    

}
