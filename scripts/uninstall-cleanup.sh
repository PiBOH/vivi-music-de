#!/bin/sh
# Axolotl Music — uninstall cleanup for Linux/macOS.
#
# On uninstall, the app's user data directory (~/.vivimusic) is a mix of real
# user data (settings, playlists, imported fonts), the backup history
# (backups/*.vivide.lbkp, plus legacy *.vivide.bkp / *.vivide.backup) and disposable caches
# (downloaded updates, audio/video/canvas/lyrics caches, logs, extracted helper
# libraries, artwork). This script keeps exactly two things - the NEWEST full
# backup (a restore point with settings and playlists) and device-sync.json -
# and deletes everything else, so a reinstall starts from a clean slate without
# losing settings/playlists. Section exports (*.vivide.sbkp, *.vivide.sections, *.pxpl) are not
# restore points and are removed with the rest.
#
# Usage:
#   uninstall-cleanup.sh                 # uses $HOME/.vivimusic
#   uninstall-cleanup.sh /home/alice     # uses /home/alice/.vivimusic
#
# What survives an uninstall:
#   <home>/.vivimusic/device-sync.json                          (settings + sync state)
#   <home>/.vivimusic/backups/<newest>.vivide.lbkp|.vivide.bkp|.vivide.backup
# Every other file - older backups included - and every cache are removed.

set -u

cleanup_dir() {
  HOME_DIR="$1"
  AXOLOTL_DIR="$HOME_DIR/.vivimusic"

  if [ ! -d "$AXOLOTL_DIR" ]; then
    echo "Axolotl Music: no user data at $AXOLOTL_DIR — nothing to clean."
    return 0
  fi

  BACKUPS_DIR="$AXOLOTL_DIR/backups"

  # The newest full backup wins across the extensions: `ls -1t` sorts by
  # modification time, so no filename parsing is needed (auto_backup_* and
  # vivimusic-de_* both carry their timestamp as the last 15 characters before
  # the extension). A section export is never a candidate.
  KEEP_BACKUP=""
  if [ -d "$BACKUPS_DIR" ]; then
    KEEP_BACKUP="$(ls -1t "$BACKUPS_DIR"/*.vivide.lbkp "$BACKUPS_DIR"/*.vivide.bkp "$BACKUPS_DIR"/*.vivide.backup 2>/dev/null | head -n 1)"
    if [ -n "$KEEP_BACKUP" ]; then
      echo "Axolotl Music: keeping the newest backup: $(basename "$KEEP_BACKUP")"
    fi
  fi

  # Delete everything under ~/.vivimusic except device-sync.json and backups/.
  for entry in "$AXOLOTL_DIR"/* "$AXOLOTL_DIR"/.[!.]*; do
    [ -e "$entry" ] || continue
    case "$(basename "$entry")" in
      backups|device-sync.json) continue ;;
    esac
    rm -rf "$entry"
    echo "Axolotl Music: removed $entry"
  done

  # Inside backups/, keep only the newest backup.
  if [ -d "$BACKUPS_DIR" ]; then
    for entry in "$BACKUPS_DIR"/* "$BACKUPS_DIR"/.[!.]*; do
      [ -e "$entry" ] || continue
      [ "$entry" = "$KEEP_BACKUP" ] && continue
      rm -rf "$entry"
    done
  fi

  echo "Axolotl Music: uninstall cleanup complete. Only the newest backup and device-sync.json remain."
}

# Clean the invoking user, plus every other home directory when running as
# root (dpkg postrm / AUR post_remove run as root on multi-user systems).
if [ "$(id -u 2>/dev/null)" = "0" ]; then
  for h in /home/*; do
    [ -d "$h" ] && cleanup_dir "$h"
  done
else
  cleanup_dir "${1:-$HOME}"
fi