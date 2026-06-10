package com.nexio.nexio.jobs.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExtractionResult {
    private int created;
    private int updated;
    private int skippedDuplicate;
    private int skippedNoise;
}
