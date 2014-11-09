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

import matplotlib.pyplot as plt
from matplotlib import rc
rc('font',**{'family':'sans-serif','sans-serif':['Helvetica']})
rc('text', usetex=True)

scHost = "localhost"
scPort = 8605

class cd:
  def __init__(self, newPath): self.newPath = newPath
  def __enter__(self): self.savedPath = os.getcwd(); os.chdir(self.newPath)
  def __exit__(self, etype, value, traceback): os.chdir(self.savedPath)

def static_var(varname, value):
  def decorate(func):
    setattr(func, varname, value)
    return func
  return decorate

@static_var("hits", 0)
def restartServers():
  global scPort
  print("Restarting server.")
  restartServers.hits += 1

  print("---Forwarding ports.")
  def forward():
    os.system("ssh -N -L 9090:localhost:8080 " +
        "-L 50070:localhost:50070 rnd20 echo")
    os.system("ssh -L 8600:localhost:8585 rnd20 echo > /dev/null")
    os.system("ssh -L 8601:localhost:8585 rnd21 echo > /dev/null")
    os.system("ssh -L 8602:localhost:8585 rnd22 echo > /dev/null")
    os.system("ssh -L 8603:localhost:8585 rnd23 echo > /dev/null")
    os.system("ssh -L 8604:localhost:8585 rnd24 echo > /dev/null")
    os.system("ssh -L 8605:localhost:8585 rnd25 echo > /dev/null")
  forward()

  print("---Killing running application.")
  try:
    with cd("/Users/amos/sitecatalyst-spark"):
      p = Popen(["fab", "kill"], stdout=PIPE, stderr=PIPE)
      p.communicate()
  except: pass

  time.sleep(10)

  print("---Checking for running Spark application.")
  try:
    p1 = Popen(['curl', "http://localhost:9090/"], stdout=PIPE)
    p2 = Popen(['grep', 'Running Drivers', '-A', '10'],
        stdin=p1.stdout, stdout=PIPE)
    p3 = Popen(['grep', '<tr>'], stdin=p2.stdout, stdout=PIPE)
    p1.stdout.close(); p2.stdout.close()
    out=p3.communicate()[0].strip()
    print(out)
    restart = len(out) > 0
  except:
    traceback.print_exc()
    restart = True
  if restart:
    print("---Restarting Spark.")
    with cd("/Users/amos/spark-deploy"):
      p = Popen(["fab", "stopSparkWorkers", "stopSparkMaster"],
          stdout=PIPE, stderr=PIPE)
      out = p.communicate()
      print(out)
      p = Popen(["fab", "startSparkWorkers", "startSparkMaster"],
          stdout=PIPE, stderr=PIPE)
      out = p.communicate()
      print(out)
  else:
    print("---Not restarting Spark.")

  time.sleep(20)
  forward()
  print("---Starting SiteCatalyst query engine.")
  with cd("/Users/amos/sitecatalyst-spark"):
    p = Popen(["fab", "sync", "start"], stdout=PIPE, stderr=PIPE)
    out = [s.decode() for s in p.communicate()]
    print(out)
    port = re.search("DriverServer: rnd2(\d)\.or1", out[0]).group(1)
    scPort = int("860"+port)
    print(scPort)
  time.sleep(20)

def requestURL(req):
  try:
    return urlopen(req).read().decode()
  except:
    print(req)
    raise

def runQuery(name, start, finish, cache=False):
  global scPort
  print("Running " + name + " with cache=" + str(cache))
  request = "http://" + scHost + ":" + str(scPort) + "/query?name=" + name + \
    "&start_ymd=" + start + "&finish_ymd=" + finish + "&profile=true"
  if cache: request += "&cache=true"
  result = yaml.load(requestURL(request))
  print("  Result: " + str(result[2]['TimeMillis'] - result[0]['TimeMillis']))
  return result

def restartSparkContext(memory, cores):
  global scPort
  print("Restarting with memory=" + memory + " and cores=" + str(cores))
  request = "http://" + scHost + ":" + str(scPort) + \
    "/restartSparkContext?memory=" + memory + "&cores=" + str(cores)
  result = requestURL(request)
  return result

