package com.bx.implatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bx.implatform.dto.TalkAddDTO;
import com.bx.implatform.dto.TalkCommentDTO;
import com.bx.implatform.dto.TalkQuery;
import com.bx.implatform.dto.TalkUpdateDTO;
import com.bx.implatform.entity.Talk;
import com.bx.implatform.vo.TalkDetailListVO;
import com.bx.implatform.vo.TalkDetailVO;
import com.bx.implatform.vo.TalkRemindVO;
import com.bx.implatform.vo.TalkVO;

import java.util.List;

/**
 * 朋友圈动态服务接口
 *
 * @author im-platform
 */
public interface TalkService extends IService<Talk> {

    /**
     * 新增动态
     *
     * @param dto 新增动态请求参数
     */
    void add(TalkAddDTO dto);

    /**
     * 更新动态
     *
     * @param dto 修改动态请求参数
     * @return 是否更新成功
     */
    boolean updateTalk(TalkUpdateDTO dto);

    /**
     * 删除动态
     *
     * @param talkId 动态id
     */
    void deleteTalk(Long talkId);

    /**
     * 查询动态列表
     *
     * @param query 查询参数
     * @return 动态详情列表
     */
    TalkDetailListVO query(TalkQuery query);

    /**
     * 查询自己动态
     *
     * @param query 查询参数
     * @return 自己的动态列表
     */
    List<TalkVO> querySelf(TalkQuery query);

    /**
     * 查询好友动态
     *
     * @param query 查询参数
     * @return 好友的动态列表
     */
    List<TalkVO> queryFriend(TalkQuery query);

    /**
     * 已读动态提醒
     * <p>
     * 标记当前用户已读取动态列表，可用于清除未读提醒等
     * </p>
     */
    void markReadNotify();

    /**
     * 动态点赞
     *
     * @param talkId 动态id
     */
    void like(Long talkId);

    /**
     * 取消点赞
     *
     * @param talkId 动态id
     */
    void unlike(Long talkId);

    /**
     * 新增动态评论
     *
     * @param dto 评论请求参数
     * @return 评论id
     */
    Long addComment(TalkCommentDTO dto);

    /**
     * 删除动态评论
     *
     * @param commentId 评论id
     */
    void deleteComment(Long commentId);

    /**
     * 拉取未读的动态提醒
     *
     * @return 未读提醒信息
     */
    TalkRemindVO loadOfflineTalkRemind();

    /**
     * 查询动态详情
     *
     * @param talkId 动态id
     * @return 动态详情
     */
    TalkDetailVO getDetail(Long talkId);
}
