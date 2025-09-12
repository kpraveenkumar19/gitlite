---
id: 5-parsing-ref-delta
title: 5. Parsing REF_DELTA
---

REF_DELTA entries carry a 20-byte base object id followed by a deflated delta stream. To reconstruct, inflate the delta, then apply it to the base object's payload, re-materializing an object with the same type header as the base.


