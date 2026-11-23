# AGENTS.md

Guidance for AI coding agents working in this repository.

## SDK engineering principles

This SDK follows the [Supabase SDK engineering principles](https://github.com/supabase/sdk/blob/main/docs/principles.md):
thin clients, logic and validation in the service, service-owned defaults, and
decoding that accepts new values. Read them before you write or review code.
Cite a principle by a link to its heading. The conventions in this repository
win for local matters such as commands, layout, and style.

## Where to look

- [README.md](README.md): modules, installation, and supported service versions
- [CONTRIBUTING.md](CONTRIBUTING.md): how to contribute, build, and run the tests
- [MIGRATION.md](MIGRATION.md): breaking changes between major versions
- [sdk-compliance.yaml](sdk-compliance.yaml): the capabilities this SDK declares
  against the [capability registry](https://github.com/supabase/sdk)
