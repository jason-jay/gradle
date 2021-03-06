/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.internal.operations.trace;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

class StrictMap<K, V> extends HashMap<K, V> {

    public StrictMap(Map<? extends K, ? extends V> m) {
        super(m);
    }

    @Override
    public V get(Object key) {
        if (!containsKey(key)) {
            throw new NoSuchElementException("No entry with key: " + key + " (keys: " + keySet() +  ")");
        }

        return super.get(key);
    }
}
