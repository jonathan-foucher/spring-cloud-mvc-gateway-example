package com.jonathanfoucher.gatewayexample.configs.filters;

import org.springframework.cloud.gateway.server.mvc.filter.SimpleFilterSupplier;
import org.springframework.context.annotation.Configuration;

@Configuration
class FilterSupplierConfig extends SimpleFilterSupplier {
    public FilterSupplierConfig() {
        super(CorrelationIdFilterFunctions.class);
    }
}
