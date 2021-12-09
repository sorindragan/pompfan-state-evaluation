# %%
import enum
import io
import itertools as it
import json
import pathlib
import sys
from IPython.core.display import HTML, display_html
from IPython.display import display
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns
import jsonlines
import base64
import matplotlib.pyplot as plt
import matplotlib.animation as animation
import seaborn as sns
import uuid
import hashlib
STEP_PREFIX = 'update'
# %%
filename = "log_distribution_detailed.jsonl"
# filename = "log_tournament_xx_xx_xxxx_xx_xx.json" # Something else
curr_dir = pathlib.Path(__file__)
log_dir = curr_dir.parent.parent / "logs"
file_to_analyse = log_dir / filename
assert file_to_analyse.exists(), f"File {file_to_analyse} does not exist"
display(f"Found {file_to_analyse} !!!")
display(f"Start loading file...")
# %%
all_results = list(jsonlines.open(file_to_analyse))
# display(all_results[:4])
df = pd.json_normalize(all_results).T
# df["profile"] = df.index
# df = df.reset_index().rename(columns={"index":"profile"})
df.columns = [f"{STEP_PREFIX}_{col}" for col in df.columns]
df[['cls', 'profile']] = list(df.index.str.split('.', 1, expand=True))
df['profile_id'] = df['profile'].apply(lambda x: hashlib.md5(x.encode('utf-8')).hexdigest()[:8])  #.str.decode('utf-8')
df.index = df["cls"] + "_" + df["profile_id"]
df.head(3)
# %%


def generate_animate(df: pd.DataFrame, skip=5):
    candindate_cols = list(df.columns[df.columns.str.startswith(STEP_PREFIX)])
    candindate_cols = candindate_cols[::skip] + [candindate_cols[-1]]

    def animate(i: int):
        # print(i)
        # print(candindate_cols[i])
        # display()
        ax = plt.gca()
        ax.clear()
        x_data = df.index
        y_data = df[candindate_cols[i]]

        graph = sns.barplot(y=y_data, x=x_data, color="blue", ci=None)
        graph.set_xticklabels(graph.get_xticklabels(), rotation=45, horizontalalignment='right', fontsize='small')
        graph.set_ylim(0, 1)
        return graph

    return animate, len(candindate_cols)


fig = plt.figure(figsize=(15, 15))
# Don't skip frames/lines; 1 is actually not skipping shit
animate, num_frames = generate_animate(df, skip=1)
anim = animation.FuncAnimation(fig, animate, frames=num_frames, interval=1000, repeat=True, blit=False)
HTML(anim.to_jshtml())
# %%
last_col = list(df.columns[df.columns.str.startswith(STEP_PREFIX)])[-1]
most_likely = df[df[last_col] == df[last_col].max()]
id_, profl = most_likely.index[0], most_likely['profile'][0]
print(f"Most likely profile:\n{id_} => {profl}")
# %%
