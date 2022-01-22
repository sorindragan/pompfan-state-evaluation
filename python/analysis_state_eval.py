# %%
from audioop import reverse
from itertools import count
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from scipy import stats
sns.set_theme(style="whitegrid")

# %%
# df = pd.read_csv("../eval/tournament_results_ExperimentStateEval_Party.jsonl")

df = pd.read_csv("../eval/tournament_results_ExperimentStateEval.jsonl")
# df = df.iloc[::2]
original_df = df.copy(deep=True)
df_zeros = df[df["utility"] == 0.0]
# drop zeros (no agreement)
df.drop(df.index[df['utility'] == 0.0], inplace=True)
eval_df = df[["evaluator", "utility"]].groupby("evaluator").agg(["sum", "count"])
eval_df["score"] = eval_df["utility"]["sum"] / eval_df["utility"]["count"]
eval_df = eval_df.sort_values("score", ascending=False)
# %%
fig = plt.figure(figsize=(10, 5))

plt.bar(["".join(ch for ch in eval if ch.isupper() or ch.isdigit())
        for eval in list(eval_df.index)
        ], eval_df["score"], color='dodgerblue',
        width=0.4)

plt.xlabel("State Evaluation Function")
plt.ylabel("Average Utility")
plt.title("Average Utility gain in self-play using each proposed Evaluation Function")
plt.show()

# %%
eval_arrays = {}
for eval in set(df["evaluator"]):
    eval_arrays["".join(ch for ch in eval if ch.isupper() or ch.isdigit())] = np.array(df[df["evaluator"] == eval]["utility"])

t_, p_ = stats.ttest_ind(
    eval_arrays["L2BMUE"], eval_arrays["RE"], equal_var=False)
print("L2BMUE", t_, p_)
t, p = stats.ttest_ind(
    eval_arrays["L2BMMUE"], eval_arrays["RE"], equal_var=False)
print("L2BMMUE", t, p)

t_, p_ = stats.ttest_ind(
    eval_arrays["L2BPUE"], eval_arrays["RE"], equal_var=False)
print("L2BPUE", t_, p_)
t, p = stats.ttest_ind(
    eval_arrays["L2BMPUE"], eval_arrays["RE"], equal_var=False)
print("L2BMPUE", t, p)
# %%
agreement_rate = {}
for eval in set(df["evaluator"]):
    agreement_rate["".join(ch for ch in eval if ch.isupper() or ch.isdigit())] = 1 - (np.array(
        df_zeros[df_zeros["evaluator"] == eval].count())[0] / np.array(original_df[original_df["evaluator"] == eval].count())[0])

agreement_rate = dict(sorted(agreement_rate.items(), key=lambda x: x[1], reverse=True))
print(agreement_rate)
# %%
fig, ax = plt.subplots(figsize=(16, 10), dpi=80)
ax.vlines(x=[i+1 for i in range(len(agreement_rate.keys()))], ymin=0, ymax=list(agreement_rate.values()),
          color='forestgreen', alpha=0.7, linewidth=4)
ax.scatter(x=[i+1 for i in range(len(agreement_rate.keys()))], y=list(agreement_rate.values()),
           s=250, color='seagreen', alpha=1)

# Title, Label, Ticks and Ylim
ax.set_title('Proportion of Agreements', fontdict={'size': 22})
ax.set_ylabel(
    'Fraction of total negotiations which ended in an agreement', fontdict={'size': 14})
ax.set_xticks([i+1 for i in range(len(agreement_rate.keys()))])
ax.set_xticklabels(list(agreement_rate.keys()), rotation=60, fontdict={
                   'horizontalalignment': 'right', 'size': 12})
ax.set_ylim(0, 0.5)

# Annotate
for i, k in enumerate(agreement_rate.keys()):
    ax.text(i+1, agreement_rate[k]+.03, s=f"$\\bf{round(agreement_rate[k], 3)}$",
            horizontalalignment='center', verticalalignment='bottom', fontsize=14)

plt.show()
# %%
# Run Point
