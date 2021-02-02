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
package mekhq.campaign.universe.enums;

import megamek.common.util.EncodeControl;

import java.util.ResourceBundle;

public enum Alphabet {
    //region Enum Declarations
    A("Alphabets.A.ccb1943.text", "Alphabets.A.icao1956.text", "Alphabets.A.greek.text"),
    B("Alphabets.B.ccb1943.text", "Alphabets.B.icao1956.text", "Alphabets.B.greek.text"),
    C("Alphabets.C.ccb1943.text", "Alphabets.C.icao1956.text", "Alphabets.C.greek.text"),
    D("Alphabets.D.ccb1943.text", "Alphabets.D.icao1956.text", "Alphabets.D.greek.text"),
    E("Alphabets.E.ccb1943.text", "Alphabets.E.icao1956.text", "Alphabets.E.greek.text"),
    F("Alphabets.F.ccb1943.text", "Alphabets.F.icao1956.text", "Alphabets.F.greek.text"),
    G("Alphabets.G.ccb1943.text", "Alphabets.G.icao1956.text", "Alphabets.G.greek.text"),
    H("Alphabets.H.ccb1943.text", "Alphabets.H.icao1956.text", "Alphabets.H.greek.text"),
    I("Alphabets.I.ccb1943.text", "Alphabets.I.icao1956.text", "Alphabets.I.greek.text"),
    J("Alphabets.J.ccb1943.text", "Alphabets.J.icao1956.text", "Alphabets.J.greek.text"),
    K("Alphabets.K.ccb1943.text", "Alphabets.K.icao1956.text", "Alphabets.K.greek.text"),
    L("Alphabets.L.ccb1943.text", "Alphabets.L.icao1956.text", "Alphabets.L.greek.text"),
    M("Alphabets.M.ccb1943.text", "Alphabets.M.icao1956.text", "Alphabets.M.greek.text"),
    N("Alphabets.N.ccb1943.text", "Alphabets.N.icao1956.text", "Alphabets.N.greek.text"),
    O("Alphabets.O.ccb1943.text", "Alphabets.O.icao1956.text", "Alphabets.O.greek.text"),
    P("Alphabets.P.ccb1943.text", "Alphabets.P.icao1956.text", "Alphabets.P.greek.text"),
    Q("Alphabets.Q.ccb1943.text", "Alphabets.Q.icao1956.text", "Alphabets.Q.greek.text"),
    R("Alphabets.R.ccb1943.text", "Alphabets.R.icao1956.text", "Alphabets.R.greek.text"),
    S("Alphabets.S.ccb1943.text", "Alphabets.S.icao1956.text", "Alphabets.S.greek.text"),
    T("Alphabets.T.ccb1943.text", "Alphabets.T.icao1956.text", "Alphabets.T.greek.text"),
    U("Alphabets.U.ccb1943.text", "Alphabets.U.icao1956.text", "Alphabets.U.greek.text"),
    V("Alphabets.V.ccb1943.text", "Alphabets.V.icao1956.text", "Alphabets.V.greek.text"),
    W("Alphabets.W.ccb1943.text", "Alphabets.W.icao1956.text", "Alphabets.W.greek.text"),
    X("Alphabets.X.ccb1943.text", "Alphabets.X.icao1956.text", "Alphabets.X.greek.text"),
    Y("Alphabets.Y.ccb1943.text", "Alphabets.Y.icao1956.text", "Alphabets.Y.greek.text"),
    Z("Alphabets.Z.ccb1943.text", "Alphabets.Z.icao1956.text", "Alphabets.Z.greek.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String ccb1943; // CCB 1943 Military Phonetic Alphabet
    private final String icao1956; // ICAO 1956 Military Phonetic Alphabet
    private final String greek; // Greek Alphabet

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    Alphabet(String ccb1943, String icao1956, String greek) {
        this.ccb1943 = resources.getString(ccb1943);
        this.icao1956 = resources.getString(icao1956);
        this.greek = resources.getString(greek);
    }
    //endregion Constructors

    //region Getters
    public String getCCB1943() {
        return ccb1943;
    }

    public String getICAO1956() {
        return icao1956;
    }

    public String getGreek() {
        return greek;
    }
    //endregion Getters
}
