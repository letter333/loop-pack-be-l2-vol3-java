package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderAdminDetailInfo;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.domain.order.OrderPeriod;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/orders")
public class OrderAdminV1Controller implements OrderAdminV1ApiSpec {

    private final OrderFacade orderFacade;

    @GetMapping
    @Override
    public ApiResponse<List<OrderAdminV1Dto.OrderAdminResponse>> getOrders(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        @RequestParam(required = false, defaultValue = "ALL") OrderPeriod period
    ) {
        List<OrderInfo> orders = orderFacade.getOrdersForAdmin(ldap, period);
        List<OrderAdminV1Dto.OrderAdminResponse> response = orders.stream()
            .map(OrderAdminV1Dto.OrderAdminResponse::from)
            .toList();
        return ApiResponse.success(response);
    }

    @GetMapping("/{orderId}")
    @Override
    public ApiResponse<OrderAdminV1Dto.OrderAdminDetailResponse> getOrderDetail(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        @PathVariable Long orderId
    ) {
        OrderAdminDetailInfo info = orderFacade.getOrderDetailForAdmin(ldap, orderId);
        return ApiResponse.success(OrderAdminV1Dto.OrderAdminDetailResponse.from(info));
    }

    @PatchMapping("/{orderId}/status")
    @Override
    public ApiResponse<OrderAdminV1Dto.OrderAdminDetailResponse> changeOrderStatus(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        @PathVariable Long orderId,
        @RequestBody @Valid OrderAdminV1Dto.ChangeStatusRequest request
    ) {
        OrderAdminDetailInfo info = orderFacade.changeOrderStatusForAdmin(ldap, orderId, request.status());
        return ApiResponse.success(OrderAdminV1Dto.OrderAdminDetailResponse.from(info));
    }
}
