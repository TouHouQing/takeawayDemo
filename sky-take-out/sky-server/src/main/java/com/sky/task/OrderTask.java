package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单任务
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder(){
        log.info("处理超时订单:{}", LocalDateTime.now());
        //查询超时订单
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLt(Orders.PENDING_PAYMENT, LocalDateTime.now().minusMinutes(15));
        //更新订单状态
        if(ordersList != null && !ordersList.isEmpty()){
            for(Orders orders : ordersList){
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelTime(LocalDateTime.now());
                orders.setCancelReason("订单超时，自动取消");
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 处理配送中订单
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder(){
        log.info("处理配送中订单:{}", LocalDateTime.now());

        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLt(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().minusMinutes(60));

        if(ordersList != null && !ordersList.isEmpty()){
            for(Orders orders : ordersList){
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }

}
