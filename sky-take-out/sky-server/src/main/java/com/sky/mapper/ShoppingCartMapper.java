package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.ShoppingCart;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 查看购物车
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 根据id修改商品数量
     * @param cart
     */
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(ShoppingCart cart);

    /**
     * 插入购物车
     * @param shoppingCart
     */
    @Insert("insert into shopping_cart (name, user_id, dish_id, setmeal_id, number, amount, create_time, image, dish_flavor) " +
            "values (#{name}, #{userId}, #{dishId}, #{setmealId}, #{number}, #{amount}, #{createTime}, #{image}, #{dishFlavor})")
    void insert(ShoppingCart shoppingCart);
}
