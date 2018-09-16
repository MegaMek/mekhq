/*
 * IExport.java
 * 
 * MegaMek - Copyright (C) 2018 - The MegaMek Team
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

package mekhq.gui.utilities;


import java.io.File;
import java.util.ResourceBundle;

import javax.swing.table.TableModel;

import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.gui.CampaignGUI;

/* 
 * This interface allows for exporting of data from MekHQ to a wide variety of file formats.
 * All exporting functionality is handled through this interface.
 * e.g. exporting personnel to a file
 */
public interface IExport
{
    // shared resourceMap across all classes that implement this interface
    static ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.IExport", new EncodeControl()); //$NON-NLS-1$

    /**
     * Exports a JTable to a file
     * @param table
     * @param file
     */
    public String exportTable(TableModel table, File file);

    /**
     * Exports finances to a file
     * @param file
     * @param format
     * @param campaign
     */
    public String exportFinances(File file, String format, Campaign campaign);

    /**
     * Exports selected warehouse parts to a file
     * @param file
     * @param gui
     */
    public String exportParts(File file, CampaignGUI gui);

    /**
     * Exports one or more selected personnel to a file
     * @param file
     * @param gui
     */
    public String exportPersonnelFile(File file, CampaignGUI gui);

    /**
     * Exports campaign options to a file
     * @param file
     * @param campaign
     */
    public String exportCampaignOptions(File file, Campaign campaign);
}
