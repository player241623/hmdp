package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginIntercepter implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;
    public LoginIntercepter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        HttpSession session = request.getSession();

//        User user = (User) session.getAttribute("user");
        String token = request.getHeader("Authorization");
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries("login:user" + token);

        if(userMap == null) {
            response.setStatus(401);
        }
        UserDTO userDTO1 = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
//        UserDTO userDTO = new UserDTO();
//        userDTO.setId(user.getId());
//        userDTO.setIcon(user.getIcon());
//        userDTO.setNickName(user.getNickName());
        UserHolder.saveUser(userDTO1);

        //延长过期时间
        stringRedisTemplate.expire("login:token" + token,30L, TimeUnit.MINUTES);
        return true;
    }
}
