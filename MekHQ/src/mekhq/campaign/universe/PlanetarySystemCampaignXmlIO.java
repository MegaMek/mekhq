/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.universe;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Campaign XML wrapper for campaign-scoped planetary system overrides. */
public final class PlanetarySystemCampaignXmlIO {
    public static final String XML_TAG = "planetarySystemOverrides";

    private static final String OVERRIDE_TAG = "planetarySystemOverride";
    private static final String ID_ATTRIBUTE = "id";

    private PlanetarySystemCampaignXmlIO() {

    }

    public static void writeToXML(PrintWriter writer, int indent, Collection<PlanetarySystem> overrides) {
        if ((overrides == null) || overrides.isEmpty()) {
            return;
        }

        List<PlanetarySystem> sortedOverrides = overrides.stream()
                                                  .filter(system -> (system != null) && (system.getId() != null))
                                                  .sorted(Comparator.comparing(PlanetarySystem::getId))
                                                  .toList();
        if (sortedOverrides.isEmpty()) {
            return;
        }

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, XML_TAG);
        for (PlanetarySystem system : sortedOverrides) {
            writeOverride(writer, indent, system);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, XML_TAG);
    }

    public static List<PlanetarySystem> parse(Node parentNode) throws IOException {
        List<PlanetarySystem> overrides = new ArrayList<>();
        NodeList childNodes = parentNode.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index++) {
            Node childNode = childNodes.item(index);
            if ((childNode.getNodeType() != Node.ELEMENT_NODE) || !OVERRIDE_TAG.equalsIgnoreCase(childNode
                  .getNodeName())) {
                continue;
            }
            String yaml = childNode.getTextContent();
            if ((yaml == null) || yaml.isBlank()) {
                continue;
            }
            String xmlId = parseOverrideId(childNode);
            PlanetarySystem system = PlanetarySystemYamlIO.read(yaml.strip());
            if (!xmlId.equals(system.getId())) {
                String message = String.format(Locale.ROOT,
                      "Planetary system override id attribute \"%s\" does not match YAML id \"%s\".", xmlId,
                      system.getId());
                throw new IOException(message);
            }
            overrides.add(system);
        }
        return overrides;
    }

    private static String parseOverrideId(Node overrideNode) throws IOException {
        Node idAttribute = overrideNode.getAttributes().getNamedItem(ID_ATTRIBUTE);
        String xmlId = idAttribute == null ? null : idAttribute.getTextContent();
        if ((xmlId == null) || xmlId.isBlank()) {
            throw new IOException("Planetary system override is missing an id attribute.");
        }
        return xmlId;
    }

    private static void writeOverride(PrintWriter writer, int indent, PlanetarySystem system) {
        try {
            String id = MHQXMLUtility.escape(system.getId());
            String yaml = PlanetarySystemYamlIO.writeToString(system);
            writer.print(MHQXMLUtility.indentStr(indent));
            writer.print('<');
            writer.print(OVERRIDE_TAG);
            writer.print(' ');
            writer.print(ID_ATTRIBUTE);
            writer.print("=\"");
            writer.print(id);
            writer.print("\"><![CDATA[");
            writer.print(escapeCData(yaml));
            writer.print("]]></");
            writer.print(OVERRIDE_TAG);
            writer.println('>');
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String escapeCData(String text) {
        return text.replace("]]>", "]]]]><![CDATA[>");
    }
}
