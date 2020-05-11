# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Changed
- Remove backwards compatibility hacks (drop support for everything older then
  SonarQube 7.8)
- Minor dependency updates
- Deprecates support for JaCoCo binary format (use the SonarQube JaCoCo plugin
  instead)

### Removed
- Compatibility with JaCoCo older the 0.7.5 (released in mid-2015)

## [1.6] - 2019-07-04

### Added
- This changelog

### Changed
- Moved project from SonarSource to community
- Update plugin to be compatible with SonarQube 6.7-7.8
- Analyze plugin on SonarCloud
- Do CI builds on Travis & AppVeyor
- Integrate codenarc converter into the default build
- Include CodeNarc as a git submodule (so the converter can run on Travis)
- Fix a bunch of deprecated constructs
- Only report overall JaCoCo test coverage
- Updated CodeNarc to 1.4 & GMetrics to 1.0

### Fixed
- fix multiple codenarc files (pmayweg#60)
- Fix bug in JaCoCo package name handling (pmayweg#74)
- Update JaCoCo for Java 10+ support
- Remove old metrics (fixes #6)

### Removed
- Coupling metrics, since SonarQube doesn't support them anymore

## [1.5] - 2017-05-10

Please see the Git history for older changes

## [1.4-RC1] - 2016-08-05

## [1.3.1] - 2015-12-02

## [1.3] - 2015-11-06

## [1.2] - 2015-08-12

## [1.1.1] - 2015-05-28

## [1.1] - 2015-03-17

## [1.0.1] - 2014-03-14

## [1.0] - 2014-02-24

## [0.6] - 2012-08-06

[Unreleased]: https://github.com/Inform-Software/sonar-groovy/compare/1.6...HEAD
[1.6]: https://github.com/Inform-Software/sonar-groovy/compare/1.5...1.6
[1.5]: https://github.com/Inform-Software/sonar-groovy/compare/1.4-RC1...1.5
[1.4-RC1]: https://github.com/Inform-Software/sonar-groovy/compare/1.3.1...1.4-RC1
[1.3.1]: https://github.com/Inform-Software/sonar-groovy/compare/1.3...1.3.1
[1.3]: https://github.com/Inform-Software/sonar-groovy/compare/1.2...1.3
[1.2]: https://github.com/Inform-Software/sonar-groovy/compare/1.1.1...1.2
[1.1.1]: https://github.com/Inform-Software/sonar-groovy/compare/1.1...1.1.1
[1.1]: https://github.com/Inform-Software/sonar-groovy/compare/1.0.1...1.1
[1.0.1]: https://github.com/Inform-Software/sonar-groovy/compare/1.0...1.0.1
[1.0]: https://github.com/Inform-Software/sonar-groovy/compare/0.6...1.0
[0.6]: https://github.com/Inform-Software/sonar-groovy/releases/tag/0.6

