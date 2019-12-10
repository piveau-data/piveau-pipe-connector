# ChangeLog

## [2.0.3](https://gitlab.fokus.fraunhofer.de/viaduct/pipe-connector/tags/2.0.3) (2019-10-16)

**Added:**
* Possibility to mount a sub-router to the connector

## [2.0.2](https://gitlab.fokus.fraunhofer.de/viaduct/pipe-connector/tags/2.0.2) (2019-10-02)

**Fixed:**
* Failed check negated

## [2.0.1](https://gitlab.fokus.fraunhofer.de/viaduct/pipe-connector/tags/2.0.1) (2019-08-28)

**Changed**
* Requires now latest LTS Java 11
* Update pipe model to 2.1.0

## [2.0.0](https://gitlab.fokus.fraunhofer.de/viaduct/pipe-connector/tags/2.0.0) (2019-08-22)

**Changed**
* `PipeLogger`, `PipeContext` and `PipeMailer` classes implemented in kotlin
* Updated to pipe-model version 2.0.0. This forces a major version update as well!

## [1.0.4](https://gitlab.fokus.fraunhofer.de/viaduct/pipe-connector/tags/1.0.4) (2019-08-13)

**Fixed:**
* Another hotfix for deep copy of pipe.

## [1.0.3](https://gitlab.fokus.fraunhofer.de/viaduct/pipe-connector/tags/1.0.3) (2019-08-12)

**Fixed:**
* Hotfix for deep copy of pipe.

## [1.0.2](https://gitlab.fokus.fraunhofer.de/viaduct/pipe-connector/tags/1.0.2) (2019-08-12)

**Fixed:**
* `setPayloadData` passing non null value of `DataType` (kotlin)

## [1.0.1](https://gitlab.fokus.fraunhofer.de/viaduct/pipe-connector/tags/1.0.1) (2019-08-09)

**Fixed:**
* Update pipe-model, fix for Pipe deserialization

## [1.0.0](https://gitlab.fokus.fraunhofer.de/viaduct/pipe-connector/tags/1.0.0) (2019-08-09)

**Added:**
* kotlin support
* Test stage as part of gitlab ci

**Changed:**
* Upgrade to Vert.x 3.8.0
* Upgrade to kotlin 1.3.41 (no implementations yet)
* Upgrade to pipe-model 1.0.0 (pure kotlin)
 
## [0.0.4](https://gitlab.fokus.fraunhofer.de/viaduct/pipe-connector/tags/0.0.4) (2019-??-??)

**Added:**
* `/health` path up status with build info
* `/metrics` path with metrics info
* `/config/schema` returns config json schema

**Changed:**
* Log output in case of forwarding
* Upgrade to Vert.x 3.7.1

**Fixed:**
* Resource filtering before build

## [0.0.3](https://gitlab.fokus.fraunhofer.de/viaduct/pipe-connector/tags/0.0.3) (2019-04-15)

**Added:**
- EMail support
- Configuration for the release plugin in the pom file
- ChangeLog

**Changed:**
- Upgrade to Vert.x 3.7.0

## [0.0.2](https://gitlab.fokus.fraunhofer.de/viaduct/pipe-connector/tags/pipe-connector-0.0.2) (2019-03-01)

## [0.0.1](https://gitlab.fokus.fraunhofer.de/viaduct/pipe-connector/tags/pipe-connector-0.0.1) (2019-03-01)