def runSpeedupExperiment(name, cache, data_dir):
  print("Running speedup experiment for '" + name + "'.")
  allCores = [1,2,4,8] + list(range(8,96+8,8))
  memoryPerWorker = "22g"
  timesToRun = 4

  if cache: outFilePath = data_dir + "/speedups-cache-" + name + ".yaml"
  else: outFilePath = data_dir + "/speedups-nocache-" + name + ".yaml"
  if os.path.isfile(outFilePath):
    with open(outFilePath, "r") as f: data = yaml.load(f)
  else: data = {}

  for cores in allCores:
    if cores not in data: data[cores] = []
    if len(data[cores]) >= timesToRun:
      print("  Already profiled for " + str(cores) + " cores, skipping.")
      continue
    while len(data[cores]) < timesToRun:
      try:
        restartServers()
        restartSparkContext(memoryPerWorker, cores)
        # Load the data into cache.
        if cache: runQuery(name, "2014-01-01", "2014-01-07", cache)
        while len(data[cores]) < timesToRun:
          result = runQuery(name, "2014-01-01", "2014-01-07", cache)
          data[cores].append(result[2]['TimeMillis'] - result[0]['TimeMillis'])
        with open(outFilePath, "w") as f:
          f.write(yaml.dump(data, indent=2, default_flow_style=False))
      except KeyboardInterrupt: sys.exit(-1)
      except Exception:
        print("Exception occurred, retrying.")
        traceback.print_exc()
        if not cache: data[cores] = []
        pass
  return data

def getStats(data):
  x = []; y = []; err = []
  sortedKeys = sorted(data)
  minX = sortedKeys[0]; maxX = sortedKeys[-1]
  minY = statistics.mean(data[minX]); maxY = minY
  for cores in sortedKeys:
    x.append(cores)
    m_data = statistics.mean(data[cores])
    if m_data < minY: minY = m_data
    if m_data > maxY: maxY = m_data
    y.append(m_data)
    err.append(statistics.stdev(data[cores]))
  return (x,y,err,minX,maxX,minY,maxY)

def plotSpeedups(query, data_cache, data_nocache, data_dir):
  fig = plt.figure()
  ax = plt.subplot(111)
  plt.title(query)
  plt.xlabel("Cores")
  plt.ylabel("Execution Time (ms)")
  (x_n, y_n, err_n, minX_n, maxX_n, minY_n, maxY_n) = getStats(data_nocache)
  plt.errorbar(x_n, y_n, yerr=err_n, marker='.', ecolor="gray")
  (x_c, y_c, err_c, minX_c, maxX_c, minY_c, maxY_c) = getStats(data_cache)
  plt.errorbar(x_c, y_c, yerr=err_c, marker='.', ecolor="gray")
  maxY = max(maxY_c, maxY_n)

  diff = abs(y_n[-1]-y_c[-1])/maxY
  if diff < 0.039:
    offset = 7
    if y_n[-1] < y_c[-1]: offset *= -1
  else: offset = 0

  # Add annotations for the endpoint values.
  ax.annotate(str(y_n[0]), xy=(x_n[0],y_n[0]), xytext=(10,0),
      textcoords='offset points', color='blue')
  ax.annotate(str(y_n[-1]), xy=(x_n[-1],y_n[-1]), xytext=(10,offset),
      textcoords='offset points', color='blue')
  ax.annotate(str(y_c[0]), xy=(x_c[0],y_c[0]), xytext=(10,0),
      textcoords='offset points', color='green')
  ax.annotate(str(y_c[-1]), xy=(x_c[-1],y_c[-1]), xytext=(10,-offset),
      textcoords='offset points', color='green')

  # Scale the axis to include error bars.
  plt.axis([minX_n-1, maxX_n+1, 0, 1.02*(maxY+max(err_c + err_n))])
  leg = ax.legend(["Without data caching", "With data caching"], fancybox=True)
  leg.get_frame().set_alpha(0.5)
  plt.grid()
  plt.savefig(data_dir + "/speedups-" + query + ".pdf")
  plt.savefig(data_dir + "/speedups-" + query + ".png")
  plt.clf()

  # Print stats.
  def two(s): return "{:.2f}".format(s)
  if y_c[-1] < y_n[-1]: speedup = two(y_n[-1]/y_c[-1])
  else: speedup = "n/a"
  print(" & ".join([query, two(y_n[-1]), two(y_c[-1]), speedup]) + r" \\")

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
  for query in queries:
    runSpeedupExperiment(query, False, args.data_dir)
    runSpeedupExperiment(query, True, args.data_dir)

if args.plot:
  print(" & ".join(["Query","No Caching","Caching","Caching Speedup"])+r" \\")
  for query in queries:
    with open(args.data_dir + "/speedups-nocache-" + query + ".yaml", "r") as f:
      data_nocache = yaml.load(f)
    with open(args.data_dir + "/speedups-cache-" + query + ".yaml", "r") as f:
      data_cache = yaml.load(f)

    plotSpeedups(query, data_cache, data_nocache, args.data_dir)
