/*
 Speclj Runner

 Usage:
 phantomjs resources/public/specs/speclj.js [auto]

 auto: will only run specs updated after the last run. (default: false)

 Each run produced/touches a timestamp file, .specljs-timestamp
 */

var nsPrefix = "c3kit.bucket"

var fs = require("fs");
var p = require('webpage').create();
var system = require('system');

String.prototype.endsWith = function (suffix) {
  return this.indexOf(suffix, this.length - suffix.length) !== -1;
};
String.prototype.startsWith = function (str) {
  return this.slice(0, str.length) == str;
};

p.onConsoleMessage = function (x) {
  fs.write("/dev/stdout", x + "\n", "w");
};

var timestampFile = ".specljs-timestamp";
writeTimestamp = function () {
  if(fs.lastModified(timestampFile) != null)
    fs.remove(timestampFile);
  fs.touch(timestampFile);
};

readTimestamp = function () {
  return fs.lastModified(timestampFile);
};

var autoMode = function () {
  return system.args[1] == "auto" && readTimestamp() != null;
};

// var logList = function (title, list) {
//   console.log(title + ":");
//   for(var i in list)
//     console.log(i);
// };

findUpdatedSpecs = function (rdeps, deps) {
  var minMillis = readTimestamp().getTime();
  var updated = {};
  for(var ns in rdeps) {
    var file = deps.idToPath_[ns];
    var path = file.substring(7);
    if(fs.lastModified(path).getTime() >= minMillis) {
      updated[ns] = true;
    }
  }
  return updated;
};

buildReverseDeps = function (deps) {
  var rdeps = {};
  for(var ns in deps.idToPath_) {
    if(ns.startsWith(nsPrefix)) {
      var file = deps.idToPath_[ns];
      var requires = deps.dependencies_[file].requires
      for(var i = 0; i < requires.length; i++) {
        var rdep = requires[i];
        if(rdep.startsWith(nsPrefix)) {
          if(!(rdep in rdeps)) {
            rdeps[rdep] = {}
          }
          rdeps[rdep][ns] = true;
        }
      }
      if(!(ns in rdeps)) {
        rdeps[ns] = {}
      }
    }
  }
  return rdeps;
};


reduceToSpecs = function (affected) {
  var result = {};
  for(var ns in affected) {
    if(ns.endsWith("_spec")) {
      result[ns.replace(/_/g, "-")] = true
    }
  }
  return result;
};

findAffectedSpecs = function () {
  var deps = p.evaluate(function () {
    return goog.debugLoader_;
  });
  var rdeps = buildReverseDeps(deps);
  var updated = findUpdatedSpecs(rdeps, deps);

  var result = {};

  var walkDeps = function (nses) {
    for(var ns in nses) {
      if(!(ns in result)) {
        result[ns] = true;
        walkDeps(rdeps[ns])
      }
    }
  };
  walkDeps(updated);

  return reduceToSpecs(result);
};

p.open("dev/specs/specs.html", function (status) {
  try {
    var specs = autoMode() ? findAffectedSpecs() : null;

    var result = p.evaluate(function (specSet) {
      return runSpecsPhantom(specSet);
    }, specs);

    writeTimestamp();
    phantom.exit(result);
  }
  catch(e) {
    console.error(e);
    phantom.exit(-1);
  }

});
