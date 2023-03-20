import json
from pathlib import Path
from plotly.subplots import make_subplots
import plotly.graph_objects as go
import numpy as np

follower_data = json.load(open(Path(__file__).parent / "resultData/test_2.0_1.47.json", "r"))

def add_trace(fig, x, y, row, col, name):
    fig.add_trace(go.Scatter(x=x, y=y, mode='lines+markers', name=name), row=row, col=col)


def create_plot(tests):
    for i, test in enumerate(tests):
        fig = make_subplots(rows=2, cols=1, subplot_titles=(test+ "x", test+ "y"))
        target = json.load(open(Path(__file__).parent / "testData/{}.json".format(test), "r"))
        follower = [p for p in follower_data if p["id"] == test]
        add_trace(fig, [p["t"] for p in target],    [p["x"] for p in target],   1, 1, "targetX")
        add_trace(fig, [p["t"] for p in follower],  [p["x"] for p in follower], 1, 1, "followerX")

        add_trace(fig, [p["t"] for p in target],    [p["y"] for p in target],   2, 1, "targetY")
        add_trace(fig, [p["t"] for p in follower],  [p["y"] for p in follower], 2, 1, "followerY")
        fig.show()

tests = [
    "circles",
    "changeSpeed",
    "full",
    "normal"
]

create_plot(tests)



