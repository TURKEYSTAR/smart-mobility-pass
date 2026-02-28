package com.smartmobility.tripservice.client;

import com.smartmobility.tripservice.dto.BillingRequest;
import com.smartmobility.tripservice.dto.BillingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "billing-service", path = "/api/billing")
public interface BillingServiceClient {

    @PostMapping("/debit")
    BillingResponse debitAccount(@RequestBody BillingRequest request, @RequestHeader("X-Internal-Service") String internalService);
}
