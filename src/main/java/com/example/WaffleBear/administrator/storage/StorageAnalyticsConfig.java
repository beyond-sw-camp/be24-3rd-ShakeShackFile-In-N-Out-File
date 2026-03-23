package com.example.WaffleBear.administrator.storage;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageAnalyticsConfig {

    public static final long DEFAULT_PROVIDER_CAPACITY_BYTES = 50L * 1024L * 1024L * 1024L * 1024L;

    @Id
    private Long id;

    private Long providerCapacityBytes;

    public void changeProviderCapacityBytes(long providerCapacityBytes) {
        this.providerCapacityBytes = providerCapacityBytes;
    }
}
