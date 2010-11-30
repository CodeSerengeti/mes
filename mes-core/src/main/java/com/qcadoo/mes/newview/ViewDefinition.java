package com.qcadoo.mes.newview;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

public interface ViewDefinition {

    String getName();

    String getPluginIdentifier();

    Map<String, Object> prepareView(Locale locale);

    JSONObject performEvent(JSONObject object, Locale locale) throws JSONException;

    ComponentPattern getComponentByPath(String path);

    Map<String, ComponentPattern> getChildren();

    ComponentPattern getChild(String name);

    boolean isMenuAccessible();

    Set<String> getJavaScriptFilePaths();

}
