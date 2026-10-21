# Third-party components

- NTM NEXT is a required external dependency, supplied by the user. Its JAR is
  not modified or bundled into the addon JAR. Upstream:
  https://github.com/Warfactory-Official/ntm-next . Its own licensing and notices apply.
- The Gradle wrapper JAR and launch scripts come from the official Gradle
  repository tag v9.2.1, https://github.com/gradle/gradle/tree/v9.2.1 . Original
  Apache-2.0 headers remain in the scripts; the JAR contains its LICENSE.
  Wrapper JAR SHA-256: 423cb469ccc0ecc31f0e4e1c309976198ccb734cdcbb7029d4bda0f18f57e8d9.
- NeoForge, Minecraft, Mixin and MixinExtras are resolved as development/runtime
  dependencies and retain their respective licenses. They are not shaded.
- Addon textures and annihilation audio are original. No NTM code or assets are
  copied into the addon's sources/resources. API names and binary descriptors
  were inspected for interoperability.

HBM Mayonnaise is an independent addon, not an official release of NTM NEXT.
