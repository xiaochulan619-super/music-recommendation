package com.musicrec.controller;

import com.musicrec.common.PageResult;
import com.musicrec.common.Result;
import com.musicrec.entity.Song;
import com.musicrec.entity.Tag;
import com.musicrec.service.TagService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {
    private final TagService tagService;
    public TagController(TagService tagService) { this.tagService = tagService; }

    @GetMapping
    public Result<List<Tag>> list(@RequestParam(required = false) String type) {
        return tagService.listTags(type);
    }

    @GetMapping("/{id}/songs")
    public Result<PageResult<Song>> songs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return tagService.getSongsByTag(id, page, size);
    }
}
