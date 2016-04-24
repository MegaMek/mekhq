/*
 * Copyright (C) 2016 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import mekhq.Utilities;
import mekhq.campaign.universe.Planet.SpectralDefinition;

/** Static method only helper class for stars */
public final class StarUtil {
    // A bunch of important astronomical constants
    /** Speed of light in a vacuum in km/s (only really important for non-hyperspace comms and sensors) */
    public static final double C = 299792.458;
    /** Julian year length in seconds (IAU standard astronomical year) */
    public static final double Y_JULIAN = 365.25 * 86400;
    /**
     * Light year in km (IAU standard as the speed of light times 365.25-day Julian Year,
     * also ISO 80000-3 Annex C
     */
    public static final double LY = C * Y_JULIAN;
    /** Astronomical unit in km (IAU standard, also ISO 80000-3 Annex C) */
    public static final double AU = 149597870.7;
    /** Standard gravity in m/s^2 (ISO 80000-3) */
    public static final double G = 9.80665;
    /** Solar luminosity in W */
    public static final double SOLAR_LUM = 3.846e26;
    /** Solar mass in kg */
    public static final double SOLAR_MASS = 1.98855e30;
    /** Solar radius in km */
    public static final double SOLAR_RADIUS = 695700.0;
    /** Effective solar temperature in K */
    public static final double SOLAR_TEMP = 5778.0;
    /** Gravitational constant (in m^3 kg^-1 s^-2 */
    public static final double GRAV_CONSTANT = 6.673848e-11;
    
    // Temperature, mass, luminosity and size are all linked together. When modifying those tables,
    // make VERY sure you don't accidentally break the inherent inequalities
    // (stars becoming smaller, dimmer, cooler and so on in one direction)
    
    // Temperature ranges for generation only. "Real" stars can lie outside of those.
    private static final int[] TEMPERATURE_RANGES = new int[]{
        65000, // Above class O
        57500, 53500, 50000, 46500, 43500, 40500, 37500, 34500, 31500, 30000, // Class O
        27000, 25000, 23000, 21000, 19500, 18000, 16500, 14000, 12000, 10000, // Class B
        9700, 9450, 9300, 9000, 8800, 8500, 8250, 8000, 7750, 7500, // Class A
        7350, 7200, 7050, 6900, 6800, 6650, 6500, 6350, 6200, 6000, // Class F
        5900, 5800, 5700, 5600, 5500, 5400, 5350, 5300, 5250, 5200, // Class G
        5100, 5000, 4900, 4800, 4650, 4500, 4300, 4100, 3900, 3700, // Class K
        3600, 3400, 3250, 3100, 2950, 2800, 2700, 2600, 2500, 2400, // Class M
        2290, 2180, 2070, 1960, 1850, 1740, 1630, 1520, 1410, 1300, // Class L
        1200, 1100, 1000, 900, 800, 700, 650, 600, 550, 500 // Class T
    };

    // Mass ranges in solar masses for generation purpose
    private static final double[] MIN_MASS = new double[] {
        2437.5, 1235, 837.5, 538.2, 371, 234.3, 151.2, 94.9, 57.72, 35.25, // Class O
        23.1, 15.6, 11.85, 8.08, 6.561, 5.494, 4.7891, 4.0338, 3.3012, 2.6628, // Class B
        2.091, 1.938, 1.8662, 1.7802, 1.74, 1.6965, 1.672, 1.584, 1.5575, 1.513, // Class A
        1.44, 1.395, 1.35, 1.305, 1.26, 1.215, 1.17, 1.125, 1.08, 1.035, // Class F
        0.99, 0.945, 0.9, 0.8775, 0.855, 0.8325, 0.81, 0.7875, 0.765, 0.7875, // Class G
        0.712, 0.68975, 0.66, 0.638, 0.609, 0.5655, 0.516, 0.4945, 0.4675, 0.44625, // Class K
        0.42, 0.378, 0.332, 0.2905, 0.2255, 0.164, 0.1215, 0.081, 0.072, 0.06, // Class M
        0.044, 0.036, 0.0296, 0.0248, 0.0216, 0.0176, 0.0152, 0.01275, 0.0126, 0.01183, // Class L
        0.01104, 0.010695, 0.01034, 0.01026, 0.010176, 0.010088, 0.009996, 0.0099, 0.009702, 0.009504 // Class T
    };
    
