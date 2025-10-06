<h1>
  <img src="assets/images/icon.png" alt="Gitlite icon" width="38" height="38" style="vertical-align: -0.25em; margin-right: 8px;" />
  GitLite
</h1>

GitLite is a simple tool to download a Git repository with single command. It focuses on a implementation of git client over Smart HTTP in Java. It’s simple, fast and aimed at basic use. It uses Git’s Smart HTTP protocol along with Git object handling under the hood and writes files directly without any ZIP downloads or extraction. 

### Table of Contents
- [Installation](#installation)
- [Usage](#usage)
- [Resources](#resources)
- [Contributing](#contributing)

### Installation

Install via Homebrew:
```bash
brew tap kpraveenkumar19/gl
brew install gitlite
```

### Usage

GitLite implements a single command: `clone`.

```bash
# If installed via Homebrew (global command)
clone https://github.com/octocat/Hello-World

# Result:
# - Repository folder: ~/Downloads/Hello-World
```

### Resources
- [Git Objects](https://git-scm.com/book/en/v2/Git-Internals-Git-Objects)
- [Git Protocol (blog post)](https://i27ae15.github.io/git-protocol-doc/docs/git-protocol/intro)
- [Git HTTP protocol](https://git-scm.com/docs/http-protocol)

### Contributing

Contributions are welcome! To propose changes:

1. Fork the repository and create a feature branch
2. Make your changes
3. Open a Pull Request with a clear description and examples


