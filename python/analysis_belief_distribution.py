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
from numpy.core.fromnumeric import size
import pandas as pd
import seaborn as sns
import jsonlines
import base64
import matplotlib.pyplot as plt
import matplotlib.animation as animation
import seaborn as sns
import uuid
import hashlib
from scipy.interpolate import make_interp_spline, BSpline
import seaborn as sns
sns.set_style("whitegrid")
sns.set_context(None)
sns.set_theme()
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
# display(all_results[:2])

#  %%
df = pd.json_normalize(all_results[1:]).T
# df["profile"] = df.index
# df = df.reset_index().rename(columns={"index":"profile"})
df.columns = [f"{STEP_PREFIX}_{col}" for col in df.columns]
df[['cls', 'profile']] = list(df.index.str.split('.', 1, expand=True))
df['profile_id'] = df['profile'].apply(lambda x: hashlib.md5(x.encode('utf-8')).hexdigest()[:8])  #.str.decode('utf-8')
df.index = df["cls"] + "_" + df["profile_id"]
df.head(5)
# %%
# data for graph showing the evolution of the probability for the particle containing the real opponent
print(df[df['cls'].str.contains("Exact")].iloc[:, 1:-3])

# df[df['cls'].str.contains("Exact")].iloc[:, 0:-5].to_csv(
#     "../eval/tmpevolution.csv", mode = "a", index = False, header = False)

# %%
# sum of weights
df[list(df.columns)[:-3]].sum(axis=1)

# %%
# PARTICLE COMPARISON
# strategytype = "Hardliner"
# fig = plt.figure(figsize=(20, 5))
# for cls in df['cls']:
#     vals = list(df.loc[df["cls"] == cls].iloc[0, 0:-3])
#     updates = df.shape[1]-3
#     cls = str(cls).split("_")[1]
#     xnew = np.linspace(0, updates-1, 200)
#     spl = make_interp_spline(list(range(updates)), vals, k=3)
#     y_smooth = spl(xnew)
#     plt.plot(xnew, y_smooth,
#              label=f"{cls} Particle",
#              linestyle='-',
#              linewidth=2,
#              color=np.append(np.array([0]), np.random.rand(2,)))

# plt.legend()
# plt.title(f"Evolution of particle probabilities in the negotiation against an {strategytype} opponent")
# plt.xlabel("Belief Update Step")
# plt.ylabel("Probability of Particle")
# plt.show()

# %%
# KNOWN STRATEGY
fig = plt.figure(figsize=(20, 5))
# colors = ["magenta","orange", "black", "green", "red", "violet", "maroon", "blue"]
for i, (cls, pid) in enumerate(zip(df['cls'], df['profile_id'])):
    vals = list(df.loc[df["profile_id"] == pid].iloc[0, 0:-3])
    updates = df.shape[1]-3
    cls = str(cls).split("_")[1]
    xnew = np.linspace(0, updates-1, 200)
    spl = make_interp_spline(list(range(updates)), vals, k=3)
    y_smooth = spl(xnew)
    if "Exact" in cls:
        plt.plot(xnew, y_smooth,
                 label=f"Correct {cls.split('act')[1]} Profile",
                 linestyle='-',
                 linewidth=2,
                #  color=colors[-1])
                 color=np.random.rand(3,))
        continue

    plt.plot(xnew, y_smooth,
             label=f"{cls} Profile_{i}",
             linestyle='-',
             linewidth=2,
            #  color=colors[i])
             color=np.random.rand(3,))

plt.legend()
plt.title(f"Evolution of particle probabilities in the case of a known strategy, but unknown profile")
plt.xlabel("Belief Update Step")
plt.ylabel("Probability of Preference Profile")
plt.show()

# %%
# KNOWN STRATEGY pie chart
# fig = plt.figure(figsize=(10, 10))
labels = [str(cls_).split("_")[1] for cls_, _ in zip(df['cls'], df["profile_id"])]
sizes = list(df[list(df.columns)[:-3]].sum(axis=1))

