package com.xiaozhi.stackchan.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaozhi.common.model.bo.StackChanPostBO;
import com.xiaozhi.common.model.bo.StackChanPostCommentBO;
import com.xiaozhi.stackchan.dal.mysql.dataobject.StackChanPostCommentDO;
import com.xiaozhi.stackchan.dal.mysql.dataobject.StackChanPostDO;
import com.xiaozhi.stackchan.dal.mysql.mapper.StackChanPostCommentMapper;
import com.xiaozhi.stackchan.dal.mysql.mapper.StackChanPostMapper;
import com.xiaozhi.stackchan.service.StackChanPostService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * StackChan 社区帖子与评论实现。
 * <p>
 * mac 一律先归一化成「小写 + 冒号」再读写（与舞蹈、App 设备侧同一口径）；
 * 删除帖子时显式清理评论，对应 Go 版 {@code device_post_comment} 上的外键级联删除。
 */
@Slf4j
@Service
public class StackChanPostServiceImpl implements StackChanPostService {

    /** 归一化前的 MAC：12 位十六进制，忽略分隔符 */
    private static final Pattern MAC_HEX = Pattern.compile("^[0-9A-Fa-f]{12}$");

    private static final int MAX_CONTENT_IMAGE_LENGTH = 512;

    @Resource
    private StackChanPostMapper stackChanPostMapper;

    @Resource
    private StackChanPostCommentMapper stackChanPostCommentMapper;

