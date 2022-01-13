# %%
import pandas as pd
import matplotlib.pyplot as plt

# %%
df = pd.read_csv("../eval/tournament_results_Experiment1A.jsonl")
eval_df = df[["Evaluator", "utility"]].groupby("Evaluator").agg(["sum", "count"])
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
