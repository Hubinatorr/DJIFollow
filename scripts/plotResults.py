import json
from pathlib import Path
from plotly.subplots import make_subplots
import plotly.graph_objects as go
import numpy as np

follower_data = json.load(open(Path(__file__).parent / "resultData/kalman.json", "r"))
# follower_data = json.load(open(Path(__file__).parent / "resultData/testtan6.json", "r"))


def add_trace(fig, x, y, row, col, name):
    fig.add_trace(go.Scatter(x=x, y=y, mode='lines+markers', name=name), row=row, col=col)


def create_plot(tests):
    for i, test in enumerate(tests):
        fig = make_subplots(rows=2, cols=1, subplot_titles=( "x", "y"))
        target = [p for p in follower_data if p["id"] == "target"]
        follower = [p for p in follower_data if p["id"] == "kalman"]
        add_trace(fig, [p["t"] for p in target], [p["x"] for p in target], 1, 1, "targetX")
        add_trace(fig, [p["t"] for p in follower], [p["x"] for p in follower], 1, 1, "followerX")

        add_trace(fig, [p["t"] for p in target], [p["y"] for p in target], 2, 1, "targetY")
        add_trace(fig, [p["t"] for p in follower], [p["y"] for p in follower], 2, 1, "followerY")

        print(len(target))
        print(len(follower))

        fig.show()


# tests = [
#     "normal"
# ]
tests = [
    "full",
]

create_plot(tests)
