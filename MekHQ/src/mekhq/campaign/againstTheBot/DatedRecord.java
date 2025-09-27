/*
 * Copyright (c) 2014 - Carl Spain. All rights reserved.
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.againstTheBot;

import java.time.LocalDate;

/*
 * Attaches a start and end date to any object.
 * Either the start or end date can be null, indicating that
 * the value should apply to all dates from the beginning
 * or to the end of the epoch, respectively.
 */
class DatedRecord<E> {
    private LocalDate start;
    private LocalDate end;
    private E value;

    public DatedRecord(LocalDate start, LocalDate end, E value) {
        this.start = start;
        this.end = end;
        this.value = value;
    }

    public void setStart(LocalDate start) {
        this.start = start;
    }

    public LocalDate getStart() {
        return start;
    }

    public void setEnd(LocalDate end) {
        this.end = end;
    }

    public LocalDate getEnd() {
        return end;
    }

    public void setValue(E v) {
        value = v;
    }

    public E getValue() {
        return value;
    }

    /**
     * @param d date to check
     *
     * @return true if d is between the start and end date, inclusive
     */
    public boolean fitsDate(LocalDate d) {
        return ((start == null) || !start.isAfter(d))
                     && ((end == null) || !end.isBefore(d));
    }
}
