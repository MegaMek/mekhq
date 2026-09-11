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
package mekhq.campaign.mission.contract.contractData;

import mekhq.campaign.mission.contract.utilities.NegotiationStepMath.Term;

/**
 * Which of a contract's five terms the employer has locked as non-negotiable. A locked term is fixed at the value it
 * was generated with: the player can neither improve it nor sacrifice it in the negotiation dialog. Locks are rolled
 * once, at contract generation, and only bind the player's negotiation - generation sets them and a GM may still edit a
 * locked term in the contract editor.
 *
 * @author Illiani
 * @since 0.51.01
 */
public record NonNegotiableTermsData(boolean payLocked, boolean supportLocked, boolean transportLocked,
      boolean salvageLocked, boolean commandLocked) {

    /** A set of terms with nothing locked - the default when a contract carries no lock data (e.g. older saves). */
    public static NonNegotiableTermsData none() {
        return new NonNegotiableTermsData(false, false, false, false, false);
    }

    /** Whether the given term is locked. */
    public boolean isLocked(Term term) {
        return switch (term) {
            case BASE_PAY -> payLocked;
            case SUPPORT -> supportLocked;
            case TRANSPORT -> transportLocked;
            case SALVAGE -> salvageLocked;
            case COMMAND_RIGHTS -> commandLocked;
        };
    }

    /** Whether any term is locked. */
    public boolean anyLocked() {
        return payLocked || supportLocked || transportLocked || salvageLocked || commandLocked;
    }

    /** A copy with the given term's lock cleared. */
    public NonNegotiableTermsData withUnlocked(Term term) {
        return new NonNegotiableTermsData(payLocked && term != Term.BASE_PAY,
              supportLocked && term != Term.SUPPORT,
              transportLocked && term != Term.TRANSPORT,
              salvageLocked && term != Term.SALVAGE,
              commandLocked && term != Term.COMMAND_RIGHTS);
    }

    /** A copy with the given term locked. */
    public NonNegotiableTermsData withLocked(Term term) {
        return new NonNegotiableTermsData(payLocked || term == Term.BASE_PAY,
              supportLocked || term == Term.SUPPORT,
              transportLocked || term == Term.TRANSPORT,
              salvageLocked || term == Term.SALVAGE,
              commandLocked || term == Term.COMMAND_RIGHTS);
    }
}
