package com.smartmobility.tripservice.client;

import com.smartmobility.tripservice.dto.BillingRequest;
import com.smartmobility.tripservice.dto.BillingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "billing-service", path = "/billing")
public interface BillingServiceClient {

    @PostMapping("/debit")
    BillingResponse debitAccount(@RequestBody BillingRequest request);
}
