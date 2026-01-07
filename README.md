# Idx Mail Tool

(!) Incubation project - not yet finished.

## Description

I got sick and tired of setting up mail rules in different e-mail clients (mobile, Linux, Windows),
so I decided to create a tiny little tool that helps me organize my email.

Features:

- Listing folders and mails of an IMAP account
- Defining retention policies for IMAP folders (after which time messages should be deleted in a folder)
- Defining rules to sort incoming messages into folders, and apply these rules.

## Usage

The generated shadow JAR file is an executable, self-contained Java command line tool, which you can run with:

```bash
java -jar target/idx-mail-tool.jar
```

If you run it with an unknown command (e.g. `help`) or the incorrect number of arguments, it will reveal its usage
information:

```bash
java -jar idx-mail-tool.jar help

Usage:
java -jar idx-mail-tool.jar [command]

Commands:
- setup: Setup IMAP connector
- folders: List all folders
- mails: List all mails
- rules: List all rules
- apply: Apply all rules
```

## Configuration

The configuration is stored locally in the user home directory, as a YAML file.
Hint: you can set up multiple accounts, each with their dedicated settings.

Example:

```yaml
accounts:
  default:
    host: mail.somehost.org
    port: 993
    tls-enabled: true
    username: "user@somehost.org"
    password: "Secret#007"
```

## Build

To build this project with Maven (default tasks: _clean install_):

    mvn

The executable `idx-mail-tool.jar` can then be found in the `target` folder.

