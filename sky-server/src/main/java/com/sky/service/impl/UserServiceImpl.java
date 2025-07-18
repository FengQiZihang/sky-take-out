package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    // 微信服务接口地址
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties wechatProperties;

    @Autowired
    private UserMapper userMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        log.info("【用户端】调用微信接口服务，获得当前微信用户的openid:");
        String openid = getOpenid(userLoginDTO.getCode());
        // 判断当openid是否为空
        if (openid == null) {
            // 抛出登录失败异常：登录失败
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        // 判断当前用户是否为新用户
        log.info("【用户端】根据openid查询用户:{}", openid);
        User user = userMapper.getByOpenid(openid);
        if (user == null) {
            // 为新用户，自动完成注册
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            log.info("【用户端】为新用户，自动完成注册:{}", user);
            userMapper.insert(user);
        }
        // 返回用户信息
        return user;
    }

    /**
     * 获取微信用户的openid
     * @param code 微信登录时获取的code
     * @return openid 微信用户的openid
     */
    private String getOpenid(String code) {
        // 调用微信接口服务，获得当前微信用户的openid
        Map<String, String> map = new HashMap<>();
        map.put("appid", wechatProperties.getAppid());
        map.put("secret", wechatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");

        String json = HttpClientUtil.doGet(WX_LOGIN, map);

        return JSON.parseObject(json).getString("openid");
    }
}
