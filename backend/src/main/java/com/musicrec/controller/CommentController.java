package com.musicrec.controller;

import com.musicrec.common.Result;
import com.musicrec.entity.Comment;
import com.musicrec.service.CommentService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;
    public CommentController(CommentService commentService) { this.commentService = commentService; }

    @PostMapping
    public Result<?> add(@RequestBody Map<String, Object> body) {
        Long songId = Long.valueOf(body.get("songId").toString());
        String content = (String) body.get("content");
        Long parentId = body.containsKey("parentId") && body.get("parentId") != null
                ? Long.valueOf(body.get("parentId").toString()) : null;
        return commentService.addComment(songId, content, parentId);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return commentService.deleteComment(id);
    }

    @GetMapping("/song/{songId}")
    public Result<List<Comment>> listBySong(@PathVariable Long songId) {
        return commentService.getComments(songId);
    }
}
