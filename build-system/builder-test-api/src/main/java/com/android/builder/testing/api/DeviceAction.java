package com.android.builder.testing.api;

import com.google.common.annotations.Beta;

@Beta
public interface DeviceAction {
    void apply(DeviceConnector device);
}