compressed_labels = []
compressed_sizes = []
rest_sum = 0
sizes_sum = sum(sizes)
idx = -1
for l, s in zip(labels, sizes):
    idx += 1
    if (s / sizes_sum) < 0.007:
        rest_sum += s
        continue
    compressed_labels.append("Correct " + l.split("act")[1] + " Profile" if "Exact" in l else l + f"Profile_{idx}")
    compressed_sizes.append(s)

compressed_labels.append("Other Profiles")
compressed_sizes.append(rest_sum)

maxs = max(compressed_sizes)
explode = tuple([0.05 if s == maxs else 0 for s in compressed_sizes])

fig1, ax1 = plt.subplots(figsize=(7, 7))
ax1.pie(compressed_sizes, explode=explode, labels=compressed_labels, autopct='%1.1f%%',
        shadow=True, startangle=0,  wedgeprops=dict(width=0.5, edgecolor='w'))
ax1.axis('equal')

plt.title('Particle Aggregated Probability Distribution', fontweight='bold')
plt.show()

#  %%
# play point for experiment 2


# %%
# KNOWN PROFILE
fig = plt.figure(figsize=(20, 5))
for cls in df['cls']:
    vals = list(df.loc[df["cls"] == cls].iloc[0, 0:-3])
    updates = df.shape[1]-3
    cls = str(cls).split("_")[1]
    xnew = np.linspace(0, updates-1, 200)
    spl = make_interp_spline(list(range(updates)), vals, k=3)
    y_smooth = spl(xnew)
    plt.plot(xnew, y_smooth,
             label=f"{cls} Strategy",
                      linestyle='-',
                      linewidth=2,
                      color=np.random.rand(3,))

plt.legend()
plt.title(f"Evolution of particle probabilities in the case of a known profile, but unknown strategy")
plt.xlabel("Belief Update Step")
plt.ylabel("Probability of Strategy type")
plt.show()

# %%
# KNOWN PROFILE pie chart
# fig = plt.figure(figsize=(10, 10))
labels = [str(cls_).split("_")[1] for cls_ in df['cls']]
sizes = list(df[list(df.columns)[:-3]].sum(axis=1))

compressed_labels = []
compressed_sizes = []
rest_sum = 0
sizes_sum = sum(sizes)
for l, s in zip(labels, sizes):
    if (s / sizes_sum) < 0.075:
        rest_sum += s
        continue
    compressed_labels.append(l)
    compressed_sizes.append(s)

compressed_labels.append("Others")
compressed_sizes.append(rest_sum)

maxs = max(compressed_sizes)
explode = tuple([0.05 if s == maxs else 0 for s in compressed_sizes])

fig1, ax1 = plt.subplots(figsize=(7, 7))
ax1.pie(compressed_sizes, explode=explode, labels=compressed_labels, autopct='%1.1f%%',
        shadow=True, startangle=90)
ax1.axis('equal')  

plt.title('Particle Aggregated Probability Distribution', fontweight='bold')
plt.show()

#  %%
# play point for experiment 3

#  %%
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
# anim.save('../visuals/particleUpdates.gif', writer='imagemagick', fps=2)
HTML(anim.to_jshtml())

# %%
most_likely = df[df[list(df.columns)[:-3]].sum(axis=1)
                 == df[list(df.columns)[:-3]].sum(axis=1).max()]
stype, profl = most_likely["cls"][0], most_likely['profile'][0]
print(f"Most likely profile:\n{stype} => {profl}")
# %%
df['weight'] = df[list(df.columns)[:-3]].sum(axis=1)
# df['distance'] = pd.Series([all_results[0]['Distance']
#                            for _ in range(len(df.index))
#                            ])
df['distance'] = all_results[0]['Distance']
# df['particle'] = df.index
df.reset_index(inplace=True)
df.rename(columns={'index': 'particle'}, inplace=True)

print(df.loc[:, ["distance", "particle", "weight"]])
# append_file_name = "realowntft"
# df.loc[:, ["distance", "particle", "weight"]].to_csv(
#     f"../eval/{append_file_name}.csv", mode="a", index=False, header=False)


# %%
