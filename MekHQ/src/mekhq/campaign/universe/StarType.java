/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.codeUtilities.MathUtility;

import java.util.Locale;

/** A class to carry information about a star. Used in planetary system **/
public class StarType {

    // Star classification data and methods
    public static final int SPECTRAL_O = 0;
    public static final int SPECTRAL_B = 1;
    public static final int SPECTRAL_A = 2;
    public static final int SPECTRAL_F = 3;
    public static final int SPECTRAL_G = 4;
    public static final int SPECTRAL_K = 5;
    public static final int SPECTRAL_M = 6;
    public static final int SPECTRAL_L = 7;
    public static final int SPECTRAL_T = 8;
    public static final int SPECTRAL_Y = 9;
    // Spectral class "D" (white dwarfs) are determined by their luminosity "VII" -
    // the number is here for sorting
    public static final int SPECTRAL_D = 99;
    // "Q" - not a proper star (neutron stars QN, pulsars QP, black holes QB, ...)
    public static final int SPECTRAL_Q = 100;
    // TODO: Wolf-Rayet stars ("W"), carbon stars ("C"), S-type stars ("S"),

    public static final String LUM_0 = "0";
    public static final String LUM_IA = "Ia";
    public static final String LUM_IAB = "Iab";
    public static final String LUM_IB = "Ib";
    // Generic class, consisting of Ia, Iab and Ib
    public static final String LUM_I = "I";
    public static final String LUM_II_EVOLVED = "I/II";
    public static final String LUM_II = "II";
    public static final String LUM_III_EVOLVED = "II/III";
    public static final String LUM_III = "III";
    public static final String LUM_IV_EVOLVED = "III/IV";
    public static final String LUM_IV = "IV";
    public static final String LUM_V_EVOLVED = "IV/V";
    public static final String LUM_V = "V";
    // typically used as a prefix "sd", not as a suffix
    public static final String LUM_VI = "VI";
    // typically used as a prefix "esd", not as a suffix
    public static final String LUM_VI_PLUS = "VI+";
    // always used as class designation "D", never as a suffix
    public static final String LUM_VII = "VII";

    private int spectralClass;
    private double subtype;
    private String luminosity;

    public StarType() {
        // empty constructor
    }

    public StarType(int spectraClass, double subtype, String luminosity) {
        this.spectralClass = spectraClass;
        this.subtype = subtype;
        this.luminosity = luminosity;
    }

    public int getSpectralClass() {
        return spectralClass;
    }

    public void setSpectralClass(int c) {
        this.spectralClass = c;
    }

    public double getSubtype() {
        return subtype;
    }

    public void setSubType(double d) {
        this.subtype = d;
    }

    public String getLuminosity() {
        return luminosity;
    }

    public void setLuminosity(String s) {
        this.luminosity = s;
    }

    @Override
    public String toString() {
        if (spectralClass == StarType.SPECTRAL_Q) {
            return (null != luminosity) ? "Q" + luminosity : "Q";
        }

        // Formatting subtype value up to two decimal points, if needed
        int subtypeValue = MathUtility.clamp((int) Math.round(subtype * 100d), 0, 999);

        String subtypeFormat = "%.2f";
        if (subtypeValue % 100 == 0) {
            subtypeFormat = "%.0f";
        } else if (subtypeValue % 10 == 0) {
            subtypeFormat = "%.1f";
        }

        if(null == luminosity) {
            // assume mid-range luminosity
            return String.format(Locale.ROOT, "%s" + subtypeFormat + "%s",
                getSpectralClassName(spectralClass),
                subtypeValue / 100.0, StarType.LUM_V);
        }

        switch(luminosity) {
            case StarType.LUM_VI:
                // subdwarfs
                return "sd" + getSpectralClassName(spectralClass) + String.format(subtypeFormat, subtypeValue / 100.0);
            case StarType.LUM_VI_PLUS:
                // extreme subdwarfs
                return "esd" + getSpectralClassName(spectralClass) + String.format(subtypeFormat, subtypeValue / 100.0);
            case StarType.LUM_VII:
                // white dwarfs
                return String.format(Locale.ROOT, "D" + subtypeFormat, subtypeValue / 100.0);
            default:
                return String.format(Locale.ROOT, "%s" + subtypeFormat + "%s",
                    getSpectralClassName(spectralClass),
                    subtypeValue / 100.0,  luminosity);

        }
    }

