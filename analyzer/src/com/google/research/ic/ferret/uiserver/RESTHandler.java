/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.google.research.ic.ferret.uiserver;

import java.util.Date;
import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.research.ic.ferret.Config;
import com.google.research.ic.ferret.Session;
import com.google.research.ic.ferret.data.DemoManager;
import com.google.research.ic.ferret.data.FilterSpec;
import com.google.research.ic.ferret.data.FilteredResultSet;
import com.google.research.ic.ferret.data.FilteredResultSet.FilteredResultSummary;
import com.google.research.ic.ferret.data.LogLoader;
import com.google.research.ic.ferret.data.ResultSet;
import com.google.research.ic.ferret.data.SearchEngine;
import com.google.research.ic.ferret.data.Snippet;
import com.google.research.ic.ferret.data.SubSequence;
import com.google.research.ic.ferret.data.UberResultSet;
import com.google.research.ic.ferret.data.attributes.Attribute;
import com.google.research.ic.ferret.data.attributes.CategoricalAttribute;
import com.google.research.ic.ferret.data.attributes.DateTimeAttribute;
import com.google.research.ic.ferret.data.attributes.NumericalAttribute;
import com.google.research.ic.ferret.test.Debug;
import com.google.research.ic.ferret.test.Shell;

@Path("/entry-point")
public class RESTHandler {

  Gson gson = null;
  static int numPollAttempts = 0;
  static boolean stopped = false;
  
  private Gson getGson() {
    if (gson == null) {
      gson = LogLoader.getLogLoader().getGson();
    }
    return gson;
  }
  
  @GET
  @Path("pollForEvents")
  @Produces(MediaType.APPLICATION_JSON)
  public String pollForEvents(@QueryParam("reset") boolean shouldReset) {
    Session session = Session.getCurrentSession();
    Debug.log("started session"); 
    
    if (shouldReset) {
      session.resetCurrentQuery();
      stopped = false;
      return null;
    }

    if (stopped) {
      return "{ \"status\" : \"stopped\" }";
    } else {
//    List<Event> events = session.dequeueDemoEvents();
      Snippet q = session.getCurrentQuery();
      String response = null;
      synchronized(session) {
        response = getGson().toJson(q, Snippet.class); 
      }
      //String response = getGson().toJson(events, ArrayList.class); // Hmmm, not totally safe b/c events is a List, not ArrayList. But I know what it really is...
      //String response2 = "{ \"numPollAttempts\" : \"" + numPollAttempts++ + "\" }";
      if (response != null && !response.equals("null")) {
        Debug.log("polled, responding " + response + " which has " + response.length());
//        response = null;
      }
      return response;
    }
  }
  
  @POST
  @Path("	")
  @Produces(MediaType.APPLICATION_JSON)
  public String getDetailedResultsForQuery(
      @FormParam("q") String querySpec,
      @FormParam("limit") int limit) {
    Debug.log("Received queryString: " + querySpec);
    Snippet currentQuery = null;
    if (querySpec.equals("current")) {
      currentQuery =  Session.getCurrentSession().getCurrentQuery();      
    } else {
      currentQuery = LogLoader.getLogLoader().getGson().fromJson(querySpec, Snippet.class);
    }
    Debug.log("Received query: " + currentQuery);

    Debug.log("limit was " + limit);

    if (limit == 0) {
      limit = 50;
    }
    
    if (currentQuery != null) {
      long t = System.currentTimeMillis();
      Debug.log("Started searching...");
      ResultSet resultSet = SearchEngine.getSearchEngine().findMatches(currentQuery).getStrongMatches();
      Debug.log("Finished searching after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");
      ResultSet trimmedResultSet = resultSet.filter(new FilterSpec(0.0, 5.0, 20));

      t = System.currentTimeMillis();
      String gsonString = getGson().toJson(trimmedResultSet);   
      Debug.log("Finished parsing JSON after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");
      Debug.log("Returning detailed results " + gsonString);
      return gsonString;
    } else {
      return null;
    }
  }

  @POST
  @Path("getSubSequence")
  @Produces(MediaType.APPLICATION_JSON)
  public String getSubSequence(@FormParam("logId") String logId,
      @FormParam("startIndex") int startIndex,
      @FormParam("endIndex") int endIndex) {
    
    Snippet log = SearchEngine.getSearchEngine().getLogById(logId);
    SubSequence subS = new SubSequence(startIndex, endIndex, log, -1.0); // uh oh, might be overloading SubSequence here
    String gsonString = getGson().toJson(subS);
    return gsonString;
  }
  
