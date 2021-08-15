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
import statsmodels.api as sm
import statsmodels.formula.api as smf
# %%
filename = "tournament_results_random.jsonl"
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
df = pd.json_normalize(all_results)
# %%
cols_conf = [
    "beliefer",
    "comparer",
    "explorer",
    "evaluator",
    "widener",
]
cols_mwws = [
    "maxWidth",
]

cols_pgws = [
    "k_a",
    "a_a",
    "a_b",
]

cols_univ = [
    "numParticles",
    "simulationTime",
]

# %%
df["name"] = None
df["name"] = df.party.copy()
df = df.rename(
    columns={
        "pwp.party.parameters.config.confBelief": "beliefer",
        "pwp.party.parameters.config.confComparer": "comparer",
        "pwp.party.parameters.config.confExplorer": "explorer",
        "pwp.party.parameters.config.confEvaluator": "evaluator",
        "pwp.party.parameters.config.confWidener": "widener",
        "pwp.party.parameters.numParticlesPerOpponent": "numParticles",
        "pwp.party.parameters.simulationTime": "simulationTime",
        "params.config.confExtra.widener.k_a": "k_a",
        "params.config.confExtra.widener.k_b": "k_b",
        "params.config.confExtra.widener.a_a": "a_a",
        "params.config.confExtra.widener.a_b": "a_b",
        "params.config.confExtra.widener.maxWidth": "maxWidth",
    })
i_subset = df.party == "POMPFANAgent"
tmp_df = df[i_subset]

df.loc[i_subset, ["name"]] = tmp_df[cols_conf[0]] + "_" + tmp_df[cols_conf[1]] + "_" + tmp_df[
    cols_conf[2]] + "_" + tmp_df[cols_conf[3]] + "_" + tmp_df[cols_conf[4]]
df[i_subset]

# %%
from sklearn import preprocessing
df["name_encoded"] = df.party.copy()
le = preprocessing.LabelEncoder()
df.loc[i_subset, ["name_encoded"]] = le.fit_transform(df[i_subset].name)
df
# %%
df["no_agreement"] = df.utility == 0.0
df["vs"] = None
df["vs_utility"] = None
for index, df_subset in df.groupby(["session", "tournamentStart", "sessionStart"]):
    party1, party2 = df_subset["name"]
    party1_enc, party2_enc = df_subset["name_encoded"]
    util1, util2 = df_subset["utility"]
    selector = (df.session == index[0]) & (df.tournamentStart == index[1]) & (df.sessionStart == index[2])
    df.loc[(df.name == party1) & selector, "vs"] = party2_enc
    df.loc[(df.name == party2) & selector, "vs"] = party1_enc
    df.loc[(df.name == party1) & selector, "vs_utility"] = util2
    df.loc[(df.name == party2) & selector, "vs_utility"] = util1

df.to_csv(file_to_analyse.parent / "data.csv")
df
# %%
df_relevant = df[df.party == "POMPFANAgent"]
df_relevant["numParticles"] = df_relevant["numParticles"].astype(float)
df_relevant["simulationTime"] = df_relevant["simulationTime"].astype(float)
df_relevant["k_a"] = df_relevant["k_a"].astype(float)
df_relevant["a_a"] = df_relevant["a_a"].astype(float)
df_relevant["k_b"] = df_relevant["k_b"].astype(float)
df_relevant["a_b"] = df_relevant["a_b"].astype(float)
df_relevant["maxWidth"] = df_relevant["maxWidth"].astype(float)
df_relevant
# %%
df_pgws = df_relevant[df_relevant.widener == "ProgressiveWideningStrategy"]
df_mwws = df_relevant[df_relevant.widener == "MaxWidthWideningStrategy"]


def mode(x):
    return x.value_counts().index[0]


def get_top_k(df, k=50):
    param_grp = ['mean', 'median', 'min', 'max']
    result_df = df.groupby(["name"]).agg({
        'utility': ['mean', 'median', 'sum'],
        'session': ['count'],
        'numParticles': param_grp,
        'simulationTime': param_grp,
        'k_a': param_grp,
        'a_a': param_grp,
        'a_b': param_grp,
        # 'k_a': ['median'],
        # 'a_a': ['median'],
        # 'a_b': ['median'],
        # 'k_a': ['min', 'max'],
        # 'a_a': ['min', 'max'],
        # 'a_b': ['min', 'max'],
        'maxWidth': param_grp,
        'no_agreement': ['mean', 'sum'],
    }).reset_index()
    result_df[("agreement", "sum")] = result_df[("session", "count")] - result_df[("no_agreement", "sum")]
    result_df[("utility", "adj.mean")] = result_df[("utility", "sum")] / result_df[("agreement", "sum")]
    result_df.columns = ['_'.join(col).strip() if len(col) > 1 else col[0] for col in result_df.columns.values]
    return result_df.sort_values([('utility_mean')], False).tail(k)


display("PGWS")
agg_df_pgws = get_top_k(df_pgws).drop(
    [
        "maxWidth_mean",
        "maxWidth_median",
        "maxWidth_min",
        "maxWidth_max",
    ],
    axis=1,
    errors='ignore',
)
display(agg_df_pgws)
agg_df_pgws.to_csv(log_dir / "data_pgws.csv")
display("MWWS")
agg_df_mwws = get_top_k(df_mwws).drop(
    [
        "k_a_mean",
        "k_a_median",
        "k_a_min",
        "k_a_max",
        "a_a_mean",
        "a_a_median",
        "a_a_min",
        "a_a_max",
        "a_b_mean",
        "a_b_median",
        "a_b_min",
        "a_b_max",
    ],
    axis=1,
    errors='ignore',
)
display(agg_df_mwws)
agg_df_mwws.to_csv(log_dir / "data_mwws.csv")

