/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.gradle.revapi.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.palantir.gradle.revapi.config.v1.DeprecatedAcceptedBreaks;
import com.palantir.gradle.revapi.config.v2.AcceptedBreak;
import com.palantir.gradle.revapi.config.v2.AllAcceptedBreaks;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
@ImmutablesStyle
@JsonDeserialize(as = ImmutableGradleRevapiConfig.class)
public abstract class GradleRevapiConfig {
    private static final String ACCEPTED_BREAKS_V2 = "acceptedBreaksV2";

    protected abstract Map<GroupNameVersion, String> versionOverrides();

    /** Overridden by immutables. */
    @Value.Default
    @JsonProperty(value = "acceptedBreaks", access = Access.WRITE_ONLY)
    protected DeprecatedAcceptedBreaks oldDeprecatedV1AcceptedBreaksDeserOnly() {
        return DeprecatedAcceptedBreaks.empty();
    }

    /** Overridden by immutables. */
    @Value.Default
    @JsonProperty(value = ACCEPTED_BREAKS_V2, access = Access.WRITE_ONLY)
    protected AllAcceptedBreaks acceptedBreaksV2DeserOnly() {
        return AllAcceptedBreaks.empty();
    }

    /** Overridden by immutables. */
    @Value.Default
    @JsonProperty(value = ACCEPTED_BREAKS_V2, access = Access.READ_ONLY)
    protected AllAcceptedBreaks acceptedBreaksV2() {
        return oldDeprecatedV1AcceptedBreaksDeserOnly()
                .upgrade()
                .andAlso(acceptedBreaksV2DeserOnly());
    }

    public final Optional<String> versionOverrideFor(GroupNameVersion groupNameVersion) {
        return Optional.ofNullable(versionOverrides().get(groupNameVersion));
    }

    public final GradleRevapiConfig addVersionOverride(GroupNameVersion groupNameVersion, String versionOverride) {
        return ImmutableGradleRevapiConfig.builder()
                .from(this)
                .putVersionOverrides(groupNameVersion, versionOverride)
                .build();
    }

    public final Set<FlattenedBreak> acceptedBreaks(GroupAndName groupAndName) {
        return acceptedBreaksV2().flattenedBreaksFor(groupAndName);
    }

    public final Set<AcceptedBreak> acceptedBreaksFor(GroupNameVersion groupNameVersion) {
        return acceptedBreaksV2().acceptedBreaksFor(groupNameVersion);
    }

    public final GradleRevapiConfig addAcceptedBreaks(
            GroupNameVersion groupNameVersion,
            Justification justification,
            Set<AcceptedBreak> newAcceptedBreaks) {

        return ImmutableGradleRevapiConfig.builder()
                .from(this)
                .acceptedBreaksV2(acceptedBreaksV2().addAcceptedBreaks(
                        groupNameVersion,
                        justification,
                        newAcceptedBreaks))
                .build();
    }

    public static GradleRevapiConfig empty() {
        return ImmutableGradleRevapiConfig.builder().build();
    }

    public static ObjectMapper newYamlObjectMapper() {
        return configureObjectMapper(new ObjectMapper(new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)));
    }

    public static ObjectMapper newJsonObjectMapper() {
        return configureObjectMapper(new ObjectMapper())
                .enable(JsonParser.Feature.ALLOW_TRAILING_COMMA);
    }

    private static ObjectMapper configureObjectMapper(ObjectMapper objectMapper) {
        return objectMapper
                .registerModule(new GuavaModule())
                .registerModule(new Jdk8Module());
    }
}
