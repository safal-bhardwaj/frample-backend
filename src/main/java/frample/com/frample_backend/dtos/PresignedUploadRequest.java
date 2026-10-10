package frample.com.frample_backend.dtos;

public record PresignedUploadRequest(String fileName, String contentType) {
}