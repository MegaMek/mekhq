/*
 * Copyright (c) 2020 - The MekHQ Team. All Rights Reserved.
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
package mekhq;

import java.util.prefs.Preferences;

public final class MekHQOptions {
    private static final Preferences userPreferences = Preferences.userRoot();

    public boolean getNoAutosaveValue() {
        return userPreferences.node(MekHqConstants.AUTOSAVE_NODE).getBoolean(MekHqConstants.NO_SAVE_KEY, false);
    }

    public void setNoAutosaveValue(boolean value) {
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putBoolean(MekHqConstants.NO_SAVE_KEY, value);
    }

    public void initializeMekHQOptions() {
        //region Autosave
        setNoAutosaveValue(getNoAutosaveValue());
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putBoolean(
                MekHqConstants.SAVE_DAILY_KEY,
                userPreferences.node(MekHqConstants.AUTOSAVE_NODE)
                        .getBoolean(MekHqConstants.SAVE_DAILY_KEY, false));
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putBoolean(
                MekHqConstants.SAVE_WEEKLY_KEY,
                userPreferences.node(MekHqConstants.AUTOSAVE_NODE)
                        .getBoolean(MekHqConstants.SAVE_WEEKLY_KEY, true));
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putBoolean(
                MekHqConstants.SAVE_MONTHLY_KEY,
                userPreferences.node(MekHqConstants.AUTOSAVE_NODE)
                        .getBoolean(MekHqConstants.SAVE_MONTHLY_KEY, false));
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putBoolean(
                MekHqConstants.SAVE_YEARLY_KEY,
                userPreferences.node(MekHqConstants.AUTOSAVE_NODE)
                        .getBoolean(MekHqConstants.SAVE_YEARLY_KEY, false));
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putBoolean(
                MekHqConstants.SAVE_BEFORE_MISSIONS_KEY,
                userPreferences.node(MekHqConstants.AUTOSAVE_NODE)
                        .getBoolean(MekHqConstants.SAVE_BEFORE_MISSIONS_KEY, false));
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putInt(
                MekHqConstants.MAXIMUM_NUMBER_SAVES_KEY,
                userPreferences.node(MekHqConstants.AUTOSAVE_NODE)
                        .getInt(MekHqConstants.MAXIMUM_NUMBER_SAVES_KEY, MekHqConstants.DEFAULT_NUMBER_SAVES));
        //endregion Autosave
    }
}
