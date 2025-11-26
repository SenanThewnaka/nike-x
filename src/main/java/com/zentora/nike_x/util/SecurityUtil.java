package com.zentora.nike_x.util;


import org.apache.commons.codec.digest.DigestUtils;
import org.mindrot.jbcrypt.BCrypt;

public class SecurityUtil {

    public static String hashPassword(String rawPassword){
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    public static boolean checkPassword(String rawPassword, String hashedPassword){
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }

    public static String hashData(String data){
        return DigestUtils.sha256Hex(data);
    }
}
