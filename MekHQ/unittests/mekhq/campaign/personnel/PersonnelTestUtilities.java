package mekhq.campaign.personnel;

import mekhq.TestUtilities;
import org.mockito.Mockito;

import java.io.InputStream;

public final class PersonnelTestUtilities {

    public static InputStream getTestAwardSet(){
        return TestUtilities.ParseBase64XmlFile("PD94bWwgdmVyc2lvbj0iMS4wIj8+Cjxhd2FyZHM+Cgk8YXdhcmQ+CgkJPG5hbWU+VGVz" +
                "dCBBd2FyZCAxPC9uYW1lPgoJCTxkZXNjcmlwdGlvbj5UZXN0IEF3YXJkIDEgZGVzY3JpcHRpb24uPC9kZXNjcmlwdGlvbj4KCQk8" +
                "cmliYm9uPlRlc3RBd2FyZDFfcmliYm9uMS5wbmc8L3JpYmJvbj4KCQk8cmliYm9uPlRlc3RBd2FyZDFfcmliYm9uMi5wbmc8L3Jp" +
                "YmJvbj4KCQk8eHA+MzwveHA+CgkJPHN0YWNrYWJsZT50cnVlPC9zdGFja2FibGU+Cgk8L2F3YXJkPgoJPGF3YXJkPgoJCTxuYW1l" +
                "PlRlc3QgQXdhcmQgMjwvbmFtZT4KCQk8ZGVzY3JpcHRpb24+VGVzdCBBd2FyZCAyIGRlc2NyaXB0aW9uLjwvZGVzY3JpcHRpb24+" +
                "CgkJPG1lZGFsPlRlc3RBd2FyZDJfbWVkYWwucG5nPC9tZWRhbD4KCQk8cmliYm9uPlRlc3RBd2FyZDJfcmliYm9uLnBuZzwvcmli" +
                "Ym9uPgoJCTx4cD4xPC94cD4KCTwvYXdhcmQ+CQkKPC9hd2FyZHM+");
    }

    public static Award getTestAward1(){
        AwardsFactory.getInstance().loadAwardsFromStream(getTestAwardSet(),"TestSet");
        return AwardsFactory.getInstance().generateNew("TestSet", "Test Award 1");
    }
}


