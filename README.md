## GitLite

Minimal Git client implemented in Java. GitLite speaks the Smart HTTP protocol, negotiates and downloads Git packfiles, parses objects, writes them to a `.git` object store, and checks out a working tree — all without shelling out to the `git` binary.

### Table of Contents
- [Project Description](#project-description)
- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Resources](#resources)
- [Contributing](#contributing)

### Project Description
GitLite focuses on a lean, educational implementation of `git clone` over Smart HTTP:
- Discovers refs via `info/refs?service=git-upload-pack`
- Negotiates and fetches pack data (`side-band-64k`)
- Parses commit/tree/blob/tag objects (with REF_DELTA support)
- Writes loose objects under `.git/objects/`
- Reconstructs the working tree directly from tree/blob objects

### Features
- **Clone over Smart HTTP**: Uses `info/refs?service=git-upload-pack` and `git-upload-pack` with `side-band-64k`.
- **Packfile parsing**: Reads PACK streams and materializes objects locally.
- **Delta support**: Supports REF_DELTA objects; OFS_DELTA is not supported yet.
- **Object store writing**: Stores objects as loose objects under `.git/objects/`.
- **Refs and HEAD**: Writes fetched refs and a direct `HEAD` to the target commit.
- **Working tree checkout**: Reconstructs files from tree/blob objects into the destination directory.
- **Simple CLI**: `clone <repo-url>`.

### Installation

Install via Homebrew:
```bash
brew tap kpraveenkumar19/gl
brew install gitlite
```

Optional: Build from source (Java 17+, Maven 3.8+):
```bash
git clone <github repo url>
cd GitLite
mvn -q -DskipTests package
./your_program.sh clone <github repo url>
```

### Usage

GitLite currently implements a single command: `clone`.

```bash
# If installed via Homebrew (global command)
clone <github repo url>

# From source (helper script)
./your_program.sh clone <github repo url>
```

Defaults and behavior:
- **Destination directory**: `~/Downloads/<repo-name>`
- **Protocols**: HTTP/HTTPS (Smart HTTP)
- **HEAD**: Written as a direct commit hash (not a symbolic ref)

Current limitations:
- OFS_DELTA entries are not yet supported
- No authentication (public repositories only)
- No shallow clone, branch checkout, push, or incremental fetch

### Resources
- [Git Objects](https://git-scm.com/book/en/v2/Git-Internals-Git-Objects)
- [Git Protocol (blog post)](https://i27ae15.github.io/git-protocol-doc/docs/git-protocol/intro)
- [Git HTTP protocol](https://git-scm.com/docs/http-protocol)

### Contributing
Contributions are welcome!

1. Fork the repository and create a feature branch:
   ```bash
   git checkout -b feature/your-feature
   ```
2. Build locally:
   ```bash
   mvn -q -DskipTests package
   ```
3. Commit your changes with a clear message:
   ```bash
   git commit -m "Add <short-description>"
   ```
4. Push the branch and open a Pull Request.

Development tips:
- Target Java 17+
- Keep code readable and well-factored; avoid unnecessary complexity

