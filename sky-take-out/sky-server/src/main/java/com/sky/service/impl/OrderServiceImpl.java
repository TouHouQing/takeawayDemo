package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    @CacheEvict(value = "orderCache", allEntries = true)
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        //处理业务异常情况（地址为空，购物车为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null) {
                throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //购物车数据
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if(shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw  new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //向订单表插入1条
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(addressBook.getDetail());
        orders.setUserName(userMapper.getById(userId).getName());
        orders.setUserId(userId);

        orderMapper.insert(orders);

        List<OrderDetail> orderDetailList = new ArrayList<>();
        //向订单明细表插入n条
        for(ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        //清空用户的购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        //封装vo返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @Override
    @CacheEvict(value = "orderCache", allEntries = true)
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    @Override
    @Transactional
    @CacheEvict(value = "orderCache", allEntries = true)
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        //通过websocket向客户端发送消息 type orderId content
        Map map = new HashMap();
        map.put("type",1);
        map.put("orderId",ordersDB.getId());
        map.put("content","订单号："+outTradeNo);

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 客户催单
     * @param id
     */
    @Override
    public void reminder(Long id){
        Orders ordersDB = orderMapper.getById(id);
        //检查订单是否存在
        if(ordersDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Map map = new HashMap();
        map.put("type",2);
        map.put("orderId",id);
        map.put("content","订单号："+ordersDB.getNumber());
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 历史订单分页查询
     * @param pageNum
     * @param pageSize
     * @param status
     */
    @Override
    @Cacheable(cacheNames = "orderCache",key = "#pageNum + \"_\" + #pageSize + \"_\" + (#status != null ? #status : 'null')")
    public PageResult historyOrder(int pageNum, int pageSize, Integer status) {
        PageHelper.startPage(pageNum,pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        Page<Orders> page = orderMapper.getByPageAndStatus(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList<>();
        for(Orders orders : page) {
            OrderVO orderVO = new OrderVO();
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
            BeanUtils.copyProperties(orders,orderVO);
            orderVO.setOrderDetailList(orderDetailList);
            list.add(orderVO);
        }
        PageResult pageResult = new PageResult(page.getTotal(),list);
        return pageResult;
    }

    /**
     * 订单详情
     * @param id
     * @return
     */
    @Override
    @Cacheable(cacheNames = "orderCache",key = "#id")
    public OrderVO orderDetail(Long id) {
        OrderVO orderVO = new OrderVO();
        Orders orders = orderMapper.getById(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        BeanUtils.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "orderCache",key = "#id")
    public void userCancelById(Long id) throws Exception {
        Orders orderDB = orderMapper.getById(id);
        if(orderDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //若商家已接单,派送中，用户取消订单需要电话沟通商家
        if(orderDB.getStatus()>2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //查询订单状态，待支付和待接单可以直接取消订单，待接单状态需要给用户退款
        Orders order = new Orders();
        order.setId(id);
        if(orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            //退款
            //调用微信退款接口
            //调用微信支付退款接口
            weChatPayUtil.refund(
                    orderDB.getNumber(), //商户订单号
                    orderDB.getNumber(), //商户退款单号
                    orderDB.getAmount(),//退款金额，单位 元
                    orderDB.getAmount());//原订单金额
            order.setPayStatus(Orders.REFUND);
        }
        //标记状态已取消
        order.setStatus(Orders.CANCELLED);
        order.setCancelReason("用户取消订单");
        order.setCancelTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "orderCache",allEntries = true)
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();
        //根据订单id查询订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        //将订单详情转换成购物车
        List<ShoppingCart> shoppingCartList = new ArrayList<>();
        for(OrderDetail orderDetail : orderDetailList){
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail,shoppingCart);
            shoppingCart.setUserId(userId);
            shoppingCartList.add(shoppingCart);
        }
        //将订单菜品添加到购物车
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    @Cacheable(cacheNames = "orderCache",key = "#ordersPageQueryDTO")
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.getByPageAndStatus(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList<>();
        //部分订单状态需要返回订单菜品信息
        List<OrderVO> orderVOList = getOrderVOList(page);
        PageResult pageResult = new PageResult(page.getTotal(),orderVOList);
        return pageResult;
    }

    /**
     * 订单统计
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        //待接单数量
        orderStatisticsVO.setToBeConfirmed(orderMapper.countByStatus(Orders.TO_BE_CONFIRMED));
        //待派送数量
        orderStatisticsVO.setConfirmed(orderMapper.countByStatus(Orders.CONFIRMED));
        //派送中数量
        orderStatisticsVO.setDeliveryInProgress(orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS));
        return orderStatisticsVO;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "orderCache",allEntries = true)
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page){
        List<OrderVO> orderVOList = new ArrayList<>();
        List<Orders> ordersList = page.getResult();
        if(!ordersList.isEmpty()){
            for(Orders orders : ordersList){
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                //订单菜品信息
                String orderDishes = getOrderDishesStr(orders);
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    private String getOrderDishesStr(Orders orders){
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
        StringBuilder orderDishes = new StringBuilder();
        for(OrderDetail orderDetail : orderDetailList){
            String name = orderDetail.getName();
            Integer number = orderDetail.getNumber();
            orderDishes.append(name).append("*").append(number).append(";");
        }
        return orderDishes.toString();
    }
}
