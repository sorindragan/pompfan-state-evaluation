# %%
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
sns.set_theme(style="whitegrid")

# %%
df = pd.read_csv("../eval/tournament_results_ExperimentStateEval.jsonl")
# drop zeros (no agreement)
df.drop(df.index[df['utility'] == 0.0], inplace=True)
eval_df = df[["evaluator", "utility"]].groupby("evaluator").agg(["sum", "count"])
eval_df["score"] = eval_df["utility"]["sum"] / eval_df["utility"]["count"]
eval_df = eval_df.sort_values("score", ascending=False)
# %%
fig = plt.figure(figsize=(10, 5))

plt.bar(["".join(ch for ch in eval if ch.isupper() or ch.isdigit())
        for eval in list(eval_df.index)
        ], eval_df["score"], color='gray',
        width=0.4)

plt.xlabel("Evaluation Function")
plt.ylabel("Average Utility")
plt.title("")
plt.show()

# %%
