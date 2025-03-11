/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel;

import megamek.common.annotations.Nullable;
import megamek.common.options.*;
import megamek.logging.MMLogger;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/**
 * An extension of PilotOptions that adds MekHQ-specific SPAs and edge triggers
 * for support and command
 * actions. Display names and descriptions are taken from SpecialAbility when
 * present, otherwise
 * from the MM option.
 *
 * @author Neoancient
 */
public class PersonnelOptions extends PilotOptions {
    private static final MMLogger logger = MMLogger.create(PersonnelOptions.class);

    public static final String EDGE_MEDICAL = "edge_when_heal_crit_fail";
    public static final String EDGE_REPAIR_BREAK_PART = "edge_when_repair_break_part";
    public static final String EDGE_REPAIR_FAILED_REFIT = "edge_when_fail_refit_check";
    public static final String EDGE_ADMIN_ACQUIRE_FAIL = "edge_when_admin_acquire_fail";

    public static final String TECH_WEAPON_SPECIALIST = "tech_weapon_specialist";
    public static final String TECH_ARMOR_SPECIALIST = "tech_armor_specialist";
    public static final String TECH_INTERNAL_SPECIALIST = "tech_internal_specialist";
    public static final String TECH_ENGINEER = "tech_engineer";
    public static final String TECH_FIXER = "tech_fixer";
    public static final String TECH_MAINTAINER = "tech_maintainer";

    @Override
    public void initialize() {
        super.initialize();

        IBasicOptionGroup l3a = null;
        IBasicOptionGroup edge = null;
        IBasicOptionGroup md = null;
        for (Enumeration<IBasicOptionGroup> e = getOptionsInfoImp().getGroups(); e.hasMoreElements();) {
            final IBasicOptionGroup group = e.nextElement();
            if ((null == l3a) && group.getKey().equals(PilotOptions.LVL3_ADVANTAGES)) {
                l3a = group;
            } else if ((null == edge) && group.getKey().equals(PilotOptions.EDGE_ADVANTAGES)) {
                edge = group;
            } else if ((null == md) && group.getKey().equals(PilotOptions.MD_ADVANTAGES)) {
                md = group;
            }
        }

        if (null == l3a) {
            // This really shouldn't happen.
            logger.warn("Could not find L3Advantage group");
            l3a = addGroup("adv", PilotOptions.LVL3_ADVANTAGES);
        }
        if (null == edge) {
            // This really shouldn't happen.
            logger.warn("Could not find edge group");
            edge = addGroup("edge", PilotOptions.EDGE_ADVANTAGES);
            addOption(edge, OptionsConstants.EDGE, 0);
        }
        if (null == md) {
            // This really shouldn't happen.
            logger.warn("Could not find augmentation (MD) group");
            md = addGroup("md", PilotOptions.MD_ADVANTAGES);
        }

        // Add MekHQ-specific options
        addOption(l3a, TECH_WEAPON_SPECIALIST, false);
        addOption(l3a, TECH_ARMOR_SPECIALIST, false);
        addOption(l3a, TECH_INTERNAL_SPECIALIST, false);
        addOption(l3a, TECH_ENGINEER, false);
        addOption(l3a, TECH_FIXER, false);
        addOption(l3a, TECH_MAINTAINER, false);

        addOption(edge, EDGE_MEDICAL, true);
        addOption(edge, EDGE_REPAIR_BREAK_PART, true);
        addOption(edge, EDGE_REPAIR_FAILED_REFIT, true);
        addOption(edge, EDGE_ADMIN_ACQUIRE_FAIL, true);

        List<CustomOption> customs = CustomOption.getCustomAbilities();
        for (CustomOption option : customs) {
            switch (option.getGroup()) {
                case PilotOptions.LVL3_ADVANTAGES:
                    addOption(l3a, option.getName(), option.getType(), option.getDefault());
                    break;
                case PilotOptions.EDGE_ADVANTAGES:
                    addOption(edge, option.getName(), option.getType(), option.getDefault());
                    break;
                case PilotOptions.MD_ADVANTAGES:
                    addOption(md, option.getName(), option.getType(), option.getDefault());
                    break;
                default:
                    throw new IllegalStateException(
                            "Unexpected value in mekhq/campaign/personnel/PersonnelOptions.java/initialize: "
                                    + option.getGroup());
            }
        }
    }

