package com.service.lacasita.service.notifications;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.service.lacasita.entity.PushSubscription;
import com.service.lacasita.repository.PushSubscriptionRepository;

/**
 * Alta y baja de suscripciones push.
 * @author Jason Nunez
 */
@Service
public class PushSubscriptionService {

    private final PushSubscriptionRepository subscriptionRepository;

    public PushSubscriptionService(PushSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public void suscribir(Long userId, String endpoint, String p256dh, String auth) {

        PushSubscription sub = subscriptionRepository.findByEndpoint(endpoint)
                .orElseGet(PushSubscription::new);

        sub.setUserId(userId);
        sub.setEndpoint(endpoint);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);

        subscriptionRepository.save(sub);
    }

    @Transactional
    public void desuscribir(String endpoint) {
        subscriptionRepository.deleteByEndpoint(endpoint);
    }
}
