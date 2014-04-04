/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.build.gradle.internal;

import static com.android.SdkConstants.FN_LOCAL_PROPERTIES;
import static com.android.build.gradle.BasePlugin.TEST_SDK_DIR;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.gradle.BasePlugin;
import com.android.builder.sdk.DefaultSdkLoader;
import com.android.builder.sdk.SdkLoader;
import com.android.builder.sdk.PlatformLoader;
import com.android.builder.sdk.SdkInfo;
import com.android.builder.sdk.TargetInfo;
import com.android.sdklib.repository.FullRevision;
import com.android.utils.ILogger;
import com.google.common.base.Charsets;
import com.google.common.io.Closeables;

import org.gradle.api.Project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Handles the all things SDK for the Gradle plugin. There is one instance per project, around
 * a singleton {@link com.android.builder.sdk.SdkLoader}.
 */
public class SdkHandler {

    @NonNull
    private final BasePlugin plugin;
    @NonNull
    private final ILogger logger;

    private SdkLoader sdkLoader;
    private File sdkFolder;
    private File ndkFolder;
    private boolean isRegularSdk = true;

    public SdkHandler(@NonNull Project project,
            @NonNull BasePlugin plugin,
            @NonNull ILogger logger) {
        this.plugin = plugin;
        this.logger = logger;
        findLocation(project);
    }

    public void initTarget(String targetHash, FullRevision buildToolRevision) {
        if (targetHash == null) {
            throw new IllegalArgumentException("android.compileSdkVersion is missing!");
        }

        if (buildToolRevision == null) {
            throw new IllegalArgumentException("android.buildToolsVersion is missing!");
        }

        SdkLoader sdkLoader = getSdkLoader();

        SdkInfo sdkInfo = sdkLoader.getSdkInfo(logger);
        TargetInfo targetInfo = sdkLoader.getTargetInfo(targetHash, buildToolRevision, logger);

        plugin.getAndroidBuilder().setTargetInfo(sdkInfo, targetInfo);
    }

    public synchronized SdkLoader getSdkLoader() {
        if (sdkLoader == null) {
            if (isRegularSdk) {
                if (sdkFolder == null) {
                    throw new RuntimeException(
                            "SDK location not found. Define location with sdk.dir in the local.properties file or with an ANDROID_HOME environment variable.");
                }

                // check if the SDK folder actually exist.
                // For internal test we provide a fake SDK location through
                // TEST_SDK_DIR in order to have an SDK, even though we don't use it
                // so in this case we ignore the check.
                if (TEST_SDK_DIR == null && !sdkFolder.isDirectory()) {
                    throw new RuntimeException(String.format(
                            "The SDK directory '%s' does not exist.", sdkFolder));
                }

                sdkLoader = DefaultSdkLoader.getLoader(sdkFolder);
            } else {
                sdkLoader = PlatformLoader.getLoader(sdkFolder);
            }
        }

        return sdkLoader;
    }

    public synchronized void unload() {
        if (sdkLoader != null) {
            if (isRegularSdk) {
                DefaultSdkLoader.unload();
            } else {
                PlatformLoader.unload();
            }

            sdkLoader = null;
        }
    }

    @Nullable
    public File getNdkFolder() {
        return ndkFolder;
    }

    private void findLocation(@NonNull Project project) {
        if (TEST_SDK_DIR != null) {
            sdkFolder = TEST_SDK_DIR;
            return;
        }

        File rootDir = project.getRootDir();
        File localProperties = new File(rootDir, FN_LOCAL_PROPERTIES);

        if (localProperties.isFile()) {

            Properties properties = new Properties();
            InputStreamReader reader = null;
            try {
                //noinspection IOResourceOpenedButNotSafelyClosed
                FileInputStream fis = new FileInputStream(localProperties);
                reader = new InputStreamReader(fis, Charsets.UTF_8);
                properties.load(reader);
            } catch (FileNotFoundException ignored) {
                // ignore since we check up front and we don't want to fail on it anyway
                // in case there's an env var.
            } catch (IOException e) {
                throw new RuntimeException("Unable to read ${localProperties}", e);
            } finally {
                Closeables.closeQuietly(reader);
            }

            String sdkDirProp = properties.getProperty("sdk.dir");

            if (sdkDirProp != null) {
                sdkFolder = new File(sdkDirProp);
            } else {
                sdkDirProp = properties.getProperty("android.dir");
                if (sdkDirProp != null) {
                    sdkFolder = new File(rootDir, sdkDirProp);
                    isRegularSdk = false;
                }
            }

            String ndkDirProp = properties.getProperty("ndk.dir");
            if (ndkDirProp != null) {
                ndkFolder = new File(ndkDirProp);
            }

        } else {
            String envVar = System.getenv("ANDROID_HOME");
            if (envVar != null) {
                sdkFolder = new File(envVar);
            } else {
                String property = System.getProperty("android.home");
                if (property != null) {
                    sdkFolder = new File(property);
                }
            }

            envVar = System.getenv("ANDROID_NDK_HOME");
            if (envVar != null) {
                ndkFolder = new File(envVar);
            }
        }
    }

    @Nullable
    public File getSdkFolder() {
        return sdkFolder;
    }
}
