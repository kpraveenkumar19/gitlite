---
id: 1-discovering-references
title: 1. Discovering References
---

Clients start by requesting `info/refs?service=git-upload-pack`.

The server responds with pkt-lines that include available refs and capabilities. The first data line usually contains a NUL-separated capability list.

Example request:

```text
GET /info/refs?service=git-upload-pack HTTP/1.1
```

Client parses pkt-lines into a map from ref names to commit IDs.


