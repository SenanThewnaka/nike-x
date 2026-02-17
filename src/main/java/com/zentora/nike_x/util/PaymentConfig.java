package com.zentora.nike_x.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.DecimalFormat;

public class PaymentConfig {
    // PayHere Sandbox Credentials
    public static final String MERCHANT_ID = "1224614"; // Replace with actual Merchant ID
    public static final String MERCHANT_SECRET = "MTAzODE2NTk5NjMwNzQxNjcwMDk4MjYyNDEzNjM0MDI3NjU3ODky"; // Replace with
                                                                                                         // actual
                                                                                                         // Secret
    public static final String CURRENCY = "LKR";

    // PayHere App Credentials for Retrieval API (Status Check)
    public static final String APP_ID = ""; // Fill from PayHere
    public static final String APP_SECRET = ""; // Fill from PayHere

    // PayHere URL (Sandbox) (Not strictly needed for backend hash, but good to
    // have)
    // public static final String PAYHERE_URL =
    // "https://sandbox.payhere.lk/pay/checkout";

    public static String getMd5(String input) {
        try {
            MessageDigest manualMd5 = MessageDigest.getInstance("MD5");
            byte[] manualDigest = manualMd5.digest(input.getBytes());
            BigInteger manualNo = new BigInteger(1, manualDigest);
            String hashtext = manualNo.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext.toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getFormattedAmount(double amount) {
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(amount);
    }
}
