# %%
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

# %%
file_name = "realtft"
known_agent_name = "SimulatedExactOpponentOppTFT"
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
fig = plt.figure(figsize=(10, 5))

plt.bar([name.split("Bid")[0] for name in weight_dist.keys()], list(weight_dist.values()), color='darkblue',
        width=0.5)

plt.xlabel("Used Distance")
plt.ylabel("Aggregated Probability Mass of Real Opponent")
plt.title("Mean probability mass received by the particle containing the real NTFT opponent")
plt.show()

# %%
# df.loc[df["distance"].isin(
#     ["IssueValueCountBidDistance", "BothUtilityBidDistance"])]

probabilities1 = [df[df["distance"] == "JaccardBidDistance"].iloc[i-11:i, :]
     for i in range(11, 66, 11)]
probabilities1w = [d["weight"] for d in probabilities1]
aggregatedprobs1 = np.array([0.0]* 11)
for d in probabilities1w:
    d = np.array(d)
    aggregatedprobs1 += d

aggregatedprobs1 = aggregatedprobs1 / aggregatedprobs1.sum()
names1 = [name.split("lated")[1] for name in probabilities1[0]["particle"]]
dict1 = dict(zip(names1, aggregatedprobs1))
print(dict1)

probabilities2 = [df[df["distance"] == "BothUtilityBidDistance"].iloc[i-11:i, :]
                  for i in range(11, 66, 11)]
probabilities2w = [d["weight"] for d in probabilities2]
aggregatedprobs2 = np.array([0.0] * 11)
for d in probabilities2w:
    d = np.array(d)
    aggregatedprobs2 += d

aggregatedprobs2 = aggregatedprobs2 / aggregatedprobs2.sum()
names2 = [name.split("lated")[1] for name in probabilities2[0]["particle"]]
dict2 = dict(zip(names2, aggregatedprobs2))
print(dict2)

# %%
fig = plt.figure(figsize=(20, 5))
width = 0.4
ind = np.arange(11)
ax = fig.add_subplot(111)
tickers = names2
b1 = ax.bar(ind, [dict1[k] for k in tickers], width,
       color='blue', label='-Ymin', align='edge')
b2 = ax.bar(ind+width, [dict2[k] for k in tickers],
       width, color='gray', label='Ymax', align='edge')

ax.set_ylabel('Probability')
ax.set_xlabel('Particle Type')
ax.set_title('Probability mass assigned to each particle; A comparison')
ax.set_xticks(ind + width)
ax.set_xticklabels(tickers)

ax.legend((b1[0], b2[0]), ('JaccardBidDistance', 'BothUtilityBidDistance'))
plt.show()


# %%
