/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ide.eclipse.adt.internal.launch.junit;

import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.launch.LaunchMessages;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestHelper;
import com.android.ide.eclipse.adt.internal.project.BaseProjectHelper;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.xml.ManifestData;
import com.android.sdklib.xml.ManifestData.Instrumentation;
import com.android.sdklib.xml.ManifestData.UsesLibrary;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;

/**
 * Provides validation for Android instrumentation test runner
 */
class InstrumentationRunnerValidator {
    private final IJavaProject mJavaProject;
    private String[] mInstrumentationNames = null;
    private boolean mHasRunnerLibrary = false;

    static final String INSTRUMENTATION_OK = null;

    /**
     * Initializes the InstrumentationRunnerValidator.
     *
     * @param javaProject the {@link IJavaProject} for the Android project to validate
     */
    InstrumentationRunnerValidator(IJavaProject javaProject) {
        mJavaProject = javaProject;
        ManifestData manifestData = AndroidManifestHelper.parseForData(javaProject.getProject());
        init(manifestData);
    }

    /**
     * Initializes the InstrumentationRunnerValidator.
     *
     * @param project the {@link IProject} for the Android project to validate
     * @throws CoreException if a fatal error occurred in initialization
     */
    InstrumentationRunnerValidator(IProject project) throws CoreException {
        this(BaseProjectHelper.getJavaProject(project));
    }

    /**
     * Initializes the InstrumentationRunnerValidator with an existing {@link AndroidManifestHelper}
     *
     * @param javaProject the {@link IJavaProject} for the Android project to validate
     * @param manifestData the {@link ManifestData} for the Android project
     */
    InstrumentationRunnerValidator(IJavaProject javaProject, ManifestData manifestData) {
        mJavaProject = javaProject;
        init(manifestData);
    }

    private void init(ManifestData manifestData) {
        Instrumentation[] instrumentations = manifestData.getInstrumentations();
        mInstrumentationNames = new String[instrumentations.length];
        for (int i = 0; i < instrumentations.length; i++) {
            mInstrumentationNames[i] = instrumentations[i].getName();
        }
        mHasRunnerLibrary = hasTestRunnerLibrary(manifestData);
    }

    /**
     * Helper method to determine if given manifest has a <code>AndroidConstants.LIBRARY_TEST_RUNNER
     * </code> library reference
     *
     * @param manifestParser the {@link ManifestData} to search
     * @return true if test runner library found, false otherwise
     */
    private boolean hasTestRunnerLibrary(ManifestData manifestData) {
       for (UsesLibrary lib : manifestData.getUsesLibraries()) {
           if (AndroidConstants.LIBRARY_TEST_RUNNER.equals(lib.getName())) {
               return true;
           }
       }
       return false;
    }

    /**
     * Return the set of instrumentation names for the Android project.
     *
     * @return <code>null</code> if error occurred parsing instrumentations, otherwise returns array
     * of instrumentation class names
     */
    String[] getInstrumentationNames() {
        return mInstrumentationNames;
    }

    /**
     * Helper method to get the first instrumentation that can be used as a test runner.
     *
     * @return fully qualified instrumentation class name. <code>null</code> if no valid
     * instrumentation can be found.
     */
    String getValidInstrumentationTestRunner() {
        for (String instrumentation : getInstrumentationNames()) {
            if (validateInstrumentationRunner(instrumentation) == INSTRUMENTATION_OK) {
                return instrumentation;
            }
        }
        return null;
    }

    /**
     * Helper method to determine if specified instrumentation can be used as a test runner
     *
     * @param instrumentation the instrumentation class name to validate. Assumes this
     *   instrumentation is one of {@link #getInstrumentationNames()}
     * @return <code>INSTRUMENTATION_OK</code> if valid, otherwise returns error message
     */
    String validateInstrumentationRunner(String instrumentation) {
        if (!mHasRunnerLibrary) {
            return String.format(LaunchMessages.InstrValidator_NoTestLibMsg_s,
                    AndroidConstants.LIBRARY_TEST_RUNNER);
        }
        // check if this instrumentation is the standard test runner
        if (!instrumentation.equals(SdkConstants.CLASS_INSTRUMENTATION_RUNNER)) {
            // check if it extends the standard test runner
            String result = BaseProjectHelper.testClassForManifest(mJavaProject,
                    instrumentation, SdkConstants.CLASS_INSTRUMENTATION_RUNNER, true);
             if (result != BaseProjectHelper.TEST_CLASS_OK) {
                return String.format(
                        LaunchMessages.InstrValidator_WrongRunnerTypeMsg_s,
                        SdkConstants.CLASS_INSTRUMENTATION_RUNNER);
             }
        }
        return INSTRUMENTATION_OK;
    }
}
