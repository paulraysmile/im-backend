package com.bx.implatform.vo;

import com.bx.imcommon.serializer.DateToLongSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.Date;

@Data
@Schema(description = "系统消息VO")
public class SystemMessageVO {

    @Schema(description = " 消息id")
    private Long id;

    @Schema(description = " 发送序列号")
    private Long SeqNo;

    @Length(max = 16,message = "标题长度不能大于16")
    @Schema(description = "标题")
    private String title;

    @Schema(description = "封面图片")
    private String coverUrl;

    @Length(max = 512,message = "简述长度不能大于512")
    @Schema(description = "简介")
    private String intro;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "消息内容类型 MessageType")
    private Integer type;

    @Schema(description = " 状态")
    private Integer status;

    @Schema(description = " 发送时间")
    @JsonSerialize(using = DateToLongSerializer.class)
    private Date sendTime;
}
