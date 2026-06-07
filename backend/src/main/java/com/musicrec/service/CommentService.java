package com.musicrec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musicrec.common.Result;
import com.musicrec.entity.Comment;
import com.musicrec.entity.User;
import com.musicrec.mapper.CommentMapper;
import com.musicrec.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentService {
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final HttpServletRequest request;

    public CommentService(CommentMapper commentMapper, UserMapper userMapper,
                          HttpServletRequest request) {
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
        this.request = request;
    }

    public Result<?> addComment(Long songId, String content, Long parentId) {
        Long userId = (Long) request.getAttribute("userId");
        Comment c = new Comment();
        c.setUserId(userId); c.setSongId(songId);
        c.setContent(content); c.setParentId(parentId);
        commentMapper.insert(c);
        return Result.success("评论成功");
    }

    public Result<?> deleteComment(Long id) {
        Long userId = (Long) request.getAttribute("userId");
        Comment c = commentMapper.selectById(id);
        if (c == null) return Result.error("评论不存在");
        if (!c.getUserId().equals(userId)) return Result.error("只能删除自己的评论");
        commentMapper.deleteById(id);
        return Result.success("删除成功");
    }

    public Result<List<Comment>> getComments(Long songId) {
        LambdaQueryWrapper<Comment> qw = new LambdaQueryWrapper<>();
        qw.eq(Comment::getSongId, songId).orderByDesc(Comment::getCreatedAt);
        List<Comment> all = commentMapper.selectList(qw);

        // 填充用户名
        Set<Long> userIds = all.stream().map(Comment::getUserId).collect(Collectors.toSet());
        Map<Long, String> userMap = new HashMap<>();
        for (Long uid : userIds) {
            User u = userMapper.selectById(uid);
            if (u != null) userMap.put(uid, u.getNickname() != null ? u.getNickname() : u.getUsername());
        }
        for (Comment c : all) c.setUsername(userMap.getOrDefault(c.getUserId(), "未知"));

        // 组装树形: parent_id==null 为一级评论，其余归入 replies
        Map<Long, Comment> map = new HashMap<>();
        List<Comment> roots = new ArrayList<>();
        for (Comment c : all) {
            map.put(c.getId(), c);
            c.setReplies(new ArrayList<>());
        }
        for (Comment c : all) {
            if (c.getParentId() == null) roots.add(c);
            else {
                Comment parent = map.get(c.getParentId());
                if (parent != null) parent.getReplies().add(c);
            }
        }
        return Result.success(roots);
    }
}