# %%
fig, axes = plt.subplots(2, 1, figsize=(15, 15))
subset_id = "ParticleFilterBelief_JaccardBidDistance_TimeConcedingExplorationPolicy_Last2BidsMeanUtilityEvaluator_MaxWidthWideningStrategy"

divider_id = "simulationTime"
ax = axes[0]
ax = sns.boxplot(data=df_mwws, x=divider_id, y="utility", ax=ax)
for tick in ax.get_xticklabels():
    tick.set_rotation(45)
ax.set_xlabel("Simulation Time")
ax.set_ylabel("Utility")

divider_id = "numParticles"
ax = axes[1]
ax = sns.boxplot(data=df_mwws, x=divider_id, y="utility", ax=ax)
for tick in ax.get_xticklabels():
    tick.set_rotation(45)
ax.set_xlabel("Num Particles per Opponent")
ax.set_ylabel("Utility")

# %%
# df_tmp = df_pgws
# md = smf.mixedlm(
#     "utility ~ belief + comparer + explorer + widener + simulationTime + np.square(simulationTime) + numParticles + np.square(numParticles) + k_a * a_a + a_b",
#     df_tmp,
#     groups=df_tmp["name"])
# mdf = md.fit()
# print(mdf.summary())

# # %%
# df_tmp = df_mwws
# md = smf.mixedlm(
#     "utility ~ belief + comparer + explorer + widener + simulationTime + np.square(simulationTime) + numParticles + np.square(numParticles) + maxWidth + np.square(maxWidth)",
#     df_tmp,
#     groups=df_tmp["name"])
# mdf = md.fit()
# print(mdf.summary())
# # %%
# fig, (ax, ax2) = plt.subplots(2, 1, figsize=(10, 7))
# df_subset = df[df.party == "POMPFANAgent"]
# ax = sns.barplot(data=df_subset, x=divider_id, y="utility", ax=ax, estimator=sum)
# for tick in ax.get_xticklabels():
#     tick.set_rotation(45)
# ax.set_xlabel("Simulation Time")
# ax.set_ylabel("Utility")
# ax.set_title("Sum of Utility of POMPFAN accross SimTime")

# ax2 = sns.barplot(data=df_subset, x=divider_id, y="no_agreement", ci=None, ax=ax2)
# for tick in ax2.get_xticklabels():
#     tick.set_rotation(45)
# ax2.set_xlabel("Simulation Time")
# ax2.set_ylabel("Non-Agreements Percentage")
# ax2.set_title("Average non-Agreements of POMPFAN accross SimTime")
# fig.tight_layout()
# plt.show()

# # %%
# fig, ax = plt.subplots(1, 1, figsize=(10, 10))
# df_no_aggreement_counts = df[df["no_agreement"]].groupby("pwp.party.parameters.simulationTime").count()
# unique_agents = df_no_aggreement_counts.index.unique()
# num_agents = len(unique_agents)
# (ax, ) = df_no_aggreement_counts["session"].plot.pie(
#     subplots=True,
#     ax=ax,
#     autopct="%.3f%%",
#     explode=[0.02] * num_agents,
#     labels=unique_agents,
# )
# ax.set_ylabel("")
# fig.suptitle("Number of non-Agreements of POMPFAN across SimTime")
# fig.tight_layout()
# plt.show()

# # %%
# sns.countplot(data=df_subset, x="name", hue=divider_id)
# # %%
# df[df["params.config.confExtra.widener.k_b"] == 1.0].groupby(["name", "params.config.confExtra.widener.k_a"
#                                                               ]).median().sort_values("utility")
# # %%

# %%
from rulefit import RuleFit
from sklearn.ensemble import RandomForestRegressor
from sklearn import preprocessing
from sklearn import metrics
df_tmp = df_mwws
df_tmp = df_tmp[df_tmp["utility"] > 0]

min_max_scaler = preprocessing.MinMaxScaler()
y = df_tmp["utility"]
X = df_tmp[cols_conf + cols_mwws + cols_univ]
X = pd.get_dummies(X, drop_first=True)
# X.loc[:,:] = min_max_scaler.fit_transform(X)

# %%
# https://towardsdatascience.com/interpretable-machine-learning-in-10-minutes-with-rulefit-and-scikit-learn-da9ebb925795
rulefit = RuleFit(tree_generator=RandomForestRegressor(n_estimators=10))
rulefit.fit(X.values, y, feature_names=X.columns)
rulefit_preds = rulefit.predict(X.values)
rulefit_rmse = metrics.r2_score(y, rulefit_preds)
print(rulefit_rmse)
# %%
pd.set_option('display.max_colwidth', 400) # Adjust row width to read the entire rule
pd.options.display.float_format = '{:.5f}'.format # Round decimals to 2 decimal places
rules = rulefit.get_rules() # Get the rules
rules = rules[rules['type']!='linear'] # Eliminate the existing explanatory variables
rules = rules[rules['coef'] != 0] # eliminate the insignificant rules
rules = rules.sort_values('support', ascending=False) # Sort the rules based on "support" value
# rules = rules[rules['rule'].str.len()>30] # optional: To see more complex rules, filter the long rules
rules.iloc[0:5] # Show the first 5 rules
# %%
import shap
import xgboost

model = xgboost.XGBRegressor().fit(X.values, y)
# model = RandomForestRegressor(n_estimators=10).fit(X.values, y)
rulefit_rmse = metrics.r2_score(y, model.predict(X.values))
print(rulefit_rmse)
explainer = shap.Explainer(model)
shap_values = explainer(X)
# shap.plots.waterfall(shap_values[0]) 
# shap.plots.waterfall(explainer.base_values[0], shap_values[0], X[0])
shap.plots.beeswarm(shap_values) 
# %%
