/*
 * Copyright 2010 the original author or authors.
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

package org.gradle.api;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.AbstractTask;
import org.gradle.internal.extensibility.NoConventionMapping;
import org.gradle.api.model.ObjectFactory;
import org.gradle.util.DeprecationLogger;

/**
 * {@code DefaultTask} is the standard {@link Task} implementation. You can extend this to implement your own task types.
 */
@NoConventionMapping
public class DefaultTask extends AbstractTask {
    /**
     * Creates a new output directory property for this task.
     *
     * @return The property.
     * @since 4.4
     * @deprecated Replaced by {@link ObjectFactory#directoryProperty()}
     */
    @Deprecated
    protected final DirectoryProperty newOutputDirectory() {
        DeprecationLogger.nagUserOfReplacedMethod("DefaultTask.newOutputDirectory()", "ObjectFactory.directoryProperty()");
        return getServices().get(ObjectFactory.class).directoryProperty();
    }

    /**
     * Creates a new output file property for this task.
     *
     * @return The property.
     * @since 4.4
     * @deprecated Replaced by {@link ObjectFactory#fileProperty()}
     */
    @Deprecated
    protected final RegularFileProperty newOutputFile() {
        DeprecationLogger.nagUserOfReplacedMethod("DefaultTask.newOutputFile()", "ObjectFactory.fileProperty()");
        return getServices().get(ObjectFactory.class).fileProperty();
    }

    /**
     * Creates a new input file property for this task.
     *
     * @return The property.
     * @since 4.4
     * @deprecated Replaced by {@link ObjectFactory#fileProperty()}
     */
    @Deprecated
    protected final RegularFileProperty newInputFile() {
        DeprecationLogger.nagUserOfReplacedMethod("DefaultTask.newInputFile()", "ObjectFactory.fileProperty()");
        return getServices().get(ObjectFactory.class).fileProperty();
    }

    /**
     * Creates a new input directory property for this task.
     *
     * @return The property.
     * @since 4.4
     * @deprecated Replaced by {@link ObjectFactory#directoryProperty()}
     */
    @Deprecated
    protected final DirectoryProperty newInputDirectory() {
        DeprecationLogger.nagUserOfReplacedMethod("DefaultTask.newInputDirectory()", "ObjectFactory.directoryProperty()");
        return getServices().get(ObjectFactory.class).directoryProperty();
    }
}
