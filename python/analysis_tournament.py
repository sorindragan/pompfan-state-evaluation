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
import shap
import xgboost
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
df["error"] = df.utility == -1.0
df["vs"] = None
df["vs_utility"] = None
for index, df_subset in df.groupby(["session", "tournamentStart", "sessionStart"]):
    try:
        party1, party2 = df_subset["name"]
        party1_enc, party2_enc = df_subset["name_encoded"]
        util1, util2 = df_subset["utility"]
        selector = (df.session == index[0]) & (df.tournamentStart == index[1]) & (df.sessionStart == index[2])
        df.loc[(df.name == party1) & selector, "vs"] = party2_enc
        df.loc[(df.name == party2) & selector, "vs"] = party1_enc
        df.loc[(df.name == party1) & selector, "vs_utility"] = util2
        df.loc[(df.name == party2) & selector, "vs_utility"] = util1
    except Exception as e:
        print("Problem with missing correspondence!!!")
        print(e)
        print(index)
        display(df_subset)

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
fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 5))
sns.boxplot(data=df_pgws, x="vs", y="utility", ax=ax1)
sns.boxplot(data=df_mwws, x="vs", y="utility", ax=ax2)
ax1.set_title("ProgessiveWidening")
ax2.set_title("MaxWidthWidening")
for ax in [ax1, ax2]:
    for tick in ax.get_xticklabels():
        tick.set_rotation(45)
    ax.set_xlabel("Opponents")
    ax.set_ylabel("Utility")
# %%

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
plt.show()
# %%
ax = sns.boxplot(data=df_relevant, x="vs", y="utility")
for tick in ax.get_xticklabels():
    tick.set_rotation(45)
ax.set_xlabel("Num Particles per Opponent")
ax.set_ylabel("Utility")
plt.show()

# %%
print("df_relevant: ")
display(
    df_relevant.groupby(["vs"]).agg({
        'utility': ['mean', 'median', 'sum'],
        'session': ['count'],
        'no_agreement': ['mean', 'sum'],
    }))
print("df_pgws: ")
display(
    df_pgws.groupby(["vs"]).agg({
        'utility': ['mean', 'median', 'sum'],
        'session': ['count'],
        'no_agreement': ['mean', 'sum'],
    }))
print("df_mwws: ")
display(
    df_mwws.groupby(["vs"]).agg({
        'utility': ['mean', 'median', 'sum'],
        'session': ['count'],
        'no_agreement': ['mean', 'sum'],
    }))

# %%
from rulefit import RuleFit
from sklearn.ensemble import RandomForestRegressor
from sklearn import preprocessing
from sklearn import metrics
# %%
df_tmp = df_mwws
df_tmp = df_tmp[df_tmp["utility"] > 0]
min_max_scaler = preprocessing.MinMaxScaler()
y = df_tmp["utility"]
X = df_tmp[cols_conf + cols_mwws + cols_univ]
X = pd.get_dummies(X, drop_first=True)
# X.loc[:,:] = min_max_scaler.fit_transform(X)

# https://towardsdatascience.com/interpretable-machine-learning-in-10-minutes-with-rulefit-and-scikit-learn-da9ebb925795
rulefit = RuleFit(tree_generator=RandomForestRegressor(n_estimators=10))
rulefit.fit(X.values, y, feature_names=X.columns)
rulefit_preds = rulefit.predict(X.values)
rulefit_rmse = metrics.r2_score(y, rulefit_preds)
print(rulefit_rmse)
rules_mwws = rulefit.get_rules()
# %%
df_tmp = df_pgws
df_tmp = df_tmp[df_tmp["utility"] > 0]
min_max_scaler = preprocessing.MinMaxScaler()
y = df_tmp["utility"]
X = df_tmp[cols_conf + cols_pgws + cols_univ]
X = pd.get_dummies(X, drop_first=True)
# X.loc[:,:] = min_max_scaler.fit_transform(X)

# https://towardsdatascience.com/interpretable-machine-learning-in-10-minutes-with-rulefit-and-scikit-learn-da9ebb925795
rulefit = RuleFit(tree_generator=RandomForestRegressor(n_estimators=10))
rulefit.fit(X.values, y, feature_names=X.columns)
rulefit_preds = rulefit.predict(X.values)
rulefit_rmse = metrics.r2_score(y, rulefit_preds)
print(rulefit_rmse)
rules_pgws = rulefit.get_rules()
# %%
rules_mwws["strategy"] = "MaxWidthWidening"
rules_pgws["strategy"] = "ProgessiveWidening"
rules = pd.concat([rules_mwws, rules_pgws])

