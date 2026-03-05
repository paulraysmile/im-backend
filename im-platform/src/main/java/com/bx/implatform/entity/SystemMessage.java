package com.bx.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


/**
 * 系统消息
 *
 * @author Blue 
 * @since 1.0.0 2024-08-11
 */

@Data
@TableName("im_system_message")
public class SystemMessage  {
	/**
	* id
	*/
	@TableId
	private Long id;

	/**
	* 标题
	*/
	private String title;

	/**
	* 封面图片
	*/
	private String coverUrl;

	/**
	* 简介
	*/
	private String intro;

	/**
	* 内容类型 0:富文本  1:外部链接
	*/
	private Integer contentType;

	/**
	* 富文本内容，base64编码
	*/
	private String richText;

	/**
	* 外部链接
	*/
	private String externLink;

	@TableLogic
	private Integer deleted;

}