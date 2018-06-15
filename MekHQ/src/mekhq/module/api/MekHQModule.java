/*
 * Copyright (c) 2018  - The MegaMek Team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.module.api;

import java.io.PrintWriter;

import org.w3c.dom.Node;

import mekhq.campaign.Campaign;

/**
 * Interface that needs to be implemented by all MekHQ plugins. Contains methods that MekHQ uses to identify
 * the plugin, perform initialization, and save state in a campaign file.
 * 
 * @author Neoancient
 *
 */
public interface MekHQModule {

    String getModuleName();
    
    void initPlugin(Campaign c);
    
    void loadFieldsFromXml(Node node);
    void writeToXml(PrintWriter pw, int indent);
}
