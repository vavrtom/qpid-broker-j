/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.qpid.server.model.preferences;

import java.security.Principal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface UserPreferences
{
    Preference createPreference(UUID id,
                                String type,
                                String name,
                                String description,
                                Set<Principal> visibilitySet,
                                Map<String, Object> preferenceValueAttributes);

    void updateOrAppend(Collection<Preference> preferences);

    Set<Preference> getPreferences();

    void replace(Collection<Preference> preferences);

    void replaceByType(String type, Collection<Preference> preferences);

    void replaceByTypeAndName(String type, String name, Preference preference);

    Set<Preference> getVisiblePreferences();
}