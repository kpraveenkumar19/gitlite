---
id: 6-reading-pack-file
title: 6. Reading Pack File
---

Clients iterate objects based on the object count, inflating payloads. For REF_DELTA, apply delta using copy/insert opcodes. After reconstructing the full payload, prepend a `<type> <size>\0` header and compute SHA-1 to address objects in the store.


