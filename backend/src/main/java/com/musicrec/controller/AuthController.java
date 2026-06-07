package com.musicrec.controller;

import com.musicrec.common.Result;
import com.musicrec.dto.LoginRequest;
import com.musicrec.dto.LoginResponse;
import com.musicrec.dto.RegisterRequest;
import com.musicrec.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }
}
