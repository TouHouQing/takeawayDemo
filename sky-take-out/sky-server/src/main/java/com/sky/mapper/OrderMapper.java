package com.sky.mapper;

import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

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
}
