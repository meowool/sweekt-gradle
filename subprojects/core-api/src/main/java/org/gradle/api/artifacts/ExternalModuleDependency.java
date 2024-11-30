/*
 * Copyright 2007-2008 the original author or authors.
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
package org.gradle.api.artifacts;

/**
 * <p>A {@code ExternalModuleDependency} is a {@link Dependency} on a module outside the current project hierarchy.</p>
 */
public interface ExternalModuleDependency extends ExternalDependency {
    /**
     * Returns whether Gradle may check for a change in the remote repository
     *
     * @see #setChanging(boolean)
     */
    boolean isChanging();

    /**
     * Sets whether Gradle may check for a change in the remote repository. If set to true, Gradle will
     * check the remote repository if either --refresh--dependencies was passed or the local entry has expired.
     * If set to false, Gradle will use the local entry even if there is a new artifact in the remote repository
     * and --refresh-dependencies was passed. Defaults to false.
     *
     * @param changing Gradle can check for a change in the remote repository, even if a local entry already exists
     * @return this
     */
    ExternalModuleDependency setChanging(boolean changing);

    /**
     * {@inheritDoc}
     */
    @Override
    ExternalModuleDependency copy();
}
