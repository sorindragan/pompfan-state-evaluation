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
display(all_results[:4])

# %%
df = pd.json_normalize(all_results)
df["no_agreement"] = df.utility == 0.0
df["vs"] = None
df["vs_utility"] = None
for index, df_subset in df.groupby(["session", "tournamentStart", "sessionStart"]):
    party1, party2 = df_subset["party"]
    util1, util2 = df_subset["utility"]
    df.loc[(df.party == party1) & (df.session == index[0]) & (df.tournamentStart == index[1]) &
           (df.sessionStart == index[2]), "vs"] = party2
    df.loc[(df.party == party2) & (df.session == index[0]) & (df.tournamentStart == index[1]) &
           (df.sessionStart == index[2]), "vs"] = party1
    df.loc[(df.party == party1) & (df.session == index[0]) & (df.tournamentStart == index[1]) &
           (df.sessionStart == index[2]), "vs_utility"] = util2
    df.loc[(df.party == party2) & (df.session == index[0]) & (df.tournamentStart == index[1]) &
           (df.sessionStart == index[2]), "vs_utility"] = util1

df.to_csv(file_to_analyse.parent / "data.csv")
df
# %%
fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(15, 12))
df_subset = df[df.party == "POMPFANAgent"]
ax1 = sns.boxplot(data=df_subset, x="pwp.party.parameters.numParticlesPerOpponent", y="utility", ax=ax1)
for tick in ax1.get_xticklabels():
    tick.set_rotation(45)
ax1.set_xlabel("NumParticlesPerOpponent")
ax1.set_ylabel("Utility")
ax2 = sns.boxplot(data=df_subset, x="vs", y="utility", hue="pwp.party.parameters.numParticlesPerOpponent", ax=ax2)
for tick in ax2.get_xticklabels():
    tick.set_rotation(45)
ax2.set_xlabel("Opponents per NumParticlesPerOpponent")
ax2.set_ylabel("Utility")
plt.show()

# %%
fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(10, 7))
df_subset = df[df.party == "POMPFANAgent"]
ax1 = sns.barplot(data=df_subset, x="pwp.party.parameters.numParticlesPerOpponent", y="utility", ax=ax1, estimator=sum)
for tick in ax1.get_xticklabels():
    tick.set_rotation(45)
ax1.set_xlabel("NumParticlesPerOpponent")
ax1.set_ylabel("Utility")
ax1.set_title("Sum of Utility of POMPFAN accross NumParticlesPerOpponent")

ax2 = sns.barplot(data=df_subset, x="pwp.party.parameters.numParticlesPerOpponent", y="no_agreement", ci=None, ax=ax2)
for tick in ax2.get_xticklabels():
    tick.set_rotation(45)
ax2.set_xlabel("NumParticlesPerOpponent")
ax2.set_ylabel("Percentage")
ax2.set_title("Average non-Agreements of POMPFAN accross NumParticlesPerOpponent")
fig.tight_layout()
plt.show()

# %%
fig, ax = plt.subplots(1, 1, figsize=(10, 10))
df_no_aggreement_counts = df[df["no_agreement"]].groupby("pwp.party.parameters.numParticlesPerOpponent").count()
unique_agents = df_no_aggreement_counts.index.unique()
num_agents = len(unique_agents)
(ax, ) = df_no_aggreement_counts["session"].plot.pie(
    subplots=True,
    ax=ax,
    autopct="%.3f%%",
    explode=[0.02] * num_agents,
    labels=unique_agents,
    # pctdistance=0.5,
)
ax.set_ylabel("")
fig.suptitle("Number of non-Agreements of POMPFAN across NumParticlesPerOpponent")
fig.tight_layout()
plt.show()

# %%
