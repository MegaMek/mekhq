/*
 * MekHQ - Copyright (C) 2017 - The MegaMek Team
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package mekhq.campaign.personnel;

import java.util.Enumeration;

import megamek.common.logging.LogLevel;
import megamek.common.options.AbstractOptionsInfo;
import megamek.common.options.IBasicOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import mekhq.MekHQ;

/**
 * An extension of PilotOptions that adds MekHQ-specific SPAs and edge triggers for support and command
 * actions.
 * 
 * @author Neoancient
 *
 */
public class PersonnelOptions extends PilotOptions {
    
    /**
     * 
     */
    private static final long serialVersionUID = -4376899952366335620L;
    
    public static final String EDGE_MEDICAL = "edge_when_heal_crit_fail";
    public static final String EDGE_REPAIR_BREAK_PART = "edge_when_repair_break_part";
    public static final String EDGE_REPAIR_FAILED_REFIT = "edge_when_fail_refit_check";
    public static final String EDGE_ADMIN_ACQUIRE_FAIL = "edge_when_admin_acquire_fail";
    public static final String EDGE_RETENTION_FAILURE = "edge_when_retention_fail";
    
    @Override
    public void initialize() {
        final String METHOD_NAME = "initialize()"; //$NON-NLS-1$
        super.initialize();

        IBasicOptionGroup edge = null;
        for (Enumeration<IBasicOptionGroup> e = getOptionsInfoImp().getGroups(); e.hasMoreElements(); ) {
            final IBasicOptionGroup group = e.nextElement();
            if (group.getKey().equals(PilotOptions.EDGE_ADVANTAGES)) {
                edge = group;
                break;
            }
        }
        
        if (null == edge) {
            // This really shouldn't happen.
            MekHQ.getLogger().log(PersonnelOptions.class, METHOD_NAME,
                    LogLevel.WARNING, "Could not find edge group"); //$NON-NLS-1$
            edge = addGroup("edge", PilotOptions.EDGE_ADVANTAGES); // $NON-NLS-1$
            addOption(edge, OptionsConstants.EDGE, 0);
        }
        
        // Add MekHQ-specific options
        addOption(edge, EDGE_MEDICAL, false);
        addOption(edge, EDGE_REPAIR_BREAK_PART, false);
        addOption(edge, EDGE_REPAIR_FAILED_REFIT, false);
        addOption(edge, EDGE_ADMIN_ACQUIRE_FAIL, false);
        addOption(edge, EDGE_RETENTION_FAILURE, false);
    }

    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return PersonnelOptionsInfo.getInstance();
    }

    private static class PersonnelOptionsInfo extends AbstractOptionsInfo {
        private static boolean initialized = false;
        private static AbstractOptionsInfo instance = new PersonnelOptionsInfo();

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
            super("PersonnelOptionsInfo"); //$NON-NLS-1$
        }
    }
}
