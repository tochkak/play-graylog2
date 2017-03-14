package ru.tochkak.logback.graylog2;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("notused")
public class AccessLogMessage {
    private Integer timestamp;
    private String method;
    private String url;
    private String remoteAddr;
    private Integer httpStatus;
    private Long responseLength;
    private Long responseTime;

    public AccessLogMessage(
            Integer timestamp,
            String method,
            String url,
            String remoteAddr,
            Integer httpStatus,
            Long responseLength,
            Long responseTime
    ) {
        this.timestamp = timestamp;
        this.method = method;
        this.url = url;
        this.remoteAddr = remoteAddr;
        this.httpStatus = httpStatus;
        this.responseLength = responseLength;
        this.responseTime = responseTime;
    }

    Map<String, Object> getFields() {
        Map<String, Object> map = new HashMap<>();

        map.put("timestamp", timestamp);
        map.put("method", method);
        map.put("url", url);
        map.put("remoteAddr", remoteAddr);
        map.put("httpStatus", httpStatus);
        map.put("responseLength", responseLength);
        map.put("responseTime", responseTime);

        return map;
    }
}
