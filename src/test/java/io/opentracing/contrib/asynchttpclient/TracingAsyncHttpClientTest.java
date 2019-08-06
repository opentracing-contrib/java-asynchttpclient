/*
 * Copyright 2016-2019 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.asynchttpclient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.opentracing.Scope;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.asynchttpclient.util.HttpConstants;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TracingAsyncHttpClientTest {
  private static final MockTracer tracer = new MockTracer();

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(8888);

  @Before
  public void before() {
    tracer.reset();
  }

  @Test
  public void test() throws Exception {
    stubFor(get(urlPathMatching("/.*"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("test")));

    AsyncHttpClient client = new TracingAsyncHttpClient(tracer);

    Request request = new RequestBuilder(HttpConstants.Methods.GET)
        .setUrl("http://localhost:8888")
        .build();

    final Response response = client.executeRequest(request).get(10, TimeUnit.SECONDS);
    assertEquals("test", response.getResponseBody());

    List<MockSpan> spans = tracer.finishedSpans();
    assertEquals(1, spans.size());

    checkSpans(spans);

    assertNull(tracer.activeSpan());
  }

  @Test
  public void testNoActiveSpan() throws Exception {
    stubFor(get(urlPathMatching("/.*"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("no-span")));

    AsyncHttpClient client = new TracingAsyncHttpClient(tracer, true);

    Request request = new RequestBuilder(HttpConstants.Methods.GET)
        .setUrl("http://localhost:8888")
        .build();

    final Response response = client.executeRequest(request).get(10, TimeUnit.SECONDS);
    assertEquals("no-span", response.getResponseBody());

    List<MockSpan> spans = tracer.finishedSpans();
    assertEquals(0, spans.size());

    assertNull(tracer.activeSpan());
  }

  @Test
  public void testWithActiveSpan() throws Exception {
    stubFor(get(urlPathMatching("/.*"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("active-span")));

    AsyncHttpClient client = new TracingAsyncHttpClient(tracer, true);

    Request request = new RequestBuilder(HttpConstants.Methods.GET)
        .setUrl("http://localhost:8888")
        .build();

    final MockSpan parent = tracer.buildSpan("parent").start();
    try (final Scope ignored = tracer.activateSpan(parent)) {
      final Response response = client.executeRequest(request).get(10, TimeUnit.SECONDS);
      assertEquals("active-span", response.getResponseBody());
    }
    parent.finish();

    List<MockSpan> spans = tracer.finishedSpans();
    assertEquals(2, spans.size());

    assertNull(tracer.activeSpan());
  }


  private void checkSpans(List<MockSpan> spans) {
    for (MockSpan mockSpan : spans) {
      assertEquals(Tags.SPAN_KIND_CLIENT, mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
      assertEquals(SpanDecorator.COMPONENT_NAME, mockSpan.tags().get(Tags.COMPONENT.getKey()));
      assertNull(mockSpan.tags().get(Tags.ERROR.getKey()));
      assertEquals(0, mockSpan.logEntries().size());
      assertEquals(0, mockSpan.generatedErrors().size());
      assertEquals("GET", mockSpan.tags().get(Tags.HTTP_METHOD.getKey()));
      assertEquals("GET", mockSpan.operationName());
      assertEquals(200, mockSpan.tags().get(Tags.HTTP_STATUS.getKey()));
    }
  }
}