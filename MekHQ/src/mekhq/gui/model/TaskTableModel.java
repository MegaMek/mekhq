/*
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Component;
import java.awt.Image;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import jakarta.annotation.Nullable;
import megamek.client.ui.util.UIUtil;
import megamek.common.rolls.TargetRoll;
import mekhq.IconPackage;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartDemand;
import mekhq.campaign.parts.PartInventory;
import mekhq.campaign.parts.PodSpace;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.StockCount;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.work.IPartWork;
import mekhq.gui.CampaignGUI;
import mekhq.gui.ITechWorkPanel;
import mekhq.gui.RepairTaskInfo;

/**
 * A table model for displaying work items
 */
public class TaskTableModel extends DataTableModel<IPartWork> {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CampaignGUI";
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

    /**
     * Sets the tasks to show, and works out once which other units need each salvaged part. A task's text is built
     * every time its row is drawn, so the check is done here rather than there.
     *
     * @param tasks the tasks to show
     */
    @Override
    public void setData(List<IPartWork> tasks) {
        recordSalvageDemand(tasks);
        super.setData(tasks);
    }

    /**
     * Tells each salvage task which other units need its part. The gaps in each hangar are gathered once and shared by
     * every task drawing on it.
     *
     * @param tasks the tasks about to be shown
     */
    static void recordSalvageDemand(List<IPartWork> tasks) {
        Map<LocalHangar, List<MissingPart>> gapsByHangar = new IdentityHashMap<>();
        for (IPartWork task : tasks) {
            if ((task instanceof Part part) && part.isSalvaging()) {
                List<MissingPart> gaps = gapsByHangar.computeIfAbsent(part.findHangarAtPlace(),
                      hangar -> PartDemand.findGaps(hangar.getUnits()));
                part.setTaskDemand(part.countDemand(gaps));
            }
        }
    }

    /**
     * Adds what a task's short lines leave out: when its stock line says only how many are coming, when they arrive and
     * how many are on order; and for a salvage task, the names of the units that need its part.
     *
     * @param task the task under the mouse
     *
     * @return the tooltip, or {@code null} for none
     */
    static @Nullable String getTaskTooltip(IPartWork task) {
        List<String> sections = new ArrayList<>();
        StockCount stock = task.getShownStock();
        String stockDetails = (stock != null) ? stock.describeDetails() : null;
        if (stockDetails != null) {
            sections.add(stockDetails);
        }
        if ((task instanceof Part part) && part.isSalvaging()) {
            String units = part.getTaskDemand().describeUnits();
            if (units != null) {
                sections.add(units);
            }
        }
        return sections.isEmpty() ? null : "<html>" + String.join("<br>", sections) + "</html>";
    }

    @Override
    public Object getValueAt(int row, int col) {
        return data.get(row).getDesc();
    }

    public IPartWork getTaskAt(int row) {
        return data.get(row);
    }

    /**
     * Builds a short "Skill Needed" indicator naming the technician skill(s) the given task accepts, for display in the
     * repair-task cell so the player can see which specialist a task calls for.
     *
     * <p>A task reports its most appropriate skill(s) via {@link IPartWork#isRightTechType(String)}. If it accepts
     * every technician skill (i.e. it has no specialist requirement) - or, defensively, none at all - this reports that
     * any technician may perform it.</p>
     *
     * @param part the task to describe
     *
     * @return an HTML fragment (with a leading {@code <br>}) naming the required skill(s)
     */
    public static String getRequiredSkillText(IPartWork part) {
        String label;
        if (part instanceof Refit) {
            String globalSkill = Person.getGlobalTechSkillNameFor(part.getUnit());
            label = (globalSkill != null) ?
                          globalSkill :
                          getTextAt(RESOURCE_BUNDLE, "TaskTableModel.requiredSkill.any");
        } else {
            String[] techSkills = SkillType.getTechSkills();
            List<String> accepted = new ArrayList<>();
            for (String skillName : techSkills) {
                if (part.isRightTechType(skillName)) {
                    accepted.add(skillName);
                }
            }

            if (accepted.isEmpty() || (accepted.size() == techSkills.length)) {
                label = getTextAt(RESOURCE_BUNDLE, "TaskTableModel.requiredSkill.any");
            } else {
                label = String.join(", ", accepted);
            }
        }

        return "<br><i>" + getFormattedTextAt(RESOURCE_BUNDLE, "TaskTableModel.requiredSkill.format", label) + "</i>";
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
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
            // Tall enough for six lines: a salvage task naming the units that need its part has one more than most
            table.setRowHeight(UIUtil.scaleForGUI(115));
            Component c = this;
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);
            setToolTipText(getTaskTooltip(getTaskAt(actualRow)));

            String taskDescription = getValueAt(actualRow, actualCol).toString();
            String requiredSkillText = getRequiredSkillText(getTaskAt(actualRow));
            int closingTagIndex = taskDescription.lastIndexOf("</html>");
            if (closingTagIndex >= 0) {
                setText(taskDescription.substring(0, closingTagIndex) +
                              requiredSkillText +
                              taskDescription.substring(closingTagIndex));
            } else {
                setText("<html>" + taskDescription + requiredSkillText + "</html>");
            }

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
                if (part instanceof MissingPart missingPart) {
                    if (!missingPart.isReplacementAvailable()) {
                        PartInventory inventories = missingPart.getPartInventory(missingPart.getNewPart());

                        if ((inventories.getTransit() > 0) || (inventories.getOrdered() > 0)) {
                            availableLevel = REPAIR_STATE.IN_TRANSIT;
                        } else {
                            availableLevel = REPAIR_STATE.NOT_AVAILABLE;
                        }
                    }
                } else if (part instanceof PodSpace podSpace && !part.isSalvaging()) {
                    StockCount stock = podSpace.countStock();
                    boolean isAnyComing = (stock.inventory().getTransit() > 0) || (stock.inventory().getOrdered() > 0);
                    //Show available if at least one replacement can be made
                    if (stock.needed() == 0) {
                        availableLevel = REPAIR_STATE.BLOCKED;
                    } else if (stock.inStock() > 0) {
                        availableLevel = REPAIR_STATE.AVAILABLE;
                    } else if (!isAnyComing) {
                        availableLevel = REPAIR_STATE.NOT_AVAILABLE;
                    } else {
                        availableLevel = REPAIR_STATE.IN_TRANSIT;
                    }
                }

                if (availableLevel == REPAIR_STATE.AVAILABLE) {
                    Person tech = panel.getSelectedTech();

                    if (null == tech) {
                        //Find a valid tech that we can copy their skill from
                        mekhq.campaign.Campaign campaign = gui.getCampaign();
                        List<Person> techs = campaign.getPlayerForce()
                                                   .getHumanResources()
                                                   .getTechs(campaign.getPlayerForce().getHangar().getUnits(),
                                                         campaign.getCampaignOptions(),
                                                         campaign.getPlayerForce().isClanForce(),
                                                         campaign.getLocalDate());

                        for (int i = techs.size() - 1; i >= 0; i--) {
                            Person techTemp = techs.get(i);

                            if ((null == techTemp) || (null == part.getUnit())) {
                                continue;
                            }

                            if (techTemp.canTech(part.getUnit().getEntity())) {
                                tech = techTemp;
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
