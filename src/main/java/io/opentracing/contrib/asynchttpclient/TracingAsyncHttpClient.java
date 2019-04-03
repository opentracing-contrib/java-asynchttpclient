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

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Request;

/**
 * An {@link org.asynchttpclient.AsyncHttpClient} that traces HTTP calls using the OpenTracing API.
 */
@SuppressWarnings("unused")
public class TracingAsyncHttpClient extends DefaultAsyncHttpClient {

  private Tracer tracer;
  private final List<SpanDecorator> decorators;
  private final boolean traceWithActiveSpanOnly;

  public TracingAsyncHttpClient(Tracer tracer) {
    this(tracer, Collections.singletonList(SpanDecorator.DEFAULT), false);
  }

  public TracingAsyncHttpClient(Tracer tracer, boolean traceWithActiveSpanOnly) {
    this(tracer, Collections.singletonList(SpanDecorator.DEFAULT), traceWithActiveSpanOnly);
  }

  public TracingAsyncHttpClient(Tracer tracer, List<SpanDecorator> decorators) {
    this(tracer, decorators, false);
  }

  public TracingAsyncHttpClient(Tracer tracer, List<SpanDecorator> decorators,
      boolean traceWithActiveSpanOnly) {
    this.tracer = tracer;
    this.decorators = new ArrayList<>(decorators);
    this.traceWithActiveSpanOnly = traceWithActiveSpanOnly;
  }

  public TracingAsyncHttpClient(AsyncHttpClientConfig config, Tracer tracer) {
    this(config, tracer, Collections.singletonList(SpanDecorator.DEFAULT), false);
  }

  public TracingAsyncHttpClient(AsyncHttpClientConfig config, Tracer tracer,
      boolean traceWithActiveSpanOnly) {
    this(config, tracer, Collections.singletonList(SpanDecorator.DEFAULT), traceWithActiveSpanOnly);
  }

  public TracingAsyncHttpClient(AsyncHttpClientConfig config, Tracer tracer,
      List<SpanDecorator> decorators) {
    this(config, tracer, decorators, false);
  }

  public TracingAsyncHttpClient(AsyncHttpClientConfig config, Tracer tracer,
      List<SpanDecorator> decorators, boolean traceWithActiveSpanOnly) {
    super(config);
    this.tracer = tracer;
    this.decorators = new ArrayList<>(decorators);
    this.traceWithActiveSpanOnly = traceWithActiveSpanOnly;
  }

  @Override
  public <T> ListenableFuture<T> executeRequest(Request request, AsyncHandler<T> handler) {
    if (traceWithActiveSpanOnly && tracer.activeSpan() == null) {
      return super.executeRequest(request, handler);
    }

    final Span span = tracer
        .buildSpan(request.getMethod())
        .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT).start();

    for (SpanDecorator decorator : decorators) {
      decorator.onRequest(request, span);
    }

    tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS,
        new HttpTextMapInjectAdapter(request.getHeaders()));

    return super.executeRequest(request, new TracingAsyncHandler<>(tracer, handler, span,
        decorators));
  }

}
