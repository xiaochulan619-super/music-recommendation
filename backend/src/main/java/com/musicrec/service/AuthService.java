package com.musicrec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musicrec.common.Result;
import com.musicrec.dto.LoginRequest;
import com.musicrec.dto.LoginResponse;
import com.musicrec.dto.RegisterRequest;
import com.musicrec.entity.User;
import com.musicrec.mapper.UserMapper;
import com.musicrec.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public Result<?> register(RegisterRequest req) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, req.getUsername());
        if (userMapper.selectCount(qw) > 0) {
            return Result.error("用户名已存在");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        userMapper.insert(user);

        return Result.success("注册成功");
    }

    public Result<LoginResponse> login(LoginRequest req) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, req.getUsername());
        User user = userMapper.selectOne(qw);

        if (user == null || !encoder.matches(req.getPassword(), user.getPassword())) {
            return Result.error(401, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setUser(LoginResponse.UserInfo.from(user));
        return Result.success(resp);
    }
}
