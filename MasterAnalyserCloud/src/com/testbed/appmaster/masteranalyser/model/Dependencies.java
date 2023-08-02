package com.testbed.appmaster.masteranalyser.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Dependencies {

  String dependencyName;
  String dependencyVersion;
  String dependencyFileName;
  String absoluteFileName;
  String checkSumMD5;
  String license;
  String ID;
  String pkgURL;
  Evidence evidence;
  List<Deviations> deviation;
}
