/*
 * ExportXML.java
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
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ResourceBundle;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.gui.CampaignGUI;
import mekhq.gui.GuiTabType;
import mekhq.gui.PersonnelTab;
import mekhq.gui.WarehouseTab;
import mekhq.gui.model.PartsTableModel;


public class ExportXML implements IExport
{
    @Override
    public String exportTable(TableModel table, File file) {
        //TODO
        return null;
    }

    @Override
    public String exportFinances(File file, String format, Campaign c) {
        //TODO
        return null;
    }

    @Override
    public String exportParts(File file, CampaignGUI gui) {
        final String METHOD_NAME = "exportParts()";
        String report = "";
        if (gui.getTab(GuiTabType.WAREHOUSE) != null) {
            try {
                JTable partsTable = ((WarehouseTab)gui.getTab(GuiTabType.WAREHOUSE)).getPartsTable();
                PartsTableModel partsModel = ((WarehouseTab)gui.getTab(GuiTabType.WAREHOUSE)).getPartsModel();
                int row = partsTable.getSelectedRow();
                if (row < 0) {
                    MekHQ.getLogger().log(ExportXML.class, METHOD_NAME, LogLevel.WARNING,
                            "ERROR: Cannot export parts if none are selected! Ignoring."); //$NON-NLS-1$
                    return resourceMap.getString("dlgNoPartsSelected.text");
                }

                int[] rows = partsTable.getSelectedRows();
                Part[] parts = new Part[rows.length];
                for (int i = 0; i < rows.length; i++) {
                    parts[i] = partsModel.getPartAt(partsTable
                            .convertRowIndexToModel(rows[i]));
                }
                FileOutputStream fos = new FileOutputStream(file);
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"));

                // File header
                pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

                ResourceBundle resourceMapHQ = ResourceBundle.getBundle("mekhq.resources.MekHQ");
                // Start the XML root.
                pw.println("<parts version=\""
                        + resourceMapHQ.getString("Application.version") + "\">");

                for (int i = 0; i < rows.length; i++) {
                    parts[i].writeToXml(pw, 1);
                }

                // Close everything out and be done with it.
                pw.println("</parts>");
                pw.flush();
                pw.close();
                fos.close();

                MekHQ.getLogger().log(ExportXML.class, METHOD_NAME, LogLevel.INFO,
                        "Parts saved to " + file); //$NON-NLS-1$
                report = Integer.toString(rows.length) + " " + resourceMap.getString("dlgPartsSavedFile.text");
            } catch (Exception ex) {
                MekHQ.getLogger().error(ExportXML.class, METHOD_NAME, ex);
                report = resourceMap.getString("dlgProblemWritingFile.text");
            }
        } else {
            report = resourceMap.getString("dlgNoParts.text");
        }
        return report;
    }

    @Override
    public String exportPersonnelFile(File file, CampaignGUI gui) {
        final String METHOD_NAME = "exportPersonnel()";
        String report = "";
        try {
            PersonnelTab pt = (PersonnelTab)gui.getTab(GuiTabType.PERSONNEL);
            int row = pt.getPersonnelTable().getSelectedRow();
            if (row < 0) {
                MekHQ.getLogger().log(ExportXML.class, METHOD_NAME, LogLevel.WARNING,
                        "ERROR: Cannot export person if no one is selected! Ignoring."); //$NON-NLS-1$
                return resourceMap.getString("dlgNoPersonSelected.text");
            }
            Person selectedPerson = pt.getPersonModel().getPerson(pt.getPersonnelTable()
                    .convertRowIndexToModel(row));
            int[] rows = pt.getPersonnelTable().getSelectedRows();
            Person[] people = new Person[rows.length];
            for (int i = 0; i < rows.length; i++) {
                people[i] = pt.getPersonModel().getPerson(pt.getPersonnelTable()
                        .convertRowIndexToModel(rows[i]));
            }

            FileOutputStream fos = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"));

            // File header
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

            ResourceBundle resourceMapHQ = ResourceBundle
                    .getBundle("mekhq.resources.MekHQ");
            // Start the XML root.
            pw.println("<personnel version=\""
                    + resourceMapHQ.getString("Application.version") + "\">");

            if (rows.length > 1) {
                for (int i = 0; i < rows.length; i++) {
                    people[i].writeToXml(pw, 1);
                }
            } else {
                selectedPerson.writeToXml(pw, 1);
            }
            // Close everything out and be done with it.
            pw.println("</personnel>");
            pw.flush();
            pw.close();
            fos.close();

            MekHQ.getLogger().log(ExportXML.class, METHOD_NAME, LogLevel.INFO,
                    "Personnel saved to " + file); //$NON-NLS-1$
            report = Integer.toString(rows.length) + " " + resourceMap.getString("dlgPersonnelSavedFile.text");
        } catch (Exception ex) {
            MekHQ.getLogger().error(ExportXML.class, METHOD_NAME, ex);
            report = resourceMap.getString("dlgProblemWritingFile.text");
        }
        return report;
    }

    @Override
    public String exportCampaignOptions(File file, Campaign campaign) {
        final String METHOD_NAME = "exportCampaignOptions()";
        String report = "";

        try {
            FileOutputStream fos = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"));

            ResourceBundle resourceMapHQ = ResourceBundle
                    .getBundle("mekhq.resources.MekHQ");
            // File header
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.println("<options version=\""
                    + resourceMapHQ.getString("Application.version") + "\">");
            // Start the XML root.
            campaign.getCampaignOptions().writeToXml(pw, 1);
            pw.println("\t<skillTypes>");
            for (String name : SkillType.skillList) {
                SkillType type = SkillType.getType(name);
                if (null != type) {
                    type.writeToXml(pw, 2);
                }
            }
            pw.println("\t</skillTypes>");
            pw.println("\t<specialAbilities>");
            for (String key : SpecialAbility.getAllSpecialAbilities().keySet()) {
                SpecialAbility.getAbility(key).writeToXml(pw, 2);
            }
            pw.println("\t</specialAbilities>");
            campaign.getRandomSkillPreferences().writeToXml(pw, 1);
            pw.println("</options>");
            // Close everything out and be done with it.
            pw.flush();
            pw.close();
            fos.close();

            report = resourceMap.getString("CampaignOptionsExportSuccess.text");
        } catch (Exception ex) {
            MekHQ.getLogger().error(ExportXML.class, METHOD_NAME, ex); //$NON-NLS-1$
            report = resourceMap.getString("dlgProblemWritingFile.text");
        }

        return report;
    }
}
