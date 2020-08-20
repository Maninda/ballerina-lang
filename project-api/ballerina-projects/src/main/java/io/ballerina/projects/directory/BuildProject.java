/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerina.projects.directory;

import io.ballerina.projects.PackageConfig;
import io.ballerina.projects.Project;
import io.ballerina.projects.model.BallerinaToml;
import io.ballerina.projects.utils.RepoUtils;

import java.nio.file.Path;

/**
 * {@code BuildProject} represents Ballerina project instance created from the project directory.
 *
 * @since 2.0.0
 */
public class BuildProject extends Project {

    public static BuildProject loadProject(Path projectPath) throws Exception {
        return new BuildProject(projectPath);
    }

    private BuildProject(Path projectPath) throws Exception {
        super();
        if (!RepoUtils.isBallerinaProject(projectPath)) {
            throw new Exception("invalid Ballerina source path:" + projectPath);
        }
        this.sourceRoot = projectPath;

        addPackage(projectPath.toString());

        // Set default build options
        this.context.setBuildOptions(new BuildProject.BuildOptions(this.context.currentPackage().ballerinaToml()));
    }

    private void addPackage(String projectPath) {
        final PackageConfig packageConfig = PackageLoader.loadPackage(projectPath, false);
        this.context.addPackage(packageConfig);
    }

    public BuildOptions getBuildOptions() {
        return (BuildOptions) this.context.getBuildOptions();
    }
    public void setBuildOptions(BuildOptions newBuildOptions) {
        BuildOptions buildOptions = (BuildOptions) this.context.getBuildOptions();
        buildOptions.setB7aConfigFile(newBuildOptions.getB7aConfigFile());
        buildOptions.setObservabilityEnabled(newBuildOptions.isObservabilityIncluded());
        buildOptions.setSkipLock(newBuildOptions.isSkipLock());
        this.context.setBuildOptions(newBuildOptions);
    }

    /**
     * {@code BuildOptions} represents build options specific to a build project.
     */
    public static class BuildOptions extends io.ballerina.projects.BuildOptions {

        private BuildOptions(BallerinaToml ballerinaToml) {
            if (ballerinaToml.getBuildOptions() != null) {
                this.observabilityIncluded = ballerinaToml.getBuildOptions().isObservabilityIncluded();
            }
//            this.skipLock = ballerinaToml.getBuildOptions().skipLock();
//            this.b7aConfigFile = ballerinaToml.getBuildOptions().getB7aConfig();
        }

        public void setObservabilityEnabled(boolean observabilityEnabled) {
            observabilityIncluded = observabilityEnabled;
        }

        public void setSkipLock(boolean skipLock) {
            this.skipLock = skipLock;
        }

        public boolean isObservabilityIncluded() {
            return observabilityIncluded;
        }

        public boolean isSkipLock() {
            return skipLock;
        }

        public  boolean isCodeCoverage() {
            return this.codeCoverage;
        }
        public void setCodeCoverage(boolean codeCoverage) {
            this.codeCoverage = codeCoverage;
        }
    }
}
