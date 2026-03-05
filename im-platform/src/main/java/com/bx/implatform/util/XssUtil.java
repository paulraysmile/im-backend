package com.bx.implatform.util;

import com.bx.implatform.contant.PatternText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class XssUtil {

    private XssUtil() {
    }

    private static final Pattern PATTERN = Pattern.compile(PatternText.XSS);

    public static boolean checkXss(String inputString) {
        if (inputString != null) {
            Matcher matcher = PATTERN.matcher(inputString);
            return matcher.find();
        }
        return false;
    }
}
