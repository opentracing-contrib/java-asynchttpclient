package io.opentracing.contrib.asynchttpclient;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import org.asynchttpclient.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * An {@link org.asynchttpclient.AsyncHttpClient} that traces HTTP calls using the OpenTracing API.
 */
@SuppressWarnings("unused")
public class TracingAsyncHttpClient extends DefaultAsyncHttpClient {

    private Tracer tracer;
    private ActiveSpanContextSource activeSpanContextSource;
    private SpanDecorator spanDecorator;
    private List<CompletableFuture<?>> traceFutures;

    @SuppressWarnings({"WeakerAccess", "unused"})
    public TracingAsyncHttpClient(Tracer tracer, ActiveSpanContextSource activeSpanContextSource, SpanDecorator spanDecorator) {
        this(tracer, activeSpanContextSource, spanDecorator, new DefaultAsyncHttpClientConfig.Builder().build());
    }

    @SuppressWarnings("WeakerAccess")
    public TracingAsyncHttpClient(Tracer tracer, ActiveSpanContextSource activeSpanContextSource, SpanDecorator spanDecorator, AsyncHttpClientConfig config) {
        super(config);
        this.tracer = tracer;
        this.activeSpanContextSource = activeSpanContextSource;
        this.spanDecorator = spanDecorator;
        // Retain a reference to futures so they aren't GC'ed before completion.
        this.traceFutures = new ArrayList<>();
    }

    @Override
    public ListenableFuture<Response> executeRequest(Request request) {
        RequestBuilder builder = new RequestBuilder(request);
        return this.executeRequest(builder);
    }

    @Override
    public ListenableFuture<Response> executeRequest(RequestBuilder builder) {
        return tracedFuture(builder,
                (requestBuilder) -> super.executeRequest(requestBuilder.build()));
    }

    @Override
    public <T> ListenableFuture<T> executeRequest(Request request, AsyncHandler<T> handler) {
        RequestBuilder builder = new RequestBuilder(request);
        return this.executeRequest(builder, handler);
    }

    @Override
    public <T> ListenableFuture<T> executeRequest(RequestBuilder builder, AsyncHandler<T> handler) {
        return tracedFuture(builder,
                (requestBuilder) -> super.executeRequest(requestBuilder.build(), handler));
    }

    private <T> ListenableFuture<T> tracedFuture(RequestBuilder requestBuilder, ListenableFutureGenerator<T> generator) {
        Tracer.SpanBuilder builder = tracer.buildSpan(spanDecorator.getOperationName(requestBuilder));


        SpanContext parent = activeSpanContextSource.getActiveSpanContext();
        builder.asChildOf(parent);
        spanDecorator.decorateSpan(builder, requestBuilder);

        final Span span = builder.start();
        tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, new TextMap() {
            @Override
            public Iterator<Map.Entry<String, String>> iterator() {
                throw new UnsupportedOperationException("iterator should never be used with Tracer.inject()");
            }

            @Override
            public void put(String key, String value) {
                requestBuilder.addHeader(key, value);
            }
        });
        ListenableFuture<T> lf = generator.generateFuture(requestBuilder);
        traceFutures.add(lf.toCompletableFuture().whenComplete(
                (t, throwable) -> {
                    span.finish();
                    clearFinished(traceFutures);
                }));
        return lf;
    }

    private static void clearFinished(List<CompletableFuture<?>> futures) {
        Iterator<CompletableFuture<?>> iter = futures.iterator();
        while (iter.hasNext()) {
            if (iter.next().isDone()) {
                iter.remove();
            }
        }
    }

    /**
     * An Abstract API that allows the TracingAsyncHttpClient to customize how spans are named and decorated.
     */
    @SuppressWarnings("WeakerAccess")
    public interface SpanDecorator {
        /**
         * @param requestBuilder the request that a span is being constructed for.
         * @return the operation name a span should use for this request.
         */
        String getOperationName(RequestBuilder requestBuilder);

        /**
         * Adds data to a span based on the contents of the request.
         * @param span the span for an operation.
         * @param requestBuilder the request that represents the operation.
         */
        void decorateSpan(Tracer.SpanBuilder span, RequestBuilder requestBuilder);


        @SuppressWarnings("unused")
        SpanDecorator DEFAULT = new SpanDecorator() {
            @Override
            public String getOperationName(RequestBuilder requestBuilder) {
                Request request = requestBuilder.build();
                return request.getUri().getHost() + ":" + request.getMethod();
            }

            @Override
            public void decorateSpan(Tracer.SpanBuilder span, RequestBuilder request) {
                // Be default, do nothing.
            }
        };
    }

    /**
     * An abstract API that allows the TracingAsyncHttpClient to customize how parent SpanContexts are discovered.
     *
     * For instance, if SpansContext are stored in a thread-local variable, an ActiveSpanContextSource could access them like so:
     * <p>Example usage:
     * <pre>{@code
     * public class SomeClass {
     *     // Thread local variable containing each thread's activeSpanContext
     *     private static final ThreadLocal<SpanContext> activeSpanContext =
     *         new ThreadLocal<SpanContext>();
     *
     * ... elsewhere ...
     * ActiveSpanContextSource spanContextSource = new ActiveSpanContextSource() {
     *     public Span activeSpanContext() {
     *         return activeSpanContext.get();
     *     }
     * };
     *
     * ...
     * }
     *}</pre>
     */
    public interface ActiveSpanContextSource{
        /**
         * Get the currently active SpanContext to serve as a parent spancontext for new spans.
         *
         * @return the currently active spancontext, or null.
         */
        SpanContext getActiveSpanContext();
    }

    private interface ListenableFutureGenerator<T> {
        ListenableFuture<T> generateFuture(RequestBuilder requestBuilder);
    }
}
