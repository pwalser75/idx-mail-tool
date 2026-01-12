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

The configuration is stored locally in the user home directory, as a YAML file named `.idx-mail-tool.yaml`.

Root config element is `accounts`, with a map of an account name (key) and properties.

### Connection properties

- `host`: mail host name or IP
- `port`: mail host IMAP port
- `tls-enabled`: whether TLS (SSL) is enabled (default: true)
- `username`: IMAP account username
- `password`: IMAP account password
- `data-retention`: List of data retention settings
- `data-retention.[*].folder`: Folder name (or part, case-insensitive) for which the settings apply
- `data-retention.[*].retention-period`: For how long the messages in that folder should be retained (eligible for
  deletion if older), as days (d), hours (h), minutes (m) and seconds (s), e.g. _90d_ or _1d 12h 30m_
- `rules`: List of mail rules to apply
- `rules.[*].senders`: List of senders (or part, case-insensitive) to select the affected messages
- `rules.[*].action`: One of `MOVE` (move the message into another folder), `COPY` (copy the message into another
  folder) or `DELETE` (immediately delete the message)
- `rules.[*].folder`: Target folder name (or part, case-insensitive) for the `MOVE` and `COPY` actions

Example:

```yaml
accounts:
  default:
    host: mail.somehost.org
    port: 993
    tls-enabled: true
    username: "user@somehost.org"
    password: "Secret#007"
    data-retention:
      - folder: Sent
        retention-period: 3650d
      - folder: Spam
        retention-period: 21d
      - folder: Drafts
        retention-period: 10d
    rules:
      - senders: swica.ch, generali.com, mobiliar.ch
        action: MOVE
        folder: Insurance
      - senders:
          - digitec.ch
          - galaxus.ch
          - paypal.ch
          - migros.ch
        action: MOVE
        folder: Shopping
      - senders: opportunity@business-offer.com
        action: DELETE
```

The rules can be described with

```bash
java -jar target/idx-mail-tool.jar rules
```

Output:

```bash
Account: default
- Rules:
  - Mails from sender "swica.ch", "generali.com", "mobiliar.ch" will be moved to folder "Insurance"
  - Mails from sender "digitec.ch", "galaxus.ch", "paypal.ch", "migros.ch" will be moved to folder "Shopping"
  - Mails from sender "opportunity@business-offer.com" will be deleted
- Data retention rules:
  - Mails in folder "Sent" will be deleted after 3650d (any before 2016-01-15T13:44:57Z)
  - Mails in folder "Spam" will be deleted after 21d (any before 2025-12-22T13:44:57Z)
  - Mails in folder "Drafts" will be deleted after 10d (any before 2026-01-02T13:44:57Z)
```

To apply/execute the rules, execute:

```bash
java -jar target/idx-mail-tool.jar apply
```

## Build

To build this project with Maven (default tasks: _clean install_):

    mvn

The executable `idx-mail-tool.jar` can then be found in the `target` folder.

