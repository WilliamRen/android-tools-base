/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.builder.internal;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Generate an AndroidManifest.xml file for test projects.
 */
public class TestManifestGenerator {

    private static final String TEMPLATE = "AndroidManifest.template";
    private static final String PH_PACKAGE = "#PACKAGE#";
    private static final String PH_MIN_SDK_VERSION = "#MINSDKVERSION#";
    private static final String PH_TARGET_SDK_VERSION = "#TARGETSDKVERSION#";
    private static final String PH_TESTED_PACKAGE = "#TESTEDPACKAGE#";
    private static final String PH_TEST_RUNNER = "#TESTRUNNER#";
    private static final String PH_HANDLE_PROFILING = "#HANDLEPROFILING#";
    private static final String PH_FUNCTIONAL_TEST = "#FUNCTIONALTEST#";

    private final String mOutputFile;
    private final String mPackageName;
    private final String mMinSdkVersion;
    private final String mTargetSdkVersion;
    private final String mTestedPackageName;
    private final String mTestRunnerName;
    private final boolean mHandleProfiling;
    private final boolean mFunctionalTest;

    public TestManifestGenerator(@NonNull String outputFile,
                          @NonNull String packageName,
                          @Nullable String minSdkVersion,
                          int targetSdkVersion,
                          @NonNull String testedPackageName,
                          @NonNull String testRunnerName,
                          @NonNull Boolean handleProfiling,
                          @NonNull Boolean functionalTest) {
        mOutputFile = outputFile;
        mPackageName = packageName;
        mMinSdkVersion = minSdkVersion;
        mTargetSdkVersion = targetSdkVersion != -1 ? Integer.toString(targetSdkVersion) : minSdkVersion;
        mTestedPackageName = testedPackageName;
        mTestRunnerName = testRunnerName;
        mHandleProfiling = handleProfiling;
        mFunctionalTest = functionalTest;
    }

    public void generate() throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        map.put(PH_PACKAGE, mPackageName);
        map.put(PH_MIN_SDK_VERSION, mMinSdkVersion);
        map.put(PH_TARGET_SDK_VERSION, mTargetSdkVersion);
        map.put(PH_TESTED_PACKAGE, mTestedPackageName);
        map.put(PH_TEST_RUNNER, mTestRunnerName);
        map.put(PH_HANDLE_PROFILING, Boolean.toString(mHandleProfiling));
        map.put(PH_FUNCTIONAL_TEST, Boolean.toString(mFunctionalTest));

        TemplateProcessor processor = new TemplateProcessor(
                TestManifestGenerator.class.getResourceAsStream(TEMPLATE),
                map);

        processor.generate(new File(mOutputFile));
    }
}
