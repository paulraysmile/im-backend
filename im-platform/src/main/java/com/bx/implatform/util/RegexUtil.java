package com.bx.implatform.util;

import com.bx.implatform.contant.PatternText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则校验
 */
public class RegexUtil {

    private static final Pattern PHONE_PATTERN = Pattern.compile(PatternText.PHONE);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(PatternText.EMAIL);

    public static Boolean isPhone(String text){
        Matcher matcher = PHONE_PATTERN.matcher(text);
        return matcher.matches();
    }

    public static Boolean isEmail(String text){
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        return matcher.matches();
    }
}
