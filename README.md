[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.ddemin/tests-router/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.ddemin/tests-router)  [![Build Status](https://travis-ci.org/ddemin/tests-router.svg?branch=master)](https://travis-ci.org/ddemin/tests-router)  [![codecov](https://codecov.io/gh/ddemin/tests-router/branch/master/graph/badge.svg)](https://codecov.io/gh/ddemin/tests-router)
# Documentation
[Please look at project's Wiki](https://github.com/ddemin/tests-router/wiki)
# Production Usage
Private and modified fork of this library is currently used by one of national-level company at Russian Federation
# TBD
Based on experience with private fork it will be nice to have next improvements:
1. Up dependencies versions (cucumber-jvm at minimum)
2. Add JUnit 5 support
3. Add Cucumber-JVM over JUnit5 support
4. Add YAML config for environments definition
5. Add "env-resource" mechanism (each environment will be have set of personal objects like DB clients, API clients, emulators and these objects will be configured in YAML)
6. Use some configuration framework (like OWNER http://owner.aeonbits.org/)
7. Increase test coverage to 80+%
8. Simplify code
