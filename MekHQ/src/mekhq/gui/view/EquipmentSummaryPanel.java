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
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
package mekhq.gui.view;

import static mekhq.utilities.MHQInternationalization.getText;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.quartermaster.ArmorKitCatalog;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * A small, reusable "Equipment" block for a {@link Person}. It lists every equipment kit the character is carrying: the
 * personal armor kit (unless it is the default coveralls) followed by any tool, medical, computer, or other equipment
 * kits, each by its display name.
 *
 * <p>Only shown when the person actually carries equipment, mirroring the way {@link LocationSummaryPanel} presents
 * a compact summary block above the tabbed detail views.</p>
 */
public class EquipmentSummaryPanel extends JPanel {

    public EquipmentSummaryPanel(Person person) {
        StringBuilder html = new StringBuilder("<html>");
        List<String> kits = equipmentDisplayNames(person);
        for (int index = 0; index < kits.size(); index++) {
            if (index > 0) {
                html.append("<br>");
            }
            html.append(escapeHtml(kits.get(index)));
        }
        html.append("</html>");

        setLayout(new BorderLayout());
        setBorder(RoundedLineBorder.createRoundedLineBorder(getText("EquipmentSummaryPanel.title")));
        add(new JLabel(html.toString()), BorderLayout.CENTER);
    }

    /**
     * @param person the character to inspect
     *
     * @return {@code true} if the person carries at least one equipment kit worth displaying
     */
    public static boolean hasEquipment(Person person) {
        return !equipmentDisplayNames(person).isEmpty();
    }

    /**
     * Every equipment kit the person is carrying, by display name, sorted alphabetically. The default coveralls armor
     * kit is excluded because it represents "no protection" rather than a real kit.
     */
    private static List<String> equipmentDisplayNames(Person person) {
        List<String> kits = new ArrayList<>();

        String armorKit = person.getArmorKitName();
        if ((armorKit != null) && !ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME.equals(armorKit)) {
            kits.add(kitDisplayName(armorKit));
        }

        for (String kitName : person.getRepairKitNames()) {
            kits.add(kitDisplayName(kitName));
        }

        kits.sort(Comparator.naturalOrder());
        return kits;
    }

    /** Resolves an equipment internal name to its display name, falling back to the raw name when unresolvable. */
    private static String kitDisplayName(String internalName) {
        EquipmentType type = EquipmentType.get(internalName);
        return (type != null) ? type.getName() : internalName;
    }

    /** Escapes the HTML metacharacters that would otherwise mis-render inside the Swing {@code <html>} label. */
    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
