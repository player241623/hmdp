package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        String key = "login:code" + phone;
        //1.校验
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2.若失败，返回错误
            return Result.fail("手机号格式错误");
        }
        //3.若成功，将验证码存入session
        String code = RandomUtil.randomNumbers(6);
//        session.setAttribute("code", code);
        stringRedisTemplate.opsForValue().set(key, code, 1L, TimeUnit.MINUTES);
        //返回验证码
        log.debug("发送验证码：{}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        String code = loginForm.getCode();

        String key = "login:code" + phone;

        if(RegexUtils.isPhoneInvalid(loginForm.getPhone())){
            return Result.fail("手机号格式错误");
        }
        //注意类型转换，类型不同equals方法直接报错
//        Object cacheCode = session.getAttribute("code");
//
//        if(cacheCode == null || !cacheCode.toString().equals(code)) {
//            return Result.fail("验证码错误");
//        }
        String redisCode = stringRedisTemplate.opsForValue().get(key);

        if(redisCode == null || !code.equals(redisCode)){
            return Result.fail("验证码错误");
        }

        User user = userMapper.getUserByPhone(phone);
        if(user == null) {
            user = createUserWithPhone(phone);
        }
        //将用户信息存入session，用于登录校验
//        session.setAttribute("user", user);
        String token = UUID.randomUUID().toString(true);
        Map<String, String> userMap = new HashMap<>();
        userMap.put("id", user.getId().toString());
        userMap.put("nickName", user.getNickName());
        userMap.put("icon", user.getIcon());
//        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
//        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO);
        stringRedisTemplate.opsForHash().putAll("login:user" + token, userMap);
        stringRedisTemplate.expire("login:user" + token, 30, TimeUnit.MINUTES);
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(6));
        userMapper.insert(user);
        return user;
    }


}
