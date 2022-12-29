/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.service;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.gui.dialog.MassRepairSalvageDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MassRepairConfiguredOptions {
    //region Variable Declarations
    private boolean useRepair;
    private boolean useSalvage;
    private boolean useExtraTime;
    private boolean useRushJob;
    private boolean allowCarryover;
    private boolean optimizeToCompleteToday;
    private boolean useAssignedTechsFirst;
    private boolean scrapImpossible;
    private boolean replacePodParts;
    private List<MRMSOption> mrmsOptions;
    private transient boolean hasActiveMRMSOption;
    //endregion Variable Declarations

    //region Constructors
    public MassRepairConfiguredOptions(Campaign campaign) {
        setup(campaign.getCampaignOptions());
    }

    public MassRepairConfiguredOptions(MassRepairSalvageDialog massRepairSalvageDialog) {
        setup(massRepairSalvageDialog);
    }
    //endregion Constructors

    //region Initialization
    public void setup(CampaignOptions options) {
        setUseRepair(options.isMRMSUseRepair());
        setUseSalvage(options.isMRMSUseSalvage());
        setUseExtraTime(options.isMRMSUseExtraTime());
        setUseRushJob(options.isMRMSUseRushJob());
        setAllowCarryover(options.isMRMSAllowCarryover());
        setOptimizeToCompleteToday(options.isMRMSOptimizeToCompleteToday());
        setScrapImpossible(options.isMRMSScrapImpossible());
        setUseAssignedTechsFirst(options.isMRMSUseAssignedTechsFirst());
        setReplacePodParts(options.isMRMSReplacePod());
        setMRMSOptions(options.getMRMSOptions());
        setHasActiveMRMSOption(getMRMSOptions().stream().anyMatch(MRMSOption::isActive));
    }

    public void setup(MassRepairSalvageDialog dlg) {
        setUseRepair(dlg.getUseRepairBox().isSelected());
        setUseSalvage(dlg.getUseSalvageBox().isSelected());
        setUseExtraTime(dlg.getUseExtraTimeBox().isSelected());
        setUseRushJob(dlg.getUseRushJobBox().isSelected());
        setAllowCarryover(dlg.getAllowCarryoverBox().isSelected());
        setOptimizeToCompleteToday(dlg.getOptimizeToCompleteTodayBox().isSelected());

        if (dlg.getScrapImpossibleBox() != null) {
            setScrapImpossible(dlg.getScrapImpossibleBox().isSelected());
        }

        if (dlg.getUseAssignedTechsFirstBox() != null) {
            setUseAssignedTechsFirst(dlg.getUseAssignedTechsFirstBox().isSelected());
        }

        if (dlg.getReplacePodPartsBox() != null) {
            setReplacePodParts(dlg.getReplacePodPartsBox().isSelected());
        }

        setMRMSOptions(new ArrayList<>());
        for (PartRepairType partRepairType : PartRepairType.getMRMSValidTypes()) {
            MassRepairSalvageDialog.MassRepairOptionControl mroc = dlg.getMassRepairOptionControlMap().get(partRepairType);

            if (mroc == null) {
                continue;
            }

            MRMSOption mrmsOption = new MRMSOption(partRepairType, mroc.getActiveBox().isSelected(),
                    mroc.getMinSkillCBox().getSelectedIndex(), mroc.getMaxSkillCBox().getSelectedIndex(),
                    (Integer) mroc.getMinBTHSpn().getValue(), (Integer) mroc.getMaxBTHSpn().getValue());

            if (mrmsOption.isActive()) {
                setHasActiveMRMSOption(true);
            }

            getMRMSOptions().add(mrmsOption);
        }
    }
    //endregion Initialization

    //region Getters/Setters
    public boolean isEnabled() {
        return useRepair() || useSalvage();
    }

    public boolean useRepair() {
        return useRepair;
    }

    public void setUseRepair(boolean useRepair) {
        this.useRepair = useRepair;
    }

    public boolean useSalvage() {
        return useSalvage;
    }

    public void setUseSalvage(boolean useSalvage) {
        this.useSalvage = useSalvage;
    }

    public boolean isUseExtraTime() {
        return useExtraTime;
    }

    public void setUseExtraTime(boolean useExtraTime) {
        this.useExtraTime = useExtraTime;
    }

    public boolean isUseRushJob() {
        return useRushJob;
    }

    public void setUseRushJob(boolean useRushJob) {
        this.useRushJob = useRushJob;
    }

    public boolean isAllowCarryover() {
        return allowCarryover;
    }

    public void setAllowCarryover(boolean allowCarryover) {
        this.allowCarryover = allowCarryover;
    }

    public boolean isOptimizeToCompleteToday() {
        return optimizeToCompleteToday;
    }

    public void setOptimizeToCompleteToday(boolean optimizeToCompleteToday) {
        this.optimizeToCompleteToday = optimizeToCompleteToday;
    }

    public boolean isUseAssignedTechsFirst() {
        return useAssignedTechsFirst;
    }

    public void setUseAssignedTechsFirst(boolean useAssignedTechsFirst) {
        this.useAssignedTechsFirst = useAssignedTechsFirst;
    }

    public boolean isScrapImpossible() {
        return scrapImpossible;
    }

    public void setScrapImpossible(boolean scrapImpossible) {
        this.scrapImpossible = scrapImpossible;
    }

    public boolean isReplacePodParts() {
        return replacePodParts;
    }

    public void setReplacePodParts(boolean replacePodParts) {
        this.replacePodParts = replacePodParts;
    }

    public List<MRMSOption> getMRMSOptions() {
        return mrmsOptions;
    }

    public void setMRMSOptions(final List<MRMSOption> mrmsOptions) {
        this.mrmsOptions = mrmsOptions;
    }

    public boolean isHActiveMRMSOption() {
        return hasActiveMRMSOption;
    }

    public void setHasActiveMRMSOption(boolean hasActiveMRMSOption) {
        this.hasActiveMRMSOption = hasActiveMRMSOption;
    }
    //endregion Getters/Setters

    public List<MRMSOption> getActiveMRMSOptions() {
        return isHActiveMRMSOption()
                ? getMRMSOptions().stream().filter(MRMSOption::isActive).collect(Collectors.toList())
                : Collections.emptyList();
    }
}
