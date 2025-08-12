package com.sky.mapper;

import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单
     * @param orders
     */
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into orders (number, status, user_id, address_book_id, order_time, checkout_time, pay_method, pay_status, " +
            "amount, remark, phone, address, consignee, estimated_delivery_time, delivery_status, " +
            "pack_amount, tableware_number, tableware_status, user_name) " +
            "values (#{number}, #{status}, #{userId}, #{addressBookId}, #{orderTime}, #{checkoutTime}, #{payMethod}, " +
            "#{payStatus}, #{amount}, #{remark}, #{phone}, #{address}, #{consignee}, #{estimatedDeliveryTime}, " +
            "#{deliveryStatus}, #{packAmount}, #{tablewareNumber}, #{tablewareStatus}, #{userName})")
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 根据状态和订单时间查询订单
     * @param status
     * @param orderTime
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTimeLt(Integer status, LocalDateTime orderTime);
}
