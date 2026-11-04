package com.xiaozhi.stackchan.service;

import com.xiaozhi.common.model.bo.StackChanPostBO;
import com.xiaozhi.common.model.bo.StackChanPostCommentBO;

import java.util.List;

/**
 * StackChan 社区帖子与评论的读写。
 * <p>
 * 帖子是公开时间线（不按 MAC 过滤），但删除只允许帖子/评论的发布设备操作：
 * 写入口一律由 Controller 传入鉴权得到的小写冒号 MAC，本层再归一化后落库。
 */
public interface StackChanPostService {

    /** 按帖子 ID 查询，不存在返回 null */
    StackChanPostBO get(Long postId);

    /** 发布帖子：mac 必填，contentText 必填，contentImage 最长 512 */
    StackChanPostBO create(String mac, String contentText, String contentImage);

    /** 帖子分页：按创建时间倒序，每条带自己的评论列表（评论按创建时间正序） */
    List<StackChanPostBO> listPage(int page, int pageSize);

    /** 删除帖子（同时清理其评论）；只允许发帖设备本人删除 */
    void delete(Long postId, String mac);

    /** 管理端删除帖子（不做设备归属校验，同时清理评论） */
    void deleteByAdmin(Long postId);

    /** 发表评论：帖子必须存在 */
    StackChanPostCommentBO createComment(Long postId, String mac, String content);

    /** 某帖子的评论分页，按创建时间正序 */
    List<StackChanPostCommentBO> listComments(Long postId, int page, int pageSize);

    /** 某帖子的评论总数（分页响应里的 total） */
    long countComments(Long postId);

    /** 删除评论；只允许评论发布设备本人删除，且评论必须属于该帖子 */
    void deleteComment(Long postId, Long commentId, String mac);
}