    /*
     * When an option is added we need to create a custom IOptionInfo instance so we
     * can
     * provide a different source for display name and description.
     */
    @Override
    protected void addOption(IBasicOptionGroup group, String name, int type, Object defaultValue) {
        super.addOption(group, name, type, defaultValue);
        ((PersonnelOptionsInfo) getOptionsInfoImp()).setOptionInfo(name);
    }

    /**
     * Returns the options of the given category that this pilot has
     */
    public Enumeration<IOption> getOptions(String grpKey) {
        for (Enumeration<IOptionGroup> i = getGroups(); i.hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            if (group.getKey().equalsIgnoreCase(grpKey)) {
                return group.getOptions();
            }
        }

        // no pilot advantages -- return an empty Enumeration
        return new Vector<IOption>().elements();
    }

    public void acquireAbility(final String type, final String name, final @Nullable Object value) {
        if (value == null) {
            return;
        }
        // we might also need to remove some prior abilities
        SpecialAbility spa = SpecialAbility.getAbility(name);
        Vector<String> toRemove = new Vector<>();
        if (null != spa) {
            toRemove = spa.getRemovedAbilities();
        }
        for (Enumeration<IOption> i = getOptions(type); i.hasMoreElements();) {
            IOption ability = i.nextElement();
            if (ability.getName().equals(name)) {
                ability.setValue(value);
            } else {
                for (String remove : toRemove) {
                    if (ability.getName().equals(remove)) {
                        ability.setValue(ability.getDefault());
                    }
                }
            }
        }
    }

    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return PersonnelOptionsInfo.getInstance();
    }

    /**
     * Custom IOptionsInfo class that allows adding additional options to the base
     * MegaMek
     * options before finalizing and also holds a hash of IOptionInfo objects for
     * the abilities
     * so we can provide names and descriptions for the MekHQ-specific options.
     *
     * @author Neoancient
     */
    private static class PersonnelOptionsInfo extends AbstractOptionsInfo {
        private static boolean initialized = false;
        private static AbstractOptionsInfo instance = new PersonnelOptionsInfo();

        private Hashtable<String, IOptionInfo> optionsHash = new Hashtable<>();

        public static AbstractOptionsInfo getInstance() {
            if (!initialized) {
                initialized = true;
                // Create a new dummy PilotOptions; ensures values initialized
                // Otherwise, could have issues when loading saved games
                new PersonnelOptions();
            }
            return instance;
        }

        protected PersonnelOptionsInfo() {
            super("PersonnelOptionsInfo");
        }

        @Override
        public IOptionInfo getOptionInfo(String name) {
            return optionsHash.get(name);
        }

        private void setOptionInfo(String name) {
            optionsHash.put(name, new PersonnelOptionInfo(name));
        }
    }

    /**
     * Access to ability names and descriptions from <code>SpecialAbility</code> if
     * the ability
     * has an entry, otherwise checks for the ability the MM PilotOptions class. If
     * not found
     * in either place, returns the lookup key instead.
     *
     * @author Neoancient
     */
    private static class PersonnelOptionInfo implements IOptionInfo {
        private String name;
        private static PilotOptions mmOptions = new PilotOptions();

        public PersonnelOptionInfo(String name) {
            this.name = name;
        }

        @Override
        public String getDisplayableName() {
            SpecialAbility spa = SpecialAbility.getOption(name);
            if (null != spa) {
                return spa.getDisplayName();
            } else if (null != mmOptions.getOption(name)) {
                return mmOptions.getOption(name).getDisplayableName();
            } else {
                return name;
            }
        }

        @Override
        public String getDisplayableNameWithValue() {
            SpecialAbility spa = SpecialAbility.getOption(name);
            if (null != spa) {
                return spa.getDisplayName();
            } else if (null != mmOptions.getOption(name)) {
                return mmOptions.getOption(name).getDisplayableName();
            } else {
                return name;
            }
        }

        @Override
        public String getDescription() {
            SpecialAbility spa = SpecialAbility.getOption(name);
            if (null != spa) {
                return spa.getDescription();
            } else if (null != mmOptions.getOption(name)) {
                return mmOptions.getOption(name).getDescription();
            } else {
                return name;
            }
        }

        @Override
        public int getTextFieldLength() {
            return 3;
        }

        @Override
        public boolean isLabelBeforeTextField() {
            return false;
        }
    }
}
