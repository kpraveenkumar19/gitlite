---
id: 4-object-header
title: 4. Object Header
---

Each object entry contains a type/size varint header. Non-delta types are commit(1), tree(2), blob(3), tag(4). Delta types are ofs-delta(6) and ref-delta(7).


