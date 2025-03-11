/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.module.api;

import java.io.PrintWriter;

import org.w3c.dom.Node;

import mekhq.campaign.Campaign;

/**
 * Interface that needs to be implemented by all MekHQ plugins. Contains methods that MekHQ uses to
 * identify the plugin, perform initialization, and save state in a campaign file.
 *
 * @author Neoancient
 */
public interface MekHQModule {

    String getModuleName();

    void initPlugin(Campaign c);

    void loadFieldsFromXml(Node node);
    void writeToXML(PrintWriter pw, int indent);
}
