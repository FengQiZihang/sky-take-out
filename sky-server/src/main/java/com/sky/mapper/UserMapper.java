package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {
    /**
     * 根据openid查询用户
     * @param openid 微信用户的openid
     * @return User 用户实体
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 插入用户数据
     * @param user 用户实体
     */
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into user (openid, name, phone, sex, id_number, avatar, create_time) " +
            "values (#{openid}, #{name}, #{phone}, #{sex}, #{idNumber}, #{avatar}, #{createTime})")
    void insert(User user);

    /**
     * 根据id查询用户
     * @param id 用户id
     * @return User 用户实体
     */
    @Select("select * from user where id = #{id}")
    User getById(Long id);

    /**
     * 根据时间范围统计用户数量
     * @param map 包含开始时间、结束时间等条件的Map
     * @return 用户数量
     */
    Integer countByMap(Map map);
}
