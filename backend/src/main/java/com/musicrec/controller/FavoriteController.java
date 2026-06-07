package com.musicrec.controller;

import com.musicrec.common.PageResult;
import com.musicrec.common.Result;
import com.musicrec.entity.Song;
import com.musicrec.service.FavoriteService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    private final FavoriteService favoriteService;
    public FavoriteController(FavoriteService favoriteService) { this.favoriteService = favoriteService; }

    @PostMapping
    public Result<?> add(@RequestBody Map<String, Long> body) {
        return favoriteService.addFavorite(body.get("songId"));
    }

    @DeleteMapping("/{songId}")
    public Result<?> remove(@PathVariable Long songId) {
        return favoriteService.removeFavorite(songId);
    }

    @GetMapping
    public Result<PageResult<Song>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return favoriteService.getFavorites(page, size);
    }

    @GetMapping("/check/{songId}")
    public Result<Boolean> check(@PathVariable Long songId) {
        return favoriteService.checkFavorite(songId);
    }
}