    /** Parser for spectral type strings */
    public void setValuesFromString(String type) {
        if ((null == type) || type.isEmpty()) {
            return;
        }

        // We make sure to not rewrite the subtype, in case we need whatever special
        // part is behind it
        Integer parsedSpectralClass = null;
        Double parsedSubtype = null;
        String parsedLuminosity = null;

        // Non-stellar objects
        if (type.startsWith("Q")) {
            spectralClass = SPECTRAL_Q;
            subtype = 0.0;
            luminosity = type.substring(1);
            return;
        }

        // Subdwarf prefix parsing
        if ((type.length() > 2) && type.startsWith("sd")) {
            // subdwarf
            parsedLuminosity = LUM_VI;
            type = type.substring(2);
        } else if ((type.length() > 3) && type.startsWith("esd")) {
            // extreme subdwarf
            parsedLuminosity = LUM_VI_PLUS;
            type = type.substring(3);
        }

        if (type.length() < 1) {
            // We can't parse an empty string
            return;
        }
        String mainClass = type.substring(0, 1);

        if (mainClass.equals("D") && type.length() > 1 && null == parsedLuminosity /* prevent "sdD..." */) {
            // white dwarf
            parsedLuminosity = LUM_VII;
            String whiteDwarfVariant = type.substring(1).replaceAll("([A-Z]*).*?$", "$1");
            if (!StarUtil.VALID_WHITE_DWARF_SUBCLASSES.contains(whiteDwarfVariant)) {
                // Don't just make up D-class variants, that's silly ...
                return;
            }
            String subTypeString = type.substring(1 + whiteDwarfVariant.length()).replaceAll("^([0-9\\.]*).*?$", "$1");
            try {
                parsedSubtype = Double.parseDouble(subTypeString);
            } catch (NumberFormatException nfex) {
                return;
            }
            // We're done here, white dwarfs have a special spectral class
            parsedSpectralClass = SPECTRAL_D;
        } else if (getSpectralClassFrom(mainClass) >= 0) {
            parsedSpectralClass = getSpectralClassFrom(mainClass);
            String subTypeString = type.length() > 1 ? type.substring(1).replaceAll("^([0-9\\.]*).*?$", "$1") : "5" /*
             * default
             */;
            try {
                parsedSubtype = Double.parseDouble(subTypeString);
            } catch (NumberFormatException nfex) {
                return;
            }
            if (type.length() > 1 + subTypeString.length() && null == parsedLuminosity) {
                // We might have a luminosity, try to parse it
                parsedLuminosity = validateLuminosity(type.substring(1 + subTypeString.length()));
            }
        }

        if (null != parsedSpectralClass && null != parsedLuminosity) {
            spectralClass = parsedSpectralClass;
            subtype = parsedSubtype;
            luminosity = parsedLuminosity;
        }
    }

    public double getDistanceToJumpPoint() {
        int spectralTypeNumber = spectralClass * 10 + (int) subtype;
        double remainder = subtype - (int) subtype;
        return MathUtility.lerp(StarUtil.getDistanceToJumpPoint(spectralTypeNumber),
            StarUtil.getDistanceToJumpPoint(spectralTypeNumber),
            remainder);
    }

    public double getSolarRechargeTime() {
        if (spectralClass == SPECTRAL_Q) {
            // Not a star, can't recharge here
            return Double.POSITIVE_INFINITY;
        }
        int intSubtype = (int) subtype;
        if (spectralClass == SPECTRAL_T) {
            // months!
            return MathUtility.lerp(StarUtil.RECHARGE_HOURS_CLASS_T[intSubtype], StarUtil.RECHARGE_HOURS_CLASS_T[intSubtype + 1],
                subtype - intSubtype);
        } else if (spectralClass == SPECTRAL_L) {
            // weeks!
            return MathUtility.lerp(StarUtil.RECHARGE_HOURS_CLASS_L[intSubtype], StarUtil.RECHARGE_HOURS_CLASS_L[intSubtype + 1],
                subtype - intSubtype);
        } else {
            return 141 + 10 * spectralClass + subtype;
        }
    }

    public String getIcon() {
        return switch (spectralClass) {
            case SPECTRAL_B -> "B_" + luminosity;
            case SPECTRAL_A -> "A_" + luminosity;
            case SPECTRAL_F -> "F_" + luminosity;
            case SPECTRAL_G -> "G_" + luminosity;
            case SPECTRAL_K -> "K_" + luminosity;
            case SPECTRAL_M -> "M_" + luminosity;
            default -> "default";
        };
    }


