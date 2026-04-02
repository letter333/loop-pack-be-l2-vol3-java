package com.loopers.interfaces.api.queue;

import com.loopers.application.queue.QueueFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/queue")
public class QueueAdminV1Controller implements QueueAdminV1ApiSpec {

    private final QueueFacade queueFacade;

    @PostMapping("/activate")
    @Override
    public ApiResponse<Object> activate(
        @RequestHeader("X-Loopers-Ldap") String ldap
    ) {
        queueFacade.activateQueue(ldap);
        return ApiResponse.success();
    }

    @PostMapping("/deactivate")
    @Override
    public ApiResponse<Object> deactivate(
        @RequestHeader("X-Loopers-Ldap") String ldap
    ) {
        queueFacade.deactivateQueue(ldap);
        return ApiResponse.success();
    }

    @GetMapping("/status")
    @Override
    public ApiResponse<QueueV1Dto.QueueStatusResponse> getStatus(
        @RequestHeader("X-Loopers-Ldap") String ldap
    ) {
        boolean active = queueFacade.isQueueActive(ldap);
        return ApiResponse.success(new QueueV1Dto.QueueStatusResponse(active));
    }
}
