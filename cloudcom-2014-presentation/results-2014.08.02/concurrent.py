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

from threading import Thread,Lock
import random

import bench_base

import matplotlib.pyplot as plt
from matplotlib import rc
rc('font',**{'family':'sans-serif','sans-serif':['Helvetica']})
rc('text', usetex=True)

timesToRun = 4
def runConcurrentExperiment(name, data_dir):
  global timesToRun
  print("Running concurrent experiment for '" + name + "'.")
  allConcurrentQueries = list(range(1,9))
  cores = 143
  memoryPerWorker = "20g"
  tps = 1500000
  def isDataFinished(concurrentQueries,d):
    if not d or not isinstance(d,list) or len(d)<concurrentQueries:
      return False
    for thread in d:
      if len(thread) < timesToRun: return False
    return True
  def run(concurrentQueries):
    g_lock = Lock()
    def threadEntry(threadNum):
      def isFinished():
        with g_lock:
          for n in results:
            if len(n) < timesToRun: return False
          return True
      try:
        while not isFinished():
          print(str(threadNum) + ": Calling query.")
          result = bench_base.runQuery(name, "2014-01-01", "2014-01-07", True)
          queryExecutionTime = result[2]['TimeMillis']-result[0]['TimeMillis']
          print(str(threadNum) + ": Query execution time: " +
              str(queryExecutionTime))
          with g_lock:
            results[threadNum].append(queryExecutionTime)
      except:
        print("Error occurred in thread.")
        traceback.print_exc()
    results = [[] for x in range(0,concurrentQueries)]
    threads = [
      Thread(target=threadEntry, args=(i,)) for i in range(0,concurrentQueries)
    ]
    [t.start() for t in threads]
    [t.join() for t in threads]
    return results

  outFilePath = data_dir + "/concurrent/" + name + ".yaml"
  if os.path.isfile(outFilePath):
    with open(outFilePath, "r") as f: data = yaml.load(f)
  else: data = {}

  for concurrentQueries in allConcurrentQueries:
    if concurrentQueries in data and \
        isDataFinished(concurrentQueries,data[concurrentQueries]):
      print("  Already profiled for " + str(concurrentQueries) +
          " concurrent queries, skipping.")
      continue
    else:
      data[concurrentQueries] = {}
    while not isDataFinished(concurrentQueries,data[concurrentQueries]):
      try:
        bench_base.restartServers()
        bench_base.restartSparkContext(memoryPerWorker, cores)

        # For cache.
        bench_base.runQuery(name, "2014-01-01", "2014-01-07", True, tps)

        data[concurrentQueries] = run(concurrentQueries)

        with open(outFilePath, "w") as f:
          f.write(yaml.dump(data, indent=2, default_flow_style=False))
      except KeyboardInterrupt: sys.exit(-1)
      except Exception:
        print("Exception occurred, retrying.")
        traceback.print_exc()
        data[concurrentQueries] = {}
        pass
  return data

def getStats(data):
  global timesToRun
  x = []; y = []; err = []
  sortedKeys = sorted(data)
  minX = sortedKeys[0]; maxX = sortedKeys[-1]
  minY = data[minX][0][0]/1000; maxY = minY
  for concurrentQueries in sortedKeys:
    x.append(concurrentQueries)
    allTimes = []
    for thread in data[concurrentQueries]:
      allTimes += thread[0:timesToRun]
    allTimes = [x/1000 for x in allTimes]
    m_data = statistics.mean(allTimes)
    if m_data < minY: minY = m_data
    if m_data > maxY: maxY = m_data
    y.append(m_data)
    err.append(statistics.stdev(allTimes))
  return (x,y,err,minX,maxX,minY,maxY)

def plotConcurrent(query, data, data_dir):
  fig = plt.figure()
  ax = plt.subplot(111)
  plt.title(query)
  plt.xlabel("Concurrent Queries")
  plt.ylabel("Execution Time (s)")
  (x, y, err, minX, maxX, minY, maxY) = getStats(data)
  plt.errorbar(x, y, yerr=err, marker='.', color="black", ecolor="gray")
  plt.axis([minX-1, maxX+1, 0, 1.02*(maxY+max(err))])
  leg = ax.legend(
    ["Caching. 1.5M target partition size. 6 workers."],
    fancybox=True
  )
  leg.get_frame().set_alpha(0.5)
  # plt.grid()
  plt.savefig(data_dir + "/concurrent/pdf/" + query + ".pdf")
  plt.savefig(data_dir + "/concurrent/png/" + query + ".png")
  plt.clf()

  # Print stats.
  def two(s): return "{:.2f}".format(s)
  print(" & ".join([query, two(y[0]), two(y[1]/y[0]), two(y[7]/y[0])]) + r" \\")

parser = argparse.ArgumentParser()
parser.add_argument("--collect-data", dest="collect", action="store_true")
parser.add_argument("--create-plots", dest="plot", action="store_true")
parser.add_argument("--data-dir", dest="data_dir", type=str, default=".")
args = parser.parse_args()

queries = [
  "Pageviews",
  "Revenue",
  "RevenueFromTopReferringDomains",
  "RevenueFromTopReferringDomainsFirstVisitGoogle",
  "TopPages",
  "TopPagesByBrowser",
  "TopPagesByPreviousTopPages",
  "TopReferringDomains",
]
if args.collect:
  if not os.path.isdir(args.data_dir + "/concurrent"):
    os.makedirs(args.data_dir + "/concurrent")
  for query in queries:
    runConcurrentExperiment(query, args.data_dir)

if args.plot:
  print(" & ".join(
    ["Query","Serial Time (ms)","2 Concurrent Slowdown","8 Concurrent Slowdown"]
  ) + r" \\ \hline")
  if not os.path.isdir(args.data_dir + "/concurrent/pdf"):
    os.makedirs(args.data_dir + "/concurrent/pdf")
  if not os.path.isdir(args.data_dir + "/concurrent/png"):
    os.makedirs(args.data_dir + "/concurrent/png")
  for query in queries:
    with open(args.data_dir + "/concurrent/" + query + ".yaml", "r") as f:
      data = yaml.load(f)

    plotConcurrent(query, data, args.data_dir)