    private static final double[] MAX_MASS = new double[] {
        5062.5, 2565, 1662.5, 1021.8, 689, 425.7, 268.8, 165.1, 98.28, 58.75, // Class O
        36.9, 24.4, 18.15, 12.12, 9.639, 7.906, 6.7509, 5.6862, 4.5588, 3.6772, // Class B
        2.829, 2.622, 2.4738, 2.3598, 2.26, 2.2035, 2.128, 2.016, 1.9425, 1.887, // Class A
        1.76, 1.705, 1.65, 1.595, 1.54, 1.485, 1.43, 1.375, 1.32, 1.265, // Class F
        1.21, 1.155, 1.1, 1.0725, 1.045, 1.0175, 0.99, 0.9625, 0.935, 0.9625, // Class G
        0.888, 0.86025, 0.84, 0.812, 0.791, 0.7345, 0.684, 0.6555, 0.6325, 0.60375, // Class K
        0.58, 0.522, 0.468, 0.4095, 0.3245, 0.236, 0.1785, 0.119, 0.108, 0.09, // Class M
        0.066, 0.054, 0.0444, 0.0372, 0.0324, 0.0264, 0.0228, 0.01725, 0.0154, 0.01417, // Class L
        0.01296, 0.012305, 0.01166, 0.01134, 0.011024, 0.010712, 0.010404, 0.0101, 0.009898, 0.009696 // Class T 
    };
    
    // Average luminosity in terms of Solar luminosity. O-class stars are BRIGHT. Generated values are +/-10% of this.
    private static final double[] AVG_LUMINOSITY = new double[] {
        12000000, 6000000, 4000000, 2500000, 1700000, 1050000, 670000, 410000, 250000, 150000, // Class O
        96000, 64000, 19600, 4890, 2290, 1160, 692, 380, 180, 85, // Class B
        35, 27, 22.5, 19, 16, 13.8, 12, 10.6, 9.7, 8.85, // Class A
        7.5, 6.56, 5.8, 5.2, 4.4, 3.75, 3.13, 2.62, 2.41, 2.03, // Class F
        1.72, 1.46, 1.23, 1.15, 0.98, 0.84, 0.76, 0.68, 0.65, 0.59, // Class G
        0.543, 0.475, 0.41, 0.355, 0.31, 0.257, 0.211, 0.187, 0.155, 0.125, // Class K
        0.1, 0.0535, 0.0321, 0.0178, 0.0106, 0.0063, 0.0045, 0.0016, 0.0008, 0.0006, // Class M
        0.00025, 0.00017, 0.00012, 0.00008, 0.000055, 0.000035, 0.000025, 0.000015, 0.000009, 0.000006, // Class L
        0.0000035, 0.000002, 0.0000012, 0.0000007, 0.00000035, 0.00000018, 0.00000011, 0.00000007, 0.00000004, 0.000000025 // Class T          
    };
    
    //taken from Dropships and Jumpships sourcebook, pg. 17. L- and T-classes estimated
    private static final double[] DISTANCE_TO_JUMP_POINT = new double[]{
        Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
        Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
        347840509855.0, 282065439915.0, 229404075188.0, 187117766777.0, 153063985045.0, // Class B
        125563499718.0, 103287722257.0,  85198295036.0,  70467069133.0,  58438309136.0,
        48590182199.0, 40506291619.0, 33853487850.0, 28364525294.0, 23824470101.0, // Class A
        20060019532.0, 16931086050.0, 14324152109.0, 12147004515.0, 10324556364.0,
        8795520975.0, 7509758447.0, 6426154651.0, 5510915132.0, 4736208289.0, // Class F
        4079054583.0, 3520442982.0, 3044611112.0, 2638462416.0, 2291092549.0,
        1993403717.0, 1737789950.0, 1517879732.0, 1328325100.0, 1164628460.0, // Class G
        1023000099.0,  900240718.0,  793644393.0,  700918272.0,  620115976.0,
        549582283.0, 487907078.0, 433886958.0, 386493164.0, 344844735.0, // Class K
        308186014.0, 275867748.0, 247331200.0, 222094749.0, 199742590.0,
        179915179.0, 162301133.0, 146630374.0, 132668292.0, 120210786.0, // Class M
        109080037.0,  99120895.0,  90197803.0,  82192147.0,  75000000.0,
        64303323.0, 58164544.0, 52741556.0, 48276182.0, 45054062.0, // Class L
        40668992.0, 37794523.0, 33581315.0, 32442633.0, 31262503.0,
        30036042.0, 29403633.0, 28757320.0, 28494691.0, 28229618.0, // Class T
        27962033.0, 27691862.0, 27419029.0, 27143454.0, 26865052.0
    };
    
