package com.musicrec.controller;

import com.musicrec.common.PageResult;
import com.musicrec.common.Result;
import com.musicrec.entity.Song;
import com.musicrec.service.SongService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/songs")
public class SongController {
    private final SongService songService;
    public SongController(SongService songService) { this.songService = songService; }

    @GetMapping
    public Result<PageResult<Song>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return songService.listSongs(keyword, page, size);
    }

    @GetMapping("/{id}")
    public Result<Song> detail(@PathVariable Long id) {
        return songService.getSongDetail(id);
    }
}
