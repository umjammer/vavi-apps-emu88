[![Release](https://jitpack.io/v/umjammer/vavi-apps-emu88.svg)](https://jitpack.io/#umjammer/vavi-apps-emu88)
[![Java CI](https://github.com/umjammer/vavi-apps-emu88/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-apps-emu88/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-apps-emu88/actions/workflows/codeql-analysys.yml/badge.svg)](https://github.com/umjammer/vavi-apps-emu88/actions/workflows/codeql-analysis.yml)
![Java](https://img.shields.io/badge/Java-17-b07219)

# vavi-apps-emu88

<img alt="logo" src="src/test/resources/logo.png" width="200" />&nbsp;&nbsp;<sub><a href="https://www.nec.com/">© NEC</a></sub>

PC-8801mkII emulator．

still wip. basic prompt is shown. 

## Install

 * https://jitpack.io/#umjammer/vavi-apps-emu88

## Usage

### roms

put 3 roms at

 - `src/main/resources/roms/romn88.bin`
 - `src/main/resources/roms/romn.bin`
 - `src/main/resources/roms/rom4th.bin`

### font

put font.rom at

 - `src/test/resources/font.rom`

put font at

- `src/main/resources/font2.png`

### run

```shell
$ mvn -P run package antrun:run@run
```

## References

* https://github.com/javaemus/consoleflex056
* https://github.com/mamedev/mame/tree/master/src/lib/formats
* https://archive.org/details/PCTechknow8801Vol.11982
* https://github.com/jnode/jnode (hardware codes inside, floppy, cdrom etc.)

## TODO

 * DMA optimization
 * DISK system
 * subsystem
 * CRTC
 * ⚠️ unit tests have random fixture problems, if it would be failed, rerun.
