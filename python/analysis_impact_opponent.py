# %%
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from scipy import stats

# %%
file_name = "known_opponent_boulware.csv"
# file_name = "known_opponent_ntft.csv"

n = 20
df = pd.read_csv(f"../eval/{file_name}")

# %%
opptype = "Boulware"

known_utils_party = list(df.iloc[:n, 2]) 
known_utils_flight = list(df.iloc[n:2*n, 2]) 
unknown_utils_party = list(df.iloc[2*n:3*n, 2]) 
unknown_utils_flight = list(df.iloc[3*n:, 2])
data = [known_utils_party, unknown_utils_party,
        known_utils_flight, unknown_utils_flight]

fig = plt.figure(figsize=(13, 6))
ax = plt.subplot()
plt.boxplot(data)
ax.set_xticklabels(["Known Opponent Party Domain", "Unknown Opponent Party Domain",
                   "Known Opponent Flight Domain", "Unknown Opponent Flight Domain"])

plt.title(f"Utility earned across multiple negotiations when the {opptype} opponent is known vs. unknown")
plt.xlabel("Setting")
plt.ylabel("Utility")
plt.show()

# %%
# statistical significange
t_party, p_party  = stats.ttest_ind(known_utils_party, unknown_utils_party)
print(t_party, p_party)
t_flight, p_flight = stats.ttest_ind(known_utils_flight, unknown_utils_flight)
print(t_flight, p_flight)

# %%
