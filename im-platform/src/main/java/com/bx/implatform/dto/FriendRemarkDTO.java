package com.bx.implatform.dto;

import com.bx.implatform.contant.PatternText;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
@Schema(description = "修改好友备注")
public class FriendRemarkDTO {

    @Schema(description = "好友用户id")
    @NotNull(message = "好友id不可为空")
    private Long friendId;

    @Schema(description = "备注名")
    @Pattern( regexp= PatternText.NICK_NAME,message = "备注名包含不合法字符")
    @Length(max = 32, message = "备注名不能大于32字符")
    private String remarkNickName;

}
