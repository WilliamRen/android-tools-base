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
package com.android.build.gradle
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.builder.DefaultBuildType
import com.android.builder.DefaultProductFlavor
import com.android.builder.model.SigningConfig
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.reflect.Instantiator
/**
 * Extension for 'application' project.
 */
public class AppExtension extends BaseExtension {

    private final DefaultDomainObjectSet<ApplicationVariant> applicationVariantList =
        new DefaultDomainObjectSet<ApplicationVariant>(ApplicationVariant.class)

    AppExtension(AppPlugin plugin, ProjectInternal project, Instantiator instantiator,
                 NamedDomainObjectContainer<DefaultBuildType> buildTypes,
                 NamedDomainObjectContainer<DefaultProductFlavor> productFlavors,
                 NamedDomainObjectContainer<SigningConfig> signingConfigs,
                 boolean isLibrary) {
        super(plugin, project, instantiator, buildTypes, productFlavors, signingConfigs, isLibrary)
    }

    public DefaultDomainObjectSet<ApplicationVariant> getApplicationVariants() {
        return applicationVariantList
    }

    @Override
    void addVariant(BaseVariant variant) {
        applicationVariantList.add((ApplicationVariant) variant)
    }
}
