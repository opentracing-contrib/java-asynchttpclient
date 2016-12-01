# Releasing java-asynchhttpclient

1. Make sure you have permission to publish for the io.opentracing namespace. See the [Sonatype Guide](http://central.sonatype.org/pages/ossrh-guide.html) for more information.

1. Export these environment variables:
```
OSSRH_USERNAME
OSSRH_PASSWORD
GPG_PASSPHRASE
```

1. Remove `snapshot` from your version in pom.xml: `0.1.0-SNAPSHOT` -> `0.1.0`

1. Push a release tag for your git commit on release: `0.1.0` for example.

1. Run `make publish`

1. Update the version # in pom.xml to be a newer snapshot version for development.
`0.2.3 -> 0.3.0-SNAPSHOT`, for example. 
