package com.testbed.appmaster.masteranalyser.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.testbed.appmaster.masteranalyser.apis.ApiAgent;
import com.testbed.appmaster.masteranalyser.apisImpl.HttpApiAgent;
import com.testbed.appmaster.masteranalyser.model.Dependencies;
import com.testbed.appmaster.masteranalyser.model.Deviations;
import com.testbed.appmaster.masteranalyser.model.EVIDENCE_CONFIDENCE;
import com.testbed.appmaster.masteranalyser.model.EVIDENCE_TYPE;
import com.testbed.appmaster.masteranalyser.model.Evidence;
import com.testbed.appmaster.masteranalyser.model.EvidenceData;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnalyserUtility {

  private static final String JAR_CHECK_SUM = "jarCheckSum";
  private static Map<String, String> registeredAnalysers = new HashMap<>();

  private static final String URL = "http://redisdbapi:8080/scaredis/getID";

  /** Regular expression to extract version numbers from file names. */
  private static final Pattern RX_VERSION =
      Pattern.compile(
          "\\d+(\\.\\d{1,6})+(\\.?([_-](release|beta|alpha|\\d+)[a-zA-Z_-]{1,3}\\d{0,8}|[a-zA-Z_-]{1,30}\\d{0,8}))?");

  /**
   * Regular expression to extract a single version number without periods. This is a last ditch
   * effort just to check in case we are missing a version number using the previous regex.
   */
  private static final Pattern RX_SINGLE_VERSION =
      Pattern.compile("\\d+(\\.?([_-](release|beta|alpha)|[a-zA-Z_-]{1,30}\\d{1,8}))?");

  /**
   * Regular expression to extract the part before the version numbers if there are any based on
   * RX_VERSION. In most cases, this part represents a more accurate name.
   */
  private static final Pattern RX_PRE_VERSION = Pattern.compile("^(.+)[_-](\\d+\\.\\d{1,6})+");

  public AnalyserUtility() {

    registerAnalysers();
  }

  private static void registerAnalysers() {

    /// take form system properties at prod
    registeredAnalysers.put("JAR", "http://aaa:8080/scajar/compute");
    registeredAnalysers.put("POM", "http://bbb:8080/scapom/compute");
  }

  public List<Dependencies> distribute(String absoluteFileName) {
    List<JsonObject> boms = new ArrayList<>();

    registeredAnalysers
        .keySet()
        .forEach(
            p -> {
              boms.add(callAnalyserAPI(p, absoluteFileName));
            });

    return collateBOMData(filterEmptyBomData(boms));
  }

  private List<JsonObject> filterEmptyBomData(List<JsonObject> bomData) {

    List<JsonObject> filteredData = new ArrayList<>();

    bomData.forEach(
        p -> {
          JsonObject jo = p.get("data").getAsJsonObject();
          if (jo != null && !jo.isJsonNull() && jo.size() != 0) {
            filteredData.add(p);
          } else {
            System.out.println("Filtered this BOM for Data missing : " + p.get("analyserType"));
          }
        });

    return filteredData;
  }

  private List<Dependencies> collateBOMData(List<JsonObject> boms) {

    Map<String, Dependencies> bomMap = new HashMap<>();

    boms.forEach(
        j -> {
          switch (j.get("analyserType").getAsString()) {
            case "JAR":
              collateJarBom(bomMap, j.get("data").getAsJsonObject());
              break;

            case "POM":
              collatePomBom(bomMap, j.get("data").getAsJsonObject());
              break;

            default:
              break;
          }
        });

    return new ArrayList<>(bomMap.values());
  }

  private void collatePomBom(Map<String, Dependencies> bom, JsonObject j) {

    JsonArray pDataArray = j.get("data").getAsJsonArray();

    // generally executed 1 time only
    pDataArray.forEach(
        p -> {
          JsonArray jsonPomData = p.getAsJsonObject().get("data").getAsJsonArray();

          // generally executed 1 time only
          jsonPomData.forEach(
              pom -> {
                String fileLocation = pom.getAsJsonObject().get("artifactLocation").getAsString();

                JsonArray pomMembers =
                    pom.getAsJsonObject().getAsJsonArray("members").getAsJsonArray();

                // generally executed multiple times
                pomMembers.forEach(
                    ext -> {

                      //					JsonObject extraData =
                      // ext.getAsJsonObject().get("extraDatas").getAsJsonArray().get(0)
                      //							.getAsJsonObject();

                      Dependencies deps = null;
                      String group = ext.getAsJsonObject().get("groupId").getAsString();
                      String productName = ext.getAsJsonObject().get("artifactId").getAsString();
                      String version = ext.getAsJsonObject().get("version").getAsString();
                      String ID = this.getID(group, productName);

                      String fileName = productName + "-" + version + ".jar";

                      if (bom.containsKey(ID)) {
                        deps = bom.get(ID);
                      } else if (bom.containsKey(createTEMPID(fileName))) {
                        deps = bom.get(createTEMPID(fileName));
                      } else {
                        deps = new Dependencies();
                        bom.put(ID, deps);
                      }

                      deps.setID(ID);

                      if (ext.getAsJsonObject().get("licenseName") != null) {
                        deps.setLicense(ext.getAsJsonObject().get("licenseName").getAsString());
                      }

                      // pkgURL used in  the OSS index later
                      deps.setPkgURL(getPackageURL(group, productName, version));

                      if (deps.getAbsoluteFileName() == null && fileLocation != null) {
                        deps.setAbsoluteFileName(fileLocation);
                      }

                      if (deps.getDependencyFileName() == null && fileLocation != null) {
                        deps.setDependencyFileName(fileName);
                      }

                      // Document the Evidence Used to deduce the POM Artifact
                      Evidence evd = deps.getEvidence();
                      if (evd == null) {
                        evd = new Evidence();
                        deps.setEvidence(evd);
                      }

                      Set<EvidenceData> data = evd.getData();
                      if (data == null) {
                        data = new HashSet<>();
                        evd.setData(data);
                      }

                      // 1-Evidence: Add POM based evidence for groupID and ArtifactID
                      // GroupID
                      EvidenceData evdData = new EvidenceData();

                      evdData.setType(EVIDENCE_TYPE.PRODUCT);
                      evdData.setSource("pom");
                      evdData.setEvidenceName("artifactId");
                      evdData.setActualEvidenceValue(productName);
                      evdData.setDeducedEvidenceValue(productName);
                      evdData.setConfidence(EVIDENCE_CONFIDENCE.HIGHEST);
                      deps.setDependencyName(productName);
                      evd.getData().add(evdData);

                      // Artifact_ID
                      evdData = new EvidenceData();
                      evdData.setType(EVIDENCE_TYPE.VENDOR);
                      evdData.setSource("pom");
                      evdData.setEvidenceName("groupId");
                      evdData.setActualEvidenceValue(group);
                      evdData.setDeducedEvidenceValue(getVendorFromPOMGroup(group));
                      evdData.setConfidence(EVIDENCE_CONFIDENCE.HIGH);
                      evd.getData().add(evdData);

                      // Version
                      evdData = new EvidenceData();
                      evdData.setType(EVIDENCE_TYPE.VERSION);
                      evdData.setSource("pom");
                      evdData.setEvidenceName("version");
                      evdData.setActualEvidenceValue(version);
                      evdData.setDeducedEvidenceValue(version);
                      evdData.setConfidence(EVIDENCE_CONFIDENCE.HIGHEST);
                      deps.setDependencyVersion(version);
                      evd.getData().add(evdData);
                    });
              });
        });
  }

  private String getPackageURL(String group, String productName, String version) {

    String packageURL = null;
    Base64.Encoder encoder = Base64.getEncoder();
    if (null != group && null != productName && null != version) {

      packageURL = "pkg:maven/" + group + "/" + productName + "@" + version;

      packageURL = encoder.encodeToString(packageURL.getBytes());
    }

    return packageURL;
  }

  private String getVendorFromPOMGroup(String group) {
    String vendor = null;
    if (group != null && (group.startsWith("org.") || group.startsWith("com."))) {

      String[] parts = group.split("\\.");
      vendor = parts[1];
    }
    return vendor;
  }

  private void collateJarBom(Map<String, Dependencies> bom, JsonObject j) {

    JsonArray jArray = j.get("deducedJarCompiled").getAsJsonArray();

    System.out.println("jar array received : " + jArray);

    jArray.forEach(
        jar -> {
          JsonObject jarObj = jar.getAsJsonObject();

          String ID = jarObj.get("ID") == null ? null : jarObj.get("ID").getAsString();

          if (ID == null || ID.isEmpty()) {
            ID = createTEMPID(jarObj.get("artifactFileName").getAsString());
          }

          Dependencies deps = null;

          if (bom.containsKey(ID)) {
            deps = bom.get(ID);
          } else {
            deps = new Dependencies();
            bom.put(ID, deps);
          }

          if (deps.getID() == null && !ID.startsWith(ResourceConstants.ID_PREFIX_TEMP)) {
            deps.setID(ID);
          }

          String dependencyFileName = jarObj.get("artifactFileName").getAsString();

          if (null != dependencyFileName && !dependencyFileName.isEmpty()) {
            deps.setDependencyFileName(dependencyFileName);
          }

          dependencyFileName = jarObj.get("artifactLocation").getAsString();

          if (null != dependencyFileName && !dependencyFileName.isEmpty()) {
            deps.setAbsoluteFileName(dependencyFileName);
          }

          // Document the Evidence Used to deduce the Jar Artifact
          Evidence evd = deps.getEvidence();
          if (evd == null) {
            evd = new Evidence();
            deps.setEvidence(evd);
          }

          Set<EvidenceData> data = evd.getData();
          if (data == null) {
            data = new HashSet<>();
            evd.setData(data);
          }

          // 1-Evidence: First Evidence is the file Name in the jar artifact.
          // The Jar May not be present in the Artifact DB

          String[] productAndVersion =
              getProductAndVersionFromFileName(jarObj.get("artifactFileName").getAsString());

          if (null != productAndVersion) {

            if (!"NONE".equalsIgnoreCase(productAndVersion[0])) {
              EvidenceData evdData = new EvidenceData();

              evdData.setType(EVIDENCE_TYPE.PRODUCT);
              evdData.setSource("file");
              evdData.setEvidenceName("name");
              evdData.setActualEvidenceValue(jarObj.get("artifactFileName").getAsString());
              evdData.setDeducedEvidenceValue(productAndVersion[0]);
              evdData.setConfidence(EVIDENCE_CONFIDENCE.LOW);

              evd.getData().add(evdData);
            }

            if (!"NONE".equalsIgnoreCase(productAndVersion[1])) {
              EvidenceData evdData = new EvidenceData();

              evdData.setType(EVIDENCE_TYPE.VERSION);
              evdData.setSource("file");
              evdData.setEvidenceName("name");
              evdData.setActualEvidenceValue(jarObj.get("artifactFileName").getAsString());
              evdData.setDeducedEvidenceValue(productAndVersion[1]);
              evdData.setConfidence(EVIDENCE_CONFIDENCE.LOW);

              evd.getData().add(evdData);
            }
          }

          // 2-Evidence: Is any CheckSum Used to deduce?
          JsonElement jsonEleCheckSUM = jarObj.get("checkSum");

          JsonArray jsonArrayCheckSUM = null;
          if (jsonEleCheckSUM != null) {
            jsonArrayCheckSUM = jsonEleCheckSUM.getAsJsonArray();
          }
          if (jsonArrayCheckSUM != null && jsonArrayCheckSUM.size() > 0) {

            final String[] jarName =
                jarObj.get("jarName").getAsString().split(ResourceConstants.NAME_SEPARATOR);

            for (JsonElement je : jsonArrayCheckSUM) {

              JsonObject jobj = je.getAsJsonObject();
              EvidenceData evdDataProd = new EvidenceData();

              String sigType = jobj.getAsJsonObject().get("signatureType").getAsString();
              if (sigType != null && JAR_CHECK_SUM.equalsIgnoreCase(sigType)) {
                deps.setCheckSumMD5(jobj.getAsJsonObject().get("signatureValue").getAsString());
              }

              evdDataProd.setType(EVIDENCE_TYPE.PRODUCT);
              evdDataProd.setSource("JAR");
              evdDataProd.setEvidenceName(
                  jobj.getAsJsonObject().get("signatureType").getAsString());
              evdDataProd.setActualEvidenceValue(
                  jobj.getAsJsonObject().get("signatureValue").getAsString());
              evdDataProd.setDeducedEvidenceValue(jarName[1]);
              evdDataProd.setConfidence(EVIDENCE_CONFIDENCE.HIGHEST);
              // set the HIGHEST product value to deps
              deps.setDependencyName(jarName[1]);
              // We know the list exist as it has already been created for the file Name
              evd.getData().add(evdDataProd);

              EvidenceData evdDataVer = new EvidenceData();
              evdDataVer.setType(EVIDENCE_TYPE.VERSION);
              evdDataVer.setSource("JAR");
              evdDataVer.setEvidenceName(jobj.getAsJsonObject().get("signatureType").getAsString());
              evdDataVer.setActualEvidenceValue(
                  jobj.getAsJsonObject().get("signatureValue").getAsString());
              evdDataVer.setDeducedEvidenceValue(jarName[2]);
              evdDataVer.setConfidence(EVIDENCE_CONFIDENCE.HIGHEST);
              deps.setDependencyVersion(jarName[2]);
              evd.getData().add(evdDataVer);
            }
          }

          // 3-Evidence: manifest Details captured
          JsonElement jsonEleManifest = jarObj.get("manifestData");
          JsonElement jsonEleManifestAttributes = null;
          JsonArray jsonArrayManifest = null;
          if (jsonEleManifest != null) {
            jsonEleManifestAttributes = jsonEleManifest.getAsJsonObject().get("manifestAtributes");
            if (jsonEleManifestAttributes != null) {
              jsonArrayManifest = jsonEleManifestAttributes.getAsJsonArray();
            }
          }
          if (jsonArrayManifest != null && jsonArrayManifest.size() > 0) {

            final List<EvidenceData> localList = new ArrayList<>();
            jsonArrayManifest.forEach(
                c -> {
                  EvidenceData evdDataManifest = new EvidenceData();
                  String manifestItem = c.getAsJsonObject().get("manifestItemName").getAsString();
                  String manifestValue = c.getAsJsonObject().get("manifestItemValue").getAsString();

                  switch (manifestItem) {
                    case "Implementation-Vendor":
                    case "Specification-Vendor":
                    case "Bundle-Vendor":
                      evdDataManifest.setType(EVIDENCE_TYPE.VENDOR);
                      evdDataManifest.setSource("Manifest");
                      evdDataManifest.setEvidenceName(manifestItem);
                      evdDataManifest.setActualEvidenceValue(manifestValue);
                      evdDataManifest.setDeducedEvidenceValue(manifestValue);
                      evdDataManifest.setConfidence(EVIDENCE_CONFIDENCE.HIGH);
                      localList.add(evdDataManifest);
                      break;
                    case "Implementation-Title":
                    case "Specification-Title":
                      evdDataManifest = new EvidenceData();
                      evdDataManifest.setType(EVIDENCE_TYPE.PRODUCT);
                      evdDataManifest.setSource("Manifest");
                      evdDataManifest.setEvidenceName(manifestItem);
                      evdDataManifest.setActualEvidenceValue(manifestValue);
                      evdDataManifest.setDeducedEvidenceValue(manifestValue);
                      evdDataManifest.setConfidence(EVIDENCE_CONFIDENCE.MEDIUM);
                      localList.add(evdDataManifest);
                      break;
                    case "Implementation-Version":
                    case "Specification-Version":
                    case "Bundle-Version":
                      evdDataManifest = new EvidenceData();
                      evdDataManifest.setType(EVIDENCE_TYPE.VERSION);
                      evdDataManifest.setSource("Manifest");
                      evdDataManifest.setEvidenceName(manifestItem);
                      evdDataManifest.setActualEvidenceValue(manifestValue);
                      evdDataManifest.setDeducedEvidenceValue(manifestValue);
                      evdDataManifest.setConfidence(EVIDENCE_CONFIDENCE.HIGH);
                      localList.add(evdDataManifest);
                      break;

                    default:
                      break;
                  }
                });

            evd.getData().addAll(localList);
          }

          // 4-Evidence: is there any deviation from the artifact
          boolean isMatched = jarObj.get("isMatch").getAsBoolean();

          if (!isMatched) {
            JsonObject deviatedClasses =
                jarObj.get("deviatedClasses") == null
                    ? null
                    : jarObj.get("deviatedClasses").getAsJsonObject();
            if (deviatedClasses != null && deviatedClasses.size() > 0) {
              List<Deviations> deviationList = deps.getDeviation();
              if (deviationList == null) {
                deviationList = new ArrayList<>();
                deps.setDeviation(deviationList);
              }
              final List<Deviations> localList = new ArrayList<>();
              deviatedClasses
                  .keySet()
                  .forEach(
                      dc -> {
                        // get the deviated classes as deviation objects
                        localList.add(new Deviations(dc, deviatedClasses.get(dc).getAsString()));
                      });

              deps.getDeviation().addAll(localList);
            }
          }
        });
  }

  private String[] getProductAndVersionFromFileName(String fileName) {

    fileName = fileName.substring(0, fileName.lastIndexOf('.'));

    String[] partArray = new String[2];
    String outcome = parsePreVersion(fileName);

    partArray[0] = (outcome == null) ? "NONE" : outcome;

    outcome = parseVersion(fileName);

    partArray[1] = (outcome == null) ? "NONE" : outcome;

    return partArray;
  }

  private String getID(String group, String product) {

    return (ResourceConstants.ID_PREFIX + group + ResourceConstants.NAME_SEPARATOR + product)
        .toLowerCase();
  }

  private String createTEMPID(String availableData) {

    String tempID =
        (ResourceConstants.ID_PREFIX_TEMP + ResourceConstants.NAME_SEPARATOR + availableData)
            .toLowerCase();

    return tempID;
  }

  public JsonObject callAnalyserAPI(String type, String absoluteFileName) {

    JsonObject jObj = new JsonObject();

    ApiAgent api = new HttpApiAgent(registeredAnalysers.get(type));

    System.out.println("Executing parser : " + type);

    try {

      jObj.add("data", api.execute(absoluteFileName));

    } catch (Exception e) {

      jObj.add(
          "data",
          new JsonParser().parse("Exception occured while analysing the json for :" + type));
    }

    JsonParser gparser = new JsonParser();
    jObj.add("analyserType", gparser.parse(type));

    return jObj;
  }

  private static String sanitiseData(String str) {

    return str.replaceAll("[^\\dA-Za-z_-]", "_");
  }

  /**
   * A utility class to extract the part before version numbers from file names (or other strings
   * containing version numbers. In most cases, this part represents a more accurate name than the
   * full file name.
   *
   * <pre>
   * Example:
   * Give the file name: library-name-1.4.1r2-release.jar
   * This function would return: library-name</pre>
   *
   * @param text the text being analyzed
   * @return the part before the version numbers if any, otherwise return the text itself.
   */
  public static String parsePreVersion(String text) {
    if (parseVersion(text) == null) {
      return text;
    }

    final Matcher matcher = RX_PRE_VERSION.matcher(text);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return text;
  }

  /**
   * A utility class to extract version numbers from file names (or other strings containing version
   * numbers.
   *
   * <pre>
   * Example:
   * Give the file name: library-name-1.4.1r2-release.jar
   * This function would return: 1.4.1.r2</pre>
   *
   * @param text the text being analyzed
   * @return a DependencyVersion containing the version
   */
  public static String parseVersion(String fileName) {

    String version = null;
    Matcher matcher = RX_VERSION.matcher(fileName);
    if (matcher.find()) {
      version = matcher.group();
    }
    // throw away the results if there are two things that look like version numbers
    if (matcher.find()) {
      return null;
    }
    if (version == null) {
      matcher = RX_SINGLE_VERSION.matcher(fileName);
      if (matcher.find()) {
        version = matcher.group();
      } else {
        return null;
      }
    }
    if (version != null && version.endsWith("-py2") && version.length() > 4) {
      version = version.substring(0, version.length() - 4);
    }
    return version;
  }
}