    // Slightly modified IO Beta table
    private static final int[] REALISTIC_SPECTRAL_TYPE = new int[]{
            Planet.SPECTRAL_F, Planet.SPECTRAL_M, Planet.SPECTRAL_G, Planet.SPECTRAL_K, Planet.SPECTRAL_M,
            Planet.SPECTRAL_M, Planet.SPECTRAL_M, Planet.SPECTRAL_M, Planet.SPECTRAL_M, Planet.SPECTRAL_L, -1};

    private static final int[] HOT_SPECTRAL_TYPE = new int[]{
        Planet.SPECTRAL_B, Planet.SPECTRAL_B, Planet.SPECTRAL_A, Planet.SPECTRAL_A, Planet.SPECTRAL_A,
        Planet.SPECTRAL_F, Planet.SPECTRAL_F, Planet.SPECTRAL_F, Planet.SPECTRAL_F, Planet.SPECTRAL_F, Planet.SPECTRAL_F};
    
    private static final int[] LIFEFRIENDLY_SPECTRAL_TYPE = new int[]{
        Planet.SPECTRAL_M, Planet.SPECTRAL_M, Planet.SPECTRAL_M, Planet.SPECTRAL_K, Planet.SPECTRAL_K,
        Planet.SPECTRAL_G, Planet.SPECTRAL_G, Planet.SPECTRAL_F, Planet.SPECTRAL_F, Planet.SPECTRAL_F, Planet.SPECTRAL_F};
    
    private static final double[] MIN_LIFE_ZONE = new double[]{
        Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
        Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
        18836034615.0, 13789104394.0, 9577962205.0, 6922924960.0, 4737540501.0, // Class B
         3371818500.0,  2604283395.0, 1989875373.0, 1438058066.0, 1079962499.0,
         812765280.0, 694412846.0, 621417251.0, 532211330.0, 476847145.0, // Class A
         408187457.0, 384701313.0, 345792134.0, 326849966.0, 294514601.0,
         278962256.0, 253563720.0, 241486956.0, 220038497.0, 210010714.0, // Class F
         191712676.0, 175148880.0, 160245499.0, 153689329.0, 141053288.0,
         129837283.0, 119622155.0, 98151248.0, 91688535.0, 86213444.0, // Class G
          82535447.0,  77425112.0, 74433863.0, 70141642.0, 66581180.0,
         63003696.0, 51915431.0, 43693947.0, 37332074.0, 32571422.0, // Class K
         28624229.0, 26182800.0, 24000141.0, 22440922.0, 21060769.0,
         19622213.0, 16407340.0, 13437355.0, 10606623.0, 8957198.0, // Class M
          7346411.0,  5735514.0,  4373667.0,  3208345.0, 2319138.0,
         2055095.0, 1833752.0, 1627530.0, 1435990.0, 1258696.0, // Class L
         1095210.0,  945094.0,  807910.0,  683220.0,  570588.0,
         477499.0, 393937.0, 319539.0, 253943.0, 196788.0, // Class T
         147711.0, 124816.0, 104182.0,  85718.0,  69334.0    
    };
    
