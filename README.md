# Whisper - Encrypted Chat App (Android)

## Overview
Whisper is a end-to-end encryption chat Application. 
All messages will be encypted on the sender's device and decrypted on the recipient's.
The server will be a relay and cant see plaintext.

## Features
- Register/login
- Device keypair in Android keystore (public/private key)
- Secure conversation 
- only cyfertext stored in server

## Security
- HTTPS/WSS
- No plaintext exchange

## Repository Structure 
.
├── .git
│   ├── (lot of files we dont worry about)
│   
│   
├── .github
│   ├── CODEOWNERS
│   ├── ISSUE_TEMPLATE.md
│   ├── PULL_REQUEST_TEMPLATE.md
│   └── workflows
│       └── android-ci.yml
├── .gitignore
├── CHANGELOG.md
├── CONTRIBUTING.md
├── README.md
└── docs
    ├── SRS.md
    ├── diagrams
    └── security-testing.md

