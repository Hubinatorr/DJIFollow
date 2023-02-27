from pathlib import Path
from time import time_ns
import json
import sys

from mcap.writer import Writer

with open('temp.mcap', "wb") as f:
    w = Writer(f)
    w.start("x-jsonschema", library="my-excellent-library")

    with open(Path(__file__).parent / "data/PosesInFrame.json", "rb") as f:
        schema = f.read()

    schema_id = w.register_schema(
        name="foxglove.PosesInFrame",
        encoding="jsonschema",
        data=schema,
    )

    channel_id = w.register_channel(
        topic="pathF",
        message_encoding="json",
        schema_id=schema_id,
    )

    data = json.load(open(Path(__file__).parent / "data/follower.json", "rb"))
    poses = []

    for pos in data:
        poses.append({
            "position": {"x": float(pos['x']), "y": float(pos['y']), "z": float(pos['z'])},
            "orientation": {"x": 0.0, "y": 0.0, "z": 0.0, "w": 0.0},
        })
        m = {
            "timestamp": {
                "sec": int(pos["Timestamp"] / 1000),
                "nsec": int(pos["Timestamp"] * 1e+6),
            },
            "frame_id": "follower",
            "poses": poses
        }
        w.add_message(
            channel_id,
            log_time=int(pos["Timestamp"] * 1e+6),
            data=json.dumps(m).encode("utf-8"),
            publish_time=int(pos["Timestamp"] * 1e+6),
        )
    data = json.load(open(Path(__file__).parent / "data/target.json", "rb"))
    poses = []

    schema_id = w.register_schema(
        name="foxglove.PosesInFrame",
        encoding="jsonschema",
        data=schema,
    )

    channel_id = w.register_channel(
        topic="pathT",
        message_encoding="json",
        schema_id=schema_id,
    )

    for pos in data:
        poses.append({
            "position": {"x": float(pos['x']), "y": float(pos['y']), "z": float(pos['z'])},
            "orientation": {"x": 0.0, "y": 0.0, "z": 0.0, "w": 0.0},
        })
        m = {
            "timestamp": {
                "sec": int(pos["Timestamp"] / 1000),
                "nsec": int(pos["Timestamp"] * 1e+6),
            },
            "frame_id": "target",
            "poses": poses
        }
        w.add_message(
            channel_id,
            log_time=int(pos["Timestamp"] * 1e+6),
            data=json.dumps(m).encode("utf-8"),
            publish_time=int(pos["Timestamp"] * 1e+6),
        )

    with open(Path(__file__).parent / "data/FrameTransform.json", "rb") as f:
        schema = f.read()

    schema_id = w.register_schema(
        name="foxglove.FrameTransform",
        encoding="jsonschema",
        data=schema,
    )

    channel_id = w.register_channel(
        topic="path",
        message_encoding="json",
        schema_id=schema_id,
    )

    m = {
        "timestamp": {
            "sec": int(0),
            "nsec": int(0),
        },
        "parent_frame_id": "target",
        "child_frame_id": "follower",
        "translation": {
            "x": 0.0,
            "y": 0.0,
            "z": 0.0,
        },
        "rotation": {
            "x": 0.0,
            "y": 0.0,
            "z": 0.0,
            "w": 0.0
        }
    }

    w.add_message(
        channel_id,
        log_time=int(0),
        data=json.dumps(m).encode("utf-8"),
        publish_time=int(0),
    )

    w.finish()


