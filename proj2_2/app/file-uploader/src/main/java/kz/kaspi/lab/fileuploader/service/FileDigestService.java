package kz.kaspi.lab.fileuploader.service;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class FileDigestService {

    public Mono<FileDigest> digest(FilePart file) {
        return toBytes(file)
                .map(bytes -> new FileDigest(sha256Hex(bytes), bytes.length, bytes));
    }

    private Mono<byte[]> toBytes(FilePart file) {
        return DataBufferUtils.join(file.content())
                .map(this::toByteArrayAndRelease);
    }

    private byte[] toByteArrayAndRelease(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        DataBufferUtils.release(dataBuffer);
        return bytes;
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    public record FileDigest(String hash, int sizeInBytes, byte[] bytes) {
    }
}
