package com.jonathanfoucher.gatewayexample.configs.filters;

import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.UUID;

import static org.springframework.util.CollectionUtils.isEmpty;

class CorrelationIdFilterFunctions {
    private static final String CORRELATION_ID_HEADER = "x-correlation-id";

    public static HandlerFilterFunction<ServerResponse, ServerResponse> generateCorrelationIdIfNotProvided() {
        return (request, next) -> {
            List<String> correlationIdValues = request.headers().header(CORRELATION_ID_HEADER);
            String correlationId;
            if (isEmpty(correlationIdValues)) {
                correlationId = UUID.randomUUID().toString();
            } else {
                correlationId = correlationIdValues.getFirst();
            }

            ServerRequest modified = ServerRequest.from(request).header(CORRELATION_ID_HEADER, correlationId).build();
            ServerResponse response = next.handle(modified);
            response.headers().add(CORRELATION_ID_HEADER, correlationId);
            return response;
        };
    }
}