    private static final double[] MAX_LIFE_ZONE = new double[]{
        Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
        Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
        38242858157.0, 27996060437.0, 19446165689.0, 14055635525.0, 9618642836.0, // Class B
         6845813319.0,  5287484468.0,  4040050000.0,  2919693648.0, 2192651135.0,
        1650159810.0, 1409868505.0, 1261665328.0, 1080550276.0, 968144204.0, // Class A
         828744231.0,  781060241.0,  702062818.0,  663604476.0, 597953886.0,
        566377913.0, 514811189.0, 490291699.0, 446744826.0, 426385389.0, // Class F
        389234826.0, 355605301.0, 325346923.0, 312035911.0, 286380918.0,
        263609029.0, 242869224.0, 199629657.0, 186594212.0, 175399766.0, // Class G
        167822075.0, 158854972.0, 150108291.0, 142701962.0, 135419349.0,
        128218049.0, 105827610.0, 89287631.0, 76400524.0, 65978008.0, // Class K
         58795714.0,  53743641.0, 49297586.0, 46062946.0, 42690748.0,
        39244426.0, 33187574.0, 27680951.0, 21613496.0, 18377700.0, // Class M
        15048294.0, 11772898.0,  8929569.0,  6594932.0,  4638276.0,
        4572412.0, 4079942.0, 3621114.0, 3194956.0, 2800492.0, // Class L
        2436749.0, 2102753.0, 1797530.0, 1520107.0, 1269509.0,
        1062395.0, 876476.0, 710946.0, 565001.0, 437836.0, // Class T
         328645.0, 277705.0, 231795.0, 190715.0, 154262.0
    };
        

    private static final int[] RECHARGE_HOURS_CLASS_L = new int[]{
            512, 616, 717, 901, 1142, 1462, 1767, 2325, 3617, 5038};

    private static final int[] RECHARGE_HOURS_CLASS_T = new int[]{
            7973, 13371, 21315, 35876, 70424, 134352, 215620, 32188, 569703, 892922};
    
    private static final Set<String> VALID_WHITE_DWARF_SUBCLASSES = new TreeSet<String>();
    static {
        VALID_WHITE_DWARF_SUBCLASSES.addAll(Arrays.asList("", //$NON-NLS-1$
            "A,B,O,Q,Z,AB,AO,AQ,AZ,BO,BQ,BZ,QZ,ABO,ABQ,ABZ,AOQ,AOZ,AQZ,BOQ," //$NON-NLS-1$
            + "BOZ,BQZ,OQZ,ABOQ,ABOZ,ABQZ,AOQZ,BOQZ,ABOQZ,C,X".split(","))); //$NON-NLS-1$ //$NON-NLS-2$
    }

    // Generators
    
    public static String generateSpectralType(Random rnd, boolean lifeFriendly) {
        return generateSpectralType(rnd, lifeFriendly, -1);
    }
    
    public static String generateSpectralType(Random rnd, boolean lifeFriendly, int spectralTypeOverride) {
        int spectralType = spectralTypeOverride;
        if(spectralTypeOverride < 0) {
            if( lifeFriendly ) {
                spectralType = LIFEFRIENDLY_SPECTRAL_TYPE[rnd.nextInt(6) + rnd.nextInt(6)];
            } else {
                spectralType = REALISTIC_SPECTRAL_TYPE[rnd.nextInt(6) + rnd.nextInt(6)];
                if( spectralType < 0 ) {
                    spectralType = HOT_SPECTRAL_TYPE[rnd.nextInt(6) + rnd.nextInt(6)];
                }
            }
        }
        // Slightly weighted towards the higher numbers
        int subType = (int)Math.floor(Utilities.lerp(0.0, 10.0, Math.pow(rnd.nextDouble(), 0.8)));
        return getSpectralType(spectralType, subType * 1.0, Planet.LUM_V);
    }

    public static double generateTemperature(Random rnd, int spectral, double subtype) {
        return Utilities.lerp(getMinTemperature(spectral, subtype), getMaxTemperature(spectral, subtype), rnd.nextDouble());
    }
    
    public static double generateMass(Random rnd, int spectral, double subtype) {
        return Utilities.lerp(getMinMass(spectral, subtype), getMaxMass(spectral, subtype), rnd.nextDouble());
    }

    public static double generateLuminosity(Random rnd, int spectral, double subtype) {
        return getAvgLuminosity(spectral, subtype) * (rnd.nextDouble() * 0.2 + 0.9);
    }

    // Temperature data
    
    private static double getTemperatureRange(int spectralTypeNumber) {
        if((spectralTypeNumber >= 0) && (spectralTypeNumber < TEMPERATURE_RANGES.length)) {
            return TEMPERATURE_RANGES[spectralTypeNumber];
        }
        return 0.0;
    }