  @POST
  @Path("getSummaryResultsForQuery")
  @Produces(MediaType.APPLICATION_JSON)
  public String getSummaryResultsForQuery(
      @FormParam("q") String querySpec) {
    stopped = true;
    Debug.log("Received queryString: " + querySpec);
    Snippet currentQuery = null;
    if (querySpec.equals("current")) {
      Debug.log("current, using session file");
      currentQuery =  Session.getCurrentSession().getCurrentQuery();      
    } else {
      Debug.log("not current, using JSON from front end" + querySpec);
      currentQuery = LogLoader.getLogLoader().getGson().fromJson(querySpec, Snippet.class);
      Debug.log("after loading, currentQuery=" + currentQuery);
    }
    Debug.log("Getting summary results, Received query: " + currentQuery);
    if (currentQuery != null && !currentQuery.equals("")) {

      long t = System.currentTimeMillis();
      UberResultSet urs = SearchEngine.getSearchEngine().findMatches(currentQuery);
      Session.getCurrentSession().setCurrentResultSet(urs);
      Debug.log("Finished searching after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");

      
      Debug.log("Query is: ");
      if (Config.debug) Shell.printEvents(currentQuery, 0, currentQuery.size());
            
      FilteredResultSummary[] summaries = new FilteredResultSummary[4];

      ResultSet strongMatches = urs.getStrongMatches();
      
      FilteredResultSet frs = null;
      int resultSize = 0;
      
      if (strongMatches != null) {
        t = System.currentTimeMillis();
        urs.getStrongMatches().getAttributeSummaries(); // force computation of AttrSummaries 
                                                        //TODO: there has to be a more elegant way!
        frs = strongMatches.filter(new FilterSpec(-1.0, -1.0, -1));
        resultSize = 0;
        if (frs.getResults() != null) {
          resultSize = frs.getResults().size();
          summaries[0] = frs.getSummary();
          summaries[0].setDisplayName("Strong Matches");
        } 
        Debug.log("found " + resultSize + " strong matches after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");        
      }
      
      ResultSet elongations = urs.getElongatedMatches();

      if (elongations != null) {
        t = System.currentTimeMillis();  
        urs.getElongatedMatches().getAttributeSummaries(); // force computation of AttrSummaries
        frs = elongations.filter(new FilterSpec(-1.0, -1.0, -1));
        resultSize = 0;
        if (frs.getResults() != null) {
          resultSize = frs.getResults().size();
          summaries[1] = frs.getSummary();
          summaries[1].setDisplayName("Elongations");
        }
        Debug.log("found " + resultSize + " elongated matches after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");
      }

      ResultSet altEndMatches = urs.getAltEndingMatches();

      if (altEndMatches != null) {
        t = System.currentTimeMillis();
        urs.getAltEndingMatches().getAttributeSummaries(); // force computation of AttrSummaries        
        frs = altEndMatches.filter(new FilterSpec(-1.0, -1.0, -1));
        resultSize = 0;
        if (frs.getResults() != null) {    
          resultSize = frs.getResults().size();
          summaries[2] = frs.getSummary();
          summaries[2].setDisplayName("Alternate Endings");
        }
        Debug.log("found " + resultSize + " altEnd matches after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");
      }

      ResultSet weakMatches = urs.getWeakMatches();

      if (weakMatches != null) {
        t = System.currentTimeMillis();
        urs.getWeakMatches().getAttributeSummaries(); // force computation of AttrSummaries
        frs = weakMatches.filter(new FilterSpec(-1.0, -1.0, -1));
        if (frs.getResults() != null) {    
          resultSize = frs.getResults().size();
          summaries[3] = frs.getSummary();
          summaries[3].setDisplayName("Weak Matches");
        }
        Debug.log("found " + resultSize + " weak matches after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");
      }

      
      t = System.currentTimeMillis();
      String gsonString = getGson().toJson(summaries);   
      Debug.log("Finished parsing JSON after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");
      Debug.log("Gonna return " + gsonString);
      return gsonString;
    } else {
      return "No results";
    }
  }
  
