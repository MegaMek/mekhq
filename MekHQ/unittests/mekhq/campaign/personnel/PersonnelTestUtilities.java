/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel;

import java.io.InputStream;
import java.util.UUID;

import org.mockito.ArgumentMatcher;

import mekhq.TestUtilities;

public final class PersonnelTestUtilities {

    public static InputStream getTestAwardSet() {
        return TestUtilities.ParseBase64XmlFile("PD94bWwgdmVyc2lvbj0iMS4wIj8+Cjxhd2FyZHM+Cgk8YXdhcmQ+CgkJPG5hbWU+VGVz" +
                "dCBBd2FyZCAxPC9uYW1lPgoJCTxkZXNjcmlwdGlvbj5UZXN0IEF3YXJkIDEgZGVzY3JpcHRpb24uPC9kZXNjcmlwdGlvbj4KCQk8" +
                "cmliYm9uPlRlc3RBd2FyZDFfcmliYm9uMS5wbmc8L3JpYmJvbj4KCQk8cmliYm9uPlRlc3RBd2FyZDFfcmliYm9uMi5wbmc8L3Jp" +
                "YmJvbj4KCQk8eHA+MzwveHA+CgkJPHN0YWNrYWJsZT50cnVlPC9zdGFja2FibGU+Cgk8L2F3YXJkPgoJPGF3YXJkPgoJCTxuYW1l" +
                "PlRlc3QgQXdhcmQgMjwvbmFtZT4KCQk8ZGVzY3JpcHRpb24+VGVzdCBBd2FyZCAyIGRlc2NyaXB0aW9uLjwvZGVzY3JpcHRpb24+" +
                "CgkJPG1lZGFsPlRlc3RBd2FyZDJfbWVkYWwucG5nPC9tZWRhbD4KCQk8cmliYm9uPlRlc3RBd2FyZDJfcmliYm9uLnBuZzwvcmli" +
                "Ym9uPgoJCTx4cD4xPC94cD4KCTwvYXdhcmQ+CQkKPC9hd2FyZHM+");
    }

    public static Award getTestAward1() {
        AwardsFactory.getInstance().loadAwardsFromStream(getTestAwardSet(), "TestSet");
        return AwardsFactory.getInstance().generateNew("TestSet", "Test Award 1");
    }

    public static ArgumentMatcher<UUID> matchPersonUUID(final UUID target) {
        return target::equals;
    }
}
