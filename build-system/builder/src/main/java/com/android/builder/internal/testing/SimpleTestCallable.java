/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.builder.internal.testing;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.testing.TestData;
import com.android.builder.testing.api.DeviceAction;
import com.android.builder.testing.api.DeviceConnector;
import com.android.builder.testing.api.DeviceException;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.utils.ILogger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Basic Callable to run tests on a given {@link DeviceConnector} using
 * {@link RemoteAndroidTestRunner}.
 */
public class SimpleTestCallable implements Callable<Boolean> {

    @NonNull
    private final String projectName;
    @NonNull
    private final DeviceConnector device;
    @NonNull
    private final String flavorName;
    @NonNull
    private final TestData testData;
    @NonNull
    private final File resultsDir;
    @NonNull
    private final File testApk;
    @Nullable
    private final File testedApk;
    @NonNull
    private final List<DeviceAction> prepareDeviceActions;
    @NonNull
    private final List<DeviceAction> scrubDeviceAction;

    private final int timeout;
    @NonNull
    private final ILogger logger;

    public SimpleTestCallable(
            @NonNull  DeviceConnector device,
            @NonNull  String projectName,
            @NonNull  String flavorName,
            @NonNull  File testApk,
            @Nullable File testedApk,
            @NonNull  TestData testData,
            @NonNull  File resultsDir,
            @NonNull  List<DeviceAction> prepareDevice,
            @NonNull  List<DeviceAction> scrubDevice,
                      int timeout,
            @NonNull  ILogger logger) {
        this.projectName = projectName;
        this.device = device;
        this.flavorName = flavorName;
        this.resultsDir = resultsDir;
        this.testApk = testApk;
        this.testedApk = testedApk;
        this.testData = testData;
        this.prepareDeviceActions = Collections.unmodifiableList(prepareDevice);
        this.scrubDeviceAction = Collections.unmodifiableList(scrubDevice);
        this.timeout = timeout;
        this.logger = logger;
    }

    @Override
    public Boolean call() throws Exception {
        String deviceName = device.getName();
        boolean isInstalled = false;

        CustomTestRunListener runListener = new CustomTestRunListener(
                deviceName, projectName, flavorName, logger);
        runListener.setReportDir(resultsDir);

        long time = System.currentTimeMillis();

        try {
            device.connect(timeout, logger);

            if (testedApk != null) {
                logger.verbose("DeviceConnector '%s': installing %s", deviceName, testedApk);
                device.installPackage(testedApk, timeout, logger);
            }

            logger.verbose("DeviceConnector '%s': installing %s", deviceName, testApk);
            device.installPackage(testApk, timeout, logger);
            isInstalled = true;

            for (DeviceAction action : prepareDeviceActions) {
                action.apply(device);
            }

            logger.info("Instrumentation info %s", testData.getInstrumentationOptions());
            RemoteAndroidTestRunner runner = new RemoteAndroidTestRunner(
                    testData.getPackageName(),
                    testData.getInstrumentationRunner(),
                    device);

            runner.setRunName(deviceName);
            runner.setMaxtimeToOutputResponse(timeout);

            for (Map.Entry<String, String> kv : testData.getInstrumentationOptions().entrySet()) {
                runner.addInstrumentationArg(kv.getKey(), kv.getValue());
            }

            runner.run(runListener);

            for (DeviceAction action : scrubDeviceAction) {
                action.apply(device);
            }

            return runListener.getRunResult().hasFailedTests();
        } catch (Exception e) {
            Map<String, String> emptyMetrics = Collections.emptyMap();

            // create a fake test output
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter pw = new PrintWriter(baos, true);
            e.printStackTrace(pw);
            TestIdentifier fakeTest = new TestIdentifier(device.getClass().getName(), "runTests");
            runListener.testStarted(fakeTest);
            runListener.testFailed(ITestRunListener.TestFailure.ERROR, fakeTest , baos.toString());
            runListener.testEnded(fakeTest, emptyMetrics);

            // end the run to generate the XML file.
            runListener.testRunEnded(System.currentTimeMillis() - time, emptyMetrics);

            // and throw
            throw e;
        } finally {
            if (isInstalled) {
                // uninstall the apps
                // This should really not be null, because if it was the build
                // would have broken before.
                uninstall(testApk, testData.getPackageName(), deviceName);

                if (testedApk != null) {
                   uninstall(testedApk, testData.getTestedPackageName(), deviceName);
                }
            }

            device.disconnect(timeout, logger);
        }
    }

    private void uninstall(@NonNull File apkFile, @Nullable String packageName,
                           @NonNull String deviceName)
            throws DeviceException {
        if (packageName != null) {
            logger.verbose("DeviceConnector '%s': uninstalling %s", deviceName, packageName);
            device.uninstallPackage(packageName, timeout, logger);
        } else {
            logger.verbose("DeviceConnector '%s': unable to uninstall %s: unable to get package name",
                    deviceName, apkFile);
        }
    }
}
