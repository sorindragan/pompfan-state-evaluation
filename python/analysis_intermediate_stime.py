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
filename = "tournament_results_simTime.jsonl"
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
df["agreed"] = df.utility == 0.0
df["vs"] = None
df["vs_utility"] = None
for index, df_subset in df.groupby(["session", "tournamentStart", "sessionStart"]):
    # df_subset = df[df["session"] == i]
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
# for idx, grp in df.groupby("party"):
#     # display(idx)
#     # display(grp)
#     sns.boxplot(data=grp, x="party", y="util")
fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(15, 12))
df_subset = df[df.party == "POMPFANAgent"]
ax1 = sns.boxplot(data=df_subset, x="pwp.party.parameters.simulationTime", y="utility", ax=ax1)
for tick in ax1.get_xticklabels():
    tick.set_rotation(45)
ax1.set_xlabel("Simulation Time")
ax1.set_ylabel("Utility")
ax2 = sns.boxplot(data=df_subset, x="vs", y="utility", hue="pwp.party.parameters.simulationTime", ax=ax2)
for tick in ax2.get_xticklabels():
    tick.set_rotation(45)
ax2.set_xlabel("Opponents per Simulation Time")
ax2.set_ylabel("Percentage")
plt.show()
# fig.legend(loc="upper left")
# handles, labels = ax.get_legend_handles_labels()
# ax.legend(handles, labels, loc="best")
# %%
fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(10, 7))
df_subset = df[df.party == "POMPFANAgent"]
ax1 = sns.barplot(data=df_subset, x="pwp.party.parameters.simulationTime", y="utility", ax=ax1, estimator=sum)
for tick in ax1.get_xticklabels():
    tick.set_rotation(45)
ax1.set_xlabel("Simulation Time")
ax1.set_ylabel("Utility")
ax1.set_title("Sum of Utility of POMPFAN accross SimTime")

ax2 = sns.barplot(data=df_subset, x="pwp.party.parameters.simulationTime", y="agreed", ci=None, ax=ax2)
for tick in ax2.get_xticklabels():
    tick.set_rotation(45)
ax2.set_xlabel("Simulation Time")
ax2.set_ylabel("Utility")
ax2.set_title("Average non-Agreements of POMPFAN accross SimTime")
fig.tight_layout()
plt.show()

# %%
fig, ax = plt.subplots(1, 1, figsize=(10, 10))
df_no_aggreement_counts = df[df["agreed"]].groupby("pwp.party.parameters.simulationTime").count()
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
fig.suptitle("Number of non-Agreements of POMPFAN across SimTime")
fig.tight_layout()
plt.show()
