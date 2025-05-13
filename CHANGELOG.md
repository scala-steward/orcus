# Changelog

## [0.28.12](https://github.com/scala-steward/orcus/compare/v0.28.12...v0.28.12) (2025-05-13)


### ⚠ BREAKING CHANGES

* move derived methods to each companion object
* remove some modules

### Features

* move the callback call into the delay function ([272a029](https://github.com/scala-steward/orcus/commit/272a029f0e9c501c01e6db87101dc0e6cebcf4dc))
* move the callback call into the delay function ([73cfebe](https://github.com/scala-steward/orcus/commit/73cfebe2cd82ef9487449d1a092fca9a1323e632))
* remove some modules ([f95640c](https://github.com/scala-steward/orcus/commit/f95640c7e1a1884ed620bfa4a92cd2475a29b95b))
* scala3 ([b5c281e](https://github.com/scala-steward/orcus/commit/b5c281eb0d8d40d531309b9368994e99d66019c1))
* scala3 ([6a6834b](https://github.com/scala-steward/orcus/commit/6a6834b791e169583e0252b10960b934b19d4650))


### Bug Fixes

* delete println ([e38c538](https://github.com/scala-steward/orcus/commit/e38c5385cc34fb6ef00a2127e11caad00e739746))
* delete println ([28dc50a](https://github.com/scala-steward/orcus/commit/28dc50abe9641eb1993ae3bbbb28f8fc4956c8fe))
* incorrect cancel handling ([06b880d](https://github.com/scala-steward/orcus/commit/06b880d726f0ec9a408b195536e329db0ccfd42c))
* incorrect cancel handling ([910a9f7](https://github.com/scala-steward/orcus/commit/910a9f7ded42514ce8cea1c6867839b9b0a0e86f))
* separate codecs for each scala version ([774020d](https://github.com/scala-steward/orcus/commit/774020d30a98e126f20bea6013efa70caa4b34fa))
* separate codecs for each scala version ([bd41eff](https://github.com/scala-steward/orcus/commit/bd41eff455118a0d90f3421df5e38b677df63eeb))


### Miscellaneous Chores

* release 0.28.12 ([547b222](https://github.com/scala-steward/orcus/commit/547b22294493765972afe5d5e80269895164ad9e))


### Code Refactoring

* move derived methods to each companion object ([cbb4d5b](https://github.com/scala-steward/orcus/commit/cbb4d5b7c2256917181c55f2ef9506c16d31138c))

## [0.28.12](https://github.com/tkrs/orcus/compare/v0.28.11...v0.28.12) (2025-05-01)


### Miscellaneous Chores

* release 0.28.12 ([547b222](https://github.com/tkrs/orcus/commit/547b22294493765972afe5d5e80269895164ad9e))

## v0.16.2 (06/08/2018)
- #148 Upgraded HBase client
---

## v0.13.0 (27/03/2018)
Support for asynchronous APIs and remove synchronous APIs.

https://github.com/tkrs/orcus/compare/v0.12.0...v0.13.0
---

## v0.12.0 (27/03/2018)
## Bugfix
- Fix to avoid a null value #58 

## Improvement
- Return empty Map when its result cells is empty #71 

https://github.com/tkrs/orcus/compare/v0.11.0...v0.12.0
---

## v0.11.0 (27/03/2018)
- Adopt iota #56 
- Update to cats 1.0.0 #47 

https://github.com/tkrs/orcus/compare/v0.10.2...v0.11.0
---

## v0.10.2 (26/12/2017)
- Add Decoder[Map[String, A]] #52
---

## v0.10.1 (25/12/2017)
- Fixed decoding Failure in ValueCodec #50
---

## v0.10.0 (25/12/2017)
- Make to creation the nullable column #49
---

## v0.9.0 (25/12/2017)
- Add timestamp to PutEncoder/PutFamilyEncoder arguments #36
- Replace Encoder result with Put from Option[Put] #37
- Fix incorrect handling of the Option[A] instance #41 
- Fix problems that do not catch NPE #42 
- 
---

## v0.8.0 (21/12/2017)
- Add PutEncoder of a Map #34 
---

## v0.7.0 (21/12/2017)
- Derive `Put` from`A` automatically #33
---

## v0.6.0 (21/12/2017)
- More FP friendly methods: #29 and #30
- Refactor row builders with Reader Monad #31 

---

## v0.5.0 (21/12/2017)
- Add FP friendly methods: #28
---

## v0.4.2 (21/12/2017)
Fix an incorrect decoding handling. #22, #26 
---

## v0.4.1 (13/12/2017)
Fixes release for v0.4.0
---

## v0.1.0 (13/12/2017)
First release!
---

## v0.2.0 (13/12/2017)
Supports following operations:

- Result#getRow
- Result#rawCells
- Result#getColumnCells
- Result#getColumnLatestCell
- Result#getFamilyMap

---

## v0.3.0 (13/12/2017)
`Codec`'s type-classes derivation will be able to automatically with Shapeless.
---

## v0.4.0 (13/12/2017)
**DEPRECATED** This release was failed...
