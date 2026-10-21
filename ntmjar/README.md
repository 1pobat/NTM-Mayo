The original user-supplied NTM NEXT binary is present as ntm-next.jar.
It was reconstructed byte-for-byte from three uploaded parts, without modifying
or rebuilding the original mod.

Verified properties:
- 63534692 bytes; ZIP CRC checks pass; 6441 classes.
- Implementation-Version: 1.0.3+1b8c6c2d; mod ID hbm.
- SHA256: 923a0a2bea1deded10a71cc7273cb687d728b27fe1c2ba06774150f09c28a273

The dependency path is <project root>/ntmjar/ntm-next.jar, relative to the
build.gradle.kts directory. compileOnly uses this file; the addon does not embed it.
The runtimeOnly declaration makes it available for development runs.

The binary is ignored by Git. The prepared GitHub workflow requires an artifact
named ntm-next-local-dependency containing ntm-next.jar, supplied by the repository
owner in the same repository. It never downloads or rebuilds NTM from source.