    public static double getMinTemperature(int spectralTypeNumber) {
        return getTemperatureRange(spectralTypeNumber + 1);
    }

    public static double getMinTemperature(int spectral, double subtype) {
        int spectralTypeNumber = spectral * 10 + (int)subtype;
        double remainder = subtype - (int)subtype;
        return Utilities.lerp(getMinTemperature(spectralTypeNumber), getMinTemperature(spectralTypeNumber), remainder);
    }

    public static double getMaxTemperature(int spectralTypeNumber) {
        return getTemperatureRange(spectralTypeNumber);
    }


    public static double getMaxTemperature(int spectral, double subtype) {
        int spectralTypeNumber = spectral * 10 + (int)subtype;
        double remainder = subtype - (int)subtype;
        return Utilities.lerp(getMaxTemperature(spectralTypeNumber), getMaxTemperature(spectralTypeNumber), remainder);
    }


    // Mass data
    
    public static double getMinMass(int spectralTypeNumber) {
        if((spectralTypeNumber >= 0) && (spectralTypeNumber < MIN_MASS.length)) {
            return MIN_MASS[spectralTypeNumber];
        }
        return 0.0;
    }
    
    public static double getMinMass(int spectral, double subtype) {
        int spectralTypeNumber = spectral * 10 + (int)subtype;
        double remainder = subtype - (int)subtype;
        return Utilities.lerp(getMinMass(spectralTypeNumber), getMinMass(spectralTypeNumber), remainder);
    }

    public static double getMaxMass(int spectralTypeNumber) {
        if((spectralTypeNumber >= 0) && (spectralTypeNumber < MAX_MASS.length)) {
            return MAX_MASS[spectralTypeNumber];
        }
        return 0.0;
    }
    
    public static double getMaxMass(int spectral, double subtype) {
        int spectralTypeNumber = spectral * 10 + (int)subtype;
        double remainder = subtype - (int)subtype;
        return Utilities.lerp(getMaxMass(spectralTypeNumber), getMaxMass(spectralTypeNumber), remainder);
    }
    
    // Luminosity data
    
    public static double getAvgLuminosity(int spectralTypeNumber) {
        if((spectralTypeNumber >= 0) && (spectralTypeNumber < AVG_LUMINOSITY.length)) {
            return AVG_LUMINOSITY[spectralTypeNumber];
        }
        return 0.0;
    }
    
    public static double getAvgLuminosity(int spectral, double subtype) {
        int spectralTypeNumber = spectral * 10 + (int)subtype;
        double remainder = subtype - (int)subtype;
        return Utilities.lerp(getAvgLuminosity(spectralTypeNumber), getAvgLuminosity(spectralTypeNumber), remainder);
    }

    public static double getDistanceToJumpPoint(int spectralTypeNumber) {
        if((spectralTypeNumber >= 0) && (spectralTypeNumber < DISTANCE_TO_JUMP_POINT.length)) {
            return DISTANCE_TO_JUMP_POINT[spectralTypeNumber];
        }
        return 0.0;
    }

    /**
     * Distance to jump point given a spectral class and subtype measured in kilometers
     */
    public static double getDistanceToJumpPoint(int spectral, double subtype) {
        int spectralTypeNumber = spectral * 10 + (int)subtype;
        double remainder = subtype - (int)subtype;
        return Utilities.lerp(getDistanceToJumpPoint(spectralTypeNumber), getDistanceToJumpPoint(spectralTypeNumber), remainder);
    }

    public static double getMaxLifeZone(int spectralTypeNumber) {
        if((spectralTypeNumber >= 0) && (spectralTypeNumber < MAX_LIFE_ZONE.length)) {
            return MAX_LIFE_ZONE[spectralTypeNumber];
        }
        return 0.0;
    }

    public static double getMaxLifeZone(int spectral, double subtype) {
        int spectralTypeNumber = spectral * 10 + (int)subtype;
        double remainder = subtype - (int)subtype;
        return Utilities.lerp(getMaxLifeZone(spectralTypeNumber), getMaxLifeZone(spectralTypeNumber), remainder);
    }

