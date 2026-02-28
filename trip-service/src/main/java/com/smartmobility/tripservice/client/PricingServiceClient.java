package com.smartmobility.tripservice.client;

import com.smartmobility.tripservice.dto.FareResultDTO;
import com.smartmobility.tripservice.dto.PricingRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "pricing-service", path = "/pricing")
public interface PricingServiceClient {

    @PostMapping("/calculate")
    FareResultDTO calculateFare(@RequestBody PricingRequest request);
}
