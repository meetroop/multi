package com.testbed.appmaster.masteranalyser.model;

import com.google.gson.Gson;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectBOM {

  private String projName;
  private String scanDate;
  private List<Dependencies> bomData;

  public String toString() {
    return new Gson().toJson(this);
  }
}
