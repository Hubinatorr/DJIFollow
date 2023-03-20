import json
from pathlib import Path

import plotly.graph_objects as go
import numpy as np


# Create figure
fig = go.Figure()
path = "resultData/normal.json"
follower = json.load(open(Path(__file__).parent / "resultData/outside.json", "r"))
target = json.load(open(Path(__file__).parent / "resultData/outt.json", "r"))


# Create traces
fig.add_trace(go.Scatter(x=[p["t"] for p in follower], y=[p["y"] for p in follower],
                         mode='lines+markers',
                         name='follower'))
fig.add_trace(go.Scatter(x=[p["t"] for p in target], y=[p["y"] for p in target],
                         mode='lines+markers',
                         name='target'))
fig.show()


# Make 10th trace visible
# fig.data[10].visible = True

# # Create and add slider
# steps = []
# for i in range(len(fig.data)):
#     step = dict(
#         method="update",
#         args=[{"visible": [False] * len(fig.data)},
#               {"title": "Slider switched to step: " + str(i)}],  # layout attribute
#     )
#     step["args"][0]["visible"][i] = True  # Toggle i'th trace to "visible"
#     steps.append(step)
#
# sliders = [dict(
#     active=10,
#     currentvalue={"prefix": "Frequency: "},
#     pad={"t": 50},
#     steps=steps
# )]
#
# fig.update_layout(
#     sliders=sliders
# )

fig.show()