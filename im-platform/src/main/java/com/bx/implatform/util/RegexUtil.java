package com.bx.implatform.util;

import com.bx.implatform.contant.PatternText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则校验
 * @author Blue
 * @version 1.0
 * @date 2025-03-21
 */
public class RegexUtil {

    public static Boolean isPhone(String text){
        // 编译正则表达式
        Pattern pattern = Pattern.compile(PatternText.PHONE);
        // 创建matcher对象
        Matcher matcher = pattern.matcher(text);
        // 进行匹配检查
        return matcher.matches();
    }

    public static Boolean isEmail(String text){
        // 编译正则表达式
        Pattern pattern = Pattern.compile(PatternText.EMAIL);
        // 创建matcher对象
        Matcher matcher = pattern.matcher(text);
        // 进行匹配检查
        return matcher.matches();
    }
}
