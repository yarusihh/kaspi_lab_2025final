package kz.kaspi.lab.fileuploader.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class FileProcessingLockService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final Duration lockTtl;
    private final String keyPrefix;

    public FileProcessingLockService(
            ReactiveStringRedisTemplate redisTemplate,
            @Value("${app.processing-lock.ttl:PT5M}") Duration lockTtl,
            @Value("${app.processing-lock.key-prefix:file:processing:}") String keyPrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.lockTtl = lockTtl;
        this.keyPrefix = keyPrefix;
    }

    public Mono<Boolean> markInProgress(String hash, String filename) {
        String key = keyFor(hash);
        return redisTemplate.opsForValue()
                .setIfAbsent(key, filename, lockTtl)
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> removeLock(String hash) {
        return redisTemplate.delete(keyFor(hash))
                .map(deletedCount -> deletedCount > 0)
                .defaultIfEmpty(false);
    }

    public long ttlSeconds() {
        return lockTtl.toSeconds();
    }

    private String keyFor(String hash) {
        return keyPrefix + hash;
    }
}
