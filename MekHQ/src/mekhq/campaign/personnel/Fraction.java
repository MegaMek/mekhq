/*
 * Copyright (c) 2014 - Carl Spain. All Rights Reserved.
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
package mekhq.campaign.personnel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import megamek.common.annotations.Nullable;

class Fraction {
    private int numerator;
    private int denominator;

    public Fraction() {
        numerator = 0;
        denominator = 1;
    }

    public Fraction(int n, int d) {
        if (d == 0) {
            throw new IllegalArgumentException("Denominator is zero.");
        }
        if (d < 0) {
            n = -n;
            d = -d;
        }
        numerator = n;
        denominator = d;
    }

    public Fraction(int i) {
        numerator = i;
        denominator = 1;
    }

    public Fraction(Fraction f) {
        numerator = f.numerator;
        denominator = f.denominator;
    }

    @Override
    public String toString() {
        return numerator + "/" + denominator;
    }

    @Override
    public boolean equals(final @Nullable Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof Fraction)) {
            return false;
        } else {
            return value() == ((Fraction) object).value();
        }
    }

    @Override
    public int hashCode() {
        return Double.valueOf(value()).hashCode();
    }

    @Override
    public Object clone() {
        return new Fraction(this);
    }

    public double value() {
        return (double) numerator / (double) denominator;
    }

    public void reduce() {
        if (denominator > 1) {
            for (int i = denominator - 1; i > 1; i--) {
                if (numerator % i == 0 && denominator % i == 0) {
                    numerator /= i;
                    denominator /= i;
                    i = denominator - 1;
                }
            }
        }
    }

    public int getNumerator() {
        return numerator;
    }

    public int getDenominator() {
        return denominator;
    }

    public void add(Fraction f) {
        numerator = numerator * f.denominator + f.numerator * denominator;
        denominator = denominator * f.denominator;
        reduce();
    }

    public void add(int i) {
        numerator += i * denominator;
        reduce();
    }

    public void sub(Fraction f) {
        numerator = numerator * f.denominator - f.numerator * denominator;
        denominator = denominator * f.denominator;
        reduce();
    }

    public void sub(int i) {
        numerator -= i * denominator;
        reduce();
    }

    public void mul(Fraction f) {
        numerator *= f.numerator;
        denominator *= f.denominator;
        reduce();
    }

    public void mul(int i) {
        numerator *= i;
        reduce();
    }

    public void div(Fraction f) {
        numerator *= f.denominator;
        denominator *= f.numerator;
        reduce();
    }

    public void div(int i) {
        denominator *= i;
    }

    public static int lcd(Collection<Fraction> list) {
        Set<Integer> denominators = new HashSet<>();
        for (Fraction f : list) {
            denominators.add(f.denominator);
        }
        boolean done = false;
        int retVal = 1;
        while (!done) {
            done = true;
            for (Integer d : denominators) {
                if (d / retVal > 1 || retVal % d != 0) {
                    retVal++;
                    done = false;
                    break;
                }
            }
        }
        return retVal;
    }
}
