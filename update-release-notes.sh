#!/bin/bash
if [ "$VERSION" == "" ]; then
  echo "Expected env:VERSION"
  exit 2
fi
RELEASE_NOTES_SUBST_FILE="./RELEASE_NOTES_SUBST.md"
if [ "${1:-}" != "" ]; then
  RELEASE_NOTES_SUBST_FILE="$1"
fi
if [ ! -f "./RELEASE_NOTES.md" ]; then
  echo "Cannot find ./RELEASE_NOTES.md"
  exit 2
fi
envsubst < ./RELEASE_NOTES.md > "$RELEASE_NOTES_SUBST_FILE"
if [ ! -f "$RELEASE_NOTES_SUBST_FILE" ]; then
  echo "Expected $RELEASE_NOTES_SUBST_FILE"
exit 2
fi
if ! grep -q -F "{{VERSION}}" "$RELEASE_NOTES_SUBST_FILE"; then
  echo "VERSION replaced in $RELEASE_NOTES_SUBST_FILE"
else
  echo "Error: VERSION was not replaced with $VERSION"
  exit 1
fi
