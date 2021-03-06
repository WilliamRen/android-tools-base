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

package com.android.build.gradle.internal.variant;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.gradle.api.BaseVariant;
import com.android.builder.VariantConfiguration;

import org.gradle.api.Task;

/**
 * Interface for Variant Factory.
 *
 * While VariantManager is the general variant management, implementation of this interface
 * provides variant type (app, lib) specific implementation.
 */
public interface VariantFactory {

    @NonNull
    BaseVariantData createVariantData(@NonNull VariantConfiguration variantConfiguration);

    @NonNull
    BaseVariant createVariantApi(@NonNull BaseVariantData variantData);

    @NonNull
    VariantConfiguration.Type getVariantConfigurationType();

    boolean isVariantPublished();

    boolean isLibrary();

    /**
     * Creates the tasks for a given BaseVariantData.
     * @param variantData the non-null BaseVariantData.
     * @param assembleTask an optional assembleTask to be used. If null, a new one is created.
     */
    void createTasks(
            @NonNull BaseVariantData variantData,
            @Nullable Task assembleTask);
}
