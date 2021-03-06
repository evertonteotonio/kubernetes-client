/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.KubernetesResourceMappingProvider;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Quantity;

public class KubernetesDeserializerTest {

	private KubernetesDeserializer.Mapping mapping;

	@BeforeEach
	public void beforeEach() {
		this.mapping = new TestableMapping(createProvider());
	}

	@Test
	public void shouldRegisterKind() {
		// given
		String version = "version1";
		String kind = "kind1";
		String key = mapping.createKey(version, kind);
		assertThat(mapping.getForKey(key), is(nullValue()));
		// when
		mapping.registerKind(version, kind, SmurfResource.class);
		// then
		Class<? extends KubernetesResource> clazz = mapping.getForKey(key);
		assertThat(clazz, equalTo(SmurfResource.class));
	}

	@Test
	public void shouldNPEIfRegisterNullKind() {
		// given
		// when
		assertThrows(NullPointerException.class, () -> {
			mapping.registerKind("version1", null, SmurfResource.class);
		});
		// then throws
	}

	@Test
	public void shouldRegisterKindWithoutVersionIfNullVersion() {
		// given
		String version = null;
		String kind = "kind1";
		String key = mapping.createKey(version, kind);
		// when
		mapping.registerKind(version, kind, SmurfResource.class);
		// then
		Class<? extends KubernetesResource> clazz = mapping.getForKey(key);
		assertThat(clazz, equalTo(SmurfResource.class));
	}

	@Test
	public void shouldRegisterProvider() {
		// given
		String key = mapping.createKey("42", "Hitchhiker");
		assertThat(mapping.getForKey(key), is(nullValue()));
		KubernetesResourceMappingProvider provider = createProvider(
				Pair.of(key, SmurfResource.class));
		// when
		mapping.registerProvider(provider);
		// then
		Class<? extends KubernetesResource> clazz = mapping.getForKey(key);
		assertThat(clazz, equalTo(SmurfResource.class));
	}

	@Test
	public void shouldReturnMappedClass() {
		// given
		String version = "version1";
		String kind = SmurfResource.class.getSimpleName();
		String key = mapping.createKey(version, kind);
		assertThat(mapping.getForKey(key), is(nullValue()));
		mapping.registerKind(version, kind, SmurfResource.class);
		// when
		Class<? extends KubernetesResource> clazz = mapping.getForKey(key);
		// then
		assertThat(clazz, equalTo(SmurfResource.class));
	}

	@Test
	public void shouldReturnNullIfKeyIsNull() {
		// given
		// when
		Class<? extends KubernetesResource> clazz = mapping.getForKey(null);
		// then
		assertThat(clazz, is(nullValue()));
	}

	@Test
	public void shouldLoadClassInPackage() {
		// given
		String key = mapping.createKey("42", Pod.class.getSimpleName());
		// when
		Class<? extends KubernetesResource> clazz = mapping.getForKey(key);
		// then
		assertThat(clazz, equalTo(Pod.class));
	}

	@Test
	public void shouldNotLoadClassInPackageIfNotKubernetesResource() {
		// given Quantity is not a KuberntesResource
		String key = mapping.createKey("42", Quantity.class.getSimpleName());
		// when
		Class<? extends KubernetesResource> clazz = mapping.getForKey(key);
		// then
		assertThat(clazz, is(nullValue()));
	}

	@Test
	public void shouldLoadClassIfKeyOnlyHasKind() {
		// given Quantity is not a KuberntesResource
		String key = mapping.createKey(null, Pod.class.getSimpleName());
		// when
		Class<? extends KubernetesResource> clazz = mapping.getForKey(key);
		// then
		assertThat(clazz, equalTo(Pod.class));
	}

	private KubernetesResourceMappingProvider createProvider(Pair<String, Class<? extends KubernetesResource>>... mappings) {
		return () -> Stream.of(mappings)
				.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}
	
	public class TestableMapping extends KubernetesDeserializer.Mapping {

		private KubernetesResourceMappingProvider provider;

		public TestableMapping(KubernetesResourceMappingProvider provider) {
			this.provider = provider;
		}

		@Override
		protected Stream<KubernetesResourceMappingProvider> getAllMappingProviders() {
			return Stream.of(provider);
		}


		@Override
		protected String createKey(String apiVersion, String kind) {
			return super.createKey(apiVersion, kind);
		}
	}

	private class SmurfResource implements KubernetesResource {
	}
}
