package com.service.lacasita.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class PushSubscriptionRequest {

    private String  endpoint;
    private Keys    keys;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Keys {
        private String p256dh;
        private String auth;
    }
}
