package com.jparest.web;

import com.jparest.mapper.CustomerMapper;
import com.jparest.mapper.ItemMapper;
import com.jparest.mapper.OrderMapper;
import com.jparest.model.Items;
import com.jparest.model.Orders;
import com.jparest.model.dto.ItemDto;
import com.jparest.model.dto.OrderDto;
import com.jparest.model.enums.OrderStatusEnum;
import com.jparest.model.wrapper.CustomerOrderWrapper;
import com.jparest.model.wrapper.OrderRequestWrapper;
import com.jparest.model.wrapper.OrderResponseWrapper;
import com.jparest.rest.ApiResponse;
import com.jparest.rest.patch.Patch;
import com.jparest.service.ItemService;
import com.jparest.service.OrderService;
import com.monitorjbl.json.JsonResult;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@RestController
@RequestMapping(value = "/v1/api")
public class OrderController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private JsonResult jsonResult = JsonResult.instance();

    @Autowired
    OrderService orderService;

    @Autowired
    ItemService itemService;

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    ItemMapper itemMapper;

    @Autowired
    CustomerMapper customerMapper;

    /**
     * Get order list.
     *
     * @return
     */
    @ApiOperation(
            value = "Get order list"
    )
    @GetMapping(
            value = "/orders",
            produces = APPLICATION_JSON_VALUE
    )
    public List<CustomerOrderWrapper> getAllOrders() {
        List<Orders> orders = this.orderService.getAllOrders();
        // Wrap customer orders.
        return wrapCustomerOrder(orders);
    }

    /**
     * Add customer order.
     *
     * @param customerId
     * @param requestContext
     * @return
     */
    @ApiOperation(
            value = "Add customer order"
    )
    @PostMapping(
            value = "/customers/{customerId}/orders",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public ApiResponse<OrderResponseWrapper> addOrder(@ApiParam(value = "Customer Id", required = true) @PathVariable(name = "customerId") Long customerId,
                                                      @ApiParam(value = "Order details", required = true) @RequestBody OrderRequestWrapper requestContext) {
        // Get items.
        List<Items> items = new ArrayList<>();
        for (Long itemId : requestContext.getItemIds()) {
            items.add(this.itemService.getItemById(itemId));
        }
        // Add items in order.
        Orders orders = this.orderMapper.mapDTOtoEntity(requestContext.getOrder());
        orders.setItems(items);
        // Call order service to add order.
        Orders result = this.orderService.addOrder(customerId, orders);
        // Set order response.
        OrderResponseWrapper responseContext = new OrderResponseWrapper();
        responseContext.setOrder(this.orderMapper.mapEntityToDTO(result));
        responseContext.setItems(this.itemMapper.mapEntitiesToDTOs(items));
        return new ApiResponse<>(HttpStatus.CREATED.value(),
                HttpStatus.CREATED,
                Arrays.asList(responseContext));
    }

    /**
     * Get orders by customer Id.
     *
     * @param customerId
     * @return
     */
    @ApiOperation(
            value = "Get orders by customer Id"
    )
    @GetMapping(
            value = "/customers/{customerId}/orders",
            produces = APPLICATION_JSON_VALUE
    )
    public List<OrderResponseWrapper> getOrdersByCustomerId(@ApiParam(value = "Customer Id", required = true) @PathVariable(name = "customerId") Long customerId) {
        List<Orders> orders = this.orderService.getOrdersByCustomerId(customerId);
        // Wrap order.
        List<OrderResponseWrapper> responseContexts = new ArrayList<>();
        for (Orders order : orders) {
            OrderResponseWrapper responseContext = new OrderResponseWrapper();
            responseContext.setOrder(this.orderMapper.mapEntityToDTO(order));
            responseContext.setItems(this.itemMapper.mapEntitiesToDTOs(order.getItems()));
            responseContexts.add(responseContext);
        }
        return responseContexts;
    }

    /**
     * Get order by Id.
     *
     * @param orderId
     * @return
     */
    @ApiOperation(
            value = "Get order by Id"
    )
    @GetMapping(
            value = "/orders/{orderId}",
            produces = APPLICATION_JSON_VALUE
    )
    public OrderResponseWrapper getOrderById(@ApiParam(value = "Order Id", required = true) @PathVariable(name = "orderId") Long orderId) {
        Orders orders = this.orderService.getOrderById(orderId);
        // Wrap order.
        OrderResponseWrapper responseContext = new OrderResponseWrapper();
        responseContext.setOrder(this.orderMapper.mapEntityToDTO(orders));
        responseContext.setItems(this.itemMapper.mapEntitiesToDTOs(orders.getItems()));
        return responseContext;
    }

    /**
     * Get items by order id.
     *
     * @param orderId
     * @return
     */
    @ApiOperation(
            value = "Get items by order Id"
    )
    @GetMapping(
            value = "/orders/{orderId}/items",
            produces = APPLICATION_JSON_VALUE
    )
    public List<ItemDto> getItemsByOrderId(@ApiParam(value = "Order Id", required = true) @PathVariable(name = "orderId") Long orderId) {
        List<Items> items = this.itemService.getItemsByOrderId(orderId);
        return this.itemMapper.mapEntitiesToDTOs(items);
    }

    /**
     * Get orders by status.
     *
     * @param status
     * @return
     */
    @ApiOperation(
            value = "Get order by status"
    )
    @GetMapping(
            value = "/orders",
            params = "status",
            produces = APPLICATION_JSON_VALUE
    )
    public List<CustomerOrderWrapper> getOrdersByStatus(@ApiParam(value = "Order status", required = true) @RequestParam(name = "status") OrderStatusEnum status) {
        List<Orders> orders = this.orderService.getOrdersByStatus(status);
        // Wrap orders.
        return wrapCustomerOrder(orders);
    }

    /**
     * Helper method to wrap customer orders.
     *
     * @param orders
     */
    private List<CustomerOrderWrapper> wrapCustomerOrder(List<Orders> orders) {
        List<CustomerOrderWrapper> orderWrappers = new ArrayList<>();
        for (Orders ordr : orders) {
            // Map orders.
            OrderResponseWrapper orderResponse = new OrderResponseWrapper();
            orderResponse.setOrder(this.orderMapper.mapEntityToDTO(ordr));
            orderResponse.setItems(this.itemMapper.mapEntitiesToDTOs(ordr.getItems()));
            // Set context.
            CustomerOrderWrapper customerOrderWrapper = new CustomerOrderWrapper();
            customerOrderWrapper.setCustomer(this.customerMapper.mapEntityToDTO(ordr.getCustomer()));
            customerOrderWrapper.setOrderDetails(orderResponse);
            orderWrappers.add(customerOrderWrapper);
        }
        return orderWrappers;
    }

    /**
     * Remove order by id.
     *
     * @param orderId
     * @return
     */
    @ApiOperation(
            value = "Remove order by Id"
    )
    @DeleteMapping(
            value = "/orders/{orderId}",
            produces = TEXT_PLAIN_VALUE
    )
    public ResponseEntity deleteOrderById(@ApiParam(value = "Order Id", required = true) @PathVariable(name = "orderId") Long orderId) {
        this.orderService.removeOrderById(orderId);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * Update order by id.
     *
     * @param orderId
     * @param dto
     * @return
     */
    @ApiOperation(
            value = "Update order by Id"
    )
    @PutMapping(
            value = "/orders/{orderId}",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public ApiResponse<OrderDto> updateOrderById(@ApiParam(value = "Order Id", required = true) @PathVariable(name = "orderId") Long orderId,
                                                 @ApiParam(value = "Order details", required = true) @RequestBody OrderDto dto) {
        Orders orders = this.orderService.updateOrderById(orderId, this.orderMapper.mapDTOtoEntity(dto));
        return new ApiResponse<>(HttpStatus.OK.value(),
                HttpStatus.OK,
                Arrays.asList(this.orderMapper.mapEntityToDTO(orders)));
    }

    /**
     * Patch order by id.
     *
     * @param orderId
     * @param patch
     * @return
     */
    @ApiOperation(
            value = "Patch order by id"
    )
    @PatchMapping(
            value = "/orders/{orderId}",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public ApiResponse<OrderDto> patchOrderById(@ApiParam(value = "Order Id", required = true) @PathVariable(name = "orderId") Long orderId,
                                                @ApiParam(value = "Patch details", required = true) @RequestBody Patch patch) {
        Orders orders = this.orderService.patchOrderById(orderId, patch);
        return new ApiResponse<>(HttpStatus.OK.value(),
                HttpStatus.OK,
                Arrays.asList(this.orderMapper.mapEntityToDTO(orders)));
    }
}