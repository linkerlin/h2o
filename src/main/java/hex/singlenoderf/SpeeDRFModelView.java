package hex.singlenoderf;

import water.DKV;
import water.Key;
import water.Request2;
import water.api.DocGen;
import water.api.Request;
import water.api.RequestArguments;
import water.api.RequestBuilders;


public class SpeeDRFModelView extends Request2 {
  static final int API_WEAVER = 1; // This file has auto-gen'd doc & json fields
  static public DocGen.FieldDoc[] DOC_FIELDS; // Initialized from Auto-Gen code.

  @Request.API(help="SpeeDRF Model Key", required = true, filter = GSKeyFilter.class)
  Key _modelKey;
  class GSKeyFilter extends RequestArguments.H2OKey { public GSKeyFilter() { super("",true); } }

  @Request.API(help="SpeeDRF Model")
  SpeeDRFModel speedrf_model;

  public static String link(String txt, Key model) {
    return "<a href='/2/SpeeDRFModelView.html?_modelKey=" + model + "'>" + txt + "</a>";
  }

  public static RequestBuilders.Response redirect(Request req, Key modelKey) {
    return RequestBuilders.Response.redirect(req, "/2/SpeeDRFModelView", "_modelKey", modelKey);
  }

  @Override public boolean toHTML(StringBuilder sb){
    speedrf_model.generateHTML("", sb);
    return true;
  }

  @Override protected RequestBuilders.Response serve() {
    speedrf_model = DKV.get(_modelKey).get();
    return RequestBuilders.Response.done(this);
  }
}