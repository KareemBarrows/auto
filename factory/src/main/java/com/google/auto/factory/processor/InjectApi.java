/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.auto.factory.processor;

import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Encapsulates the choice of {@code jakarta.inject} or {@code javax.inject} depending on what is on
 * the class path.
 */
@AutoValue
abstract class InjectApi {
  abstract TypeElement inject();

  abstract TypeElement provider();

  private static final ImmutableList<String> PREFIXES_IN_ORDER =
      ImmutableList.of("jakarta.inject.", "javax.inject.");

  static InjectApi from(Elements elementUtils, @Nullable String apiPrefix) {
    ImmutableList<String> apiPackages =
        (apiPrefix == null) ? PREFIXES_IN_ORDER : ImmutableList.of(apiPrefix + ".inject.");
    for (String apiPackage : apiPackages) {
      Map<String, TypeElement> apiMap = apiMap(elementUtils, apiPackage);
      TypeElement inject = apiMap.get("Inject");
      TypeElement provider = apiMap.get("Provider");
      if (inject != null && provider != null) {
        return new AutoValue_InjectApi(inject, provider);
      }
    }
    String missing = apiPackages.stream().map(s -> s + ".*").collect(joining(" or "));
    throw new IllegalStateException("Class path must include " + missing);
  }

  private static Map<String, TypeElement> apiMap(Elements elementUtils, String apiPackage) {
    Map<String, TypeElement> map = new HashMap<>();
    for (String name : API_CLASSES) {
      TypeElement type = elementUtils.getTypeElement(apiPackage + name);
      if (type != null) {
        map.put(name, type);
      }
    }
    return map;
  }

  private static final ImmutableSet<String> API_CLASSES = ImmutableSet.of("Inject", "Provider");
}
