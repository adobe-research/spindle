#!/usr/bin/env python3

import argparse
from urllib.request import urlopen
import yaml
import statistics
import os
import sys
import time
import re
from subprocess import Popen, PIPE
import traceback

import bench_base

import matplotlib.pyplot as plt
from matplotlib import rc
rc('font',**{'family':'sans-serif','sans-serif':['Helvetica']})
rc('text', usetex=True)

scHost = "localhost"
scPort = 8605

def runCachingExperiment(name, data_dir):
  print("Running caching experiment for '" + name + "'.")
  memoryPerWorker = "21g"
  cores = 143
  timesToRun = 4
  tps = 1500000

  outFilePath = data_dir + "/caching/" + name + ".yaml"
  if os.path.isfile(outFilePath):
    with open(outFilePath, "r") as f: data = yaml.load(f)
  else: data = {}

  for cache in [True,False]:
    if cache not in data: data[cache] = []
    if len(data[cache]) >= timesToRun:
      print("  Already profiled for cache = " + str(cache) + ", skipping.")
      continue
    while len(data[cache]) < timesToRun:
      try:
        bench_base.restartServers()
        bench_base.restartSparkContext(memoryPerWorker, cores)
        # Load the data into cache.
        if cache: bench_base.runQuery(name,"2014-01-01","2014-01-07",cache,tps)
        while len(data[cache]) < timesToRun:
          result = bench_base.runQuery(name,"2014-01-01","2014-01-07",cache,tps)
          data[cache].append(result[2]['TimeMillis'] - result[0]['TimeMillis'])
        with open(outFilePath, "w") as f:
          f.write(yaml.dump(data, indent=2, default_flow_style=False))
      except KeyboardInterrupt: sys.exit(-1)
      except Exception:
        print("Exception occurred, retrying.")
        traceback.print_exc()
        if not cache: data[cache] = []
        pass
  return data

def getStats(data):
  y_cache = []; err_cache = []
  y_nocache = []; err_nocache = []
  names = []; shortNames = []
  i = 0
  for query in sorted(data):
    y_cache.append(statistics.mean(data[query][True]))
    err_cache.append(statistics.stdev(data[query][True]))
    y_nocache.append(statistics.mean(data[query][False]))
    err_nocache.append(statistics.stdev(data[query][False]))
    names.append(query)
    shortNames.append("Q{}".format(i))
    i+=1
  return (y_cache, err_cache, y_nocache, err_nocache, names, shortNames)

def plotSpeedups(data, data_dir):
  bar_width = 0.35
  fig = plt.figure()
  ax = plt.subplot(111)
  plt.title("Speedups from Caching")
  plt.ylabel("Execution Time (ms)")
  (y_cache,err_cache,y_nocache,err_nocache,names,shortNames) = getStats(data)
  ind_c = range(len(y_cache))
  ind_n = [x+bar_width for x in ind_c]
  bar_c = ax.bar(ind_c, y_cache, bar_width, color='grey', yerr=err_cache,
      ecolor="#363636")
  bar_n = ax.bar(ind_n, y_nocache, bar_width, color='white', edgecolor="black",
      hatch="/", yerr=err_nocache, ecolor="#363636")
  # plt.errorbar(x, y, yerr=err, marker='.', color='black',ecolor="gray")
  # plt.axis([0, 1.02*maxX, 0, 1.02*(maxY+max(err))])
  # plt.grid()
  ax.set_xticks(ind_n)
  ax.set_xticklabels(shortNames)
  leg = ax.legend((bar_c, bar_n), ("Caching", "No Caching"),
    fancybox=True, loc="upper left")
  leg.get_frame().set_alpha(0.5)

  def autolabel(rects):
    # attach some text labels
    for rect in rects:
      height = rect.get_height()
      ax.text(rect.get_x()+rect.get_width(), height+10, '%d'%int(height),
        ha='right', va='bottom', size=9)
  autolabel(bar_c)
  autolabel(bar_n)

  plt.savefig(data_dir + "/caching/caching.pdf")
  plt.savefig(data_dir + "/caching/caching.png")
  plt.clf()

  print(r"Name & Short Name \\ \hline")
  for n,sn in zip(names,shortNames):
    print(r"{} & {} \\".format(n,sn))

  # Print stats.
  # def two(s): return "{:.2f}".format(s)
  # print(" & ".join([query, two(min(y)), two(y[-1])]) + r" \\")

parser = argparse.ArgumentParser()
parser.add_argument("--collect-data", dest="collect", action="store_true")
parser.add_argument("--create-plots", dest="plot", action="store_true")
parser.add_argument("--data-dir", dest="data_dir", type=str, default=".")
args = parser.parse_args()

queries = [
  "Pageviews",
  "TopPages",
  "TopPagesByBrowser",
  "TopPagesByPreviousTopPages",
  "Revenue",
  "TopReferringDomains",
  "RevenueFromTopReferringDomains",
  "RevenueFromTopReferringDomainsFirstVisitGoogle"
]
if args.collect:
  if not os.path.isdir(args.data_dir + "/caching"):
    os.makedirs(args.data_dir + "/caching")
  for query in queries:
    runCachingExperiment(query, args.data_dir)

if args.plot:
  print(" & ".join(["Query","Best Execution Time","Final Execution Time"]) +
    r" \\ \hline")
  data = {}
  for query in queries:
    with open(args.data_dir+"/caching/"+query+".yaml", "r") as f:
      data.update({query: yaml.load(f)})

  print(data)
  plotSpeedups(data, args.data_dir)
