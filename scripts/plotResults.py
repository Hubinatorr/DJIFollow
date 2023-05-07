import json
from pathlib import Path
from plotly.subplots import make_subplots
import plotly.graph_objects as go
import numpy as np

follower_data = json.load(open(Path(__file__).parent / "resultData/full/1505.json", "r"))
target_data = json.load(open(Path(__file__).parent / "testData/full.json", "r"))


def add_trace(fig, x, y, row, col, name):
    fig.add_trace(go.Scatter(x=x, y=y, mode='lines+markers', name=name), row=row, col=col)


# tests = [
#     "normal"
# ]


fig = make_subplots(rows=1, cols=1, subplot_titles=("x", "y"))
target = [p for p in follower_data]
follower = [p for p in target_data]
add_trace(fig, [p["t"] - target[0]["t"] for p in target], [p["x"] - target[0]["x"] for p in target], 1, 1, "targetX")
add_trace(fig, [p["t"] - follower[0]["t"] for p in follower], [p["x"] - follower[0]["x"] for p in follower], 1, 1, "followerX")


print(len(target))
print(len(follower))

fig.show()
