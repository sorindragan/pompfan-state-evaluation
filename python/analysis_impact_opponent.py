# %%
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from scipy import stats
import seaborn as sns
sns.set_theme(style="whitegrid")

# %%
# file_name = "known_opponent_boulware.csv"
file_name = "result_last_experiment.jsonl"


# file_name = "known_opponent_ntft.csv"

# domain_name = "Party"
domain_name = "Abstract"

# opptype = "OwnTFT"
opptype = "Boulware"


n = 10
df = pd.read_csv(f"../eval/{file_name}")

# %%


known_utils_partyz = list(df.iloc[:n, 2]) 
unknown_utils_partyz = list(df.iloc[n:2*n, 2]) 
wrong_utils_partyz = list(df.iloc[2*n:, 2])

known_utils_party = list(filter(lambda num: num != 0, known_utils_partyz))
unknown_utils_party = list(filter(lambda num: num != 0, unknown_utils_partyz))
wrong_utils_party = list(filter(lambda num: num != 0, wrong_utils_partyz))

data = [known_utils_party, unknown_utils_party, wrong_utils_party]

fig = plt.figure(figsize=(14, 6))
ax = sns.boxplot(data=data,  palette="Set2", width=0.5, linewidth=2.5)
# plt.boxplot(data)
ax.set_xticklabels([f"Known Opponent {domain_name} Domain", f"Multiple Unknown Opponents {domain_name} Domain",
                   f"Single Unknown Opponent {domain_name} Domain"])

plt.title(f"Utility earned across multiple negotiations when the {opptype} opponent is known vs. unknown")
plt.xlabel("Setting")
plt.ylabel("Utility")
plt.show()

# %%
# statistical significange
t_party_m, p_party_m  = stats.ttest_ind(known_utils_party, unknown_utils_party)
print(t_party_m, p_party_m)
t_party_s, p_party_s = stats.ttest_ind(known_utils_party, wrong_utils_party)
print(t_party_s, p_party_s)

# %%
known_p_zeros = np.count_nonzero(
    known_utils_partyz) / len(known_utils_partyz)
unknown_p_zeros = np.count_nonzero(
    unknown_utils_partyz) / len(unknown_utils_partyz)
wrong_p_zeros = np.count_nonzero(
    wrong_utils_partyz) / len(wrong_utils_partyz)

print(known_p_zeros, unknown_p_zeros, wrong_p_zeros)

# %%
