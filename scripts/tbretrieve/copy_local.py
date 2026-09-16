#!/usr/bin/env python3
"""
copy_local.py

Copy the most recent ZIP archive from each deployment's local
download directory to its configured local copy destination.

Configuration is read from the same TOML file used by
totalbackup_retrieve.py.

Python 3.11+
"""

import argparse
import shutil
import sys
import tomllib
from dataclasses import dataclass
from pathlib import Path


@dataclass
class Deployment:
    name: str
    download_dir: Path
    local_copy_enabled: bool
    copy_to_dir: Path | None
    local_copy_retention: int


def load_config(config_path: Path) -> list[Deployment]:
    """Load deployment configuration from the TOML file."""

    with config_path.open("rb") as f:
        config = tomllib.load(f)

    deployments = config.get("deployments")
    if not deployments:
        raise ValueError("No [[deployments]] entries found in configuration.")

    result = []

    for deployment in deployments:
        name = deployment.get("name")
        download_dir = deployment.get("download_dir")

        if not name:
            raise ValueError("Each deployment must have a 'name'.")
        if not download_dir:
            raise ValueError(
                f"Deployment '{name}' must have a 'download_dir'."
            )

        local_copy_enabled = deployment.get("local_copy_enabled", False)
        copy_to_dir_value = deployment.get("copy_to_dir")
        local_copy_retention = deployment.get("local_copy_retention", 1)

        if local_copy_enabled:
            if not copy_to_dir_value:
                raise ValueError(
                    f"Deployment '{name}' has local_copy_enabled = true "
                    "but no 'copy_to_dir' was specified."
                )

            if local_copy_retention <= 0:
                raise ValueError(
                    f"Deployment '{name}' must have a "
                    "'local_copy_retention' greater than zero."
                )

        result.append(
            Deployment(
                name=name,
                download_dir=Path(download_dir).expanduser(),
                local_copy_enabled=local_copy_enabled,
                copy_to_dir=(
                    Path(copy_to_dir_value).expanduser()
                    if copy_to_dir_value
                    else None
                ),
                local_copy_retention=local_copy_retention,
            )
        )

    return result


def find_latest_zip(directory: Path) -> Path | None:
    """Return the most recently modified ZIP file in a directory."""

    if not directory.is_dir():
        raise FileNotFoundError(
            f"Download directory does not exist: {directory}"
        )

    zip_files = [
        path
        for path in directory.iterdir()
        if path.is_file() and path.suffix.lower() == ".zip"
    ]

    if not zip_files:
        return None

    return max(zip_files, key=lambda path: path.stat().st_mtime)


def copy_latest_archive(deployment: Deployment, dry_run: bool = False) -> bool:
    """Copy the newest ZIP from download_dir to copy_to_dir."""

    if not deployment.local_copy_enabled:
        print(
            f"[{deployment.name}] Local copy disabled; skipping."
        )
        return False

    if deployment.copy_to_dir is None:
        raise ValueError(
            f"Deployment '{deployment.name}' has local copy enabled "
            "but no copy destination is configured."
        )

    source = find_latest_zip(deployment.download_dir)

    if source is None:
        print(
            f"[{deployment.name}] No ZIP files found in "
            f"{deployment.download_dir}; skipping."
        )
        return False

    destination_dir = deployment.copy_to_dir
    destination = destination_dir / source.name

    print(
        f"[{deployment.name}] Copying:\n"
        f"    Source:      {source}\n"
        f"    Destination: {destination}"
    )

    if dry_run:
        return True

    destination_dir.mkdir(parents=True, exist_ok=True)

    # copy2 preserves the source file's metadata, including modification time.
    shutil.copy2(source, destination)

    print(f"[{deployment.name}] Copy complete.")

    return True


def cleanup_old_archives(
    directory: Path,
    num_to_retain: int,
    dry_run: bool = False,
) -> None:
    """Retain only the newest num_to_retain ZIP files."""

    if num_to_retain <= 0:
        raise ValueError("num_to_retain must be greater than zero.")

    if not directory.is_dir():
        return

    zip_files = sorted(
        (
            path
            for path in directory.iterdir()
            if path.is_file() and path.suffix.lower() == ".zip"
        ),
        key=lambda path: path.stat().st_mtime,
        reverse=True,
    )

    files_to_delete = zip_files[num_to_retain:]

    for path in files_to_delete:
        if dry_run:
            print(
                f"Would delete old archive: {path}"
            )
        else:
            print(
                f"Deleting old archive: {path}"
            )
            path.unlink()


def process_deployment(
    deployment: Deployment,
    dry_run: bool = False,
) -> None:
    """Process one deployment."""

    if not deployment.local_copy_enabled:
        print(
            f"[{deployment.name}] Local copy disabled; skipping."
        )
        return

    copied = copy_latest_archive(
        deployment,
        dry_run=dry_run,
    )

    if not copied:
        return

    assert deployment.copy_to_dir is not None

    cleanup_old_archives(
        deployment.copy_to_dir,
        deployment.local_copy_retention,
        dry_run=dry_run,
    )


def parse_args() -> argparse.Namespace:
    """Parse command-line arguments."""

    parser = argparse.ArgumentParser(
        description=(
            "Copy the most recent local ZIP archive for each enabled "
            "deployment to its configured local destination."
        )
    )

    parser.add_argument(
        "--config",
        required=True,
        type=Path,
        help="Path to the TOML configuration file.",
    )

    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show what would be copied/deleted without making changes.",
    )

    return parser.parse_args()


def main() -> int:
    """Program entry point."""

    args = parse_args()

    try:
        deployments = load_config(args.config)

        for deployment in deployments:
            try:
                process_deployment(
                    deployment,
                    dry_run=args.dry_run,
                )
            except Exception as exc:
                print(
                    f"[{deployment.name}] ERROR: {exc}",
                    file=sys.stderr,
                )

    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    sys.exit(main())