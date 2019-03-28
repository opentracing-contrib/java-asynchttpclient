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
import io.opentracing.tag.Tags;
import java.util.HashMap;
import java.util.Map;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.Request;

/**
 * Span decorator to add tags, logs and operation name.
 */
public interface SpanDecorator {
  String COMPONENT_NAME = "asynchttpclient";

  /**
   * Decorate span before a request is made.
   *
   * @param request request
   * @param span span
   */
  void onRequest(Request request, Span span);


  /**
   * Decorate span on a response status
   *
   * @param responseStatus response status
   * @param span span
   */
  void onStatus(HttpResponseStatus responseStatus, Span span);

  /**
   * Decorate span on an error
   *
   * @param throwable exception
   * @param span span
   */
  void onError(Throwable throwable, Span span);


  SpanDecorator DEFAULT = new SpanDecorator() {
    @Override
    public void onRequest(Request request, Span span) {
      Tags.COMPONENT.set(span, COMPONENT_NAME);
      Tags.HTTP_METHOD.set(span, request.getMethod());
      Tags.HTTP_URL.set(span, request.getUrl());
    }

    @Override
    public void onStatus(HttpResponseStatus responseStatus, Span span) {
      span.setTag(Tags.HTTP_STATUS.getKey(), responseStatus.getStatusCode());
    }

    @Override
    public void onError(Throwable throwable, Span span) {
      Tags.ERROR.set(span, Boolean.TRUE);
      if (throwable != null) {
        span.log(errorLogs(throwable));
      }
    }

    private Map<String, Object> errorLogs(final Throwable throwable) {
      final Map<String, Object> errorLogs = new HashMap<>(2);
      errorLogs.put("event", Tags.ERROR.getKey());
      errorLogs.put("error.object", throwable);
      return errorLogs;
    }
  };

}


