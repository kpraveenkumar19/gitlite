---
id: intro
title: GIT-Protocol
---

This section documents how the Git Smart HTTP protocol works and the steps needed to implement a minimal client, following ideas similar to Codecrafters.

Some links that you might find helpful:

- https://www.git-scm.com/docs/http-protocol
- https://github.com/git/git/blob/795ea8776befc95ea2becd8020c7a284677b4161/Documentation/gitprotocol-pack.txt
- https://github.com/git/git/blob/795ea8776befc95ea2becd8020c7a284677b4161/Documentation/gitformat-pack.txt
- https://codewords.recurse.com/issues/three/unpacking-git-packfiles
- https://medium.com/@concertdaw/sneaky-git-number-encoding-ddcc5db5329f
- https://stackoverflow.com/questions/68062812/what-does-the-git-smart-https-protocol-fully-look-like-in-all-its-glory

See the next pages for details on reference discovery, upload-pack, and pack parsing.