    @Override
    public StackChanPostBO get(Long postId) {
        StackChanPostDO dataObject = postId == null ? null : stackChanPostMapper.selectById(postId);
        return dataObject == null ? null : toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StackChanPostBO create(String mac, String contentText, String contentImage) {
        StackChanPostDO dataObject = new StackChanPostDO();
        dataObject.setMac(normalizeMac(mac));
        dataObject.setContentText(requireContentText(contentText));
        dataObject.setContentImage(requireContentImage(contentImage));
        stackChanPostMapper.insert(dataObject);
        log.info("StackChan 帖子已发布: deviceId={}, postId={}", dataObject.getMac(), dataObject.getId());
        return toBO(dataObject);
    }

    @Override
    public List<StackChanPostBO> listPage(int page, int pageSize) {
        // 时间线不需要总数，跳过 count 查询
        Page<StackChanPostDO> pageParam = new Page<>(page, pageSize, false);
        List<StackChanPostDO> posts = stackChanPostMapper.selectPage(pageParam,
                Wrappers.<StackChanPostDO>lambdaQuery()
                    .orderByDesc(StackChanPostDO::getCreateTime)
                    .orderByDesc(StackChanPostDO::getId))
            .getRecords();
        if (posts.isEmpty()) {
            return new ArrayList<>();
        }
        // 一次性把这一页帖子的评论捞出来，避免逐帖查询
        Map<Long, List<StackChanPostCommentBO>> commentsByPost = commentsByPostIds(
            posts.stream().map(StackChanPostDO::getId).toList());
        List<StackChanPostBO> result = new ArrayList<>(posts.size());
        for (StackChanPostDO post : posts) {
            StackChanPostBO bo = toBO(post);
            bo.setPostCommentList(commentsByPost.getOrDefault(post.getId(), new ArrayList<>()));
            result.add(bo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long postId, String mac) {
        StackChanPostDO dataObject = postId == null ? null : stackChanPostMapper.selectById(postId);
        if (dataObject == null) {
            throw new IllegalArgumentException("Post does not exist");
        }
        if (!normalizeMac(mac).equalsIgnoreCase(dataObject.getMac())) {
            throw new IllegalArgumentException("No authority to delete this post");
        }
        stackChanPostCommentMapper.delete(Wrappers.<StackChanPostCommentDO>lambdaQuery()
            .eq(StackChanPostCommentDO::getPostId, postId));
        stackChanPostMapper.deleteById(postId);
        log.info("StackChan 帖子已删除: postId={}, deviceId={}", postId, dataObject.getMac());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByAdmin(Long postId) {
        StackChanPostDO dataObject = postId == null ? null : stackChanPostMapper.selectById(postId);
        if (dataObject == null) {
            return;
        }
        stackChanPostCommentMapper.delete(Wrappers.<StackChanPostCommentDO>lambdaQuery()
            .eq(StackChanPostCommentDO::getPostId, postId));
        stackChanPostMapper.deleteById(postId);
        log.info("StackChan 帖子已被管理端删除: postId={}, deviceId={}", postId, dataObject.getMac());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StackChanPostCommentBO createComment(Long postId, String mac, String content) {
        if (postId == null) {
            throw new IllegalArgumentException("postId cannot be empty");
        }
        if (stackChanPostMapper.selectById(postId) == null) {
            throw new IllegalArgumentException("Post does not exist");
        }
        StackChanPostCommentDO dataObject = new StackChanPostCommentDO();
        dataObject.setPostId(postId);
        dataObject.setMac(normalizeMac(mac));
        dataObject.setContent(trimToNull(content));
        stackChanPostCommentMapper.insert(dataObject);
        log.info("StackChan 评论已发表: postId={}, commentId={}, deviceId={}",
            postId, dataObject.getId(), dataObject.getMac());
        return toCommentBO(dataObject);
    }

    @Override
    public List<StackChanPostCommentBO> listComments(Long postId, int page, int pageSize) {
        if (postId == null) {
            return new ArrayList<>();
        }
        Page<StackChanPostCommentDO> pageParam = new Page<>(page, pageSize, false);
        return stackChanPostCommentMapper.selectPage(pageParam,
                Wrappers.<StackChanPostCommentDO>lambdaQuery()
                    .eq(StackChanPostCommentDO::getPostId, postId)
                    .orderByAsc(StackChanPostCommentDO::getCreateTime)
                    .orderByAsc(StackChanPostCommentDO::getId))
            .getRecords()
            .stream()
            .map(this::toCommentBO)
            .toList();
    }

    @Override
    public long countComments(Long postId) {
        if (postId == null) {
            return 0L;
        }
        Long total = stackChanPostCommentMapper.selectCount(Wrappers.<StackChanPostCommentDO>lambdaQuery()
            .eq(StackChanPostCommentDO::getPostId, postId));
        return total == null ? 0L : total;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long postId, Long commentId, String mac) {
        StackChanPostCommentDO dataObject = commentId == null ? null : stackChanPostCommentMapper.selectById(commentId);
        if (dataObject == null || !dataObject.getPostId().equals(postId)) {
            throw new IllegalArgumentException("Comment does not exist");
        }
        if (!normalizeMac(mac).equalsIgnoreCase(dataObject.getMac())) {
            throw new IllegalArgumentException("No authority to delete this comment");
        }
        stackChanPostCommentMapper.deleteById(commentId);
        log.info("StackChan 评论已删除: postId={}, commentId={}, deviceId={}", postId, commentId, dataObject.getMac());
    }

    private Map<Long, List<StackChanPostCommentBO>> commentsByPostIds(List<Long> postIds) {
        List<StackChanPostCommentDO> comments = stackChanPostCommentMapper.selectList(
            Wrappers.<StackChanPostCommentDO>lambdaQuery()
                .in(StackChanPostCommentDO::getPostId, postIds)
                .orderByAsc(StackChanPostCommentDO::getCreateTime)
                .orderByAsc(StackChanPostCommentDO::getId));
        Map<Long, List<StackChanPostCommentBO>> grouped = new LinkedHashMap<>();
        for (StackChanPostCommentDO comment : comments) {
            grouped.computeIfAbsent(comment.getPostId(), key -> new ArrayList<>()).add(toCommentBO(comment));
        }
        return grouped;
    }

    private String requireContentText(String rawContentText) {
        if (!StringUtils.hasText(rawContentText)) {
            throw new IllegalArgumentException("content_text cannot be empty");
        }
        return rawContentText.trim();
    }

    private String requireContentImage(String rawContentImage) {
        String contentImage = trimToNull(rawContentImage);
        if (contentImage != null && contentImage.length() > MAX_CONTENT_IMAGE_LENGTH) {
            throw new IllegalArgumentException(
                "content_image is too long (max " + MAX_CONTENT_IMAGE_LENGTH + " characters)");
        }
        return contentImage;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 统一成小写冒号格式：设备侧 Header（AA-BB-…/AA:BB:…）与 App 侧 12 位纯十六进制都收敛到这里。
     */
    private String normalizeMac(String rawMac) {
        if (!StringUtils.hasText(rawMac)) {
            throw new IllegalArgumentException("Device MAC cannot be empty");
        }
        String hex = rawMac.trim().replaceAll("[^0-9A-Fa-f]", "");
        if (!MAC_HEX.matcher(hex).matches()) {
            throw new IllegalArgumentException("Invalid device MAC: " + rawMac);
        }
        hex = hex.toLowerCase(Locale.ROOT);
        StringBuilder mac = new StringBuilder(17);
        for (int i = 0; i < hex.length(); i += 2) {
            if (i > 0) {
                mac.append(':');
            }
            mac.append(hex, i, i + 2);
        }
        return mac.toString();
    }

    private StackChanPostBO toBO(StackChanPostDO dataObject) {
        StackChanPostBO bo = new StackChanPostBO();
        bo.setId(dataObject.getId());
        bo.setMac(dataObject.getMac());
        bo.setContentText(dataObject.getContentText());
        bo.setContentImage(dataObject.getContentImage());
        bo.setCreateTime(dataObject.getCreateTime());
        return bo;
    }

    private StackChanPostCommentBO toCommentBO(StackChanPostCommentDO dataObject) {
        StackChanPostCommentBO bo = new StackChanPostCommentBO();
        bo.setId(dataObject.getId());
        bo.setPostId(dataObject.getPostId());
        bo.setMac(dataObject.getMac());
        bo.setContent(dataObject.getContent());
        bo.setCreateTime(dataObject.getCreateTime());
        return bo;
    }
}
