package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ShopTypeMapper shopTypeMapper;
    public Result typelist(){
        String key = "shoptype";
        //查缓存，如果存在，直接返回
        String jsonShopTypeList = stringRedisTemplate.opsForValue().get(key);

        //注意先判断存在再做转换，不然可能空指针
        if(jsonShopTypeList != null){
            List<ShopType> cacheShopTypeList = JSONUtil.toList(jsonShopTypeList, ShopType.class);
            return Result.ok(cacheShopTypeList);
        }
        //不存在，查数据库
        List<ShopType> shopTypeList = shopTypeMapper.list();
        //数据库不存在返回false
        if(shopTypeList == null){
            return Result.fail("分类不存在");
        }
        //存在，返回并加入缓存
        String jsonList = JSONUtil.toJsonStr(shopTypeList);
        stringRedisTemplate.opsForValue().set(key, jsonList);
        return Result.ok(shopTypeList);
    };
}
