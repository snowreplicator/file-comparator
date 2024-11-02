package com.snowreplicator.filecomparator.view_model;

import java.nio.file.Path;

public record FileInfo(Path path, byte[] hash) {
}
