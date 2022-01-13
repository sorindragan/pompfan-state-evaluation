# %%
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
sns.set_style("whitegrid")
sns.set_context(None)
sns.set_theme()
# %%
known_agent_name = "SimulatedExactConceder"
file_name = f"real{known_agent_name.split('Exact')[1].lower()}"
agent = known_agent_name.split("Exact")[1]
df = pd.read_csv(f"../eval/{file_name}.csv")
df['particle'] = df['particle'].str.split("_").str[:2].str.join("")
df

# %%
distances = set(list(df["distance"]))
new_d = pd.DataFrame()
for i, d in enumerate(distances):
    total = float(df[df['distance']==d].groupby("particle").sum().sum())
    df_d = df[df['distance'] == d].groupby("particle").sum()
    df_d["weight"] = df_d["weight"] / total
    # if i == 0:
    #     new_d.insert(i, "opponent", df["particle"])

    print(df_d["weight"])
    new_d.insert(i, d, df_d["weight"])

new_d["opponent_type"] = new_d.index

weight_dist = new_d[new_d["opponent_type"] == known_agent_name]
weight_dist = dict(zip(list(weight_dist), list(weight_dist.values)[0][:-1]))
# %%
fig = plt.figure(figsize=(12, 5))

plt.bar([name.split("Bid")[0] for name in weight_dist.keys()], list(weight_dist.values()), color='darkblue',
        width=0.5)

plt.xlabel("Used Distance")
plt.ylabel("Aggregated Probability Mass of Real Opponent")
plt.title(f"Mean probability mass received by the particle containing the real {agent} opponent")
plt.show()

# %%
# df.loc[df["distance"].isin(
#     ["IssueValueCountBidDistance", "BothUtilityBidDistance"])]

distances = ["JaccardBidDistance", "BothUtilityBidDistance"]

particles = [
    'SimulatedBoulware',
    'SimulatedConceder',
    f'{known_agent_name}',
    'SimulatedHardLiner',
    'SimulatedLinear',
    'SimulatedOppUtilTFT',
    'SimulatedOwnUtilTFT']

probabilities1 = np.array([df.loc[(df["distance"] == distances[0]) & (
    df["particle"] == particle)].sum()["weight"] for particle in particles])

aggregatedprobs1 = probabilities1 / probabilities1.sum()

dict1 = dict(zip(particles, aggregatedprobs1))
# dict1
probabilities2 = np.array([df.loc[(df["distance"] == distances[1]) & (
    df["particle"] == particle)].sum()["weight"] for particle in particles])

aggregatedprobs2 = probabilities2 / probabilities2.sum()

dict2 = dict(zip(particles, aggregatedprobs2))

# %%
fig = plt.figure(figsize=(19, 5))
width = 0.4
ind = np.arange(7)
ax = fig.add_subplot(111)
tickers = particles
b1 = ax.bar(ind, [dict1[k] for k in tickers], width,
       color='darkblue', label='-Ymin', align='edge')
b2 = ax.bar(ind+width, [dict2[k] for k in tickers],
       width, color='lightgray', label='Ymax', align='edge')

ax.set_ylabel('Probability')
ax.set_xlabel('Particle Type')
ax.set_title('Probability mass assigned to each particle; A comparison')
ax.set_xticks(ind + width)
ax.set_xticklabels([name.split("imulated")[1]+"Particle" for name in particles])

ax.legend((b1[0], b2[0]), (distances[0], distances[1]))
plt.show()


# %%
# particle evolution
# dfr = pd.read_csv(f"../eval/tmpevolution.csv", header=0)
# bu = list(dfr.iloc[0,:])
# ivc = list(dfr.iloc[1,:])
# plt.plot(list(range(len(bu))), bu, label="BothUtilityDistance", color="darkgreen")
# plt.plot(list(range(len(bu))), ivc, label="IssueValueCountDistance", color="orchid")
# plt.legend()
# # plt.title("Certanty the model has that the particle containing the real NTFT opponent is indeed the real one")
# plt.title("Evolution of the probability assigned to the particle \n containing the real Boulware opponent \n using two different distance metrics")
# plt.xlabel("Belief Update Step")
# plt.ylabel("Probability")
# plt.show()

# %%
