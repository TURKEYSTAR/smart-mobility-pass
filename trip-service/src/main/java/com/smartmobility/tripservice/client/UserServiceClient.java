package com.smartmobility.tripservice.client;

import com.smartmobility.tripservice.dto.PassValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service", path = "/users")
public interface UserServiceClient {

    @GetMapping("/pass/{passId}/validate")
    PassValidationResponse validatePass(@PathVariable("passId") UUID passId);

    @GetMapping("/passes/{passId}/balance")
    PassValidationResponse getPassBalance(@PathVariable("passId") UUID passId);
}
