package com.bx.implatform.controller;

import com.bx.implatform.config.props.AppProperties;
import com.bx.implatform.config.props.RegistrationProperties;
import com.bx.implatform.config.props.WebrtcProperties;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.exception.GlobalException;
import com.bx.implatform.result.Result;
import com.bx.implatform.result.ResultUtils;
import com.bx.implatform.vo.CheckVersionVO;
import com.bx.implatform.vo.SystemConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Tag(name = "系统相关")
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemController {

    private final WebrtcProperties webrtcProps;

    private final RegistrationProperties registrationProps;

    private final AppProperties appProps;

    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/config")
    @Operation(summary = "加载系统配置", description = "加载系统配置")
    public Result<SystemConfigVO> loadConfig() {
        SystemConfigVO config = new SystemConfigVO();
        config.setWebrtc(webrtcProps);
        config.setRegistration(registrationProps);
        config.setApp(appProps);
        config.setAppInReview(redisTemplate.hasKey(RedisKey.IM_APP_REVIEW));
        return ResultUtils.success(config);
    }

    @GetMapping("/checkVersion")
    @Operation(summary = "检查应用版本", description = "检查应用版本")
    public Result<CheckVersionVO> checkVersion(@RequestParam String version) {
        CheckVersionVO vo = new CheckVersionVO();
        if (compare(appProps.getVersion(), version) > 0) {
            vo.setIsLatestVersion(false);
            vo.setChangeLog(appProps.getChangeLog());
        } else {
            vo.setIsLatestVersion(true);
        }
        return ResultUtils.success(vo);
    }

    public int compare(String v1, String v2) {
        Pattern pattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher m1 = pattern.matcher(v1);
        Matcher m2 = pattern.matcher(v2);
        if (!m1.matches() || !m2.matches()) {
            throw new GlobalException("版本号格式异常");
        }
        int[] version1 = {Integer.parseInt(m1.group(1)), Integer.parseInt(m1.group(2)), Integer.parseInt(m1.group(3))};
        int[] version2 = {Integer.parseInt(m2.group(1)), Integer.parseInt(m2.group(2)), Integer.parseInt(m2.group(3))};
        for (int i = 0; i < 3; i++) {
            if (version1[i] != version2[i]) {
                return Integer.compare(version1[i], version2[i]);
            }
        }
        return 0;
    }

}
