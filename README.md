## GitLite

Minimal Git client implemented in Java. GitLite speaks the Smart HTTP protocol, negotiates and downloads Git packfiles, parses objects (with REF_DELTA support), writes them to a `.git` object store, and checks out a working tree — all without shelling out to the `git` binary.

### Features

- **Clone over Smart HTTP**: Uses `info/refs?service=git-upload-pack` and `git-upload-pack` with `side-band-64k`.
- **Packfile parsing**: Reads PACK streams and materializes objects locally.
- **Delta support**: Handles REF_DELTA objects; OFS_DELTA is not supported yet.
- **Object store writing**: Stores objects as loose objects under `.git/objects/`.
- **Refs and HEAD**: Writes fetched refs and a direct `HEAD` to the target commit.
- **Working tree checkout**: Reconstructs files from tree/blob objects into the destination directory.
- **Simple CLI**: `clone <repo-url>`.

### Installation

You can install GitLite via Homebrew or build from source.

#### Option 1: Homebrew

```bash
brew tap kpraveenkumar19/gl
brew install gitlite
clone <github repo url>
```

#### Option 2: Build from source

Requirements:
- Java 17+
- Maven 3.8+

Steps:
```bash
git clone <github repo url>
cd GitLite
mvn -q -DskipTests package
./your_program.sh clone <github repo url>
```

### Usage

GitLite currently implements a single command: `clone`.

```bash
# If installed via Homebrew
clone <github repo url>

# From source (script)
./your_program.sh clone <github repo url>
```

Behavior and defaults:
- **Destination**: Clones into `~/Downloads/<repo-name>`.
- **Protocols**: HTTP/HTTPS Smart HTTP endpoints.
- **HEAD**: Written as a direct commit hash (not a symbolic ref).

Limitations (current):
- OFS_DELTA pack entries are not supported.
- No authentication (public repositories only).
- No shallow clones, branches/checkout commands, push, or fetch updates beyond initial clone.

### Technologies Used

- Java 17 (standard library)
- Maven (build)
- `java.net.http.HttpClient` (Smart HTTP)
- zlib inflate/deflate (`Inflater`, `DeflaterOutputStream`)
- SHA-1 hashing (`MessageDigest`)

### Project Structure

```text
src/main/java/Main.java   # CLI entrypoint and Git protocol/pack logic
pom.xml                   # Maven configuration
your_program.sh           # Build-and-run helper script
```

### Contributing

Contributions are welcome!

1. Fork the repository and create your feature branch:
   ```bash
   git checkout -b feature/your-feature
   ```
2. Build locally:
   ```bash
   mvn -q -DskipTests package
   ```
3. Commit your changes with a clear message:
   ```bash
   git commit -m "feat: add <short-description>"
   ```
4. Push the branch and open a Pull Request.

Development tips:
- Prefer Java 17+.
- Keep code readable and well-factored; avoid unnecessary complexity.

### License

This repository does not currently include a license file. If you plan to use this project beyond local experimentation, please open an issue to discuss licensing or submit a PR adding a `LICENSE` file (e.g., MIT/Apache-2.0).
 
### Documentation

Published docs (GitHub Pages): when deployed, they will be available at `https://kpraveenkumar19.github.io/gitlite/docs/git-protocol/intro`.



