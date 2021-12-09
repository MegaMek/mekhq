/*
 * UnitTestUtilities.java
 *
 * Copyright (C) 2018 MegaMek team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.unit;

import megamek.common.loaders.*;
import megamek.common.util.BuildingBlock;
import mekhq.TestUtilities;
import org.junit.Assert;

import megamek.common.Entity;
import mekhq.campaign.Campaign;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public final class UnitTestUtilities {

    public static Unit addAndGetUnit(Campaign campaign, Entity entity) {
        campaign.addNewUnit(entity, false, 0);
        for (Unit unit : campaign.getHangar().getUnits()) {
            return unit;
        }

        Assert.fail("Did not add unit to campaign");
        return null;
    }

    public static Entity ParseBase64MtfFile(String base64) {
        try {
            InputStream in = new ByteArrayInputStream(TestUtilities.Decode(base64));
            MtfFile parser = new MtfFile(in);

            return parser.getEntity();
        } catch (EntityLoadingException e) {
            Assert.fail(e.toString());
        }

        return null;
    }

    public static Entity parseBase64BlkFile(String base64) {
        try {
            InputStream in = new ByteArrayInputStream(TestUtilities.Decode(base64));
            IMechLoader loader;
            
            BuildingBlock bb = new BuildingBlock(in);
            if (bb.exists("UnitType")) {
                String sType = bb.getDataAsString("UnitType")[0];
                if (sType.equals("Tank") || sType.equals("Naval")
                        || sType.equals("Surface") || sType.equals("Hydrofoil")) {
                    loader = new BLKTankFile(bb);
                } else if (sType.equals("Infantry")) {
                    loader = new BLKInfantryFile(bb);
                } else if (sType.equals("BattleArmor")) {
                    loader = new BLKBattleArmorFile(bb);
                } else if (sType.equals("ProtoMech")) {
                    loader = new BLKProtoFile(bb);
                } else if (sType.equals("Mech")) {
                    loader = new BLKMechFile(bb);
                } else if (sType.equals("VTOL")) {
                    loader = new BLKVTOLFile(bb);
                } else if (sType.equals("GunEmplacement")) {
                    loader = new BLKGunEmplacementFile(bb);
                } else if (sType.equals("SupportTank")) {
                    loader = new BLKSupportTankFile(bb);
                } else if (sType.equals("LargeSupportTank")) {
                    loader = new BLKLargeSupportTankFile(bb);
                } else if (sType.equals("SupportVTOL")) {
                    loader = new BLKSupportVTOLFile(bb);
                } else if (sType.equals("Aero")) {
                    loader = new BLKAeroFile(bb);
                } else if (sType.equals("FixedWingSupport")) {
                    loader = new BLKFixedWingSupportFile(bb);
                } else if (sType.equals("ConvFighter")) {
                    loader = new BLKConvFighterFile(bb);
                } else if (sType.equals("SmallCraft")) {
                    loader = new BLKSmallCraftFile(bb);
                } else if (sType.equals("Dropship")) {
                    loader = new BLKDropshipFile(bb);
                } else if (sType.equals("Jumpship")) {
                    loader = new BLKJumpshipFile(bb);
                } else if (sType.equals("Warship")) {
                    loader = new BLKWarshipFile(bb);
                } else if (sType.equals("SpaceStation")) {
                    loader = new BLKSpaceStationFile(bb);
                } else {
                    throw new EntityLoadingException("Unknown UnitType: "
                            + sType);
                }
            } else {
                loader = new BLKMechFile(bb);
            }

            return loader.getEntity();
        } catch (EntityLoadingException e) {
            Assert.fail(e.toString());
        }

        return null;
    }

    public static Entity getLocustLCT1V() {
        // megamek/megamek/data/mechfiles/mechs/3039u/Locust LCT-1V.mtf
        return ParseBase64MtfFile(
             "VmVyc2lvbjoxLjAKTG9jdXN0CkxDVC0xVgoKQ29uZmlnOkJpcGVkClRlY2hCYXNlO"
            + "klubmVyIFNwaGVyZQpFcmE6MjQ5OQpTb3VyY2U6VFJPIDMwMzkgLSBBZ2Ugb2YgV"
            + "2FyClJ1bGVzIExldmVsOjEKCk1hc3M6MjAKRW5naW5lOjE2MCBGdXNpb24gRW5na"
            + "W5lClN0cnVjdHVyZTpTdGFuZGFyZApNeW9tZXI6U3RhbmRhcmQKCkhlYXQgU2lua"
            + "3M6MTAgU2luZ2xlCldhbGsgTVA6OApKdW1wIE1QOjAKCkFybW9yOlN0YW5kYXJkK"
            + "ElubmVyIFNwaGVyZSkKTEEgQXJtb3I6NApSQSBBcm1vcjo0CkxUIEFybW9yOjgKU"
            + "lQgQXJtb3I6OApDVCBBcm1vcjoxMApIRCBBcm1vcjo4CkxMIEFybW9yOjgKUkwgQ"
            + "XJtb3I6OApSVEwgQXJtb3I6MgpSVFIgQXJtb3I6MgpSVEMgQXJtb3I6MgoKV2Vhc"
            + "G9uczozCk1lZGl1bSBMYXNlciwgQ2VudGVyIFRvcnNvCk1hY2hpbmUgR3VuLCBMZ"
            + "WZ0IEFybQpNYWNoaW5lIEd1biwgUmlnaHQgQXJtCgpMZWZ0IEFybToKU2hvdWxkZ"
            + "XIKVXBwZXIgQXJtIEFjdHVhdG9yCk1hY2hpbmUgR3VuCi1FbXB0eS0KLUVtcHR5L"
            + "QotRW1wdHktCi1FbXB0eS0KLUVtcHR5LQotRW1wdHktCi1FbXB0eS0KLUVtcHR5L"
            + "QotRW1wdHktCgpSaWdodCBBcm06ClNob3VsZGVyClVwcGVyIEFybSBBY3R1YXRvc"
            + "gpNYWNoaW5lIEd1bgotRW1wdHktCi1FbXB0eS0KLUVtcHR5LQotRW1wdHktCi1Fb"
            + "XB0eS0KLUVtcHR5LQotRW1wdHktCi1FbXB0eS0KLUVtcHR5LQoKTGVmdCBUb3Jzb"
            + "zoKLUVtcHR5LQotRW1wdHktCi1FbXB0eS0KLUVtcHR5LQotRW1wdHktCi1FbXB0e"
            + "S0KLUVtcHR5LQotRW1wdHktCi1FbXB0eS0KLUVtcHR5LQotRW1wdHktCi1FbXB0e"
            + "S0KClJpZ2h0IFRvcnNvOgotRW1wdHktCi1FbXB0eS0KLUVtcHR5LQotRW1wdHktC"
            + "i1FbXB0eS0KLUVtcHR5LQotRW1wdHktCi1FbXB0eS0KLUVtcHR5LQotRW1wdHktC"
            + "i1FbXB0eS0KLUVtcHR5LQoKQ2VudGVyIFRvcnNvOgpGdXNpb24gRW5naW5lCkZ1c"
            + "2lvbiBFbmdpbmUKRnVzaW9uIEVuZ2luZQpHeXJvCkd5cm8KR3lybwpHeXJvCkZ1c"
            + "2lvbiBFbmdpbmUKRnVzaW9uIEVuZ2luZQpGdXNpb24gRW5naW5lCk1lZGl1bSBMY"
            + "XNlcgpJUyBBbW1vIE1HIC0gRnVsbAoKSGVhZDoKTGlmZSBTdXBwb3J0ClNlbnNvc"
            + "nMKQ29ja3BpdAotRW1wdHktClNlbnNvcnMKTGlmZSBTdXBwb3J0Ci1FbXB0eS0KL"
            + "UVtcHR5LQotRW1wdHktCi1FbXB0eS0KLUVtcHR5LQotRW1wdHktCgpMZWZ0IExlZ"
            + "zoKSGlwClVwcGVyIExlZyBBY3R1YXRvcgpMb3dlciBMZWcgQWN0dWF0b3IKRm9vd"
            + "CBBY3R1YXRvcgpIZWF0IFNpbmsKSGVhdCBTaW5rCi1FbXB0eS0KLUVtcHR5LQotR"
            + "W1wdHktCi1FbXB0eS0KLUVtcHR5LQotRW1wdHktCgpSaWdodCBMZWc6CkhpcApVc"
            + "HBlciBMZWcgQWN0dWF0b3IKTG93ZXIgTGVnIEFjdHVhdG9yCkZvb3QgQWN0dWF0b"
            + "3IKSGVhdCBTaW5rCkhlYXQgU2luawotRW1wdHktCi1FbXB0eS0KLUVtcHR5LQotR"
            + "W1wdHktCi1FbXB0eS0KLUVtcHR5LQo=");
    }

    public static Entity getLocustLCT1E() {
        // megamek/megamek/data/mechfiles/mechs/3039u/Locust LCT-1E.mtf
        return ParseBase64MtfFile(
            "VmVyc2lvbjoxLjANCkxvY3VzdA0KTENULTFFDQoNCkNvbmZpZzpCaXBlZA0KVGVjaEJhc2U6SW5u"
            + "ZXIgU3BoZXJlDQpFcmE6MjgxMQ0KU291cmNlOlRSTyAzMDM5IC0gU3VjY2Vzc2lvbiBXYXJzDQpS"
            + "dWxlcyBMZXZlbDoxDQoNCk1hc3M6MjANCkVuZ2luZToxNjAgRnVzaW9uIEVuZ2luZQ0KU3RydWN0"
            + "dXJlOlN0YW5kYXJkDQpNeW9tZXI6U3RhbmRhcmQNCg0KSGVhdCBTaW5rczoxMCBTaW5nbGUNCldh"
            + "bGsgTVA6OA0KSnVtcCBNUDowDQoNCkFybW9yOlN0YW5kYXJkKElubmVyIFNwaGVyZSkNCkxBIEFy"
            + "bW9yOjQNClJBIEFybW9yOjQNCkxUIEFybW9yOjgNClJUIEFybW9yOjgNCkNUIEFybW9yOjEwDQpI"
            + "RCBBcm1vcjo4DQpMTCBBcm1vcjo4DQpSTCBBcm1vcjo4DQpSVEwgQXJtb3I6Mg0KUlRSIEFybW9y"
            + "OjINClJUQyBBcm1vcjoyDQoNCldlYXBvbnM6NA0KTWVkaXVtIExhc2VyLCBSaWdodCBBcm0NCk1l"
            + "ZGl1bSBMYXNlciwgTGVmdCBBcm0NClNtYWxsIExhc2VyLCBSaWdodCBBcm0NClNtYWxsIExhc2Vy"
            + "LCBMZWZ0IEFybQ0KDQpMZWZ0IEFybToNClNob3VsZGVyDQpVcHBlciBBcm0gQWN0dWF0b3INCk1l"
            + "ZGl1bSBMYXNlcg0KU21hbGwgTGFzZXINCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0"
            + "eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCg0KUmlnaHQgQXJtOg0KU2hv"
            + "dWxkZXINClVwcGVyIEFybSBBY3R1YXRvcg0KTWVkaXVtIExhc2VyDQpTbWFsbCBMYXNlcg0KLUVt"
            + "cHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5"
            + "LQ0KLUVtcHR5LQ0KDQpMZWZ0IFRvcnNvOg0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVt"
            + "cHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5"
            + "LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KDQpSaWdodCBUb3JzbzoNCi1FbXB0eS0NCi1FbXB0eS0NCi1F"
            + "bXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0"
            + "eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCg0KQ2VudGVyIFRvcnNvOg0KRnVzaW9uIEVu"
            + "Z2luZQ0KRnVzaW9uIEVuZ2luZQ0KRnVzaW9uIEVuZ2luZQ0KR3lybw0KR3lybw0KR3lybw0KR3ly"
            + "bw0KRnVzaW9uIEVuZ2luZQ0KRnVzaW9uIEVuZ2luZQ0KRnVzaW9uIEVuZ2luZQ0KLUVtcHR5LQ0K"
            + "LUVtcHR5LQ0KDQpIZWFkOg0KTGlmZSBTdXBwb3J0DQpTZW5zb3JzDQpDb2NrcGl0DQotRW1wdHkt"
            + "DQpTZW5zb3JzDQpMaWZlIFN1cHBvcnQNCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0"
            + "eS0NCi1FbXB0eS0NCi1FbXB0eS0NCg0KTGVmdCBMZWc6DQpIaXANClVwcGVyIExlZyBBY3R1YXRv"
            + "cg0KTG93ZXIgTGVnIEFjdHVhdG9yDQpGb290IEFjdHVhdG9yDQpIZWF0IFNpbmsNCkhlYXQgU2lu"
            + "aw0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0K"
            + "DQpSaWdodCBMZWc6DQpIaXANClVwcGVyIExlZyBBY3R1YXRvcg0KTG93ZXIgTGVnIEFjdHVhdG9y"
            + "DQpGb290IEFjdHVhdG9yDQpIZWF0IFNpbmsNCkhlYXQgU2luaw0KLUVtcHR5LQ0KLUVtcHR5LQ0K"
            + "LUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KDQoNCk92ZXJ2aWV3OiBUaGUgTG9j"
            + "dXN0IGlzIHVuZG91YnRlZGx5IG9uZSBvZiB0aGUgbW9zdCBwb3B1bGFyIGFuZCBwcmV2YWxlbnQg"
            + "bGlnaHQgQmF0dGxlTWVjaHMgZXZlciBtYWRlLiBGaXJzdCBwcm9kdWNlZCBpbiAyNDk5LCB0aGUg"
            + "YWxtb3N0IGRvemVuIGRpc3RpbmN0IGZhY3RvcmllcyBtYW51ZmFjdHVyaW5nIHRoZSBkZXNpZ24g"
            + "cXVpY2tseSBzcHJlYWQgdGhlIGRlc2lnbiB0byBldmVyeSBwb3dlciBpbiBodW1hbiBzcGFjZS4g"
            + "SXRzIGNvbWJpbmF0aW9uIG9mIHRvdWdoIGFybW9yIChmb3IgaXRzIHNpemUpLCBleGNlcHRpb25h"
            + "bCBzcGVlZCwgYW5kIG1vc3QgaW1wb3J0YW50bHksIGxvdyBjb3N0IGhhdmUgYWxsIGNvbnRyaWJ1"
            + "dGVkIHRvIHRoZSBMb2N1c3QncyBzdWNjZXNzLiBJdCByZW1haW5zIHRoZSBiZW5jaG1hcmsgZm9y"
            + "IG1hbnkgc2NvdXRpbmcgZGVzaWducywgYW5kIGl0cyBjb250aW51YWwgdXBncmFkZXMgaGF2ZSBl"
            + "bnN1cmVkIHRoYXQgaXQgcmVtYWlucyBqdXN0IGFzIGVmZmVjdGl2ZSB3aXRoIGV2ZXJ5IG5ldyBj"
            + "b25mbGljdCB0aGF0IGFwcGVhcnNzLg0KDQpDYXBhYmlsaXRpZXM6IEFzIHRoZSBMb2N1c3Qgd2Fz"
            + "IGZpcnN0IGRldmVsb3BlZCBhcyBhIHJlY29uIHBsYXRmb3JtLCBzcGVlZCBpcyBwYXJhbW91bnQg"
            + "dG8gdGhlIGRlc2lnbidzIHBoaWxvc29waHkuIFdoaWxlIG1hbnkgdmFyaWFudHMgY2hhbmdlIHRo"
            + "ZSB3ZWFwb25yeSB0byBmaWxsIHNwZWNpZmljIHRhc2tzIG9yIHB1cnBvc2VzLCBMb2N1c3RzIGFy"
            + "ZSBuZWFybHkgYWx3YXlzIHByZXNzZWQgaW50byBzZXJ2aWNlIGluIHdheXMgd2hlcmUgdGhleSBj"
            + "YW4gYmVzdCB0YWtlIGFkdmFudGFnZSBvZiB0aGVpciBzcGVlZC4gV2hlbiBpbiBsaW5lIHJlZ2lt"
            + "ZW50cywgdGhleSBjYW4gYWN0IGFzIGEgZGVhZGx5IGZsYW5rZXJzIG9yIGhhcmFzc2VycywgYW5k"
            + "IGFyZSBvZnRlbiB1c2VkIGluIHJlYWN0aW9uYXJ5IHJvbGVzIHRvIHF1aWNrbHkgcGx1ZyBob2xl"
            + "cyBpbiBhIGZsdWlkIGJhdHRsZSBsaW5lLiBUaGUgc3RydWN0dXJhbCBmb3JtIG9mIExvY3VzdHMg"
            + "dGhlbXNlbHZlcyBhcmUgdGhlaXIgZ3JlYXRlc3Qgd2Vha25lc3M7IHdpdGggbm8gaGFuZHMsIHRo"
            + "ZXkgYXJlIGRpc2FkdmFudGFnZWQgaW4gcGh5aXNpY2FsIGNvbWJhdCBhbmQgb2NjYXNpb25hbGx5"
            + "IGhhdmUgZGlmZmljdWx0eSByaWdodGluZyB0aGVtc2VsdmVzIGFmdGVyIGEgZmFsbC4NCg0KRGVw"
            + "bG95bWVudDogT25lIG9mIHRoZSBtb3N0IGNvbW1vbiBkZXNpZ25zIGV2ZW4gcHJvZHVjZWQsIGV2"
            + "ZW4gdGhlIHNtYWxsZXN0IG1lcmNlbmFyeSBvciBwaXJhdGUgb3V0Zml0cyB3aWxsIG9mdGVuIGZp"
            + "ZWxkIG9uZSBvciBtb3JlIG9mIHRoZSBkZXNpZ24uIFByb2R1Y3Rpb24gZm9yIHRoZSBMb2N1c3Qg"
            + "aGFzIGNvbnRpbnVlZCB1bmludGVycnVwdGVkIGZvciBjZW50dXJpZXMsIGFuZCBpdCBwbGF5cyBh"
            + "biBpbXBvcnRhbnQgcm9sZSBpbiB0aGUgbWlsaXRhcmllcyBvZiBtYW55IHNtYWxsZXIgbmF0aW9u"
            + "cy4gTGFja2luZyBhbnkgbm9zZS1tb3VudGVkIHdlYXBvbmV5LCB0aGUgLTFFIGlzIHVzZWQgdG8g"
            + "YXR0YWNrICJoYXJkZXIiIHRhcmdldHMgdGhhbiB0aGUgYmFzZSB2YXJpYW50LiBJdCBpcyBhIGNv"
            + "bW1vbiBtb2RlbCwgYW5kIGlzIGNvbW1vbmx5IGZvdW5kIHRocm91Z2hvdXQga25vd24gc3BhY2Uu"
            + "DQoNCnN5c3RlbW1hbnVmYWN0dXJlcjpDSEFTU0lTOkJlcmdhbg0Kc3lzdGVtbW9kZTpDSEFTU0lT"
            + "OlZJSQ0Kc3lzdGVtbWFudWZhY3R1cmVyOkVOR0lORTpMVFYNCnN5c3RlbW1vZGU6RU5HSU5FOjE2"
            + "MA0Kc3lzdGVtbWFudWZhY3R1cmVyOkFSTU9SOlN0YXJTbGFiDQpzeXN0ZW1tb2RlOkFSTU9SOi8x"
            + "DQpzeXN0ZW1tYW51ZmFjdHVyZXI6Q09NTVVOSUNBVElPTlM6R2FycmV0dA0Kc3lzdGVtbW9kZTpD"
            + "T01NVU5JQ0FUSU9OUzpUMTAtQg0Kc3lzdGVtbWFudWZhY3R1cmVyOlRBUkdFVElORzpPL1ANCnN5"
            + "c3RlbW1vZGU6VEFSR0VUSU5HOjkxMQ=="
        );
    }

    public static Entity getWaspLAMMk1() {
        // megamek/megamek/data/mechfiles/mechs/LAMS/Wasp LAM Mk I WSP-100.mtf
        return ParseBase64MtfFile(
            "VmVyc2lvbjoxLjAKV2FzcCBMQU0gTWsgSQpXU1AtMTAwCgpDb25maWc6TEFNClRlY2"
            + "hCYXNlOklubmVyIFNwaGVyZQpFcmE6MjY5MApTb3VyY2U6VFJPMzA4NQpSdWxlcy"
            + "BMZXZlbDozCgpNYXNzOjMwCkVuZ2luZToxNTAgRnVzaW9uIEVuZ2luZShJUykKU3"
            + "RydWN0dXJlOklTIFN0YW5kYXJkCk15b21lcjpTdGFuZGFyZAoKSGVhdCBTaW5rcz"
            + "oxMCBTaW5nbGUKV2FsayBNUDo1Ckp1bXAgTVA6NAoKQXJtb3I6U3RhbmRhcmQoSW"
            + "5uZXIgU3BoZXJlKQpMQSBBcm1vcjo1ClJBIEFybW9yOjUKTFQgQXJtb3I6OApSVC"
            + "BBcm1vcjo4CkNUIEFybW9yOjkKSEQgQXJtb3I6OApMTCBBcm1vcjo3ClJMIEFybW"
            + "9yOjcKUlRMIEFybW9yOjIKUlRSIEFybW9yOjIKUlRDIEFybW9yOjMKCldlYXBvbn"
            + "M6MgpNZWRpdW0gTGFzZXIsIFJpZ2h0IEFybQpTUk0gMiAoT1MpLCBDZW50ZXIgVG"
            + "9yc28KCkxlZnQgQXJtOgpTaG91bGRlcgpVcHBlciBBcm0gQWN0dWF0b3IKTG93ZX"
            + "IgQXJtIEFjdHVhdG9yCkhhbmQgQWN0dWF0b3IKLUVtcHR5LQotRW1wdHktCi1FbX"
            + "B0eS0KLUVtcHR5LQotRW1wdHktCi1FbXB0eS0KLUVtcHR5LQotRW1wdHktCgpSaW"
            + "dodCBBcm06ClNob3VsZGVyClVwcGVyIEFybSBBY3R1YXRvcgpMb3dlciBBcm0gQW"
            + "N0dWF0b3IKSGFuZCBBY3R1YXRvcgpNZWRpdW0gTGFzZXIKLUVtcHR5LQotRW1wdH"
            + "ktCi1FbXB0eS0KLUVtcHR5LQotRW1wdHktCi1FbXB0eS0KLUVtcHR5LQoKTGVmdC"
            + "BUb3JzbzoKTGFuZGluZyBHZWFyCkF2aW9uaWNzCkhlYXQgU2luawpIZWF0IFNpbm"
            + "sKSGVhdCBTaW5rCi1FbXB0eS0KLUVtcHR5LQotRW1wdHktCi1FbXB0eS0KLUVtcH"
            + "R5LQotRW1wdHktCi1FbXB0eS0KClJpZ2h0IFRvcnNvOgpMYW5kaW5nIEdlYXIKQX"
            + "Zpb25pY3MKSGVhdCBTaW5rCkNhcmdvICgxIHRvbikKQ2FyZ28gKDEgdG9uKQpDYX"
            + "JnbyAoMSB0b24pCkNhcmdvICgxIHRvbikKQ2FyZ28gKDEgdG9uKQotRW1wdHktCi"
            + "1FbXB0eS0KLUVtcHR5LQotRW1wdHktCgpDZW50ZXIgVG9yc286CkZ1c2lvbiBFbm"
            + "dpbmUKRnVzaW9uIEVuZ2luZQpGdXNpb24gRW5naW5lCkd5cm8KR3lybwpHeXJvCk"
            + "d5cm8KRnVzaW9uIEVuZ2luZQpGdXNpb24gRW5naW5lCkZ1c2lvbiBFbmdpbmUKTG"
            + "FuZGluZyBHZWFyCklTU1JNMk9TCgpIZWFkOgpMaWZlIFN1cHBvcnQKU2Vuc29ycw"
            + "pDb2NrcGl0CkF2aW9uaWNzClNlbnNvcnMKTGlmZSBTdXBwb3J0Ci1FbXB0eS0KLU"
            + "VtcHR5LQotRW1wdHktCi1FbXB0eS0KLUVtcHR5LQotRW1wdHktCgpMZWZ0IExlZz"
            + "oKSGlwClVwcGVyIExlZyBBY3R1YXRvcgpMb3dlciBMZWcgQWN0dWF0b3IKRm9vdC"
            + "BBY3R1YXRvcgpKdW1wIEpldApKdW1wIEpldAotRW1wdHktCi1FbXB0eS0KLUVtcH"
            + "R5LQotRW1wdHktCi1FbXB0eS0KLUVtcHR5LQoKUmlnaHQgTGVnOgpIaXAKVXBwZX"
            + "IgTGVnIEFjdHVhdG9yCkxvd2VyIExlZyBBY3R1YXRvcgpGb290IEFjdHVhdG9yCk"
            + "p1bXAgSmV0Ckp1bXAgSmV0Ci1FbXB0eS0KLUVtcHR5LQotRW1wdHktCi1FbXB0eS"
            + "0KLUVtcHR5LQotRW1wdHktCg");
    }

    public static Entity getArionStandard() {
        // megamek/megamek/data/mechfiles/mechs/QuadVees/Arion (Standard).mtf
        return ParseBase64MtfFile(
            "VmVyc2lvbjoxLjAKQXJpb24KKFN0YW5kYXJkKQoKQ29uZmlnOlF1YWRWZWUKVGVjaE"
            + "Jhc2U6Q2xhbgpFcmE6MzEzNgpTb3VyY2U6VFJPIDMxNDUgVGhlIENsYW5zIC0gTG"
            + "F0ZSBSZXB1YmxpYwpSdWxlcyBMZXZlbDozCgpNYXNzOjM1CkVuZ2luZToyMTAgRn"
            + "VzaW9uIChDbGFuKSBFbmdpbmUoSVMpClN0cnVjdHVyZTpDbGFuIEVuZG8gU3RlZW"
            + "wKTXlvbWVyOlN0YW5kYXJkCk1vdGl2ZTpUcmFjawoKSGVhdCBTaW5rczoxMCBMYX"
            + "NlcgpXYWxrIE1QOjYKSnVtcCBNUDowCgpBcm1vcjpGZXJyby1GaWJyb3VzKENsYW"
            + "4pCkZMTCBBcm1vcjo1CkZSTCBBcm1vcjo1CkxUIEFybW9yOjUKUlQgQXJtb3I6NQ"
            + "pDVCBBcm1vcjo2CkhEIEFybW9yOjMKUkxMIEFybW9yOjUKUlJMIEFybW9yOjUKUl"
            + "RMIEFybW9yOjMKUlRSIEFybW9yOjMKUlRDIEFybW9yOjMKCldlYXBvbnM6MwpFUi"
            + "BMYXJnZSBMYXNlciwgQ2VudGVyIFRvcnNvClNSTSA0LCBMZWZ0IFRvcnNvClNSTS"
            + "A0LCBSaWdodCBUb3JzbwoKRnJvbnQgTGVmdCBMZWc6CkhpcApVcHBlciBMZWcgQW"
            + "N0dWF0b3IKTG93ZXIgTGVnIEFjdHVhdG9yCkZvb3QgQWN0dWF0b3IKQ29udmVyc2"
            + "lvbiBHZWFyClRyYWNrcwoKRnJvbnQgUmlnaHQgTGVnOgpIaXAKVXBwZXIgTGVnIE"
            + "FjdHVhdG9yCkxvd2VyIExlZyBBY3R1YXRvcgpGb290IEFjdHVhdG9yCkNvbnZlcn"
            + "Npb24gR2VhcgpUcmFja3MKCkxlZnQgVG9yc286Ckxhc2VyIEhlYXQgU2luawpMYX"
            + "NlciBIZWF0IFNpbmsKQ0xTUk00CkNsYW4gQW1tbyBTUk0tNApDbGFuIEVuZG8gU3"
            + "RlZWwKQ2xhbiBFbmRvIFN0ZWVsCkNsYW4gRW5kbyBTdGVlbApDbGFuIEVuZG8gU3"
            + "RlZWwKQ2xhbiBFbmRvIFN0ZWVsCkNsYW4gRW5kbyBTdGVlbApDbGFuIEVuZG8gU3"
            + "RlZWwKLUVtcHR5LQoKUmlnaHQgVG9yc286Ckxhc2VyIEhlYXQgU2luawpMYXNlci"
            + "BIZWF0IFNpbmsKQ0xTUk00CkNsYW4gRmVycm8tRmlicm91cwpDbGFuIEZlcnJvLU"
            + "ZpYnJvdXMKQ2xhbiBGZXJyby1GaWJyb3VzCkNsYW4gRmVycm8tRmlicm91cwpDbG"
            + "FuIEZlcnJvLUZpYnJvdXMKQ2xhbiBGZXJyby1GaWJyb3VzCkNsYW4gRmVycm8tRm"
            + "licm91cwotRW1wdHktCi1FbXB0eS0KCkNlbnRlciBUb3JzbzoKRnVzaW9uIEVuZ2"
            + "luZQpGdXNpb24gRW5naW5lCkZ1c2lvbiBFbmdpbmUKR3lybwpHeXJvCkd5cm8KR3"
            + "lybwpGdXNpb24gRW5naW5lCkZ1c2lvbiBFbmdpbmUKRnVzaW9uIEVuZ2luZQpDTE"
            + "VSTGFyZ2VMYXNlcgotRW1wdHktCgpIZWFkOgpMaWZlIFN1cHBvcnQKU2Vuc29ycw"
            + "pDb2NrcGl0CkNvY2twaXQKU2Vuc29ycwpMaWZlIFN1cHBvcnQKClJlYXIgTGVmdC"
            + "BMZWc6CkhpcApVcHBlciBMZWcgQWN0dWF0b3IKTG93ZXIgTGVnIEFjdHVhdG9yCk"
            + "Zvb3QgQWN0dWF0b3IKQ29udmVyc2lvbiBHZWFyClRyYWNrcwoKUmVhciBSaWdodC"
            + "BMZWc6CkhpcApVcHBlciBMZWcgQWN0dWF0b3IKTG93ZXIgTGVnIEFjdHVhdG9yCk"
            + "Zvb3QgQWN0dWF0b3IKQ29udmVyc2lvbiBHZWFyClRyYWNrcwoK");
    }

    public static Entity getJavelinJVN10N() {
        // megamek/megamek/data/mechfiles/mechs/3039u/Javelin JVN-10N.mtf
        return ParseBase64MtfFile(
            "VmVyc2lvbjoxLjANCkphdmVsaW4NCkpWTi0xME4NCg0KQ29uZmlnOkJpcGVkDQpUZWNoQmFzZTpJ"
            + "bm5lciBTcGhlcmUNCkVyYToyNzUxDQpTb3VyY2U6VFJPIDMwMzkgLSBTdGFyIExlYWd1ZQ0KUnVs"
            + "ZXMgTGV2ZWw6MQ0KDQpNYXNzOjMwDQpFbmdpbmU6MTgwIEZ1c2lvbiBFbmdpbmUNClN0cnVjdHVy"
            + "ZTpTdGFuZGFyZA0KTXlvbWVyOlN0YW5kYXJkDQoNCkhlYXQgU2lua3M6MTAgU2luZ2xlDQpXYWxr"
            + "IE1QOjYNCkp1bXAgTVA6Ng0KDQpBcm1vcjpTdGFuZGFyZChJbm5lciBTcGhlcmUpDQpMQSBBcm1v"
            + "cjo2DQpSQSBBcm1vcjo2DQpMVCBBcm1vcjo4DQpSVCBBcm1vcjo4DQpDVCBBcm1vcjo4DQpIRCBB"
            + "cm1vcjo2DQpMTCBBcm1vcjo4DQpSTCBBcm1vcjo4DQpSVEwgQXJtb3I6Mg0KUlRSIEFybW9yOjIN"
            + "ClJUQyBBcm1vcjoyDQoNCldlYXBvbnM6Mg0KU1JNIDYsIExlZnQgVG9yc28NClNSTSA2LCBSaWdo"
            + "dCBUb3Jzbw0KDQpMZWZ0IEFybToNClNob3VsZGVyDQpVcHBlciBBcm0gQWN0dWF0b3INCkxvd2Vy"
            + "IEFybSBBY3R1YXRvcg0KSGFuZCBBY3R1YXRvcg0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0K"
            + "LUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KDQpSaWdodCBBcm06"
            + "DQpTaG91bGRlcg0KVXBwZXIgQXJtIEFjdHVhdG9yDQpMb3dlciBBcm0gQWN0dWF0b3INCkhhbmQg"
            + "QWN0dWF0b3INCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1F"
            + "bXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCg0KTGVmdCBUb3JzbzoNCkhlYXQgU2luaw0KU1JNIDYN"
            + "ClNSTSA2DQpJUyBBbW1vIFNSTS02DQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQotRW1wdHkt"
            + "DQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQoNClJpZ2h0IFRvcnNvOg0KSGVh"
            + "dCBTaW5rDQpIZWF0IFNpbmsNClNSTSA2DQpTUk0gNg0KSVMgQW1tbyBTUk0tNg0KLUVtcHR5LQ0K"
            + "LUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KDQpD"
            + "ZW50ZXIgVG9yc286DQpGdXNpb24gRW5naW5lDQpGdXNpb24gRW5naW5lDQpGdXNpb24gRW5naW5l"
            + "DQpHeXJvDQpHeXJvDQpHeXJvDQpHeXJvDQpGdXNpb24gRW5naW5lDQpGdXNpb24gRW5naW5lDQpG"
            + "dXNpb24gRW5naW5lDQpKdW1wIEpldA0KSnVtcCBKZXQNCg0KSGVhZDoNCkxpZmUgU3VwcG9ydA0K"
            + "U2Vuc29ycw0KQ29ja3BpdA0KLUVtcHR5LQ0KU2Vuc29ycw0KTGlmZSBTdXBwb3J0DQotRW1wdHkt"
            + "DQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQoNCkxlZnQgTGVn"
            + "Og0KSGlwDQpVcHBlciBMZWcgQWN0dWF0b3INCkxvd2VyIExlZyBBY3R1YXRvcg0KRm9vdCBBY3R1"
            + "YXRvcg0KSnVtcCBKZXQNCkp1bXAgSmV0DQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQotRW1w"
            + "dHktDQotRW1wdHktDQotRW1wdHktDQoNClJpZ2h0IExlZzoNCkhpcA0KVXBwZXIgTGVnIEFjdHVh"
            + "dG9yDQpMb3dlciBMZWcgQWN0dWF0b3INCkZvb3QgQWN0dWF0b3INCkp1bXAgSmV0DQpKdW1wIEpl"
            + "dA0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0K"
            + "DQoNCk92ZXJ2aWV3OiBUaGUgSmF2ZWxpbiB3YXMgaW50cm9kdWNlZCBieSBTdG9ybXZhbmdlciBB"
            + "c3NlbWJsaWVzIHRvd2FyZHMgdGhlIGVuZCBvZiB0aGUgU3RhciBMZWFndWUuIEl0IHdhcyBub3Qg"
            + "d2VsbCBpbnRlZ3JhdGVkIGludG8gR3JlYXQgSG91c2UgYXJtaWVzIGF0IHRoZSBzdGFydCBvZiB0"
            + "aGUgU3VjY2Vzc2lvbiBXYXJzIGFzIGEgcmVzdWx0LCBhbmQgaXRzIGFwcGVhcmFuY2UgYW5kIGxh"
            + "cmdlbHkgdW5rbm93biBjYXBhYmlsaXRpZXMgb2Z0ZW4gc3VycHJpc2VkIGNvbWJhdGFudHMuIFRy"
            + "aWFsIGFuZCBlcnJvciBzaG93ZWQgdGhhdCBKYXZlbGlucyBvcGVyYXRlZCBiZXN0IGFzIGEgd2Vs"
            + "bC1hcm1lZCBzY291dCBvciBhbWJ1c2hlciwgYW5kIGV2ZW4gdGhlIGRlc3RydWN0aW9uIG9mIGl0"
            + "cyBvbmx5IHByb2R1Y3Rpb24gZmFjaWxpdHkgb24gQ2FwaCBkaWQgbGl0dGxlIHRvIGxvd2VyIGl0"
            + "cyBncm93aW5nIHBvcHVsYXJpdHkuIA0KDQpDYXBhYmlsaXRpZXM6IFRoZSBoZWF2eSBmaXJlcG93"
            + "ZXIgb2YgbW9zdCBKYXZlbGlucyBnaXZlIGl0IGEgc3VycHJpc2luZyBwdW5jaC4gTWFueSBsaWdo"
            + "dCBzY291dHMgdGhhdCBydW4gYWZvdWwgb2Ygb25lIHF1aWNrbHkgbGVhcm4gdG8gcHVsbCBiYWNr"
            + "IHJhdGhlciB0aGFuIGZhY2UgYSBKYXZlbGluIGhlYWQgb24sIGFzIG1vc3QgSmF2ZWxpbnMgbGFj"
            + "ayB0aGUgd2VhcG9ucyB0byBmaWdodCBhdCByYW5nZS4gSXRzIHNwZWVkIGF2ZXJhZ2UgZm9yIG1v"
            + "c3QgbGlnaHQgJ01lY2hzLCBpdHMgYXJtb3IgaXMgYWxzbyBzY2FudCBiZXR0ZXIgdGhhbiBtYW55"
            + "ICJidWciIGRlc2lnbnMgc3VjaCBhcyB0aGUgU3RpbmdlciBvciBXYXNwLiBJZiB0aGUgSmF2ZWxp"
            + "biBoYXMgb25lIGZsYXcsIGhvd2V2ZXIsIGl0IGlzIHRoZSB1bnVzdWFsIGNlbnRlciBvZiBncmF2"
            + "aXR5IGZvciB0aGUgZGVzaWduLiBUaGUgSmF2ZWxpbidzIGNoZXN0IHByb3RydWRlcyBmb3dhcmQg"
            + "b24gdGhlIHRvcnNvLCBhbmQgbWFueSBNZWNoV2FycmlvcnMgbmV3IHRvIHRoZSBkZXNpZ24gY2Fu"
            + "IGVhc2lseSBzdHVtYmxlIG9yIGZhbGwgd2hlbiBydW5uaW5nIGF0IGhpZ2hlciBzcGVlZHMgb3Ig"
            + "b24gdW5zdGFibGUgdGVycmFpbi4NCg0KRGVwbG95bWVudDogQSB3aWRlc3ByZWFkIGRlc2lnbiwg"
            + "dGhlIEphdmxpbiBjYW4gYmUgZm91bmQgaW4gbmVhcmx5IGFsbCAgcGFydHMgb2YgdGhlIElubmVy"
            + "IFNwaGVyZS4gVGhlIG9ubHkgc3RhdGUgdG8gZmlybWx5IGVtYnJhY2UgdGhlIGRlc2lnbiwgdGhl"
            + "IEZlZGVyYXRlZCBTdW5zIGhhcyBhIG5vdGFibHkgaGlnaGVyIGNvbmNlbnRyYXRpb24gb2YgSmF2"
            + "ZWxpbnMgd2l0aGluIHRoZWlyIGZvcmNlcywgd2l0aCBzZXZlcmFsIHJlZ2ltZW50cyBmaWVsZGlu"
            + "ZyBkb3plbnMgb2YgdGhlIGRlc2lnbi4gVGhpcyBydW5zIHRydWUgZm9yIHRoZSBvcmlnaW5hbCBK"
            + "YXZlbGluLCB3aG9zIG51bWJlcnMgaGF2ZSBvbmx5IHNvbWV3aGF0IGRyb3BwZWQgaW4gdGhlIEZl"
            + "ZGVyYXRlZCBTdW5zIGV2ZW4gYWZ0ZXIgcmVjZW50IHZpb2xlbnQgY29uZmxpY3RzLiBUaGVyZSBp"
            + "cyBhbHNvIGEgbm90YWJsZSBudW1iZXIgd2l0aGluIHRoZSBicmVhay1vZmYgRmlsdHZlbHQgQ29h"
            + "bGl0aW9uLCB3aG8gYXBwcmVjaWF0ZSB0aGUgcmVsaWFibGUgbmF0dXJlIG9mIHRoZSBkZXNpZ24u"
            + "DQoNCnN5c3RlbW1hbnVmYWN0dXJlcjpDSEFTU0lTOkR1cmFseXRlDQpzeXN0ZW1tb2RlOkNIQVNT"
            + "SVM6MjQ2DQpzeXN0ZW1tYW51ZmFjdHVyZXI6RU5HSU5FOkdNDQpzeXN0ZW1tb2RlOkVOR0lORTox"
            + "ODANCnN5c3RlbW1hbnVmYWN0dXJlcjpBUk1PUjpTdGFyR3VhcmQNCnN5c3RlbW1vZGU6QVJNT1I6"
            + "SQ0Kc3lzdGVtbWFudWZhY3R1cmVyOkNPTU1VTklDQVRJT05TOkdhcnJldA0Kc3lzdGVtbW9kZTpD"
            + "T01NVU5JQ0FUSU9OUzpUMTBCDQpzeXN0ZW1tYW51ZmFjdHVyZXI6VEFSR0VUSU5HOkR5bmF0ZWMN"
            + "CnN5c3RlbW1vZGU6VEFSR0VUSU5HOjEyOEMNCg=="
        );
    }

    public static Entity getJavelinJVN10A() {
        // megamek/megamek/data/mechfiles/mechs/RS Succession Wars/Javelin JVN-10A.mtf
        return ParseBase64MtfFile(
            "VmVyc2lvbjoxLjANCkphdmVsaW4NCkpWTi0xMEENCg0KQ29uZmlnOkJpcGVkDQpUZWNoQmFzZTpJ"
            + "bm5lciBTcGhlcmUNCkVyYToyNzUyDQpTb3VyY2U6UlM6U3VjY2Vzc2lvbiBXYXJzIC0gU3VjY2Vz"
            + "c2lvbiBXYXJzDQpSdWxlcyBMZXZlbDoxDQoNCk1hc3M6MzANCkVuZ2luZToxODAgRnVzaW9uIEVu"
            + "Z2luZShJUykNClN0cnVjdHVyZTpJUyBTdGFuZGFyZA0KTXlvbWVyOlN0YW5kYXJkDQoNCkhlYXQg"
            + "U2lua3M6MTAgU2luZ2xlDQpXYWxrIE1QOjYNCkp1bXAgTVA6Ng0KDQpBcm1vcjpTdGFuZGFyZChJ"
            + "bm5lciBTcGhlcmUpDQpMQSBBcm1vcjo2DQpSQSBBcm1vcjo2DQpMVCBBcm1vcjo4DQpSVCBBcm1v"
            + "cjo4DQpDVCBBcm1vcjo4DQpIRCBBcm1vcjo2DQpMTCBBcm1vcjo4DQpSTCBBcm1vcjo4DQpSVEwg"
            + "QXJtb3I6Mg0KUlRSIEFybW9yOjINClJUQyBBcm1vcjoyDQoNCldlYXBvbnM6MQ0KTFJNIDE1LCBS"
            + "aWdodCBUb3Jzbw0KDQpMZWZ0IEFybToNClNob3VsZGVyDQpVcHBlciBBcm0gQWN0dWF0b3INCkxv"
            + "d2VyIEFybSBBY3R1YXRvcg0KSGFuZCBBY3R1YXRvcg0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5"
            + "LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KDQpSaWdodCBB"
            + "cm06DQpTaG91bGRlcg0KVXBwZXIgQXJtIEFjdHVhdG9yDQpMb3dlciBBcm0gQWN0dWF0b3INCkhh"
            + "bmQgQWN0dWF0b3INCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0N"
            + "Ci1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCg0KTGVmdCBUb3JzbzoNCkhlYXQgU2luaw0KLUVt"
            + "cHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5"
            + "LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KDQpSaWdodCBUb3JzbzoNCkxS"
            + "TSAxNQ0KTFJNIDE1DQpMUk0gMTUNCklTIEFtbW8gTFJNLTE1DQpIZWF0IFNpbmsNCkhlYXQgU2lu"
            + "aw0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0K"
            + "DQpDZW50ZXIgVG9yc286DQpGdXNpb24gRW5naW5lDQpGdXNpb24gRW5naW5lDQpGdXNpb24gRW5n"
            + "aW5lDQpHeXJvDQpHeXJvDQpHeXJvDQpHeXJvDQpGdXNpb24gRW5naW5lDQpGdXNpb24gRW5naW5l"
            + "DQpGdXNpb24gRW5naW5lDQpKdW1wIEpldA0KSnVtcCBKZXQNCg0KSGVhZDoNCkxpZmUgU3VwcG9y"
            + "dA0KU2Vuc29ycw0KQ29ja3BpdA0KLUVtcHR5LQ0KU2Vuc29ycw0KTGlmZSBTdXBwb3J0DQotRW1w"
            + "dHktDQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQoNCkxlZnQg"
            + "TGVnOg0KSGlwDQpVcHBlciBMZWcgQWN0dWF0b3INCkxvd2VyIExlZyBBY3R1YXRvcg0KRm9vdCBB"
            + "Y3R1YXRvcg0KSnVtcCBKZXQNCkp1bXAgSmV0DQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQot"
            + "RW1wdHktDQotRW1wdHktDQotRW1wdHktDQoNClJpZ2h0IExlZzoNCkhpcA0KVXBwZXIgTGVnIEFj"
            + "dHVhdG9yDQpMb3dlciBMZWcgQWN0dWF0b3INCkZvb3QgQWN0dWF0b3INCkp1bXAgSmV0DQpKdW1w"
            + "IEpldA0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5"
            + "LQ0K"
        );
    }

    public static Entity getFleaFLE4() {
        return ParseBase64MtfFile(
            "VmVyc2lvbjoxLjANCkZsZWENCkZMRS00DQoNCkNvbmZpZzpCaXBlZA0KVGVjaEJhc2U6SW5uZXIg"
            + "U3BoZXJlDQpFcmE6MjUwMQ0KU291cmNlOlRSTyAzMDM5IC0gQWdlIG9mIFdhcg0KUnVsZXMgTGV2"
            + "ZWw6MQ0KDQpNYXNzOjIwDQpFbmdpbmU6MTIwIEZ1c2lvbiBFbmdpbmUNClN0cnVjdHVyZTpTdGFu"
            + "ZGFyZA0KTXlvbWVyOlN0YW5kYXJkDQoNCkhlYXQgU2lua3M6MTAgU2luZ2xlDQpXYWxrIE1QOjYN"
            + "Ckp1bXAgTVA6MA0KDQpBcm1vcjpTdGFuZGFyZChJbm5lciBTcGhlcmUpDQpMQSBBcm1vcjozDQpS"
            + "QSBBcm1vcjozDQpMVCBBcm1vcjozDQpSVCBBcm1vcjozDQpDVCBBcm1vcjo1DQpIRCBBcm1vcjo1"
            + "DQpMTCBBcm1vcjozDQpSTCBBcm1vcjozDQpSVEwgQXJtb3I6MQ0KUlRSIEFybW9yOjENClJUQyBB"
            + "cm1vcjoyDQoNCldlYXBvbnM6NA0KTGFyZ2UgTGFzZXIsIFJpZ2h0IEFybQ0KU21hbGwgTGFzZXIs"
            + "IExlZnQgQXJtDQpTbWFsbCBMYXNlciwgTGVmdCBBcm0NCkZsYW1lciwgQ2VudGVyIFRvcnNvDQoN"
            + "CkxlZnQgQXJtOg0KU2hvdWxkZXINClVwcGVyIEFybSBBY3R1YXRvcg0KU21hbGwgTGFzZXINClNt"
            + "YWxsIExhc2VyDQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQot"
            + "RW1wdHktDQotRW1wdHktDQotRW1wdHktDQoNClJpZ2h0IEFybToNClNob3VsZGVyDQpVcHBlciBB"
            + "cm0gQWN0dWF0b3INCkxhcmdlIExhc2VyDQpMYXJnZSBMYXNlcg0KLUVtcHR5LQ0KLUVtcHR5LQ0K"
            + "LUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KDQpM"
            + "ZWZ0IFRvcnNvOg0KSGVhdCBTaW5rDQpIZWF0IFNpbmsNCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0"
            + "eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0N"
            + "Ci1FbXB0eS0NCg0KUmlnaHQgVG9yc286DQpIZWF0IFNpbmsNCkhlYXQgU2luaw0KLUVtcHR5LQ0K"
            + "LUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVt"
            + "cHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KDQpDZW50ZXIgVG9yc286DQpGdXNpb24gRW5naW5lDQpG"
            + "dXNpb24gRW5naW5lDQpGdXNpb24gRW5naW5lDQpHeXJvDQpHeXJvDQpHeXJvDQpHeXJvDQpGdXNp"
            + "b24gRW5naW5lDQpGdXNpb24gRW5naW5lDQpGdXNpb24gRW5naW5lDQpGbGFtZXIgKFIpDQotRW1w"
            + "dHktDQoNCkhlYWQ6DQpMaWZlIFN1cHBvcnQNClNlbnNvcnMNCkNvY2twaXQNCi1FbXB0eS0NClNl"
            + "bnNvcnMNCkxpZmUgU3VwcG9ydA0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0K"
            + "LUVtcHR5LQ0KLUVtcHR5LQ0KDQpMZWZ0IExlZzoNCkhpcA0KVXBwZXIgTGVnIEFjdHVhdG9yDQpM"
            + "b3dlciBMZWcgQWN0dWF0b3INCkZvb3QgQWN0dWF0b3INCkhlYXQgU2luaw0KLUVtcHR5LQ0KLUVt"
            + "cHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KDQpSaWdo"
            + "dCBMZWc6DQpIaXANClVwcGVyIExlZyBBY3R1YXRvcg0KTG93ZXIgTGVnIEFjdHVhdG9yDQpGb290"
            + "IEFjdHVhdG9yDQpIZWF0IFNpbmsNCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0N"
            + "Ci1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCg0KDQpPdmVydmlldzogVGhlIEZsZWEgd2FzIHRo"
            + "ZSBkaXJlY3QgZGVjZW5kYW50IG9mIHRoZSBGcmVlIFdvcmxkcyBMZWFndWUgZmlyc3Qgc2NvdXQg"
            + "J01lY2gsIHRoZSBUcm9vcGVyLiBOZWl0aGVyIHRoZSBUcm9vcGVyIG5vciB0aGUgRmxlYSB3b3Vs"
            + "ZCBpbXByZXNzIGFueSBtaWxpdGFyeSBhbmFseXN0cywgdGhvdWdoLCB3aXRoIHRoZWlyIGdlbmVy"
            + "YWxseSBzbG93IHNwZWVkIGFuZCBmcmFnaWxlIGFybW9yIG1ha2luZyB0aGVtIGVhc3kgcHJleSBm"
            + "b3IgZXZlbiB0aGUgbGlnaHRlc3Qgb3Bwb3NpbmcgQmF0dGxlTWVjaHMuIFdpdGggcHJvZHVjdGlv"
            + "biBvZiB0aGUgRmxlYSBjZWFzaW5nIGV2ZW4gYmVmb3JlIHRoZSBzdGFydCBvZiB0aGUgU3VjY2Vz"
            + "c2lvbiBXYXJzLCB0aGlzIGdlbmVyYWxseSB1bnBvcHVsYXIgZGVzaWduIHNlZW1lZCBkZXN0aW5l"
            + "ZCBmb3IgdGhlIGhpc3RvcnkgYm9va3MgdW50aWwgdGhlIGFwcGVhcmFuY2Ugb2YgV29sZidzIERy"
            + "YWdvb25zIGFuZCB0aGVpciBtYW55IGFuY2llbnQgQmF0dGxlTWVjaHMsIEZsZWEgaW5jbHVkZWQu"
            + "DQoNCkNhcGFiaWxpdGllczogVGhlIEZsZWEsIGFzIGEgJ01lY2ggd2l0aCBtYW55IGRpZmZlcmVu"
            + "dCB2YXJpYW50cywgY2FuIGFjY29tcGxpc2ggbWFueSBkaWZmZXJlbnQgcm9sZXMuIEhvd2V2ZXIs"
            + "IG1vc3Qgb2YgdGhlc2UgbW9kZWxzIHdvcmsgYmVzdCBpbiBpbmZhbnRyeSBzdXBwb3J0IHJvbGVz"
            + "LCB3aGVyZSB0aGUgaGVhdmllciBhcm1vciBhbmQgZmlyZXBvd2VyIG9mIHRoZSAnTWVjaCBtYWtl"
            + "IGl0IGEgdmFsdWVkIGZyaWVuZCBhbmQgYSBkYW5nZXJvdXMgZm9lLiBUaGUgZmlyZXBvd2VyIG9m"
            + "IHRoZSBGbGVhIGl0c2VsZiBpcyBzdHJvbmdlciB0aGFuIG9uZSBtaWdodCBzdXNwZWN0OyBpZiBj"
            + "b3JuZXJlZCwgaW5hdHRlbnRpdmUgc2NvdXQgJ01lY2hzIGNhbiBxdWlja2x5IGZpbmQgdGhlbXNl"
            + "bHZlcyBhdCB0aGUgbWVyY3kgb2YgYSBGbGVhLg0KDQpEZXBsb3ltZW50OiBUaGUgbWFpbiBFYXJ0"
            + "aHdlcmtzIGZhY3RvcnkgZm9yIHRoZSBGbGVhIHdhcyByZW9wZW5lZCBmb2xsb3dpbmcgdGhlIHNp"
            + "Z25pbmcgb2YgYW4gZXhjbHVzaXZlIGNvbnRyYWN0IHdpdGggdGhlIFdvbGYncyBEcmFnb29ucy4g"
            + "SW5pdGlhbGx5IG9ubHkgcHJvZHVjaW5nIHJlcGxhY2VtZW50IHBhcnRzIGFuZCBhbiBvY2Nhc2lv"
            + "bmFsIHJlcGxhY2VtZW50ICdNZWNoLCBFYXJ0aHdlcmtzIGV2ZW50dWFsbHkgb3BlbmVkIGEgc2Vj"
            + "b25kIGxpbmUgdG8ga2VlcCB1cCB3aXRoIGluY3JlYXNpbmcgZGVtYW5kLiBFeHBpcmF0aW9uIG9m"
            + "IHRoZSBEcmFnb29uIGNvbnRyYWN0IGluIDMwNDEgd2FzIHdlbGNvbWVkIGJ5IEVhcnRod2Vya3M7"
            + "IHRoZSBwdWJsaWNpdHkgZ2FpbmVkIHRocm91Z2ggdGhlIERyYWdvb25zIGhhZCBjcmVhdGVkIGEg"
            + "aGVhbHRoeSBtYXJrZXQgZm9yIHRoZSBGbGVhLiBUaGVpciBvcmlnaW5hbCBtb2RlbCwgdGhlIEZM"
            + "RS00LCB3b3VsZCByZW1haW4gaW4gRnJlZSBXb3JsZHMgTGVhZ3VlIHNlcnZpY2UgdW50aWwgdGhl"
            + "IHRhaWwgZW5kIG9mIHRoZSBDbGFuIEludmFzaW9uLg0KDQpzeXN0ZW1tYW51ZmFjdHVyZXI6Q0hB"
            + "U1NJUzpFYXJ0aHdlcmtzDQpzeXN0ZW1tb2RlOkNIQVNTSVM6VHJvb3Blcg0Kc3lzdGVtbWFudWZh"
            + "Y3R1cmVyOkVOR0lORTpHTQ0Kc3lzdGVtbW9kZTpFTkdJTkU6MTIwDQpzeXN0ZW1tYW51ZmFjdHVy"
            + "ZXI6QVJNT1I6TGl2aW5nc3Rvbg0Kc3lzdGVtbW9kZTpBUk1PUjpDZXJhbWljcw0Kc3lzdGVtbWFu"
            + "dWZhY3R1cmVyOkNPTU1VTklDQVRJT05TOk5laWwNCnN5c3RlbW1vZGU6Q09NTVVOSUNBVElPTlM6"
            + "MjAwMA0Kc3lzdGVtbWFudWZhY3R1cmVyOlRBUkdFVElORzpEYWxiYW4NCnN5c3RlbW1vZGU6VEFS"
            + "R0VUSU5HOkhpUmV6LUINCg=="
        );
    }

    public static Entity getFleaFLE15() {
        return ParseBase64MtfFile(
            "VmVyc2lvbjoxLjANCkZsZWENCkZMRS0xNQ0KDQpDb25maWc6QmlwZWQNClRlY2hCYXNlOklubmVy"
            + "IFNwaGVyZQ0KRXJhOjI1MjMNClNvdXJjZTpUUk8gMzAzOSAtIEFnZSBvZiBXYXINClJ1bGVzIExl"
            + "dmVsOjENCg0KTWFzczoyMA0KRW5naW5lOjEyMCBGdXNpb24gRW5naW5lDQpTdHJ1Y3R1cmU6U3Rh"
            + "bmRhcmQNCk15b21lcjpTdGFuZGFyZA0KDQpIZWF0IFNpbmtzOjEwIFNpbmdsZQ0KV2FsayBNUDo2"
            + "DQpKdW1wIE1QOjANCg0KQXJtb3I6U3RhbmRhcmQoSW5uZXIgU3BoZXJlKQ0KTEEgQXJtb3I6NA0K"
            + "UkEgQXJtb3I6NA0KTFQgQXJtb3I6NQ0KUlQgQXJtb3I6NQ0KQ1QgQXJtb3I6OA0KSEQgQXJtb3I6"
            + "NQ0KTEwgQXJtb3I6NA0KUkwgQXJtb3I6NA0KUlRMIEFybW9yOjMNClJUUiBBcm1vcjozDQpSVEMg"
            + "QXJtb3I6Mw0KDQpXZWFwb25zOjcNCk1lZGl1bSBMYXNlciwgUmlnaHQgQXJtDQpNZWRpdW0gTGFz"
            + "ZXIsIExlZnQgQXJtDQpNYWNoaW5lIEd1biwgTGVmdCBBcm0NCk1hY2hpbmUgR3VuLCBSaWdodCBB"
            + "cm0NCkZsYW1lciwgQ2VudGVyIFRvcnNvDQpTbWFsbCBMYXNlciwgTGVmdCBUb3Jzbw0KU21hbGwg"
            + "TGFzZXIsIFJpZ2h0IFRvcnNvDQoNCkxlZnQgQXJtOg0KU2hvdWxkZXINClVwcGVyIEFybSBBY3R1"
            + "YXRvcg0KTWVkaXVtIExhc2VyDQpNYWNoaW5lIEd1bg0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5"
            + "LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KDQpSaWdodCBB"
            + "cm06DQpTaG91bGRlcg0KVXBwZXIgQXJtIEFjdHVhdG9yDQpNZWRpdW0gTGFzZXINCk1hY2hpbmUg"
            + "R3VuDQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQotRW1wdHkt"
            + "DQotRW1wdHktDQotRW1wdHktDQoNCkxlZnQgVG9yc286DQpIZWF0IFNpbmsNCkhlYXQgU2luaw0K"
            + "U21hbGwgTGFzZXIgKFIpDQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQotRW1w"
            + "dHktDQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQotRW1wdHktDQoNClJpZ2h0IFRvcnNvOg0K"
            + "SGVhdCBTaW5rDQpIZWF0IFNpbmsNClNtYWxsIExhc2VyIChSKQ0KLUVtcHR5LQ0KLUVtcHR5LQ0K"
            + "LUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVt"
            + "cHR5LQ0KDQpDZW50ZXIgVG9yc286DQpGdXNpb24gRW5naW5lDQpGdXNpb24gRW5naW5lDQpGdXNp"
            + "b24gRW5naW5lDQpHeXJvDQpHeXJvDQpHeXJvDQpHeXJvDQpGdXNpb24gRW5naW5lDQpGdXNpb24g"
            + "RW5naW5lDQpGdXNpb24gRW5naW5lDQpGbGFtZXINCklTIEFtbW8gTUcgLSBGdWxsDQoNCkhlYWQ6"
            + "DQpMaWZlIFN1cHBvcnQNClNlbnNvcnMNCkNvY2twaXQNCi1FbXB0eS0NClNlbnNvcnMNCkxpZmUg"
            + "U3VwcG9ydA0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVt"
            + "cHR5LQ0KDQpMZWZ0IExlZzoNCkhpcA0KVXBwZXIgTGVnIEFjdHVhdG9yDQpMb3dlciBMZWcgQWN0"
            + "dWF0b3INCkZvb3QgQWN0dWF0b3INCkhlYXQgU2luaw0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5"
            + "LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KLUVtcHR5LQ0KDQpSaWdodCBMZWc6DQpIaXAN"
            + "ClVwcGVyIExlZyBBY3R1YXRvcg0KTG93ZXIgTGVnIEFjdHVhdG9yDQpGb290IEFjdHVhdG9yDQpI"
            + "ZWF0IFNpbmsNCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1FbXB0eS0NCi1F"
            + "bXB0eS0NCi1FbXB0eS0NCg0KDQpPdmVydmlldzogVGhlIEZsZWEgd2FzIHRoZSBkaXJlY3QgZGVj"
            + "ZW5kYW50IG9mIHRoZSBGcmVlIFdvcmxkcyBMZWFndWUgZmlyc3Qgc2NvdXQgJ01lY2gsIHRoZSBU"
            + "cm9vcGVyLiBOZWl0aGVyIHRoZSBUcm9vcGVyIG5vciB0aGUgRmxlYSB3b3VsZCBpbXByZXNzIGFu"
            + "eSBtaWxpdGFyeSBhbmFseXN0cywgdGhvdWdoLCB3aXRoIHRoZWlyIGdlbmVyYWxseSBzbG93IHNw"
            + "ZWVkIGFuZCBmcmFnaWxlIGFybW9yIG1ha2luZyB0aGVtIGVhc3kgcHJleSBmb3IgZXZlbiB0aGUg"
            + "bGlnaHRlc3Qgb3Bwb3NpbmcgQmF0dGxlTWVjaHMuIFdpdGggcHJvZHVjdGlvbiBvZiB0aGUgRmxl"
            + "YSBjZWFzaW5nIGV2ZW4gYmVmb3JlIHRoZSBzdGFydCBvZiB0aGUgU3VjY2Vzc2lvbiBXYXJzLCB0"
            + "aGlzIGdlbmVyYWxseSB1bnBvcHVsYXIgZGVzaWduIHNlZW1lZCBkZXN0aW5lZCBmb3IgdGhlIGhp"
            + "c3RvcnkgYm9va3MgdW50aWwgdGhlIGFwcGVhcmFuY2Ugb2YgV29sZidzIERyYWdvb25zIGFuZCB0"
            + "aGVpciBtYW55IGFuY2llbnQgQmF0dGxlTWVjaHMsIEZsZWEgaW5jbHVkZWQuDQoNCkNhcGFiaWxp"
            + "dGllczogVGhlIEZsZWEsIGFzIGEgJ01lY2ggd2l0aCBtYW55IGRpZmZlcmVudCB2YXJpYW50cywg"
            + "Y2FuIGFjY29tcGxpc2ggbWFueSBkaWZmZXJlbnQgcm9sZXMuIEhvd2V2ZXIsIG1vc3Qgb2YgdGhl"
            + "c2UgbW9kZWxzIHdvcmsgYmVzdCBpbiBpbmZhbnRyeSBzdXBwb3J0IHJvbGVzLCB3aGVyZSB0aGUg"
            + "aGVhdmllciBhcm1vciBhbmQgZmlyZXBvd2VyIG9mIHRoZSAnTWVjaCBtYWtlIGl0IGEgdmFsdWVk"
            + "IGZyaWVuZCBhbmQgYSBkYW5nZXJvdXMgZm9lLiBUaGUgZmlyZXBvd2VyIG9mIHRoZSBGbGVhIGl0"
            + "c2VsZiBpcyBzdHJvbmdlciB0aGFuIG9uZSBtaWdodCBzdXNwZWN0OyBpZiBjb3JuZXJlZCwgaW5h"
            + "dHRlbnRpdmUgc2NvdXQgJ01lY2hzIGNhbiBxdWlja2x5IGZpbmQgdGhlbXNlbHZlcyBhdCB0aGUg"
            + "bWVyY3kgb2YgYSBGbGVhLg0KDQpEZXBsb3ltZW50OiBUaGUgbWFpbiBFYXJ0aHdlcmtzIGZhY3Rv"
            + "cnkgZm9yIHRoZSBGbGVhIHdhcyByZW9wZW5lZCBmb2xsb3dpbmcgdGhlIHNpZ25pbmcgb2YgYW4g"
            + "ZXhjbHVzaXZlIGNvbnRyYWN0IHdpdGggdGhlIFdvbGYncyBEcmFnb29ucy4gSW5pdGlhbGx5IG9u"
            + "bHkgcHJvZHVjaW5nIHJlcGxhY2VtZW50IHBhcnRzIGFuZCBhbiBvY2Nhc2lvbmFsIHJlcGxhY2Vt"
            + "ZW50ICdNZWNoLCBFYXJ0aHdlcmtzIGV2ZW50dWFsbHkgb3BlbmVkIGEgc2Vjb25kIGxpbmUgdG8g"
            + "a2VlcCB1cCB3aXRoIGluY3JlYXNpbmcgZGVtYW5kLiBFeHBpcmF0aW9uIG9mIHRoZSBEcmFnb29u"
            + "IGNvbnRyYWN0IGluIDMwNDEgd2FzIHdlbGNvbWVkIGJ5IEVhcnRod2Vya3M7IHRoZSBwdWJsaWNp"
            + "dHkgZ2FpbmVkIHRocm91Z2ggdGhlIERyYWdvb25zIGhhZCBjcmVhdGVkIGEgaGVhbHRoeSBtYXJr"
            + "ZXQgZm9yIHRoZSBGbGVhLiBBIGNvbW1vbiB2YXJpYW50LCB0aGUgRkxFLTE1IHdhcyBhIGRlZGlj"
            + "YXRlZCBhbnRpLWluZmFudHJ5IHZhcmlhbnQgZm91bmQgd2l0aGluIHRoZSBGcmVlIFdvcmxkcyBM"
            + "ZWFndWUgYW5kIERyYWdvb24gZm9yY2VzLiBJdHMgcm9sZSBzYXcgbG93IGF0dHJpdGlvbiByYXRl"
            + "cywgYW5kIHRoaXMgb2xkZXIgbW9kZWwgcmVtYWluZWQgaW4gc2VydmljZSB1bnRpbCB0aGUgZW5k"
            + "IG9mIHRoZSBKaWhhZC4NCg0Kc3lzdGVtbWFudWZhY3R1cmVyOkNIQVNTSVM6RWFydGh3ZXJrcw0K"
            + "c3lzdGVtbW9kZTpDSEFTU0lTOlRyb29wZXINCnN5c3RlbW1hbnVmYWN0dXJlcjpFTkdJTkU6R00N"
            + "CnN5c3RlbW1vZGU6RU5HSU5FOjEyMA0Kc3lzdGVtbWFudWZhY3R1cmVyOkFSTU9SOkxpdmluZ3N0"
            + "b24NCnN5c3RlbW1vZGU6QVJNT1I6Q2VyYW1pY3MNCnN5c3RlbW1hbnVmYWN0dXJlcjpDT01NVU5J"
            + "Q0FUSU9OUzpOZWlsDQpzeXN0ZW1tb2RlOkNPTU1VTklDQVRJT05TOjIwMDANCnN5c3RlbW1hbnVm"
            + "YWN0dXJlcjpUQVJHRVRJTkc6RGFsYmFuDQpzeXN0ZW1tb2RlOlRBUkdFVElORzpIaVJlei1CDQo="
        );
    }

    public static Entity getHeavyTrackedApcStandard() {
        return parseBase64BlkFile("I2J1aWxkaW5nIGJsb2NrIGRhdGEgZmlsZQo8QmxvY2tWZXJzaW9uPgoxCjwvQmxvY2tWZXJzaW9u"
            + "PgojV3JpdGUgdGhlIHZlcnNpb24gbnVtYmVyIGp1c3QgaW4gY2FzZS4uLgo8VmVyc2lvbj4KTUFN"
            + "MAo8L1ZlcnNpb24+CjxVbml0VHlwZT4KVGFuawo8L1VuaXRUeXBlPgo8TmFtZT4KSGVhdnkgVHJh"
            + "Y2tlZCBBUEMKPC9OYW1lPgo8TW9kZWw+CihTdGFuZGFyZCkKPC9Nb2RlbD4KPFRvbm5hZ2U+CjIw"
            + "CjwvVG9ubmFnZT4KPGNydWlzZU1QPgo1CjwvY3J1aXNlTVA+CjxBcm1vcj4KMjAKMTMKMTMKMTAK"
            + "PC9Bcm1vcj4KPEZyb250IEVxdWlwbWVudD4KSVNNYWNoaW5lIEd1bgpJU01hY2hpbmUgR3VuCjwv"
            + "RnJvbnQgRXF1aXBtZW50Pgo8dHJhbnNwb3J0ZXJzPgpUcm9vcFNwYWNlOjYKPC90cmFuc3BvcnRl"
            + "cnM+CjxCb2R5IEVxdWlwbWVudD4KSVNNRyBBbW1vICgxMDApCjwvQm9keSBFcXVpcG1lbnQ+Cjx0"
            + "eXBlPgpJUyBMZXZlbCAxCjwvdHlwZT4KPHllYXI+CjI0NzAKPC95ZWFyPgo8aW50ZXJuYWxfdHlw"
            + "ZT4KMAo8L2ludGVybmFsX3R5cGU+Cjxhcm1vcl90eXBlPgowCjwvYXJtb3JfdHlwZT4KPGVuZ2lu"
            + "ZV90eXBlPgoxCjwvZW5naW5lX3R5cGU+Cjxtb3Rpb25fdHlwZT4KVHJhY2tlZAo8L21vdGlvbl90"
            + "eXBlPgo8c291cmNlPgpTdGFyIExlYWd1ZQo8L3NvdXJjZT4K");
    }

    public static Entity getHeavyTrackedApcMg() {
        return parseBase64BlkFile("I2J1aWxkaW5nIGJsb2NrIGRhdGEgZmlsZQo8QmxvY2tWZXJzaW9uPgoxCjwvQmxvY2tWZXJzaW9u"
            + "PgojV3JpdGUgdGhlIHZlcnNpb24gbnVtYmVyIGp1c3QgaW4gY2FzZS4uLgo8VmVyc2lvbj4KTUFN"
            + "MAo8L1ZlcnNpb24+CjxVbml0VHlwZT4KVGFuawo8L1VuaXRUeXBlPgo8TmFtZT4KSGVhdnkgVHJh"
            + "Y2tlZCBBUEMKPC9OYW1lPgo8TW9kZWw+CihNRykKPC9Nb2RlbD4KPFRvbm5hZ2U+CjIwCjwvVG9u"
            + "bmFnZT4KPGNydWlzZU1QPgo1CjwvY3J1aXNlTVA+CjxBcm1vcj4KMjAKMTMKMTMKMTAKPC9Bcm1v"
            + "cj4KPEZyb250IEVxdWlwbWVudD4KSVNNYWNoaW5lIEd1bgpJU01hY2hpbmUgR3VuCjwvRnJvbnQg"
            + "RXF1aXBtZW50Pgo8UmlnaHQgRXF1aXBtZW50PgpJU01hY2hpbmUgR3VuCjwvUmlnaHQgRXF1aXBt"
            + "ZW50Pgo8TGVmdCBFcXVpcG1lbnQ+CklTTWFjaGluZSBHdW4KPC9MZWZ0IEVxdWlwbWVudD4KPFJl"
            + "YXIgRXF1aXBtZW50PgpJU01hY2hpbmUgR3VuCklTTWFjaGluZSBHdW4KPC9SZWFyIEVxdWlwbWVu"
            + "dD4KPHRyYW5zcG9ydGVycz4KVHJvb3BTcGFjZTozCjwvdHJhbnNwb3J0ZXJzPgo8Qm9keSBFcXVp"
            + "cG1lbnQ+CklTTUcgQW1tbyAoMjAwKQpJU01HIEFtbW8gKDEwMCkKPC9Cb2R5IEVxdWlwbWVudD4K"
            + "PHR5cGU+CklTIExldmVsIDEKPC90eXBlPgo8eWVhcj4KMjQ3MAo8L3llYXI+CjxpbnRlcm5hbF90"
            + "eXBlPgowCjwvaW50ZXJuYWxfdHlwZT4KPGFybW9yX3R5cGU+CjAKPC9hcm1vcl90eXBlPgo8ZW5n"
            + "aW5lX3R5cGU+CjEKPC9lbmdpbmVfdHlwZT4KPG1vdGlvbl90eXBlPgpUcmFja2VkCjwvbW90aW9u"
            + "X3R5cGU+Cjxzb3VyY2U+ClN0YXIgTGVhZ3VlCjwvc291cmNlPgo=");
    }
}
