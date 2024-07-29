# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Fixed
- Cell table highlighted when entering game
- Guardians not highlighted with point balance when level too high


## [1.2.0]
### Added
- Option to highlight guardians based on current points to keep points balanced
- Configurable guardian outlines
- Option to not highlight guardians that are out of level range
- Notify when barrier unlocks (tylanphear)
- Configurable quick pass delay (tylanphear)
- Highlight deposit pool when runes in inventory (JKLimov)
- Toggle for rune icons
- Save points and load on launch
### Changed
- Project formatting
- Updated build
- isInMinigame now calculated using varbit
### Fixed
- Canvas returned null for text location causing a null reference (robin239)

## [1.1.0]
- Upstream version