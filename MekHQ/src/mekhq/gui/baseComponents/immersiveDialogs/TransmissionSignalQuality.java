/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.baseComponents.immersiveDialogs;

/** Defines the visual fidelity of an immersive dialog's video transmission. */
public enum TransmissionSignalQuality {
    CLEAR(0.04f, 5, 0.08f, 5, 0.12f, 48, 1, 2, 3, 2, 6, 0.30f,
          "ImmersiveDialog.header.status.clear"),
    REMOTE(0.14f, 3, 0.22f, 24, 0.32f, 12, 1, 4, 12, 4, 18, 0.65f,
          "ImmersiveDialog.header.status.remote"),
    DEGRADED(0.22f, 2, 0.30f, 42, 0.44f, 5, 2, 5, 20, 8, 32, 0.80f,
          "ImmersiveDialog.header.status.degraded");

    final float tintOpacity;
    final int scanlineGap;
    final float scanlineOpacity;
    final int noiseMarks;
    final float noiseOpacity;
    final int glitchInterval;
    final int glitchFrameMinimum;
    final int glitchFrameMaximum;
    final int glitchOffset;
    final int glitchHeightMinimum;
    final int glitchHeightMaximum;
    final float glitchLineOpacity;
    final String statusResourceKey;

    TransmissionSignalQuality(float tintOpacity, int scanlineGap, float scanlineOpacity, int noiseMarks,
          float noiseOpacity, int glitchInterval, int glitchFrameMinimum, int glitchFrameMaximum, int glitchOffset,
          int glitchHeightMinimum, int glitchHeightMaximum, float glitchLineOpacity, String statusResourceKey) {
        this.tintOpacity = tintOpacity;
        this.scanlineGap = scanlineGap;
        this.scanlineOpacity = scanlineOpacity;
        this.noiseMarks = noiseMarks;
        this.noiseOpacity = noiseOpacity;
        this.glitchInterval = glitchInterval;
        this.glitchFrameMinimum = glitchFrameMinimum;
        this.glitchFrameMaximum = glitchFrameMaximum;
        this.glitchOffset = glitchOffset;
        this.glitchHeightMinimum = glitchHeightMinimum;
        this.glitchHeightMaximum = glitchHeightMaximum;
        this.glitchLineOpacity = glitchLineOpacity;
        this.statusResourceKey = statusResourceKey;
    }
}
