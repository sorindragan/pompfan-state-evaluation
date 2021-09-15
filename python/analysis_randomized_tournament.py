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
        "pwp.party.parameters.dataCollectionTime": "dataCollectionTime",
        "params.config.confExtra.widener.k_a": "k_a",
        "params.config.confExtra.widener.k_b": "k_b",
        "params.config.confExtra.widener.a_a": "a_a",
        "params.config.confExtra.widener.a_b": "a_b",
        "params.config.confExtra.widener.maxWidth": "maxWidth",
        "pwp.profile": "profile",
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
df["profile"] = df["profile"].str.split("/").str[-1]
df["no_agreement"] = (df.utility == 0.0) * 1.0
df["agreement"] = (df.utility != 0.0) * 1.0
df["error"] = df.utility == -1.0
df["vs"] = None
df["vs_utility"] = 0
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
        display(df_subset.iloc[:, :])

df.to_csv(file_to_analyse.parent / "data.csv")
df
# %%
df_relevant = df[df.party == "POMPFANAgent"].copy()
df_relevant.loc[:, ["numParticles"]] = df_relevant.loc[:, ["numParticles"]].astype(float)
df_relevant.loc[:, ["simulationTime"]] = df_relevant.loc[:, ["simulationTime"]].astype(float)
df_relevant.loc[:, ["dataCollectionTime"]] = df_relevant.loc[:, ["dataCollectionTime"]].astype(float)
df_relevant.loc[:, ["k_a"]] = df_relevant.loc[:, ["k_a"]].astype(float)
df_relevant.loc[:, ["a_a"]] = df_relevant.loc[:, ["a_a"]].astype(float)
df_relevant.loc[:, ["k_b"]] = df_relevant.loc[:, ["k_b"]].astype(float)
df_relevant.loc[:, ["a_b"]] = df_relevant.loc[:, ["a_b"]].astype(float)
df_relevant.loc[:, ["maxWidth"]] = df_relevant.loc[:, ["maxWidth"]].astype(float)
df_relevant["utility_bins"] = pd.cut(df_relevant['utility'], 11)
df_relevant
# %%
df_pgws = df_relevant[df_relevant.widener == "ProgressiveWideningStrategy"]
df_mwws = df_relevant[df_relevant.widener == "MaxWidthWideningStrategy"]
# %%


def mode(x):
    return x.value_counts().index[0]


