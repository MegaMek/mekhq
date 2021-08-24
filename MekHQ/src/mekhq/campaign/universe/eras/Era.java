/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.eras;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.universe.enums.EraFlag;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class Era {
    //region Variable Declarations
    private String code;
    private String name;
    private LocalDate end;
    private Set<EraFlag> flags;
    //endregion Variable Declarations

    //region Constructors
    public Era() {
        setCode("???");
        setName("");
        setEnd(LocalDate.ofYearDay(9999, 1));
        setFlags(new HashSet<>());
    }
    //endregion Constructors

    //region Getters/Setters
    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public LocalDate getEnd() {
        return end;
    }

    public void setEnd(final LocalDate end) {
        this.end = end;
    }

    public Set<EraFlag> getFlags() {
        return flags;
    }

    public void setFlags(final Set<EraFlag> flags) {
        this.flags = flags;
    }
    //endregion Getters/Setters

    public boolean hasFlag(final EraFlag... flags) {
        return Stream.of(flags).anyMatch(getFlags()::contains);
    }

    //region File I/O
    public static @Nullable Era generateInstanceFromXML(final NodeList nl) {
        final Era era = new Era();

        for (int x = 0; x < nl.getLength(); x++) {
            try {
                final Node wn = nl.item(x);
                switch (wn.getNodeName()) {
                    case "code":
                        era.setCode(MekHqXmlUtil.unEscape(wn.getTextContent().trim()));
                        break;
                    case "name":
                        era.setName(MekHqXmlUtil.unEscape(wn.getTextContent().trim()));
                        break;
                    case "end":
                        era.setEnd(MekHqXmlUtil.parseDate(wn.getTextContent().trim()));
                        break;
                    case "flag":
                        era.getFlags().add(EraFlag.valueOf(wn.getTextContent().trim()));
                        break;
                }
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
                return null;
            }
        }

        return era.getCode().equals("???") ? null : era;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(final @Nullable Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof Era)) {
            return false;
        } else {
            return getCode().equals(((Era) object).getCode());
        }
    }

    @Override
    public int hashCode() {
        return getCode().hashCode();
    }
}
