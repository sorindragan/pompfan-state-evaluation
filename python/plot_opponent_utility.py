#  %%
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from scipy.interpolate import make_interp_spline

# agent_name = "BOULWARE"
# agent_name = "CONCEDER"
# agent_name = "HARDLINER"
# agent_name = "LINEAR"
agent_name = "TFT"


smoothing = False

df = pd.read_csv(f"../eval/{agent_name.lower()}_bids.csv") 
# %%

time = df["time"].to_numpy()
target_util = df["target"].to_numpy()
sent_util = df["actual"].to_numpy()
# plt.figure(figsize=(7, 5))
plt.figure(facecolor='white')
if smoothing:
    t_len = len(time)
    x = np.linspace(0, 1, 100)
    spl1 = make_interp_spline(np.linspace(0, 1, t_len), target_util, k=3)
    spl2 = make_interp_spline(np.linspace(0, 1, t_len), sent_util, k=3)
    y_t = spl1(x)
    y_s = spl2(x)

    plt.plot(x, y_t, color="red", label='Target Utility')
    plt.plot(x, y_s, color="blue", label='Utility of Actual Bid')
else:
    plt.plot(time, target_util, color="red", label='Target Utility')
    plt.plot(time, sent_util, color="blue", label='Utility of Actual Bid')
    m = np.mean(abs(target_util - sent_util))
    plt.text(0.07, 0.87, f"The mean difference between target and actual is {m:.4f}")

plt.legend()
plt.title(f"{agent_name} Target Utility vs Chosen Utility")
plt.savefig(f"../visuals/opponents/{agent_name.lower()}_party.png")
plt.show()
#  %%
