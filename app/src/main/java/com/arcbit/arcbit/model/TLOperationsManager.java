package com.arcbit.arcbit.model;

public class TLOperationsManager {
    public enum TLDownloadState {
        NotDownloading,
        QueuedForDownloading,
        Downloading,
        Downloaded,
        Failed
    }
}
