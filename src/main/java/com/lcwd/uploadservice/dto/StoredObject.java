package com.lcwd.uploadservice.dto;

import java.io.InputStream;

// One stored object's content, streamed rather than buffered — used to proxy HLS
// playlists/segments straight through to an HTTP response.
public record StoredObject(InputStream content, long contentLength, String contentType) {}