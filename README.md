[![Build Status][ci-img]][ci] [![Coverage Status][cov-img]][cov] [![Released Version][maven-img]][maven] [![Apache-2.0 license](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

OpenTracing Instrumentation for org.asynchttpclient

## Requirements

- [AsyncHttpClient](https://github.com/AsyncHttpClient/async-http-client) >= 2.0.0
- [OpenTracing Java](https://github.com/opentracing/opentracing-java) = 0.32.0

## Installation

Releases are hosted on Maven Central.

pom.xml
```xml
<dependency>
    <groupId>io.opentracing.contrib</groupId>
    <artifactId>opentracing-asynchttpclient</artifactId>
    <version>VERSION</version>
</dependency>
```

## Usage

```java
// Instantiate tracer
Tracer tracer = ...


// Build TracingAsyncHttpClient
AsyncHttpClient client = new TracingAsyncHttpClient(tracer);

```

## License

[Apache 2.0 License](./LICENSE).

[ci-img]: https://travis-ci.org/opentracing-contrib/java-asynchttpclient.svg?branch=master
[ci]: https://travis-ci.org/opentracing-contrib/java-asynchttpclient
[cov-img]: https://coveralls.io/repos/github/opentracing-contrib/java-asynchttpclient/badge.svg?branch=master
[cov]: https://coveralls.io/github/opentracing-contrib/java-asynchttpclient?branch=master
[maven-img]: https://img.shields.io/maven-central/v/io.opentracing.contrib/opentracing-asynchttpclient.svg
[maven]: http://search.maven.org/#search%7Cga%7C1%7Copentracing-asynchttpclient

