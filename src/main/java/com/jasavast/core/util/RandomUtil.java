package com.jasavast.core.util;

import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;

public class RandomUtil {
    public static String getRandomReset(){
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(new byte[64]);
        return RandomStringUtils.random(20,
                0,0,true,true,null,secureRandom);
    }
    public static String getRandomRef(){
        SecureRandom random = new SecureRandom();
        random.nextBytes(new byte[32]);
        return RandomStringUtils.random(6,0,0,true,true,null,random);
    }
}
