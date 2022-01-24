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
import mekhq.MekHQ;

import java.util.ResourceBundle;

/**
 * @author Justin "Windchild" Bowen
 */
public enum Alphabet {
    //region Enum Declarations
    A("Alphabets.A.ccb1943.text", "Alphabets.A.icao1956.text", "Alphabets.A.english.text", "Alphabets.A.greek.text"),
    B("Alphabets.B.ccb1943.text", "Alphabets.B.icao1956.text", "Alphabets.B.english.text", "Alphabets.B.greek.text"),
    C("Alphabets.C.ccb1943.text", "Alphabets.C.icao1956.text", "Alphabets.C.english.text", "Alphabets.C.greek.text"),
    D("Alphabets.D.ccb1943.text", "Alphabets.D.icao1956.text", "Alphabets.D.english.text", "Alphabets.D.greek.text"),
    E("Alphabets.E.ccb1943.text", "Alphabets.E.icao1956.text", "Alphabets.E.english.text", "Alphabets.E.greek.text"),
    F("Alphabets.F.ccb1943.text", "Alphabets.F.icao1956.text", "Alphabets.F.english.text", "Alphabets.F.greek.text"),
    G("Alphabets.G.ccb1943.text", "Alphabets.G.icao1956.text", "Alphabets.G.english.text", "Alphabets.G.greek.text"),
    H("Alphabets.H.ccb1943.text", "Alphabets.H.icao1956.text", "Alphabets.H.english.text", "Alphabets.H.greek.text"),
    I("Alphabets.I.ccb1943.text", "Alphabets.I.icao1956.text", "Alphabets.I.english.text", "Alphabets.I.greek.text"),
    J("Alphabets.J.ccb1943.text", "Alphabets.J.icao1956.text", "Alphabets.J.english.text", "Alphabets.J.greek.text"),
    K("Alphabets.K.ccb1943.text", "Alphabets.K.icao1956.text", "Alphabets.K.english.text", "Alphabets.K.greek.text"),
    L("Alphabets.L.ccb1943.text", "Alphabets.L.icao1956.text", "Alphabets.L.english.text", "Alphabets.L.greek.text"),
    M("Alphabets.M.ccb1943.text", "Alphabets.M.icao1956.text", "Alphabets.M.english.text", "Alphabets.M.greek.text"),
    N("Alphabets.N.ccb1943.text", "Alphabets.N.icao1956.text", "Alphabets.N.english.text", "Alphabets.N.greek.text"),
    O("Alphabets.O.ccb1943.text", "Alphabets.O.icao1956.text", "Alphabets.O.english.text", "Alphabets.O.greek.text"),
    P("Alphabets.P.ccb1943.text", "Alphabets.P.icao1956.text", "Alphabets.P.english.text", "Alphabets.P.greek.text"),
    Q("Alphabets.Q.ccb1943.text", "Alphabets.Q.icao1956.text", "Alphabets.Q.english.text", "Alphabets.Q.greek.text"),
    R("Alphabets.R.ccb1943.text", "Alphabets.R.icao1956.text", "Alphabets.R.english.text", "Alphabets.R.greek.text"),
    S("Alphabets.S.ccb1943.text", "Alphabets.S.icao1956.text", "Alphabets.S.english.text", "Alphabets.S.greek.text"),
    T("Alphabets.T.ccb1943.text", "Alphabets.T.icao1956.text", "Alphabets.T.english.text", "Alphabets.T.greek.text"),
    U("Alphabets.U.ccb1943.text", "Alphabets.U.icao1956.text", "Alphabets.U.english.text", "Alphabets.U.greek.text"),
    V("Alphabets.V.ccb1943.text", "Alphabets.V.icao1956.text", "Alphabets.V.english.text", "Alphabets.V.greek.text"),
    W("Alphabets.W.ccb1943.text", "Alphabets.W.icao1956.text", "Alphabets.W.english.text", "Alphabets.W.greek.text"),
    X("Alphabets.X.ccb1943.text", "Alphabets.X.icao1956.text", "Alphabets.X.english.text", "Alphabets.X.greek.text"),
    Y("Alphabets.Y.ccb1943.text", "Alphabets.Y.icao1956.text", "Alphabets.Y.english.text", "Alphabets.Y.greek.text"),
    Z("Alphabets.Z.ccb1943.text", "Alphabets.Z.icao1956.text", "Alphabets.Z.english.text", "Alphabets.Z.greek.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String ccb1943; // CCB 1943 Military Phonetic Alphabet
    private final String icao1956; // ICAO 1956 Military Phonetic Alphabet
    private final String english; // English Alphabet
    private final String greek; // Greek Alphabet
    //endregion Variable Declarations

    //region Constructors
    Alphabet(final String ccb1943, final String icao1956, final String english, final String greek) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe",
                MekHQ.getMHQOptions().getLocale(), new EncodeControl());
        this.ccb1943 = resources.getString(ccb1943);
        this.icao1956 = resources.getString(icao1956);
        this.english = resources.getString(english);
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

    public String getEnglish() {
        return english;
    }

    public String getGreek() {
        return greek;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isA() {
        return this == A;
    }

    public boolean isB() {
        return this == B;
    }

    public boolean isC() {
        return this == C;
    }

    public boolean isD() {
        return this == D;
    }

    public boolean isE() {
        return this == E;
    }

    public boolean isF() {
        return this == F;
    }

    public boolean isG() {
        return this == G;
    }

    public boolean isH() {
        return this == H;
    }

    public boolean isI() {
        return this == I;
    }

    public boolean isJ() {
        return this == J;
    }

    public boolean isK() {
        return this == K;
    }

    public boolean isL() {
        return this == L;
    }

    public boolean isM() {
        return this == M;
    }

    public boolean isN() {
        return this == N;
    }

    public boolean isO() {
        return this == O;
    }

    public boolean isP() {
        return this == P;
    }

    public boolean isQ() {
        return this == Q;
    }

    public boolean isR() {
        return this == R;
    }

    public boolean isS() {
        return this == S;
    }

    public boolean isT() {
        return this == T;
    }

    public boolean isU() {
        return this == U;
    }

    public boolean isV() {
        return this == V;
    }

    public boolean isW() {
        return this == W;
    }

    public boolean isX() {
        return this == X;
    }

    public boolean isY() {
        return this == Y;
    }

    public boolean isZ() {
        return this == Z;
    }
    //endregion Boolean Comparison Methods
}
