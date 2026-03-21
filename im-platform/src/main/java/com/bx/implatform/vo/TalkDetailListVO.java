package com.bx.implatform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 动态详情列表VO
 *
 * @author im-platform
 */
@Data
@Schema(description = "动态详情列表")
public class TalkDetailListVO {

    @Schema(description = "动态详情列表")
    private List<TalkDetailVO> detaiList;

    @Schema(description = "用户展示信息Map，key为用户id")
    private Map<Long, UserShowVO> userShowMap;
}
