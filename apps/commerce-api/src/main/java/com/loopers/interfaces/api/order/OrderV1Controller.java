package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderDetailInfo;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.domain.order.OrderPeriod;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<OrderV1Dto.OrderDetailResponse> createOrder(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @Valid @RequestBody OrderV1Dto.CreateOrderRequest request
    ) {
        OrderDetailInfo info = orderFacade.createOrder(loginId, password, request.toCommand());
        return ApiResponse.success(OrderV1Dto.OrderDetailResponse.from(info));
    }

    @GetMapping
    @Override
    public ApiResponse<List<OrderV1Dto.OrderResponse>> getOrders(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @RequestParam(required = false, defaultValue = "THREE_MONTHS") OrderPeriod period
    ) {
        List<OrderInfo> orders = orderFacade.getOrders(loginId, password, period);
        List<OrderV1Dto.OrderResponse> response = orders.stream()
            .map(OrderV1Dto.OrderResponse::from)
            .toList();
        return ApiResponse.success(response);
    }

    @GetMapping("/{orderId}")
    @Override
    public ApiResponse<OrderV1Dto.OrderDetailResponse> getOrderDetail(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @PathVariable Long orderId
    ) {
        OrderDetailInfo info = orderFacade.getOrderDetail(loginId, password, orderId);
        return ApiResponse.success(OrderV1Dto.OrderDetailResponse.from(info));
    }

    @PatchMapping("/{orderId}/cancel")
    @Override
    public ApiResponse<OrderV1Dto.OrderDetailResponse> cancelOrder(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @PathVariable Long orderId
    ) {
        OrderDetailInfo info = orderFacade.cancelOrder(loginId, password, orderId);
        return ApiResponse.success(OrderV1Dto.OrderDetailResponse.from(info));
    }
}
