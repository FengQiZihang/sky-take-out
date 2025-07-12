package com.sky.service;

import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;

public interface UserService {

    /**
     * 微信登录
     * @param userLoginDTO 用户登录DTO
     * @return User 用户实体
     * @throws LoginFailedException 登录失败
     */
    User wxLogin(UserLoginDTO userLoginDTO);
}
