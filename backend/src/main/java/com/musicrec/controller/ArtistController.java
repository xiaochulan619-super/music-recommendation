package com.musicrec.controller;

import com.musicrec.common.PageResult;
import com.musicrec.common.Result;
import com.musicrec.entity.Artist;
import com.musicrec.service.ArtistService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/artists")
public class ArtistController {
    private final ArtistService artistService;
    public ArtistController(ArtistService artistService) { this.artistService = artistService; }

    @GetMapping
    public Result<PageResult<Artist>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return artistService.listArtists(keyword, page, size);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return artistService.getArtistDetail(id);
    }
}
