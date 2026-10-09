# Cuicatl local retention and sanitization policy

Date: 2026-10-09

## Default

Saved measurement sessions remain app-private until the user explicitly deletes them. Sharing/exporting does not delete the source session by default.

## Current controls

### Delete local session

Deletes one saved session's app-private measurement directory and matching cached CSV export(s). If the deleted session is the provenance source for the active reference adjustment, that adjustment is cleared.

Copies already shared, downloaded, mailed, backed up, or copied by another application are outside Cuicatl's control and are not deleted.

### Sanitize saved sessions

Deletes all sessions whose durable state is `SAVED`, clears Cuicatl's cached CSV exports, and clears the active reference adjustment. An active `STARTING`, `RUNNING`, or `FINALIZING` session is not deleted.

Both destructive actions require explicit confirmation.

## Meaning of sanitize

Cuicatl uses **logical deletion**: files under the application's private session/export storage are removed through the filesystem. The app does not claim forensic secure erasure of flash storage; wear leveling, filesystem behavior, device backups, or external copies may retain data outside the application's control.

## Future Share / Email option

Android's generic share flow (`ACTION_SEND`) does not reliably report that an email was delivered, an upload completed, or the receiving application permanently stored the attachment. Therefore Cuicatl must not implement "delete after successful email/upload" by assuming that returning from the chooser means success.

The safe future UX is:

- default: **Keep local session**;
- optional: **Prompt to delete local session after returning from Share**;
- when enabled, returning to Cuicatl opens an explicit confirmation dialog;
- deletion occurs only after that confirmation;
- no automatic deletion occurs merely because the external share target was opened.

A provider-specific upload integration that can return a trustworthy completion result may define a stronger delete-after-confirmed-upload workflow later, separately from generic Android sharing.

## Reference-adjustment provenance

Deleting the saved session used to derive the active reference adjustment removes that active adjustment as well. This avoids retaining a correction whose local provenance record has been intentionally sanitized.
