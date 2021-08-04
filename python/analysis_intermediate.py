# %%
import enum
import io
import itertools as it
import json
import pathlib
import sys
from IPython.display import display
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns
import jsonlines
# %%
filename = "tournament_results_random.jsonl"
# filename = "log_tournament_xx_xx_xxxx_xx_xx.json" # Something else
curr_dir = pathlib.Path(__file__)
log_dir = curr_dir.parent.parent / "eval"
file_to_analyse = log_dir / filename
assert file_to_analyse.exists(), f"File {file_to_analyse} does not exist"
display(f"Found {file_to_analyse} !!!")
display(f"Start loading file...")
# %%
all_results = list(jsonlines.open(file_to_analyse))
# display(all_results[:4])

# %%
df = pd.json_normalize(all_results)
# this makes no sense
df["no_agreement"] = df.utility == 0.0
df["vs"] = None
df["vs_utility"] = None
for index, df_subset in df.groupby(["session", "tournamentStart", "sessionStart"]):
    party1, party2 = df_subset["party"]
    util1, util2 = df_subset["utility"]
    df.loc[(df.party == party1) & (df.session == index[0]) & (df.tournamentStart == index[1]) & (df.sessionStart == index[2]), "vs"] = party2
    df.loc[(df.party == party2) & (df.session == index[0]) & (df.tournamentStart == index[1]) & (df.sessionStart == index[2]), "vs"] = party1
    df.loc[(df.party == party1) & (df.session == index[0]) & (df.tournamentStart == index[1]) & (df.sessionStart == index[2]), "vs_utility"] = util2
    df.loc[(df.party == party2) & (df.session == index[0]) & (df.tournamentStart == index[1]) & (df.sessionStart == index[2]), "vs_utility"] = util1

df.to_csv(file_to_analyse.parent / "data.csv")
# %%

fig, ax = plt.subplots(1, 1, figsize=(10, 5))
ax = sns.boxplot(data=df[df.vs == "POMPFANAgent"], x="party", y="vs_utility")
for tick in ax.get_xticklabels():
    tick.set_rotation(45)
ax.set_xlabel("Party")
ax.set_ylabel("Utility")
plt.show()

# %%
fig, ax = plt.subplots(1, 1, figsize=(10, 10))
df_utility_sum = df.groupby("party").sum()
unique_agents = df_utility_sum.index.unique()
num_agents = len(unique_agents)
(ax, ) = df_utility_sum["utility"].plot.bar(
    subplots=True,
    ax=ax,
)
for tick in ax.get_xticklabels():
    tick.set_rotation(45)
ax.set_xlabel("Party")
ax.set_ylabel("Total Utility")
fig.suptitle("Utility")
fig.tight_layout()
plt.show()

# %%
fig, ax = plt.subplots(1, 1, figsize=(10, 10))
df_no_aggreement_counts = df[df["no_agreement"]].groupby("party").count()
unique_agents = df_no_aggreement_counts.index.unique()
num_agents = len(unique_agents)
(ax, ) = df_no_aggreement_counts["session"].plot.pie(
    subplots=True,
    ax=ax,
    autopct="%.3f%%",
    explode=[0.02] * num_agents,
    labels=unique_agents,
)
ax.set_ylabel("")
fig.suptitle("Number of non-Agreements")
fig.tight_layout()
plt.show()

# %%
interesting_cols = ["POMPFANAgent", "Hardliner"]
groups = df[df["party"].isin(interesting_cols)].groupby(["party", "no_agreement"]).count().reset_index("no_agreement")
num_pies = len(interesting_cols)
fig, axes = plt.subplots(1, num_pies, figsize=(5 * num_pies, 5))
cnt = 0
for idx, ax in zip(groups.index.unique(), axes):
    grp = groups[groups.index == idx]
    ax.pie(
        data=grp,
        x="session",
        autopct="%.3f%%",
        explode=[0.02] * len(interesting_cols),
        labels="no_agreement",
    )
    ax.set_xlabel(idx)
fig.suptitle(f"Number of non-Agreements")
fig.tight_layout()
plt.show()

# %%
