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

import mekhq.campaign.mission.contract.contractData.NegotiationStepMath.Term;

/**
 * The outcome of the one active-negotiation attempt a contract is allowed. Its presence on a contract records that the
 * attempt has been spent (so it cannot be repeated), and describes what happened.
 *
 * <p>Both outcomes come from the same opposed check and carry a {@code netMargin} (positive = the player prevailed,
 * negative = the employer did) plus a signed per-term change. What the change means depends on the {@link Kind}: for a
 * {@link Kind#HAGGLE} it is the number of meaningful steps the term moved (positive improved it, negative lowered it);
 * for a {@link Kind#EXCEPTION} it is {@code +1} if the term's non-negotiable status was waived, {@code -1} if it was
 * newly locked, {@code 0} if unchanged.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public record ActiveNegotiationData(Kind kind, int netMargin, int payDelta, int supportDelta, int transportDelta,
      int salvageDelta, int commandDelta) {

    /** Which stakes the re-negotiation played for: the terms' values, or their non-negotiable flags. */
    public enum Kind {
        HAGGLE, EXCEPTION
    }

    /** A haggle outcome: per-term change is the signed count of meaningful steps the term moved. */
    public static ActiveNegotiationData haggle(int netMargin, int payDelta, int supportDelta, int transportDelta,
          int salvageDelta, int commandDelta) {
        return new ActiveNegotiationData(Kind.HAGGLE, netMargin, payDelta, supportDelta, transportDelta, salvageDelta,
              commandDelta);
    }

    /** An exception outcome: per-term change is +1 (waived), -1 (newly locked), or 0 (unchanged). */
    public static ActiveNegotiationData exception(int netMargin, int payDelta, int supportDelta, int transportDelta,
          int salvageDelta, int commandDelta) {
        return new ActiveNegotiationData(Kind.EXCEPTION,
              netMargin,
              payDelta,
              supportDelta,
              transportDelta,
              salvageDelta,
              commandDelta);
    }

    /** The signed change applied to the given term (steps for a haggle, lock toggle for an exception). */
    public int deltaFor(Term term) {
        return switch (term) {
            case BASE_PAY -> payDelta;
            case SUPPORT -> supportDelta;
            case TRANSPORT -> transportDelta;
            case SALVAGE -> salvageDelta;
            case COMMAND_RIGHTS -> commandDelta;
        };
    }
}
