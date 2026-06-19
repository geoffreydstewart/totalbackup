#!/usr/bin/env python3

"""
totalbackup_retrieve.py

Download the most recent .zip archive from one or more remote hosts using
SSH/SFTP and a TOML configuration file.

Requirements:
    Python 3.11+
    paramiko

Usage:
    python totalbackup_retrieve.py
    python totalbackup_retrieve.py --config myconfig.toml
    python totalbackup_retrieve.py --dry-run
"""

from __future__ import annotations

import argparse
import getpass
import os
import stat
import sys
import tomllib
import traceback
from dataclasses import dataclass
from pathlib import Path

import paramiko


# ---------------------------------------------------------------------------
# Models
# ---------------------------------------------------------------------------


@dataclass
class SSHConfig:
    username: str | None
    private_key_path: str
    host: str | None
    port: int
    key_passphrase_env: str | None
    known_hosts: str | None


@dataclass
class Deployment:
    name: str
    remote_dir: str
    download_dir: str
    username: str | None
    host: str | None
    port: int | None


# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------


def expand_path(path: str) -> str:
    return str(Path(path).expanduser())


def load_config(config_path: str) -> tuple[SSHConfig, list[Deployment]]:
    with open(config_path, "rb") as f:
        data = tomllib.load(f)

    ssh_section = data.get("ssh", {})

    ssh_config = SSHConfig(
        username=ssh_section.get("username"),
        private_key_path=expand_path(ssh_section["private_key_path"]),
        host=ssh_section.get("host"),
        port=ssh_section.get("port", 22),
        key_passphrase_env=ssh_section.get("key_passphrase_env"),
        known_hosts=expand_path(ssh_section["known_hosts"])
        if ssh_section.get("known_hosts")
        else None,
    )

    deployments = []

    for item in data.get("deployments", []):
        deployments.append(
            Deployment(
                name=item["name"],
                remote_dir=item["remote_dir"],
                download_dir=expand_path(item["download_dir"]),
                username=item.get("username"),
                host=item.get("host"),
                port=item.get("port"),
            )
        )

    if not deployments:
        raise ValueError("No deployments defined in configuration.")

    return ssh_config, deployments


# ---------------------------------------------------------------------------
# SSH Helpers
# ---------------------------------------------------------------------------


def load_private_key(
    private_key_path: str,
    passphrase: str | None,
) -> paramiko.PKey:
    loaders = (
        # paramiko.Ed25519Key,
        paramiko.RSAKey,
        # paramiko.ECDSAKey,
    )

    last_error = None

    for loader in loaders:
        try:
            return loader.from_private_key_file(
                private_key_path,
                password=passphrase,
            )
        except Exception as exc:
            last_error = exc

    raise RuntimeError(
        f"Unable to load private key: {private_key_path}"
    ) from last_error


def determine_passphrase(
    ssh_config: SSHConfig,
    private_key_path: str,
) -> str | None:
    env_name = ssh_config.key_passphrase_env

    if env_name:
        value = os.environ.get(env_name)
        if value:
            return value

    try:
        load_private_key(private_key_path, None)
        return None
    except Exception:
        pass

    return getpass.getpass(
        prompt=f"Passphrase for {private_key_path}: "
    )


def create_client(
    host: str,
    username: str,
    port: int,
    private_key_path: str,
    passphrase: str | None,
    known_hosts: str | None,
) -> paramiko.SSHClient:
    client = paramiko.SSHClient()

    if known_hosts:
        client.load_host_keys(known_hosts)
        client.set_missing_host_key_policy(
            paramiko.RejectPolicy()
        )
    else:
        client.load_system_host_keys()
        client.set_missing_host_key_policy(
            paramiko.RejectPolicy()
        )

    pkey = load_private_key(private_key_path, passphrase)

    client.connect(
        hostname=host,
        port=port,
        username=username,
        pkey=pkey,
        look_for_keys=False,
        allow_agent=False,
        timeout=30,
    )

    return client


# ---------------------------------------------------------------------------
# Remote Operations
# ---------------------------------------------------------------------------


def find_latest_zip(
    sftp: paramiko.SFTPClient,
    remote_dir: str,
):
    entries = sftp.listdir_attr(remote_dir)

    zip_files = []

    for entry in entries:
        if not stat.S_ISREG(entry.st_mode):
            continue

        if not entry.filename.lower().endswith(".zip"):
            continue

        zip_files.append(entry)

    if not zip_files:
        return None

    return max(zip_files, key=lambda item: item.st_mtime)


def download_latest_archive(
    deployment: Deployment,
    ssh_config: SSHConfig,
    passphrase: str | None,
    dry_run: bool,
) -> tuple[bool, str]:
    username = deployment.username or ssh_config.username
    host = deployment.host or ssh_config.host

    if not username:
        raise ValueError(
            f"No username configured for deployment "
            f"{deployment.name}"
        )
    
    if not host:
        raise ValueError(
            f"No host configured for deployment "
            f"{deployment.name}"
        )

    port = deployment.port or ssh_config.port

    client = None

    try:
        client = create_client(
            host=host,
            username=username,
            port=port,
            private_key_path=ssh_config.private_key_path,
            passphrase=passphrase,
            known_hosts=ssh_config.known_hosts,
        )

        sftp = client.open_sftp()

        latest = find_latest_zip(
            sftp=sftp,
            remote_dir=deployment.remote_dir,
        )

        if latest is None:
            return True, "no zip files found"

        remote_file = (
            f"{deployment.remote_dir.rstrip('/')}/"
            f"{latest.filename}"
        )

        local_dir = Path(deployment.download_dir)
        local_dir.mkdir(parents=True, exist_ok=True)

        local_file = local_dir / latest.filename

        if dry_run:
            return (
                True,
                f"would download {remote_file}",
            )

        sftp.get(remote_file, str(local_file))

        return (
            True,
            f"downloaded {latest.filename}",
        )

    finally:
        if client:
            client.close()


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------


def parse_args():
    parser = argparse.ArgumentParser(
        description=(
            "Retrieve the latest TotalBackup zip archive "
            "from configured hosts."
        )
    )

    parser.add_argument(
        "--config",
        default="totalbackup_retrieve.toml",
        help="Path to TOML configuration file.",
    )

    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="List archives without downloading.",
    )

    return parser.parse_args()


def main() -> int:
    args = parse_args()

    try:
        ssh_config, deployments = load_config(args.config)
    except Exception as exc:
        print(
            f"ERROR loading config: {exc}",
            file=sys.stderr,
        )
        traceback.print_exc()
        return 1

    try:
        passphrase = determine_passphrase(
            ssh_config,
            ssh_config.private_key_path,
        )
    except Exception as exc:
        print(
            f"ERROR loading private key: {exc}",
            file=sys.stderr,
        )
        traceback.print_exc()
        return 1

    failures = 0

    for deployment in deployments:
        try:
            success, message = download_latest_archive(
                deployment=deployment,
                ssh_config=ssh_config,
                passphrase=passphrase,
                dry_run=args.dry_run,
            )

            print(f"{deployment.name}: {message}")

            if not success:
                failures += 1

        except Exception as exc:
            failures += 1
            print(
                f"{deployment.name}: ERROR: {exc}",
                file=sys.stderr,
            )
            traceback.print_exc()

    return 0 if failures == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