# %%
df_tmp = df_pgws
df_tmp = df_tmp[df_tmp["utility"] > 0]
min_max_scaler = preprocessing.MinMaxScaler()
y = df_tmp["utility"]
X = df_tmp[cols_conf + cols_pgws + cols_univ]
X = pd.get_dummies(X, drop_first=True)
model = xgboost.XGBRegressor().fit(X.values, y)
# model = RandomForestRegressor(n_estimators=10).fit(X.values, y)
rulefit_rmse = metrics.r2_score(y, model.predict(X.values))
print(rulefit_rmse)
explainer = shap.Explainer(model)
shap_values = explainer(X)
shap.plots.beeswarm(shap_values)
# %%
df_tmp = df_mwws
df_tmp = df_tmp[df_tmp["utility"] > 0]
min_max_scaler = preprocessing.MinMaxScaler()
y = df_tmp["utility"]
X = df_tmp[cols_conf + cols_mwws + cols_univ]
X = pd.get_dummies(X, drop_first=True)
model = xgboost.XGBRegressor().fit(X.values, y)
# model = RandomForestRegressor(n_estimators=10).fit(X.values, y)
rulefit_rmse = metrics.r2_score(y, model.predict(X.values))
print(rulefit_rmse)
explainer = shap.Explainer(model)
shap_values = explainer(X)
shap.plots.beeswarm(shap_values)

# %%
pd.set_option('display.max_colwidth', 400)  # Adjust row width to read the entire rule
pd.options.display.float_format = '{:.5f}'.format  # Round decimals to 2 decimal places
# rules = rulefit.get_rules()  # Get the rules
rules = rules_pgws  # Get the rules
rules = rules[rules['type'] != 'linear']  # Eliminate the existing explanatory variables
rules = rules[rules['coef'] != 0]  # eliminate the insignificant rules
rules = rules.sort_values('importance', ascending=False)  # Sort the rules based on "support" value
# rules = rules[rules['rule'].str.len()>30] # optional: To see more complex rules, filter the long rules
rules.iloc[0:20]  # Show the first 5 rules

from adjustText import adjust_text
fig = plt.figure(figsize=(15, 10))
ax = sns.scatterplot(data=rules, x="coef", y="support", size="importance", alpha=0.5, legend=False, sizes=(20, 2000))
TEXTS = []
BG_WHITE = "#fbf9f4"
GREY_LIGHT = "#b4aea9"
GREY50 = "#7F7F7F"
GREY30 = "#4d4d4d"
topk_rules = rules.iloc[0:3]
for i in range(len(topk_rules)):
    x = topk_rules["coef"].iloc[i]
    y = topk_rules["support"].iloc[i]
    text = topk_rules["rule"].iloc[i]
    TEXTS.append(ax.text(x, y, text, color=GREY30, fontsize=14, fontname="Poppins"))

adjust_text(TEXTS, expand_points=(1, 10), arrowprops=dict(arrowstyle="->", color=GREY50, lw=2), ax=fig.axes[0])
# legend = ax.legend(
#     loc=(0.85, 0.025), # bottom-right
#     labelspacing=1.5,  # add space between labels
#     markerscale=1.5,   # increase marker size
#     frameon=False      # don't put a frame
# )
plt.show()
# %%
pd.set_option('display.max_colwidth', 400)  # Adjust row width to read the entire rule
pd.options.display.float_format = '{:.5f}'.format  # Round decimals to 2 decimal places
# rules = rulefit.get_rules()  # Get the rules
rules = rules_mwws  # Get the rules
rules = rules[rules['type'] != 'linear']  # Eliminate the existing explanatory variables
rules = rules[rules['coef'] != 0]  # eliminate the insignificant rules
rules = rules.sort_values('importance', ascending=False)  # Sort the rules based on "support" value
# rules = rules[rules['rule'].str.len()>30] # optional: To see more complex rules, filter the long rules
rules.iloc[0:20]  # Show the first 5 rules

from adjustText import adjust_text
fig = plt.figure(figsize=(15, 10))
ax = sns.scatterplot(data=rules, x="coef", y="support", size="importance", alpha=0.5, legend=False, sizes=(20, 2000))
TEXTS = []
BG_WHITE = "#fbf9f4"
GREY_LIGHT = "#b4aea9"
GREY50 = "#7F7F7F"
GREY30 = "#4d4d4d"
topk_rules = rules.iloc[0:3]
for i in range(len(topk_rules)):
    x = topk_rules["coef"].iloc[i]
    y = topk_rules["support"].iloc[i]
    text = topk_rules["rule"].iloc[i]
    TEXTS.append(ax.text(x, y, text, color=GREY30, fontsize=14, fontname="Poppins"))

adjust_text(TEXTS, expand_points=(1, 10), arrowprops=dict(arrowstyle="->", color=GREY50, lw=2), ax=fig.axes[0])
# legend = ax.legend(
#     loc=(0.85, 0.025), # bottom-right
#     labelspacing=1.5,  # add space between labels
#     markerscale=1.5,   # increase marker size
#     frameon=False      # don't put a frame
# )
plt.show()
# %%