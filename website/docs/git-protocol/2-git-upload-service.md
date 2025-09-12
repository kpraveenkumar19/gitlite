---
id: 2-git-upload-service
title: 2. Git-Upload Service
---

After refs are known, the client negotiates wants and sends a request body as pkt-lines to `/git-upload-pack`.

Capabilities like `side-band-64k` and `no-progress` are often requested for simpler parsing.

Example send:

```text
0032want <sha> side-band-64k no-progress\n
0000
0009done\n
```

The response is a side-banded stream where channel 1 carries the PACK data.