    public static double getMinLifeZone(int spectralTypeNumber) {
        if((spectralTypeNumber >= 0) && (spectralTypeNumber < MIN_LIFE_ZONE.length)) {
            return MIN_LIFE_ZONE[spectralTypeNumber];
        }
        return 0.0;
    }

    public static double getMinLifeZone(int spectral, double subtype) {
        int spectralTypeNumber = spectral * 10 + (int)subtype;
        double remainder = subtype - (int)subtype;
        return Utilities.lerp(getMinLifeZone(spectralTypeNumber), getMinLifeZone(spectralTypeNumber), remainder);
    }

    public static Integer getSolarRechargeTime(int spectralClass, double subtype) {
        if(spectralClass == Planet.SPECTRAL_Q) {
            // Not a star, can't recharge here
            return null;
        }
        if(spectralClass == Planet.SPECTRAL_T) {
            // months!
            return RECHARGE_HOURS_CLASS_T[(int)subtype];
        } else if(spectralClass == Planet.SPECTRAL_L) {
            // weeks!
            return RECHARGE_HOURS_CLASS_L[(int)subtype];
        } else {
            return 141 + 10*spectralClass + (int)subtype;
        }
    }

    public static int getSpectralClassFrom(String spectral) {
        switch(spectral.trim().toUpperCase(Locale.ROOT)) {
            case "O": return Planet.SPECTRAL_O; //$NON-NLS-1$
            case "B": return Planet.SPECTRAL_B; //$NON-NLS-1$
            case "A": return Planet.SPECTRAL_A; //$NON-NLS-1$
            case "F": return Planet.SPECTRAL_F; //$NON-NLS-1$
            case "G": return Planet.SPECTRAL_G; //$NON-NLS-1$
            case "K": return Planet.SPECTRAL_K; //$NON-NLS-1$
            case "M": return Planet.SPECTRAL_M; //$NON-NLS-1$
            case "L": return Planet.SPECTRAL_L; //$NON-NLS-1$
            case "T": return Planet.SPECTRAL_T; //$NON-NLS-1$
            case "Y": return Planet.SPECTRAL_Y; //$NON-NLS-1$
            default: return -1;
        }
    }

    public static String getSpectralClassName(int spectral) {
        switch(spectral) {
            case Planet.SPECTRAL_O: return "O"; //$NON-NLS-1$
            case Planet.SPECTRAL_B: return "B"; //$NON-NLS-1$
            case Planet.SPECTRAL_A: return "A"; //$NON-NLS-1$
            case Planet.SPECTRAL_F: return "F"; //$NON-NLS-1$
            case Planet.SPECTRAL_G: return "G"; //$NON-NLS-1$
            case Planet.SPECTRAL_K: return "K"; //$NON-NLS-1$
            case Planet.SPECTRAL_M: return "M"; //$NON-NLS-1$
            case Planet.SPECTRAL_L: return "L"; //$NON-NLS-1$
            case Planet.SPECTRAL_T: return "T"; //$NON-NLS-1$
            case Planet.SPECTRAL_Y: return "Y"; //$NON-NLS-1$
            default: return "?"; //$NON-NLS-1$
        }
    }