def get_top_k(df, k=50):
    param_grp = ['mean', 'median', 'min', 'max']
    result_df = df.groupby(["name"]).agg({
        'utility': ['mean', 'median', 'sum'],
        'session': ['count'],
        'numParticles': param_grp,
        'simulationTime': param_grp,
        'dataCollectionTime': param_grp,
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
sns.boxplot(data=df_pgws.sort_values("vs"), x="vs", y="utility", ax=ax1)
sns.boxplot(data=df_mwws.sort_values("vs"), x="vs", y="utility", ax=ax2)
ax1.set_title("ProgessiveWidening")
ax2.set_title("MaxWidthWidening")
for ax in [ax1, ax2]:
    for tick in ax.get_xticklabels():
        tick.set_rotation(45)
    ax.set_xlabel("Opponents")
    ax.set_ylabel("Utility")
# %%


# %%
def plot_param(df, ax, divider_id):
    # df = df[df["agreement"]==1]
    ax = sns.boxplot(data=df, x=divider_id, y="utility", ax=ax)
    for tick in ax.get_xticklabels():
        tick.set_rotation(45)
    ax.set_xlabel(divider_id)
    ax.set_ylabel("Utility")


fig, all_axes = plt.subplots(3, 2, figsize=(18, 15))
axes = all_axes.flatten()

divider_id = "simulationTime"
ax = axes[0]
plot_param(df_mwws, ax, divider_id)

divider_id = "simulationTime"
ax = axes[1]
plot_param(df_pgws, ax, divider_id)

divider_id = "dataCollectionTime"
ax = axes[2]
plot_param(df_mwws, ax, divider_id)

divider_id = "dataCollectionTime"
ax = axes[3]
plot_param(df_pgws, ax, divider_id)

divider_id = "numParticles"
ax = axes[4]
plot_param(df_mwws, ax, divider_id)

divider_id = "numParticles"
ax = axes[5]
plot_param(df_pgws, ax, divider_id)


# %%
def plot_my_util_vs_opponents(df, ax):
    ax = sns.boxplot(data=df.sort_values("vs"), x="vs", y="utility", ax=ax)
    for tick in ax.get_xticklabels():
        tick.set_rotation(30)
    ax.set_xlabel("By Opponent")
    ax.set_ylabel("Own Utility")


def plot_opponents_util_vs_me(df, ax):
    ax = sns.boxplot(data=df.sort_values("vs"), x="vs", y="vs_utility", ax=ax)
    for tick in ax.get_xticklabels():
        tick.set_rotation(30)
    ax.set_xlabel("By Opponent")
    ax.set_ylabel("Opponent Utility")


fig, all_axes = plt.subplots(2, 3, figsize=(18, 15))
axes = all_axes.flatten()

ax = axes[0]
plot_my_util_vs_opponents(df_relevant, ax)
ax = axes[1]
plot_my_util_vs_opponents(df_mwws, ax)
ax = axes[2]
plot_my_util_vs_opponents(df_pgws, ax)
ax = axes[3]
plot_opponents_util_vs_me(df_relevant, ax)
ax = axes[4]
plot_opponents_util_vs_me(df_mwws, ax)
ax = axes[5]
plot_opponents_util_vs_me(df_pgws, ax)

fig.tight_layout()
plt.show()

# %%
aggregator = ["evaluator"]
agg_mapper = {
    'utility': ['mean', 'median', 'sum'],
    'session': ['count'],
    'agreement': ['mean', 'sum'],
}

print("df_relevant: ")
df_relevant.groupby(["widener", "vs"] + aggregator).agg(agg_mapper)
# %%
print("df_pgws: ")
display(df_pgws.groupby(["widener", "vs"] + aggregator).agg(agg_mapper))
# %%
print("df_mwws: ")
display(df_mwws.groupby(["widener", "vs"] + aggregator).agg(agg_mapper))


# %%
def explore_configs(df, ax, aggregator1, aggregator2):
    agg_mapper = {
        'utility': ['mean', 'median', 'sum'],
        'session': ['count'],
        'agreement': ['mean', 'sum'],
    }
    tmp_df = df.groupby(["widener"] + aggregator2 + aggregator1).agg(agg_mapper).reset_index()
    tmp_df.columns = tmp_df.columns.map('_'.join).str.strip('_')
    ax = sns.scatterplot(
        data=tmp_df,
        x=aggregator1[0],
        y=aggregator2[0],
        size="utility_mean",
        hue="widener",
        alpha=0.7,
        legend=True,
        ax=ax,
        sizes=(100, 1000),
    )
    for tick in ax.get_xticklabels():
        tick.set_rotation(30)
    ax.set_xlabel("By Opponent")
    ax.set_ylabel("Opponent Utility")
    ax.legend(bbox_to_anchor=(1.05, 1), loc=2, borderaxespad=0.)


aggregator1 = ["beliefer"]
aggregator2 = ["vs"]
fig = plt.figure(figsize=(10, 5))
ax = plt.gca()
explore_configs(df_relevant, ax, aggregator1, aggregator2)
plt.show()
# %%
# fig = plt.figure(figsize=(10, 5))
# ax = plt.gca()
# aggregator1 = ["evaluator"]
# aggregator2 = ["vs"]
# agg_mapper = {
#     'utility': ['mean'],
#     'simulationTime': ['mean'],
#     'dataCollectionTime': ['mean'],
#     'numParticles': ['mean'],
#     'session': ['count'],
#     'agreement': ['mean'],
# }
# tmp_df = df_mwws.copy()
# tmp_df = tmp_df.groupby([
#     "widener",
#     "evaluator",
#     "explorer",
#     "comparer",
#     "beliefer",
#     # "simulationTime",
#     # "numParticles",
#     "vs",
# ]).agg(agg_mapper).reset_index()
# tmp_df.columns = tmp_df.columns.map('_'.join).str.strip('_')
# display(tmp_df)
# ax = sns.scatterplot(
#     data=df_mwws[(df_mwws.vs == "Hardliner") & (df_mwws.agreement == 1)],
#     x="dataCollectionTime",
#     y="utility",
#     hue="simulationTime",
#     # hue="maxWidth",
#     alpha=0.7,
#     legend=True,
#     ax=ax,
#     # sizes=(100, 1000),
# )
# for tick in ax.get_xticklabels():
#     tick.set_rotation(30)
# # ax.set_xlabel("By Opponent")
# # ax.set_ylabel("Opponent Utility")
# ax.legend(bbox_to_anchor=(1.05, 1), loc=2, borderaxespad=0.)
# plt.show()
# %%
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
    "dataCollectionTime",
    # "vs",
    # "profile",
    # "agreement",
]
# %%
fig = plt.figure(figsize=(15, 10))
df_tmp = df_pgws
# df_tmp = df_tmp[df_tmp["utility"] > 0]
min_max_scaler = preprocessing.MinMaxScaler()
y = df_tmp["utility"]
X = df_tmp[cols_conf + cols_pgws + cols_univ]
X = pd.get_dummies(X, drop_first=False)
ax = sns.heatmap(X.corr())
plt.show()
# %%
from rulefit import RuleFit
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn import preprocessing
from sklearn import metrics
# %%
df_tmp = df_mwws
# df_tmp = df_tmp[df_tmp["utility"] > 0]
min_max_scaler = preprocessing.MinMaxScaler()
y = df_tmp["utility"]
X = df_tmp[cols_conf + cols_mwws + cols_univ]
X = pd.get_dummies(X, drop_first=False)
# X.loc[:,:] = min_max_scaler.fit_transform(X)

# https://towardsdatascience.com/interpretable-machine-learning-in-10-minutes-with-rulefit-and-scikit-learn-da9ebb925795
rulefit = RuleFit(tree_generator=GradientBoostingRegressor(learning_rate=0.001, n_estimators=50))
rulefit.fit(X.values, y, feature_names=X.columns)
rulefit_preds = rulefit.predict(X.values)
rulefit_rmse = metrics.r2_score(y, rulefit_preds)
rules_mwws = rulefit.get_rules()
print(rulefit_rmse)
# %%
df_tmp = df_pgws
# df_tmp = df_tmp[df_tmp["utility"] > 0]
min_max_scaler = preprocessing.MinMaxScaler()
y = df_tmp["utility"]
X = df_tmp[cols_conf + cols_pgws + cols_univ]
X = pd.get_dummies(X, drop_first=False)
# X.loc[:,:] = min_max_scaler.fit_transform(X)

# https://towardsdatascience.com/interpretable-machine-learning-in-10-minutes-with-rulefit-and-scikit-learn-da9ebb925795
rulefit = RuleFit(tree_generator=GradientBoostingRegressor(learning_rate=0.001, n_estimators=50))
rulefit.fit(X.values, y, feature_names=X.columns)
rulefit_preds = rulefit.predict(X.values)
rulefit_rmse = metrics.r2_score(y, rulefit_preds)
rules_pgws = rulefit.get_rules()
print(rulefit_rmse)
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
X = pd.get_dummies(X, drop_first=False)
model = xgboost.XGBRegressor(max_depth=3).fit(X.values, y)
# model = GradientBoostingRegressor(learning_rate=1, n_estimators=50).fit(X.values, y)
rulefit_rmse = metrics.r2_score(y, model.predict(X.values))
print(rulefit_rmse)
explainer = shap.Explainer(model)
shap_values = explainer(X)
shap.summary_plot(shap_values, X)
# %%
df_tmp = df_mwws
df_tmp = df_tmp[df_tmp["utility"] > 0]
min_max_scaler = preprocessing.MinMaxScaler()
y = df_tmp["utility"]
X = df_tmp[cols_conf + cols_mwws + cols_univ]
X = pd.get_dummies(X, drop_first=False)
model = xgboost.XGBRegressor().fit(X.values, y)
# model = RandomForestRegressor(n_estimators=10).fit(X.values, y)
rulefit_rmse = metrics.r2_score(y, model.predict(X.values))
print(rulefit_rmse)
explainer = shap.Explainer(model)
shap_values = explainer(X)
shap.summary_plot(shap_values, X)

# %%
pd.set_option('display.max_colwidth', 400)  # Adjust row width to read the entire rule
pd.options.display.float_format = '{:.5f}'.format  # Round decimals to 2 decimal places
# rules = rulefit.get_rules()  # Get the rules
def plot_rule_bubbles(rules):
    rules = rules[rules['type'] != 'linear']  # Eliminate the existing explanatory variables
    rules = rules[rules['coef'] != 0]  # eliminate the insignificant rules
    rules = rules.sort_values('importance', ascending=False)  # Sort the rules based on "support" value
    # rules = rules[rules['rule'].str.len()>30] # optional: To see more complex rules, filter the long rules
    rules.iloc[0:20]  # Show the first 5 rules

    from adjustText import adjust_text
    fig = plt.figure(figsize=(15, 10))
    ax = sns.scatterplot(data=rules, x="coef", size="support", y="importance", alpha=0.5, legend=False, sizes=(20, 2000))
    TEXTS = []
    BG_WHITE = "#fbf9f4"
    GREY_LIGHT = "#b4aea9"
    GREY50 = "#7F7F7F"
    GREY30 = "#4d4d4d"
    topk_rules = rules.iloc[0:5]
    for i in range(len(topk_rules)):
        x = topk_rules["coef"].iloc[i]
        y = topk_rules["importance"].iloc[i]
        text = topk_rules["rule"].iloc[i]
        TEXTS.append(ax.text(x, y, text, color=GREY30, fontsize=10, fontname="Poppins"))

    adjust_text(TEXTS, expand_points=(2, 1), arrowprops=dict(arrowstyle="->", color=GREY50, lw=2), ax=fig.axes[0])
# legend = ax.legend(
#     loc=(0.85, 0.025), # bottom-right
#     labelspacing=1.5,  # add space between labels
#     markerscale=1.5,   # increase marker size
#     frameon=False      # don't put a frame
    # )
# %%
rules = rules_pgws  # Get the rules
plot_rule_bubbles(rules)
plt.show()
# %%
rules = rules_mwws  # Get the rules
plot_rule_bubbles(rules)
plt.show()
# %%
# %%
