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

import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MtfFile;
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
}
