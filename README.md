# tsm-migrator
Migrator application og sykmeldinger

## Technologies used
* Kotlin
* Ktor
* Gradle
* Kafka
* Postgres

#### Requirements
* JDK 17

### Building the application
#### Compile and package application
To build locally and run the integration tests you can simply run
``` bash
./gradlew shadowJar
```
 or  on windows 
`gradlew.bat shadowJar`

### Upgrading the gradle wrapper
Find the newest version of gradle here: https://gradle.org/releases/ Then run this command:

``` bash
./gradlew wrapper --gradle-version $gradleVersjon
```

### Contact
This project is maintained by [navikt/tsm](CODEOWNERS)

Questions and/or feature requests? 
Please create an [issue](https://github.com/navikt/tsm-migrator/issues)

If you work in [@navikt](https://github.com/navikt) you can reach us at the Slack
channel [#team-sykmelding](https://nav-it.slack.com/archives/CMA3XV997)
