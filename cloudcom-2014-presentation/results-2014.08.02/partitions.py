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

def runPartitionExperiment(name, data_dir):
  print("Running partition experiment for '" + name + "'.")
  targetPartitionSizes = [10000, 50000, 100000, 200000,
      300000, 400000, 500000, 750000, 1000000, 1500000]
  memoryPerWorker = "21g"
  cores = 143
  timesToRun = 4
  cache = True

  outFilePath = data_dir + "/partitions/" + name + ".yaml"
  if os.path.isfile(outFilePath):
    with open(outFilePath, "r") as f: data = yaml.load(f)
  else: data = {}

  for tps in targetPartitionSizes:
    if tps not in data: data[tps] = []
    if len(data[tps]) >= timesToRun:
      print("  Already profiled for " + str(tps) +
          " targetPartitionSizes, skipping.")
      continue
    while len(data[tps]) < timesToRun:
      try:
        bench_base.restartServers()
        bench_base.restartSparkContext(memoryPerWorker, cores)
        # Load the data into cache.
        if cache: bench_base.runQuery(name,"2014-01-01","2014-01-07",cache,tps)
        while len(data[tps]) < timesToRun:
          result = bench_base.runQuery(name,"2014-01-01","2014-01-07",cache,tps)
          data[tps].append(result[2]['TimeMillis'] - result[0]['TimeMillis'])
        with open(outFilePath, "w") as f:
          f.write(yaml.dump(data, indent=2, default_flow_style=False))
      except KeyboardInterrupt: sys.exit(-1)
      except Exception:
        print("Exception occurred, retrying.")
        traceback.print_exc()
        if not cache: data[tps] = []
        pass
  return data

def getStats(data):
  x = []; y = []; err = []
  sortedKeys = sorted(data)
  minX = sortedKeys[0]; maxX = sortedKeys[-1]
  minY = statistics.mean(data[minX])/1000; maxY = minY
  for tps in sortedKeys:
    data[tps] = [x/1000 for x in data[tps]]
    x.append(tps)
    m_data = statistics.mean(data[tps])
    if m_data < minY: minY = m_data
    if m_data > maxY: maxY = m_data
    y.append(m_data)
    err.append(statistics.stdev(data[tps]))
  return (x,y,err,minX,maxX,minY,maxY)

def plotSpeedups(query, data, data_dir):
  fig = plt.figure()
  ax = plt.subplot(111)
  plt.title(query)
  plt.xlabel("Target Partition Sizes")
  plt.ylabel("Execution Time (s)")
  (x, y, err, minX, maxX, minY, maxY) = getStats(data)
  plt.errorbar(x, y, yerr=err, marker='.', color='black',ecolor="gray")
  plt.axis([0, 1.02*maxX, 0, 1.02*(maxY+max(err))])
  leg = ax.legend(["Caching. 6 workers."], fancybox=True)
  leg.get_frame().set_alpha(0.5)
  # plt.grid()

  # Add annotations for the endpoint values.
  # ax.annotate("("+str(x[0])+","+str(y[0])+")", xy=(x[0],y[0]), xytext=(0,0),
  #     textcoords='offset points', color='black')
  # ax.annotate(str(y[1]), xy=(x[1],y[1]), xytext=(0,0),
  #     textcoords='offset points', color='black')
  ax.annotate(str(y[-1]), xy=(x[-1],y[-1]), xytext=(10,0),
      textcoords='offset points', color='black')

  plt.savefig(data_dir + "/partitions/pdf/" + query + ".pdf")
  plt.savefig(data_dir + "/partitions/png/" + query + ".png")
  plt.clf()

  # Print stats.
  def two(s): return "{:.2f}".format(s)
  print(" & ".join([query, two(min(y)), two(y[-1])]) + r" \\")

parser = argparse.ArgumentParser()
parser.add_argument("--collect-data", dest="collect", action="store_true")
parser.add_argument("--create-plots", dest="plot", action="store_true")
parser.add_argument("--data-dir", dest="data_dir", type=str, default=".")
args = parser.parse_args()

queriesWithPartitioning = [
  "RevenueFromTopReferringDomains",
  "RevenueFromTopReferringDomainsFirstVisitGoogle",
  "TopPages",
  "TopPagesByBrowser",
  "TopPagesByPreviousTopPages",
  "TopReferringDomains"
]
if args.collect:
  if not os.path.isdir(args.data_dir + "/partitions"):
    os.makedirs(args.data_dir + "/partitions")
  for query in queriesWithPartitioning:
    runPartitionExperiment(query, args.data_dir)

if args.plot:
  print(" & ".join(["Query","Best Execution Time","Final Execution Time"]) +
    r" \\ \hline")
  if not os.path.isdir(args.data_dir + "/partitions/pdf"):
    os.makedirs(args.data_dir + "/partitions/pdf")
  if not os.path.isdir(args.data_dir + "/partitions/png"):
    os.makedirs(args.data_dir + "/partitions/png")
  for query in queriesWithPartitioning:
    with open(args.data_dir+"/partitions/"+query+".yaml", "r") as f:
      data = yaml.load(f)

    plotSpeedups(query, data, args.data_dir)
