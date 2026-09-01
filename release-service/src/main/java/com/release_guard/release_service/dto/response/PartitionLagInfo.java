package com.release_guard.release_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PartitionLagInfo(
		@Schema(description = "Kafka topic name", example = "risk.calculated")
		String topic,

		@Schema(description = "Partition index", example = "0")
		int partition,

		@Schema(description = "Latest log end offset in Kafka topic partition", example = "42")
		long logEndOffset,

		@Schema(description = "Current committed offset by consumer group", example = "40")
		long currentOffset,

		@Schema(description = "Calculated consumer lag (logEndOffset - currentOffset)", example = "2")
		long lag
) {
}
