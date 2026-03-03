package com.loopers.interfaces.api.address;

import com.loopers.application.address.AddressFacade;
import com.loopers.application.address.AddressInfo;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/addresses")
public class AddressV1Controller implements AddressV1ApiSpec {

    private final AddressFacade addressFacade;

    @GetMapping
    @Override
    public ApiResponse<List<AddressV1Dto.AddressResponse>> getAddresses(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password
    ) {
        List<AddressInfo> addresses = addressFacade.getAddresses(loginId, password);
        List<AddressV1Dto.AddressResponse> response = addresses.stream()
            .map(AddressV1Dto.AddressResponse::from)
            .toList();
        return ApiResponse.success(response);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<AddressV1Dto.AddressResponse> registerAddress(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @Valid @RequestBody AddressV1Dto.RegisterRequest request
    ) {
        AddressInfo info = addressFacade.register(loginId, password, request.toCommand());
        return ApiResponse.success(AddressV1Dto.AddressResponse.from(info));
    }

    @PutMapping("/{addressId}")
    @Override
    public ApiResponse<AddressV1Dto.AddressResponse> updateAddress(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @PathVariable Long addressId,
        @Valid @RequestBody AddressV1Dto.UpdateRequest request
    ) {
        AddressInfo info = addressFacade.update(loginId, password, addressId, request.toCommand());
        return ApiResponse.success(AddressV1Dto.AddressResponse.from(info));
    }

    @DeleteMapping("/{addressId}")
    @Override
    public ApiResponse<Void> deleteAddress(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @PathVariable Long addressId
    ) {
        addressFacade.delete(loginId, password, addressId);
        return ApiResponse.success(null);
    }

    @PatchMapping("/{addressId}/default")
    @Override
    public ApiResponse<AddressV1Dto.AddressResponse> setDefaultAddress(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @PathVariable Long addressId
    ) {
        AddressInfo info = addressFacade.setDefault(loginId, password, addressId);
        return ApiResponse.success(AddressV1Dto.AddressResponse.from(info));
    }
}
