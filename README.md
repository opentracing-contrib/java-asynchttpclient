# java-asynchttpclient

OpenTracing Instrumentation for org.asynchttpclient

## Requirements

- [AsyncHttpClient](https://github.com/AsyncHttpClient/async-http-client) >= 2.0.0
- [OpenTracing Java](https://github.com/opentracing/opentracing-java) >= 0.16.0

## Installation

Releases are hosted on Maven Central.

pom.xml
```xml
<dependency>
    <groupId>io.opentracing.contrib.asynchttpclient</groupId>
    <artifactId>asynchttpclient-opentracing</artifactId>
    <version>0.1.0</version>
</dependency>
```

build.gradle
```groovy
dependencies {
    compile 'io.opentracing.contrib.asynchttpclient:asynchtpclient-opentracing:0.1.0'
}
```

## Usage

- Intialize an OpenTracing tracer
- Create an `ActiveSpanSource` to return a parent span when a span is created for a request.
- Create a `SpanDecorator` or use the `DEFAULT` implementation.
- Create a `TracingAsyncHttpClient` and use it to make requests.

```java
class MyClass {
    final ThreadLocal<Span> activeSpan;

    public MyClass() {
        activeSpan = new ThreadLocal<Span>() {};

        // Source that can extract span data and keep it in a thread-local:
        TracingAsyncHttpClient.ActiveSpanSource activeSpanSource = 
            new TracingAsyncHttpClient.ActiveSpanSource() {
                public Span getActiveSpan() {
                    return activeSpan.get();
                }
            };

        Tracer tracer = ... // Any OpenTracing-compatible tracer.

        AsyncHttpClient client = new TracingAsyncHttpClient(
                    tracer,
                    activeSpanSource,
                    SpanDecorator.DEFAULT
                );
    }
}
```
