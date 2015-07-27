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
package com.google.research.ic.ferret;

import com.google.research.ic.ferret.data.ResultSet;
import com.google.research.ic.ferret.data.attributes.Bin;
import com.google.research.ic.ferret.test.Debug;

import java.awt.Dimension;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * TODO: Insert description here. (generated by marknewman)
 */
public class ResultSetSummaryPanel extends JPanel {

  
  public ResultSetSummaryPanel(ResultSet results) {
    Map <String, List<Bin>> summaries = results.getAttributeSummaries();
    
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    // create summary panels
    JPanel summaryPanel = null;
    JLabel summaryLabel = null;
    
    Debug.log("Creating results panel. Summaries: " + summaries);
    
    for (String s : summaries.keySet() ) {
      List<Bin> sumBins = summaries.get(s);
      AttributeSummaryPanel attrSumPanel = new AttributeSummaryPanel(sumBins);
      add(attrSumPanel);
    }    
    setPreferredSize(new Dimension(400, 400));
  }
  
}