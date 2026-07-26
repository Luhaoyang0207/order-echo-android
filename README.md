# OrderEcho

**餐厅通话录音管理** — a small, offline Android app for reviewing and safely retaining Huawei phone-call recordings.

## What it does

On the restaurant's Huawei BAC-AL00 running Android 8.0, the system phone app already records calls to:

```text
/storage/emulated/0/Sounds/Callrecord/
```

OrderEcho will scan those `.amr` files, show them grouped by month and date, play them inside the app, and remove recordings older than the selected retention period.

## What it does not do

- Record telephone calls
- Send files or data over the network
- Move or rename Huawei recordings
- Require root access

## Status

The repository currently contains the approved product design and handoff documentation. Android implementation has not started yet.

See [docs/HANDOFF.md](docs/HANDOFF.md) for the current goal and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the approved design.

