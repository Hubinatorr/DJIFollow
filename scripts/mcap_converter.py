from pathlib import Path
from time import time_ns
import json
import sys

from mcap.writer import Writer

files = ["normalSim1", "normalSim2", "normalSim3", "normalTest1", "normalTest2"]

with open('out.mcap', "wb") as f:
    w = Writer(f)
    w.start("x-jsonschema", library="my-excellent-library")

    with open(Path(__file__).parent / "schemas/PosesInFrame.json", "rb") as f:
        schema = f.read()

    schema_id = w.register_schema(
        name="foxglove.PosesInFrame",
        encoding="jsonschema",
        data=schema,
    )

    with open(Path(__file__).parent / "schemas/FrameTransform.json", "rb") as f:
        schema = f.read()

    frame_schema_id = w.register_schema(
        name="foxglove.FrameTransform",
        encoding="jsonschema",
        data=schema,
    )

    frame_channel_id = w.register_channel(
        topic="path",
        message_encoding="json",
        schema_id=schema_id,
    )

    for i, file in files:
        channel_id = w.register_channel(
            topic=file,
            message_encoding="json",
            schema_id=schema_id,
        )
        path = "data/" + file + ".json"
        data = json.load(open(Path(__file__).parent / path, "rb"))
        poses = []

        for pos in data:
            poses.append({
                "position": {"x": float(pos['x']), "y": float(pos['y']), "z": float(pos['z'])},
                "orientation": {"x": 0.0, "y": 0.0, "z": 0.0, "w": 0.0},
            })
            m = {
                "timestamp": {
                    "sec": int(pos["t"] / 1000),
                    "nsec": int(pos["t"] * 1e+6),
                },
                "frame_id": file,
                "poses": poses
            }
            w.add_message(
                channel_id,
                log_time=int(pos["t"] * 1e+6),
                data=json.dumps(m).encode("utf-8"),
                publish_time=int(pos["t"] * 1e+6),
            )
    w.finish()