  @POST
  @Path("getFilteredResults")
  @Produces(MediaType.APPLICATION_JSON)
  public String getFilteredResults(
      @FormParam("filterParams") String filterParams) {

    Debug.log("filterParams: " + filterParams);

    JsonParser parser = new JsonParser();
    JsonObject rootObj = parser.parse(filterParams).getAsJsonObject();
    String attrName = rootObj.get("attrName").getAsString();
    String values = rootObj.get("values").getAsString();
    
    JsonObject rSummObj = rootObj.getAsJsonObject("rSummary");
    FilteredResultSummary rSummary = getGson().fromJson(rSummObj, FilteredResultSummary.class);
    FilterSpec fSpec = null;
    Attribute attr = rSummary.getAttributes().get(attrName);

    Debug.log("values: " + values);
    Debug.log("attrType: " + attr.getType());
    
    if (attr.getType().equals(CategoricalAttribute.TYPE)) {
      String operator = FilterSpec.EQUALS;
      fSpec = new FilterSpec(rSummary.getMinDist(), rSummary.getMaxDist(), 20, attrName, operator, values);      
    } else if (attr.getType().equals(NumericalAttribute.TYPE)) {
      if (values.contains("-")) {      
        String min = values.split("-")[0];
        String max = values.split("-")[1];
        String operator = FilterSpec.BETWEEN;
        fSpec = new FilterSpec(rSummary.getMinDist(), rSummary.getMaxDist(), 20, 
            attrName, operator, Double.parseDouble(min), Double.parseDouble(max));
      } else {
        // assume a single value
        String operator = FilterSpec.EQUALS;
        fSpec = new FilterSpec(rSummary.getMinDist(), rSummary.getMaxDist(), 20, 
            attrName, operator, Double.parseDouble(values));        
      }      
    } else if (attr.getType().equals(DateTimeAttribute.TYPE)) {
      if (values.contains("-")) {      
        String min = values.split("-")[0];
        String max = values.split("-")[1];
        String operator = FilterSpec.BETWEEN;
        
        Debug.log("Creating FilterSpec to return " + operator + " " + min + " and " + max);
        fSpec = new FilterSpec(rSummary.getMinDist(), rSummary.getMaxDist(), 20, 
            attrName, operator, new Date(Long.parseLong(min)), new Date(Long.parseLong(max)));
      } else {
        // assume a single value
        String operator = FilterSpec.EQUALS;
        fSpec = new FilterSpec(rSummary.getMinDist(), rSummary.getMaxDist(), 20, 
            attrName, operator, new Date(Long.parseLong(values)));    
      }
    }

    ResultSet currentResults = Session.getCurrentSession().getCurrentResultSet().getStrongMatches();
    if (currentResults != null) {
      FilteredResultSet filteredResults = currentResults.filter(fSpec);
      String jsonString = getGson().toJson(filteredResults);   
      //Debug.log("*** Returning Filtered Results: " + jsonString);
      return jsonString;
    } else {
      return null;
    }
  }
  
  /**
   * Used for testing - returns a list of demo snippets to the UI
   * @return
   */
  
  @GET
  @Path("getDemoSnippets")
  @Produces(MediaType.APPLICATION_JSON)
  public String getDemoSnippets() {

    List<Snippet> demoSnippets = DemoManager.getDemoManager().getAllDemoSnippets();
    if (demoSnippets == null || demoSnippets.size() == 0) {
      return null;
    } else {
      return getGson().toJson(demoSnippets);
    }
  }
  
  // OLD AND DEPRECATED BELOW HERE
  
  @GET
  @Path("test")
  @Produces(MediaType.TEXT_PLAIN)
  public String test() {
    return "Test";
  }
  
  @GET
  @Path("foobar")
  @Produces(MediaType.APPLICATION_JSON)
  public String json() {

    return new String("{ \"foo\" : \"bar\" }");
  
  }

  @GET
  @Path("demoSnippet")
  @Produces(MediaType.APPLICATION_JSON)
  public String demoSnippet(@QueryParam("demo") String demo) {
    Debug.log("received query param demo = " + demo);
    List<Snippet> snips = DemoManager.getDemoManager().getAllDemoSnippets();
    Debug.log("Got " + snips + " about to return them as JSON");
    if (snips == null || snips.size() == 0) {
      return "{ \"error\": \"no snips\" }";
    }
    Session.getCurrentSession().setCurrentQuery(snips.get(0));
    String gsonString = getGson().toJson(snips.get(0)); // just do the first one
    return gsonString;
  }
  
  @GET
  @Path("__getDetailedResultsForQuery")
  @Produces(MediaType.APPLICATION_JSON)
  public String submitDemoQuery(
      @QueryParam("q") String querySpec, 
      @QueryParam("limit") int limit) {
    Debug.log("Received queryString: " + querySpec);
    Snippet currentQuery = null;
    if (querySpec.equals("current")) {
      currentQuery =  Session.getCurrentSession().getCurrentQuery();      
    } else {
      currentQuery = LogLoader.getLogLoader().getGson().fromJson(querySpec, Snippet.class);
    }
    Debug.log("Received query: " + currentQuery);

    Debug.log("limit was " + limit);

    if (limit == 0) {
      limit = 50;
    }
    
    if (currentQuery != null) {
      long t = System.currentTimeMillis();
      Debug.log("Started searching...");
      ResultSet resultSet = SearchEngine.getSearchEngine().findMatches(currentQuery).getStrongMatches();
      Debug.log("Finished searching after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");
      ResultSet trimmedResultSet = resultSet.filter(new FilterSpec(0.0, 5.0, 20));

      t = System.currentTimeMillis();
      String gsonString = getGson().toJson(trimmedResultSet);   
      Debug.log("Finished parsing JSON after " + ((System.currentTimeMillis() - t)/1000.0) + " secs");
      Debug.log("Gonna return " + gsonString);
      return gsonString;
    } else {
      return null;
    }
  }

}
