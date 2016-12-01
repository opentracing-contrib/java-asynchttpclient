build:
	./mvnw -s settings.xml install

publish: check-env-vars
	./mvnw -s settings.xml clean deploy

check-env-vars:
	if [ -z "${OSSRH_USERNAME}" -o -z "${OSSRH_PASSWORD}" -o -z "${GPG_PASSPHRASE}" ] ; then echo "\n\nERROR: Missing required environment variables; see the Makefile\n\n" ; exit 1 ; fi

