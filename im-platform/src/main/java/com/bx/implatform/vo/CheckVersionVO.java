package com.bx.implatform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author Blue
 * @version 1.0
 */
@Data
@Schema(description = "检查更新")
public class CheckVersionVO {

    @Schema(description = "是否最新版本")
    private Boolean isLatestVersion;

    @Schema(description = "更新日志")
    private List<String> changeLog;
}
