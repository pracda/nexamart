package com.nexamart.admin.dto;

import java.util.List;

public record FraudScanResult(List<FraudFlag> flags, String scanNote) {
}
