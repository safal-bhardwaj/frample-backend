package frample.com.frample_backend.dtos;

public record PresignedUploadResponse(String uploadUrl, String fileKey) {
}