    /** @return canonical name for the given combination of spectral class, subtype and luminosity */
    public static String getSpectralType(Integer spectralClass, Double subtype, String luminosity) {
        if( null == spectralClass || null == subtype ) {
            return null;
        }
        
        if(spectralClass == Planet.SPECTRAL_Q) {
            return (null != luminosity) ? "Q" + luminosity : "Q"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        // Formatting subtype value up to two decimal points, if needed
        int subtypeValue = (int)Math.round(subtype * 100);
        if( subtypeValue < 0 ) { subtypeValue = 0; }
        if( subtypeValue > 999 ) { subtypeValue = 999; }
        
        String subtypeFormat = "%.2f"; //$NON-NLS-1$
        if( subtypeValue % 100 == 0 ) { subtypeFormat = "%.0f"; } //$NON-NLS-1$
        else if( subtypeValue % 10 == 0 ) { subtypeFormat = "%.1f"; } //$NON-NLS-1$
        
        if( luminosity == Planet.LUM_VI ) {
            // subdwarfs
            return "sd" + getSpectralClassName(spectralClass) + String.format(subtypeFormat, subtypeValue / 100.0); //$NON-NLS-1$
        } else if( luminosity == Planet.LUM_VI_PLUS ) {
            // extreme subdwarfs
            return "esd" + getSpectralClassName(spectralClass) + String.format(subtypeFormat, subtypeValue / 100.0); //$NON-NLS-1$
        } else if( luminosity == Planet.LUM_VII ) {
            // white dwarfs
            return String.format(Locale.ROOT, "D" + subtypeFormat, subtypeValue / 100.0); //$NON-NLS-1$
        } else {
            // main class
            return String.format(Locale.ROOT, "%s" + subtypeFormat + "%s", //$NON-NLS-1$ //$NON-NLS-2$
                    getSpectralClassName(spectralClass),
                    subtypeValue / 100.0, (null != luminosity ? luminosity : Planet.LUM_V));
        }
    }

    /** Parser for spectral type strings */
    public static SpectralDefinition parseSpectralType(String type) {
        if((null == type) || type.isEmpty()) {
            return null;
        }
        
        // We make sure to not rewrite the subtype, in case we need whatever special part is behind it
        String parsedSpectralType = type;
        Integer parsedSpectralClass = null;
        Double parsedSubtype = null;
        String parsedLuminosity = null;
        
        // Non-stellar objects
        if(type.startsWith("Q")) {
            return new SpectralDefinition(type, Planet.SPECTRAL_Q, 0.0, type.substring(1));
        }
        
        // Subdwarf prefix parsing
        if( type.length() > 2 && type.startsWith("sd") ) { //$NON-NLS-1$
            // subdwarf
            parsedLuminosity = Planet.LUM_VI;
            type = type.substring(2);
        }
        else if( type.length() > 3 && type.startsWith("esd") ) { //$NON-NLS-1$
            // extreme subdwarf
            parsedLuminosity = Planet.LUM_VI_PLUS;
            type = type.substring(3);
        }
        
        if( type.length() < 1 ) {
            // We can't parse an empty string
            return null;
        }
        String mainClass = type.substring(0, 1);
        
        if( mainClass.equals("D") && type.length() > 1 && null == parsedLuminosity /* prevent "sdD..." */ ) { //$NON-NLS-1$
            // white dwarf
            parsedLuminosity = Planet.LUM_VII;
            String whiteDwarfVariant = type.substring(1).replaceAll("([A-Z]*).*?$", "$1"); //$NON-NLS-1$ //$NON-NLS-2$
            if( !VALID_WHITE_DWARF_SUBCLASSES.contains(whiteDwarfVariant) ) {
                // Don't just make up D-class variants, that's silly ...
                return null;
            }
            String subTypeString = type.substring(1 + whiteDwarfVariant.length()).replaceAll("^([0-9\\.]*).*?$", "$1"); //$NON-NLS-1$ //$NON-NLS-2$
            try {
                parsedSubtype = Double.parseDouble(subTypeString);
            } catch( NumberFormatException nfex ) {
                return null;
            }
            // We're done here, white dwarfs have a special spectral class
            parsedSpectralClass = Planet.SPECTRAL_D;
        } else if( getSpectralClassFrom(mainClass) >= 0 ) {
            parsedSpectralClass = getSpectralClassFrom(mainClass);
            String subTypeString = type.length() > 1 ? type.substring(1).replaceAll("^([0-9\\.]*).*?$", "$1") : "5" /* default */; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            try {
                parsedSubtype = Double.parseDouble(subTypeString);
            } catch( NumberFormatException nfex ) {
                return null;
            }
            if( type.length() > 1 + subTypeString.length() && null == parsedLuminosity ) {
                // We might have a luminosity, try to parse it
                parsedLuminosity = validateLuminosity(type.substring(1 + subTypeString.length()));
                if( null != parsedLuminosity && parsedLuminosity.equals(Planet.LUM_VII) ) {
                    // That's not how white dwarfs work
                    return null;
                }
            }
        }
        
        if( null != parsedSpectralClass && null != parsedSubtype && null != parsedLuminosity ) {
            return new SpectralDefinition(parsedSpectralType, parsedSpectralClass, parsedSubtype, parsedLuminosity);
        } else {
            return null;
        }
    }

    /**
     * @param lc string which starts with some luminosity description
     * @return the canonical luminosity string based on how this string starts, or <i>null</i> if it doesn't look like luminosity
     */
    protected static String validateLuminosity(String lc) {
        // The order of entries here is important
        if( lc.startsWith("I/II") ) { return Planet.LUM_II_EVOLVED; } //$NON-NLS-1$
        if( lc.startsWith("I-II") ) { return Planet.LUM_II_EVOLVED; } //$NON-NLS-1$
        if( lc.startsWith("Ib/II") ) { return Planet.LUM_II_EVOLVED; } //$NON-NLS-1$
        if( lc.startsWith("Ib-II") ) { return Planet.LUM_II_EVOLVED; } //$NON-NLS-1$
        if( lc.startsWith("II/III") ) { return Planet.LUM_III_EVOLVED; } //$NON-NLS-1$
        if( lc.startsWith("II-III") ) { return Planet.LUM_III_EVOLVED; } //$NON-NLS-1$
        if( lc.startsWith("III/IV") ) { return Planet.LUM_IV_EVOLVED; } //$NON-NLS-1$
        if( lc.startsWith("III-IV") ) { return Planet.LUM_IV_EVOLVED; } //$NON-NLS-1$
        if( lc.startsWith("IV/V") ) { return Planet.LUM_V_EVOLVED; } //$NON-NLS-1$
        if( lc.startsWith("IV-V") ) { return Planet.LUM_V_EVOLVED; } //$NON-NLS-1$
        if( lc.startsWith("III") ) { return Planet.LUM_III; } //$NON-NLS-1$
        if( lc.startsWith("II") ) { return Planet.LUM_II; } //$NON-NLS-1$
        if( lc.startsWith("IV") ) { return Planet.LUM_IV; } //$NON-NLS-1$
        if( lc.startsWith("Ia-0") ) { return Planet.LUM_0; } // Alias //$NON-NLS-1$
        if( lc.startsWith("Ia0") ) { return Planet.LUM_0; } // Alias //$NON-NLS-1$
        if( lc.startsWith("Ia+") ) { return Planet.LUM_0; } // Alias //$NON-NLS-1$
        if( lc.startsWith("Iab") ) { return Planet.LUM_IAB; } //$NON-NLS-1$
        if( lc.startsWith("Ia") ) { return Planet.LUM_IA; } //$NON-NLS-1$
        if( lc.startsWith("Ib") ) { return Planet.LUM_IB; } //$NON-NLS-1$
        if( lc.startsWith("I") ) { return Planet.LUM_I; } // includes Ia, Iab and Ib //$NON-NLS-1$
        if( lc.startsWith("O") ) { return Planet.LUM_0; } //$NON-NLS-1$
        if( lc.startsWith("VII") ) { return Planet.LUM_VII; } //$NON-NLS-1$
        if( lc.startsWith("VI+") ) { return Planet.LUM_VI_PLUS; } //$NON-NLS-1$
        if( lc.startsWith("VI") ) { return Planet.LUM_VI; } //$NON-NLS-1$
        if( lc.startsWith("V") ) { return Planet.LUM_V; } //$NON-NLS-1$
        return null;
    }
    
    public static String getPopulationRatingString(int pops) {
        if(pops < 0) {
            return "None";
        }
        switch(pops) {
            case 0: return "Few";
            case 1: return "Tens";
            case 2: return "Hundreds";
            case 3: return "Thousands";
            case 4: return "Tens of thousands";
            case 5: return "Hundreds of thousands";
            case 6: return "Millions";
            case 7: return "Tens of millions";
            case 8: return "Hundreds of millions";
            case 9: return "Billions";
            case 10: return "Tens of billions";
            case 11: return "Hundreds of billions";
            case 12: return "Trillions";
            default: return "Uncountable";
        }
    }
    
    public static String getControlRatingString(int cr) {
        if(cr < 0) {
            return "in total anarchy";
        }
        switch(cr) {
            case 0: return "in anarchy";
            case 1: return "very free society";
            case 2: return "free society";
            case 3: return "moderately free society";
            case 4: return "controlled society";
            case 5: return "repressive";
            case 6: return "under total control";
            default: return "enslaved population";
        }
    }
    private StarUtil() {}
}