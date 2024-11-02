package com.snowreplicator.filecomparator.view_model;

import java.nio.file.Path;

public record InputScanParams(Path inputDir, Path outputDir, boolean inputSubDirsEnabled, boolean outputSubDirsEnabled) {
}
