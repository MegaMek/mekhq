package mekhq.campaign.personnel;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.UUID;

import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Ancestors implements Serializable {
	private UUID id;
	private UUID fatherId;
	private UUID motherId;

	public Ancestors(Campaign c) {
		this(null, null, c);
	}

	public Ancestors(UUID father, UUID mother, Campaign c) {
		fatherId = father;
		motherId = mother;

		// Generate Id
		id = UUID.randomUUID();
	}

	public static Ancestors generateInstanceFromXML(Node wn, Campaign c) {
		Ancestors retVal = null;

		try {
			retVal = new Ancestors(c);

            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

	            if (wn2.getNodeName().equalsIgnoreCase("id")) {
	                retVal.id = UUID.fromString(wn2.getTextContent());
	            } else if (wn2.getNodeName().equalsIgnoreCase("fatherId")) {
	            	retVal.fatherId = UUID.fromString(wn2.getTextContent());
	            } else if (wn2.getNodeName().equalsIgnoreCase("motherId")) {
	            	retVal.motherId = UUID.fromString(wn2.getTextContent());
	            }
            }
		} catch (Exception e) {
		    MekHQ.getLogger().error(Ancestors.class, "generateInstanceFromXML(Node,Campaign,Version)", e); //$NON-NLS-1$
		}

		return retVal;
	}
}
