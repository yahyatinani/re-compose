# Re-compose

[![build](https://github.com/yahyatinani/re-compose/actions/workflows/main.yml/badge.svg)](https://github.com/yahyatinani/re-compose/actions/workflows/main.yml)
![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/io.github.yahyatinani.recompose/re-compose?server=https%3A%2F%2Fs01.oss.sonatype.org%2F&label=latest%20release&color=blue)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/io.github.yahyatinani.recompose/re-compose?server=https%3A%2F%2Fs01.oss.sonatype.org%2F&label=latest%20snapshots)](https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/yahyatinani/recompose/re-compose/)
![GitHub](https://img.shields.io/github/license/yahyatinani/re-compose)

`re-compose` is a data-oriented event driven framework for manging state in
Android Compose applications.

### Dependencies:

* **re-compose**: core module for Jetpack Compose apps.
    * `implementation "io.github.yahyatinani.recompose:re-compose:$version"`
* **http-fx**: It wraps the ktor android client for http requests. (WIP)
    * `implementation "io.github.yahyatinani.recompose:http-fx:$version"`
* **paging-fx**: It wraps the paging compose library. (WIP)
    * `implementation "io.github.yahyatinani.recompose:paging-fx:$version"`
* **fsm**: WIP
    * `implementation "io.github.yahyatinani.recompose:fsm:$version"`
