package kz.kaspi.lab.fileuploaderreactive.infrastructure.dedup;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RecentChecksumCache {

    private final Map<String, Instant> checksums = new HashMap<>();

    public synchronized boolean contains(String checksum) {
        cleanupExpired();
        Instant expiresAt = checksums.get(checksum);
        return expiresAt != null && expiresAt.isAfter(Instant.now());
    }

    public synchronized void put(String checksum, Duration ttl) {
        cleanupExpired();
        checksums.put(checksum, Instant.now().plus(ttl));
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        Iterator<Map.Entry<String, Instant>> iterator = checksums.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Instant> entry = iterator.next();
            if (!entry.getValue().isAfter(now)) {
                iterator.remove();
            }
        }
    }
}
