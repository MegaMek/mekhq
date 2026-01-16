/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.model;

import java.awt.Component;
import java.awt.Image;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import megamek.client.ui.util.UIUtil;
import megamek.common.rolls.TargetRoll;
import mekhq.IconPackage;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInventory;
import mekhq.campaign.parts.PodSpace;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.work.IPartWork;
import mekhq.gui.CampaignGUI;
import mekhq.gui.ITechWorkPanel;
import mekhq.gui.RepairTaskInfo;

/**
 * A table model for displaying work items
 */
public class TaskTableModel extends DataTableModel<IPartWork> {
    private static final Map<String, Person> techCache = new HashMap<>();

    private final CampaignGUI gui;
    private final ITechWorkPanel panel;

    private interface REPAIR_STATE { // TODO : Enum Swapover
        int AVAILABLE = 0;
        int NOT_AVAILABLE = 1;
        int IN_TRANSIT = 2;
        int BLOCKED = 3;
        int SCHEDULED = 4;
    }

    public TaskTableModel(CampaignGUI gui, ITechWorkPanel panel) {
        columnNames = new String[] { "Tasks" };
        data = new ArrayList<>();
        this.gui = gui;
        this.panel = panel;
    }

    @Override
    public Object getValueAt(int row, int col) {
        return data.get(row).getDesc();
    }

    public IPartWork getTaskAt(int row) {
        return data.get(row);
    }

    public IPartWork[] getTasksAt(int[] rows) {
        IPartWork[] tasks = new IPartWork[rows.length];
        for (int i = 0; i < rows.length; i++) {
            int row = rows[i];
            tasks[i] = data.get(row);
        }
        return tasks;
    }

    public TaskTableModel.Renderer getRenderer(IconPackage icons) {
        return new TaskTableModel.Renderer(icons);
    }

    public class Renderer extends RepairTaskInfo implements TableCellRenderer {

        public Renderer(IconPackage icons) {
            super(icons);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
              boolean isSelected, boolean hasFocus,
              int row, int column) {
            int newRowHeight = UIUtil.scaleForGUI(100);
            if (table.getRowHeight(row) != newRowHeight) {
                table.setRowHeight(newRowHeight);
            }
            Component c = this;
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);

            setText("<html>" + getValueAt(actualRow, actualCol).toString() + "</html>");

            if (isSelected) {
                highlightBorder();
            } else {
                unhighlightBorder();
            }

            c.setBackground(table.getBackground());
            c.setForeground(table.getForeground());

            IPartWork part = getTaskAt(actualRow);

            int availableLevel = REPAIR_STATE.AVAILABLE;

            if (null != part.getTech()) {
                availableLevel = REPAIR_STATE.SCHEDULED;
            } else {
                if (part instanceof MissingPart) {
                    if (!((MissingPart) part).isReplacementAvailable()) {
                        PartInventory inventories = gui.getCampaign()
                                                          .getPartInventory(((MissingPart) part).getNewPart());

                        if ((inventories.getTransit() > 0) || (inventories.getOrdered() > 0)) {
                            availableLevel = REPAIR_STATE.IN_TRANSIT;
                        } else {
                            availableLevel = REPAIR_STATE.NOT_AVAILABLE;
                        }
                    }
                } else if (part instanceof PodSpace && !part.isSalvaging()) {
                    Matcher m = Pattern.compile(".*(\\d+)/(\\d+).*(\\d+) in transit, (\\d+) on order.*")
                                      .matcher(part.getDetails());
                    if (m.matches()) {
                        //Show available if at least one replacement can be made
                        if (m.group(2).equals("0")) {
                            availableLevel = REPAIR_STATE.BLOCKED;
                        } else if (!m.group(1).equals("0")) {
                            availableLevel = REPAIR_STATE.AVAILABLE;
                        } else if (m.group(3).equals("0") && m.group(4).equals("0")) {
                            availableLevel = REPAIR_STATE.NOT_AVAILABLE;
                        } else {
                            availableLevel = REPAIR_STATE.IN_TRANSIT;
                        }
                    }
                }

                if (availableLevel == REPAIR_STATE.AVAILABLE) {
                    Person tech = panel.getSelectedTech();

                    if (tech == null) {
                        tech = panel.getTempTech();
                    }
                    if (null == tech) {
                        //Find a valid tech that we can copy their skill from
                        List<Person> techs = gui.getCampaign().getTechs();

                        for (int i = techs.size() - 1; i >= 0; i--) {
                            Person techTemp = techs.get(i);

                            if ((null == techTemp) || (null == part.getUnit())) {
                                continue;
                            }

                            if (techTemp.canTech(part.getUnit().getEntity())) {
                                tech = techTemp;
                                panel.setTempTech(techTemp);
                                break;
                            }
                        }

                        if (null != tech) {
                            Skill partSkill = tech.getSkillForWorkingOn(part);

                            // If the tech has no applicable skill, skip dummy-tech creation and let getTargetFor()
                            // handle the "cannot repair" case.
                            if (partSkill != null) {
                                String skillName = partSkill.getType().getName();

                                // Find a tech in our placeholder cache
                                Person cachedTech = techCache.get(skillName);

                                if (cachedTech == null) {
                                    // Create a dummy elite tech with the proper skill and 1 minute
                                    // and put it in our cache for later use
                                    cachedTech = new Person("Temp",
                                          String.format("Tech (%s)", skillName),
                                          gui.getCampaign());
                                    cachedTech.addSkill(skillName,
                                          partSkill.getType().getEliteLevel(), 1);
                                    cachedTech.setMinutesLeft(1);

                                    techCache.put(skillName, cachedTech);
                                }

                                tech = cachedTech;
                            }
                        }
                    }

                    if (null != tech) {
                        TargetRoll roll = gui.getCampaign().getTargetFor(part, tech);

                        if ((roll.getValue() == TargetRoll.IMPOSSIBLE) ||
                                  (roll.getValue() == TargetRoll.AUTOMATIC_FAIL) ||
                                  (roll.getValue() == TargetRoll.CHECK_FALSE)) {
                            availableLevel = REPAIR_STATE.BLOCKED;
                        }
                    }
                }
            }

            String imgMod = "";
            boolean setSecondary = false;

            switch (availableLevel) {
                case REPAIR_STATE.BLOCKED:
                    imgMod = "_impossible";
                    break;

                case REPAIR_STATE.IN_TRANSIT:
                    imgMod = "_transit";
                    break;

                case REPAIR_STATE.NOT_AVAILABLE:
                    imgMod = "_na";
                    break;

                case REPAIR_STATE.SCHEDULED:
                    setSecondary = true;
                    break;
            }

            String[] imgData = Part.findPartImage(part);
            String imgPath = imgData[0] + imgData[1] + imgMod + ".png";

            Image imgTool = getToolkit().getImage(imgPath);

            this.setImage(imgTool);

            if (setSecondary) {
                this.setSecondaryImage(getToolkit().getImage("data/images/misc/repair/working.png")); // TODO : Remove inline file path
            } else {
                this.setSecondaryImage(null);
            }

            return c;
        }
    }
}