    public static int getSpectralClassFrom(String spectral) {
        switch (spectral.trim().toUpperCase(Locale.ROOT)) {
            case "O":
                return SPECTRAL_O;
            case "B":
                return SPECTRAL_B;
            case "A":
                return SPECTRAL_A;
            case "F":
                return SPECTRAL_F;
            case "G":
                return SPECTRAL_G;
            case "K":
                return SPECTRAL_K;
            case "M":
                return SPECTRAL_M;
            case "L":
                return SPECTRAL_L;
            case "T":
                return SPECTRAL_T;
            case "Y":
                return SPECTRAL_Y;
            default:
                return -1;
        }
    }

    public static String getSpectralClassName(int spectral) {
        switch (spectral) {
            case StarType.SPECTRAL_O:
                return "O";
            case StarType.SPECTRAL_B:
                return "B";
            case StarType.SPECTRAL_A:
                return "A";
            case StarType.SPECTRAL_F:
                return "F";
            case StarType.SPECTRAL_G:
                return "G";
            case StarType.SPECTRAL_K:
                return "K";
            case StarType.SPECTRAL_M:
                return "M";
            case StarType.SPECTRAL_L:
                return "L";
            case StarType.SPECTRAL_T:
                return "T";
            case StarType.SPECTRAL_Y:
                return "Y";
            default:
                return "?";
        }
    }

    /**
     * @param lc string which starts with some luminosity description
     * @return the canonical luminosity string based on how this string starts, or
     *         <i>null</i> if it doesn't look like luminosity
     */
    private static String validateLuminosity(String lc) {
        // The order of entries here is important
        if (lc.startsWith("I/II")) {
            return LUM_II_EVOLVED;
        }
        if (lc.startsWith("I-II")) {
            return LUM_II_EVOLVED;
        }
        if (lc.startsWith("Ib/II")) {
            return LUM_II_EVOLVED;
        }
        if (lc.startsWith("Ib-II")) {
            return LUM_II_EVOLVED;
        }
        if (lc.startsWith("II/III")) {
            return LUM_III_EVOLVED;
        }
        if (lc.startsWith("II-III")) {
            return LUM_III_EVOLVED;
        }
        if (lc.startsWith("III/IV")) {
            return LUM_IV_EVOLVED;
        }
        if (lc.startsWith("III-IV")) {
            return LUM_IV_EVOLVED;
        }
        if (lc.startsWith("IV/V")) {
            return LUM_V_EVOLVED;
        }
        if (lc.startsWith("IV-V")) {
            return LUM_V_EVOLVED;
        }
        if (lc.startsWith("III")) {
            return LUM_III;
        }
        if (lc.startsWith("II")) {
            return LUM_II;
        }
        if (lc.startsWith("IV")) {
            return LUM_IV;
        }
        if (lc.startsWith("Ia-0")) {
            return LUM_0;
        } // Alias
        if (lc.startsWith("Ia0")) {
            return LUM_0;
        } // Alias
        if (lc.startsWith("Ia+")) {
            return LUM_0;
        } // Alias
        if (lc.startsWith("Iab")) {
            return LUM_IAB;
        }
        if (lc.startsWith("Ia")) {
            return LUM_IA;
        }
        if (lc.startsWith("Ib")) {
            return LUM_IB;
        }
        if (lc.startsWith("I")) {
            return LUM_I;
        } // includes Ia, Iab and Ib
        if (lc.startsWith("O")) {
            return LUM_0;
        }
        if (lc.startsWith("VII")) {
            return LUM_VII;
        }
        if (lc.startsWith("VI+")) {
            return LUM_VI_PLUS;
        }
        if (lc.startsWith("VI")) {
            return LUM_VI;
        }
        if (lc.startsWith("V")) {
            return LUM_V;
        }
        return null;
    }

    public static class StarTypeDeserializer extends StdDeserializer<StarType> {

        public StarTypeDeserializer() {
            this(null);
        }

        public StarTypeDeserializer(final Class<?> vc) {
            super(vc);
        }

        @Override
        public StarType deserialize(final JsonParser jsonParser, final DeserializationContext context) {
            try {
                StarType star = new StarType();
                star.setValuesFromString(jsonParser.getText());
                return star;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

}
