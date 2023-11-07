package org.sunbird.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
@RunWith(JUnit4.class)
public class Base64UtilTest {

    @Test
    public void testEncodingAndDecoding() {
        String originalString = "Hello, World!";
        byte[] originalBytes = originalString.getBytes();

        // Test encoding and then decoding the string
        String encodedString = Base64Util.encodeToString(originalBytes, Base64Util.DEFAULT);
        byte[] decodedBytes = Base64Util.decode(encodedString, Base64Util.DEFAULT);

        assertEquals(originalString, new String(decodedBytes));

        // Test encoding and then decoding with NO_WRAP flag
        String encodedStringNoWrap = Base64Util.encodeToString(originalBytes, Base64Util.NO_WRAP);
        byte[] decodedBytesNoWrap = Base64Util.decode(encodedStringNoWrap, Base64Util.NO_WRAP);

        assertEquals(originalString, new String(decodedBytesNoWrap));

        // Test encoding and then decoding with URL_SAFE flag
        String encodedStringUrlSafe = Base64Util.encodeToString(originalBytes, Base64Util.URL_SAFE);
        byte[] decodedBytesUrlSafe = Base64Util.decode(encodedStringUrlSafe, Base64Util.URL_SAFE);

        assertEquals(originalString, new String(decodedBytesUrlSafe));
    }

    @Test
    public void testCustomFlagsEncodingAndDecoding() {
        String originalString = "Custom Flags Test";
        byte[] originalBytes = originalString.getBytes();

        // Test encoding and then decoding with custom flags
        int customFlags = Base64Util.NO_PADDING | Base64Util.URL_SAFE;
        String encodedString = Base64Util.encodeToString(originalBytes, customFlags);
        byte[] decodedBytes = Base64Util.decode(encodedString, customFlags);

        assertEquals(originalString, new String(decodedBytes));
    }

    @Test
    public void testEncodeDecodeWithDifferentInput() {
        // Test encoding and decoding with random non-empty string
        String inputString = "This is a test string for encoding and decoding!";
        byte[] inputBytes = inputString.getBytes();
        String encodedString = Base64Util.encodeToString(inputBytes, Base64Util.DEFAULT);
        byte[] decodedBytes = Base64Util.decode(encodedString, Base64Util.DEFAULT);
        assertEquals(inputString, new String(decodedBytes));

        // Test encoding and decoding with special characters
        String specialCharacters = "!@#$%^&*()_+{}[]:\";'<>?,./";
        byte[] specialCharsBytes = specialCharacters.getBytes();
        String encodedSpecialChars = Base64Util.encodeToString(specialCharsBytes, Base64Util.DEFAULT);
        byte[] decodedSpecialChars = Base64Util.decode(encodedSpecialChars, Base64Util.DEFAULT);
        assertEquals(specialCharacters, new String(decodedSpecialChars));

        // Test encoding and decoding with large byte array
        byte[] largeByteArray = new byte[1024 * 1024]; // 1 MB array
        new Random().nextBytes(largeByteArray);
        String encodedLargeArray = Base64Util.encodeToString(largeByteArray, Base64Util.DEFAULT);
        byte[] decodedLargeArray = Base64Util.decode(encodedLargeArray, Base64Util.DEFAULT);
        assertArrayEquals(largeByteArray, decodedLargeArray);
    }

    @Test
    public void testEncodingAndDecodingWithEmptyString() {
        String originalString = "";
        byte[] originalBytes = originalString.getBytes();

        // Test encoding and then decoding the empty string
        String encodedString = Base64Util.encodeToString(originalBytes, Base64Util.DEFAULT);
        byte[] decodedBytes = Base64Util.decode(encodedString, Base64Util.DEFAULT);

        assertEquals(originalString, new String(decodedBytes));
    }

    @Test(expected = NullPointerException.class)
    public void testEncodingAndDecodingWithNullInput() {
        String originalString = null;

        // This should throw a NullPointerException
        Base64Util.encodeToString(originalString.getBytes(), Base64Util.DEFAULT);
    }


//    private String inputString;
//
//    public Base64UtilTest(String inputString) {
//        this.inputString = inputString;
//    }
//
//    @Parameterized.Parameters
//    public static Collection<Object[]> data() {
//        return Arrays.asList(new Object[][]{
//                {"Hello, World!"},
//                {"Custom Flags Test"},
//                {"This is a test string for encoding and decoding!"},
//                {"!@#$%^&*()_+{}[]:\";'<>?,./"},
//                {""}
//        });
//    }
//
//    @Test
//    public void testEncodingAndDecoding() {
//        byte[] originalBytes = inputString.getBytes();
//
//        // Test encoding and then decoding the string
//        String encodedString = Base64Util.encodeToString(originalBytes, Base64Util.DEFAULT);
//        byte[] decodedBytes = Base64Util.decode(encodedString, Base64Util.DEFAULT);
//
//        assertEquals(inputString, new String(decodedBytes));
//    }
}
